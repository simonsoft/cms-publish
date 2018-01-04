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

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.web.ReposHtmlHelper;
import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingRecipe;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingSet;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;
import se.simonsoft.cms.release.translation.CmsItemTranslation;
import se.simonsoft.cms.release.translation.TranslationTracking;
import se.simonsoft.cms.reporting.CmsItemLookupReporting;

@Path("/publish4")
public class PublishResource {
	
	private final String hostname;
	private final Map<CmsRepository, CmsItemLookupReporting> lookup;
	private final PublishConfigurationDefault publishConfiguration;
	private final PublishPackageZip repackageService;
	private final ReposHtmlHelper htmlHelper;
	private final Map<CmsRepository, TranslationTracking> trackingMap;

	@Inject
	public PublishResource(@Named("config:se.simonsoft.cms.hostname") String hostname,
			Map<CmsRepository, CmsItemLookupReporting> lookup,
			PublishConfigurationDefault publishConfiguration,
			PublishPackageZip repackageService,
			Map<CmsRepository, TranslationTracking> trackingMap,
			ReposHtmlHelper htmlHelper) {
		
		this.hostname = hostname;
		this.lookup = lookup;
		this.publishConfiguration = publishConfiguration;
		this.repackageService = repackageService;
		this.trackingMap = trackingMap;
		this.htmlHelper = htmlHelper;
	}
	
	private static final Logger logger = LoggerFactory.getLogger(PublishResource.class);
	
	@GET
	@Path("release")
	@Produces(MediaType.TEXT_HTML)
	public String getReleaseForm(@QueryParam("item") CmsItemIdArg itemId) throws Exception {
		
		if (itemId == null) {
			throw new IllegalArgumentException("Field 'item': required");
		}
		
		logger.debug("Getting form for item: {}", itemId);
		
		CmsItemLookupReporting cmsItemLookupReporting = lookup.get(itemId.getRepository());
		CmsItem item = cmsItemLookupReporting.getItem(itemId);
		CmsItemPublish itemPublish = new CmsItemPublish(item);
		
		logger.debug("Requesting profilingSet...");
		PublishProfilingSet itemProfilingSet = publishConfiguration.getItemProfilingSet(itemPublish);
		
		Map<String, PublishProfilingRecipe> itemProfilings = null;
		if (itemProfilingSet == null) {
			itemProfilings = new HashMap<String, PublishProfilingRecipe>(); //Setting it to empty hashMap to make velocity compile. Maybe we should make null check in template?
			logger.debug("There is no profiles configured for item with id: {}", itemId);
		} else {
			itemProfilings = itemProfilingSet.getMap();
			logger.debug("ItemId: {} has: {} configured profiles", itemId, itemProfilings.size());
		}
		
		Map<String, PublishConfig> configuration = publishConfiguration.getConfigurationFiltered(itemPublish);
		
		VelocityEngine engine = getVelocityEngine(); //TODO: maybe we should inject velocity engine.

		VelocityContext context = new VelocityContext();
		Template template = engine.getTemplate("se/simonsoft/cms/publish/templates/batch-publish-template.vm");
		
		context.put("item", item);
		context.put("itemProfiling", itemProfilings);
		context.put("configuration", configuration);
		context.put("reposHeadTags", htmlHelper.getHeadTags(null));

		StringWriter wr = new StringWriter();
		template.merge(context, wr);

		return wr.toString();
	}
	
	@GET
	@Path("release/download")
	@Produces("application/zip")
	public Response getDownload(@QueryParam("item") CmsItemIdArg itemId,
						@QueryParam("includerelease") boolean includeRelease,
						@QueryParam("includetranslations") boolean includeTranslations,
						@QueryParam("profiling") String[] profiling,
						@QueryParam("publication") String publication) throws Exception {
		
		logger.debug("Download of item: {} requested with master: {}, translations: {} and profiles: {}", itemId, includeRelease, includeTranslations, Arrays.toString(profiling));
		
		if (itemId == null) {
			throw new IllegalArgumentException("Field 'item': required");
		}
		
		itemId.setHostnameOrValidate(this.hostname);
		
		if (!itemId.isPegged()) {
			throw new IllegalArgumentException("Field 'item': revision is required");
		}
		
		final Set<CmsItem> items = new HashSet<CmsItem>();
		
		CmsItemLookupReporting lookupReporting = lookup.get(itemId.getRepository());
		CmsItem masterItem = lookupReporting.getItem(itemId);
		items.add(masterItem);
		
		Map<String, PublishConfig> configurationFiltered = publishConfiguration.getConfigurationFiltered(new CmsItemPublish(masterItem));
		final PublishConfig simplePdfConfig = configurationFiltered.get("simple-pdf"); // trying with one known config to start with.
		
		List<CmsItemTranslation> translations = null;
		if (includeTranslations) {
			TranslationTracking translationTracking = trackingMap.get(itemId.getRepository());
			translations = translationTracking.getTranslations(itemId); // Using deprecated method until TODO in translationTracking is resolved.
			logger.debug("Found {} translations.", translations.size());
			
			for (CmsItemTranslation t: translations) {
				CmsItem tItem = lookupReporting.getItem(t.getTranslation());
				items.add(tItem);
			}
		}
		
	    StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(OutputStream os) throws IOException, WebApplicationException {
            	repackageService.getZip(items, "simple-pdf", simplePdfConfig, null, os);
            }
        };
		
		ResponseBuilder responseBuilder = Response.ok(stream, MediaType.APPLICATION_OCTET_STREAM);
		
		return responseBuilder.build();
	}
	
	private VelocityEngine getVelocityEngine() {
		//TODO: Maybe this should be injected. Initializing a Velocity Engine requires configuration properties for logging that should be standard.
		VelocityEngine engine = new VelocityEngine();
		engine.setProperty("runtime.references.strict", true);
		
		Properties p = new Properties();
		p.setProperty(RuntimeConstants.RESOURCE_LOADER, "class");
		p.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		p.put("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.SimpleLog4JLogSystem");
		p.put("runtime.log.logsystem.log4j.category", "velocity");
		p.put("runtime.log.logsystem.log4j.logger", "velocity");
		
		try {
			engine.init(p);
		} catch (Exception e) {
			throw new RuntimeException("Could not initilize Velocity engine with given properties.");
		}
		
		return engine;
	}
}
