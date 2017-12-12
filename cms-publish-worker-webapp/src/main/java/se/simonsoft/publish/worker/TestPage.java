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
package se.simonsoft.publish.worker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Properties;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;

import se.simonsoft.cms.publish.PublishException;
import se.simonsoft.cms.publish.PublishTicket;
import se.simonsoft.cms.publish.abxpe.PublishServicePe;
import se.simonsoft.cms.publish.databinds.publish.job.PublishJobOptions;
import se.simonsoft.cms.publish.impl.PublishRequestDefault;

@Path("/test")
public class TestPage {

	private String publishHost = "http://localhost:8080";
	private String publishPath = "/e3/servlet/e3";
	private PublishServicePe peService;
	private ObjectReader reader;
	
	@Inject
	public TestPage(PublishServicePe pe, ObjectReader reader) {
		this.peService = pe;
		this.reader = reader;
	}
	

	@POST
	@Produces(MediaType.TEXT_HTML)
	@Path("publish/document")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public String urlCall(@FormParam("itemId") final String itemId, @FormParam("format") String format) throws Exception, InterruptedException, IOException, PublishException {
		if(itemId == "" || itemId == null) {
			throw new IllegalArgumentException("The given itemID was either empty or null");
		}
		if ( format == "" || format == null ) {
			throw new IllegalArgumentException("The given format was either empty or null");
		}
		
		//TODO: Fix pathname (take name from itemId string). Use PE directly?
		int indexOf = itemId.lastIndexOf("/");
		CharSequence subSequence = itemId.subSequence(indexOf, itemId.length());
		String pathName = subSequence.toString();
		
		PublishJobOptions options = new PublishJobOptions();
		options.setSource(itemId);
		options.setFormat(format);
		options.setPathname(pathName);
		options.setType("abxpe");
		PublishJobService service = new PublishJobService(peService);
		PublishTicket ticket= service.publishJob(options);
		
		VelocityEngine engine = new VelocityEngine();
		Properties p = new Properties();
		p.setProperty(RuntimeConstants.RESOURCE_LOADER, "class");
		p.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		engine.init(p);
		VelocityContext context = new VelocityContext();
		context.put("ticketNumber", ticket.toString());
		
		Template template = engine.getTemplate("se/simonsoft/publish/worker/templates/GetTicketTemplate.vm");
		
		StringWriter wr = new StringWriter();
		template.merge(context, wr);
		
		return wr.toString();
	}

	@GET
	@Path("form")
	@Produces(MediaType.TEXT_HTML)
	public String getForm() throws IOException, Exception {
		VelocityEngine engine = new VelocityEngine();
		Properties p = new Properties();
		p.setProperty(RuntimeConstants.RESOURCE_LOADER, "class");
		p.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		engine.init(p);

		VelocityContext context = new VelocityContext();
		Template template = engine.getTemplate("se/simonsoft/publish/worker/templates/DocumentFormTemplate.vm");

		StringWriter wr = new StringWriter();
		template.merge(context, wr);

		return wr.toString();
	}
	
	@Path("ticket")
	@GET
	@Produces(MediaType.TEXT_HTML)
	public String getTicketForm() throws IOException, Exception {
		VelocityEngine engine = new VelocityEngine();
		Properties p = new Properties();
		p.setProperty(RuntimeConstants.RESOURCE_LOADER, "class");
		p.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		engine.init(p);

		VelocityContext context = new VelocityContext();
		
		Template template = engine.getTemplate("se/simonsoft/publish/worker/templates/TicketFormTemplate.vm");
		
		StringWriter wr = new StringWriter();
		template.merge(context, wr);
		return wr.toString();
	}
	
	@GET
	@Path("ticket/result")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response getResult(@QueryParam("ticketnumber") String ticketNumber) throws IOException, PublishException {
		if ( ticketNumber == "" || ticketNumber == null ) {
			throw new IllegalArgumentException("The given ticketnumber was either empty or null");
		}
		PublishRequestDefault request = new PublishRequestDefault();
		PublishTicket ticket = new PublishTicket(ticketNumber);
		
		
		request.addConfig("host", this.publishHost);
		request.addConfig("path", this.publishPath);
		
		Boolean completed = peService.isCompleted(ticket, request);
		if ( !completed ) {
			throw new IllegalStateException("Job is not completed.");
		}
		
		File temp = File.createTempFile("se.simonsoft.publish.worker.test", "");
		FileOutputStream fopStream = new FileOutputStream(temp);
		peService.getResultStream(ticket, request, fopStream);

		ResponseBuilder response = Response.ok(temp, MediaType.APPLICATION_OCTET_STREAM);
	    response.header("Content-Disposition", "attachment; filename=document"+ ticket.toString() +".zip");
	    return response.build();
	}
	
	@GET
	@Produces(MediaType.TEXT_HTML)
	@Path("publish/job")
	public String getPublishJobForm() throws Exception, IOException {
		VelocityEngine engine = new VelocityEngine();
		Properties p = new Properties();
		p.setProperty(RuntimeConstants.RESOURCE_LOADER, "class");
		p.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		engine.init(p);

		VelocityContext context = new VelocityContext();
		
		Template template = engine.getTemplate("se/simonsoft/publish/worker/templates/PublishJobForm.vm");
		
		StringWriter wr = new StringWriter();
		template.merge(context, wr);
		return wr.toString();
	}
	
	@POST
	@Produces(MediaType.TEXT_HTML)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Path("publish/job")
	public String PublishJob(@FormParam("jsonString") String jsonstring) throws JsonProcessingException, IOException, InterruptedException, PublishException, Exception {
		if(jsonstring == "" || jsonstring == null) {
			throw new IllegalArgumentException("The given json String was either empty or null");
		}
		ObjectReader reader = this.reader.forType(PublishJobOptions.class);
		PublishJobOptions job = reader.readValue(jsonstring);

		PublishJobService service = new PublishJobService(peService);
		
		PublishTicket ticket = service.publishJob(job);
		
		VelocityEngine engine = new VelocityEngine();
		Properties p = new Properties();
		p.setProperty(RuntimeConstants.RESOURCE_LOADER, "class");
		p.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		engine.init(p);

		VelocityContext context = new VelocityContext();
		context.put("ticketNumber", ticket.toString());
		
		Template template = engine.getTemplate("se/simonsoft/publish/worker/templates/GetTicketTemplate.vm");
		
		StringWriter wr = new StringWriter();
		template.merge(context, wr);
		
		return wr.toString();
	}
}
