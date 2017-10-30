package se.simonsoft.cms.publish.databinds.publish.job;

import java.util.HashMap;

public class PublishJobStorage {
	private String type;
	private String pathprefix;
	private String pathconfigname;
	private String pathdir;
	private String pathnamebase;
	private HashMap<String, String> params;
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getPathprefix() {
		return pathprefix;
	}
	public void setPathprefix(String pathprefix) {
		this.pathprefix = pathprefix;
	}
	public String getPathconfigname() {
		return pathconfigname;
	}
	public void setPathconfigname(String pathconfigname) {
		this.pathconfigname = pathconfigname;
	}
	public String getPathdir() {
		return pathdir;
	}
	public void setPathdir(String pathdir) {
		this.pathdir = pathdir;
	}
	public String getPathnamebase() {
		return pathnamebase;
	}
	public void setPathnamebase(String pathnamebase) {
		this.pathnamebase = pathnamebase;
	}
	public HashMap<String, String> getParams() {
		return params;
	}
	public void setParams(HashMap<String, String> params) {
		this.params = params;
	}
}
