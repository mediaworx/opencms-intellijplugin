/*
 * This file is part of the OpenCms plugin for IntelliJ by mediaworx.
 *
 * For further information about the OpenCms plugin for IntelliJ, please
 * see the project website at GitHub:
 * https://github.com/mediaworx/opencms-intellijplugin
 *
 * Copyright (C) 2007-2016 mediaworx berlin AG (http://www.mediaworx.com)
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

package com.mediaworx.intellij.opencmsplugin.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;

/**
 * Parent for all actions connecting to OpemCms. Checks if a debug session is running and halted and if so  shows an
 * error message instead of executing the action to avoid deadlocks.
 */
public abstract class OpenCmsConnectionAction extends OpenCmsPluginAction {

	@Override
	public void actionPerformed(AnActionEvent event) {
		super.actionPerformed(event);
		XDebugSession[] debugSessions = XDebuggerManager.getInstance(plugin.getProject()).getDebugSessions();
		boolean isDebugHalted = false;
		for (XDebugSession debugSession : debugSessions) {
			if (debugSession.isPaused() || debugSession.isSuspended()) {
				isDebugHalted = true;
				break;
			}
		}
		if (!isDebugHalted) {
			executeAction(event);
		}
		else {
			Messages.showDialog("OpenCms Action not possible when a debug session is paused.\nPlease resume or stop the debug session and try again.",
					"Error", new String[]{"Ok"}, 0, Messages.getErrorIcon());

		}
	}

	protected abstract void executeAction(AnActionEvent event);

}
