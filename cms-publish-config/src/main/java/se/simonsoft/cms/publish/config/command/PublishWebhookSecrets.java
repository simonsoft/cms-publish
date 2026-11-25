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
 * Secrets that can be referenced from publish webhook header values using the
 * placeholder syntax <code>{secrets:NAME}</code>, see {@link PublishWebhookSecretResolver}.
 * 
 * Implementations must never log secret values.
 */
public interface PublishWebhookSecrets {

	/**
	 * @param name secret name, matching <code>[A-Za-z0-9_]+</code>
	 * @return the secret, null if no secret is configured with that name
	 */
	PublishWebhookSecret getSecret(String name);
	
}
