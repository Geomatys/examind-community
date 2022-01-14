/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.constellation.ws.rs;

import org.geotoolkit.sld.MutableStyledLayerDescriptor;
import org.geotoolkit.sld.xml.Specification;
import org.geotoolkit.sld.xml.StyleXmlIO;
import org.opengis.util.FactoryException;

import javax.xml.bind.JAXBException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Guilhem Legal (Geomatys).
 * @author Fabien Bernard (Geomatys).
 */
public class MapUtilities {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.ws.rs");

    public static MutableStyledLayerDescriptor toSLD(final String sldBody, final String sldURL,
                                                     final Specification.StyledLayerDescriptor version) throws MalformedURLException {
        final Object src;

        if (sldBody != null && !sldBody.trim().isEmpty()) {
            src = new StringReader(sldBody);
        } else if (sldURL != null && !sldURL.trim().isEmpty()) {
            src = new URL(sldURL);
        } else {
            return null;
        }

        final StyleXmlIO styleIO = new StyleXmlIO();
        try {
            return styleIO.readSLD(src, version);
        } catch (JAXBException ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
        } catch (FactoryException ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
        }

        return null;
    }
}
