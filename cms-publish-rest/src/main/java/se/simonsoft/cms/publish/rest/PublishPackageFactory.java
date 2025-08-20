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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingRecipe;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;
import se.simonsoft.cms.release.ReleaseLabel;
import se.simonsoft.cms.release.translation.CmsItemTranslation;
import se.simonsoft.cms.release.translation.TranslationTracking;
import se.simonsoft.cms.reporting.CmsItemLookupReporting;

public class PublishPackageFactory {
	
	private final CmsItemLookupReporting lookupReporting;
	private final TranslationTracking translationTracking;
	private final PublishConfigurationDefault publishConfiguration;
	
	private static final Logger logger =  LoggerFactory.getLogger(PublishPackageFactory.class); 
	
	@Inject
	public PublishPackageFactory(
			CmsItemLookupReporting lookupReporting,
			TranslationTracking translationTracking,
			PublishConfigurationDefault publishConfiguration
			) {
		
		this.lookupReporting = lookupReporting;
		this.translationTracking = translationTracking;
		this.publishConfiguration = publishConfiguration;
	}
	

	public PublishPackage getPublishPackage(CmsItemId itemId, PublishPackageOptions options) {
		return getPublishPackage(itemId, options.getPublication(), options.isIncludeRelease(), options.isIncludeTranslations(), options.getProfiling());
	}
	
	public PublishPackage getPublishPackage(CmsItemId itemId, String publication, boolean includeRelease, boolean includeTranslations, LinkedHashSet<String> profiling) {
		if (itemId == null) {
			throw new IllegalArgumentException("Field 'item': required");
		}

		if (itemId.getPegRev() == null) {
			throw new IllegalArgumentException("Field 'item': revision is required");
		}

		if (publication == null || publication.isEmpty()) {
			throw new IllegalArgumentException("Field 'publication': publication name is required");
		}

		if (!includeRelease && !includeTranslations) {
			throw new IllegalArgumentException("Field 'includerelease': must be selected if 'includetranslations' is disabled");
		}

		if (profiling == null) {
			// Cleaner code below if normalizing on non-null.
			profiling = new LinkedHashSet<>();
		}
		
		final List<CmsItemPublish> items = new ArrayList<CmsItemPublish>();

		CmsItem releaseItem = lookupReporting.getItem(itemId);

		// The Release config will be guiding regardless if the Release is included or not. The Translation config must be equivalent if separately specified.
		Map<String, PublishConfig> configurationsRelease = publishConfiguration.getConfiguration(releaseItem.getId());
		final PublishConfig publishConfig = configurationsRelease.get(publication);

		if (publishConfig == null) {
			throw new IllegalArgumentException("Publish Configuration must be defined for the Release: " + publication);
		}

		if (includeRelease) {
			items.add(new CmsItemPublish(releaseItem));
		}

		if (includeTranslations) {
			List<CmsItem> translationItems = getTranslationItems(itemId, publishConfig);
			if (translationItems.isEmpty()) {
				// NOTE: Adjust message if the filtering in getTranslationItems does additional aspects in the future, could add "etc" after "status". 
				logger.info("Translations requested, no translations found matching the configured status.");
				// #1580 No longer throwing exception, the UI is now much clearer and exception is incompatible with PublishPackageCommandHandler.
				//throw new IllegalArgumentException("Translations requested, no translations found matching the configured status.");
			}
			translationItems.forEach(translationItem -> items.add(new CmsItemPublish(translationItem)));
		}

		final LinkedHashSet<CmsItemPublish> publishedItems = new LinkedHashSet<>();
		for (CmsItemPublish item : items) {
			Map<String, PublishConfig> configurationFiltered = publishConfiguration.getConfigurationFiltered(item);
			if (configurationFiltered.containsKey(publication)) {
				publishedItems.add(item);
			} else {
				String msg = MessageFormatter.format("Field 'publication': publication name '{}' not defined for item {}.", publication, item.getId()).getMessage();
				throw new IllegalArgumentException(msg);
			}
		}

		// Profiling:
		// Filtering above takes care of mismatch btw includeFiltering and whether item has Profiling.
		// The 'profiling' parameter should only be allowed if configuration has includeProfiling = true, required then.
		if (Boolean.TRUE.equals(publishConfig.getProfilingInclude())) {
			if (profiling.size() == 0) {
				String msg = MessageFormatter.format("Field 'profiling': required for publication name '{}'.", publication).getMessage();
				throw new IllegalArgumentException(msg);
			}
		} else {
			if (profiling.size() > 0) {
				String msg = MessageFormatter.format("Field 'profiling': not allowed for publication name '{}'.", publication).getMessage();
				throw new IllegalArgumentException(msg);
			}
		}

		final Set<PublishProfilingRecipe> profilingSet;
		if (profiling.size() > 0) {
			profilingSet = getProfilingSetSelected(new CmsItemPublish(releaseItem), publishConfig, profiling);
		} else {
			profilingSet = null;
		}

		ReleaseLabel releaseLabel = new ReleaseLabel(new CmsItemPublish(releaseItem).getReleaseLabel());
		return new PublishPackage(publication, publishConfig, profilingSet, publishedItems, releaseItem.getId(), releaseLabel);
	}

	// Order will not be preserved, should not matter for packaging the publications.
	private Set<PublishProfilingRecipe> getProfilingSetSelected(CmsItemPublish item, PublishConfig publishConfig, LinkedHashSet<String> profiling) {

		Map<String, PublishProfilingRecipe> profilingAll = this.publishConfiguration.getItemProfilingSet(item).getMap();
		HashSet<PublishProfilingRecipe> profilingSet = new HashSet<PublishProfilingRecipe>(profilingAll.size());
		List<String> profilingInclude = publishConfig.getProfilingNameInclude();

		for (String name : profiling) {
			if (name.trim().isEmpty()) {
				throw new IllegalArgumentException("Field 'profiling': empty value");
			}

			if (!profilingAll.containsKey(name)) {
				String msg = MessageFormatter.format("Field 'profiling': name '{}' not defined on item.", name).getMessage();
				throw new IllegalArgumentException(msg);
			}

			if (profilingInclude != null && !profilingInclude.contains(name)) {
				String msg = MessageFormatter.format("Field 'profiling': name '{}' not included in selected publish config", name).getMessage();
				throw new IllegalArgumentException(msg);
			}

			profilingSet.add(profilingAll.get(name));
		}
		return profilingSet;
	}

	
	private List<CmsItem> getTranslationItems(CmsItemId itemId, PublishConfig publishConfig) {
		final List<CmsItemTranslation> translations = translationTracking.getTranslations(itemId); // Using deprecated method until TODO in translationTracking is resolved.

		logger.debug("Found {} translations.", translations.size());

		List<String> statusInclude = null;
		// publishConfig is allowed to be null
		if (publishConfig != null) {
			statusInclude = publishConfig.getStatusInclude();
		}
		// Normalize: empty list is not meaningful when publishing translations.
		if (statusInclude != null && statusInclude.isEmpty()) {
			statusInclude = null;
		}

		List<CmsItem> items = new ArrayList<CmsItem>();
		for (CmsItemTranslation t : translations) {
			// The CmsItemTranslation is now backed by a full CmsItem. 
			// However, it is a head item and the PublishPackage should contain revision items.
			// The getTranslation() method returns a CmsItemId with peg rev, as it has always done. 
			CmsItem tItem = lookupReporting.getItem(t.getTranslation());
			// Filter Translations with status not included by the PublishConfiguration.
			// Currently not filtering on other aspects, would likely indicate configuration mismatch (user should get a failure).
			if (statusInclude == null || statusInclude.contains(tItem.getStatus())) {
				logger.debug("Publish including Translation: {}", tItem.getId());
				items.add(tItem);
			} else {
				logger.debug("Publish excluding Translation (status = {}): {}", tItem.getStatus(), tItem.getId());
			}
		}
		return items;
	}

	
}
