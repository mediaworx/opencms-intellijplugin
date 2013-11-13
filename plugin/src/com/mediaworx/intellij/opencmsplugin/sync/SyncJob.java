package com.mediaworx.intellij.opencmsplugin.sync;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.ui.Messages;
import com.mediaworx.intellij.opencmsplugin.components.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.configuration.ModuleExportPoint;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;
import com.mediaworx.intellij.opencmsplugin.entities.ExportEntity;
import com.mediaworx.intellij.opencmsplugin.entities.SyncEntity;
import com.mediaworx.intellij.opencmsplugin.exceptions.CmsPushException;
import com.mediaworx.intellij.opencmsplugin.opencms.OpenCmsModule;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SyncJob {

	private OpenCmsPlugin plugin;
    private OpenCmsPluginConfigurationData config;
	private VfsAdapter adapter;
	private List<SyncEntity> syncList;
	private List<SyncEntity> refreshEntityList;
	private List<ExportEntity> exportList;

	public SyncJob(OpenCmsPlugin plugin) {
		this.plugin = plugin;
        config = plugin.getPluginConfiguration();
		adapter = plugin.getVfsAdapter();
		this.syncList = new ArrayList<SyncEntity>();
		this.refreshEntityList = new ArrayList<SyncEntity>();
		this.exportList = new ArrayList<ExportEntity>();
	}

	public void execute() {
		final StringBuilder outputBuffer = new StringBuilder(4000);
		Runnable deployRunner = new Runnable() {

			public void run() {

				ProgressIndicatorManager progressIndicatorManager = new ProgressIndicatorManager() {
					ProgressIndicator indicator;

					public void init() {
						indicator = ProgressManager.getInstance().getProgressIndicator();
						indicator.setIndeterminate(false);
						indicator.setText("Syncing files and folders");
					}

					public void setText(final String text) {
						indicator.setText(text);
					}

					public void setProgress(double fraction) {
						indicator.setFraction(fraction);
					}

					public boolean isCanceled() {
						return indicator.isCanceled();
					}
				};

				progressIndicatorManager.init();
				int c = 0;

				int numSyncEntities = numSyncEntities() + numExportEntities();
				for (SyncEntity entity : getSyncList()) {
					if (progressIndicatorManager != null) {
						if (progressIndicatorManager.isCanceled()) {
							return;
						}

						progressIndicatorManager.setProgress((double) c++ / numSyncEntities);
					}

					String syncResult = doSync(entity);
					outputBuffer.append(syncResult).append('\n');
				}
				outputBuffer.append("---- Sync finished ----");

                if (numExportEntities() > 0) {
                    outputBuffer.append("\n\n");
                    for (ExportEntity entity : getExportList()) {
                        if (progressIndicatorManager != null) {
                            if (progressIndicatorManager.isCanceled()) {
                                return;
                            }

                            progressIndicatorManager.setProgress((double) c++ / numSyncEntities);
                        }

                        String syncResult = doExportPointHandling(entity);

                        outputBuffer.append(syncResult).append('\n');
                    }
                    outputBuffer.append("---- Copying of ExportPoints finished ----");
                }
			}
		};

		ProgressManager.getInstance().runProcessWithProgressSynchronously(deployRunner, "Syncing with OpenCms VFS ...", true, plugin.getProject());

		String msg = outputBuffer.toString();
		Messages.showMessageDialog(msg, "OpenCms VFS Sync", msg.contains("ERROR") ? Messages.getErrorIcon() : Messages.getInformationIcon());
	}


	public String doSync(SyncEntity entity) {
		String syncResult = "";
		if (entity.getSyncAction() == SyncAction.PUSH) {
			syncResult = doPush(entity);
		}
		else if (entity.getSyncAction() == SyncAction.PULL) {
			syncResult = doPull(entity);
		}
		else if (entity.getSyncAction() == SyncAction.DELETE_RFS) {
			syncResult = doDeleteFromRfs(entity);
		}
		else if (entity.getSyncAction() == SyncAction.DELETE_VFS) {
			syncResult = doDeleteFromVfs(entity);
		}
		return syncResult;
	}


	public String doPush(SyncEntity entity) {

		boolean success = false;
		String errormessage = null;

		if (entity.isFolder()) {
			try {
				adapter.createFolder(entity.getVfsPath());
				success = true;
			}
			catch (Exception e) {
				errormessage = "Error pushing Folder "+entity.getVfsPath()+"\n"+e.getMessage();
			}
		}
		else if (entity.isFile()) {
			try {
				adapter.pushFile(entity);
				success = true;
			}
			catch (CmsPushException e) {
				errormessage = e.getMessage();
			}
		}

		StringBuilder confirmation = new StringBuilder();
		if (success) {
			confirmation.append("PUSH: ").append(entity.getVfsPath()).append(" pushed to VFS");
			if (entity.replaceExistingEntity()) {
				confirmation.append(" replacing an existing entity");
			}
		}
		else {
			confirmation.append("PUSH FAILED! ");
			confirmation.append(errormessage);
		}

		return confirmation.toString();
	}

    // TODO: Improve error handling
	public String doPull(SyncEntity entity) {
		StringBuilder confirmation = new StringBuilder();

        if (entity.isFolder()) {
            try {
	            FileUtils.forceMkdir(new File(entity.getRfsPath()));
            } catch (IOException e) {
                System.out.println("There was an Exception creating a local directory: " + e + "\n" + e.getMessage());
           }
        }
        else {
            adapter.pullFile(entity);
        }

		confirmation.append("PULL: ").append(entity.getRfsPath()).append(" pulled from VFS");
		if (entity.replaceExistingEntity()) {
			confirmation.append(" replacing an existing entity");
		}

		return confirmation.toString();
	}

	public String doDeleteFromRfs(SyncEntity entity) {
		StringBuilder confirmation = new StringBuilder("DELETE: ").append(entity.getRfsPath()).append(" (not in the VFS) - ");
		File rfsFile = entity.getRealFile();
		if (FileUtils.deleteQuietly(rfsFile)) {
			confirmation.append(" SUCCESS");
		}
		else {
			confirmation.append(" FAILED!");
		}
		return confirmation.toString();
	}

	public String doDeleteFromVfs(SyncEntity entity) {
		StringBuilder confirmation = new StringBuilder("DELETE: ").append(entity.getVfsPath()).append(" (not in the RFS) - ");
		if (adapter.deleteResource(entity.getVfsPath())) {
			confirmation.append(" SUCCESS");
		}
		else {
			confirmation.append(" FAILED!");
		}
		return confirmation.toString();
	}

	public String doExportPointHandling(ExportEntity entity) {
        StringBuilder confirmation = new StringBuilder();

		System.out.println("ExportEntity: " + entity);

		if (!entity.isToBeDeleted()) {
			confirmation.append("Copy of ").append(entity.getVfsPath()).append(" to ").append(entity.getDestination()).append(" - ");
	        File file = new File(entity.getSourcePath());
	        if (file.exists()) {
	            if (file.isFile()) {
	                try {
	                    FileUtils.copyFile(file, new File(entity.getTargetPath()));
	                    confirmation.append("SUCCESS");
	                } catch (IOException e) {
	                    confirmation.append("FAILED (").append(e.getMessage()).append(")");
	                }
	            }
	            else if (file.isDirectory()) {
	                try {
	                    FileUtils.copyDirectory(file, new File(entity.getTargetPath()));
	                    confirmation.append("SUCCESS");
	                } catch (IOException e) {
	                    confirmation.append("FAILED (").append(e.getMessage()).append(")");
	                }
	            }
	        }
	        else {
	            confirmation.append(" - FILE NOT FOUND");
	        }
		}
		else {
			confirmation.append("Resource ").append(entity.getVfsPath()).append(" removed, deletion of exported file ")
					.append(entity.getDestination()).append(" - ");
			File file = new File(entity.getTargetPath());
			if (file.exists()) {
				if (FileUtils.deleteQuietly(file)) {
					confirmation.append("SUCCESS");
				}
				else {
					confirmation.append("FAILED");
				}
			}
			else {
				confirmation.append("NOT NECESSARY (doesn't exist)");
			}
		}
        return confirmation.toString();
    }

	public interface ProgressIndicatorManager {

		void init();

		void setProgress(double fraction);

		boolean isCanceled();

		void setText(String text);
	}

	public List<SyncEntity> getSyncList() {
		return syncList;
	}

	public List<SyncEntity> getRefreshEntityList() {
		return refreshEntityList;
	}

	public List<ExportEntity> getExportList() {
		return exportList;
	}

	public void addSyncEntity(OpenCmsModule ocmsModule, SyncEntity entity) {
		if (!syncList.contains(entity)) {
			if (entity.getSyncAction() == SyncAction.PULL || entity.getSyncAction() == SyncAction.DELETE_RFS) {
				this.refreshEntityList.add(entity);
			}
			if (entity.getSyncAction() != SyncAction.DELETE_VFS) {
	            addSyncEntityToExportListIfNecessary(ocmsModule, entity);
			}
            syncList.add(entity);
		}
	}

    public void addSyncEntityToExportListIfNecessary(OpenCmsModule ocmsModule, SyncEntity syncEntity) {

	    List<ModuleExportPoint> exportPoints = ocmsModule.getExportPoints();

        if (exportPoints != null) {

	        String localModuleVfsRoot = ocmsModule.getLocalVfsRoot();
	        String entityVfsPath = syncEntity.getVfsPath();

		    for (ModuleExportPoint exportPoint : exportPoints) {
			    String vfsSource = exportPoint.getVfsSource();
	            if (entityVfsPath.startsWith(vfsSource)) {
	                String destination = exportPoint.getRfsTarget();
	                String relativePath = entityVfsPath.substring(vfsSource.length());
	                ExportEntity exportEntity = new ExportEntity();
	                exportEntity.setSourcePath(localModuleVfsRoot+entityVfsPath);
	                exportEntity.setTargetPath(config.getWebappRoot() + "/" + destination + relativePath);
	                exportEntity.setVfsPath(entityVfsPath);
	                exportEntity.setDestination(destination);
		            exportEntity.setToBeDeleted(syncEntity.getSyncAction() == SyncAction.DELETE_RFS);
	                addExportEntity(exportEntity);
	            }
	        }
        }
    }

	public void addExportEntity(ExportEntity entity) {
		exportList.add(entity);
	}

	public int numSyncEntities() {
		return syncList.size();
	}

	public boolean hasSyncEntities() {
		return numSyncEntities() > 0;
	}

	public int getNumRefreshEntities() {
		return refreshEntityList.size();
	}

	public boolean hasRefreshEntities() {
		return getNumRefreshEntities() > 0;
	}

	public int numExportEntities() {
		return exportList.size();
	}

	public boolean hasExportEntities() {
		return numExportEntities() > 0;
	}

}
