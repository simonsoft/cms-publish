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
package se.simonsoft.cms.publish.ant.tasks;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.publish.PublishException;
import se.simonsoft.cms.publish.PublishRequest;
import se.simonsoft.cms.publish.PublishSourceCmsItemId;
import se.simonsoft.cms.publish.PublishTicket;
import se.simonsoft.cms.publish.abxpe.PublishServicePe;
import se.simonsoft.cms.publish.ant.nodes.ConfigNode;
import se.simonsoft.cms.publish.ant.nodes.ConfigsNode;
import se.simonsoft.cms.publish.ant.nodes.ParamNode;
import se.simonsoft.cms.publish.ant.nodes.ParamsNode;
import se.simonsoft.cms.publish.impl.PublishRequestDefault;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

/*
 * The ant task XML will contain config and params.
 * P
 */

// Not sure about the name
public class PublishSingleTask extends Task{
	
	protected ConfigsNode configs;
	protected String publishservice;
	protected ParamsNode params;
	protected String workspace;
	protected String jobname;
	protected String filename;
	protected String buildid;
	protected String buildnumber;
	protected String jenkinshome;
	protected boolean fail; // Not sure how to use 
	
	/**
	 * @return the jenkinshome
	 */
	public String getJenkinshome() {
		return jenkinshome;
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
	 * @param jenkinshome the jenkinshome to set
	 */
	public void setJenkinshome(String jenkinshome) {
		this.jenkinshome = jenkinshome;
	}
	
	/**
	 * @return the buildnumber
	 */
	public String getBuildnumber() {
		return buildnumber;
	}

	/**
	 * @param buildnumber the buildnumber to set
	 */
	public void setBuildnumber(String buildnumber) {
		this.buildnumber = buildnumber;
	}

	/**
	 * @return the workspace
	 */
	public String getWorkspace() {
		return workspace;
	}

	/**
	 * @return the filename
	 */
	public String getFilename() {
		return filename;
	}

	/**
	 * @param filename the storelocation to set
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}

	/**
	 * @return the buildid
	 */
	public String getBuildid() {
		return buildid;
	}

	/**
	 * @param buildid the buildid to set
	 */
	public void setBuildid(String buildid) {
		this.buildid = buildid;
	}

	/**
	 * @param workspace the workspace to set
	 */
	public void setWorkspace(String workspace) {
		this.workspace = workspace;
	}

	/**
	 * @return the jobname
	 */
	public String getJobname() {
		return jobname;
	}

	/**
	 * @param jobname the jobname to set
	 */
	public void setJobname(String jobname) {
		this.jobname = jobname;
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

	/**
	 * @return the configs
	 */
	public ConfigsNode getConfigs() {
		return configs;
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

	
	// Executes the task
	public void execute() {
		
        try {
			log("PublishRequest started");
			this.publishRequest();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (PublishException e) {
			log("Publication exception: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/*
	 * Sends a publish request and stores the result
	 */
	private boolean publishRequest() throws InterruptedException, PublishException{
		
		PublishTicket ticket;
		// If the publishservice attribute equals PE we shoudl use the Publishing Engine
		if(this.getPublishservice().equals("PE")){
			PublishRequestDefault publishRequest = this.createRequestDefault(); // Create the default request
			PublishServicePe publishService = new PublishServicePe(); // Create a PE service
			ticket = publishService.requestPublish(publishRequest); // Send the request
			
			log("Publish request id: " + ticket.toString());
			
			boolean isComplete = false;
			int i = 1;
			while(!isComplete){
				log("Completed check #" + i++);
				isComplete = publishService.isCompleted(ticket, publishRequest);
				Thread.sleep(2000); // Be patient
			}
			// Get the PDF (or whatever it is) IF we get a 200. 
			publishService.getResultStream(ticket, publishRequest, this.configureResultLocation());
		}
		return true;
	}
	
	/*
	 * Create a file at the storelocation and return an outputstream for the
	 * publishservice to use
	 * Right now we store all results in {jenkinshome}/publications/buildfolder/
	 * ie: /Users/Shared/Jenkins/Home/publications/44_2014-05-25_21-15-43/
	 * We could also store this in the build folder itself...not sure which is preferred.
	 */
	private FileOutputStream configureResultLocation(){

		File outputfile = null;

		FileOutputStream outputStream = null;

		try {
			// First create the folders
			/*
			File outputpath = new File(this.jenkinshome + "/publications/" + this.buildnumber + "_" + this.buildid + "/");
			if(!outputpath.mkdirs())
			{
				throw new BuildException("Could not create dirs " + outputpath.getAbsolutePath());
			}
			*/
			// Then we create the file in that location
			outputfile = new File(this.filename);
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
	
	/*
	 * Create the default request and set the config, params and other properties
	 */
	private PublishRequestDefault createRequestDefault()
	{
		PublishRequestDefault publishRequest = new PublishRequestDefault();
		// set config
		if (null != configs && configs.isValid()) {
			for (final ConfigNode config : configs.getConfigs()) {
				log("Configs: " + config.getName() + ":" + config.getValue());
				publishRequest.addConfig(config.getName(), config.getValue());
			}
		}
		// set params
		if (null != params && params.isValid()) {
			for (final ParamNode param : params.getParams()) {
				log("Params: " + param.getName() + ":" + param.getValue());
				if(param.getName().equals("file")){
					publishRequest.setFile(
							new PublishSourceCmsItemId(
									new CmsItemIdArg(param.getValue())
									));
					log("PublishRequestFile: " + publishRequest.getFile().getURI());
				}
				publishRequest.addParam	(param.getName(), param.getValue());


			}	
		}
		return publishRequest;
	}
}
