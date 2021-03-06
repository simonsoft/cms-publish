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
package se.simonsoft.cms.publish.config.export;

import se.simonsoft.cms.item.export.CmsExportJob;
import se.simonsoft.cms.item.export.CmsExportJobSingle;
import se.simonsoft.cms.item.export.CmsExportJobZip;
import se.simonsoft.cms.item.export.CmsExportPrefix;
import se.simonsoft.cms.item.export.CmsImportJob;
import se.simonsoft.cms.item.export.CmsImportJobSingle;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobStorage;

public class PublishExportJobFactory { //extends CmsExportJobSingle implements CmsExportJob.SingleItem, CmsImportJob {

	public static CmsExportJobSingle getExportJobSingle(PublishJobStorage storage, String jobExtension) {
		return new CmsExportJobSingle(createJobPrefix(storage), getJobName(storage), jobExtension);
	}
	
	public static CmsExportJob getExportJobZip(PublishJobStorage storage, String jobExtension) {
		return new CmsExportJobZip(createJobPrefix(storage), getJobName(storage), jobExtension);
	}
	
	
	public static CmsImportJob getImportJobSingle(PublishJobStorage storage, String jobExtension) {
		return new CmsImportJobSingle(createJobPrefix(storage), getJobName(storage), jobExtension);
	}

	
	public static CmsExportPrefix createJobPrefix(PublishJobStorage storage) {
		return new CmsExportPrefix(storage.getPathconfigname());
	}

	public static String getJobName(PublishJobStorage storage) {
		//Path dir is preceded by slash, this will cause double slashes in the exportJob. Therefore we will remove it. 
		String pathdir = storage.getPathdir();
		
		if (pathdir != null && pathdir.startsWith("/")) {
			pathdir = pathdir.replaceFirst("/", "");
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append(pathdir);
		sb.append("/");
		sb.append(storage.getPathnamebase());
		return sb.toString();
	}

}
