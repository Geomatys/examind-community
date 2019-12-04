
package org.constellation.store.observation.db;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;
import org.apache.sis.feature.builder.AttributeTypeBuilder;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.referencing.CRS;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.storage.feature.FeatureReader;
import org.geotoolkit.storage.feature.FeatureStoreRuntimeException;
import org.geotoolkit.data.om.OMFeatureTypes;
import org.geotoolkit.geometry.jts.JTS;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
class SOSDatabaseFeatureReader implements FeatureReader {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.store.observation.db");
    protected final Connection cnx;
    private boolean firstCRS = true;
    protected FeatureType type;
    private final ResultSet result;
    protected Feature current = null;
    private CoordinateReferenceSystem crs;

    protected final String schemaPrefix;

    SOSDatabaseFeatureReader(Connection cnx, boolean isPostgres, final FeatureType type, final String schemaPrefix) throws SQLException {
        this.type = type;
        this.cnx = cnx;
        this.schemaPrefix = schemaPrefix;
        final PreparedStatement stmtAll;
        if (isPostgres) {
            stmtAll = cnx.prepareStatement("SELECT \"id\", \"name\", \"description\", \"sampledfeature\", st_asBinary(\"shape\"), \"crs\" FROM \"" + schemaPrefix + "om\".\"sampling_features\"");
        } else {
            stmtAll = cnx.prepareStatement("SELECT * FROM \"" + schemaPrefix + "om\".\"sampling_features\"");
        }
        result = stmtAll.executeQuery();
    }

    @Override
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
        Feature candidate = current;
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
        if (firstCRS) {
            try {
                String crsCode = result.getString("crs");
                if (crsCode == null) {
                    LOGGER.warning("Missing CRS in sampling_feature. using default EPSG:4326");
                    crsCode = "4326";
                }
                crs = CRS.forCode("EPSG:" + crsCode);
                final FeatureTypeBuilder ftb = new FeatureTypeBuilder(type);
                ((AttributeTypeBuilder)ftb.getProperty("position")).setCRS(crs);
                type = ftb.build();
                firstCRS = false;
            } catch (FactoryException ex) {
                throw new IOException(ex);
            }
        }

        current = type.newInstance();
        final String id = result.getString("id");
        current.setPropertyValue(AttributeConvention.IDENTIFIER_PROPERTY.toString(), id);
        final byte[] b = result.getBytes(5);
        final Geometry geom;
        if (b != null) {
            WKBReader reader = new WKBReader();
            geom = reader.read(b);
        } else {
            geom = null;
        }
        JTS.setCRS(geom, crs);
        current.setPropertyValue(OMFeatureTypes.ATT_DESC.toString(),result.getString("description"));
        current.setPropertyValue(OMFeatureTypes.ATT_NAME.toString(),result.getString("name"));
        current.setPropertyValue(OMFeatureTypes.ATT_SAMPLED.toString(),result.getString("sampledfeature"));
        current.setPropertyValue(OMFeatureTypes.ATT_POSITION.toString(),geom);
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
        throw new FeatureStoreRuntimeException("Not supported.");
    }

}
