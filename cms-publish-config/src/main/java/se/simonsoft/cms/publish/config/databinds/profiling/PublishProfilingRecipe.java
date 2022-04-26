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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import se.simonsoft.cms.item.CmsProfilingRecipe;

/** Contains the name and logical expression for a single profiling setup. 
 * Corresponds to one profiles element in an XML document.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PublishProfilingRecipe implements CmsProfilingRecipe {
	public static final String URL_ENCODING_CHARSET = "UTF-8";
	public static final Pattern INVALID_EXPR = Pattern.compile(".*[ <>].*");
	
	/**
	 * The additional attributes that generated the Logical Expression as stored by Editor in the JSON.
	 * Also contains "name", "logicalexpr" and "_stage".
	 */
	private Map<String,String> attributes = new LinkedHashMap<String, String>();
	
	public PublishProfilingRecipe() {
		// Default constructor.
	}
	
	public PublishProfilingRecipe(String name, Map<String, String> attributes) {
		this(name, null, attributes);
	}
	
	@Deprecated // deprecating since we will move towards CMS filtering instead of logicalexpr.
	public PublishProfilingRecipe(String name, String expr, Map<String, String> attributes) {

		// Verify that the expr does not contain '<','>' or space (or any other URL-disallowed char).
		if (expr != null) {
			Matcher m = INVALID_EXPR.matcher(expr);
			if (m.matches()) {
				throw new IllegalArgumentException("Not an URL encoded logical expression: " + expr);
			}
		}
		
		if (attributes != null && (attributes.containsKey("name") || attributes.containsKey("logicalexpr"))) {
			throw new IllegalArgumentException("Attributes map must not contain 'name' or 'logicalexpr' attributes");
		}
		if (attributes != null) {
			this.attributes.putAll(attributes);
		}
		this.attributes.put("name", name);
		if (expr != null) {
			this.attributes.put("logicalexpr", expr);
		}
	}
	
	
	@JsonIgnore
	public String getName() {
		return attributes.get("name");
	}
	

	@JsonIgnore
	public boolean isStageRelease() {
		return ("release".equals(attributes.get("_stage")));
	}
	
	@JsonIgnore
	public boolean isStagePublish() {
		return (attributes.get("_stage") == null || "publish".equals(attributes.get("_stage")));
	}
	
	
	/** The Logical Expression of the profiles definition, encoded.
	 * Should be encoded when placed in XML attribute but non-encoded when sent to composer pipeline.
	 * See {@link SvnProfiling#encodeLogicalExpr(String)} for encoding. 
	 * @return logical expression, encoded
	 */
	@JsonIgnore
	public String getLogicalExpr() {
		return attributes.get("logicalexpr");
	}
	
	
	@JsonIgnore
	public String getLogicalExprDecoded() {
		return decodeString(getLogicalExpr());
	}
	
	/**
	 * @return additional attributes that generated the Logical Expression.
	 */
	@JsonIgnore
	public Map<String,String> getAttributesFilter() {
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		for (Entry<String, String> e: attributes.entrySet()) {
			String name = e.getKey();
			if (name.toLowerCase().equals("logicalexpr")) {
				// Filter logicalexpr
			} else if (name.toLowerCase().equals("name")) {
				// Filter name
			} else if (name.toLowerCase().startsWith("xmlns:")) {
	    		// Filter namespace definitions.
	    	} else if (name.toLowerCase().startsWith("cms:")) {
	    		// Filter cms attributes.
	    	} else if (name.startsWith("_")) {
	    		// Filter underscore attributes.
			} else {
				result.put(e.getKey(), e.getValue());
			}
		}
		return result;
	}

	@JsonAnyGetter // Preserve attributes
	//@JsonIgnore // Suppress attributes
	public Map<String,String> getAttributes() {
		return new LinkedHashMap<String, String>(this.attributes);
	}

    @JsonAnySetter
    public void setAttribute(String name, String value) {
   		this.attributes.put(name, value);
    }
	
	
	/**
	 * Validate that the filter is not empty.
	 */
	@JsonIgnore
	public void validateFilter() {
		if (getAttributesFilter().isEmpty()) {
			throw new IllegalArgumentException("Profiling filter must not be empty for '" + getName() + "'.");
		}
		
	}
	
	private static String decodeString(String encoded) {

		try {
			return URLDecoder.decode(encoded, URL_ENCODING_CHARSET);

		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("predefined encoding not supported: " + URL_ENCODING_CHARSET, e);
		}
	}
}
