package se.simonsoft.cms.publish.databinds;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonDeserialization {
	public PublishConfig JsonDeserialize(String input)throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper m = new ObjectMapper();
		m.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		File filePath = new File(input);
		PublishConfig publishConfig = m.readValue(filePath, PublishConfig.class);
		
		
		return publishConfig;
	}
}
