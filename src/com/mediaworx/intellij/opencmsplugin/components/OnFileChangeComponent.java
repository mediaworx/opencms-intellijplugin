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
import com.mediaworx.intellij.opencmsplugin.cmis.VfsAdapter;
import com.mediaworx.intellij.opencmsplugin.configuration.ModuleExportPoint;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsModule;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;
import com.mediaworx.intellij.opencmsplugin.exceptions.CmsPermissionDeniedException;
import com.mediaworx.intellij.opencmsplugin.tools.PathTools;
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
    private OpenCmsPluginConfigurationData config;

    public OnFileChangeComponent(Project project) {
        this.project = project;
        this.config = project.getComponent(OpenCmsPluginConfigurationComponent.class).getConfigurationData();
    }


    @NotNull
    public String getComponentName() {
        return "OpenCmsPluginOnFileChangeComponent";
    }

    public void initComponent() {

        if (config != null && config.isOpenCmsPluginActive()) {

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
				vfsAdapter = project.getComponent(OpenCmsPluginComponent.class).getVfsAdapter();
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

			for (VFileEvent event : vFileEvents) {

				// File is deleted
				if (event instanceof VFileDeleteEvent) {

					VirtualFile file = event.getFile();

					if (file != null) {
						System.out.println("The following file was deleted: " + file.getPath());

						// check if the file belongs to an OpenCms module
						if (PathTools.isFileInModulePath(config, file)) {
							String vfsPath = PathTools.getVfsPathFromIdeaVFile(PathTools.getModuleName(config, file), config, file);
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

						VirtualFile oldParent = ((VFileMoveEvent) event).getOldParent();
						VirtualFile newParent = ((VFileMoveEvent) event).getNewParent();

						// old and new parent are in a module -> move the file in the OpenCms VFS
						if (PathTools.isFileInModulePath(config, oldParent) && PathTools.isFileInModulePath(config, newParent)) {
							String module = PathTools.getModuleName(config, file);
							String oldParentPath = PathTools.getVfsPathFromIdeaVFile(module, config, oldParent);
							String vfsPath = oldParentPath + "/" + file.getName();
							if (getVfsAdapter().exists(vfsPath)) {
								String newParentPath = PathTools.getVfsPathFromIdeaVFile(module, config, newParent);
								vfsFilesToBeMoved.add(new VfsFileMoveInfo(vfsPath, oldParentPath, newParentPath));
							}
						}

						// if the new parent path is not inside a module, remove it
						else if (PathTools.isFileInModulePath(config, oldParent) && !PathTools.isFileInModulePath(config, newParent)) {
							String oldParentPath = PathTools.getVfsPathFromIdeaVFile(PathTools.getModuleName(config, oldParent), config, oldParent);
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
						VirtualFile file = event.getFile();
						if (file != null) {
							System.out.println("The following file was renamed: " + file.getPath());

							String oldName = (String) ((VFilePropertyChangeEvent) event).getOldValue();
							String newName = (String) ((VFilePropertyChangeEvent) event).getNewValue();
							String module = PathTools.getModuleName(config, file);
							String newVfsPath = PathTools.getVfsPathFromIdeaVFile(module, config, file);
							String oldVfsPath = newVfsPath.replaceFirst(newName, oldName);

							if (getVfsAdapter().exists(oldVfsPath)) {
								vfsFilesToBeRenamed.add(new VfsFileRenameInfo(oldVfsPath, newName));
							}
						}
					}
				}
			}

			List<ModuleExportPoint> allExportPoints = null;
			List<File> refreshFiles = null;

			if (vfsFilesToBeDeleted.size() > 0 || vfsFilesToBeMoved.size() > 0 || vfsFilesToBeRenamed.size() > 0) {
				allExportPoints = new ArrayList<ModuleExportPoint>();
				refreshFiles = new ArrayList<File>();
				for (OpenCmsModule module : config.getModules().values()) {
					List<ModuleExportPoint> exportPoints = module.getExportPoints();
					if (exportPoints != null) {
						for (ModuleExportPoint exportPoint : exportPoints) {
							allExportPoints.add(exportPoint);
						}
					}
				}

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
							Folder oldParent = (Folder) getVfsAdapter().getVfsObject(moveInfo.getOldParentPath());
							Folder newParent = (Folder) getVfsAdapter().getVfsObject(moveInfo.getNewParentPath());
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
