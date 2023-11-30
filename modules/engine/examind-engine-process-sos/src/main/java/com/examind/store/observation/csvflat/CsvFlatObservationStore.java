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

package com.examind.store.observation.csvflat;

import com.examind.store.observation.DataFileReader;
import com.examind.store.observation.ObservationBlock;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.geotoolkit.observation.ObservationStore;
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
import static com.examind.store.observation.FileParsingObservationStoreFactory.RESULT_COLUMN;
import static com.examind.store.observation.FileParsingObservationStoreFactory.TYPE_COLUMN;
import static com.examind.store.observation.FileParsingObservationStoreFactory.UOM_COLUMN;
import static com.examind.store.observation.FileParsingObservationStoreFactory.getMultipleValues;
import com.examind.store.observation.MeasureField;
import com.examind.store.observation.ObservedProperty;
import java.util.concurrent.atomic.AtomicInteger;
import org.constellation.exception.ConstellationStoreException;
import org.geotoolkit.observation.model.Phenomenon;
import org.geotoolkit.observation.model.Procedure;
import org.geotoolkit.observation.model.SamplingFeature;
import org.geotoolkit.observation.query.DatasetQuery;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.parameter.ParameterValueGroup;

/**
 * Implementation of an observation store for csv flat observation data based on {@link CSVFeatureStore}.
 *
 * @author Maxime Gavens (Geomatys)
 * @author Guilhem Legal (Geomatys)
 *
 */
public class CsvFlatObservationStore extends FileParsingObservationStore implements ObservationStore {

    private final String valueColumn;
    private final Set<String> csvFlatobsPropColumns;
    private final Set<String> obsPropNameColumns;
    private final String typeColumn;
    private final String uomColumn;

    /**
     * use to avoid loading obsPropColumns at store creation.
     */
    private boolean obsPropColumnsLoaded = false;
    private final Set<String> obsPropFilterColumns;

    public CsvFlatObservationStore(final ParameterValueGroup params) throws DataStoreException, IOException {
        super(params);

        this.valueColumn = (String) params.parameter(RESULT_COLUMN.getName().toString()).getValue();
        this.csvFlatobsPropColumns = getMultipleValues(params, OBS_PROP_COLUMN.getName().toString());
        this.obsPropNameColumns = getMultipleValues(params, OBS_PROP_NAME_COLUMN.getName().toString());
        this.typeColumn = (String) params.parameter(TYPE_COLUMN.getName().toString()).getValue();
        this.uomColumn = (String) params.parameter(UOM_COLUMN.getName().toString()).getValue();

        this.obsPropFilterColumns = getMultipleValues(params, OBS_PROP_FILTER_COLUMN.getName().toString());
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
                try {
                    this.obsPropColumns = extractCodes(mimeType, dataFile, csvFlatobsPropColumns, delimiter, quotechar, noHeader, directColumnIndex);
                } catch (ConstellationStoreException ex) {
                    throw new DataStoreException(ex.getMessage(), ex);
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
        return DataStores.getProviderById(CsvFlatObservationStoreFactory.NAME);
    }

    @Override
    public ObservationDataset getDataset(final DatasetQuery query) throws DataStoreException {

        // pre-load the obsProp colmuns has we don't want to open twice the file
        // some DataFileReader are not concurrent (like xlsx) ans this will cause issue
        final List<String> sortedMeasureColumns = getObsPropColumns().stream().sorted().collect(Collectors.toList());

        // open csv file with a delimiter set as process SosHarvester input.
        try (final DataFileReader reader = getDataFileReader()) {

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
            
            int latitudeIndex    = getColumnIndex(latitudeColumn,      headers, doubleFields, directColumnIndex, laxHeader, maxIndex);
            int longitudeIndex   = getColumnIndex(longitudeColumn,     headers, doubleFields, directColumnIndex, laxHeader, maxIndex);
            int foiIndex         = getColumnIndex(foiColumn,           headers,               directColumnIndex, laxHeader, maxIndex);
            int zIndex           = getColumnIndex(zColumn,             headers,               directColumnIndex, laxHeader, maxIndex);
            int procIndex        = getColumnIndex(procedureColumn,     headers,               directColumnIndex, laxHeader, maxIndex);
            int procNameIndex    = getColumnIndex(procedureNameColumn, headers,               directColumnIndex, laxHeader, maxIndex);
            int procDescIndex    = getColumnIndex(procedureDescColumn, headers,               directColumnIndex, laxHeader, maxIndex);
            int valueColumnIndex = getColumnIndex(valueColumn,         headers, doubleFields, directColumnIndex, laxHeader, maxIndex);
            int uomColumnIndex   = getColumnIndex(uomColumn,           headers,               directColumnIndex, laxHeader, maxIndex);
            int typeColumnIndex  = getColumnIndex(typeColumn,          headers,               directColumnIndex, laxHeader, maxIndex);

            List<Integer> dateIndexes              = getColumnIndexes(dateColumns,               headers, directColumnIndex, laxHeader, maxIndex);
            List<Integer> mainIndexes              = getColumnIndexes(mainColumns,               headers, directColumnIndex, laxHeader, maxIndex);
            List<Integer> obsPropColumnIndexes     = getColumnIndexes(csvFlatobsPropColumns,     headers, directColumnIndex, laxHeader, maxIndex);
            List<Integer> obsPropNameColumnIndexes = getColumnIndexes(obsPropNameColumns,        headers, directColumnIndex, laxHeader, maxIndex);
            List<Integer> qualityIndexes           = getColumnIndexes(qualityColumns,            headers, directColumnIndex, laxHeader, maxIndex);

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
            final Map<String, ObservedProperty> observedProperties = new HashMap<>();
            
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
            List<MeasureField> qualityFields      = buildQualityFields();

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
                if (typeColumnIndex!=-1) {
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
                    return new MeasureColumns(sortedMeasureColumns, new ArrayList<>(), currentMainColumns, cot, qualityFields);
                });


                // look for current procedure (for observation separation)
                final Procedure currentProc = parseProcedure(line, procIndex, procNameIndex, procDescIndex);
                if (currentProc == null) {
                    LOGGER.finer("skipping line due to null procedure.");
                    continue;
                }
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

                ObservedProperty observedProperty = parseObservedProperty(line, obsPropColumnIndexes, obsPropNameColumnIndexes, uomColumnIndex, observedProperties);

                // checks if row matches the observed properties wanted
                if (!sortedMeasureColumns.contains(observedProperty.id)) {
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

                // update spatial information
                try {
                    final double[] position = extractLinePosition(latitudeIndex, longitudeIndex, currentProc.getId(), line);
                    if (position.length == 2) {
                        final double latitude = position[0];
                        final double longitude = position[1];
                        result.spatialBound.addXYCoordinate(longitude, latitude);
                        currentBlock.addPosition(millis, latitude, longitude);
                    }
                } catch (NumberFormatException | ParseException ex) {
                    LOGGER.fine(String.format("Problem parsing lat/lon field at line %d.(Error msg='%s'). skipping line...", lineNumber, ex.getMessage()));
                    continue;
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
                // todo quality field types
                String[] qualityValues = new String[qualityIndexes.size()];
                for (int i = 0; i < qualityIndexes.size(); i++) {
                    Integer qIndex = qualityIndexes.get(i);
                    qualityValues[i] = asString(line[qIndex]);
                 }
                currentBlock.appendValue(mainValue, observedProperty.id, measureValue, lineNumber, qualityValues);
            }


            /*
            3- build results
            =============*/
            final Set<Phenomenon> phenomenons = new HashSet<>();
            final Set<SamplingFeature> samplingFeatures = new HashSet<>();
            int obsCpt = 0;
            final String fileName = dataFile.getFileName().toString();
            for (ObservationBlock ob : observationBlock.values()) {
                final String oid = fileName + '-' + obsCpt;
                obsCpt++;
                buildObservation(result, oid, ob, phenomenons, samplingFeatures, query.getResponseFormat());
            }
            return result;
        } catch (IOException ex) {
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
     * @param obsPropColumnIndexes Columns for observed property id.
     * @param obsPropNameColumnIndexes Columns for observed property name.
     * @param uomColumnIndex Column for observed property unit of measure.
     * @param cache Cached map of observed properties.
     *
     * @return an observed property
     */
    protected ObservedProperty parseObservedProperty(Object[] line, List<Integer> obsPropColumnIndexes, List<Integer> obsPropNameColumnIndexes, Integer uomColumnIndex, Map<String, ObservedProperty> cache) {
        String fixedId   = obsPropIds.isEmpty()  ? null  : obsPropIds.get(0);
        String fixedName = obsPropNames.isEmpty() ? null : obsPropNames.get(0);

        String observedProperty     = getMultiOrFixedValue(line, fixedId, obsPropColumnIndexes);
        String observedPropertyName = getMultiOrFixedValue(line, fixedName, obsPropNameColumnIndexes);
        String observedPropertyUOM  = asString(getColumnValue(uomColumnIndex, line, null));
        return new ObservedProperty(observedProperty, observedPropertyName, observedPropertyUOM);
    }

    @Override
    protected Set<String> extractProcedureIds() throws DataStoreException {
        if (procedureColumn == null) return Collections.singleton(getProcedureID());

        // pre-load the obsProp colmuns has we don't want to open twice the file
        // some DataFileReader are not concurrent (like xlsx) ans this will cause issue
        final Set<String> obspropColumns = getObsPropColumns();

        final Set<String> result = new HashSet();
        // open csv file
        try (final DataFileReader reader = getDataFileReader()) {

            String[] headers = null;
            if (!noHeader) {
                headers = reader.getHeaders();
            }
            int lineNumber = 1;
            final AtomicInteger maxIndex  = new AtomicInteger();
            
            // prepare procedure/type column indices
            int procIndex       = getColumnIndex(procedureColumn, headers, directColumnIndex, laxHeader, maxIndex);
            int typeColumnIndex = getColumnIndex(typeColumn,      headers, directColumnIndex, laxHeader, maxIndex);
            
            List<Integer> obsPropColumnIndexes  = getColumnIndexes(csvFlatobsPropColumns, headers, directColumnIndex, laxHeader, maxIndex);

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

                // checks if row matches the observed properties filter
                String observedProperty = getMultiOrFixedValue(line, fixedObsId, obsPropColumnIndexes);
                if (!obspropColumns.contains(observedProperty)) {
                    continue;
                }

                String procId = extractWithRegex(procRegex, asString(line[procIndex]));
                result.add(procedureId + procId);
            }
            return result;

        } catch (IOException ex) {
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
        final Set<String> obspropColumnFilters = getObsPropColumns();

        // open csv file
        try (final DataFileReader reader = getDataFileReader()) {

            String[] headers = null;
            if (!noHeader) {
                headers = reader.getHeaders();
            }
            // sometimes in some files, the last columns are empty, and so do not appears in the line
            // so we want to consider a line as imcomplete only if the last index we look for is missing.
            final AtomicInteger maxIndex  = new AtomicInteger();
            final List<Integer> doubleFields = new ArrayList<>();

            final List<Integer> dateIndexes           = getColumnIndexes(dateColumns,          headers, directColumnIndex, laxHeader, maxIndex);
            final List<Integer> obsPropColumnIndexes  = getColumnIndexes(csvFlatobsPropColumns, headers, directColumnIndex, laxHeader, maxIndex);

            int procedureIndex   = getColumnIndex(procedureColumn,     headers, directColumnIndex, laxHeader, maxIndex);
            int procNameIndex    = getColumnIndex(procedureNameColumn, headers, directColumnIndex, laxHeader, maxIndex);
            int procDescIndex    = getColumnIndex(procedureDescColumn, headers, directColumnIndex, laxHeader, maxIndex);
            int typeColumnIndex  = getColumnIndex(typeColumn,          headers, directColumnIndex, laxHeader, maxIndex);

            int latitudeIndex    = getColumnIndex(latitudeColumn,      headers, doubleFields, directColumnIndex, laxHeader, maxIndex);
            int longitudeIndex   = getColumnIndex(longitudeColumn,     headers, doubleFields, directColumnIndex, laxHeader, maxIndex);

            // for line validation
            int valueColumnIndex = getColumnIndex(valueColumn,         headers, doubleFields, directColumnIndex, laxHeader, maxIndex);

            String fixedObsId    = obsPropIds.isEmpty()  ? null  : obsPropIds.get(0);

            final List<String> obsTypeCodes   = getObsTypeCodes();
            Map<String, ProcedureDataset> result = new LinkedHashMap<>();
            final Set<String> knownPositions     = new HashSet<>();
            Procedure previousProc               = null;
            ProcedureDataset currentPTree        = null;
            int lineNumber                    = 1;
            
            final Iterator<Object[]> it = reader.iterator(!noHeader);
            while (it.hasNext()) {
                lineNumber++;
                final Object[] line   = it.next();

                // verify that the line is complete (meaning that the line is at least as long as the last index we look for)
                if (verifyLineCompletion(line, lineNumber, headers, maxIndex)) {
                    LOGGER.finer("skipping empty line " + lineNumber);
                    continue;
                }

                // verify that the line is not empty (meaning the observed property is not empty)
                if (verifyEmptyLineStr(line, lineNumber, obsPropColumnIndexes)) {
                    LOGGER.fine("skipping line due to none expected variable present.");
                    continue;
                }

                // verify that the line is not empty (meaning that not all of the measure value selected are empty)
                if (verifyEmptyLine(line, lineNumber, doubleFields)) {
                    LOGGER.fine("skipping line due to none expected variable present.");
                    continue;
                }

                Date dateParse        = null;

                // checks if row matches the observed data types
                final String currentObstType;
                if (typeColumnIndex != -1) {
                    if (!obsTypeCodes.contains(asString(line[typeColumnIndex]))) continue;
                    currentObstType = getObservationTypeFromCode(asString(line[typeColumnIndex]));
                } else {
                    currentObstType = observationType;
                }

                final Procedure currentProc = parseProcedure(line, procedureIndex, procNameIndex, procDescIndex);
                if (currentProc == null) {
                    LOGGER.finer("skipping line due to null procedure.");
                    continue;
                }
                if (!query.getSensorIds().isEmpty() && !query.getSensorIds().contains(currentProc.getId())) {
                    LOGGER.finer("skipping line due to sensor filter.");
                    continue;
                }

                final String observedProperty = getMultiOrFixedValue(line, fixedObsId, obsPropColumnIndexes);
                
                // checks if row matches the observed properties wanted
                if (!obspropColumnFilters.contains(observedProperty)) {
                    continue;
                }

                if (previousProc == null || !Objects.equals(currentProc.getId(), previousProc.getId()) || currentPTree == null) {
                    currentPTree = result.computeIfAbsent(currentProc.getId(), pid -> new ProcedureDataset(currentProc.getId(), currentProc.getName(), currentProc.getDescription(), PROCEDURE_TREE_TYPE, currentObstType, new ArrayList<>(), null));
                }

                // add used field
                if (!currentPTree.fields.contains(observedProperty)) {
                    currentPTree.fields.add(observedProperty);
                }

                // update temporal interval
                if (!dateIndexes.isEmpty()) {
                    Optional<Long> d = parseDate(line, null, dateIndexes, new SimpleDateFormat(this.dateFormat), lineNumber);
                    if (d.isEmpty()) {
                        continue;
                    } else {
                        dateParse = new Date(d.get());
                    }
                    currentPTree.spatialBound.addDate(dateParse);
                }

                // update spatial information
                try {
                    final double[] position = extractLinePosition(latitudeIndex, longitudeIndex, currentProc.getId(), line);
                    if (position.length == 2) {
                        // only record when the sensor move
                        final String posKey = currentProc.getId() + '-' + position[0] + "_" + position[1];
                        if (!knownPositions.contains(posKey)) {
                            knownPositions.add(posKey);
                            Coordinate dp = new Coordinate(position[1], position[0]);
                            currentPTree.spatialBound.addLocation(dateParse, GF.createPoint(dp));
                        }
                    }
                } catch (NumberFormatException | ParseException ex) {
                    LOGGER.fine(String.format("Problem parsing lat/lon field at line %d.(Error msg='%s'). skipping line...", lineNumber, ex.getMessage()));
                    continue;
                }
                previousProc = currentProc;
            }

            return new ArrayList<>(result.values());
        } catch (IOException ex) {
            throw new DataStoreException("Problem reading csv file", ex);
        }
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
        return CsvFlatObservationStoreFactory.NAME;
    }
}
