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
package se.simonsoft.cms.publish.rest.config;

import java.util.List;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import se.simonsoft.cms.publish.rest.PublishJobFactory;
import se.simonsoft.cms.publish.rest.PublishJobStorageFactory;
import se.simonsoft.cms.publish.rest.config.filter.PublishConfigFilter;
import se.simonsoft.cms.publish.rest.config.filter.PublishConfigFilterAreaMain;
import se.simonsoft.cms.publish.rest.config.filter.PublishConfigFilterElementName;
import se.simonsoft.cms.publish.rest.config.filter.PublishConfigFilterPathext;
import se.simonsoft.cms.publish.rest.config.filter.PublishConfigFilterPathnamebaseRegex;
import se.simonsoft.cms.publish.rest.config.filter.PublishConfigFilterPrerelease;
import se.simonsoft.cms.publish.rest.config.filter.PublishConfigFilterProfiling;
import se.simonsoft.cms.publish.rest.config.filter.PublishConfigFilterStatus;
import se.simonsoft.cms.publish.rest.config.filter.PublishConfigFilterType;

/** Shared factories hold only configuration; each invocation creates its own job state. */
public class PublishProducer {

	@Produces
	@Singleton
	public PublishJobStorageFactory createStorageFactory(PublishRuntimeConfig config) {
		// Preserve the legacy default for both a missing and an empty bucket setting.
		return new PublishJobStorageFactory(config.cloudId(), config.bucket().orElse("cms-automation"));
	}

	@Produces
	@Singleton
	public PublishJobFactory createJobFactory(PublishRuntimeConfig config, PublishJobStorageFactory storageFactory) {
		return new PublishJobFactory(config.cloudId(), storageFactory);
	}

	@Produces
	@Singleton
	public List<PublishConfigFilter> createFilters() {
		// Preserve PublishModule's order. Active and Visible are added by PublishConfigurationDefault.
		return List.of(new PublishConfigFilterPathnamebaseRegex(), new PublishConfigFilterPathext(),
				new PublishConfigFilterPrerelease(), new PublishConfigFilterAreaMain(),
				new PublishConfigFilterStatus(), new PublishConfigFilterElementName(),
				new PublishConfigFilterType(), new PublishConfigFilterProfiling());
	}
}
