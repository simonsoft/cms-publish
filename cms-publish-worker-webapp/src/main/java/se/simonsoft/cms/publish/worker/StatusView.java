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

import java.io.StringWriter;
import java.util.List;
import java.util.Properties;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

import se.simonsoft.cms.publish.worker.status.report.WorkerStatusReport;
import se.simonsoft.cms.publish.worker.status.report.WorkerStatusReport.WorkerEvent;

@Path("status")
public class StatusView {
	
	private final WorkerStatusReport statusReport;
	private final VelocityEngine engine;
	
	@Inject
	public StatusView(WorkerStatusReport statusReport) {
		this.statusReport = statusReport;
		this.engine = getVelocityEngine();
	}
	
	@GET
	@Produces(MediaType.TEXT_HTML)
	public String getStatus() throws Exception {
		
		
		List<WorkerEvent> workerEvents = statusReport.getWorkerEvents();
		WorkerEvent workerLoop = statusReport.getLastWorkerLoop();
		
		VelocityContext context = new VelocityContext();
		context.put("workerEvents", workerEvents);
		context.put("workerLoop", workerLoop);
		
		Template template = engine.getTemplate("se/simonsoft/cms/publish/worker/templates/StatusTemplate.vm");
		
		StringWriter wr = new StringWriter();
		template.merge(context, wr);
		return wr.toString();
	}
	
	
	private VelocityEngine getVelocityEngine() {
		VelocityEngine engine = new VelocityEngine();
		Properties p = new Properties();
		p.setProperty(RuntimeConstants.RESOURCE_LOADER, "class");
		p.setProperty("runtime.references.strict", "true");
		p.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		engine.init(p);
		return engine;
	}
}
