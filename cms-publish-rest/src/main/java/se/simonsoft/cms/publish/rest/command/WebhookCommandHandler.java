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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import se.simonsoft.cms.item.export.CmsExportProvider;
import se.simonsoft.cms.item.export.CmsExportUrlPresigner;
import se.simonsoft.cms.item.export.CmsImportJob;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobDelivery;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobOptions;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobStorage;
import se.simonsoft.cms.publish.config.export.PublishExportJobFactory;

public class WebhookCommandHandler implements ExternalCommandHandler<PublishJobOptions> {


	private final Long expiryMinutes;
	private final HttpClient client;
	private final CmsExportUrlPresigner presigner;

	private final String archiveExt = "zip";
	private final String manifestExt = "json";
	
	private static final Logger logger = LoggerFactory.getLogger(WebhookCommandHandler.class);

	@Inject
	public WebhookCommandHandler(
						@Named("config:se.simonsoft.cms.publish.webhook.expiry") Long expiryMinutes,
						@Named("config:se.simonsoft.cms.publish.export") CmsExportProvider exportProvider, 
						HttpClient client) {
		
		this.expiryMinutes = expiryMinutes;
		this.client = client;
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
		
		// #1344: When executing in cms-webapp (code should be separate service)
		// - Use CmsItemLookup towards svn to get head revision (actually preferable to get svn log after this revision).
		// - If later revision exists with identical cms:status value, check if manifest has been written, then suppress this webhook.
		
		if (options.getDelivery() == null) {
			throw new IllegalArgumentException("Need a valid PublishJobDelivery object with param url.");
		}
		final Map<String, String> params = options.getDelivery().getParams();
		final PublishJobStorage storage = options.getStorage();
		
		if (storage == null) {
			throw new IllegalArgumentException("Need a valid PublishJobStorage object with params archive and manifest.");
		}
		
		logger.debug("WebhookCommandHandler endpoint: {}", params.get("url"));
		
		
		String manifest = null;
		String archive = null;

		if (storage.getType() == null || storage.getType().equals("s3")) {
			Boolean presign = Boolean.parseBoolean(params.get("presign"));
			
			archive = getS3Url(storage, archiveExt, presign).toString();
			manifest = getS3Url(storage, manifestExt, presign).toString();
		} else {
			archive = options.getProgress().getParams().get("archive");
			manifest = options.getProgress().getParams().get("manifest");
			if (archive == null || manifest == null) {
				throw new IllegalArgumentException("Illegal paths to archive and or manifest.");
			}
		}

		LinkedHashMap<String, String> postParams = new LinkedHashMap<String, String>();
		if (params.containsKey("form-archive-name")) {
			postParams.put(params.get("form-archive-name"), archive);
		} else {
			postParams.put("archive", archive);
		}
		if (params.containsKey("form-manifest-name")) {
			postParams.put(params.get("form-manifest-name"), manifest);
		} else {
			postParams.put("manifest", manifest);
		}
		
		
		HttpResponse response;
		try {
			response = makeRequest(options.getDelivery(), getPostBody(postParams));
		} catch (Exception e) {
			logger.warn("Webhook connect failed: {}", e.getMessage(), e);
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
			logger.debug("Webhook HTTP request...");
			HttpResponse resp = client.execute(request);
			return resp;
			
		} catch (IOException e) {
			throw new RuntimeException("Failed executing HTTP request: " + delivery.getParams().get("url"), e);
		} finally {
			request.releaseConnection();
		}
	}
	
	private List<NameValuePair> getPostBody(Map<String, String> postParams) {

		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		for (Entry<String, String> entry: postParams.entrySet()) {
			pairs.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
		}
	    return pairs;
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
