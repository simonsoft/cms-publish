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
package se.simonsoft.cms.publish.config.databinds.job;

import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingRecipe;

/**
 * Minimal representation of ProfilingRecipe when serialized in a PublishJob.
 * Created from {@link PublishProfilingRecipe#getPublishJobProfiling()}.
 * 
 * NOTE: Logical expression is decoded in this representation.
 */
public class PublishJobProfiling {

	private String name;
	private String logicalexpr;

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getLogicalexpr() {
		return logicalexpr;
	}
	public void setLogicalexpr(String logicalexpr) {
		this.logicalexpr = logicalexpr;
	}
}