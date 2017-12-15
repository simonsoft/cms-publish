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

public class PublishConfigFilterType implements PublishConfigFilter {
	
	private final String typeInclude = "embd_xml_a_type"; //TODO: Unsure if this is the correct name: type-include embd_xml_a_type
	
	@Override
	public boolean accept(PublishConfig config, CmsItem item) {
		String type = (String) item.getMeta().get(typeInclude);
		
		boolean accept = false;
		List<String> typeInclude = config.getTypeInclude();
		if (typeInclude == null || typeInclude.contains(type)) { 
			accept = true;
		}
		
		return accept;
	}

}
