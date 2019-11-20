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

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.command.ExternalCommandHandler;
import se.simonsoft.cms.item.export.CmsExportJob;
import se.simonsoft.cms.item.export.CmsExportProvider;
import se.simonsoft.cms.item.export.CmsExportWriter;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobOptions;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobPreProcess;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobStorage;
import se.simonsoft.cms.publish.config.export.PublishExportJobFactory;
import se.simonsoft.cms.release.export.ReleaseExportOptions;
import se.simonsoft.cms.release.export.ReleaseExportService;

public class PublishPreprocessCommandHandler implements ExternalCommandHandler<PublishJobOptions> {

	private final CmsExportProvider exportProvider;
	private final Map<CmsRepository, ReleaseExportService> exportServices;
	//private final String bucketName;
	//private final AmazonS3 s3Client;

	private static final Logger logger = LoggerFactory.getLogger(PublishPreprocessCommandHandler.class);

	@Inject
	public PublishPreprocessCommandHandler(
			@Named("config:se.simonsoft.cms.publish.export") CmsExportProvider exportProvider, 
			//@Named("config:se.simonsoft.cms.publish.bucket") String bucketName, 
			Map<CmsRepository, ReleaseExportService> exportServices
			//Region region, 
			//AWSCredentialsProvider credentials
			) {

		this.exportProvider = exportProvider;
		this.exportServices = exportServices;
		//this.bucketName = bucketName;
		//this.s3Client = AmazonS3Client.builder().withRegion(region.getName()).withCredentials(credentials).build();
	}

	@Override
	public String handleExternalCommand(CmsItemId itemId, PublishJobOptions options) {

		final PublishJobPreProcess preprocess = options.getPreprocess();
		if (options.getDelivery() == null) {
			throw new IllegalArgumentException("Need a valid PublishJobPreProcess object.");
		}

		final PublishJobStorage storage = options.getStorage();
		if (storage == null) {
			throw new IllegalArgumentException("Need a valid PublishJobStorage object.");
		}

		if (preprocess.getType() == null) {
			throw new IllegalArgumentException("Need a valid PublishJobPreProcess object with type attribute.");
		} else if (preprocess.getType().equals("webapp-export")) {
			logger.debug("Performing Webapp Export: {}", itemId);
			doWebappExport(itemId, options);
		} else {
			throw new IllegalArgumentException("Unsupported Preprocess type: " + preprocess.getType());
		}

		return null;
	}

	
	void doWebappExport(CmsItemId itemId, PublishJobOptions options) {
		final PublishJobPreProcess preprocess = options.getPreprocess();
		final PublishJobStorage storage = options.getStorage();

		if (storage.getType() != null && !storage.getType().equals("s3")) {
			throw new IllegalArgumentException("Unsupported storage type: " + storage.getType());
		}

		// Storing with extension: .preprocess.zip
		// Unless options.type = "none", then export final output: .zip
		// Unrelated - Multi-step engines: .progress.zip
		String pathext = "preprocess.zip";
		if (options.getType().equals("none") || options.getType().equals("export")) {
			// There is no engine stage in this publish config, storing as publish result.
			pathext = "zip";
		}
		
		// Use preprocess options.
		ReleaseExportOptions exportOptions = new ReleaseExportOptions(preprocess.getParams()); 
		ReleaseExportService exportService = this.exportServices.get(itemId.getRepository());

		CmsExportJob job = PublishExportJobFactory.getExportJobZip(storage, pathext);
		exportService.exportRelease(itemId, exportOptions, job);
		
		job.prepare();

		logger.debug("Preparing writer for export...");
		CmsExportWriter exportWriter = this.exportProvider.getWriter();
		exportWriter.prepare(job);
		logger.debug("Writer is prepared. Writing job to S3.");
		exportWriter.write();
		logger.debug("Jobs manifest has been exported to S3 at path: {}", job.getJobPath());
	}

}
