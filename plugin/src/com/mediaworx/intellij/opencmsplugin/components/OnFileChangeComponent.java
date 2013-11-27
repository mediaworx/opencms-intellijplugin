package com.mediaworx.intellij.opencmsplugin.components;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

/**
 * handles changes (edits, moves, renames, deletions) to files in the IntelliJ File System
 */
public class OnFileChangeComponent implements ProjectComponent {

	private Project project;

	public OnFileChangeComponent(Project project) {
		this.project = project;
	}

	@NotNull
	public String getComponentName() {
		return "OpenCmsPlugin.OnFileChangeComponent";
	}

	public void initComponent() {
		MessageBus bus = ApplicationManager.getApplication().getMessageBus();
		MessageBusConnection connection = bus.connect();

		OpenCmsPlugin plugin = project.getComponent(OpenCmsPlugin.class);
		OpenCmsModuleFileChangeListener fileChangeListener = new OpenCmsModuleFileChangeListener(plugin);
		connection.subscribe(VirtualFileManager.VFS_CHANGES, fileChangeListener);

		// TODO: think about automatically syncing file save events of module files straight to the VFS
//		connection.subscribe(AppTopics.FILE_DOCUMENT_SYNC,
//				new FileDocumentManagerAdapter() {
//					@Override
//					public void beforeDocumentSaving(@NotNull Document document) {
//						LOG.debug("Document was saved: "+document);
//					}
//				}
//		);
	}

	public void disposeComponent() {
	}

	public void projectOpened() {
	}

	public void projectClosed() {
		project = null;
	}

}
