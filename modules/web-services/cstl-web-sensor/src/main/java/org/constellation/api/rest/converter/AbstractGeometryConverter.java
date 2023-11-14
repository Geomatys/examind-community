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
import java.util.Arrays;
import java.util.List;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.geotoolkit.gml.xml.AbstractGeometry;
import org.geotoolkit.gml.xml.GMLMarshallerPool;
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
public class AbstractGeometryConverter implements HttpMessageConverter<AbstractGeometry> {

    @Override
    public boolean canRead(Class<?> type, MediaType mt) {
        return AbstractGeometry.class.isAssignableFrom(type);
    }

    @Override
    public boolean canWrite(Class<?> type, MediaType mt) {
        return false;
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(MediaType.APPLICATION_XML, MediaType.TEXT_XML);
    }

    @Override
    public AbstractGeometry read(Class<? extends AbstractGeometry> type, HttpInputMessage him) throws IOException, HttpMessageNotReadableException {
                /*
                TODO JSON

                Map<String, String> nSMap = new HashMap<String, String>(0);
                nSMap.put("http://www.constellation.org/config", "constellation-config");
                JettisonConfig config = JettisonConfig.mappedJettison().xml2JsonNs(nSMap).build();
                JettisonJaxbContext cxtx = new JettisonJaxbContext(config, "org.constellation.dto:" +
                        "org.constellation.generic.database:" +
                        "org.geotoolkit.ogc.xml.v110:" +
                        "org.apache.sis.xml.bind.metadata.geometry:" +
                        "org.geotoolkit.gml.xml.v311");
                JettisonUnmarshaller jsonUnmarshaller = cxtx.createJsonUnmarshaller();
                context = jsonUnmarshaller.unmarshalFromJSON(entityStream, LayerContext.class);*/

        try {
            final Unmarshaller m = GMLMarshallerPool.getInstance().acquireUnmarshaller();
            Object obj = m.unmarshal(him.getBody());
            if (obj instanceof JAXBElement) {
                obj = ((JAXBElement)obj).getValue();
            }
            AbstractGeometry context = (AbstractGeometry) obj;
            GMLMarshallerPool.getInstance().recycle(m);
            return context;
        } catch (JAXBException ex) {
            throw new IOException(ex);
        }
            
    }

    @Override
    public void write(AbstractGeometry t, MediaType mt, HttpOutputMessage hom) throws IOException, HttpMessageNotWritableException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
