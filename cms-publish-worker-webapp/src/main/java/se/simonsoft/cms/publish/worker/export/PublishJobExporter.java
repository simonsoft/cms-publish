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
package se.simonsoft.cms.publish.worker.export;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentialsProvider;

import se.simonsoft.cms.export.storage.CmsExportAwsWriterSingle;
import se.simonsoft.cms.item.export.CmsExportPath;
import se.simonsoft.cms.publish.PublishTicket;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobOptions;
import se.simonsoft.cms.publish.config.export.PublishExportJob;
import se.simonsoft.publish.worker.PublishJobService;

public class PublishJobExporter {
	
	private final String jobExtension = "zip";
	private final CmsExportAwsWriterSingle writer;
	private PublishJobService publishJobService;

	
	private static final Logger logger = LoggerFactory.getLogger(PublishJobExporter.class);
	
	@Inject
	public PublishJobExporter(@Named("config:se.simonsoft.cms.cloudid") String cloudId,
			@Named("config:se.simonsoft.cms.publish.bucket") String bucketName,
			AWSCredentialsProvider credentials,
			PublishJobService publishService) {
		
		this.writer = new CmsExportAwsWriterSingle(cloudId, bucketName, credentials);
		this.publishJobService = publishService;
	}
	
	
	public String exportJob(PublishJobOptions jobOptions) {
		logger.debug("Preparing publishJob {} for export to s3", jobOptions.getPathname());
		
		PublishExportJob job = new PublishExportJob(jobOptions.getStorage(), this.jobExtension);
		
		PublishTicket publishTicket = new PublishTicket(jobOptions.getProgress().getParams().get("ticket"));
		CmsExportItemPublishJob exportItem = new CmsExportItemPublishJob(publishTicket,
				publishJobService,
				new CmsExportPath("/".concat(jobOptions.getStorage().getPathnamebase().concat(".zip"))));
		job.addExportItem(exportItem);
		job.prepare();
		
		logger.debug("Preparing writer for export...");
		writer.prepare(job);
		logger.debug("Writer is prepared. Writing job to S3.");
		writer.write();

		logger.debug("Job has been exported to S3.");
		
		return job.getJobPath();
	}
}
