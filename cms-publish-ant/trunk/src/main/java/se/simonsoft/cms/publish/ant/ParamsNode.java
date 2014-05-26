package se.simonsoft.cms.publish.ant;

import java.util.ArrayList;
import java.util.List;

public class ParamsNode {

	protected List<ParamNode> params = new ArrayList<ParamNode>();

	public ParamsNode() {
		super();
	}

	public void addParam(final ParamNode param) {
		params.add(param);
	}

	public List<ParamNode> getParams() {
		return params;
	}

	public boolean isValid() {
		if (params.size() > 0) {
			return true;
	    }
	    return false;
	}
}
