
package org.constellation.store.observation.db;

import org.apache.sis.feature.builder.AttributeTypeBuilder;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.referencing.CRS;
import org.apache.sis.storage.DataStoreException;
import org.constellation.util.Util;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.observation.feature.OMFeatureTypes;
import org.geotoolkit.storage.feature.FeatureStoreRuntimeException;
import org.geotoolkit.util.collection.CloseableIterator;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBReader;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class OM2SensorFeatureReader implements CloseableIterator<Feature> {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.store.observation.db");
    protected final Connection cnx;
    private boolean firstCRS = true;
    protected FeatureType type;
    private final ResultSet result;
    protected Feature current = null;
    private CoordinateReferenceSystem crs;

    protected final String schemaPrefix;

    @SuppressWarnings("squid:S2095")
    public OM2SensorFeatureReader(Connection cnx, boolean isPostgres, final FeatureType type, final String schemaPrefix) throws SQLException, DataStoreException {
        this.type = type;
        this.cnx = cnx;
        if (Util.containsForbiddenCharacter(schemaPrefix)) {
            throw new DataStoreException("Invalid schema prefix value");
        }
        this.schemaPrefix = schemaPrefix;
        final PreparedStatement stmtAll;
        if (isPostgres) {
            stmtAll = cnx.prepareStatement("SELECT \"id\", st_asBinary(\"shape\") as \"shape\", \"crs\" FROM \"" + schemaPrefix + "om\".\"procedures\"");//NOSONAR
        } else {
            stmtAll = cnx.prepareStatement("SELECT * FROM \"" + schemaPrefix + "om\".\"procedures\"");//NOSONAR
        }
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
                    LOGGER.warning("Missing CRS in sensor. using default EPSG:4326");
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
        final byte[] b = result.getBytes("shape");
        final Geometry geom;
        if (b != null) {
            WKBReader reader = new WKBReader();
            geom = reader.read(b);
            if (geom != null) {
                JTS.setCRS(geom, crs);
            }
        } else {
            geom = null;
        }
        current.setPropertyValue(OMFeatureTypes.SENSOR_ATT_ID.toString(), id);
        current.setPropertyValue(OMFeatureTypes.SENSOR_ATT_POSITION.toString(),geom);
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
