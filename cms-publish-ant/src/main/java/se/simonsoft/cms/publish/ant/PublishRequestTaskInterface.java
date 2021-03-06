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
package se.simonsoft.cms.publish.ant;

import se.simonsoft.cms.publish.PublishRequest;
import se.simonsoft.cms.publish.ant.nodes.ConfigsNode;
import se.simonsoft.cms.publish.ant.nodes.JobNode;
import se.simonsoft.cms.publish.ant.nodes.JobsNode;
import se.simonsoft.cms.publish.ant.nodes.ParamsNode;

/*
 *  An interface for a standard PublishRequest task.
 *  Not sure if this is the way to go. Perhaps a general class that we can extend?
 *  The intention is to have this template to build future PE or other Publishing Service tasks with
 */
public interface PublishRequestTaskInterface {
	
	
	/**
	 * @return the outputfolder
	 */
	public String getOutputfolder();

	/**
	 * @param outputfolder the outputfolder path to set
	 */
	public void setOutputfolder(String outputfolder);

	/**
	 * @return the zipoutput
	 */
	public String getZipoutput();

	/**
	 * @param zipoutput the zipoutput to set
	 */
	public void setZipoutput(String zipoutput);
	
	/**
	 * @return the fail
	 */
	public boolean isFail();

	/**
	 * @param fail the fail to set
	 */
	public void setFail(boolean fail);
	
	/**
	 * @param jobs the jobs to set
	 */
	public void addConfiguredJobs(JobsNode jobs);
	
	/**
	 * @return the jobs
	 */
	public JobsNode getJobs();
	
	/*
	 *  Perform a publish request ( bad name)
	 */
	public void doPublishRequest(JobNode job);
	
	
	/**
	 * @return the configs
	 */
	public ConfigsNode getConfigs();
	
	/**
	 * @param configs the configs to set
	 */
	public void addConfiguredConfigs(ConfigsNode configs);
	
	/**
	 * Create a PublishRequest
	 * @return a PublishRequest object with all parameters for a publish request
	 */
	public PublishRequest createPublishRequest(ParamsNode paramsNode);
	
	/*
	 * Perform checks for if a job is completed 
	 */
	public boolean isCompleted();
	
	
	public void getPublishResult();
	
	
	//public void retrievePublishSource();
}
