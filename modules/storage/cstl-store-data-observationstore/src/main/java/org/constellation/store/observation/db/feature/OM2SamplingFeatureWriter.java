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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.event.StoreListeners;
import org.constellation.util.Util;
import org.geotoolkit.feature.FeatureExt;
import org.geotoolkit.filter.FilterUtilities;
import static org.geotoolkit.observation.feature.OMFeatureTypes.*;
import org.geotoolkit.storage.event.FeatureStoreContentEvent;
import org.geotoolkit.storage.feature.FeatureStoreRuntimeException;
import org.locationtech.jts.io.WKBWriter;
import org.opengis.feature.Feature;
import org.opengis.filter.ResourceId;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class OM2SamplingFeatureWriter implements OM2FeatureWriter {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.store.observation.db");
    private final Connection cnx;
    private final String schemaPrefix;
    private final String idBase;

    public OM2SamplingFeatureWriter(Connection cnx, final String schemaPrefix, final String idBase) throws SQLException, DataStoreException {
        this.cnx = cnx;
        if (Util.containsForbiddenCharacter(schemaPrefix)) {
            throw new DataStoreException("Invalid schema prefix value");
        }
        this.schemaPrefix = schemaPrefix;
        this.idBase = idBase;
    }

    @Override
    public List<ResourceId> add(Iterator<? extends Feature> features) throws DataStoreException {
        final List<ResourceId> result = new ArrayList<>();

        try (PreparedStatement stmtWrite = cnx.prepareStatement("INSERT INTO \"" + schemaPrefix + "om\".\"sampling_features\" VALUES(?,?,?,?,?,?)")) {

            while (features.hasNext()) {
                final Feature feature = features.next();
                ResourceId identifier = FeatureExt.getId(feature);
                if (identifier == null || identifier.getIdentifier().isEmpty()) {
                    identifier = getNewFeatureId();
                }

                stmtWrite.setString(1, identifier.getIdentifier());
                Collection<String> names = ((Collection)feature.getPropertyValue(SF_ATT_NAME.toString()));
                Collection<String> sampleds = ((Collection)feature.getPropertyValue(SF_ATT_SAMPLED.toString()));
                stmtWrite.setString(2, (String) names.iterator().next());
                stmtWrite.setString(3, (String)feature.getPropertyValue(SF_ATT_DESC.toString()));
                stmtWrite.setString(4, sampleds.isEmpty() ? null : sampleds.iterator().next());
                final Optional<org.locationtech.jts.geom.Geometry> geometry = FeatureExt.getDefaultGeometryValue(feature)
                        .filter(org.locationtech.jts.geom.Geometry.class::isInstance)
                        .map(org.locationtech.jts.geom.Geometry.class::cast);
                if (geometry.isPresent()) {
                    final org.locationtech.jts.geom.Geometry geom = geometry.get();
                    WKBWriter writer = new WKBWriter();
                    final int SRID = geom.getSRID();
                    stmtWrite.setBytes(5, writer.write(geom));
                    stmtWrite.setInt(6, SRID);
                } else {
                    stmtWrite.setNull(5, java.sql.Types.VARBINARY);
                    stmtWrite.setNull(6, java.sql.Types.INTEGER);
                }
                stmtWrite.executeUpdate();
                result.add(identifier);
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "Error while iinserting sampling point", ex);
        }
        return result;
    }

    @Override
    public void close() {
        try {
            cnx.close();
        } catch (SQLException ex) {
            throw new FeatureStoreRuntimeException(ex);
        }
    }

    public ResourceId getNewFeatureId() {
        try (final PreparedStatement stmtLastId = cnx.prepareStatement("SELECT COUNT(*) FROM \"" + schemaPrefix + "om\".\"sampling_features\"");
             final ResultSet result = stmtLastId.executeQuery()){
            if (result.next()) {
                final int nb = result.getInt(1) + 1;
                return FilterUtilities.FF.resourceId(idBase + nb);
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, null, ex);
        }
        return null;
    }

}
