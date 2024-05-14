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

package com.examind.store.observation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.geotoolkit.observation.model.GeoSpatialBound;
import org.geotoolkit.observation.model.Procedure;
import org.geotoolkit.observation.model.ResultMode;
import org.geotoolkit.observation.result.ResultBuilder;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.temporal.TemporalPrimitive;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ObservationBlock {

    public final Procedure procedure;

    public String featureID;

    public final String observationType;

    private final Positions positions;

    public final MeasureBuilder cmb;

    public final GeoSpatialBound currentSpaBound;

    public ObservationBlock(Procedure procedure, String featureID, MeasureBuilder cmb, String observationType) {
        this.procedure = procedure;
        this.featureID = featureID;
        this.cmb = cmb;
        this.currentSpaBound = new GeoSpatialBound();
        this.positions = new Positions();
        this.observationType = observationType;
    }

    public void addPosition(Long millis, double latitude, double longitude) {
        this.positions.addPosition(millis, latitude, longitude);
        currentSpaBound.addXYCoordinate(longitude, latitude);
    }

    public void addDate(final long millis) {
        currentSpaBound.addDate(millis);
    }

    public List<Coordinate> getPositions() {
        return positions.positions;
    }

    public TemporalPrimitive getTimeObject() {
        return currentSpaBound.getTimeObject();
    }

    public Set<Map.Entry<Long, List<Coordinate>>> getHistoricalPositions() {
        return positions.historicalPositions.entrySet();
    }

    public Set<MeasureField> getUsedFields() {
        return cmb.getUsedMeasureColumns();
    }

    public ResultBuilder getResults(ResultMode resultMode) {
        return cmb.buildMeasureStringBuilderFromMap(resultMode);
    }

    public int getResultsCount() {
        return cmb.getMeasureCount();
    }

    public void appendValue(Number mainValue, String measureCode, Object measureValue, int lineNumber, String[] qualityValues) {
        cmb.appendValue(mainValue, measureCode, measureValue, lineNumber, qualityValues);
    }

    public void updateObservedProperties(List<ObservedProperty> observedProperties) {
        for (ObservedProperty op : observedProperties) {
            cmb.updateObservedProperty(op);
        }
    }

    public void updateObservedProperty(ObservedProperty observedProperty) {
        cmb.updateObservedProperty(observedProperty);
    }

    public static class Positions {

        public final Set<String> knownPositions = new HashSet<>();
        public final List<Coordinate> positions = new ArrayList<>();
        public final Map<Long, List<Coordinate>> historicalPositions = new HashMap<>();

        public void addPosition(Long millis, double latitude, double longitude) {
            final String posKey = latitude + "_" + longitude;
            if (!knownPositions.contains(posKey)) {
                knownPositions.add(posKey);
                final Coordinate pos = new Coordinate(longitude, latitude);
                positions.add(pos);
                if (millis != null) {
                    if (historicalPositions.containsKey(millis)) {
                        historicalPositions.get(millis).add(pos);
                    } else {
                        List<Coordinate> hpos = new ArrayList<>();
                        hpos.add(pos);
                        historicalPositions.put(millis, hpos);
                    }
                }
            }
        }
    }
}
