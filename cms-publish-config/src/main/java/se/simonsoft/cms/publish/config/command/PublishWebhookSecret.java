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

/**
 * A secret for publish webhook headers, see {@link PublishWebhookSecrets}.
 */
public final class PublishWebhookSecret {

	private final String value;
	private final String host;
	
	/**
	 * @param value the secret value
	 * @param host the only webhook URL host the secret may be sent to, null if not restricted
	 */
	public PublishWebhookSecret(String value, String host) {
		this.value = value;
		this.host = host;
	}
	
	public String getValue() {
		return value;
	}
	
	/**
	 * @return the only webhook URL host the secret may be sent to, null if not restricted
	 */
	public String getHost() {
		return host;
	}
	
	@Override
	public String toString() {
		return "PublishWebhookSecret[***]";
	}
}
