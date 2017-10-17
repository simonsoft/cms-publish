package se.simonsoft.cms.publish.databinds.publish.job;

import java.util.Map;

public class PublishJobPublish {
	private String type;
	private String format;
	private Map<String, String> params;
	private PublishJobProfiling profiling;
	private PublishJobReport3 report3;
	private PublishJobStorage storage;
	private PublishJobPostProcess postprocess;
	private PublishJobDelivery delivery;
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getFormat() {
		return format;
	}
	public void setFormat(String format) {
		this.format = format;
	}
	public Map<String, String> getParams() {
		return params;
	}
	public void setParams(Map<String, String> params) {
		this.params = params;
	}
	public PublishJobProfiling getProfiling() {
		return profiling;
	}
	public void setProfiling(PublishJobProfiling profiling) {
		this.profiling = profiling;
	}
	public PublishJobReport3 getReport3() {
		return report3;
	}
	public void setReport3(PublishJobReport3 report3) {
		this.report3 = report3;
	}
	public PublishJobStorage getStorage() {
		return storage;
	}
	public void setStorage(PublishJobStorage storage) {
		this.storage = storage;
	}
	public PublishJobPostProcess getPostprocess() {
		return postprocess;
	}
	public void setPostprocess(PublishJobPostProcess postprocess) {
		this.postprocess = postprocess;
	}
	public PublishJobDelivery getDelivery() {
		return delivery;
	}
	public void setDelivery(PublishJobDelivery delivery) {
		this.delivery = delivery;
	}
	
	

}
