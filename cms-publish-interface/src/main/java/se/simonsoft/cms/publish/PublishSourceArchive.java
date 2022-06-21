package se.simonsoft.cms.publish;

import java.io.InputStream;

public class PublishSourceArchive implements PublishSource {

	private final InputStream inputStream;
	private final String inputEntry;
	
	public PublishSourceArchive(InputStream inputStream, String inputEntry) {
		this.inputStream = inputStream;
		this.inputEntry = inputEntry;
	}
	
	@Override
	public String getURI() {
		return null;
	}

	@Override
	public InputStream getInputStream() {
		return inputStream;
	}

	@Override
	public String getInputEntry() {
		return inputEntry;
	}
	

}
