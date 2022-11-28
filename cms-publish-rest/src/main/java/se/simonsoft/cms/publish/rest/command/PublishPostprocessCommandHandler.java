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

import java.util.Arrays;
import java.util.LinkedHashSet;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectWriter;

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.command.ExternalCommandHandler;
import se.simonsoft.cms.item.export.CmsExportProvider;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobOptions;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobPostProcess;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobStorage;
import se.simonsoft.cms.publish.config.export.PublishJobResultLookup;
import se.simonsoft.cms.publish.rest.PublishPackageOptions;

public class PublishPostprocessCommandHandler implements ExternalCommandHandler<PublishJobOptions> {

	private final CmsExportProvider exportProvider;
	@SuppressWarnings("unused")
	private final PublishJobResultLookup resultLookup;
	private final PublishPackageCommandHandler packageCommandHandler;
	

	private static final Logger logger = LoggerFactory.getLogger(PublishPostprocessCommandHandler.class);

	@Inject
	public PublishPostprocessCommandHandler(
			@Named("config:se.simonsoft.cms.publish.export") CmsExportProvider exportProvider,
			PublishJobResultLookup resultLookup,
			PublishPackageCommandHandler packageCommandHandler,
			ObjectWriter objectWriter
			) {

		this.exportProvider = exportProvider;
		this.resultLookup = resultLookup;
		this.packageCommandHandler = packageCommandHandler;
	}
	
	@Override
	public Class<PublishJobOptions> getArgumentsClass() {
		return PublishJobOptions.class;
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
		} else if (postprocess.getType().equals("webapp-package")) {
			logger.debug("Performing webapp-package as Postprocess: {}", itemId);
			doWebappPackage(itemId, options);
		} else {
			throw new IllegalArgumentException("Unsupported Postprocess type: " + postprocess.getType());
		}

		// NOTE: It is tricky to attempt any modification of large publish results (zip) in Java Webapp.
		// It must be fully streamed which makes it difficult to peek into which files are in the zip.
		// Such tasks are safer to attempt in a Lambda with multiple GB RAM for a short time.
		return null;
	}

	
	private void doWebappPackage(CmsItemId itemId, PublishJobOptions options) {
		final PublishJobPostProcess postprocess = options.getPostprocess();

		// There will be no PublishResult from earlier steps.
		// The Zip service likely checks existance before starting the actual streaming.
		/*		
		if (!resultLookup.isPublishResultExists(itemId, options)) {
			logger.warn("Abort postprocess, publish result does not exist: " + itemId);
			throw new CommandRuntimeException("PublishResultMissing");
		}
		*/
	
		PublishPackageOptions packageOptions = new PublishPackageOptions();
		packageOptions.setPublication(postprocess.getParams().get("configname"));
		// Probably better to add locale filter to config / package rather than configurable includeRelease / includeTranslation.
		packageOptions.setIncludeRelease(true);
		packageOptions.setIncludeTranslations(true);
		// This execution applies to a single profiles recipe, so the package can only have that single profiles recipe.
		// Packaging multiple profiles into the same zip requires a different mechanism.
		if (options.getProfiling() != null) {
			packageOptions.setProfiling(new LinkedHashSet<String>(Arrays.asList(options.getProfiling().getName())));
		}
		
		packageCommandHandler.handlePackageZip(itemId, packageOptions, options.getStorage(), this.exportProvider);
	}

	

}
