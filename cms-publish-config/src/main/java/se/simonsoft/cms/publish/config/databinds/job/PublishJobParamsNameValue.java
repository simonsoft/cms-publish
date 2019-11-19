package se.simonsoft.cms.publish.config.databinds.job;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;

import se.simonsoft.cms.publish.config.databinds.config.PublishConfigParameter;

/**
 * Quick hack to serialize parameter collections as JSON array of Name-Value pairs.
 * Improved compatibility with Step Functions JSONPath.
 * 
 * @author takesson
 */
public class PublishJobParamsNameValue extends LinkedHashSet<PublishConfigParameter> {

	private static final long serialVersionUID = 1L;
	

	public PublishJobParamsNameValue(Map<String, String> paramMap) {
		for (Entry<String, String> param: paramMap.entrySet()) {
			this.add(new PublishConfigParameter(param.getKey(), param.getValue()));
		}
	}
	
	// TODO: Make unmodifiable.

}
