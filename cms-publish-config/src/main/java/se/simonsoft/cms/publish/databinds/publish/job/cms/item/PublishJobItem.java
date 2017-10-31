package se.simonsoft.cms.publish.databinds.publish.job.cms.item;

import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsItemKind;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.properties.CmsItemProperties;
import se.simonsoft.cms.item.properties.CmsItemPropertiesMap;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PublishJobItem implements CmsItem {

	private String logicalhead;
	private String date;
	private String repourl;
	private String kind;
	private String namebase;
	private PublishJobItemCommit commit;
	private String uri;
	private String url;
	private String logical;
	private long revision;
	private boolean head;
	private String path;
	private PublishJobItemFile file;
	private HashMap<String, Object> meta;
	private String name;
	private PublishJobItemChecksum checksum;
	private CmsItemPropertiesMap properties;

	public String getLogicalhead() {
		return logicalhead;
	}
	public void setLogicalhead(String logicalhead) {
		this.logicalhead = logicalhead;
	}
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	public String getRepourl() {
		return repourl;
	}
	public void setRepourl(String repourl) {
		this.repourl = repourl;
	}
	public void setKind(String kind) {
		this.kind = kind;
	}
	public String getNamebase() {
		return namebase;
	}
	public void setNamebase(String namebase) {
		this.namebase = namebase;
	}
	public PublishJobItemCommit getCommit() {
		return commit;
	}
	public void setCommit(PublishJobItemCommit commit) {
		this.commit = commit;
	}
	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getLogical() {
		return logical;
	}
	public void setLogical(String logical) {
		this.logical = logical;
	}
	public long getRevision() {
		return revision;
	}
	public void setRevision(long revision) {
		this.revision = revision;
	}
	public boolean isHead() {
		return head;
	}
	public void setHead(boolean head) {
		this.head = head;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public PublishJobItemFile getFile() {
		return file;
	}
	public void setFile(PublishJobItemFile file) {
		this.file = file;
	}
	public HashMap<String, Object> getMeta() {
		return meta;
	}
	public void setMeta(HashMap<String, Object> meta) {
		this.meta = meta;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public PublishJobItemChecksum getChecksum() {
		return checksum;
	}
	public void setChecksum(PublishJobItemChecksum checksum) {
		this.checksum = checksum;
	}
	public CmsItemProperties getProperties() {
		return properties;
	}
	public void setProperties(CmsItemPropertiesMap properties) {
		this.properties = properties;
	}
	@JsonIgnore
	@Override
	public CmsItemId getId() {
		CmsItemPath itemPath = new CmsItemPath(this.path);
		CmsRepository repo = new CmsRepository(this.repourl);
		CmsItemId itemId = repo.getItemId(itemPath, this.revision);

		return itemId;
	}
	@JsonIgnore
	@Override
	public RepoRevision getRevisionChanged() {
		RepoRevision repoR = null;
		Date date = repoR.parseDate(this.date);
		repoR = new RepoRevision(this.revision, date);

		return repoR;
	}
	@JsonIgnore
	@Override
	public String getRevisionChangedAuthor() {
		return null;
	}
	@JsonIgnore
	@Override
	public CmsItemKind getKind() {
		CmsItemKind itemKind = CmsItemKind.fromString(this.kind);
		
		return itemKind;
	}
	@JsonIgnore
	@Override
	public String getStatus() {
		String status = null;
		if(this.properties.get("cms:status") != null) {
			status = this.properties.getString("cms:status");
		}
		return status;
	}
	@JsonIgnore
	@Override
	public long getFilesize() {
		return this.file.getSize();
	}
	@JsonIgnore
	@Override
	public void getContents(OutputStream receiver) throws UnsupportedOperationException {
	}
}
