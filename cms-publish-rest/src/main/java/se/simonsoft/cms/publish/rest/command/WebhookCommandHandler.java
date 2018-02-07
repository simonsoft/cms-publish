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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.command.ExternalCommandHandler;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobOptions;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobProgress;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobStorage;
import se.simonsoft.cms.publish.config.export.PublishExportJob;

public class WebhookCommandHandler implements ExternalCommandHandler<PublishJobOptions>{


	private final AmazonS3 s3Client;
	private final Long expiry;
	private final String bucketName;
	private final String archiveExt = "zip";
	private final String manifestExt = "json";
	private final HttpClient client;

	private static final Logger logger = LoggerFactory.getLogger(WebhookCommandHandler.class);

	@Inject
	public WebhookCommandHandler(
						@Named("config:se.simonsoft.cms.publish.webhook.expiry") Long expiryMinutes,
						@Named("config:se.simonsoft.cms.publish.bucket") String bucketName,
						HttpClient client,
						Region region,
						AWSCredentialsProvider credentials) {

		this.expiry = expiryMinutes;
		this.bucketName = bucketName;
		this.client = client;
		this.s3Client = AmazonS3Client.builder()
				.withRegion(region.getName())
				.withCredentials(credentials)
				.build();
	}

	@Override
	public String handleExternalCommand(CmsItemId itemId, PublishJobOptions options) {
		
		logger.debug("WebhookCommandHandler called, will send request to: {}", options.getDelivery().getParams().get("url"));
		
		final PublishJobStorage storage = options.getStorage();
		
		if (storage == null) {
			throw new IllegalArgumentException("WebhookCommandHandler need a valid PublishJobStorage object.");
		}
		
		String manifest = null;
		String archive = null;

		if (storage.getType() == null || storage.getType().equals("s3")) {
			Boolean presign = new Boolean(options.getDelivery().getParams().get("presign"));
			archive = getS3Url(getParentPath(storage, archiveExt), presign).toString();
			manifest = getS3Url(getParentPath(storage, manifestExt), presign).toString();
		} else {
			PublishJobProgress progress = options.getProgress();
			if (progress == null) {
				throw new IllegalArgumentException("Storage is set to: {}, need a valid PublishJobProgress object.");
			}
			archive = progress.getParams().get("archive");
			manifest = progress.getParams().get("manifest");
		}
		
		if (archive == null || manifest == null) {
			throw new IllegalArgumentException("Illegal paths/urls to archive and or manifest.");
		}
		
		makeRequest(options.getDelivery().getParams().get("url"), getPostBody(archive, manifest));
		
		
		return null;
	}
	
	private HttpResponse makeRequest(String urlStr, List<NameValuePair> pairs) {
		HttpPost request = new HttpPost(urlStr);
		
		try {
			request.setEntity(new UrlEncodedFormEntity(pairs));
			logger.debug("Making request...");
			HttpResponse resp = client.execute(request);
			return resp;
			
		} catch (IOException e) {
			throw new RuntimeException("Failed", e);
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
		if (presign != null && presign) { 
			GeneratePresignedUrlRequest request =
					new GeneratePresignedUrlRequest(bucketName, path);
			request.setMethod(HttpMethod.GET);
			request.setExpiration(getExpiryDate());
			url = s3Client.generatePresignedUrl(request);
		} else {
			url = s3Client.getUrl(bucketName, path);
		}
		
       return url;
	}

	private Date getExpiryDate() {
		Calendar date = Calendar.getInstance();
		long t = date.getTimeInMillis();
		long millis = TimeUnit.MINUTES.toMillis(expiry);
		return new Date(t + millis);
	}

	private String getParentPath(PublishJobStorage storage, String extension) {
		return new PublishExportJob(storage, extension).getJobPath();
	}
}
