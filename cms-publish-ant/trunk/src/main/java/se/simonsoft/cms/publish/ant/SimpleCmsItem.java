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
package se.simonsoft.cms.publish.ant;

import java.util.ArrayList;

public interface SimpleCmsItem {
	
	/**
	 * @return the lang
	 */
	public String getLang();
	
	/**
	 * @param lang the lang to set
	 */
	public void setLang(String lang);
	
	/**
	 * @return the rev
	 */
	public Long getRev();
	
	/**
	 * @param rev the rev to set
	 */
	public void setRev(Long rev);
	
	/**
	 * @return the name
	 */
	public String getName();
	
	/**
	 * @param name the name to set
	 */
	public void setName(String name);
	
	/**
	 * @return the id
	 */
	public String getId();
	
	/**
	 * @param id the id to set
	 */
	public void setId(String id);
	
	/**
	 * @return the dependencies
	 */
	public ArrayList<String> getDependencies();
	
	/**
	 * @param dependencies the dependencies to set
	 */
	public void setDependencies(ArrayList<String> dependencies);
	
	/**
	 * @return the status
	 */
	public String getStatus();
	
	/**
	 * @param status the status to set
	 */
	public void setStatus(String status);
}
