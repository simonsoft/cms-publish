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
package se.simonsoft.cms.publish.config.export;


import java.io.InputStream;
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.fasterxml.jackson.databind.ObjectWriter;

import se.simonsoft.cms.export.storage.CmsExportAwsWriterSingle;
import se.simonsoft.cms.item.export.CmsExportItemInputStream;
import se.simonsoft.cms.item.export.CmsExportPath;
import se.simonsoft.cms.item.export.CmsExportWriter;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobManifest;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobOptions;
import se.simonsoft.cms.publish.config.manifest.CmsExportItemPublishManifest;
import se.simonsoft.cms.publish.config.status.report.WorkerStatusReport;
import se.simonsoft.cms.publish.config.status.report.WorkerStatusReport.WorkerEvent;

public class PublishJobExportS3Service implements PublishJobExportService {
	
	private final CmsExportWriter writer;
	private final String jobExtension = "zip";
	private final String manifestExtension = "json";
	private ObjectWriter writerPublishManifest;
	private WorkerStatusReport statusReport;
	
	
	private static final Logger logger = LoggerFactory.getLogger(PublishJobExportS3Service.class);
	
	@Inject
	public PublishJobExportS3Service(@Named("config:se.simonsoft.cms.cloudid") String cloudId,
			@Named("config:se.simonsoft.cms.publish.bucket") String bucketName,
			AWSCredentialsProvider credentials,
			ObjectWriter writerPublishManifest,
			WorkerStatusReport statusReport) {
		this.writer = new CmsExportAwsWriterSingle(cloudId, bucketName, credentials);
		this.writerPublishManifest = writerPublishManifest.forType(PublishJobManifest.class);
		this.statusReport = statusReport;
	}

	@Override
	public String exportJob(InputStream is, PublishJobOptions jobOptions) {
		logger.debug("Preparing publishJob {} for export to s3", jobOptions.getPathname());
		
		PublishExportJob job = new PublishExportJob(jobOptions.getStorage(), this.jobExtension);
		CmsExportItemInputStream exportItem = new CmsExportItemInputStream(
				is,
				new CmsExportPath("/".concat(jobOptions.getStorage().getPathnamebase().concat(".zip")))); 
		job.addExportItem(exportItem);
		job.prepare();
		
		logger.debug("Preparing writer for export...");
		writer.prepare(job);
		logger.debug("Writer is prepared. Writing job to S3.");
		writer.write();

		logger.debug("Job has been exported to S3.");
		updateStatusReport(new Date(), "Job exported to S3", jobOptions.getProgress().getParams().get("ticket"));
		return job.getJobPath();
	}

	@Override
	public String exportJobManifest(PublishJobOptions jobOptions) {
		
		if (jobOptions == null) {
			throw new IllegalArgumentException("Requires a valid PublishJobOptions object.");
		}
		
		PublishJobManifest manifest = jobOptions.getManifest();
		if (manifest == null) {
			throw new IllegalArgumentException("Requires a valid PublishJobManifest object.");
		}
		
		logger.debug("Preparing publishJob manifest{} for export to s3", manifest);
		
		PublishExportJob job = new PublishExportJob(jobOptions.getStorage(), this.manifestExtension);
		CmsExportItemPublishManifest exportItem = new CmsExportItemPublishManifest(writerPublishManifest, manifest);
		
		job.addExportItem(exportItem);
		job.prepare();
		
		logger.debug("Preparing writer for export...");
		writer.prepare(job);
		logger.debug("Writer is prepared. Writing job to S3.");
		writer.write();
		logger.debug("Jobs manifest has been exported to S3.");
		return job.getJobPath();
	}
	
	private void updateStatusReport(Date timeStamp, String action, String description) {
		WorkerEvent event = new WorkerEvent(action, timeStamp, description);
		statusReport.addWorkerEvent(event);
	}
}