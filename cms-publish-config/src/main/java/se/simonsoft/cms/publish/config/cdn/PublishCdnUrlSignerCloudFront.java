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
package se.simonsoft.cms.publish.config.cdn;


import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.encoding.CmsItemURLEncoder;
import se.simonsoft.cms.item.info.CmsAuthenticationException;
import se.simonsoft.cms.item.info.CmsCurrentUser;

public class PublishCdnUrlSignerCloudFront {
	
	private static final SecureRandom srand = new SecureRandom();
	private static final Logger logger = LoggerFactory.getLogger(PublishCdnUrlSignerCloudFront.class);
	private static final CmsItemURLEncoder encoder = new CmsItemURLEncoder(); // There might be a few too many safe chars.

	private PublishCdnConfig cdnConfig;
	
	public PublishCdnUrlSignerCloudFront(PublishCdnConfig cdnConfig) {
		this.cdnConfig = cdnConfig;
	}
	

	/**
	 * @param cdn
	 * @param path non-encoded path
	 * @return
	 */
	public String getUrlDocument(String cdn, String path, Map<String, List<String>> query) {
		validatePath(path);
		StringBuilder resource = new StringBuilder();
		resource.append("https://");
		resource.append(cdnConfig.getHostname(cdn));
		resource.append(encoder.encode(path));
		// TODO: Support query
		return resource.toString();
	}
	
	
	/**
	 * Provides a complete URL to the full cdn microsite.
	 * @param cdn
	 * @param path to include in the url, non-encoded
	 * @param currentUser in order to validate user roles against CDN configuration (also supports '*' in roles config) 
	 * @param expires
	 * @return
	 */
	public String getUrlSiteSigned(String cdn, Optional<String> path, Map<String, List<String>> query, CmsCurrentUser currentUser, Instant expires) {
		List<String> docPathSegments = new ArrayList<String>();
		docPathSegments.add("*"); // Top level wildcard.
		
		String hostname = cdnConfig.getHostname(cdn);
		String keyId = cdnConfig.getPrivateKeyId(cdn);
		
		// Validating roles allowed to see the CDN.
		String result;
		if (keyId != null) {
			// Non-public CDN, verify Roles.
			// Currently only supporting allowing all authenticated users (or not).
			// Roles are not set in the config by default, which means signing is not permitted.
			Set<String> roles = cdnConfig.getAuthRoles(cdn);
			if (!currentUser.hasRole(roles)) {
				String msg = MessageFormatter.format("Access denied to CDN '{}'", cdn).getMessage();
				logger.warn(msg);
				throw new CmsAuthenticationException(msg);
			}
			logger.info("Access allowed to CDN '{}' {} {}", cdn, currentUser.getUsername(), currentUser.getUserRoles());
			
			result = getSignedUrlWithCustomPolicy(hostname, docPathSegments, path.orElse("/"), query, keyId, cdnConfig.getPrivateKey(cdn), expires);
		} else {
			result = getUrlDocument(cdn, path.orElse("/"), query);
		}
		return result;
	}
	
	
	
	
	/**
	 * Provides a complete URL to a document, signed if the CDN requires signature.
	 * The signature allows access to all files in the 'pathDocument' folder.
	 * NOTE: It is the responsibility of the caller to verify user read access to the document (and the 'docno' strategy must be controlled). 
	 * 
	 * @param cdn
	 * @param pathDocument a path to a folder where the signature is valid, typically the 'pathdocument' or only 'docno' to allow all locales
	 * @param path non-encoded path
	 * @param expires
	 * @return
	 */
	public String getUrlDocumentSigned(String cdn, CmsItemPath pathDocument, String path, Map<String, List<String>> query, Instant expires) {
		// Using CmsItemPath for convenience. Prohibits a minimum och chars, currently *, / and \.
		List<String> docPathSegments = new ArrayList<String>(pathDocument.getPathSegments());
		docPathSegments.add("*"); // End with wildcard.
		// Cloudfront accepts multiple wildcards. 
		// Slash are not treated in any special way, ok when '/{docno}/' is in the path.
		// Rearranged CDN path in CMS 5.1 placing '/{docno}/' first, allowing lang-dropdown to work.
		// NOTE: CMS 5.1 prepared to support lang-dropdown signatures but it requires signature depth config (or similar).
		// However, the typical use case requires a separate service providing the signed urls, this service is only for authors.
		
		String hostname = cdnConfig.getHostname(cdn);
		String keyId = cdnConfig.getPrivateKeyId(cdn);
		
		// TODO: Consider adding "index.html" for Public CDNs (gettor in cdnConfig, perhaps from SSM);
		// Probably keep the current approach without index.html for all CDNs.
		
		String result;
		if (keyId != null) {
			result = getSignedUrlWithCustomPolicy(hostname, docPathSegments, path, query, keyId, cdnConfig.getPrivateKey(cdn), expires);
		} else {
			result = getUrlDocument(cdn, path, query);
		}
		return result;
	}
	
	
	public static String getSignedUrlWithCustomPolicy(String hostname, List<String> docPathSegments, String path, Map<String, List<String>> query, String keyPairId, PrivateKey privateKey, Instant expires) {
		validatePath(path);
		StringBuilder resource = new StringBuilder();
		resource.append("https://");
		resource.append(hostname);
		resource.append(encoder.encode(path));
		String resourceUrlOrPath = resource.toString();
		
		try {
			String customPolicy = buildCustomPolicy(hostname, docPathSegments, expires);
			String customPolicyBase64 = Base64.getEncoder().encodeToString(customPolicy.getBytes(StandardCharsets.UTF_8));
			byte[] signatureBytes = signWithSha1Rsa(customPolicy.getBytes(StandardCharsets.UTF_8), privateKey);
			String urlSafeSignature = makeBytesUrlSafe(signatureBytes);
			// TODO: Support query, ensure signature replaces any existing query params with the same name.
			return resourceUrlOrPath + (resourceUrlOrPath.indexOf('?') >= 0 ? "&" : "?") + "Expires=" + expires.getEpochSecond() + "&Signature=" + urlSafeSignature + "&Key-Pair-Id=" + keyPairId + "&Policy=" + customPolicyBase64;
		} catch (InvalidKeyException e) {
			throw new RuntimeException("Couldn't sign url", e);
		}
	}
	
	private static String buildCustomPolicy(String hostname, List<String> docPathSegments, Instant expires) {
		StringBuilder resource = new StringBuilder();
		resource.append("https://");
		resource.append(hostname);
		for (String s: docPathSegments) {
			resource.append('/');
			resource.append(encoder.encode(s));
		}
		
		// Since we not adding additional conditions, just resource wildcards, possible to reuse the canned string.
		String policy = buildCannedPolicy(resource.toString(), expires);
		return policy;
	}

	
	/**
	 * Generates a signed url that expires after given date.
	 * 
	 * @param resourceUrlOrPath The url.
	 * @param keyPairId         The keypair id used to sign.
	 * @param privateKey        The private key.
	 * @param dateLessThan      The expire date/time.
	 * @return A valid cloudwatch url.
	 * @throws SdkException If any errors occur during the signing process.
	 */
	public static String getSignedUrlWithCannedPolicy(String resourceUrlOrPath, String keyPairId, PrivateKey privateKey, Instant expires) {
		try {
			String cannedPolicy = buildCannedPolicy(resourceUrlOrPath, expires);
			byte[] signatureBytes = signWithSha1Rsa(cannedPolicy.getBytes(StandardCharsets.UTF_8), privateKey);
			String urlSafeSignature = makeBytesUrlSafe(signatureBytes);
			return resourceUrlOrPath + (resourceUrlOrPath.indexOf('?') >= 0 ? "&" : "?") + "Expires=" + expires.getEpochSecond() + "&Signature=" + urlSafeSignature + "&Key-Pair-Id=" + keyPairId;
		} catch (InvalidKeyException e) {
			throw new RuntimeException("Couldn't sign url", e);
		}
	}

	/**
	 * Returns a "canned" policy for the given parameters.
	 * For more information, see <a href=
	 * "http://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/private-content-signed-urls-overview.html"
	 * >Overview of Signed URLs</a>.
	 * 
	 * @param resourceUrlOrPath The resource to grant access.
	 * @param dateLessThan      The expiration time.
	 * @return the aws policy as a string.
	 */
	public static String buildCannedPolicy(String resourceUrlOrPath, Instant expires) {
		return "{\"Statement\":[{\"Resource\":\"" + resourceUrlOrPath + "\",\"Condition\":{\"DateLessThan\":{\"AWS:EpochTime\":" + expires.getEpochSecond() + "}}}]}";
	}

	/**
	 * Signs the data given with the private key given, using the SHA1withRSA
	 * algorithm provided by bouncy castle.
	 * 
	 * @param dataToSign The data to sign.
	 * @param privateKey The private key.
	 * @return A signature.
	 * @throws InvalidKeyException if an invalid key was provided.
	 */
	public static byte[] signWithSha1Rsa(byte[] dataToSign, PrivateKey privateKey) throws InvalidKeyException {
		Signature signature;
		try {
			signature = Signature.getInstance("SHA1withRSA");
			signature.initSign(privateKey, srand);
			signature.update(dataToSign);
			return signature.sign();
		} catch (NoSuchAlgorithmException | SignatureException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Converts the given data to be safe for use in signed URLs for a private
	 * distribution by using specialized Base64 encoding.
	 * 
	 * @param bytes The bytes
	 */
	public static String makeBytesUrlSafe(byte[] bytes) {
		byte[] encoded = java.util.Base64.getEncoder().encode(bytes);

		for (int i = 0; i < encoded.length; i++) {
			switch (encoded[i]) {
			case '+':
				encoded[i] = '-';
				continue;
			case '=':
				encoded[i] = '_';
				continue;
			case '/':
				encoded[i] = '~';
				continue;
			default:
				continue;
			}
		}
		return new String(encoded, StandardCharsets.UTF_8);
	}
	
	public static void validatePath(String path) {
		if (path == null || !path.startsWith("/")) {
			throw new IllegalArgumentException("Invalid path: " + path);
		}
	}
	
}
