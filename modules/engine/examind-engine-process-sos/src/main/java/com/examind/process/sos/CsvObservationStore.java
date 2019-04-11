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

package com.examind.process.sos;

import com.opencsv.CSVReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
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
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.data.csv.CSVFeatureStore;
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
 */
public class CsvObservationStore extends CSVFeatureStore implements ObservationStore {

    private static final String PROCEDURE_TREE_TYPE = "Component";

    private static final Logger LOGGER = Logging.getLogger("org.geotoolkit.data");

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
            final Set<String> measureColumns, String observationType, String foiColumn) throws DataStoreException, MalformedURLException {
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
        return IOUtilities.filenameWithoutExtension(dataFile);
    }

    @Override
    public ExtractionResult getResults() throws DataStoreException {
        return getResults(null);
    }

    @Override
    public ExtractionResult getResults(final String affectedSensorId, final List<String> sensorIDs) throws DataStoreException {
        LOGGER.warning("CSVObservation store does not allow to override sensor ID");
        return getResults(sensorIDs);
    }

    @Override
    public ExtractionResult getResults(final List<String> sensorIDs) throws DataStoreException {


        // open csv file
        try (final CSVReader reader = new CSVReader(new FileReader(dataFile.toFile()))) {

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

                // read headers
                final String[] headers = it.next();
                final List<String> measureFields = new ArrayList<>();
                final List<Integer> ignoredFields = new ArrayList<>();

                for (int i = 0; i < headers.length; i++) {
                    final String header = headers[i];

                    if (mainColumn.equals(header)) {
                        mainIndex = i;
                    } else if (dateColumn.equals(header)) {
                        dateIndex = i;
                        if ("Profile".equals(observationType))  ignoredFields.add(dateIndex);
                    } else if (latitudeColumn.equals(header)) {
                        latitudeIndex = i;
                        if (!"Trajectory".equals(observationType))  ignoredFields.add(latitudeIndex);
                    } else if (longitudeColumn.equals(header)) {
                        longitudeIndex = i;
                        if (!"Trajectory".equals(observationType)) ignoredFields.add(longitudeIndex);
                    } else if (measureColumns.contains(header)) {
                        measureFields.add(header);
                    } else {
                        ignoredFields.add(i);
                    }
                }


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
                final Phenomenon phenomenon         = OMUtils.getPhenomenon("2.0.0", fields);
                result.phenomenons.add(phenomenon);


                /*
                3- compute measures
                =================*/

                // builder of measure string
                final MeasureStringBuilder msb = new MeasureStringBuilder();
                int count = 0;

                // spatial / temporal boundaries
                final GeoSpatialBound gsb = new GeoSpatialBound();

                // memorize positions to compute FOI
                final List<DirectPosition> positions = new ArrayList<>();

                final DateFormat sdf = new SimpleDateFormat(this.dateFormat);
                while (it.hasNext()) {
                    count++;
                    final String[] line = it.next();

                    /*
                    a- build spatio-temporal information
                    ==================================*/

                    // update temporal interval
                    if (dateIndex != -1) {
                        final long millis = sdf.parse(line[dateIndex]).getTime();
                        gsb.addDate(millis);
                    }

                    // update spatial information
                    if (latitudeIndex != -1 && longitudeIndex != -1) {
                        final double longitude = Double.parseDouble(line[longitudeIndex]);
                        final double latitude = Double.parseDouble(line[latitudeIndex]);
                        DirectPosition pos = SOSXmlFactory.buildDirectPosition("2.0.0", "EPSG:4326", 2, Arrays.asList(latitude, longitude));
                        if (!positions.contains(pos)) {
                            positions.add(pos);
                        }
                        gsb.addXYCoordinate(longitude, latitude);
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
                            final long millis = sdf.parse(line[mainIndex]).getTime();
                            msb.appendDate(millis);
                        }
                    }

                    // memorize indices to skip
                    final int[] skippedIndices = ArrayUtils.toPrimitive(ignoredFields.toArray(new Integer[ignoredFields.size()]));

                    // loop over columns to build measure string
                    for (int i = 0; i < line.length; i++) {
                        if(i != mainIndex && Arrays.binarySearch(skippedIndices, i) < 0) {
                            try {
                                msb.appendValue(Double.parseDouble(line[i]));
                            } catch (NumberFormatException ex) {
                                LOGGER.warning(String.format("Problem parsing double value at line %d and column %d (value='%s')", count, i, line[i]));
                                msb.appendValue(0.);
                            }
                        }
                    }

                    msb.closeBlock();
                }


                /*
                3- build result
                =============*/

                final String identifier = UUID.randomUUID().toString();
                final String procedureID = getProcedureID();

                // sampling feature of interest
                final SamplingFeature sp;
                if (positions.size() > 1) {
                    sp = OMUtils.buildSamplingCurve("foi-" + identifier, positions);
                } else {
                    sp = OMUtils.buildSamplingPoint("foi-" + identifier, positions.get(0).getOrdinate(0),  positions.get(0).getOrdinate(1));
                }
                result.addFeatureOfInterest(sp);
                gsb.addGeometry((AbstractGeometry) sp.getGeometry());

                result.observations.add(OMUtils.buildObservation(identifier,                    // id
                                                                 sp,                            // foi
                                                                 phenomenon,                    // phenomenon
                                                                 procedureID,                   // procedure
                                                                 count,                         // result
                                                                 datarecord,                    // result
                                                                 msb,                            // result
                                                                 gsb.getTimeObject("2.0.0"))   // time);
                );

                result.spatialBound.merge(gsb);

                // build procedure tree
                final ProcedureTree procedure = new ProcedureTree(procedureID, PROCEDURE_TREE_TYPE);
                procedure.spatialBound.merge(gsb);
                result.procedures.add(procedure);

                return result;
            }
            throw new DataStoreException("csv headers not found");
        } catch (IOException | ParseException ex) {
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
        try (final CSVReader reader = new CSVReader(new FileReader(dataFile.toFile()))) {

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
        try (final CSVReader reader = new CSVReader(new FileReader(dataFile.toFile()))) {

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
                        }
                        else {
                            if(minDate.compareTo(dateParse) > 0) {
                                minDate = dateParse;
                            } else if (maxDate.compareTo(dateParse) < 0) {
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
        try (final CSVReader reader = new CSVReader(new FileReader(dataFile.toFile()))) {

            final Iterator<String[]> it = reader.iterator();

            // at least one line is expected to contain headers information
            if (it.hasNext()) {

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
                        final Date dateParse = new SimpleDateFormat(this.dateFormat).parse(line[dateIndex]);
                        if (minDate == null && maxDate == null) {
                            minDate = dateParse;
                            maxDate = dateParse;
                        }
                        else {
                            if(minDate.compareTo(dateParse) > 0) {
                                minDate = dateParse;
                            } else if (maxDate.compareTo(dateParse) < 0) {
                                maxDate = dateParse;
                            }
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
        } catch (IOException | ParseException ex) {
            LOGGER.log(Level.WARNING, "problem reading csv file", ex);
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

    /**
     * {@inheritDoc }
     */
    @Override
    public ObservationFilter cloneObservationFilter(ObservationFilter toClone) {
        throw new UnsupportedOperationException("Filtering is not supported on this observation store.");
    }

}
