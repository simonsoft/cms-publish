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
package se.simonsoft.cms.publish.rest.command;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.command.CommandRuntimeException;
import se.simonsoft.cms.item.command.ExternalCommandHandler;
import se.simonsoft.cms.item.export.CmsExportJob;
import se.simonsoft.cms.item.export.CmsExportProvider;
import se.simonsoft.cms.item.export.CmsExportWriter;
import se.simonsoft.cms.publish.config.cdn.PublishCdnConfig;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobManifest;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobOptions;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobPreProcess;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobProgress;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobStorage;
import se.simonsoft.cms.publish.config.export.PublishExportJobFactory;
import se.simonsoft.cms.publish.rest.cdn.PublishCdnSearchApiKeyGeneratorAlgolia;
import se.simonsoft.cms.release.export.ReleaseExportOptions;
import se.simonsoft.cms.release.export.ReleaseExportService;

public class PublishPreprocessCommandHandler implements ExternalCommandHandler<PublishJobOptions> {

	private final CmsExportProvider exportProvider;
	private final Map<CmsRepository, ReleaseExportService> exportServices;
	private final PublishCdnSearchApiKeyGeneratorAlgolia cdnSearchKeyGenerator;
	private final ObjectWriter writerPublishManifest;
	private final ObjectWriter writerJobProgress;

	// Consider injecting list of potential secondary artifacts.
	private final Set<String> secondaryExportArtifacts = new HashSet<String>(Arrays.asList("algolia", "graphics"));
	
	private static final Logger logger = LoggerFactory.getLogger(PublishPreprocessCommandHandler.class);

	@Inject
	public PublishPreprocessCommandHandler(
			@Named("config:se.simonsoft.cms.publish.export") CmsExportProvider exportProvider, 
			Map<CmsRepository, ReleaseExportService> exportServices,
			PublishCdnConfig cdnConfig,
			ObjectWriter objectWriter
			) {

		this.exportProvider = exportProvider;
		this.exportServices = exportServices;
		this.cdnSearchKeyGenerator = new PublishCdnSearchApiKeyGeneratorAlgolia(cdnConfig);
		this.writerPublishManifest = objectWriter.forType(PublishJobManifest.class).withDefaultPrettyPrinter();
		this.writerJobProgress = objectWriter.forType(PublishJobProgress.class);
	}

	@Override
	public Class<PublishJobOptions> getArgumentsClass() {
		return PublishJobOptions.class;
	}
	
	@Override
	public String handleExternalCommand(CmsItemId itemId, PublishJobOptions options) {

		final PublishJobPreProcess preprocess = options.getPreprocess();
		if (preprocess == null) {
			throw new IllegalArgumentException("Need a valid PublishJobPreProcess object.");
		}

		final PublishJobStorage storage = options.getStorage();
		if (storage == null) {
			throw new IllegalArgumentException("Need a valid PublishJobStorage object.");
		}

		if (preprocess.getType() == null) {
			throw new IllegalArgumentException("Need a valid PublishJobPreProcess object with type attribute.");
		} else if (preprocess.getType().startsWith("webapp-export")) {
			logger.debug("Performing Webapp Export: {}", itemId);
			doWebappExport(itemId, options);
		} else {
			throw new IllegalArgumentException("Unsupported Preprocess type: " + preprocess.getType());
		}

		final PublishJobProgress progress = options.getProgress();
		
		try {
			return writerJobProgress.writeValueAsString(progress);
		} catch (JsonProcessingException e) {
			throw new CommandRuntimeException("JsonProcessingException", e);
		}
	}

	// Test coverage: public and returning CmsExportWriter enables integration testing.
	public LinkedList<CmsExportWriter> doWebappExport(CmsItemId itemId, PublishJobOptions options) {
		final PublishJobPreProcess preprocess = options.getPreprocess();
		final PublishJobStorage storage = options.getStorage();
		final PublishJobManifest manifest = options.getManifest();
		
		String tagStep = "preprocess";
		String tagCdn = ""; // Value length 0 is allowed.

		if (options.getType() == null) {
			throw new IllegalArgumentException("Unsupported job type: must not be null");
		}
		
		if (storage.getType() != null && !storage.getType().equals("s3")) {
			throw new IllegalArgumentException("Unsupported storage type: " + storage.getType());
		}

		// Storing with extension: .preprocess.zip
		// Unless options.type = "none", then export final output: .zip
		// Unrelated - Multi-step engines: .progress.zip
		String pathext = "preprocess.zip";
		if (options.getType().equals("none") || options.getType().equals("export")) {
			// There is no engine stage in this publish config, storing as publish result.
			pathext = "zip";
			tagStep = "engine";
		}
		
		// Cdn tagging and Algolia search apikey
		if (manifest != null && manifest.getCustom() != null && manifest.getCustom().containsKey("cdn")) {
			tagCdn = manifest.getCustom().get("cdn");
			// #1644: Provide Algolia config in the manifest.
			// Manifest changes will be transient (not visible in Step Functions).
			String appId = this.cdnSearchKeyGenerator.getSearchAppId(tagCdn);
			if (appId != null) {
				String docno = manifest.getDocument().get("docno");
				String apiKey = this.cdnSearchKeyGenerator.getSearchApiKeyDocument(tagCdn, docno);
				manifest.getCustom().put("cdn-search-appid", appId);
				manifest.getCustom().put("cdn-search-apikey", apiKey);
			}
		}
		
		// Use preprocess options to populate the Export Options.
		ReleaseExportOptions exportOptions = new ReleaseExportOptions(preprocess.getParams());
		// Set default values for known export types (unless already defined);
		String type = preprocess.getType(); // Multiple preprocess types (subtypes) get routed here.
		if ("webapp-export-abxpe".equals(type)) {
			setExportOptionsDefaultAbxpe(exportOptions);
		} else if ("webapp-export-ditaot".equals(type)) {
			setExportOptionsDefaultDitaot(exportOptions);
		}
		
		// Manifest available to XSL transforms.
		String manifestPathext = "json";
		if (manifest != null) {
			if (manifest.getPathext() != null) {
				manifestPathext = manifest.getPathext(); 
			}
			try {
				exportOptions.setManifest(this.writerPublishManifest.writeValueAsString(manifest));
			} catch (JsonProcessingException e) {
				String msg = MessageFormatter.format("Failed to serialize manifest during export: {}", e.getMessage(), e).getMessage();
				logger.error(msg);
				// Consider throwing exception specific to Command Handlers.
				throw new IllegalArgumentException(msg);
			}
		}
		
		// Support profiling
		if (exportOptions.getProfilingEnable() && options.getProfiling() != null) {
			options.getProfiling().validateFilter(); // Ensure that filters have not been lost in serialization.
			exportOptions.setProfile(options.getProfiling());
		}
		
		ReleaseExportService exportService = this.exportServices.get(itemId.getRepository());
		LinkedList<CmsExportWriter> result = new LinkedList<CmsExportWriter>();

		CmsExportJob job = PublishExportJobFactory.getExportJobZip(storage, pathext);
		setTags(job, tagStep, tagCdn);
		HashMap<String, CmsExportJob> secondaryJobs = new HashMap<>();
		for (String artifact: this.secondaryExportArtifacts) {
			CmsExportJob sJob = PublishExportJobFactory.getExportJobZip(storage, artifact + ".zip");
			setTags(sJob, tagStep, tagCdn);
			secondaryJobs.put(artifact, sJob);
		}
		secondaryJobs.put("manifest", PublishExportJobFactory.getExportJobSingle(storage, manifestPathext));
		
		// Export service
		exportService.exportRelease(itemId, exportOptions, job, secondaryJobs);
		
		// Export secondary jobs first, ensures the export is not considered success unless all jobs succeed.
		// Secondary exports cannot be accessed easily by caller (not part of return value). Intended for automation. 
		result.addAll(doExportSecondaryJobs(secondaryJobs, this.exportProvider));
		
		// Export primary job.
		job.prepare();
		logger.debug("Preparing writer for export...");
		CmsExportWriter exportWriter = this.exportProvider.getWriter();
		exportWriter.prepare(job);
		logger.debug("Uploading export to S3 at path: {}", job.getJobPath());
		exportWriter.write();
		logger.debug("Uploaded export to S3 at path: {}", job.getJobPath());
		result.add(0, exportWriter); // Primary job writer is always first in the list.
		return result;
	}

	
	/** Export secondary artifacts / zip files (graphics, search indexing, ...).
	 * @param secondaryJobs
	 * @param exportProvider
	 */
	public static LinkedList<CmsExportWriter> doExportSecondaryJobs(HashMap<String, CmsExportJob> secondaryJobs, CmsExportProvider exportProvider) {
		LinkedList<CmsExportWriter> result = new LinkedList<CmsExportWriter>();
		for (String artifact: secondaryJobs.keySet()) {
			CmsExportJob job = secondaryJobs.get(artifact);
			if (!job.isEmpty()) {
				logger.debug("Preparing export of secondary: {}", artifact);
				job.prepare();
				CmsExportWriter exportWriter = exportProvider.getWriter();
				logger.debug("Preparing writer of secondary: {}", artifact);
				exportWriter.prepare(job);
				logger.debug("Writing export of secondary: {}", artifact);
				exportWriter.write();
				logger.debug("Exported secondary: {}", artifact);
				result.add(exportWriter);
			}
		}
		return result;
	}

	
	private void setExportOptionsDefaultAbxpe(ReleaseExportOptions options) {
		// Set a name that is unlikely to collide with sections.
		if (options.getDocumentNameBase() == null) {
			options.setDocumentNameBase("_document");
		}
		// Always use .xml, for both book and ditamap. Avoids logic in worker-webapp.
		if (options.getDocumentExtension() == null) {
			options.setDocumentExtension("xml");
		}
		// Split ditabase into topics, likely faster to process in PE.
		// The XSL default is now to split, can be overridden here if faster to process in PE (including export, unzip and clean-up).
		// Split ditabase is likely better from a support-perspective if sending data to PTC.
		
		// Profiling is enabled by default because it is often more efficient (excludes filtered graphics).
		// Can disable profiling with "ProfilingEnable": false in order to filter on PE (as with adapter).
		
		// #1529: Fully resolve and remove @keyref in order to support PE without adapter (will otherwise overwrite with empty key text).
		if (options.getKeyrefKeywordOutput() == null) {
			options.setKeyrefKeywordOutput("resolve");
		}
	}
	
	private void setExportOptionsDefaultDitaot(ReleaseExportOptions options) {
		// DITA-OT container now expects _document.ditamap (initially site.ditamap).
		// No risk for collision with topics due to different extensions.
		// Risk for collision with sub-maps / keydef-maps?
		if (options.getDocumentNameBase() == null) {
			options.setDocumentNameBase("_document");
		}
		// Always use .ditamap, for both book and ditamap. Book needs XSL transform into DITA.
		if (options.getDocumentExtension() == null) {
			options.setDocumentExtension("ditamap");
		}
		// Split ditabase into topics, required by DITA-OT.
		// The XSL default is now to split.
		
		// Export graphics to secondary artifact.
		if (options.getGraphicsSecondary() == null) {
			options.setGraphicsSecondary(true);
		}
		
		// Suppress keyref attributes for resolved keywords.
		if (options.getKeyrefKeywordOutput() == null) {
			options.setKeyrefKeywordOutput("resolve");
		}
		
		// Resolve keyref attributes for href.
		if (options.getKeyrefHrefOutput() == null) {
			options.setKeyrefHrefOutput("resolve");
		}
		
		// Suppress all cms-namespace attributes, not allowed by DITA-OT. Revision metadata must be set in other attributes.
		if (options.getCmsAttributesSuppressAll() == null) {
			options.setCmsAttributesSuppressAll(true);
		}
	}
	
	private void setTags(CmsExportJob job, String tagStep, String tagCdn) {
		job.withTagging("PublishStep", tagStep);
		job.withTagging("PublishCdn", tagCdn);
	}

}
