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
package se.simonsoft.cms.publish.worker.export;

import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.cms.item.export.CmsExportItem;
import se.simonsoft.cms.item.export.CmsExportPath;
import se.simonsoft.cms.publish.PublishException;
import se.simonsoft.cms.publish.PublishTicket;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobOptions;
import se.simonsoft.cms.publish.worker.PublishJobService;

public class CmsExportItemPublish implements CmsExportItem {
	
	private final PublishTicket ticket;
	private final PublishJobService publishJobService;
	private final CmsExportPath cmsExportPath;
	
	private boolean ready = false;
	private PublishJobOptions options;
	
	private static final Logger logger = LoggerFactory.getLogger(CmsExportItemPublish.class);
	
	public CmsExportItemPublish(PublishTicket ticket, PublishJobOptions options, PublishJobService publishJobService, CmsExportPath cmsExportPath) {

		if (ticket == null) {
			throw new IllegalArgumentException("PublishExportItem ticket must not be null.");
		}
		if (options == null) {
			throw new IllegalArgumentException("PublishExportItem options must not be null.");
		}
		
		this.ticket =  ticket;
		this.options = options;
		this.publishJobService = publishJobService;
		this.cmsExportPath = cmsExportPath;		
	}

	@Override
	public Long prepare() {
		
		if (this.ready) {
			throw new IllegalStateException("Export item:" + "PublishExportItem" + " is already prepared");
		}
		logger.debug("PublishExport item with ticket: {} is prepared for export", ticket);
		this.ready = true;
		return null; // Could size be determined via a HEAD request to the publish service?
	}

	@Override
	public Boolean isReady() {
		return this.ready;
	}

	@Override
	public void getResultStream(OutputStream stream) {
		try {
			this.publishJobService.getCompletedJob(options, ticket, stream);
		} catch (IOException | PublishException e) {
			logger.error("Failed when writing publish job. {}", e.getMessage());
			throw new RuntimeException(e);
		}
	}

	@Override
	public CmsExportPath getResultPath() {
		return this.cmsExportPath;
	}

}

