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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;

public class PublishConfigFilterType implements PublishConfigFilter {
	
	private final String fieldNameBook = "embd_xml_a_type";
	private final String fieldNameDita = "meta_s_s_xml_a_othermeta_cms-type";
	
	@Override
	public boolean accept(PublishConfig config, CmsItem item) {
		String fieldValueBook = (String) item.getMeta().get(fieldNameBook);
		String fieldValueDita = (String) item.getMeta().get(fieldNameDita);
		// Support multi-value othermeta.
		List<String> valuesDita = fieldValueDita == null ? new ArrayList<String>(): fieldValueDita.lines().collect(Collectors.toList());
		
		
		boolean accept = false;
		List<String> configInclude = config.getTypeInclude();
		if (configInclude == null || configInclude.contains(fieldValueBook) || !intersect(configInclude, valuesDita).isEmpty()) { 
			accept = true;
		}
		
		return accept;
	}
	
	private Set<String> intersect(List<String> list, List<String> otherList) {
		
		Set<String> result = list.stream()
				  .distinct()
				  .filter(otherList::contains)
				  .collect(Collectors.toSet());
		return result;
	}

}
