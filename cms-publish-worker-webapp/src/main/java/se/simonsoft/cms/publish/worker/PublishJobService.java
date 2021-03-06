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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import se.simonsoft.cms.export.aws.CmsExportProviderAwsSingle;
import se.simonsoft.cms.item.export.CmsExportPrefix;
import se.simonsoft.cms.item.export.CmsExportReader;
import se.simonsoft.cms.item.export.CmsImportJob;
import se.simonsoft.cms.publish.PublishException;
import se.simonsoft.cms.publish.PublishFormat;
import se.simonsoft.cms.publish.PublishSource;
import se.simonsoft.cms.publish.PublishTicket;
import se.simonsoft.cms.publish.abxpe.PublishServicePe;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobOptions;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobProgress;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingRecipe;
import se.simonsoft.cms.publish.config.export.PublishExportJobFactory;
import se.simonsoft.cms.publish.impl.PublishRequestDefault;
import se.simonsoft.cms.publish.worker.startup.Environment;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;

public class PublishJobService {

	private final PublishServicePe pe;
	private final String publishHost = "http://localhost:8080";
	private final String publishPath = "/e3/servlet/e3";
	private final String aptapplicationPrefix;
	private final AwsCredentialsProvider credentials;
	private final Region region;
	private boolean rootFolderEnable = Boolean.valueOf(new Environment().getParamOptional("APTROOTFOLDERWEBENABLE"));

	private static final Logger logger = LoggerFactory.getLogger(PublishJobService.class);

	@Inject
	public PublishJobService(PublishServicePe pe, @Named("APTAPPLICATION") String aptapplicationPrefix) {
		this.pe = pe;
		this.aptapplicationPrefix = aptapplicationPrefix;
		this.region = null;
		this.credentials = null;
	}

	@Inject
	public PublishJobService(PublishServicePe pe,
			@Named("APTAPPLICATION") String aptapplicationPrefix,
			AwsCredentialsProvider credentials,
			Region region) {
		this.pe = pe;
		this.aptapplicationPrefix = aptapplicationPrefix;
		this.region = region;
		this.credentials = credentials;
	}

	public PublishTicket publishJob(PublishJobOptions jobOptions) throws InterruptedException, PublishException {
		if (jobOptions == null) {
			throw new NullPointerException("The given PublishJob was null");
		}

		logger.debug("Request to publish with job: {}", jobOptions);

		PublishRequestDefault request = new PublishRequestDefault();

		PublishFormat format = pe.getPublishFormat(jobOptions.getFormat());
		logger.debug("Will be published with format: {}", format.getFormat());

		request.addConfig("host", publishHost);
		request.addConfig("path", publishPath);
		request = this.getConfigParams(request, jobOptions);

		final String itemId;

		if (jobOptions.getSource() == null) {
			if (jobOptions.getStorage() != null) {
				String type = jobOptions.getStorage().getType();
				if (type != null && !type.equals("s3")) {
					String msg = MessageFormatter.format("Failed to import the source. Invalid storage type: {}", type).getMessage();
					throw new IllegalStateException(msg);
				}
				// The source lies on the S3 storage
				itemId = retrieveSource(jobOptions);
			} else {
				throw new NullPointerException("The storage cannot be null when the source is!");
			}
		} else {
			// The source is to be retrieved from a repository
			itemId = jobOptions.getSource();
		}

		logger.debug("Item to publish {}", itemId);
		PublishSource source = new PublishSource() {

			@Override
			public String getURI() {
				return itemId;
			}

		};
		request.setFile(source);
		request.setFormat(format);
		logger.debug("Request is created with file: {} and format {}, sending to PE", source, format);
		PublishTicket ticket = pe.requestPublish(request);
		logger.debug("PE returned a ticket: {}", ticket.toString());

		return ticket;
	}

	public void getCompletedJob(PublishJobOptions jobOptions, PublishTicket ticket, OutputStream outputStream) throws IOException, PublishException {
		logger.debug("Getting OutputStream from job with ticket: {}", ticket);
		if ( ticket.toString() == "" || ticket == null ) {
			throw new IllegalArgumentException("The given ticket was either empty or null");
		}
		if(!isCompleted(ticket)) {
			throw new PublishException("The specified job with ticketnumber " + ticket.toString() + " is not ready yet");
		}
		if (jobOptions != null && jobOptions.getProgress().getParams().containsKey("temp")) {
			deleteTemporaryDirectory(jobOptions.getProgress());
		}
		PublishRequestDefault request = new PublishRequestDefault();
		request.addConfig("host", this.publishHost);
		request.addConfig("path", this.publishPath);
		// #1293: No longer adding the root folder for web output. 
		// Done by CMS Webapp during repackaging instead.
		if (this.rootFolderEnable  && jobOptions != null && jobOptions.getFormat().equals("web")) {
			logger.debug("Reuested format is web, creating temp file to be able to add root folder.");
			String filePath = writeToTmpFile(jobOptions, ticket, request);

			FileInputStream fis = new FileInputStream(filePath);
			PublishZipFolderUtil.addRootFolder(jobOptions.getPathname(), fis, outputStream);
			fis.close();
		} else {
			pe.getResultStream(ticket, request, outputStream);
		}
		logger.debug("Got OutputStream from job with ticket: {}", ticket);
	}
	
	
	private PublishRequestDefault getConfigParams(PublishRequestDefault request, PublishJobOptions options) {
		logger.debug("Adding data to the jobs params: [}");
		request.addParam("zip-output", "yes");
		request.addParam("zip-root", options.getPathname());
		request.addParam("type", options.getFormat());
		request.addParam("file-type", "xml");
		
		PublishProfilingRecipe profiling = options.getProfiling();
		if (profiling != null) {
			String profileExpr = profiling.getLogicalExprDecoded();
			if (profileExpr == null || profileExpr.trim().isEmpty()) {
				throw new IllegalArgumentException("Profiling logicalexpr must not be empty: " + profiling.getName());
			}
			logger.debug("Profiling expr: {}", profileExpr);
			request.addParam("profile", "logicalexpression=".concat(profileExpr));
		}

		if ( options.getParams() != null ) {
			Set<Entry<String,String>> entrySet = options.getParams().entrySet();
			for (Map.Entry<String, String> entry : entrySet) {
				request.addParam(entry.getKey(), formatParam(entry.getValue(), options.getPathname()));
			}
		}
		return request;
	}

	private String formatParam(String param, String pathName) {

		final String prefix = "$aptapplication";
		final String prefixPath = "$pathname";
		String result;

		if (param.startsWith(prefix)) {
			result = this.aptapplicationPrefix.concat(param.substring(prefix.length()));
		} else if (param.startsWith(prefixPath)){
			result = pathName.concat(param.substring(prefixPath.length()));
		} else {
			result = param;
		}
		return result;
	}

	public boolean isCompleted(PublishTicket ticket) throws PublishException {
		logger.debug("Checking if job with ticket: {} is done", ticket.toString());
		PublishRequestDefault request = new PublishRequestDefault();
		request.addConfig("host", this.publishHost);
		request.addConfig("path", this.publishPath);
		return pe.isCompleted(ticket, request);
	}

	private String writeToTmpFile(PublishJobOptions jobOptions, PublishTicket ticket, PublishRequestDefault request) throws IOException, PublishException {

		File temp = File.createTempFile(jobOptions.getPathname(), ".tmp");
		FileOutputStream fout = new FileOutputStream(temp);
		pe.getResultStream(ticket, request, fout);
		logger.debug("Temporary file placed at: {}", temp.getAbsolutePath());
		fout.flush();
		fout.close();

		return temp.getAbsolutePath();
	}

	private String retrieveSource(PublishJobOptions jobOptions) {
		CmsImportJob downloadJob = PublishExportJobFactory.getImportJobSingle(jobOptions.getStorage(), "preprocess.zip");
		CmsExportPrefix cmsPrefix = new CmsExportPrefix(jobOptions.getStorage().getPathversion());
		String cloudId = jobOptions.getStorage().getPathcloudid();
		String bucketName = jobOptions.getStorage().getParams().get("s3bucket");
		CmsExportProviderAwsSingle exportProvider = new CmsExportProviderAwsSingle(cmsPrefix, cloudId, bucketName, region, credentials);
		CmsExportReader reader = exportProvider.getReader();
		reader.prepare(downloadJob);
		InputStream contents = reader.getContents();
		ZipInputStream zis = new ZipInputStream(contents);
		try {
			Path temp = Files.createTempDirectory(null);
			jobOptions.getProgress().getParams().put("temp", temp.toString());
			ZipEntry zipEntry = zis.getNextEntry();
			while(zipEntry != null) {
				logger.debug("Unzipping: {}", zipEntry.getName());
				Path path = Paths.get(temp.toString(), zipEntry.getName());
				Files.createDirectories(path.getParent());
				FileOutputStream fos = new FileOutputStream(path.toString());
				IOUtils.copy(zis, fos);
				fos.close();
				zis.closeEntry();
				zipEntry = zis.getNextEntry();
			}
			return temp.toString() + "/_document.xml";
		} catch (IOException e) {
			logger.debug("Error when trying to download new zip entries: {}", e.getMessage());
			throw new RuntimeException(e);
		}
	}

	private void deleteDirectory(Path path) throws IOException {
		if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
			try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
				for (Path entry : entries) {
					deleteDirectory(entry);
				}
			}
		}
		Files.delete(path);
	}

	private void deleteTemporaryDirectory(PublishJobProgress progress) {
		try {
			String path = progress.getParams().get("temp");
			logger.debug("Deleting the temporary directory: {}", path);
			deleteDirectory(Paths.get(path));
			progress.getParams().remove("temp");
		} catch (IOException e) {
			logger.warn("Failed to delete the temporary directory: {}", e.getMessage(), e);
		}
	}
}
