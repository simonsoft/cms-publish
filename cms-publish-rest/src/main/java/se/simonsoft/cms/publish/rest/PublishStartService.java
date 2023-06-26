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

import com.fasterxml.jackson.databind.ObjectReader;

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

	private final PublishWebhookCommandHandler publishWebhookCommandHandler;
	private final CmsItemLookupReporting lookupReporting;
	private final TranslationTracking translationTracking;
	private final PublishConfiguration publishConfiguration;
	private final PublishExecutor publishExecutor;
	private final PublishJobFactory jobFactory;
	ObjectReader profilingSetReader;

	private static final Logger logger = LoggerFactory.getLogger(PublishStartService.class);

	@Inject
	public PublishStartService(
			PublishWebhookCommandHandler publishWebhookCommandHandler,
			CmsItemLookupReporting lookupReporting,
			TranslationTracking translationTracking,
			PublishConfiguration publishConfiguration,
			PublishExecutor publishExecutor,
			PublishJobFactory jobFactory,
			ObjectReader reader) {

		this.publishWebhookCommandHandler = publishWebhookCommandHandler;
		this.lookupReporting = lookupReporting;
		this.translationTracking = translationTracking;
		this.publishConfiguration = publishConfiguration;
		this.publishExecutor = publishExecutor;
		this.jobFactory = jobFactory;
		this.profilingSetReader = reader.forType(ProfilingSet.class);
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

		PublishJobOptions publishJobOptions = publishJob.getOptions();
		PublishJobDelivery delivery = publishJobOptions.getDelivery();
		PublishJobStorage storage = publishJobOptions.getStorage();
		Optional<PublishJobProgress> progress = Optional.of(publishJobOptions.getProgress());

		if (storage == null) {
			throw new IllegalArgumentException("Need a valid PublishJobStorage object with params archive and manifest.");
		}

		logger.debug("WebhookCommandHandler endpoint: {}", delivery.getParams().get("url"));
		LinkedHashMap<String, String> result = publishWebhookCommandHandler.getPostPayload(delivery, storage, progress);

		return result;
	}

	private PublishConfig getPublishConfiguration(CmsItemId itemId, String name) {

		Map<String, PublishConfig> configs = publishConfiguration.getConfiguration(itemId);
		return configs.get(name);
	}

	private PublishJob getPublishJob(CmsItemId itemId, PublishStartOptions options, PublishConfig config) {

		CmsItem itemRelease = this.lookupReporting.getItem(itemId);

		CmsItemPublish itemPublish;
		String locale = options.getLocale();
		if (locale != null && !locale.isBlank() && !(new CmsItemPublish(itemRelease)).getReleaseLocale().equals(locale)) {
			CmsItemTranslation translation = null;
			List<CmsItemTranslation> translations = translationTracking.getTranslations(itemId.withPegRev(itemRelease.getRevisionChanged().getNumber()));
			// Find the translation and create itemPublish, throw exception if no translation exists.
			for (CmsItemTranslation item: translations) {
				if (item.getLocale().toString().equals(locale)) {
					translation = item;
					break;
				}
			}
			if (translation != null) {
				itemPublish = new CmsItemPublish(translation.getItem());
			} else {
				throw new IllegalArgumentException("Unable to find a translation for the intended locale.");
			}
		} else {
			itemPublish = new CmsItemPublish(itemRelease);
		}

		// TODO: Should we validate that publishconfig actually applies to itemPublish (e.g. does the Translation have the correct status).

		TranslationLocalesMapping localesRfc = (TranslationLocalesMapping) this.publishConfiguration.getTranslationLocalesMapping(itemPublish);
		PublishProfilingRecipe profilingRecipe = null;
		// Use profilingname if set (get recipe from itemPublish)
		if (itemPublish.hasProfiles() && options.getProfilingname() != null && options.getProfilingname().length() > 0) {
			String profilingValue = itemPublish.getProperties().getString(ReleaseProperties.PROPNAME_PROFILING);
			PublishProfilingSet profilingSet = ProfilingSet.getProfilingSet(profilingValue, profilingSetReader);
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
		if (options.getStartpathname() != null || options.getStartprofiling() != null || options.getStartcustom().size() > 0) {
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
