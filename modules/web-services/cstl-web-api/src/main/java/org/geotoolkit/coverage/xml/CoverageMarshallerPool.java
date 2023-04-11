
package org.geotoolkit.coverage.xml;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.apache.sis.xml.MarshallerPool;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class CoverageMarshallerPool {

    private static final MarshallerPool INSTANCE;

    static {
        try {
            INSTANCE = new MarshallerPool(JAXBContext.newInstance("org.geotoolkit.coverage.xml"), null);
        } catch (JAXBException ex) {
            throw new AssertionError(ex); // Should never happen, unless we have a build configuration problem.
        }
    }

    private CoverageMarshallerPool() {
    }

    public static MarshallerPool getInstance() {
        return INSTANCE;
    }
}
