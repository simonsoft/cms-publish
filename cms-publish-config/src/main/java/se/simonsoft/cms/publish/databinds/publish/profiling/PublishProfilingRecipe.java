package se.simonsoft.cms.publish.databinds.publish.profiling;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PublishProfilingRecipe {
	private String name;
	private String logicalexpr;
	public static final String URL_ENCODING_CHARSET = "UTF-8";
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getLogicalexpr() {
		return logicalexpr;
	}
	public void setLogicalexpr(String logicalexpr) {
		this.logicalexpr = logicalexpr;
	}
	public String getLogicalExprDecoded() {
		return decodeString(logicalexpr);
	}
	private static String decodeString(String encoded) {

		try {
			return URLDecoder.decode(encoded, URL_ENCODING_CHARSET);

		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("predefined encoding not supported: " + URL_ENCODING_CHARSET, e);
		}
	}
}
