package com.examind.process.admin;

import com.examind.process.admin.renderedpyramid.PyramidProcess;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import javax.imageio.spi.ServiceRegistry;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.identification.DefaultServiceIdentification;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.processing.AbstractProcessingRegistry;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.identification.Identification;

/**
 *
 * @author Alexis Manin (Geomatys)
 */
public class AdminProcessRegistry extends AbstractProcessingRegistry {

    static final Logger LOGGER = Logging.getLogger("com.examind.process.admin");

    public static final String NAME = "administration";
    public static final DefaultServiceIdentification IDENTIFICATION;
    static {
        IDENTIFICATION = new DefaultServiceIdentification();
        final Identifier id = new DefaultIdentifier(NAME);
        final DefaultCitation citation = new DefaultCitation(NAME);
        citation.setIdentifiers(Collections.singleton(id));
        IDENTIFICATION.setCitation(citation);
    }

    public AdminProcessRegistry() {
        super(findDescriptors());
    }

    @Override
    public Identification getIdentification() {
        return IDENTIFICATION;
    }

    /**
     * Find all processDescriptor defined in META-INF/service files.
     * @return
     */
    private static synchronized ProcessDescriptor[] findDescriptors() {
        final Iterator<AdminProcessDescriptor> ite = ServiceRegistry.lookupProviders(AdminProcessDescriptor.class);
        final List<ProcessDescriptor> descriptors = new ArrayList<>();
        while (ite.hasNext()) {
            descriptors.add((ProcessDescriptor) ite.next());
        }
        return descriptors.toArray(new ProcessDescriptor[descriptors.size()]);
    }
}
