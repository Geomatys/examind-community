/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2021 Geomatys.
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

package com.examind.process.sos.csvcoriolis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.geotoolkit.sos.netcdf.GeoSpatialBound;
import org.geotoolkit.sos.xml.SOSXmlFactory;
import org.opengis.geometry.DirectPosition;
import org.opengis.temporal.TemporalGeometricPrimitive;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ObservationBlock {

    public String procedureId;

    public String featureID;

    private Positions positions;

    public CoriolisMeasureBuilder cmb;
    
    public GeoSpatialBound currentSpaBound;

    public ObservationBlock(String procedureId, String featureID, CoriolisMeasureBuilder cmb) {
        this.procedureId = procedureId;
        this.featureID = featureID;
        this.cmb = cmb;
        this.currentSpaBound = new GeoSpatialBound();
        this.positions = new Positions();
    }

    public void addPosition(Long millis, double latitude, double longitude) {
        this.positions.addPosition(millis, latitude, longitude);
        currentSpaBound.addXYCoordinate(longitude, latitude);
    }

    public void addDate(final long millis) {
        currentSpaBound.addDate(millis);
    }

    public List<DirectPosition> getPositions() {
        return positions.positions;
    }

    public TemporalGeometricPrimitive getTimeObject() {
        return currentSpaBound.getTimeObject("2.0.0");
    }

    public Set<Map.Entry<Long, List<DirectPosition>>> getHistoricalPositions() {
        return positions.historicalPositions.entrySet();
    }

    public static class Positions {

        public final Set<String> knownPositions = new HashSet<>();
        public final List<DirectPosition> positions = new ArrayList<>();
        public final Map<Long, List<DirectPosition>> historicalPositions = new HashMap<>();

        public void addPosition(Long millis, double latitude, double longitude) {
            final String posKey = latitude + "_" + longitude;
            if (!knownPositions.contains(posKey)) {
                knownPositions.add(posKey);
                final DirectPosition pos = SOSXmlFactory.buildDirectPosition("2.0.0", "EPSG:4326", 2, Arrays.asList(latitude, longitude));
                positions.add(pos);
                if (millis != null) {
                    if (historicalPositions.containsKey(millis)) {
                        historicalPositions.get(millis).add(pos);
                    } else {
                        List<DirectPosition> hpos = new ArrayList<>();
                        hpos.add(pos);
                        historicalPositions.put(millis, hpos);
                    }
                }
            }
        }
    }
}
