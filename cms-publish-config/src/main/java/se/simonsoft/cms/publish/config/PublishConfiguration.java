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
package se.simonsoft.cms.publish.config;

import java.util.Map;

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingSet;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;

public interface PublishConfiguration {

	/**
	 * @param itemId
	 * @return all publish configurations on the parent folder of itemId
	 */
	public Map<String, PublishConfig> getConfiguration(CmsItemId itemId);
	
	/**
	 * @param item
	 * @return the publish configurations applicable to item
	 */
	public Map<String, PublishConfig> getConfigurationFiltered(CmsItemPublish item);

	/**
	 * @param item
	 * @return the active publish configurations applicable to item
	 */
	public Map<String, PublishConfig> getConfigurationActive(CmsItemPublish item);
	
	/**
	 * @param item
	 * @return the visible publish configurations applicable to item
	 */
	public Map<String, PublishConfig> getConfigurationVisible(CmsItemPublish item);

	/**
	 * @param itemPublish
	 * @return profiling recipes defined on the item and intended for publish
	 */
	public PublishProfilingSet getItemProfilingSet(CmsItemPublish itemPublish);
	
	
	/**
	 * @param itemid
	 * @return TranslationLocalesMapping for RFC locale / lang labels
	 */
	// TODO: consider implementing CmsLabel and CmsLabelMapping interface / abstract class in cms-item.
	public Object getTranslationLocalesMapping(CmsItemPublish item);
}
