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
package se.simonsoft.cms.publish.rest;

import java.util.*;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.command.CommandRuntimeException;
import se.simonsoft.cms.item.command.ExternalCommandHandler;
import se.simonsoft.cms.publish.config.PublishConfiguration;
import se.simonsoft.cms.publish.config.PublishExecutor;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.job.PublishJob;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobManifest;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingRecipe;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingSet;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;
import se.simonsoft.cms.publish.rest.PublishJobFactory;
import se.simonsoft.cms.publish.rest.PublishStartOptions;
import se.simonsoft.cms.release.ProfilingSet;
import se.simonsoft.cms.release.ReleaseProperties;
import se.simonsoft.cms.release.translation.TranslationLocalesMapping;
import se.simonsoft.cms.reporting.CmsItemLookupReporting;

public class PublishStartService {

	private final CmsItemLookupReporting lookupReporting;
	private final PublishConfiguration publishConfiguration;
	private final PublishExecutor publishExecutor;
	private final PublishJobFactory jobFactory;

	private static final Logger logger = LoggerFactory.getLogger(PublishStartService.class);

	@Inject
	public PublishStartService(
			CmsItemLookupReporting lookupReporting,
			PublishConfiguration publishConfiguration,
			PublishExecutor publishExecutor,
			PublishJobFactory jobFactory) {

		this.lookupReporting = lookupReporting;
		this.publishConfiguration = publishConfiguration;
		this.publishExecutor = publishExecutor;
		this.jobFactory = jobFactory;
	}


	public LinkedHashMap<String, String> doPublishStartItem(CmsItemId itemId, PublishStartOptions options) {

		PublishConfig config = getPublishConfiguration(itemId, options.getPublication());
		PublishJob publishJob = getPublishJob(itemId, options, config);

		Set<String> uuids = publishExecutor.startPublishJobs(new LinkedHashSet<PublishJob>(Arrays.asList(publishJob)));

		// Verify that one job was started, log the UUID.
		uuids.forEach(uuid -> logger.info("Publish job started: {}", uuid));
		if (uuids.size() != 1) {
			throw new CommandRuntimeException("StartPublishJobError", String.format("None or several publish jobs started: %d", uuids.size()));
		}

		LinkedHashMap<String, String> result = null; // TODO: Use PublishWebhookCommandHandler

		return result;
	}

	private PublishConfig getPublishConfiguration(CmsItemId itemId, String name) {

		Map<String, PublishConfig> configs = publishConfiguration.getConfiguration(itemId);
		return configs.get(name);
	}

	private PublishJob getPublishJob(CmsItemId itemId, PublishStartOptions options, PublishConfig config) {

		CmsItem item = this.lookupReporting.getItem(itemId);
		CmsItemPublish itemPublish = new CmsItemPublish(item);
		TranslationLocalesMapping localesRfc = (TranslationLocalesMapping) this.publishConfiguration.getTranslationLocalesMapping(itemPublish);

		PublishProfilingRecipe profilingRecipe = null;
		// Use profilingname if set (get recipe from itemPublish)
		if (itemPublish.hasProfiles() && options.getProfilingname() != null && options.getProfilingname().length() > 0) {
			ObjectMapper objectMapper = new ObjectMapper();
			ObjectReader profilingReader = objectMapper.readerFor(ProfilingSet.class);
			String profilingValue = item.getProperties().getString(ReleaseProperties.PROPNAME_PROFILING);
			PublishProfilingSet profilingSet = ProfilingSet.getProfilingSet(profilingValue, profilingReader);
			profilingRecipe = profilingSet.get(options.getProfilingname());
		} else if (options.getStartprofiling() != null) { // Use startprofiling if set (must not contain 'name' or 'logicalexpr')
			profilingRecipe = options.getStartprofiling();
			if (profilingRecipe.getName() != null || profilingRecipe.getLogicalExpr() != null) {
				throw new IllegalArgumentException("Dynamic profiling recipes must not contain 'name' or 'logicalexpr'");
			}
		} else {
			// no profiling.
			profilingRecipe = new PublishProfilingRecipe();
		}

		// Verify that the config is intended for profiling, if profilingRecipe != null.
		if (profilingRecipe != null && !config.getProfilingInclude()) {
			throw new IllegalArgumentException("Requested profiling is not properly configured");
		}

		if (options.getExecutionid() == null) {
			throw new RuntimeException("No 'executionid' was assigned");
		}

		// TODO: Need specific scenariotest setting 'startinput' but not 'startprofiling'.
		// Set profilingRecipe.name to executionid if any of the start* parameters are provided (even if profilingRecipe == null).
		if (options.getStartpathname() != null || options.getStartcustom().size() > 0) {
			profilingRecipe.setAttribute("name", options.getExecutionid());
		} else {
			// TODO: Figure out how to manage profilingRecipe.name when no profiling is used (method validateFilter() throws exception when no filter parameters are set).
		}

		PublishJob job = jobFactory.getPublishJob(itemPublish, config, options.getPublication(), profilingRecipe, localesRfc, Optional.ofNullable(options.getStartpathname()), Optional.ofNullable(options.getStartcustom()));

		PublishJobManifest manifest = job.getOptions().getManifest();
		LinkedHashMap<String, String> custom = manifest.getCustom();
		custom.putAll(options.getStartcustom());
		manifest.setCustom(custom);
		job.getOptions().setManifest(manifest);

		return job;
	}

}
