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

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.management.RuntimeErrorException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.config.CmsResourceContext;
import se.simonsoft.cms.item.events.ItemChangedEventListener;
import se.simonsoft.cms.item.info.CmsRepositoryLookup;
import se.simonsoft.cms.item.workflow.WorkflowExecutor;
import se.simonsoft.cms.item.workflow.WorkflowItemInput;
import se.simonsoft.cms.publish.config.filter.PublishConfigFilter;
import se.simonsoft.cms.publish.databinds.publish.config.PublishConfig;

public class PublishItemChangedEventListener implements ItemChangedEventListener {

	private final CmsRepositoryLookup lookup;
	private final WorkflowExecutor<WorkflowItemInput> workflowExecutor;
	private List<PublishConfigFilter> filters;
	private ObjectWriter writer;
	private ObjectReader reader;

	private static final Logger logger = LoggerFactory.getLogger(PublishItemChangedEventListener.class);

	@Inject
	public PublishItemChangedEventListener(
			CmsRepositoryLookup lookup,
			@Named("config:se.simonsoft.cms.aws.workflow") WorkflowExecutor<WorkflowItemInput> workflowExecutor,
			List<PublishConfigFilter> filters,
			ObjectWriter writer,
			ObjectReader reader) {
		
		this.lookup = lookup;
		this.workflowExecutor = workflowExecutor;
		this.filters = filters;
		this.writer = writer;
		this.reader = reader;
	}

	@Override
	public void onItemChange(CmsItem item) {
		logger.debug("publish");
		CmsResourceContext context = this.lookup.getConfig(item.getId(), item.getKind());
		PublishConfig parseConfig = parseConfig(context);
		
		for (PublishConfigFilter filter: filters) {
			filter.accept(parseConfig, item);
		}
		
	}
	
	
	private PublishConfig parseConfig(CmsResourceContext context) {
		logger.debug("Parsing context to PublishConfig... ");
		ObjectWriter contextWriter = writer.forType(CmsResourceContext.class);
		ObjectReader configReader = reader.forType(PublishConfig.class);
		PublishConfig config = null;
		try {
			String contextStr = contextWriter.writeValueAsString(context);
			config = configReader.readValue(contextStr);
			
		} catch (JsonProcessingException e) {
			logger.error("Could not parse ResourceContext to a String");
			throw new IllegalArgumentException("PublishItem needs a valid CmsResourceContext", e);
		} catch (IOException e) {
			logger.debug("Trying to deserilze context to publishConfig caused filed with: {}", e.getMessage());
			throw new RuntimeException();
		}
		
		return config; 
	}
	
	private String evaluatePathNameTmpl(String template, CmsItem item) {
		PublishConfigTemplateString tmplStr = new PublishConfigTemplateString(template);
		tmplStr.withEntry("item", item);
		return tmplStr.evaluate();
	}
}

