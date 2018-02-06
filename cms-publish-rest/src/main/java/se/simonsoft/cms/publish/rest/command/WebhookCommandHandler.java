package se.simonsoft.cms.publish.rest.command;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.HttpMethod;
import com.amazonaws.Response;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;

import se.repos.restclient.RestClient;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.command.ExternalCommandHandler;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobOptions;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobStorage;

public class WebhookCommandHandler implements ExternalCommandHandler<PublishJobOptions>{


	private final AmazonS3 s3Client;
	private final RestClient restClient;
	private final Date expiery;
	private final String bucketName;
	private final String archiveExt = ".zip";
	private final String manifestExt = ".json";

	private static final Logger logger = LoggerFactory.getLogger(WebhookCommandHandler.class);

	@Inject
	public WebhookCommandHandler(
						RestClient restClient,
						Date expiery,
						@Named("config:se.simonsoft.cms.publish.bucket") String bucketName,
						Region region,
						AWSCredentialsProvider credentials) {

		this.restClient = restClient;
		this.expiery = expiery;
		this.bucketName = bucketName;
		this.s3Client = AmazonS3Client.builder()
				.withRegion(region.getName())
				.withCredentials(credentials)
				.build();
	}

	@Override
	public String handleExternalCommand(CmsItemId itemId, PublishJobOptions options) {
		
		logger.debug("WebhookCommandHandler called, will call webhook.");
		
		final PublishJobStorage storage = options.getStorage();

		if (storage.getType() == null || storage.getType().equals("s3")) {
			
			final String s3BasePath = getS3BasePath(storage);
			
			String manifestUrl;
			String archiveUrl;
			String presign = options.getDelivery().getParams().get("presign");
			if (presign != null && presign.equals("true")) {
				logger.debug("Generating presigned urls with expiery: {}", expiery.getTime());
				archiveUrl = getPresignedUrl(expiery ,s3BasePath, archiveExt).toString();
				manifestUrl = getPresignedUrl(expiery, s3BasePath, manifestExt).toString();
			} else {
				logger.debug("do not presign the urls bucket: {}", bucketName);
				archiveUrl = String.format("%s/%s%s", bucketName, s3BasePath, archiveExt);
				manifestUrl = String.format("%s/%s%s", bucketName, s3BasePath, manifestExt);
			}
			
			logger.debug("Post body archive: {}, manifest: {}", archiveUrl, manifestUrl);
			makeRequest(options.getDelivery().getParams().get("url"), getPostBody(archiveUrl, manifestUrl));
		}

		return null;
	}
	
	private void makeRequest(String urlStr, List<NameValuePair> pairs) {
		
		HttpClient client= new DefaultHttpClient();
		HttpPost request = new HttpPost(urlStr);

		try {
			request.setEntity(new UrlEncodedFormEntity(pairs));
			HttpResponse resp = client.execute(request);
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

	private URL getPresignedUrl(Date expiery, String s3BasePath, String extension) {
		logger.debug("Url: {}", s3BasePath.concat(extension));
		GeneratePresignedUrlRequest request =
                new GeneratePresignedUrlRequest("cms-review-jandersson", s3BasePath.concat(extension));
        request.setMethod(HttpMethod.GET);
        request.setExpiration(expiery);
        
       return s3Client.generatePresignedUrl(request);
	}

	private String getS3BasePath(PublishJobStorage storage) {
		StringBuilder sb = new StringBuilder();
		sb.append(storage.getPathversion());
		sb.append("/");
		sb.append(storage.getPathcloudid());
		sb.append("/");
		sb.append(storage.getPathconfigname());
		sb.append(storage.getPathdir());
		sb.append("/");
		sb.append(storage.getPathnamebase());
		return sb.toString();
	}
}
