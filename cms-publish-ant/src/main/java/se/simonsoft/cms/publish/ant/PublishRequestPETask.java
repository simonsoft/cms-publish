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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.cms.item.CmsItem;
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
/**
 * Publishes a single or multiple "publish jobs".
 * Will require at a minimum a logicalid to publish.
 * 
 * @author joakimdurehed
 *
 */
public class PublishRequestPETask extends Task implements PublishRequestTaskInterface {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	protected ConfigsNode configs;
	protected String publishservice;
	protected JobsNode jobs;
	protected String zipoutput;
	protected String outputfolder;
	protected String errorlogpath;
	private ArrayList<PublishJob> publishedJobs;
	private PublishServicePe publishService;
	protected boolean fail;
	private final ErrorLoggerHelper errorLogger = new ErrorLoggerHelper();
	private static final long SLEEP = 2000; // Milliseconds to sleep btw PE checks
	
	@Override
	public void addConfiguredJobs(JobsNode jobs) {
		this.jobs = jobs;
	}

	@Override
	public JobsNode getJobs() {
		return this.jobs;
	}
	
	@Override
	public ConfigsNode getConfigs() {
		return configs;
	}

	@Override
	public void addConfiguredConfigs(ConfigsNode configs) {
		this.configs = configs;
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
	public String getZipoutput() {
		return zipoutput;
	}
	
	@Override
	public void setZipoutput(String zipoutput) {
		this.zipoutput = zipoutput;
	}
	
	@Override
	public boolean isFail() {
		return this.fail;
	}
	
	@Override
	public void setFail(boolean fail) {
		this.fail = fail;
	}
	
	/**
	 * @return the errorlogpath
	 */
	public String getErrorlogpath() {
		return errorlogpath;
	}

	/**
	 * @param errorlogpath the errorlogpath to set
	 */
	public void setErrorlogpath(String errorlogpath) {
		this.errorlogpath = errorlogpath;
	}

	public void execute() {
		
		// Instantiate some stuff we need
		this.publishService = new PublishServicePe(); // Create a PE service
		this.publishedJobs = new ArrayList<PublishJob>();
		
		this.publishJobs(); // Publish all the jobs
		
		if(this.isCompleted()) { // Check if the jobs are ready
			this.getPublishResult(); // Download the result
		}
		
		// A Test: // Does not work.
		/*
		CmsItem cmsitem = (CmsItem) this.getProject().getProperties().get("CMSITEM");
		if(cmsitem != null) {
			logger.debug("CmsItem: {}", cmsitem.getId().getRelPath().getNameBase());
		} else {
			logger.debug("cmsitem NOT passed to publish");
		}
		//*/
		
	}
	
	/**
	 * Published the Jobs in the Jobs list
	 */
	private void publishJobs()
	{
		for (final JobNode job : jobs.getJobs()) {
			this.doPublishRequest(job); // Should we call it makePublishRequest?
		}
	}
	
	@Override
	public void doPublishRequest(JobNode job) {	
		// Create the request
		PublishRequestDefault publishRequest = (PublishRequestDefault) this.createPublishRequest(job.getParams());
		
		// Store the request and id as a job
		PublishJob publishJob = this.createPublishJob(publishRequest, job);
		
		// Send the request
		this.sendPublishRequest(publishRequest, publishJob);
	}
	
	/**
	 * Condfigures a PublishJob
	 * @param publishRequest the publishRequest to associate with the PublisJob
	 * @param job the jonNode (configuration) to associate with the PublishJob
	 * @return
	 */
	private PublishJob createPublishJob(PublishRequestDefault publishRequest, JobNode job)
	{
		PublishJob publishJob = new PublishJob(publishRequest, job.getFilename());
		
		// Let's store if this publishJob requested a zip package or not
		for (final ParamNode param : job.getParams().getParams()) {
			if(param.getName().equals("zip-output")) {
				publishJob.setZip(param.getValue());
			}
		}
		
		return publishJob;
	}
	
	/*
	 * Send the actual request to PublishService
	 * For now we take a publishJob too
	 */
	/**
	 * Makes the publish request for the publishJob
	 * @param publishRequest
	 * @param publishJob
	 */
	private void sendPublishRequest(PublishRequestDefault publishRequest, PublishJob publishJob)
	{
		// Send the request

		PublishTicket ticket;
		try {
			ticket = publishService.requestPublish(publishRequest);
		} catch (PublishException e) {
			throw new BuildException("Publish Service communication error.");
		}

		if(ticket == null){
			log("Could not send request to PublishingEngine");
			throw new BuildException("Publish Service communication error.");
			// Here we would like to output PE error.
			//this.addToErrorLog("PublicationException: Did not get response back from PE for file: " + publishRequest.getFile().getURI());
		}else {
			// Store the request and id as a job
			publishJob.setTicket(ticket); // Set the ticket
			if(!this.publishedJobs.contains(publishJob)){
				this.publishedJobs.add(publishJob);
			}
			
		}
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
				} else if(param.getName().equals("type")) { // For the type we create a publishformat to set
					final String type = param.getValue();
					publishRequest.setFormat(this.publishService.getPublishFormat(param.getValue()));
					log("publishRequest Format: " + publishRequest.getFormat().getFormat());
				} else {
					// For arbitrary params just add them
					publishRequest.addParam	(param.getName(), param.getValue());
				}
			}
		}
		return publishRequest;
	}

	
	@Override
	public boolean isCompleted() {
		log("Check if publish requests are completed");
		boolean isAllComplete = false;
		
		int checks = 1;
		
		while(!isAllComplete){
			int completedCount = 0;
			// Check if a publishJob is completed
			for (PublishJob publishJob : this.publishedJobs) {
				boolean isComplete = false;
				if(!publishJob.isCompleted()) {
					try {
						isComplete = publishService.isCompleted(publishJob.getTicket(), publishJob.getPublishRequest());
					} catch (PublishException e) {
						throw new BuildException("Publish Service communication error.");
					}
					
					//log("Ticket id " + publishJob.getTicket().toString() + " check. Total checks: #" + checks++);
					if(isComplete) {
						publishJob.setCompleted(isComplete);
						int seconds = (int)(checks * SLEEP / 1000) % 60;
						log("Ticket id: " + publishJob.getTicket().toString() +" completed after approx " + seconds + " seconds.");					
					}
				}
				checks++;
			}
			// Count the number of completed jobs
			for (PublishJob publishJob : this.publishedJobs) {
				if(publishJob.isCompleted()) {
					completedCount++;
				}
			}
			
			// Are all jobs completed?
			if(completedCount == this.publishedJobs.size()) {
				isAllComplete = true;
			}
			
			try {
				Thread.sleep(SLEEP);  // Be patient
			} catch (InterruptedException e) {
				logger.error("Interrupted when checking for completed jobs: {}", e.getMessage());
			}
		}
		return true;
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
			String fileName = publishJob.getFilename(); 
			try {
				
				// If this publish required a zip package, let's make sure we set proper file type
				if (publishJob.isZip().equals("yes")){
					if(publishJob.getFilename().contains(".zip")) {
						fileName = publishJob.getFilename();
					}
					else {
						fileName = publishJob.getFilename() + ".zip";
					}
				}
				// We allow two tries because we have seen that the PE can behave strange and failed on the first try but ALWAYS 
				// succeeding on the second go. We have not found out what is causing this. All we know is, if it fails once, 
				// it will succeed on next publish
				if(publishJob.getNumberOfTries() >= 1) { // If we have at least one more try let's do it
					
					publishJob.setNumberOfTries(publishJob.getNumberOfTries() - 1); // Count one try down
					log("Retrieve result: " + fileName + " try: " + publishJob.getNumberOfTries());
					// If we have not retrieved this job before, retrieve it
					if(!publishJob.isRetreived()) {
						publishJob.setRetreived(true); // Mark it retrieved
						this.publishService.getResultStream(publishJob.getTicket(),
							publishJob.getPublishRequest(), 
							this.getStorageLocation(this.outputfolder, fileName));
					}
					
				}
				
			} catch (PublishException e) {
				
				log("Ticket: " + publishJob.getTicket().toString() + " failed for file: " + 
								publishJob.getPublishRequest().getFile().getURI());
				// Remove 
				fileHelper.delete(new File(this.outputfolder + "/" + fileName));
				publishJob.setRetreived(false); // Reset retrieve state
				
				if(publishJob.getNumberOfTries() == 0) {
					
					errorLogger.addToErrorLog("PublishException for ticket: " + publishJob.getTicket().toString() + 
							". Publish failed for file: " + publishJob.getPublishRequest().getFile().getURI() + 
							" with errors: " + e.getMessage() + "\n", this.getErrorlogpath());
					// Here we would like to download and parse error log from PE
					
				} else {
					log("Trying to publish " + publishJob.getPublishRequest().getFile().getURI() + " again");
					
					// Remove this publishJob from the stack of jobs
					// this.publishedJobs.remove(publishJob);
						
					// Then send it to publishing again
					publishJob.setCompleted(false); // Reset completed state
					this.sendPublishRequest((PublishRequestDefault) publishJob.getPublishRequest(), publishJob);
					// 
					if(this.isCompleted()) {
						this.getPublishResult();
					}
					
					break;
				}	
			}
		}
	}
	
	/**
	 * Creates a FileOutputStream at the outputfolder/filename location and returns it
	 * @param outputFolder
	 * @param fileName
	 * @return
	 */
	private FileOutputStream getStorageLocation(String outputFolder, String fileName) {
		
		File outputfile = null;

		FileOutputStream outputStream = null;

		try {
			// Make sure the outputfolder exists
			File folder = new File(outputFolder);
			if(!folder.exists()){
				folder.mkdir();
			}
			
			// Then we create the file in that location
			outputfile = new File(outputFolder + File.separator + fileName);
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
