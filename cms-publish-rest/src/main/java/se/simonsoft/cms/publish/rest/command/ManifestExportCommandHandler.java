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
package se.simonsoft.cms.publish.rest.command;


import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.cms.export.storage.CmsExportAwsReaderSingle;
import se.simonsoft.cms.export.storage.CmsExportAwsWriterSingle;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.command.CommandRuntimeException;
import se.simonsoft.cms.item.command.ExternalCommandHandler;
import se.simonsoft.cms.item.export.CmsExportAccessDeniedException;
import se.simonsoft.cms.item.export.CmsExportJobNotFoundException;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobManifest;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobOptions;
import se.simonsoft.cms.publish.config.export.PublishExportJob;
import se.simonsoft.cms.publish.config.manifest.CmsExportItemPublishManifest;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.fasterxml.jackson.databind.ObjectWriter;

public class ManifestExportCommandHandler implements ExternalCommandHandler<PublishJobOptions>{


	private static final Logger logger = LoggerFactory.getLogger(ManifestExportCommandHandler.class);
	private final ObjectWriter writerPublishManifest;
	private final String extensionManifest = "json";
	private final String extensionPublishResult = "zip";
	private final String cloudId;
	private final String bucketName;
	private final AWSCredentialsProvider credentials;

	@Inject
	public ManifestExportCommandHandler(@Named("config:se.simonsoft.cms.cloudid") String cloudId,
			@Named("config:se.simonsoft.cms.publish.bucket") String bucketName,
			AWSCredentialsProvider credentials,
			ObjectWriter objectWriter) {
		
		this.writerPublishManifest = objectWriter;
		this.cloudId = cloudId;
		this.bucketName = bucketName;
		this.credentials = credentials;
		
	}

	@Override
	public String handleExternalCommand(CmsItemId itemId, PublishJobOptions options) {
		logger.debug("Requesting export of PublishJob manifest.");
		
		PublishJobManifest manifest = options.getManifest();
		if (manifest == null) {
			throw new IllegalArgumentException("Requires a valid PublishJobManifest object.");
		}
		
		if (!isPublishResultExists(itemId, options)) {
			logger.warn("Abort manifest export, publish result does not exist: " + itemId);
			throw new CommandRuntimeException("PublishResultMissing");
		}
		
		logger.debug("Preparing publishJob manifest{} for export to s3", manifest);

		PublishExportJob job = new PublishExportJob(options.getStorage(), this.extensionManifest);
		CmsExportItemPublishManifest exportItem = new CmsExportItemPublishManifest(writerPublishManifest, manifest);
		
		job.addExportItem(exportItem);
		job.prepare();

		logger.debug("Preparing writer for export...");
		CmsExportAwsWriterSingle exportWriter = new CmsExportAwsWriterSingle(cloudId, bucketName, credentials);
		exportWriter.prepare(job);
		logger.debug("Writer is prepared. Writing job to S3.");
		exportWriter.write();
		logger.debug("Jobs manifest has been exported to S3 at path: {}", job.getJobPath());
		
		return null;
	}
	
	
	private boolean isPublishResultExists(CmsItemId itemId, PublishJobOptions options) {
		
		boolean result = false;
		
		// TODO: Change type to CmsImportJob.
		PublishExportJob job = new PublishExportJob(options.getStorage(), this.extensionPublishResult);
		// No items, no prepare for CmsImportJob.

		logger.debug("Preparing reader in order to verify that Publish result exists...");
		CmsExportAwsReaderSingle exportReader = new CmsExportAwsReaderSingle(cloudId, bucketName, credentials);
		try {
			exportReader.prepare(job);
			// TODO: Can we get the size to verify that the file is not empty?
			result = true;
		} catch (CmsExportAccessDeniedException | CmsExportJobNotFoundException e) {
			logger.info("Publish result missing: {}", itemId);
		} catch (Exception e) {
			logger.warn("Exception when reading Publish bucket: {}", e.getMessage(), e);
		}
		
		return result;
	}

	
}
