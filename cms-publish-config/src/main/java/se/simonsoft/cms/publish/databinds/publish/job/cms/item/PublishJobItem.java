package se.simonsoft.cms.publish.databinds.publish.job.cms.item;

import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

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
	private int revision;
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
	public int getRevision() {
		return revision;
	}
	public void setRevision(int revision) {
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
	@Override
	public String toString() {
		return "PublishJobItem [locicalhead=" + logicalhead + ", date=" + date + ", repourl=" + repourl + ", kind=" + kind + ", namebase=" + namebase + ", commit=" + commit + ", uri=" + uri + ", url="
				+ url + ", logical=" + logical + ", revision=" + revision + ", head=" + head + ", path=" + path + ", file=" + file + ", meta=" + meta + ", name=" + name + ", checksum=" + checksum
				+ ", properties=" + properties + "]";
	}
	@Override
	public CmsItemId getId() {
		CmsItemPath itemPath = new CmsItemPath(this.path);
		CmsRepository repo = new CmsRepository(this.url);
		long rev = this.revision;
		CmsItemId itemId = repo.getItemId().withRelPath(itemPath).withPegRev(rev);

		return itemId;
	}
	@Override
	public RepoRevision getRevisionChanged() {
		Date date = null;
		try {
			String jsonDate = this.date;
			jsonDate = jsonDate.replaceAll("T", " ");
			DateFormat format = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss.SSS");
			date = format.parse(jsonDate);
		}catch(ParseException e) {
			e.printStackTrace();
		}
		RepoRevision repoR = new RepoRevision(this.revision, date);

		return repoR;
	}
	@Override
	public String getRevisionChangedAuthor() {
		throw new UnsupportedOperationException("Author is not available");
	}
	@Override
	public CmsItemKind getKind() {
		CmsItemKind itemKind = CmsItemKind.fromString(this.kind);
		return itemKind;
	}
	@Override
	public String getStatus() {
		String status = null;
		if(this.properties.get("cms:status") != null) {
			status = this.properties.getString("cms:status");
		}
		return status;
	}
	@Override
	public long getFilesize() {
		return this.file.getSize();
	}
	@Override
	public void getContents(OutputStream receiver) throws UnsupportedOperationException {
		throw new UnsupportedOperationException("Content is not available");

	}
}
