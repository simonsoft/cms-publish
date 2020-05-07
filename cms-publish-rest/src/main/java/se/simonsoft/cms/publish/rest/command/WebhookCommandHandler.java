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
package se.simonsoft.cms.publish.rest.command;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.command.CommandRuntimeException;
import se.simonsoft.cms.item.command.ExternalCommandHandler;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobDelivery;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobOptions;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobStorage;
import se.simonsoft.cms.publish.config.export.PublishExportJobFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

public class WebhookCommandHandler implements ExternalCommandHandler<PublishJobOptions>{


	private final Long expiryMinutes;
	private final String bucketName;
	private final HttpClient client;
	private final S3Utilities s3Utils;
	private final S3Presigner presigner;

	private final String archiveExt = "zip";
	private final String manifestExt = "json";
	
	private static final Logger logger = LoggerFactory.getLogger(WebhookCommandHandler.class);

	@Inject
	public WebhookCommandHandler(
						@Named("config:se.simonsoft.cms.publish.webhook.expiry") Long expiryMinutes,
						@Named("config:se.simonsoft.cms.publish.bucket") String bucketName,
						HttpClient client,
						Region region,
						AwsCredentialsProvider credentials) {
		
		this.expiryMinutes = expiryMinutes;
		this.bucketName = bucketName;
		this.client = client;
		this.s3Utils = S3Utilities.builder()
				.region(region)
				.build();
		this.presigner = S3Presigner.builder()
				.region(region)
				.credentialsProvider(credentials)
				.build();
	}

	@Override
	public String handleExternalCommand(CmsItemId itemId, PublishJobOptions options) {
		
		// TODO: We might be able to suppress webhooks that executes after the document has been iterated.
		// This could prevent out-of-order webhooks.
		// Might need PublishJob, especially if we want to evaluate if this publishconfig is supposed to execute for the subsequent iterations. 
		
		if (options.getDelivery() == null) {
			throw new IllegalArgumentException("Need a valid PublishJobDelivery object with param url.");
		}
		
		final PublishJobStorage storage = options.getStorage();
		
		if (storage == null) {
			throw new IllegalArgumentException("Need a valid PublishJobStorage object with params archive and manifest.");
		}
		
		logger.debug("WebhookCommandHandler will try to send request to: {}", options.getDelivery().getParams().get("url"));
		
		
		String manifest = null;
		String archive = null;

		if (storage.getType() == null || storage.getType().equals("s3")) {
			Boolean presign = new Boolean(options.getDelivery().getParams().get("presign"));
			
			String archiveKey = getS3Key(storage, archiveExt);
			archive = getS3Url(archiveKey, presign).toString();
			
			String manifestKey = getS3Key(storage, manifestExt);
			manifest = getS3Url(manifestKey, presign).toString();
		} else {
			archive = options.getProgress().getParams().get("archive");
			manifest = options.getProgress().getParams().get("manifest");
			if (archive == null || manifest == null) {
				throw new IllegalArgumentException("Illegal paths to archive and or manifest.");
			}
		}
		
		HttpResponse response;
		try {
			response = makeRequest(options.getDelivery(), getPostBody(archive, manifest));
		} catch (Exception e) {
			throw new CommandRuntimeException("WebhookFailed", e);
		}
		
		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode >= 200 && statusCode <= 299) {
			logger.debug("Got a response with status code: {}", statusCode);
		} else if (statusCode >= 300 && statusCode <= 399){
			logger.warn("Server responded with redirect ({}): {}", statusCode, response.getHeaders("Location"));
			throw new CommandRuntimeException("WebhookFailed");
		} else {
			throw new CommandRuntimeException("WebhookFailed");
		}
		return null;
	}
	
	private HttpResponse makeRequest(PublishJobDelivery delivery, List<NameValuePair> pairs) {
		HttpPost request = new HttpPost(delivery.getParams().get("url"));
		
		for (Entry<String, String> e: delivery.getHeaders().entrySet()) {
			request.addHeader(e.getKey(), e.getValue());
		}
		
		try {
			request.setEntity(new UrlEncodedFormEntity(pairs, StandardCharsets.UTF_8));
			logger.debug("Making request...");
			HttpResponse resp = client.execute(request);
			return resp;
			
		} catch (IOException e) {
			throw new RuntimeException("Failed executing HTTP request: " + delivery.getParams().get("url"), e);
		} finally {
			request.releaseConnection();
		}
	}
	
	private List<NameValuePair> getPostBody(String archive, String manifest) {

		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		pairs.add(new BasicNameValuePair("archive", archive));
		pairs.add(new BasicNameValuePair("manifest", manifest));
		
	    return pairs;
	}

	private URL getS3Url(String path, Boolean presign) {
		
		URL url = null;
		
		GetUrlRequest getUrlRequest = GetUrlRequest.builder()
				.bucket(bucketName)
				.key(path)
				.build();
		
		GetObjectRequest getObjectRequest = GetObjectRequest.builder()
				.bucket(bucketName)
				.key(path)
				.build();
		
		if (presign != null && presign) {
			GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
					.signatureDuration(Duration.ofMinutes(this.expiryMinutes))
					.getObjectRequest(getObjectRequest)
					.build();
			
			PresignedGetObjectRequest presignedGetObjectRequest = presigner.presignGetObject(getObjectPresignRequest);
			
			if (!presignedGetObjectRequest.isBrowserExecutable()) {
				throw new IllegalStateException("Presigned S3 GET request is not Browser Compatible: " + path);
			}
			url = presignedGetObjectRequest.url();
		} else {
			url = s3Utils.getUrl(getUrlRequest);
		}
		
       return url;
	}


	private String getS3Key(PublishJobStorage storage, String extension) {
		
		StringBuilder sb = new StringBuilder();
		sb.append(storage.getPathversion());
		sb.append("/");
		sb.append(storage.getPathcloudid());
		sb.append("/");
		sb.append(PublishExportJobFactory.getImportJobSingle(storage, extension).getJobPath());

		return sb.toString();
	}
}
