package se.simonsoft.cms.publish;

/**
 * Represents a format, e.g. PDF, WEB, EPUB.
 * Format string approved by {@link PublishService#getPublishFormat(String)}.
 * {@link #toString()} can be a label if {@link #getFormat()} is a human-unreadable identifier.
 * @author takesson
 *
 */
public interface PublishFormat {

	public enum Compression {
		zip,
		tgz
	}
	
	/**
	 * @return Format identifier as understood by the publish service.
	 */
	String getFormat();
	
	/**
	 * 
	 * @return non-null if output file for the format is an archive
	 */
	Compression getOutputCompression();
	
}
