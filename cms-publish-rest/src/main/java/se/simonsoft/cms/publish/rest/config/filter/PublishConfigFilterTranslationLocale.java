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

import java.util.List;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;

/**
 * Filters Translations on TranslationLocale (#1683).
 * Items that are not Translations are always accepted.
 * A null or empty translationLocaleInclude accepts all locales.
 */
public class PublishConfigFilterTranslationLocale implements PublishConfigFilter {

	@Override
	public boolean accept(PublishConfig config, CmsItem item) {
		
		if (!(item instanceof CmsItemPublish)) {
			return false;
		}
		
		CmsItemPublish itemPublish = (CmsItemPublish) item;
		
		if (!itemPublish.isTranslation()) {
			return true; // Only Translations are filtered.
		}
		
		return isLocaleIncluded(config, itemPublish.getTranslationLocale());
	}
	
	
	public static boolean isLocaleIncluded(PublishConfig config, String locale) {
		
		List<String> include = config.getTranslationLocaleInclude();
		if (include == null || include.isEmpty()) {
			return true;
		}
		if (locale == null) {
			return false; // Translation without locale is not included when a list is configured.
		}
		return include.contains(locale);
	}

}
