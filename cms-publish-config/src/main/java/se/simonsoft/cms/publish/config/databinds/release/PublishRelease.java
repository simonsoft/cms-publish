package se.simonsoft.cms.publish.config.databinds.release;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingRecipe;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class PublishRelease {

    private CmsItem item;
    private Map<String, PublishConfig> config;
    private Map<String, PublishProfilingRecipe> profiling;

    public PublishRelease(CmsItem item, Map<String, PublishConfig> config, Map<String, PublishProfilingRecipe> profiling) {
        this.item = item;
        this.config = config;
        this.profiling = profiling;
    }

    public CmsItem getItem() { return this.item; }
    public void setItem(CmsItem item) { this.item = item; }

    public Map<String, PublishConfig> getConfig() { return this.config; }
    public void setConfig(Map<String, PublishConfig> config) { this.config = config; }

    public Map<String, PublishProfilingRecipe> getProfiling() { return this.profiling; }
    public void setProfiling(Map<String, PublishProfilingRecipe> profiling) { this.profiling = profiling; }
}
