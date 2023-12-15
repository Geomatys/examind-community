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
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
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
import org.constellation.admin.SpringHelper;

import static org.constellation.api.CommonConstants.RESPONSE_FORMAT_V100_XML;
import static org.constellation.api.CommonConstants.RESPONSE_FORMAT_V200_XML;
import org.constellation.business.IDatasourceBusiness;
import static org.constellation.store.observation.db.OMSQLDialect.POSTGRES;

import org.constellation.store.observation.db.feature.SensorFeatureSet;
import org.constellation.util.SQLUtilities;
import org.constellation.util.Util;
import org.geotoolkit.observation.AbstractFilteredObservationStore;
import org.geotoolkit.observation.OMUtils;
import org.geotoolkit.observation.ObservationFilterReader;
import org.geotoolkit.observation.ObservationReader;
import org.geotoolkit.observation.ObservationStoreCapabilities;
import org.geotoolkit.observation.ObservationWriter;
import org.geotoolkit.observation.feature.OMFeatureTypes;
import org.geotoolkit.observation.model.OMEntity;
import org.geotoolkit.observation.model.Observation;
import org.geotoolkit.observation.model.Phenomenon;
import org.geotoolkit.observation.model.Procedure;
import org.geotoolkit.observation.model.ProcedureDataset;

import org.geotoolkit.observation.model.ResponseMode;
import org.geotoolkit.observation.model.SamplingFeature;
import org.geotoolkit.observation.query.DatasetQuery;
import org.geotoolkit.observation.query.HistoricalLocationQuery;
import static org.geotoolkit.observation.query.ObservationQueryUtilities.buildQueryForSensor;
import org.geotoolkit.observation.query.ProcedureQuery;
import org.geotoolkit.storage.DataStores;
import static org.geotoolkit.observation.query.ObservationQueryUtilities.buildQueryForSensors;
import org.locationtech.jts.geom.Geometry;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class SOSDatabaseObservationStore extends AbstractFilteredObservationStore implements Aggregate {

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

    protected final OMSQLDialect dialect;

    public SOSDatabaseObservationStore(final ParameterValueGroup params) throws DataStoreException {
        super(Parameters.castOrWrap(params));
        try {

            // driver
            final String driver = SOSDatabaseParamsUtils.getDriverClassName(params);

            // jdbc url
            final String jdbcUrl = SOSDatabaseParamsUtils.getJDBCUrl(params);

            // hiroku form url
            final String hirokuUrl = SOSDatabaseParamsUtils.getHirokuUrl(params);

            // username
            final String user = (String) params.parameter(SOSDatabaseObservationStoreFactory.USER.getName().toString()).getValue();

            // password
            final String passwd = (String) params.parameter(SOSDatabaseObservationStoreFactory.PASSWD.getName().toString()).getValue();

            // examind special for sharing datasource (disabled for derby)
            DataSource candidate = null;
            if (hirokuUrl != null) {
                try {
                    IDatasourceBusiness dsBusiness = SpringHelper.getBean(IDatasourceBusiness.class).orElse(null);
                    candidate = dsBusiness.getSQLDatasource(hirokuUrl, user, passwd).orElse(null);
                    if (candidate == null) {
                        LOGGER.info("No existing examind datasource found.");
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, "Unable to get an existing examind datasource.", ex);
                }
            }
            // fall back on direct datasource instanciation.
            if (candidate == null) {
                candidate = SQLUtilities.getDataSource(jdbcUrl, driver, user, passwd);
            }
            source =  candidate;

            dialect = OMSQLDialect.valueOf(((String) params.parameter(SOSDatabaseObservationStoreFactory.SGBDTYPE.getName().toString()).getValue()).toUpperCase());
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
                
                if (dialect.equals(OMSQLDialect.DUCKDB)) {
                    try (Statement loadExt = c.createStatement()) {
                        loadExt.execute("INSTALL spatial");
                        loadExt.execute("LOAD spatial");
                    }
                }
            } catch (SQLException ex) {
                throw new DataStoreException(ex);
            }
        } catch(IOException ex) {
            throw new DataStoreException(ex);
        }
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
            reader = new OM2ObservationReader(source, dialect, schemaPrefix, properties, timescaleDB);
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
            writer = new OM2ObservationWriter(source, dialect, schemaPrefix, properties, timescaleDB, maxFieldByTable);
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
            filter = new OM2ObservationFilterReader(source, dialect, schemaPrefix, properties, timescaleDB);
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

    /**
     * overridden while geotk does not handle sensorId filter
     */
    @Override
    public List<ProcedureDataset> getProcedureDatasets(DatasetQuery query) throws DataStoreException {
        final List<ProcedureDataset> results = new ArrayList<>();

        final ObservationFilterReader procFilter = getFilter();
        
        procFilter.init(new ProcedureQuery());
        // TODO  <start> move up to geotk
        procFilter.setProcedure(query.getSensorIds());
        // TODO  <end> move up to geotk
        
        for (org.opengis.observation.Process p : procFilter.getProcesses()) {

            final Procedure proc  =  (Procedure) p;
            final String omType = (String) proc.getProperties().getOrDefault("type", "timeseries");
            final ProcedureDataset procedure = new ProcedureDataset(proc.getId(), proc.getName(), proc.getDescription(), "Component", omType, new ArrayList<>(), null);

            Observation template = (Observation) getReader().getTemplateForProcedure(proc.getId());

            // complete fields and location
            if (template != null) {
                final Phenomenon phenProp = template.getObservedProperty();
                if (phenProp != null) {
                    final List<String> fields = OMUtils.getPhenomenonsFieldIdentifiers(phenProp);
                    for (String field : fields) {
                        if (!procedure.fields.contains(field)) {
                            procedure.fields.add(field);
                        }
                    }
                }
                SamplingFeature foim = template.getFeatureOfInterest();
                procedure.spatialBound.appendLocation(template.getSamplingTime(), foim);
            }

            // get historical locations
            HistoricalLocationQuery hquery = (HistoricalLocationQuery) buildQueryForSensor(OMEntity.HISTORICAL_LOCATION, proc.getId());
            Map<Date, Geometry> sensorLocations = getHistoricalSensorLocations(hquery).getOrDefault(proc.getId(), Collections.EMPTY_MAP);
            procedure.spatialBound.getHistoricalLocations().putAll(sensorLocations);
            results.add(procedure);
        }
        return results;
    }
}
