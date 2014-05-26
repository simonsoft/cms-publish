package se.simonsoft.cms.publish.ant;

/**
 * @author joakimdurehed
 *
 */
public class ParamNode {
	protected String name;
	protected String value;

	public ParamNode() {
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
