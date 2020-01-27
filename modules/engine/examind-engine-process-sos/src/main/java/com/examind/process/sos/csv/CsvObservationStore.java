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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.data.csv.CSVStore;
import org.geotoolkit.gml.xml.AbstractGeometry;
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

    private final String procedureId;

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
            final Set<String> measureColumns, String observationType, String foiColumn, final String procedureId, final boolean extractUom) throws DataStoreException, MalformedURLException {
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
        this.procedureId = procedureId;
        this.extractUom = extractUom;
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
        names.add(NamesExt.create(getProcedureID()));
        return names;
    }

    private String getProcedureID() {
        if (procedureId == null) {
            return IOUtilities.filenameWithoutExtension(dataFile);
        }
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
        if (affectedSensorId != null) {
            LOGGER.warning("CSVObservation store does not allow to override sensor ID");
        }

        int obsCpt = 0;

        // open csv file
        try (final CSVReader reader = new CSVReader(Files.newBufferedReader(dataFile))) {

            final Iterator<String[]> it = reader.iterator();

            // at least one line is expected to contain headers information
            if (it.hasNext()) {

                /*
                1- filter prepare spatial/time column indices from ordinary fields
                  --  lat/lon fields are added only in measure for trajectory observation
                ================================================================*/
                int mainIndex = -1;
                int dateIndex = -1;
                int latitudeIndex = -1;
                int longitudeIndex = -1;
                int foiIndex = -1;

                // read headers
                final String[] headers = it.next();
                final List<String> measureFields = new ArrayList<>();
                final List<Integer> ignoredFields = new ArrayList<>();

                for (int i = 0; i < headers.length; i++) {
                    final String header = headers[i];

                    if (header.equals(mainColumn)) {
                        mainIndex = i;
                        if (header.equals(dateColumn)) dateIndex = i;
                    } else if (header.equals(foiColumn)) {
                        foiIndex = i;
                        ignoredFields.add(i);
                    } else if (header.equals(dateColumn)) {
                        dateIndex = i;
                        if ("Profile".equals(observationType))  ignoredFields.add(dateIndex);
                    } else if (header.equals(latitudeColumn)) {
                        latitudeIndex = i;
                        if (!"Trajectory".equals(observationType))  ignoredFields.add(latitudeIndex);
                    } else if (header.equals(longitudeColumn)) {
                        longitudeIndex = i;
                        if (!"Trajectory".equals(observationType)) ignoredFields.add(longitudeIndex);
                    } else if (measureColumns.contains(header)) {
                        measureFields.add(header);
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
                    case "Trajectory": datarecord = OMUtils.getDataRecordTrajectory("2.0.0", fields);break;
                    case "Profile"   : datarecord = OMUtils.getDataRecordProfile("2.0.0", fields);   break;
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
                final GeoSpatialBound globalSpaBound = new GeoSpatialBound();
                final DateFormat sdf = new SimpleDateFormat(this.dateFormat);

                // -- single observation related variables --
                int currentCount                      = 0;
                String currentFoi                     = null;
                GeoSpatialBound currentSpaBound = new GeoSpatialBound();
                // builder of measure string
                MeasureStringBuilder msb = new MeasureStringBuilder();
                // memorize positions to compute FOI
                final List<DirectPosition> positions = new ArrayList<>();

                // -- previous variables leading to new observations --
                String previousFoi = null;

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

                    // look for current foi
                    if (foiIndex != -1) {
                        currentFoi = line[foiIndex];
                    }


                    // closing current observation and starting new one
                    if (previousFoi != null && !previousFoi.equals(currentFoi)) {

                        final String oid = dataFile.getFileName().toString() + '-' + obsCpt;
                        obsCpt++;
                        final String procedureID = getProcedureID();

                        // sampling feature of interest
                        String foiID = "foi-" + oid;
                        if (previousFoi != null) {
                            foiID = previousFoi;
                        }
                        final SamplingFeature sp = buildFOIByGeom(foiID, positions, samplingFeatures);
                        result.addFeatureOfInterest(sp);
                        globalSpaBound.addGeometry((AbstractGeometry) sp.getGeometry());

                        result.observations.add(OMUtils.buildObservation(oid,                           // id
                                                                         sp,                            // foi
                                                                         phenomenon,                    // phenomenon
                                                                         procedureID,                   // procedure
                                                                         currentCount,                  // count
                                                                         datarecord,                    // result structure
                                                                         msb,                           // measures
                                                                         currentSpaBound.getTimeObject("2.0.0"))   // time
                        );

                        // reset single observation related variables
                        currentCount    = 0;
                        currentSpaBound = new GeoSpatialBound();
                        positions.clear();
                        msb = new MeasureStringBuilder();
                    }

                    previousFoi = currentFoi;
                    currentCount++;


                    /*
                    a- build spatio-temporal information
                    ==================================*/

                    // update temporal interval
                    if (dateIndex != -1) {
                        try {
                            final long millis = sdf.parse(line[dateIndex]).getTime();
                            globalSpaBound.addDate(millis);
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
                        }
                        globalSpaBound.addXYCoordinate(longitude, latitude);
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
                                final long millis = sdf.parse(line[mainIndex]).getTime();
                                msb.appendDate(millis);
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
                final String procedureID = getProcedureID();

                // sampling feature of interest
                String foiID = "foi-" + oid;
                if (previousFoi != null) {
                    foiID = previousFoi;
                }
                final SamplingFeature sp = buildFOIByGeom(foiID, positions, samplingFeatures);
                result.addFeatureOfInterest(sp);
                globalSpaBound.addGeometry((AbstractGeometry) sp.getGeometry());

                result.observations.add(OMUtils.buildObservation(oid,                           // id
                                                                 sp,                            // foi
                                                                 phenomenon,                    // phenomenon
                                                                 procedureID,                   // procedure
                                                                 count,                         // count
                                                                 datarecord,                    // result structure
                                                                 msb,                           // measures
                                                                 currentSpaBound.getTimeObject("2.0.0"))   // time
                );



                result.spatialBound.merge(globalSpaBound);

                // build procedure tree
                final ProcedureTree procedure = new ProcedureTree(procedureID, PROCEDURE_TREE_TYPE);
                procedure.spatialBound.merge(globalSpaBound);
                result.procedures.add(procedure);

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

                // prepare spatial/time column indices
                int dateIndex = -1;
                int latitudeIndex = -1;
                int longitudeIndex = -1;

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
                    }
                }

                Date minDate = null;
                Date maxDate = null;

                while (it.hasNext()) {
                    final String[] line = it.next();

                    // update temporal information
                    if (dateIndex != -1) {
                        final Date dateParse = new SimpleDateFormat(this.dateFormat).parse(line[dateIndex]);
                        if (minDate == null && maxDate == null) {
                            minDate = dateParse;
                            maxDate = dateParse;
                        } else {
                            if(minDate == null || minDate.compareTo(dateParse) > 0) {
                                minDate = dateParse;
                            } else if (maxDate == null || maxDate.compareTo(dateParse) < 0) {
                                maxDate = dateParse;
                            }
                        }
                    }

                    // update spatial information
                    if (latitudeIndex != -1 && longitudeIndex != -1) {
                        result.addXYCoordinate(
                                Double.parseDouble(line[longitudeIndex]),
                                Double.parseDouble(line[latitudeIndex]));
                    }
                }

                // set temporal interval
                if (minDate != null && maxDate != null) {
                    result.dateStart = minDate;
                    result.dateEnd = maxDate;
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
//        return new CsvObservationReader(dataFile, analyze);
        throw new UnsupportedOperationException();
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
                    }
                }

                // procedure tree instanciation
                final ProcedureTree procedureTree = new ProcedureTree(getProcedureID(), PROCEDURE_TREE_TYPE, measureFields);

                Date minDate = null;
                Date maxDate = null;

                while (it.hasNext()) {
                    final String[] line = it.next();

                    // update temporal interval
                    if (dateIndex != -1) {
                        try {
                            final Date dateParse = new SimpleDateFormat(this.dateFormat).parse(line[dateIndex]);
                            if (minDate == null && maxDate == null) {
                                minDate = dateParse;
                                maxDate = dateParse;
                            } else {
                                if(minDate.compareTo(dateParse) > 0) {
                                    minDate = dateParse;
                                } else if (maxDate.compareTo(dateParse) < 0) {
                                    maxDate = dateParse;
                                }
                            }
                        } catch (ParseException ex) {
                            LOGGER.warning(String.format("Problem parsing date for main field at line %d and column %d (value='%s'). skipping line...", count, dateIndex, line[dateIndex]));
                            continue;
                        }
                    }

                    // update spatial information
                    if (latitudeIndex != -1 && longitudeIndex != -1) {
                        procedureTree.spatialBound.addXYCoordinate(
                                Double.parseDouble(line[longitudeIndex]), Double.parseDouble(line[latitudeIndex]));
                    }
                }

                // set temporal interval
                if (minDate != null && maxDate != null) {
                    procedureTree.spatialBound.dateStart = minDate;
                    procedureTree.spatialBound.dateEnd = maxDate;
                }

                return Collections.singletonList(procedureTree);
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
        throw new UnsupportedOperationException("Filtering is not supported on this observation store.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public ObservationWriter getWriter() {
        throw new UnsupportedOperationException("Writing is not supported on this observation store.");
    }
}
