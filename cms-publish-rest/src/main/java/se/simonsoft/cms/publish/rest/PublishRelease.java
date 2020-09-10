/**
 * Copyright (C) 2009-2017 Simonsoft Nordic AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.simonsoft.cms.publish.rest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingRecipe;
import se.simonsoft.cms.reporting.response.CmsItemRepositem;

import java.util.Map;
import java.util.Set;

public class PublishRelease {

    private CmsItemRepositem item;
    private Map<String, PublishConfig> config;
    private Map<String, PublishProfilingRecipe> profiling;
    private Map<String, Set<String>> translationExecutions;
    private Map<String, Set<String>> releaseExecutions;

    public PublishRelease() {
        // Default constructor for Jackson.
    }

    public PublishRelease(
            CmsItemRepositem item,
            Map<String, PublishConfig> config,
            Map<String, PublishProfilingRecipe> profiling,
            Map<String, Set<String>> translationExecutions,
            Map<String, Set<String>> releaseExecutions) {

        this.item = item;
        this.config = config;
        this.profiling = profiling;
        this.translationExecutions = translationExecutions;
        this.releaseExecutions = releaseExecutions;
    }

    @JsonProperty
    public CmsItemRepositem getItem() { return this.item; }
    public void setItem(CmsItemRepositem item) { this.item = item; }

    @JsonProperty
    public Map<String, PublishConfig> getConfig() { return this.config; }
    public void setConfig(Map<String, PublishConfig> config) { this.config = config; }

    @JsonProperty
    public Map<String, PublishProfilingRecipe> getProfiling() { return this.profiling; }
    public void setProfiling(Map<String, PublishProfilingRecipe> profiling) { this.profiling = profiling; }

    @JsonIgnore
    public Map<String, Set<String>> getTranslationExecutions() { return this.translationExecutions; }
    public void setTranslationExecutions(Map<String, Set<String>> translationExecutions) { this.translationExecutions = translationExecutions; }

    @JsonIgnore
    public Map<String, Set<String>> getReleaseExecutions() { return this.releaseExecutions; }
    public void setReleaseExecutions(Map<String, Set<String>> releaseExecutions) { this.releaseExecutions = releaseExecutions; }
}
