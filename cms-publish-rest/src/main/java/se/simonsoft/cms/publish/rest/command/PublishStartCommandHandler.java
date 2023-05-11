package se.simonsoft.cms.publish.rest.command;

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.command.ExternalCommandHandler;
import se.simonsoft.cms.publish.rest.PublishStartOptions;

public class PublishStartCommandHandler implements ExternalCommandHandler<PublishStartOptions> {

	@Override
	public String handleExternalCommand(CmsItemId item, PublishStartOptions arguments) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class<PublishStartOptions> getArgumentsClass() {
		return PublishStartOptions.class;
	}

}
