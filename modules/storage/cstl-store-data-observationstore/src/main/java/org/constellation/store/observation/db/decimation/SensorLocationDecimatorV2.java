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

import org.constellation.util.SQLResult;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.Utilities;
import org.constellation.store.observation.db.OM2Utils;
import org.constellation.store.observation.db.model.OMSQLDialect;
import org.geotoolkit.geometry.GeometricUtilities;
import org.geotoolkit.geometry.GeometricUtilities.WrapResolution;
import org.geotoolkit.geometry.jts.JTS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 *
 * @author guilhem
 */
public class SensorLocationDecimatorV2 extends AbstractSensorLocationDecimator {

    public SensorLocationDecimatorV2(GeneralEnvelope envelopeFilter, int width, final Map<Object, long[]> times, OMSQLDialect dialect) {
        super(envelopeFilter, width, times, dialect);
    }

    @Override
    public Map<String, Map<Date, Geometry>> processLocations(SQLResult rs) throws SQLException, DataStoreException {
        Geometry spaFilter = null;
        final CoordinateReferenceSystem envCRS;
        if (envelopeFilter != null) {
            envCRS = envelopeFilter.getCoordinateReferenceSystem();
            spaFilter = GeometricUtilities.toJTSGeometry(envelopeFilter, WrapResolution.NONE);
        } else {
            envCRS = CommonCRS.WGS84.normalizedGeographic();
        }
        Map<String, Map<Integer, List>> procedureCells = new HashMap<>();
        Map<Integer, List> currentGeoms = null;
        long start = -1;
        long step  = -1;
        String prevProc = null;
        int tIndex = 0; //dates are ordened
        final WKBReader reader = new WKBReader(JTS_GEOM_FACTORY);
        while (rs.next()) {
            try {
                final String procedure = rs.getString("procedure");

                if (!procedure.equals(prevProc)) {
                    step = times.get(procedure)[1];
                    start = times.get(procedure)[0];
                    currentGeoms = new HashMap<>();
                    procedureCells.put(procedure, currentGeoms);
                    tIndex = 0;
                }
                prevProc = procedure;

                org.locationtech.jts.geom.Geometry geom = readGeom(rs, 3);
                if (geom != null) {
                    final int srid = rs.getInt(4);
                    final CoordinateReferenceSystem currentCRS = OM2Utils.parsePostgisCRS(srid);
                    JTS.setCRS(geom, currentCRS);
                    // reproject geom to envelope CRS if needed
                    if (!Utilities.equalsIgnoreMetadata(currentCRS, envCRS)) {
                        try {
                            geom = org.apache.sis.internal.feature.jts.JTS.transform(geom, envCRS);
                        } catch (TransformException ex) {
                            throw new DataStoreException(ex);
                        }
                    }
                } else {
                    continue;
                }

                // exclude from spatial filter  (will be removed when postgis filter will be set in request)
                if (spaFilter != null && !spaFilter.intersects(geom)) {
                    continue;
                }

                // ajust the time index
                final long time = rs.getTimestamp("time").getTime();
                while (time > (start + step) && tIndex != nbCell - 1) {
                    start = start + step;
                    tIndex++;
                }
                if (currentGeoms.containsKey(tIndex)) {
                    currentGeoms.get(tIndex).add(geom);
                } else {
                    List<org.locationtech.jts.geom.Geometry> geoms = new ArrayList<>();
                    geoms.add(geom);
                    currentGeoms.put(tIndex, geoms);
                }

            } catch (ParseException | FactoryException ex) {
                throw new DataStoreException(ex);
            }
        }

        Map<String, Map<Date, Geometry>> locations = new LinkedHashMap<>();
        // merge the geometries in each cells
        for (Map.Entry<String, Map<Integer, List>> entry : procedureCells.entrySet()) {

            String procedure = entry.getKey();
            Map<Integer, List> cells = entry.getValue();
            step = times.get(procedure)[1];
            start = times.get(procedure)[0];
            for (int t = 0; t < nbCell; t++) {
                Geometry geom;
                if (!cells.containsKey(t)) {
                    continue;
                }
                List<org.locationtech.jts.geom.Geometry> cellgeoms = cells.get(t);
                if (cellgeoms == null || cellgeoms.isEmpty()) {
                    continue;
                } else if (cellgeoms.size() == 1) {
                    geom = cellgeoms.get(0);
                } else {
                    // merge geometries
                    GeometryCollection coll = new GeometryCollection(cellgeoms.toArray(new Geometry[cellgeoms.size()]), JTS_GEOM_FACTORY);
                    geom = coll.getCentroid();
                }

                //JTS.setCRS(geom, defaultCRS);

                final Map<Date, Geometry> procedureLocations;
                if (locations.containsKey(procedure)) {
                    procedureLocations = locations.get(procedure);
                } else {
                    procedureLocations = new LinkedHashMap<>();
                    locations.put(procedure, procedureLocations);
                }
                final Date time = new Date(start + (step*t) + step/2);
                procedureLocations.put(time, geom);
            }
        }
        return locations;
    }
}
