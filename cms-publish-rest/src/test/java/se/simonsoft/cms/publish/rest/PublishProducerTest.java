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
package se.simonsoft.cms.publish.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.Arc;
import io.quarkus.test.component.QuarkusComponentTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfigManifest;
import se.simonsoft.cms.publish.config.databinds.job.PublishJob;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;
import se.simonsoft.cms.publish.rest.config.PublishProducer;
import se.simonsoft.cms.publish.rest.config.filter.PublishConfigFilter;
import se.simonsoft.cms.release.translation.TranslationLocalesMapping;

/** Exercises production producers and config without booting unrelated repository/HTTP/AWS services. */
@QuarkusComponentTest(PublishProducer.class)
class PublishProducerTest {

	@Inject PublishJobFactory jobs;
	@Inject PublishJobStorageFactory storage;
	@Inject List<PublishConfigFilter> filters;

	@Test
	void sharedFactoriesCreateFreshJobStateAndRecoverAfterFailure() throws Exception {
		assertEquals(1, Arc.container().beanManager().getBeans(PublishJobFactory.class).size());
		assertEquals(1, Arc.container().beanManager().getBeans(PublishJobStorageFactory.class).size());
		assertEquals(PublishProducer.class, Arc.container().beanManager().getBeans(PublishJobFactory.class).iterator().next().getBeanClass());
		assertEquals(PublishProducer.class, Arc.container().beanManager().getBeans(PublishJobStorageFactory.class).iterator().next().getBeanClass());
		assertSame(jobs, Arc.container().instance(PublishJobFactory.class).get());
		assertSame(storage, Arc.container().instance(PublishJobStorageFactory.class).get());
		PublishConfig config = configuration();
		config.getAreas().get(0).setDocnoDocumentTemplate("${item.getId().getRelPath().getNameBase()}");
		config.getOptions().setManifest(new PublishConfigManifest());
		config.getOptions().getManifest().getCustomTemplates().put("source", "${item.getId().getLogicalId()}");
		config.getOptions().getManifest().getMetaTemplates().put("cloud", "${cloudid}");
		PublishJob first = job(config, "repo-a", "first", 7);
		PublishJob second = job(config, "repo-b", "second", 8);
		assertSeparateState(first, second);
		var firstManifest = first.getOptions().getManifest();
		var secondManifest = second.getOptions().getManifest();
		assertEquals("first", firstManifest.getDocument().get("docno"));
		assertEquals("second", secondManifest.getDocument().get("docno"));
		assertEquals(first.getItemId().getLogicalId(), firstManifest.getCustom().get("source"));
		assertEquals(second.getItemId().getLogicalId(), secondManifest.getCustom().get("source"));
		firstManifest.getDocument().put("docno", "changed");
		firstManifest.getCustom().put("source", "changed");
		firstManifest.getMeta().put("cloud", "changed");
		assertEquals("second", secondManifest.getDocument().get("docno"));
		assertEquals(second.getItemId().getLogicalId(), secondManifest.getCustom().get("source"));
		assertEquals("publish-test", secondManifest.getMeta().get("cloud"));
		assertEquals(Map.of("source", "${item.getId().getLogicalId()}"),
				config.getOptions().getManifest().getCustomTemplates());
		assertEquals(Map.of("cloud", "${cloudid}"), config.getOptions().getManifest().getMetaTemplates());
		assertEquals("publish-test", first.getOptions().getStorage().getPathcloudid());
		assertEquals("publish-test-bucket", first.getOptions().getStorage().getParams().get("s3bucket"));
		assertEquals("publish-test_first.pdf", first.getOptions().getPathname());
		assertEquals("first_r0000000007", first.getOptions().getStorage().getPathnamebase());
		assertEquals("s3://publish-test-bucket/cms4/publish-test/pdf/documents/first.xml/",
				first.getOptions().getStorage().getParams().get("s3urlbase"));
		assertEquals("s3://publish-test-bucket/cms4/publish-test/pdf/documents/second.xml/",
				second.getOptions().getStorage().getParams().get("s3urlbase"));
		assertTrue(config.getOptions().getStorage().getParams().isEmpty());
		first.getOptions().getStorage().getParams().put("s3bucket", "changed");
		first.getOptions().getManifest().getJob().put("configname", "changed");
		first.setUserId("first-user");
		assertEquals("publish-test-bucket", second.getOptions().getStorage().getParams().get("s3bucket"));
		assertEquals("pdf", second.getOptions().getManifest().getJob().get("configname"));
		assertNull(second.getUserId());

		IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
				() -> job(config, "repo-a", "invalid", null));
		assertTrue(failure.getMessage().startsWith("ItemId must have revision:"));
		PublishJob next = job(config, "repo-a", "next", 9);
		assertSeparateState(first, next);
		assertEquals("publish-test_next.pdf", next.getOptions().getPathname());
		assertEquals("publish-test-bucket", next.getOptions().getStorage().getParams().get("s3bucket"));
		assertNull(next.getUserId());
	}

	@Test
	@Timeout(10)
	void concurrentJobsDoNotShareResults() throws Exception {
		PublishConfig config = configuration();
		CyclicBarrier start = new CyclicBarrier(2);
		try (var executor = Executors.newFixedThreadPool(2)) {
			List<Callable<PublishJob>> tasks = List.of(
					() -> { start.await(); return job(config, "repo-a", "a", 11); },
					() -> { start.await(); return job(config, "repo-b", "b", 12); });
			var results = executor.invokeAll(tasks);
			PublishJob first = results.get(0).get();
			PublishJob second = results.get(1).get();
			assertSeparateState(first, second);
			assertEquals("repo-a", first.getItemId().getRepository().getName());
			assertEquals("repo-b", second.getItemId().getRepository().getName());
			assertEquals("publish-test_a.pdf", first.getOptions().getPathname());
			assertEquals("publish-test_b.pdf", second.getOptions().getPathname());
		}
	}

	@Test
	void preservesFilterOrderWithoutMakingActiveOrVisibleGlobal() {
		assertEquals(List.of("PublishConfigFilterPathnamebaseRegex", "PublishConfigFilterPathext",
				"PublishConfigFilterPrerelease", "PublishConfigFilterAreaMain", "PublishConfigFilterStatus",
				"PublishConfigFilterElementName", "PublishConfigFilterType", "PublishConfigFilterProfiling"),
				filters.stream().map(filter -> filter.getClass().getSimpleName()).toList());
		PublishConfig config = new PublishConfig();
		config.setPrereleaseInclude(true);
		config.setAreaMainInclude(true);
		CmsItemPublish item = item("repo", "doc", 1);
		assertFalse(config.isActive());
		assertFalse(config.isVisible());
		assertTrue(filters.stream().allMatch(filter -> filter.accept(config, item)));
		config.setPathNameBaseInclude("other");
		assertFalse(filters.stream().allMatch(filter -> filter.accept(config, item)));
	}

	private PublishConfig configuration() throws Exception {
		try (var input = getClass().getResourceAsStream("config/filter/publish-config-simple.json")) {
			PublishConfig config = new ObjectMapper().readValue(input, PublishConfig.class);
			config.getAreas().get(0).setPathnameTemplate("${cloudid}_${item.getId().getRelPath().getNameBase()}.pdf");
			return config;
		}
	}

	private PublishJob job(PublishConfig config, String repository, String name, Integer revision) {
		return jobs.getPublishJob(item(repository, name, revision), config, "pdf", null,
				new TranslationLocalesMapping("en-US", "en-US"), Optional.empty(), Optional.empty());
	}

	private CmsItemPublish item(String repository, String name, Integer revision) {
		CmsItemPublish item = mock(CmsItemPublish.class);
		when(item.getId()).thenReturn(new CmsItemIdArg("x-svn://public.example/svn/" + repository
				+ "^/documents/" + name + ".xml" + (revision == null ? "" : "?p=" + revision)));
		when(item.getMeta()).thenReturn(Map.of("count_elements_topic", 1L));
		when(item.getLocale()).thenReturn("en-US");
		return item;
	}

	private void assertSeparateState(PublishJob first, PublishJob second) {
		assertNotSame(first, second);
		assertNotSame(first.getOptions(), second.getOptions());
		assertNotSame(first.getOptions().getStorage(), second.getOptions().getStorage());
		assertNotSame(first.getOptions().getStorage().getParams(), second.getOptions().getStorage().getParams());
		assertNotSame(first.getOptions().getManifest(), second.getOptions().getManifest());
		assertNotSame(first.getOptions().getManifest().getJob(), second.getOptions().getManifest().getJob());
		assertNotSame(first.getOptions().getProgress(), second.getOptions().getProgress());
	}
}
