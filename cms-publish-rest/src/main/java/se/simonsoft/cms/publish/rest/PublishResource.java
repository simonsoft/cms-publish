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
import se.simonsoft.cms.item.export.CmsExportAccessDeniedException;
import se.simonsoft.cms.item.export.CmsExportJobNotFoundException;
import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.item.workflow.WorkflowExecution;
import se.simonsoft.cms.item.workflow.WorkflowExecutionStatus;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.job.PublishJob;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingRecipe;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingSet;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;
import se.simonsoft.cms.release.translation.CmsItemTranslation;
import se.simonsoft.cms.release.translation.TranslationTracking;
import se.simonsoft.cms.reporting.CmsItemLookupReporting;

@Path("/publish4")
public class PublishResource {
	
	private final WorkflowExecutionStatus executionsStatus;
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
			@Named("config:se.simonsoft.cms.aws.workflow.publish.executions") WorkflowExecutionStatus executionStatus,
			Map<CmsRepository, CmsItemLookupReporting> lookup,
			PublishConfigurationDefault publishConfiguration,
			PublishPackageZip repackageService,
			Map<CmsRepository, TranslationTracking> trackingMap,
			ReposHtmlHelper htmlHelper,
			PublishJobStorageFactory storageFactory,
			VelocityEngine templateEngine
			) {
		
		this.hostname = hostname;
		this.executionsStatus = executionStatus;
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
		

		PublishProfilingSet itemProfilingSet = publishConfiguration.getItemProfilingSet(itemPublish);
		Map<String, PublishProfilingRecipe> itemProfilings = new HashMap<>(); //Initialize the map to prevent NPE in Velocity.
		if (itemProfilingSet != null) {
			itemProfilings = itemProfilingSet.getMap();
			logger.debug("ItemId: {} has: {} configured profiles", itemId, itemProfilings.size());
		}
		
		Map<String, PublishConfig> configuration = publishConfiguration.getConfigurationFiltered(itemPublish);
		
		VelocityContext context = new VelocityContext();
		Template template = templateEngine.getTemplate("se/simonsoft/cms/publish/rest/export-release-form.vm");
		context.put("item", item);
		context.put("itemProfiling", itemProfilings);
		context.put("configuration", configuration);
		context.put("reposHeadTags", htmlHelper.getHeadTags(null));
		
		Set<WorkflowExecution> releaseExecutions = executionsStatus.getWorkflowExecutions(itemId, true);
		//Key: Execution status, Value set<configNames> 
		Map<String, Set<String>> configStatusRelease = getExecutionConfigs(releaseExecutions);
		context.put("releaseExecutions", configStatusRelease);
		
		Set<WorkflowExecution> translationExecutions = getExecutionStatusForTranslations(itemId);
		//Key: Execution status, Value set<configNames> 
		Map<String, Set<String>> configStatusTrans = getExecutionConfigs(translationExecutions);
		context.put("translationExecutions", configStatusTrans);
		
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
		for (CmsItem item: items) {
			Map<String, PublishConfig> configurationFiltered = publishConfiguration.getConfigurationFiltered(new CmsItemPublish(item));
			if (configurationFiltered.containsKey(publication)) {
				publishedItems.add(item);
			} else {
				String msg = MessageFormatter.format("Field 'publication': publication name '{}' not defined for item {}.", publication, item.getId()).getMessage();
				throw new IllegalArgumentException(msg);
			}
		}
		
		// The Release config will be guiding regardless if the Release is included or not. The Translation config must be equivalent if separately specified.
		Map<String, PublishConfig> configurationsRelease = publishConfiguration.getConfigurationFiltered(new CmsItemPublish(releaseItem));
		final PublishConfig publishConfig = configurationsRelease.get(publication);
		
		if (publishConfig.getOptions().getStorage() != null) {
			String type = publishConfig.getOptions().getStorage().getType();
			if (type != null && !type.equals("s3")) {
				String msg = MessageFormatter.format("Field 'publication': publication name '{}' can not be exported (configured for non-default storage).", publication).getMessage();
				throw new IllegalStateException(msg);
			}
		}
		
		// Profiling:
		// Filtering above takes care of mismatch btw includeFiltering and whether item has Profiling.
		// The 'profiling' parameter should only be allowed if configuration has includeProfiling = true, required then.
		if (Boolean.TRUE.equals(publishConfig.getProfilingInclude())) {
			if (profiling.length == 0) {
				String msg = MessageFormatter.format("Field 'profiling': required for publication name '{}'.", publication).getMessage();
				throw new IllegalArgumentException(msg);
			}
		} else {
			if (profiling.length > 0) {
				String msg = MessageFormatter.format("Field 'profiling': not allowed for publication name '{}'.", publication).getMessage();
				throw new IllegalArgumentException(msg);
			}
		}
		
		
		final Set<PublishProfilingRecipe> profilingSet;
		if (profiling.length > 0) {
			profilingSet = getProfilingSetSelected(new CmsItemPublish(releaseItem), publishConfig, profiling);
		} else {
			profilingSet = null;
		}
		
		
		StreamingOutput stream = new StreamingOutput() {
			@Override
			public void write(OutputStream os) throws IOException, WebApplicationException {
				try {
					repackageService.getZip(publishedItems, publication, publishConfig, profilingSet, os);
				} catch (CmsExportJobNotFoundException e) {
					String message = MessageFormatter.format("Published job does not exist: {}", e.getExportJob().getJobPath().toString()).getMessage();
					throw new IllegalStateException(message, e);
				} catch (CmsExportAccessDeniedException e) {
					// We can not determine if the exception is because lack of list buckets credentials or that the job is missing.
					String message = MessageFormatter.format("Published job does not exist: {}", e.getExportJob().getJobPath().toString()).getMessage();
					throw new IllegalStateException(message, e);
				}
			}
		};
		
		return Response.ok(stream, MediaType.APPLICATION_OCTET_STREAM)
				.header("Content-Disposition", "attachment; filename=" + getFilenameDownload(items, publication, releaseItem) + ".zip")
				.build();
	}
	
	private String getFilenameDownload(List<CmsItem> items, String publication, CmsItem releaseItem) {
		
		String releaseLabel = new CmsItemPublish(releaseItem).getReleaseLabel();
		
		if (releaseLabel == null) {
			throw new IllegalStateException("The Release does not have a Release Label property.");
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append(releaseItem.getId().getRelPath().getNameBase());
		sb.append("_" + releaseLabel);
		sb.append("_" + publication);
		long revLatest = getRevLatest(items);
		sb.append(String.format("_r%010d", revLatest));
		
		return sb.toString();
	}
	
	private long getRevLatest(List<CmsItem> items) {
		
		long rev = 0;
		for (CmsItem item: items) {
			long number = item.getId().getPegRev();
			if (rev < number) {
				rev = number;
			}
		}
		return rev;
	}
	
	
	// Order will not be preserved, should not matter for packaging the publications.
	private Set<PublishProfilingRecipe> getProfilingSetSelected(CmsItemPublish item, PublishConfig publishConfig, String[] profiling) {
		
		Map<String, PublishProfilingRecipe> profilingAll = this.publishConfiguration.getItemProfilingSet(item).getMap();
		HashSet<PublishProfilingRecipe> profilingSet = new HashSet<PublishProfilingRecipe>(profilingAll.size());
		List<String> profilingInclude = publishConfig.getProfilingNameInclude();
		
		for (String name: profiling) {
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
	
	
	private Map<String, Set<String>> getExecutionConfigs(Set<WorkflowExecution> executions) {
		// Key: Execution status, Value set<configNames> 
		Map<String, Set<String>> configStatuses = new HashMap<String, Set<String>>();
		
		for (WorkflowExecution we: executions) {
			PublishJob input = (PublishJob) we.getInput();
			Set<String> set = configStatuses.get(we.getStatus());
			if (set == null) {
				set = new HashSet<String>();
			}
			set.add(input.getConfigname());
			configStatuses.put(we.getStatus(), set);
		}
		
		return configStatuses;
	}
	
	private Set<WorkflowExecution> getExecutionStatusForTranslations(CmsItemId release) {
		
		List<CmsItem> translationItems = getTranslationItems(release, null);
		Set<WorkflowExecution> executions = new HashSet<WorkflowExecution>();
		for (CmsItem i: translationItems) {
			Set<WorkflowExecution> workflowExecutions = executionsStatus.getWorkflowExecutions(i.getId(), false);
			executions.addAll(workflowExecutions);
		}
		return executions;
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
