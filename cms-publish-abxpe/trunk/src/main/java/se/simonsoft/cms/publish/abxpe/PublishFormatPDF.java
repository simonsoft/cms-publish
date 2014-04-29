package se.simonsoft.cms.publish.abxpe;

import se.simonsoft.cms.publish.PublishFormat;

public class PublishFormatPDF implements PublishFormat {

	@Override
	public String getFormat() {
		// PDF format
		return "pdf";
	}

	@Override
	public Compression getOutputCompression() {
		// PE does not compress PDFs by default.
		return null;
	}

}
