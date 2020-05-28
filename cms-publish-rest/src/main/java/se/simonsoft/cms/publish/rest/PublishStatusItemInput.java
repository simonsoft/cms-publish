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

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.item.workflow.WorkflowItemInput;

public final class PublishStatusItemInput implements WorkflowItemInput {

	private String action;
	private String itemid;

	public PublishStatusItemInput(
			String itemid,
			String action
	) {
		this.itemid = itemid;
		this.action = action;
	}

	@Override
	public CmsItemId getItemId()  {
		return new CmsItemIdArg(this.itemid);
	}

	@Override
	public String getAction() {
		return this.action;
	}

	public void setAction(String action) {
		this.action = action;
	}
}
