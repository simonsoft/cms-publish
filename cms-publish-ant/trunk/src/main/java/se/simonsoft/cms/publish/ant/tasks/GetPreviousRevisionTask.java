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
package se.simonsoft.cms.publish.ant.tasks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.Task;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.publish.ant.FailedToInitializeException;
import se.simonsoft.cms.publish.ant.nodes.ConfigNode;
import se.simonsoft.cms.publish.ant.nodes.ConfigsNode;
import se.simonsoft.cms.publish.ant.nodes.ParamsNode;
import se.simonsoft.publish.ant.helper.RestClientReportRequest;

/**
 * Retrieves head revision from index and saves it as JSON to file.
 * Also retrieves previous head stored.
 * 
 * @author joakimdurehed
 *
 */
public class GetPreviousRevisionTask extends Task {
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	protected ConfigsNode configs;
	protected ParamsNode params;
	protected String file; 
	
	/**
	 * @return the file
	 */
	public String getFile() {
		return file;
	}

	/**
	 * @param file the file to set
	 */
	public void setFile(String file) {
		this.file = file;
	}

	/**
	 * @return the params
	 */
	public ParamsNode getParams() {
		return params;
	}

	/**
	 * @param params
	 *            the params to set
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
	 * @param configs
	 *            the configs to set
	 */
	public void addConfiguredConfigs(ConfigsNode configs) {
		this.configs = configs;
	}
	
	/**
	 * Executes the task
	 */
	public void execute() 
	{
		HashMap<String, String> prevHead = this.parseFile();
		
		// Fetch the curreht head according to index
		RepoRevision currHead = this.requestHeadRevision();
		
		if(prevHead != null || !prevHead.get("rev").equals("")) {
			this.writeRevisionToFile(currHead); // Save the current head
		}
		//DateFormat df =  new SimpleDateFormat();
		logger.debug("Prev head {} and prev date {}", prevHead.get("rev"), prevHead.get("date"));
		//RepoRevision previousHead = new RepoRevision(Long.parseLong(prevHead.get("rev")), df.parse(prevHead.get("date")));
		
		//if(currHead.isNewer(previousHead))
		
		Long prevHeadRev = 0L;
		// If the previous head rev is higher than 0 ++ it and set it to property prevhead.
		if(Long.parseLong(prevHead.get("rev")) > 0L ) {
			prevHeadRev = Long.parseLong(prevHead.get("rev"));
			prevHeadRev = prevHeadRev + 1; // Iterate once so we don't re-publish older rev
			logger.debug("Setting prevhead to {}", prevHeadRev);
			
		} else if (prevHead.get("rev").equals("0")) {
			prevHeadRev = Long.parseLong(prevHead.get("rev"));
			logger.debug("Setting prevhead to {}", prevHeadRev);
		}
		
		this.getProject().setProperty("prevhead", prevHeadRev.toString());
		
	}
	

	/**
	 * Stores revision to file
	 */
	private void writeRevisionToFile(RepoRevision head) 
	{
		
		File revFile = FileUtils.getFile(this.getFile());
		
		String revJSONObject = this.saveAsJSON(head);
		
		try {
			FileUtils.writeStringToFile(revFile, revJSONObject, "UTF-8");
		} catch (IOException e) {
			logger.warn("Could not write to {} with message {}. Stacktrace:\n{}",this.getFile(), e.getMessage(), e.getStackTrace());
		}

	}
	
	/**
	 * Save RepoRevision as JSON and return as String
	 * @param head
	 * @return
	 */
	private String saveAsJSON(RepoRevision head) 
	{
		JSONObject obj = new JSONObject();
		obj.put("rev", head.toString());
		obj.put("date", head.getDate()); // Adding date just because we can
		
		StringWriter out = new StringWriter();
		
		try {
			obj.writeJSONString(out);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return out.toString();
	}
	/**
	 * Get the last indexed revision (most often equal to head) as String
	 * @return the revision as String
	 */
	private RepoRevision requestHeadRevision()
	{
		RestClientReportRequest req = new RestClientReportRequest();
		
		// Set the configs
		this.addConfigsToRequest(req);
		RepoRevision revisionCompleted = null;
		try {
			revisionCompleted = req.getRevisionCompleted();
			
		} catch (FailedToInitializeException e) {
			logger.warn("Could not get RevisionCompleted with message \n{}", e.getMessage());
		}
		
		return revisionCompleted;
	}
	
	/**
	 * Reads the file storing the previous revision and return its value
	 * @return String revision value
	 */
	private HashMap<String, String> parseFile()
	{
		
		HashMap<String, String> values = new HashMap<String, String>();
		values.put("rev", "0");
		values.put("date", "");
		
		if(this.getFile().equals("") || this.getFile() == null) {
			logger.info("No revision file is provided! Returning rev 0 as default");
			return values; // Return 0
		}
		
		if(!FileUtils.getFile(this.getFile()).exists()){
			logger.info("No previois {} file exists. Returning rev 0 as default", this.getFile());
			return values;	// Return 0
		}
		
		try {
			
			JSONParser parser = new JSONParser();
			
			JSONObject revInfo = (JSONObject) parser.parse(FileUtils.readFileToString(FileUtils.getFile(this.getFile(), "UTF-8")));
			values.clear();
			values.put("rev", (String) revInfo.get("rev"));
			values.put("date", (String) revInfo.get("date"));
			
		} catch (IOException e) {
			logger.warn("Could not access {} with message {}. Stacktrace:\n{}",this.getFile(), e.getMessage(), e.getStackTrace());
		} catch (ParseException e) {
			logger.warn("Could not parse {} with message {}. Stacktrace:\n{}", this.getFile(), e.getMessage(), e.getStackTrace());
		}
		// Return result
		return values; 
	}
	
	/**
	 * Sets all configs to RestClientReportRequest configs map
	 * @param request
	 */
	private void addConfigsToRequest(RestClientReportRequest request) {
		if (null != configs && configs.isValid()) {
			for (final ConfigNode config : configs.getConfigs()) {
				request.addConfig(config.getName(), config.getValue());
			}
		}
	}
}
