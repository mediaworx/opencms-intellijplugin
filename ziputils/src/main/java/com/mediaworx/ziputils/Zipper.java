package com.mediaworx.ziputils;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * Convenience class providing an easy way to create zip files and add files or folders to it. Uses Apache
 * commons-compress under the hood.
 */
public class Zipper {

	private static final Logger LOG = LoggerFactory.getLogger(Zipper.class);
	private static final String DEFAULT_ENCODING = "UTF-8";

	private OutputStream zipOutputStream;
	private ArchiveOutputStream zip;

	/**
	 * Creates a new zip file.
	 * @param zipFilename           filename for the zip
	 * @param zipTargetFolderPath   path of the target folder where the zip should be stored (it will be created if it
	 *                              doesn't exist)
	 * @throws IOException Exceptions from the underlying package framework are bubbled up
	 */
	public Zipper(String zipFilename, String zipTargetFolderPath) throws IOException {

		File targetFolder = new File(zipTargetFolderPath);
		if (!targetFolder.exists()) {
			FileUtils.forceMkdir(targetFolder);
		}

		if (!zipTargetFolderPath.endsWith(File.separator)) {
			zipTargetFolderPath = zipTargetFolderPath.concat(File.separator);
		}
		zipOutputStream = new FileOutputStream(zipTargetFolderPath + zipFilename);
		try {
			zip = new ArchiveStreamFactory().createArchiveOutputStream(ArchiveStreamFactory.ZIP, zipOutputStream);
		}
		catch (ArchiveException e) {
			// This should never happen, because "zip" is a known archive type, but let's log it anyway
			LOG.error("Cant create an archive of type " + ArchiveStreamFactory.ZIP);
		}
	}

	/**
	 * Adds the given file to the zip archive using the given relative path.
	 * @param zipRelativePath   the path of the file relative to the zip root
	 * @param file              the file to be added
	 * @throws IOException Exceptions from the underlying package framework are bubbled up
	 */
	public void addFile(String zipRelativePath, File file) throws IOException {
		zip.putArchiveEntry(new ZipArchiveEntry(zipRelativePath));
		IOUtils.copy(new FileInputStream(file), zip);
		zip.closeArchiveEntry();
	}

	/**
	 * Adds a directory entry with the given relative path to the zip.
	 * @param zipRelativePath   the path of the directory relative to the zip root
	 * @throws IOException Exceptions from the underlying package framework are bubbled up
	 */
	public void addDirectory(String zipRelativePath) throws IOException {
		if (!zipRelativePath.endsWith("/")) {
			zipRelativePath = zipRelativePath.concat("/");
		}
		zip.putArchiveEntry(new ZipArchiveEntry(zipRelativePath));
		zip.closeArchiveEntry();
	}

	/**
	 * Adds the given plaintextContent as a file to the zip archive using the given relative path using the default
	 * encoding (UTF-8).
	 * @param zipRelativePath   the path of the file relative to the zip root
	 * @param plaintextContent  text content to be added as a file
	 * @throws IOException Exceptions from the underlying package framework are bubbled up
	 */
	public void addStringAsFile(String zipRelativePath, String plaintextContent) throws IOException {
		addStringAsFile(zipRelativePath, plaintextContent, DEFAULT_ENCODING);
	}

	/**
	 * Adds the given plaintextContent as a file to the zip archive using the given relative path using the given
	 * encoding.
	 * @param zipRelativePath   the path of the file relative to the zip root
	 * @param plaintextContent  text content to be added as a file
	 * @param encoding          the encoding to be used
	 * @throws IOException Exceptions from the underlying package framework are bubbled up
	 */
	public void addStringAsFile(String zipRelativePath, String plaintextContent, String encoding) throws IOException {
		zip.putArchiveEntry(new ZipArchiveEntry(zipRelativePath));
		IOUtils.copy(new ByteArrayInputStream(plaintextContent.getBytes(encoding)), zip);
		zip.closeArchiveEntry();
	}

	/**
	 * Finalizes the zip file and writes it to the disk. It may be wise to call this method in a <code>finally</code>
	 * block to avoid dangling file streams.
	 */
	public void finish() {
		if (zip != null) {
			try {
				zip.finish();
			}
			catch (IOException e) {
				// this should never happen, because entries are always closed, but let's log it anyway
				LOG.error("error closing the zip", e);
			}
		}
		if (zipOutputStream != null) {
			try {
				zipOutputStream.close();
			}
			catch (IOException e) {
				LOG.error("error closing the zip output stream");
			}
		}
	}
}
