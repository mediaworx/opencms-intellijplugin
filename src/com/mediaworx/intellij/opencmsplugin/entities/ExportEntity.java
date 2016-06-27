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

package com.mediaworx.intellij.opencmsplugin.entities;

/**
 * Bean containing information of resources to be exported from the VFS to the webapp's RFS (due to the configuration of
 * module export points in OpenCms)
 */
public class ExportEntity {

	/**
	 * Creates a new export entity
	 */
	public ExportEntity() {
	}

	String sourcePath;
	String targetPath;
	String vfsPath;
	String destination;
	boolean toBeDeleted;

	/**
	 * Returns the RFS source path
 	 * @return the RFS source path
	 */
	public String getSourcePath() {
		return sourcePath;
	}

	/**
	 * Sets the RFS source path
	 * @param sourcePath the RFS source path
	 */
	public void setSourcePath(String sourcePath) {
		this.sourcePath = sourcePath;
	}

	/**
	 * Returns the export target path (somewhere in the webapp's folder structure)
	 * @return the export target path
	 */
	public String getTargetPath() {
		return targetPath;
	}

	/**
	 * Sets the export target path
	 * @param targetPath the export target path
	 */
	public void setTargetPath(String targetPath) {
		this.targetPath = targetPath;
	}

	/**
	 * Gets the entity's OpenCms VFS path
	 * @return the entity's OpenCms VFS path
	 */
	public String getVfsPath() {
		return vfsPath;
	}

	/**
	 * Sets the entity's OpenCms VFS path
	 * @param vfsPath the entity's OpenCms VFS path
	 */
	public void setVfsPath(String vfsPath) {
		this.vfsPath = vfsPath;
	}

	/**
	 * Returns the export point's destination (the RFS target configured in OpenCms)
	 * @return the export point's destination
	 */
	public String getDestination() {
		return destination;
	}

	/**
	 * Sets the export point's destination
	 * @param destination the export point's destination (the RFS target configured in OpenCms)
	 */
	public void setDestination(String destination) {
		this.destination = destination;
	}

	/**
	 * Flag denoting if the exported entity is to be deleted
	 * @return <code>true</code> if the exported entity is to be deleted, <code>false</code> otherwise
	 */
	public boolean isToBeDeleted() {
		return toBeDeleted;
	}

	/**
	 * Sets the flag denoting if the exported entity is to be deleted
	 * @param toBeDeleted <code>true</code> if the exported entity is to be deleted, <code>false</code> otherwise
	 */
	public void setToBeDeleted(boolean toBeDeleted) {
		this.toBeDeleted = toBeDeleted;
	}

	/**
	 * Returns a String representation of this export entity (for debugging purposes)
	 * @return a String representation of this export entity
	 */
	@Override
	public String toString() {
		return "ExportEntity {\n" +
				"  sourcePath:  " + sourcePath + "\n" +
				"  targetPath:  " + targetPath + "\n" +
				"  vfsPath:     " + vfsPath + "\n" +
				"  destination: " + destination + "\n" +
				"  toBeDeleted: " + toBeDeleted + "\n" +
				"} " + super.toString();
	}
}
