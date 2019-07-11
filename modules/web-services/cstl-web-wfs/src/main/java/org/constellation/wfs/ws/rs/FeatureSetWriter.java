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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.data.FeatureStoreRuntimeException;
import org.geotoolkit.data.FeatureStoreUtilities;
import org.geotoolkit.data.FeatureWriter;
import org.geotoolkit.data.geojson.GeoJSONStreamWriter;
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
public class FeatureSetWriter implements HttpMessageConverter<FeatureSetWrapper> {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.wfs.ws.rs");

    @Override
    public boolean canRead(Class<?> clazz, org.springframework.http.MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, org.springframework.http.MediaType mediaType) {
        return FeatureSetWrapper.class.isAssignableFrom(clazz);
    }

    @Override
    public List<org.springframework.http.MediaType> getSupportedMediaTypes() {
        return Arrays.asList(MediaType.APPLICATION_JSON); // GML_3_1_1_MIME  and GML_3_2_1_MIME are not supported by Spring
    }

    @Override
    public FeatureSetWrapper read(Class<? extends FeatureSetWrapper> type, HttpInputMessage him) throws IOException, HttpMessageNotReadableException {
        throw new HttpMessageNotReadableException("FeatureSetWrapper message converter do not support reading.");
    }

    @Override
    public void write(FeatureSetWrapper t, org.springframework.http.MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {


        MediaType media = null;
        try {
            media = outputMessage.getHeaders().getContentType();
        } catch (InvalidMediaTypeException ex) {
            // we let pass for GML mme type not supported by Spring
        }

        if (MediaType.APPLICATION_JSON.equals(media)) {
            try (FeatureWriter featureWriter = new GeoJSONStreamWriter(outputMessage.getBody(), t.getFeatureSet().getType(), JsonEncoding.UTF8, 4, true)) {
                FeatureStoreUtilities.write(featureWriter, t.getFeatureSet().features(false).collect(Collectors.toList()));
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
                    final Optional<Feature> feat = t.getFeatureSet().features(true).findFirst();

                    if (feat.isPresent()) {
                        Feature f = feat.get();
                        featureWriter.write(f, outputMessage.getBody(), t.getNbMatched());
                    } else {
                        //write an empty collection
                        //featureWriter.write(FeatureStreams.emptyCollection(t.getFeatureCollection()), outputMessage.getBody(), t.getNbMatched());
                        featureWriter.write(t.getFeatureSet(), outputMessage.getBody(), t.getNbMatched());
                    }

                } else {
                    featureWriter.write(t.getFeatureSet(), outputMessage.getBody(), t.getNbMatched());
                }
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Exception while writing the feature collection", ex);
            }
        }
    }

}
