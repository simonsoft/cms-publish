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
package se.simonsoft.cms.publish.config.databinds.config;

public class PublishConfigArea {

	private String type = null;
	private String pathnameTemplate;
	private String docnoDocumentTemplate;
	private String docnoMasterTemplate;
	
	public PublishConfigArea() {
		
	}
	
	
	public String getType() {
		return type;
	}


	public void setType(String type) {
		this.type = type;
	}


	public String getPathnameTemplate() {
		return this.pathnameTemplate;
	}

	public void setPathnameTemplate(String pathnameTemplate) {
		this.pathnameTemplate = pathnameTemplate;
	}

	public String getDocnoDocumentTemplate() {
		return this.docnoDocumentTemplate;
	}

	public void setDocnoDocumentTemplate(String docnoDocumentTemplate) {
		this.docnoDocumentTemplate = docnoDocumentTemplate;
	}

	public String getDocnoMasterTemplate() {
		return this.docnoMasterTemplate;
	}

	public void setDocnoMasterTemplate(String docnoMasterTemplate) {
		this.docnoMasterTemplate = docnoMasterTemplate;
	}

	
	


}
