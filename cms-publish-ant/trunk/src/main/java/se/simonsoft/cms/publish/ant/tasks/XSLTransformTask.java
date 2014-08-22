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

import java.io.File;

import org.apache.tools.ant.Task;

import se.simonsoft.cms.xmlsource.transform.DefaultTransformerService;
import se.simonsoft.cms.xmlsource.transform.SaxonTransformerFactory;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

public class XSLTransformTask extends Task {

	protected String inputSourcePath;
	protected String transformSourcePath;
	protected String outputSourcePath;
	
	/**
	 * @return the inputSourcePath
	 */
	public String getInputSourcePath() {
		return inputSourcePath;
	}

	/**
	 * @param inputSourcePath the inputSourcePath to set
	 */
	public void setInputSourcePath(String inputSourcePath) {
		this.inputSourcePath = inputSourcePath;
	}

	/**
	 * @return the transformSourcePath
	 */
	public String getTransformSourcePath() {
		return transformSourcePath;
	}

	/**
	 * @param transformSourcePath the transformSourcePath to set
	 */
	public void setTransformSourcePath(String transformSourcePath) {
		this.transformSourcePath = transformSourcePath;
	}

	/**
	 * @return the outputSourcePath
	 */
	public String getOutputSourcePath() {
		return outputSourcePath;
	}

	/**
	 * @param outputSourcePath the resultPath to set
	 */
	public void setOutputSourcePath(String outputSourcePath) {
		this.outputSourcePath = outputSourcePath;
	}

	private Transformer transformer;
	
	public void execute()
	{
		this.configureTransformer();
		this.transform();
	}
	
	private void transform()
	{
		Source text = new StreamSource(new File(this.getInputSourcePath()));
		
        try {
        	log("Transform " + this.getInputSourcePath() + " to " + this.getOutputSourcePath());
			transformer.transform(text, new StreamResult(new File(this.getOutputSourcePath())));
		} catch (TransformerException e) {
			log("TransformError: " + e.getMessage());
		}
	}
	
	private void configureTransformer() 
	{
		Source xslt = new StreamSource(this.getTransformSourcePath());
		//SaxonTransformerFactory saxonFactory = new SaxonTransformerFactory(xslt, null);
		//DefaultTransformerService transformerService = new DefaultTransformerService(saxonFactory);
		
		TransformerFactory transFact = TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", null);
		
	    try {
	    	transFact.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			this.transformer = transFact.newTransformer(xslt);
		} catch (TransformerConfigurationException e) {
			log("TransformConfigurationError: " + e.getMessage());
			e.printStackTrace();
		}
		
	}
}
