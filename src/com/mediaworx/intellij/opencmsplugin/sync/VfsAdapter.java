/*
 * This file is part of the OpenCms plugin for IntelliJ by mediaworx.
 *
 * For further information about the OpenCms plugin for IntelliJ, please
 * see the project website at GitHub:
 * https://github.com/mediaworx/opencms-intellijplugin
 *
 * Copyright (C) 2007-2014 mediaworx berlin AG (http://www.mediaworx.com)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 3 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.mediaworx.intellij.opencmsplugin.sync;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.Messages;
import com.mediaworx.intellij.opencmsplugin.entities.SyncEntity;
import com.mediaworx.intellij.opencmsplugin.exceptions.CmsConnectionException;
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
import org.apache.chemistry.opencmis.commons.exceptions.*;
import org.apache.commons.io.FileUtils;

import javax.activation.MimetypesFileTypeMap;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter used to sync the RFS with the OpenCms VFS. Doesn't handle properties, siblings or content types.
 * @author Kai Widmann, widmann@mediaworx.com
 */
public class VfsAdapter {

	private static final Logger LOG = Logger.getInstance(VfsAdapter.class);

	/** the CMIS session */
	private Session session;

	/** boolean flag denoting if the adapter is connected */
	private boolean connected;

	/** repository URL, for OpenCms CMIS usually "http://localhost:8080/opencms/cmisatom/cmis-offline/" */
	private String atompubUrl;

	/** OpenCms user with sufficient privileges to read/write from/to the VFS, e.g. "Admin" */
	private String user;

	/** the OpenCms user's password */
	private String password;

	/**
	 * creates a new VfsAdapter that may be connected by calling {@link #startSession()}
	 * @param atompubUrl repository URL, for OpenCms CMIS usually "http://localhost:8080/opencms/cmisatom/cmis-offline/"
	 * @param user       OpenCms user with sufficient privileges to read/write from/to the VFS, e.g. "Admin"
	 * @param password   the OpenCms user's password
	 */
	public VfsAdapter(String atompubUrl, String user, String password) {

	    if (atompubUrl == null || atompubUrl.length() == 0) {
		    throw new IllegalArgumentException("parameter atompubUrl must not be null or empty");
	    }
	    if (user == null || user.length() == 0) {
		    throw new IllegalArgumentException("parameter user must not be null or empty");
	    }
	    if (password == null || password.length() == 0) {
		    throw new IllegalArgumentException("parameter password must not be null or empty");
	    }

	    this.atompubUrl = atompubUrl;
	    this.user = user;
		this.password = password;
	}

	/**
	 * starts the CMIS session that is used to push or pull files/folders
	 */
	public void startSession() throws CmsConnectionException {

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
				    LOG.info("Found repository: " + r.getName());
			    }

			    // create session with the first (and only) repository
			    Repository repository = repositories.get(0);
			    sessionParams.put(SessionParameter.REPOSITORY_ID, repository.getId());
			    sessionParams.put(SessionParameter.CONNECT_TIMEOUT, "500");

			    LOG.info("Starting CMIS session using repository " + atompubUrl);
			    this.session = sessionFactory.createSession(sessionParams);

			    if (this.session != null) {
				    connected = true;
			    }
			    else {
				    connected = false;
				    LOG.info("Error: CMIS session is null");
			    }
		    }
		    catch (Exception e) {
			    LOG.info("Exception connecting to VFS", e);
			    connected = false;
			    throw new CmsConnectionException("Connection to OpenCms VFS failed. Is OpenCms running?");
		    }
		}
	}

	/**
	 * checks if a VFS resource exists at the given path
	 * @param path  the path to be checked (full root path, e.g.
	 *              <code>/system/modules/com.mycompany.mymodule/classes/messages.properties</code>)
	 * @return  <code>true</code> if the resource exists in the VFS, <code>false</code> otherwise
	 */
	public boolean exists(String path) {
	    if (!connected) {
		    LOG.warn("not connected");
		    return false;
	    }
		if (!path.startsWith("/")) {
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

	/**
	 * retrieves (pulls) the VFS resource at the given path
	 * @param path  path of the resource to be pulled
	 * @return  the VFS resource
	 * @throws CmsPermissionDeniedException
	 */
	public CmisObject getVfsObject(String path) throws CmsPermissionDeniedException {
	    if (!connected) {
		    LOG.warn("not connected");
		    return null;
	    }
	    try {
	        return session.getObjectByPath(path);
	    }
	    catch (CmisObjectNotFoundException e) {
	        return null;
	    }
	    catch (CmisPermissionDeniedException e) {
		    LOG.warn("Permission denied, can't access "+path, e);
		    throw new CmsPermissionDeniedException("Permission denied, can't access "+path, e);
	    }
		catch (CmisConnectionException e) {
			Messages.showDialog("Error connecting to the VFS" + e.getMessage() + "\nIs OpenCms running?",
					"Error", new String[]{"Ok"}, 0, Messages.getErrorIcon());
			LOG.warn("Error connecting to the VFS", e);
			connected = false;
			return null;
		}
	}

	/**
	 * retrieves a VFS folder, creating it if it doesn't exist
	 * @param path  the path of the folder to be retrieved
	 * @return  the VFS folder (may be newly created)
	 */
	private Folder getOrCreateFolder(String path) {
	    if (!connected) {
		    LOG.warn("not connected");
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
	        LOG.info("creating folder "+path);
	        LOG.info("parent path "+parentPath);
	        LOG.info("foldername "+foldername);

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

	/**
	 * creates a folder in the VFS and returns it. If the folder already exists, the existing folder is returned
	 * @param path  the folder's VFS path (full root path, e.g.
	 *              <code>/system/modules/com.mycompany.mymodule/classes</code>)
	 * @return  the newly created folder (or the folder that existed previously)
	 */
	public Folder createFolder(String path) {
		return getOrCreateFolder(path);
	}

	/**
	 * pushs a file from the RFS to the VFS
	 * @param entity    the sync entity representing the file to be pushed
	 * @return  a CMIS document of the newly created VFS file
	 * @throws CmsPushException
	 */
	public Document pushFile(SyncEntity entity) throws CmsPushException {
		if (!connected) {
			LOG.info("not connected");
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

			// if the file already exists in the VFS ...
			if (entity.replaceExistingEntity()) {
				// ... update its content
				vfsFile = (Document)entity.getVfsObject();
				vfsFile.setContentStream(contentStream, true, true);
			}
			// if the file doesn't exist in the VFS
			else {
				// ... get the parent folder object from the VFS
				String parentPath = entity.getVfsPath().substring(0, entity.getVfsPath().lastIndexOf("/"));
				Folder parent = getOrCreateFolder(parentPath);

				// ... and create the file as Document Object under the parent folder
				Map<String, Object> properties = new HashMap<String, Object>();
				properties.put(PropertyIds.OBJECT_TYPE_ID, BaseTypeId.CMIS_DOCUMENT.value());
				properties.put(PropertyIds.NAME, rfsFile.getName());
				vfsFile = parent.createDocument(properties, contentStream, VersioningState.NONE);
			}

			// Set file modification date in the VFS to the RFS file date
	        // This does not work when using OpenCms since OpenCms always sets the date of the CMIS change event
			// That's why the file date in the RFS has to be set to the VFS date (see finally block)
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
			LOG.info("File not found.");
		}
		catch(CmisNameConstraintViolationException e) {
			throw new CmsPushException("Could not push entity "+entity.getVfsPath()+", there was a problem with the resource name.\n"+e.getMessage(), e);
		}
		catch(CmisRuntimeException e) {
			throw new CmsPushException("Could not push entity "+entity.getVfsPath()+", there may be an issue with a lock.\n"+e.getMessage(), e);
		}
		finally {
			try {
				if (rfsFileInputStream != null) {
					rfsFileInputStream.close();
				}

				if (vfsFileModifiedTime > 0) {
					// Since setting the modification Date on the VFS file ain't possible, set the date for the RFS file
					if (rfsFile.setLastModified(vfsFileModifiedTime)) {
						LOG.info("Setting lastModificationDate successful");
					}
					else {
						LOG.info("Setting lastModificationDate NOT successful");
					}
				}
			}
			catch (IOException e) {
				// do nothing
			}
		}

		return vfsFile;
	}

	/**
	 * pulls a VFS file to the RFS
	 * @param syncEntity    the sync entity representing the file to be pulled
	 */
	public void pullFile(SyncEntity syncEntity) {
	    if (!connected) {
		    LOG.info("not connected");
		    return;
	    }
	    Document document = (Document)syncEntity.getVfsObject();

	    LOG.info("Pulling "+syncEntity.getVfsPath()+" to "+syncEntity.getOcmsModule().getLocalVfsRoot());

	    InputStream is = document.getContentStream().getStream();
	    File rfsFile = createRealFile(syncEntity);
	    OutputStream os = null;
	    try {
	        os = new FileOutputStream(rfsFile);
	        byte[] buffer = new byte[4096];
	        for (int n; (n = is.read(buffer)) != -1; ) {
	            os.write(buffer, 0, n);
	        }
	    }
	    catch (IOException e) {
	        LOG.info("There was an Exception writing to the local file " + syncEntity.getRfsPath() + ": " + e + "\n" + e.getMessage());
	    }
	    finally {
	        try {
	            is.close();
	        }
	        catch (IOException e) {
	            // Do nothing
	        }
		    if (os != null) {
			    try {
				    os.close();
			    }
			    catch (IOException e) {
				    // Do nothing
			    }
		    }
	        if (!rfsFile.setLastModified(document.getLastModificationDate().getTimeInMillis())) {
		        LOG.info("there was an error setting the modification date for " + syncEntity.getRfsPath());
	        }
	    }
	}


	public File createRealFile(SyncEntity syncEntity) {
		File realFile = new File(syncEntity.getRfsPath());
		if (!realFile.exists()) {
			try {
				if (syncEntity.isFolder()) {
					if (!realFile.mkdirs()) {
						LOG.warn("The directories for " + syncEntity.getRfsPath() + " could not be created");
					}
				}
				else {
					File parentFolder = realFile.getParentFile();
					if (!parentFolder.exists()) {
						FileUtils.forceMkdir(parentFolder);
					}
					if (!realFile.createNewFile()) {
						LOG.warn("The file " + syncEntity.getRfsPath() + " could not be created");
					}
				}
			}
			catch (IOException e) {
				LOG.warn("There was an Exception creating the local file " + syncEntity.getRfsPath(), e);
			}
		}
		syncEntity.setRealFile(realFile);
		return realFile;
	}



	/**
	 * deletes a file or folder from the VFS
	 * @param vfsPath   the path of the resource to be deleted (full root path, e.g.
	 *              <code>/system/modules/com.mycompany.mymodule/formatters/delete_me.jsp</code>)
	 */
	public boolean deleteResource(String vfsPath) {
	    if (!connected) {
		    LOG.warn("not connected");
		    return false;
	    }
		boolean success = false;
	    CmisObject vfsFile = null;
	    try {
		    vfsFile = getVfsObject(vfsPath);
	    }
	    catch (CmsPermissionDeniedException e) {
		    LOG.warn("Can't delete " + vfsPath + ", permission denied", e);
	    }
	    if (vfsFile != null) {
		    // Folders
	        if (vfsFile instanceof Folder) {
	            LOG.info("Deleting the following folder from the VFS: "+vfsPath);
	            List<String> failedResourcePaths = ((Folder)vfsFile).deleteTree(true, UnfileObject.DELETE, true);
		        success = failedResourcePaths == null || failedResourcePaths.size() <= 0;
	        }
	        // Files
	        else {
	            LOG.info("Deleting the following file from the VFS: "+vfsPath);
	            vfsFile.delete();
		        success = true;
	        }
	    }
		return success;
	}

	/**
	 * checks if the adapter is connected (a CMIS session is active)
	 * @return  <code>true</code> id the adapter is connected, <code>false</code> otherwise
	 */
	public boolean isConnected() {
		if (!connected) {
			return false;
		}
		if (session == null) {
			return false;
		}
		Folder folder = null;
		try {
			folder = session.getRootFolder();
		}
		catch (CmisConnectionException e) {
			LOG.info("Can't read CMIS repository root folder, not connected", e);
			connected = false;
		}
		catch (CmisPermissionDeniedException e) {
			LOG.info("CMIS says permission denied, not connected", e);
			connected = false;
		}
	    return folder != null && connected;
	}

	/**
	 * clears the CMIS session cache
	 */
	public void clearCache() {
	    session.clear();
	}


	/**
	 * logs the CMIS capabilities, for debugging purposes
	 */
	public void logCapabilities() {
		LOG.info("Printing repository capabilities...");
		final RepositoryInfo repInfo = session.getRepositoryInfo();
		RepositoryCapabilities cap = repInfo.getCapabilities();
		LOG.info("\nNavigation Capabilities");
		LOG.info("-----------------------");
		LOG.info("Get descendants supported: " + (cap.isGetDescendantsSupported() ? "true" : "false"));
		LOG.info("Get folder tree supported: " + (cap.isGetFolderTreeSupported() ? "true" : "false"));
		LOG.info("\nObject Capabilities");
		LOG.info("-----------------------");
		LOG.info("Content Stream: " + cap.getContentStreamUpdatesCapability().value());
		LOG.info("Changes: " + cap.getChangesCapability().value());
		LOG.info("Renditions: " + cap.getRenditionsCapability().value());
		LOG.info("\nFiling Capabilities");
		LOG.info("-----------------------");
		LOG.info("Multifiling supported: " + (cap.isMultifilingSupported() ? "true" : "false"));
		LOG.info("Unfiling supported: " + (cap.isUnfilingSupported() ? "true" : "false"));
		LOG.info("Version specific filing supported: " + (cap.isVersionSpecificFilingSupported() ? "true" : "false"));
		LOG.info("\nVersioning Capabilities");
		LOG.info("-----------------------");
		LOG.info("PWC searchable: " + (cap.isPwcSearchableSupported() ? "true" : "false"));
		LOG.info("PWC updatable: " + (cap.isPwcUpdatableSupported() ? "true" : "false"));
		LOG.info("All versions searchable: " + (cap.isAllVersionsSearchableSupported() ? "true" : "false"));
		LOG.info("\nQuery Capabilities");
		LOG.info("-----------------------");
		LOG.info("Query: " + cap.getQueryCapability().value());
		LOG.info("Join: " + cap.getJoinCapability().value());
		LOG.info("\nACL Capabilities");
		LOG.info("-----------------------");
		LOG.info("ACL: " + cap.getAclCapability().value());
		LOG.info("End of  repository capabilities");
	}

	/**
	 * logs the properties of a CMIS object, for debugging purposes
	 */
	public void logCmisObjectProperties(CmisObject object) {
		List<Property<?>> properties = object.getProperties();
		for (Property<?> property : properties) {
			LOG.info("Property: "+property.getDisplayName()+" ("+property.getLocalName()+") - "+property.getValue().toString());
		}
	}
}
