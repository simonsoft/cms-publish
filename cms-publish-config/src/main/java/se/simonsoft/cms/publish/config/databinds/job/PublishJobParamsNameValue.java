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

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;

import se.simonsoft.cms.publish.config.databinds.config.PublishConfigParameter;

/**
 * Quick hack to serialize parameter collections as JSON array of Name-Value pairs.
 * Improved compatibility with Step Functions JSONPath.
 * 
 * @author takesson
 */
public class PublishJobParamsNameValue extends LinkedHashSet<PublishConfigParameter> {

	private static final long serialVersionUID = 1L;
	

	public PublishJobParamsNameValue(Map<String, String> paramMap) {
		for (Entry<String, String> param: paramMap.entrySet()) {
			this.add(new PublishConfigParameter(param.getKey(), param.getValue()));
		}
	}
	
	// TODO: Make unmodifiable.

}
