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
package se.simonsoft.cms.publish.worker;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.cms.publish.worker.startup.WorkerApplication;
import se.simonsoft.cms.version.CmsComponentVersion;
import se.simonsoft.cms.version.CmsComponents;


@Path("versions")
public class VersionsResource {
	
	
	private static final Logger logger = LoggerFactory.getLogger(VersionsResource.class);

	@GET
	@Produces(MediaType.TEXT_HTML)
	public String getVersions() throws Exception {
		VelocityEngine engine = new VelocityEngine();
		Properties p = new Properties();
		p.setProperty(RuntimeConstants.RESOURCE_LOADER, "class");
		p.setProperty("runtime.references.strict", "true");
		p.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		engine.init(p);
		
		
		VelocityContext context = new VelocityContext();
		//Name of dep and version (String) defined in pom.xml
		Map<String, String> versions = new HashMap<String, String>();
		for (String name: CmsComponents.getNames()) {
			CmsComponentVersion version = CmsComponents.getVersion(name);
			versions.put(name, version.getVersion());
		}
		
		context.put("depVersions", versions);
		logger.debug("Versions webapp: {}", WorkerApplication.getWebappVersion().toString());
		context.put("webappVersion", WorkerApplication.getWebappVersion().toString());
		
		Template template = engine.getTemplate("se/simonsoft/publish/worker/templates/VersionsTemplate.vm");
		
		StringWriter wr = new StringWriter();
		template.merge(context, wr);
		return wr.toString();
	}
	
}
