/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com
 *
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
package org.constellation.store.observation.db.decimation;

import java.util.Map;
import org.constellation.store.observation.db.model.OMSQLDialect;
import org.constellation.store.observation.db.SensorLocationProcessor;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.geometry.Envelope;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class AbstractSensorLocationDecimator extends SensorLocationProcessor {

    protected static final GeometryFactory JTS_GEOM_FACTORY = org.geotoolkit.geometry.jts.JTS.getFactory();

    protected final int nbCell;

    protected final Map<Object, long[]> times;

    public AbstractSensorLocationDecimator(Envelope envelopeFilter, int width, final Map<Object, long[]> times, OMSQLDialect dialect) {
        super(envelopeFilter, dialect);
        this.nbCell = width;
        this.times = times;
    }
}
