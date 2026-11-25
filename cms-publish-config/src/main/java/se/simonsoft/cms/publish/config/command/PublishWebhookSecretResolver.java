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

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import se.simonsoft.cms.item.command.CommandRuntimeException;

/**
 * CMS-2117: Resolves secret placeholders in publish webhook header values.
 * 
 * Syntax is the Qute namespace expression <code>{secrets:NAME}</code> with NAME matching <code>[A-Za-z0-9_]+</code>,
 * no whitespace inside the braces. This implementation is intentionally limited to that syntax
 * so that a later switch to a Qute engine (namespace resolver, no value resolvers) gives the same result.
 * 
 * Rules:
 * <ul>
 * <li>Header values without <code>{secrets:</code> are passed through unchanged.</li>
 * <li>A header value with a placeholder must not contain any other '{' or '}' (Qute compatibility).</li>
 * <li>Unknown secret name fails, never resolves to empty string.</li>
 * <li>Secret values with control characters fail, ensures the HTTP client never reports the value in an error message.</li>
 * <li>If the secret is bound to a host, the webhook URL host must match exactly (case-insensitive).</li>
 * <li>Single pass, resolved values are never scanned for placeholders.</li>
 * </ul>
 * Error messages never contain secret values.
 */
public class PublishWebhookSecretResolver {
	
	public static final String ERROR_NAME = "WebhookSecretFailed";
	
	static final String PLACEHOLDER_START = "{secrets:";
	static final Pattern PLACEHOLDER = Pattern.compile("\\{secrets:([A-Za-z0-9_]+)\\}");
	
	private final PublishWebhookSecrets secrets;
	
	public PublishWebhookSecretResolver(PublishWebhookSecrets secrets) {
		this.secrets = secrets;
	}

	/**
	 * @param headers header values, possibly with placeholders, not modified
	 * @param url the webhook URL, used for host binding
	 * @return new map with resolved header values, must not be logged
	 */
	public Map<String, String> resolveHeaders(Map<String, String> headers, String url) {
		
		Map<String, String> result = new LinkedHashMap<>();
		if (headers == null) {
			return result;
		}
		for (Entry<String, String> e: headers.entrySet()) {
			String value = e.getValue();
			if (value != null && value.contains(PLACEHOLDER_START)) {
				value = resolveValue(e.getKey(), value, url);
			}
			result.put(e.getKey(), value);
		}
		return result;
	}
	
	private String resolveValue(String headerName, String template, String url) {
		
		// Qute compatibility: no other expressions or braces than the supported placeholders.
		String remaining = PLACEHOLDER.matcher(template).replaceAll("");
		if (remaining.contains("{") || remaining.contains("}")) {
			throw new CommandRuntimeException(ERROR_NAME, "Invalid secret placeholder in webhook header '" + headerName + "', use {secrets:NAME} with NAME matching [A-Za-z0-9_]+ and no other braces.");
		}
		
		Matcher m = PLACEHOLDER.matcher(template);
		StringBuilder sb = new StringBuilder();
		while (m.find()) {
			m.appendReplacement(sb, Matcher.quoteReplacement(getSecretValue(m.group(1), url)));
		}
		m.appendTail(sb);
		return sb.toString();
	}
	
	private String getSecretValue(String name, String url) {
		
		PublishWebhookSecret secret = secrets.getSecret(name);
		if (secret == null || secret.getValue() == null || secret.getValue().isEmpty()) {
			throw new CommandRuntimeException(ERROR_NAME, "Webhook secret is not configured: " + name);
		}
		if (!isValidHeaderValue(secret.getValue())) {
			throw new CommandRuntimeException(ERROR_NAME, "Webhook secret contains control characters: " + name);
		}
		
		String host = secret.getHost();
		if (host != null && !host.isBlank()) {
			String urlHost = getHost(url);
			if (urlHost == null || !host.trim().equalsIgnoreCase(urlHost)) {
				throw new CommandRuntimeException(ERROR_NAME, "Webhook secret '" + name + "' is not allowed for host: " + urlHost);
			}
		}
		return secret.getValue();
	}
	
	static boolean isValidHeaderValue(String value) {
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if ((c < 0x20 && c != '\t') || c == 0x7F) {
				return false;
			}
		}
		return true;
	}
	
	static String getHost(String url) {
		if (url == null) {
			return null;
		}
		try {
			return URI.create(url).getHost();
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
	
}
