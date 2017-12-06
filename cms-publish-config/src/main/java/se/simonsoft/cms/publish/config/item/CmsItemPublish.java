package se.simonsoft.cms.publish.config.item;

import java.io.OutputStream;
import java.util.Map;

import se.simonsoft.cms.item.Checksum;
import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsItemKind;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.properties.CmsItemProperties;

public class CmsItemPublish implements CmsItem {

	private final CmsItem item;
	
	public CmsItemPublish(CmsItem item) {
		this.item = item;
	}

	public boolean isRelease() {
		return item.getProperties().containsProperty("abx:ReleaseMaster") && item.getProperties().containsProperty("abx:ReleaseLabel");
	}
	
	public boolean isTranslation() {
		return item.getProperties().containsProperty("abx:TranslationLocale");
	}
	
	public String getReleaseLabel() {
		return item.getProperties().getString("abx:ReleaseLabel");
	}
	
	public String getTranslationLocale() {
		return item.getProperties().getString("abx:TranslationLocale");
	}
	
	
	// START: Passthrough methods.
	
	@Override
	public CmsItemId getId() {
		return item.getId();
	}

	@Override
	public RepoRevision getRevisionChanged() {
		return item.getRevisionChanged();
	}

	@Override
	public String getRevisionChangedAuthor() {
		return item.getRevisionChangedAuthor();
	}

	@Override
	public CmsItemKind getKind() {
		return item.getKind();
	}

	@Override
	public String getStatus() {
		return item.getStatus();
	}

	@Override
	public Checksum getChecksum() {
		return item.getChecksum();
	}

	@Override
	public CmsItemProperties getProperties() {
		return item.getProperties();
	}

	@Override
	public Map<String, Object> getMeta() {
		return item.getMeta();
	}

	@Override
	public long getFilesize() {
		return item.getFilesize();
	}

	@Override
	public void getContents(OutputStream receiver) throws UnsupportedOperationException {
		item.getContents(receiver);
	}

}
