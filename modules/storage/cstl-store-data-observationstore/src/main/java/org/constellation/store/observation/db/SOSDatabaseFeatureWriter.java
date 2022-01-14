
package org.constellation.store.observation.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.feature.FeatureExt;
import org.geotoolkit.storage.feature.FeatureStoreRuntimeException;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
class SOSDatabaseFeatureWriter extends SOSDatabaseFeatureReader {

    protected static final Logger LOGGER = Logger.getLogger("org.geotoolkit.data.om");
    // TODO WRITE private static final String SQL_WRITE_SAMPLING_POINT = "INSERT INTO \"" + schemaPrefix + "om\".\"sampling_features\" VALUES(?,?,?,?,?,?)";


    Feature candidate = null;

    SOSDatabaseFeatureWriter(Connection cnx, boolean isPostgres, final FeatureType type, final String schemaPrefix) throws SQLException, DataStoreException {
        super(cnx, isPostgres, type, schemaPrefix);
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
    public void remove() throws FeatureStoreRuntimeException {
        if (candidate == null) {
            return;
        }

        try (PreparedStatement stmtDelete = cnx.prepareStatement("DELETE FROM \"" + schemaPrefix + "om\".\"sampling_features\" WHERE \"id\" = ?")) {//NOSONAR
            stmtDelete.setString(1, FeatureExt.getId(candidate).getIdentifier());
            stmtDelete.executeUpdate();
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "Error while removing features", ex);
        }
    }

}
