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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
					//JSONObject prop = (JSONObject) item.get("prop");
					JSONObject commit = (JSONObject) item.get("commit");
					
					Long rev = (Long) commit.get("rev");
					// Properties we want to work with soon:
					//String lang = (String) prop.get("abx:lang");
					//String status = (String) prop.get("cms:status");
					String id = (String) item.get("logical");
					String name = (String) item.get("name");
					
					
					// Find match for "_" in filename
					
					//Matcher matcher = pattern.matcher(name);
					if(name.contains("_")) {
						String slices[] = name.split("_");
						name = slices[0];
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
					// id is logical id and name is name without file end
					parsedItems.add(id + ";"+ name );
					log("id:" + id + " name: " + name);
				}
			}
			this.storeLatestRevision(latestRev); // Make sure we store the latest rev.
		return parsedItems;	
			
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/*
	 * We store the latest revison for the next report request.
	 * This might be a stupid way of going about this. Can we ask the report API for modified date instead? I can't find that at least.
	 */
	private void storeLatestRevision(Long revision){
		
		// Create the file
		File revFile = new File(revStoreFileName);
		BufferedWriter writer = null;
		try {
			// Add the rev, overwrites if exist
			writer = new BufferedWriter(new FileWriter(revFile));
			writer.write(revision.toString());
			writer.write("\n!!! NEVER EDIT THIS FILE !!!");
			
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
