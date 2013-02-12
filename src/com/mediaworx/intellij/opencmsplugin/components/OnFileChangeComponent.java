package com.mediaworx.intellij.opencmsplugin.components;

import com.intellij.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManagerAdapter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.*;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import com.mediaworx.intellij.opencmsplugin.cmis.VfsAdapter;
import com.mediaworx.intellij.opencmsplugin.configuration.OpenCmsPluginConfigurationData;
import com.mediaworx.intellij.opencmsplugin.exceptions.CmsPermissionDeniedException;
import com.mediaworx.intellij.opencmsplugin.tools.PathTools;
import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class OnFileChangeComponent implements ProjectComponent {

    private Project project;
    private OpenCmsPluginConfigurationData config;
    private VfsAdapter vfsAdapter;

    public OnFileChangeComponent(Project project) {
        this.project = project;
        this.config = project.getComponent(OpenCmsPluginConfigurationComponent.class).getConfigurationData();
        this.vfsAdapter = project.getComponent(OpenCmsPluginComponent.class).getVfsAdapter();
    }


    @NotNull
    public String getComponentName() {
        return "My OnFileChangeComponent";
    }

    public void initComponent() {

        if (config != null && config.isOpenCmsPluginActive() && vfsAdapter != null && vfsAdapter.isConnected()) {

            MessageBus bus = ApplicationManager.getApplication().getMessageBus();
            MessageBusConnection connection = bus.connect();

            connection.subscribe(AppTopics.FILE_DOCUMENT_SYNC,
                    new FileDocumentManagerAdapter() {
                        @Override
                        public void beforeDocumentSaving(Document document) {
                            System.out.println("Document was saved: "+document);
                            // TODO: Überlegen, ob File Save Events direkt ins VFS gesynct werden sollen
                        }
                    });

            connection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
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

                             // check if the file belongs to an OpenCms module
                            if (PathTools.isFileInModulePath(config, file)) {
                                String vfsPath = PathTools.getVfsPathFromIdeaVFile(PathTools.getModuleName(config, file), config, file);
                                if (vfsAdapter.exists(vfsPath)) {
                                    vfsFilesToBeDeleted.add(vfsPath);
                                }
                            }
                        }

                        // File is moved
                        if (event instanceof VFileMoveEvent) {
                            VirtualFile file = event.getFile();
                            VirtualFile oldParent = ((VFileMoveEvent) event).getOldParent();
                            VirtualFile newParent = ((VFileMoveEvent) event).getNewParent();

                            // old and new parent are in a module -> move the file in the OpenCms VFS
                            if (PathTools.isFileInModulePath(config, oldParent) && PathTools.isFileInModulePath(config, newParent)) {
                                String module = PathTools.getModuleName(config, file);
                                String oldParentPath = PathTools.getVfsPathFromIdeaVFile(module, config, oldParent);
                                String vfsPath = oldParentPath+"/"+file.getName();
                                if (vfsAdapter.exists(vfsPath)) {
                                    String newParentPath = PathTools.getVfsPathFromIdeaVFile(module, config, newParent);
                                    vfsFilesToBeMoved.add(new VfsFileMoveInfo(vfsPath, oldParentPath, newParentPath));
                                }
                            }

                            // check if the file belongs to an OpenCms module
                            if (PathTools.isFileInModulePath(config, file)) {
                                String vfsPath = PathTools.getVfsPathFromIdeaVFile(PathTools.getModuleName(config, file), config, file);
                                if (vfsAdapter.exists(vfsPath)) {
                                    vfsFilesToBeDeleted.add(vfsPath);
                                }
                            }
                        }

                        // File is renamed
                        if (event instanceof VFilePropertyChangeEvent) {
                            String propertyName = ((VFilePropertyChangeEvent) event).getPropertyName();
                            if (propertyName.equals("name")) {
                                VirtualFile file = event.getFile();
                                String oldName = (String)((VFilePropertyChangeEvent) event).getOldValue();
                                String newName = (String)((VFilePropertyChangeEvent) event).getNewValue();
                                String module = PathTools.getModuleName(config, file);
                                String newVfsPath = PathTools.getVfsPathFromIdeaVFile(module, config, file);
                                String oldVfsPath = newVfsPath.replaceFirst(newName, oldName);

                                if (vfsAdapter.exists(oldVfsPath)) {
                                    vfsFilesToBeRenamed.add(new VfsFileRenameInfo(oldVfsPath, newName));
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

                        int dlgStatus = Messages.showOkCancelDialog(msg.toString(), "Delete files/folders?", Messages.getQuestionIcon());

                        if (dlgStatus == 0) {
                            for (String vfsFileToBeDeleted : vfsFilesToBeDeleted) {
                                vfsAdapter.deleteFile(vfsFileToBeDeleted);
                            }
                        }
                    }

                    // Move files
                    if (vfsFilesToBeMoved.size() > 0) {
                        StringBuilder msg = new StringBuilder("Do you want to move the following files/folders in the OpenCms VFS as well?");
                        for (VfsFileMoveInfo vfsFileToBeMoved : vfsFilesToBeMoved) {
                            msg.append("\n").append(vfsFileToBeMoved.getVfsPath());
                        }

                        int dlgStatus = Messages.showOkCancelDialog(msg.toString(), "Move files/folders?", Messages.getQuestionIcon());

                        if (dlgStatus == 0) {
                            for (VfsFileMoveInfo moveInfo : vfsFilesToBeMoved) {
	                            try {
	                                System.out.println("Moving "+moveInfo.getVfsPath()+" to "+moveInfo.getNewParentPath());
	                                Folder oldParent = (Folder)vfsAdapter.getVfsObject(moveInfo.getOldParentPath());
	                                Folder newParent = (Folder)vfsAdapter.getVfsObject(moveInfo.getNewParentPath());
	                                if (newParent == null) {
	                                    newParent = vfsAdapter.createFolder(moveInfo.getNewParentPath());
	                                }
	                                FileableCmisObject file = (FileableCmisObject)vfsAdapter.getVfsObject(moveInfo.getVfsPath());
	                                file.move(oldParent, newParent);
	                            }
	                            catch (CmsPermissionDeniedException e) {
		                            Messages.showDialog("Error moving files/folders. " + e.getMessage(),
				                            "Error", new String[]{"Ok"}, 0, Messages.getErrorIcon());
	                            }
                            }
                        }
                    }

                    // Rename files
                    if (vfsFilesToBeRenamed.size() > 0) {
                        StringBuilder msg = new StringBuilder("Do you want to rename the following files/folders in the OpenCms VFS as well?");
                        for (VfsFileRenameInfo vfsFileToBeRenamed : vfsFilesToBeRenamed) {
                            msg.append("\n").append(vfsFileToBeRenamed.getOldVfsPath()+" -> "+vfsFileToBeRenamed.getNewName());
                        }

                        int dlgStatus = Messages.showOkCancelDialog(msg.toString(), "Move files/folders?", Messages.getQuestionIcon());

                        if (dlgStatus == 0) {
                            for (VfsFileRenameInfo renameInfo : vfsFilesToBeRenamed) {
                                System.out.println("Renaming " + renameInfo.getOldVfsPath() + " to " + renameInfo.getNewName());
	                            try {
		                            CmisObject file = vfsAdapter.getVfsObject(renameInfo.oldVfsPath);
		                            HashMap<String, Object> properties = new HashMap<String, Object>();
	                                properties.put(PropertyIds.NAME, renameInfo.getNewName());
	                                file.updateProperties(properties);
	                            }
	                            catch (CmsPermissionDeniedException e) {
		                            System.out.println("Exception moving files: " + e.getMessage());
		                            Messages.showDialog("Error moving files/folders. "+e.getMessage(),
				                            "Error", new String[]{"Ok"}, 0, Messages.getErrorIcon());
	                            }
                            }
                        }
                    }

                }
            });
        }
    }

    public void disposeComponent() {
    }

    public void projectOpened() {
    }

    public void projectClosed() {
    }


    private class VfsFileMoveInfo {

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

    private class VfsFileRenameInfo {
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
