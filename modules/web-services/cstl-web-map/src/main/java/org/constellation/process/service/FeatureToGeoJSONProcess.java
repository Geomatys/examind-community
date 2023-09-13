/*
 *     Examind Community - An open source and standard compliant SDI
 *     https://community.examind.com/
 *
 *  Copyright 2022 Geomatys.
 *
 *  Licensed under the Apache License, Version 2.0 (    the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/
package org.constellation.process.service;

import com.fasterxml.jackson.core.JsonEncoding;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.stream.Stream;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureQuery;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.util.Utilities;
import org.constellation.process.AbstractCstlProcess;
import static org.constellation.process.service.FeatureToGeoJSONDescriptor.FEATURESET;
import static org.constellation.process.service.FeatureToGeoJSONDescriptor.GEOJSON_OUTPUT;
import org.geotoolkit.feature.FeatureExt;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.geotoolkit.storage.feature.FeatureStoreRuntimeException;
import org.geotoolkit.storage.geojson.GeoJSONStreamWriter;
import org.opengis.feature.Feature;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class FeatureToGeoJSONProcess  extends AbstractCstlProcess {

    public FeatureToGeoJSONProcess(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
        super(desc, parameter);
    }

    @Override
    protected void execute() throws ProcessException {
        try {
            FeatureSet fs = inputParameters.getMandatoryValue(FEATURESET);

            /*
            * always reproject to CRS:84 if not already
            */
            final CoordinateReferenceSystem crs = FeatureExt.getCRS(fs.getType());
            if (!Utilities.equalsApproximately(crs, CommonCRS.defaultGeographic())) {
                FeatureQuery query = org.geotoolkit.storage.feature.query.Query.reproject(fs.getType(), CommonCRS.defaultGeographic());
                fs = fs.subset(query);
            }
            
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                 try (GeoJSONStreamWriter featureWriter = new GeoJSONStreamWriter(out, fs.getType(), JsonEncoding.UTF8, 4, true);
                    Stream<Feature> stream = fs.features(false)) {
                   Iterator<Feature> iterator = stream.iterator();
                   featureWriter.writeCollection(new ArrayList<>(), null, null);
                   while (iterator.hasNext()) {
                       Feature next = iterator.next();
                       Feature neww = featureWriter.next();
                       FeatureExt.copy(next, neww, false);
                       featureWriter.write();
                   }
                 }
                 outputParameters.getOrCreate(GEOJSON_OUTPUT).setValue(new String(out.toByteArray()));
            }
            
        } catch (DataStoreException | IOException | FeatureStoreRuntimeException ex) {
            throw new ProcessException(ex.getMessage(), this, ex);
        }
    }
}