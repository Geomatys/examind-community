/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
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
package org.constellation.store.observation.db;

import java.util.Map;
import org.apache.sis.geometry.GeneralEnvelope;
import org.locationtech.jts.geom.GeometryFactory;

/**
 *
 * @author guilhem
 */
public abstract class AbstractSensorLocationDecimator extends SensorLocationProcessor {

    protected static final GeometryFactory JTS_GEOM_FACTORY = org.geotoolkit.geometry.jts.JTS.getFactory();

    protected final int nbCell;

    protected final Map<Object, long[]> times;

    public AbstractSensorLocationDecimator(GeneralEnvelope envelopeFilter, String gmlVersion, int width, final Map<Object, long[]> times) {
        super(envelopeFilter, gmlVersion);
        this.nbCell = width;
        this.times = times;
    }

}
