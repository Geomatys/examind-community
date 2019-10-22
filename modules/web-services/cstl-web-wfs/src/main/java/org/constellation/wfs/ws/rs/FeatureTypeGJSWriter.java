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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.storage.feature.FeatureStoreRuntimeException;
import org.geotoolkit.data.geojson.utils.FeatureTypeUtils;
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
public class FeatureTypeGJSWriter implements HttpMessageConverter<FeatureTypeList> {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.wfs.ws.rs");

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return FeatureTypeList.class.isAssignableFrom(clazz);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(MediaType.APPLICATION_JSON); // GML_3_1_1_MIME  and GML_3_2_1_MIME are not supported by Spring
    }

    @Override
    public FeatureTypeList read(Class<? extends FeatureTypeList> type, HttpInputMessage him) throws IOException, HttpMessageNotReadableException {
        throw new HttpMessageNotReadableException("FeatureTypeList message converter do not support reading.");
    }

    @Override
    public void write(FeatureTypeList t, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        try {
             FeatureTypeUtils.writeFeatureTypes(t.types, outputMessage.getBody());
        } catch (DataStoreException ex) {
            LOGGER.log(Level.SEVERE, "DataStore exception while writing the feature collection", ex);
        } catch (FeatureStoreRuntimeException ex) {
            LOGGER.log(Level.SEVERE, "DataStoreRuntimeException exception while writing the feature collection", ex);
        }
    }

}

