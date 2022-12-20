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


import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.bind.DatatypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.info.CmsAuthenticationException;
import se.simonsoft.cms.item.info.CmsCurrentUser;

public class PublishCdnUrlSignerCloudFront {
	
	private static final SecureRandom srand = new SecureRandom();
	private static final Logger logger = LoggerFactory.getLogger(PublishCdnUrlSignerCloudFront.class);

	private PublishCdnConfig cdnConfig;
	
	public PublishCdnUrlSignerCloudFront(PublishCdnConfig cdnConfig) {
		this.cdnConfig = cdnConfig;
	}
	

	public String getUrlDocument(String cdn, String path) {
		StringBuilder resource = new StringBuilder();
		resource.append("https://");
		resource.append(cdnConfig.getHostname(cdn));
		resource.append(path);
		// TODO: Decide if "index.html" should be appended (using Cloudfront Functions on public CDN?)
		return resource.toString();
	}
	
	
	/**
	 * Provides a complete URL to the full cdn microsite.
	 * @param cdn
	 * @param currentUser in order to validate user roles against CDN configuration (NOT IMPLEMENTED, only supports '*' in roles config) 
	 * @param expires
	 * @return
	 */
	public String getUrlSigned(String cdn, CmsCurrentUser currentUser, Instant expires) {
		String path = "/";
		List<String> docPathSegments = new ArrayList<String>();
		docPathSegments.add("*"); // Top level wildcard.
		
		String hostname = cdnConfig.getHostname(cdn);
		String keyId = cdnConfig.getPrivateKeyId(cdn);
		
		// TODO: Consider validating roles allowed to see the CDN.
		String result;
		if (keyId != null) {
			// Non-public CDN, verify Roles.
			// Currently only supporting allowing all authenticated users (or not).
			// Roles are not set in the config by default, which means signing is not permitted.
			Set<String> roles = cdnConfig.getAuthRoles(cdn);
			if (roles == null || !roles.contains("*")) {
				String msg = MessageFormatter.format("Access denied to CDN '{}'", cdn).getMessage();
				logger.warn(msg);
				throw new CmsAuthenticationException(msg);
			}
			logger.info("Access allowed to CDN '{}' {} {}", cdn, currentUser.getUsername(), currentUser.getUserRoles());
			
			result = getSignedUrlWithCustomPolicy(hostname, path, docPathSegments, keyId, cdnConfig.getPrivateKey(cdn), expires);
		} else {
			result = getUrlDocument(cdn, path);
		}
		return result;
	}
	
	
	
	
	/**
	 * Provides a complete URL to a document, signed if the CDN requires signature.
	 * The signature allows access to all files in the parent folder (siblings to the path parameter).
	 * 
	 * @param cdn
	 * @param path
	 * @param expires
	 * @return
	 */
	public String getUrlDocumentSigned(String cdn, String path, Instant expires) {
		// Using CmsItemPath for convenience. Prohibits a minumum och chars, currently * and \.
		CmsItemPath docPath = new CmsItemPath(path).getParent();
		List<String> docPathSegments = new ArrayList<String>(docPath.getPathSegments());
		docPathSegments.add("*"); // End with wildcard.
		// Cloudfront accepts multiple wildcards. 
		// Slash are not treated in any special way, ok when '/{docno}/' is in the path.
		// Rearranged CDN path in CMS 5.1 placing '/{docno}/' first, allowing lang-dropdown to work.
		// NOTE: CMS 5.1 prepared to support lang-dropdown signatures but it requires signature depth config (or similar).
		// However, the typical use case requires a separate service providing the signed urls, this service is only for authors.
		// TODO: Extend the api with ability to wildcard earlier (could be a config from PublishCdnConfig).
		
		String hostname = cdnConfig.getHostname(cdn);
		String keyId = cdnConfig.getPrivateKeyId(cdn);
		
		// TODO: Consider adding "index.html" for Public CDNs (gettor in cdnConfig, perhaps from SSM);
		
		String result;
		if (keyId != null) {
			result = getSignedUrlWithCustomPolicy(hostname, path, docPathSegments, keyId, cdnConfig.getPrivateKey(cdn), expires);
		} else {
			result = getUrlDocument(cdn, path);
		}
		return result;
	}
	
	
	public static String getSignedUrlWithCustomPolicy(String hostname, String path, List<String> docPathSegments, String keyPairId, PrivateKey privateKey, Instant expires) {
		StringBuilder resource = new StringBuilder();
		resource.append("https://");
		resource.append(hostname);
		resource.append(path);
		String resourceUrlOrPath = resource.toString();
		
		try {
			String customPolicy = buildCustomPolicy(hostname, docPathSegments, expires);
			String customPolicyBase64 = DatatypeConverter.printBase64Binary(customPolicy.getBytes(StandardCharsets.UTF_8));
			byte[] signatureBytes = signWithSha1Rsa(customPolicy.getBytes(StandardCharsets.UTF_8), privateKey);
			String urlSafeSignature = makeBytesUrlSafe(signatureBytes);
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
			resource.append(s);
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
	
}
