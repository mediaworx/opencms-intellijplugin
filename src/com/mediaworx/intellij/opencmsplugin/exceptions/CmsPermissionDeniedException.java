package com.mediaworx.intellij.opencmsplugin.exceptions;

public class CmsPermissionDeniedException extends Exception {

	public CmsPermissionDeniedException(String message) {
		super(message);
	}

	public CmsPermissionDeniedException(String message, Throwable cause) {
		super(message, cause);
	}
}
