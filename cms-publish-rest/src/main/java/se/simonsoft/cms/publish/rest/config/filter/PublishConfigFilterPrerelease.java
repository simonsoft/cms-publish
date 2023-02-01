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
package se.simonsoft.cms.publish.rest.config.filter;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.structure.CmsLabelVersion;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;

public class PublishConfigFilterPrerelease implements PublishConfigFilter {

	
	@Override
	public boolean accept(PublishConfig config, CmsItem item) {
		
		if (!(item instanceof CmsItemPublish)) {
			return false;
		}
		
		CmsItemPublish itemPublish = (CmsItemPublish) item;
		
		if (itemPublish.isRelease() || itemPublish.isTranslation()) {
			String releaseLabelStr = itemPublish.getReleaseLabel();
			if (releaseLabelStr == null || releaseLabelStr.isBlank()) {
				return config.isPrereleaseInclude(); // Missing release label is equivalent to being a prerelease.
			}
			CmsLabelVersion label = new CmsLabelVersion(releaseLabelStr);
			if (label.getPrereleaseSegments().isEmpty()) {
				return true; // Always include Release (not a prerelease).
			} else {
				return config.isPrereleaseInclude(); // Include prerelease if configured.
			}
		
		} else {
			return config.isPrereleaseInclude(); // Include Main / Author area if configured, another filter based on isAreaMainInclude()
		}
	}

}
