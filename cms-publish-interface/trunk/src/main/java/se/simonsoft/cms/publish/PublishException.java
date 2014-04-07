package se.simonsoft.cms.publish;

public class PublishException extends Exception {

	private static final long serialVersionUID = 1L;

	public PublishException(String message, Throwable cause) {
		super(message, cause);
	}

	public PublishException(String message) {
		super(message);
	}

}
