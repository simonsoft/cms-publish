package se.simonsoft.cms.publish;

/**
 * Represents a format, e.g. PDF, WEB, EPUB.
 * 
 * @author takesson
 *
 */
public class PublishFormat {

	String format;
	
	public PublishFormat(String format) {
		
		this.format = format;
	}
	
	// Staffan, I can foresee that we might communicate certain other aspect, such as whether the output is zipped.
	// You generally like settors better than massive constructors. But then it is difficult to make immutable?
	
	
}
