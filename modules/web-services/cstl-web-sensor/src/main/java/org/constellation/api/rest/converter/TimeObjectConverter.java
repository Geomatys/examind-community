/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2017 Geomatys.
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
package org.constellation.api.rest.converter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamWriter;
import org.codehaus.jettison.mapped.Configuration;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;
import org.codehaus.jettison.mapped.MappedXMLStreamWriter;
import org.geotoolkit.gml.xml.GMLMarshallerPool;
import org.opengis.temporal.TemporalGeometricPrimitive;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class TimeObjectConverter implements HttpMessageConverter<TemporalGeometricPrimitive> {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.rest.api");

    @Override
    public boolean canRead(Class<?> type, MediaType mt) {
        return false;
    }

    @Override
    public boolean canWrite(Class<?> type, MediaType mt) {
        return TemporalGeometricPrimitive.class.isAssignableFrom(type);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML);
    }

    @Override
    public TemporalGeometricPrimitive read(Class<? extends TemporalGeometricPrimitive> type, HttpInputMessage him) throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write(TemporalGeometricPrimitive r, MediaType mt, HttpOutputMessage hom) throws IOException, HttpMessageNotWritableException {
        try {
            // if it's a json POST, create a JSonMarshaller.
            if (mt.equals(MediaType.APPLICATION_JSON)) {
                //transform xlm namespace to json namespace
                Map<String, String> nSMap = new HashMap<>(0);
                nSMap.put("http://www.opengis.net/gml/3.2", "gml32");
                nSMap.put("http://www.opengis.net/gml", "gml");

                // create json marshaller configuration and context
                final Configuration config = new Configuration(nSMap);
                MappedNamespaceConvention con = new MappedNamespaceConvention(config);
                Writer writer = new OutputStreamWriter(hom.getBody());
                XMLStreamWriter xmlStreamWriter = new MappedXMLStreamWriter(con, writer);

                // create marshaller
                JAXBContext jc = JAXBContext.newInstance("org.geotoolkit.gml.xml.v311:org.geotoolkit.gml.xml.v321");
                Marshaller marshaller = jc.createMarshaller();

                // Marshall object
                marshaller.marshal(r, xmlStreamWriter);
            } else {
                // Default : use xml marshaller
                final Marshaller m = GMLMarshallerPool.getInstance().acquireMarshaller();
                m.marshal(r, hom.getBody());
                GMLMarshallerPool.getInstance().recycle(m);
            }

        } catch (JAXBException ex) {
            LOGGER.log(Level.SEVERE, "JAXB exception while writing the layerContext", ex);
        }
    }

}
