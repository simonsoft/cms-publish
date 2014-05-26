package se.simonsoft.cms.publish.ant;

public class ConfigNode {
	
	protected String name;
	protected String value;

	public ConfigNode() {
		super();
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(final String value) {
		this.value = value;
	}
}
