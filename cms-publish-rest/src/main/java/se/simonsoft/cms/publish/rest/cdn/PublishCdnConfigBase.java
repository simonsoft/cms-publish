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

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.xml.bind.DatatypeConverter;

public abstract class PublishCdnConfigBase implements PublishCdnConfig {

	public static PrivateKey readPrivateKey(String privateKeyPkcs8) throws Exception {
		// https://stackoverflow.com/questions/15344125/load-a-rsa-private-key-in-java-algid-parse-error-not-a-sequence/21458628
	    String privateKeyBase64 = privateKeyPkcs8
	      .replace("-----BEGIN PRIVATE KEY-----", "")
	      //.replaceAll(System.lineSeparator(), "")
	      .replace("-----END PRIVATE KEY-----", "");

	    
	    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(DatatypeConverter.parseBase64Binary(privateKeyBase64));
	    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
	    PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
	    return privateKey;
	}
}
