package se.simonsoft.cms.publish.rest;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.junit.Test;
import org.mockito.Mockito;

import se.simonsoft.cms.reporting.CmsItemLookupReporting;
import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfigOptions;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingRecipe;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingSet;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;
import se.simonsoft.cms.publish.rest.config.filter.PublishConfigFilter;

public class PublishResourceTest {
	
	@Test
	public void publishResourceTest() throws Exception {
		Map<CmsRepository, CmsItemLookupReporting> lookupMapMock = Mockito.mock(Map.class);
		PublishConfigurationDefault publishConfigurationMock = Mockito.mock(PublishConfigurationDefault.class);
		CmsItemLookupReporting lookupReportingMock = Mockito.mock(CmsItemLookupReporting.class);
		List<PublishConfigFilter> configFiltersMock = Mockito.mock(List.class);
		CmsItem itemMock = Mockito.mock(CmsItem.class);
		RepoRevision revision = new RepoRevision(203, new Date());
		CmsItemIdArg itemId = new CmsItemIdArg("x-svn://demo.simonsoftcms.se/svn/demo1^/vvab/xml/Docs/Sa%20s.xml?p=9");
		
		PublishConfig config = new PublishConfig();
		config.setVisible(true);
		config.setOptions(new PublishConfigOptions());
		config.getOptions().setFormat("pdf");
		Map<String, PublishConfig> configMap = new HashMap();
		configMap.put("config", config);
		
		PublishProfilingRecipe recipe = new PublishProfilingRecipe();
		recipe.setName("Active");
		PublishProfilingSet ppSet = new PublishProfilingSet();
		ppSet.add(recipe);
				
		Mockito.when(lookupMapMock.get(itemId.getRepository())).thenReturn(lookupReportingMock);
		Mockito.when(lookupReportingMock.getItem(itemId)).thenReturn(itemMock);
		Mockito.when(itemMock.getId()).thenReturn(itemId);
		Mockito.when(itemMock.getRevisionChanged()).thenReturn(revision);
		Mockito.when(publishConfigurationMock.getConfigurationFiltered(Mockito.any(CmsItemPublish.class))).thenReturn(configMap);
		Mockito.when(publishConfigurationMock.getItemProfilingSet(Mockito.any(CmsItemPublish.class))).thenReturn(ppSet);
		PublishResource resource = new PublishResource("localhost", lookupMapMock, publishConfigurationMock, configFiltersMock);
		
		String releaseForm = resource.getReleaseForm(itemId);
		
		assertTrue(releaseForm.contains("http://demo.simonsoftcms.se/svn/demo1"));
		assertTrue(releaseForm.contains("Sa s.xml"));
		assertTrue(releaseForm.contains("203"));
		assertTrue(releaseForm.contains("/vvab/xml/Docs/Sa s.xml"));
		assertTrue(releaseForm.contains("pdf"));
		assertTrue(releaseForm.contains("Active"));
		
//		File tmpFile = new File("apa.html");
//		FileOutputStream output = new FileOutputStream(tmpFile);
//		output.write(releaseForm.getBytes());
//		output.close();
		
	}
}
