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
import javax.ws.rs.core.MediaType;

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

@Path("/publishjobservice")
public class PublishJobPage {
	
	private ObjectReader reader;
	private PublishServicePe pe;
	
	@Inject
	public PublishJobPage(ObjectReader reader, PublishServicePe pe) {
		this.reader = reader;
		this.pe = pe;
	}
	
	@GET
	@Produces(MediaType.TEXT_HTML)
	public String getPublishJobForm() {
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
	@Path("publishjob")
	public String PublishJob(@FormParam("jsonString") String jsonstring) throws JsonProcessingException, IOException, InterruptedException, PublishException {
		if(jsonstring == "" || jsonstring == null) {
			throw new IllegalArgumentException("The given json String was either empty or null");
		}
		ObjectReader reader = this.reader.forType(PublishJobOptions.class);
		PublishJobOptions job = reader.readValue(jsonstring);

		PublishJobService service = new PublishJobService(pe);
		
		PublishTicket ticket = service.publishJob(job);
		
		return "PE is done!" + ticket.toString() +"";
		
	}
}