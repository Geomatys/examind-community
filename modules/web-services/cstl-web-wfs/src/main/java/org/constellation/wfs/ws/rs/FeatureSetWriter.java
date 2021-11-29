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
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.feature.FeatureExt;
import org.geotoolkit.feature.xml.jaxp.JAXPStreamFeatureWriter;
import org.geotoolkit.storage.feature.FeatureStoreRuntimeException;
import org.geotoolkit.storage.geojson.GeoJSONStreamWriter;
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
        throw new HttpMessageNotReadableException("FeatureSetWrapper message converter do not support reading.", him);
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
            if (t.getFeatureSet().size() > 1) {
                throw new UnsupportedOperationException("TODO MULTIPLE JSON");
            }
            try (GeoJSONStreamWriter featureWriter = new GeoJSONStreamWriter(outputMessage.getBody(), t.getFeatureSet().get(0).getType(), JsonEncoding.UTF8, 4, true);
                 Stream<Feature> stream = t.getFeatureSet().get(0).features(false)) {
                Iterator<Feature> iterator = stream.iterator();
                while (iterator.hasNext()) {
                    Feature next = iterator.next();
                    Feature neww = featureWriter.next();
                    FeatureExt.copy(next, neww, false);
                    featureWriter.write();
                }
            } catch (DataStoreException ex) {
                LOGGER.log(Level.SEVERE, "DataStore exception while writing the feature collection", ex);
            } catch (FeatureStoreRuntimeException ex) {
                LOGGER.log(Level.SEVERE, "DataStoreRuntimeException exception while writing the feature collection", ex);
            }

        } else {
            try {
                final JAXPStreamFeatureWriter featureWriter = new JAXPStreamFeatureWriter(t.getGmlVersion(), t.getWfsVersion(), t.getSchemaLocations());
                //write possible namespaces first, even if they are not used
                //this avoids jaxb to add them millions of times
                if (!"3.1.1".equals(t.getGmlVersion())) featureWriter.getCommonPrefixes().put("gml30", "http://www.opengis.net/gml");
                featureWriter.getCommonPrefixes().put("gmd", "http://www.isotc211.org/2005/gmd");
                featureWriter.getCommonPrefixes().put("gmx", "http://www.isotc211.org/2005/gmx");
                featureWriter.getCommonPrefixes().put("srv1", "http://www.isotc211.org/2005/srv");
                featureWriter.getCommonPrefixes().put("gco", "http://www.isotc211.org/2005/gco");

                if (t.isWriteSingleFeature()) {
                    //write a single feature without collection element container
                    final Optional<Feature> feat;
                    if (!t.getFeatureSet().isEmpty()) {
                        feat = t.getFeatureSet().get(0).features(true).findFirst();
                    } else {
                        feat = Optional.empty();
                    }

                    if (feat.isPresent()) {
                        Feature f = feat.get();
                        featureWriter.write(f, outputMessage.getBody(), t.getNbMatched());
                    } else {
                        //write an empty collection
                        featureWriter.write(t.getFeatureSet(), outputMessage.getBody(), t.getNbMatched());
                    }

                } else {
                    if (t.getFeatureSet().size() == 1) {
                        featureWriter.write(t.getFeatureSet().get(0), outputMessage.getBody(), t.getNbMatched());
                    } else {
                        featureWriter.write(t.getFeatureSet(), outputMessage.getBody(), t.getNbMatched());
                    }
                }
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Exception while writing the feature collection", ex);
            }
        }
    }

}
