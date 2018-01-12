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
import se.simonsoft.cms.item.dav.DavShareArea;
import se.simonsoft.cms.item.export.CmsExportWriter;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobOptions;

public class PublishExportWriterProvider {
	
	private String cloudId;
	private String bucketName;
	private AWSCredentialsProvider credentials;
	private File davParent;
	private DavShareArea davShareArea;
	
	@Inject
	public PublishExportWriterProvider(@Named("config:se.simonsoft.cms.cloudid") String cloudId, 
    		@Named("config:se.simonsoft.cms.aws.bucket.name") String bucketName, 
    		AWSCredentialsProvider credentials) {
		
		this.cloudId = cloudId;
		this.bucketName = bucketName;
		this.credentials = credentials;
		
	}
	
	@Inject
	public PublishExportWriterProvider(@Named("config:se.simonsoft.cms.dav.local") File davParent, DavShareArea davShareArea) {
		
		this.davParent = davParent;
		this.davShareArea = davShareArea;
		
		throw new UnsupportedOperationException("PublishExportWriterProvider can not yet provide a dav writer.");
		
	}
	
	public CmsExportWriter getWriter(PublishJobOptions options) {
		
		//TODO: what should options object contain to choose a local writer?
		if (options.getParams().get("storage") != null && options.getParams().get("storage").equals("local")) {
			//TODO: Implement with CmsExportDavWriterSingle 
			throw new UnsupportedOperationException("Exporting the files to local disk is not yet implemented.");
		}
		
		return new CmsExportAwsWriterSingle(cloudId, bucketName, credentials);
	}

}
