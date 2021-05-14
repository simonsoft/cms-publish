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
package se.simonsoft.cms.publish.config.export;

import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.export.CmsExportAccessDeniedException;
import se.simonsoft.cms.item.export.CmsExportJobNotFoundException;
import se.simonsoft.cms.item.export.CmsExportProvider;
import se.simonsoft.cms.item.export.CmsExportReader;
import se.simonsoft.cms.item.export.CmsImportJob;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobOptions;

public class PublishJobResultLookup {
	
	private static final Logger logger = LoggerFactory.getLogger(PublishJobResultLookup.class);
	
	private final CmsExportProvider exportProvider;

	private final String extensionPublishResult = "zip";
	
	public PublishJobResultLookup(
			@Named("config:se.simonsoft.cms.publish.export") CmsExportProvider exportProvider
			) {
		
		this.exportProvider = exportProvider;
	}

	
	public boolean isPublishResultExists(CmsItemId itemId, PublishJobOptions options) {
		return isPublishResultExists(itemId, options, this.extensionPublishResult);
	}
	
	public boolean isPublishResultExists(CmsItemId itemId, PublishJobOptions options, String extension) {
		
		boolean result = false;
		
		CmsImportJob job = PublishExportJobFactory.getImportJobSingle(options.getStorage(), extension);
		// No items, no prepare for CmsImportJob.

		logger.debug("Preparing reader in order to verify that Publish result exists...");
		CmsExportReader exportReader = this.exportProvider.getReader();
		try {
			exportReader.prepare(job);
			// TODO: Can we get the size to verify that the file is not empty?
			result = true;
		} catch (CmsExportAccessDeniedException | CmsExportJobNotFoundException e) {
			logger.info("Publish result missing: {}", itemId);
		} catch (Exception e) {
			logger.warn("Exception when reading Publish result: {}", e.getMessage(), e);
		}
		
		return result;
	}
	
}
