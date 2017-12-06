package se.simonsoft.cms.publish.config.manifest;

import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectWriter;

import se.simonsoft.cms.item.export.CmsExportItem;
import se.simonsoft.cms.item.export.CmsExportPath;
import se.simonsoft.cms.publish.databinds.publish.job.PublishJobManifest;

public class CmsExportItemPublishManifest implements CmsExportItem {

	private final ObjectWriter writer;
	private final PublishJobManifest jobManifest;
	
	private boolean ready = false;
	
	private Logger logger = LoggerFactory.getLogger(CmsExportItemPublishManifest.class);
	
	public CmsExportItemPublishManifest(ObjectWriter writer, PublishJobManifest jobManifest) {
		
		this.writer = writer.forType(PublishJobManifest.class);
		this.jobManifest = jobManifest.forPublish();
	}

	@Override
	public void prepare() {
		
        if (ready) {
            throw new IllegalStateException("Export item:" + "Publish manifest" + " is already prepared");
        }
		// TODO Can we add jobid here?
		this.ready = true;
	}

    @Override
    public Boolean isReady() {
        return this.ready ;
    }

	@Override
	public void getResultStream(OutputStream stream) {
		
        if (!ready) {
            throw new IllegalStateException("Export item is not ready for export");
        }
		
        // TODO: Potentially support Velocity template for custom manifest formats.
		try {
			this.writer.writeValue(stream, this.jobManifest);
		} catch (IOException e) {
			logger.error("Failed to write JSON manifest: " + e.getMessage());
			throw new RuntimeException("Failed to write JSON manifest: " + e.getMessage(), e); 
		}
	}

	@Override
	public CmsExportPath getResultPath() {
		
		throw new IllegalArgumentException("Publish manifest should not be placed inside an archive.");
	}

}
