/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com/
 * Copyright 2022 Geomatys.
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
package org.constellation.metadata;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.sis.xml.MarshallerPool;
import org.constellation.admin.SpringHelper;
import org.constellation.business.IMetadataBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.metadata.configuration.CSWConfigurer;
import org.constellation.metadata.core.CSWworker;
import org.constellation.test.SpringContextTest;
import org.geotoolkit.xml.AnchoredMarshallerPool;
import org.junit.AfterClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Node;

import static org.junit.Assert.assertNotNull;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class AbstractCSWworkerTest extends SpringContextTest {

    @Autowired
    protected IServiceBusiness serviceBusiness;

    @Autowired
    protected IProviderBusiness providerBusiness;

    @Autowired
    protected IMetadataBusiness metadataBusiness;

    protected static CSWworker worker;

    protected static MarshallerPool pool;

    protected static final Logger LOGGER = Logger.getLogger("org.constellation.metadata");

    protected boolean typeCheckUpdate = true;

    protected static boolean onlyIso = false;

    protected static void fillPoolAnchor(AnchoredMarshallerPool pool) {
        try {
            pool.addAnchor("Common Data Index record", new URI("SDN:L231:3:CDI"));
            pool.addAnchor("France", new URI("SDN:C320:2:FR"));
            pool.addAnchor("EPSG:4326", new URI("SDN:L101:2:4326"));
            pool.addAnchor("2", new URI("SDN:C371:1:2"));
            pool.addAnchor("35", new URI("SDN:C371:1:35"));
            pool.addAnchor("Transmittance and attenuance of the water column", new URI("SDN:P021:35:ATTN"));
            pool.addAnchor("Electrical conductivity of the water column", new URI("SDN:P021:35:CNDC"));
            pool.addAnchor("Dissolved oxygen parameters in the water column", new URI("SDN:P021:35:DOXY"));
            pool.addAnchor("Light extinction and diffusion coefficients", new URI("SDN:P021:35:EXCO"));
            pool.addAnchor("Dissolved noble gas concentration parameters in the water column", new URI("SDN:P021:35:HEXC"));
            pool.addAnchor("Optical backscatter", new URI("SDN:P021:35:OPBS"));
            pool.addAnchor("Salinity of the water column", new URI("SDN:P021:35:PSAL"));
            pool.addAnchor("Dissolved concentration parameters for 'other' gases in the water column", new URI("SDN:P021:35:SCOX"));
            pool.addAnchor("Temperature of the water column", new URI("SDN:P021:35:TEMP"));
            pool.addAnchor("Visible waveband radiance and irradiance measurements in the atmosphere", new URI("SDN:P021:35:VSRA"));
            pool.addAnchor("Visible waveband radiance and irradiance measurements in the water column", new URI("SDN:P021:35:VSRW"));
            pool.addAnchor("MEDATLAS ASCII", new URI("SDN:L241:1:MEDATLAS"));
        } catch (URISyntaxException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        } catch (IllegalStateException ex) {
            // this exception happen when we try to put 2 twice the same anchor.
            // for this test we call many times this method in a static instance (MarshallerPool)
            // so for now we do bnothing here
            // TODO find a way to call this only one time in the CSW test
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        try {
            if (worker != null) {
                worker.destroy();
            }
            CSWConfigurer configurer = SpringHelper.getBean(CSWConfigurer.class).orElse(null);
            configurer.removeIndex("default");
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
        }
        try {
            final IServiceBusiness service = SpringHelper.getBean(IServiceBusiness.class).orElse(null);
            if (service != null) {
                service.deleteAll();
            }
            final IProviderBusiness provider = SpringHelper.getBean(IProviderBusiness.class).orElse(null);
            if (provider != null) {
                provider.removeAll();
            }
            final IMetadataBusiness mdService = SpringHelper.getBean(IMetadataBusiness.class).orElse(null);
            if (mdService != null) {
                mdService.deleteAllMetadata();
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    /**
     * Convert an XML node to a Jaxb mapped value, by writing the node to a String (as XML)
     * and reading it back using Jaxb.
     *
     * @param xmlNode The node to decode.
     * @param jaxbValueType The wanted output data type
     * @param unmarshaller Unmarshaller to use when parsing XML
     * @return Decoded object, never null.
     * @throws RuntimeException If conversion fails
     */
    public static <T> T decode(Node xmlNode, Class<T> jaxbValueType, Unmarshaller unmarshaller) {
        var writer = new StringWriter();
        try {
            TransformerFactory.newInstance()
                              .newTransformer()
                              .transform(new DOMSource(xmlNode), new StreamResult(writer));
        } catch (TransformerException e) {
            throw new RuntimeException("Cannot serialize XML node", e);
        }

        try {
            var decoded = unmarshaller.unmarshal(new StringReader(writer.toString()));
            if (decoded instanceof JAXBElement<?> je) decoded = je.getValue();
            assertNotNull(decoded);

            if (jaxbValueType.isInstance(decoded)) return (T) decoded;
            else throw new RuntimeException("XML Node has been decoded to type "+decoded.getClass()+ " instead of "+jaxbValueType);
        } catch (JAXBException e) {
            throw new RuntimeException("Cannot read back XML node", e);
        }

    }
}
