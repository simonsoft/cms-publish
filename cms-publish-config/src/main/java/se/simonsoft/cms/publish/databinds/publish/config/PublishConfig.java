package se.simonsoft.cms.publish.databinds.publish.config;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=false)
public class PublishConfig {

	private boolean active;
	private boolean visible;
	@JsonProperty ("status-include")
	private List<String> statusInclude;
	@JsonProperty ("profiling-include")
	private List<String> profilingInclude;
	@JsonProperty ("pathname-template")
	private String pathNameTemplate;
	private PublishConfigPublish publish;
	
	public PublishConfig(PublishConfig pc) {
		this.active = pc.isActive();
		this.visible = pc.isVisible();
		this.statusInclude = pc.getStatusInclude();
		this.profilingInclude = pc.getProfilingInclude();
		this.pathNameTemplate = pc.getPathNameTemplate();
		this.publish = pc.getPublish();
	}
	public PublishConfig() {
		
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
	public void setStatusInclude(List<String> statusInclude) {
		this.statusInclude = statusInclude;
	}
	public List<String> getProfilingInclude() {
		return profilingInclude;
	}
	public void setProfilingInclude(List<String> profilingInclude) {
		this.profilingInclude = profilingInclude;
	}
	public String getPathNameTemplate() {
		return pathNameTemplate;
	}
	public void setPathNameTemplate(String pathNameTemplate) {
		this.pathNameTemplate = pathNameTemplate;
	}
	public PublishConfigPublish getPublish() {
		return publish;
	}
	public void setPublish(PublishConfigPublish publish) {
		this.publish = publish;
	}
}
