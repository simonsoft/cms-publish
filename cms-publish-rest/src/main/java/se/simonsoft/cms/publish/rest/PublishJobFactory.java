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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.publish.config.PublishConfigTemplateString;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfigArea;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfigStorage;
import se.simonsoft.cms.publish.config.databinds.job.PublishJob;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobStorage;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingRecipe;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;
import se.simonsoft.cms.release.translation.TranslationLocalesMapping;

public class PublishJobFactory {


	private final String cloudId;
	private final String type = "publish-job";
	private final String action = "publish-preprocess"; // Preprocess is the first stage in Workflow (CMS 4.4), can potentially request webapp work (depends on preprocess.type).
	private final PublishJobStorageFactory storageFactory;
	
	
	private static final Logger logger = LoggerFactory.getLogger(PublishJobFactory.class);

	@Inject
	public PublishJobFactory(
			@Named("config:se.simonsoft.cms.cloudid") String cloudId,
			PublishJobStorageFactory storageFactory) {
		
		this.cloudId = cloudId;
		this.storageFactory = storageFactory;
	}
	
	
	public Set<PublishJob> getPublishJobsForPackage(PublishPackage publishPackage, PublishConfigurationDefault publishConfiguration) {
		
		Set<PublishJob> jobs = new LinkedHashSet<PublishJob>();
		// Single config, looping over items.
		for (CmsItem item: publishPackage.getPublishedItems()) {
			CmsItemPublish itemPublish = (CmsItemPublish) item;
			String configName = publishPackage.getPublication();
			PublishConfig config = publishPackage.getPublishConfig();
			// A PublishPackage is currently not relevant on Author Area, so getReleaseLocale() should work as fallback.
			TranslationLocalesMapping localesRfc = publishConfiguration.getTranslationLocalesMapping(itemPublish);
			Set<PublishProfilingRecipe> profilingSet = publishPackage.getProfilingSet();
			
			// Verify filtering for condition not handled below: profilingInclude == false && hasProfiles == true
			// Copied from PublishItemChangedEventListener, needed here?
			if (itemPublish.hasProfiles() && config.getProfilingInclude() != null && Boolean.FALSE.equals(config.getProfilingInclude())) {
				throw new IllegalArgumentException("Item should not have profiling, filtering incorrect.");
			}
			
			if (Boolean.TRUE.equals(config.getProfilingInclude())) {
				// Profiling, zero or more publications per item.
				// Will return empty List<PublishJob> if item has no profiles or filtered by 'profilingNameInclude'.
				//PublishProfilingSet profilingSet = publishConfiguration.getItemProfilingSet(itemPublish);
				// Trusting the PublishPackage profilingSet, this method will filter on ProfilingNameInclude.
				jobs.addAll(getPublishJobsProfiling(itemPublish, config, configName, profilingSet, localesRfc));
			} else {
				// Normal, non-profiling job.
				PublishJob pj = getPublishJob(itemPublish, config, configName, null, localesRfc);
				jobs.add(pj);
			}
		}
		return jobs;
	}
	
	
	public List<PublishJob> getPublishJobsProfiling(CmsItemPublish itemPublish, PublishConfig config, String configName, Iterable<PublishProfilingRecipe> profilingSet, TranslationLocalesMapping localesRfc) {
		List<PublishJob> profiledJobs = new ArrayList<PublishJob>();
				
		for (PublishProfilingRecipe profilesRecipe: profilingSet) {
			List<String> profilingNames = config.getProfilingNameInclude();
			// Filter on profilesNameInclude if set.
			if (profilingNames == null || profilingNames.contains(profilesRecipe.getName())) {
				profiledJobs.add(getPublishJob(itemPublish, config, configName, profilesRecipe, localesRfc));
			}
		}
		return profiledJobs;
	}
	
	// TODO: Consider API diffentiating btw full PublishJob (start job) and light PublishJob (download/status/...).
	// Such differentiation might also be confusing.
	public PublishJob getPublishJob(CmsItemPublish item, PublishConfig c, String configName, PublishProfilingRecipe profiling, TranslationLocalesMapping localesRfc) {
		PublishConfigTemplateString templateEvaluator = getTemplateEvaluator(item, configName, profiling, localesRfc);
		PublishJobManifestBuilder manifestBuilder = new PublishJobManifestBuilder(templateEvaluator, localesRfc);
		
		PublishConfigArea area = PublishJobManifestBuilder.getArea(item, c.getAreas());
		PublishJob pj = new PublishJob(c);
		pj.setArea(area); 
		pj.setItemid(item.getId().getLogicalIdFull()); // Event workflow has itemid hostname so publish workflow should as well.
		pj.setAction(this.action); 
		pj.setType(this.type);
		pj.setConfigname(configName);
		
		// The source attribute is used by publishing engines with the ability to directly fetch data from the CMS.
		// When Preprocess is configured, the source attribute should be null because the preprocessed / exported data should be used instead.
		if (pj.getOptions().getPreprocess().getType() == null || pj.getOptions().getPreprocess().getType().equals("none")) {
			pj.getOptions().setSource(item.getId().getLogicalId());			
		}
		if (profiling != null) {
			pj.getOptions().setProfiling(profiling);
		}
		
		PublishConfigStorage configStorage = pj.getOptions().getStorage();
		PublishJobStorage storage = storageFactory.getInstance(configStorage, item, configName, profiling);
		pj.getOptions().setStorage(storage);
		
		String pathname = templateEvaluator.evaluate(area.getPathnameTemplate());
		pj.getOptions().setPathname(pathname);
		
		// Build the Manifest, modifies the existing manifest object.
		manifestBuilder.build(item, pj);
		
		// Decided that template evaluation of params is NOT the long term solution, at least for now.
		// Distribution implementations should use the Manifest for template evaluation, similar to external consumers.
		// Evaluate Velocity for params
		// Normally we evaluate config fields '...Templates'.
		
		// Need to refresh the template evaluator to contain the storage object.
		// Decided against exposing the storage object. This is internal and should be understood by workflow Tasks.
		/*
		templateEvaluator = getTemplateEvaluator(item, profiling, storage);
		manifestBuilder = new PublishJobManifestBuilder(templateEvaluator);
		*/
		// Decided that template evaluation of params is NOT the long term solution. At least for now.
		// The below temporary implementation only evaluates the options.params map, not deeper options.*.params maps. 
		/*
		pj.getOptions().setParams(manifestBuilder.buildMap(item, pj.getOptions().getParams()));
		*/
		
		logger.debug("Created PublishJob from config: {}", configName);
		return pj;
	}
	

	
	private PublishConfigTemplateString getTemplateEvaluator(CmsItemPublish item, String configName, PublishProfilingRecipe profiling, TranslationLocalesMapping localesRfc) {
		PublishConfigTemplateString tmplStr = new PublishConfigTemplateString();
		// Define "$aptpath" transparently to allow strict references without escape requirement in JSON.
		// Important if allowing evaluation of params in the future.
		tmplStr.withEntry("aptpath", "$aptpath");
		// Add the cloudid, might be useful for delivery.
		tmplStr.withEntry("cloudid", this.cloudId);
		// Add config name, might be useful for delivery.
		tmplStr.withEntry("configname", configName);
		// Add the item
		tmplStr.withEntry("item", item);
		// Add profiling object, can be null;
		tmplStr.withEntry("profiling", profiling);
		// Add access to the Locales mapping instance.
		tmplStr.withEntry("localesRfc", localesRfc);
		// Add storage object to allow configuration of parameters with S3 key etc.
		//tmplStr.withEntry("storage", storage);
		return tmplStr;
	}
}

