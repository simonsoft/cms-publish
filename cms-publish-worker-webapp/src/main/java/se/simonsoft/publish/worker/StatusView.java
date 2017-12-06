package se.simonsoft.publish.worker;

import java.io.StringWriter;
import java.util.Properties;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

@Path("status")
public class StatusView {
	
	@GET
	@Produces(MediaType.TEXT_HTML)
	public String getStatus() {
		VelocityEngine engine = new VelocityEngine();
		Properties p = new Properties();
		p.setProperty(RuntimeConstants.RESOURCE_LOADER, "class");
		p.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		engine.init(p);

		VelocityContext context = new VelocityContext();
		
		Template template = engine.getTemplate("se/simonsoft/publish/worker/templates/StatusTemplate.vm");
		
		StringWriter wr = new StringWriter();
		template.merge(context, wr);
		
		return wr.toString();
	}
}
