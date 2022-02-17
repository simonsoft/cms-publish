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

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.command.CommandRuntimeException;
import se.simonsoft.cms.item.command.ExternalCommandHandler;
import se.simonsoft.cms.item.export.CmsExportItem;
import se.simonsoft.cms.item.export.CmsExportJobSingle;
import se.simonsoft.cms.item.export.CmsExportProvider;
import se.simonsoft.cms.item.export.CmsExportWriter;
import se.simonsoft.cms.item.workflow.WorkflowExecutionId;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobManifest;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobOptions;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobProgress;
import se.simonsoft.cms.publish.config.export.PublishExportJobFactory;
import se.simonsoft.cms.publish.config.export.PublishJobResultLookup;
import se.simonsoft.cms.publish.config.manifest.CmsExportItemPublishManifest;
import se.simonsoft.cms.publish.config.manifest.CmsExportItemPublishManifestVelocity;

import com.fasterxml.jackson.databind.ObjectWriter;

public class PublishManifestExportCommandHandler implements ExternalCommandHandler<PublishJobOptions>{


	private static final Logger logger = LoggerFactory.getLogger(PublishManifestExportCommandHandler.class);
	private final CmsExportProvider exportProvider;
	private final ObjectWriter writerPublishManifest;
	private final PublishJobResultLookup resultLookup; // TODO: Inject in a future major release.
	

	@Inject
	public PublishManifestExportCommandHandler(
			@Named("config:se.simonsoft.cms.publish.export") CmsExportProvider exportProvider,
			ObjectWriter objectWriter
			) {
		
		this.exportProvider = exportProvider;
		this.writerPublishManifest = objectWriter;
		
		this.resultLookup = new PublishJobResultLookup(exportProvider);
	}

	@Override
	public String handleExternalCommand(CmsItemId itemId, PublishJobOptions options) {
		logger.debug("Requesting export of PublishJob manifest.");
		
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
		
		if (!resultLookup.isPublishResultExists(itemId, options)) {
			logger.warn("Abort manifest export, publish result does not exist: " + itemId);
			throw new CommandRuntimeException("PublishResultMissing");
		}
		// Augment the manifest with UUID of the workflow execution, not available before starting the workflow.
		doInsertJobId(manifest, progress);
		
		logger.trace("Preparing publishJob manifest for export to S3: {}", manifest);

		CmsExportItem exportItem; 
		if (manifest.getTemplate() != null) {
			logger.debug("Manifest will be serialized with velocity");
			exportItem = new CmsExportItemPublishManifestVelocity(options);
		} else {
			exportItem = new CmsExportItemPublishManifest(writerPublishManifest, manifest);
		}
		
		CmsExportJobSingle job = PublishExportJobFactory.getExportJobSingle(options.getStorage(), manifest.getPathext());
		job.addExportItem(exportItem);
		job.prepare();

		logger.debug("Preparing writer for export...");
		CmsExportWriter exportWriter = this.exportProvider.getWriter();
		exportWriter.prepare(job);
		logger.debug("Writer is prepared. Writing job to S3.");
		exportWriter.write();
		logger.debug("Jobs manifest has been exported to S3 at path: {}", job.getJobPath());
		
		if (exportWriter instanceof CmsExportWriter.LocalFileSystem) {
			options.getProgress().getParams().put("manifest", ((CmsExportWriter.LocalFileSystem) exportWriter).getExportPath().toString());
		}
		
		return null;
	}
	
	/**
	 * Insert the Execution UUID as manifest.job.id before writing the manifest to file.
	 * @param manifest
	 * @param progress
	 */
	private void doInsertJobId(PublishJobManifest manifest, PublishJobProgress progress) {
		
		String executionId = progress.getParams().get("executionid");
		if (executionId == null || executionId.isBlank()) {
			throw new CommandRuntimeException("PublishExecutionIdMissing");
		}
		
		WorkflowExecutionId id = new WorkflowExecutionId(executionId);
		if (!id.hasUuid()) {
			throw new CommandRuntimeException("PublishExecutionIdMalformed", "no UUID detected");
		}
		manifest.getJob().put("id", id.getUuid());
	}
	
}
