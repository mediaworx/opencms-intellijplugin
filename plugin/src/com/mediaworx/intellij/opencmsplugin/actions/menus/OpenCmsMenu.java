package com.mediaworx.intellij.opencmsplugin.actions.menus;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.diagnostic.Logger;
import com.mediaworx.intellij.opencmsplugin.OpenCmsPlugin;

public abstract class OpenCmsMenu extends DefaultActionGroup {

	private static final Logger LOG = Logger.getInstance(OpenCmsMenu.class);

	protected OpenCmsPlugin plugin;
	protected ActionManager actionManager;

	protected OpenCmsMenu(OpenCmsPlugin plugin, boolean popup) {
		super("_OpenCms", popup);
		this.plugin = plugin;
		actionManager = ActionManager.getInstance();
		registerActions();
	}

	protected abstract void registerActions();

	public abstract void unregisterActions();

}
