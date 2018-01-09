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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import javax.ws.rs.core.StreamingOutput;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import se.repos.web.ReposHtmlHelper;
import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.export.CmsExportJobNotFoundException;
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
	private final PublishJobStorageFactory storageFactory;
	
	private VelocityEngine templateEngine;

	@Inject
	public PublishResource(@Named("config:se.simonsoft.cms.hostname") String hostname,
			Map<CmsRepository, CmsItemLookupReporting> lookup,
			PublishConfigurationDefault publishConfiguration,
			PublishPackageZip repackageService,
			Map<CmsRepository, TranslationTracking> trackingMap,
			ReposHtmlHelper htmlHelper,
			PublishJobStorageFactory storageFactory,
			VelocityEngine templateEngine
			) {
		
		this.hostname = hostname;
		this.lookup = lookup;
		this.publishConfiguration = publishConfiguration;
		this.repackageService = repackageService;
		this.trackingMap = trackingMap;
		this.htmlHelper = htmlHelper;
		this.storageFactory = storageFactory;
		this.templateEngine = templateEngine;
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
		
		Map<String, PublishProfilingRecipe> itemProfilings = new HashMap<>(); //Initialize the map to prevent NPE in Velocity.
		if (itemProfilingSet != null) {
			itemProfilings = itemProfilingSet.getMap();
			logger.debug("ItemId: {} has: {} configured profiles", itemId, itemProfilings.size());
		}
		
		Map<String, PublishConfig> configuration = publishConfiguration.getConfigurationFiltered(itemPublish);
		
		VelocityContext context = new VelocityContext();
		Template template = templateEngine.getTemplate("se/simonsoft/cms/publish/templates/batch-publish-template.vm");
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
						@QueryParam("publication") final String publication) throws Exception {
		
		logger.debug("Download of item: {} requested with master: {}, translations: {} and profiles: {}", itemId, includeRelease, includeTranslations, Arrays.toString(profiling));
		
		if (itemId == null) {
			throw new IllegalArgumentException("Field 'item': required");
		}
		
		itemId.setHostnameOrValidate(this.hostname);
		
		if (!itemId.isPegged()) {
			throw new IllegalArgumentException("Field 'item': revision is required");
		}
		
		if (publication == null || publication.isEmpty()) {
			throw new IllegalArgumentException("Field 'publication': publication name is required");
		}
		
		if (!includeRelease && !includeTranslations) {
			throw new IllegalArgumentException("Field 'includerelease': must be selected if 'includetranslations' is disabled");
		}
		
		final List<CmsItem> items = new ArrayList<CmsItem>();
		
		final CmsItemLookupReporting lookupReporting = lookup.get(itemId.getRepository());
		CmsItem releaseItem = lookupReporting.getItem(itemId);
		
		if (includeRelease) {
			items.add(releaseItem);
		}
		
		if (includeTranslations) {
			List<CmsItem> translationItems = getTranslationItems(itemId, publication);
			if (translationItems.isEmpty()) {
				throw new IllegalArgumentException("Translations requested, no translations found.");
			}
			items.addAll(translationItems);
		}
		
		final Set<CmsItem> publishedItems = new HashSet<CmsItem>();
		Map<String, PublishConfig> configurationFiltered = null;
		for (CmsItem item: items) {
			configurationFiltered = publishConfiguration.getConfigurationFiltered(new CmsItemPublish(item));
			if (configurationFiltered.containsKey(publication)) {
				publishedItems.add(item);
			}
		}
		
		final PublishConfig publishConfig = configurationFiltered.get(publication);
		
		StreamingOutput stream = new StreamingOutput() {
			@Override
			public void write(OutputStream os) throws IOException, WebApplicationException {
				try {
					repackageService.getZip(publishedItems, publication, publishConfig, null, os); // Profiles are null at the moment.
				} catch (CmsExportJobNotFoundException e) {
					String message = MessageFormatter.format("Published job does not exist: {}", e.getExportJob().getJobPath().toString()).getMessage();
					throw new IllegalStateException(message, e);
				}
			}
		};
		
		return Response.ok(stream, MediaType.APPLICATION_OCTET_STREAM)
				.header("Content-Disposition", "attachment; filename=" + storageFactory.getNameBase(itemId, null) + ".zip")
				.build();
	}

	private List<CmsItem> getTranslationItems(CmsItemId itemId, String publication) {
		final CmsItemLookupReporting lookupReporting = lookup.get(itemId.getRepository());
		final TranslationTracking translationTracking = trackingMap.get(itemId.getRepository());
		final List<CmsItemTranslation> translations = translationTracking.getTranslations(itemId); // Using deprecated method until TODO in translationTracking is resolved.

		logger.debug("Found {} translations.", translations.size());

		List<CmsItem> items = new ArrayList<CmsItem>();
		for (CmsItemTranslation t: translations) {
			CmsItem tItem = lookupReporting.getItem(t.getTranslation());
			items.add(tItem);
		}
		
		return items;
	}
}
