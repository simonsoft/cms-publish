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

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.config.CmsResourceContext;
import se.simonsoft.cms.item.events.ItemChangedEventListener;
import se.simonsoft.cms.item.info.CmsRepositoryLookup;
import se.simonsoft.cms.item.workflow.WorkflowExecutor;
import se.simonsoft.cms.item.workflow.WorkflowItemInput;

public class PublishItemChangedEventListener implements ItemChangedEventListener {

	private final CmsRepositoryLookup lookup;
	private final WorkflowExecutor<WorkflowItemInput> workflowExecutor;

	private static final Logger logger = LoggerFactory.getLogger(PublishItemChangedEventListener.class);

	@Inject
	public PublishItemChangedEventListener(
			CmsRepositoryLookup lookup,
			@Named("config:se.simonsoft.cms.aws.workflow") WorkflowExecutor<WorkflowItemInput> workflowExecutor) {
		
		this.lookup = lookup;
		this.workflowExecutor = workflowExecutor;
	}

	@Override
	public void onItemChange(CmsItem item) {
		logger.debug("publish");
		CmsResourceContext config = this.lookup.getConfig(item.getId(), item.getKind());
		//TODO: get read config and start a publish.
	}
	
}

