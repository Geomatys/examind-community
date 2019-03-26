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

import com.fasterxml.jackson.core.JsonEncoding;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.feature.xml.XmlFeatureWriter;
import org.geotoolkit.feature.xml.jaxp.JAXPStreamFeatureWriter;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.data.FeatureIterator;
import org.geotoolkit.data.FeatureStreams;
import org.geotoolkit.data.FeatureStoreRuntimeException;
import org.geotoolkit.data.FeatureStoreUtilities;
import org.geotoolkit.data.FeatureWriter;
import org.geotoolkit.data.geojson.GeoJSONStreamWriter;
import org.geotoolkit.nio.IOUtilities;
import org.opengis.feature.Feature;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class FeatureCollectionWriter implements HttpMessageConverter<FeatureCollectionWrapper> {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.wfs.ws.rs");

    @Override
    public boolean canRead(Class<?> clazz, org.springframework.http.MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, org.springframework.http.MediaType mediaType) {
        return FeatureCollectionWrapper.class.isAssignableFrom(clazz);
    }

    @Override
    public List<org.springframework.http.MediaType> getSupportedMediaTypes() {
        return Arrays.asList(MediaType.APPLICATION_JSON); // GML_3_1_1_MIME  and GML_3_2_1_MIME are not supported by Spring
    }
    
    @Override
    public FeatureCollectionWrapper read(Class<? extends FeatureCollectionWrapper> type, HttpInputMessage him) throws IOException, HttpMessageNotReadableException {
        throw new HttpMessageNotReadableException("FeatureCollectionWrapper message converter do not support reading.");
    }

    @Override
    public void write(FeatureCollectionWrapper t, org.springframework.http.MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        
   
        MediaType media = null;
        try {
            media = outputMessage.getHeaders().getContentType();
        } catch (InvalidMediaTypeException ex) {
            // we let pass for GML mme type not supported by Spring
        }
        
        if (MediaType.APPLICATION_JSON.equals(media)) {
            try (FeatureWriter featureWriter = new GeoJSONStreamWriter(outputMessage.getBody(), t.getFeatureCollection().getType(), JsonEncoding.UTF8, 4, true)) {
                FeatureStoreUtilities.write(featureWriter, t.getFeatureCollection());
            } catch (DataStoreException ex) {
                LOGGER.log(Level.SEVERE, "DataStore exception while writing the feature collection", ex);
            } catch (FeatureStoreRuntimeException ex) {
                LOGGER.log(Level.SEVERE, "DataStoreRuntimeException exception while writing the feature collection", ex);
            }
        
        } else {
            try {
                final XmlFeatureWriter featureWriter = new JAXPStreamFeatureWriter(t.getGmlVersion(), t.getWfsVersion(), t.getSchemaLocations());
                if (t.isWriteSingleFeature()) {
                    //write a single feature without collection element container
                    final FeatureIterator ite = t.getFeatureCollection().iterator();
                    try {
                        if (ite.hasNext()) {
                            Feature f = ite.next();
                            featureWriter.write(f, outputMessage.getBody(), t.getNbMatched());
                        } else {
                            //write an empty collection
                            featureWriter.write(FeatureStreams.emptyCollection(t.getFeatureCollection()), outputMessage.getBody(), t.getNbMatched());
                        }
                    } finally {
                        ite.close();
                    }
                } else {
                    featureWriter.write(t.getFeatureCollection(), outputMessage.getBody(), t.getNbMatched());
                }
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Exception while writing the feature collection", ex);
            }
        }
    }

}
