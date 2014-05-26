package se.simonsoft.cms.publish.ant;

import java.util.ArrayList;
import java.util.List;

public class ConfigsNode {
	
	protected List<ConfigNode> configs = new ArrayList<ConfigNode>();

	public ConfigsNode() {
		super();
	}
	
	  public void addConfig(final ConfigNode config) {
	    configs.add(config);
	  }
	
	  public List<ConfigNode> getConfigs() {
	    return configs;
	  }
	
	  public boolean isValid() {
	    if (configs.size() > 0) {
	      return true;
	    }
	    return false;
	  }
}
