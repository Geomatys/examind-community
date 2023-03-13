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

package com.examind.store.observation.csv;

import com.examind.store.observation.DataFileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.geotoolkit.observation.ObservationStore;
import org.geotoolkit.observation.model.ObservationDataset;
import org.geotoolkit.observation.model.ProcedureDataset;
import org.geotoolkit.storage.DataStores;

import static com.examind.store.observation.FileParsingUtils.*;
import com.examind.store.observation.FileParsingObservationStore;
import static com.examind.store.observation.FileParsingObservationStoreFactory.OBS_PROP_COLUMN_TYPE;
import static com.examind.store.observation.FileParsingObservationStoreFactory.getMultipleValuesList;
import com.examind.store.observation.ObservationBlock;
import com.examind.store.observation.ObservedProperty;
import java.util.Collections;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.geotoolkit.observation.model.FieldType;
import org.geotoolkit.observation.model.Phenomenon;
import org.geotoolkit.observation.model.SamplingFeature;
import org.geotoolkit.observation.query.DatasetQuery;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.opengis.parameter.ParameterValueGroup;

/**
 * Implementation of an observation store for csv observation data based on {@link CSVFeatureStore}.
 *
 * @author Samuel Andrés (Geomatys)
 * @author Guilhem Legal (Geomatys)
 *
 */
public class CsvObservationStore extends FileParsingObservationStore implements ObservationStore {

    protected List<String> obsPropColumnsTypes;

    public CsvObservationStore(final ParameterValueGroup params) throws DataStoreException,IOException {
        super(params);
        this.obsPropColumnsTypes = getMultipleValuesList(params, OBS_PROP_COLUMN_TYPE.getName().getCode());
    }

    @Override
    public DataStoreProvider getProvider() {
        return DataStores.getProviderById(CsvObservationStoreFactory.NAME);
    }

    @Override
    public ObservationDataset getDataset(final DatasetQuery query) throws DataStoreException {

        // open csv file
        try (final DataFileReader reader = getDataFileReader()) {

            String[] headers = null;
            if (!noHeader) {
                headers = reader.getHeaders();
            }

            /*
            1- filter prepare spatial/time column indices from ordinary fields
            ================================================================*/
            int latitudeIndex  = getColumnIndex(latitudeColumn,      headers, directColumnIndex);
            int longitudeIndex = getColumnIndex(longitudeColumn,     headers, directColumnIndex);
            int foiIndex       = getColumnIndex(foiColumn,           headers, directColumnIndex);
            int procIndex      = getColumnIndex(procedureColumn,     headers, directColumnIndex);
            int procNameIndex  = getColumnIndex(procedureNameColumn, headers, directColumnIndex);
            int procDescIndex  = getColumnIndex(procedureDescColumn, headers, directColumnIndex);

            final List<Integer> dateIndexes = getColumnIndexes(dateColumns, headers, directColumnIndex);
            final List<Integer> mainIndexes = getColumnIndexes(mainColumns, headers, directColumnIndex);

            if (mainIndexes.isEmpty()) {
                throw new DataStoreException("Unexpected column main:" + mainColumns);
            }

            final List<String> measureFields = new ArrayList<>();
            if ("Profile".equals(observationType))   {
                if (mainColumns.size() > 1) {
                    throw new DataStoreException("Multiple main columns is not yet supported for Profile");
                }
                measureFields.add(mainColumns.get(0));
            }
            final List<Integer> obsPropIndexes = getColumnIndexes(obsPropColumns, headers, measureFields, directColumnIndex);
            if (obsPropIndexes.isEmpty()) {
                throw new DataStoreException("No observed properties columns have been found in the headers: "+ obsPropColumns.stream().collect(Collectors.joining(", ", "[ ", " ]")));
            }
            final Map<Integer, FieldType> obsPropFields = new HashMap<>();
            for (int i = 0; i < obsPropIndexes.size(); i++) {
                FieldType ft = FieldType.QUANTITY;
                if (i < obsPropColumnsTypes.size()) {
                    ft = FieldType.valueOf(obsPropColumnsTypes.get(i));
                }
                obsPropFields.put(obsPropIndexes.get(i), ft);
            }

            // special case where there is no header, and a specified observation peorperty identifier
            ObservedProperty fixedObsProp = null;
            if (directColumnIndex && noHeader && obsPropId != null) {
                measureFields.add(obsPropId);
                fixedObsProp = new ObservedProperty(obsPropId, obsPropName, null);
            }
            
             // final result
            final ObservationDataset result = new ObservationDataset();

            /*
            2- compute measures
            =================*/

            int lineNumber = 1;

            // spatial / temporal boundaries
            final DateFormat sdf = new SimpleDateFormat(this.dateFormat);

            // -- single observation related variables --
            String currentFoi                     = null;
            Long currentTime                      = null;

            final Iterator<Object[]> it = reader.iterator(!noHeader);
            while (it.hasNext()) {
                lineNumber++;
                final Object[] line = it.next();

                // verify that the line is not empty (meaning that not all of the measure value selected are empty)
                if (verifyEmptyLine(line, lineNumber, obsPropFields, sdf)) {
                    LOGGER.fine("skipping line due to none expected variable present.");
                    continue;
                }

                // look for current procedure (for observation separation)
                String currentProc;
                if (procIndex != -1) {
                    final String procId = extractWithRegex(procRegex, (String) line[procIndex]);
                    currentProc = procedureId + procId;
                    if (!query.getSensorIds().isEmpty() && !query.getSensorIds().contains(currentProc)) {
                        LOGGER.finer("skipping line due to sensor filter.");
                        continue;
                    }
                } else {
                    currentProc = getProcedureID();
                }

                // look for current procedure name
                String currentProcName = (String) getColumnValue(procNameIndex, line, currentProc);

                // look for current procedure description
                String currentProcDesc = (String) getColumnValue(procDescIndex, line, null);

                // look for current foi (for observation separation)
                currentFoi = (String) getColumnValue(foiIndex, line, currentFoi);

                // look for current date (for non timeseries observation separation)
                if (!dateIndexes.equals(mainIndexes)) {
                    Optional<Long> dateO = parseDate(line, null, dateIndexes, sdf, lineNumber);
                    if (dateO.isPresent()) {
                        currentTime = dateO.get();
                    } else {
                        continue;
                    }
                }

                ObservationBlock currentBlock = getOrCreateObservationBlock(currentProc, currentProcName, currentProcDesc, currentFoi, currentTime, measureFields, obsPropColumnsTypes, mainColumns, observationType, qualityColumns, qualityTypes);

                if (fixedObsProp != null) {
                    currentBlock.updateObservedProperty(fixedObsProp);
                }
                
                /*
                a- build spatio-temporal information
                ==================================*/

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
                    final double[] position = extractLinePosition(latitudeIndex, longitudeIndex, currentProc, line);
                    if (position.length == 2) {
                        final double latitude = position[0];
                        final double longitude = position[1];
                        result.spatialBound.addXYCoordinate(longitude, latitude);
                        currentBlock.addPosition(millis, latitude, longitude);
                    }
                } catch (ParseException | NumberFormatException ex) {
                    LOGGER.fine(String.format("Problem parsing lat/lon for date field at line %d (Error msg='%s'). skipping line...", lineNumber, ex.getMessage()));
                    continue;
                }
                /*
                b- build measure string
                =====================*/

                // add main field
                Optional<? extends Number> mainO = parseMain(line, millis, mainIndexes, sdf, lineNumber, observationType);
                Number mainValue ;
                if (mainO.isPresent()) {
                    mainValue = mainO.get();
                } else {
                    continue;
                }

                if (noHeader && obsPropId == null) {
                    throw new DataStoreException("In noHeader mode, you must set a fixed observated property id");
                } else if (noHeader && obsPropFields.size() > 1) {
                    throw new DataStoreException("Multiple observed property is not yet supported In noHeader mode");
                }
                // loop over columns to build measure string
                for (Map.Entry<Integer, FieldType> field : obsPropFields.entrySet()) {
                    int i = field.getKey();
                    FieldType ft = field.getValue();
                    if (ft == null) throw new IllegalStateException("FIeld type sould never be null");

                    try {
                        final Object measureValue;
                        if (line[i] == null) {
                            measureValue = null;
                        } else {
                            measureValue = switch (ft) {
                                case BOOLEAN  -> parseBoolean(line[i]);
                                case QUANTITY -> parseDouble(line[i]);
                                case TEXT     -> line[i] instanceof String ? line[i] : line[i].toString();
                                case TIME     -> parseObjectDate(line[i], sdf);
                            };
                        }

                        String fieldName;
                        if (noHeader) {
                            fieldName = obsPropId;
                        } else {
                            fieldName = headers[i];
                        }
                        // TODO quality values
                        currentBlock.appendValue(mainValue, fieldName, measureValue, lineNumber, new String[0]);
                    } catch (ParseException | NumberFormatException ex) {
                        if (!(line[i] instanceof String str && str.isEmpty())) {
                            LOGGER.fine(String.format("Problem parsing '%s value at line %d and column %d (value='%s')", field.getValue().toString(), lineNumber, i, line[i]));
                        }
                    }
                }
            }


            /*
            3- build result
            =============*/
            final Set<Phenomenon> phenomenons = new HashSet<>();
            final Set<SamplingFeature> samplingFeatures = new HashSet<>();
            int obsCpt = 0;
            final String fileName = dataFile.getFileName().toString();
            for (ObservationBlock ob : observationBlock.values()) {
                final String oid = fileName + '-' + obsCpt;
                obsCpt++;
                buildObservation(result, oid, ob, phenomenons, samplingFeatures);
            }
            return result;
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "problem reading csv file", ex);
            throw new DataStoreException(ex);
        }
    }


    @Override
    protected Set<String> extractProcedureIds() throws DataStoreException {
        if (procedureColumn == null) return Collections.singleton(getProcedureID());
        
        final Set<String> result = new HashSet();
        // open csv file
        try (final DataFileReader reader = getDataFileReader()) {

            String[] headers = null;
            if (!noHeader) {
                headers = reader.getHeaders();
            }

            int procIndex = getColumnIndex(procedureColumn, headers, directColumnIndex);

            final Iterator<Object[]> it = reader.iterator(!noHeader);
            while (it.hasNext()) {
                final Object[] line = it.next();
                if (procIndex != -1) {
                    String procId = extractWithRegex(procRegex, (String) line[procIndex]);
                    result.add(procedureId + procId);
                }
            }
            return result;
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "problem reading csv file", ex);
            throw new DataStoreException(ex);
        }
    }

    @Override
    public Set<String> extractPhenomenonIds() throws DataStoreException {
        // open csv file
        try (final DataFileReader reader = getDataFileReader()) {

            // read headers
            String[] headers = null;
            if (!noHeader) {
                headers = reader.getHeaders();
            }
            final Set<String> measureFields = new HashSet<>();

            // used to fill measure Fields list
            getColumnIndexes(obsPropColumns, headers, measureFields, directColumnIndex);

            // special case where there is no header, and a specified observation peorperty identifier
            if (directColumnIndex && noHeader && obsPropId != null) {
                measureFields.add(obsPropId);
            }
            return measureFields;
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "problem reading csv file", ex);
            throw new DataStoreException(ex);
        }
    }

    @Override
    public List<ProcedureDataset> getProcedureDatasets(DatasetQuery query) throws DataStoreException {
        // open csv file
        try (final DataFileReader reader = getDataFileReader()) {

            String[] headers = null;
            if (!noHeader) {
                headers = reader.getHeaders();
            }
            int lineNumber = 1;

            // prepare spatial/time column indices
            final DateFormat sdf = new SimpleDateFormat(this.dateFormat);
            final List<String> measureFields = new ArrayList<>();
            
            int latitudeIndex  = getColumnIndex(latitudeColumn,      headers, directColumnIndex);
            int longitudeIndex = getColumnIndex(longitudeColumn,     headers, directColumnIndex);
            int procedureIndex = getColumnIndex(procedureColumn,     headers, directColumnIndex);
            int procDescIndex  = getColumnIndex(procedureNameColumn, headers, directColumnIndex);

            final List<Integer> dateIndexes = getColumnIndexes(dateColumns, headers, directColumnIndex);
            // used to fill measure Fields list
            final List<Integer> obsPropIndexes = getColumnIndexes(obsPropColumns, headers, measureFields, directColumnIndex);
            final Map<Integer, FieldType> obsPropFields = new HashMap<>();
            for (int i = 0; i < obsPropIndexes.size(); i++) {
                FieldType ft = FieldType.QUANTITY;
                if (i < obsPropColumnsTypes.size()) {
                    ft = FieldType.valueOf(obsPropColumnsTypes.get(i));
                }
                obsPropFields.put(obsPropIndexes.get(i), ft);
            }

            // special case where there is no header, and a specified observation peorperty identifier
            if (directColumnIndex && noHeader && obsPropId != null) {
                measureFields.add(obsPropId);
            }

            Map<String, ProcedureDataset> result = new HashMap<>();
            final Set<String> knownPositions  = new HashSet<>();
            String previousProc               = null;
            ProcedureDataset currentPTree        = null;
            final Iterator<Object[]> it = reader.iterator(!noHeader);
            while (it.hasNext()) {
                lineNumber++;
                final Object[] line   = it.next();

                // verify that the line is not empty (meaning that not all of the measure value selected are empty)
                if (verifyEmptyLine(line, lineNumber, obsPropFields, sdf)) {
                    LOGGER.fine("skipping line due to none expected variable present.");
                    continue;
                }
                
                Date dateParse        = null;
                final String currentProc;
                if (procedureIndex != -1) {
                    final String procId = extractWithRegex(procRegex, (String) line[procedureIndex]);
                    currentProc = procedureId + procId;
                    if (!query.getSensorIds().isEmpty() && !query.getSensorIds().contains(currentProc)) {
                        LOGGER.finer("skipping line due to sensor filter.");
                        continue;
                    }
                } else {
                    currentProc = getProcedureID();
                }

                // look for current procedure description
                String currentProcDesc = (String) getColumnValue(procDescIndex, line, currentProc);

                if (!currentProc.equals(previousProc) || currentPTree == null) {
                    currentPTree = result.computeIfAbsent(currentProc, procedure -> new ProcedureDataset(procedure, currentProcDesc, null, PROCEDURE_TREE_TYPE, observationType.toLowerCase(), measureFields, null));
                }

                // update temporal interval
                if (!dateIndexes.isEmpty()) {
                    String value = "";
                    try {
                        for (Integer dateIndex : dateIndexes) {
                            value += line[dateIndex];
                        }
                        dateParse = new SimpleDateFormat(this.dateFormat).parse(value);
                        currentPTree.spatialBound.addDate(dateParse);
                    } catch (ParseException ex) {
                        LOGGER.fine(String.format("Problem parsing date for main field at line %d (value='%s'). skipping line...", lineNumber, value));
                        continue;
                    }
                }

                 // update spatial information
                try {
                    final double[] position = extractLinePosition(latitudeIndex, longitudeIndex, currentProc, line);
                    if (position.length == 2) {
                        // only record when the sensor move
                        final String posKey = currentProc + '-' + position[0] + "_" + position[1];
                        if (!knownPositions.contains(posKey)) {
                            knownPositions.add(posKey);
                            Coordinate dp = new Coordinate(position[1], position[0]);
                            Geometry geom = GF.createPoint(dp);
                            currentPTree.spatialBound.addLocation(dateParse, geom);
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

    @Override
    protected String getStoreIdentifier() {
        return CsvObservationStoreFactory.NAME;
    }
}