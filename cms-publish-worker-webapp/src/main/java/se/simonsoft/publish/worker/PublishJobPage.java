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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import se.simonsoft.cms.publish.PublishFormat;
import se.simonsoft.cms.publish.PublishSource;
import se.simonsoft.cms.publish.PublishTicket;
import se.simonsoft.cms.publish.abxpe.PublishServicePe;
import se.simonsoft.cms.publish.databinds.publish.config.PublishConfig;
import se.simonsoft.cms.publish.databinds.publish.job.PublishJob;
import se.simonsoft.cms.publish.impl.PublishRequestDefault;

@Path("publishjobservice")
public class PublishJobPage {
	
	ObjectMapper mapper;
	ObjectReader reader;
	private String publishHost = "http://localhost:8080";
	private String publishPath = "/e3/servlet/e3";
	
	@Inject
	public PublishJobPage(ObjectMapper mapper, PublishServicePe pe) {
		this.mapper = mapper;
	}
	
	@GET
	@Path("publishjobform")
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
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("publishjob")
	public String PublishJob(@FormParam("jsonString") String jsonString) throws JsonProcessingException, IOException, InterruptedException {
		if(jsonString == "" || jsonString == null) {
			throw new IllegalArgumentException("The given json String was either empty or null");
		}
		
		reader = mapper.reader(PublishJob.class);
		PublishJob job = reader.readValue(jsonString);
		
		PublishJobService service = new PublishJobService();
		PublishTicket publishJob = service.PublishJob(job);
		
		return null;
		
	}
}