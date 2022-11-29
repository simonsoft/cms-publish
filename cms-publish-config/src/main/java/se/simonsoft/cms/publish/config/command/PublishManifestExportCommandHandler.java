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
package se.simonsoft.cms.publish.config.command;


import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.command.CommandRuntimeException;
import se.simonsoft.cms.item.command.ExternalCommandHandler;
import se.simonsoft.cms.item.export.CmsExportItem;
import se.simonsoft.cms.item.export.CmsExportJobSingle;
import se.simonsoft.cms.item.export.CmsExportProvider;
import se.simonsoft.cms.item.export.CmsExportWriter;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobManifest;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobOptions;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobProgress;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobStorage;
import se.simonsoft.cms.publish.config.export.PublishExportJobFactory;
import se.simonsoft.cms.publish.config.export.PublishJobResultLookup;
import se.simonsoft.cms.publish.config.manifest.CmsExportItemPublishManifest;
import se.simonsoft.cms.publish.config.manifest.CmsExportItemPublishManifestVelocity;

public class PublishManifestExportCommandHandler implements ExternalCommandHandler<PublishJobOptions>{


	private static final Logger logger = LoggerFactory.getLogger(PublishManifestExportCommandHandler.class);
	private final CmsExportProvider exportProvider;
	private final ObjectWriter writerPublishManifest;
	private final ObjectWriter writerJobProgress;
	private final PublishJobResultLookup resultLookup; // TODO: Inject in a future major release.
	

	@Inject
	public PublishManifestExportCommandHandler(
			@Named("config:se.simonsoft.cms.publish.export") CmsExportProvider exportProvider,
			ObjectWriter objectWriter
			) {
		
		this.exportProvider = exportProvider;
		this.writerPublishManifest = objectWriter.forType(PublishJobManifest.class);
		this.writerJobProgress = objectWriter.forType(PublishJobProgress.class);
		
		this.resultLookup = new PublishJobResultLookup(exportProvider);
	}

	@Override
	public Class<PublishJobOptions> getArgumentsClass() {
		return PublishJobOptions.class;
	}
	
	@Override
	public String handleExternalCommand(CmsItemId itemId, PublishJobOptions options) {
		logger.debug("Requesting export of PublishJob manifest.");
		String tagStep = "manifest";
		String tagCdn = ""; // Value length 0 is allowed.
		
		PublishJobProgress progress = options.getProgress();
		if (progress == null) {
			throw new IllegalArgumentException("Requires a valid PublishJobProgress object.");
		}
		
		PublishJobManifest manifest = options.getManifest();
		if (manifest == null) {
			throw new IllegalArgumentException("Requires a valid PublishJobManifest object.");
		}
		
		if (manifest.getPathext() == null) {
			throw new IllegalArgumentException("Requires a valid PublishJobManifest object with 'pathext' field. Indicates missing 'docno...' config.");
		}
		
		if (manifest.getCustom() != null && manifest.getCustom().containsKey("cdn")) {
			tagCdn = manifest.getCustom().get("cdn");
		}
		
		if (!resultLookup.isPublishResultExists(itemId, options)) {
			logger.warn("Abort manifest export, publish result does not exist: " + itemId);
			throw new CommandRuntimeException("PublishResultMissing");
		}
		
		// Check if preprocess has generated a custom manifest, should not be overwritten.
		boolean manifestCustom = resultLookup.isPublishResultExists(itemId, options, manifest.getPathext());
		
		logger.trace("Preparing publishJob manifest for export to S3: {}", manifest);

		CmsExportItem exportItem; 
		if (manifest.getTemplate() != null) {
			logger.debug("Manifest will be serialized with velocity");
			// TODO: Remove when supporting export of XSL-generated manifest.
			exportItem = new CmsExportItemPublishManifestVelocity(options);
		} else {
			exportItem = new CmsExportItemPublishManifest(writerPublishManifest, manifest);
		}
		
		// #1707 Always export the standard manifest as 'index'.
		doExportManifest(options.getStorage(), new CmsExportItemPublishManifest(writerPublishManifest, manifest), "cms-index", tagStep, tagCdn);
		
		if (manifestCustom) {
			// TODO: Support custom manifest for local FS. Probably a separate export command (delivery) for both zip and manifest.
			logger.info("Manifest export suppressed due to custom manifest already in place.");
		} else {
			CmsExportWriter exportWriter = doExportManifest(options.getStorage(), exportItem, manifest.getPathext(), tagStep, tagCdn);
			
			if (exportWriter instanceof CmsExportWriter.LocalFileSystem) {
				options.getProgress().getParams().put("manifest", ((CmsExportWriter.LocalFileSystem) exportWriter).getExportPath().toString());
			}
		}
		
		
		try {
			return writerJobProgress.writeValueAsString(progress);
		} catch (JsonProcessingException e) {
			throw new CommandRuntimeException("JsonProcessingException", e);
		}
	}
	
	private CmsExportWriter doExportManifest(PublishJobStorage storage, CmsExportItem exportItem, String pathext, String tagStep, String tagCdn) {
		
		CmsExportJobSingle job = PublishExportJobFactory.getExportJobSingle(storage, pathext);
		job.addExportItem(exportItem);
		job.withTagging("PublishStep", tagStep);
		job.withTagging("PublishCdn", tagCdn);
		job.prepare();

		logger.debug("Preparing writer for export...");
		CmsExportWriter exportWriter = this.exportProvider.getWriter();
		exportWriter.prepare(job);
		logger.debug("Writer is prepared. Writing job to S3.");
		exportWriter.write();
		logger.debug("Jobs manifest has been exported to S3 at path: {}", job.getJobPath());
		return exportWriter;
	}
	
}
