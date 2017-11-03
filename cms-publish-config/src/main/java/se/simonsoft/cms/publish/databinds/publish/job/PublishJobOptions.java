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
package se.simonsoft.cms.publish.databinds.publish.job;

import java.util.Map;

public class PublishJobOptions {
	private String pathname;
	private String type;
	private String format;
	private Map<String, String> params;
	private PublishJobProfiling profiling;
	private PublishJobReport3 report3;
	private PublishJobStorage storage;
	private PublishJobPostProcess postprocess;
	private PublishJobProgress progress;
	private PublishJobDelivery delivery;

	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getFormat() {
		return format;
	}
	public void setFormat(String format) {
		this.format = format;
	}
	public Map<String, String> getParams() {
		return params;
	}
	public void setParams(Map<String, String> params) {
		this.params = params;
	}
	public PublishJobProfiling getProfiling() {
		return profiling;
	}
	public void setProfiling(PublishJobProfiling profiling) {
		this.profiling = profiling;
	}
	public PublishJobReport3 getReport3() {
		return report3;
	}
	public void setReport3(PublishJobReport3 report3) {
		this.report3 = report3;
	}
	public PublishJobStorage getStorage() {
		return storage;
	}
	public void setStorage(PublishJobStorage storage) {
		this.storage = storage;
	}
	public PublishJobPostProcess getPostprocess() {
		return postprocess;
	}
	public void setPostprocess(PublishJobPostProcess postprocess) {
		this.postprocess = postprocess;
	}
	public String getPathname() {
		return pathname;
	}
	public void setPathname(String pathname) {
		this.pathname = pathname;
	}
	public PublishJobProgress getProgress() {
		return progress;
	}
	public void setProgress(PublishJobProgress progress) {
		this.progress = progress;
	}
	public PublishJobDelivery getDelivery() {
		return delivery;
	}
	public void setDelivery(PublishJobDelivery delivery) {
		this.delivery = delivery;
	}
}
