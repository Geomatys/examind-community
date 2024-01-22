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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.storage.DataStoreException;
import org.constellation.store.observation.db.model.OMSQLDialect;
import static org.constellation.store.observation.db.model.OMSQLDialect.DERBY;
import static org.constellation.store.observation.db.model.OMSQLDialect.DUCKDB;
import static org.constellation.store.observation.db.model.OMSQLDialect.POSTGRES;
import org.constellation.util.Util;
import org.geotoolkit.feature.FeatureExt;
import org.geotoolkit.filter.FilterUtilities;
import org.geotoolkit.storage.feature.FeatureStoreRuntimeException;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBWriter;
import org.locationtech.jts.io.WKTWriter;
import org.opengis.feature.Feature;
import org.opengis.filter.ResourceId;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class OM2SensorFeatureWriter implements OM2FeatureWriter {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.store.observation.db");
    private final Connection cnx;
    protected final String schemaPrefix;
    protected final String idBase;
    protected final OMSQLDialect dialect;

    public OM2SensorFeatureWriter(Connection cnx, final String schemaPrefix, final String idBase, final OMSQLDialect dialect) throws SQLException, DataStoreException {
        this.cnx = cnx;
        if (Util.containsForbiddenCharacter(schemaPrefix)) {
            throw new DataStoreException("Invalid schema prefix value");
        }
        this.schemaPrefix = schemaPrefix;
        this.idBase = idBase;
        this.dialect = dialect;
    }

    @Override
    public List<ResourceId> add(Iterator<? extends Feature> features) throws DataStoreException {
        final List<ResourceId> result = new ArrayList<>();

        final String geomField = switch(dialect) {
            case POSTGRES, DERBY  -> "?";
            case DUCKDB           -> "ST_GeomFromText(cast (? as varchar))";
        };
        try (PreparedStatement stmtWrite = cnx.prepareStatement("INSERT INTO \"" + schemaPrefix + "om\".\"procedures\" VALUES(?," + geomField + ",?)")) { //NOSONAR

            while (features.hasNext()) {
                final Feature feature = features.next();
                ResourceId identifier = FeatureExt.getId(feature);
                if (identifier == null || identifier.getIdentifier().isEmpty()) {
                    identifier = getNewFeatureId();
                }

                stmtWrite.setString(1, identifier.getIdentifier());
                final Optional<Geometry> geometry = FeatureExt.getDefaultGeometryValue(feature)
                        .filter(Geometry.class::isInstance)
                        .map(Geometry.class::cast);
                if (geometry.isPresent()) {
                    final Geometry geom = geometry.get();
                    final int SRID = geom.getSRID();
                    if (dialect.equals(OMSQLDialect.DUCKDB)) {
                        WKTWriter writer = new WKTWriter();
                        String wkt = writer.write(geom);
                        stmtWrite.setString(2,  wkt);
                    } else {
                        final WKBWriter writer = new WKBWriter();
                        byte[] bytes = writer.write(geom);
                        stmtWrite.setBytes(2, bytes);
                    }
                    stmtWrite.setInt(3, SRID);
                } else {
                    stmtWrite.setNull(2, Types.VARCHAR);
                    stmtWrite.setNull(3, Types.INTEGER);
                }
                stmtWrite.executeUpdate();
                result.add(identifier);
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "Error while writing procedure feature", ex);
        }
        return result;
    }

    private void setGeometry(PreparedStatement stmt, Geometry geom, int index) throws SQLException {
        if (dialect.equals(OMSQLDialect.DUCKDB)) {

        } else {

        }
    }

    private ResourceId getNewFeatureId() {
        try (final PreparedStatement stmtLastId = cnx.prepareStatement("SELECT \"id\" FROM \"" + schemaPrefix + "om\".\"procedures\" ORDER BY \"id\" ASC");//NOSONAR
             final ResultSet result = stmtLastId.executeQuery()) {
                // keep the last
                String id = null;
                while (result.next()) {
                    id = result.getString(1);
                }
                if (id != null) {
                    try {
                        final int i = Integer.parseInt(id.substring(idBase.length()));
                        return FilterUtilities.FF.resourceId(idBase + i);
                    } catch (NumberFormatException ex) {
                        LOGGER.warning("a snesor ID is malformed in procedures tables");
                    }
                } else {
                    return FilterUtilities.FF.resourceId(idBase + 1);
                }
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, null, ex);
        }
        return null;
    }

    @Override
    public void close() {
        try {
            cnx.close();
        } catch (SQLException ex) {
            throw new FeatureStoreRuntimeException(ex);
        }
    }
}
