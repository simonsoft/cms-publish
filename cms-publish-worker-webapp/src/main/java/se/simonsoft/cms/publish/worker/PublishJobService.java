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
import java.io.OutputStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.cms.publish.PublishException;
import se.simonsoft.cms.publish.PublishFormat;
import se.simonsoft.cms.publish.PublishSource;
import se.simonsoft.cms.publish.PublishTicket;
import se.simonsoft.cms.publish.abxpe.PublishServicePe;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobOptions;
import se.simonsoft.cms.publish.impl.PublishRequestDefault;

public class PublishJobService {

	private final PublishServicePe pe;
	private final String publishHost = "http://localhost:8080";
	private final String publishPath = "/e3/servlet/e3";
	private final String aptapplicationPrefix;
	
	private static final Logger logger = LoggerFactory.getLogger(PublishJobService.class);
	
	@Inject
	public PublishJobService(PublishServicePe pe, @Named("APTAPPLICATION") String aptapplicationPrefix) {
		this.pe = pe;
		this.aptapplicationPrefix = aptapplicationPrefix;
	}

	public PublishTicket publishJob(PublishJobOptions jobOptions) throws InterruptedException, PublishException {
		if ( jobOptions == null ) {
			throw new NullPointerException("The given PublishJob was null");
		}
		
		logger.debug("Request to publish with job: {}", jobOptions);
		
		PublishRequestDefault request = new PublishRequestDefault();

		PublishFormat format = pe.getPublishFormat(jobOptions.getFormat());
		logger.debug("Will be published with format: {}", format.getFormat());

		request.addConfig("host", publishHost);
		request.addConfig("path", publishPath);
		request = this.getConfigParams(request, jobOptions);
		
		final String itemId = jobOptions.getSource();
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
		logger.debug("Getting OutputStream from job with ticket: {}", ticket.toString());
		if ( ticket.toString() == "" || ticket == null ) {
			throw new IllegalArgumentException("The given ticket was either empty or null");
		}
		if(!isCompleted(ticket)) {
			throw new PublishException("The specified job with ticketnumber " + ticket.toString() + " is not ready yet");
		}
		PublishRequestDefault request = new PublishRequestDefault();
		request.addConfig("host", this.publishHost);
		request.addConfig("path", this.publishPath);
		
		
		if (jobOptions != null && jobOptions.getFormat().equals("web")) {
			logger.debug("Reuested format is web, creating temp file to be able to add root folder.");
			String filePath = writeToTmpFile(jobOptions, ticket, request);
			
			FileInputStream fis = new FileInputStream(filePath);
			PublishZipFolderUtil.addRootFolder(jobOptions.getPathname(), fis, outputStream);
			fis.close();
		} else {
			pe.getResultStream(ticket, request, outputStream);
		}
		
	}
	private PublishRequestDefault getConfigParams(PublishRequestDefault request, PublishJobOptions options) {
		logger.debug("Adding data to the jobs params: [}");
		request.addParam("zip-output", "yes");
		request.addParam("zip-root", options.getPathname());
		request.addParam("type", options.getFormat());
		request.addParam("file-type", "xml");

		Set<Entry<String,String>> entrySet = options.getParams().entrySet();
		for (Map.Entry<String, String> entry : entrySet) {
			request.addParam(entry.getKey(), formatParam(entry.getValue()));
		}
		return request;
	}
	
	private String formatParam(String param) {
		
		final String prefix = "$aptapplication";
		String result;
		
		if (param.startsWith(prefix)) {
			result = this.aptapplicationPrefix.concat(param.substring(prefix.length()));
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
}
