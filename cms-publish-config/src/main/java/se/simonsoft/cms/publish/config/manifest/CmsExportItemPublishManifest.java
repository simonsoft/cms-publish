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
package se.simonsoft.cms.publish.config.manifest;

import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectWriter;

import se.simonsoft.cms.item.export.CmsExportItem;
import se.simonsoft.cms.item.export.CmsExportPath;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobManifest;

public class CmsExportItemPublishManifest implements CmsExportItem {

	private final ObjectWriter writer;
	private final PublishJobManifest jobManifest;
	
	private boolean ready = false;
	
	private Logger logger = LoggerFactory.getLogger(CmsExportItemPublishManifest.class);
	
	public CmsExportItemPublishManifest(ObjectWriter writer, PublishJobManifest jobManifest) {
		
		this.writer = writer.forType(PublishJobManifest.class);
		this.jobManifest = jobManifest.forPublish();
	}

	@Override
	public void prepare() {
		
        if (ready) {
            throw new IllegalStateException("Export item:" + "Publish manifest" + " is already prepared");
        }
		// TODO Can we add jobid here?
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
		
        // TODO: Potentially support Velocity template for custom manifest formats.
		try {
			this.writer.writeValue(stream, this.jobManifest);
		} catch (IOException e) {
			logger.error("Failed to write JSON manifest: " + e.getMessage());
			throw new RuntimeException("Failed to write JSON manifest: " + e.getMessage(), e); 
		}
	}

	@Override
	public CmsExportPath getResultPath() {
		
		throw new IllegalArgumentException("Publish manifest should not be placed inside an archive.");
	}

}
