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

import se.simonsoft.cms.publish.databinds.publish.config.PublishConfigOptions;

public class PublishJobOptions extends PublishConfigOptions {
	private String pathname;
	private String source;
	private PublishJobProfiling profiling;
	private PublishJobStorage storage;
	private PublishJobPostProcess postprocess;
	private PublishJobProgress progress;
	private PublishJobDelivery delivery;

	public String getSource() {
		return source;
	}
	public void setSource(String source) {
		this.source = source;
	}
	public PublishJobProfiling getProfiling() {
		return profiling;
	}
	public void setProfiling(PublishJobProfiling profiling) {
		this.profiling = profiling;
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
