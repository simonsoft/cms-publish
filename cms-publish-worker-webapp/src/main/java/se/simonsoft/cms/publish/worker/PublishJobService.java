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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import se.simonsoft.cms.export.storage.CmsExportAwsReaderSingle;
import se.simonsoft.cms.item.export.CmsExportProvider;
import se.simonsoft.cms.item.export.CmsExportReader;
import se.simonsoft.cms.item.export.CmsImportJob;
import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.item.stream.ByteArrayInOutStream;
import se.simonsoft.cms.publish.PublishException;
import se.simonsoft.cms.publish.PublishFormat;
import se.simonsoft.cms.publish.PublishSource;
import se.simonsoft.cms.publish.PublishSourceArchive;
import se.simonsoft.cms.publish.PublishSourceCmsItemId;
import se.simonsoft.cms.publish.PublishTicket;
import se.simonsoft.cms.publish.abxpe.PublishServicePe;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobOptions;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingRecipe;
import se.simonsoft.cms.publish.config.export.PublishExportJobFactory;
import se.simonsoft.cms.publish.impl.PublishRequestDefault;
import se.simonsoft.cms.publish.worker.startup.Environment;

public class PublishJobService {

	private final Map<String, CmsExportProvider> exportProviders;
	private final PublishServicePe pe;
	private final String publishPath = "/e3/servlet/e3";
	private final String aptapplicationPrefix;
	private boolean rootFolderEnable = Boolean.valueOf(new Environment().getParamOptional("APTROOTFOLDERWEBENABLE"));

	private static final Logger logger = LoggerFactory.getLogger(PublishJobService.class);

	@Inject
	public PublishJobService(
			Map<String, CmsExportProvider> exportProviders,
			PublishServicePe pe, 
			@Named("APTAPPLICATION") String aptapplicationPrefix) {

		this.exportProviders = exportProviders;
		this.pe = pe;
		this.aptapplicationPrefix = aptapplicationPrefix;
	}


	public PublishTicket publishJob(PublishJobOptions jobOptions) throws InterruptedException, PublishException {
		if (jobOptions == null) {
			throw new NullPointerException("The given PublishJob was null");
		}

		logger.trace("Request to publish with job: {}", jobOptions);

		PublishRequestDefault request = new PublishRequestDefault();

		PublishFormat format = pe.getPublishFormat(jobOptions.getFormat());
		logger.debug("Request to publish with format: {}", format.getFormat());

		request.addConfig("path", publishPath);
		request = this.getConfigParams(request, jobOptions);

		final PublishSource source;

		if (jobOptions.getSource() == null) {
			if (jobOptions.getStorage() != null) {
				String type = jobOptions.getStorage().getType();
				if (type != null && !type.equals("s3")) {
					String msg = MessageFormatter.format("Failed to import the source. Invalid storage type: {}", type).getMessage();
					throw new IllegalStateException(msg);
				}
				// The source is in the S3 storage
				source = getSourceStreaming(jobOptions, "_document.xml");
			} else {
				throw new NullPointerException("The storage cannot be null when the source is!");
			}
			
		} else if (jobOptions.getSource().endsWith(".zip")) {
			// Test mode, use zip from disk.
			
			source = getSourceLocalFile(jobOptions.getSource(), "_document.xml");
		} else {
			// The source is to be retrieved from a repository
			// PE 8.1.2.0+ no longer supports this.
			final String itemId = jobOptions.getSource();
			logger.debug("Item to publish {}", itemId);
			source = new PublishSourceCmsItemId(new CmsItemIdArg(itemId));
		}

		
		request.setFile(source);
		request.setFormat(format);
		logger.debug("Request is created with file: {} and format {}, sending to PE", source, format);
		PublishTicket ticket = pe.requestPublish(request);
		logger.debug("PE returned a ticket: {}", ticket.toString());

		return ticket;
	}

	public void getCompletedJob(PublishJobOptions jobOptions, PublishTicket ticket, OutputStream outputStream) throws IOException, PublishException {
		logger.debug("Getting OutputStream from job with ticket: {}", ticket);
		if (ticket == null || ticket.toString().isBlank()) {
			throw new IllegalArgumentException("The given ticket was either empty or null");
		}
		if(!isCompleted(ticket)) {
			throw new PublishException("The specified job with ticketnumber " + ticket.toString() + " is not ready yet");
		}
		
		PublishRequestDefault request = new PublishRequestDefault();
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
		logger.trace("Adding data to the jobs params: {}", request);
		request.addParam("zip-output", "yes");
		request.addParam("zip-root", options.getPathname());
		// 'type' parameter is defined by request.setFormat(..)
		/*
		request.addParam("type", options.getFormat());
		*/
		
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
		if (ticket == null) {
			throw new IllegalArgumentException("Ticket must not be null");
		}
		logger.debug("Checking if job with ticket: {} is done", ticket.toString());
		PublishRequestDefault request = new PublishRequestDefault();
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

	private PublishSource getSourceStreaming(PublishJobOptions jobOptions, String inputEntry) {
		CmsImportJob downloadJob = PublishExportJobFactory.getImportJobSingle(jobOptions.getStorage(), "preprocess.zip");
		CmsExportProvider exportProvider = exportProviders.get(jobOptions.getStorage().getType());
		CmsExportReader reader = exportProvider.getReader();
		reader.prepare(downloadJob);
		logger.debug("Prepared CmsExportReader for streaming publish source archive: {}", reader.getMeta().keySet());
		
		Long contentLength = reader.getContentLength();
		Supplier<InputStream> content = new Supplier<InputStream>() {
			
			@Override
			public InputStream get() {
				InputStream is = reader.getContents();
				logger.debug("Supplier providing publish source inputstream: {}", is.getClass());
				return is;
			}
		};
		return new PublishSourceArchive(content, contentLength, inputEntry);
	}
	
	// NOTE: Do NOT use in production!
	private PublishSource getSourceBuffered(PublishJobOptions jobOptions, String inputEntry) {
		CmsImportJob downloadJob = PublishExportJobFactory.getImportJobSingle(jobOptions.getStorage(), "preprocess.zip");
		CmsExportProvider exportProvider = exportProviders.get(jobOptions.getStorage().getType());
		CmsExportReader reader = exportProvider.getReader();
		reader.prepare(downloadJob);
		logger.debug("Prepared CmsExportReader for buffered publish source archive: {}", reader.getMeta().keySet());
		ByteArrayInOutStream baios = new ByteArrayInOutStream();
		try {
			reader.getContents(baios);
		} catch (IOException e) {
			throw new RuntimeException("S3 download failed: " + e.getMessage(), e);
		}
		logger.debug("Downloaded CmsExportReader for buffered publish source archive: {}", reader.getMeta().keySet());
		
		Long contentLength = reader.getContentLength();
		Supplier<InputStream> content = new Supplier<InputStream>() {
			
			@Override
			public InputStream get() {
				InputStream is = baios.getInputStream();
				logger.debug("Supplier providing buffered publish source inputstream: {}", is.getClass());
				return is;
			}
		};
		return new PublishSourceArchive(content, contentLength, inputEntry);
	}
	
	private PublishSource getSourceLocalFile(String filename, String inputEntry) {
		Path path = Path.of("C:\\Temp\\", filename);
		logger.info("Attempt test publish from local file: {}", path);
		
		if (!path.toFile().canRead()) {
			throw new IllegalArgumentException(filename);
		}
		
		Long contentLength = path.toFile().length();
		Supplier<InputStream> content = new Supplier<InputStream>() {
			
			@Override
			public InputStream get() {
				InputStream is;
				try {
					is = new FileInputStream(path.toFile());
					logger.debug("Supplier providing publish source inputstream: {}", is.getClass());
					return is;
				} catch (FileNotFoundException e) {
					throw new IllegalArgumentException(filename);
				}
			}
		};
		return new PublishSourceArchive(content, contentLength, inputEntry);
	}

}
