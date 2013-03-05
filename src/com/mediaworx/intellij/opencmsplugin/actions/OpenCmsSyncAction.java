package com.mediaworx.intellij.opencmsplugin.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.mediaworx.intellij.opencmsplugin.cmis.VfsAdapter;
import com.mediaworx.intellij.opencmsplugin.components.OpenCmsPluginComponent;
import com.mediaworx.intellij.opencmsplugin.components.OpenCmsPluginConfigurationComponent;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;
import com.mediaworx.intellij.opencmsplugin.entities.*;
import com.mediaworx.intellij.opencmsplugin.exceptions.CmsPermissionDeniedException;
import com.mediaworx.intellij.opencmsplugin.tools.PathTools;
import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;

import java.io.File;
import java.util.*;

// TODO: prüfen und überlegen, ob es möglich und sinnvoll ist, Auto-Sync bei File change events zu realisieren
// TODO: Sync von JSP-Files -> eher nicht, da diese von OpenCms automatisch aktualisiert werden
public class OpenCmsSyncAction extends AnAction {

    Project project;
    OpenCmsPluginConfigurationData config;
    VfsAdapter vfsAdapter;
	private SyncJob syncJob;

	@Override
	public void actionPerformed(AnActionEvent event) {

	    this.project = DataKeys.PROJECT.getData(event.getDataContext());
		this.config = project.getComponent(OpenCmsPluginConfigurationComponent.class).getConfigurationData();

		System.out.println("Event: " + event);
		System.out.println("Config: " + config);
		System.out.println("Plugin active? " + config.isOpenCmsPluginActive());

		if (config.isOpenCmsPluginActive()) {

	        this.vfsAdapter = project.getComponent(OpenCmsPluginComponent.class).getVfsAdapter();

	        if (vfsAdapter != null) {

		        // Not connected yet (maybe OpenCms wasn't started when the project opened)
		        if (!vfsAdapter.isConnected()) {
			        // Try again to connect
			        vfsAdapter.startSession();

			        // Still not connected? Show an error message and stop
			        if (!vfsAdapter.isConnected()) {
				        Messages.showDialog("Connection to OpenCms VFS failed. Is OpenCms running?",
						        "Error", new String[]{"Ok"}, 0, Messages.getErrorIcon());

				        // Get the hell out of here
				        return;
			        }
		        }

	            this.vfsAdapter.clearCache();

	            FileDocumentManager.getInstance().saveAllDocuments();
		        FileDocumentManager.getInstance().reloadFiles();

		        VirtualFile[] selectedFiles = event.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
		        int numSyncEntities;
		        this.syncJob = new SyncJob(project, config, vfsAdapter);

		        StringBuilder message = new StringBuilder();

		        try {
			        executeFileAnalysis(selectedFiles, message);
			        if (syncJob == null) {
				        return;
			        }
			        numSyncEntities = syncJob.numSyncEntities();
		        }
		        catch (CmsPermissionDeniedException e) {
			        Messages.showDialog("Error! OpenCms VFS-Adapter says:\n" + e.getMessage(),
					        "Error", new String[]{"Ok"}, 0, Messages.getErrorIcon());

			        // Get the hell out of here
			        return;
		        }

		        boolean proceed = syncJob != null && syncJob.hasSyncEntities();
				System.out.println("proceed? "+proceed);

	            if (numSyncEntities == 0) {
	                proceed = false;
		            message.append("Nothing to sync");
	                Messages.showMessageDialog(message.toString(), "OpenCms VFS Sync", Messages.getInformationIcon());
	            }
	            else if (numSyncEntities > 1) {
		            message.append("The following ").append(numSyncEntities).append(" files or folders will be synced to or from OpenCms VFS:\n\n");
					List<SyncEntity> syncEntities = syncJob.getSyncList();
		            for (SyncEntity syncEntity : syncEntities) {
			            message.append(syncEntity.getSyncMode() == SyncMode.PUSH ? "PUSH " : "PULL ");
			            message.append(syncEntity.getVfsPath());
			            if (!syncEntity.replaceExistingEntity()) {
				            message.append(" (new)");
			            }
					    message.append("\n");
		            }
		            message.append("\nProceed?");
	                int dlgStatus = Messages.showOkCancelDialog( message.toString(), "Start OpenCms VFS Sync?", Messages.getQuestionIcon());
	                proceed = dlgStatus == 0;
	            }

	            if (proceed) {
	                syncJob.execute();
	                VirtualFileManager.getInstance().refresh(false);
	            }
	        }
		}
	}

	private void executeFileAnalysis(final VirtualFile[] selectedFiles, final StringBuilder message) throws CmsPermissionDeniedException {
		Runnable deployRunner = new Runnable() {

			public void run() {

				ProgressIndicatorManager progressIndicatorManager = new ProgressIndicatorManager() {
					ProgressIndicator indicator;

					public void setText(final String text) {
						indicator.setText(text);
					}

					public void init() {
						indicator = ProgressManager.getInstance().getProgressIndicator();
						indicator.setIndeterminate(true);
						indicator.setText("Please wait");
					}

					public boolean isCanceled() {
						return indicator.isCanceled();
					}
				};

				progressIndicatorManager.init();

				try {
					handleSelectedFiles(selectedFiles, message, progressIndicatorManager);
					if (progressIndicatorManager.isCanceled()) {
						syncJob = null;
						return;
					}
				}
				catch (CmsPermissionDeniedException e) {
					message.append(e.getMessage());
				}
			}
		};

		ProgressManager.getInstance().runProcessWithProgressSynchronously(deployRunner, "Analyzing local and VFS files and folders ...", true, project);
	}

	private void handleSelectedFiles(final VirtualFile[] selectedFiles, StringBuilder message, ProgressIndicatorManager progressIndicatorManager) throws CmsPermissionDeniedException {
		for (VirtualFile selectedFile : selectedFiles) {
			final String pathLC = selectedFile.getPath().toLowerCase();
			if (pathLC.contains(".git")
					|| pathLC.contains(".svn")
					|| pathLC.contains(".cvs")
					|| selectedFile.getName().equals("#SyncJob.txt")
					|| selectedFile.getName().equals(".config")
					|| selectedFile.getName().equals("manifest.xml")) {
				// do nothing (filter VCS files and OpenCms Sync Metadata)
				System.out.println("File is ignored");
			}
			// File is not in the sync path, ignore
			else if (!PathTools.isFileInModulePath(config, selectedFile)) {
				message.append("Ignoring '" + selectedFile.getPath() + "' (not a module path).\n");
				System.out.println("File is not in the sync path, ignore");
			}
			else {
				System.out.println("Handling a module or a file in a module");
				handleFile(selectedFile, SyncMode.AUTO, progressIndicatorManager);
			}
		}
	}

	private void handleFile(final VirtualFile file, SyncMode syncMode, ProgressIndicatorManager progressIndicatorManager) throws CmsPermissionDeniedException {

		if (progressIndicatorManager.isCanceled()) {
			return;
		}
		System.out.println("Handle file/folder " + file.getPath());

		String module = PathTools.getModuleName(config, file);
		System.out.println("Module: "+module);
        syncJob.initModuleExportPoints(module);

		walkFileTree(module, file, syncMode, progressIndicatorManager);
	}

	// TODO: Datum nicht aus IntelliJ-VFS, sondern Real-File
	// TODO: handle cases where a folder on the vfs has the same name as a file on the rfs or vice versa
	private void walkFileTree(String module, VirtualFile file, SyncMode syncMode, ProgressIndicatorManager progressIndicatorManager) throws CmsPermissionDeniedException {

		if (progressIndicatorManager.isCanceled()) {
			return;
		}
		if (module == null) {
			System.out.println("No modules configured");
			return;
		}

		String vfsPath = PathTools.getVfsPathFromIdeaVFile(PathTools.getModuleName(config, file), config, file);
        String rfsPath = file.getPath();

		System.out.println("VFS path is " + vfsPath);

		boolean vfsObjectExists;
		CmisObject vfsObject = null;

		if (syncMode != SyncMode.PUSH) {
			// Get the corresponding vfs object (if it exists)
			vfsObject = vfsAdapter.getVfsObject(vfsPath);

			vfsObjectExists = vfsObject != null;
			System.out.println(vfsPath + (vfsObjectExists ? " exists" : " does not exist"));
		}
		else {
			vfsObjectExists = false;
		}

        // It's a folder, check if it is already there and compare contents
        if (file.isDirectory()) {
            // The folder is not there, so push it with all child contents
            if (!vfsObjectExists) {
                System.out.println("It's a folder that does not exist on the VFS, PUSH recursively");
                addPushFolderTreeToSyncJob(module, vfsPath, file, false, progressIndicatorManager);
            }
            // The Folder is there, compare contents of VFS and RFS
            else {
                System.out.println("It's a folder that does exist on the VFS, compare");
 
                // Get folder content from the vfs, put it in a set
                System.out.println("Getting VFS content");
                System.out.print("Children: ");
                ItemIterable<CmisObject> vfsChildren = ((Folder) vfsObject).getChildren();
                Map<String, CmisObject> vfsChildMap = new LinkedHashMap<String, CmisObject>();
                for (CmisObject vfsChild : vfsChildren) {
                    vfsChildMap.put(vfsChild.getName(), vfsChild);
                    System.out.print(vfsChild.getName() + " ");
                }
                System.out.println();

                System.out.println("Looping RFS children");
                VirtualFile[] rfsChildren = file.getChildren();

                for (VirtualFile rfsChild : rfsChildren) {
	                if (progressIndicatorManager.isCanceled()) {
		                return;
	                }
	                String filename = rfsChild.getName();

                    // The file/folder does not exist on the VFS, recurse in PUSH mode
                    if (!vfsChildMap.containsKey(filename)) {
                        System.out.println("RFS child " + rfsChild.getName() + " is not on the VFS, handle it in PUSH mode");
                        walkFileTree(module, rfsChild, SyncMode.PUSH, progressIndicatorManager);
                    }
                    // The file/folder does exist on the VFS, recurse in AUTO mode
                    else {
                        System.out.println("RFS child " + rfsChild.getName() + " exists on the VFS, handle it in AUTO mode");
                        walkFileTree(module, rfsChild, SyncMode.AUTO, progressIndicatorManager);

                        // remove the file from the vfsChildren map, so that only files that exist only on the vfs will be left
                        vfsChildMap.remove(filename);
                    }
                }

                System.out.println("Handle files/folders that exist only on the vfs");
                // Handle files/folders that exist only on the vfs
                for (CmisObject vfsChild : vfsChildMap.values()) {
	                if (progressIndicatorManager.isCanceled()) {
		                return;
	                }
	                String childVfsPath = vfsPath + "/" + vfsChild.getName();
                    String childRfsPath = rfsPath + File.separator + vfsChild.getName();

                    // files
                    if (vfsChild.getBaseTypeId().equals(BaseTypeId.CMIS_DOCUMENT)) {
                        addPullFileToSyncJob(module, childVfsPath, childRfsPath,  vfsChild, false);
                    }
                    // folders
                    else if (vfsChild.getBaseTypeId().equals(BaseTypeId.CMIS_FOLDER)) {
                        addPullFolderTreeToSyncJob(module, childVfsPath, childRfsPath,  vfsChild, false);
                    }
                }

            }
        }
		// It's a file
		else if (!file.isSpecialFile()) {
			// The file is not there, so push it
			if (!vfsObjectExists) {
				System.out.println("It's a file that does not exist on the VFS, PUSH");
				addPushFileToSyncJob(module, vfsPath, file, null);
			}
			// The file exists, check which one is newer
			else {
				System.out.println("It's a file that exists on the VFS, compare dates");
				File realFile = new File(file.getPath());
				Date localDate = new Date(realFile.lastModified());
				Date vfsDate = vfsObject.getLastModificationDate().getTime();
				if (localDate.after(vfsDate)) {
					System.out.println("RFS file is newer, PUSH");
					addPushFileToSyncJob(module, vfsPath, file, (Document) vfsObject);
				}
				else if (vfsDate.after(localDate)) {
					System.out.println("VFS file is newer, PULL");
					addPullFileToSyncJob(module, vfsPath, rfsPath, vfsObject, true);
				}
				else {
					System.out.println("VFS file and RFS file have the same date, ignore");
				}
			}
		}

	}


	private SyncEntity getSyncEntity(SyncEntityType type, String vfsPath, String rfsPath, VirtualFile file, CmisObject vfsObject, SyncMode syncMode, boolean replaceExistingEntity) {
		SyncEntity entity;
		if (type == SyncEntityType.FILE) {
			entity = new SyncFile();
		}
		else {
			entity = new SyncFolder();
		}
		entity.setVfsPath(vfsPath);
        entity.setRfsPath(rfsPath);
		entity.setIdeaVFile(file);
		entity.setVfsObject(vfsObject);
		entity.setSyncMode(syncMode);
		entity.setReplaceExistingEntity(replaceExistingEntity);
		return entity;
	}

	private void addPushFileToSyncJob(String module, String vfsPath, VirtualFile file, Document vfsFile) {
		System.out.println("Adding PUSH file " + vfsPath);
		SyncFile syncFile = (SyncFile)getSyncEntity(SyncEntityType.FILE, vfsPath, file.getPath(), file, vfsFile, SyncMode.PUSH, vfsFile != null);
		syncJob.addSyncEntity(module, syncFile);
	}

	private void addPushFolderTreeToSyncJob(String module, String vfsPath, VirtualFile file, boolean replaceExistingEntity, ProgressIndicatorManager progressIndicatorManager) {
		System.out.println("Adding PUSH folder " + vfsPath);
		SyncFolder syncFile = (SyncFolder) getSyncEntity(SyncEntityType.FOLDER, vfsPath, file.getPath(), file, null, SyncMode.PUSH, replaceExistingEntity);
		syncJob.addSyncEntity(module, syncFile);

		System.out.println("Get children of folder " + vfsPath);
		VirtualFile[] children = file.getChildren();
		for (VirtualFile child : children) {
			System.out.println("Handle PUSH child " + child.getPath());
			try {
				walkFileTree(module, child, SyncMode.PUSH, progressIndicatorManager);
			}
			catch (CmsPermissionDeniedException e) {
				System.out.println("Exception walking the file tree: "+e.getMessage());
			}
		}
	}

    private void addPullFileToSyncJob(String module, String vfsPath, String rfsPath, CmisObject vfsObject, boolean replaceExistingEntity) {
        System.out.println("Adding PULL file " + vfsPath);
        SyncFile syncFile = (SyncFile)getSyncEntity(SyncEntityType.FILE, vfsPath, rfsPath, null, vfsObject, SyncMode.PULL, replaceExistingEntity);
        syncJob.addSyncEntity(module, syncFile);
    }

    private void addPullFolderTreeToSyncJob(String module, String vfsPath, String rfsPath, CmisObject vfsObject, boolean replaceExistingEntity) {
		System.out.println("Adding PULL folder " + vfsPath);
		SyncFolder syncFile = (SyncFolder) getSyncEntity(SyncEntityType.FOLDER, vfsPath, rfsPath, null, vfsObject, SyncMode.PULL, replaceExistingEntity);
		syncJob.addSyncEntity(module, syncFile);

		// traverse folder, add children to the SyncJob
		System.out.println("Get children of VFS folder " + vfsPath);
		ItemIterable<CmisObject> vfsChildren = ((Folder) vfsObject).getChildren();
		for (CmisObject child : vfsChildren) {
            String childVfsPath = vfsPath + "/" + child.getName();
            String childRfsPath = rfsPath + File.separator + child.getName();

			// files
			if (child.getBaseTypeId().equals(BaseTypeId.CMIS_DOCUMENT)) {
				addPullFileToSyncJob(module, childVfsPath, childRfsPath, child, false);
			}
			// folders
			else if (child.getBaseTypeId().equals(BaseTypeId.CMIS_FOLDER)) {
				addPullFolderTreeToSyncJob(module, childVfsPath, childRfsPath, child, false);
			}
		}
	}

	public interface ProgressIndicatorManager {
		void init();

		boolean isCanceled();
	}

}
