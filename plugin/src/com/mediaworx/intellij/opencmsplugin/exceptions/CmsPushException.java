package com.mediaworx.intellij.opencmsplugin.exceptions;

/**
 * Created with IntelliJ IDEA.
 * User: widmann
 * Date: 12.02.13
 * Time: 11:36
 * To change this template use File | Settings | File Templates.
 */
public class CmsPushException extends Exception {

	public CmsPushException() {
		super();
	}

	public CmsPushException(String message) {
		super(message);
	}

	public CmsPushException(String message, Throwable cause) {
		super(message, cause);
	}
}
