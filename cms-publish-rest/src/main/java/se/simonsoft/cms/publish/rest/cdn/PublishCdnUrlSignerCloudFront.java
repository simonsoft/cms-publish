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

import javax.xml.bind.DatatypeConverter;

import se.simonsoft.cms.item.CmsItemPath;
import software.amazon.awssdk.core.exception.SdkException;

public class PublishCdnUrlSignerCloudFront {
	
	private static final SecureRandom srand = new SecureRandom();

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
	
	public String getUrlDocumentSigned(String cdn, String path, Instant expires) {
		
		// Using CmsItemPath for convenience. Prohibits a minumum och chars, currently * and \.
		CmsItemPath docPath = new CmsItemPath(path).getParent();
		List<String> docPathSegments = new ArrayList<String>(docPath.getPathSegments());
		docPathSegments.add("*"); // End with wildcard.
		/*
		// Cloudfront accepts multiple wildcards. 
		// Slash are not treated in any special way, ok when '/{docno}/' is in the path.
		docPathSegments.set(0, "*"); // TODO: Consider wildcard for locale. Not sure how that will work with Preview.
		*/
		
		String result = getSignedUrlWithCustomPolicy(cdnConfig.getHostname(cdn), path, docPathSegments, cdnConfig.getPrivateKeyId(cdn), cdnConfig.getPrivateKey(cdn), expires);
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