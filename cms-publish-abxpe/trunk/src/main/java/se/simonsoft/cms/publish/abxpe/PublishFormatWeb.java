package se.simonsoft.cms.publish.abxpe;

import se.simonsoft.cms.publish.PublishFormat;

public class PublishFormatWeb implements PublishFormat {

	@Override
	public String getFormat() {
		// TODO Auto-generated method stub
		return "web";
	}

	@Override
	public Compression getOutputCompression() {
		// PE uses zip as compression
		return Compression.zip;
	}

}
