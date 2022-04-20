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
package se.simonsoft.cms.publish.rest.cdn;

import se.simonsoft.cms.item.CmsItemId;

public class PublishCdnItem {

	private CmsItemId itemId;
	private String format; // job format, add additional field if we need CDN formatsuffix (already in pathformat).
	private String cdn;
	private String pathname;
	private String pathformat;
	
	
	public CmsItemId getItemId() {
		return itemId;
	}
	public void setItemId(CmsItemId itemId) {
		this.itemId = itemId;
	}
	public String getFormat() {
		return format;
	}
	public void setFormat(String format) {
		this.format = format;
	}
	public String getCdn() {
		return cdn;
	}
	public void setCdn(String cdn) {
		this.cdn = cdn;
	}
	public String getPathname() {
		return pathname;
	}
	public void setPathname(String pathname) {
		this.pathname = pathname;
	}
	public String getPathformat() {
		return pathformat;
	}
	public void setPathformat(String pathformat) {
		this.pathformat = pathformat;
	}
	
	
	
	
	
}
