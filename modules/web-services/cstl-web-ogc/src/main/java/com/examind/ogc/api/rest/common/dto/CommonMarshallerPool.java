
package com.examind.ogc.api.rest.common.dto;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.apache.sis.xml.MarshallerPool;

/**
 * @author Quentin Bialota (Geomatys)
 * @author Guilhem Legal (Geomatys)
 */
public class CommonMarshallerPool {

    private static final MarshallerPool INSTANCE;

    static {
        try {
            INSTANCE = new MarshallerPool(JAXBContext.newInstance("com.examind.ogc.api.rest.common.dto"), null);
        } catch (JAXBException ex) {
            throw new AssertionError(ex); // Should never happen, unless we have a build configuration problem.
        }
    }

    private CommonMarshallerPool() {
    }

    public static MarshallerPool getInstance() {
        return INSTANCE;
    }
}
