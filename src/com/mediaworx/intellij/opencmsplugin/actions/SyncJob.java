package com.mediaworx.intellij.opencmsplugin.actions;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.mediaworx.intellij.opencmsplugin.cmis.VfsAdapter;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;
import com.mediaworx.intellij.opencmsplugin.entities.ExportEntity;
import com.mediaworx.intellij.opencmsplugin.entities.SyncEntity;
import com.mediaworx.intellij.opencmsplugin.entities.SyncMode;
import com.mediaworx.intellij.opencmsplugin.exceptions.CmsPushException;
import org.apache.commons.io.FileUtils;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: widmann
 * Date: 24.01.13
 * Time: 14:27
 * To change this template use File | Settings | File Templates.
 */
public class SyncJob {

    private static final String MODULECONFIGPATH = File.separator+"WEB-INF"+File.separator+"config"+File.separator+"opencms-modules.xml";

    private static final HashMap<String, HashMap<String, String>> EXPORTPOINTS = new HashMap<String, HashMap<String, String>>();

	private Project project;
    private OpenCmsPluginConfigurationData config;
	private VfsAdapter adapter;
	private List<SyncEntity> syncList;
	private List<SyncEntity> pullEntityList;
	private List<ExportEntity> exportList;

	public SyncJob(Project project, OpenCmsPluginConfigurationData config, VfsAdapter adapter) {
		this.project = project;
        this.config = config;
		this.adapter = adapter;
		this.syncList = new ArrayList<SyncEntity>();
		this.pullEntityList = new ArrayList<SyncEntity>();
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

                        String syncResult = doExportPointCopy(entity);

                        outputBuffer.append(syncResult).append('\n');
                    }
                    outputBuffer.append("---- Copying of ExportPoints finished ----");
                }
			}
		};

		ProgressManager.getInstance().runProcessWithProgressSynchronously(deployRunner, "Syncing with OpenCms VFS ...", true, project);

		String msg = outputBuffer.toString();
		Messages.showMessageDialog(msg, "OpenCms VFS Sync", msg.contains("ERROR") ? Messages.getErrorIcon() : Messages.getInformationIcon());
	}


    public void initModuleExportPoints(String module) {

        if (module != null && !EXPORTPOINTS.containsKey(module)) {
            System.out.println("Initializing export points for module "+module);

            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setValidating(false);
                factory.setFeature("http://xml.org/sax/features/namespaces", false);
                factory.setFeature("http://xml.org/sax/features/validation", false);
                factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
                factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
                DocumentBuilder builder = factory.newDocumentBuilder();

                String moduleConfig = config.getWebappRoot()+MODULECONFIGPATH;

                org.w3c.dom.Document doc = builder.parse(moduleConfig);
                XPathFactory xPathfactory = XPathFactory.newInstance();
                XPath xpath = xPathfactory.newXPath();
                XPathExpression expr = xpath.compile(String.format("/opencms/modules/module/name[normalize-space(text())=\"%s\"]/../exportpoints/exportpoint", module));

                NodeList nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
                int numExportPoints = nl.getLength();

                EXPORTPOINTS.put(module, new HashMap<String, String>(numExportPoints));

                for (int i = 0; i < numExportPoints; i++) {
                    Node n = nl.item(i);
                    NamedNodeMap attr = n.getAttributes();
                    String uri = attr.getNamedItem("uri").getNodeValue();
                    String destination = attr.getNamedItem("destination").getNodeValue();
                    System.out.println("Exportpoint "+(i+1)+": uri="+uri+" - destination="+destination);
                    EXPORTPOINTS.get(module).put(uri, destination);
                }
            }
            catch (Exception e) {
                System.out.println("There was an Exception initializing export points: "+e+"\n"+e.getMessage());
            }
        }
    }

	public String doSync(SyncEntity entity) {
		String syncResult;
		if (entity.getSyncMode() == SyncMode.PUSH) {
			syncResult = doPush(entity);
		}
		else {
			syncResult = doPull(entity);
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

    public String doExportPointCopy(ExportEntity entity) {
        StringBuilder confirmation = new StringBuilder("Copy of ").append(entity.getVfsPath()).append(" to ").append(entity.getDestination()).append(" - ");
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

	public List<SyncEntity> getPullEntityList() {
		return pullEntityList;
	}

	public List<ExportEntity> getExportList() {
		return exportList;
	}

	public void setExportList(List<ExportEntity> exportList) {
		this.exportList = exportList;
	}

	public void addSyncEntity(String module, SyncEntity entity) {
		if (!syncList.contains(entity)) {
			if (entity.getSyncMode() == SyncMode.PULL) {
				this.pullEntityList.add(entity);
			}
            addSyncEntityToExportListIfNecessary(module, entity);
            syncList.add(entity);
		}
	}

    public void addSyncEntityToExportListIfNecessary(String moduleName, SyncEntity syncEntity) {
        for (String uri : EXPORTPOINTS.get(moduleName).keySet()) {
            String entityPath = syncEntity.getVfsPath();
            if (entityPath.startsWith(uri)) {
                String destination = EXPORTPOINTS.get(moduleName).get(uri);
                String relativePath = entityPath.substring(uri.length());
                ExportEntity exportEntity = new ExportEntity();
                exportEntity.setSourcePath(config.getLocalModuleVfsRoot(moduleName)+entityPath);
                exportEntity.setTargetPath(config.getWebappRoot() + File.separator + destination + relativePath);
                exportEntity.setVfsPath(entityPath);
                exportEntity.setDestination(destination);
                addExportEntity(exportEntity);
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

	public int getNumPullEntities() {
		return pullEntityList.size();
	}

	public boolean hasPullEntities() {
		return getNumPullEntities() > 0;
	}

	public int numExportEntities() {
		return exportList.size();
	}

	public boolean hasExportEntities() {
		return numExportEntities() > 0;
	}

}
