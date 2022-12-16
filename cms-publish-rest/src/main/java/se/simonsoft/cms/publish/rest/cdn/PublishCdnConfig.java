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
package se.simonsoft.cms.publish.rest.cdn;

import java.security.PrivateKey;
import java.util.Set;

public interface PublishCdnConfig {

	/**
	 * Host name for the CDN, required
	 * @param cdn
	 * @return host name
	 * @throws IllegalStateException if not defined
	 */
	String getHostname(String cdn);
	
	/**
	 * Key ID if the CDN is not public.
	 * 
	 * @param cdn
	 * @return keyId or null
	 */
	String getPrivateKeyId(String cdn);
	
	/**
	 * Private Key if the CDN is not public. Should not be called if {@link #getPrivateKeyId(String)} returns null.
	 * @param cdn
	 * @return
	 */
	PrivateKey getPrivateKey(String cdn);

	// Consider adding:
	// - expiry time
	// - wildcard location, how many path segments before wildcard
	
	
	/**
	 * Roles allowed full access to the cdn.
	 * @param cdn
	 * @return
	 */
	Set<String> getAuthRoles(String cdn);
	
}
