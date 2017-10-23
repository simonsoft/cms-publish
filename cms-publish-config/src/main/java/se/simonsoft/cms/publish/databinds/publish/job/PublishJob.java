package se.simonsoft.cms.publish.databinds.publish.job;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import se.simonsoft.cms.publish.databinds.publish.config.PublishConfig;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PublishJob extends PublishConfig {

	private String configname;
	private String type;
	private String action;
	private String itemid;
	private PublishJobPublish publish;

	public PublishJob(PublishJob pj) {
		this.configname = pj.getConfigname();
		this.type = pj.getType();
		this.action = pj.getAction();
		this.itemid = pj.getItemid();
		this.publish = pj.getPublishJob();
		this.active = pj.isActive();
		this.visible = pj.isVisible();
		this.statusInclude = pj.getStatusInclude();
		this.profilingInclude = pj.getProfilingInclude();
		this.pathnameTemplate = pj.getPathnameTemplate();
	}
	public PublishJob() {
		super();
	}
	public String getConfigname() {
		return configname;
	}
	public void setConfigname(String configname) {
		this.configname = configname;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getAction() {
		return action;
	}
	public void setAction(String action) {
		this.action = action;
	}
	public String getItemid() {
		return itemid;
	}
	public void setItemid(String itemid) {
		this.itemid = itemid;
	}
	public PublishJobPublish getPublishJob() {
		return publish;
	}
	public void setPublish(PublishJobPublish publish) {
		this.publish = publish;
	}
	public boolean isActive() {
		return active;
	}
	public void setActive(boolean active) {
		this.active = active;
	}
	public boolean isVisible() {
		return visible;
	}
	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	public List<String> getStatusInclude() {
		return statusInclude;
	}
	public void setStatusInclude(List<String> statucInclude) {
		this.statusInclude = statucInclude;
	}
	public List<String> getProfilingInclude() {
		return profilingInclude;
	}
	public void setProfilingInclude(List<String> profilingInclude) {
		this.profilingInclude = profilingInclude;
	}
	public String getPathnameTemplate() {
		return pathnameTemplate;
	}
	public void setPathnameTemplate(String pathnameTemplate) {
		this.pathnameTemplate = pathnameTemplate;
	}
}
