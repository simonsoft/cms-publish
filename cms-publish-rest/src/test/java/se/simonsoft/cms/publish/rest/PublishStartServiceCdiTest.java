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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ClientProxy;
import io.quarkus.test.component.QuarkusComponentTest;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.publish.config.PublishConfiguration;
import se.simonsoft.cms.publish.config.PublishExecutor;
import se.simonsoft.cms.publish.config.command.PublishWebhookCommandHandler;
import se.simonsoft.cms.publish.rest.config.PublishProducer;
import se.simonsoft.cms.release.translation.TranslationTracking;
import se.simonsoft.cms.reporting.CmsItemLookupReporting;

/** Service lifecycle only; repository, translation, webhook and execution are explicit test boundaries. */
@QuarkusComponentTest({PublishStartService.class, PublishProducer.class, PublishStartServiceCdiTest.Dependencies.class})
class PublishStartServiceCdiTest {

	@Inject PublishStartService service;
	@Inject Dependencies dependencies;

	@Test
	void usesOneInstancePerRequestAndReleasesItAfterFailure() {
		var beans = Arc.container().beanManager().getBeans(PublishStartService.class);
		assertEquals(1, beans.size());
		assertEquals(PublishStartService.class, beans.iterator().next().getBeanClass());
		assertEquals(RequestScoped.class, beans.iterator().next().getScope());
		var context = Arc.container().requestContext();
		var caller = context.getStateIfActive();
		context.deactivate();
		try {
			Object previous = null;
			for (String repo : new String[] {"alpha", "beta"}) {
				context.activate();
				try {
					Object current = ClientProxy.unwrap(service);
					assertSame(current, ClientProxy.unwrap(Arc.container().instance(PublishStartService.class).get()));
					assertNotSame(previous, current);
					var item = new CmsItemIdArg("x-svn://public.example/svn/" + repo + "/doc.xml?p=1");
					var options = new PublishStartOptions();
					options.setPublication("missing");
					when(dependencies.configuration.getConfiguration(item)).thenReturn(java.util.Map.of());
					assertEquals("Publish config does not exist: missing", assertThrows(IllegalArgumentException.class,
							() -> service.doPublishStartItem(item, options)).getMessage());
					verify(dependencies.configuration).getConfiguration(item);
					assertSame(current, ClientProxy.unwrap(service));
					previous = current;
				} finally {
					context.terminate();
				}
			}
			verifyNoInteractions(dependencies.lookup, dependencies.translations, dependencies.executor, dependencies.webhook);
		} finally {
			if (caller != null) {
				context.activate(caller);
			}
		}
	}

	@Test
	void requiresAnActiveRequestBeforeCallingCollaborators() {
		var context = Arc.container().requestContext();
		var caller = context.getStateIfActive();
		context.deactivate();
		try {
			assertThrows(ContextNotActiveException.class, () -> service.doPublishStartItem(
					new CmsItemIdArg("x-svn://public.example/svn/alpha/doc.xml?p=1"), new PublishStartOptions()));
			verifyNoInteractions(dependencies.configuration, dependencies.lookup, dependencies.translations,
					dependencies.executor, dependencies.webhook);
		} finally {
			if (caller != null) {
				context.activate(caller);
			}
		}
	}

	@Singleton
	static class Dependencies {
		final PublishConfiguration configuration = mock(PublishConfiguration.class);
		final CmsItemLookupReporting lookup = mock(CmsItemLookupReporting.class);
		final TranslationTracking translations = mock(TranslationTracking.class);
		final PublishExecutor executor = mock(PublishExecutor.class);
		final PublishWebhookCommandHandler webhook = mock(PublishWebhookCommandHandler.class);
		final ObjectMapper mapper = new ObjectMapper();

		@Produces @Singleton
		PublishConfiguration configuration() { return configuration; }
		@Produces @Singleton
		CmsItemLookupReporting lookup() { return lookup; }
		@Produces @Singleton
		TranslationTracking translations() { return translations; }
		@Produces @Singleton
		PublishExecutor executor() { return executor; }
		@Produces @Singleton
		PublishWebhookCommandHandler webhook() { return webhook; }
		@Produces @Singleton
		ObjectReader reader() { return mapper.reader(); }
		@Produces @Singleton
		ObjectWriter writer() { return mapper.writer(); }
	}
}
