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

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.export.CmsExportProvider;
import se.simonsoft.cms.item.export.CmsExportReader;
import se.simonsoft.cms.item.export.CmsImportJob;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobStorage;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingRecipe;
import se.simonsoft.cms.publish.config.export.PublishExportJobFactory;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;

public class PublishPackageZipBuilder {
	
	private final CmsExportProvider exportProvider;
	private final PublishJobStorageFactory storageFactory;

	private static final Logger logger = LoggerFactory.getLogger(PublishPackageZipBuilder.class);
	
	@Inject
	public PublishPackageZipBuilder(
			@Named("config:se.simonsoft.cms.publish.export") CmsExportProvider exportProvider,
			PublishJobStorageFactory storageFactory) {
		
		this.exportProvider = exportProvider;
		this.storageFactory = storageFactory;
	}
	
	// Note: All aspects of PublishConfig does not necessarily apply to all items.
	public void getZip(Set<CmsItem> items, String configName, PublishConfig config, Set<PublishProfilingRecipe> profilingSet, OutputStream os) {
		
		final List<CmsImportJob> downloadJobs = new ArrayList<CmsImportJob>();
		final List<CmsExportReader> readers = new ArrayList<>();
		final ZipOutputStream zos = new ZipOutputStream(os); 
		
		logger.debug("Creating PublishExportJobs from: {} items", items.size());
		for (CmsItem item: items) {
			if (profilingSet == null) {
				// No profiling, one publication per item.
				downloadJobs.add(getPublishDownloadJob(item, config, configName, null));
			} else {
				// Profiling, zero or more publications per item.
				for (PublishProfilingRecipe profilingRecipe: profilingSet) {
					downloadJobs.add(getPublishDownloadJob(item, config, configName, profilingRecipe));
				}
			}
		}
		logger.debug("PublishExportJobs created.");
		
		if (downloadJobs.isEmpty()) {
			throw new IllegalArgumentException("No publications to export with this combination of items and profiling.");
		}
		
		logger.debug("Creating readers for: {} import jobs", downloadJobs.size());
		for (CmsImportJob j: downloadJobs) {
			CmsExportReader r = exportProvider.getReader();
			r.prepare(j);
			readers.add(r);
		}
		
		
		for (CmsExportReader r: readers) {
			InputStream contents = r.getContents();
			// Assuming that published zip files do not contain non-file data btw file entries such that the zip becomes incompatible with native Java ZIP (which does not read the "central directory").
			// https://stackoverflow.com/questions/12030703/uncompressing-a-zip-file-in-memory-in-java
			ZipInputStream zis = new ZipInputStream(contents);
			writeEntries(zis, zos);

			try {
				// AWS warns about "Not all bytes were read from the S3ObjectInputStream" draining the stream.
				int trailing = IOUtils.copy(contents, new NullOutputStream());
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
	
	private void writeEntries(ZipInputStream zis, ZipOutputStream zos) {
		try {
			
			ZipEntry nextEntry = zis.getNextEntry();
			while(nextEntry != null) {
				zos.putNextEntry(nextEntry);
				logger.debug("Writing entry: {} to new zip", nextEntry.getName());
				
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
	
	private CmsImportJob getPublishDownloadJob(CmsItem item, PublishConfig config, String configName, PublishProfilingRecipe profiling) {
		CmsItemPublish cmsItemPublish = new CmsItemPublish(item);
		PublishJobStorage s = storageFactory.getInstance(config.getOptions().getStorage(), cmsItemPublish, configName, profiling);
		return PublishExportJobFactory.getImportJobSingle(s, "zip");
	}

}
