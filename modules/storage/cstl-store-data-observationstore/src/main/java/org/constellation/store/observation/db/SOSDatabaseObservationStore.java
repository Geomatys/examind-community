/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2014, Geomatys
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.constellation.store.observation.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import org.apache.sis.parameter.Parameters;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.Resource;

import static org.constellation.api.CommonConstants.RESPONSE_FORMAT_V100_XML;
import static org.constellation.api.CommonConstants.RESPONSE_FORMAT_V200_XML;

import org.constellation.store.observation.db.feature.SensorFeatureSet;
import org.constellation.util.SQLUtilities;
import org.constellation.util.Util;
import org.geotoolkit.observation.AbstractFilteredObservationStore;
import org.geotoolkit.observation.ObservationFilterReader;
import org.geotoolkit.observation.ObservationReader;
import org.geotoolkit.observation.ObservationStoreCapabilities;
import org.geotoolkit.observation.ObservationWriter;
import org.geotoolkit.observation.feature.OMFeatureTypes;

import org.geotoolkit.observation.model.ResponseMode;
import org.geotoolkit.storage.DataStores;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class SOSDatabaseObservationStore extends AbstractFilteredObservationStore implements Aggregate {

    private final String SQL_WRITE_SAMPLING_POINT;
    private final String SQL_GET_LAST_ID;

    static final Map<String, List<String>> RESPONSE_FORMAT = new HashMap<>();
    static {
        RESPONSE_FORMAT.put("1.0.0", Arrays.asList(RESPONSE_FORMAT_V100_XML));
        RESPONSE_FORMAT.put("2.0.0", Arrays.asList(RESPONSE_FORMAT_V200_XML));
    }

    protected ObservationReader reader;
    protected ObservationWriter writer;
    protected ObservationFilterReader filter;
    protected final DataSource source;
    protected final String schemaPrefix;
    protected final boolean timescaleDB;
    protected final int maxFieldByTable;

    protected final boolean isPostgres;
    private final List<Resource> components = new ArrayList<>();


    public SOSDatabaseObservationStore(final ParameterValueGroup params) throws DataStoreException {
        super(Parameters.castOrWrap(params));
        try {

            // driver
            final String driver = SOSDatabaseParamsUtils.getDriverClassName(params);

            // url
            final String jdbcUrl = SOSDatabaseParamsUtils.getJDBCUrl(params);

            // username
            final String user = (String) params.parameter(SOSDatabaseObservationStoreFactory.USER.getName().toString()).getValue();

            // password
            final String passwd = (String) params.parameter(SOSDatabaseObservationStoreFactory.PASSWD.getName().toString()).getValue();

            source =  SQLUtilities.getDataSource(driver, jdbcUrl, user, passwd);

            isPostgres = driver.startsWith("org.postgresql");
            timescaleDB = (Boolean) params.parameter(SOSDatabaseObservationStoreFactory.TIMESCALEDB.getName().toString()).getValue();

            String sp =  (String) params.parameter(SOSDatabaseObservationStoreFactory.SCHEMA_PREFIX.getName().toString()).getValue();
            if (sp == null) {
                this.schemaPrefix = "";
            } else {
                if (Util.containsForbiddenCharacter(sp)) {
                    throw new DataStoreException("Invalid schema prefix value");
                }
                this.schemaPrefix = sp;
            }
            this.maxFieldByTable = (int) params.parameter(SOSDatabaseObservationStoreFactory.MAX_FIELD_BY_TABLE.getName().toString()).getValue();

            // build database structure if needed
            buildDatasource();

            // Test if the connection is valid
            try(final Connection c = this.source.getConnection()) {
                // TODO: add a validation test here (query db metadata ?)
            } catch (SQLException ex) {
                throw new DataStoreException(ex);
            }
        } catch(IOException ex) {
            throw new DataStoreException(ex);
        }

        SQL_WRITE_SAMPLING_POINT = "INSERT INTO \"" + schemaPrefix + "om\".\"sampling_features\" VALUES(?,?,?,?,?,?)";
        SQL_GET_LAST_ID = "SELECT COUNT(*) FROM \"" + schemaPrefix + "om\".\"sampling_features\"";
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public DataStoreProvider getProvider() {
        return DataStores.getProviderById(SOSDatabaseObservationStoreFactory.NAME);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    protected String getStoreIdentifier() {
        return "om2-observation";
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public synchronized Collection<? extends Resource> components() throws DataStoreException {
        if (featureSets == null) {
            featureSets = new ArrayList<>();
            featureSets.add(new SensorFeatureSet(this, OMFeatureTypes.buildSamplingFeatureFeatureType(), source, isPostgres, schemaPrefix, SensorFeatureSet.ReaderType.SAMPLING_FEATURE));
            featureSets.add(new SensorFeatureSet(this, OMFeatureTypes.buildSensorFeatureType(), source, isPostgres, schemaPrefix, SensorFeatureSet.ReaderType.SENSOR_FEATURE));
        }
        return featureSets;
    }

    private Connection getConnection() throws SQLException{
        return source.getConnection();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void close() throws DataStoreException {
        if (reader != null) reader.destroy();
        if (writer != null) writer.destroy();
        if (filter != null) filter.destroy();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public synchronized ObservationReader getReader() throws DataStoreException {
        if (reader == null) {
            final Map<String,Object> properties = getBasicProperties();
            reader = new OM2ObservationReader(source, isPostgres, schemaPrefix, properties, timescaleDB);
        }
        return reader;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public synchronized ObservationWriter getWriter() throws DataStoreException {
        if (writer == null) {
            final Map<String,Object> properties = getBasicProperties();
            writer = new OM2ObservationWriter(source, isPostgres, schemaPrefix, properties, timescaleDB, maxFieldByTable);
        }
        return writer;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public synchronized ObservationFilterReader getFilter() throws DataStoreException {
        if (filter == null) {
            final Map<String,Object> properties = getBasicProperties();
            filter = new OM2ObservationFilterReader(source, isPostgres, schemaPrefix, properties, timescaleDB);
        }
        return new OM2ObservationFilterReader((OM2ObservationFilter) filter);
    }

    @Override
    public ObservationStoreCapabilities getCapabilities() {
        final List<ResponseMode> responseMode = Arrays.asList(ResponseMode.INLINE, ResponseMode.RESULT_TEMPLATE);
        return new ObservationStoreCapabilities(true, false, false, Arrays.asList("result"), RESPONSE_FORMAT, responseMode, true);
    }

    private boolean buildDatasource() throws DataStoreException {
        try {
            if (OM2DatabaseCreator.validConnection(source)) {
                if (OM2DatabaseCreator.isPostgisInstalled(source, true)) {
                    if (!OM2DatabaseCreator.structurePresent(source, schemaPrefix)) {
                        OM2DatabaseCreator.createObservationDatabase(source, true, null, schemaPrefix);
                        return true;
                    } else {
                        boolean updated = OM2DatabaseCreator.updateStructure(source, schemaPrefix, true);
                        if (updated) {
                            LOGGER.info("OM2 structure already present (updated)");
                        } else {
                            LOGGER.info("OM2 structure already present");
                        }
                    }
                    return true;
                } else {
                    LOGGER.warning("Missing Postgis extension.");
                }
            } else {
                LOGGER.warning("unable to connect OM datasource");
            }
            return false;
        } catch (SQLException | IOException ex) {
            throw new DataStoreException("Erro while building OM2 datasource", ex);
        }
    }
}
