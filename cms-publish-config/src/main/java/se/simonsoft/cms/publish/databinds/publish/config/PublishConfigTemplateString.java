package se.simonsoft.cms.publish.databinds.publish.config;

import java.io.StringWriter;
import java.io.Writer;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

public class PublishConfigTemplateString {
	private String templateString;
	private static VelocityEngine ve;
	private VelocityContext context = new VelocityContext();

	public PublishConfigTemplateString(String templateString) {
		this.templateString = templateString;
		ve = getVelocityEngine();
		ve.setProperty("velocimacro.library.autoreload", false);
		ve.setProperty("file.resource.loader.cache", true);
//		ve.setProperty("file.resource.loader.modificationCheckInterval", -1);
	}
	public void withEntry(String str, Object obj) {
		context.put(str, obj);
	}
	public String evaluate() {
		Writer writer = new StringWriter();
		ve.evaluate(context, writer, "ve.evaluate", templateString);
		return writer.toString();
	}
	private VelocityEngine getVelocityEngine() {	
		return new VelocityEngine();
	}
}