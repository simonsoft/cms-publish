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

	private static final Logger logger = LoggerFactory.getLogger(PublishPreprocessCommandHandler.class);

	@Inject
	public PublishPreprocessCommandHandler(
			@Named("config:se.simonsoft.cms.publish.export") CmsExportProvider exportProvider, 
			Map<CmsRepository, ReleaseExportService> exportServices
			) {

		this.exportProvider = exportProvider;
		this.exportServices = exportServices;
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
		} else if (preprocess.getType().startsWith("webapp-export")) {
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
		
		// Use preprocess options to populate the Export Options.
		ReleaseExportOptions exportOptions = new ReleaseExportOptions(preprocess.getParams());
		// Set default values for known export types (unless already defined);
		String type = preprocess.getType(); // Multiple preprocess types (subtypes) get routed here.
		if ("webapp-export-abxpe".equals(type)) {
			setExportOptionsDefaultAbxpe(exportOptions);
		} else if ("webapp-export-ditaot".equals(type)) {
			setExportOptionsDefaultDitaot(exportOptions);
		} 
		
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

	private void setExportOptionsDefaultAbxpe(ReleaseExportOptions options) {
		// Set a name that is unlikely to collide with sections.
		if (options.getDocumentNameBase() == null) {
			options.setDocumentNameBase("_document");
		}
		// Always use .xml, for both book and ditamap. Avoids logic in worker-webapp.
		if (options.getDocumentExtension() == null) {
			options.setDocumentExtension("xml");
		}
		// Split ditabase into topics, likely faster to process in PE.
		// The XSL default is now to split, can be overridden here if faster to process in PE (including export, unzip and clean-up).
		// Split ditabase is likely better from a support-perspective if sending data to PTC.
	}
	
	private void setExportOptionsDefaultDitaot(ReleaseExportOptions options) {
		// DITA-OT container expects site.ditamap, no risk for collision with topics due to different extensions.
		// Risk for collision with sub-maps / keydef-maps?
		if (options.getDocumentNameBase() == null) {
			options.setDocumentNameBase("site");
		}
		// Always use .ditamap, for both book and ditamap. Book needs XSL transform into DITA.
		if (options.getDocumentExtension() == null) {
			options.setDocumentExtension("ditamap");
		}
		// Split ditabase into topics, required by DITA-OT.
		// The XSL default is now to split.
		
		// Suppress all cms-namespace attributes, not allowed by DITA-OT. Revision metadata must be set in other attributes.
		if (options.getCmsAttributesSuppressAll() == null) {
			options.setCmsAttributesSuppressAll(true);
		}
	}

}
