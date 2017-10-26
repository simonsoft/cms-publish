package se.simonsoft.cms.publish.databinds.publish.config;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

public class PublishConfigTemplateString {
	private String templateString;
	private static VelocityEngine ve;
	private VelocityContext context = new VelocityContext();
	
	public PublishConfigTemplateString(String templateString) {
		this.templateString = templateString;
		ve = getVelocityEngine();
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