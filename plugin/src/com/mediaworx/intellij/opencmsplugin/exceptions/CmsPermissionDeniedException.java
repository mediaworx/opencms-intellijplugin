package com.mediaworx.intellij.opencmsplugin.exceptions;

import java.math.BigInteger;

public class CmsPermissionDeniedException extends Exception {

	public CmsPermissionDeniedException() {
		super();
	}

	public CmsPermissionDeniedException(String message) {
		super(message);
	}

	public CmsPermissionDeniedException(String message, Throwable cause) {
		super(message, cause);
	}
}
