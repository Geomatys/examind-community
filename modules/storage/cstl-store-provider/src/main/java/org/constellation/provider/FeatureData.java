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

package org.constellation.provider;

import java.time.temporal.Temporal;
import java.util.Date;
import java.util.Optional;
import org.constellation.util.DimensionDef;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;

import org.apache.sis.storage.FeatureSet;

import org.constellation.exception.ConstellationStoreException;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.crs.VerticalCRS;

/**
 * Layer details for Feature sources.
 * Provide extra methods to access the underlying datastore.
 *
 * @author Johann Sorel (Geomatys)
 */
public interface FeatureData extends Data<FeatureSet> {

    FeatureType getType() throws ConstellationStoreException;

    Object[] getPropertyValues(String property) throws ConstellationStoreException;

    /**
     *
     * NOTE: for now, we force a {@link Date} as expression value type.
     * In the future, we might want to relax this constraint,
     * to support data native time representation (such as {@link Temporal java.time API}).
     *
     * @return An expression that extract time value from any given feature from this data.
     */
    default Optional<DimensionDef<TemporalCRS, Feature, Date>> getTimeDimension() { return Optional.empty(); }

    default Optional<DimensionDef<VerticalCRS, Feature, Double>> getElevationDimension() { return Optional.empty(); }
}
