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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import se.simonsoft.cms.publish.config.databinds.job.item.PublishJobItem;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PublishJobReport3 {
	private Map<String, String> meta = new HashMap<>();
	private List<PublishJobItem> items = new ArrayList<>();
	
	public Map<String, String> getMeta() {
		return meta;
	}
	public void setMeta(Map<String, String> meta) {
		this.meta = meta;
	}
	public List<PublishJobItem> getItems() {
		return items;
	}
	public void setItems(List<PublishJobItem> items) {
		this.items = items;
	}
}