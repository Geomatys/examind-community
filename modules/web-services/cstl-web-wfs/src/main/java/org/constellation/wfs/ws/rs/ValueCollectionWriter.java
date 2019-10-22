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

package org.constellation.wfs.ws.rs;

import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.storage.feature.FeatureStoreRuntimeException;
import org.geotoolkit.feature.xml.XmlFeatureWriter;
import org.geotoolkit.feature.xml.jaxp.JAXPStreamValueCollectionWriter;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
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
public class ValueCollectionWriter implements HttpMessageConverter<ValueCollectionWrapper>  {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.wfs.ws.rs");

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return ValueCollectionWrapper.class.isAssignableFrom(clazz);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(MediaType.APPLICATION_XML, MediaType.TEXT_XML);
    }

    @Override
    public ValueCollectionWrapper read(Class<? extends ValueCollectionWrapper> type, HttpInputMessage him) throws IOException, HttpMessageNotReadableException {
        throw new HttpMessageNotReadableException("ValueCollectionWrapper message converter do not support reading.");
    }
    
    @Override
    public void write(ValueCollectionWrapper t, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        try {
            final XmlFeatureWriter featureWriter = new JAXPStreamValueCollectionWriter(t.getValueReference());
            featureWriter.write(t.getFeatureSet(), outputMessage.getBody());
        } catch (XMLStreamException ex) {
            LOGGER.log(Level.SEVERE, "Stax exception while writing the feature collection", ex);
        } catch (DataStoreException ex) {
            LOGGER.log(Level.SEVERE, "DataStore exception while writing the feature collection", ex);
        } catch (FeatureStoreRuntimeException ex) {
            LOGGER.log(Level.SEVERE, "DataStoreRuntimeException exception while writing the feature collection", ex);
        }
    }

}
