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
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CRS;
import org.apache.sis.storage.DataStoreException;
import static org.constellation.store.observation.db.OM2BaseReader.defaultCRS;
import org.geotoolkit.geometry.jts.JTS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

/**
 *
 * @author guilhem
 */
public class SensorLocationProcessor {

    protected final GeneralEnvelope envelopeFilter;

    public SensorLocationProcessor(GeneralEnvelope envelopeFilter) {
        this.envelopeFilter = envelopeFilter;
    }

    public Map<String, Map<Date, Geometry>> processLocations(ResultSet rs) throws SQLException, DataStoreException {
        Polygon spaFilter = null;
        if (envelopeFilter != null) {
            spaFilter = JTS.toGeometry(envelopeFilter);
        }
        Map<String, Map<Date, Geometry>> locations = new LinkedHashMap<>();
        while (rs.next()) {
            try {
                final String procedure = rs.getString("procedure");
                final Date time = new Date(rs.getTimestamp("time").getTime());
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
                    WKBReader reader = new WKBReader();
                    geom             = reader.read(b);
                    JTS.setCRS(geom, crs);
                } else {
                    continue;
                }
                // exclude from spatial filter (will be removed when postgis filter will be set in request)
                if (spaFilter != null && !spaFilter.intersects(geom)) {
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
            } catch (FactoryException | ParseException ex) {
                throw new DataStoreException(ex);
            }
        }
        return locations;
    }
}
