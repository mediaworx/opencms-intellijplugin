package com.mediaworx.intellij.opencmsplugin.toolwindow;

import com.intellij.openapi.diagnostic.Logger;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;

public class OpenCmsToolWindowConsole extends JTextPane {

	private static final Logger LOG = Logger.getInstance(OpenCmsToolWindowConsole.class);

	private JScrollPane scrollPane;
	private StyledDocument consoleDocument;
	private SimpleAttributeSet errorAttributes;

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
	}

	public void clear() {
		setText("");
	}

	public void info(String str) {
		append(str + "\n", null);
	}

	public void error(String str) {
		append(str + "\n", errorAttributes);
	}

	private void append(String str, AttributeSet attributeSet) {
		try {
			consoleDocument.insertString(consoleDocument.getLength(), str, attributeSet);
			if (scrollPane != null && scrollPane.getVerticalScrollBar() != null) {
				JScrollBar scrollbar = scrollPane.getVerticalScrollBar();
				scrollPane.getVerticalScrollBar().setValue(scrollbar.getMaximum() - scrollbar.getVisibleAmount());
			}
		}
		catch (BadLocationException e) {
			LOG.warn("Exception while appending content to the console", e);
		}
	}
}
