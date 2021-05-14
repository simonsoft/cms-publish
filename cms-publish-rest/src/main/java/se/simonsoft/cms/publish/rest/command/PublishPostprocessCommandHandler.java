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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectWriter;

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.command.CommandRuntimeException;
import se.simonsoft.cms.item.command.ExternalCommandHandler;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobOptions;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobPostProcess;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobStorage;
import se.simonsoft.cms.publish.config.export.PublishJobResultLookup;
import se.simonsoft.cms.release.export.ReleaseExportService;

public class PublishPostprocessCommandHandler implements ExternalCommandHandler<PublishJobOptions> {

	//private final CmsExportProvider exportProvider;
	private final PublishJobResultLookup resultLookup;
	@SuppressWarnings("unused")
	private final Map<CmsRepository, ReleaseExportService> exportServices;

	private static final Logger logger = LoggerFactory.getLogger(PublishPostprocessCommandHandler.class);

	@Inject
	public PublishPostprocessCommandHandler(
			//@Named("config:se.simonsoft.cms.publish.export") CmsExportProvider exportProvider,
			PublishJobResultLookup resultLookup,
			Map<CmsRepository, ReleaseExportService> exportServices,
			ObjectWriter objectWriter
			) {

		//this.exportProvider = exportProvider;
		this.resultLookup = resultLookup;
		this.exportServices = exportServices;
	}

	@Override
	public String handleExternalCommand(CmsItemId itemId, PublishJobOptions options) {

		final PublishJobPostProcess postprocess = options.getPostprocess();
		if (postprocess == null) {
			throw new IllegalArgumentException("Need a valid PublishJobPostProcess object.");
		}

		final PublishJobStorage storage = options.getStorage();
		if (storage == null) {
			throw new IllegalArgumentException("Need a valid PublishJobStorage object.");
		}

		if (postprocess.getType() == null) {
			throw new IllegalArgumentException("Need a valid PublishJobPostProcess object with type attribute.");
		} else if (postprocess.getType().equals("webapp")) {
			logger.debug("Performing Webapp Postprocess: {}", itemId);
			doWebappPostprocess(itemId, options);
		} else {
			throw new IllegalArgumentException("Unsupported Postprocess type: " + postprocess.getType());
		}

		return null;
	}

	
	private void doWebappPostprocess(CmsItemId itemId, PublishJobOptions options) {
		
		if (!resultLookup.isPublishResultExists(itemId, options)) {
			logger.warn("Abort postprocess, publish result does not exist: " + itemId);
			throw new CommandRuntimeException("PublishResultMissing");
		}
		
		// NOTE: It is tricky to attempt any modification of large publish results (zip) in Java Webapp.
		// It must be fully streamed which makes it difficult to peek into which files are in the zip.
		// Such tasks are safer to attempt in a Lambda with multiple GB RAM for a short time.
		logger.info("Webapp Postprocess is currently a noop.");
	}


}
