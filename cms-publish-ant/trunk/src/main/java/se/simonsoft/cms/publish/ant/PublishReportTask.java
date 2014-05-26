package se.simonsoft.cms.publish.ant;

import org.apache.tools.ant.Task;

import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.publish.PublishSourceCmsItemId;
import se.simonsoft.cms.publish.impl.PublishRequestDefault;

public class PublishReportTask extends Task {
	protected ConfigsNode configs;
	protected String publishservice;
	protected ParamsNode params;
	protected String workspace;
	protected String jobname;
	protected String filename;
	protected String buildid;
	protected String buildnumber;
	protected String jenkinshome;
	protected boolean fail;

	/**
	 * @return the jenkinshome
	 */
	public String getJenkinshome() {
		return jenkinshome;
	}

	/**
	 * @return the fail
	 */
	public boolean isFail() {
		return fail;
	}

	/**
	 * @param fail the fail to set
	 */
	public void setFail(boolean fail) {
		this.fail = fail;
	}

	/**
	 * @param jenkinshome the jenkinshome to set
	 */
	public void setJenkinshome(String jenkinshome) {
		this.jenkinshome = jenkinshome;
	}
	
	/**
	 * @return the buildnumber
	 */
	public String getBuildnumber() {
		return buildnumber;
	}

	/**
	 * @param buildnumber the buildnumber to set
	 */
	public void setBuildnumber(String buildnumber) {
		this.buildnumber = buildnumber;
	}

	/**
	 * @return the workspace
	 */
	public String getWorkspace() {
		return workspace;
	}

	/**
	 * @return the storelocation
	 */
	public String getFilename() {
		return filename;
	}

	/**
	 * @param storelocation the storelocation to set
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}

	/**
	 * @return the buildid
	 */
	public String getBuildid() {
		return buildid;
	}

	/**
	 * @param buildid the buildid to set
	 */
	public void setBuildid(String buildid) {
		this.buildid = buildid;
	}

	/**
	 * @param workspace the workspace to set
	 */
	public void setWorkspace(String workspace) {
		this.workspace = workspace;
	}

	/**
	 * @return the jobname
	 */
	public String getJobname() {
		return jobname;
	}

	/**
	 * @param jobname the jobname to set
	 */
	public void setJobname(String jobname) {
		this.jobname = jobname;
	}

	/**
	 * @return the params
	 */
	public ParamsNode getParams() {
		return params;
	}

	/**
	 * @param params the params to set
	 */
	public void addConfiguredParams(ParamsNode params) {
		this.params = params;
	}

	/**
	 * @return the configs
	 */
	public ConfigsNode getConfigs() {
		return configs;
	}

	/**
	 * @param configs the configs to set
	 */
	public void addConfiguredConfigs(ConfigsNode configs) {
		this.configs = configs;
	}

	/**
	 * @return the publishservice
	 */
	public String getPublishservice() {
		return publishservice;
	}

	/**
	 * @param publishservice the publishservice to set
	 */
	public void setPublishservice(String publishservice) {
		this.publishservice = publishservice;
	}
	
	public void execute(){
		
	}
	
	private void parseItemsForPublish()
	{
		
	}
	
	/*
	 * Create the default request and set the config, params and other properties
	 */
	private PublishRequestDefault createRequestDefault()
	{
		PublishRequestDefault publishRequest = new PublishRequestDefault();
		// set config
		if (null != configs && configs.isValid()) {
			for (final ConfigNode config : configs.getConfigs()) {
				log("Configs: " + config.getName() + ":" + config.getValue());
				publishRequest.addConfig(config.getName(), config.getValue());
			}
		}
		// set params
		if (null != params && params.isValid()) {
			for (final ParamNode param : params.getParams()) {
				log("Params: " + param.getName() + ":" + param.getValue());
				if(param.getName().equals("file")){
					publishRequest.setFile(
							new PublishSourceCmsItemId(
									new CmsItemIdArg(param.getValue())
									));
					log("PublishRequestFile: " + publishRequest.getFile().getURI());
				}
				publishRequest.addParam	(param.getName(), param.getValue());


			}	
		}
		return publishRequest;
	}
}
