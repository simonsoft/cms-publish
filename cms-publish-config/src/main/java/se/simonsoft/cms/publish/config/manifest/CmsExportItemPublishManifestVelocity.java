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
import se.simonsoft.cms.publish.config.PublishConfigTemplateString;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobManifest;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobOptions;


/**
 * ExportItem that will use Velocity template from config to serialize the manifest.
 * Velocity engine is configured with XML escape tool.
 * The Velocity engine is configured to strict evaluation, requires null checks on maps that could be null.
 * Will not accept that a key is mapped to a null value, if there is a chance for this it has to be handled with null checks in template.
 * @deprecated #1707 Replaced by XSL-generated manifest.
 * @author jandersson
 */
public class CmsExportItemPublishManifestVelocity implements CmsExportItem {
	
	private final PublishJobOptions jobOptions;
	private final PublishJobManifest jobManifest;
	private final String template;
	
	private boolean ready = false;

	public CmsExportItemPublishManifestVelocity(
			PublishJobOptions jobOptions
			) {
		
		this.jobOptions = jobOptions;
		// Getting template string before it is removed in forPublish.
		this.template = jobOptions.getManifest().getTemplate();
		this.jobManifest = jobOptions.getManifest().forPublish();
	}
	
	@Override
	public Long prepare() {
		
        if (ready) {
            throw new IllegalStateException("Export item:" + " Publish manifest velocity" + " is already prepared");
        }
        
        if (template == null) {
        	throw new IllegalStateException("Export item:" + " Publish manifest velocity" + " requires a valid velocity template");
        }

		this.ready = true;
		return null; // Size is unknown.
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
		
		t.withEntry("job", jobManifest.getJob());
		t.withEntry("document", jobManifest.getDocument());
		t.withEntry("master", jobManifest.getMaster());
		t.withEntry("custom", jobManifest.getCustom());
		t.withEntry("meta", jobManifest.getMeta());
		
		// Providing the storage object for custom manifests.
		// Using leading uppercase to distinguish from objects inside the manifest.
		t.withEntry("Storage", jobOptions.getStorage());
		
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
