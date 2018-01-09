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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentialsProvider;

import se.simonsoft.cms.export.storage.CmsExportAwsReaderSingle;
import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.export.CmsExportItemCmsFile;
import se.simonsoft.cms.item.export.CmsExportPath;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobStorage;
import se.simonsoft.cms.publish.config.export.PublishExportJob;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;

public class PublishPackageZip {
	
	private final PublishJobStorageFactory storageFactory;

	private final String cloudId;
	private final String bucketName;
	private final AWSCredentialsProvider credentials;

	private static final Logger logger = LoggerFactory.getLogger(PublishPackageZip.class);
	
	@Inject
	public PublishPackageZip(@Named("config:se.simonsoft.cms.cloudid") String cloudId,
							@Named("config:se.simonsoft.cms.publish.bucket") String bucketName,
							AWSCredentialsProvider credentials,
							PublishJobStorageFactory storageFactory) {
		
		this.cloudId = cloudId;
		this.bucketName = bucketName;
		this.credentials = credentials;
		this.storageFactory = storageFactory;
	}
	
	
	public void getZip(Set<CmsItem> items, String configName, PublishConfig config, Set<String> profiles, OutputStream os) {
		
		final List<PublishExportJob> downloadJobs = new ArrayList<PublishExportJob>();
		final List<CmsExportAwsReaderSingle> awsReaders = new ArrayList<>();
		final ZipOutputStream zos = new ZipOutputStream(os); 
		
		for (CmsItem item: items) {
			logger.debug("Creating PublishExportJobs from: {} items", items.size());
			PublishExportJob downloadJob = getPublishDownloadJob(item, config, configName);
			downloadJobs.add(downloadJob);
			logger.debug("PublishExportJobs created.");
		}
		
		
		logger.debug("Creating readers for: {} import jobs", downloadJobs.size());
		for (PublishExportJob j: downloadJobs) {
			CmsExportAwsReaderSingle r = new CmsExportAwsReaderSingle(cloudId, bucketName, credentials);
			r.prepare(j);
			awsReaders.add(r);
		}
		
		
		for (CmsExportAwsReaderSingle r: awsReaders) {
			
			InputStream contents = r.getContents();
			// Assuming that published zip files do not contain non-file data btw file entries such that the zip becomes incompatible with native Java ZIP (which does not read the "central directory").
			// https://stackoverflow.com/questions/12030703/uncompressing-a-zip-file-in-memory-in-java
			ZipInputStream zis = new ZipInputStream(contents);
			writeEntries(zis, zos);
			
			try {
				// AWS warns about "Not all bytes were read from the S3ObjectInputStream" draining the stream.
				byte[] buffer = new byte[1024];
				int length;
				int trailing = 0;
				while ((length = contents.read(buffer)) != -1) {
					 trailing = trailing + contents.read(buffer, 0, length);
				}
				logger.debug("Trailing size: {}", trailing);
				
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
		logger.debug("Getting zip files at S3 and re-package them.");
	}
	
	private void writeEntries(ZipInputStream zis, ZipOutputStream zos) {
		try {
			
			ZipEntry nextEntry = zis.getNextEntry();
			while(nextEntry != null) {
				zos.putNextEntry(nextEntry);
				logger.debug("Writing entry: {} to new zip", nextEntry.getName());
				
				byte[] buffer = new byte[1024];
				int length;
				while ((length = zis.read(buffer)) != -1) {
					zos.write(buffer, 0, length);
				}
				
				zos.closeEntry();
				zis.closeEntry();
				nextEntry = zis.getNextEntry();
			}
			
		} catch (IOException e) {
			logger.debug("Error when trying to write new zip entries: {}", e.getMessage());
			throw new RuntimeException(e);
		}
	}
	
	private PublishExportJob getPublishDownloadJob(CmsItem item, PublishConfig config, String configName) {
		CmsItemPublish cmsItemPublish = new CmsItemPublish(item);
		PublishJobStorage s = storageFactory.getInstance(config.getOptions().getStorage(), cmsItemPublish, configName, null);
		return new PublishExportJob(s, "zip");
	}

}
