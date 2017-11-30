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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

import se.simonsoft.cms.publish.PublishException;
import se.simonsoft.cms.publish.PublishFormat;
import se.simonsoft.cms.publish.PublishSource;
import se.simonsoft.cms.publish.PublishTicket;
import se.simonsoft.cms.publish.abxpe.PublishServicePe;
import se.simonsoft.cms.publish.impl.PublishRequestDefault;

@Path("/test")
public class TestPage {

	private String publishHost = "http://localhost:8080";
	private String publishPath = "/e3/servlet/e3";
	private PublishServicePe peService;
	
	@Inject
	public TestPage(PublishServicePe pe) {
		this.peService = pe;
	}
	

	@POST
	@Produces(MediaType.TEXT_PLAIN)
	@Path("publish")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public String urlCall(@FormParam("itemId") final String itemId, @FormParam("format") String format) throws InterruptedException, IOException, PublishException {
		if(itemId == "" || itemId == null) {
			throw new IllegalArgumentException("The given itemID was either empty or null");
		}
		if ( format == "" || format == null ) {
			throw new IllegalArgumentException("The given format was either empty or null");
		}
		
		PublishFormat publishFormat = peService.getPublishFormat(format);
		PublishRequestDefault request = new PublishRequestDefault();

		// Add config
		request.addConfig("host", this.publishHost);
		request.addConfig("path", this.publishPath);

		PublishSource source = new PublishSource() {

			@Override
			public String getURI() {
				return itemId;
			}
		};
		request.setFile(source);
		request.setFormat(publishFormat);
		PublishTicket ticket = peService.requestPublish(request);
		//TODO: Return link to get job page with included ticket number.
		return "PE is done! Your ticket number is: " + ticket.toString();
	}

	@GET
	@Path("form")
	@Produces(MediaType.TEXT_HTML)
	public String getForm() throws IOException {
		VelocityEngine engine = new VelocityEngine();
		Properties p = new Properties();
		p.setProperty(RuntimeConstants.RESOURCE_LOADER, "class");
		p.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		engine.init(p);

		VelocityContext context = new VelocityContext();
		//TODO: refactor naming of forms.
		Template template = engine.getTemplate("se/simonsoft/publish/worker/templates/formTemplate.vm");

		StringWriter wr = new StringWriter();
		template.merge(context, wr);

		return wr.toString();
	}
	
	@Path("ticket")
	@GET
	@Produces(MediaType.TEXT_HTML)
	public String getTicketForm() {
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
}
