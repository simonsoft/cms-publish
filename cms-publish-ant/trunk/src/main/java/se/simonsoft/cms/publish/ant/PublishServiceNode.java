package se.simonsoft.cms.publish.ant;

public class PublishServiceNode {

	protected String username;
	protected String password;
	protected boolean show = false;

	public PublishServiceNode() {
		super();
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(final String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(final String password) {
		this.password = password;
	}

	public boolean isShow() {
		return show;
	}

	public void setShow(final boolean show) {
		this.show = show;
	}

	public boolean isValid() {
		if (null != username && username.length() > 0) {
			return true;
		}
		return false;
	}
}
