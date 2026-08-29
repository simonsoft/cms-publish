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


import java.time.Duration;
import java.time.Instant;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.command.CommandRuntimeException;
import se.simonsoft.cms.item.command.ExternalCommandHandler;
import se.simonsoft.cms.item.export.CmsExportProvider;
import se.simonsoft.cms.item.workflow.WorkflowExecutionId;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobManifest;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobOptions;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobProgress;

public class PublishManifestJobidCommandHandler implements ExternalCommandHandler<PublishJobOptions>{


	private static final Logger logger = LoggerFactory.getLogger(PublishManifestJobidCommandHandler.class);
	private final ObjectWriter writerPublishManifest;
	

	@Inject
	public PublishManifestJobidCommandHandler(
			@Named("config:se.simonsoft.cms.publish.export") CmsExportProvider exportProvider,
			ObjectWriter objectWriter
			) {
		
		this.writerPublishManifest = objectWriter.forType(PublishJobManifest.class);
	}
	
	@Override
	public Class<PublishJobOptions> getArgumentsClass() {
		return PublishJobOptions.class;
	}

	@Override
	public String handleExternalCommand(CmsItemId itemId, PublishJobOptions options) {
		logger.debug("Requesting augment jobid in PublishJob manifest.");
		
		PublishJobProgress progress = options.getProgress();
		if (progress == null) {
			throw new IllegalArgumentException("Requires a valid PublishJobProgress object.");
		}
		
		PublishJobManifest manifest = options.getManifest();
		if (manifest == null) {
			throw new IllegalArgumentException("Requires a valid PublishJobManifest object.");
		}
		
		// Augment the manifest with UUID of the workflow execution, not available before starting the workflow.
		doInsertJobId(manifest, progress);
		// Augment the manifest with the true StartTime of the execution.
		doInsertJobStart(manifest, progress);
		// Augment the manifest with the calculated expiration, when configured.
		doInsertJobExpiration(manifest, options);

		// Consider deleting manifests (json / index) for restarted jobs.
		
		try {
			return writerPublishManifest.writeValueAsString(manifest);
		} catch (JsonProcessingException e) {
			throw new CommandRuntimeException("JsonProcessingException", e);
		}
	}
	
	/**
	 * Insert the Execution UUID as manifest.job.id.
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
	
	/**
	 * Insert the Execution StartTime as manifest.job.start.
	 * @param manifest
	 * @param progress
	 */
	private void doInsertJobStart(PublishJobManifest manifest, PublishJobProgress progress) {
		
		String start = progress.getParams().get("executionstart");
		if (start == null || start.isBlank()) {
			throw new CommandRuntimeException("PublishExecutionStartMissing");
		}
		
		manifest.getJob().put("start", start);
	}

	/**
	 * Insert the calculated expiration as manifest.job.expiration, when delivery.params.expiration is configured.
	 * Same data type as manifest.job.start: an ISO-8601 dateTime string.
	 * @param manifest
	 * @param options
	 */
	private void doInsertJobExpiration(PublishJobManifest manifest, PublishJobOptions options) {

		if (options.getDelivery() == null) {
			return;
		}
		String expirationDays = options.getDelivery().getParams().get("expiration");
		if (expirationDays == null) {
			return;
		}

		String start = manifest.getJob().get("start");
		Instant expiration = Instant.parse(start).plus(Duration.ofDays(Long.parseLong(expirationDays)));
		manifest.getJob().put("expiration", expiration.toString());
	}

}
