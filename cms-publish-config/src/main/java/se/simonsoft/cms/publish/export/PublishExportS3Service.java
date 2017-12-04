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
package se.simonsoft.cms.publish.export;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;

import javax.inject.Inject;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.cms.export.storage.CmsExportAwsWriterSingle;
import se.simonsoft.cms.item.export.CmsExportItemInputStream;
import se.simonsoft.cms.item.export.CmsExportPath;
import se.simonsoft.cms.item.export.CmsExportWriter;
import se.simonsoft.cms.publish.databinds.publish.job.PublishJobOptions;

public class PublishExportS3Service implements PublishJobExportService {
	
	private final CmsExportWriter writer;
	private final String jobExtension = "zip";
	
	
	private static final Logger logger = LoggerFactory.getLogger(PublishExportS3Service.class);
	
	@Inject
	public PublishExportS3Service(CmsExportAwsWriterSingle writer) { 
		this.writer = writer;
	}

	@Override
	public String exportJob(OutputStream os, PublishJobOptions jobOptions) {
		logger.debug("Preparing publishJob {} for export to s3", jobOptions.getPathname());
		
		PublishExportJob job = new PublishExportJob(jobOptions.getStorage(), this.jobExtension);
		ByteArrayOutputStream baos = (ByteArrayOutputStream) os;
		CmsExportItemInputStream exportItem = new CmsExportItemInputStream(new ByteArrayInputStream(baos.toByteArray()), new CmsExportPath("/".concat(jobOptions.getStorage().getPathnamebase()))); //TODO Is the item path correct this way?
		job.addExportItem(exportItem);
		job.prepare();
		
		
		logger.debug("Prepareing writer for export...");
		
		if (job.isReady()) {
			writer.prepare(job);
			if (writer.isReady()) {
				logger.debug("Writer is prepared. Writing job to S3.");
				writer.write();
			}
		}
		
		logger.debug("Job has been exported to S3.");
		return job.getJobPath();
	}

}
