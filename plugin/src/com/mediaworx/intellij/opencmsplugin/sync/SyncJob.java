package com.mediaworx.intellij.opencmsplugin.sync;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.ui.Messages;
import com.mediaworx.intellij.opencmsplugin.components.OpenCmsPlugin;
import com.mediaworx.intellij.opencmsplugin.configuration.ModuleExportPoint;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;
import com.mediaworx.intellij.opencmsplugin.entities.ExportEntity;
import com.mediaworx.intellij.opencmsplugin.entities.SyncEntity;
import com.mediaworx.intellij.opencmsplugin.entities.SyncFolder;
import com.mediaworx.intellij.opencmsplugin.exceptions.CmsPushException;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
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

				ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
				indicator.setIndeterminate(false);

				int c = 0;
				int step = 1;
				int numSteps = 1;
				if (plugin.getPluginConfiguration().isPluginConnectorEnabled()) {
					numSteps += 1;
				}
				if (numExportEntities() > 0) {
					numSteps += 1;
				}
				indicator.setText("Step " + (step++) + "/" + numSteps + ": Syncing files and folders");

				int numSyncEntities = syncList.size();
				for (SyncEntity entity : syncList) {
					if (indicator.isCanceled()) {
						return;
					}

					String syncResult = doSync(entity);
					indicator.setFraction((double)c++ / numSyncEntities);
					outputBuffer.append(syncResult).append('\n');
				}
				outputBuffer.append("---- Sync finished ----");

				if (plugin.getPluginConfiguration().isPluginConnectorEnabled()) {

					metaInfoHandling: {

						indicator.setText("Step " + (step++) + "/" + numSteps + ": Pulling resource meta data from OpenCms");
						indicator.setIndeterminate(true);

						// build lists with all resources for which meta information is to be pulled / deleted
						ArrayList<String> pullEntityList = new ArrayList<String>();
						for (SyncEntity entity : syncList) {
							if (!entity.getSyncAction().isDeleteAction()) {
								pullEntityList.add(entity.getVfsPath());
							}
						}

						HashMap<String, String> metaInfos;

						try {
							metaInfos = plugin.getPluginConnector().getResourceInfos(pullEntityList);
						}
						catch (IOException e) {
							System.out.println("IOException while trying to retrieve meta infos");
							e.printStackTrace(System.out);
							outputBuffer.append("There was an error retrieving resource meta infos from OpenCms");
							break metaInfoHandling;
						}

						indicator.setIndeterminate(false);
						indicator.setFraction(0);
					    c = 0;
						int numMetaEntities = syncList.size();

						if (numMetaEntities > 0) {
							outputBuffer.append("\n\n");
							for (SyncEntity entity : syncList) {
								String metaResult = doMetaInfoHandling(metaInfos, entity);
								indicator.setFraction((double)c++ / numMetaEntities);
								outputBuffer.append(metaResult).append('\n');
							}
						}
					}
					outputBuffer.append("---- Resource meta info pull finished ----");
				}

                if (numExportEntities() > 0) {
	                indicator.setText("Step " + step + "/" + numSteps + ": Handling export points");
	                indicator.setFraction(0);
	            	c = 0;
					int numExportEntities = numExportEntities();

                    outputBuffer.append("\n\n");
                    for (ExportEntity entity : exportList) {
                        if (indicator.isCanceled()) {
                            return;
                        }
                        String exportPointResult = doExportPointHandling(entity);
	                    indicator.setFraction((double)c++ / numExportEntities);
                        outputBuffer.append(exportPointResult).append('\n');
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

		confirmation.append("PULL: ").append(entity.getVfsPath()).append(" pulled from VFS to ").append(entity.getOcmsModule().getLocalVfsRoot());
		if (entity.replaceExistingEntity()) {
			confirmation.append(" replacing an existing entity");
		}

		return confirmation.toString();
	}

	public String doDeleteFromRfs(SyncEntity entity) {
		StringBuilder confirmation = new StringBuilder("DELETE: ").append(entity.getVfsPath()).append(" from ").append(entity.getOcmsModule().getLocalVfsRoot()).append(" (not in the VFS) - ");
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

	private String doMetaInfoHandling(HashMap<String,String> metaInfos, SyncEntity entity) {
		String metaInfoPath = entity.getMetaInfoPath();
		File metaInfoFile = new File(metaInfoPath);

		if (entity.getSyncAction().isDeleteAction()) {
			FileUtils.deleteQuietly(metaInfoFile);
			return "DELETE: " + metaInfoPath;
		}

		if (metaInfos.containsKey(entity.getVfsPath())) {
			if (entity instanceof SyncFolder) {
				String metaFolderPath = ((SyncFolder)entity).getMetaInfoFolderPath();
				File metaFolder = new File(metaFolderPath);
				if (!metaFolder.exists()) {
					try {
						FileUtils.forceMkdir(metaFolder);
					}
					catch (IOException e) {
						String message = "ERROR: cant create meta info directory " + metaFolderPath;
						System.out.println(message);
						e.printStackTrace(System.out);
						return message;
					}
				}
			}
			try {
				FileUtils.writeStringToFile(metaInfoFile, metaInfos.get(entity.getVfsPath()), Charset.forName("UTF-8"));
			}
			catch (IOException e) {
				String message = "ERROR: cant create meta info file " + metaInfoPath;
				System.out.println(message);
				e.printStackTrace(System.out);
				return message;
			}

		}
		else {
			String message = entity.getVfsPath() + " not found in meta info map.";
			System.out.println(message);
			return message;
		}
		return "PULL: Meta info file pulled: " + metaInfoPath;
	}

	public String doExportPointHandling(ExportEntity entity) {
        StringBuilder confirmation = new StringBuilder();

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

	public List<SyncEntity> getSyncList() {
		return syncList;
	}

	public List<SyncEntity> getRefreshEntityList() {
		return refreshEntityList;
	}

	public void addSyncEntity(SyncEntity entity) {
		if (!syncList.contains(entity)) {
			if (entity.getSyncAction() == SyncAction.PULL || entity.getSyncAction() == SyncAction.DELETE_RFS) {
				this.refreshEntityList.add(entity);
			}
			if (entity.getSyncAction() != SyncAction.DELETE_VFS) {
	            addSyncEntityToExportListIfNecessary(entity);
			}
            syncList.add(entity);
		}
	}

    public void addSyncEntityToExportListIfNecessary(SyncEntity syncEntity) {

	    List<ModuleExportPoint> exportPoints = syncEntity.getOcmsModule().getExportPoints();

        if (exportPoints != null) {

	        String localModuleVfsRoot = syncEntity.getOcmsModule().getLocalVfsRoot();
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

}
