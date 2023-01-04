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

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.command.ExternalCommandHandler;
import se.simonsoft.cms.item.export.CmsExportJobSingle;
import se.simonsoft.cms.item.export.CmsExportProvider;
import se.simonsoft.cms.item.export.CmsExportWriter;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobStorage;
import se.simonsoft.cms.publish.config.export.PublishExportJobFactory;
import se.simonsoft.cms.publish.rest.PublishPackage;
import se.simonsoft.cms.publish.rest.PublishPackageFactory;
import se.simonsoft.cms.publish.rest.PublishPackageOptions;
import se.simonsoft.cms.publish.rest.PublishPackageZipBuilder;

public class PublishPackageCommandHandler implements ExternalCommandHandler<PublishPackageOptions> {

	private final Map<CmsRepository, PublishPackageFactory> packageFactory;
	private final PublishPackageZipBuilder publishPackageZip;
	
	private static final Logger logger = LoggerFactory.getLogger(PublishPackageCommandHandler.class);
	
	
	@Inject
	public PublishPackageCommandHandler(Map<CmsRepository, PublishPackageFactory> packageFactory, PublishPackageZipBuilder publishPackageZip) {
		
		this.packageFactory = packageFactory;
		this.publishPackageZip = publishPackageZip;
	}
	
	
	@Override
	public Class<PublishPackageOptions> getArgumentsClass() {
		return PublishPackageOptions.class;
	}

	@Override
	public String handleExternalCommand(CmsItemId item, PublishPackageOptions arguments) {
		throw new UnsupportedOperationException("Not yet implemented, requires storage provider.");
		// TODO: Need some separate S3 storage with aggressive lifecycle rules.
		// This handler could be interesting in order to move the packaging out of the webapp request handler.
		
		// When implementing, support multiple profiles into the same zip (since the Publish Postprocess mechanism cannot). 
	}

	
	public void handlePackageZip(CmsItemId item, PublishPackageOptions options, PublishJobStorage storage, CmsExportProvider exportProvider) {
		String tagStep = "package";
		PublishPackage publishPackage = this.packageFactory.get(item.getRepository()).getPublishPackage(item, options);
		// TODO: Consider supporting override on the zip prefix boolean.
		CmsExportItemPublishPackage exportItem = new CmsExportItemPublishPackage(publishPackage, this.publishPackageZip);
		
		CmsExportJobSingle job = PublishExportJobFactory.getExportJobSingle(storage, "zip");
		job.addExportItem(exportItem);
		job.withTagging("PublishStep", tagStep);
		//job.withTagging("PublishCdn", tagCdn);
		job.prepare();

		logger.debug("Prepare Package writer for export...");
		CmsExportWriter exportWriter = exportProvider.getWriter();
		exportWriter.prepare(job);
		logger.debug("Prepared Package writer, writing Package to S3.");
		exportWriter.write();
		logger.debug("Package writer completed.");
	}
	
}
