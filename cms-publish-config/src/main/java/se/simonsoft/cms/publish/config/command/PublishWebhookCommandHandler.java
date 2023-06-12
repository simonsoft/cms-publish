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

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.command.CommandRuntimeException;
import se.simonsoft.cms.item.command.ExternalCommandHandler;
import se.simonsoft.cms.item.export.CmsExportProvider;
import se.simonsoft.cms.item.export.CmsExportUrlPresigner;
import se.simonsoft.cms.item.export.CmsImportJob;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobDelivery;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobOptions;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobProgress;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobStorage;
import se.simonsoft.cms.publish.config.export.PublishExportJobFactory;

/**
 * NOTE: This is a quick refactoring replacing Apache httpclient with Java 11 httpclient.
 * There is significant refactoring and alignment with TSP-webhook remaining.
 *
 */
public class PublishWebhookCommandHandler implements ExternalCommandHandler<PublishJobOptions> {


	private final Long expiryMinutes;
	private final HttpClient httpClient;
	private final CmsExportUrlPresigner presigner;

	private final String archiveExt = "zip";
	private final String manifestExt = "json";
	
	private static final Logger logger = LoggerFactory.getLogger(PublishWebhookCommandHandler.class);

	@Inject
	public PublishWebhookCommandHandler(
						@Named("config:se.simonsoft.cms.publish.webhook.expiry") Long expiryMinutes,
						@Named("config:se.simonsoft.cms.publish.export") CmsExportProvider exportProvider, 
						@Named("config:se.simonsoft.cms.publish.webhook") HttpClient httpClient) {
		
		this.expiryMinutes = expiryMinutes;
		this.httpClient = httpClient;
		this.presigner = exportProvider.getUrlPresigner();
	}
	
	@Override
	public Class<PublishJobOptions> getArgumentsClass() {
		return PublishJobOptions.class;
	}

	@Override
	public String handleExternalCommand(CmsItemId itemId, PublishJobOptions options) {
		
		// TODO: We might be able to suppress webhooks that executes after the document has been iterated.
		// This could prevent out-of-order webhooks.
		// Might need PublishJob, especially if we want to evaluate if this publishconfig is supposed to execute for the subsequent iterations. 
		
		// #1344: When executing in cms-webapp ('PublishOutdated' should be separate service)
		// - Use CmsItemLookup towards svn to get head revision (actually preferable to get svn log after this revision).
		// - If later revision exists with identical cms:status value, check if manifest has been written, then suppress this webhook.
		
		if (options.getDelivery() == null) {
			throw new IllegalArgumentException("Need a valid PublishJobDelivery object with param url.");
		}
		final PublishJobDelivery delivery = options.getDelivery();
		final PublishJobStorage storage = options.getStorage();
		
		if (storage == null) {
			throw new IllegalArgumentException("Need a valid PublishJobStorage object with params archive and manifest.");
		}
		
		logger.debug("WebhookCommandHandler endpoint: {}", delivery.getParams().get("url"));
		LinkedHashMap<String, String> postParams = getPostPayload(delivery, storage, Optional.of(options.getProgress()));
		
		// TODO: Consider writing a HttpResponse BodyHandler which retrieves the body if contentlength is short (see TSP-webhook).
		HttpResponse<Void> response;
		try {
			response = makeRequest(delivery.getParams().get("url"), delivery.getHeaders(), getPostBody(postParams), getPostContentType());
		} catch (HttpTimeoutException e) {
			logger.warn("Webhook Socket Timeout: " + delivery.getParams().get("url"));
			throw new CommandRuntimeException("WebhookRequestTimeout", e.getMessage());
		} catch (Exception e) {
			logger.warn("Webhook connect failed: {}", e.getMessage(), e);
			throw new CommandRuntimeException("WebhookFailed", e);
		}
		
		int statusCode = response.statusCode();
		// TODO: Align the statusCode handling and exceptions with TSP-webhook.
		// Consider making 202 Accepted configurable (OK / retry).
		if (statusCode >= 200 && statusCode <= 299) {
			logger.debug("Got a response with status code: {}", statusCode);
		} else if (statusCode >= 300 && statusCode <= 399){
			logger.warn("Server responded with redirect ({}): {}", statusCode, response.headers().firstValue("Location"));
			throw new CommandRuntimeException("WebhookFailed");
		} else {
			throw new CommandRuntimeException("WebhookFailed");
		}
		return null;
	}
	
	private HttpResponse<Void> makeRequest(String url, Map<String,String> headers, BodyPublisher body, String contentType) throws IOException {
		
		Builder postRequestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .POST(body)
                .header("Content-Type", contentType);
		
		for (Entry<String, String> e: headers.entrySet()) {
			postRequestBuilder.header(e.getKey(), e.getValue());
		}
		
		try {
			HttpRequest postRequest = postRequestBuilder.build();
			logger.debug("Webhook HTTP request...");
			HttpResponse<Void> response = httpClient.send(postRequest, HttpResponse.BodyHandlers.discarding());
			return response;
			
		} catch (InterruptedException e) {
			logger.error("Interrupted: {}", e.getMessage(), e.getStackTrace());
			throw new RuntimeException("Failed executing HTTP request (" + e.getClass().getName() + "): " + e.getMessage(), e);
		}
	}
	
	protected BodyPublisher getPostBody(Map<String, String> postParams) {

		String form = postParams.entrySet().stream()
			      .map(entry -> String.join("=",
	                        URLEncoder.encode(entry.getKey().toString(), StandardCharsets.UTF_8),
	                        URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8)))
			      .collect(Collectors.joining("&"));
	    return BodyPublishers.ofString(form);
	}
	
	protected String getPostContentType() {
		return "application/x-www-form-urlencoded; charset=UTF-8";
	}

	public LinkedHashMap<String, String> getPostPayload(PublishJobDelivery delivery, PublishJobStorage storage, Optional<PublishJobProgress> progress) {
		String manifest = null;
		String archive = null;

		if (storage.getType() == null || storage.getType().equals("s3")) {
			Boolean presign = Boolean.parseBoolean(delivery.getParams().get("presign"));
			
			archive = getS3Url(storage, archiveExt, presign).toString();
			manifest = getS3Url(storage, manifestExt, presign).toString();
		} else {
			archive = progress.orElse(new PublishJobProgress()).getParams().get("archive");
			manifest = progress.orElse(new PublishJobProgress()).getParams().get("manifest");
			if (archive == null || manifest == null) {
				throw new IllegalArgumentException("Illegal paths to archive and or manifest.");
			}
		}

		LinkedHashMap<String, String> postParams = new LinkedHashMap<String, String>();
		if (delivery.getParams().containsKey("form-archive-name")) {
			postParams.put(delivery.getParams().get("form-archive-name"), archive);
		} else {
			postParams.put("archive", archive);
		}
		if (delivery.getParams().containsKey("form-manifest-name")) {
			postParams.put(delivery.getParams().get("form-manifest-name"), manifest);
		} else {
			postParams.put("manifest", manifest);
		}
		return postParams;
	}

	
	private URL getS3Url(PublishJobStorage storage, String extension, Boolean presign) {
		
		URL url = null;
		CmsImportJob job = PublishExportJobFactory.getImportJobSingle(storage, extension);
		
		if (presign != null && presign) {
			
			url = presigner.getPresignedGet(job, Duration.ofMinutes(expiryMinutes));
		} else {
			url = presigner.getUrl(job);
		}
		
       return url;
	}

}
