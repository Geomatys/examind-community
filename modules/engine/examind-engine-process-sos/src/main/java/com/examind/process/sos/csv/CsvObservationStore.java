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

package com.examind.process.sos.csv;

import static com.examind.process.sos.csv.CsvObservationStoreUtils.buildFOIByGeom;
import static com.examind.process.sos.csv.CsvObservationStoreUtils.buildGeom;
import static com.examind.process.sos.csv.CsvObservationStoreUtils.getDataRecordProfile;
import static com.examind.process.sos.csv.CsvObservationStoreUtils.getDataRecordTrajectory;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.data.csv.CSVStore;
import org.geotoolkit.gml.xml.AbstractGeometry;
import org.geotoolkit.gml.xml.GMLXmlFactory;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.observation.ObservationFilterReader;
import org.geotoolkit.observation.ObservationReader;
import org.geotoolkit.observation.ObservationStore;
import org.geotoolkit.observation.ObservationWriter;
import org.geotoolkit.sampling.xml.SamplingFeature;
import org.geotoolkit.sos.MeasureStringBuilder;
import org.geotoolkit.sos.netcdf.ExtractionResult;
import org.geotoolkit.sos.netcdf.ExtractionResult.ProcedureTree;
import org.geotoolkit.sos.netcdf.Field;
import org.geotoolkit.sos.netcdf.GeoSpatialBound;
import org.geotoolkit.sos.netcdf.OMUtils;
import org.geotoolkit.sos.xml.SOSXmlFactory;
import org.geotoolkit.storage.DataStores;
import org.geotoolkit.swe.xml.AbstractDataRecord;
import org.geotoolkit.swe.xml.Phenomenon;
import org.geotoolkit.util.NamesExt;
import org.opengis.feature.FeatureType;
import org.opengis.geometry.DirectPosition;
import org.opengis.temporal.TemporalGeometricPrimitive;
import org.opengis.util.GenericName;

/**
 * Implementation of an observation store for csv observation data based on {@link CSVFeatureStore}.
 *
 * @author Samuel Andrés (Geomatys)
 * @author Guilhem Legal (Geomatys)
 *
 */
public class CsvObservationStore extends CSVStore implements ObservationStore {

    private static final String PROCEDURE_TREE_TYPE = "Component";

    private static final Logger LOGGER = Logging.getLogger("com.examind.process.sos.csv");

    private final Path dataFile;

    private final String mainColumn;

    // date column expected header
    private final String dateColumn;
    // date format
    private final String dateFormat;
    // longitude column expected header
    private final String longitudeColumn;
    // latitude column expected header
    private final String latitudeColumn;

    private final String foiColumn;

    private final Set<String> measureColumns;

    // timeSeries / trajectory / profiles
    private final String observationType;

    /**
     * Act as a single sensor ID if no procedureColumn is supplied.
     * Act as a prefix else.
     */
    private final String procedureId;
    private final String procedureColumn;

    private final boolean extractUom;

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
    public CsvObservationStore(final Path observationFile, final char separator, final FeatureType featureType,
            final String mainColumn, final String dateColumn, final String dateTimeformat, final String longitudeColumn, final String latitudeColumn,
            final Set<String> measureColumns, String observationType, String foiColumn, final String procedureId, final String procedureColumn, final boolean extractUom) throws DataStoreException, MalformedURLException {
        super(observationFile, separator, featureType);
        dataFile = observationFile;
        this.mainColumn = mainColumn;
        this.dateColumn = dateColumn;
        this.dateFormat = dateTimeformat;
        this.longitudeColumn = longitudeColumn;
        this.latitudeColumn = latitudeColumn;
        this.measureColumns = measureColumns;
        this.observationType = observationType;
        this.foiColumn = foiColumn;
        this.procedureColumn = procedureColumn;
        this.extractUom = extractUom;
        
        if (procedureId == null && procedureColumn == null) {
            this.procedureId = IOUtilities.filenameWithoutExtension(dataFile);
        } else if (procedureId == null) {
            this.procedureId = ""; // empty template
        } else {
            this.procedureId = procedureId;
        }
    }

    @Override
    public DataStoreProvider getProvider() {
        return DataStores.getProviderById(CsvObservationStoreFactory.NAME);
    }

    ////////////////////////////////////////////////////////////////////////////
    // OBSERVATION STORE ///////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    @Override
    public Set<GenericName> getProcedureNames() {
        final Set<GenericName> names = new HashSet<>();
        if (procedureColumn == null) {
            names.add(NamesExt.create(getProcedureID()));
        } else {
            names.addAll(extractProcedures());
        }
        return names;
    }

    private Set<GenericName> extractProcedures() {

        final Set<GenericName> result = new HashSet();
        // open csv file
        try (final CSVReader reader = new CSVReader(Files.newBufferedReader(dataFile))) {

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
            throw new BackingStoreException("csv headers not found");
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "problem reading csv file", ex);
            throw new BackingStoreException(ex);
        }
    }

    private String getProcedureID() {
        return procedureId;
    }

    @Override
    public ExtractionResult getResults() throws DataStoreException {
        return getResults(null, null, new HashSet<>(), new HashSet<>());
    }

    @Override
    public ExtractionResult getResults(final List<String> sensorIDs) throws DataStoreException {
        return getResults(null, sensorIDs, new HashSet<>(), new HashSet<>());
    }

    @Override
    public ExtractionResult getResults(final String affectedSensorId, final List<String> sensorIDs) throws DataStoreException {
        return getResults(affectedSensorId, sensorIDs, new HashSet<>(), new HashSet<>());
    }

    @Override
    public ExtractionResult getResults(final String affectedSensorId, final List<String> sensorIDs,
            final Set<org.opengis.observation.Phenomenon> phenomenons, final Set<org.opengis.observation.sampling.SamplingFeature> samplingFeatures) throws DataStoreException {

        int obsCpt = 0;

        // open csv file
        try (final CSVReader reader = new CSVReader(Files.newBufferedReader(dataFile))) {

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

                // read headers
                final String[] headers = it.next();
                final List<String> measureFields = new ArrayList<>();
                final List<Integer> ignoredFields = new ArrayList<>();

                for (int i = 0; i < headers.length; i++) {
                    final String header = headers[i];

                    if (header.equals(mainColumn)) {
                        mainIndex = i;
                        if (header.equals(dateColumn)) dateIndex = i;
                        if ("Profile".equals(observationType))  measureFields.add(header);
                    } else if (header.equals(foiColumn)) {
                        foiIndex = i;
                        ignoredFields.add(i);
                    } else if (header.equals(dateColumn)) {
                        dateIndex = i;
                        if ("Profile".equals(observationType))  ignoredFields.add(dateIndex);
                    } else if (header.equals(latitudeColumn)) {
                        latitudeIndex = i;
                        ignoredFields.add(latitudeIndex);
                    } else if (header.equals(longitudeColumn)) {
                        longitudeIndex = i;
                        ignoredFields.add(longitudeIndex);
                    } else if (measureColumns.contains(header)) {
                        measureFields.add(header);
                    } else if (header.equals(procedureColumn)) {
                        procIndex = i;
                        ignoredFields.add(procIndex);
                    } else {
                        ignoredFields.add(i);
                    }
                }

                // memorize indices to skip
                final int[] skippedIndices = ArrayUtils.toPrimitive(ignoredFields.toArray(new Integer[ignoredFields.size()]));

                /*
                2- set ordinary fields
                =====================*/
                final List<Field> fields = new ArrayList<>();
                for (final String field : measureFields) {
                    String name;
                    String uom;
                    int b = field.indexOf('(');
                    int o = field.indexOf(')');
                    if (extractUom && b != -1 && o != -1 && b < o) {
                        name = field.substring(0, b).trim();
                        uom  = field.substring(b + 1, o);
                    } else {
                        name = field;
                        uom  = null;
                    }
                    fields.add(new Field(name, null, 1, "", null, uom));
                }

                final ExtractionResult result = new ExtractionResult();
                result.fields.addAll(measureFields);

                final AbstractDataRecord datarecord;
                switch (observationType) {
                    case "Timeserie" : datarecord = OMUtils.getDataRecordTimeSeries("2.0.0", fields);break;
                    case "Trajectory": datarecord = getDataRecordTrajectory("2.0.0", fields);break;
                    case "Profile"   : datarecord = getDataRecordProfile("2.0.0", fields);   break;
                    default: throw new IllegalArgumentException("Unexpected observation type:" + observationType + ". Allowed values are Timeserie, Trajectory, Profile.");
                }

                Phenomenon phenomenon = OMUtils.getPhenomenon("2.0.0", fields, "", phenomenons);
                result.phenomenons.add(phenomenon);


                /*
                3- compute measures
                =================*/

                // -- global variables --
                int count = 0;

                // spatial / temporal boundaries
                final DateFormat sdf = new SimpleDateFormat(this.dateFormat);

                // -- single observation related variables --
                int currentCount                      = 0;
                String currentFoi                     = null;
                String currentProc                    = null;
                Long currentTime                      = null;
                GeoSpatialBound currentSpaBound = new GeoSpatialBound();
                // builder of measure string
                MeasureStringBuilder msb = new MeasureStringBuilder();
                // memorize positions to compute FOI
                final List<DirectPosition> positions = new ArrayList<>();
                final Map<Long, List<DirectPosition>> historicalPositions = new HashMap<>();

                // -- previous variables leading to new observations --
                String previousFoi  = null;
                String previousProc = null;
                Long previousTime   = null;

                while (it.hasNext()) {
                    count++;
                    final String[] line = it.next();

                    // verify that the line is not empty (meaning that not all of the measure value selected are empty)
                    boolean empty = true;
                    for (int i = 0; i < line.length; i++) {
                        if(i != mainIndex && Arrays.binarySearch(skippedIndices, i) < 0) {
                            try {
                                Double.parseDouble(line[i]);
                                empty = false;
                                break;
                            } catch (NumberFormatException ex) {
                                if (!line[i].isEmpty()) {
                                    LOGGER.warning(String.format("Problem parsing double value at line %d and column %d (value='%s')", count, i, line[i]));
                                }
                            }
                        }
                    }

                    if (empty) {
                        LOGGER.info("skipping line due to none expected variable present.");
                        continue;
                    }
                    
                    // look for current procedure (for observation separation)
                    if (procIndex != -1) {
                        currentProc = procedureId + line[procIndex];
                        if (sensorIDs != null && !sensorIDs.isEmpty() && !sensorIDs.contains(currentProc)) {
                            LOGGER.finer("skipping line due to none specified sensor related.");
                            continue;
                        }
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
                            LOGGER.warning(String.format("Problem parsing date for date field at line %d and column %d (value='%s'). skipping line...", count, dateIndex, line[dateIndex]));
                            continue;
                        }
                    }

                    // closing current observation and starting new one
                    if (previousFoi  != null && !previousFoi.equals(currentFoi)   ||
                        previousTime != null && !previousTime.equals(currentTime) ||
                        previousProc != null && !previousProc.equals(currentProc)) {

                        final String oid = dataFile.getFileName().toString() + '-' + obsCpt;
                        obsCpt++;

                        // procedure
                        String procedureID = getProcedureID();
                        if (previousProc != null) {
                            procedureID = previousProc;
                        }

                        // sampling feature of interest
                        String foiID = "foi-" + UUID.randomUUID();
                        if (previousFoi != null) {
                            foiID = previousFoi;
                        }

                        final SamplingFeature sp = buildFOIByGeom(foiID, positions, samplingFeatures);
                        result.addFeatureOfInterest(sp);

                        result.observations.add(OMUtils.buildObservation(oid,                           // id
                                                                         sp,                            // foi
                                                                         phenomenon,                    // phenomenon
                                                                         procedureID,                   // procedure
                                                                         currentCount,                  // count
                                                                         datarecord,                    // result structure
                                                                         msb,                           // measures
                                                                         currentSpaBound.getTimeObject("2.0.0"))   // time
                        );

                        // build new procedure tree
                        final ProcedureTree procedure = getOrCreateProcedureTree(result, procedureID, PROCEDURE_TREE_TYPE, observationType.toLowerCase());
                        for (Entry<Long, List<DirectPosition>> entry : historicalPositions.entrySet()) {
                            procedure.spatialBound.addLocation(new Date(entry.getKey()), buildGeom(entry.getValue()));
                        }

                        procedure.spatialBound.merge(currentSpaBound);
                        

                        // reset single observation related variables
                        currentCount    = 0;
                        currentSpaBound = new GeoSpatialBound();
                        positions.clear();
                        msb = new MeasureStringBuilder();
                        historicalPositions.clear();
                    }

                    previousFoi = currentFoi;
                    previousTime = currentTime;
                    previousProc = currentProc;
                    currentCount++;


                    /*
                    a- build spatio-temporal information
                    ==================================*/

                    // update temporal interval
                    Long millis = null;
                    if (dateIndex != -1) {
                        try {
                            millis = sdf.parse(line[dateIndex]).getTime();
                            result.spatialBound.addDate(millis);
                            currentSpaBound.addDate(millis);
                        } catch (ParseException ex) {
                            LOGGER.warning(String.format("Problem parsing date for date field at line %d and column %d (value='%s'). skipping line...", count, dateIndex, line[dateIndex]));
                            continue;
                        }
                    }

                    // update spatial information
                    if (latitudeIndex != -1 && longitudeIndex != -1) {
                        final double longitude = Double.parseDouble(line[longitudeIndex]);
                        final double latitude = Double.parseDouble(line[latitudeIndex]);
                        DirectPosition pos = SOSXmlFactory.buildDirectPosition("2.0.0", "EPSG:4326", 2, Arrays.asList(latitude, longitude));
                        if (!positions.contains(pos)) {
                            positions.add(pos);
                            if (millis != null) {
                                if (historicalPositions.containsKey(millis)) {
                                    historicalPositions.get(millis).add(pos);
                                } else {
                                    List<DirectPosition> hpos = new ArrayList<>();
                                    hpos.add(pos);
                                    historicalPositions.put(millis, hpos);
                                }
                            }
                        }
                        result.spatialBound.addXYCoordinate(longitude, latitude);
                        currentSpaBound.addXYCoordinate(longitude, latitude);
                    }


                    /*
                    b- build measure string
                    =====================*/

                    // add main field
                    if (mainIndex != -1) {

                        // assume that for profile main field is a double
                        if ("Profile".equals(observationType)) {
                            try {
                                msb.appendValue(Double.parseDouble(line[mainIndex]));
                            } catch (NumberFormatException ex) {
                                LOGGER.warning(String.format("Problem parsing double for main field at line %d and column %d (value='%s'). skipping line...", count, mainIndex, line[mainIndex]));
                                continue;
                            }
                        // assume that is a date otherwise
                        } else {
                            try {
                                final long m = sdf.parse(line[mainIndex]).getTime();
                                msb.appendDate(m);
                            } catch (ParseException ex) {
                                LOGGER.warning(String.format("Problem parsing date for main field at line %d and column %d (value='%s'). skipping line...", count, mainIndex, line[mainIndex]));
                                continue;
                            }
                        }
                    }

                    // loop over columns to build measure string
                    for (int i = 0; i < line.length; i++) {
                        if(i != mainIndex && Arrays.binarySearch(skippedIndices, i) < 0) {
                            try {
                                msb.appendValue(Double.parseDouble(line[i]));
                            } catch (NumberFormatException ex) {
                                if (!line[i].isEmpty()) {
                                    LOGGER.warning(String.format("Problem parsing double value at line %d and column %d (value='%s')", count, i, line[i]));
                                }
                                msb.appendValue(Double.NaN);
                            }
                        }
                    }

                    msb.closeBlock();
                }


                /*
                3- build result
                =============*/

                final String oid = dataFile.getFileName().toString() + '-' + obsCpt;
                obsCpt++;

                String procedureID = getProcedureID();
                if (previousProc != null) {
                    procedureID = previousProc;
                }

                // sampling feature of interest
                String foiID = "foi-" + UUID.randomUUID();
                if (previousFoi != null) {
                    foiID = previousFoi;
                }

                final SamplingFeature sp = buildFOIByGeom(foiID, positions, samplingFeatures);
                result.addFeatureOfInterest(sp);

                result.observations.add(OMUtils.buildObservation(oid,                           // id
                                                                 sp,                            // foi
                                                                 phenomenon,                    // phenomenon
                                                                 procedureID,                   // procedure
                                                                 count,                         // count
                                                                 datarecord,                    // result structure
                                                                 msb,                           // measures
                                                                 currentSpaBound.getTimeObject("2.0.0"))   // time
                );

                // build procedure tree
                final ProcedureTree procedure = getOrCreateProcedureTree(result, procedureID, PROCEDURE_TREE_TYPE, observationType.toLowerCase());
                for (Entry<Long, List<DirectPosition>> entry : historicalPositions.entrySet()) {
                    procedure.spatialBound.addLocation(new Date(entry.getKey()), buildGeom(entry.getValue()));
                }
                procedure.spatialBound.merge(currentSpaBound);
                
                return result;
            }
            throw new DataStoreException("csv headers not found");
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "problem reading csv file", ex);
            throw new DataStoreException(ex);
        }
    }



    @Override
    public void close() throws DataStoreException {
        // do nothing
    }

    @Override
    public Set<String> getPhenomenonNames() {

        // open csv file
        try (final CSVReader reader = new CSVReader(Files.newBufferedReader(dataFile))) {

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
        try (final CSVReader reader = new CSVReader(Files.newBufferedReader(dataFile))) {

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

    /**
     * {@inheritDoc }
     */
    @Override
    public Path[] getComponentFiles() throws DataStoreException {
        return new Path[]{dataFile};
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public ObservationReader getReader() {
        return null;
    }

    @Override
    public List<ExtractionResult.ProcedureTree> getProcedures() throws DataStoreException {
        // open csv file
        try (final CSVReader reader = new CSVReader(Files.newBufferedReader(dataFile))) {

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
                        if (!currentProc.equals(previousProc)) {
                            procedureTree = new ProcedureTree(currentProc, PROCEDURE_TREE_TYPE, observationType.toLowerCase(), measureFields);
                            result.add(procedureTree);
                        }

                    } else if (procedureTree == null) {
                        procedureTree = new ProcedureTree(getProcedureID(), PROCEDURE_TREE_TYPE, observationType.toLowerCase(), measureFields);
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
                            DirectPosition dp = new GeneralDirectPosition(Double.parseDouble(line[longitudeIndex]), Double.parseDouble(line[latitudeIndex]));
                            geom = GMLXmlFactory.buildPoint("3.2.1", null, dp);
                        } catch (NumberFormatException ex) {
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

    /**
     * {@inheritDoc }
     */
    @Override
    public ObservationFilterReader getFilter() {
        return null;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public ObservationWriter getWriter() {
        return null;
    }

    private ProcedureTree getOrCreateProcedureTree(final ExtractionResult result, final String procedureId, final String type, final String omType) {
        for (ProcedureTree tree : result.procedures) {
            if (tree.id.equals(procedureId)) {
                return tree;
            }
        }
        ProcedureTree tree = new ProcedureTree(procedureId, type, omType);
        result.procedures.add(tree);
        return tree;
    }
}
