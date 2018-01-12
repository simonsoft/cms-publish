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
package se.simonsoft.cms.publish.worker;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;

import com.amazonaws.auth.AWSCredentialsProvider;

import se.simonsoft.cms.export.storage.CmsExportAwsWriterSingle;
import se.simonsoft.cms.export.storage.CmsExportDavWriterSingle;
import se.simonsoft.cms.item.export.CmsExportWriter;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobOptions;

public class PublishExportWriterProvider {
	
	private String cloudId;
	private String bucketName;
	private AWSCredentialsProvider credentials;
	private File fsParent;
	
	@Inject
	public PublishExportWriterProvider(
			@Named("config:se.simonsoft.cms.cloudid") String cloudId, 
    		@Named("config:se.simonsoft.cms.aws.bucket.name") String bucketName, 
    		AWSCredentialsProvider credentials) {
		
		this.cloudId = cloudId;
		this.bucketName = bucketName;
		this.credentials = credentials;
		
	}
	
	@Inject
	public PublishExportWriterProvider(@Named("config:se.simonsoft.cms.dav.local") File fsParent) {
		
		this.fsParent = fsParent;
		
		throw new UnsupportedOperationException("PublishExportWriterProvider can not yet provide a dav writer.");
		
	}
	
	public CmsExportWriter getWriter(PublishJobOptions options) {
		
		if (options == null) {
			throw new IllegalArgumentException("Provider need a valid PublishJobOptions object");
		}
		
		String storageType = options.getStorage().getType();
		
		if (storageType == null) {
			throw new IllegalArgumentException("No storage type selected");
		}
		
		CmsExportWriter exportWriter = null;
		if (storageType.trim().equals("s3")) {
			exportWriter = new CmsExportAwsWriterSingle(cloudId, bucketName, credentials); 
		} else if (storageType.trim().equals("fs")) {
			exportWriter = new CmsExportDavWriterSingle(fsParent, null); //TODO: We will need to refactor dav writer into fs writer without the secret and expiry.
		} else {
			throw new IllegalArgumentException("Provider can only provid writers for s3 and fs, requsted writer: " + storageType);
		}
		
		return exportWriter; 
	}

}
