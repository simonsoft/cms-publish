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
package se.simonsoft.cms.publish.databinds.publish.config;

import java.io.StringWriter;
import java.io.Writer;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

public class PublishConfigTemplateString {
	private static VelocityEngine ve;
	private VelocityContext context = new VelocityContext();
	
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
	public String evaluate(String templateString) {
		Writer writer = new StringWriter();
		getVelocityEngine().evaluate(context, writer, "PublishConfigTemplateString", templateString);
		return writer.toString();
	}
	private VelocityEngine getVelocityEngine() {
		if(this.ve != null) {
			return this.ve;
		}
		VelocityEngine ve = new VelocityEngine();
		ve.init();
		
		this.ve = ve;
		return this.ve;
	}
}