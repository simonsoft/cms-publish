package se.simonsoft.cms.publish.databinds.publish.config;

import java.io.StringWriter;
import java.io.Writer;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

public class PublishConfigTemplateString {
	private String templateString;
	private static VelocityEngine ve;
	private VelocityContext context = new VelocityContext();
	
	/**
	 * @param templateString Velocity String
	 */
	public PublishConfigTemplateString(String templateString) {
		this.templateString = templateString;
		ve = getVelocityEngine();
	}
	/**
	 * Sets up a VelocityContext for later use in withEntry()
	 * @param str Key used in Velocity string
	 * @param obj Object mapped by the key value
	 */
	public void withEntry(String str, Object obj) {
		context.put(str, obj);
	}
	/**
	 * 
	 * @return String evaluated from VelocityContext and String provided in constructor
	 */
	public String evaluate() {
		Writer writer = new StringWriter();
		ve.evaluate(context, writer, "ve.evaluate", templateString);
		return writer.toString();
	}
	private VelocityEngine getVelocityEngine() {	
		return new VelocityEngine();
	}
}