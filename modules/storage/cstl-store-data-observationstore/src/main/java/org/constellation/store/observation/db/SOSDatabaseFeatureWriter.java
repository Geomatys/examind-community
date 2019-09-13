
package org.constellation.store.observation.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotoolkit.feature.FeatureExt;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.data.FeatureStoreRuntimeException;
import org.geotoolkit.data.FeatureWriter;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
class SOSDatabaseFeatureWriter extends SOSDatabaseFeatureReader implements FeatureWriter {

    protected static final Logger LOGGER = Logging.getLogger("org.geotoolkit.data.om");
    // TODO WRITE private static final String SQL_WRITE_SAMPLING_POINT = "INSERT INTO \"" + schemaPrefix + "om\".\"sampling_features\" VALUES(?,?,?,?,?,?)";


    Feature candidate = null;

    SOSDatabaseFeatureWriter(Connection cnx, boolean isPostgres, final FeatureType type, final String schemaPrefix) throws SQLException {
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

        try (PreparedStatement stmtDelete = cnx.prepareStatement("DELETE FROM \"" + schemaPrefix + "om\".\"sampling_features\" WHERE \"id\" = ?")) {
            stmtDelete.setString(1, FeatureExt.getId(candidate).getID());
            stmtDelete.executeUpdate();
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "Error while removing features", ex);
        }
    }

    @Override
    public void write() throws FeatureStoreRuntimeException {
        throw new FeatureStoreRuntimeException("Not supported.");
    }

}
