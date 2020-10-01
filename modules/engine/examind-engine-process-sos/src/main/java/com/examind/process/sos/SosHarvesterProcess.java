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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.apache.sis.storage.DataStoreProvider;
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
import org.constellation.dto.importdata.ResourceStoreAnalysisV3;
import org.constellation.dto.process.ServiceProcessReference;
import org.constellation.exception.ConfigurationException;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.provider.DataProvider;
import org.constellation.provider.DataProviders;
import org.geotoolkit.data.csv.CSVProvider;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.constellation.dto.service.config.sos.ExtractionResult;
import org.geotoolkit.storage.DataStores;
import org.geotoolkit.util.StringUtilities;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.beans.factory.annotation.Autowired;
import static com.examind.process.sos.SosHarvesterProcessDescriptor.*;
import com.examind.sensor.component.SensorServiceBusiness;
import com.google.common.base.Objects;
import java.net.URI;
import java.util.Collections;
import org.apache.sis.internal.storage.query.SimpleQuery;
import org.apache.sis.internal.system.DefaultFactories;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.dto.SensorReference;
import org.constellation.dto.importdata.FileBean;
import org.constellation.dto.importdata.ResourceAnalysisV3;
import org.constellation.dto.service.ServiceComplete;
import org.constellation.dto.service.config.sos.ProcedureTree;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.provider.ObservationProvider;
import org.geotoolkit.observation.xml.AbstractObservation;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.observation.Phenomenon;
import org.opengis.observation.sampling.SamplingFeature;

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
    private IServiceBusiness serviceBusiness;

    @Autowired
    private IProviderBusiness providerBusiness;

    @Autowired
    private SensorServiceBusiness sensorServBusiness;

    public SosHarvesterProcess(final ProcessDescriptor desc, final ParameterValueGroup input) {
        super(desc,input);
    }

    @Override
    protected void execute() throws ProcessException {
        LOGGER.info("executiing sos insertion process");

        /*
        0- Paramètres fixés
        =================*/

        final String storeId = inputParameters.getValue(STORE_ID);
        final String format = inputParameters.getValue(FORMAT);
        final int userId = 1; // admin //assertAuthentificated(req);
        final String valueColumn = inputParameters.getValue(VALUE_COLUMN);
        final String codeColumn = inputParameters.getValue(CODE_COLUMN);
        final String typeColumn = inputParameters.getValue(TYPE_COLUMN);

        /*
        1- Récupération des paramètres du process
        =======================================*/

        final String sourceFolderStr = inputParameters.getValue(DATA_FOLDER);
        final String user = inputParameters.getValue(USER);
        final String pwd  = inputParameters.getValue(PWD);
        final boolean remoteRead = inputParameters.getValue(REMOTE_READ);
        final boolean extractUom = inputParameters.getValue(EXTRACT_UOM);


        final List<ServiceProcessReference> services = new ArrayList<>();
        for (GeneralParameterValue param : inputParameters.values()) {
            if (param.getDescriptor().getName().getCode().equals(SERVICE_ID.getName().getCode())) {
                services.add((ServiceProcessReference) ((ParameterValue)param).getValue());
            }
        }

        final String datasetIdentifier = inputParameters.getValue(DATASET_IDENTIFIER);
        final String procedureId = inputParameters.getValue(PROCEDURE_ID);
        final String procedureColumn = inputParameters.getValue(PROCEDURE_COLUMN);
        final boolean removePrevious = inputParameters.getValue(REMOVE_PREVIOUS);

        final String separator = inputParameters.getValue(SEPARATOR);
        final String mainColumn = inputParameters.getValue(MAIN_COLUMN);
        final String dateColumn = inputParameters.getValue(DATE_COLUMN);
        final String dateFormat = inputParameters.getValue(DATE_FORMAT);
        final String longitudeColumn = inputParameters.getValue(LONGITUDE_COLUMN);
        final String latitudeColumn = inputParameters.getValue(LATITUDE_COLUMN);
        final String foiColumn = inputParameters.getValue(FOI_COLUMN);
        final String observationType = inputParameters.getValue(OBS_TYPE);

        // prepare the results
        int nbFileInserted = 0;
        int nbObsInserted  = 0;

        final List<String> measureColumns = new ArrayList<>();
        for (GeneralParameterValue param : inputParameters.values()) {
            if (param.getDescriptor().getName().getCode().equals(MEASURE_COLUMNS.getName().getCode())) {
                measureColumns.add(((ParameterValue)param).stringValue());
            }
        }

        /*
        2- Détermination des données à importer
        =====================================*/
        final URI dataUri = URI.create(sourceFolderStr);

        final int dsId;
        DataSource ds = datasourceBusiness.getByUrl(sourceFolderStr);
        if (ds == null) {
            LOGGER.info("Creating new datasource");

            ds = new DataSource();
            ds.setType(dataUri.getScheme());
            ds.setUrl(sourceFolderStr);
            ds.setStoreId(storeId);
            ds.setUsername(user);
            ds.setPwd(pwd);
            ds.setFormat(format);
            ds.setPermanent(Boolean.TRUE);
            ds.setReadFromRemote(remoteRead);
            dsId = datasourceBusiness.create(ds);
            ds = datasourceBusiness.getDatasource(dsId);
        } else {
            LOGGER.info("Using already created datasource");
            dsId = ds.getId();
        }

        // remove previous integration
        if (removePrevious) {
            LOGGER.info("Removing previous integration");
            try {
                Set<Integer> providers = new HashSet<>();
                List<DataSourceSelectedPath> paths = datasourceBusiness.getSelectedPath(ds, Integer.MAX_VALUE);
                for (DataSourceSelectedPath path : paths) {
                    if (path.getProviderId() != null && path.getProviderId() != -1) {
                        providers.add(path.getProviderId());
                    }
                }

                // remove data
                Set<SensorReference> sensors = new HashSet<>();
                for (Integer pid : providers) {
                    for (Integer dataId : providerBusiness.getDataIdsFromProviderId(pid)) {
                        sensors.addAll(sensorBusiness.getByDataId(dataId));
                    }
                    providerBusiness.removeProvider(pid);
                }

                // remove sensors
                for (SensorReference sid : sensors) {

                    // unlink from SOS
                    for (Integer service : sensorBusiness.getLinkedServiceIds(sid.getId())) {
                        sensorServBusiness.removeSensor(service, sid.getIdentifier());
                    }

                    // remove sensor
                    sensorBusiness.delete(sid.getId());
                }

                datasourceBusiness.clearSelectedPaths(dsId);

                // update datasource
                ds.setReadFromRemote(remoteRead);
                ds.setStoreId(storeId);
                ds.setUsername(user);
                ds.setPwd(pwd);
                ds.setFormat(format);
                datasourceBusiness.update(ds);

            } catch (ConstellationException ex) {
                throw new ProcessException("Error while removing previous insertion.", this, ex);
            }
        }

        final String ext = storeId.equals("observationCsvFile") || storeId.equals("observationCsvCoriolisFile") ? ".csv" : ".dbf";

        try {
            for (FileBean child : datasourceBusiness.exploreDatasource(dsId, "/")) {
                if (child.getName().endsWith(ext)) {
                    if (datasourceBusiness.getSelectedPath(ds, '/' + child.getName()) == null) {
                        datasourceBusiness.addSelectedPath(dsId, '/' + child.getName());
                    }
                }
            }
        } catch (ConstellationException e) {
            throw new ProcessException("Error occurs during directory browsing", this, e);
        }



        /*
        3- Imporation des données et génération des fichiers SensorML
        ===========================================================*/

        // http://localhost:8080/examind/API/internal/datas/store/observationFile
        final DataStoreProvider factory = DataStores.getProviderById(storeId);
        final List<Integer> dataToIntegrate = new ArrayList<>();

        if (factory != null) {
            final DataCustomConfiguration.Type storeParams = DataProviders.buildDatastoreConfiguration(factory, "observation-store", null);
            storeParams.setSelected(true);


            // http://localhost:8080/examind/API/datasources/106/analysisV3
            final ProviderConfiguration provConfig = new ProviderConfiguration(storeParams.getCategory(), storeParams.getId());
            storeParams.cleanupEmptyProperty();
            storeParams.propertyToMap(provConfig.getParameters());

            provConfig.getParameters().put(CSVProvider.SEPARATOR.getName().toString(), separator);
            provConfig.getParameters().put(FileParsingObservationStoreFactory.MAIN_COLUMN.getName().toString(), mainColumn);
            provConfig.getParameters().put(FileParsingObservationStoreFactory.DATE_COLUMN.getName().toString(), dateColumn);
            provConfig.getParameters().put(FileParsingObservationStoreFactory.DATE_FORMAT.getName().toString(), dateFormat);
            provConfig.getParameters().put(FileParsingObservationStoreFactory.LONGITUDE_COLUMN.getName().toString(), longitudeColumn);
            provConfig.getParameters().put(FileParsingObservationStoreFactory.LATITUDE_COLUMN.getName().toString(), latitudeColumn);
            provConfig.getParameters().put(FileParsingObservationStoreFactory.FOI_COLUMN.getName().toString(), foiColumn);
            provConfig.getParameters().put(FileParsingObservationStoreFactory.MEASURE_COLUMNS_SEPARATOR.getName().toString(), ",");
            provConfig.getParameters().put(FileParsingObservationStoreFactory.MEASURE_COLUMNS.getName().toString(), StringUtilities.toCommaSeparatedValues(measureColumns));
            provConfig.getParameters().put(FileParsingObservationStoreFactory.OBSERVATION_TYPE.getName().toString(), observationType);
            provConfig.getParameters().put(FileParsingObservationStoreFactory.PROCEDURE_ID.getName().toString(), procedureId);
            provConfig.getParameters().put(FileParsingObservationStoreFactory.EXTRACT_UOM.getName().toString(), Boolean.toString(extractUom));
            provConfig.getParameters().put(FileParsingObservationStoreFactory.PROCEDURE_COLUMN.getName().toString(), procedureColumn);
            provConfig.getParameters().put(FileParsingObservationStoreFactory.VALUE_COLUMN.getName().toString(), valueColumn);
            provConfig.getParameters().put(FileParsingObservationStoreFactory.CODE_COLUMN.getName().toString(), codeColumn);
            provConfig.getParameters().put(FileParsingObservationStoreFactory.TYPE_COLUMN.getName().toString(), typeColumn);

            try {
                datasourceBusiness.computeDatasourceStores(ds.getId(), false, storeId, true);


//                if (analyseDatasourceV3.getStores().isEmpty()) {
//                    throw new ProcessException("No CSV files detected", this);
//                }

                Integer datasetId = datasetBusiness.getDatasetId(datasetIdentifier);
                if (datasetId == null)  {
                    datasetId = datasetBusiness.createDataset(datasetIdentifier, null, null);
                }


                List<DataSourceSelectedPath> paths = datasourceBusiness.getSelectedPath(ds, Integer.MAX_VALUE);

                final boolean hidden = false; // true

                for (final DataSourceSelectedPath p : paths) {

                    switch (p.getStatus()) {
                        case "NO_DATA":
                        case "ERROR":
                            LOGGER.log(Level.INFO, "No data / Error in file: {0}", p.getPath());
                            break;
                        case "INTEGRATED":
                        case "COMPLETED":
                            LOGGER.log(Level.INFO, "File already integrated for file: {0}", p.getPath());
                            break;
                        case "REMOVED":
                            LOGGER.log(Level.INFO, "Removing data for file: {0}", p.getPath());
                            providerBusiness.removeProvider(p.getProviderId());
                            // TODO full removal
                            datasourceBusiness.removePath(ds, p.getPath());
                            break;
                        default:
                            LOGGER.log(Level.INFO, "Integrating data file: {0}", p.getPath());
                            ResourceStoreAnalysisV3 store = datasourceBusiness.treatDataPath(p, ds, provConfig, true, datasetId, userId);
                            for (ResourceAnalysisV3 resourceStore : store.getResources()) {
                                final DataBrief acceptData = dataBusiness.acceptData(resourceStore.getId(), userId, hidden);
                                dataBusiness.updateDataDataSetId(acceptData.getId(), datasetId);
                                dataToIntegrate.add(acceptData.getId());
                            }
                            nbFileInserted++;
                    }
                }

            } catch (ConstellationException ex) {
                LOGGER.warning(ex.getMessage());
                throw new ProcessException("Error while analysing the files", this, ex);
            } finally {
                datasourceBusiness.close(dsId);
            }
        }

        /*
        4- Publication des données correspondant à chaque SensorML sur le service SOS
        ===========================================================================*/

        try {
            for (final Integer dataId : dataToIntegrate) {

                List<Integer> ids = generateSensorML(dataId);

                // ajout d'un capteur au services
                for (Integer sensorID : ids) {
                    nbObsInserted = nbObsInserted + importSensor(services, sensorID, dataId);
                }
            }

        } catch (ConfigurationException | ConstellationStoreException | SQLException ex) {
            throw new ProcessException(ex.getMessage(), this, ex);
        }

        outputParameters.getOrCreate(SosHarvesterProcessDescriptor.OBSERVATION_INSERTED).setValue(nbObsInserted);
        outputParameters.getOrCreate(SosHarvesterProcessDescriptor.FILE_INSERTED).setValue(nbFileInserted);
    }

    private List<Integer> generateSensorML(final int dataId) throws ConstellationStoreException, ConfigurationException, SQLException, ProcessException {

        final Integer providerId    = dataBusiness.getDataProvider(dataId);
        final Integer smlProviderId = sensorBusiness.getDefaultInternalProviderID();
        final DataProvider provider = DataProviders.getProvider(providerId);
        final List<Integer> ids = new ArrayList<>();

        if (provider instanceof ObservationProvider) {
            final List<ProcedureTree> procedures = ((ObservationProvider)provider).getProcedureTrees(null, Collections.EMPTY_MAP);

            // SensorML generation
            for (final ProcedureTree process : procedures) {
                ids.add(sensorBusiness.generateSensorForData(dataId, process, smlProviderId, null));
            }
        } else {
            throw new ProcessException("none observation store found", this);
        }
        return ids;
    }

    private List<SamplingFeature> getFeatureOfInterestForProcedure(String procedure, ObservationProvider provider) throws ConstellationStoreException {
        final FilterFactory ff = DefaultFactories.forBuildin(FilterFactory.class);
        final SimpleQuery subquery = new SimpleQuery();
        final Filter filter = ff.equals(ff.property("procedure"), ff.literal(procedure));
        subquery.setFilter(filter);
        return provider.getFeatureOfInterest(subquery, Collections.singletonMap("version", "2.0.0"));
    }

    private int importSensor(final List<ServiceProcessReference> sosRefs, final Integer sensorID, final int dataId) throws ConfigurationException, ConstellationStoreException {
        int nbObservationInserted                   = 0;
        final Sensor sensor                         = sensorBusiness.getSensor(sensorID);
        final List<Sensor> sensorChildren           = sensorBusiness.getChildren(sensor.getParent());
        final List<ObservationProvider> treated     = new ArrayList<>();

        for (ServiceProcessReference sosRef : sosRefs) {
            final ObservationProvider omServiceProvider = getOMProvider(sosRef.getId());
            final List<String> alreadyLinked            = sensorBusiness.getLinkedSensorIdentifiers(sosRef.getId(), null);
            final Set<Phenomenon> existingPhenomenons   = new HashSet<>(omServiceProvider.getPhenomenon(new SimpleQuery(), Collections.singletonMap("version", "1.0.0")));
            final Set<SamplingFeature> existingFois     = new HashSet<>(getFeatureOfInterestForProcedure(sensor.getIdentifier(), omServiceProvider));
            final Set<Integer> providerIDs              = new HashSet<>();
            final List<String> sensorIds                = new ArrayList<>();

            providerIDs.add(dataBusiness.getDataProvider(dataId));

            // import main sensor
            if (!alreadyLinked.contains(sensor.getIdentifier())) {
                sensorBusiness.addSensorToService(sosRef.getId(), sensorID);
            }
             sensorIds.add(sensor.getIdentifier());

            //import sensor children
            for (Sensor child : sensorChildren) {
                providerIDs.addAll(sensorBusiness.getLinkedDataProviderIds(child.getId()));
                if (!alreadyLinked.contains(sensor.getIdentifier())) {
                    sensorBusiness.addSensorToService(sosRef.getId(), child.getId());
                }
                sensorIds.add(child.getIdentifier());
            }

            boolean alreadyInserted = false;
            for (ObservationProvider o : treated) {
                if (sameObservationProvider(o, omServiceProvider)) {
                    alreadyInserted = true;
                }
            }

            // import observations
            if (!alreadyInserted) {
                for (Integer providerId : providerIDs) {
                    final DataProvider provider;
                    try {
                        provider = DataProviders.getProvider(providerId);
                        if (provider instanceof ObservationProvider) {
                            ObservationProvider omProvider = (ObservationProvider) provider;
                            final ExtractionResult result = omProvider.extractResults(sensor.getIdentifier(), sensorIds, existingPhenomenons, existingFois);

                            existingPhenomenons.addAll(result.getPhenomenons());
                            existingFois.addAll(result.getFeatureOfInterest());

                            // update sensor location
                            for (ProcedureTree process : result.getProcedures()) {
                                sensorServBusiness.writeProcedure(sosRef.getId(), process);
                            }
                            result.getObservations().stream().forEach(obs -> ((AbstractObservation)obs).setName(null));

                            // import in O&M database
                            sensorServBusiness.importObservations(sosRef.getId(), result.getObservations(), result.getPhenomenons());
                            nbObservationInserted = nbObservationInserted + result.getObservations().size();
                        } else {
                            LOGGER.info("Failure : Available only on Observation provider for now");
                        }
                    } catch (ConstellationStoreException ex) {
                        LOGGER.warning(ex.getMessage());
                    }
                }
                treated.add(omServiceProvider);
            }
            LOGGER.info(String.format("ajout du capteur %s au service %s", sensorID, sosRef.getName()));
            serviceBusiness.restart(sosRef.getId());
        }
        return nbObservationInserted;
    }

    protected ObservationProvider getOMProvider(final Integer serviceID) throws ConfigurationException {
        final List<Integer> providers = serviceBusiness.getLinkedProviders(serviceID);
        for (Integer providerID : providers) {
            final DataProvider p = DataProviders.getProvider(providerID);
            if(p instanceof ObservationProvider){
                // TODO for now we only take one provider by type
                return (ObservationProvider) p;
            }
        }
        throw new ConfigurationException("there is no OM provider linked to this ID:" + serviceID);
    }

    /**
     * hack method to compare two observation provider on the same datasource (should not be allowed in the future.)
     */
    private boolean sameObservationProvider(ObservationProvider op1, ObservationProvider op2) {
        final ParameterValueGroup source1 = op1.getSource();
        final ParameterValueGroup source2 = op2.getSource();
        ParameterValueGroup fconfig1 = source1.groups("choice").get(0).groups("SOSDBParameters").get(0);
        ParameterValueGroup fconfig2 = source2.groups("choice").get(0).groups("SOSDBParameters").get(0);

        final Object host1     = fconfig1.parameter("host").getValue();
        final Object database1 = fconfig1.parameter("database").getValue();
        final Object schema1   = fconfig1.parameter("schema-prefix").getValue();

        final Object host2     = fconfig2.parameter("host").getValue();
        final Object database2 = fconfig2.parameter("database").getValue();
        final Object schema2   = fconfig2.parameter("schema-prefix").getValue();

        return Objects.equal(host1,     host2)     &&
               Objects.equal(database1, database2) &&
               Objects.equal(schema1,   schema2);

    }
}
