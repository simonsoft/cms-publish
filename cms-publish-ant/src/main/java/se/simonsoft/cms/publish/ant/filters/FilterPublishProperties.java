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
package se.simonsoft.cms.publish.ant.filters;

import java.util.List;

import org.apache.tools.ant.Project;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.RepoRevision;

/**
 * Filters what properties to pass on to publishing
 * @author joakimdurehed
 *
 */
public interface FilterPublishProperties {

	/**
	 * Initializes toosl to use in filter
	 * @param itemList
	 * @param headRev
	 * @param project
	 */
	public void initFilter(CmsItem item, RepoRevision headRev, Project project, String publishTarget);
	
	/**
	 * Runs the filter including actually passing properties to publishing
	 */
	public void runFilter();
	
}
