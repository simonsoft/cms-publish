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
package se.simonsoft.cms.publish.config.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.net.httpserver.HttpServer;

import se.simonsoft.cms.item.command.CommandRuntimeException;
import se.simonsoft.cms.item.export.CmsExportProvider;
import se.simonsoft.cms.item.export.CmsExportReader;
import se.simonsoft.cms.item.export.CmsExportUrlPresigner;
import se.simonsoft.cms.item.export.CmsExportWriter;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobDelivery;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobOptions;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobProgress;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobStorage;

public class PublishWebhookCommandHandlerTest {

	private HttpServer server;
	private final Map<String, String> received = new ConcurrentHashMap<>();
	
	private final CmsExportProvider exportProvider = new CmsExportProvider() {
		@Override public CmsExportReader getReader() { return null; }
		@Override public CmsExportWriter getWriter() { return null; }
		@Override public CmsExportUrlPresigner getUrlPresigner() { return null; }
	};
	
	@Before
	public void setUp() throws Exception {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/hook", exchange -> {
			String auth = exchange.getRequestHeaders().getFirst("Authorization");
			received.put("Authorization", auth == null ? "" : auth);
			exchange.sendResponseHeaders(200, -1);
			exchange.close();
		});
		server.start();
	}
	
	@After
	public void tearDown() {
		server.stop(0);
	}
	
	private String getUrl() {
		return "http://127.0.0.1:" + server.getAddress().getPort() + "/hook";
	}
	
	private PublishJobOptions getOptions(String url, String authorization) {
		PublishJobDelivery delivery = new PublishJobDelivery();
		delivery.setType("webhook");
		Map<String, String> params = new LinkedHashMap<>();
		params.put("url", url);
		delivery.setParams(params);
		Map<String, String> headers = new LinkedHashMap<>();
		headers.put("Authorization", authorization);
		delivery.setHeaders(headers);
		
		PublishJobStorage storage = new PublishJobStorage();
		storage.setType("local");
		PublishJobProgress progress = new PublishJobProgress();
		progress.getParams().put("archive", "file:///archive.zip");
		progress.getParams().put("manifest", "file:///manifest.json");
		
		PublishJobOptions options = new PublishJobOptions();
		options.setDelivery(delivery);
		options.setStorage(storage);
		options.setProgress(progress);
		return options;
	}
	
	private PublishWebhookCommandHandler getHandler(PublishWebhookSecrets secrets) {
		return new PublishWebhookCommandHandler(60L, exportProvider, HttpClient.newHttpClient(), secrets);
	}
	
	@Test
	public void testSecretResolvedInHeader() {
		PublishWebhookSecrets secrets = new PublishWebhookSecretResolverTest.SecretsMap().with("ACME_TOKEN", "s3cr3t", "127.0.0.1");
		PublishJobOptions options = getOptions(getUrl(), "Bearer {secrets:ACME_TOKEN}");
		
		getHandler(secrets).handleExternalCommand(null, options);
		assertEquals("Bearer s3cr3t", received.get("Authorization"));
		// Job options still contain only the placeholder.
		assertEquals("Bearer {secrets:ACME_TOKEN}", options.getDelivery().getHeaders().get("Authorization"));
	}
	
	@Test
	public void testLiteralHeaderUnchanged() {
		getHandler(new PublishWebhookSecretResolverTest.SecretsMap()).handleExternalCommand(null, getOptions(getUrl(), "Bearer literal"));
		assertEquals("Bearer literal", received.get("Authorization"));
	}
	
	@Test
	public void testHostMismatchNotSent() {
		PublishWebhookSecrets secrets = new PublishWebhookSecretResolverTest.SecretsMap().with("ACME_TOKEN", "s3cr3t", "api.example.com");
		try {
			getHandler(secrets).handleExternalCommand(null, getOptions(getUrl(), "Bearer {secrets:ACME_TOKEN}"));
			fail("Expected CommandRuntimeException");
		} catch (CommandRuntimeException e) {
			assertEquals(PublishWebhookSecretResolver.ERROR_NAME, e.getErrorName());
		}
		assertTrue(received.isEmpty());
	}
	
	@Test
	public void testFailureMasked() {
		// Invalid header value (newline) makes the HTTP client throw, message must not contain the secret.
		PublishWebhookSecrets secrets = new PublishWebhookSecretResolverTest.SecretsMap().with("ACME_TOKEN", "s3cr3t\nX-Injected: 1", null);
		try {
			getHandler(secrets).handleExternalCommand(null, getOptions(getUrl(), "Bearer {secrets:ACME_TOKEN}"));
			fail("Expected CommandRuntimeException");
		} catch (CommandRuntimeException e) {
			assertEquals("WebhookFailed", e.getErrorName());
			assertFalse(e.getMessage(), e.getMessage().contains("s3cr3t"));
			assertTrue(e.getCause() == null);
		}
		assertTrue(received.isEmpty());
	}
	
}
