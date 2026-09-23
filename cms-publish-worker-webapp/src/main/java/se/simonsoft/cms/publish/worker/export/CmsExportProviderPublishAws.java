package se.simonsoft.cms.publish.worker.export;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import se.simonsoft.cms.export.aws.CmsExportProviderAwsSingle;
import se.simonsoft.cms.item.export.CmsExportPrefix;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;



@Singleton
public class CmsExportProviderPublishAws extends CmsExportProviderAwsSingle /* implements ServletContextListener */ {
	
	@Inject
	public CmsExportProviderPublishAws(
			@Named("config:se.simonsoft.cms.cloudid") String cloudId, 
			@Named("config:se.simonsoft.cms.publish.bucket") String bucketName,
    		Region region, 
    		AwsCredentialsProvider credentials,
    		S3Configuration s3Configuration
			) {
		
		super(new CmsExportPrefix("cms4"), cloudId, bucketName, region, credentials, s3Configuration, Optional.empty());
		//CmsRestServletContextListener.registerDestroyListener(this);
	}

	@Override
	protected SdkAsyncHttpClient getAsyncHttpClient() {
		
		software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient.Builder builder = NettyNioAsyncHttpClient.builder()
				//.connectionAcquisitionTimeout(Duration.ofSeconds(5))
				.maxConcurrency(4);
		return builder.build();
	}
	
	
	// Copied from Wink webapp.
	// TODO: Wire up to this webapp.
	/*
	
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		// Registering in constructor.
	}

	// TODO: Unfortunately unable to get the contextDestroyed event from ServletContextListener in Wink on Tomcat 9. It used to work on Tomcat 7.
	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		this.shutdown();
	}
	*/

}
