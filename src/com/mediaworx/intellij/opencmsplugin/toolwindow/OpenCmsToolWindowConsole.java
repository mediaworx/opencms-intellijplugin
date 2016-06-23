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

package com.mediaworx.intellij.opencmsplugin.toolwindow;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;

public class OpenCmsToolWindowConsole extends JTextPane {

	private static final Logger LOG = Logger.getInstance(OpenCmsToolWindowConsole.class);

	private JScrollPane scrollPane;
	private StyledDocument consoleDocument;
	private SimpleAttributeSet errorAttributes;
	private SimpleAttributeSet noticeAttributes;

	public OpenCmsToolWindowConsole() {
		super();
		init();
	}

	public void setScrollPane(JScrollPane scrollPane) {
		this.scrollPane = scrollPane;
	}

	private void init() {
		setEditable(false);

		consoleDocument = getStyledDocument();
		errorAttributes = new SimpleAttributeSet();
		StyleConstants.setForeground(errorAttributes, Color.RED);
		StyleConstants.setBold(errorAttributes, true);

		noticeAttributes = new SimpleAttributeSet();
		StyleConstants.setForeground(noticeAttributes, Color.BLUE);
		StyleConstants.setBold(noticeAttributes, true);
	}

	public void clear() {
		setText("");
	}

	public void append(String str) {
		append(str, null);
	}

	public void info(String str) {
		append(str + "\n", null);
	}

	public void notice(String str) {
		append(str + "\n", noticeAttributes);
	}

	public void error(String str) {
		append(str + "\n", errorAttributes);
	}

	private void append(String str, AttributeSet attributeSet) {
		UIUtil.invokeLaterIfNeeded(new ConsoleAppender(str, attributeSet));
	}

	public StyledDocument getConsoleDocument() {
		return consoleDocument;
	}

	private class ConsoleAppender implements Runnable {

		String stringToAppend;
		AttributeSet attributeSet;

		private ConsoleAppender(String stringToAppend, AttributeSet attributeSet) {
			this.stringToAppend = stringToAppend;
			this.attributeSet = attributeSet;
		}

		@Override
		public void run() {
			try {
				consoleDocument.insertString(consoleDocument.getLength(), stringToAppend, attributeSet);
			}
			catch (BadLocationException e) {
				LOG.warn("Exception while appending content to the console", e);
			}
			if (scrollPane != null && scrollPane.getVerticalScrollBar() != null) {
				JScrollBar scrollbar = scrollPane.getVerticalScrollBar();
				scrollPane.getVerticalScrollBar().setValue(scrollbar.getMaximum() - scrollbar.getVisibleAmount());
			}
		}

	}
}
