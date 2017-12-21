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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.model.S3ObjectInputStream;

import se.simonsoft.cms.export.storage.CmsExportAwsReaderSingle;
import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobStorage;
import se.simonsoft.cms.publish.config.export.PublishExportJob;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;

public class PublishPackageZip {
	
	private final CmsExportAwsReaderSingle awsReader;
	private final PublishJobStorageFactory storageFactory;

	private static final Logger logger = LoggerFactory.getLogger(PublishPackageZip.class);
	
	public PublishPackageZip(CmsExportAwsReaderSingle awsReader,
								PublishJobStorageFactory storageFactory) {
		
		this.awsReader = awsReader;
		this.storageFactory = storageFactory;
	}
	
	
	public void getZip(Set<CmsItem> items, PublishConfig config, Set<String> profiles, OutputStream os, String configName) {
		
		if (!(os instanceof ZipOutputStream)) {
			throw new IllegalArgumentException("PublishPackageZip handles only zip outputs. Given type of OutputStream: " + os.getClass());
		}
		
		ZipOutputStream zos = (ZipOutputStream) os; 
		
		List<PublishExportJob> jobs = new ArrayList<PublishExportJob>();
		
		for (CmsItem item: items) {
			logger.debug("Creating PublishExportJobs from: {} items", items.size());
			PublishExportJob job = getPublishExportJob(item, config, configName);
			jobs.add(job);
			logger.debug("PublishExportJobs created.");
		}
		
		for (PublishExportJob j: jobs) {
			logger.debug("Getting job: {}", j.getJobName());
			awsReader.prepare(j);
			InputStream contents = awsReader.getContents();
			ZipInputStream zis = new ZipInputStream(contents);
			
			try {
				
				ZipEntry nextEntry = zis.getNextEntry();
				while(nextEntry != null) {
					zos.putNextEntry(nextEntry);
					
					byte[] buffer = new byte[1024];
					int length;
					while ((length = zis.read(buffer)) != -1) {
						zos.write(buffer, 0, length);
					}
					
					zos.closeEntry();
					nextEntry = zis.getNextEntry();
				}
			} catch (IOException e) {
				logger.debug("e: {}", e.getMessage());
				throw new RuntimeException("Could not read zip entry", e);
			} finally {
				try {
					zis.close();
					contents.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		logger.debug("Getting zip files at S3 and re-package them.");
		
	}
	
	
	private PublishExportJob getPublishExportJob(CmsItem item, PublishConfig config, String configName) {
		CmsItemPublish cmsItemPublish = new CmsItemPublish(item);
		PublishJobStorage s = storageFactory.getInstance(config.getOptions().getStorage(), cmsItemPublish, configName, null);
		PublishExportJob job = new PublishExportJob(s, "zip");
		return job;
	}

}
