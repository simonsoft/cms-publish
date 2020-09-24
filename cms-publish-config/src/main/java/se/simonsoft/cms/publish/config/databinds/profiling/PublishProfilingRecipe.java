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
package se.simonsoft.cms.publish.config.databinds.profiling;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import se.simonsoft.cms.item.CmsProfilingRecipe;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobProfiling;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Contains the name and logical expression for a single profiling setup. 
 * Corresponds to one profiles element in an XML document.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PublishProfilingRecipe implements CmsProfilingRecipe {
	public static final String URL_ENCODING_CHARSET = "UTF-8";
	public static final Pattern INVALID_EXPR = Pattern.compile(".*[ <>].*");
	/**
	 * The Name of the profiles definition. Must be valid as part of a filename.
	 */
	private String name;
	/**
	 * The Logical Expression of the profiles definition. MUST be in encoded form.
	 */
	private String logicalexpr;
	
	/**
	 * The additional attributes that generated the Logical Expression as stored by Editor in the JSON.
	 */
	private Map<String,String> attributes = new HashMap<String, String>();
	
	public PublishProfilingRecipe() {
		// Default constructor.
	}
	
	public PublishProfilingRecipe(String name, String expr, Map<String, String> attributes) {

		// Verify that the expr does not contain '<','>' or space (or any other URL-disallowed char).
		Matcher m = INVALID_EXPR.matcher(expr);
		if (m.matches()) {
			throw new IllegalArgumentException("Not an URL encoded logical expression: " + expr);
		}
		
		if (attributes != null && (attributes.containsKey("name") || attributes.containsKey("logicalexpr"))) {
			throw new IllegalArgumentException("Attributes map must not contain 'name' or 'logicalexpr' attributes");
		}

		this.name = name;
		this.logicalexpr = expr;
		this.attributes = attributes;
	}
	
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	/** The Logical Expression of the profiles definition, encoded.
	 * Should be encoded when placed in XML attribute but non-encoded when sent to composer pipeline.
	 * See {@link SvnProfiling#encodeLogicalExpr(String)} for encoding. 
	 * @return logical expression, encoded
	 */
	@JsonIgnore
	public String getLogicalExpr() {
		return logicalexpr;
	}
	
	public String getLogicalexpr() {
		return logicalexpr;
	}
	
	public void setLogicalexpr(String logicalexpr) {
		this.logicalexpr = logicalexpr;
	}
	
	@JsonIgnore
	public String getLogicalExprDecoded() {
		return decodeString(logicalexpr);
	}
	
	/**
	 * @return additional attributes that generated the Logical Expression.
	 */
	//@JsonAnyGetter // Preserve attributes
	@JsonIgnore // Suppress attributes
	public Map<String,String> getAttributes() {
		return new HashMap<String, String>(this.attributes);
	}

    @JsonAnySetter
    public void setAttribute(String name, String value) {
    	if (name.toLowerCase().equals("name")) {
    		throw new IllegalArgumentException("should call setName()");
    	} else if (name.toLowerCase().equals("logicalexpr")) {
    		throw new IllegalArgumentException("should call setLogicalexpr()");
    	} else if (name.toLowerCase().startsWith("xmlns:")) {
    		// Filter namespace definitions.
    	} else if (name.toLowerCase().startsWith("cms:")) {
    		// Filter cms attributes, see specific settors.
    	}
    	this.attributes.put(name, value);
    }
	
	@JsonIgnore
	public PublishJobProfiling getPublishJobProfiling() {
		PublishJobProfiling result = new PublishJobProfiling();
		result.setName(getName());
		// The logicalexpr field in a PublishJob contains the decoded logical expression (encoded when stored as property in CMS).
		result.setLogicalexpr(getLogicalExprDecoded());
		return result;
	}
	
	private static String decodeString(String encoded) {

		try {
			return URLDecoder.decode(encoded, URL_ENCODING_CHARSET);

		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("predefined encoding not supported: " + URL_ENCODING_CHARSET, e);
		}
	}
}
