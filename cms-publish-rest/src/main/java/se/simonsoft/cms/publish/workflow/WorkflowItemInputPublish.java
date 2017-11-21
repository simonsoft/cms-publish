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
package se.simonsoft.cms.publish.workflow;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.workflow.WorkflowItemInput;
import se.simonsoft.cms.publish.databinds.publish.job.PublishJob;

public class WorkflowItemInputPublish implements WorkflowItemInput {
	
	private static final String PUBLISH = "publish";
	private final CmsItemId itemId;
	private final PublishJob publishJob;
	
	public WorkflowItemInputPublish(CmsItemId itemId, PublishJob publishJob) {
		this.itemId = itemId;
		this.publishJob = publishJob;
	}

	@Override
	public String getAction() {
		return PUBLISH;
	}

	@Override
	public CmsItemId getItemId() {
		return this.itemId;
	}
	
	public PublishJob getPublishJob() {
		return publishJob;
	}

}
