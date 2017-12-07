package se.simonsoft.cms.publish.rest;

import java.util.HashMap;
import java.util.Map;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;
import se.simonsoft.cms.publish.databinds.publish.config.PublishConfigTemplateString;
import se.simonsoft.cms.publish.databinds.publish.job.PublishJob;
import se.simonsoft.cms.publish.databinds.publish.job.PublishJobManifest;

public class PublishJobManifestBuilder {

	private final PublishConfigTemplateString templateEvaluator;
	
	private static final String DEFAULT_TYPE = "default";
	
	public PublishJobManifestBuilder(PublishConfigTemplateString templateEvaluator) {
	
		this.templateEvaluator = templateEvaluator;
	}

	
	public void build(CmsItemPublish item, PublishJob job) {
		
		PublishJobManifest manifest = job.getOptions().getManifest();
		
		if (manifest.getType() == null) {
			manifest.setType(DEFAULT_TYPE);
		}
		
		if (hasDocnoTemplate(job)) {
			manifest.setDocument(buildDocument(item, job));
			if (hasDocnoMasterTemplate(job) && isTranslation(item)) {
				manifest.setMaster(null);
			} else {
				manifest.setMaster(null);
			}
			manifest.setCustom(null);
			manifest.setMeta(null);
		} else {
			// Prevent incomplete manifest when docno has not been configured.
			manifest.setDocument(null);
			manifest.setMaster(null);
			manifest.setCustom(null);
			manifest.setMeta(null);
		}
		
		
		
	}
	
	private Map<String, String> buildDocument(CmsItemPublish item, PublishJob job) {
	
		Map<String, String> result = new HashMap<String, String>();
		
		String docnoTemplate = job.getArea().getDocnoDocumentTemplate();
		
		String docno = templateEvaluator.evaluate(docnoTemplate);
		result.put("docno", docno);
		
		
		return result;
	}
	
	
	private boolean hasDocnoTemplate(PublishJob job) {
	
		String docnoTemplate = job.getArea().getDocnoDocumentTemplate();
		if (docnoTemplate == null || docnoTemplate.trim().isEmpty()) {
			return false;
		}
		return true;
	}
	
	private boolean hasDocnoMasterTemplate(PublishJob job) {
		
		String docnoTemplate = job.getArea().getDocnoMasterTemplate();
		if (docnoTemplate == null || docnoTemplate.trim().isEmpty()) {
			return false;
		}
		return true;
	}
	
	private boolean isTranslation(CmsItem item) {
		
		String locale = item.getProperties().getString("abx:TranslationLocale");
		return (locale != null && !locale.isEmpty());
	}
	
}
