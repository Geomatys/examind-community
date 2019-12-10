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

package com.examind.process.sos.dbf;

import static com.examind.process.sos.csv.CsvObservationStoreUtils.buildFOIById;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.data.dbf.DbaseFileFeatureStore;
import org.geotoolkit.data.dbf.DbaseFileHeader;
import org.geotoolkit.data.dbf.DbaseFileReader;
import org.geotoolkit.data.dbf.DbaseFileReader.Row;
import org.geotoolkit.gml.xml.AbstractGeometry;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.observation.ObservationFilter;
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
import org.opengis.geometry.DirectPosition;
import org.opengis.temporal.TemporalGeometricPrimitive;
import org.opengis.util.GenericName;

/**
 * Implementation of an observation store for csv observation data based on {@link CSVFeatureStore}.
 *
 * @author Samuel Andrés (Geomatys)
 */
public class DbfObservationStore extends DbaseFileFeatureStore implements ObservationStore {

    private static final String PROCEDURE_TREE_TYPE = "Component";

    private static final Logger LOGGER = Logging.getLogger("com.examind.process.sos.dbf");

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

    /**
     *
     * @param observationFile path to the dbf observation file
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
    public DbfObservationStore(final Path observationFile,
            final String mainColumn, final String dateColumn, final String dateTimeformat, final String longitudeColumn, final String latitudeColumn,
            final Set<String> measureColumns, String observationType, String foiColumn, final String procedureId) throws DataStoreException, MalformedURLException {
        super(observationFile);
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
    }

    @Override
    public DataStoreProvider getProvider() {
        return DataStores.getProviderById(DbfObservationStoreFactory.NAME);
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
            LOGGER.warning("DBFObservation store does not allow to override sensor ID");
        }

        int obsCpt = 0;

        try (SeekableByteChannel sbc = Files.newByteChannel(dataFile, StandardOpenOption.READ)){

            final DbaseFileReader reader  = new DbaseFileReader(sbc, true, null);
            final DbaseFileHeader headers = reader.getHeader();

            // expected to contain headers information
            if (headers != null) {

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
                final List<String> measureFields = new ArrayList<>();
                final List<Integer> ignoredFields = new ArrayList<>();

                for (int i = 0; i < headers.getNumFields(); i++) {
                    final String header = headers.getFieldName(i);

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
                    fields.add(new Field(field, 1, ""));
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

                while (reader.hasNext()) {
                    count++;
                    final Row line = reader.next();

                    // verify that the line is not empty (meaning that not all of the measure value selected are empty)
                    boolean empty = true;
                    for (int i = 0; i < headers.getNumFields(); i++) {
                        if (i != mainIndex && Arrays.binarySearch(skippedIndices, i) < 0) {
                            if (line.read(i) != null) {
                                empty = false;
                                break;
                            }
                        }
                    }

                    if (empty) {
                        LOGGER.info("skipping row due to none expected variable present.");
                        continue;
                    }

                    // look for current foi
                    if (foiIndex != -1) {
                        currentFoi = line.read(foiIndex).toString();
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
                        final SamplingFeature sp = buildFOIById(foiID, positions, samplingFeatures);
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
                        final Object dateObj = line.read(dateIndex);
                        final long millis;
                        if (dateObj instanceof Double) {
                            millis = dateFromDouble((Double)dateObj).getTime();
                        } else if (dateObj instanceof String) {
                            millis = sdf.parse((String)dateObj).getTime();
                        } else if (dateObj instanceof Date) {
                            millis = ((Date) dateObj).getTime();
                        } else {
                            throw new ClassCastException("Unhandled date type");
                        }
                        globalSpaBound.addDate(millis);
                        currentSpaBound.addDate(millis);
                    }

                    // update spatial information
                    if (latitudeIndex != -1 && longitudeIndex != -1) {
                        final double longitude = ((Double)line.read(longitudeIndex));
                        final double latitude  = ((Double)line.read(latitudeIndex));
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
                                msb.appendValue((Double)(line.read(mainIndex)));
                            } catch (ClassCastException ex) {
                                LOGGER.warning(String.format("Problem parsing double for main field at line %d and column %d (value='%s'). skipping line...", count, mainIndex, line.read(mainIndex)));
                                continue;
                            }
                        // assume that is a date otherwise
                        } else {
                            try {
                                final Object dateObj = line.read(mainIndex);
                                final Date millis;
                                if (dateObj instanceof Double) {
                                    millis = dateFromDouble((Double)dateObj);
                                } else if (dateObj instanceof String) {
                                    millis = sdf.parse((String)dateObj);
                                } else if (dateObj instanceof Date) {
                                    millis = (Date) dateObj;
                                } else {
                                    throw new ClassCastException("Unhandled date type");
                                }
                                msb.appendDate(millis);
                            } catch (ClassCastException ex) {
                                LOGGER.warning(String.format("Problem parsing date for main field at line %d and column %d (value='%s'). skipping line...", count, mainIndex, line.read(mainIndex)));
                                continue;
                            }
                        }
                    }

                    // loop over columns to build measure string
                    for (int i = 0; i < headers.getNumFields(); i++) {
                        if(i != mainIndex && Arrays.binarySearch(skippedIndices, i) < 0) {
                            Object value = line.read(i);
                            try {
                                msb.appendValue((Double)value);
                            } catch (ClassCastException ex) {
                                LOGGER.warning(String.format("Problem parsing double value at line %d and column %d (value='%s')", count, i, value));
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
                final SamplingFeature sp = buildFOIById(foiID, positions, samplingFeatures);
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
            throw new DataStoreException("dbf headers not found");
        } catch (IOException | ParseException ex) {
            LOGGER.log(Level.WARNING, "problem reading dbf file", ex);
            throw new DataStoreException(ex);
        }
    }



    @Override
    public void close() throws DataStoreException {
        // do nothing
    }

    @Override
    public Set<String> getPhenomenonNames() {

        try (SeekableByteChannel sbc = Files.newByteChannel(dataFile, StandardOpenOption.READ)){

            final DbaseFileReader reader  = new DbaseFileReader(sbc, true, null);
            final DbaseFileHeader headers = reader.getHeader();


            // at least one line is expected to contain headers information
            if (headers != null) {

                // read headers
                final Set<String> measureFields = new HashSet<>();
                for (int i = 0; i < headers.getNumFields(); i++) {
                    final String header = headers.getFieldName(i);

                    if (measureColumns.contains(header)) {
                        measureFields.add(header);
                    }
                }
                return measureFields;
            }
            return Collections.emptySet();
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "problem reading dbf file", ex);
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public TemporalGeometricPrimitive getTemporalBounds() throws DataStoreException {

        final GeoSpatialBound result = new GeoSpatialBound();
        // open dbf file
        try (SeekableByteChannel sbc = Files.newByteChannel(dataFile, StandardOpenOption.READ)){

            final DbaseFileReader reader  = new DbaseFileReader(sbc, true, null);
            final DbaseFileHeader headers = reader.getHeader();


            // at least one line is expected to contain headers information
            if (headers != null) {

                // prepare spatial/time column indices
                int dateIndex = -1;
                int latitudeIndex = -1;
                int longitudeIndex = -1;

                // read headers
                for (int i = 0; i < headers.getNumFields(); i++) {
                    final String header = headers.getFieldName(i);

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

                while (reader.hasNext()) {
                    final Row line = reader.next();

                    // update temporal information
                    if (dateIndex != -1) {
                        final Object dateObj = line.read(dateIndex);
                        final Date dateParse;
                        if (dateObj instanceof Double) {
                            dateParse = dateFromDouble((Double)dateObj);
                        } else if (dateObj instanceof String) {
                            dateParse = new SimpleDateFormat(this.dateFormat).parse((String)dateObj);
                        } else if (dateObj instanceof Date) {
                            dateParse = (Date) dateObj;
                        } else {
                            throw new ClassCastException("Unhandled date type");
                        }
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
                                (Double)(line.read(longitudeIndex)),
                                (Double)(line.read(latitudeIndex)));
                    }
                }

                // set temporal interval
                if (minDate != null && maxDate != null) {
                    result.dateStart = minDate;
                    result.dateEnd = maxDate;
                }

                return result.getTimeObject("2.0.0");
            }
            throw new DataStoreException("dbf headers not found");
        } catch (IOException | ParseException ex) {
            LOGGER.log(Level.WARNING, "problem reading dbf file", ex);
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
//        return new DbfObservationReader(dataFile, analyze);
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ExtractionResult.ProcedureTree> getProcedures() throws DataStoreException {


        // open dbf file
        // open dbf file
        try (SeekableByteChannel sbc = Files.newByteChannel(dataFile, StandardOpenOption.READ)){

            final DbaseFileReader reader  = new DbaseFileReader(sbc, true, null);
            final DbaseFileHeader headers = reader.getHeader();

            int count = 0;

            // at least one line is expected to contain headers information
            if (headers != null) {
                count++;

                // prepare spatial/time column indices
                int dateIndex = -1;
                int latitudeIndex = -1;
                int longitudeIndex = -1;

                // read headers
                final List<String> measureFields = new ArrayList<>();
                for (int i = 0; i < headers.getNumFields(); i++) {
                    final String header = headers.getFieldName(i);

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

                while (reader.hasNext()) {
                    final Row line = reader.next();

                    // update temporal interval
                    if (dateIndex != -1) {
                        try {
                            final Object dateObj = line.read(dateIndex);
                            final Date dateParse;
                            if (dateObj instanceof Double) {
                                dateParse = dateFromDouble((Double)dateObj);
                            } else if (dateObj instanceof String) {
                                dateParse = new SimpleDateFormat(this.dateFormat).parse((String)dateObj);
                            } else if (dateObj instanceof Date) {
                                dateParse = (Date) dateObj;
                            } else {
                                throw new ClassCastException("Unhandled date type");
                            }
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
                        } catch (ClassCastException ex) {
                            LOGGER.warning(String.format("Problem parsing date for main field at line %d and column %d (value='%s'). skipping line...", count, dateIndex, line.read(dateIndex)));
                            continue;
                        }
                    }

                    // update spatial information
                    if (latitudeIndex != -1 && longitudeIndex != -1) {
                        procedureTree.spatialBound.addXYCoordinate(
                                (Double)line.read(longitudeIndex),
                                (Double)line.read(latitudeIndex));
                    }
                }

                // set temporal interval
                if (minDate != null && maxDate != null) {
                    procedureTree.spatialBound.dateStart = minDate;
                    procedureTree.spatialBound.dateEnd = maxDate;
                }

                return Collections.singletonList(procedureTree);
            }
            throw new DataStoreException("dbf headers not found");
        } catch (IOException | ParseException ex) {
            LOGGER.log(Level.WARNING, "problem reading dbf file", ex);
            throw new DataStoreException(ex);
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public ObservationFilter getFilter() {
        throw new UnsupportedOperationException("Filtering is not supported on this observation store.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public ObservationWriter getWriter() {
        throw new UnsupportedOperationException("Writing is not supported on this observation store.");
    }

    private static final long TIME_AT_2000;
    static {
        long candidate = 0L;
        try {
            candidate = new SimpleDateFormat("yyyy-MM-dd").parse("2000-01-01").getTime();
        } catch (ParseException ex) {
            LOGGER.log(Level.SEVERE, "Errow while caculationg time at 2000-01-01", ex);
        }
        TIME_AT_2000 = candidate;
    }

    /**
     * Assume that the date is the number of second since the  first january 2000.
     *
     * @param myDouble
     * @return
     * @throws ParseException
     */
    private static Date dateFromDouble(double myDouble) throws ParseException {
        long i = (long) (myDouble*1000);
        long l = TIME_AT_2000 + i;
        return new Date(l);
    }
}
