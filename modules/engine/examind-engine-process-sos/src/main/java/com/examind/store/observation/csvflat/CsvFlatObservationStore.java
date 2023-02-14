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
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.geotoolkit.gml.xml.AbstractGeometry;
import org.geotoolkit.gml.xml.GMLXmlFactory;
import org.geotoolkit.observation.ObservationStore;
import org.geotoolkit.observation.model.ExtractionResult;
import org.geotoolkit.observation.model.ExtractionResult.ProcedureTree;
import org.geotoolkit.storage.DataStores;
import org.geotoolkit.util.NamesExt;
import org.opengis.feature.FeatureType;
import org.opengis.geometry.DirectPosition;
import org.opengis.util.GenericName;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static com.examind.store.observation.csvflat.CsvFlatUtils.*;
import static com.examind.store.observation.FileParsingUtils.*;
import com.examind.store.observation.FileParsingObservationStore;
import com.examind.store.observation.ObservedProperty;
import org.constellation.exception.ConstellationStoreException;

/**
 * Implementation of an observation store for csv flat observation data based on {@link CSVFeatureStore}.
 *
 * @author Maxime Gavens (Geomatys)
 * @author Guilhem Legal (Geomatys)
 *
 */
public class CsvFlatObservationStore extends FileParsingObservationStore implements ObservationStore {

    private final String valueColumn;
    private final Set<String> obsPropColumns;
    private final Set<String> obsPropNameColumns;
    private final String typeColumn;
    private final String uomColumn;

    /**
     *
     * @param observationFile path to the csv observation file
     * @param separator character used as field separator
     * @param quotechar character used for quoted values
     * @param featureType the feature type
     * @param mainColumn the name (header) of the main column (date or pression/depth for profiles)
     * @param dateColumn the name (header) of the date column
     * @param dateTimeformat the date format (see {@link SimpleDateFormat})
     * @param longitudeColumn the name (header) of the longitude column
     * @param latitudeColumn the name (header) of the latitude column
     * @param obsPropFilterColumns the names (headers) of the measure columns
     * @param foiColumn the name (header) of the feature of interest column
     * @param valueColumn the name (header) of the measure column
     * @param obsPropColumns the names (header) of the code measure columns
     * 
     * @throws DataStoreException
     * @throws MalformedURLException
     */
    public CsvFlatObservationStore(final Path observationFile, final char separator, final char quotechar, final FeatureType featureType,
                                       final List<String> mainColumn, final List<String> dateColumn, final String dateTimeformat, final String longitudeColumn, final String latitudeColumn,
                                       final Set<String> obsPropFilterColumns, String observationType, String foiColumn, final String procedureId, final String procedureColumn, 
                                       final String procedureNameColumn, final String procedureDescColumn, final String procedureRegex, final String zColumn, final String uomColumn, final String uomRegex,
                                       final String valueColumn, final Set<String> obsPropColumns, final Set<String> obsPropNameColumns, final String typeColumn,  String obsPropRegex,
                                       final String mimeType, final String obsPropId, final String obsPropName, final boolean noHeader, final boolean directColumnIndex, final List<String> qualtityColumns,
                                       final List<String> qualityTypes) throws DataStoreException, MalformedURLException {
        super(observationFile, separator, quotechar, featureType, mainColumn, dateColumn, dateTimeformat, longitudeColumn, latitudeColumn, obsPropFilterColumns, observationType,
              foiColumn, procedureId, procedureColumn, procedureNameColumn, procedureDescColumn, procedureRegex, zColumn, uomRegex, obsPropRegex, obsPropId, obsPropName, mimeType,
              noHeader, directColumnIndex, qualtityColumns, qualityTypes);
        this.valueColumn = valueColumn;
        this.obsPropColumns = obsPropColumns;
        this.obsPropNameColumns = obsPropNameColumns;
        this.typeColumn = typeColumn;
        this.uomColumn = uomColumn;

        // special case for hard coded observed property
        if (obsPropId != null && !obsPropId.isEmpty()) {
            this.measureColumns = Collections.singleton(obsPropId);
        // special case for * measure columns
        // if the store is open with missing mime type we skip this part.
        } else if (obsPropFilterColumns.isEmpty() && mimeType != null) {
            try {
                this.measureColumns = extractCodes(mimeType, dataFile, obsPropColumns, separator, quotechar, noHeader, directColumnIndex);
            } catch (ConstellationStoreException ex) {
                throw new DataStoreException(ex);
            }
        } else {
             this.measureColumns = obsPropFilterColumns;
        }

    }

    @Override
    public DataStoreProvider getProvider() {
        return DataStores.getProviderById(CsvFlatObservationStoreFactory.NAME);
    }

    @Override
    protected Set<GenericName> extractProcedures() throws DataStoreException {

        final Set<GenericName> result = new HashSet();
        // open csv file
        try (final DataFileReader reader = getDataFileReader()) {

            String[] headers = null;
            if (!noHeader) {
                headers = reader.getHeaders();
            }

            // prepare procedure/type column indices
            int procIndex       = getColumnIndex(procedureColumn, headers);
            int typeColumnIndex = getColumnIndex(typeColumn, headers);


            final Iterator<String[]> it = reader.iterator(!noHeader);

            final List<String> obsTypeCodes = getObsTypeCodes();
            while (it.hasNext()) {
                final String[] line = it.next();
                if (procIndex != -1) {
                    // checks if row matches the observed data types
                    if (typeColumnIndex != -1) {
                        if (!obsTypeCodes.contains(line[typeColumnIndex])) continue;
                    }
                    String procId = extractWithRegex(procRegex, line[procIndex]);
                    result.add(NamesExt.create(procedureId + procId));
                }
            }
            return result;
            
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "problem reading csv file", ex);
            throw new DataStoreException(ex);
        }
    }

    @Override
    public ExtractionResult getResults(final String affectedSensorId, final List<String> sensorIDs) throws DataStoreException {

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
            
            int latitudeIndex    = getColumnIndex(latitudeColumn, headers, doubleFields);
            int longitudeIndex   = getColumnIndex(longitudeColumn, headers, doubleFields);
            int foiIndex         = getColumnIndex(foiColumn, headers);
            int zIndex           = getColumnIndex(zColumn, headers);
            int procIndex        = getColumnIndex(procedureColumn, headers);
            int procNameIndex    = getColumnIndex(procedureNameColumn, headers);
            int procDescIndex    = getColumnIndex(procedureDescColumn, headers);
            int valueColumnIndex = getColumnIndex(valueColumn, headers, doubleFields);
            int uomColumnIndex   = getColumnIndex(uomColumn, headers);
            int typeColumnIndex  = getColumnIndex(typeColumn, headers);

            List<Integer> dateIndexes              = getColumnIndexes(dateColumns, headers);
            List<Integer> mainIndexes              = getColumnIndexes(mainColumns, headers);
            List<Integer> obsPropColumnIndexes     = getColumnIndexes(obsPropColumns, headers);
            List<Integer> obsPropNameColumnIndexes = getColumnIndexes(obsPropNameColumns, headers);
            List<Integer> qualityIndexes           = getColumnIndexes(qualityColumns, headers);

            if (obsPropColumnIndexes.isEmpty()) {
                throw new DataStoreException("Unexpected columns code:" + Arrays.toString(obsPropColumns.toArray()));
            }
            if (valueColumnIndex == -1) {
                throw new DataStoreException("Unexpected column value:" + valueColumn);
            }
            if (mainIndexes.isEmpty() && observationType != null) {
                throw new DataStoreException("Unexpected column main:" + mainColumns);
            }

            // add measure column
            final List<String> sortedMeasureColumns = measureColumns.stream().sorted().collect(Collectors.toList());

            // final result
            final ExtractionResult result = new ExtractionResult();

            /*
            2- compute measures
            =================*/

            int lineNumber = 1;

            // spatial / temporal boundaries
            final DateFormat sdf = new SimpleDateFormat(this.dateFormat);

            // -- single observation related variables --
            String currentProc;
            Long currentTime                      = null;
            String currentFoi                     = null;
            List<String> currentMainColumns       = mainColumns;
            String currentObstType                = observationType;
            final List<String> obsTypeCodes       = getObsTypeCodes();

            final Iterator<String[]> it = reader.iterator(!noHeader);
            while (it.hasNext()) {
                lineNumber++;
                final String[] line = it.next();

                if (line.length == 0) {
                    LOGGER.fine("skipping empty line.");
                    continue;
                }

                // verify that the line is not empty (meaning that not all of the measure value selected are empty)
                if (verifyEmptyLine(line, lineNumber, doubleFields)) {
                    LOGGER.fine("skipping line due to none expected variable present.");
                    continue;
                }

                // checks if row matches the observed data types
                if (typeColumnIndex!=-1) {
                    if (!obsTypeCodes.contains(line[typeColumnIndex])) continue;
                    if (observationType == null) {
                        currentObstType = getObservationTypeFromCode(line[typeColumnIndex]);
                        if (currentObstType.equals("Profile")) {
                            mainIndexes = Arrays.asList(zIndex);
                            currentMainColumns = Arrays.asList(zColumn);
                        } else {
                            mainIndexes = dateIndexes;
                            currentMainColumns = dateColumns;
                        }
                    }
                }

                // look for current procedure (for observation separation)
                if (procIndex != -1) {
                    String procId = extractWithRegex(procRegex, line[procIndex]);
                    currentProc = procedureId + procId;
                    if (sensorIDs != null && !sensorIDs.isEmpty() && !sensorIDs.contains(currentProc)) {
                        LOGGER.finer("skipping line due to none specified sensor related.");
                        continue;
                    }
                } else {
                    currentProc = procedureId;
                }

                // look for current procedure name
                String currentProcName = getColumnValue(procNameIndex, line, currentProc);

                // look for current procedure description
                String currentProcDesc = getColumnValue(procDescIndex, line, null);

                // look for current foi (for observation separation)
                currentFoi = getColumnValue(foiIndex, line, currentFoi);

                // look for current date (for profile observation separation)
                if (!dateIndexes.equals(mainIndexes)) {
                    Optional<Long> dateO = parseDate(line, null, dateIndexes, sdf, lineNumber);
                    if (dateO.isPresent()) {
                        currentTime = dateO.get();
                    } else {
                        continue;
                    }
                }

                ObservedProperty observedProperty = parseObservedProperty(line, obsPropColumnIndexes, obsPropNameColumnIndexes, uomColumnIndex);

                // checks if row matches the observed properties wanted
                if (!sortedMeasureColumns.contains(observedProperty.id)) {
                    continue;
                }

                ObservationBlock currentBlock = getOrCreateObservationBlock(currentProc, currentProcName, currentProcDesc, currentFoi, currentTime, sortedMeasureColumns, currentMainColumns, currentObstType, qualityColumns, qualityTypes);

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
                    final double[] position = extractLinePosition(latitudeIndex, longitudeIndex, currentProc, line);
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
                    qualityValues[i] = line[qIndex];
                 }
                currentBlock.appendValue(mainValue, observedProperty.id, measureValue, lineNumber, qualityValues);
            }


            /*
            3- build results
            =============*/
            final Set<org.opengis.observation.Phenomenon> phenomenons = new HashSet<>();
            final Set<org.opengis.observation.sampling.SamplingFeature> samplingFeatures = new HashSet<>();
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

    protected ObservedProperty parseObservedProperty(String[] line, List<Integer> obsPropColumnIndexes, List<Integer> obsPropNameColumnIndexes, Integer uomColumnIndex) {
        String observedProperty     = getMultiOrFixedValue(line, obsPropId, obsPropColumnIndexes);
        String observedPropertyName = getMultiOrFixedValue(line, obsPropName, obsPropNameColumnIndexes);
        String observedPropertyUOM  = getColumnValue(uomColumnIndex, line, null);
        return new ObservedProperty(observedProperty, observedPropertyName, observedPropertyUOM);
    }

    @Override
    public Set<String> getPhenomenonNames() {
        return measureColumns;
    }

    @Override
    public List<ProcedureTree> getProcedures() throws DataStoreException {
        // open csv file
        try (final DataFileReader reader = getDataFileReader()) {

            String[] headers = null;
            if (!noHeader) {
                headers = reader.getHeaders();
            }

            final List<Integer> doubleFields         = new ArrayList<>();
            final List<Integer> dateIndexes           = getColumnIndexes(dateColumns, headers);
            final List<Integer> obsPropColumnIndexes  = getColumnIndexes(obsPropColumns, headers);
            int latitudeIndex   = getColumnIndex(latitudeColumn, headers, doubleFields);
            int longitudeIndex  = getColumnIndex(longitudeColumn, headers, doubleFields);
            int valueColumnIndex = getColumnIndex(valueColumn, headers, doubleFields);
            int procedureIndex  = getColumnIndex(procedureColumn, headers);
            int procDescIndex   = getColumnIndex(procedureNameColumn, headers);
            int typeColumnIndex = getColumnIndex(typeColumn, headers);

            final List<String> obsTypeCodes   = getObsTypeCodes();
            Map<String, ProcedureTree> result = new HashMap<>();
            final Set<String> knownPositions  = new HashSet<>();
            String previousProc               = null;
            ProcedureTree currentPTree        = null;
            int lineNumber                    = 1;
            
            final Iterator<String[]> it = reader.iterator(!noHeader);
            while (it.hasNext()) {
                lineNumber++;
                final String[] line   = it.next();

                if (line.length == 0) {
                    LOGGER.fine("skipping empty line.");
                    continue;
                }

                AbstractGeometry geom = null;
                Date dateParse        = null;

                // checks if row matches the observed data types
                final String currentObstType;
                if (typeColumnIndex != -1) {
                    if (!obsTypeCodes.contains(line[typeColumnIndex])) continue;
                    currentObstType = getObservationTypeFromCode(line[typeColumnIndex]);
                } else {
                    currentObstType = observationType;
                }

                // verify that the line is not empty (meaning that not all of the measure value selected are empty)
                if (verifyEmptyLine(line, lineNumber, doubleFields)) {
                    LOGGER.fine("skipping line due to none expected variable present.");
                    continue;
                }

                final String currentProc;
                if (procedureIndex != -1) {
                    String procId = extractWithRegex(procRegex, line[procedureIndex]);
                    currentProc = procedureId + procId;
                } else {
                    currentProc = getProcedureID();
                }

                final String observedProperty = getMultiOrFixedValue(line, obsPropId, obsPropColumnIndexes);
                
                // checks if row matches the observed properties wanted
                if (!measureColumns.contains(observedProperty)) {
                    continue;
                }

                // look for current procedure description
                final String currentProcDesc = getColumnValue(procDescIndex, line, currentProc);

                if (!currentProc.equals(previousProc) || currentPTree == null) {
                    currentPTree = result.computeIfAbsent(currentProc, procedure -> new ProcedureTree(procedure, currentProcDesc, null, PROCEDURE_TREE_TYPE, currentObstType, measureColumns));
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
                            DirectPosition dp = new GeneralDirectPosition(position[1], position[0]);
                            geom = GMLXmlFactory.buildPoint("3.2.1", null, dp);
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
        switch (observationType) {
            case "Timeserie" : return Arrays.asList("TS");
            case "Trajectory": return Arrays.asList("TR");
            case "Profile"   : return Arrays.asList("PR");
            default: throw new IllegalArgumentException("Unexpected observation type:" + observationType + ". Allowed values are Timeserie, Trajectory, Profile.");
        }
    }

    private String getObservationTypeFromCode(String code) {
        switch (code) {
            case "TS" : return "Timeserie";
            case "TR" : return "Trajectory";
            case "PR" : return "Profile";
            default: throw new IllegalArgumentException("Unexpected observation type code:" + code + ". Allowed values are TS, TR, PR.");
        }
    }
}
