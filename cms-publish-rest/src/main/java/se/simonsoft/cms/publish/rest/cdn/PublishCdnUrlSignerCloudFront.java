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

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Date;

import software.amazon.awssdk.core.exception.SdkException;

public class PublishCdnUrlSignerCloudFront {
	
	private static final SecureRandom srand = new SecureRandom();

	private PublishCdnConfig cdnConfig;
	
	public PublishCdnUrlSignerCloudFront(PublishCdnConfig cdnConfig) {
		this.cdnConfig = cdnConfig;
	}
	

	public String getSignedUrlDocument(String cdn, String path) {
		
		
		Date dateLessThan = new Date();
		dateLessThan.setHours(23); // TODO: Replace with sending an Instant or no of minutes.
		
		String result = getSignedUrlWithCannedPolicy("https://" + cdnConfig.getHostname(cdn) + path, cdnConfig.getPrivateKeyId(cdn), cdnConfig.getPrivateKey(cdn), dateLessThan);
		return result;
	}
	
	
	
	  /**
	   * Generates a signed url that expires after given date.
	   * @param resourceUrlOrPath The url.
	   * @param keyPairId The keypair id used to sign.
	   * @param privateKey The private key.
	   * @param dateLessThan The expire date/time.
	   * @return A valid cloudwatch url.
	   * @throws SdkException If any errors occur during the signing process.
	   */
	  public static String getSignedUrlWithCannedPolicy(String resourceUrlOrPath,
	                                                    String keyPairId,
	                                                    PrivateKey privateKey,
	                                                    Date dateLessThan) {
	    try {
	      String cannedPolicy = buildCannedPolicy(resourceUrlOrPath, dateLessThan);
	      byte[] signatureBytes = signWithSha1Rsa(cannedPolicy.getBytes(StandardCharsets.UTF_8), privateKey);
	      String urlSafeSignature = makeBytesUrlSafe(signatureBytes);
	      return resourceUrlOrPath
	          + (resourceUrlOrPath.indexOf('?') >= 0 ? "&" : "?")
	          + "Expires=" + MILLISECONDS.toSeconds(dateLessThan.getTime())
	          + "&Signature=" + urlSafeSignature
	          + "&Key-Pair-Id=" + keyPairId;
	    } catch (InvalidKeyException e) {
	      throw new RuntimeException("Couldn't sign url", e);
	    }
	  }

	  /**
	   * Returns a "canned" policy for the given parameters.
	   * For more information, see <a href=
	   * "http://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/private-content-signed-urls-overview.html"
	   * >Overview of Signed URLs</a>.
	   * @param resourceUrlOrPath The resource to grant access.
	   * @param dateLessThan The expiration time.
	   * @return the aws policy as a string.
	   */
	  public static String buildCannedPolicy(String resourceUrlOrPath,
	                                         Date dateLessThan) {
	    return "{\"Statement\":[{\"Resource\":\""
	        + resourceUrlOrPath
	        + "\",\"Condition\":{\"DateLessThan\":{\"AWS:EpochTime\":"
	        + MILLISECONDS.toSeconds(dateLessThan.getTime())
	        + "}}}]}";
	  }

	  /**
	   * Signs the data given with the private key given, using the SHA1withRSA
	   * algorithm provided by bouncy castle.
	   * @param dataToSign The data to sign.
	   * @param privateKey The private key.
	   * @return A signature.
	   * @throws InvalidKeyException if an invalid key was provided.
	   */
	  public static byte[] signWithSha1Rsa(byte[] dataToSign,
	                                       PrivateKey privateKey) throws InvalidKeyException {
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
	
	/*
	// Signed URLs for a private distribution
	// Note that Java only supports SSL certificates in DER format, 
	// so you will need to convert your PEM-formatted file to DER format. 
	// To do this, you can use openssl:
	// openssl pkcs8 -topk8 -nocrypt -in origin.pem -inform PEM -out new.der 
	//	    -outform DER 
	// So the encoder works correctly, you should also add the bouncy castle jar
	// to your project and then add the provider.
	
	Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
	
	String distributionDomain = "a1b2c3d4e5f6g7.cloudfront.net";
	String privateKeyFilePath = "/path/to/rsa-private-key.der";
	String s3ObjectKey = "s3/object/key.txt";
	String policyResourcePath = "https://" + distributionDomain + "/" + s3ObjectKey;
	
	// Convert your DER file into a byte array.
	
	byte[] derPrivateKey = ServiceUtils.readInputStreamToBytes(new
	    FileInputStream(privateKeyFilePath));
	
	// Generate a "canned" signed URL to allow access to a 
	// specific distribution and file
	
	String signedUrlCanned = CloudFrontService.signUrlCanned(
	    "https://" + distributionDomain + "/" + s3ObjectKey, // Resource URL or Path
	    keyPairId,     // Certificate identifier, 
	                   // an active trusted signer for the distribution
	    derPrivateKey, // DER Private key data
	    ServiceUtils.parseIso8601Date("2011-11-14T22:20:00.000Z") // DateLessThan
	    );
	System.out.println(signedUrlCanned);
	
	// Build a policy document to define custom restrictions for a signed URL.
	
	String policy = CloudFrontService.buildPolicyForSignedUrl(
	    // Resource path (optional, can include '*' and '?' wildcards)
	    policyResourcePath, 
	    // DateLessThan
	    ServiceUtils.parseIso8601Date("2011-11-14T22:20:00.000Z"), 
	    // CIDR IP address restriction (optional, 0.0.0.0/0 means everyone)
	    "0.0.0.0/0", 
	    // DateGreaterThan (optional)
	    ServiceUtils.parseIso8601Date("2011-10-16T06:31:56.000Z")
	    );
	
	// Generate a signed URL using a custom policy document.
	
	String signedUrl = CloudFrontService.signUrl(
	    // Resource URL or Path
	    "https://" + distributionDomain + "/" + s3ObjectKey, 
	    // Certificate identifier, an active trusted signer for the distribution
	    keyPairId,     
	    // DER Private key data
	    derPrivateKey, 
	    // Access control policy
	    policy 
	    );
	System.out.println(signedUrl);
	*/
}
