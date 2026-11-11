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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ClientProxy;
import io.quarkus.test.component.QuarkusComponentTest;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsItemKind;
import se.simonsoft.cms.item.config.CmsConfigOption;
import se.simonsoft.cms.item.config.CmsResourceContext;
import se.simonsoft.cms.item.impl.CmsConfigOptionBase;
import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.item.info.CmsRepositoryLookup;
import se.simonsoft.cms.publish.config.PublishConfiguration;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;
import se.simonsoft.cms.publish.rest.config.PublishProducer;

/** Real service, filters and JSON parsing; only the application's repository boundary is mocked. */
@QuarkusComponentTest({PublishConfigurationDefault.class, PublishProducer.class, PublishConfigurationCdiTest.Dependencies.class})
class PublishConfigurationCdiTest {

	@Inject PublishConfiguration configuration;
	@Inject PublishConfigurationDefault implementation;
	@Inject Dependencies dependencies;

	@BeforeEach
	void resetLookups() {
		reset(dependencies.globalLookup, dependencies.repositoryLookup);
	}

	@Test
	void usesGlobalLookupAndParentHeadWithOneServicePerRequest() {
		var beans = Arc.container().beanManager().getBeans(PublishConfiguration.class);
		assertEquals(1, beans.size());
		assertEquals(PublishConfigurationDefault.class, beans.iterator().next().getBeanClass());
		assertEquals(RequestScoped.class, beans.iterator().next().getScope());
		when(dependencies.globalLookup.getConfig(any(), eq(CmsItemKind.Folder))).thenAnswer(invocation -> {
			CmsItemId folder = invocation.getArgument(0);
			return context(folder, Map.of("pdf", rule(true, true, folder.getRepository().getName())));
		});

		var request = Arc.container().requestContext();
		var outer = request.isActive() ? request.getState() : null;
		if (outer != null) {
			request.deactivate();
		}
		try {
			Object previous = null;
			for (String repository : new String[]{"repo-a", "repo-b"}) {
				request.activate();
				try {
					Object current = ClientProxy.unwrap(configuration);
					assertSame(current, ClientProxy.unwrap(implementation));
					assertNotSame(previous, current);
					assertEquals(repository, configuration.getConfiguration(item(repository).getId()).get("pdf").getDescription());
					previous = current;
				} finally {
					request.terminate();
				}
			}
		} finally {
			if (outer != null) {
				request.activate(outer);
			}
		}
		var folders = ArgumentCaptor.forClass(CmsItemId.class);
		verify(dependencies.globalLookup, times(2)).getConfig(folders.capture(), eq(CmsItemKind.Folder));
		assertEquals(Set.of("repo-a", "repo-b"), folders.getAllValues().stream()
				.map(folder -> folder.getRepository().getName()).collect(java.util.stream.Collectors.toSet()));
		for (CmsItemId folder : folders.getAllValues()) {
			assertEquals("/documents", folder.getRelPath().getPath());
			assertNull(folder.getPegRev());
			assertEquals("public.example", folder.getRepository().getHost());
		}
		verifyNoInteractions(dependencies.repositoryLookup);
	}

	@Test
	void keepsActiveVisibleAndCommonFiltersSeparateAndReadsFreshConfiguration() {
		CmsItemPublish item = item("repo-a");
		CmsItemId itemId = item.getId();
		when(dependencies.globalLookup.getConfig(any(), eq(CmsItemKind.Folder))).thenReturn(context(itemId, Map.of(
				"active", rule(true, false, "active"),
				"visible", rule(false, true, "visible"),
				"neither", rule(false, false, "neither"),
				"blocked", "{\"active\":true,\"visible\":true,\"areaMainInclude\":true,\"prereleaseInclude\":true,\"pathNameBaseInclude\":\"other\"}")));
		assertEquals(Set.of("active", "visible", "neither", "blocked"), configuration.getConfiguration(item.getId()).keySet());
		assertEquals(Set.of("active", "visible", "neither"), configuration.getConfigurationFiltered(item).keySet());
		assertEquals(Set.of("active"), configuration.getConfigurationActive(item).keySet());
		assertEquals(Set.of("visible"), configuration.getConfigurationVisible(item).keySet());
		assertEquals(Set.of("active", "visible", "neither"), configuration.getConfigurationFiltered(item).keySet());
		var first = configuration.getConfiguration(item.getId()).get("visible");
		first.setVisible(false);
		var next = configuration.getConfiguration(item.getId()).get("visible");
		assertNotSame(first, next);
		assertTrue(next.isVisible());
		verifyNoInteractions(dependencies.repositoryLookup);
	}

	@Test
	void preservesLenientActiveAndStrictParsingAndRecoversAfterFailure() {
		CmsItemPublish item = item("repo-a");
		CmsItemId itemId = item.getId();
		when(dependencies.globalLookup.getConfig(any(), eq(CmsItemKind.Folder))).thenReturn(context(itemId, Map.of(
				"valid", rule(true, true, "valid"), "broken", "{invalid")));
		assertEquals(Set.of("valid"), configuration.getConfigurationActive(item).keySet());
		assertTrue(assertThrows(RuntimeException.class, () -> configuration.getConfiguration(item.getId()))
				.getMessage().startsWith("Illegal publish configuration 'cmsconfig-publish:broken':"));
		assertThrows(RuntimeException.class, () -> configuration.getConfigurationFiltered(item));
		assertThrows(RuntimeException.class, () -> configuration.getConfigurationVisible(item));
		when(dependencies.globalLookup.getConfig(any(), eq(CmsItemKind.Folder))).thenReturn(context(itemId,
				Map.of("valid", rule(true, true, "recovered"))));
		assertEquals("recovered", configuration.getConfigurationVisible(item).get("valid").getDescription());
		verifyNoInteractions(dependencies.repositoryLookup);
	}

	private CmsItemPublish item(String repository) {
		CmsItemPublish item = mock(CmsItemPublish.class);
		when(item.getId()).thenReturn(new CmsItemIdArg("x-svn://public.example/svn/" + repository + "^/documents/doc.xml?p=17"));
		when(item.getMeta()).thenReturn(Map.of());
		return item;
	}

	private CmsResourceContext context(CmsItemId folder, Map<String, String> rules) {
		Map<String, CmsConfigOption> options = new HashMap<>();
		rules.forEach((name, json) -> options.put(name, new CmsConfigOptionBase<>("cmsconfig-publish:" + name, json)));
		return new CmsResourceContext(folder, options);
	}

	private String rule(boolean active, boolean visible, String description) {
		return """
				{"active":%s,"visible":%s,"description":"%s","areaMainInclude":true,"prereleaseInclude":true}
				""".formatted(active, visible, description);
	}

	@Singleton
	static class Dependencies {
		final CmsRepositoryLookup globalLookup = mock(CmsRepositoryLookup.class);
		final CmsRepositoryLookup repositoryLookup = mock(CmsRepositoryLookup.class);

		@Produces
		@Singleton
		@Named("global")
		CmsRepositoryLookup globalLookup() {
			return globalLookup;
		}

		// A separate default lookup ensures that removing @Named("global") cannot silently pass.
		@Produces
		@Singleton
		CmsRepositoryLookup repositoryLookup() {
			return repositoryLookup;
		}

		@Produces
		@Singleton
		ObjectReader reader() {
			return new ObjectMapper().reader();
		}
	}
}
