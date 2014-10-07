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
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author joakimdurehed
 *
 */
public class ReportResponseParserV1 implements ReportResponseParser {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private ArrayList<SimpleCmsItemV1Impl> cmsItems;
	

	@Override
	public void setFields(ArrayList<String> fields) {
		// TODO Auto-generated method stub

	}

	@Override
	public void parseJSONResponse(String response) {
		logger.debug("start");
		JSONParser parser = new JSONParser();
		this.cmsItems = new ArrayList<SimpleCmsItemV1Impl>();
		
		Long latestRev = 0L; // init rev counter. With this we'll find the latest/highest revision.
		
		try {
			
			JSONObject jsonreport = (JSONObject)parser.parse(response);
			
			JSONArray items = (JSONArray)jsonreport.get("items");
			logger.debug("Item count: " + items.size());
			if(items.size() > 0) {
				
				Iterator<JSONObject> iterator = items.iterator();
				
				while (iterator.hasNext()) 
				{
					SimpleCmsItemV1Impl cmsitem = new SimpleCmsItemV1Impl();
					
					// Parse
					JSONObject item = iterator.next();
					JSONObject prop = (JSONObject) item.get("prop");
					JSONObject commit = (JSONObject) item.get("commit");
					JSONArray dependencies = (JSONArray) item.get("abx:dependencies");
					
					// Set logical id
					cmsitem.setId(item.get("logical").toString());
					cmsitem.setName(item.get("name").toString());
					cmsitem.setLang(prop.get("abx:lang").toString());
					cmsitem.setStatus(prop.get("cms:status").toString());
					cmsitem.setRev((Long) commit.get("rev"));
					
					cmsitem.setDependencies(dependencies);
					
					this.cmsItems.add(cmsitem);
				}
			}
			
			logger.debug("ends");
		} catch(ParseException e) {
			logger.debug("ParseException at position: " + e.getPosition());
		}
	}

	@Override
	public List getParsedResult() {
		// Return the parsed result
		return this.cmsItems;
	}



}
