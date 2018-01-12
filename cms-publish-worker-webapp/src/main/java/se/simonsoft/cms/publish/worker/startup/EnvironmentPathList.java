package se.simonsoft.cms.publish.worker.startup;

public class EnvironmentPathList {

	private final Environment environment;
	
	public EnvironmentPathList(Environment environment) {
		this.environment = environment;
	}
	
	String getPathFirst(String name) {
		
		String result = null;
		
		String envVariable = this.environment.getParamOptional(name);
		
		String[] split = null;
		if (envVariable != null && !envVariable.trim().isEmpty()) {
			split = envVariable.split(";");
		}
		if (split != null && split.length > 0) {
			result = split[0].replaceAll("\\\\", "/");
		}
		return result;
	}
}
