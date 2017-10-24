package se.simonsoft.cms.publish.databinds.publish.job;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;

public class PublishJobReport3Json extends PublishJobReport3 {
	
	
	private String items;

	public String getItemsString() {
		return items;
	}
	public void setItems(JsonNode itemsString) {
		this.items = itemsString.toString();
	}
}
