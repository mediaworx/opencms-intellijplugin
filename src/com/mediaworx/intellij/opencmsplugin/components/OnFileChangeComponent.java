package com.mediaworx.intellij.opencmsplugin.components;

import com.intellij.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManagerAdapter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent;
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import com.mediaworx.intellij.opencmsplugin.configuration.ModuleExportPoint;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;
import com.mediaworx.intellij.opencmsplugin.exceptions.CmsPermissionDeniedException;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModules;
import com.mediaworx.intellij.opencmsplugin.sync.VfsAdapter;
import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * handles changes (edits, moves, renames, deletions) to files in the IntelliJ File System
 */
public class OnFileChangeComponent implements ProjectComponent {

    private Project project;
	private OpenCmsPlugin plugin;
    private OpenCmsPluginConfigurationData config;

    public OnFileChangeComponent(Project project) {
        this.project = project;
    }


    @NotNull
    public String getComponentName() {
        return "OpenCmsPluginOnFileChangeComponent";
    }

    public void initComponent() {

	    plugin = project.getComponent(OpenCmsPlugin.class);
        config = plugin.getPluginConfiguration();

	    MessageBus bus = ApplicationManager.getApplication().getMessageBus();
        MessageBusConnection connection = bus.connect();

        connection.subscribe(AppTopics.FILE_DOCUMENT_SYNC,
                new FileDocumentManagerAdapter() {
                    @Override
                    public void beforeDocumentSaving(@NotNull Document document) {
                        System.out.println("Document was saved: "+document);
                        // TODO: Überlegen, ob File Save Events direkt ins VFS gesynct werden sollen
                    }
                });

        connection.subscribe(VirtualFileManager.VFS_CHANGES, fileChangeListener);
    }

    public void disposeComponent() {
    }

    public void projectOpened() {
    }

    public void projectClosed() {
    }

	private BulkFileListener fileChangeListener = new BulkFileListener() {

		VfsAdapter vfsAdapter;

		private VfsAdapter getVfsAdapter() {

			if (vfsAdapter == null) {
				vfsAdapter = plugin.getVfsAdapter();
			}

			// Not connected yet (maybe OpenCms wasn't started when the project opened)
			if (!vfsAdapter.isConnected()) {
				// Try to connect
				vfsAdapter.startSession();

				// Still not connected? Show an error message and stop
				if (!vfsAdapter.isConnected()) {
					System.out.println("VFS adapter is not connected. File change events won't be handled!");
				}
			}
			return vfsAdapter;
		}

		public void before(@NotNull List<? extends VFileEvent> vFileEvents) {
		}

		public void after(@NotNull List<? extends VFileEvent> vFileEvents) {

			ArrayList<String> vfsFilesToBeDeleted = new ArrayList<String>();
			ArrayList<VfsFileMoveInfo> vfsFilesToBeMoved = new ArrayList<VfsFileMoveInfo>();
			ArrayList<VfsFileRenameInfo> vfsFilesToBeRenamed = new ArrayList<VfsFileRenameInfo>();

			OpenCmsModules ocmsModules = plugin.getOpenCmsModules();

			for (VFileEvent event : vFileEvents) {

				// File is deleted
				if (event instanceof VFileDeleteEvent) {

					VirtualFile ideaVFile = event.getFile();

					if (ideaVFile != null) {
						System.out.println("The following file was deleted: " + ideaVFile.getPath());

						OpenCmsModule ocmsModule = ocmsModules.getModuleForIdeaVFile(ideaVFile);

						// check if the file belongs to an OpenCms module
						if (ocmsModule  != null) {
							String vfsPath = ocmsModule.getVfsPathForIdeaVFile(ideaVFile);
							if (getVfsAdapter().exists(vfsPath)) {
								vfsFilesToBeDeleted.add(vfsPath);
							}
						}
					}
				}

				// File is moved
				if (event instanceof VFileMoveEvent) {
					VirtualFile file = event.getFile();

					if (file != null) {
						System.out.println("The following file was moved: " + file.getPath());

						VirtualFile oldParent = ((VFileMoveEvent)event).getOldParent();
						VirtualFile newParent = ((VFileMoveEvent)event).getNewParent();
						OpenCmsModule oldParentOcmsModule = ocmsModules.getModuleForIdeaVFile(oldParent);
						OpenCmsModule newParentOcmsModule = ocmsModules.getModuleForIdeaVFile(newParent);

						// old and new parent are in a module -> move the file in the OpenCms VFS
						if (oldParentOcmsModule != null && newParentOcmsModule != null) {
							String oldParentPath = oldParentOcmsModule.getVfsPathForIdeaVFile(oldParent);
							String vfsPath = oldParentPath + "/" + file.getName();
							if (getVfsAdapter().exists(vfsPath)) {
								String newParentPath = newParentOcmsModule.getVfsPathForIdeaVFile(newParent);
								vfsFilesToBeMoved.add(new VfsFileMoveInfo(vfsPath, oldParentPath, newParentPath));
							}
						}

						// if the new parent path is not inside a module, remove it
						else if (oldParentOcmsModule != null && newParentOcmsModule == null) {
							String oldParentPath = oldParentOcmsModule.getVfsPathForIdeaVFile(oldParent);
							String vfsPath = oldParentPath + "/" + file.getName();

							System.out.println("File was moved out of the module path, deleting " + vfsPath);

							if (getVfsAdapter().exists(vfsPath)) {
								vfsFilesToBeDeleted.add(vfsPath);
							}
						}
					}
				}

				// File is renamed
				if (event instanceof VFilePropertyChangeEvent) {

					String propertyName = ((VFilePropertyChangeEvent)event).getPropertyName();
					if ("name".equals(propertyName)) {
						VirtualFile ideaVFile = event.getFile();
						if (ideaVFile != null) {
							System.out.println("The following file was renamed: " + ideaVFile.getPath());

							OpenCmsModule ocmsModule = ocmsModules.getModuleForIdeaVFile(ideaVFile);

							if (ocmsModule != null) {
								String oldName = (String)((VFilePropertyChangeEvent) event).getOldValue();
								String newName = (String)((VFilePropertyChangeEvent) event).getNewValue();
								String newVfsPath = ocmsModule.getVfsPathForIdeaVFile(ideaVFile);
								String oldVfsPath = newVfsPath.replaceFirst(newName, oldName);

								if (getVfsAdapter().exists(oldVfsPath)) {
									vfsFilesToBeRenamed.add(new VfsFileRenameInfo(oldVfsPath, newName));
								}
							}
						}
					}
				}
			}

			List<ModuleExportPoint> allExportPoints = null;
			List<File> refreshFiles = null;

			if (vfsFilesToBeDeleted.size() > 0 || vfsFilesToBeMoved.size() > 0 || vfsFilesToBeRenamed.size() > 0) {
				refreshFiles = new ArrayList<File>();
				allExportPoints = ocmsModules.getAllExportPoints();
			}

			// Delete files
			if (vfsFilesToBeDeleted.size() > 0) {

				StringBuilder msg = new StringBuilder("Do you want to delete the following files/folders from the OpenCms VFS as well?");
				for (String vfsFileToBeDeleted : vfsFilesToBeDeleted) {
					msg.append("\n").append(vfsFileToBeDeleted);
				}

				int dlgStatus = Messages.showOkCancelDialog(msg.toString(), "Delete Files/Folders?", Messages.getQuestionIcon());

				if (dlgStatus == 0) {
					for (String vfsFileToBeDeleted : vfsFilesToBeDeleted) {
						getVfsAdapter().deleteResource(vfsFileToBeDeleted);
						// check export points
						for (ModuleExportPoint exportPoint : allExportPoints) {
							if (vfsFileToBeDeleted.startsWith(exportPoint.getVfsSource())) {
				                String destination = exportPoint.getRfsTarget();
				                String relativePath = vfsFileToBeDeleted.substring(exportPoint.getVfsSource().length());
					            String exportedPath = config.getWebappRoot() + File.separator + destination + relativePath;
					            File exportedFileToBeDeleted = new File(exportedPath);
					            if (exportedFileToBeDeleted.exists()) {
						            FileUtils.deleteQuietly(exportedFileToBeDeleted);
									refreshFiles.add(exportedFileToBeDeleted);
					            }
					            break;
							}
						}
					}
				}
			}

			// Move files
			if (vfsFilesToBeMoved.size() > 0) {
				StringBuilder msg = new StringBuilder("Do you want to move the following files/folders in the OpenCms VFS as well?");
				for (VfsFileMoveInfo vfsFileToBeMoved : vfsFilesToBeMoved) {
					msg.append("\n").append(vfsFileToBeMoved.getVfsPath());
				}

				int dlgStatus = Messages.showOkCancelDialog(msg.toString(), "Move Files/Folders?", Messages.getQuestionIcon());

				if (dlgStatus == 0) {
					for (VfsFileMoveInfo moveInfo : vfsFilesToBeMoved) {
						try {
							System.out.println("Moving " + moveInfo.getVfsPath() + " to " + moveInfo.getNewParentPath());
							Folder oldParent = (Folder)getVfsAdapter().getVfsObject(moveInfo.getOldParentPath());
							Folder newParent = (Folder)getVfsAdapter().getVfsObject(moveInfo.getNewParentPath());
							if (newParent == null) {
								newParent = getVfsAdapter().createFolder(moveInfo.getNewParentPath());
							}
							FileableCmisObject file = (FileableCmisObject) getVfsAdapter().getVfsObject(moveInfo.getVfsPath());
							file.move(oldParent, newParent);

							// TODO: ExportPoints berücksichtigen, wenn ein File in eine Export-Struktur verschoben wurde oder/und aus einer Export-Struktur entfernt wurde
						}
						catch (CmsPermissionDeniedException e) {
							Messages.showDialog("Error moving files/folders." + e.getMessage(),
									"Error", new String[]{"Ok"}, 0, Messages.getErrorIcon());
						}
					}
				}
			}

			// Rename files
			if (vfsFilesToBeRenamed.size() > 0) {
				StringBuilder msg = new StringBuilder("Do you want to rename the following files/folders in the OpenCms VFS as well?");
				for (VfsFileRenameInfo vfsFileToBeRenamed : vfsFilesToBeRenamed) {
					msg.append("\n").append(vfsFileToBeRenamed.getOldVfsPath()).append(" -> ").append(vfsFileToBeRenamed.getNewName());
				}

				int dlgStatus = Messages.showOkCancelDialog(msg.toString(), "Move Files/Folders?", Messages.getQuestionIcon());

				if (dlgStatus == 0) {
					for (VfsFileRenameInfo renameInfo : vfsFilesToBeRenamed) {
						System.out.println("Renaming " + renameInfo.getOldVfsPath() + " to " + renameInfo.getNewName());
						try {
							CmisObject file = getVfsAdapter().getVfsObject(renameInfo.oldVfsPath);
							HashMap<String, Object> properties = new HashMap<String, Object>();
							properties.put(PropertyIds.NAME, renameInfo.getNewName());
							file.updateProperties(properties);

							// TODO: ExportPoints berücksichtigen, wenn ein File innerhalb einer Export-Struktur umbenannt wurde.
						}
						catch (CmsPermissionDeniedException e) {
							System.out.println("Exception moving files: " + e.getMessage());
							Messages.showDialog("Error moving files/folders. " + e.getMessage(),
									"Error", new String[]{"Ok"}, 0, Messages.getErrorIcon());
						}
					}
				}
			}

			if (refreshFiles != null && refreshFiles.size() > 0) {
				LocalFileSystem.getInstance().refreshIoFiles(refreshFiles);
			}
		}
	};


    private static class VfsFileMoveInfo {

        private String vfsPath;
        private String oldParentPath;
        private String newParentPath;

        private VfsFileMoveInfo(String vfsPath, String oldParentPath, String newParentPath) {
            this.vfsPath = vfsPath;
            this.oldParentPath = oldParentPath;
            this.newParentPath = newParentPath;
        }

        public String getVfsPath() {
            return vfsPath;
        }

        public String getOldParentPath() {
            return oldParentPath;
        }

        public String getNewParentPath() {
            return newParentPath;
        }
    }

    private static class VfsFileRenameInfo {
        String oldVfsPath;
        String newName;

        private VfsFileRenameInfo(String oldVfsPath, String newName) {
            this.oldVfsPath = oldVfsPath;
            this.newName = newName;
        }

        public String getOldVfsPath() {
            return oldVfsPath;
        }

        public String getNewName() {
            return newName;
        }
    }
}
