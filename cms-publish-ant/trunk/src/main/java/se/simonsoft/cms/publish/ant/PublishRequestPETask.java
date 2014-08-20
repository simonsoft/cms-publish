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

import java.io.File;
import java.util.ArrayList;

import org.apache.tools.ant.Task;

import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.publish.PublishException;
import se.simonsoft.cms.publish.PublishRequest;
import se.simonsoft.cms.publish.PublishSource;
import se.simonsoft.cms.publish.PublishSourceCmsItemId;
import se.simonsoft.cms.publish.PublishTicket;
import se.simonsoft.cms.publish.abxpe.PublishServicePe;
import se.simonsoft.cms.publish.ant.nodes.ConfigNode;
import se.simonsoft.cms.publish.ant.nodes.ConfigsNode;
import se.simonsoft.cms.publish.ant.nodes.JobNode;
import se.simonsoft.cms.publish.ant.nodes.JobsNode;
import se.simonsoft.cms.publish.ant.nodes.ParamNode;
import se.simonsoft.cms.publish.ant.nodes.ParamsNode;
import se.simonsoft.cms.publish.impl.PublishRequestDefault;
import se.simonsoft.publish.ant.helper.ErrorLoggerHelper;
import se.simonsoft.publish.ant.helper.FileManagementHelper;
/*
 * A Publish Task that publishes whatever "job" one specifies in build file
 * 
 */
public class PublishRequestPETask extends Task implements PublishRequestTaskInterface {
	
	protected ConfigsNode configs;
	protected String publishservice;
	protected JobsNode jobs;
	protected boolean zipoutput;
	protected String outputfolder;
	private ArrayList<PublishJob> publishedJobs;
	private PublishServicePe publishService;
	protected boolean fail;
	private final ErrorLoggerHelper errorLogger = new ErrorLoggerHelper();
	private int tryCount = 2;
	
	@Override
	public void addConfiguredJobs(JobsNode jobs) {
		this.jobs = jobs;
	}

	@Override
	public JobsNode getJobs() {
		return this.jobs;
	}
	
	@Override
	public String getOutputfolder() {
		return this.outputfolder;
	}
	
	@Override
	public void setOutputfolder(String outputfolder) {
		this.outputfolder = outputfolder;
		
	}
	
	@Override
	public boolean getZipoutput() {
		return false;
	}
	
	@Override
	public void setZipoutput(String zipoutput) {
		this.zipoutput = Boolean.parseBoolean(zipoutput);
		
	}
	@Override
	public boolean isFail() {
		return this.fail;
	}
	
	@Override
	public void setFail(boolean fail) {
		this.fail = fail;
	}
	
	
	public void execute() {
		
		this.publishService = new PublishServicePe(); // Create a PE service
		
		this.publishJobs(); // Publish the jobs
		
		if(this.isCompleted()) { // Check if the jobs are ready
			this.getPublishResult(); // Download the result
		}
	}
	
	private void publishJobs()
	{
		this.publishedJobs = new ArrayList<PublishJob>();
		
		for (final JobNode job : jobs.getJobs()) {
			this.doPublishRequest(job);
		}
	}
	
	@Override
	public void doPublishRequest(JobNode job) {
		
		// Create the request
		PublishRequestDefault publishRequest = (PublishRequestDefault) this.createPublishRequest(job.getParams());

		// Send the request
		PublishTicket ticket = publishService.requestPublish(publishRequest);

		if(ticket == null){
			log("Could not send request to PublishingEngine");
			// Here we would like to output PE error.
			//this.addToErrorLog("PublicationException: Did not get response back from PE for file: " + publishRequest.getFile().getURI());
		}else {
			// Store the request and id as a job
			PublishJob publishJob = new PublishJob(ticket, publishRequest, job.getFilename());
			// Save the job
			this.publishedJobs.add(publishJob);
		}
	}

	@Override
	public boolean isCompleted() {
		log("Check if publish requests are completed");
		boolean isAllComplete = false;
		int completedCount = 0;
		int checks = 1;
		
		while(!isAllComplete){
		
			for (PublishJob publishJob : this.publishedJobs) {
				boolean isComplete = false;
				if(!publishJob.isCompleted()) {
					isComplete = publishService.isCompleted(publishJob.getTicket(), publishJob.getPublishRequest());
					//log("Ticket id " + publishJob.getTicket().toString() + " check. Total checks: #" + checks++);
					if(isComplete) {
						publishJob.setCompleted(isComplete);
						log("Ticket id: " + publishJob.getTicket().toString() +" completed after " + checks + " checks.");					
						completedCount++;
					}
				}	
				checks++;
			}
			// Are all jobs completed?
			if(completedCount == this.publishedJobs.size()) {
				isAllComplete = true;
			}
			
			try {
				Thread.sleep(2000);  // Be patient
			} catch (InterruptedException e) {
				log("Error when checking for completed jobs: " + e.getMessage());
			}
		}
		return true;
	}

	@Override
	public PublishRequest createPublishRequest(ParamsNode paramsNode) {
		log("Create a publishrequest");
		
		PublishRequestDefault publishRequest = new PublishRequestDefault();
		
		// set config
		if (null != configs && configs.isValid()) {
			for (final ConfigNode config : configs.getConfigs()) {
				log("Configs: " + config.getName() + ":" + config.getValue());
				publishRequest.addConfig(config.getName(), config.getValue());
			}
		}
		
		// set params
		if (null != paramsNode && paramsNode.isValid()) {
			for (final ParamNode param : paramsNode.getParams()) {
				log("Params: " + param.getName() + ":" + param.getValue());
				
				if(param.getName().equals("file")) {
					// We remove the .xml file ending for naming convention.
					
					// TODO set correct publish source (now we default to cmsitem)
					// Should set this as a parameter to the job?
					publishRequest.setFile(
							new PublishSourceCmsItemId(
									new CmsItemIdArg(param.getValue())
									));
					log("PublishRequestFile: " + publishRequest.getFile().getURI());
				}
				// For the type we create a publishformat to set
				if(param.getName().equals("type")) {
					final String type = param.getValue();
					publishRequest.setFormat(this.publishService.getPublishFormat(param.getValue()));
					log("publishRequest Format: " + publishRequest.getFormat().getFormat());
				}
				// For arbitrary params just add them
				publishRequest.addParam	(param.getName(), param.getValue());
			}
		}
		return publishRequest;
	}
	 // Not now
	public PublishSource retrivePublishSource(String uri){
		
			
		return null;
		
	}

	@Override
	public void getPublishResult() {	
		log("Getting the results");
		FileManagementHelper fileHelper = new FileManagementHelper();
		
		for (PublishJob publishJob : this.publishedJobs) {
			
			try {
				tryCount--;
				this.publishService.getResultStream(publishJob.getTicket(),
													publishJob.getPublishRequest(), 
													fileHelper.createStorageLocation(this.outputfolder, publishJob.getFilename()));
			} catch (PublishException e) {
				log("Ticket: " + publishJob.getTicket().toString() + " failed for file: " + 
								publishJob.getPublishRequest().getFile().getURI());
				if(tryCount != 0) {
					log("Trying to publish " + publishJob.getPublishRequest().getFile().getURI() + " again");
					this.doPublishRequest(publishJob);
				}
				
				errorLogger.addToErrorLog("PublishException for ticket: " + publishJob.getTicket().toString() + 
						". Publish failed for file: " + publishJob.getPublishRequest().getFile().getURI() + 
						" with errors: " + e.getMessage() + "\n");
				
				// Let's also remove the output
				fileHelper.delete(new File(outputfolder + "/" + publishJob.getFilename()));

			}
		}
	}
}
