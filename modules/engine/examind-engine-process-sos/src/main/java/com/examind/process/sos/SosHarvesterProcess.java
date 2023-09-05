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
import org.constellation.dto.DataCustomConfiguration;
import org.constellation.dto.DataSource;
import org.constellation.dto.DataSourceSelectedPath;
import org.constellation.dto.ProviderConfiguration;
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
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.beans.factory.annotation.Autowired;
import static com.examind.process.sos.SosHarvesterProcessDescriptor.*;
import static com.examind.process.sos.SosHarvesterUtils.*;
import static com.examind.store.observation.FileParsingUtils.equalsGeom;
import java.util.Objects;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.logging.Level;
import org.constellation.business.IDatasourceBusiness.AnalysisState;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.ISensorServiceBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.dto.importdata.FileBean;
import org.constellation.dto.importdata.StoreFormat;
import org.constellation.dto.service.config.sos.ProcedureDataset;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationStoreException;
import static org.constellation.process.ProcessUtils.addMultipleValues;
import org.constellation.provider.ObservationProvider;
import org.geotoolkit.observation.model.CompositePhenomenon;
import org.geotoolkit.observation.query.DatasetQuery;
import org.geotoolkit.observation.query.ObservedPropertyQuery;
import org.geotoolkit.observation.query.SamplingFeatureQuery;
import org.opengis.observation.Observation;
import org.opengis.observation.Phenomenon;
import org.opengis.observation.sampling.SamplingFeature;
import static org.constellation.process.ProcessUtils.getMultipleValues;

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
    private ISensorServiceBusiness sensorServBusiness;

    private double progress;

    public SosHarvesterProcess(final ProcessDescriptor desc, final ParameterValueGroup input) {
        super(desc,input);
    }

    @Override
    protected void execute() throws ProcessException {
        LOGGER.info("executing sos insertion process");

        this.progress = 0.0;

        /*
        =======================================
        1- Process parameters extraction.
        =======================================*/

        final String storeId = inputParameters.getValue(STORE_ID);
        final String format = inputParameters.getValue(FORMAT);

        final String sourceFolderStr = inputParameters.getValue(DATA_FOLDER);
        final String user = inputParameters.getValue(USER);
        final String pwd  = inputParameters.getValue(PWD);
        final boolean remoteRead = inputParameters.getValue(REMOTE_READ);
        final boolean generateMetadata = inputParameters.getValue(GENERATE_METADATA);

        final boolean checkFiles = inputParameters.getValue(CHECK_FILE);

        final List<ServiceProcessReference> serviceRefs = getMultipleValues(inputParameters, SERVICE_ID);

        final String datasetIdentifier = inputParameters.getValue(DATASET_IDENTIFIER);
        final String procedureId = inputParameters.getValue(THING_ID);
        final String procedureName = inputParameters.getValue(THING_NAME);
        final String procedureDesc = inputParameters.getValue(THING_DESC);
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
        final List<String> mainColumns = getMultipleValues(inputParameters, MAIN_COLUMN);
        final List<String> dateColumns = getMultipleValues(inputParameters,DATE_COLUMN);
        final String dateFormat = inputParameters.getValue(DATE_FORMAT);
        final String longitudeColumn = inputParameters.getValue(LONGITUDE_COLUMN);
        final String latitudeColumn = inputParameters.getValue(LATITUDE_COLUMN);
        final String foiColumn = inputParameters.getValue(FOI_COLUMN);
        final String observationType = inputParameters.getValue(OBS_TYPE);
        final String zColumn    = inputParameters.getValue(Z_COLUMN);
        final List<String> qualityColumns = getMultipleValues(inputParameters,QUALITY_COLUMN);
        final List<String> qualityColumnsIds = getMultipleValues(inputParameters,QUALITY_COLUMN_ID);
        final List<String> qualityColumnsTypes = getMultipleValues(inputParameters,QUALITY_COLUMN_TYPE);

        // csv-flat special
        final String typeColumn  = inputParameters.getValue(TYPE_COLUMN);
        final String valueColumn = inputParameters.getValue(RESULT_COLUMN);
        final String obsPropId   = inputParameters.getValue(OBS_PROP_ID);
        final List<String> obsPropColumns     = getMultipleValues(inputParameters,OBS_PROP_COLUMN);
        final List<String> obsPropColumnTypes = getMultipleValues(inputParameters,OBS_PROP_COLUMN_TYPE);
        final String obsPropRegex = inputParameters.getValue(OBS_PROP_REGEX);
        final String obsPropName  = inputParameters.getValue(OBS_PROP_NAME);
        final List<String> ObsPropNameColumns = getMultipleValues(inputParameters,OBS_PROP_NAME_COLUMN);
        final List<String> obsPropFilterColumns = getMultipleValues(inputParameters,OBS_PROP_COLUMNS_FILTER);

        final String uomColumn   = inputParameters.getValue(UOM_COLUMN);
        final String uomRegex    = inputParameters.getValue(UOM_REGEX);
        final String uomID       = inputParameters.getValue(UOM_ID);
        final int userId         = 1; // always admin for now

        final SosHarvestFileChecker customChecker   = inputParameters.getValue(FILE_CHECKER);

        // verify that the services are correct sensor services
        Collection<SensorService> services;
        try {
            Map<String, SensorService> serviceMap = new HashMap<>();
            for (ServiceProcessReference serv : serviceRefs) {
                ObservationProvider serviceOMProvider = getServiceOMProvider(serv.getId(), serviceBusiness);
                String key = serviceOMProvider.getDatasourceKey();
                if (!serviceMap.containsKey(key)) {
                    List<ServiceProcessReference> refs = new ArrayList<>();
                    refs.add(serv);
                    serviceMap.put(key, new SensorService(serviceOMProvider, refs));
                } else {
                    serviceMap.get(key).services.add(serv);
                }
            }
            services = serviceMap.values();
        } catch (ConfigurationException ex) {
            throw new ProcessException("Error while checking sensor service", this, ex);
        }

        StringBuilder checkReport = new StringBuilder();
        StringBuilder errorReport = new StringBuilder();
        Exception error = null;
        SosHarvestFileChecker checker = null;
        if (checkFiles) {
            checker = customChecker != null ? customChecker : new SosHarvestFileChecker();
            checker.setTargetServices(services);
        }

        // prepare the results
        int nbObsInserted  = 0;

        final List<String> alreadyInsertedFiles = new ArrayList<>();
        final List<String> insertedFiles = new ArrayList<>();
        final List<String> removedFiles = new ArrayList<>();
        final List<String> errorFiles = new ArrayList<>();
        final List<Integer> integratedData = new ArrayList<>();

        if (observationType == null && !storeId.equals("observationCsvFlatFile")) {
            throw new ProcessException("The observation type can't be null except for csvFlat store with type column", this);
        }

        /*
        =======================================
        2- Datasource file creation.
           The datasource will hold records of the files that has been already integrated.
        =======================================*/
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
                ds.setPermanent(Boolean.TRUE); // TODO maybe always permanent is not a good id.
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

        // for informations purpose, as for single file we don't have acess to the file name
        String singleFileName = "";
        try {
            Path dataFile = datasourceBusiness.getDatasourcePath(dsId, "/");
            if (Files.isRegularFile(dataFile)) {
                singleFileName = dataFile.getFileName().toString();
            }
        } catch (ConstellationException ex) {
            throw new ProcessException("Error while accesing the datasource", this, ex);
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
        =======================================
        3- Data integration into examind.
           create the csv store and their associated data.
        =======================================*/

        // http://localhost:8080/examind/API/internal/datas/store/observationFile
        final DataStoreProvider factory = DataStores.getProviderById(storeId);
        final Map<String, Integer> dataFileToIntegrate = new LinkedHashMap<>();

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
            provConfig.getParameters().put(FileParsingObservationStoreFactory.PROCEDURE_NAME.getName().toString(), procedureName);
            provConfig.getParameters().put(FileParsingObservationStoreFactory.PROCEDURE_DESC.getName().toString(), procedureDesc);
            provConfig.getParameters().put(FileParsingObservationStoreFactory.UOM_REGEX.getName().toString(), uomRegex);
            provConfig.getParameters().put(FileParsingObservationStoreFactory.UOM_ID.getName().toString(), uomID);
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

                    String filePath = p.getPath() + singleFileName;
                    try {
                        switch (p.getStatus()) {
                            case "NO_DATA", "ERROR" -> {
                                fireAndLog("No data / Error in file: " + filePath, 0);
                                errorFiles.add(filePath);
                            }
                            case "INTEGRATED", "COMPLETED" -> {
                                fireAndLog("File already integrated for file: " + filePath, 0);
                                alreadyInsertedFiles.add(filePath);
                            }
                            case "REMOVED" -> {
                                fireAndLog("Removing data for file: " + filePath, 0);
                                providerBusiness.removeProvider(p.getProviderId());
                                // TODO full removal
                                datasourceBusiness.removePath(dsId, p.getPath());
                                removedFiles.add(filePath);
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
                                fireAndLog("Integrating data file: " + filePath, 0);
                                Integer dataId = integrateDataFile(p, dsId, provConfig, datasetId, userId);
                                if (dataId != null) {
                                    dataBusiness.acceptData(dataId, userId, generateMetadata, false);
                                    dataBusiness.updateDataDataSetId(dataId, datasetId);
                                    dataFileToIntegrate.put(filePath, dataId);

                                // i don't know if this can really happen
                                } else {
                                    throw new ConstellationException("File:" + filePath + " has produced no data.");
                                }
                            }
                        }
                    } catch (ConstellationException ex) {
                        LOGGER.warning("Error while analysing the file:" + ex.getMessage());
                        if (!errorFiles.contains(filePath)) {
                             errorFiles.add(filePath);
                        }
                        error = new Exception(filePath + ":\n" + ex.getMessage(), ex);;
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
        =======================================
        4- Insert each data for each csv file into the sensor services.
        =======================================*/
        final double byData = 100.0 / dataFileToIntegrate.size();
        for (final Entry<String, Integer> fileEntry : dataFileToIntegrate.entrySet()) {
            final String fileName = fileEntry.getKey();
            final Integer dataId  = fileEntry.getValue();

            boolean accept = true;
            if (checker != null) {
                checker.clear();
                accept = checker.checkFile(fileName, dataId);
                error  = checker.getError();
                checkReport.append(checker.getReport());
            }
            if (accept) {
                try {
                    int currentNbObs = importSensor(services, dataId, byData);
                    nbObsInserted = nbObsInserted + currentNbObs;

                    // add the integrated data id and file to results
                    insertedFiles.add(fileName);
                    integratedData.add(dataId);

                } catch (ConstellationException ex) {
                    LOGGER.log(Level.WARNING, "ERROR while inserting file:" + fileName + " into the sensor services.", ex);
                    // add the error file name to results
                    error = new Exception(fileName + ":\n" + ex.getMessage(), ex);
                    errorFiles.add(fileName);
                    try {
                        dataBusiness.removeData(dataId, false);
                    } catch(ConstellationException subex) {
                        LOGGER.log(Level.WARNING, "An exception occurs while removing a sensor data that cause an insertion error", subex);
                    }
                }
            } else {
                errorFiles.add(fileName);
                try {
                    dataBusiness.removeData(dataId, false);
                } catch(ConstellationException subex) {
                    LOGGER.log(Level.WARNING, "An exception occurs while removing a sensor data that cause an insertion error", subex);
                }
            }
            if (error != null) {
                errorReport.append(error.getMessage());
                if (errorReport.charAt(errorReport.length() -1)  != '\n') {
                    errorReport.append('\n');
                }
            }
        }

        /* we throw an error if none file has succeeded insertion:
         * - There is only one file to insert, and the insertion failed, we throw the recorded error.
         * - All the files are in error.
         */

        if (insertedFiles.isEmpty() && alreadyInsertedFiles.isEmpty()) {
            if (errorFiles.size() == 1) {
                if (error != null) {
                    throw new ProcessException(error.getMessage(), this, error);
                // the file as been in error during analyze phase
                // so we don't have much information to send
                } else {
                    throw new ProcessException("File analyze failed", this);
                }
            } else if (!errorFiles.isEmpty()) {
                String msg = "All the files insertion failed";
                if (!errorReport.isEmpty()) {
                    msg = msg + ":\n" + errorReport.toString();
                }
                throw new ProcessException(msg, this);
            }
        }

        try {
            // reload service at the end
            for (SensorService sserv : services) {
                for (ServiceProcessReference serv : sserv.services) {
                    serviceBusiness.restart(serv.getId());
                }
            }

        } catch (ConstellationException ex) {
            throw new ProcessException("Error while restarting services", this, ex);
        }

        // set the results
        addMultipleValues(outputParameters, integratedData, SosHarvesterProcessDescriptor.GENERATE_DATA_IDS);
        addMultipleValues(outputParameters, errorFiles, SosHarvesterProcessDescriptor.FILE_ERROR);
        outputParameters.getOrCreate(SosHarvesterProcessDescriptor.FILE_ERROR_COUNT).setValue(errorFiles.size());
        addMultipleValues(outputParameters, alreadyInsertedFiles, SosHarvesterProcessDescriptor.FILE_ALREADY_INSERTED);
        outputParameters.getOrCreate(SosHarvesterProcessDescriptor.FILE_ALREADY_INSERTED_COUNT).setValue(alreadyInsertedFiles.size());
        addMultipleValues(outputParameters, insertedFiles, SosHarvesterProcessDescriptor.FILE_INSERTED);
        outputParameters.getOrCreate(SosHarvesterProcessDescriptor.FILE_INSERTED_COUNT).setValue(insertedFiles.size());
        addMultipleValues(outputParameters, removedFiles, SosHarvesterProcessDescriptor.FILE_REMOVED);
        outputParameters.getOrCreate(SosHarvesterProcessDescriptor.FILE_REMOVED_COUNT).setValue(removedFiles.size());
        outputParameters.getOrCreate(SosHarvesterProcessDescriptor.OBSERVATION_INSERTED).setValue(nbObsInserted);
        if (!checkReport.isEmpty()) {
            outputParameters.getOrCreate(SosHarvesterProcessDescriptor.CHECK_REPORT).setValue(checkReport.toString());
        }
    }

    private Integer integrateDataFile(DataSourceSelectedPath p, Integer dsid, ProviderConfiguration provConfig, Integer datasetId, Integer owner) throws ConstellationException {
        Integer result = null;
        ResourceStoreAnalysisV3 store = datasourceBusiness.treatDataPath(p, dsid, provConfig, true, datasetId, owner);

        // here we are going to assume that only one data is generated for a file (for later simplification).
        // this is the case for csv stores. we are going to throw some error if its not te case, but we expect this to never happen
        if (store.getResources().size() > 1) {
            throw new ConstellationException("A csv file produce more than one data, its not supported");
        } else if (!store.getResources().isEmpty()) {
            result = store.getResources().get(0).getId();
        } else {
            return result;
        }
        return result;
    }

    private int importSensor(final Collection<SensorService> services, final int dataId, final double byData) throws ConstellationException {
        Integer providerId = dataBusiness.getDataProvider(dataId);
        final DataProvider provider = DataProviders.getProvider(providerId);
        ObservationProvider csvOmProvider;
        if (provider instanceof ObservationProvider) {
             csvOmProvider = (ObservationProvider) provider;
        } else {
            throw new ConfigurationException("Failure : Available only on Observation provider for now");
        }
        int nbObsTotal                              = 0;
        final List<ObservationProvider> treated     = new ArrayList<>();
        final double byInsert                       = byData / services.size();

        for (SensorService sosRef : services) {
            fireAndLog("inserting data " + dataId + " into the service sensor provider " + sosRef.provider.getId(), byInsert);
            
            final ObservationProvider omServiceProvider = sosRef.provider;

            // import observation dataset into the service provider
            final Set<Phenomenon> existingPhenomenons   = new HashSet<>(omServiceProvider.getPhenomenon(new ObservedPropertyQuery()));
            final Set<SamplingFeature> existingFois     = new HashSet<>(omServiceProvider.getFeatureOfInterest(new SamplingFeatureQuery()));

            final ObservationDataset result = csvOmProvider.extractResults(new DatasetQuery());
            if (result.getObservations().isEmpty()) {
                throw new ConstellationException("The data provider did not produce any observations.");
            }
            reuseExistingPhenomenonAndFOI(result, existingPhenomenons, existingFois);

            // generate sensor
            final long start = System.currentTimeMillis();
            final List<Integer> sensorIds = new ArrayList<>();
            for (ProcedureDataset process : result.getProcedures()) {
                omServiceProvider.writeProcedure(process);
                Integer sid =  sensorBusiness.generateSensor(process, null, null, dataId);
                sensorIds.add(sid);
            }
            result.getObservations().stream().forEach(obs -> ((org.geotoolkit.observation.model.Observation)obs).setName(null));

            // import observation in the service provider
            for (Observation obs : result.getObservations()) {
                omServiceProvider.writeObservation(obs);
            }
            LOGGER.log(Level.INFO, "observations imported in :{0} ms", (System.currentTimeMillis() - start));

            nbObsTotal = nbObsTotal + result.getObservations().size();
            treated.add(omServiceProvider);

            for (ServiceProcessReference servRef : sosRef.services) {
                // link sensors to the service
                for (Integer sensorID : sensorIds) {
                    sensorBusiness.addSensorToService(servRef.getId(), sensorID);
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

    private void fireAndLog(final String msg, double progress) {
        LOGGER.info(msg);
        this.progress = this.progress + progress;
        fireProgressing(msg, (float) this.progress, false);
    }
}
