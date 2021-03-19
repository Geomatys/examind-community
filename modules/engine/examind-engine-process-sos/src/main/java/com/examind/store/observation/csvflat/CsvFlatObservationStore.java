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

import com.examind.store.observation.MeasureBuilder;
import com.examind.store.observation.ObservationBlock;
import com.opencsv.CSVReader;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.util.collection.BackingStoreException;
import org.geotoolkit.gml.xml.AbstractGeometry;
import org.geotoolkit.gml.xml.GMLXmlFactory;
import org.geotoolkit.observation.ObservationStore;
import org.geotoolkit.sos.netcdf.ExtractionResult;
import org.geotoolkit.sos.netcdf.ExtractionResult.ProcedureTree;
import org.geotoolkit.sos.netcdf.GeoSpatialBound;
import org.geotoolkit.storage.DataStores;
import org.geotoolkit.util.NamesExt;
import org.opengis.feature.FeatureType;
import org.opengis.geometry.DirectPosition;
import org.opengis.temporal.TemporalGeometricPrimitive;
import org.opengis.util.GenericName;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
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
    private final Set<String> codeColumns;
    private final String typeColumn;

    /**
     *
     * @param observationFile path to the csv observation file
     * @param separator character used as field separator
     * @param featureType the feature type
     * @param mainColumn the name (header) of the main column (date or pression/depth for profiles)
     * @param dateColumn the name (header) of the date column
     * @param dateTimeformat the date format (see {@link SimpleDateFormat})
     * @param longitudeColumn the name (header) of the longitude column
     * @param latitudeColumn the name (header) of the latitude column
     * @param measureColumns the names (headers) of the measure columns
     * @param foiColumn the name (header) of the feature of interest column
     * @param valueColumn the name (header) of the measure column
     * @param codeColumns the names (header) of the code measure columns
     * 
     * @throws DataStoreException
     * @throws MalformedURLException
     */
    public CsvFlatObservationStore(final Path observationFile, final char separator, final char quotechar, final FeatureType featureType,
                                       final String mainColumn, final String dateColumn, final String dateTimeformat, final String longitudeColumn, final String latitudeColumn,
                                       final Set<String> measureColumns, String observationType, String foiColumn, final String procedureId, final String procedureColumn,
                                       final boolean extractUom, final String valueColumn, final Set<String> codeColumns, final String typeColumn) throws DataStoreException, MalformedURLException {
        super(observationFile, separator, quotechar, featureType, mainColumn, dateColumn, dateTimeformat, longitudeColumn, latitudeColumn, measureColumns, observationType, foiColumn, procedureId, procedureColumn, extractUom);
        this.valueColumn = valueColumn;
        this.codeColumns = codeColumns;
        this.typeColumn = typeColumn;

        // special case for * measure columns
        if (measureColumns.size() == 1 && measureColumns.iterator().next().equals("*")) {
            try {
                this.measureColumns = extractCodes(dataFile, codeColumns, separator);
            } catch (ConstellationStoreException ex) {
                throw new DataStoreException(ex);
            }
        } else {
             this.measureColumns = measureColumns;
        }

    }

    @Override
    public DataStoreProvider getProvider() {
        return DataStores.getProviderById(CsvFlatObservationStoreFactory.NAME);
    }

    @Override
    protected Set<GenericName> extractProcedures() {

        final Set<GenericName> result = new HashSet();
        // open csv file
        try (final CSVReader reader = new CSVReader(Files.newBufferedReader(dataFile), delimiter, quotechar)) {

            final Iterator<String[]> it = reader.iterator();

            // at least one line is expected to contain headers information
            if (it.hasNext()) {

                // prepare procedure/type column indices
                int procIndex = -1;
                int typeColumnIndex = -1;

                // read headers
                final String[] headers = it.next();
                for (int i = 0; i < headers.length; i++) {
                    final String header = headers[i];
                    if (header.equals(procedureColumn)) {
                        procIndex = i;
                    } else if (header.equals(typeColumn)) {
                        typeColumnIndex = i;
                    }
                }
                
                final String obsTypeCode = getObsTypeCode();
                while (it.hasNext()) {
                    final String[] line = it.next();
                    if (procIndex != -1) {
                        // checks if row matches the observed data types
                        if (typeColumnIndex != -1) {
                            if (!line[typeColumnIndex].equals(obsTypeCode)) continue;
                        }
                        result.add(NamesExt.create(procedureId + line[procIndex]));
                    }
                }
                return result;
            }
            throw new BackingStoreException("csv headers not found");
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "problem reading csv file", ex);
            throw new BackingStoreException(ex);
        }
    }

    @Override
    public ExtractionResult getResults(final String affectedSensorId, final List<String> sensorIDs,
            final Set<org.opengis.observation.Phenomenon> phenomenons, final Set<org.opengis.observation.sampling.SamplingFeature> samplingFeatures) throws DataStoreException {

        int obsCpt = 0;
        final String fileName = dataFile.getFileName().toString();
        
        // open csv file with a delimiter set as process SosHarvester input.
        try (final CSVReader reader = new CSVReader(Files.newBufferedReader(dataFile), delimiter, quotechar)) {

            final Iterator<String[]> it = reader.iterator();

            // at least one line is expected to contain headers information
            if (it.hasNext()) {

                /*
                1- filter prepare spatial/time column indices from ordinary fields
                ================================================================*/
                int mainIndex = -1;
                int dateIndex = -1;
                int latitudeIndex = -1;
                int longitudeIndex = -1;
                int foiIndex = -1;
                int procIndex = -1;
                int valueColumnIndex = -1;
                List<Integer> codeColumnIndexes = new ArrayList<>();
                int typeColumnIndex = -1;

                // read headers
                final String[] headers = it.next();
                final List<Integer> doubleFields = new ArrayList<>();

                for (int i = 0; i < headers.length; i++) {
                    final String header = headers[i];
                    if (header.equals(mainColumn)) {
                        mainIndex = i;
                    }
                    if (header.equals(foiColumn)) {
                        foiIndex = i;
                    }
                    if (header.equals(dateColumn)) {
                        dateIndex = i;
                    }
                    if (header.equals(latitudeColumn)) {
                        latitudeIndex = i;
                        doubleFields.add(i);
                    }
                    if (header.equals(longitudeColumn)) {
                        longitudeIndex = i;
                        doubleFields.add(i);
                    }
                    if (header.equals(valueColumn)) {
                        valueColumnIndex = i;
                        doubleFields.add(i);
                    }
                    if (codeColumns.contains(header)) {
                        codeColumnIndexes.add(i);
                    }
                    if (header.equals(typeColumn)) {
                        typeColumnIndex = i;
                    }
                    if (header.equals(procedureColumn)) {
                        procIndex = i;
                    }
                }

                if (codeColumnIndexes.isEmpty()) {
                    throw new DataStoreException("Unexpected columns code:" + Arrays.toString(codeColumns.toArray()));
                }
                if (valueColumnIndex == -1) {
                    throw new DataStoreException("Unexpected column value:" + valueColumn);
                }
                if (mainIndex == -1) {
                    throw new DataStoreException("Unexpected column main:" + mainColumn);
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
                String currentFoi                     = null;
                String currentProc                    = null;
                Long currentTime                      = null;
                final String obsTypeCode              = getObsTypeCode();
                
                // measure map used to collect measure data then construct the MeasureStringBuilder
                final MeasureBuilder template = new MeasureBuilder(obsTypeCode.equals("PR"), sortedMeasureColumns, mainColumn);

                while (it.hasNext()) {
                    lineNumber++;
                    final String[] line = it.next();

                    // verify that the line is not empty (meaning that not all of the measure value selected are empty)
                    if (verifyEmptyLine(line, lineNumber, doubleFields)) {
                        LOGGER.info("skipping line due to none expected variable present.");
                        continue;
                    }
                    
                    // checks if row matches the observed data types
                    if (typeColumnIndex!=-1 && !line[typeColumnIndex].equals(obsTypeCode)) continue;
                    
                    // look for current procedure (for observation separation)
                    if (procIndex != -1) {
                        currentProc = procedureId + line[procIndex];
                        if (sensorIDs != null && !sensorIDs.isEmpty() && !sensorIDs.contains(currentProc)) {
                            LOGGER.finer("skipping line due to none specified sensor related.");
                            continue;
                        }
                    } else {
                        currentProc = procedureId;
                    }
                    
                    // look for current foi (for observation separation)
                    if (foiIndex != -1) {
                        currentFoi = line[foiIndex];
                    }

                    // look for current date (for profile observation separation)
                    if (dateIndex != mainIndex) {
                        try {
                            currentTime = sdf.parse(line[dateIndex]).getTime();
                        } catch (ParseException ex) {
                            LOGGER.warning(String.format("Problem parsing date for date field at line %d and column %d (value='%s'). skipping line...", lineNumber, dateIndex, line[dateIndex]));
                            continue;
                        }
                    }

                    // Concatenate values from input code columns
                    String concatenatedCodeColumnsValues = "";
                    boolean first = true;
                    for (Integer codeColumnIndex : codeColumnIndexes) {
                        if (!first) {
                            concatenatedCodeColumnsValues += "-";
                        }
                        concatenatedCodeColumnsValues += line[codeColumnIndex];
                        first = false;
                    }

                    // checks if row matches the observed properties wanted
                    if (!sortedMeasureColumns.contains(concatenatedCodeColumnsValues)) {
                        continue;
                    }

                    ObservationBlock currentBlock = getOrCreateObservationBlock(currentProc, currentFoi, currentTime, template);

                    // update temporal interval
                    Long millis = null;
                    if (dateIndex != -1) {
                        try {
                            if (currentTime != null) {
                                millis = currentTime;
                            } else {
                                millis = sdf.parse(line[dateIndex]).getTime();
                            }
                            result.spatialBound.addDate(millis);
                            currentBlock.addDate(millis);
                        } catch (ParseException ex) {
                            LOGGER.warning(String.format("Problem parsing date for date field at line %d and column %d (value='%s'). skipping line...", lineNumber, dateIndex, line[dateIndex]));
                            continue;
                        }
                    }

                    // update spatial information
                    if (latitudeIndex != -1 && longitudeIndex != -1) {
                        final double longitude = parseDouble(line[longitudeIndex]);
                        final double latitude = parseDouble(line[latitudeIndex]);
                        result.spatialBound.addXYCoordinate(longitude, latitude);
                        currentBlock.addPosition(millis, latitude, longitude);
                    }

                    // parse main value
                    Number mainValue;
                    try {
                         // assume that for profile main field is a double
                        if (obsTypeCode.equals("PR")) {
                            mainValue = parseDouble(line[mainIndex]);

                        // assume that is a date otherwise
                        } else {
                            // little optimization if date column == main column
                            if (millis != null) {
                                mainValue = millis;
                            } else {
                                mainValue = sdf.parse(line[mainIndex]).getTime();
                            }
                        }
                    } catch (ParseException | NumberFormatException ex) {
                        LOGGER.warning(String.format("Problem parsing date/double for main field at line %d and column %d (value='%s'). skipping line...", lineNumber, mainIndex, line[mainIndex]));
                        continue;
                    }

                    // parse Measure value
                    double measureValue;
                    try {
                        measureValue = parseDouble(line[valueColumnIndex]);
                    } catch (ParseException | NumberFormatException ex) {
                        LOGGER.warning(String.format("Problem parsing double for measure field at line %d and column %d (value='%s'). skipping line...", lineNumber, valueColumnIndex, line[valueColumnIndex]));
                        continue;
                    }
                    currentBlock.appendValue(mainValue, concatenatedCodeColumnsValues, measureValue, lineNumber);
                }


                /*
                3- build results
                =============*/
                for (ObservationBlock ob : observationBlock.values()) {
                    final String oid = fileName + '-' + obsCpt;
                    obsCpt++;
                    buildObservation(result, oid, ob, phenomenons, samplingFeatures);
                }
                return result;
            }
            throw new DataStoreException("csv headers not found");
        } catch (IOException | ParseException ex) {
            LOGGER.log(Level.WARNING, "problem reading csv file", ex);
            throw new DataStoreException(ex);
        }
    }

    @Override
    public Set<String> getPhenomenonNames() {
        return measureColumns;
    }

    @Override
    public TemporalGeometricPrimitive getTemporalBounds() throws DataStoreException {

        final GeoSpatialBound result = new GeoSpatialBound();
        // open csv file
        try (final CSVReader reader = new CSVReader(Files.newBufferedReader(dataFile), delimiter, quotechar)) {

            final Iterator<String[]> it = reader.iterator();

            // at least one line is expected to contain headers information
            if (it.hasNext()) {

                // prepare time column indices
                int dateIndex = -1;

                // read headers
                final String[] headers = it.next();
                for (int i = 0; i < headers.length; i++) {
                    final String header = headers[i];
                    if (dateColumn.equals(header)) {
                        dateIndex = i;
                    }
                }

                while (it.hasNext()) {
                    final String[] line = it.next();
                    // update temporal information
                    if (dateIndex != -1) {
                        final Date dateParse = new SimpleDateFormat(this.dateFormat).parse(line[dateIndex]);
                        result.addDate(dateParse);
                    }
                }
                return result.getTimeObject("2.0.0");
            }
            throw new DataStoreException("csv headers not found");
        } catch (IOException | ParseException ex) {
            LOGGER.log(Level.WARNING, "problem reading csv file", ex);
            throw new DataStoreException(ex);
        }
    }

    @Override
    public List<ProcedureTree> getProcedures() throws DataStoreException {
        // open csv file
        try (final CSVReader reader = new CSVReader(Files.newBufferedReader(dataFile), delimiter, quotechar)) {

            final Iterator<String[]> it = reader.iterator();
            int count = 0;

            // at least one line is expected to contain headers information
            if (it.hasNext()) {
                count++;

                // prepare spatial/time column indices
                int dateIndex = -1;
                int latitudeIndex = -1;
                int longitudeIndex = -1;
                int procedureIndex = -1;
                int typeColumnIndex = -1;

                // read headers
                final String[] headers = it.next();
                for (int i = 0; i < headers.length; i++) {
                    final String header = headers[i];

                    if (dateColumn.equals(header)) {
                        dateIndex = i;
                    } else if (latitudeColumn.equals(header)) {
                        latitudeIndex = i;
                    } else if (longitudeColumn.equals(header)) {
                        longitudeIndex = i;
                    } else if (header.equals(procedureColumn)) {
                        procedureIndex = i;
                    } else if (header.equals(typeColumn)) {
                        typeColumnIndex = i;
                    }
                }

                final String obsTypeCode   = getObsTypeCode();
                List<ProcedureTree> result = new ArrayList<>();
                String currentProc          = null;
                String previousProc         = null;
                ProcedureTree procedureTree = null;
                while (it.hasNext()) {
                    final String[] line   = it.next();
                    AbstractGeometry geom = null;
                    Date dateParse        = null;
                    
                    // checks if row matches the observed data types
                    if (typeColumnIndex != -1) {
                        if (!line[typeColumnIndex].equals(obsTypeCode)) continue;
                    }

                    if (procedureIndex != -1) {
                        currentProc = procedureId + line[procedureIndex];
                        if (!currentProc.equals(previousProc)) {
                            procedureTree = new ProcedureTree(currentProc, PROCEDURE_TREE_TYPE, observationType.toLowerCase(), measureColumns);
                            result.add(procedureTree);
                        }

                    } else if (procedureTree == null) {
                        procedureTree = new ProcedureTree(getProcedureID(), PROCEDURE_TREE_TYPE, observationType.toLowerCase(), measureColumns);
                        result.add(procedureTree);
                    }


                    // update temporal interval
                    if (dateIndex != -1) {
                        try {
                            dateParse = new SimpleDateFormat(this.dateFormat).parse(line[dateIndex]);
                        } catch (ParseException ex) {
                            LOGGER.warning(String.format("Problem parsing date for main field at line %d and column %d (value='%s'). skipping line...", count, dateIndex, line[dateIndex]));
                            continue;
                        }
                    }

                    // update spatial information
                    if (latitudeIndex != -1 && longitudeIndex != -1) {
                        try {
                            DirectPosition dp = new GeneralDirectPosition(parseDouble(line[longitudeIndex]), parseDouble(line[latitudeIndex]));
                            geom = GMLXmlFactory.buildPoint("3.2.1", null, dp);
                        } catch (NumberFormatException | ParseException ex) {
                            LOGGER.warning(String.format("Problem parsing lat/lon field at line %d.", count));
                        }
                    }
                    procedureTree.spatialBound.addLocation(dateParse, geom);
                    previousProc = currentProc;
                }

                return result;
            }
            throw new DataStoreException("csv headers not found");
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "problem reading csv file", ex);
            throw new DataStoreException(ex);
        }
    }

    private String getObsTypeCode() {
        switch (observationType) {
            case "Timeserie" : return "TS";
            case "Trajectory": return "TR";
            case "Profile"   : return "PR";
            default: throw new IllegalArgumentException("Unexpected observation type:" + observationType + ". Allowed values are Timeserie, Trajectory, Profile.");
        }
    }
}
