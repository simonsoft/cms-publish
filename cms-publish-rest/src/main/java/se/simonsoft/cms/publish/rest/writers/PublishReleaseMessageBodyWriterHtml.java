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
package se.simonsoft.cms.publish.rest.writers;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.io.VelocityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.repos.web.ReposHtmlHelper;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.publish.rest.PublishRelease;

import javax.inject.Inject;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

@Provider
@Produces(MediaType.TEXT_HTML)
public class PublishReleaseMessageBodyWriterHtml implements MessageBodyWriter<PublishRelease> {

	private static final Logger logger = LoggerFactory.getLogger(PublishReleaseMessageBodyWriterHtml.class);
	private ReposHtmlHelper reposHtmlHelper;
	private VelocityEngine templateEngine;

	@Inject
	public PublishReleaseMessageBodyWriterHtml(ReposHtmlHelper reposHtmlHelper, VelocityEngine templateEngine) {
		this.reposHtmlHelper = reposHtmlHelper;
		this.templateEngine = templateEngine;
	}

	@Override
	public long getSize(PublishRelease publishRelease, Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
		return -1;
	}

	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotaions, MediaType mediaType) {
		return type == PublishRelease.class;
	}

	@Override
	public void writeTo(PublishRelease publishRelease, Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> multivaluedMap, OutputStream outputStream) throws IOException {
		logger.debug("Serializing publish release information to html");

		CmsItemId itemId = publishRelease.getItem().getId();
		VelocityContext context = new VelocityContext();
		Template template = null;

		try {
			template = templateEngine.getTemplate("se/simonsoft/cms/publish/rest/export-release-form.vm");
		} catch (ResourceNotFoundException e) {
			throw new IllegalStateException("Requested html template do not exist.", e);
		} catch (ParseErrorException e) {
			throw new IllegalStateException("Could not parse html template.", e);
		} catch (Exception e) {
			throw new IllegalStateException("Failed when trying to get template.", e);
		}

		context.put("item", publishRelease.getItem());
		context.put("itemProfiling", publishRelease.getProfiling());
		context.put("configuration", publishRelease.getConfig());
		context.put("releaseExecutions", publishRelease.getReleaseExecutions());
		context.put("translationExecutions", publishRelease.getTranslationExecutions());
		context.put("reposHeadTags", reposHtmlHelper.getHeadTags(null));

		VelocityWriter writer = new VelocityWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
		template.merge(context, writer);
		writer.flush();
	}
}