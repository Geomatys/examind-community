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
import static org.constellation.store.observation.db.OM2BaseReader.LOGGER;
import static org.constellation.store.observation.db.OM2BaseReader.defaultCRS;
import org.geotoolkit.gml.JTStoGeometry;
import org.geotoolkit.gml.xml.AbstractGeometry;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.opengis.geometry.Geometry;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

/**
 *
 * @author guilhem
 */
public class SensorLocationDecimator extends AbstractSensorLocationDecimator {

    public SensorLocationDecimator(GeneralEnvelope envelopeFilter, String gmlVersion, int width, final Map<Object, long[]> times) {
        super(envelopeFilter, gmlVersion, width, times);
    }

    @Override
    public Map<String, Map<Date, Geometry>> processLocations(ResultSet rs) throws SQLException, DataStoreException {

        final Envelope[][] geoCells = new Envelope[nbCell][nbCell];

        // prepare geometrie cells
        final double envMinx = envelopeFilter.getMinimum(0);
        final double envMiny = envelopeFilter.getMinimum(1);
        final double xStep = envelopeFilter.getSpan(0) / nbCell;
        final double yStep = envelopeFilter.getSpan(1) / nbCell;
        for (int i = 0; i < nbCell; i++) {
            for (int j = 0; j < nbCell; j++) {
                double minx = envMinx + i*xStep;
                double maxx = minx + xStep;
                double miny = envMiny + j*yStep;
                double maxy = miny + yStep;
                geoCells[i][j] = new Envelope(minx, maxx, miny, maxy);
            }
        }

        // prepare a first grid reducing the gris size by 10
        // in order to reduce the cell by cell intersect
        // and perform a pre-search
        // TODO, use multiple level like in a R-Tree
        List<NarrowEnvelope> nEnvs = new ArrayList<>();
        int reduce = 10;
        int tmpNbCell = nbCell/reduce;
        final double fLvlXStep = envelopeFilter.getSpan(0) / tmpNbCell;
        final double flvlyStep = envelopeFilter.getSpan(1) / tmpNbCell;
        for (int i = 0; i < tmpNbCell; i++) {
            for (int j = 0; j < tmpNbCell; j++) {
                double minx = envMinx + i*fLvlXStep;
                double maxx = minx + fLvlXStep;
                double miny = envMiny + j*flvlyStep;
                double maxy = miny + flvlyStep;
                int i_min, i_max, j_min, j_max;
                i_min = i * (nbCell / tmpNbCell);
                i_max = (i+1) * (nbCell / tmpNbCell);
                j_min = j * (nbCell / tmpNbCell);
                j_max = (j+1) * (nbCell / tmpNbCell);
                nEnvs.add(new NarrowEnvelope(new Envelope(minx, maxx, miny, maxy), i_min, i_max, j_min, j_max));
            }
        }

        Map<String, Map<TripleKey, List>> procedureCells = new HashMap<>();
        Map<TripleKey, List> curentCells = null;
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
                    curentCells = new HashMap<>();
                    procedureCells.put(procedure, curentCells);
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
                    if (!(geom instanceof Point)) {
                        LOGGER.warning("Geometry is not a point. excluded from decimation");
                        continue;
                    }
                } else {
                    continue;
                }

                Coordinate coord = ((Point)geom).getCoordinate();
                // find the correct cell where to put the geometry

                // first round to find the region whe the data is located
                int i_min, i_max, j_min, j_max;
                if (nEnvs.isEmpty()) {
                    i_min = 0; i_max = nbCell; j_min = 0; j_max = nbCell;
                } else {
                    i_min = -1; i_max = -1; j_min = -1; j_max = -1;
                    for (NarrowEnvelope nEnv : nEnvs) {
                        if (nEnv.env.intersects(coord)) {
                            i_min = nEnv.i_min;
                            j_min = nEnv.j_min;
                            i_max = nEnv.i_max;
                            j_max = nEnv.j_max;
                            break;
                        }
                    }

                    if (i_min == -1 || j_min == -1 || i_max == -1 ||j_max == -1) {
                        // this should not happen any longer when a correct postgis filter will be perfomed on the SQL query
                        continue;
                    }
                }

                 // ajust the time index
                final long time = rs.getTimestamp("time").getTime();
                while (time > (start + step) && tIndex != nbCell - 1) {
                    start = start + step;
                    tIndex++;
                }

                // search cell by cell
                boolean cellFound = false;
                csearch:for (int i = i_min; i < i_max; i++) {
                    for (int j = j_min; j < j_max; j++) {
                        Envelope cellEnv = geoCells[i][j];
                        if (cellEnv.intersects(coord)) {
                            TripleKey key = new TripleKey(tIndex, i, j);
                            if (!curentCells.containsKey(key)) {
                                List geoms = new ArrayList<>();
                                geoms.add(geom);
                                curentCells.put(key, geoms);
                            } else {
                                curentCells.get(key).add(geom);
                            }
                            cellFound = true;
                            break csearch;
                        }
                    }
                }


                /*debug
                if (!cellFound) {
                    LOGGER.info("No cell found for: " + geom);
                    for (int i = 0; i < nbCell; i++) {
                        for (int j = 0; j < nbCell; j++) {
                            Envelope cellEnv = geoCells[i][j];
                            LOGGER.info(cellEnv.toString());
                        }
                    }

                }*/



            } catch (FactoryException | ParseException ex) {
                throw new DataStoreException(ex);
            }
        }
        
        Map<String, Map<Date, Geometry>> locations = new LinkedHashMap<>();
        // merge the geometries in each cells
        for (Map.Entry<String, Map<TripleKey, List>> entry : procedureCells.entrySet()) {

            String procedure = entry.getKey();
            Map<TripleKey, List> cells = entry.getValue();
            step = times.get(procedure)[1];
            start = times.get(procedure)[0];
            for (int t = 0; t < nbCell; t++) {
                boolean tfound = false;
                final Date time = new Date(start + (step*t) + step/2);
                for (int i = 0; i < nbCell; i++) {
                    for (int j = 0; j < nbCell; j++) {

                        TripleKey key = new TripleKey(t, i, j);
                        org.locationtech.jts.geom.Geometry geom;
                        if (!cells.containsKey(key)) {
                            continue;
                        }
                        List<org.locationtech.jts.geom.Geometry> cellgeoms = cells.get(key);
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

                            if (gmlGeom instanceof Geometry g) {
                                procedureLocations.put(time, g);
                                tfound = true;
                            } else {
                                throw new DataStoreException("GML geometry cannot be casted as an Opengis one");
                            }
                        } catch (FactoryException ex) {
                            throw new DataStoreException(ex);
                        }
                    }
                }
                /*if (!tfound) {
                    LOGGER.finer("no date found for index:" + t + "\n "
                              + "min   : " +  format2.format(new Date(start + (step*t))) + "\n "
                              + "medium: " +  format2.format(new Date(start + (step*t) + step/2)) + "\n "
                              + "max   : " +  format2.format(new Date(start + (step*t) + step)) );
                }*/
            }
        }
        return locations;
    }

    private class NarrowEnvelope {
        public final Envelope env;
        public final int i_min, i_max, j_min, j_max;

        public NarrowEnvelope(Envelope env, int i_min, int i_max, int j_min, int j_max) {
            this.env = env;
            this.i_max = i_max;
            this.i_min = i_min;
            this.j_max = j_max;
            this.j_min = j_min;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("Env[ ").append(env.getMinX()).append(", ").append(env.getMaxX()).append(", ").append(env.getMinY()).append(", ").append(env.getMaxY()).append("]\n");
            sb.append("Bound[").append(i_min).append( ", ").append(i_max).append(", ").append(j_min ).append(", ").append(j_max).append(']');
            return sb.toString();
        }
    }

    private class TripleKey {
        private final int t, i, j;

        public TripleKey(int t, int i, int j) {
            this.i = i;
            this.j = j;
            this.t = t;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (this.getClass() == obj.getClass()) {
                TripleKey that = (TripleKey) obj;
                return this.i == that.i &&
                       this.j == that.j &&
                       this.t == that.t;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return t + 1009 * j + 1000003 * i;
        }
    }
}
