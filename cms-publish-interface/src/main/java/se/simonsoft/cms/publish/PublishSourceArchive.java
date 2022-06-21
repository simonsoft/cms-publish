package se.simonsoft.cms.publish;

import java.io.InputStream;
import java.util.function.Supplier;

public class PublishSourceArchive implements PublishSource {

	private final Supplier<InputStream> inputStream;
	private final String inputEntry;
	
	public PublishSourceArchive(Supplier<InputStream> inputStream, String inputEntry) {
		this.inputStream = inputStream;
		this.inputEntry = inputEntry;
	}
	
	@Override
	public String getURI() {
		return null;
	}

	@Override
	public Supplier<InputStream> getInputStream() {
		return inputStream;
	}

	@Override
	public String getInputEntry() {
		return inputEntry;
	}
	

}
