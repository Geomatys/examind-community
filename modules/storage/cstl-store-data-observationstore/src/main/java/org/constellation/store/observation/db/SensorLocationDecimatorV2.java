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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CRS;
import org.apache.sis.storage.DataStoreException;
import static org.constellation.store.observation.db.OM2BaseReader.defaultCRS;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.gml.JTStoGeometry;
import org.geotoolkit.gml.xml.AbstractGeometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.opengis.geometry.Geometry;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

/**
 *
 * @author guilhem
 */
public class SensorLocationDecimatorV2 extends AbstractSensorLocationDecimator {

    public SensorLocationDecimatorV2(GeneralEnvelope envelopeFilter, String gmlVersion, int width, final Map<Object, long[]> times) {
        super(envelopeFilter, gmlVersion, width, times);
    }

    @Override
    public Map<String, Map<Date, Geometry>> processLocations(ResultSet rs) throws SQLException, DataStoreException {
        Polygon spaFilter = null;
        if (envelopeFilter != null) {
            spaFilter = JTS.toGeometry(envelopeFilter);
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

                final byte[] b = rs.getBytes(3);
                final int srid = rs.getInt(4);
                final CoordinateReferenceSystem crs;
                if (srid != 0) {
                    crs = CRS.forCode("urn:ogc:def:crs:EPSG::" + srid);
                } else {
                    crs = defaultCRS;
                }
                final org.locationtech.jts.geom.Geometry geom;
                if (b != null) {
                    geom = reader.read(b);
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

            } catch (FactoryException | ParseException ex) {
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
                org.locationtech.jts.geom.Geometry geom;
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
                    GeometryCollection coll = new GeometryCollection(cellgeoms.toArray(new org.locationtech.jts.geom.Geometry[cellgeoms.size()]), JTS_GEOM_FACTORY);
                    geom = coll.getCentroid();
                }

                try {
                    final AbstractGeometry gmlGeom = JTStoGeometry.toGML(gmlVersion, geom, defaultCRS);

                    final Map<Date, Geometry> procedureLocations;
                    if (locations.containsKey(procedure)) {
                        procedureLocations = locations.get(procedure);
                    } else {
                        procedureLocations = new LinkedHashMap<>();
                        locations.put(procedure, procedureLocations);
                    }
                    final Date time = new Date(start + (step*t) + step/2);
                    if (gmlGeom instanceof Geometry) {
                        procedureLocations.put(time, (Geometry) gmlGeom);
                    } else {
                        throw new DataStoreException("GML geometry cannot be casted as an Opengis one");
                    }
                } catch (FactoryException ex) {
                    throw new DataStoreException(ex);
                }
            }
        }
        return locations;
    }
}
