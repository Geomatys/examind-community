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
package org.constellation.store.observation.db;

import org.constellation.store.observation.db.model.OMSQLDialect;
import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.sis.storage.DataStoreException;
import org.constellation.util.SQLResult;
import org.geotoolkit.geometry.jts.JTS;
import static org.geotoolkit.observation.OMUtils.geometryMatchEnvelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKTReader;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class SensorLocationProcessor {

    protected static final Logger LOGGER = Logger.getLogger("org.constellation.store.observation.db");
    
    protected final Envelope envelopeFilter;
    protected final OMSQLDialect dialect;

    public SensorLocationProcessor(Envelope envelopeFilter, OMSQLDialect dialect) {
        this.envelopeFilter = envelopeFilter;
        this.dialect = dialect;
    }

    public Map<String, Map<Date, Geometry>> processLocations(SQLResult rs) throws SQLException, DataStoreException {
        Map<String, Map<Date, Geometry>> locations = new LinkedHashMap<>();
        try {
            while (rs.next()) {
            
                final String procedure = rs.getString("procedure");
                final Date time = new Date(rs.getTimestamp("time").getTime());
                org.locationtech.jts.geom.Geometry geom = readGeom(rs, 3);
                
                if (geom == null) continue;

                final int srid = rs.getInt(4);
                final CoordinateReferenceSystem currentCRS = OM2Utils.parsePostgisCRS(srid);
                JTS.setCRS(geom, currentCRS);
                
                // exclude from spatial filter (will be removed when postgis filter will be set in request)
                if (envelopeFilter != null && !geometryMatchEnvelope(geom, envelopeFilter)) {
                    continue;
                }

                final Map<Date, Geometry> procedureLocations;
                if (locations.containsKey(procedure)) {
                    procedureLocations = locations.get(procedure);
                } else {
                    procedureLocations = new LinkedHashMap<>();
                    locations.put(procedure, procedureLocations);

                }
                procedureLocations.put(time, geom);
            }
         } catch (FactoryException | ParseException ex) {
            throw new DataStoreException(ex);
        }
        return locations;
    }

    protected org.locationtech.jts.geom.Geometry readGeom(SQLResult rs, int index) throws SQLException, ParseException {
        org.locationtech.jts.geom.Geometry geom = null;
        // duck db driver does not support a lot of bytes handling. TODO, look for future driver improvement
        if (dialect.equals(OMSQLDialect.DUCKDB)) {
           String s = rs.getString(index);
            if (s != null) {
                WKTReader reader = new WKTReader();
                geom = reader.read(s);
            }
        } else {
            final byte[] b = rs.getBytes(index);
            if (b != null) {
                WKBReader reader = new WKBReader();
                geom = reader.read(b);
            }
        }
        return geom;
    }
}
