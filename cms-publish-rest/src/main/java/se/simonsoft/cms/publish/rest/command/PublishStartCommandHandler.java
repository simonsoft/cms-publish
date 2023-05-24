package se.simonsoft.cms.publish.rest.command;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectReader;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.command.ExternalCommandHandler;
import se.simonsoft.cms.publish.config.PublishConfiguration;
import se.simonsoft.cms.publish.config.PublishExecutor;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.job.PublishJob;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingRecipe;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;
import se.simonsoft.cms.publish.rest.PublishJobFactory;
import se.simonsoft.cms.publish.rest.PublishStartOptions;
import se.simonsoft.cms.release.translation.TranslationLocalesMapping;

public class PublishStartCommandHandler implements ExternalCommandHandler<PublishStartOptions> {

	private final PublishConfiguration publishConfiguration;
	private final PublishExecutor publishExecutor;
	private final PublishJobFactory jobFactory;
	
	@Inject
	public PublishStartCommandHandler(
			PublishConfiguration publishConfiguration,
			PublishExecutor publishExecutor,
			PublishJobFactory jobFactory,
			ObjectReader reader) {
		
		this.publishConfiguration = publishConfiguration;
		this.publishExecutor = publishExecutor;
		this.jobFactory = jobFactory;
	}
	
	@Override
	public String handleExternalCommand(CmsItemId itemId, PublishStartOptions options) {
		// TODO Ensure that error conditions throw CommandRuntimeExceptions with name and message that makes sense.
		Set<PublishJob> jobs = new HashSet<>();
		
		
		publishExecutor.startPublishJobs(jobs);
		
		return null;
	}

	@Override
	public Class<PublishStartOptions> getArgumentsClass() {
		return PublishStartOptions.class;
	}
	
	private PublishConfig getPublishConfiguration(CmsItemId itemId, String name) {
		
		Map<String, PublishConfig> configs = publishConfiguration.getConfiguration(itemId);
		return configs.get(name);
	}

	private PublishJob getPublishJob(CmsItemId itemId, PublishStartOptions options, PublishConfig config) {
		
		CmsItem item = null; // TODO: Get the item from reporting service.
		CmsItemPublish itemPublish = new CmsItemPublish(item);
		TranslationLocalesMapping localesRfc = (TranslationLocalesMapping) this.publishConfiguration.getTranslationLocalesMapping(itemPublish);

		// Use profilingname (get recipe from itemPublish), recipe from options or no profiling.
		// Verify that the config is intended for profiling, if set.
		PublishProfilingRecipe profilingRecipe = null;
		// Verify that a dynamic profiling recipe does not contain 'name' or 'logicalexpr'.
		
		
		// Set profilingRecipe.name to executionid if any of the start* parameters are provided.
		// Figure out how to manage profilingRecipe.name when no profiling is used (method validateFilter() throws exception when no filter parameters are set).
		// Need specific scenariotest setting 'startinput' but not 'startprofiling'.
		
		PublishJob job = jobFactory.getPublishJob(itemPublish, config, options.getPublication(), profilingRecipe, localesRfc, Optional.ofNullable(options.getStartpathname()));
		// TODO: set the options.getStartinput() in manifest.custom.startinput
		return job;
	}
	
}
