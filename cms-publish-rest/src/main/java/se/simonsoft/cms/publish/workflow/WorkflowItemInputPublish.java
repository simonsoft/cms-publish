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
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.item.workflow.WorkflowItemInput;
import se.simonsoft.cms.publish.databinds.publish.job.PublishJob;

public class WorkflowItemInputPublish implements WorkflowItemInput {
	
	private static final String PUBLISH = "publish";
	private CmsItemId itemId;
	private final PublishJob publishJob;
	
	public WorkflowItemInputPublish(CmsItemId itemId, PublishJob publishJob) {
		this.itemId = itemId;
		this.publishJob = publishJob;
	}

	@Override
	public String getAction() {
		return PUBLISH;
	}
	
	@JsonSetter("itemid")
	public void setId(String itemId) {
		this.itemId = new CmsItemIdArg(itemId);
	}
	
	@JsonGetter("itemid")
	public String getItemIdJson() {
		return this.itemId.getLogicalIdFull();
	}

	@Override
	@JsonIgnore
	public CmsItemId getItemId() {
		return this.itemId;
	}
	
	public PublishJob getPublishJob() {
		return publishJob;
	}

}
