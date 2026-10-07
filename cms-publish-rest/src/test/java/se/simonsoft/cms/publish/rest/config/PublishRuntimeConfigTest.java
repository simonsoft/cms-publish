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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Map;

import io.smallrye.config.ConfigValidationException;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfigBuilder;
import org.junit.jupiter.api.Test;

import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;

class PublishRuntimeConfigTest {

	@Test
	void missingAndEmptyBucketsKeepTheLegacyDefault() {
		assertBucket("cms-automation", Map.of("cms.cloudid", "test-cloud"));
		assertBucket("cms-automation", Map.of("cms.cloudid", "test-cloud", "cms.publish.bucket", ""));
	}

	@Test
	void usesTheConfiguredBucketAndCloudId() {
		assertBucket("custom-bucket", Map.of("cms.cloudid", "test-cloud", "cms.publish.bucket", "custom-bucket"));
	}

	@Test
	void cloudIdHasNoProductionDefault() {
		assertThrows(ConfigValidationException.class, () -> configuration(Map.of()));
		assertThrows(ConfigValidationException.class, () -> configuration(Map.of("cms.cloudid", "")));
	}

	@Test
	void misspelledSettingsAreRejected() {
		assertThrows(ConfigValidationException.class, () -> configuration(
				Map.of("cms.cloudid", "test-cloud", "cms.publish.buket", "wrong")));
	}

	private void assertBucket(String expected, Map<String, String> settings) {
		CmsItemPublish item = mock(CmsItemPublish.class);
		when(item.getId()).thenReturn(new CmsItemIdArg("x-svn://public.example/svn/repo^/documents/doc.xml?p=7"));
		var factory = new PublishProducer().createStorageFactory(configuration(settings));
		var storage = factory.getInstance(null, item, "pdf", null);
		assertEquals("test-cloud", storage.getPathcloudid());
		assertEquals(expected, storage.getParams().get("s3bucket"));
		assertEquals("s3://" + expected + "/cms4/test-cloud/pdf/documents/doc.xml/", storage.getParams().get("s3urlbase"));
	}

	private PublishRuntimeConfig configuration(Map<String, String> settings) {
		return new SmallRyeConfigBuilder()
				.withSources(new PropertiesConfigSource(settings, "publish-config-test", 100))
				.withMapping(PublishRuntimeConfig.class)
				.build().getConfigMapping(PublishRuntimeConfig.class);
	}
}
