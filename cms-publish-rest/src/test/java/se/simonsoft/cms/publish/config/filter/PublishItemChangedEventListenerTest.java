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
package se.simonsoft.cms.publish.config.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsItemKind;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.config.CmsConfigOption;
import se.simonsoft.cms.item.config.CmsResourceContext;
import se.simonsoft.cms.item.impl.CmsConfigOptionBase;
import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.item.info.CmsRepositoryLookup;
import se.simonsoft.cms.item.properties.CmsItemPropertiesMap;
import se.simonsoft.cms.item.workflow.WorkflowExecutor;
import se.simonsoft.cms.item.workflow.WorkflowItemInput;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfigArea;
import se.simonsoft.cms.publish.config.databinds.job.PublishJob;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobManifest;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobOptions;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobStorage;
import se.simonsoft.cms.publish.rest.config.filter.PublishConfigFilter;
import se.simonsoft.cms.publish.rest.config.filter.PublishConfigFilterActive;
import se.simonsoft.cms.publish.rest.config.filter.PublishConfigFilterStatus;
import se.simonsoft.cms.publish.rest.config.filter.PublishConfigFilterType;
import se.simonsoft.cms.publish.rest.event.PublishItemChangedEventListener;

public class PublishItemChangedEventListenerTest {

	private ObjectMapper mapper = new ObjectMapper();
	//Declaring all mocked objects. @Before will init them as clean mocks before each test and each individual test has to specify the mocks own behaviors. 
	@Mock CmsResourceContext mockContext;
	@Mock CmsItem mockItem;
	@Mock CmsRepositoryLookup mockLookup;
	@Mock Iterator<CmsConfigOption> mockOptionIterator;
	@Mock WorkflowExecutor<WorkflowItemInput> mockWorkflowExec; 
	
	private final String configStatusPath = "se/simonsoft/cms/publish/config/filter/publish-config-status.json";
	private final String publishJobStatusRelease = "se/simonsoft/cms/publish/config/filter/publish-job-status-release.json";
	private final String publishJobStatusTranslation = "se/simonsoft/cms/publish/config/filter/publish-job-status-translation.json";
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}
	
	@Test
	public void testDefaultItemEvent() throws Exception {
		
		//CmsItem mock. Not possible to get a real CmsItem in this context.
		CmsItemIdArg itemIdArg = new CmsItemIdArg(new CmsRepository("/svn", "demo1"), new CmsItemPath("/vvab/xml/documents/900276.xml"));
		itemIdArg.setHostname("ubuntu-cheftest1.pdsvision.net");
		CmsItemId itemId = itemIdArg.withPegRev(443L);
		when(mockItem.getId()).thenReturn(itemId);
		when(mockItem.getKind()).thenReturn(CmsItemKind.File);
		when(mockItem.getStatus()).thenReturn("Released");
		when(mockItem.getProperties()).thenReturn(new CmsItemPropertiesMap("cms:status", "Released"));
		
		HashMap<String, Object> metaMap = new HashMap<String, Object>();
		metaMap.put("embd_xml_a_type", "abxpe");
		when(mockItem.getMeta()).thenReturn(metaMap);
		
		//CmsRepositoryLookup mock. when called with mocked item it will return the mocked CmsResourceContext. 
		when(mockLookup.getConfig(mockItem.getId(), mockItem.getKind())).thenReturn(mockContext);
		
		//Mocking the iterator in mockContext. Easier way then instantiating mockContext with a real set of config.
		when(mockContext.iterator()).thenReturn(mockOptionIterator);
		when(mockOptionIterator.hasNext()).thenReturn(true, true, false); //First time hasNext(); is called answer true, second time true...
		
		//Instantiate a real CmsCongigOptionBase to be returned from mocked iterator when next() is called.
		CmsConfigOptionBase<String> configOptionStatus = new CmsConfigOptionBase<>("cmsconfig-publish:status", getPublishConfigFromPath(configStatusPath ));
		CmsConfigOptionBase<String> configOptionBogus = new CmsConfigOptionBase<>("cmsconfig-bogus:bogus", getPublishConfigFromPath(configStatusPath));
		when(mockOptionIterator.next()).thenReturn(configOptionStatus, configOptionBogus);
		
		//Real implementations of the filters. Declared as spies to be able to verify that they have been called.
		List<PublishConfigFilter> filters = new ArrayList<PublishConfigFilter>();
		PublishConfigFilterActive activeFilterSpy = spy(new PublishConfigFilterActive());
		filters.add(activeFilterSpy);
		
		PublishConfigFilterType typeFilterSpy = spy(new PublishConfigFilterType());
		filters.add(typeFilterSpy);
		
		PublishConfigFilterStatus statusFilterSpy = spy(new PublishConfigFilterStatus());
		filters.add(statusFilterSpy);
		
		PublishItemChangedEventListener eventListener = new PublishItemChangedEventListener(mockLookup,
																mockWorkflowExec,
																filters,
																mapper.reader());
		//Test starting point. 
		eventListener.onItemChange(mockItem);
		
		//Verifies that our mocks and spies has been called a certain amount of times.
		verify(mockLookup, times(1)).getConfig(mockItem.getId(), mockItem.getKind());
		verify(mockOptionIterator, times(2)).next();
		verify(activeFilterSpy, times(1)).accept(any(PublishConfig.class), any(CmsItem.class));
		verify(typeFilterSpy, times(1)).accept(any(PublishConfig.class), any(CmsItem.class));
		verify(statusFilterSpy, times(1)).accept(any(PublishConfig.class), any(CmsItem.class));
		
		//Caputures PublishJob arguments that our mocked workflow been called with.
		ArgumentCaptor<PublishJob> argCaptor = ArgumentCaptor.forClass(PublishJob.class); 
		verify(mockWorkflowExec, times(1)).startExecution(argCaptor.capture());
		
		//Asserts on argument that executor has been called with.
		PublishJob publishJob = argCaptor.getValue();
		assertEquals("status", publishJob.getConfigname());
		assertEquals("publish-noop", publishJob.getAction());
		assertTrue(publishJob.getStatusInclude().contains("Review"));
		assertTrue(publishJob.getStatusInclude().contains("Released"));
		
		assertEquals("DOC_900276_Released", publishJob.getOptions().getPathname());
		assertEquals("x-svn:///svn/demo1^/vvab/xml/documents/900276.xml?p=443", publishJob.getItemid());
		assertEquals("x-svn:///svn/demo1^/vvab/xml/documents/900276.xml?p=443", publishJob.getOptions().getSource());
		
		//Storage
		PublishJobStorage storage = publishJob.getOptions().getStorage();
		assertEquals("s3", storage.getType());
		assertEquals("/vvab/xml/documents/900276.xml", storage.getPathdir());
		assertEquals("900276_r0000000443", storage.getPathnamebase());
		assertEquals("cms4", storage.getPathversion());
		assertEquals("status", storage.getPathconfigname());
		
		// Manifest
		assertEquals("DOC_$", publishJob.getArea().getDocnoDocumentTemplate().substring(0, 5));
		PublishJobManifest manifest =  publishJob.getOptions().getManifest();
		assertEquals("default", manifest.getType());
		assertEquals("DOC_900276", manifest.getDocument().get("docno"));
		assertEquals("demo1", storage.getPathcloudid());
	}
	
	
	@Test
	public void testReleaseItemChangedWithValidated() throws Exception {
		
		//CmsItem mock. Not possible to get a real CmsItem in this context.
		CmsItemIdArg itemId = (CmsItemIdArg) new CmsItemIdArg(new CmsRepository("/svn", "demo1"), new CmsItemPath("/vvab/release/B/xml/documents/900108.xml")).withPegRev(145L);
		itemId.setHostname("ubuntu-cheftest1.pdsvision.net");
		when(mockItem.getId()).thenReturn(itemId);
		when(mockItem.getKind()).thenReturn(CmsItemKind.File);
		when(mockItem.getStatus()).thenReturn("Released");
		CmsItemPropertiesMap props = new CmsItemPropertiesMap("cms:status", "Released");
		props.and("abx:ReleaseMaster", "bogus");
		props.and("abx:ReleaseLabel", "B");
		props.and("abx:ReleaseLocale", "sv-SE"); // Added to Release Area in CMS 4.3. 
		when(mockItem.getProperties()).thenReturn(props);
		
		HashMap<String, Object> metaMap = new HashMap<String, Object>();
		metaMap.put("embd_xml_a_type", "operator");
		when(mockItem.getMeta()).thenReturn(metaMap);
		
		//CmsRepositoryLookup mock. when called with mocked item it will return the mocked CmsResourceContext. 
		when(mockLookup.getConfig(mockItem.getId(), mockItem.getKind())).thenReturn(mockContext);
		
		//Mocking the iterator in mockContext. Easier way then instantiating mockContext with a real set of config.
		when(mockContext.iterator()).thenReturn(mockOptionIterator);
		when(mockOptionIterator.hasNext()).thenReturn(true, false); //First time answer true, second time answer false.
		
		//Instantiate a real CmsCongigOptionBase to be returned from mocked iterator when next() is called.
		CmsConfigOptionBase<String> configOptionStatus = new CmsConfigOptionBase<>("cmsconfig-publish:simple-pdf", getPublishConfigFromPath(configStatusPath));
		when(mockOptionIterator.next()).thenReturn(configOptionStatus);
		
		List<PublishConfigFilter> filters = new ArrayList<PublishConfigFilter>();
		filters.add(new PublishConfigFilterActive());
		filters.add(new PublishConfigFilterType());
		filters.add(new PublishConfigFilterStatus());
		
		PublishItemChangedEventListener eventListener = new PublishItemChangedEventListener(mockLookup,
																mockWorkflowExec,
																filters,
																mapper.reader());
		eventListener.onItemChange(mockItem);
		
		//Captures PublishJob arguments that our mocked workflow been called with.
		ArgumentCaptor<PublishJob> argCaptor = ArgumentCaptor.forClass(PublishJob.class);
		
		//Verifies that our mocked workflowExecutor has been called a certain amount of times.
		verify(mockWorkflowExec, times(1)).startExecution(argCaptor.capture());
		
		PublishJob publishJob = argCaptor.getValue();
		
		String statusJobFromFile = getPublishConfigFromPath(publishJobStatusRelease);
		ObjectReader publishJobWriter = mapper.reader().forType(PublishJob.class);
		PublishJob pjValidated = publishJobWriter.readValue(statusJobFromFile);
		
		//Assert against validated and deserialized publish-job-status.json file.
		assertEquals(pjValidated.getConfigname(), publishJob.getConfigname());
		assertEquals(pjValidated.getType(), publishJob.getType());
		assertEquals(pjValidated.getAction(), publishJob.getAction());
		assertEquals(pjValidated.isActive(), publishJob.isActive());
		assertEquals(pjValidated.isVisible(), publishJob.isVisible());
		assertTrue(pjValidated.getStatusInclude().contains(publishJob.getStatusInclude().get(0)));
		assertTrue(pjValidated.getStatusInclude().contains(publishJob.getStatusInclude().get(1)));
		assertEquals(pjValidated.getArea().getPathnameTemplate(), publishJob.getArea().getPathnameTemplate());
		assertEquals(pjValidated.getItemid(), publishJob.getItemid());
		
		PublishJobOptions optionsValidated = pjValidated.getOptions();
		PublishJobOptions options = publishJob.getOptions();
		
		assertEquals(optionsValidated.getPathname(), options.getPathname());
		assertEquals(optionsValidated.getType(), options.getType());
		assertEquals(optionsValidated.getFormat(), options.getFormat());
		assertEquals(optionsValidated.getSource(), options.getSource());
		
		assertEquals(optionsValidated.getParams().get("stylesheet"), options.getParams().get("stylesheet"));
		assertEquals(optionsValidated.getParams().get("pdfconfig"), options.getParams().get("pdfconfig"));
		
		assertEquals(optionsValidated.getStorage().getType(), options.getStorage().getType());
		assertEquals(optionsValidated.getStorage().getPathversion(), options.getStorage().getPathversion());
		assertEquals(optionsValidated.getStorage().getPathconfigname(), options.getStorage().getPathconfigname());
		assertEquals(optionsValidated.getStorage().getPathcloudid(), options.getStorage().getPathcloudid());
		assertEquals(optionsValidated.getStorage().getPathdir(), options.getStorage().getPathdir());
		assertEquals(optionsValidated.getStorage().getPathnamebase(), options.getStorage().getPathnamebase());
		assertEquals(optionsValidated.getStorage().getParams().get("s3bucket"), options.getStorage().getParams().get("s3bucket"));
		
		PublishJobManifest manifestValidated = optionsValidated.getManifest();
		PublishJobManifest manifest = options.getManifest();
		
		assertEquals(manifestValidated.getType(), manifest.getType());
		assertEquals(manifestValidated.getJob(), manifest.getJob());
		assertEquals(manifestValidated.getDocument(), manifest.getDocument());
		assertEquals(manifestValidated.getMeta(), manifest.getMeta());
		assertEquals(manifestValidated.getCustom(), manifest.getCustom());	
	}
	
	@Test
	public void testTranslationItemChangedWithValidated() throws Exception {
		
		//CmsItem mock. Not possible to get a real CmsItem in this context.
		CmsItemIdArg itemId = (CmsItemIdArg) new CmsItemIdArg(new CmsRepository("/svn", "demo1"), new CmsItemPath("/vvab/lang/en-GB/release/B/xml/documents/900108.xml")).withPegRev(145L);
		itemId.setHostname("ubuntu-cheftest1.pdsvision.net");
		when(mockItem.getId()).thenReturn(itemId);
		when(mockItem.getKind()).thenReturn(CmsItemKind.File);
		when(mockItem.getStatus()).thenReturn("Released");
		CmsItemPropertiesMap props = new CmsItemPropertiesMap("cms:status", "Released");
		props.and("abx:TranslationMaster", "bogus");
		props.and("abx:ReleaseLabel", "B");
		props.and("abx:ReleaseLocale", "sv-SE");
		props.and("abx:TranslationLocale", "en-GB");
		when(mockItem.getProperties()).thenReturn(props);
		
		HashMap<String, Object> metaMap = new HashMap<String, Object>();
		metaMap.put("embd_xml_a_type", "operator");
		when(mockItem.getMeta()).thenReturn(metaMap);
		
		//CmsRepositoryLookup mock. when called with mocked item it will return the mocked CmsResourceContext. 
		when(mockLookup.getConfig(mockItem.getId(), mockItem.getKind())).thenReturn(mockContext);
		
		//Mocking the iterator in mockContext. Easier way then instantiating mockContext with a real set of config.
		when(mockContext.iterator()).thenReturn(mockOptionIterator);
		when(mockOptionIterator.hasNext()).thenReturn(true, false); //First time answer true, second time answer false.
		
		//Instantiate a real CmsCongigOptionBase to be returned from mocked iterator when next() is called.
		CmsConfigOptionBase<String> configOptionStatus = new CmsConfigOptionBase<>("cmsconfig-publish:simple-pdf", getPublishConfigFromPath(configStatusPath));
		when(mockOptionIterator.next()).thenReturn(configOptionStatus);
		
		List<PublishConfigFilter> filters = new ArrayList<PublishConfigFilter>();
		filters.add(new PublishConfigFilterActive());
		filters.add(new PublishConfigFilterType());
		filters.add(new PublishConfigFilterStatus());
		
		PublishItemChangedEventListener eventListener = new PublishItemChangedEventListener(mockLookup,
																mockWorkflowExec,
																filters,
																mapper.reader());
		eventListener.onItemChange(mockItem);
		
		//Captures PublishJob arguments that our mocked workflow been called with.
		ArgumentCaptor<PublishJob> argCaptor = ArgumentCaptor.forClass(PublishJob.class);
		
		//Verifies that our mocked workflowExecutor has been called a certain amount of times.
		verify(mockWorkflowExec, times(1)).startExecution(argCaptor.capture());
		
		PublishJob publishJob = argCaptor.getValue();
		
		String statusJobFromFile = getPublishConfigFromPath(publishJobStatusTranslation);
		ObjectReader publishJobWriter = mapper.reader().forType(PublishJob.class);
		PublishJob pjValidated = publishJobWriter.readValue(statusJobFromFile);
		
		//Assert against validated and deserialized publish-job-status.json file.
		assertEquals(pjValidated.getConfigname(), publishJob.getConfigname());
		assertEquals(pjValidated.getType(), publishJob.getType());
		assertEquals(pjValidated.getAction(), publishJob.getAction());
		assertEquals(pjValidated.isActive(), publishJob.isActive());
		assertEquals(pjValidated.isVisible(), publishJob.isVisible());
		assertTrue(pjValidated.getStatusInclude().contains(publishJob.getStatusInclude().get(0)));
		assertTrue(pjValidated.getStatusInclude().contains(publishJob.getStatusInclude().get(1)));
		assertEquals(pjValidated.getArea().getPathnameTemplate(), publishJob.getArea().getPathnameTemplate());
		assertEquals(pjValidated.getItemid(), publishJob.getItemid());
		
		PublishJobOptions optionsValidated = pjValidated.getOptions();
		PublishJobOptions options = publishJob.getOptions();
		
		assertEquals(optionsValidated.getPathname(), options.getPathname());
		assertEquals(optionsValidated.getType(), options.getType());
		assertEquals(optionsValidated.getFormat(), options.getFormat());
		assertEquals(optionsValidated.getSource(), options.getSource());
		
		assertEquals(optionsValidated.getParams().get("stylesheet"), options.getParams().get("stylesheet"));
		assertEquals(optionsValidated.getParams().get("pdfconfig"), options.getParams().get("pdfconfig"));
		
		assertEquals(optionsValidated.getStorage().getType(), options.getStorage().getType());
		assertEquals(optionsValidated.getStorage().getPathversion(), options.getStorage().getPathversion());
		assertEquals(optionsValidated.getStorage().getPathconfigname(), options.getStorage().getPathconfigname());
		assertEquals(optionsValidated.getStorage().getPathcloudid(), options.getStorage().getPathcloudid());
		assertEquals(optionsValidated.getStorage().getPathdir(), options.getStorage().getPathdir());
		assertEquals(optionsValidated.getStorage().getPathnamebase(), options.getStorage().getPathnamebase());
		assertEquals(optionsValidated.getStorage().getParams().get("s3bucket"), options.getStorage().getParams().get("s3bucket"));
		
		PublishConfigArea areaValidated = pjValidated.getArea();
		PublishConfigArea area = publishJob.getArea();
		assertEquals(areaValidated.getType(), area.getType());
		
		PublishJobManifest manifestValidated = optionsValidated.getManifest();
		PublishJobManifest manifest = options.getManifest();
				
		assertEquals(manifestValidated.getType(), manifest.getType());
		assertEquals(manifestValidated.getJob(), manifest.getJob());
		assertEquals(manifestValidated.getDocument(), manifest.getDocument());
		assertEquals(manifestValidated.getMaster(), manifest.getMaster());
		assertEquals(manifestValidated.getMeta(), manifest.getMeta());
		assertEquals(manifestValidated.getCustom(), manifest.getCustom());	
	}
	
	
	private String getPublishConfigFromPath(String path) throws FileNotFoundException, IOException {
		InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(path);
		BufferedReader br = new BufferedReader(new InputStreamReader(resourceAsStream));
		StringBuilder out = new StringBuilder();
		String line;
		while((line = br.readLine()) != null) {
			out.append(line);
		}
		br.close();
		return out.toString();
	}
}
