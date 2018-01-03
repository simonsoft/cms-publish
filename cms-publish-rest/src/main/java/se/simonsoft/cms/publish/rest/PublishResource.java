package se.simonsoft.cms.publish.rest;

import java.io.StringWriter;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.repos.web.ReposHtmlHelper;
import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingRecipe;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingSet;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;
import se.simonsoft.cms.reporting.CmsItemLookupReporting;

@Path("/publish4")
public class PublishResource {
	
	private final String hostname;
	private final Map<CmsRepository, CmsItemLookupReporting> lookup;
	private final PublishConfigurationDefault publishConfiguration;
	private final PublishPackageZip repackageService;
	private final ReposHtmlHelper htmlHelper;

	@Inject
	public PublishResource(@Named("config:se.simonsoft.cms.hostname") String hostname,
			Map<CmsRepository, CmsItemLookupReporting> lookup,
			PublishConfigurationDefault publishConfiguration,
			PublishPackageZip repackageService,
			ReposHtmlHelper htmlHelper) {
		this.hostname = hostname;
		this.lookup = lookup;
		this.publishConfiguration = publishConfiguration;
		this.repackageService = repackageService;
		this.htmlHelper = htmlHelper;
	}
	
	private static final Logger logger = LoggerFactory.getLogger(PublishResource.class);
	
	@GET
	@Path("release")
	@Produces(MediaType.TEXT_HTML)
	public String getReleaseForm(@QueryParam("item") CmsItemIdArg itemId) throws Exception {
		
		if (itemId == null) {
			throw new IllegalArgumentException("Field 'item': required");
		}
		
		logger.debug("Getting form for item: {}", itemId);
		
		CmsItemLookupReporting cmsItemLookupReporting = lookup.get(itemId.getRepository());
		CmsItem item = cmsItemLookupReporting.getItem(itemId);
		CmsItemPublish itemPublish = new CmsItemPublish(item);
		
		PublishProfilingSet itemProfilingSet = publishConfiguration.getItemProfilingSet(itemPublish);
		Map<String, PublishConfig> configuration = publishConfiguration.getConfigurationFiltered(itemPublish);
		Map<String, PublishProfilingRecipe> itemProfiling = itemProfilingSet.getMap();
		
		VelocityEngine engine = new VelocityEngine();
		Properties p = new Properties();
		p.setProperty(RuntimeConstants.RESOURCE_LOADER, "class");
		p.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		engine.init(p);

		VelocityContext context = new VelocityContext();
		Template template = engine.getTemplate("se/simonsoft/cms/publish/templates/batch-publish-template.vm");
		
		context.put("item", item);
		context.put("itemProfiling", itemProfiling);
		context.put("configuration", configuration);
		context.put("reposHeadTags", htmlHelper.getHeadTags(null));

		StringWriter wr = new StringWriter();
		template.merge(context, wr);

		return wr.toString();
	}
	
	@GET
	@Path("release/download")
	@Produces("application/zip")
	public Response getDownload(@QueryParam("item") CmsItemIdArg itemId,
						@QueryParam("includemaster") boolean includeMaster,
						@QueryParam("includetranslations") boolean includeTranslations,
						@QueryParam("profiling") String[] profiling,
						@QueryParam("publication") String publication) throws Exception {
		
		if (itemId == null) {
			throw new IllegalArgumentException("Field 'item': required");
		}
		
		itemId.setHostnameOrValidate(this.hostname);
		
		if (!itemId.isPegged()) {
			throw new IllegalArgumentException("Field 'item': revision is required");
		}
		
		logger.debug("Download of item: {} requested, with master: {} and transaltions: {}", itemId, includeMaster, includeTranslations);
		
		return Response.ok("Succesfully called get release/download").build();
	}
}
