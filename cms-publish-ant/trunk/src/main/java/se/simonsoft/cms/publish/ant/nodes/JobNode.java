/**
 * Copyright (C) 2009-2013 Simonsoft Nordic AB
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
package se.simonsoft.cms.publish.ant.nodes;

/*
 * Object that have necessary information about whats unique for a specific publishjob
 */
public class JobNode {
	
	protected ParamsNode params;
	protected String filename;
	protected String zipoutput;
	protected String rootfilename;
	
	/**
	 * @return the rootfilename
	 */
	public String getRootfilename() {
		
		return rootfilename;
	}

	/**
	 * @param rootfilename the rootfilename to set
	 */
	public void setRootfilename(String rootfilename) {
		this.rootfilename = rootfilename;
	}

	/**
	 * @return the zipoutput
	 */
	public String getZipoutput() {
		return zipoutput;
	}

	/**
	 * @param zipoutput the zipoutput to set
	 */
	public void setZipoutput(String zipoutput) {
		this.zipoutput = zipoutput;
	}

	/**
	 * @return the filename
	 */
	public String getFilename() {
		return filename;
	}

	/**
	 * @param filename the filename to set
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}

	/**
	 * @return the params
	 */
	public ParamsNode getParams() {
		return params;
	}

	/**
	 * @param params the params to set
	 */
	public void addConfiguredParams(ParamsNode params) {
		this.params = params;
	}
	
}
