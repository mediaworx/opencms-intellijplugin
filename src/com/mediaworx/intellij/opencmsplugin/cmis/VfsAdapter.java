package com.mediaworx.intellij.opencmsplugin.cmis;

import com.mediaworx.intellij.opencmsplugin.entities.SyncEntity;
import com.mediaworx.intellij.opencmsplugin.exceptions.CmsPermissionDeniedException;
import com.mediaworx.intellij.opencmsplugin.exceptions.CmsPushException;
import org.apache.chemistry.opencmis.client.api.*;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.RepositoryCapabilities;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.UnfileObject;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisNameConstraintViolationException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisPermissionDeniedException;

import javax.activation.MimetypesFileTypeMap;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VfsAdapter {

    private Session session;
    private boolean connected;

	private String atompubUrl;
	private String user;
	private String password;

    // TODO: Handle ConnectionException
    public VfsAdapter(String atompubUrl, String user, String password) {

	    if (atompubUrl == null || atompubUrl.length() == 0) {
		    throw new RuntimeException("parameter atompubUrl must not be null or empty");
	    }
	    if (user == null || user.length() == 0) {
		    throw new RuntimeException("parameter user must not be null or empty");
	    }
	    if (password == null || password.length() == 0) {
		    throw new RuntimeException("parameter password must not be null or empty");
	    }

        this.atompubUrl = atompubUrl;
	    this.user = user;
	    this.password = password;

	    startSession();
    }

    public void startSession() {

	    if (password != null && password.length() > 0) {

		    Map<String, String> sessionParams = new HashMap<String, String>();

	        // Create a SessionFactory and set up the SessionParameter map
		    SessionFactory sessionFactory = SessionFactoryImpl.newInstance();

	        // user credentials
	        sessionParams.put(SessionParameter.USER, user);
	        sessionParams.put(SessionParameter.PASSWORD, password);

	        // repository
	        sessionParams.put(SessionParameter.ATOMPUB_URL, atompubUrl);
	        sessionParams.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());

		    try {
			    // find all the repositories at this URL - there should only be one.
			    List<Repository> repositories = sessionFactory.getRepositories(sessionParams);
			    for (Repository r : repositories) {
				    System.out.println("Found repository: " + r.getName());
			    }

			    // create session with the first (and only) repository
			    Repository repository = repositories.get(0);
			    sessionParams.put(SessionParameter.REPOSITORY_ID, repository.getId());
			    sessionParams.put(SessionParameter.CONNECT_TIMEOUT, "500");

			    System.out.println("Starting CMIS session using repository " + atompubUrl);
			    this.session = sessionFactory.createSession(sessionParams);

			    if (this.session != null) {
				    connected = true;
			    }
			    else {
				    connected = false;
				    System.out.println("Error: CMIS session is null");
			    }
		    }
		    catch (Exception e) {
			    System.out.println("Exception connecting to VFS: " + e.getMessage());
			    connected = false;
		    }
		}
    }

    public boolean exists(String path) {
	    if (!connected) {
		    System.out.println("not connected");
		    return false;
	    }
        try {
            session.getObjectByPath(path);
            return true;
        }
        catch (CmisObjectNotFoundException e) {
            return false;
        }
    }

    public CmisObject getVfsObject(String path) throws CmsPermissionDeniedException {
	    if (!connected) {
		    System.out.println("not connected");
		    return null;
	    }
	    try {
            return session.getObjectByPath(path);
        }
        catch (CmisObjectNotFoundException e) {
            return null;
        }
	    catch (CmisPermissionDeniedException e) {
		    e.printStackTrace(System.out);
		    throw new CmsPermissionDeniedException("Permission denied, can't access "+path, e);
	    }
    }

    private Folder getOrCreateFolder(String path) {
	    if (!connected) {
		    System.out.println("not connected");
		    return null;
	    }

	    // check if the folder exists
        try {
            return (Folder)session.getObjectByPath(path);
        }
        // if the folder does not exist, create it
        catch (CmisObjectNotFoundException e) {
            String parentPath = path.substring(0, path.lastIndexOf("/"));
            String foldername = path.substring(path.lastIndexOf("/") + 1, path.length());
            System.out.println("creating folder "+path);
            System.out.println("parent path "+parentPath);
            System.out.println("foldername "+foldername);

            Folder parent;

            try {
                parent = (Folder)session.getObjectByPath(parentPath);
            }
            catch (CmisObjectNotFoundException e2) {
                parent = getOrCreateFolder(parentPath);
            }

            Map<String, String> newFolderProps = new HashMap<String, String>();
            newFolderProps.put(PropertyIds.OBJECT_TYPE_ID, BaseTypeId.CMIS_FOLDER.value());
            newFolderProps.put(PropertyIds.NAME, foldername);
            return parent.createFolder(newFolderProps);
        }
    }

	public Folder createFolder(String path) {
		return getOrCreateFolder(path);
	}

	public Document pushFile(SyncEntity entity) throws CmsPushException {
		if (!connected) {
			System.out.println("not connected");
			return null;
		}

		File rfsFile = entity.getRealFile();
		FileInputStream rfsFileInputStream = null;
		Document vfsFile = null;
		long vfsFileModifiedTime = 0;

		try {
			rfsFileInputStream = new FileInputStream(rfsFile);
			String mimetype = new MimetypesFileTypeMap().getContentType(rfsFile);

			ContentStream contentStream = session.getObjectFactory().createContentStream(rfsFile.getName(),
					rfsFile.length(), mimetype, rfsFileInputStream);

			if (entity.replaceExistingEntity()) {
				vfsFile = (Document)entity.getVfsObject();
				vfsFile.setContentStream(contentStream, true, true);
			}
			else {
				// get the parent folder object from vfs
				String parentPath = entity.getVfsPath().substring(0, entity.getVfsPath().lastIndexOf("/"));
				Folder parent = getOrCreateFolder(parentPath);

				// Create the file as Document Object
				Map<String, Object> properties = new HashMap<String, Object>();
				properties.put(PropertyIds.OBJECT_TYPE_ID, BaseTypeId.CMIS_DOCUMENT.value());
				properties.put(PropertyIds.NAME, rfsFile.getName());
				vfsFile = parent.createDocument(properties, contentStream, VersioningState.NONE);
			}

			// Set file modification date
            // This does not work when using OpenCms since OpenCms always sets the date of the CMIS change event
			/*
			Map<String, Object> dateProperties = new HashMap<String, Object>(1);
			GregorianCalendar modifiedGC = new GregorianCalendar();
			modifiedGC.setTime(new Date(rfsFile.lastModified()));
			dateProperties.put(PropertyIds.LAST_MODIFICATION_DATE, modifiedGC);
			vfsFile.updateProperties(dateProperties, false);
			*/

			vfsFileModifiedTime = vfsFile.getLastModificationDate().getTimeInMillis();
		}
		catch (FileNotFoundException e) {
			System.out.println("File not found.");
		}
		catch(CmisNameConstraintViolationException e) {
			throw new CmsPushException("Could not push entity "+entity.getVfsPath()+".\n"+e.getMessage(), e);
		}
		finally {
			try {
				if (rfsFileInputStream != null) {
					rfsFileInputStream.close();
				}

				if (vfsFileModifiedTime > 0) {
					// Since setting the modification Date on the vfs file ain't possible, set the date for the rfs file
					if (rfsFile.setLastModified(vfsFileModifiedTime)) {
						System.out.println("Setting lastModificationDate successful");
					}
					else {
						System.out.println("Setting lastModificationDate NOT successful");
					}
				}
			}
			catch (IOException e) {
				// do nothing
			}
		}

		return vfsFile;
	}

    public void pullFile(SyncEntity syncEntity) {
	    if (!connected) {
		    System.out.println("not connected");
		    return;
	    }
	    Document document = (Document)syncEntity.getVfsObject();

        System.out.println("Pulling "+syncEntity.getVfsPath()+" to "+syncEntity.getRfsPath());

        InputStream is = document.getContentStream().getStream();
        File rfsFile = syncEntity.createRealFile();
	    OutputStream os = null;
	    try {
	        os = new FileOutputStream(rfsFile);
	        byte[] buffer = new byte[4096];
            for (int n; (n = is.read(buffer)) != -1; ) {
                os.write(buffer, 0, n);
            }
        }
        catch (IOException e) {
            System.out.println("There was an Exception writing to the local file "+syncEntity.getRfsPath()+": "+e+"\n"+e.getMessage());
            rfsFile.delete();
        }
        finally {
            try {
	            is.close();
	            os.close();
            }
            catch (IOException e) {
	            // Do nothing
            }
            rfsFile.setLastModified(document.getLastModificationDate().getTimeInMillis());
        }
    }


    public void deleteFile(String vfsPath) {
	    if (!connected) {
		    System.out.println("not connected");
		    return;
	    }
	    CmisObject vfsFile = null;
	    try {
		    vfsFile = getVfsObject(vfsPath);
	    }
	    catch (CmsPermissionDeniedException e) {
		    e.printStackTrace(System.out);
	    }
	    if (vfsFile != null) {
            if (vfsFile instanceof Folder) {
                System.out.println("Deleting the following folder from the VFS: "+vfsPath);
                ((Folder)vfsFile).deleteTree(true, UnfileObject.DELETE, true);
            }
            else {
                System.out.println("Deleting the following file from the VFS: "+vfsPath);
                vfsFile.delete();
            }
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public void clearCache() {
        session.clear();
    }


	public void logCapabilities() {
		System.out.println("Printing repository capabilities...");
		final RepositoryInfo repInfo = session.getRepositoryInfo();
		RepositoryCapabilities cap = repInfo.getCapabilities();
		System.out.println("\nNavigation Capabilities");
		System.out.println("-----------------------");
		System.out.println("Get descendants supported: " + (cap.isGetDescendantsSupported() ? "true" : "false"));
		System.out.println("Get folder tree supported: " + (cap.isGetFolderTreeSupported() ? "true" : "false"));
		System.out.println("\nObject Capabilities");
		System.out.println("-----------------------");
		System.out.println("Content Stream: " + cap.getContentStreamUpdatesCapability().value());
		System.out.println("Changes: " + cap.getChangesCapability().value());
		System.out.println("Renditions: " + cap.getRenditionsCapability().value());
		System.out.println("\nFiling Capabilities");
		System.out.println("-----------------------");
		System.out.println("Multifiling supported: " + (cap.isMultifilingSupported() ? "true" : "false"));
		System.out.println("Unfiling supported: " + (cap.isUnfilingSupported() ? "true" : "false"));
		System.out.println("Version specific filing supported: " + (cap.isVersionSpecificFilingSupported() ? "true" : "false"));
		System.out.println("\nVersioning Capabilities");
		System.out.println("-----------------------");
		System.out.println("PWC searchable: " + (cap.isPwcSearchableSupported() ? "true" : "false"));
		System.out.println("PWC updatable: " + (cap.isPwcUpdatableSupported() ? "true" : "false"));
		System.out.println("All versions searchable: " + (cap.isAllVersionsSearchableSupported() ? "true" : "false"));
		System.out.println("\nQuery Capabilities");
		System.out.println("-----------------------");
		System.out.println("Query: " + cap.getQueryCapability().value());
		System.out.println("Join: " + cap.getJoinCapability().value());
		System.out.println("\nACL Capabilities");
		System.out.println("-----------------------");
		System.out.println("ACL: " + cap.getAclCapability().value());
		System.out.println("End of  repository capabilities");
	}

	public void logCmisObjectProperties(CmisObject object) {
		List<Property<?>> properties = object.getProperties();
		for (Property<?> property : properties) {
			System.out.println("Property: "+property.getDisplayName()+" ("+property.getLocalName()+") - "+property.getValue().toString());
		}
	}
}
