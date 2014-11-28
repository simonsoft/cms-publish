/**
 * Copyright (C) 2009-2013 Simonsoft Nordic AB
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
package se.simonsoft.cms.publish.ant.nodes;

import java.util.ArrayList;
import java.util.List;

public class FiltersNode {
	
	protected List<FilterNode> filters = new ArrayList<FilterNode>();
	
	public FiltersNode() {
		// TODO Auto-generated constructor stub
	}
	
	public void addFilter(final FilterNode filter) {
		filters.add(filter);
	}

	public List<FilterNode> getFilters() {
		return filters;
	}

	public boolean isValid() {
		if (filters.size() > 0) {
			return true;
		}
		return false;
	}

}
