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
package se.simonsoft.cms.publish.rest;

import java.awt.event.ItemEvent;
import java.io.StringWriter;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.cms.item.impl.CmsItemIdArg;

@Path("/publish4")
public class PublishResource {
	
	private final String hostname;

	@Inject
	public PublishResource(@Named("config:se.simonsoft.cms.hostname") String hostname) {
		this.hostname = hostname;
	}
	
	private static final Logger logger = LoggerFactory.getLogger(PublishResource.class);
	
	@GET
	@Path("release")
	@Produces(MediaType.TEXT_HTML)
	public String getReleaseForm(@QueryParam("item") CmsItemIdArg itemId) throws Exception {
		
		if (itemId == null) {
			throw new IllegalArgumentException("Field 'item': required");
		}
		
		VelocityEngine engine = new VelocityEngine();
		Properties p = new Properties();
		p.setProperty(RuntimeConstants.RESOURCE_LOADER, "class");
		p.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		engine.init(p);

		VelocityContext context = new VelocityContext();
		Template template = engine.getTemplate("se/simonsoft/cms/publish/config/templates/batch-publish-template.vm");
		
		context.put("item", itemId);

		StringWriter wr = new StringWriter();
		template.merge(context, wr);

		return wr.toString();
	}
	
	@GET
	@Path("release/download")
	@Produces("application/zip")
	public Response getDownload(@QueryParam("item") CmsItemIdArg itemId,
						@QueryParam("includemaster") boolean includeMaster,
						@QueryParam("includetranslations") boolean includeTranslations,
						@QueryParam("profiling") String[] profiling,
						@QueryParam("publication") String publication) throws Exception {
		
		if (itemId == null) {
			throw new IllegalArgumentException("Field 'item': required");
		}
		
		itemId.setHostnameOrValidate(this.hostname);
		
		if (!itemId.isPegged()) {
			throw new IllegalArgumentException("Field 'item': revision is required");
		}
		
		logger.debug("Not yet implemented");
		
		//TODO: Should produce zip output. 
		return Response.ok("Succesfully called get release/download").build();
	}
	
	
}
