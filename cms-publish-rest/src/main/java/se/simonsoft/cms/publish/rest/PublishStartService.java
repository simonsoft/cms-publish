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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;

import com.fasterxml.jackson.databind.ObjectWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.command.CommandRuntimeException;
import se.simonsoft.cms.publish.config.PublishConfiguration;
import se.simonsoft.cms.publish.config.PublishExecutor;
import se.simonsoft.cms.publish.config.command.PublishWebhookCommandHandler;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.job.*;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingRecipe;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingSet;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;
import se.simonsoft.cms.release.ProfilingSet;
import se.simonsoft.cms.release.ReleaseProperties;
import se.simonsoft.cms.release.translation.CmsItemTranslation;
import se.simonsoft.cms.release.translation.TranslationLocalesMapping;
import se.simonsoft.cms.release.translation.TranslationTracking;
import se.simonsoft.cms.reporting.CmsItemLookupReporting;

public class PublishStartService {

	private static final int MAX_START_PATH_NAME_SIZE = 100;
	private static final int MAX_START_CUSTOM_NAME_SIZE = 100 * 1024;
	private static final int MAX_START_PROFILING_SIZE = 100 * 1024;
	private final PublishWebhookCommandHandler publishWebhookCommandHandler;
	private final CmsItemLookupReporting lookupReporting;
	private final TranslationTracking translationTracking;
	private final PublishConfiguration publishConfiguration;
	private final PublishExecutor publishExecutor;
	private final PublishJobFactory jobFactory;
	private final ObjectReader profilingSetReader;
	private final ObjectWriter writer;

	private static final Logger logger = LoggerFactory.getLogger(PublishStartService.class);

	@Inject
	public PublishStartService(
			PublishWebhookCommandHandler publishWebhookCommandHandler,
			CmsItemLookupReporting lookupReporting,
			TranslationTracking translationTracking,
			PublishConfiguration publishConfiguration,
			PublishExecutor publishExecutor,
			PublishJobFactory jobFactory,
			ObjectReader reader,
			ObjectWriter writer) {

		this.publishWebhookCommandHandler = publishWebhookCommandHandler;
		this.lookupReporting = lookupReporting;
		this.translationTracking = translationTracking;
		this.publishConfiguration = publishConfiguration;
		this.publishExecutor = publishExecutor;
		this.jobFactory = jobFactory;
		this.profilingSetReader = reader.forType(PublishProfilingSet.class);
		this.writer = writer;
	}


	public LinkedHashMap<String, String> doPublishStartItem(CmsItemId itemId, PublishStartOptions options) {

		if (options.getStartpathname() != null && options.getStartpathname().getBytes().length > MAX_START_PATH_NAME_SIZE) {
			throw new IllegalArgumentException(String.format("The startpathname size exceeds the %d bytes limit.", MAX_START_PATH_NAME_SIZE));
		}

		if (options.getStartcustom() != null) {
			try {
				String jsonString = writer.writeValueAsString(options.getStartcustom());
				int jsonStringLength = jsonString.getBytes().length;
				if (jsonStringLength > MAX_START_CUSTOM_NAME_SIZE) {
					throw new IllegalArgumentException(String.format("The startcustom size exceeds the %d bytes limit.", MAX_START_CUSTOM_NAME_SIZE));
				}
			} catch (JsonProcessingException e) {
				throw new IllegalArgumentException("Failed to serialize the startcustom.", e);
			}
		}

		if (options.getStartprofiling() != null) {
			try {
				String jsonString = writer.writeValueAsString(options.getStartprofiling());
				int jsonStringLength = jsonString.getBytes().length;
				if (jsonStringLength > MAX_START_PROFILING_SIZE) {
					throw new IllegalArgumentException(String.format("The startprofiling size exceeds the %d bytes limit.", MAX_START_PROFILING_SIZE));
				}
			} catch (JsonProcessingException e) {
				throw new IllegalArgumentException("Failed to serialize the startprofiling.", e);
			}
		}

		PublishConfig config = getPublishConfiguration(itemId, options.getPublication());
		PublishJob publishJob = getPublishJob(itemId, options, config);

		Set<String> uuids = publishExecutor.startPublishJobs(new LinkedHashSet<PublishJob>(Arrays.asList(publishJob)));

		// Verify that one job was started, log the UUID.
		uuids.forEach(uuid -> logger.info("Publish job started: {}", uuid));
		if (uuids.size() != 1) {
			throw new CommandRuntimeException("StartPublishJobError", String.format("None or several publish jobs started: %d", uuids.size()));
		}

		PublishJobOptions publishJobOptions = publishJob.getOptions();
		PublishJobDelivery delivery = publishJobOptions.getDelivery();
		PublishJobStorage storage = publishJobOptions.getStorage();
		Optional<PublishJobProgress> progress = Optional.of(publishJobOptions.getProgress());

		if (storage == null) {
			throw new IllegalArgumentException("Need a valid PublishJobStorage object with params archive and manifest.");
		}

		logger.debug("WebhookCommandHandler endpoint: {}", delivery.getParams().get("url"));
		LinkedHashMap<String, String> result = publishWebhookCommandHandler.getPostPayload(delivery, storage, progress);
		
		// #1770: Add the job UUID to the API response to enable a status query API?
		// TODO: Consider a webhook call for failure.
		
		return result;
	}

	private PublishConfig getPublishConfiguration(CmsItemId itemId, String name) {

		Map<String, PublishConfig> configs = publishConfiguration.getConfiguration(itemId);
		if (!configs.containsKey(name)) {
			throw new IllegalArgumentException("Publish config does not exist: " + name);
		}
		return configs.get(name);
	}
	
	private boolean isPublishConfigurationValid(CmsItemPublish item, String name) {

		Map<String, PublishConfig> configs = publishConfiguration.getConfigurationFiltered(item);
		return configs.containsKey(name);
	}

	private PublishJob getPublishJob(CmsItemId itemId, PublishStartOptions options, PublishConfig config) {
		if (config == null) {
			throw new IllegalArgumentException("PublishConfig must not be null.");
		}
		if (itemId.getPegRev() != null) {
			throw new IllegalArgumentException("ItemId must not specify a revision.");
		}

		CmsItem itemReleaseHead = this.lookupReporting.getItem(itemId);
		CmsItemId itemIdRev = itemId.withPegRev(itemReleaseHead.getRevisionChanged().getNumber());

		CmsItemPublish itemPublish;
		String locale = options.getLocale();
		if (locale != null && !locale.isBlank() && !(new CmsItemPublish(itemReleaseHead)).getReleaseLocale().equals(locale)) {
			CmsItemTranslation translation = null;
			List<CmsItemTranslation> translations = translationTracking.getTranslations(itemIdRev);
			// Find the translation and create itemPublish, throw exception if no translation exists.
			for (CmsItemTranslation item: translations) {
				if (item.getLocale().toString().equals(locale)) {
					translation = item;
					break;
				}
			}
			if (translation != null) {
				// The translation item is "head" while the translationId has revision.
				CmsItem itemTranslation = this.lookupReporting.getItem(translation.getTranslation());
				itemPublish = new CmsItemPublish(itemTranslation);
			} else {
				throw new IllegalArgumentException("Unable to find a translation for the intended locale.");
			}
		} else {
			CmsItem itemRelease = this.lookupReporting.getItem(itemIdRev);
			itemPublish = new CmsItemPublish(itemRelease);
		}

		// Validate that publishconfig actually applies to itemPublish (e.g. does the Translation have the correct status).
		if (!isPublishConfigurationValid(itemPublish, options.getPublication())) {
			String msg = String.format("Publish start config '%s' not valid for item: %s", options.getPublication(), itemPublish.getId());
			logger.info(msg);
			throw new IllegalArgumentException(msg);
		}

		TranslationLocalesMapping localesRfc = (TranslationLocalesMapping) this.publishConfiguration.getTranslationLocalesMapping(itemPublish);
		PublishProfilingRecipe profilingRecipe = null;
		// Use profilingname if set (get recipe from itemPublish)
		if (itemPublish.hasProfiles() && options.getProfilingname() != null && options.getProfilingname().length() > 0) {
			// Get recipe from the document properties.
			String profilingValue = itemPublish.getProperties().getString(ReleaseProperties.PROPNAME_PROFILING);
			// Use .getProfilingSetPublish() in order to only consider recipes intended for publish.
			PublishProfilingSet profilingSet = ProfilingSet.getProfilingSet(profilingValue, profilingSetReader).getProfilingSetPublish();
			profilingRecipe = profilingSet.get(options.getProfilingname());
			if (profilingRecipe == null) {
				throw new IllegalArgumentException("Predefined profiling recipe must be defined in document properties: " + options.getProfilingname());
			}
		} else if (options.getStartprofiling() != null) { // Use startprofiling if set (must not contain 'name' or 'logicalexpr')
			profilingRecipe = options.getStartprofiling();
			if (profilingRecipe.getName() != null || profilingRecipe.getLogicalExpr() != null) {
				throw new IllegalArgumentException("Dynamic profiling recipes must not contain 'name' or 'logicalexpr'");
			}
		} else {
			// No profiling.
		}

		// Verify that the config is intended for profiling, if profilingRecipe != null.
		if (profilingRecipe != null && !profilingRecipe.getAttributesFilter().isEmpty() && (config.getProfilingInclude() != null && !config.getProfilingInclude())) {
			throw new IllegalArgumentException("Requested profiling is not properly configured");
		}

		if (options.getExecutionid() == null) {
			throw new RuntimeException("No 'executionid' was assigned");
		}

		// Need specific scenariotest setting 'startcustom' but not 'startprofiling'.
		// Set profilingRecipe.name to executionid if any of the start* parameters are provided (even if profilingRecipe == null).
		if (options.getStartpathname() != null || options.getStartprofiling() != null || (options.getStartcustom() != null && options.getStartcustom().size() > 0)) {
			if (profilingRecipe == null) {
				profilingRecipe = new PublishProfilingRecipe(options.getExecutionid(), new HashMap<>());
			} else {
				profilingRecipe.setAttribute("name", options.getExecutionid());
			}
		}
		if (options.getStartprofiling() != null) {
			// Ensure invalid profiling filter is reported before starting the process.
			profilingRecipe.validateFilter();
		}

		PublishJob job = jobFactory.getPublishJob(itemPublish, config, options.getPublication(), profilingRecipe, localesRfc, Optional.ofNullable(options.getStartpathname()), Optional.ofNullable(options.getStartcustom()));

		PublishJobManifest manifest = job.getOptions().getManifest();
		if (options.getStartcustom() != null) {
			LinkedHashMap<String, String> custom = (manifest.getCustom() != null) ? manifest.getCustom() : new LinkedHashMap<>();
			custom.putAll(options.getStartcustom());
			manifest.setCustom(custom);
		}
		job.getOptions().setManifest(manifest);

		return job;
	}

}
