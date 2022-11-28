/**
 * Copyright (C) 2009-2017 Simonsoft Nordic AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.simonsoft.cms.publish.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.cms.item.export.CmsExportProvider;
import se.simonsoft.cms.item.export.CmsExportReader;
import se.simonsoft.cms.item.export.CmsImportJob;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.job.PublishJob;
import se.simonsoft.cms.publish.config.export.PublishExportJobFactory;

public class PublishPackageZipBuilder {
	
	private final CmsExportProvider exportProvider;
	private final PublishJobFactory jobFactory;
	private final PublishConfigurationDefault publishConfiguration;
	
	private final List<String> zipFolderDisabledFormats;

	private static final Logger logger = LoggerFactory.getLogger(PublishPackageZipBuilder.class);
	
	@Inject
	public PublishPackageZipBuilder(
			@Named("config:se.simonsoft.cms.publish.export") CmsExportProvider exportProvider,
			PublishJobFactory jobFactory,
			PublishConfigurationDefault publishConfiguration) {
		
		this.exportProvider = exportProvider;
		this.jobFactory = jobFactory;
		this.publishConfiguration = publishConfiguration;
		
		String formats = "pdf|postscript|epub|htmlhelp"; // TODO: Enable inject of config. 
		this.zipFolderDisabledFormats = Arrays.asList(formats.split("\\|"));
	}
	
	// Note: All aspects of PublishConfig does not necessarily apply to all items.
	// The config is typically from the Release and assumed to apply to Translations as well.
	// It is very rare to define a different publish config for Translations, typically use the config areas in a unified config. 
	public void getZip(PublishPackage publishPackage, OutputStream os) {
		boolean addZipPrefix = !isZipFolderDisabled(publishPackage.getPublishConfig());
		getZip(publishPackage, addZipPrefix, os);
	}
	
	public void getZip(PublishPackage publishPackage, boolean addZipPrefix, OutputStream os) {
		
		final LinkedHashMap<PublishJob, CmsExportReader> readers = new LinkedHashMap<>();
		final ZipOutputStream zos = new ZipOutputStream(os); 
		
		logger.debug("Creating PublishExportJobs from: {} items", publishPackage.getPublishedItems().size());
		final Set<PublishJob> downloadJobs = jobFactory.getPublishJobsForPackage(publishPackage, publishConfiguration);
		logger.debug("PublishExportJobs created.");
		
		if (downloadJobs.isEmpty()) {
			// Might not happen in practice, no validation above that the profiling recipe(s) is defined on the item(s). Will fail during download.
			throw new IllegalArgumentException("No publications to export with this combination of items and profiling.");
		}
		
		logger.debug("Creating readers for: {} import jobs", downloadJobs.size());
		for (PublishJob pj: downloadJobs) {
			CmsExportReader r = exportProvider.getReader();
			r.prepare(getImportJob(pj));
			readers.put(pj, r);
		}
		
		HashMap<String, String> entries = new HashMap<>();
		
		logger.debug("Export zip package '{}' with ZIP prefix: {}", publishPackage.getPublication(), addZipPrefix);
		for (PublishJob pj: readers.keySet()) {
			CmsExportReader r = readers.get(pj);
			logger.info("Export zip processing source job for: {}", pj.getItemid());
			InputStream contents = r.getContents();
			// Assuming that published zip files do not contain non-file data btw file entries such that the zip becomes incompatible with native Java ZIP (which does not read the "central directory").
			// https://stackoverflow.com/questions/12030703/uncompressing-a-zip-file-in-memory-in-java
			ZipInputStream zis = new ZipInputStream(contents);
			
			// Some publication types should get an additional folder level.
			String folderName = "";
			if (addZipPrefix) {
				folderName = pj.getOptions().getPathname();
			}
			writeEntries(zis, zos, folderName, entries);

			try {
				// AWS warns about "Not all bytes were read from the S3ObjectInputStream" draining the stream.
				int trailing = IOUtils.copy(contents, NullOutputStream.NULL_OUTPUT_STREAM);
				logger.debug("S3 zip file trailing size: {}", trailing);
				
				zis.close();
			} catch (IOException e) {
				logger.warn("Could not close S3 InputStream: {}", e.getMessage());
			}

		}
		
		try {
			zos.finish();
			
		} catch (IOException e) {
			logger.error("Finish zip package failed with IOException: {}", e.getMessage(), e);
			throw new RuntimeException("Finish zip package failed with IOException: " + e.getMessage());
		} catch (Exception e) {
			logger.error("Finish zip package failed with Exception: {}", e.getMessage(), e);
			throw new RuntimeException("Finish zip package failed with Exception: " + e.getMessage());
		}
		logger.debug("Re-packaged zip files from S3.");
	}
	
	private void writeEntries(ZipInputStream zis, ZipOutputStream zos, String folderName, HashMap<String, String> entries) {
		String folderPrefix = "";
		if (folderName != null && !folderName.trim().isEmpty()) {
			folderPrefix = folderName;
			if (!folderPrefix.endsWith("/")) {
				folderPrefix = folderPrefix.concat("/");
			}
		}
		
		try {
			ZipEntry nextEntry = zis.getNextEntry();
			while(nextEntry != null) {
				// Must not reuse entry from ZIS. Might work when zip generated by Java but instable otherwise.
				String path = folderPrefix.concat(nextEntry.getName());
				// There is no CRC32 in the zip filed from PE (or not read by Java).
				DigestInputStream dis = new DigestInputStream(zis, MessageDigest.getInstance("SHA-1"));
				
				// Deduplicate / skip if the target path already has a file with the same checksum.
				if (!entries.containsKey(path)) {
					
					ZipEntry outEntry = new ZipEntry(path);
					outEntry.setTime(nextEntry.getTime()); // Preserve the file time.
					// Consider preserving the compression method if smarter than java.util.zip.
					zos.putNextEntry(outEntry);
					logger.debug("Writing entry: {} to new zip", outEntry.getName());
					
					IOUtils.copy(dis, zos);
					
					zos.closeEntry();
					zis.closeEntry();
					
					entries.put(path, toHex(dis.getMessageDigest().digest()));
					logger.debug("Zip entry exported {} : {}", path, entries.get(path));
				} else {
					IOUtils.copy(dis, NullOutputStream.NULL_OUTPUT_STREAM);
					zis.closeEntry();
					
					String digest = toHex(dis.getMessageDigest().digest());
					if (digest.equals(entries.get(path))) {
						logger.info("Deduplicating entry: {}", path);
					} else {
						logger.error("Zip entry {} duplicate {} != {}", path, digest, entries.get(path));
						throw new RuntimeException("Duplicate zip entry with different content: " + path);
					}
				}
				nextEntry = zis.getNextEntry();
			}
			
		} catch (IOException e) {
			logger.error("Export zip package failed with IOException: {}", e.getMessage(), e);
			throw new RuntimeException("Export zip package failed with IOException: " + e.getMessage());
		} catch (Exception e) {
			logger.error("Export zip package failed with Exception: {}", e.getMessage(), e);
			throw new RuntimeException("Export zip package failed with Exception: " + e.getMessage());
		}
	}

	
	private CmsImportJob getImportJob(PublishJob pj) {
		return PublishExportJobFactory.getImportJobSingle(pj.getOptions().getStorage(), "zip");
	}
	
	private boolean isZipFolderDisabled(PublishConfig config) {
		return this.zipFolderDisabledFormats.contains(config.getOptions().getFormat());
	}
	
	// http://stackoverflow.com/questions/332079/in-java-how-do-i-convert-a-byte-array-to-a-string-of-hex-digits-while-keeping-le
	private String toHex(byte[] bytes) {
		java.math.BigInteger bi = new java.math.BigInteger(1, bytes);
		return String.format("%0" + (bytes.length << 1) + "X", bi).toLowerCase();
	}

}
