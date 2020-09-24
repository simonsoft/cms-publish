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

import org.apache.velocity.app.VelocityEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import se.repos.web.ReposHtmlHelper;
import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.export.*;
import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.item.workflow.WorkflowExecution;
import se.simonsoft.cms.item.workflow.WorkflowExecutionStatus;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.job.PublishJob;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingRecipe;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingSet;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;
import se.simonsoft.cms.release.ReleaseLabel;
import se.simonsoft.cms.release.translation.CmsItemTranslation;
import se.simonsoft.cms.release.translation.TranslationTracking;
import se.simonsoft.cms.reporting.CmsItemLookupReporting;
import se.simonsoft.cms.reporting.response.CmsItemRepositem;

@Path("/publish4")
public class PublishResource {
	
	private final WorkflowExecutionStatus executionsStatus;
	private final String hostname;
	private final Map<CmsRepository, CmsItemLookupReporting> lookup;
	private final PublishConfigurationDefault publishConfiguration;
	private final PublishPackageZipBuilder repackageService;
	private final PublishPackageStatus statusService;
	private final ReposHtmlHelper htmlHelper;
	private final Map<CmsRepository, TranslationTracking> trackingMap;
	@SuppressWarnings("unused")
	private final PublishJobStorageFactory storageFactory;

	private VelocityEngine templateEngine;

	@Inject
	public PublishResource(@Named("config:se.simonsoft.cms.hostname") String hostname,
			@Named("config:se.simonsoft.cms.aws.workflow.publish.executions") WorkflowExecutionStatus executionStatus,
			Map<CmsRepository, CmsItemLookupReporting> lookup,
			PublishConfigurationDefault publishConfiguration,
			PublishPackageZipBuilder repackageService,
			PublishPackageStatus statusService,
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
		this.statusService = statusService;
		this.trackingMap = trackingMap;
		this.htmlHelper = htmlHelper;
		this.storageFactory = storageFactory;
		this.templateEngine = templateEngine;
	}
	
	private static final Logger logger = LoggerFactory.getLogger(PublishResource.class);

	public PublishRelease getPublishRelease(CmsItemIdArg itemId) throws Exception {

		CmsItemLookupReporting cmsItemLookupReporting = lookup.get(itemId.getRepository());
		CmsItem item = cmsItemLookupReporting.getItem(itemId);
		CmsItemPublish itemPublish = new CmsItemPublish(item);

		PublishProfilingSet itemProfilingSet = publishConfiguration.getItemProfilingSet(itemPublish);
		Map<String, PublishProfilingRecipe> itemProfilings = new HashMap<>(); // Initialize the map to prevent NPE in Velocity.
		if (itemProfilingSet != null) {
			itemProfilings = itemProfilingSet.getMap();
		}

		Map<String, PublishConfig> configuration = publishConfiguration.getConfigurationFiltered(itemPublish);

		// Avoid displaying an empty dialog, too complex to handle in Velocity (probably possible though).
		if (configuration.isEmpty()) {
			throw new IllegalStateException("No publications are configured.");
		} else {
			int visible = 0;
			for (Entry<String, PublishConfig> config : configuration.entrySet()) {
				if (config.getValue().isVisible()) {
					visible++;
				}
			}
			if (visible == 0) {
				throw new IllegalStateException("No publications are configured to be visible.");
			}
		}

		return new PublishRelease((CmsItemRepositem) item, configuration, itemProfilings);
	}

	@GET
	@Path("release")
	@Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_JSON})
	public PublishRelease getRelease(@QueryParam("item") CmsItemIdArg itemId) throws Exception {

		if (itemId == null) {
			throw new IllegalArgumentException("Field 'item': required");
		}

		logger.debug("Getting release form for item: {}", itemId);

		PublishRelease publishRelease = getPublishRelease(itemId);

		Map<String, PublishProfilingRecipe> itemProfilings = publishRelease.getProfiling();

		if (!itemProfilings.isEmpty()) {
			logger.debug("ItemId: {} has: {} configured profiles", itemId, itemProfilings.size());
		}

		return publishRelease;
	}

	public PublishPackage getPublishPackage(CmsItemIdArg itemId, boolean includeRelease, boolean includeTranslations, String[] profiling, String publication) throws Exception {
		
		if (itemId == null) {
			throw new IllegalArgumentException("Field 'item': required");
		}
		
		if (!itemId.isPegged()) {
			throw new IllegalArgumentException("Field 'item': revision is required");
		}
		
		if (publication == null || publication.isEmpty()) {
			throw new IllegalArgumentException("Field 'publication': publication name is required");
		}
		
		if (!includeRelease && !includeTranslations) {
			throw new IllegalArgumentException("Field 'includerelease': must be selected if 'includetranslations' is disabled");
		}
		
		itemId.setHostnameOrValidate(this.hostname);
		final List<CmsItem> items = new ArrayList<CmsItem>();
		
		final CmsItemLookupReporting lookupReporting = lookup.get(itemId.getRepository());
		CmsItem releaseItem = lookupReporting.getItem(itemId);
		
		// The Release config will be guiding regardless if the Release is included or not. The Translation config must be equivalent if separately specified.
		Map<String, PublishConfig> configurationsRelease = publishConfiguration.getConfiguration(releaseItem.getId());
		final PublishConfig publishConfig = configurationsRelease.get(publication);
		
		if (publishConfig == null) {
			throw new IllegalArgumentException("Publish Configuration must be defined for the Release: " + publication);
		}
		
		if (includeRelease) {
			items.add(releaseItem);
		}
		
		if (includeTranslations) {
			List<CmsItem> translationItems = getTranslationItems(itemId, publishConfig);
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
		
		ReleaseLabel releaseLabel = new ReleaseLabel(new CmsItemPublish(releaseItem).getReleaseLabel());
		return new PublishPackage(publication, publishConfig, profilingSet, publishedItems, releaseItem.getId(), releaseLabel);
	}
	
	
	
	@GET
	@Path("release/status")
	@Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_JSON})
	public Set<WorkflowExecution> getStatus(@QueryParam("item") CmsItemIdArg itemId,
						@QueryParam("includerelease") boolean includeRelease,
						@QueryParam("includetranslations") boolean includeTranslations,
						@QueryParam("profiling") String[] profiling,
						@QueryParam("publication") final String publication) throws Exception {
		
		logger.debug("Status of Release: {} requested with release: {}, translations: {} and profiles: {}", itemId, includeRelease, includeTranslations, Arrays.toString(profiling));

		if (profiling != null && profiling.length != 0) {
			throw new IllegalArgumentException("Field 'profiling': profiling is currently not supported");
		}

		PublishPackage publishPackage = getPublishPackage(itemId, includeRelease, includeTranslations, profiling, publication);
		return statusService.getStatus(publishPackage);
	}
	
	
	@GET
	@Path("release/download")
	@Produces("application/zip")
	public Response getDownload(@QueryParam("item") CmsItemIdArg itemId,
						@QueryParam("includerelease") boolean includeRelease,
						@QueryParam("includetranslations") boolean includeTranslations,
						@QueryParam("profiling") String[] profiling,
						@QueryParam("publication") final String publication) throws Exception {
		
		logger.debug("Download of Release: {} requested with release: {}, translations: {} and profiles: {}", itemId, includeRelease, includeTranslations, Arrays.toString(profiling));
		
		
		PublishPackage publishPackage = getPublishPackage(itemId, includeRelease, includeTranslations, profiling, publication);
		
		PublishConfig publishConfig = publishPackage.getPublishConfig();
		if (publishConfig.getOptions().getStorage() != null) {
			String type = publishConfig.getOptions().getStorage().getType();
			if (type != null && !type.equals("s3")) {
				String msg = MessageFormatter.format("Field 'publication': publication name '{}' can not be exported (configured for non-default storage).", publication).getMessage();
				throw new IllegalStateException(msg);
			}
		}
		
		StreamingOutput stream = getDownloadStreamingOutput(publishPackage);
		return Response.ok(stream, MediaType.APPLICATION_OCTET_STREAM)
				.header("Content-Disposition", "attachment; filename=\"" + getFilenameDownload(publishPackage) + ".zip\"")
				.build();
	}
	
	
	private StreamingOutput getDownloadStreamingOutput(final PublishPackage publishPackage) {
		
		StreamingOutput stream = new StreamingOutput() {
			@Override
			public void write(OutputStream os) throws IOException, WebApplicationException {
				try {
					repackageService.getZip(publishPackage, os);
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
		return stream;
	}
	
	
	private String getFilenameDownload(PublishPackage publishPackage) {
		
		ReleaseLabel releaseLabel = publishPackage.getReleaseLabel();
		
		if (releaseLabel == null) {
			throw new IllegalStateException("The Release does not have a Release Label property.");
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append(publishPackage.getReleaseItemId().getRelPath().getNameBase());
		sb.append("_" + releaseLabel.getLabel());
		sb.append("_" + publishPackage.getPublication());
		long revLatest = publishPackage.getRevisionLatest();
		sb.append(String.format("_r%010d", revLatest));
		
		return sb.toString();
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

	private List<CmsItem> getTranslationItems(CmsItemId itemId, PublishConfig publishConfig) {
		final CmsItemLookupReporting lookupReporting = lookup.get(itemId.getRepository());
		final TranslationTracking translationTracking = trackingMap.get(itemId.getRepository());
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
		for (CmsItemTranslation t: translations) {
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
