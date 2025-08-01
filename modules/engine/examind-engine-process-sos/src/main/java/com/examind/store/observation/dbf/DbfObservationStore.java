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

import com.examind.store.observation.AbstractCsvStore;
import com.examind.store.observation.DataFileReader;
import static com.examind.store.observation.FileParsingUtils.*;
import com.examind.store.observation.MeasureField;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.geotoolkit.observation.model.Field;
import static org.geotoolkit.observation.model.FieldDataType.BOOLEAN;
import static org.geotoolkit.observation.model.FieldDataType.JSON;
import static org.geotoolkit.observation.model.FieldDataType.QUANTITY;
import static org.geotoolkit.observation.model.FieldDataType.TEXT;
import static org.geotoolkit.observation.model.FieldDataType.TIME;
import org.geotoolkit.observation.model.ObservationDataset;
import org.geotoolkit.observation.model.ProcedureDataset;
import org.geotoolkit.observation.model.Phenomenon;
import org.geotoolkit.observation.model.Procedure;
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
public class DbfObservationStore extends AbstractCsvStore {

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
            final List<Integer> mainIndexes      = getColumnIndexes(mainColumns,      headers, directColumnIndex, laxHeader, MAIN_QUALIFIER);
            final List<Integer> dateIndexes      = getColumnIndexes(dateColumns,      headers, directColumnIndex, laxHeader, DATE_QUALIFIER);
            final List<Integer> qualityIndexes   = getColumnIndexes(qualityColumns,   headers, directColumnIndex, laxHeader, QUALITY_QUALIFIER);
            final List<Integer> parameterIndexes = getColumnIndexes(parameterColumns, headers, directColumnIndex, laxHeader, PARAMETER_QUALIFIER);

            int latitudeIndex  = getColumnIndex(latitudeColumn,      headers, directColumnIndex, laxHeader);
            int longitudeIndex = getColumnIndex(longitudeColumn,     headers, directColumnIndex, laxHeader);
            int foiIndex       = getColumnIndex(foiColumn,           headers, directColumnIndex, laxHeader);
            int procIndex      = getColumnIndex(procedureColumn,     headers, directColumnIndex, laxHeader);
            int procNameIndex  = getColumnIndex(procedureNameColumn, headers, directColumnIndex, laxHeader);
            int procDescIndex  = getColumnIndex(procedureDescColumn, headers, directColumnIndex, laxHeader);
            int procPropMapIndex = getColumnIndex(procedurePropertiesMapColumn, headers, directColumnIndex, laxHeader);
            
            final Map<Integer, String> procMapIndexes = getNamedColumnIndexes(procedurePropertieColumns, headers, directColumnIndex,laxHeader);

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
            final List<Integer> obsPropIndexes = getColumnIndexes(obsPropColumns, headers, measureFields, directColumnIndex, laxHeader, OBS_PROP_QUALIFIER);

            if (noHeader && obsPropIds.size() != obsPropColumns.size()) {
                throw new DataStoreException("In noHeader mode, you must set fixed observated property ids");
            }

            final List<MeasureField> obsPropFields = getObsPropFields(obsPropIndexes, qualityIndexes, parameterIndexes, headers);

           // special case where there is no header, and a specified observation property identifier
            List<ObservedProperty> fixedObsProperties = getObservedProperties(measureFields);
            MeasureColumns measureColumns             = new MeasureColumns(obsPropFields, mainColumns, observationType);

             // final result
            final ObservationDataset result = new ObservationDataset();
            final Map<String, ObservationBlock> observationBlock = new LinkedHashMap<>();
            /*
            2- compute measures
            =================*/

            int lineNumber = 1;

            // spatial / temporal boundaries
            DateFormat sdf = null;
            if (this.dateFormat != null) {
                sdf = new SimpleDateFormat(this.dateFormat);
            }

            // -- single observation related variables --
            String currentFoi                     = null;
            Long currentTime                      = null;

            final Iterator<Object[]> it = reader.iterator(!noHeader);

            while (it.hasNext()) {
                lineNumber++;
                final Object[] line = it.next();

                // verify that the line is not empty (meaning that not all of the measure value selected are empty)
                if (verifyEmptyLine(line, lineNumber, obsPropIndexes)) {
                    LOGGER.fine("skipping line due to none expected variable present.");
                    continue;
                }

                // look for current procedure (for observation separation)
                final Procedure currentProc = parseProcedure(line, procIndex, procNameIndex, procDescIndex, procPropMapIndex, procMapIndexes);
                if (currentProc == null) {
                    LOGGER.finer("skipping line due to null procedure.");
                    continue;
                }
                if (!query.getSensorIds().isEmpty() && !query.getSensorIds().contains(currentProc.getId())) {
                    LOGGER.finer("skipping line due to sensor examind.");
                    continue;
                }

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

                ObservationBlock currentBlock = getOrCreateObservationBlock(observationBlock, currentProc, currentFoi, currentTime, measureColumns);

                currentBlock.updateObservedProperties(fixedObsProperties);

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
                    final double[] position = extractLinePosition(latitudeIndex, longitudeIndex, currentProc.getId(), line);
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

                // loop over columns to build measure string
                for (MeasureField field : obsPropFields) {
                    int index          = field.columnIndex;
                    Object value       = line[index];

                    try {
                        final Object measureValue;
                        if (value == null) {
                            measureValue = null;
                        } else {
                            measureValue = switch (field.dataType) {
                                case BOOLEAN  -> parseBoolean(value);
                                case QUANTITY -> parseDouble(value);
                                case TEXT     -> value instanceof String ? value : value.toString();
                                case TIME     -> parseObjectDate(value, sdf);
                                case JSON     -> parseMap(value);
                            };
                        }

                        Object[] qValues = new Object[field.qualityFields.size()];
                        for (int i = 0; i < qValues.length; i++) {
                            MeasureField qField = field.qualityFields.get(i);
                            qValues[i] = parseFieldValue(line[qField.columnIndex], qField.dataType, sdf);
                        }
                        Object[] pValues = new Object[field.parameterFields.size()];
                        for (int i = 0; i < pValues.length; i++) {
                            MeasureField pField = field.parameterFields.get(i);
                            pValues[i] = parseFieldValue(line[pField.columnIndex], pField.dataType, sdf);
                        }
                        
                        currentBlock.appendValue(mainValue, field.name, measureValue, lineNumber, qValues, pValues);
                    } catch (ParseException | NumberFormatException ex) {
                        if (!(line[index] instanceof String str && str.isEmpty())) {
                            LOGGER.fine(String.format("Problem parsing '%s value at line %d and column %d (value='%s')", field.dataType.toString(), lineNumber, index, line[index]));
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
            final String fileName = dataFileName;
            for (ObservationBlock ob : observationBlock.values()) {
                final String oid = fileName + '-' + obsCpt;
                obsCpt++;
                buildObservation(result, oid, ob, phenomenons, samplingFeatures, query.getResponseFormat());
            }
            return result;
            
        } catch (IOException | InterruptedException ex) {
            throw new DataStoreException("problem reading dbf file", ex);
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

            int procIndex = getColumnIndex(procedureColumn, headers, directColumnIndex, laxHeader);

            final Iterator<Object[]> it = reader.iterator(!noHeader);
            while (it.hasNext()) {
                final Object[] line = it.next();
                if (procIndex != -1) {
                    String procId = extractWithRegex(procRegex, asString(line[procIndex]));
                    result.add(procedureId + procId);
                }
            }
            return result;
        } catch (IOException | InterruptedException ex) {
            LOGGER.log(Level.WARNING, "problem reading csv file", ex);
            throw new DataStoreException(ex);
        }
    }

    @Override
    protected Set<String> extractPhenomenonIds() throws DataStoreException {
        // special case where the observation property identifiers are specified
        if (!obsPropIds.isEmpty()) {
            return new HashSet<>(obsPropIds);
        }

        // TODO verify existence?
        return obsPropColumns;
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
            final List<String> measureFields = new ArrayList<>();
            int latitudeIndex  = getColumnIndex(latitudeColumn,      headers, directColumnIndex, laxHeader);
            int longitudeIndex = getColumnIndex(longitudeColumn,     headers, directColumnIndex, laxHeader);
            int procedureIndex = getColumnIndex(procedureColumn,     headers, directColumnIndex, laxHeader);
            int procDescIndex  = getColumnIndex(procedureNameColumn, headers, directColumnIndex, laxHeader);

            final List<Integer> dateIndexes      = getColumnIndexes(dateColumns,      headers, directColumnIndex, laxHeader, DATE_QUALIFIER);
            final List<Integer> obsPropIndexes   = getColumnIndexes(obsPropColumns,   headers, directColumnIndex, laxHeader, OBS_PROP_QUALIFIER);
            final List<Integer> qualityIndexes   = getColumnIndexes(qualityColumns,   headers, directColumnIndex, laxHeader, QUALITY_QUALIFIER);
            final List<Integer> parameterIndexes = getColumnIndexes(parameterColumns, headers, directColumnIndex, laxHeader, PARAMETER_QUALIFIER);
            
            final List<MeasureField> obsPropFields = getObsPropFields(obsPropIndexes, qualityIndexes, parameterIndexes, headers);
            final List<Field> fields               = toFields(obsPropFields, observationType);
            

            // special case where there is no header, and a specified observation peorperty identifier
            if (directColumnIndex && noHeader && !obsPropIds.isEmpty()) {
                measureFields.addAll(obsPropIds);
            }
            
            DateFormat sdf = null;
            if (this.dateFormat != null) {
                sdf = new SimpleDateFormat(this.dateFormat);
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
                if (verifyEmptyLine(line, lineNumber, obsPropIndexes)) {
                    LOGGER.fine("skipping line due to none expected variable present.");
                    continue;
                }

                Date dateParse        = null;
                final String currentProc;
                if (procedureIndex != -1) {
                    final String procId = extractWithRegex(procRegex, asString(line[procedureIndex]));
                    currentProc = procedureId + procId;
                    if (!query.getSensorIds().isEmpty() && !query.getSensorIds().contains(currentProc)) {
                        LOGGER.finer("skipping line due to sensor examind.");
                        continue;
                    }
                } else {
                    currentProc = getProcedureID();
                }

                // look for current procedure description
                String currentProcDesc = asString(getColumnValue(procDescIndex, line, currentProc));

                if (!currentProc.equals(previousProc) || currentPTree == null) {
                    currentPTree = result.computeIfAbsent(currentProc, 
                            procedure -> new ProcedureDataset(procedure, 
                                                              currentProcDesc, 
                                                              null, 
                                                              PROCEDURE_TREE_TYPE, 
                                                              observationType.toLowerCase(),
                                                              fields, null));
                }

                // update temporal interval
                if (!dateIndexes.isEmpty()) {
                    Optional<Long> d = parseDate(line, null, dateIndexes, sdf, lineNumber);
                    if (d.isEmpty()) {
                        continue;
                    } else {
                        dateParse = new Date(d.get());
                    }
                    currentPTree.spatialBound.addDate(dateParse);
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
        } catch (IOException | InterruptedException ex) {
            LOGGER.log(Level.WARNING, "problem reading csv file", ex);
            throw new DataStoreException(ex);
        }
    }

    @Override
    protected String getStoreIdentifier() {
        return CsvObservationStoreFactory.NAME;
    }
}
