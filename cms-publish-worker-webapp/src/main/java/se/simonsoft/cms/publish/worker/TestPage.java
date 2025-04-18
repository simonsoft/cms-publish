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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.util.Properties;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

import com.fasterxml.jackson.databind.ObjectReader;

import se.simonsoft.cms.publish.PublishException;
import se.simonsoft.cms.publish.PublishTicket;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobOptions;

@Path("/test")
public class TestPage {

	private final PublishJobService publishJobService;
	private ObjectReader reader;
	private final VelocityEngine engine;
	
	@Inject
	public TestPage(PublishJobService publishJobService, ObjectReader reader) {
		this.publishJobService = publishJobService;
		this.reader = reader;
		this.engine = getVelocityEngine();
	}
	
	private VelocityEngine getVelocityEngine() {
		VelocityEngine engine = new VelocityEngine();
		Properties p = new Properties();
		p.setProperty(RuntimeConstants.RESOURCE_LOADER, "class");
		p.setProperty("runtime.references.strict", "true");
		p.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		engine.init(p);
		return engine;
	}
	

	@POST
	@Produces(MediaType.TEXT_HTML)
	@Path("publish/document")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public String doPublishDocument(@FormParam("itemId") final String itemId, @FormParam("format") String format) throws Exception {
		if(itemId == "" || itemId == null) {
			throw new IllegalArgumentException("The given itemID was either empty or null");
		}
		if ( format == "" || format == null ) {
			throw new IllegalArgumentException("The given format was either empty or null");
		}
		
		//TODO: Fix pathname (take name from itemId string). Use PE directly?
		/*
		int indexOf = itemId.lastIndexOf("/");
		CharSequence subSequence = itemId.subSequence(indexOf, itemId.length());
		String pathName = subSequence.toString();
		*/
		String pathName = "TestPublishWorker";
		
		PublishJobOptions options = new PublishJobOptions();
		options.setSource(itemId);
		options.setFormat(format);
		options.setPathname(pathName);
		options.setType("abxpe");

		PublishTicket ticket= publishJobService.publishJob(options);
		
		VelocityContext context = new VelocityContext();
		context.put("ticketNumber", ticket.toString());
		
		Template template = engine.getTemplate("se/simonsoft/cms/publish/worker/templates/GetTicketTemplate.vm");
		
		StringWriter wr = new StringWriter();
		template.merge(context, wr);
		
		return wr.toString();
	}

	@GET
	@Path("publish/document")
	@Produces(MediaType.TEXT_HTML)
	public String getPublishDocumentForm() throws Exception {

		VelocityContext context = new VelocityContext();
		context.put("aptpath", "$aptpath");
		Template template = engine.getTemplate("se/simonsoft/cms/publish/worker/templates/DocumentFormTemplate.vm");

		StringWriter wr = new StringWriter();
		template.merge(context, wr);

		return wr.toString();
	}
	
	@Path("ticket")
	@GET
	@Produces(MediaType.TEXT_HTML)
	public String getTicketForm() throws Exception {

		VelocityContext context = new VelocityContext();
		
		Template template = engine.getTemplate("se/simonsoft/cms/publish/worker/templates/TicketFormTemplate.vm");
		
		StringWriter wr = new StringWriter();
		template.merge(context, wr);
		return wr.toString();
	}
	
	@GET
	@Path("ticket/result")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response getTicketResult(@QueryParam("ticketnumber") String ticketNumber, @QueryParam("options") String  optionsJson) throws IOException, PublishException {
		
		if ( ticketNumber == null || ticketNumber.trim().isEmpty()) {
			throw new IllegalArgumentException("The given ticketnumber was either empty or null");
		}
		
		PublishTicket ticket = new PublishTicket(ticketNumber);
		
		Boolean completed = publishJobService.isCompleted(ticket);
		if ( !completed ) {
			throw new IllegalStateException("Job is not completed.");
		}
		
		PublishJobOptions options = null;
		if (optionsJson != null && !optionsJson.trim().isEmpty()) {
			ObjectReader readerOptions = reader.forType(PublishJobOptions.class);
			options = readerOptions.readValue(optionsJson);
		}
		
		File temp = File.createTempFile("se.simonsoft.publish.worker.test", "");
		FileOutputStream fopStream = new FileOutputStream(temp);
		publishJobService.getCompletedJob(options, ticket, fopStream);

		ResponseBuilder response = Response.ok(temp, MediaType.APPLICATION_OCTET_STREAM);
	    response.header("Content-Disposition", "attachment; filename=document"+ ticket.toString() +".zip");
	    return response.build();
	}
	
	@GET
	@Produces(MediaType.TEXT_HTML)
	@Path("publish/job")
	public String getPublishJobForm() throws Exception {

		VelocityContext context = new VelocityContext();
		context.put("aptpath", "$aptpath");
		Template template = engine.getTemplate("se/simonsoft/cms/publish/worker/templates/PublishJobForm.vm");
		
		StringWriter wr = new StringWriter();
		template.merge(context, wr);
		return wr.toString();
	}
	
	@POST
	@Produces(MediaType.TEXT_HTML)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Path("publish/job")
	public String doPublishJob(@FormParam("jsonString") String jsonstring) throws Exception {
		if(jsonstring == "" || jsonstring == null) {
			throw new IllegalArgumentException("The given json String was either empty or null");
		}
		ObjectReader reader = this.reader.forType(PublishJobOptions.class);
		PublishJobOptions jobOptions = reader.readValue(jsonstring);
		
		PublishTicket ticket = publishJobService.publishJob(jobOptions);

		VelocityContext context = new VelocityContext();
		context.put("ticketNumber", ticket.toString());
		context.put("options", URLEncoder.encode(jsonstring, "UTF-8").replaceAll("\\+", "%20"));
		
		Template template = engine.getTemplate("se/simonsoft/cms/publish/worker/templates/GetTicketTemplate.vm");
		
		StringWriter wr = new StringWriter();
		template.merge(context, wr);
		
		return wr.toString();
	}
}
