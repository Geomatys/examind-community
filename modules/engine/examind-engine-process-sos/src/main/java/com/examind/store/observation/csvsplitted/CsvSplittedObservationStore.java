/*
 *     Examind Community - An open source and standard compliant SDI
 *     https://community.examind.com/
 *
 *  Copyright 2025 Geomatys.
 *
 *  Licensed under the Apache License, Version 2.0 (    the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/

package com.examind.store.observation.csvsplitted;

import com.examind.store.observation.DataFileReader;
import com.examind.store.observation.ObservationBlock;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.geotoolkit.observation.model.ObservationDataset;
import org.geotoolkit.observation.model.ProcedureDataset;
import org.geotoolkit.storage.DataStores;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static com.examind.store.observation.csvflat.CsvFlatUtils.*;
import static com.examind.store.observation.FileParsingUtils.*;
import com.examind.store.observation.FileParsingObservationStore;
import static com.examind.store.observation.FileParsingObservationStoreFactory.OBS_PROP_COLUMN;
import static com.examind.store.observation.FileParsingObservationStoreFactory.OBS_PROP_FILTER_COLUMN;
import static com.examind.store.observation.FileParsingObservationStoreFactory.OBS_PROP_NAME_COLUMN;
import static com.examind.store.observation.FileParsingObservationStoreFactory.OBS_PROP_DESC_COLUMN;
import static com.examind.store.observation.FileParsingObservationStoreFactory.OBS_PROP_PROPERTIES_COLUMN;
import static com.examind.store.observation.FileParsingObservationStoreFactory.OBS_PROP_PROPERTIES_MAP_COLUMN;
import static com.examind.store.observation.FileParsingObservationStoreFactory.RESULT_COLUMN;
import static com.examind.store.observation.FileParsingObservationStoreFactory.TYPE_COLUMN;
import static com.examind.store.observation.FileParsingObservationStoreFactory.UOM_COLUMN;
import static com.examind.store.observation.FileParsingObservationStoreFactory.getMultipleValues;
import static com.examind.store.observation.FileParsingObservationStoreFactory.getMultipleValuesList;
import com.examind.store.observation.MeasureField;
import com.examind.store.observation.ObservedProperty;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.constellation.exception.ConstellationStoreException;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.FieldDataType;
import org.geotoolkit.observation.model.FieldType;
import org.geotoolkit.observation.model.Phenomenon;
import org.geotoolkit.observation.model.Procedure;
import org.geotoolkit.observation.model.SamplingFeature;
import org.geotoolkit.observation.query.DatasetQuery;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.parameter.ParameterValueGroup;

/**
 * Implementation of an observation store for csv flat observation data based on {@link CSVFeatureStore}.
 *
 * @author Guilhem Legal (Geomatys)
 *
 */
public class CsvSplittedObservationStore extends FileParsingObservationStore {
    
    private Path stationFile;
    private Path obsPropFile;
    private Path observationFile;

    private final String valueColumn;
    private final Set<String> csvFlatobsPropColumns;
    private final List<String> obsPropNameColumns;
    private final List<String> obsPropDescColumns;
    private final String typeColumn;
    private final String uomColumn;
    
    protected final String obsPropPropertiesMapColumn;
    protected final Set<String> obsPropPropertieColumns;

    /**
     * use to avoid loading obsPropColumns at store creation.
     */
    private boolean obsPropColumnsLoaded = false;
    private final Set<String> obsPropFilterColumns;

    public CsvSplittedObservationStore(final ParameterValueGroup params) throws DataStoreException, IOException {
        super(params);

        this.observationFile = dataFile;
        final Path parent    = dataFile.getParent();
        this.stationFile     = parent.resolve("sensors.csv");
        this.obsPropFile     = parent.resolve("variables.csv");
        
        this.valueColumn = (String) params.parameter(RESULT_COLUMN.getName().toString()).getValue();
        this.csvFlatobsPropColumns = getMultipleValues(params, OBS_PROP_COLUMN.getName().toString());
        this.obsPropNameColumns = getMultipleValuesList(params, OBS_PROP_NAME_COLUMN.getName().toString());
        this.obsPropDescColumns = getMultipleValuesList(params, OBS_PROP_DESC_COLUMN.getName().toString());
        this.typeColumn = (String) params.parameter(TYPE_COLUMN.getName().toString()).getValue();
        this.uomColumn = (String) params.parameter(UOM_COLUMN.getName().toString()).getValue();

        this.obsPropFilterColumns = getMultipleValues(params, OBS_PROP_FILTER_COLUMN.getName().toString());
        this.obsPropPropertiesMapColumn = (String) params.parameter(OBS_PROP_PROPERTIES_MAP_COLUMN.getName().toString()).getValue();
        this.obsPropPropertieColumns = getMultipleValues(params, OBS_PROP_PROPERTIES_COLUMN.getName().toString());
    }

    /**
     * Load obsPropColumns.
     * 
     * @return
     * @throws DataStoreException
     */
    private synchronized  Set<String> getObsPropColumns() throws DataStoreException {
        if (!obsPropColumnsLoaded) {
            // special case for hard coded observed property
            // in flat mode, only one is accepted
            if (!obsPropIds.isEmpty()) {
                this.obsPropColumns = new HashSet(obsPropIds);
            // special case for * measure columns
            // if the store is open with missing mime type we skip this part.
            } else if (obsPropFilterColumns.isEmpty() && mimeType != null) {
                try (final DataFileReader reader = getDataFileReader(obsPropFile)) {
                    this.obsPropColumns = extractCodes(reader, csvFlatobsPropColumns, noHeader, directColumnIndex, obsPropRegex);
                } catch (ConstellationStoreException ex) {
                    throw new DataStoreException(ex.getMessage(), ex);
                } catch (IOException | IndexOutOfBoundsException | InterruptedException ex) {
                    throw new DataStoreException("problem reading variables csv file", ex);
                }
            } else {
                 this.obsPropColumns = obsPropFilterColumns;
            }
            obsPropColumnsLoaded = true;
        }
        return this.obsPropColumns;
    }

    @Override
    public DataStoreProvider getProvider() {
        return DataStores.getProviderById(CsvSplittedObservationStoreFactory.NAME);
    }
    
    private String getProcedureId(int procIndex, Object[] line) {
        final String id;
        if (procIndex != -1) {
            String procId = extractWithRegex(procRegex, asString(line[procIndex]));
            id = procedureId + procId;
        } else {
            id = getProcedureID();
        }
        return id;
    }
    
    private String getObsPropId(List<Integer> obsPropColumnIndexes, Object[] line) {
        String fixedId   = obsPropIds.isEmpty()  ? null  : obsPropIds.get(0);
        return getMultiOrFixedValue(line, fixedId, obsPropColumnIndexes, obsPropRegex);
    }
    
    private static class ProcedureWithPosition {
        public final Procedure procedure;
        public final double[] position;
        
        public ProcedureWithPosition(Procedure procedure, double[] position) {
            this.procedure = procedure;
            this.position = position;
        }
    }

    @Override
    public ObservationDataset getDataset(final DatasetQuery query) throws DataStoreException {

        // pre-load the obsProp colmuns has we don't want to open twice the file
        // some DataFileReader are not concurrent (like xlsx) ans this will cause issue
        final List<String> sortedMeasureColumns = getObsPropColumns().stream().sorted().collect(Collectors.toList());

       /* -------------------------------------------------------------------- 
        *
        * 1) load procedure from the csv file
        * -------------------------------------------------------------------- 
        */
        Map<String, ProcedureWithPosition> procedureMap = new HashMap<>();
        try (final DataFileReader reader = getDataFileReader(stationFile)) {
            
            String[] headers = null;
            if (!noHeader) {
                headers = reader.getHeaders();
            }
            
            final List<Integer> doubleFields = new ArrayList<>();
            // sometimes in some files, the last columns are empty, and so do not appears in the line
            // so we want to consider a line as imcomplete only if the last index we look for is missing.
            AtomicInteger maxIndex  = new AtomicInteger();
            
            int latitudeIndex    = getColumnIndex(latitudeColumn,      headers, doubleFields, directColumnIndex, laxHeader, maxIndex);
            int longitudeIndex   = getColumnIndex(longitudeColumn,     headers, doubleFields, directColumnIndex, laxHeader, maxIndex);
            int procIndex        = getColumnIndex(procedureColumn,     headers,               directColumnIndex, laxHeader, maxIndex);
            int procNameIndex    = getColumnIndex(procedureNameColumn, headers,               directColumnIndex, laxHeader, maxIndex);
            int procDescIndex    = getColumnIndex(procedureDescColumn, headers,               directColumnIndex, laxHeader, maxIndex);
            
            int procPropMapIndex    = getColumnIndex(procedurePropertiesMapColumn,  headers,      directColumnIndex, laxHeader, maxIndex);
            
            Map<Integer, String> procPropIndexes    = getNamedColumnIndexes(procedurePropertieColumns, headers, directColumnIndex,laxHeader, maxIndex);
            
            int lineNumber = 1;
            final Iterator<Object[]> it = reader.iterator(!noHeader);
            while (it.hasNext()) {
                lineNumber++;
                final Object[] line = it.next();

                // verify that the line is complete (meaning that the line is at least as long as the last index we look for)
                if (verifyLineCompletion(line, lineNumber, headers, maxIndex)) {
                    LOGGER.finer("skipping empty line " + lineNumber);
                    continue;
                }

                // verify that the line is not empty (meaning that not all of the measure value selected are empty)
                if (verifyEmptyLine(line, lineNumber, doubleFields)) {
                    LOGGER.fine("skipping line due to none expected variable present.");
                    continue;
                }
                
                final String id = getProcedureId(procIndex, line);
                final Procedure proc = parseProcedure(line, procIndex, procNameIndex, procDescIndex, procPropMapIndex, procPropIndexes);
                final double[] position;
                try {
                    position = extractLinePosition(latitudeIndex, longitudeIndex, id, line);
                } catch (NumberFormatException | ParseException ex) {
                    LOGGER.fine(String.format("Problem parsing lat/lon field at line %d.(Error msg='%s'). skipping line...", lineNumber, ex.getMessage()));
                    continue;
                }
                
                procedureMap.put(id, new ProcedureWithPosition(proc, position));
            }
            
        } catch (IOException | InterruptedException ex) {
            LOGGER.log(Level.WARNING, "problem reading station csv file", ex);
            throw new DataStoreException(ex);
        }
        
        /* -------------------------------------------------------------------- 
        *
        * 2) load observed properties from the csv file
        * -------------------------------------------------------------------- 
        */
        Map<String, ObservedProperty> obsPropMap = new HashMap<>();
        try (final DataFileReader reader = getDataFileReader(obsPropFile)) {
            
            String[] headers = null;
            if (!noHeader) {
                headers = reader.getHeaders();
            }
            
            // sometimes in some files, the last columns are empty, and so do not appears in the line
            // so we want to consider a line as imcomplete only if the last index we look for is missing.
            AtomicInteger maxIndex  = new AtomicInteger();
            
            int uomColumnIndex   = getColumnIndex(uomColumn,           headers,               directColumnIndex, laxHeader, maxIndex);
            int obsPropPropMapIndex = getColumnIndex(obsPropPropertiesMapColumn,    headers,      directColumnIndex, laxHeader, maxIndex);
            List<Integer> obsPropColumnIndexes      = getColumnIndexes(csvFlatobsPropColumns,     headers, directColumnIndex, laxHeader, maxIndex, OBS_PROP_QUALIFIER);
            List<Integer> obsPropNameColumnIndexes  = getColumnIndexes(obsPropNameColumns,        headers, directColumnIndex, laxHeader, maxIndex, OBS_PROP_NAME_QUALIFIER);
            List<Integer> obsPropDescColumnIndexes  = getColumnIndexes(obsPropDescColumns,        headers, directColumnIndex, laxHeader, maxIndex, OBS_PROP_DESC_QUALIFIER);
            Map<Integer, String> obsPropPropIndexes = getNamedColumnIndexes(obsPropPropertieColumns,   headers, directColumnIndex,laxHeader, maxIndex);
            
            int lineNumber = 1;
            final Iterator<Object[]> it = reader.iterator(!noHeader);
            while (it.hasNext()) {
                lineNumber++;
                final Object[] line = it.next();

                // verify that the line is complete (meaning that the line is at least as long as the last index we look for)
                if (verifyLineCompletion(line, lineNumber, headers, maxIndex)) {
                    LOGGER.finer("skipping empty line " + lineNumber);
                    continue;
                }

                String obsPropId         = getObsPropId(obsPropColumnIndexes, line);
                ObservedProperty obsProp = parseObservedProperty(line, obsPropId, obsPropNameColumnIndexes, obsPropDescColumnIndexes, uomColumnIndex, obsPropPropMapIndex, obsPropPropIndexes, Map.of());
                obsPropMap.put(obsPropId, obsProp);
                
            }
            
        } catch (IOException | InterruptedException ex) {
            LOGGER.log(Level.WARNING, "problem reading variables csv file", ex);
            throw new DataStoreException(ex);
        }
        
        // open csv file with a delimiter set as process SosHarvester input.
        try (final DataFileReader reader = getDataFileReader(observationFile)) {

            String[] headers = null;
            if (!noHeader) {
                headers = reader.getHeaders();
            }

            /*
            1- filter prepare spatial/time column indices from ordinary fields
            ================================================================*/
            final List<Integer> doubleFields = new ArrayList<>();

            // sometimes in some files, the last columns are empty, and so do not appears in the line
            // so we want to consider a line as imcomplete only if the last index we look for is missing.
            AtomicInteger maxIndex  = new AtomicInteger();
            
            int foiIndex         = getColumnIndex(foiColumn,           headers,               directColumnIndex, laxHeader, maxIndex);
            int zIndex           = getColumnIndex(zColumn,             headers,               directColumnIndex, laxHeader, maxIndex);
            int procIndex        = getColumnIndex(procedureColumn,     headers,               directColumnIndex, laxHeader, maxIndex);
            int valueColumnIndex = getColumnIndex(valueColumn,         headers, doubleFields, directColumnIndex, laxHeader, maxIndex);
            int typeColumnIndex  = getColumnIndex(typeColumn,          headers,               directColumnIndex, laxHeader, maxIndex);
            
            List<Integer> dateIndexes              = getColumnIndexes(dateColumns,               headers, directColumnIndex, laxHeader, maxIndex, DATE_QUALIFIER);
            List<Integer> mainIndexes              = getColumnIndexes(mainColumns,               headers, directColumnIndex, laxHeader, maxIndex, MAIN_QUALIFIER);
            List<Integer> obsPropColumnIndexes     = getColumnIndexes(csvFlatobsPropColumns,     headers, directColumnIndex, laxHeader, maxIndex, OBS_PROP_QUALIFIER);
            List<Integer> qualityIndexes           = getColumnIndexes(qualityColumns,            headers, directColumnIndex, laxHeader, maxIndex, QUALITY_QUALIFIER);
            List<Integer> parameterIndexes         = getColumnIndexes(parameterColumns,          headers, directColumnIndex, laxHeader, maxIndex, PARAMETER_QUALIFIER);
            
            if (obsPropColumnIndexes.isEmpty()) {
                throw new DataStoreException("Unexpected columns code:" + Arrays.toString(csvFlatobsPropColumns.toArray()));
            }
            if (valueColumnIndex == -1) {
                throw new DataStoreException("Unexpected column value:" + valueColumn);
            }
            if (mainIndexes.isEmpty() && observationType != null) {
                throw new DataStoreException("Unexpected column main:" + mainColumns);
            }

            // final result
            final ObservationDataset result = new ObservationDataset();
            final Map<String, ObservationBlock> observationBlock = new LinkedHashMap<>();
            
            /*
            2- compute measures
            =================*/

            int lineNumber = 1;

            // spatial / temporal boundaries
            final DateFormat sdf = new SimpleDateFormat(this.dateFormat);

            // -- single observation related variables --
            Long currentTime                      = null;
            String currentFoi                     = null;
            String currentObstType                = observationType;
            final List<String> obsTypeCodes       = getObsTypeCodes();
            List<MeasureField> qualityFields      = buildExtraMeasureFields(qualityColumns, qualityColumnsIds, qualityColumnsTypes);
            List<MeasureField> parameterFields    = buildExtraMeasureFields(parameterColumns, parameterColumnsIds, parameterColumnsTypes);

            final Map<String, MeasureColumns> measureColumnsMap = new HashMap<>();

            final Iterator<Object[]> it = reader.iterator(!noHeader);
            while (it.hasNext()) {
                lineNumber++;
                final Object[] line = it.next();

                // verify that the line is complete (meaning that the line is at least as long as the last index we look for)
                if (verifyLineCompletion(line, lineNumber, headers, maxIndex)) {
                    LOGGER.finer("skipping empty line " + lineNumber);
                    continue;
                }

                // verify that the line is not empty (meaning that not all of the measure value selected are empty)
                if (verifyEmptyLine(line, lineNumber, doubleFields)) {
                    LOGGER.fine("skipping line due to none expected variable present.");
                    continue;
                }

                // checks if row matches the observed data types
                final List<String> currentMainColumns;
                if (typeColumnIndex != -1) {
                    if (!obsTypeCodes.contains(asString(line[typeColumnIndex]))) continue;
                    if (observationType == null) {
                        currentObstType = getObservationTypeFromCode(asString(line[typeColumnIndex]));
                        if (currentObstType.equals("Profile")) {
                            mainIndexes = Arrays.asList(zIndex);
                            currentMainColumns = Arrays.asList(zColumn);
                        } else {
                            mainIndexes = dateIndexes;
                            currentMainColumns = dateColumns;
                        }
                    } else {
                        currentMainColumns = mainColumns;
                    }
                } else {
                    currentMainColumns = mainColumns;
                }
                
                MeasureColumns measureColums = measureColumnsMap.computeIfAbsent(currentObstType, cot -> {
                    List<MeasureField> measureFields = new ArrayList<>();
                     // initialize description
                    int offset = "Profile".equals(observationType) ? 1 : 0;
                    for (int j = 0, k = offset; j < sortedMeasureColumns.size(); j++, k++) {
                        String mc = sortedMeasureColumns.get(j);
                        FieldDataType type = FieldDataType.QUANTITY;
                        measureFields.add(new MeasureField(-1, mc, type, qualityFields, parameterFields));
                    }
                    return new MeasureColumns(measureFields, currentMainColumns, cot);
                });


                // look for current procedure (for observation separation)
                String procId = getProcedureId(procIndex, line);
                final ProcedureWithPosition procWpos = procedureMap.get(procId);
                if (procWpos == null) {
                    LOGGER.finer("skipping line due to null procedure.");
                    continue;
                }
                final Procedure currentProc = procWpos.procedure;
                if (!query.getSensorIds().isEmpty() && !query.getSensorIds().contains(currentProc.getId())) {
                    LOGGER.finer("skipping line due to sensor filter.");
                    continue;
                }

                // look for current foi (for observation separation)
                currentFoi = asString(getColumnValue(foiIndex, line, currentFoi));

                // look for current date (for profile observation separation)
                if (!dateIndexes.equals(mainIndexes)) {
                    Optional<Long> dateO = parseDate(line, null, dateIndexes, sdf, lineNumber);
                    if (dateO.isPresent()) {
                        currentTime = dateO.get();
                    } else {
                        continue;
                    }
                }

                String obsPropId = getObsPropId(obsPropColumnIndexes, line);
                ObservedProperty observedProperty = obsPropMap.get(obsPropId);

                // checks if row matches the observed properties wanted
                if (observedProperty == null || !sortedMeasureColumns.contains(observedProperty.id)) {
                    continue;
                }

                ObservationBlock currentBlock = getOrCreateObservationBlock(observationBlock, currentProc, currentFoi, currentTime, measureColums);

                currentBlock.updateObservedProperty(observedProperty);

                // update temporal interval
                Long millis = null;
                if (!dateIndexes.isEmpty()) {
                    Optional<Long> dateO = parseDate(line, millis, dateIndexes, sdf, lineNumber);
                    if (dateO.isPresent()) {
                        millis = dateO.get();
                        result.spatialBound.addDate(millis);
                        currentBlock.addDate(millis);
                    } else {
                        continue;
                    }
                }

                // update spatial information TODO
                if (procWpos.position.length == 2) {
                    final double latitude = procWpos.position[0];
                    final double longitude = procWpos.position[1];
                    result.spatialBound.addXYCoordinate(longitude, latitude);
                    currentBlock.addPosition(millis, latitude, longitude);
                }

                // parse main value
                Optional<? extends Number> mainO = parseMain(line, millis, mainIndexes, sdf, lineNumber, currentObstType);
                Number mainValue ;
                if (mainO.isPresent()) {
                    mainValue = mainO.get();
                } else {
                    continue;
                }

                // parse Measure value
                double measureValue;
                try {
                    measureValue = parseDouble(line[valueColumnIndex]);
                } catch (ParseException | NumberFormatException ex) {
                    LOGGER.fine(String.format("Problem parsing double for measure field at line %d and column %d (value='%s'). skipping line...", lineNumber, valueColumnIndex, line[valueColumnIndex]));
                    continue;
                }
                // todo quality/parameter field types
                String[] qualityValues = new String[qualityIndexes.size()];
                for (int i = 0; i < qualityIndexes.size(); i++) {
                    Integer qIndex = qualityIndexes.get(i);
                    qualityValues[i] = asString(line[qIndex]);
                }
                String[] parameterValues = new String[parameterIndexes.size()];
                for (int i = 0; i < parameterIndexes.size(); i++) {
                    Integer pIndex = parameterIndexes.get(i);
                    parameterValues[i] = asString(line[pIndex]);
                 }
                currentBlock.appendValue(mainValue, observedProperty.id, measureValue, lineNumber, qualityValues, parameterValues);
            }


            /*
            3- build results
            =============*/
            final Set<Phenomenon> phenomenons = new HashSet<>();
            final Set<SamplingFeature> samplingFeatures = new HashSet<>();
            int obsCpt = 0;
            for (ObservationBlock ob : observationBlock.values()) {
                final String oid = dataFileName + '-' + obsCpt;
                obsCpt++;
                buildObservation(result, oid, ob, phenomenons, samplingFeatures, query.getResponseFormat());
            }
            return result;
        } catch (IOException | InterruptedException ex) {
            LOGGER.log(Level.WARNING, "problem reading csv file", ex);
            throw new DataStoreException(ex);
        }
    }

    /**
     * Extract the current observed property (id, name, uom).This method can be overriden by subclasses
     *
     * (The cache map is not used in this implementation for now)
     * 
     * @param line the current csv line.
     * @param obsPropId Observed property id.
     * @param obsPropNameColumnIndexes Columns for observed property name.
     * @param uomColumnIndex Column for observed property unit of measure.
     * @param cache Cached map of observed properties.
     *
     * @return an observed property
     */
    protected ObservedProperty parseObservedProperty(Object[] line, String obsPropId, List<Integer> obsPropNameColumnIndexes, List<Integer> obsPropDescColumnIndexes, Integer uomColumnIndex, int obsPropPropMapIndex, Map<Integer, String> obsPropPropIndexes, Map<String, ObservedProperty> cache) {
        String fixedName = obsPropNames.isEmpty() ? null : obsPropNames.get(0);
        String fixedDesc = obsPropDescs.isEmpty() ? null : obsPropDescs.get(0);
        
        String observedPropertyName = getMultiOrFixedValue(line, fixedName, obsPropNameColumnIndexes);
        String observedPropertyUOM  = extractWithRegex(uomRegex, asString(getColumnValue(uomColumnIndex, line, null)));
        String observedPropertyDesc = getMultiOrFixedValue(line, fixedDesc, obsPropDescColumnIndexes);
        Map<String, Object> properties = new HashMap<>();
        if (obsPropPropMapIndex != -1) {
            properties.putAll(getColumnMapValue(obsPropPropMapIndex, line));
        }
        for (Map.Entry<Integer, String> entry : obsPropPropIndexes.entrySet()) {
            String value = asString(getColumnValue(entry.getKey(), line, null));
            if (value != null) {
                properties.put(entry.getValue(), value);
            }
        }
        return new ObservedProperty(obsPropId, observedPropertyName, observedPropertyUOM, observedPropertyDesc, properties);
    }

    @Override
    protected Set<String> extractProcedureIds() throws DataStoreException {
        if (procedureColumn == null) return Collections.singleton(getProcedureID());

        // pre-load the obsProp colmuns has we don't want to open twice the file
        // some DataFileReader are not concurrent (like xlsx) ans this will cause issue
        final Set<String> obspropColumns = getObsPropColumns();

        final Set<String> result = new HashSet();
        // open csv file
        try (final DataFileReader reader = getDataFileReader(stationFile)) {

            String[] headers = null;
            if (!noHeader) {
                headers = reader.getHeaders();
            }
            int lineNumber = 1;
            final AtomicInteger maxIndex  = new AtomicInteger();
            
            // prepare procedure/type column indices
            int procIndex       = getColumnIndex(procedureColumn, headers, directColumnIndex, laxHeader, maxIndex);
            int typeColumnIndex = getColumnIndex(typeColumn,      headers, directColumnIndex, laxHeader, maxIndex);
            
            if (procIndex == -1) throw new DataStoreException("Unable to find the procedure column: " + procedureColumn);
            String fixedObsId   = obsPropIds.isEmpty()  ? null  : obsPropIds.get(0);

            final Iterator<Object[]> it = reader.iterator(!noHeader);

            final List<String> obsTypeCodes = getObsTypeCodes();
            while (it.hasNext()) {
                lineNumber++;
                final Object[] line = it.next();

                // verify that the line is complete (meaning that the line is at least as long as the last index we look for)
                if (verifyLineCompletion(line, lineNumber, headers, maxIndex)) {
                    LOGGER.finer("skipping empty line " + lineNumber);
                    continue;
                }
                
                // to be perfectly correct we should look for empty measure
                if (verifyEmptyLineStr(line, lineNumber, Arrays.asList(procIndex))) {
                    LOGGER.fine("skipping line due to empty procedure column.");
                    continue;
                }

                // checks if row matches the observed data types
                if (typeColumnIndex != -1) {
                    if (!obsTypeCodes.contains(asString(line[typeColumnIndex]))) continue;
                }

                String procId = extractWithRegex(procRegex, asString(line[procIndex]));
                result.add(procedureId + procId);
            }
            return result;

        } catch (IOException | InterruptedException ex) {
            LOGGER.log(Level.WARNING, "problem reading csv file", ex);
            throw new DataStoreException(ex);
        }
    }
    
    @Override
    public Set<String> extractPhenomenonIds() throws DataStoreException {
        // TODO verify existence?
        return getObsPropColumns();
    }

    @Override
    public List<ProcedureDataset> getProcedureDatasets(DatasetQuery query) throws DataStoreException {
        // pre-load the obsProp columns has we don't want to open twice the file
        // some DataFileReader are not concurrent (like xlsx) ans this will cause issue
        final List<String> sortedMeasureColumns = getObsPropColumns().stream().sorted().collect(Collectors.toList());

        Map<String, ProcedureDataset> result = new LinkedHashMap<>();
        Map<String, double[]> procedurePos   = new HashMap<>();
        String fixedObsId                    = obsPropIds.isEmpty()  ? null  : obsPropIds.get(0);
        String currentObstType               = observationType;
       
        // open station csv file
        try (final DataFileReader reader = getDataFileReader(stationFile)) {

            String[] headers = null;
            if (!noHeader) {
                headers = reader.getHeaders();
            }
            // sometimes in some files, the last columns are empty, and so do not appears in the line
            // so we want to consider a line as imcomplete only if the last index we look for is missing.
            final AtomicInteger maxIndex  = new AtomicInteger();
            final List<Integer> doubleFields = new ArrayList<>();

            int latitudeIndex    = getColumnIndex(latitudeColumn,      headers, doubleFields, directColumnIndex, laxHeader, maxIndex);
            int longitudeIndex   = getColumnIndex(longitudeColumn,     headers, doubleFields, directColumnIndex, laxHeader, maxIndex);
            
            int procedureIndex   = getColumnIndex(procedureColumn,              headers, directColumnIndex, laxHeader, maxIndex);
            int procNameIndex    = getColumnIndex(procedureNameColumn,          headers, directColumnIndex, laxHeader, maxIndex);
            int procDescIndex    = getColumnIndex(procedureDescColumn,          headers, directColumnIndex, laxHeader, maxIndex);
            int procPropMapIndex = getColumnIndex(procedurePropertiesMapColumn, headers, directColumnIndex, laxHeader, maxIndex);

            Map<Integer, String> procPropIndexes = getNamedColumnIndexes(procedurePropertieColumns, headers, directColumnIndex,laxHeader, maxIndex);

            int lineNumber                       = 1;
            
            final Iterator<Object[]> it = reader.iterator(!noHeader);
            while (it.hasNext()) {
                lineNumber++;
                final Object[] line   = it.next();

                // verify that the line is complete (meaning that the line is at least as long as the last index we look for)
                if (verifyLineCompletion(line, lineNumber, headers, maxIndex)) {
                    LOGGER.finer("skipping empty line " + lineNumber);
                    continue;
                }
               
                final Procedure currentProc = parseProcedure(line, procedureIndex, procNameIndex, procDescIndex, procPropMapIndex, procPropIndexes);
                if (currentProc == null) {
                    LOGGER.finer("skipping line due to null procedure.");
                    continue;
                }
                if (!query.getSensorIds().isEmpty() && !query.getSensorIds().contains(currentProc.getId())) {
                    LOGGER.finer("skipping line due to sensor filter.");
                    continue;
                }
                
                List<Field> fields = new ArrayList<>();
                addMainField(currentObstType, fields);
                ProcedureDataset currentPTree = new ProcedureDataset(currentProc.getId(), 
                                                    currentProc.getName(),
                                                    currentProc.getDescription(), 
                                                    PROCEDURE_TREE_TYPE, 
                                                    currentObstType, 
                                                    fields, 
                                                    currentProc.getProperties());
                result.put(currentProc.getId(), currentPTree);
                
                final double[] position;
                try {
                    position = extractLinePosition(latitudeIndex, longitudeIndex, currentProc.getId(), line);
                } catch (NumberFormatException | ParseException ex) {
                    LOGGER.fine(String.format("Problem parsing lat/lon field at line %d.(Error msg='%s'). skipping line...", lineNumber, ex.getMessage()));
                    continue;
                }
                procedurePos.put(currentProc.getId(), position);
            }

        } catch (IOException | InterruptedException ex) {
            throw new DataStoreException("Problem reading station csv file", ex);
        }
        
        
        try (final DataFileReader reader = getDataFileReader(observationFile)) {

            String[] headers = null;
            if (!noHeader) {
                headers = reader.getHeaders();
            }
            // sometimes in some files, the last columns are empty, and so do not appears in the line
            // so we want to consider a line as imcomplete only if the last index we look for is missing.
            final AtomicInteger maxIndex  = new AtomicInteger();
            final List<Integer> doubleFields = new ArrayList<>();

            final List<Integer> dateIndexes           = getColumnIndexes(dateColumns,           headers, directColumnIndex, laxHeader, maxIndex, DATE_QUALIFIER);
            final List<Integer> obsPropColumnIndexes  = getColumnIndexes(csvFlatobsPropColumns, headers, directColumnIndex, laxHeader, maxIndex, OBS_PROP_QUALIFIER);

            int procedureIndex   = getColumnIndex(procedureColumn,     headers,               directColumnIndex, laxHeader, maxIndex);
            int valueColumnIndex = getColumnIndex(valueColumn,         headers, doubleFields, directColumnIndex, laxHeader, maxIndex);
            
            final List<Field> qualityFields      = buildExtraFields(qualityColumns, qualityColumnsIds, qualityColumnsTypes, FieldType.QUALITY);
            final List<Field> parameterFields    = buildExtraFields(parameterColumns, parameterColumnsIds, parameterColumnsTypes, FieldType.PARAMETER);
            final Map<String, Map<String, Field>> measureColumnsMap = new HashMap<>();
            
            int lineNumber                       = 1;
            
            final Iterator<Object[]> it = reader.iterator(!noHeader);
            while (it.hasNext()) {
                lineNumber++;
                final Object[] line   = it.next();
                
                final String procedureId = getProcedureId(procedureIndex, line);
                final ProcedureDataset currentPTree = result.get(procedureId);
                if (currentPTree == null) {
                    continue;
                }
                
                final String observedProperty = getMultiOrFixedValue(line, fixedObsId, obsPropColumnIndexes, obsPropRegex);
                
                Map<String, Field> fieldMap = measureColumnsMap.computeIfAbsent(currentObstType, cot -> {
                    Map<String, Field> measureFields = new HashMap<>();
                     // initialize description
                    int offset = "Profile".equals(observationType) ? 1 : 0;
                    for (int j = 0, k = offset; j < sortedMeasureColumns.size(); j++, k++) {
                        String mc = sortedMeasureColumns.get(j);
                        FieldDataType dataType = FieldDataType.QUANTITY;
                        FieldType type = FieldType.MEASURE;
                        measureFields.put(mc, new Field(k, dataType, mc, mc, null, null, type, qualityFields, parameterFields));
                    }
                    return measureFields;
                });
                
                
                // checks if row matches the observed properties wanted
                if (!sortedMeasureColumns.contains(observedProperty)) {
                    continue;
                }

                // add used field
                Field f = fieldMap.get(observedProperty);
                if (!currentPTree.fields.contains(f)) {
                    currentPTree.fields.add(f);
                }

                // update temporal interval
                Date dateParse        = null;
                if (!dateIndexes.isEmpty()) {
                    Optional<Long> d = parseDate(line, null, dateIndexes, new SimpleDateFormat(this.dateFormat), lineNumber);
                    if (d.isEmpty()) {
                        continue;
                    } else {
                        dateParse = new Date(d.get());
                    }
                    currentPTree.spatialBound.addDate(dateParse);
                }

                // update spatial information only record when the first location
                final double[] position = procedurePos.get(procedureId);
                if (position.length == 2 && currentPTree.spatialBound.getHistoricalLocations().isEmpty()) {
                    Coordinate dp = new Coordinate(position[1], position[0]);
                    currentPTree.spatialBound.addLocation(dateParse, GF.createPoint(dp));
                }
                
            }
        } catch (IOException | InterruptedException ex) {
            throw new DataStoreException("Problem reading observation csv file", ex);
        }
        return new ArrayList<>(result.values());
    }
    
    /**
     * return the allowed values for the "typeColumn".
     * Dependending if the parameter ObservationType is null or not,
     *
     * @return
     */
    private List<String> getObsTypeCodes() {
        if (observationType == null) {
            return Arrays.asList("TS", "TR", "PR");
        }
        return switch (observationType) {
            case "Timeserie" ->  Arrays.asList("TS");
            case "Trajectory"->  Arrays.asList("TR");
            case "Profile"   ->  Arrays.asList("PR");
            default -> throw new IllegalArgumentException("Unexpected observation type:" + observationType + ". Allowed values are Timeserie, Trajectory, Profile.");
        };
    }

    private String getObservationTypeFromCode(String code) {
        return switch (code) {
            case "TS" -> "Timeserie";
            case "TR" -> "Trajectory";
            case "PR" -> "Profile";
            default-> throw new IllegalArgumentException("Unexpected observation type code:" + code + ". Allowed values are TS, TR, PR.");
        };
    }

    @Override
    protected String getStoreIdentifier() {
        return CsvSplittedObservationStoreFactory.NAME;
    }
}
