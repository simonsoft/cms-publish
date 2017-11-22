package se.simonsoft.publish.worker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Map;

import javax.inject.Inject;

import se.simonsoft.cms.publish.PublishException;
import se.simonsoft.cms.publish.PublishFormat;
import se.simonsoft.cms.publish.PublishTicket;
import se.simonsoft.cms.publish.abxpe.PublishServicePe;
import se.simonsoft.cms.publish.databinds.publish.job.*;
import se.simonsoft.cms.publish.impl.PublishRequestDefault;
import se.simonsoft.cms.publish.PublishSource;

public class PublishJobService {

	private final PublishServicePe pe;
	private String publishHost = "http://localhost:8080";
	private String publishPath = "/e3/servlet/e3";
	
	@Inject
	public PublishJobService(PublishServicePe pe) {
		this.pe = pe;
	}

	public PublishTicket publishJob(PublishJobOptions job) throws InterruptedException, PublishException {
		if ( job == null ) {
			throw new NullPointerException("The given PublishJob was null");
		}
		PublishRequestDefault request = new PublishRequestDefault();

		PublishFormat format = pe.getPublishFormat(job.getFormat());

		request.addConfig("host", publishHost);
		request.addConfig("path", publishPath);
		request = this.getConfigParams(request, job);
		
		final String itemId = job.getSource();
		PublishSource source = new PublishSource() {
			
			@Override
			public String getURI() {
				return itemId;
			}
		};
		request.setFile(source);
		request.setFormat(format);
		PublishTicket ticket = pe.requestPublish(request);
		
		boolean isComplete = false;
		while (!isComplete ) {
			Thread.sleep(1000);
			isComplete = pe.isCompleted(ticket, request);
		}
		return ticket;
	}
	
	public String getCompletedJob(PublishTicket ticket) throws IOException, PublishException {
		if ( ticket.toString() == "" || ticket == null ) {
			throw new IllegalArgumentException("The given ticket was either empty or null");
		}
		PublishRequestDefault request = new PublishRequestDefault();
		request.addConfig("host", this.publishHost);
		request.addConfig("path", this.publishPath);
		
		File temp = File.createTempFile("se.simonsoft.publish.worker.file", "");
		FileOutputStream fopStream = new FileOutputStream(temp);
		pe.getResultStream(ticket, request, fopStream);
		
		InputStream stream = new FileInputStream(temp);
		StringBuilder sb = new StringBuilder();
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
		String line = reader.readLine();

		while(line !=null) {
			sb.append(line);
			sb.append("\n");
			line = reader.readLine();
		}
		reader.close();
		
		return sb.toString();
	}

	private PublishRequestDefault getConfigParams(PublishRequestDefault request, PublishJobOptions job) {
		PublishJobOptions options = job;
		request.addParam("zip-output", "yes");
		request.addParam("zip-root", options.getPathname());
		request.addParam("type", options.getType());
		request.addParam("format", options.getFormat());

		Iterator iterator = options.getParams().entrySet().iterator();
		while ( iterator.hasNext() ) {
			Map.Entry pair = (Map.Entry) iterator.next();
			request.addParam(pair.getKey().toString(), pair.getValue().toString());
		}
		return request;
	}
}
