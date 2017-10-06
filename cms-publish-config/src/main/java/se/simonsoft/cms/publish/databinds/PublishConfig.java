package se.simonsoft.cms.publish.databinds;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PublishConfig {

	private boolean active;
	private boolean visible;
	@JsonProperty("status-include")
	private List<String> statusInclude;
	@JsonProperty("profiling-include")
	private List<String> profilingInclude;
	@JsonProperty("pathname-template")
	private String pathNameTemplate;
	private PublishConfigPublish publish;
	
	public PublishConfig(boolean active, boolean visible, List<String> statusInclude, List<String> profilingInclude,
			String pathNameTemplate, PublishConfigPublish publish) {
		this.active = active;
		this.visible = visible;
		this.statusInclude = statusInclude;
		this.profilingInclude = profilingInclude;
		this.pathNameTemplate = pathNameTemplate;
		this.publish = publish;
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
	@Override
	public String toString() {
		return "PublishConfig [active=" + active + ", visible=" + visible + ", statusInclude=" + statusInclude
				+ ", profilingInclude=" + profilingInclude + ", pathNameTemplate=" + pathNameTemplate + ", publish="
				+ publish + "]";
	}
	
}
