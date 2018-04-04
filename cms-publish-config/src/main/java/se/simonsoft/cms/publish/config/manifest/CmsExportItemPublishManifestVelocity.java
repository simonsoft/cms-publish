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

import se.simonsoft.cms.item.export.CmsExportItem;
import se.simonsoft.cms.item.export.CmsExportPath;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfigTemplateString;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobManifest;


/**
 * ExportItem that will use Velocity to parse template from config.
 * Velocity engine is configured with XML escape tool. 
 * The Velocity engine is configured to strict evaluation, requires null checks on maps that could be null.
 * Will not accept null values.
 * @author jandersson
 */
public class CmsExportItemPublishManifestVelocity implements CmsExportItem {
	
	private final PublishJobManifest jobManifest;
	private final String template;
	
	private boolean ready = false;

	public CmsExportItemPublishManifestVelocity(
			PublishJobManifest jobManifest
			) {
		
		// Getting template string before it is removed in forPublish.
		this.template = jobManifest.getTemplate();
		this.jobManifest = jobManifest.forPublish();
	}
	
	@Override
	public void prepare() {
		
        if (ready) {
            throw new IllegalStateException("Export item:" + " Publish manifest velocity" + " is already prepared");
        }
        
        if (template == null) {
        	throw new IllegalStateException("Export item:" + " Publish manifest velocity" + " requires a valid velocity template");
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
		
		
		PublishConfigTemplateString t = new PublishConfigTemplateString();
		
		t.withEntry("jobs", jobManifest.getJob());
		t.withEntry("document", jobManifest.getDocument());
		t.withEntry("master", jobManifest.getMaster());
		t.withEntry("custom", jobManifest.getCustom());
		t.withEntry("meta", jobManifest.getMeta());
		
		String evaluated = t.evaluate(template);
		
		try {
			stream.write(evaluated.getBytes());
		} catch (IOException e) {
			throw new RuntimeException("Could not write Velocity evaluated template string.");
		}
		
	}

	@Override
	public CmsExportPath getResultPath() {
		return null; // CmsExportJobSingle validates that getResultPath() is null.
	}

}
