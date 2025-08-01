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

import org.constellation.store.observation.db.model.OMSQLDialect;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.sql.DataSource;

import org.apache.sis.parameter.Parameters;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.Resource;
import org.apache.sis.util.Version;
import org.constellation.admin.SpringHelper;

import static org.constellation.api.CommonConstants.RESPONSE_FORMAT_V100_XML;
import static org.constellation.api.CommonConstants.RESPONSE_FORMAT_V200_XML;
import org.constellation.business.IDatasourceBusiness;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.constellation.exception.ConstellationException;
import static org.constellation.store.observation.db.SOSDatabaseObservationStoreFactory.*;
import static org.constellation.store.observation.db.model.OMSQLDialect.POSTGRES;

import org.constellation.store.observation.db.feature.SensorFeatureSet;
import org.constellation.store.observation.db.mixed.MixedObservationFilterReader;
import org.constellation.util.SQLUtilities;
import org.constellation.util.Util;
import org.geotoolkit.observation.AbstractFilteredObservationStore;
import org.geotoolkit.observation.FilterAppend;
import org.geotoolkit.observation.ObservationFilterReader;
import org.geotoolkit.observation.ObservationReader;
import org.geotoolkit.observation.ObservationStoreCapabilities;
import org.geotoolkit.observation.ObservationWriter;
import org.geotoolkit.observation.feature.OMFeatureTypes;
import org.geotoolkit.observation.model.OMEntity;
import static org.geotoolkit.observation.model.OMEntity.FEATURE_OF_INTEREST;
import static org.geotoolkit.observation.model.OMEntity.HISTORICAL_LOCATION;
import static org.geotoolkit.observation.model.OMEntity.LOCATION;

import org.geotoolkit.observation.model.ResponseMode;
import org.geotoolkit.observation.query.LocationQuery;
import org.geotoolkit.storage.DataStores;
import org.opengis.filter.BinarySpatialOperator;
import org.opengis.filter.Filter;
import org.opengis.filter.LogicalOperator;
import org.opengis.filter.LogicalOperatorName;
import org.opengis.filter.ValueReference;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class SOSDatabaseObservationStore extends AbstractFilteredObservationStore implements Aggregate {

    @Autowired
    private IDatasourceBusiness datasourceBusiness;
    
    public static final String SQL_DIALECT = "sql_dialect";
    public static final String TIMESCALEDB_VERSION = "timescaledb_version";

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
    protected final String mode;
    protected final Version timescaleDBVersion;
    protected final String decimationAlgorithm;
    protected final int maxFieldByTable;

    protected final OMSQLDialect dialect;

    public SOSDatabaseObservationStore(final Parameters params) throws DataStoreException {
        super(params);
        SpringHelper.injectDependencies(this);
        Integer datasourceId = params.getMandatoryValue(DATASOURCE_ID);
        var exads = datasourceBusiness.getDatasource(datasourceId);
        if (exads == null) throw new DataStoreException("No examind datasource find for id: " + datasourceId);
        try {
            source = datasourceBusiness.getSQLDatasource(datasourceId).orElse(null);
        } catch (ConstellationException ex) {
            throw new DataStoreException(ex);
        }
        dialect = OMSQLDialect.valueOf(SQLUtilities.getSGBDType(exads.getUrl()).toUpperCase());
        mode    = params.getValue(SOSDatabaseObservationStoreFactory.MODE);
        boolean timescaleDB = params.getValue(SOSDatabaseObservationStoreFactory.TIMESCALEDB);

        String sp = params.getValue(SOSDatabaseObservationStoreFactory.SCHEMA_PREFIX);
        if (sp == null) {
            this.schemaPrefix = "";
        } else {
            if (Util.containsForbiddenCharacter(sp)) {
                throw new DataStoreException("Invalid schema prefix value");
            }
            this.schemaPrefix = sp;
        }
        this.maxFieldByTable = params.getValue(SOSDatabaseObservationStoreFactory.MAX_FIELD_BY_TABLE);

        // decimation algorithm
        String decAlgo = params.getValue(SOSDatabaseObservationStoreFactory.DECIMATION_ALGORITHM);

        // allow to get default value from application properties if not set
        if (decAlgo == null || decAlgo.isEmpty()) {
            this.decimationAlgorithm = Application.getProperty(AppProperty.EXA_OM2_DEFAULT_DECIMATION_ALGORITHM, "");
        } else {
            this.decimationAlgorithm = decAlgo;
        }

         // build database structure if needed
         buildDatasource();

        // Test if the connection is valid
        try(final Connection c = this.source.getConnection()) {
            if (dialect.equals(OMSQLDialect.DUCKDB)) {
                try (Statement loadExt = c.createStatement()) {
                    String tsExtDirValue = Application.getProperty(AppProperty.EXA_OM2_DUCKDB_EXTENSION_DIRECTORY, null);
                    if (tsExtDirValue != null) {
                        loadExt.execute("SET extension_directory='" + tsExtDirValue + "'");
                    }
                    loadExt.execute("INSTALL spatial");
                    loadExt.execute("LOAD spatial");
                }
            }
            if (timescaleDB) {
                try (Statement stmt = c.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT installed_version FROM pg_available_extensions where name = 'timescaledb'")) {
                    if (rs.next()) {
                        String version = rs.getString(1);
                        LOGGER.log(Level.INFO, "TimescaleDB version: {0}", version);
                        timescaleDBVersion = new Version(version);
                    } else {
                        LOGGER.warning("Unable to read timescaleDB version, assuming < 1.11.0");
                        timescaleDBVersion = new Version("1.10.0");
                    }
                }
            } else {
                timescaleDBVersion = null;
            }
        } catch (SQLException ex) {
            throw new DataStoreException(ex);
        }
    }

    @Override
    protected Map<String, Object> getBasicProperties() {
        Map<String, Object> properties= super.getBasicProperties();
        properties.put(SQL_DIALECT, dialect);
        properties.put(SCHEMA_PREFIX_NAME, schemaPrefix);
        properties.put(TIMESCALEDB_VERSION, timescaleDBVersion);
        properties.put(DECIMATION_ALGORITHM_NAME, decimationAlgorithm);
        return properties;
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
            featureSets.add(new SensorFeatureSet(this, OMFeatureTypes.buildSamplingFeatureFeatureType(), source, dialect, schemaPrefix, SensorFeatureSet.ReaderType.SAMPLING_FEATURE));
            featureSets.add(new SensorFeatureSet(this, OMFeatureTypes.buildSensorFeatureType(),          source, dialect, schemaPrefix, SensorFeatureSet.ReaderType.SENSOR_FEATURE));
        }
        return featureSets;
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
            reader = new OM2ObservationReader(source, properties);
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
            writer = new OM2ObservationWriter(source, properties, maxFieldByTable);
        }
        return writer;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public synchronized ObservationFilterReader getFilter() throws DataStoreException {
        if (mode.equals("mixed")) {
            if (filter == null) {
                final Map<String,Object> properties = getBasicProperties();
                filter = new MixedObservationFilterReader(source, properties);
            }
            return new MixedObservationFilterReader((OM2ObservationFilter) filter);
        } else {
            if (filter == null) {
                final Map<String,Object> properties = getBasicProperties();
                filter = new OM2ObservationFilterReader(source, properties);
            }
            return new OM2ObservationFilterReader((OM2ObservationFilter) filter);
        }
    }

    @Override
    public ObservationStoreCapabilities getCapabilities() {
        final List<ResponseMode> responseMode = Arrays.asList(ResponseMode.INLINE, ResponseMode.RESULT_TEMPLATE);
        return new ObservationStoreCapabilities(true, false, false, Arrays.asList("result"), RESPONSE_FORMAT, responseMode, true);
    }

    private boolean buildDatasource() throws DataStoreException {
        try {
            if (OM2DatabaseCreator.validConnection(source)) {

                if (dialect.equals(POSTGRES) && !OM2DatabaseCreator.isPostgisInstalled(source)) {
                    LOGGER.warning("Missing Postgis extension.");
                    return false;
                }
                if (!OM2DatabaseCreator.structurePresent(source, schemaPrefix)) {
                    OM2DatabaseCreator.createObservationDatabase(source, dialect, schemaPrefix);
                    return true;
                } else {
                    boolean updated = OM2DatabaseCreator.updateStructure(source, schemaPrefix, dialect);
                    if (updated) {
                        LOGGER.info("OM2 structure already present (updated)");
                    } else {
                        LOGGER.info("OM2 structure already present");
                    }
                }
                return true;

            } else {
                LOGGER.warning("unable to connect OM datasource");
            }
            return false;
        } catch (SQLException | IOException ex) {
            throw new DataStoreException("Erro while building OM2 datasource", ex);
        }
    }

    @Override
    protected FilterAppend createNewFilterAppend() {
        return new OM2FilterAppend();
    }
    
    @Override
    protected FilterAppend handleBBOXFilter(OMEntity mode, final ObservationFilterReader localOmFilter, BinarySpatialOperator bbox) throws DataStoreException {
        switch (mode) {
            case LOCATION, HISTORICAL_LOCATION, FEATURE_OF_INTEREST ->  {
                return localOmFilter.setBoundingBox(bbox);
            }
            default       -> {
                if (getCapabilities().isBoundedObservation) {
                    return localOmFilter.setBoundingBox(bbox);
                } else {
                    if (bbox.getOperand1() instanceof ValueReference ref && ref.getXPath() != null && ref.getXPath().endsWith("location")) {
                        LocationQuery query = new LocationQuery(bbox);
                        List<String> locIds = List.copyOf(getEntityNames(query));
                        if (!locIds.isEmpty()) {
                            if (locIds.size() == 1) {
                                return localOmFilter.setProcedure(locIds.get(0));
                            } else {
                                OM2FilterAppend result = new OM2FilterAppend();
                                localOmFilter.startFilterBlock();
                                for (int i = 0; i < locIds.size(); i++) {
                                    // if not first we append the logical operator
                                    if (i > 0) localOmFilter.appendFilterOperator(LogicalOperatorName.OR, result);
                                    
                                    FilterAppend fa = localOmFilter.setProcedure(locIds.get(i));
                                    
                                     // we may have to remove it
                                    if (i > 0) localOmFilter.removeFilterOperator(LogicalOperatorName.OR, result, fa);
                                    
                                    result = (OM2FilterAppend) result.merge(fa);
                                }
                                localOmFilter.endFilterBlock(LogicalOperatorName.OR, result);
                                return result;
                            }
                        } else {
                            // invalid the results
                            return localOmFilter.setProcedure("unexisting-proc");
                        }
                    } else {
                        List<String> fois = getFeaturesOfInterestForBBOX(bbox);
                        if (!fois.isEmpty()) {
                            if (fois.size() == 1) {
                                return localOmFilter.setFeatureOfInterest(fois.get(0));
                            } else {
                                OM2FilterAppend result = new OM2FilterAppend();
                                localOmFilter.startFilterBlock();
                                for (int i = 0; i < fois.size(); i++) {
                                     // if not first we append the logical operator
                                    if (i > 0) localOmFilter.appendFilterOperator(LogicalOperatorName.OR, result);
                                    
                                    FilterAppend fa = localOmFilter.setFeatureOfInterest(fois.get(i));
                                    
                                    // we may have to remove it
                                    if (i > 0) localOmFilter.removeFilterOperator(LogicalOperatorName.OR, result, fa);
                                    
                                    result = (OM2FilterAppend) result.merge(fa);
                                }
                                localOmFilter.endFilterBlock(LogicalOperatorName.OR, result);
                                return result;
                            }
                        } else {
                            // invalid the results
                            return localOmFilter.setFeatureOfInterest("unexisting-foi");
                        }
                    }
                }
            }
        }
    }
    
    
    
    
    @Override
    protected FilterAppend handleLogicalFilter(final OMEntity entityType, final ObservationFilterReader localOmFilter, LogicalOperator<?> logFilter) throws DataStoreException {
        OM2FilterAppend result = new OM2FilterAppend();
        LogicalOperatorName type = logFilter.getOperatorType();
        localOmFilter.startFilterBlock(type);
        int nbFilter = logFilter.getOperands().size();
        for (int i = 0; i < nbFilter; i++) {
            Filter f = logFilter.getOperands().get(i);
            
            // if not first we append the logical operator
            if (i > 0) localOmFilter.appendFilterOperator(type, result);
            
            FilterAppend fa = handleFilter(entityType, f, localOmFilter);
            
            // we may have to remove it
            if (i > 0) localOmFilter.removeFilterOperator(type, result, fa);
            
            result = result.merge(fa);
        }
        localOmFilter.endFilterBlock(type, result);
        return result;
    }
}
