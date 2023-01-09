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

package com.examind.store.observation.dbf;

import com.examind.store.observation.DataFileReader;
import com.examind.store.observation.FileParsingObservationStore;
import static com.examind.store.observation.FileParsingUtils.*;
import com.examind.store.observation.ObservationBlock;
import com.examind.store.observation.ObservedProperty;
import com.examind.store.observation.csv.CsvObservationStoreFactory;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.geotoolkit.observation.ObservationStore;
import org.geotoolkit.observation.model.ObservationDataset;
import org.geotoolkit.observation.model.ProcedureDataset;
import org.geotoolkit.observation.model.Phenomenon;
import org.geotoolkit.observation.model.SamplingFeature;
import org.geotoolkit.observation.query.DatasetQuery;
import org.geotoolkit.storage.DataStores;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.opengis.parameter.ParameterValueGroup;

/**
 * Implementation of an observation store for csv observation data based on {@link CSVFeatureStore}.
 *
 * @author Samuel Andrés (Geomatys)
 */
public class DbfObservationStore extends FileParsingObservationStore implements ObservationStore {


    public DbfObservationStore(final ParameterValueGroup params) throws DataStoreException,IOException {
        super(params);
    }

    @Override
    public DataStoreProvider getProvider() {
        return DataStores.getProviderById(DbfObservationStoreFactory.NAME);
    }

    @Override
    public ObservationDataset getDataset(DatasetQuery query) throws DataStoreException {
        if (query.getAffectedSensorID() != null) {
            LOGGER.warning("DBFObservation store does not allow to override sensor ID");
        }
        try (final DataFileReader reader = getDataFileReader()) {

            final String[] headers = reader.getHeaders();

            // expected to contain headers information
            if (headers == null)  throw new DataStoreException("Missing headers");

            /*
            1- filter prepare spatial/time column indices from ordinary fields
            ================================================================*/
            final List<Integer> mainIndexes = getColumnIndexes(mainColumns, headers);
            final List<Integer> dateIndexes = getColumnIndexes(dateColumns, headers);


            int latitudeIndex  = getColumnIndex(latitudeColumn, headers);
            int longitudeIndex = getColumnIndex(longitudeColumn, headers);
            int foiIndex       = getColumnIndex(foiColumn, headers);
            int procIndex      = getColumnIndex(procedureColumn, headers);
            int procNameIndex  = getColumnIndex(procedureNameColumn, headers);
            int procDescIndex  = getColumnIndex(procedureDescColumn, headers);

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
            final List<Integer> doubleFields = getColumnIndexes(measureColumns, headers, measureFields);

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
            String currentProc                    = null;
            Long currentTime                      = null;

            final Iterator<Object[]> it = reader.iterator(!noHeader);

            while (it.hasNext()) {
                lineNumber++;
                final Object[] line = it.next();

                // verify that the line is not empty (meaning that not all of the measure value selected are empty)
                if (verifyEmptyLine(line, lineNumber, doubleFields)) {
                    LOGGER.fine("skipping line due to none expected variable present.");
                    continue;
                }

                // look for current procedure (for observation separation)
                if (procIndex != -1) {
                    final String procId = extractWithRegex(procRegex, (String) line[procIndex]);
                    currentProc = procedureId + procId;
                    if (!query.getSensorIds().isEmpty() && !query.getSensorIds().contains(currentProc)) {
                        LOGGER.finer("skipping line due to none specified sensor related.");
                        continue;
                    }
                } else {
                    currentProc = procedureId;
                }

                 // look for current procedure name
                String currentProcName = (String) getColumnValue(procNameIndex, line, currentProc);

                // look for current procedure description
                String currentProcDesc = (String) getColumnValue(procDescIndex, line, null);

                // look for current foi (for observation separation)
                currentFoi = Objects.toString(getColumnValue(foiIndex, line, currentFoi));

                // look for current date (for non timeseries observation separation)
                if (!dateIndexes.equals(mainIndexes)) {
                    Optional<Long> dateO = parseDate(line, null, dateIndexes, sdf, lineNumber);
                    if (dateO.isPresent()) {
                        currentTime = dateO.get();
                    } else {
                        continue;
                    }
                }

                ObservationBlock currentBlock = getOrCreateObservationBlock(currentProc, currentProcName, currentProcDesc, currentFoi, currentTime, measureFields, mainColumns, observationType, qualityColumns, qualityTypes);

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
                    throw new DataStoreException("In noHeader mdoe, you must set a fixed observated property id");
                } else if (noHeader && doubleFields.size() > 1) {
                    throw new DataStoreException("Multiple observated property is not yet supported In noHeader mode");
                }
                // loop over columns to build measure string
                for (int i : doubleFields) {
                    try {
                        double measureValue = parseDouble(line[i]);
                        String fieldName;
                        if (noHeader) {
                            fieldName = obsPropId;
                        } else {
                            fieldName = headers[i];
                        }
                        // TODO quality values
                        currentBlock.appendValue(mainValue, fieldName, measureValue, lineNumber, new String[0]);
                    } catch (ParseException | NumberFormatException ex) {
                        LOGGER.fine(String.format("Problem reading double value at line %d and column %d", lineNumber, i));
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
            LOGGER.log(Level.WARNING, "problem reading dbf file", ex);
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

            int procIndex = getColumnIndex(procedureColumn, headers);

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
    protected Set<String> extractPhenomenonIds() throws DataStoreException {
        if (procedureColumn == null) return Collections.singleton(getProcedureID());

        final Set<String> result = new HashSet();
        // open csv file
        try (final DataFileReader reader = getDataFileReader()) {

            String[] headers = null;
            if (!noHeader) {
                headers = reader.getHeaders();
            }

            int procIndex = getColumnIndex(procedureColumn, headers);

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
            LOGGER.log(Level.WARNING, "problem reading dbf file", ex);
            throw new DataStoreException(ex);
        }
    }

    @Override
    public List<ProcedureDataset> getProcedures() throws DataStoreException {
        // open csv file
        try (final DataFileReader reader = getDataFileReader()) {

            String[] headers = null;
            if (!noHeader) {
                headers = reader.getHeaders();
            }
            int lineNumber = 1;

            // prepare spatial/time column indices
            final List<String> measureFields = new ArrayList<>();
            final List<Integer> dateIndexes = getColumnIndexes(dateColumns, headers);
            int latitudeIndex = getColumnIndex(latitudeColumn, headers);
            int longitudeIndex = getColumnIndex(longitudeColumn, headers);
            int procedureIndex = getColumnIndex(procedureColumn, headers);
            int procDescIndex = getColumnIndex(procedureNameColumn, headers);

            // used to fill measure Fields list
            final List<Integer> doubleFields = getColumnIndexes(measureColumns, headers, measureFields);

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
                if (verifyEmptyLine(line, lineNumber, doubleFields)) {
                    LOGGER.fine("skipping line due to none expected variable present.");
                    continue;
                }

                Date dateParse        = null;
                final String currentProc;
                if (procedureIndex != -1) {
                    final String procId = extractWithRegex(procRegex, (String) line[procedureIndex]);
                    currentProc = procedureId + procId;
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
            LOGGER.log(Level.WARNING, "problem reading csv file", ex);
            throw new DataStoreException(ex);
        }
    }

    @Override
    protected String getStoreIdentifier() {
        return CsvObservationStoreFactory.NAME;
    }
}
