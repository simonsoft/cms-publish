package se.simonsoft.cms.publish.ant.filters;

import java.util.List;

import org.apache.tools.ant.Project;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.RepoRevision;

/**
 * Filters what properties to pass on to publishing
 * @author joakimdurehed
 *
 */
public interface FilterPublishProperties {

	/**
	 * Initializes toosl to use in filter
	 * @param itemList
	 * @param headRev
	 * @param project
	 */
	public void initFilter(CmsItem item, RepoRevision headRev, Project project, String publishTarget);
	
	/**
	 * Runs the filter including actually passing properties to publishing
	 */
	public void runFilter();
	
}
