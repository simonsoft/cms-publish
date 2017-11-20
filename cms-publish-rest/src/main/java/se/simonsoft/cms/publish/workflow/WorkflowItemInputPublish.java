package se.simonsoft.cms.publish.workflow;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.workflow.WorkflowItemInput;
import se.simonsoft.cms.publish.databinds.publish.job.PublishJob;

public class WorkflowItemInputPublish implements WorkflowItemInput {
	
	private static final String PUBLISH = "publish";
	private final CmsItemId itemId;
	private final PublishJob publishJob;
	
	public WorkflowItemInputPublish(CmsItemId itemId, PublishJob publishJob) {
		this.itemId = itemId;
		this.publishJob = publishJob;
	}

	@Override
	public String getAction() {
		return PUBLISH;
	}

	@Override
	public CmsItemId getItemId() {
		return this.itemId;
	}
	
	public PublishJob getPublishJob() {
		return publishJob;
	}

}
