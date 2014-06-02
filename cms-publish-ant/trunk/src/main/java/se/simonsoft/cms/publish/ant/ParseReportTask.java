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
import java.util.Iterator;

import org.apache.tools.ant.Task;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ParseReportTask extends Task {

	protected String report;
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
		this.getProject().setProperty(propertyName, string.toString());
	}
	
	private ArrayList<String> parseJSON() {
		log("Parsing result");
		JSONParser parser = new JSONParser();
		ArrayList<String> parsedItems = new ArrayList<String>();
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
					String lang = (String) prop.get("abx:lang");
					String status = (String) prop.get("cms:status");
					String id = (String) item.get("logical");
					String name = (String) item.get("name");
					
					parsedItems.add(id + ";"+ name );
					log("id:" + id + " status: " + status);
				}
			}
		return parsedItems;	
			
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}
}
