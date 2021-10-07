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

import com.opencsv.CSVReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.geotoolkit.gml.xml.AbstractGeometry;
import org.geotoolkit.gml.xml.GMLXmlFactory;
import org.geotoolkit.observation.ObservationStore;
import org.geotoolkit.observation.model.ExtractionResult;
import org.geotoolkit.observation.model.ExtractionResult.ProcedureTree;
import org.geotoolkit.observation.model.GeoSpatialBound;
import org.geotoolkit.storage.DataStores;
import org.geotoolkit.util.NamesExt;
import org.opengis.feature.FeatureType;
import org.opengis.geometry.DirectPosition;
import org.opengis.temporal.TemporalGeometricPrimitive;
import org.opengis.util.GenericName;

import static com.examind.store.observation.FileParsingUtils.*;
import com.examind.store.observation.FileParsingObservationStore;
import com.examind.store.observation.ObservationBlock;

/**
 * Implementation of an observation store for csv observation data based on {@link CSVFeatureStore}.
 *
 * @author Samuel Andrés (Geomatys)
 * @author Guilhem Legal (Geomatys)
 *
 */
public class CsvObservationStore extends FileParsingObservationStore implements ObservationStore {

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
     * @throws DataStoreException
     * @throws MalformedURLException
     */
    public CsvObservationStore(final Path observationFile, final char separator, final char quotechar, final FeatureType featureType,
            final String mainColumn, final String dateColumn, final String dateTimeformat, final String longitudeColumn, final String latitudeColumn,
            final Set<String> measureColumns, String observationType, String foiColumn, final String procedureId, final String procedureColumn, final String procedureNameColumn, final String procedureDescColumn, final String zColumn, final boolean extractUom) throws DataStoreException, MalformedURLException {
        super(observationFile, separator, quotechar, featureType, mainColumn, dateColumn, dateTimeformat, longitudeColumn, latitudeColumn, measureColumns, observationType, foiColumn, procedureId, procedureColumn, procedureNameColumn, procedureDescColumn, zColumn, extractUom);
    }

    @Override
    public DataStoreProvider getProvider() {
        return DataStores.getProviderById(CsvObservationStoreFactory.NAME);
    }

    @Override
    protected Set<GenericName> extractProcedures() throws DataStoreException {
        final Set<GenericName> result = new HashSet();
        // open csv file
        try (final CSVReader reader = new CSVReader(Files.newBufferedReader(dataFile), delimiter, quotechar)) {

            final Iterator<String[]> it = reader.iterator();

            // at least one line is expected to contain headers information
            if (it.hasNext()) {

                // prepare time column indices
                int procIndex = -1;

                // read headers
                final String[] headers = it.next();
                for (int i = 0; i < headers.length; i++) {
                    final String header = headers[i];
                    if (procedureColumn.equals(header)) {
                        procIndex = i;
                    }
                }

                while (it.hasNext()) {
                    final String[] line = it.next();
                    // update temporal information
                    if (procIndex != -1) {
                        result.add(NamesExt.create(procedureId + line[procIndex]));
                    }
                }
                return result;
            }
            throw new DataStoreException("csv headers not found");
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "problem reading csv file", ex);
            throw new DataStoreException(ex);
        }
    }

    @Override
    public ExtractionResult getResults(final String affectedSensorId, final List<String> sensorIDs,
            final Set<org.opengis.observation.Phenomenon> phenomenons, final Set<org.opengis.observation.sampling.SamplingFeature> samplingFeatures) throws DataStoreException {

        int obsCpt = 0;
        final String fileName = dataFile.getFileName().toString();
        
        // open csv file
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
                int procNameIndex = -1;
                int procDescIndex = -1;

                // read headers
                final String[] headers = it.next();
                final List<String> measureFields = new ArrayList<>();
                final List<Integer> doubleFields = new ArrayList<>();

                for (int i = 0; i < headers.length; i++) {
                    final String header = headers[i];
                    if (header.equals(mainColumn)) {
                        mainIndex = i;
                        if ("Profile".equals(observationType))   {
                            measureFields.add(header);
                        }
                    }
                    if (header.equals(foiColumn)) {
                        foiIndex = i;
                    }
                    if (header.equals(dateColumn)) {
                        dateIndex = i;
                    }
                    if (header.equals(latitudeColumn)) {
                        latitudeIndex = i;
                    }
                    if (header.equals(longitudeColumn)) {
                        longitudeIndex = i;
                    }
                    if (measureColumns.contains(header)) {
                        measureFields.add(header);
                        doubleFields.add(i);
                    }
                    if (header.equals(procedureColumn)) {
                        procIndex = i;
                    }
                    if (header.equals(procedureNameColumn)) {
                        procNameIndex = i;
                    }
                    if (header.equals(procedureDescColumn)) {
                        procDescIndex = i;
                    }
                }

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

                while (it.hasNext()) {
                    lineNumber++;
                    final String[] line = it.next();

                    // verify that the line is not empty (meaning that not all of the measure value selected are empty)
                    if (verifyEmptyLine(line, lineNumber, doubleFields)) {
                        LOGGER.fine("skipping line due to none expected variable present.");
                        continue;
                    }

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

                    // look for current procedure name
                    String currentProcName = currentProc;
                    if (procNameIndex != -1) {
                        currentProcName = line[procNameIndex];
                    }

                    // look for current procedure description
                    String currentProcDesc = null;
                    if (procDescIndex != -1) {
                        currentProcDesc = line[procDescIndex];
                    }

                    // look for current foi (for observation separation)
                    if (foiIndex != -1) {
                        currentFoi = line[foiIndex];
                    }

                    // look for current date (for non timeseries observation separation)
                    if (dateIndex != mainIndex) {
                        try {
                            currentTime = sdf.parse(line[dateIndex]).getTime();
                        } catch (ParseException ex) {
                            LOGGER.fine(String.format("Problem parsing date for date field at line %d and column %d (value='%s'). skipping line...", lineNumber, dateIndex, line[dateIndex]));
                            continue;
                        }
                    }

                    ObservationBlock currentBlock = getOrCreateObservationBlock(currentProc, currentProcName, currentProcDesc, currentFoi, currentTime, measureFields, mainColumn, observationType);

                    /*
                    a- build spatio-temporal information
                    ==================================*/

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
                            LOGGER.fine(String.format("Problem parsing date for date field at line %d and column %d (value='%s'). skipping line...", lineNumber, dateIndex, line[dateIndex]));
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

                    /*
                    b- build measure string
                    =====================*/

                    // add main field
                    Number mainValue;
                    try {
                        // assume that for profile main field is a double
                        if ("Profile".equals(observationType)) {
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
                        LOGGER.fine(String.format("Problem parsing date/double for main field at line %d and column %d (value='%s'). skipping line...", lineNumber, mainIndex, line[mainIndex]));
                        continue;
                    }

                    // loop over columns to build measure string
                    for (int i : doubleFields) {
                        try {
                            double measureValue = parseDouble(line[i]);
                            currentBlock.appendValue(mainValue, headers[i], measureValue, lineNumber);
                        } catch (ParseException | NumberFormatException ex) {
                            if (!line[i].isEmpty()) {
                                LOGGER.fine(String.format("Problem parsing double value at line %d and column %d (value='%s')", lineNumber, i, line[i]));
                            }
                        }
                    }
                }


                /*
                3- build result
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

        // open csv file
        try (final CSVReader reader = new CSVReader(Files.newBufferedReader(dataFile), delimiter, quotechar)) {

            final Iterator<String[]> it = reader.iterator();

            // at least one line is expected to contain headers information
            if (it.hasNext()) {

                // read headers
                final String[] headers = it.next();
                final Set<String> measureFields = new HashSet<>();
                for (int i = 0; i < headers.length; i++) {
                    final String header = headers[i];

                    if (measureColumns.contains(header)) {
                        measureFields.add(header);
                    }
                }
                return measureFields;
            }
            return Collections.emptySet();
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "problem reading csv file", ex);
            throw new UncheckedIOException(ex);
        }
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
    public List<ExtractionResult.ProcedureTree> getProcedures() throws DataStoreException {
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
                int procDescIndex = -1;

                // read headers
                final String[] headers = it.next();
                final List<String> measureFields = new ArrayList<>();
                for (int i = 0; i < headers.length; i++) {
                    final String header = headers[i];

                    if (dateColumn.equals(header)) {
                        dateIndex = i;
                    } else if (latitudeColumn.equals(header)) {
                        latitudeIndex = i;
                    } else if (longitudeColumn.equals(header)) {
                        longitudeIndex = i;
                    } else if (measureColumns.contains(header)) {
                        measureFields.add(header);
                    } else if (header.equals(procedureColumn)) {
                        procedureIndex = i;
                    } else if (header.equals(procedureNameColumn)) {
                        procDescIndex = i;
                    }
                }


                List<ProcedureTree> result = new ArrayList<>();
                String currentProc          = null;
                String previousProc         = null;
                ProcedureTree procedureTree = null;
                while (it.hasNext()) {
                    final String[] line   = it.next();
                    AbstractGeometry geom = null;
                    Date dateParse        = null;

                    if (procedureIndex != -1) {
                        currentProc = procedureId + line[procedureIndex];
                    } else if (procedureTree == null) {
                        currentProc = getProcedureID();
                    }

                    // look for current procedure description
                    String currentProcDesc = currentProc;
                    if (procDescIndex != -1) {
                        currentProcDesc = line[procDescIndex];
                    }

                    if (!currentProc.equals(previousProc) || procedureTree == null) {
                        procedureTree = new ProcedureTree(currentProc, currentProcDesc, null, PROCEDURE_TREE_TYPE, observationType.toLowerCase(), measureFields);
                        result.add(procedureTree);
                    }

                    // update temporal interval
                    if (dateIndex != -1) {
                        try {
                            dateParse = new SimpleDateFormat(this.dateFormat).parse(line[dateIndex]);
                        } catch (ParseException ex) {
                            LOGGER.fine(String.format("Problem parsing date for main field at line %d and column %d (value='%s'). skipping line...", count, dateIndex, line[dateIndex]));
                            continue;
                        }
                    }

                    // update spatial information
                    if (latitudeIndex != -1 && longitudeIndex != -1) {
                        try {
                            DirectPosition dp = new GeneralDirectPosition(parseDouble(line[longitudeIndex]), parseDouble(line[latitudeIndex]));
                            geom = GMLXmlFactory.buildPoint("3.2.1", null, dp);
                        } catch (NumberFormatException | ParseException ex) {
                            LOGGER.fine(String.format("Problem parsing lat/lon field at line %d.", count));
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
}