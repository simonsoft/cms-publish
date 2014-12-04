/**
 * Copyright (C) 2009-2013 Simonsoft Nordic AB
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
package se.simonsoft.cms.publish.ant.filters;

import org.apache.tools.ant.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.RepoRevision;

public class MultipleLangPublishingFilter implements FilterPublishProperties {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private CmsItem item;
	private RepoRevision headRev;
	private Project project;
	private String publishTarget;

	public MultipleLangPublishingFilter() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void initFilter(CmsItem item, RepoRevision headRev, Project project,
			String publishTarget) {
		this.item = item;
		this.headRev = headRev;
		this.project = project;
		this.publishTarget = publishTarget;

	}

	@Override
	public void runFilter() {

		this.publishItem(this.determineOutputPath());

	}

	/**
	 * Determines the outputpath to be used for publishing
	 * 
	 * @return
	 */
	private String determineOutputPath() {
		
		StringBuilder newpath = new StringBuilder();
		
		String outputpath = this.project.getProperty("outputfolder");
		String masterlang = this.project.getProperty("masterlang");
		
		logger.debug("outputfolder {} masterlang {}", outputpath, masterlang);
		
		newpath.append(outputpath);
		
		// ${outputfolder}/${filename}/translations/${lang}
		if (!masterlang.equals(item.getProperties().getString("abx:lang"))) {
			logger.debug("Change outputpath for translation");
			newpath.append("/"
					+ this.item.getId().getRelPath().getNameBase());
			newpath.append("/translations/"
					+ item.getProperties().getString("abx:lang"));
			logger.debug("Path for translation: {}", newpath.toString());

		}
		return newpath.toString();
	}

	/**
	 * Publishes an item using the publishTarget
	 * 
	 * @param outputPath
	 */
	private void publishItem(String outputPath) {


		this.project.setProperty("param.file",
				item.getId().withPegRev(this.headRev.getNumber()).toString());

		this.project.setProperty("filename", item.getId().getRelPath()
				.getNameBase());

		this.project.setProperty("lang",
				item.getProperties().getString("abx:lang"));

		this.project.setProperty("outputpath", outputPath);

		// A test:
		this.project.getProperties().put("CMSITEM", this.item);

		// RepoRevision itemRepoRev = item.getRevisionChanged();
		logger.debug("file: {} filename: {} lang {}  path {}", item.getId()
				.withPegRev(this.headRev.getNumber()).toString(), item.getId()
				.getRelPath().getNameBase(),
				item.getProperties().getString("abx:lang"), outputPath);

		this.project.executeTarget(this.publishTarget);
	}

}
