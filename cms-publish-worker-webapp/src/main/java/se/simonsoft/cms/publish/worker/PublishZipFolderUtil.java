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
package se.simonsoft.cms.publish.worker;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PublishZipFolderUtil {
	
	private static final Logger logger = LoggerFactory.getLogger(PublishZipFolderUtil.class);
	
	public static void addRootFolder(String folderName, InputStream in, OutputStream out) {
		ZipInputStream zis = new ZipInputStream(in);
		ZipOutputStream zos = new ZipOutputStream(out);
		try {
			ZipEntry nextEntry = zis.getNextEntry();
			while(nextEntry != null) {
				String filePath = folderName.concat("/").concat(nextEntry.getName());
				logger.trace("Entry will be put at path: {}", filePath);
				zos.putNextEntry(new ZipEntry(filePath));
				IOUtils.copy(zis, zos);
				
				zos.closeEntry();
				zis.closeEntry();
				
				nextEntry = zis.getNextEntry();
			}
			zos.finish();
			zos.close();
			
		} catch (IOException e) {
			logger.error("Error when trying to add root folder to zip.", e.getMessage());
			throw new RuntimeException(e);
		}
		
	}


}
