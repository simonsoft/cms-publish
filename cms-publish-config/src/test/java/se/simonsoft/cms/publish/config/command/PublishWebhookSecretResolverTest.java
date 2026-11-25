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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import se.simonsoft.cms.item.command.CommandRuntimeException;

public class PublishWebhookSecretResolverTest {
	
	static final String URL = "https://api.example.com/cms/publish";

	static class SecretsMap implements PublishWebhookSecrets {
		final Map<String, String> values = new HashMap<>();
		final Map<String, String> hosts = new HashMap<>();
		
		SecretsMap with(String name, String value, String host) {
			values.put(name, value);
			if (host != null) {
				hosts.put(name, host);
			}
			return this;
		}
		
		@Override
		public PublishWebhookSecret getSecret(String name) {
			if (!values.containsKey(name)) {
				return null;
			}
			return new PublishWebhookSecret(values.get(name), hosts.get(name));
		}
	}
	
	private static Map<String, String> headers(String name, String value) {
		Map<String, String> h = new LinkedHashMap<>();
		h.put(name, value);
		return h;
	}
	
	private static CommandRuntimeException assertFails(PublishWebhookSecretResolver resolver, Map<String, String> headers, String url) {
		try {
			resolver.resolveHeaders(headers, url);
			fail("Expected CommandRuntimeException");
			return null;
		} catch (CommandRuntimeException e) {
			assertEquals(PublishWebhookSecretResolver.ERROR_NAME, e.getErrorName());
			return e;
		}
	}
	
	@Test
	public void testNoPlaceholderUnchanged() {
		PublishWebhookSecretResolver resolver = new PublishWebhookSecretResolver(new SecretsMap());
		Map<String, String> h = new LinkedHashMap<>();
		h.put("Authorization", "Bearer literal");
		h.put("X-Json", "{\"a\": \"b\"}");
		h.put("X-Space", "{ secrets:A}"); // Not a Qute expression (whitespace), literal text.
		
		assertEquals(h, resolver.resolveHeaders(h, URL));
	}
	
	@Test
	public void testNullHeaders() {
		PublishWebhookSecretResolver resolver = new PublishWebhookSecretResolver(new SecretsMap());
		assertTrue(resolver.resolveHeaders(null, URL).isEmpty());
	}
	
	@Test
	public void testResolve() {
		SecretsMap secrets = new SecretsMap().with("ACME_TOKEN", "s3cr3t", null).with("USER", "u$er\\1", null);
		PublishWebhookSecretResolver resolver = new PublishWebhookSecretResolver(secrets);
		Map<String, String> h = new LinkedHashMap<>();
		h.put("Authorization", "Bearer {secrets:ACME_TOKEN}");
		h.put("X-Combined", "{secrets:USER}:{secrets:ACME_TOKEN}");
		h.put("X-Other", "plain");
		
		Map<String, String> r = resolver.resolveHeaders(h, URL);
		assertEquals("Bearer s3cr3t", r.get("Authorization"));
		assertEquals("u$er\\1:s3cr3t", r.get("X-Combined"));
		assertEquals("plain", r.get("X-Other"));
		// Input not modified.
		assertEquals("Bearer {secrets:ACME_TOKEN}", h.get("Authorization"));
	}
	
	@Test
	public void testSinglePass() {
		SecretsMap secrets = new SecretsMap().with("A", "{secrets:B}", null).with("B", "b", null);
		PublishWebhookSecretResolver resolver = new PublishWebhookSecretResolver(secrets);
		assertEquals("{secrets:B}", resolver.resolveHeaders(headers("X", "{secrets:A}"), URL).get("X"));
	}
	
	@Test
	public void testUnknownSecret() {
		PublishWebhookSecretResolver resolver = new PublishWebhookSecretResolver(new SecretsMap().with("EMPTY", "", null));
		CommandRuntimeException e = assertFails(resolver, headers("Authorization", "Bearer {secrets:MISSING}"), URL);
		assertTrue(e.getMessage(), e.getMessage().contains("MISSING"));
		assertFails(resolver, headers("Authorization", "Bearer {secrets:EMPTY}"), URL);
	}
	
	@Test
	public void testInvalidSyntax() {
		PublishWebhookSecretResolver resolver = new PublishWebhookSecretResolver(new SecretsMap().with("A", "a", null));
		assertFails(resolver, headers("X", "{secrets:bad-name}"), URL);
		assertFails(resolver, headers("X", "{secrets:A }"), URL);
		assertFails(resolver, headers("X", "{secrets:A"), URL);
		assertFails(resolver, headers("X", "{secrets:A} {other}"), URL);
		assertFails(resolver, headers("X", "{secrets:A}}"), URL);
	}
	
	@Test
	public void testHostBinding() {
		SecretsMap secrets = new SecretsMap().with("A", "a", "api.example.com");
		PublishWebhookSecretResolver resolver = new PublishWebhookSecretResolver(secrets);
		
		assertEquals("a", resolver.resolveHeaders(headers("X", "{secrets:A}"), URL).get("X"));
		assertEquals("a", resolver.resolveHeaders(headers("X", "{secrets:A}"), "https://API.Example.com:8443/x?y=z").get("X"));
		
		CommandRuntimeException e = assertFails(resolver, headers("X", "{secrets:A}"), "https://attacker.example.net/");
		assertTrue(e.getMessage(), e.getMessage().contains("attacker.example.net"));
		assertFails(resolver, headers("X", "{secrets:A}"), "https://evil.api.example.com/");
		assertFails(resolver, headers("X", "{secrets:A}"), "https://api.example.com.evil.net/");
		assertFails(resolver, headers("X", "{secrets:A}"), "https://user@attacker.example.net\\@api.example.com/");
		assertFails(resolver, headers("X", "{secrets:A}"), "not a url");
		assertFails(resolver, headers("X", "{secrets:A}"), null);
	}
	
	@Test
	public void testUnboundSecretAnyHost() {
		PublishWebhookSecretResolver resolver = new PublishWebhookSecretResolver(new SecretsMap().with("A", "a", null));
		assertEquals("a", resolver.resolveHeaders(headers("X", "{secrets:A}"), "https://any.example.net/").get("X"));
	}
	
	@Test
	public void testControlCharacters() {
		SecretsMap secrets = new SecretsMap().with("NL", "s3cr3t\nX-Injected: 1", null).with("DEL", "s3cr3t\u007F", null).with("TAB", "a\tb", null);
		PublishWebhookSecretResolver resolver = new PublishWebhookSecretResolver(secrets);
		CommandRuntimeException e = assertFails(resolver, headers("X", "{secrets:NL}"), URL);
		assertTrue(e.getMessage(), e.getMessage().contains("NL"));
		assertFalse(e.getMessage(), e.getMessage().contains("s3cr3t"));
		assertFails(resolver, headers("X", "{secrets:DEL}"), URL);
		assertEquals("a\tb", resolver.resolveHeaders(headers("X", "{secrets:TAB}"), URL).get("X"));
	}
	
}
