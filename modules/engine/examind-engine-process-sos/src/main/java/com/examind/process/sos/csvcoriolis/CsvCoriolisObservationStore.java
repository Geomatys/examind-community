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

package com.examind.process.sos.csvcoriolis;

import com.opencsv.CSVReader;
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

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.examind.process.sos.csvcoriolis.CsvCoriolisObservationStoreUtils.*;

/**
 * Implementation of an observation store for csv coriolis observation data based on {@link CSVFeatureStore}.
 *
 * @author Maxime Gavens (Geomatys)
 *
 */
public class CsvCoriolisObservationStore extends CSVStore implements ObservationStore {

    private static final String PROCEDURE_TREE_TYPE = "Component";

    private static final Logger LOGGER = Logging.getLogger("com.examind.process.sos.csvcoriolis");

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

    private final String valueColumn;
    private final String codeColumn;
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
     * @param codeColumn the name (header) of the code measure column
     * 
     * @throws DataStoreException
     * @throws MalformedURLException
     */
    public CsvCoriolisObservationStore(final Path observationFile, final char separator, final FeatureType featureType,
                                       final String mainColumn, final String dateColumn, final String dateTimeformat, final String longitudeColumn, final String latitudeColumn,
                                       final Set<String> measureColumns, String observationType, String foiColumn, final String procedureId, final String procedureColumn,
                                       final boolean extractUom, final String valueColumn, final String codeColumn, final String typeColumn) throws DataStoreException, MalformedURLException {
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
        this.valueColumn = valueColumn;
        this.codeColumn = codeColumn;
        this.typeColumn = typeColumn;
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
        return DataStores.getProviderById(CsvCoriolisObservationStoreFactory.NAME);
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
        final String fileName = dataFile.getFileName().toString();
        
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
                int valueColumnIndex = -1;
                int codeColumnIndex = -1;
                int typeColumnIndex = -1;

                // read headers
                final String[] headers = it.next();
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
                        ignoredFields.add(latitudeIndex);
                    } else if (header.equals(longitudeColumn)) {
                        longitudeIndex = i;
                        ignoredFields.add(longitudeIndex);
                    } else if (header.equals(valueColumn)) {
                        valueColumnIndex = i;
                    } else if (header.equals(codeColumn)) {
                        codeColumnIndex = i;
                        ignoredFields.add(codeColumnIndex);
                    } else if (header.equals(typeColumn)) {
                        typeColumnIndex = i;
                        ignoredFields.add(typeColumnIndex);
                    } else if (header.equals(procedureColumn)) {
                        procIndex = i;
                        ignoredFields.add(procIndex);
                    } else {
                        ignoredFields.add(i);
                    }
                }
                
                if (typeColumnIndex == -1) {
                    throw new DataStoreException("missing column type:" + typeColumn);
                }
                if (codeColumnIndex == -1) {
                    throw new DataStoreException("Unexpected column code:" + codeColumn);
                }
                if (valueColumnIndex == -1) {
                    throw new DataStoreException("Unexpected column value:" + valueColumn);
                }
                if (mainIndex == -1) {
                    throw new DataStoreException("Unexpected column main:" + mainColumn);
                }

                // add measure column
                final Set<String> measureColumnFound = new HashSet<>();
                final List<String> sortedMeasureColumns = measureColumns.stream().sorted().collect(Collectors.toList());

                // memorize indices to skip
                final int[] skippedIndices = ArrayUtils.toPrimitive(ignoredFields.toArray(new Integer[ignoredFields.size()]));

                // final result
                final ExtractionResult result = new ExtractionResult();

                /*
                2- compute measures
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
                GeoSpatialBound currentSpaBound       = new GeoSpatialBound();
                final String obsTypeCode              = getObsTypeCode();
                
                // measure map used to collect measure data then construct the MeasureStringBuilder
                final CoriolisMeasureBuilder mmb = new CoriolisMeasureBuilder(observationType, sdf, sortedMeasureColumns);
                
                // memorize positions to compute FOI
                final Set<String> knownPositions = new HashSet<>();
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
                    
                    // checks if row matches the observed data types
                    if (!line[typeColumnIndex].equals(obsTypeCode)) continue;
                    
                    // look for current foi (for observation separation)
                    if (foiIndex != -1) {
                        currentFoi = line[foiIndex];
                    }

                    // look for current procedure (for observation separation)
                    if (procIndex != -1) {
                        currentProc = procedureId + line[procIndex];
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

                        // procedure
                        String procedureID = getProcedureID();
                        if (previousProc != null) {
                            procedureID = previousProc;
                        }

                        if (procedureID.equals(affectedSensorId)) {
                            
                            final String oid = fileName + '-' + obsCpt;
                            obsCpt++;
                        
                            // sampling feature of interest
                            String foiID = previousFoi != null ? previousFoi : "foi-" + UUID.randomUUID();
                            final SamplingFeature sp = buildFOIByGeom(foiID, positions, samplingFeatures);
                            
                            // On extrait les types de mesure trouvées dans la donnée
                            measureColumnFound.addAll(mmb.getMeasureFromMap());
                            // Construction du measureStringBuilder à partir des données collectées dans le hashmap
                            MeasureStringBuilder msb = mmb.buildMeasureStringBuilderFromMap(measureColumnFound, obsTypeCode.equals("PR"));
                            
                            buildObservation(result, procedureID, oid, sp, measureColumnFound, sortedMeasureColumns, phenomenons, currentCount, currentSpaBound, msb, historicalPositions);
                            
                        } else {
                            LOGGER.finer("observation excluded from extraction. procedure does not match " + affectedSensorId);
                        }

                        // reset single observation related variables
                        currentCount    = 0;
                        currentSpaBound = new GeoSpatialBound();
                        positions.clear();
                        knownPositions.clear();
                        mmb.clear();
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
                            if (currentTime != null) {
                                millis = currentTime;
                            } else {
                                millis = sdf.parse(line[dateIndex]).getTime();
                            }
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
                        final String posKey = latitude + "_" + longitude;
                        if (!knownPositions.contains(posKey)) {
                            knownPositions.add(posKey);
                            final DirectPosition pos = SOSXmlFactory.buildDirectPosition("2.0.0", "EPSG:4326", 2, Arrays.asList(latitude, longitude));
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
                    b- build measure map
                    =====================*/
                    try {
                        mmb.parseLine(line[mainIndex], currentTime, line[codeColumnIndex], line[valueColumnIndex], count, valueColumnIndex);
                    } catch (ParseException | NumberFormatException ex) {
                        LOGGER.warning(String.format("Problem parsing date/double for main field at line %d and column %d (value='%s'). skipping line...", count, mainIndex, line[mainIndex]));
                        continue;
                    }
                }


                /*
                3- build result
                =============*/

                String procedureID = getProcedureID();
                if (previousProc != null) {
                    procedureID = previousProc;
                }

                if (procedureID.equals(affectedSensorId)) {
                    final String oid = fileName + '-' + obsCpt;
                    obsCpt++;
                
                    // sampling feature of interest
                    String foiID = previousFoi != null ? previousFoi : "foi-" + UUID.randomUUID();
                    final SamplingFeature sp = buildFOIByGeom(foiID, positions, samplingFeatures);
                    
                    // On extrait les types de mesure trouvées dans la donnée
                    measureColumnFound.addAll(mmb.getMeasureFromMap());
                    // Construction du measureStringBuilder à partir des données collectées dans le hashmap
                    MeasureStringBuilder msb = mmb.buildMeasureStringBuilderFromMap(measureColumnFound, obsTypeCode.equals("PR"));

                    buildObservation(result, procedureID, oid, sp, measureColumnFound, sortedMeasureColumns, phenomenons, currentCount, currentSpaBound, msb, historicalPositions);
                }

                return result;
            }
            throw new DataStoreException("csv headers not found");
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "problem reading csv file", ex);
            throw new DataStoreException(ex);
        }
    }
    
    private void buildObservation(ExtractionResult result, String procedureID, String oid, final SamplingFeature sp, Set<String> measureColumnFound, List<String> sortedMeasureColumns,  
            Set<org.opengis.observation.Phenomenon> phenomenons, int currentCount, GeoSpatialBound currentSpaBound, MeasureStringBuilder msb, final Map<Long, List<DirectPosition>> historicalPositions) {
               
        // On complète les champs de mesures seulement avec celles trouvées dans la donnée
        List<String> filteredMeasure = new ArrayList<>();
        if ("Profile".equals(observationType))  filteredMeasure.add(mainColumn);
        for (String m: sortedMeasureColumns) {
            if (measureColumnFound.contains(m)) filteredMeasure.add(m);
        }
        
        final List<Field> fields = new ArrayList<>();
        for (final String field : filteredMeasure) {
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

        Phenomenon phenomenon = OMUtils.getPhenomenon("2.0.0", fields, "", phenomenons);
        // update phenomenon list
        if (!phenomenons.contains(phenomenon)) {
            phenomenons.add(phenomenon);
        }

        final AbstractDataRecord datarecord;
        switch (observationType) {
            case "Timeserie" : datarecord = OMUtils.getDataRecordTimeSeries("2.0.0", fields);break;
            case "Trajectory": datarecord = getDataRecordTrajectory("2.0.0", fields); break;
            case "Profile"   : datarecord = getDataRecordProfile("2.0.0", fields);break;
            default: throw new IllegalArgumentException("Unexpected observation type:" + observationType + ". Allowed values are Timeserie, Trajectory, Profile.");
        }

        result.observations.add(OMUtils.buildObservation(oid,                                      // id
                                                         sp,                                       // foi
                                                         phenomenon,                               // phenomenon
                                                         procedureID,                              // procedure
                                                         currentCount,                             // count
                                                         datarecord,                               // result structure
                                                         msb,                                      // measures
                                                         currentSpaBound.getTimeObject("2.0.0"))   // time
                            );
        
        result.addFeatureOfInterest(sp);
        
        if (!result.phenomenons.contains(phenomenon)) {
            result.phenomenons.add(phenomenon);
        }
        
        for (String mf : filteredMeasure) {
            if (!result.fields.contains(mf)) {
                result.fields.add(mf);
            }
        }
        
        // build procedure tree
        final ProcedureTree procedure = getOrCreateProcedureTree(result, procedureID, PROCEDURE_TREE_TYPE, observationType.toLowerCase());
        for (Entry<Long, List<DirectPosition>> entry : historicalPositions.entrySet()) {
            procedure.spatialBound.addLocation(new Date(entry.getKey()), buildGeom(entry.getValue()));
        }
        procedure.spatialBound.merge(currentSpaBound);
        
    }
    


    @Override
    public void close() throws DataStoreException {
        // do nothing
    }

    @Override
    public Set<String> getPhenomenonNames() {
        return measureColumns;
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
    public List<ProcedureTree> getProcedures() throws DataStoreException {
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

    private String getObsTypeCode() {
        switch (observationType) {
            case "Timeserie" : return "TS";
            case "Trajectory": return "TR";
            case "Profile"   : return "PR";
            default: throw new IllegalArgumentException("Unexpected observation type:" + observationType + ". Allowed values are Timeserie, Trajectory, Profile.");
        }
    }
}
