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
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.UUID;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import org.apache.velocity.app.VelocityEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import se.repos.web.ReposHtmlHelper;
import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.export.CmsExportAccessDeniedException;
import se.simonsoft.cms.item.export.CmsExportJobNotFoundException;
import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.item.info.CmsItemLookup;
import se.simonsoft.cms.item.workflow.WorkflowExecution;
import se.simonsoft.cms.publish.config.PublishExecutor;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.job.PublishJob;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingRecipe;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingSet;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;
import se.simonsoft.cms.release.ReleaseLabel;
import se.simonsoft.cms.reporting.CmsItemLookupReporting;
import se.simonsoft.cms.reporting.repositem.CmsItemSearch;
import se.simonsoft.cms.reporting.response.CmsItemRepositem;

@Path("/publish4")
public class PublishResource {

	private static final int MAX_START_BODY_SIZE = 110 * 1024;
	private final String hostname;
	private final CmsItemSearch cmsItemSearch;
	private final Map<CmsRepository, CmsItemLookup> lookupMap;
	private final Map<CmsRepository, CmsItemLookupReporting> lookupReportingMap;
	private final PublishConfigurationDefault publishConfiguration;
	private final PublishPackageZipBuilder repackageService;
	private final PublishPackageStatus statusService;
	private final Map<CmsRepository, PublishStartService> publishStartService;
	private final Map<CmsRepository, PublishPackageFactory> packageFactory;
	@SuppressWarnings("unused")
	private final ReposHtmlHelper htmlHelper;
	@SuppressWarnings("unused")
	private final PublishJobStorageFactory storageFactory;
	private final PublishJobFactory jobFactory;
	private final PublishExecutor publishExecutor;
	private final ObjectReader publishStartOptionsReader;
	private final ObjectWriter writer;

	@SuppressWarnings("unused")
	private VelocityEngine templateEngine;

	@Inject
	public PublishResource(@Named("config:se.simonsoft.cms.hostname") String hostname,
			CmsItemSearch cmsItemSearch,
			Map<CmsRepository, CmsItemLookup> lookup,
			Map<CmsRepository, CmsItemLookupReporting> lookupReporting,
			PublishConfigurationDefault publishConfiguration,
			PublishPackageZipBuilder repackageService,
			PublishPackageStatus statusService,
			Map<CmsRepository, PublishStartService> publishStartService,
			Map<CmsRepository, PublishPackageFactory> packageFactory,
			ReposHtmlHelper htmlHelper,
			PublishJobStorageFactory storageFactory,
			PublishJobFactory jobFactory,
			PublishExecutor publishExecutor,
			VelocityEngine templateEngine,
			ObjectReader reader,
			ObjectWriter writer
			) {
		
		this.hostname = hostname;
		this.cmsItemSearch = cmsItemSearch;
		this.lookupMap = lookup;
		this.lookupReportingMap = lookupReporting;
		this.publishConfiguration = publishConfiguration;
		this.repackageService = repackageService;
		this.statusService = statusService;
		this.publishStartService = publishStartService;
		this.packageFactory = packageFactory;
		this.htmlHelper = htmlHelper;
		this.storageFactory = storageFactory;
		this.jobFactory = jobFactory;
		this.publishExecutor = publishExecutor;
		this.templateEngine = templateEngine;
		this.publishStartOptionsReader = reader.forType(PublishStartOptions.class);
		this.writer = writer;
	}
	
	private static final Logger logger = LoggerFactory.getLogger(PublishResource.class);

	public PublishRelease getPublishRelease(CmsItemIdArg itemId, boolean includeVisibleFalse) throws Exception {

		CmsItemLookupReporting cmsItemLookupReporting = lookupReportingMap.get(itemId.getRepository());
		// Would be preferable to suppress large meta fields like the Ditamap.
		CmsItem item = cmsItemLookupReporting.getItem(itemId);
		CmsItemPublish itemPublish = new CmsItemPublish(item);

		PublishProfilingSet itemProfilingSet = publishConfiguration.getItemProfilingSet(itemPublish);
		Map<String, PublishProfilingRecipe> itemProfilings = new HashMap<>(); // Initialize the map to prevent NPE in Velocity.
		if (itemProfilingSet != null) {
			itemProfilings = itemProfilingSet.getMap();
		}

		// Configs filtered for the Release item. 
		Map<String, PublishConfig> configuration;
		if (includeVisibleFalse) {
			// Showing also visible: false configs when advanced flag is set. 
			configuration = publishConfiguration.getConfigurationFiltered(itemPublish);
		} else {
			// Showing only Visible configs in the UI.
			configuration = publishConfiguration.getConfigurationVisible(itemPublish);
		}

		// Avoid displaying an empty dialog, too complex to handle in Velocity (probably possible though).
		// CMS 5.2: Let React handle situation where no configs apply / are visible.
		if (configuration.isEmpty()) {
			logger.debug("No publications are configured/visible (includeVisibleFalse: {}).", includeVisibleFalse);
			/* 
			if (!includeVisibleFalse && !publishConfiguration.getConfigurationFiltered(itemPublish).isEmpty()) {
				throw new IllegalStateException("No publications are configured to be visible.");
			}
			throw new IllegalStateException("No publications are configured.");
			*/
		}
		return new PublishRelease((CmsItemRepositem) item, configuration, itemProfilings);
	}

	
	@GET
	@Path("release")
	@Produces({MediaType.TEXT_HTML})
	public Response getReleaseHtml(@QueryParam("item") CmsItemIdArg itemId,
			@QueryParam("advanced") String advanced) throws Exception {

		// Typically no pegrev (never from our UI) but allowing it.
		if (itemId == null) {
			throw new IllegalArgumentException("Field 'item': required");
		}
		boolean includeVisibleFalse = (advanced != null);
		
		// For html it is not really necessary to construct the full PublishRelease object.
		// However, it is very important to properly display any error message thrown when failing to deserialize the publish configs.
		// Currently displayed instead of React UI. In the future it can be displayed by the React UI if the JSON request fails.
		
		logger.debug("Getting export publications form for item: {}", itemId);
		PublishRelease publishRelease = getPublishRelease(itemId, includeVisibleFalse);
		Response response = Response.ok(publishRelease)
				.header("Vary", "Accept")
				.build();
		return response;
	}

	
	@GET
	@Path("release")
	@Produces({MediaType.APPLICATION_JSON})
	public Response getReleaseJson(@QueryParam("item") CmsItemIdArg itemId,
			@QueryParam("advanced") String advanced) throws Exception {

		// Typically no pegrev (never from our UI) but allowing it.
		// Not sure if the reporting query, status query, start operation etc does the right thing with pegrev.
		if (itemId == null) {
			throw new IllegalArgumentException("Field 'item': required");
		}
		boolean includeVisibleFalse = (advanced != null);
		
		// #1693 Prevent JSON response if indexing is behind the Release item, HTTP '202 Accepted' with additional headers for progress.
		logger.debug("Getting export publications config for item: {}", itemId);
		PublishRelease publishRelease = getPublishRelease(itemId, includeVisibleFalse);
		CmsItem item = lookupMap.get(itemId.getRepository()).getItem(itemId);
		Response response;
		if (item.getRevisionChanged().equals(publishRelease.getItem().getRevisionChanged())) {
			response = Response.ok(publishRelease)
					.header("Vary", "Accept")
					.build();
		} else if (item.getRevisionChanged().isNewer(publishRelease.getItem().getRevisionChanged())) {
			// Need to wait for indexing to complete the revision.
			RepoRevision indexing = null;
			try {
				// Best effort attempt to get indexing status.
				indexing = this.cmsItemSearch.getRevisionCompleted(itemId.getRepository().getName(), null);
			} catch (Exception e) {
				logger.warn("Failed to determine indexing completed revision: {}", e.getMessage(), e);
			}
			//response = Response.accepted() // Likely not supported by Wink.
			response = Response.status(Status.ACCEPTED)
					.header("CMS-Revision-Item", item.getRevisionChanged().getNumberPadded())
					.header("CMS-Revision-Index", (indexing != null) ? indexing.getNumberPadded() : "")
					.build();
		} else {
			throw new IllegalStateException("Release has a higher revision in indexing: " + publishRelease.getItem());
		}
		return response;
	}
	
	/**
	 * @param itemId
	 * @param 
	 * @return JSON containing started execution ID and presigned urls (same as webhook).
	 * @throws Exception
	 */
	@POST
	@Path("api/start")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response doStartApi(@QueryParam("item") CmsItemIdArg itemId, String body) throws Exception {
		
		logger.debug("Start publication requested with item: {} and options: '{}'", itemId, body);

		if (body != null && body.getBytes().length > MAX_START_BODY_SIZE) {
			throw new IllegalArgumentException(String.format("The body size exceeds the %d bytes limit.", MAX_START_BODY_SIZE));
		}

		// Deserialize the body.
		PublishStartOptions options = publishStartOptionsReader.readValue(body);

		// Generate and log a UUID, set into options.executionid regardless if set already.
		UUID uuid = UUID.randomUUID();
		options.setExecutionid(uuid.toString());
		logger.debug("Generated executionid: '{}'", uuid);

		LinkedHashMap<String, String> result = publishStartService.get(itemId.getRepository()).doPublishStartItem(itemId, options);

		String jsonString = writer.writeValueAsString(result);
		Response response = Response.ok(jsonString)
				.header("Vary", "Accept")
				.build();
		return response;
	}
	
	/**
	 * @param itemId
	 * @param includeRelease
	 * @param includeTranslations
	 * @param profiling
	 * @param publication
	 * @return Set<String> containing started execution IDs
	 * @throws Exception
	 */
	@POST
	@Path("release/start")
	@Produces({MediaType.APPLICATION_JSON})
	public Response doStart(@FormParam("item") CmsItemIdArg itemId,
			@QueryParam("includerelease") boolean includeRelease,
			@QueryParam("includetranslations") boolean includeTranslations,
			@QueryParam("profiling") String[] profiling,
			@QueryParam("publication") final String publication,
			@QueryParam("advanced") String advanced) throws Exception {
		
		logger.debug("Start publication '{}' requested with item: {} and profiles: '{}'", publication, itemId, Arrays.toString(profiling));
		logger.debug("Start publication '{}' requested with item: {} and advanced: '{}'", publication, itemId, advanced);
		boolean allowStartSucceeded = (advanced != null);
		itemId.setHostnameOrValidate(this.hostname);
		
		// TODO: Support multiple profiling parameters, when statusService can support the filtering. 
		if (profiling != null && profiling.length > 1) {
			throw new IllegalArgumentException("Field 'profiling': multiple profiling parameters is currently not supported");
		}
		
		PublishPackage publishPackage = getPublishPackage(itemId, includeRelease, includeTranslations, profiling, publication);
		Set<PublishJob> jobs = this.jobFactory.getPublishJobsForPackage(publishPackage, this.publishConfiguration);
		
		jobs = statusService.getJobsStartAllowed(publishPackage, jobs, allowStartSucceeded);
		logger.info("Starting publish execution '{}' for {} items.", publication, jobs.size());
		Set<String> result = publishExecutor.startPublishJobs(jobs);
		
		// Requiring GenericEntity for Iterable<?>.
		GenericEntity<Iterable<String>> ge = new GenericEntity<Iterable<String>>(result) {};
		logger.debug("Publish start returning GenericEntity type: {}", ge.getType());
		Response response = Response.ok(ge)
				.header("Vary", "Accept")
				.build();
		return response;
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
		itemId.setHostnameOrValidate(this.hostname);
		
		if (profiling != null && profiling.length > 1) {
			throw new IllegalArgumentException("Field 'profiling': multiple profiling parameters is currently not supported");
		}
		
		
		// The Publish Package requires itemId/masterId with pegrev.
		// This service will get relatively high load so it is better to avoid an additional indexing request, i.e. require pegrev.
		PublishPackage publishPackage = getPublishPackage(itemId, includeRelease, includeTranslations, profiling, publication);
		
		// Avoid displaying publish status for a previous revision of the item, refuse if non-latest revision requested.
		CmsItemLookup itemLookup = this.lookupMap.get(itemId.getRepository());
		CmsItem itemHead = itemLookup.getItem(itemId.withPegRev(null));
		if (itemHead.getRevisionChanged().getNumber() != itemId.getPegRev().longValue()) {
			String msg = MessageFormatter.format("Publish status requested for non-latest revision of Release document, try reloading the page. The latest revision is {}.", itemHead.getRevisionChanged().getNumber()).getMessage();
			logger.warn(msg);
			throw new IllegalArgumentException(msg);
		}
		
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
		itemId.setHostnameOrValidate(this.hostname);
		
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
	
	private PublishPackage getPublishPackage(CmsItemId itemId, boolean includeRelease, boolean includeTranslations, String[] profiling, String publication) throws Exception {
		LinkedHashSet<String> profilingSet = null;
		if (profiling != null && profiling.length > 0) {
			profilingSet = new LinkedHashSet<>();
			for (String p: profiling) {
				profilingSet.add(p);
			}
		}
		return this.packageFactory.get(itemId.getRepository()).getPublishPackage(itemId, publication, includeRelease, includeTranslations, profilingSet);
	}
	
	
	
}
