package com.mediaworx.intellij.opencmsplugin.entities;

import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.mediaworx.intellij.opencmsplugin.cmis.VfsAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: widmann
 * Date: 24.01.13
 * Time: 14:27
 * To change this template use File | Settings | File Templates.
 */
public class SyncJob {

	private Project project;
	private VfsAdapter adapter;
	private List<SyncEntity> syncList;
	private List<ExportEntity> exportList;

	public SyncJob(Project project, VfsAdapter adapter) {
		this.project = project;
		this.adapter = adapter;
		this.syncList = new ArrayList<SyncEntity>();
		this.exportList = new ArrayList<ExportEntity>();
	}

	public void execute() {
		final StringBuilder outputBuffer = new StringBuilder(4000);
		Runnable deployRunner = new Runnable() {

			public void run() {

				ProgressIndicatorManager progressIndicatorManager = new ProgressIndicatorManager() {
					ProgressIndicator indicator;


					public void setText(final String text) {
						indicator.setText(text);
					}

					public void init() {
						indicator = ProgressManager.getInstance().getProgressIndicator();
						indicator.setIndeterminate(false);
						indicator.setText("Syncing files and folders");
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

				try {
					int numSyncEntities = numSyncEntities();
					for (SyncEntity entity : getSyncList()) {
						if (progressIndicatorManager != null) {
							if (progressIndicatorManager.isCanceled()) {
								return;
							}

							progressIndicatorManager.setProgress((double) c++ / numSyncEntities);
						}

						String syncResult;

						if (entity.getSyncMode() == SyncMode.PUSH) {
							syncResult = doPush(entity);
						}
						else {
							syncResult = doPull(entity);
						}

						outputBuffer.append(syncResult).append('\n');
					}
					outputBuffer.append("\n---- Sync finished ----");
				}
				catch (Exception e) {
					e.printStackTrace();
				}

			}
		};

		ProgressManager.getInstance().runProcessWithProgressSynchronously(deployRunner, "Syncing with OpenCms VFS ...", true, project);

		String msg = outputBuffer.toString();
		Messages.showMessageDialog(msg, "OpenCms VFS Sync", msg.contains("ERROR") ? Messages.getErrorIcon() : Messages.getInformationIcon());
	}

	public String doPush(SyncEntity entity) {
		StringBuilder confirmation = new StringBuilder();

		if (entity.isFolder()) {
			adapter.createFolder(entity.getPath());
		}
		else if (entity.isFile()) {
			adapter.pushFile(entity);
		}

		confirmation.append("PUSH: ").append(entity.getPath()).append(" pushed to VFS");
		if (entity.replaceExistingEntity()) {
			confirmation.append(" replacing an existing entity");
		}

		return confirmation.toString();
	}

	public String doPull(SyncEntity entity) {
		StringBuilder confirmation = new StringBuilder();

		// TODO: Sync with VFS, handle errors, beachten: das ChangeDate muss auf das ChangeDate der VFS-Datei gesetzt werden (hoffentlich geht das)
		confirmation.append("PULL: ").append(entity.getPath()).append(" pulled from VFS");
		if (entity.replaceExistingEntity()) {
			confirmation.append(" replacing an existing entity");
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

	public void setSyncList(List<SyncEntity> syncList) {
		this.syncList = syncList;
	}

	public List<ExportEntity> getExportList() {
		return exportList;
	}

	public void setExportList(List<ExportEntity> exportList) {
		this.exportList = exportList;
	}

	public void addSyncEntity(SyncEntity entity) {
		if (!syncList.contains(entity)) {
			syncList.add(entity);
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

	public int numExportEntities() {
		return exportList.size();
	}

	public boolean hasExportEntities() {
		return numExportEntities() > 0;
	}

}
