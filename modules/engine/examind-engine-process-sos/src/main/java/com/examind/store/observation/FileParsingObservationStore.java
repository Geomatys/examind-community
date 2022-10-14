/*
 *     Examind Community - An open source and standard compliant SDI
 *     https://community.examind.com/
 *
 *  Copyright 2022 Geomatys.
 *
 *  Licensed under the Apache License, Version 2.0 (    the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/
package com.examind.store.observation;

import static com.examind.store.observation.FileParsingUtils.buildFOIByGeom;
import static com.examind.store.observation.FileParsingUtils.buildGeom;
import static com.examind.store.observation.FileParsingUtils.extractWithRegex;
import static com.examind.store.observation.FileParsingUtils.getDataRecordProfile;
import static com.examind.store.observation.FileParsingUtils.getDataRecordTrajectory;
import static com.examind.store.observation.FileParsingUtils.parseDouble;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.storage.DataStoreException;
import static org.constellation.api.CommonConstants.RESPONSE_FORMAT_V100_XML;
import static org.constellation.api.CommonConstants.RESPONSE_FORMAT_V200_XML;
import org.geotoolkit.data.csv.CSVStore;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.observation.ObservationFilterReader;
import org.geotoolkit.observation.ObservationReader;
import org.geotoolkit.observation.ObservationStore;
import org.geotoolkit.observation.ObservationWriter;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.sampling.xml.SamplingFeature;
import org.geotoolkit.sos.MeasureStringBuilder;
import org.geotoolkit.observation.model.ExtractionResult;
import org.geotoolkit.observation.model.ExtractionResult.ProcedureTree;
import org.geotoolkit.observation.OMUtils;
import org.geotoolkit.observation.ObservationStoreCapabilities;
import org.geotoolkit.observation.delegate.StoreDelegatingObservationFilter;
import org.geotoolkit.observation.delegate.StoreDelegatingObservationReader;
import org.geotoolkit.observation.model.FieldType;
import org.geotoolkit.observation.model.GeoSpatialBound;
import org.geotoolkit.sos.xml.ResponseModeType;
import org.geotoolkit.sos.xml.SOSXmlFactory;
import org.geotoolkit.swe.xml.AbstractDataRecord;
import org.geotoolkit.swe.xml.Phenomenon;
import org.geotoolkit.util.NamesExt;
import org.opengis.observation.Process;
import org.opengis.feature.FeatureType;
import org.opengis.geometry.DirectPosition;
import org.opengis.temporal.TemporalGeometricPrimitive;
import org.opengis.util.GenericName;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class FileParsingObservationStore extends CSVStore implements ObservationStore {

    protected static final Logger LOGGER = Logger.getLogger("com.examind.store.observation");

    protected static final String PROCEDURE_TREE_TYPE = "Component";

    // main file is not protected in CSVStore.
    protected final Path dataFile;

    // separator is nor protected in CSVStore.
    protected final char delimiter;
    protected final char quotechar;

    protected final List<String> mainColumns;

    // date column expected header 
    protected final List<String> dateColumns;
    // date format correspuding to the dateColumn
    protected final String dateFormat;

    // longitude column expected header
    protected final String longitudeColumn;
    // latitude column expected header
    protected final String latitudeColumn;
    // depth column expected header
    protected final String zColumn;

    // Feature Of interest Column (Optionnal)
    protected final String foiColumn;

    protected Set<String> measureColumns;

    protected final String obsPropId;
    protected final String obsPropName;

    // timeSeries / trajectory / profiles
    protected final String observationType;

    protected final List<String> qualityColumns;
    protected final List<String> qualityTypes;

    /**
     * Act as a single sensor ID if no procedureColumn is supplied.
     * Act as a prefix else.
     */
    protected final String procedureId;
    protected final String procedureColumn;
    protected final String procedureNameColumn;
    protected final String procedureDescColumn;

    protected final String procRegex;
    protected final String uomRegex;
    protected final String obsPropRegex;

    protected final String mimeType;

    protected final boolean noHeader;
    protected final boolean directColumnIndex;

    public FileParsingObservationStore(final Path f, final char separator, final char quotechar, FeatureType ft, 
            final List<String> mainColumn, final List<String> dateColumn, final String dateTimeformat, final String longitudeColumn,
            final String latitudeColumn, final Set<String> measureColumns, String observationType, String foiColumn,
            final String procedureId, final String procedureColumn, final String procedureNameColumn, final String procedureDescColumn, final String procRegex,
            final String zColumn, final String uomRegex, final String obsPropRegex, final String obsPropId, final String obsPropName, String mimeType, final boolean noHeader,
            final boolean directColumnIndex, final List<String> qualityColumns, final List<String> qualityTypes) throws MalformedURLException, DataStoreException{
        super(f, separator, ft);
        this.dataFile = f;
        this.delimiter = separator;
        this.quotechar = quotechar;
        this.mainColumns = mainColumn;
        this.dateColumns = dateColumn;
        this.dateFormat = dateTimeformat;
        this.longitudeColumn = longitudeColumn;
        this.latitudeColumn = latitudeColumn;
        this.measureColumns = measureColumns;
        this.observationType = observationType;
        this.foiColumn = foiColumn;
        this.procedureColumn = procedureColumn;
        this.procedureNameColumn = procedureNameColumn;
        this.procedureDescColumn = procedureDescColumn;
        this.procRegex = procRegex;
        this.zColumn = zColumn;
        this.uomRegex = uomRegex;
        this.obsPropRegex = obsPropRegex;
        this.mimeType = mimeType;
        this.noHeader = noHeader;
        this.directColumnIndex = directColumnIndex;
        this.obsPropId = obsPropId;
        this.obsPropName = obsPropName;
        this.qualityColumns = qualityColumns;
        this.qualityTypes = qualityTypes;

        if (procedureId == null && procedureColumn == null) {
            this.procedureId = IOUtilities.filenameWithoutExtension(dataFile);
        } else if (procedureId == null) {
            this.procedureId = ""; // empty template
        } else {
            this.procedureId = procedureId;
        }
    }

    @Override
    public Set<GenericName> getProcedureNames() throws DataStoreException {
        final Set<GenericName> names = new HashSet<>();
        if (procedureColumn == null) {
            names.add(NamesExt.create(getProcedureID()));
        } else {
            names.addAll(extractProcedures());
        }
        return names;
    }

    protected abstract Set<GenericName> extractProcedures() throws DataStoreException;

    protected String getProcedureID() {
        return procedureId;
    }

    @Override
    public ObservationStoreCapabilities getCapabilities() {
        final Map<String, List<String>> responseFormats = new HashMap<>();
        responseFormats.put("1.0.0", Arrays.asList(RESPONSE_FORMAT_V100_XML));
        responseFormats.put("2.0.0", Arrays.asList(RESPONSE_FORMAT_V200_XML));

        final List<String> responseMode = Arrays.asList(ResponseModeType.INLINE.value());

        return new ObservationStoreCapabilities(false, false, false, new ArrayList<>(), responseFormats, responseMode, false);
    }

    @Override
    public ExtractionResult getResults() throws DataStoreException {
        return getResults(null, null);
    }

    @Override
    public ExtractionResult getResults(final List<String> sensorIDs) throws DataStoreException {
        return getResults(null, sensorIDs);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public ObservationReader getReader() {
        return new StoreDelegatingObservationReader(this);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public ObservationFilterReader getFilter() {
        return new StoreDelegatingObservationFilter(this);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public ObservationWriter getWriter() {
        return null;
    }

    @Override
    public ObservationStoreCapabilities getCapabilities() {
        final Map<String, List<String>> responseFormats = new HashMap<>();
        responseFormats.put("1.0.0", Arrays.asList(RESPONSE_FORMAT_V100_XML));
        responseFormats.put("2.0.0", Arrays.asList(RESPONSE_FORMAT_V200_XML));

        final List<String> responseMode = Arrays.asList(ResponseModeType.INLINE.value());

        return new ObservationStoreCapabilities(false, false, false, new ArrayList<>(), responseFormats, responseMode, false);
    }

    @Override
    public void close() throws DataStoreException {
        // do nothing
    }

    protected ProcedureTree getOrCreateProcedureTree(final ExtractionResult result, final String procedureId, final String procedureName, final String procedureDesc, final String type, final String omType) {
        for (ProcedureTree tree : result.procedures) {
            if (tree.id.equals(procedureId)) {
                return tree;
            }
        }
        ProcedureTree tree = new ProcedureTree(procedureId, procedureName, procedureDesc, type, omType);
        result.procedures.add(tree);
        return tree;
    }

    protected final Map<String, ObservationBlock> observationBlock = new HashMap<>();

    protected ObservationBlock getOrCreateObservationBlock(String procedureId, String procedureName, String procedureDesc, String foiID, Long time, List<String> measureColumns, List<String> mainColumns, String observationType, List<String> qualtityColumns, List<String> qualityTypes) {
        String key = procedureId + '-' + foiID + '-' + time;
        if (observationBlock.containsKey(key)) {
            return observationBlock.get(key);
        } else {
            MeasureBuilder cmb = new MeasureBuilder(observationType.equals("Profile"), measureColumns, mainColumns, qualtityColumns, qualityTypes);
            ObservationBlock ob = new ObservationBlock(procedureId, procedureName, procedureDesc, foiID, cmb, observationType);
            observationBlock.put(key, ob);
            return ob;
        }
    }

    protected void buildObservation(ExtractionResult result, String oid, ObservationBlock ob,
            Set<org.opengis.observation.Phenomenon> phenomenons, final Set<org.opengis.observation.sampling.SamplingFeature> samplingFeatures) {

        // On extrait les types de mesure trouvées dans la donnée
        Map<String, MeasureField> filteredMeasure = ob.getUsedFields();

        if (filteredMeasure.isEmpty() ||
            ("Profile".equals(ob.observationType) && filteredMeasure.size() == 1)) {
            LOGGER.log(Level.FINE, "no measure available for {0}", ob.procedureId);
            return;
        }

        final List<Field> fields = new ArrayList<>();
        int i = 1;
        for (final Entry<String, MeasureField> field : filteredMeasure.entrySet()) {
            String name  = field.getKey();
            String uom   = field.getValue().uom;
            
            uom  = extractWithRegex(uomRegex, name, uom);
            name = extractWithRegex(obsPropRegex, name);

            String label = field.getValue().label != null ? field.getValue().label : name;
            final List<Field> qualityFields = new ArrayList<>();
            for (MeasureField qmField : field.getValue().qualityFields) {
                FieldType fType = FieldType.fromLabel(qmField.type);
                qualityFields.add(new Field(-1, fType, qmField.name, qmField.label, null, qmField.uom));
            }
            fields.add(new Field(i, FieldType.QUANTITY, name, label, null, uom, qualityFields));
            i++;
        }

        // Get existing or create a new Phenomenon
        Phenomenon phenomenon = OMUtils.getPhenomenon("2.0.0", fields, "", phenomenons);
        if (!phenomenons.contains(phenomenon)) {
            phenomenons.add(phenomenon);
        }

        // Get existing or create a new feature of interest
        if (ob.featureID == null) {
            ob.featureID = "foi-" + UUID.randomUUID();
        }
        final SamplingFeature sp = buildFOIByGeom(ob.featureID, ob.getPositions(), samplingFeatures);
        if (!samplingFeatures.contains(sp)) {
            samplingFeatures.add(sp);
        }

        final AbstractDataRecord datarecord;
        switch (ob.observationType) {
            case "Timeserie" : datarecord = OMUtils.getDataRecordTimeSeries("2.0.0", fields);break;
            case "Trajectory": datarecord = getDataRecordTrajectory("2.0.0", fields); break;
            case "Profile"   : datarecord = getDataRecordProfile("2.0.0", fields);break;
            default: throw new IllegalArgumentException("Unexpected observation type:" + ob.observationType + ". Allowed values are Timeserie, Trajectory, Profile.");
        }

        // Construction du measureStringBuilder à partir des données collectées dans le hashmap
        MeasureStringBuilder msb = ob.getResults();
        final int currentCount   = ob.getResultsCount();

        final Process proc = SOSXmlFactory.buildProcess("2.0.0", ob.procedureId, ob.procedureName, ob.procedureDesc);

        result.observations.add(OMUtils.buildObservation(oid,                                      // id
                                                         sp,                                       // foi
                                                         phenomenon,                               // phenomenon
                                                         proc,                                     // procedure
                                                         currentCount,                             // count
                                                         datarecord,                               // result structure
                                                         msb,                                      // measures
                                                         ob.getTimeObject())                       // time
                            );

        result.addFeatureOfInterest(sp);

        if (!result.phenomenons.contains(phenomenon)) {
            result.phenomenons.add(phenomenon);
        }

        for (String mf : filteredMeasure.keySet()) {
            if (!result.fields.contains(mf)) {
                result.fields.add(mf);
            }
        }

        // build procedure tree
        final ProcedureTree procedure = getOrCreateProcedureTree(result, ob.procedureId, ob.procedureName, ob.procedureDesc, PROCEDURE_TREE_TYPE, ob.observationType.toLowerCase());
        for (Map.Entry<Long, List<DirectPosition>> entry : ob.getHistoricalPositions()) {
            procedure.spatialBound.addLocation(new Date(entry.getKey()), buildGeom(entry.getValue()));
        }
        procedure.spatialBound.merge(ob.currentSpaBound);
    }

    @Override
    public TemporalGeometricPrimitive getTemporalBounds() throws DataStoreException {

        try (final DataFileReader reader = getDataFileReader()) {

            // prepare time column indices
            List<Integer> dateIndexes = getColumnIndexes(dateColumns, reader);

            if (dateIndexes.isEmpty()) return null;

            final Iterator<String[]> it = reader.iterator(!noHeader);

            final GeoSpatialBound result = new GeoSpatialBound();
            final SimpleDateFormat sdf = new SimpleDateFormat(this.dateFormat);
            while (it.hasNext()) {
                final String[] line = it.next();
                String value = "";
                for (Integer dateIndex : dateIndexes) {
                    value += line[dateIndex];
                }
                if (!(value = value.trim()).isEmpty()) {
                    result.addDate(sdf.parse(value));
                }
            }
            return result.getTimeObject("2.0.0");
            
        } catch (IOException | ParseException ex) {
            throw new DataStoreException("Failed extracting dates from input file: " + ex.getMessage(), ex);
        }
    }

    /**
     * Extract the current location of the specified procedure in the current line.
     *
     * Method overriden by sub-classes
     *
     * @param latitudeIndex column index of the latitude or {@code -1} if not available
     * @param longitudeIndex column index of the longitude or {@code -1} if not available
     * @param procedure current procedure.
     * @param line The current csv line processed
     *
     * @return A LAT / LON double array or an empty array if not found
     * @throws ParseException if the values of lat or lon column can not be parsed as a double
     */
    protected double[] extractLinePosition(int latitudeIndex, int longitudeIndex, String procedure, String[] line) throws ParseException, NumberFormatException {
        if (latitudeIndex != -1 && longitudeIndex != -1) {
            final double latitude = parseDouble(line[latitudeIndex]);
            final double longitude = parseDouble(line[longitudeIndex]);
            return new double[] {latitude, longitude};
        }
        return new double[0];
    }

    protected Optional<Long> parseDate(String[] line, final Long preComputeValue, List<Integer> dateIndexes, final DateFormat sdf, int lineNumber) {
        if (preComputeValue != null) return Optional.of(preComputeValue);
        String value = "";
        for (Integer dateIndex : dateIndexes) {
            value += line[dateIndex];
        }
        try {
            return Optional.of(sdf.parse(value).getTime());
        } catch (ParseException ex) {
            LOGGER.fine(String.format("Problem parsing date for date field at line %d (value='%s'). skipping line...", lineNumber, value));
            return Optional.empty();
        }
    }

    protected Optional<? extends Number> parseMain(String[] line, final Long preComputeDateValue, List<Integer> mainIndexes, final DateFormat sdf, int lineNumber, String currentObsType) throws DataStoreException {
        
        // assume that for profile main field is a double
        if ("Profile".equals(currentObsType)) {
            if (mainIndexes.size() > 1) {
                throw new DataStoreException("Multiple main columns is not yet supported for Profile");
            }
            String value = line[mainIndexes.get(0)];
            try {
                return Optional.of(parseDouble(value));
            } catch (ParseException | NumberFormatException ex) {
                LOGGER.fine(String.format("Problem parsing double for main field at line %d (value='%s'). skipping line...", lineNumber, value));
                return Optional.empty();
            }

        // assume that is a date otherwise
        } else {
            return parseDate(line, preComputeDateValue, mainIndexes, sdf, lineNumber);
        }
    }
    
    protected int getColumnIndex(String columnName, DataFileReader reader) throws IOException {
        if (columnName == null) return -1;
        if (directColumnIndex) {
            return Integer.parseInt(columnName);
        }
        final String[] headers = reader.getHeaders();
        return getColumnIndex(columnName, headers);
    }

    protected int getColumnIndex(String columnName, String[] headers) throws IOException {
        return getColumnIndex(columnName, headers, null);
    }

    protected int getColumnIndex(String columnName, String[] headers, List<Integer> appendIndex) throws IOException {
        if (columnName == null) return -1;
        if (directColumnIndex) {
            return Integer.parseInt(columnName);
        }
        for (int i = 0; i < headers.length; i++) {
            final String header = headers[i];
            if (header.equals(columnName)) {
                if (appendIndex != null) {
                    appendIndex.add(i);
                }
                return i;
            }
        }
        return -1;
    }

    protected List<Integer> getColumnIndexes(Collection<String> columnNames, DataFileReader reader) throws IOException {
        if (directColumnIndex) {
            List<Integer> results = new ArrayList<>();
            for (String columnName : columnNames) {
                results.add(Integer.parseInt(columnName));
            }
            return results;
        }
        final String[] headers = reader.getHeaders();
        return getColumnIndexes(columnNames, headers);
    }

    protected List<Integer> getColumnIndexes(Collection<String> columnNames, String[] headers) throws IOException {
        return getColumnIndexes(columnNames, headers, null);
    }

    protected List<Integer> getColumnIndexes(Collection<String> columnNames, String[] headers, Collection<String> appendName) throws IOException {
        List<Integer> results = new ArrayList<>();
        if (directColumnIndex) {
            for (String columnName : columnNames) {
                int index = Integer.parseInt(columnName);
                results.add(index);
                if (headers != null) {
                    appendName.add(headers[index]);
                }
            }
            return results;
        }
        for (int i = 0; i < headers.length; i++) {
            final String header = headers[i];
            if (columnNames.contains(header)) {
                results.add(i);
            }
            if (appendName != null) {
                appendName.add(header);
            }
        }
        return results;
    }

    protected DataFileReader getDataFileReader() throws IOException {
        return FileParsingUtils.getDataFileReader(mimeType, dataFile, delimiter, quotechar);
    }
}
