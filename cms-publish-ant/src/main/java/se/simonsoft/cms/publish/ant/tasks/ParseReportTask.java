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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.Task;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
/*
 * Parse the JSON response from CMS Reporting 1.0
 */
public class ParseReportTask extends Task {

	protected String report;
	private String revStoreFileName = "latestrev.txt";
	private ArrayList<String> items;
	
	
	/**
	 * @return the report
	 */
	public String getReport() {
		return report;
	}

	/**
	 * @param report the report to set
	 */
	public void setReport(String report) {
		this.report = report;
	}

	public void execute() {
		this.items = this.parseJSON();
		this.setMuliValueProperty(this.items, "items");
	}
	
	/*
	 * Set a string with delimited by comma to a property
	 */
	private void setMuliValueProperty(ArrayList<String> values, String propertyName)
	{
		StringBuffer string = new StringBuffer();
		
		for (String value: values){
			string.append(value + ",");
		}
		// Set the ant property
		this.getProject().setProperty(propertyName, string.toString());
	}
	
	private ArrayList<String> parseJSON() {
		log("Parsing result");
		JSONParser parser = new JSONParser();
		ArrayList<String> parsedItems = new ArrayList<String>();
		//Pattern pattern = Pattern.compile("_");
		Long latestRev = 0L; // init rev counter. With this we'll find the latest/highest revision.
		try {
			
			JSONObject jsonreport = (JSONObject)parser.parse(report);
			
			JSONArray items = (JSONArray)jsonreport.get("items");
			log("Item count: " + items.size());
			if(items.size() > 0) {
				
				Iterator<JSONObject> iterator = items.iterator();
				
				while (iterator.hasNext()) 
				{
					JSONObject item = iterator.next();
					JSONObject prop = (JSONObject) item.get("prop");
					JSONObject commit = (JSONObject) item.get("commit");
					
					Long rev = (Long) commit.get("rev");
					// Properties we want to work with soon:
					String lang = (String) prop.get("abx:lang");
					String status = (String) prop.get("cms:status");
					String id = (String) item.get("logical");
					String name = (String) item.get("name");
					
		
					// If we find a underscore we know that this is a dependency
					// and that the parent file has the same name except the underscore 
					// ie: 1122048_comp.xml is a dependency to 1122048.xml
					// This is of obviously VERY customer dependent.
					if(name.contains("_")) {
						String slices[] = name.split("_");
						name = slices[0];
						
						// Modify logical id to match parent
						int index = id.lastIndexOf("/");
						String updatedId = id;
						updatedId = updatedId.substring(0, index);
						//log("Substring: " + updatedId);
						updatedId = updatedId + "/" + name + ".xml";
						//log("Changed to parent logical id: " + updatedId);
						id = updatedId;
					}
					
					// Remove the filetype
					
					if(name.contains(".")) {
						
						String slices[] = name.split("\\.");
						name = slices[0];
					}
					// Find the highest revision
					if(latestRev < rev){
						latestRev = rev;
					}
					// Make sure we don't publish the same item twice
					if(parsedItems.indexOf(id + ";"+ name) == -1) {
						// id is logical id and name is name without file end
						parsedItems.add(id + ";"+ name);
						log("id:" + id + " name: " + name);
						// A test, for each item we find send it to publish
						//this.passToPublish(id, name, lang, status);
					}
					
					// id is logical id and name is name without file end
					//parsedItems.add(id + ";"+ name);
					//log("id:" + id + " name: " + name);
				}
			}
			log("Number of items to publish: " + parsedItems.size());
			this.storeLatestRevision(latestRev); // Make sure we store the latest rev.
		return parsedItems;	
			
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	// Test to see if we how we can call other targets with properties
	private void passToPublish(String filename, String file, String lang, String status)
	{
		this.getProject().setProperty("filename", filename);
		this.getProject().setProperty("file", file);
		this.getProject().setProperty("lang", lang);
		this.getProject().setProperty("status", status);
		this.getProject().executeTarget("publish");
		
	}
	
	/*
	 * We store the latest revison for the next report request.
	 * This might be a stupid way of going about this. Can we ask the report API for modified date instead? I can't find that at least.
	 */
	private void storeLatestRevision(Long revision){
	
		// Create the file
		File revFile = new File(revStoreFileName);
		
		// If we already have a revStore make sure we don't overwrite with 0
		if(revFile.exists()){
			
			if(revision.equals(0L)) {
				// Keep the previous revision.
				log("Keep current revision");
				return;
			}
		}
		
		BufferedWriter writer = null;
		try {
			// Add the rev, overwrites if exist
			writer = new BufferedWriter(new FileWriter(revFile));
			writer.write(revision.toString());
			writer.write("\n!!! NEVER EDIT THIS FILE MANUALLY !!!");
			
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			try {
				writer.close();
			} catch (IOException e) {
				
				e.printStackTrace();
			}
		}
	}
}
