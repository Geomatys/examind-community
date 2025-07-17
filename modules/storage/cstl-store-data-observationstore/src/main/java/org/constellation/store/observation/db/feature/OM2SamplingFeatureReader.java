/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com/
 *
 * Copyright 2023 Geomatys.
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
package org.constellation.store.observation.db.feature;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.feature.builder.AttributeTypeBuilder;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.privy.AttributeConvention;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.Utilities;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.store.observation.db.OM2Utils;
import org.constellation.util.OMSQLDialect;
import static org.constellation.util.OMSQLDialect.DUCKDB;
import static org.constellation.util.OMSQLDialect.POSTGRES;
import org.constellation.util.Util;
import org.geotoolkit.feature.FeatureExt;
import org.geotoolkit.storage.feature.FeatureStoreRuntimeException;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.observation.feature.OMFeatureTypes;
import org.geotoolkit.util.collection.CloseableIterator;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class OM2SamplingFeatureReader implements CloseableIterator<Feature> {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.store.observation.db");
    private final Connection cnx;
    private boolean firstCRS = true;
    private FeatureType type;
    private final ResultSet result;

    private Feature candidate = null;
    private Feature current   = null;
    private CoordinateReferenceSystem crs;

    protected final String schemaPrefix;

    protected final OMSQLDialect dialect;

    @SuppressWarnings("squid:S2095")
    public OM2SamplingFeatureReader(Connection cnx, OMSQLDialect dialect, final FeatureType type, final String schemaPrefix) throws SQLException, DataStoreException {
        this.type = type;
        this.cnx = cnx;
        if (Util.containsForbiddenCharacter(schemaPrefix)) {
            throw new DataStoreException("Invalid schema prefix value");
        }
        this.schemaPrefix = schemaPrefix;
        this.dialect = dialect;
        final PreparedStatement stmtAll = switch(dialect) {
            case POSTGRES -> cnx.prepareStatement("SELECT \"id\", \"name\", \"description\", \"sampledfeature\", st_asBinary(\"shape\") as \"shape\", \"crs\" FROM \"" + schemaPrefix + "om\".\"sampling_features\"");//NOSONAR
            case DUCKDB   -> cnx.prepareStatement("SELECT  \"id\", \"name\", \"description\", \"sampledfeature\", ST_AsText(\"shape\") as \"shape\", \"crs\" FROM \"" + schemaPrefix + "om\".\"sampling_features\"");//NOSONAR
            default       -> cnx.prepareStatement("SELECT * FROM \"" + schemaPrefix + "om\".\"sampling_features\"");//NOSONAR
        };
        result = stmtAll.executeQuery();

    }

    public FeatureType getFeatureType() {
        return type;
    }

    @Override
    public Feature next() throws FeatureStoreRuntimeException {
        try {
            read();
        } catch (Exception ex) {
            throw new FeatureStoreRuntimeException(ex);
        }
        candidate = current;
        current = null;
        return candidate;
    }

    @Override
    public boolean hasNext() throws FeatureStoreRuntimeException {
        try {
            read();
        } catch (Exception ex) {
            throw new FeatureStoreRuntimeException(ex);
        }
        return current != null;
    }

    protected void read() throws Exception {
        if (current != null) {
            return;
        }
        if (!result.next()) {
            return;
        }
        int srid = result.getInt("crs");
       CoordinateReferenceSystem currentCRS = OM2Utils.parsePostgisCRS(srid);
        if (firstCRS) {
            crs = currentCRS;
            final FeatureTypeBuilder ftb = new FeatureTypeBuilder(type);
            ((AttributeTypeBuilder)ftb.getProperty("position")).setCRS(crs);
            type = ftb.build();
            firstCRS = false;
        }

        current = type.newInstance();
        final String id = result.getString("id");
        current.setPropertyValue(AttributeConvention.IDENTIFIER_PROPERTY.toString(), id);
        Geometry geom = null;
        if (dialect.equals(DUCKDB)) {
            String s = result.getString("shape");
            if (s != null) {
                WKTReader reader = new WKTReader();
                geom = reader.read(s);
            }
        } else {
            byte[] b = result.getBytes("shape");
            if (b != null) {
                WKBReader reader = new WKBReader();
                geom = reader.read(b);
            }
        }
        if (geom != null) {
            JTS.setCRS(geom, currentCRS);
        }
        if (!Utilities.equalsIgnoreMetadata(currentCRS, crs)) {
            try {
                geom =  org.apache.sis.geometry.wrapper.jts.JTS.transform(geom, crs);
            } catch (TransformException ex) {
                throw new ConstellationStoreException(ex);
            }
        }
        current.setPropertyValue(OMFeatureTypes.SF_ATT_DESC.toString(),result.getString("description"));
        current.setPropertyValue(OMFeatureTypes.SF_ATT_NAME.toString(),result.getString("name"));
        current.setPropertyValue(OMFeatureTypes.SF_ATT_SAMPLED.toString(),result.getString("sampledfeature"));
        current.setPropertyValue(OMFeatureTypes.SF_ATT_POSITION.toString(),geom);
    }

    @Override
    public void close() {
        try {
            result.close();
            cnx.close();
        } catch (SQLException ex) {
            throw new FeatureStoreRuntimeException(ex);
        }
    }

    @Override
    public void remove() throws FeatureStoreRuntimeException {
        if (candidate == null) {
            return;
        }
        try (PreparedStatement stmtDelete = cnx.prepareStatement("DELETE FROM \"" + schemaPrefix + "om\".\"sampling_features\" WHERE \"id\" = ?")) {//NOSONAR

            stmtDelete.setString(1, FeatureExt.getId(candidate).getIdentifier());
            stmtDelete.executeUpdate();

        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "Error while deleting procedure features", ex);
        }
    }
}
