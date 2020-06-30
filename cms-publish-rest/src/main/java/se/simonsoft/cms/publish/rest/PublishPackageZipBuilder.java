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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.export.CmsExportProvider;
import se.simonsoft.cms.item.export.CmsExportReader;
import se.simonsoft.cms.item.export.CmsImportJob;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.job.PublishJob;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingRecipe;
import se.simonsoft.cms.publish.config.export.PublishExportJobFactory;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;

public class PublishPackageZipBuilder {
	
	private final CmsExportProvider exportProvider;
	private final PublishJobFactory jobFactory;
	
	private final List<String> zipFolderDisabledFormats;

	private static final Logger logger = LoggerFactory.getLogger(PublishPackageZipBuilder.class);
	
	@Inject
	public PublishPackageZipBuilder(
			@Named("config:se.simonsoft.cms.publish.export") CmsExportProvider exportProvider,
			PublishJobFactory jobFactory) {
		
		this.exportProvider = exportProvider;
		this.jobFactory = jobFactory;
		
		// TODO: Consider supporting deduplication of graphics and the ability to avoid ZIP folder for some formats, e.g. HTML, RTF.
		String formats = "pdf, postscript, epub, htmlhelp"; // TODO: Enable inject of config. 
		this.zipFolderDisabledFormats = Arrays.asList(formats.split(","));
	}
	
	// Note: All aspects of PublishConfig does not necessarily apply to all items.
	// The config is typically from the Release and assumed to apply to Translations as well.
	// Assuming that naming Template applies across all items.
	public void getZip(PublishPackage publishPackage, OutputStream os) {
		
		final List<PublishJob> downloadJobs = new ArrayList<>();
		final LinkedHashMap<PublishJob, CmsExportReader> readers = new LinkedHashMap<>();
		final ZipOutputStream zos = new ZipOutputStream(os); 
		
		logger.debug("Creating PublishExportJobs from: {} items", publishPackage.getPublishedItems().size());
		for (CmsItem item: publishPackage.getPublishedItems()) {
			if (publishPackage.getProfilingSet() == null) {
				// No profiling, one publication per item.
				downloadJobs.add(getPublishDownloadJob(item, publishPackage.getPublishConfig(), publishPackage.getPublication(), null));
			} else {
				// Profiling, zero or more publications per item.
				for (PublishProfilingRecipe profilingRecipe: publishPackage.getProfilingSet()) {
					downloadJobs.add(getPublishDownloadJob(item, publishPackage.getPublishConfig(), publishPackage.getPublication(), profilingRecipe));
				}
			}
		}
		logger.debug("PublishExportJobs created.");
		
		if (downloadJobs.isEmpty()) {
			throw new IllegalArgumentException("No publications to export with this combination of items and profiling.");
		}
		
		logger.debug("Creating readers for: {} import jobs", downloadJobs.size());
		for (PublishJob pj: downloadJobs) {
			CmsExportReader r = exportProvider.getReader();
			r.prepare(getImportJob(pj));
			readers.put(pj, r);
		}
		
		boolean addZipPrefix = !isZipFolderDisabled(publishPackage.getPublishConfig());
		logger.debug("Export zip package '{}' with ZIP prefix: {}", publishPackage.getPublication(), addZipPrefix);
		
		for (PublishJob pj: readers.keySet()) {
			CmsExportReader r = readers.get(pj);
			InputStream contents = r.getContents();
			// Assuming that published zip files do not contain non-file data btw file entries such that the zip becomes incompatible with native Java ZIP (which does not read the "central directory").
			// https://stackoverflow.com/questions/12030703/uncompressing-a-zip-file-in-memory-in-java
			ZipInputStream zis = new ZipInputStream(contents);
			
			// Some publication types should get an additional folder level.
			String folderName = "";
			if (addZipPrefix) {
				folderName = pj.getOptions().getPathname();
			}
			writeEntries(zis, zos, folderName);

			try {
				// AWS warns about "Not all bytes were read from the S3ObjectInputStream" draining the stream.
				int trailing = IOUtils.copy(contents, NullOutputStream.NULL_OUTPUT_STREAM);
				logger.debug("S3 zip file trailing size: {}", trailing);
				
				zis.close();
			} catch (IOException e) {
				logger.warn("Could not close inputStream from Aws: {}", e.getMessage());
			}

		}
		
		try {
			zos.finish();
		} catch (IOException e) {
			logger.error("Could not finish zip");
			throw new RuntimeException("Could not finish zip", e);
		}
		logger.debug("Re-packaged zip files from S3.");
	}
	
	private void writeEntries(ZipInputStream zis, ZipOutputStream zos, String folderName) {
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
				ZipEntry outEntry = new ZipEntry(folderPrefix.concat(nextEntry.getName()));
				zos.putNextEntry(outEntry);
				logger.debug("Writing entry: {} to new zip", outEntry.getName());
				
				IOUtils.copy(zis, zos);
				
				zos.closeEntry();
				zis.closeEntry();
				nextEntry = zis.getNextEntry();
			}
			
		} catch (IOException e) {
			logger.debug("Error when trying to write new zip entries: {}", e.getMessage());
			throw new RuntimeException(e);
		}
	}
	
	private PublishJob getPublishDownloadJob(CmsItem item, PublishConfig config, String configName, PublishProfilingRecipe profiling) {
		CmsItemPublish cmsItemPublish = new CmsItemPublish(item);
		PublishJob pj = this.jobFactory.getPublishJob(cmsItemPublish, config, configName, profiling);
		return pj;
	}
	
	private CmsImportJob getImportJob(PublishJob pj) {
		return PublishExportJobFactory.getImportJobSingle(pj.getOptions().getStorage(), "zip");
	}
	
	private boolean isZipFolderDisabled(PublishConfig config) {
		return this.zipFolderDisabledFormats.contains(config.getOptions().getFormat());
	}

}
