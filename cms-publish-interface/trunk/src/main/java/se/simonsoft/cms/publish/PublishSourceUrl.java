package se.simonsoft.cms.publish;

import java.net.URL;

public class PublishSourceUrl implements PublishSource {

	private URL url;

	public PublishSourceUrl(URL url) {
		this.url = url;
	}
	
	@Override
	public String getURI() {
		return url.toString();
	}

}
