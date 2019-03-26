/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2011, Geomatys
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
package com.examind.process.sos;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.constellation.api.ServiceDef;
import org.constellation.business.IDataBusiness;
import org.constellation.business.IDatasetBusiness;
import org.constellation.business.IDatasourceBusiness;
import org.constellation.business.ISensorBusiness;
import org.constellation.dto.DataBrief;
import org.constellation.dto.DataCustomConfiguration;
import org.constellation.dto.DataSource;
import org.constellation.dto.DataSourceSelectedPath;
import org.constellation.dto.ProviderConfiguration;
import org.constellation.dto.Sensor;
import org.constellation.dto.importdata.DatasourceAnalysisV3;
import org.constellation.dto.importdata.ResourceAnalysisV3;
import org.constellation.dto.importdata.ResourceStoreAnalysisV3;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.provider.DataProvider;
import org.constellation.provider.DataProviders;
import org.constellation.sos.configuration.SOSConfigurer;
import org.constellation.sos.ws.SOSUtils;
import org.constellation.sos.ws.SensorMLGenerator;
import org.constellation.ws.IWSEngine;
import org.geotoolkit.data.csv.CSVFeatureStoreFactory;
import org.geotoolkit.gml.xml.v321.AbstractGeometryType;
import org.geotoolkit.observation.ObservationStore;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.geotoolkit.sos.netcdf.ExtractionResult;
import org.geotoolkit.storage.DataStores;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Moissonnage de données de capteur au format csv et publication dans un service SOS
 *
 * @author Samuel Andrés (Geomatys)
 */
public class SosHarvesterProcess extends AbstractCstlProcess {

    @Autowired
    private IDataBusiness dataBusiness;

    @Autowired
    private IDatasetBusiness datasetBusiness;

    @Autowired
    private IDatasourceBusiness datasourceBusiness;

    @Autowired
    private ISensorBusiness sensorBusiness;

    @Autowired
    private IWSEngine wsengine;

    public SosHarvesterProcess(final ProcessDescriptor desc, final ParameterValueGroup input) {
        super(desc,input);
    }

    @Override
    protected void execute() throws ProcessException {
        LOGGER.info("exécution du process sos");

        /*
        0- Paramètres fixés
        =================*/

        final String storeId = "observationCsvFile";// "observationFile";
        final String format = "text/csv; subtype=\"om\"";// "application/x-netcdf";
//        final String storeId = "observationFile";
//        final String format = "application/x-netcdf";
        final int userId = 1; // admin //assertAuthentificated(req);

        /*
        1- Récupération des paramètres du process
        =======================================*/

        final Path sourceFolder = Paths.get(inputParameters.getValue(SosHarvesterProcessDescriptor.DATA_FOLDER));
        final String sosId = inputParameters.getValue(SosHarvesterProcessDescriptor.SERVICE_ID);
        final String datasetIdentifier = inputParameters.getValue(SosHarvesterProcessDescriptor.DATASET_IDENTIFIER);
        final Character separator = inputParameters.getValue(SosHarvesterProcessDescriptor.SEPARATOR);
        final String dateColumn = inputParameters.getValue(SosHarvesterProcessDescriptor.DATE_COLUMN);
        final String dateFormat = inputParameters.getValue(SosHarvesterProcessDescriptor.DATE_FORMAT);
        final String longitudeColumn = inputParameters.getValue(SosHarvesterProcessDescriptor.LONGITUDE_COLUMN);
        final String latitudeColumn = inputParameters.getValue(SosHarvesterProcessDescriptor.LATITUDE_COLUMN);
        final String measureColumns = inputParameters.getValue(SosHarvesterProcessDescriptor.MEASURE_COLUMNS);

        /*
        2- Détermination des données à importer
        =====================================*/

        DataSource ds = new DataSource();
        ds.setType("file");
        ds.setUrl(sourceFolder.toUri().toString());
        ds.setStoreId(storeId);
        ds.setFormat(format);

        final int dsId = datasourceBusiness.create(ds);
        ds = datasourceBusiness.getDatasource(dsId);
        datasourceBusiness.updateDatasourceAnalysisState(dsId, "NOT_STARTED");

        // http://localhost:8080/examind/API/datasources/112/selectedPath
        if (sourceFolder.toFile().isDirectory()) {
            for (final File file : sourceFolder.toFile().listFiles()) {
//                if("tsg-FNFP.csv".equals(file.getName())) {
//                if("NOAAShipTrackWTEC_5031_e90c_b602.nc".equals(file.getName())) {
                if (file.getName().endsWith(".csv")) {
                    datasourceBusiness.addSelectedPath(dsId, '/' + file.getName());
                }
            }
        }

        try {
            List<DataSourceSelectedPath> paths = datasourceBusiness.getSelectedPath(ds, null);
        } catch (ConstellationException ex) {
            Logger.getLogger(SosHarvesterProcess.class.getName()).log(Level.SEVERE, null, ex);
        }


        /*
        3- Imporation des données et génération des fichiers SensorML
        ===========================================================*/

        // http://localhost:8080/examind/API/internal/datas/store/observationFile
        final DataStoreProvider factory = DataStores.getProviderById(storeId);
        final List<String> ids = new ArrayList<>(); // identifiants des SensorMLs pour chaque "procédure"

        if (factory != null) {
            final DataCustomConfiguration.Type storeParams = buildDatastoreConfiguration(factory, "data-store", null);
            storeParams.setSelected(true);


            // http://localhost:8080/examind/API/datasources/106/analysisV3
            final ProviderConfiguration provConfig = new ProviderConfiguration(storeParams.getCategory(), storeParams.getId());
            storeParams.cleanupEmptyProperty();
            storeParams.propertyToMap(provConfig.getParameters());

            provConfig.getParameters().put(CSVFeatureStoreFactory.SEPARATOR.getName().toString(), separator.toString());
            provConfig.getParameters().put(CsvObservationStoreFactory.DATE_COLUMN.getName().toString(), dateColumn);
            provConfig.getParameters().put(CsvObservationStoreFactory.DATE_FORMAT.getName().toString(), dateFormat);
            provConfig.getParameters().put(CsvObservationStoreFactory.LONGITUDE_COLUMN.getName().toString(), longitudeColumn);
            provConfig.getParameters().put(CsvObservationStoreFactory.LATITUDE_COLUMN.getName().toString(), latitudeColumn);
            provConfig.getParameters().put(CsvObservationStoreFactory.MEASURE_COLUMNS.getName().toString(), measureColumns);

            try {
                DatasourceAnalysisV3 analyseDatasourceV3 = datasourceBusiness.analyseDatasourceV3(dsId, provConfig);


                // http://localhost:8080/examind/API/datas/accept?hidden=true

                final Integer datasetId = datasetBusiness.getDatasetId(datasetIdentifier);

                if(datasetId == null) {
                    throw new ProcessException("", this);
                }

                final boolean hidden = false; // true

                for (final ResourceStoreAnalysisV3 resourceStore : analyseDatasourceV3.getStores()) {

                    final DataBrief acceptData = dataBusiness.acceptData(resourceStore.getResources().get(0).getId(), userId, hidden);
                    dataBusiness.updateDataDataSetId(acceptData.getId(), datasetId);


                    // génération du sensorML
                    ids.addAll(generateSensorML(acceptData.getId()));

                }

                datasourceBusiness.delete(dsId);
            } catch (Exception ex) {
                LOGGER.warning(ex.getMessage());
                throw new ProcessException("", this, ex);
            }
        }



        /*
        4- Publication des données correspondant à chaque SensorML sur le service SOS
        ===========================================================================*/


        try {

            final SOSConfigurer configurer = (SOSConfigurer) wsengine.newInstance(ServiceDef.Specification.SOS);

            for(final String sensorID : ids) {
                // retrait d'un capteur du SOS
                // http://localhost:8080/examind/API/SOS/sos1/sensor/NOAAShipTrackWTEC_5031_e90c_b602
                configurer.removeSensor(sosId, sensorID);
                LOGGER.info(String.format("retrait du capteur %s du service %s", sosId, sensorID));

                // ajout d'un capteur au SOS
                // http://localhost:8080/examind/API/SOS/sos1/sensor/import/NOAAShipTrackWTEC_5031_e90c_b602
                importSensor(sosId, sensorID, configurer);
                LOGGER.info(String.format("ajout du capteur %s au service %s", sosId, sensorID));
            }

        } catch (ConfigurationException ex) {
            LOGGER.warning(ex.getMessage());
        }
    }

    private List<String> generateSensorML(final int dataId) throws DataStoreException, ConfigurationException, SQLException, ProcessException {

        final Integer providerId = dataBusiness.getDataProvider(dataId);
        final DataProvider provider = DataProviders.getProvider(providerId);
        final List<ExtractionResult.ProcedureTree> procedures;
        final List<String> ids = new ArrayList<>();

        final ObservationStore store = SOSUtils.getObservationStore(provider);
        if (store != null) {
            procedures = store.getProcedures();

            // SensorML generation
            for (final ExtractionResult.ProcedureTree process : procedures) {
                generateSensorML(dataId, process, null);
                ids.add(process.id);
            }
        } else {
            throw new ProcessException("none observation store found", this);
        }
        return ids;
    }



    private void generateSensorML(final int dataID, final ExtractionResult.ProcedureTree process, final String parentID) throws SQLException, ConfigurationException {

        final Properties prop = new Properties();
        prop.put("id",         process.id);
        if (process.spatialBound.dateStart != null) {
            prop.put("beginTime",  process.spatialBound.dateStart);
        }
        if (process.spatialBound.dateEnd != null) {
            prop.put("endTime",    process.spatialBound.dateEnd);
        }
        if (process.spatialBound.minx != null) {
            prop.put("longitude",  process.spatialBound.minx);
        }
        if (process.spatialBound.miny != null) {
            prop.put("latitude",   process.spatialBound.miny);
        }
        prop.put("phenomenon", process.fields);

        Sensor sensor = sensorBusiness.getSensor(process.id);
        if (sensor == null) {
            Integer providerID = sensorBusiness.getDefaultInternalProviderID();
            sensor = sensorBusiness.create(process.id, process.type, parentID, null, System.currentTimeMillis(), providerID);

        }

        final List<String> component = new ArrayList<>();
        for (final ExtractionResult.ProcedureTree child : process.children) {
            component.add(child.id);
            generateSensorML(dataID, child, process.id);
        }
        prop.put("component", component);
        final String sml = SensorMLGenerator.getTemplateSensorMLString(prop, process.type);

        // update sml
        sensorBusiness.updateSensorMetadata(sensor.getId(), sml);
        sensorBusiness.linkDataToSensor(dataID, sensor.getId());
    }

    private void importSensor(final String id, final String sensorID, final SOSConfigurer configurer) throws ConfigurationException{
        final Sensor sensor               = sensorBusiness.getSensor(sensorID);
        final List<Sensor> sensorChildren = sensorBusiness.getChildren(sensor.getParent());
        final List<Integer> dataProviders = sensorBusiness.getLinkedDataProviderIds(sensor.getId());
        final List<String> sensorIds      = new ArrayList<>();

        sensorBusiness.addSensorToSOS(id, sensorID);
        sensorIds.add(sensorID);

        //import sensor children
        for (Sensor child : sensorChildren) {
            dataProviders.addAll(sensorBusiness.getLinkedDataProviderIds(child.getId()));
            sensorBusiness.addSensorToSOS(id, child.getIdentifier());
            sensorIds.add(child.getIdentifier());
        }

        // look for provider ids (remove doublon)
        final Set<Integer> providerIDs = new HashSet<>();
        for (Integer dataProvider : dataProviders) {
            providerIDs.add(dataProvider);
        }

        // import observations
        for (Integer providerId : providerIDs) {
            final DataProvider provider;
            try {
                provider = DataProviders.getProvider(providerId);
                final ObservationStore store = SOSUtils.getObservationStore(provider);
                final ExtractionResult result;
                if (store != null) {
                    result = store.getResults(sensorID, sensorIds);

                    // update sensor location
                    for (ExtractionResult.ProcedureTree process : result.procedures) {
                        writeProcedures(id, process, null, configurer);
                    }

                    // import in O&M database
                    configurer.importObservations(id, result.observations, result.phenomenons);
                } else {
                    LOGGER.info("Failure : Available only on Observation provider (and netCDF coverage) for now");
                }
            } catch (DataStoreException ex) {
                LOGGER.warning(ex.getMessage());
            }
        }
    }


    private static void writeProcedures(final String id, final ExtractionResult.ProcedureTree process, final String parent, final SOSConfigurer configurer) throws ConfigurationException {
        final AbstractGeometryType geom = (AbstractGeometryType) process.spatialBound.getGeometry("2.0.0");
        configurer.writeProcedure(id, process.id, geom, parent, process.type);
        for (ExtractionResult.ProcedureTree child : process.children) {
            writeProcedures(id, child, process.id, configurer);
        }
    }

    private static DataCustomConfiguration.Type buildDatastoreConfiguration(DataStoreProvider factory, String category, String tag) {
        final String id    = factory.getOpenParameters().getName().getCode();
        final String title = String.valueOf(factory.getShortName());
        String description = null;
        if (factory.getOpenParameters().getDescription() != null) {
            description = String.valueOf(factory.getOpenParameters().getDescription());
        }
        final  DataCustomConfiguration.Property property = toDataStorePojo(factory.getOpenParameters());
        return new DataCustomConfiguration.Type(id, title, category, tag, description, property);
    }


    private static DataCustomConfiguration.Property toDataStorePojo(GeneralParameterDescriptor desc){
        final DataCustomConfiguration.Property prop = new DataCustomConfiguration.Property();
        prop.setId(desc.getName().getCode());
        if(desc.getDescription()!=null) prop.setDescription(String.valueOf(desc.getDescription()));
        prop.setOptional(desc.getMinimumOccurs()==0);

        if(desc instanceof ParameterDescriptorGroup){
            final ParameterDescriptorGroup d = (ParameterDescriptorGroup)desc;
            for(GeneralParameterDescriptor child : d.descriptors()){
                prop.getProperties().add(toDataStorePojo(child));
            }
        }else if(desc instanceof ParameterDescriptor){
            final ParameterDescriptor d = (ParameterDescriptor)desc;
            final Object defaut = d.getDefaultValue();
            if(defaut!=null && DataCustomConfiguration.MARSHALLABLE.contains(defaut.getClass())){
                prop.setValue(defaut);
            }
            prop.setType(d.getValueClass().getSimpleName());
        }

        return prop;
    }
}
