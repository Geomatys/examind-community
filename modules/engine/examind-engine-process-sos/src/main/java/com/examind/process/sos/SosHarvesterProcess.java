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

import com.examind.store.observation.FileParsingObservationStoreFactory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import org.constellation.dto.service.config.sos.ObservationDataset;
import org.geotoolkit.storage.DataStores;
import org.geotoolkit.util.StringUtilities;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.beans.factory.annotation.Autowired;
import static com.examind.process.sos.SosHarvesterProcessDescriptor.*;
import com.examind.sensor.component.SensorServiceBusiness;
import static com.examind.store.observation.FileParsingUtils.equalsGeom;
import java.util.Objects;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.constellation.business.IDatasourceBusiness.AnalysisState;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.dto.SensorReference;
import org.constellation.dto.importdata.FileBean;
import org.constellation.dto.importdata.ResourceAnalysisV3;
import org.constellation.dto.importdata.StoreFormat;
import org.constellation.dto.service.config.sos.ProcedureDataset;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.provider.ObservationProvider;
import org.geotoolkit.observation.model.CompositePhenomenon;
import org.geotoolkit.observation.query.DatasetQuery;
import org.geotoolkit.observation.query.ObservedPropertyQuery;
import org.geotoolkit.observation.query.SamplingFeatureQuery;
import org.opengis.observation.Observation;
import org.opengis.observation.Phenomenon;
import org.opengis.observation.sampling.SamplingFeature;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptor;

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

    private double progress;

    public SosHarvesterProcess(final ProcessDescriptor desc, final ParameterValueGroup input) {
        super(desc,input);
    }

    @Override
    protected void execute() throws ProcessException {
        LOGGER.info("executing sos insertion process");

        this.progress = 0.0;

        /*
        0- Paramètres fixés
        =================*/

        final String storeId = inputParameters.getValue(STORE_ID);
        final String format = inputParameters.getValue(FORMAT);

        /*
        1- Récupération des paramètres du process
        =======================================*/

        final String sourceFolderStr = inputParameters.getValue(DATA_FOLDER);
        final String user = inputParameters.getValue(USER);
        final String pwd  = inputParameters.getValue(PWD);
        final boolean remoteRead = inputParameters.getValue(REMOTE_READ);

        final List<ServiceProcessReference> services = new ArrayList<>();
        for (GeneralParameterValue param : inputParameters.values()) {
            if (param.getDescriptor().getName().getCode().equals(SERVICE_ID.getName().getCode())) {
                services.add((ServiceProcessReference) ((ParameterValue)param).getValue());
            }
        }

        final String datasetIdentifier = inputParameters.getValue(DATASET_IDENTIFIER);
        final String procedureId = inputParameters.getValue(THING_ID);
        final String procedureColumn = inputParameters.getValue(THING_COLUMN);
        final String procedureNameColumn = inputParameters.getValue(THING_NAME_COLUMN);
        final String procedureDescColumn = inputParameters.getValue(THING_DESC_COLUMN);
        final String procedureRegex = inputParameters.getValue(THING_REGEX);
        final boolean removePrevious = inputParameters.getValue(REMOVE_PREVIOUS);
        final boolean directColumnIndex = inputParameters.getValue(DIRECT_COLUMN_INDEX);
        final boolean noHeader = inputParameters.getValue(NO_HEADER);
        final boolean laxHeader = inputParameters.getValue(LAX_HEADER);

        final String separator = inputParameters.getValue(SEPARATOR);
        final String charQuote = inputParameters.getValue(CHARQUOTE);
        final List<String> mainColumns = getMultipleValues(MAIN_COLUMN);
        final List<String> dateColumns = getMultipleValues(DATE_COLUMN);
        final String dateFormat = inputParameters.getValue(DATE_FORMAT);
        final String longitudeColumn = inputParameters.getValue(LONGITUDE_COLUMN);
        final String latitudeColumn = inputParameters.getValue(LATITUDE_COLUMN);
        final String foiColumn = inputParameters.getValue(FOI_COLUMN);
        final String observationType = inputParameters.getValue(OBS_TYPE);
        final String zColumn    = inputParameters.getValue(Z_COLUMN);
        final List<String> qualityColumns = getMultipleValues(QUALITY_COLUMN);
        final List<String> qualityColumnsIds = getMultipleValues(QUALITY_COLUMN_ID);
        final List<String> qualityColumnsTypes = getMultipleValues(QUALITY_COLUMN_TYPE);

        // csv-flat special
        final String typeColumn  = inputParameters.getValue(TYPE_COLUMN);
        final String valueColumn = inputParameters.getValue(RESULT_COLUMN);
        final String obsPropId   = inputParameters.getValue(OBS_PROP_ID);
        final List<String> obsPropColumns     = getMultipleValues(OBS_PROP_COLUMN);
        final List<String> obsPropColumnTypes = getMultipleValues(OBS_PROP_COLUMN_TYPE);
        final String obsPropRegex = inputParameters.getValue(OBS_PROP_REGEX);
        final String obsPropName  = inputParameters.getValue(OBS_PROP_NAME);
        final List<String> ObsPropNameColumns = getMultipleValues(OBS_PROP_NAME_COLUMN);
        final List<String> obsPropFilterColumns = getMultipleValues(OBS_PROP_COLUMNS_FILTER);

        final String uomColumn   = inputParameters.getValue(UOM_COLUMN);
        final String uomRegex    = inputParameters.getValue(UOM_REGEX);

        // prepare the results
        int nbFileInserted = 0;
        int nbObsInserted  = 0;

        if (observationType == null && !storeId.equals("observationCsvFlatFile")) {
            throw new ProcessException("The observation type can't be null except for csvFlat store with type column", this);
        }

        /*
        2- Détermination des données à importer
        =====================================*/
        final URI dataUri = URI.create(sourceFolderStr);

        final int dsId;
        DataSource ds;
        List<DataSource> dss = datasourceBusiness.search(sourceFolderStr, storeId, format);
        if (dss.isEmpty()) {
            try {
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
            } catch (ConstellationException ex) {
                throw new ProcessException("Error while creating datasource", this, ex);
            }
        } else {
            LOGGER.info("Using already created datasource");
            if (dss.size() > 1) {
                LOGGER.warning("Multiple datasource found. using the first we found");
            }
            ds   = dss.get(0);
            dsId = ds.getId();
        }

        // remove previous integration
        if (removePrevious) {
            fireAndLog("Removing previous integration", 0);
            try {
                Set<Integer> providers = new HashSet<>();
                List<DataSourceSelectedPath> paths = datasourceBusiness.getSelectedPath(dsId, Integer.MAX_VALUE);
                for (DataSourceSelectedPath path : paths) {
                    if (path.getProviderId() != null && path.getProviderId() != -1) {
                        providers.add(path.getProviderId());
                    }
                }

                // remove data
                Set<Integer> dataIds = new HashSet<>();
                for (Integer pid : providers) {
                    dataIds.addAll(providerBusiness.getDataIdsFromProviderId(pid));
                }

                for (Integer dataId : dataIds) {
                    sensorServBusiness.removeDataObservationsFromServices(dataId);
                    dataBusiness.removeData(dataId, false);
                }

                datasourceBusiness.clearSelectedPaths(dsId);
                datasourceBusiness.clearPaths(dsId);

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

        try {
            datasourceBusiness.updateDatasourceAnalysisState(dsId,  AnalysisState.NOT_STARTED.name());
            datasourceBusiness.computeDatasourceStores(dsId, false, storeId, true, false);
            datasourceBusiness.recordSelectedPath(dsId, true);
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
            provConfig.getParameters().put(FileParsingObservationStoreFactory.CHARQUOTE.getName().toString(), charQuote);
            provConfig.getParameters().put(FileParsingObservationStoreFactory.MAIN_COLUMN.getName().toString(), StringUtilities.toCommaSeparatedValues(mainColumns));
            provConfig.getParameters().put(FileParsingObservationStoreFactory.DATE_COLUMN.getName().toString(), StringUtilities.toCommaSeparatedValues(dateColumns));
            provConfig.getParameters().put(FileParsingObservationStoreFactory.DATE_FORMAT.getName().toString(), dateFormat);
            provConfig.getParameters().put(FileParsingObservationStoreFactory.LONGITUDE_COLUMN.getName().toString(), longitudeColumn);
            provConfig.getParameters().put(FileParsingObservationStoreFactory.LATITUDE_COLUMN.getName().toString(), latitudeColumn);
            provConfig.getParameters().put(FileParsingObservationStoreFactory.FOI_COLUMN.getName().toString(), foiColumn);
            provConfig.getParameters().put(FileParsingObservationStoreFactory.OBS_PROP_FILTER_COLUMN.getName().toString(), StringUtilities.toCommaSeparatedValues(obsPropFilterColumns));
            provConfig.getParameters().put(FileParsingObservationStoreFactory.OBSERVATION_TYPE.getName().toString(), observationType);
            provConfig.getParameters().put(FileParsingObservationStoreFactory.PROCEDURE_ID.getName().toString(), procedureId);
            provConfig.getParameters().put(FileParsingObservationStoreFactory.UOM_REGEX.getName().toString(), uomRegex);
            provConfig.getParameters().put(FileParsingObservationStoreFactory.PROCEDURE_COLUMN.getName().toString(), procedureColumn);
            provConfig.getParameters().put(FileParsingObservationStoreFactory.PROCEDURE_NAME_COLUMN.getName().toString(), procedureNameColumn);
            provConfig.getParameters().put(FileParsingObservationStoreFactory.PROCEDURE_DESC_COLUMN.getName().toString(), procedureDescColumn);
            provConfig.getParameters().put(FileParsingObservationStoreFactory.PROCEDURE_REGEX.getName().toString(), procedureRegex);
            provConfig.getParameters().put(FileParsingObservationStoreFactory.RESULT_COLUMN.getName().toString(), valueColumn);
            provConfig.getParameters().put(FileParsingObservationStoreFactory.OBS_PROP_ID.getName().toString(), obsPropId);
            provConfig.getParameters().put(FileParsingObservationStoreFactory.OBS_PROP_COLUMN.getName().toString(), StringUtilities.toCommaSeparatedValues(obsPropColumns));
            provConfig.getParameters().put(FileParsingObservationStoreFactory.OBS_PROP_COLUMN_TYPE.getName().toString(), StringUtilities.toCommaSeparatedValues(obsPropColumnTypes));
            provConfig.getParameters().put(FileParsingObservationStoreFactory.OBS_PROP_NAME.getName().toString(), obsPropName);
            provConfig.getParameters().put(FileParsingObservationStoreFactory.OBS_PROP_NAME_COLUMN.getName().toString(), StringUtilities.toCommaSeparatedValues(ObsPropNameColumns));
            provConfig.getParameters().put(FileParsingObservationStoreFactory.OBS_PROP_REGEX.getName().toString(), obsPropRegex);
            provConfig.getParameters().put(FileParsingObservationStoreFactory.QUALITY_COLUMN.getName().toString(), StringUtilities.toCommaSeparatedValues(qualityColumns));
            provConfig.getParameters().put(FileParsingObservationStoreFactory.QUALITY_COLUMN_ID.getName().toString(), StringUtilities.toCommaSeparatedValues(qualityColumnsIds));
            provConfig.getParameters().put(FileParsingObservationStoreFactory.QUALITY_COLUMN_TYPE.getName().toString(), StringUtilities.toCommaSeparatedValues(qualityColumnsTypes));
            provConfig.getParameters().put(FileParsingObservationStoreFactory.TYPE_COLUMN.getName().toString(), typeColumn);
            provConfig.getParameters().put(FileParsingObservationStoreFactory.Z_COLUMN.getName().toString(), zColumn);
            provConfig.getParameters().put(FileParsingObservationStoreFactory.UOM_COLUMN.getName().toString(), uomColumn);
            provConfig.getParameters().put(FileParsingObservationStoreFactory.FILE_MIME_TYPE.getName().toString(), format);
            provConfig.getParameters().put(FileParsingObservationStoreFactory.DIRECT_COLUMN_INDEX.getName().toString(), Boolean.toString(directColumnIndex));
            provConfig.getParameters().put(FileParsingObservationStoreFactory.NO_HEADER.getName().toString(), Boolean.toString(noHeader));
            provConfig.getParameters().put(FileParsingObservationStoreFactory.LAX_HEADER.getName().toString(), Boolean.toString(laxHeader));
            
            final Map<String, Object> extraStoreParams = inputParameters.getValue(EXTRA_STORE_PARAMETERS);
            if (extraStoreParams != null) {
                for (Entry<String, Object> extraStoreParam : extraStoreParams.entrySet()) {
                    provConfig.getParameters().put(extraStoreParam.getKey(), extraStoreParam.getValue().toString());
                }
            }
            
            try {
                Integer datasetId = datasetBusiness.getDatasetId(datasetIdentifier);
                if (datasetId == null)  {
                    datasetId = datasetBusiness.createDataset(datasetIdentifier, null, null);
                }

                List<DataSourceSelectedPath> paths = datasourceBusiness.getSelectedPath(dsId, Integer.MAX_VALUE);

                for (final DataSourceSelectedPath p : paths) {

                    switch (p.getStatus()) {
                        case "NO_DATA", "ERROR" -> {
                            fireAndLog("No data / Error in file: " + p.getPath(), 0);
                        }
                        case "INTEGRATED", "COMPLETED" -> {
                            fireAndLog("File already integrated for file: " + p.getPath(), 0);
                        }
                        case "REMOVED" -> {
                            fireAndLog("Removing data for file: " + p.getPath(), 0);
                            providerBusiness.removeProvider(p.getProviderId());
                            // TODO full removal
                            datasourceBusiness.removePath(dsId, p.getPath());
                        }
                        default -> {
                            // in the case of a null format, meaning that multiple format are accepted.
                            // we need to determine the format of each file
                            if (format == null) {
                                boolean formatFound = false;
                                Optional<FileBean> fb = datasourceBusiness.getAnalyzedPath(dsId, p.getPath());
                                if (fb.isPresent()) {
                                    List<StoreFormat> types = fb.get().getTypes();
                                    for (StoreFormat sf : types) {
                                        if (storeId.equals(sf.getStore())) {
                                            provConfig.getParameters().put(FileParsingObservationStoreFactory.FILE_MIME_TYPE.getName().toString(), sf.getFormat());
                                            formatFound = true;
                                            break;
                                        }
                                    }
                                }
                                if (!formatFound) {
                                    fireWarningOccurred("Unable to determine the format of file:" + p.getPath(), (float) this.progress, null);
                                    continue;
                                }
                            }
                            fireAndLog("Integrating data file: " + p.getPath(), 0);
                            dataToIntegrate.addAll(integratingDataFile(p, dsId, provConfig, datasetId));
                            nbFileInserted++;
                        }
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
        GeneralParameterDescriptor obsInDesc = outputParameters.getDescriptor().descriptor(SosHarvesterProcessDescriptor.GENERATE_DATA_IDS_NAME);
        try {
            double byData = 100.0 / dataToIntegrate.size();
            for (final Integer dataId : dataToIntegrate) {

                    int currentNbObs = importSensor(services, dataId, byData);
                    nbObsInserted = nbObsInserted + currentNbObs;
                    
                    ParameterValue pv = (ParameterValue) obsInDesc.createValue();
                    pv.setValue(dataId);
                    outputParameters.values().add(pv);
            }

            // reload service at the end
            for (ServiceProcessReference serv : services) {
                serviceBusiness.restart(serv.getId());
            }

        } catch (ConfigurationException | ConstellationStoreException ex) {
            throw new ProcessException(ex.getMessage(), this, ex);
        }

        outputParameters.getOrCreate(SosHarvesterProcessDescriptor.OBSERVATION_INSERTED).setValue(nbObsInserted);
        outputParameters.getOrCreate(SosHarvesterProcessDescriptor.FILE_INSERTED).setValue(nbFileInserted);
    }

    private List<Integer> integratingDataFile(DataSourceSelectedPath p, Integer dsid, ProviderConfiguration provConfig, Integer datasetId) throws ConstellationException {
        List<Integer> dataToIntegrate = new ArrayList<>();
        int userId = 1;
        ResourceStoreAnalysisV3 store = datasourceBusiness.treatDataPath(p, dsid, provConfig, true, datasetId, userId);
        for (ResourceAnalysisV3 resourceStore : store.getResources()) {
            final DataBrief acceptData = dataBusiness.acceptData(resourceStore.getId(), userId, false);
            dataBusiness.updateDataDataSetId(acceptData.getId(), datasetId);
            dataToIntegrate.add(acceptData.getId());
        }
        return dataToIntegrate;
    }

    private List<String> listSensorInData(final int dataId) throws ConstellationStoreException, ConfigurationException {

        final Integer providerId    = dataBusiness.getDataProvider(dataId);
        final DataProvider provider = DataProviders.getProvider(providerId);
        final List<String> ids = new ArrayList<>();

        if (provider instanceof ObservationProvider op) {
            final List<ProcedureDataset> procedures = op.getProcedureTrees(null);

            // SensorML generation
            for (final ProcedureDataset process : procedures) {
                ids.add(process.getId());
            }
        } else {
            throw new ConfigurationException("none observation store found");
        }
        return ids;
    }

    private int importSensor(final List<ServiceProcessReference> sosRefs, final int dataId, final double byData) throws ConfigurationException, ConstellationStoreException {
        Integer providerId = dataBusiness.getDataProvider(dataId);
        final DataProvider provider = DataProviders.getProvider(providerId);
        ObservationProvider omProvider;
        if (provider instanceof ObservationProvider) {
             omProvider = (ObservationProvider) provider;
        } else {
            throw new ConfigurationException("Failure : Available only on Observation provider for now");
        }
        int nbObsTotal                              = 0;
        final List<ObservationProvider> treated     = new ArrayList<>();
        final double byInsert                       = byData / sosRefs.size();

        for (ServiceProcessReference sosRef : sosRefs) {
            final ObservationProvider omServiceProvider = getOMProvider(sosRef.getId());
            final Set<Phenomenon> existingPhenomenons   = new HashSet<>(omServiceProvider.getPhenomenon(new ObservedPropertyQuery()));
            final Set<SamplingFeature> existingFois     = new HashSet<>(getFeatureOfInterest(omServiceProvider));

            boolean alreadyInserted = false;
            for (ObservationProvider o : treated) {
                if (sameObservationProvider(o, omServiceProvider)) {
                    alreadyInserted = true;
                }
            }

            // import observations
            if (!alreadyInserted) {
                try {
                    final ObservationDataset result = omProvider.extractResults(new DatasetQuery());
                    reuseExistingPhenomenonAndFOI(result, existingPhenomenons, existingFois);
                    if (!result.getObservations().isEmpty()) {

                        // generate sensor
                        for (ProcedureDataset process : result.getProcedures()) {
                            sensorServBusiness.writeProcedure(sosRef.getId(), process);
                            Integer sid =  sensorBusiness.generateSensor(process, null, null, dataId);
                            sensorBusiness.addSensorToService(sosRef.getId(), sid);
                        }
                        result.getObservations().stream().forEach(obs -> ((org.geotoolkit.observation.model.Observation)obs).setName(null));

                        // import in O&M database
                        List<Phenomenon> modelPhens = result.getPhenomenons().stream().map(phen -> (Phenomenon) phen).toList();
                        sensorServBusiness.importObservations(sosRef.getId(), result.getObservations(), modelPhens);
                        nbObsTotal = nbObsTotal + result.getObservations().size();
                        fireAndLog("insertion dans le service " + sosRef.getName(), byInsert);
                    }
                } catch (ConstellationStoreException ex) {
                    LOGGER.warning(ex.getMessage());
                }
                treated.add(omServiceProvider);
            } else {
                List<String> ids = listSensorInData(dataId);
                for (String sensorID : ids) {
                    final Sensor sensor = sensorBusiness.getSensor(sensorID);
                    if (sensor != null) {
                        sensorBusiness.addSensorToService(sosRef.getId(), sensor.getId());
                    }
                }
            }
        }
        return nbObsTotal;
    }

    private void reuseExistingPhenomenonAndFOI(final ObservationDataset result, final Set<Phenomenon> existingPhenomenons, final Set<SamplingFeature> existingFois) {
        /**
         * look for an already existing (composite) phenomenon to use instead of inserting a new one
         */
        Map<String, org.geotoolkit.observation.model.Phenomenon> phenomenonToReplace = new HashMap<>();
        List<Phenomenon> phenToRemove = new ArrayList<>();
        for (Phenomenon newPhen : result.getPhenomenons()) {
            if (newPhen instanceof CompositePhenomenon newCompo) {
                for (org.opengis.observation.Phenomenon existingPhen : existingPhenomenons) {
                    if (existingPhen instanceof CompositePhenomenon existingCphen) {
                        if (Objects.equals(existingCphen.getComponent(), newCompo.getComponent())) {
                            String newCompoId = newCompo.getId();
                            phenomenonToReplace.put(newCompoId, existingCphen);
                            phenToRemove.add(newPhen);
                            break;
                        }
                    }
                }
            }
        }
        result.getPhenomenons().addAll(phenomenonToReplace.values());
        result.getPhenomenons().removeAll(phenToRemove);

        /**
         * look for an already existing sampling feature to use instead of inserting a new one.
         * we look for an equal geometry (only work for point)
         */
        Map<String, org.geotoolkit.observation.model.SamplingFeature> featureToReplace = new HashMap<>();
        List<SamplingFeature> foiToRemove = new ArrayList<>();
        for (SamplingFeature newFoi : result.getFeatureOfInterest()) {
            if (newFoi instanceof org.geotoolkit.observation.model.SamplingFeature newSFoi) {
                for (SamplingFeature existingFoi : existingFois) {
                    if (existingFoi instanceof org.geotoolkit.observation.model.SamplingFeature existingSFoi) {
                        if (existingSFoi.getGeometry() != null && newSFoi.getGeometry() != null && equalsGeom(newSFoi.getGeometry(), existingSFoi.getGeometry())) {
                            featureToReplace.put(newSFoi.getId(), existingSFoi);
                            foiToRemove.add(newFoi);
                            break;
                        }
                    }
                }
            }
        }
        result.getFeatureOfInterest().addAll(featureToReplace.values());
        result.getFeatureOfInterest().removeAll(foiToRemove);


        // replace phenomenons / feature of interests in each observation
        for (Observation obs : result.getObservations()) {
            if (obs instanceof org.geotoolkit.observation.model.Observation aobs) {
                if (obs.getObservedProperty() instanceof CompositePhenomenon cphen) {
                    String phenId = cphen.getId();
                    org.geotoolkit.observation.model.Phenomenon existPhen = phenomenonToReplace.get(phenId);
                    if (existPhen != null) {
                        aobs.setObservedProperty(existPhen);
                    }
                }
                if (aobs.getFeatureOfInterest() != null) {
                    String foiId = aobs.getFeatureOfInterest().getId();
                    org.geotoolkit.observation.model.SamplingFeature existFoi = featureToReplace.get(foiId);
                    if (existFoi != null) {
                        aobs.setFeatureOfInterest(existFoi);
                    }
                }
            } else {
                throw new IllegalArgumentException("Unexpected observation implementation:" + obs.getClass().getName());
            }
        }
    }

    private List<SamplingFeature> getFeatureOfInterest(ObservationProvider provider) throws ConstellationStoreException {
        return provider.getFeatureOfInterest(new SamplingFeatureQuery());
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
        return Objects.equals(op1.getDatasourceKey(), op2.getDatasourceKey());
    }

    private void fireAndLog(final String msg, double progress) {
        LOGGER.info(msg);
        this.progress = this.progress + progress;
        fireProgressing(msg, (float) this.progress, false);
    }

    private List<String> getMultipleValues(ParameterDescriptor<String> desc) {
        List<String> results = new ArrayList<>();
        for (GeneralParameterValue param : inputParameters.values()) {
            if (param instanceof ParameterValue pval) {
                if (param.getDescriptor().getName().getCode().equals(desc.getName().getCode())) {
                    results.add(pval.stringValue());
                }
            }
        }
        return results;
    }

}
