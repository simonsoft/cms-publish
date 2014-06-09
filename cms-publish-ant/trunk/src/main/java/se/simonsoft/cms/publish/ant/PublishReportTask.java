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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.publish.PublishException;
import se.simonsoft.cms.publish.PublishFormat;
import se.simonsoft.cms.publish.PublishSourceCmsItemId;
import se.simonsoft.cms.publish.PublishTicket;
import se.simonsoft.cms.publish.abxpe.PublishServicePe;
import se.simonsoft.cms.publish.impl.PublishRequestDefault;

/*
 * Publish a report item to the number of outputs set by "jobs".
 * This is specifically designed for certain user and not a general publish task.
 *
 */
public class PublishReportTask extends Task {
	protected ConfigsNode configs;
	protected String publishservice;
	protected JobsNode jobs;
	protected String zipoutput;
	protected String outputfolder;
	private ArrayList<PublishJob> publishedJobs;
	private PublishServicePe publishService;
	protected boolean fail;
	
	/**
	 * @return the outputfolder
	 */
	public String getOutputfolder() {
		return outputfolder;
	}

	/**
	 * @param outputfolder the outputfolder to set
	 */
	public void setOutputfolder(String outputfolder) {
		this.outputfolder = outputfolder;
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
	 * @return the fail
	 */
	public boolean isFail() {
		return fail;
	}

	/**
	 * @param fail the fail to set
	 */
	public void setFail(boolean fail) {
		this.fail = fail;
	}

	/**
	 * @return the configs
	 */
	public ConfigsNode getConfigs() {
		return configs;
	}

	/**
	 * @param jobs the jobs to set
	 */
	public void addConfiguredJobs(JobsNode jobs) {
		this.jobs = jobs;
	}
	
	/**
	 * @return the jobs
	 */
	public JobsNode getJobs() {
		return jobs;
	}

	/**
	 * @param configs the configs to set
	 */
	public void addConfiguredConfigs(ConfigsNode configs) {
		this.configs = configs;
	}

	/**
	 * @return the publishservice
	 */
	public String getPublishservice() {
		return publishservice;
	}

	/**
	 * @param publishservice the publishservice to set
	 */
	public void setPublishservice(String publishservice) {
		this.publishservice = publishservice;
	}
	
	public void execute() {
		this.publishService = new PublishServicePe(); // Create a PE service
		
		this.publishJobs(); // Publish the jobs
		
		try {
			
			if(this.isCompleted()) { // Check if the jobs are ready
				this.getResult(); // Download the result
			}
			
		} catch (InterruptedException e) {
			
			e.printStackTrace();
		} catch (PublishException e) {
			e.printStackTrace();
		}
	}
	/*
	 * We use current date time as 
	 */
	private String getDateTime()
	{
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date now = new Date();
		return dateFormat.format(now);
	}
	
	/*
	 * Publish each job
	 */
	private void publishJobs()
	{
		this.publishedJobs = new ArrayList<PublishJob>();
		
		for (final JobNode job : jobs.getJobs()) {
			// Create the request
			PublishRequestDefault publishRequest = this.createRequestDefault(job.getParams());
			// Send the request
			PublishTicket ticket = publishService.requestPublish(publishRequest);
			// Store the request and id as a job
			PublishJob publishJob = new PublishJob(ticket, publishRequest, job.getFilename());
			// Save the job
			this.publishedJobs.add(publishJob);
			
			log("Publish request id: " + ticket.toString());
		}
	}
	
	/*
	 * Create the default request and set the config, params and other properties
	 */
	private PublishRequestDefault createRequestDefault(ParamsNode paramsNode)
	{
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
					// This is specific to customer, and might be part of parsed report later when 
					//  use CMS Item Objects instead
					// TODO remove this naming convention to a better suited place
					String slices[] = param.getValue().split(".");
					
					// TODO set correct publish source (now we default to cmsitem)
					// Should set this as a parameter to the job?
					publishRequest.setFile(
							new PublishSourceCmsItemId(
									new CmsItemIdArg(slices[0])
									));
					log("PublishRequestFile: " + publishRequest.getFile().getURI());
				}
				// For the type we create a publishformat to set (this is a fix, need to do this another way, by asking publish service)
				if(param.getName().equals("type")) {
					final String type = param.getValue();
					log("Set type: " + param.getValue());
					publishRequest.setFormat(this.publishService.getPublishFormat(param.getValue()));
				}
				// For arbitrary params just add them
				publishRequest.addParam	(param.getName(), param.getValue());
			}
		}
		return publishRequest;
	}
	
	/*
	 * Go through all publish jobs and check if they are completed
	 */
	private boolean isCompleted() throws InterruptedException{
		log("Check if publish requests are completed");
		boolean isAllComplete = false;
		int completedCount = 0;
		int checks = 1;
		while(!isAllComplete){
		
			for (PublishJob publishJob : this.publishedJobs) {
				boolean isComplete = false;
				if(!publishJob.isCompleted()) {
					isComplete = publishService.isCompleted(publishJob.getTicket(), publishJob.getPublishRequest());
					log("Ticket id " + publishJob.getTicket().toString() + " check. Total checks: #" + checks++);
					if(isComplete) {
						publishJob.setCompleted(isComplete);
						log("Ticket id: " + publishJob.getTicket().toString() +" completed");
						completedCount++;
					}
				}
				// Timeout. This is an arbitrary number. What should count as a timeout?
				// TODO timeout?
				if(completedCount > 500){
					return false;
				}
			}
			// Are all jobs completed?
			if(completedCount == this.publishedJobs.size()) {
				isAllComplete = true;
			}
			Thread.sleep(2000); // Be patient
		}
		return true;
	}
	
	private void getResult() throws PublishException {
		log("Getting the results");
		for (PublishJob publishJob : this.publishedJobs) {
			
			// TODO unzip the ones that should no be zipped archives..
			
			this.publishService.getResultStream(publishJob.getTicket(), publishJob.getPublishRequest(), this.resultLocation(publishJob.getFilename()));
		}
	}
	
	/*
	 *  Get the location to store the result in
	 */
	private FileOutputStream resultLocation(String filename) {

		File outputfile = null;

		FileOutputStream outputStream = null;
		
		try {
			// Then we create the file in that location
			outputfile = new File(outputfolder + "/" + filename);
			log("Absolutepath: " + outputfile.getAbsolutePath());

			outputfile.createNewFile();
			// Connect the stream to the file
			outputStream = new FileOutputStream(outputfile);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new BuildException("Could not store file at (file not found) " + outputfile.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
			throw new BuildException("Could not store file at (IO) " + outputfile.getAbsolutePath());
		}

		return outputStream;
	}
}
