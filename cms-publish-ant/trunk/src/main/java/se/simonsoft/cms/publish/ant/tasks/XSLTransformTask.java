package se.simonsoft.cms.publish.ant.tasks;

import org.apache.tools.ant.Task;

import se.simonsoft.cms.xmlsource.transform.DefaultTransformerService;
import se.simonsoft.cms.xmlsource.transform.SaxonTransformerFactory;

public class XSLTransformTask extends Task {

	public void execute()
	{
		
	}
	
	private void transform() 
	{
		DefaultTransformerService transformerService = new DefaultTransformerService(new SaxonTransformerFactory(null, null));
		
	}
}
