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
import java.net.MalformedURLException;
import java.nio.file.Path;
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
     * @param quotechar character used for quoted values
     * @param featureType the feature type
     * @param mainColumn the name (header) of the main column (date or pression/depth for profiles)
     * @param dateColumn the name (header) of the date column
     * @param dateTimeformat the date format (see {@link SimpleDateFormat})
     * @param longitudeColumn the name (header) of the longitude column
     * @param latitudeColumn the name (header) of the latitude column
     * @param measureColumns the names (headers) of the measure columns
     * @param observationType Type of the observation to extract (Timeseries, profile).
     * @param foiColumn the name (header) of the feature of interest column
     * @param procedureId
     * @param procedureColumn
     * @param procedureNameColumn
     * @param procedureDescColumn
     * @param zColumn
     * @param uomRegex
     * @param obsPropRegex
     * @throws DataStoreException
     * @throws MalformedURLException
     */
    public CsvObservationStore(final Path observationFile, final char separator, final char quotechar, final FeatureType featureType,
            final List<String> mainColumn, final List<String> dateColumn, final String dateTimeformat, final String longitudeColumn, final String latitudeColumn,
            final Set<String> measureColumns, String observationType, String foiColumn, final String procedureId, final String procedureColumn, 
            final String procedureNameColumn, final String procedureDescColumn, final String zColumn, final String uomRegex, String obsPropRegex, 
            String mimeType, final String obsPropId, final String obsPropName, final boolean noHeader, final boolean directColumnIndex) throws DataStoreException, MalformedURLException {
        super(observationFile, separator, quotechar, featureType, mainColumn, dateColumn, dateTimeformat, longitudeColumn, latitudeColumn, measureColumns, observationType, 
              foiColumn, procedureId, procedureColumn, procedureNameColumn, procedureDescColumn, zColumn, uomRegex, obsPropRegex, obsPropId, obsPropName, mimeType, noHeader, directColumnIndex);
    }

    @Override
    public DataStoreProvider getProvider() {
        return DataStores.getProviderById(CsvObservationStoreFactory.NAME);
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

            int procIndex = getColumnIndex(procedureColumn, headers);

            final Iterator<String[]> it = reader.iterator(!noHeader);
            while (it.hasNext()) {
                final String[] line = it.next();
                if (procIndex != -1) {
                    result.add(NamesExt.create(procedureId + line[procIndex]));
                }
            }
            return result;
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "problem reading csv file", ex);
            throw new DataStoreException(ex);
        }
    }

    @Override
    public ExtractionResult getResults(final String affectedSensorId, final List<String> sensorIDs,
            final Set<org.opengis.observation.Phenomenon> phenomenons, final Set<org.opengis.observation.sampling.SamplingFeature> samplingFeatures) throws DataStoreException {

        // open csv file
        try (final DataFileReader reader = getDataFileReader()) {

            String[] headers = null;
            if (!noHeader) {
                headers = reader.getHeaders();
            }

            /*
            1- filter prepare spatial/time column indices from ordinary fields
            ================================================================*/
            int latitudeIndex  = getColumnIndex(latitudeColumn, headers);
            int longitudeIndex = getColumnIndex(longitudeColumn, headers);
            int foiIndex       = getColumnIndex(foiColumn, headers);
            int procIndex      = getColumnIndex(procedureColumn, headers);
            int procNameIndex  = getColumnIndex(procedureNameColumn, headers);
            int procDescIndex  = getColumnIndex(procedureDescColumn, headers);

            final List<Integer> dateIndexes = getColumnIndexes(dateColumns, headers);
            final List<Integer> mainIndexes = getColumnIndexes(mainColumns, headers);

            final List<String> measureFields = new ArrayList<>();
            if ("Profile".equals(observationType))   {
                if (mainColumns.size() > 1) {
                    throw new DataStoreException("Multiple main columns is not yet supported for Profile");
                }
                measureFields.add(mainColumns.get(0));
            }
            final List<Integer> doubleFields = getColumnIndexes(measureColumns, headers, measureFields);

            // special case where there is no header, and a specified observation peorperty identifier
            if (directColumnIndex && noHeader && obsPropId != null) {
                measureFields.add(obsPropId);
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

            final Iterator<String[]> it = reader.iterator(!noHeader);
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
                String currentProcName = getColumnValue(procNameIndex, line, currentProc);

                // look for current procedure description
                String currentProcDesc = getColumnValue(procDescIndex, line, null);

                // look for current foi (for observation separation)
                currentFoi = getColumnValue(foiIndex, line, currentFoi);

                // look for current date (for non timeseries observation separation)
                if (!dateIndexes.equals(mainIndexes)) {
                    String value = "";
                    try {
                        for (Integer dateIndex : dateIndexes) {
                            value += line[dateIndex];
                        }
                        currentTime = sdf.parse(value).getTime();
                    } catch (ParseException ex) {
                        LOGGER.fine(String.format("Problem parsing date for date field at line %d (value='%s'). skipping line...", lineNumber, value));
                        continue;
                    }
                }

                ObservationBlock currentBlock = getOrCreateObservationBlock(currentProc, currentProcName, currentProcDesc, currentFoi, currentTime, measureFields, mainColumns, observationType);

                /*
                a- build spatio-temporal information
                ==================================*/

                // update temporal interval
                Long millis = null;
                if (!dateIndexes.isEmpty()) {
                    String value = "";
                    try {
                        if (currentTime != null) {
                            millis = currentTime;
                        } else {
                            for (Integer dateIndex : dateIndexes) {
                                value += line[dateIndex];
                            }
                            millis = sdf.parse(value).getTime();
                        }
                        result.spatialBound.addDate(millis);
                        currentBlock.addDate(millis);
                    } catch (ParseException ex) {
                        LOGGER.fine(String.format("Problem parsing date for date field at line %d (value='%s'). skipping line...", lineNumber, value));
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
                Number mainValue;
                String value = "";
                try {
                    // assume that for profile main field is a double
                    if ("Profile".equals(observationType)) {
                        if (mainIndexes.size() > 1) {
                            throw new DataStoreException("Multiple main columns is not yet supported for Profile");
                        }
                        value = line[mainIndexes.get(0)];
                        mainValue = parseDouble(value);

                    // assume that is a date otherwise
                    } else {
                        // little optimization if date column == main column
                        if (millis != null) {
                            mainValue = millis;
                        } else {
                            for (Integer dateIndex : dateIndexes) {
                                value += line[dateIndex];
                            }
                            mainValue = sdf.parse(value).getTime();
                        }
                    }
                } catch (ParseException | NumberFormatException ex) {
                    LOGGER.fine(String.format("Problem parsing date/double for main field at line %d (value='%s'). skipping line...", lineNumber, value));
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
                        currentBlock.appendValue(mainValue, fieldName, measureValue, lineNumber);
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
    public Set<String> getPhenomenonNames() throws DataStoreException {
        // open csv file
        try (final DataFileReader reader = getDataFileReader()) {

            // read headers
            String[] headers = null;
            if (!noHeader) {
                headers = reader.getHeaders();
            }
            final Set<String> measureFields = new HashSet<>();

            // used to fill measure Fields list
            getColumnIndexes(measureColumns, headers, measureFields);

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
    public List<ExtractionResult.ProcedureTree> getProcedures() throws DataStoreException {
        // open csv file
        try (final DataFileReader reader = getDataFileReader()) {

            String[] headers = null;
            if (!noHeader) {
                headers = reader.getHeaders();
            }
            int count = 1;

            // prepare spatial/time column indices
            final List<String> measureFields = new ArrayList<>();
            final List<Integer> dateIndexes = getColumnIndexes(dateColumns, headers);
            int latitudeIndex = getColumnIndex(latitudeColumn, headers);
            int longitudeIndex = getColumnIndex(longitudeColumn, headers);
            int procedureIndex = getColumnIndex(procedureColumn, headers);
            int procDescIndex = getColumnIndex(procedureNameColumn, headers);

            // used to fill measure Fields list
            getColumnIndexes(measureColumns, headers, measureFields);

            // special case where there is no header, and a specified observation peorperty identifier
            if (directColumnIndex && noHeader && obsPropId != null) {
                measureFields.add(obsPropId);
            }

            List<ProcedureTree> result = new ArrayList<>();
            String currentProc          = null;
            String previousProc         = null;
            ProcedureTree procedureTree = null;
            final Iterator<String[]> it = reader.iterator(!noHeader);
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
                String currentProcDesc = getColumnValue(procDescIndex, line, currentProc);

                if (!currentProc.equals(previousProc) || procedureTree == null) {
                    procedureTree = new ProcedureTree(currentProc, currentProcDesc, null, PROCEDURE_TREE_TYPE, observationType.toLowerCase(), measureFields);
                    result.add(procedureTree);
                }

                // update temporal interval
                if (!dateIndexes.isEmpty()) {
                    String value = "";
                    try {
                        for (Integer dateIndex : dateIndexes) {
                            value += line[dateIndex];
                        }
                        dateParse = new SimpleDateFormat(this.dateFormat).parse(value);
                    } catch (ParseException ex) {
                        LOGGER.fine(String.format("Problem parsing date for main field at line %d (value='%s'). skipping line...", count, value));
                        continue;
                    }
                }

                 // update spatial information
                try {
                    final double[] position = extractLinePosition(latitudeIndex, longitudeIndex, currentProc, line);
                    if (position.length == 2) {
                        DirectPosition dp = new GeneralDirectPosition(position[1], position[0]);
                        geom = GMLXmlFactory.buildPoint("3.2.1", null, dp);
                        procedureTree.spatialBound.addLocation(dateParse, geom);
                    }
                } catch (NumberFormatException | ParseException ex) {
                    LOGGER.fine(String.format("Problem parsing lat/lon field at line %d.(Error msg='%s'). skipping line...", count, ex.getMessage()));
                    continue;
                }
                previousProc = currentProc;
            }

            return result;
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "problem reading csv file", ex);
            throw new DataStoreException(ex);
        }
    }
}