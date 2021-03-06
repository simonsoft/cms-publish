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
package se.simonsoft.cms.publish.config.databinds.job;

import se.simonsoft.cms.publish.config.databinds.config.PublishConfigStorage;

public class PublishJobStorage extends PublishConfigStorage {
	
	private String pathversion;
	private String pathcloudid;
	private String pathconfigname;
	private String pathdir;
	private String pathnamebase;
	
	public PublishJobStorage() {
		super();
	}
	
	public PublishJobStorage(PublishConfigStorage config) {
		
		if (config != null) {
			setParams(config.getParams());
			setType(config.getType());
		}
	}
	
	public String getPathversion() {
		return pathversion;
	}
	public void setPathversion(String pathversion) {
		this.pathversion = pathversion;
	}
	public String getPathcloudid() {
		return pathcloudid;
	}
	public void setPathcloudid(String pathcloudid) {
		this.pathcloudid = pathcloudid;
	}
	public String getPathconfigname() {
		return pathconfigname;
	}
	public void setPathconfigname(String pathconfigname) {
		this.pathconfigname = pathconfigname;
	}
	public String getPathdir() {
		return pathdir;
	}
	public void setPathdir(String pathdir) {
		this.pathdir = pathdir;
	}
	public String getPathnamebase() {
		return pathnamebase;
	}
	public void setPathnamebase(String pathnamebase) {
		this.pathnamebase = pathnamebase;
	}
}
