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
package se.simonsoft.cms.publish.config;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.tools.generic.EscapeTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

public class PublishConfigTemplateString {
	private static VelocityEngine ve;
	private VelocityContext context;
	
	private static final Logger logger = LoggerFactory.getLogger(PublishConfigTemplateString.class);
	
	
	public PublishConfigTemplateString() {
		
		this.context = new VelocityContext();
		this.context.put("esc", new EscapeTool());
	}
	
	/**
	 * Sets up a VelocityContext for later use in evaluate()
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
	public String evaluate(String templateString) {
		Writer writer = new StringWriter();
		try {
			getVelocityEngine().evaluate(context, writer, "PublishConfigTemplateString", templateString);
		} catch (VelocityException e) {
			String msg = MessageFormatter.format("Failed to evaluate template string: '{}' - {}", templateString, e.getMessage()).getMessage();
			logger.error(msg, e);
			throw new RuntimeException(msg, e);
		}
		return writer.toString();
	}
	private VelocityEngine getVelocityEngine() {
		if(PublishConfigTemplateString.ve != null) {
			return PublishConfigTemplateString.ve;
		}
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty("runtime.references.strict", true);
		Properties props = new Properties();
		//Disable velocity logging.
		props.put("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.NullLogChute");
		ve.init(props);
		
		PublishConfigTemplateString.ve = ve;
		return PublishConfigTemplateString.ve;
	}
}