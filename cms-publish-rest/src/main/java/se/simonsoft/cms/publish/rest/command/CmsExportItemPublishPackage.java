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

import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.cms.item.export.CmsExportItem;
import se.simonsoft.cms.item.export.CmsExportPath;
import se.simonsoft.cms.publish.rest.PublishPackage;
import se.simonsoft.cms.publish.rest.PublishPackageZipBuilder;

public class CmsExportItemPublishPackage implements CmsExportItem {

	private final PublishPackage publishPackage;
	private final PublishPackageZipBuilder publishPackageZip;
	
	private boolean ready = false;
	
	private Logger logger = LoggerFactory.getLogger(CmsExportItemPublishPackage.class);
	
	public CmsExportItemPublishPackage(PublishPackage publishPackage, PublishPackageZipBuilder publishPackageZip) {
		this.publishPackage = publishPackage;
		this.publishPackageZip = publishPackageZip;
	}

	
	@Override
	public void prepare() {
        if (ready) {
            throw new IllegalStateException("Export item:" + "Publish package" + " is already prepared");
        }
		this.ready = true;
	}

	
    @Override
    public Boolean isReady() {
        return this.ready ;
    }

    
	@Override
	public void getResultStream(OutputStream stream) {
        if (!ready) {
            throw new IllegalStateException("Export item is not ready for export");
        }
		
        logger.info("Publish Package processing...");
		this.publishPackageZip.getZip(this.publishPackage, stream);
		logger.info("Publish Package processed.");
	}

	
	@Override
	public CmsExportPath getResultPath() {
		return null; // CmsExportJobSingle validates that getResultPath() is null.
	}

}
