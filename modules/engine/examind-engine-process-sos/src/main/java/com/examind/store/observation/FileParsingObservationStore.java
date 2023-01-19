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

import static com.examind.store.observation.FileParsingObservationStoreFactory.*;
import static com.examind.store.observation.FileParsingUtils.*;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import org.apache.sis.internal.storage.ResourceOnFileSystem;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.Resource;
import static org.constellation.api.CommonConstants.RESPONSE_FORMAT_V100_XML;
import static org.constellation.api.CommonConstants.RESPONSE_FORMAT_V200_XML;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.observation.AbstractObservationStore;
import org.geotoolkit.observation.ObservationFilterReader;
import org.geotoolkit.observation.ObservationReader;
import org.geotoolkit.observation.ObservationStore;
import org.geotoolkit.sos.MeasureStringBuilder;
import org.geotoolkit.observation.OMUtils;
import org.geotoolkit.observation.ObservationStoreCapabilities;
import org.geotoolkit.observation.delegate.StoreDelegatingObservationFilter;
import org.geotoolkit.observation.delegate.StoreDelegatingObservationReader;
import org.geotoolkit.observation.feature.OMFeatureTypes;
import org.geotoolkit.observation.feature.SensorFeatureSet;
import org.geotoolkit.observation.model.ComplexResult;
import org.geotoolkit.observation.model.ObservationDataset;
import org.geotoolkit.observation.model.ProcedureDataset;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.FieldType;
import org.geotoolkit.observation.model.GeoSpatialBound;
import static org.geotoolkit.observation.model.OMEntity.LOCATION;
import org.geotoolkit.observation.model.Observation;
import org.geotoolkit.observation.model.Phenomenon;
import org.geotoolkit.observation.model.Procedure;
import org.geotoolkit.observation.model.ResponseMode;
import org.geotoolkit.observation.model.SamplingFeature;
import static org.geotoolkit.observation.model.TextEncoderProperties.DEFAULT_ENCODING;
import org.geotoolkit.observation.query.AbstractObservationQuery;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.feature.FeatureType;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.temporal.TemporalGeometricPrimitive;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class FileParsingObservationStore extends AbstractObservationStore implements ObservationStore, ResourceOnFileSystem {

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

    protected static final GeometryFactory GF = new GeometryFactory();

    private final FeatureType ft;

    public FileParsingObservationStore(ParameterValueGroup params) throws IOException, DataStoreException{
        super(params);

        this.dataFile = Paths.get((URI) params.parameter(PATH.getName().toString()).getValue());
        Character sep = Parameters.castOrWrap(params).getValue(SEPARATOR);
        this.delimiter = sep != null ? sep : 0;
        Character qc =  Parameters.castOrWrap(params).getValue(CHARQUOTE);
        this.quotechar = qc != null ? qc : 0;

        this.mainColumns = getMultipleValuesList(params, MAIN_COLUMN.getName().toString());
        this.dateColumns = getMultipleValuesList(params, DATE_COLUMN.getName().toString());
        
        this.dateFormat = (String) params.parameter(DATE_FORMAT.getName().toString()).getValue();
        
        this.longitudeColumn = (String) params.parameter(LONGITUDE_COLUMN.getName().toString()).getValue();
        this.latitudeColumn = (String) params.parameter(LATITUDE_COLUMN.getName().toString()).getValue();
        this.measureColumns = getMultipleValues(params, OBS_PROP_COLUMN.getName().getCode());
        this.observationType = (String) params.parameter(OBSERVATION_TYPE.getName().toString()).getValue();
        this.foiColumn = (String) params.parameter(FOI_COLUMN.getName().toString()).getValue();
        this.procedureColumn = (String) params.parameter(PROCEDURE_COLUMN.getName().toString()).getValue();
        this.procedureNameColumn = (String) params.parameter(PROCEDURE_NAME_COLUMN.getName().toString()).getValue();
        this.procedureDescColumn = (String) params.parameter(PROCEDURE_DESC_COLUMN.getName().toString()).getValue();
        this.procRegex = (String) params.parameter(PROCEDURE_REGEX.getName().toString()).getValue();
        this.zColumn = (String) params.parameter(Z_COLUMN.getName().toString()).getValue();
        this.uomRegex = (String) params.parameter(UOM_REGEX.getName().toString()).getValue();
        this.obsPropRegex = (String) params.parameter(OBS_PROP_REGEX.getName().toString()).getValue();
        this.mimeType =  (String) params.parameter(FILE_MIME_TYPE.getName().toString()).getValue();
        this.noHeader = (boolean) params.parameter(NO_HEADER.getName().toString()).getValue();
        this.directColumnIndex = (boolean) params.parameter(DIRECT_COLUMN_INDEX.getName().toString()).getValue();
        this.obsPropId = (String) params.parameter(OBS_PROP_ID.getName().toString()).getValue();
        this.obsPropName = (String) params.parameter(OBS_PROP_NAME.getName().toString()).getValue();
        this.qualityColumns = getMultipleValuesList(params, QUALITY_COLUMN.getName().getCode());
        this.qualityTypes = getMultipleValuesList(params, QUALITY_COLUMN_TYPE.getName().getCode());

        String pid = (String) params.parameter(PROCEDURE_ID.getName().toString()).getValue();
        if (pid == null && procedureColumn == null) {
            this.procedureId = IOUtilities.filenameWithoutExtension(dataFile);
        } else if (pid == null) {
            this.procedureId = ""; // empty template
        } else {
            this.procedureId = pid;
        }
        this.ft = FileParsingUtils.buildFeatureType(dataFile.toUri(), mimeType, delimiter, quotechar, dateColumns, longitudeColumn, latitudeColumn, measureColumns);
    }

    @Override
    public Set<String> getEntityNames(AbstractObservationQuery query) throws DataStoreException {
        if (query.getEntityType() == null) {
            throw new DataStoreException("initialisation of the filter missing.");
        }
        switch (query.getEntityType()) {
            case OBSERVED_PROPERTY:   return extractPhenomenonIds();
            case LOCATION:
            case PROCEDURE:           return extractProcedureIds();
            case FEATURE_OF_INTEREST:
            case OFFERING:
            case OBSERVATION:
            case HISTORICAL_LOCATION:
            case RESULT:
                throw new DataStoreException("entity name listing not implemented yet: " + query.getEntityType());
            default:
                throw new DataStoreException("unexpected object type:" + query.getEntityType());
        }
    }

    @Override
    public synchronized Collection<? extends Resource> components() throws DataStoreException {
        if (featureSets == null) {
            featureSets = new ArrayList<>();
            featureSets.add(new SensorFeatureSet(this, OMFeatureTypes.buildSensorFeatureType(dataFile.getFileName().toString())));
        }
        return featureSets;
    }

    protected abstract Set<String> extractProcedureIds() throws DataStoreException;

    protected abstract Set<String> extractPhenomenonIds() throws DataStoreException;

    protected String getProcedureID() {
        return procedureId;
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

    @Override
    public ObservationStoreCapabilities getCapabilities() {
        final Map<String, List<String>> responseFormats = new HashMap<>();
        responseFormats.put("1.0.0", Arrays.asList(RESPONSE_FORMAT_V100_XML));
        responseFormats.put("2.0.0", Arrays.asList(RESPONSE_FORMAT_V200_XML));

        final List<ResponseMode> responseMode = Arrays.asList(ResponseMode.INLINE);

        return new ObservationStoreCapabilities(false, false, false, new ArrayList<>(), responseFormats, responseMode, false);
    }

    @Override
    public void close() throws DataStoreException {
        // do nothing
    }

    protected ProcedureDataset getOrCreateProcedureTree(final ObservationDataset result, final String procedureId, final String procedureName, final String procedureDesc, final String type, final String omType) {
        for (ProcedureDataset tree : result.procedures) {
            if (tree.getId().equals(procedureId)) {
                return tree;
            }
        }
        ProcedureDataset tree = new ProcedureDataset(procedureId, procedureName, procedureDesc, type, omType, new ArrayList<>(), null);
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

    protected void buildObservation(ObservationDataset result, String oid, ObservationBlock ob,
            Set<Phenomenon> phenomenons, final Set<SamplingFeature> samplingFeatures) {

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
        Phenomenon phenomenon = OMUtils.getPhenomenonModels(null, fields, "", phenomenons);
        if (!phenomenons.contains(phenomenon)) {
            phenomenons.add(phenomenon);
        }

        // Get existing or create a new feature of interest
        final SamplingFeature sp;
        if (ob.featureID == null) {
            ob.featureID = "foi-" + UUID.randomUUID();
            // for unamed foi, we look for equals existing foi
            sp = buildFOIByGeom(ob.featureID, ob.getPositions(), samplingFeatures);
        } else {
            final Geometry geom = buildGeom(ob.getPositions());
            sp = new SamplingFeature(ob.featureID, null, null, null, ob.featureID, geom);
        }
        
        if (!samplingFeatures.contains(sp)) {
            samplingFeatures.add(sp);
        }

        switch (ob.observationType) {
            case "Timeserie"  -> fields.add(0, OMUtils.TIME_FIELD);
            case "Trajectory" -> fields.add(0, OMUtils.TIME_FIELD);
            case "Profile"    -> {}
            default           -> throw new IllegalArgumentException("Unexpected observation type:" + observationType + ". Allowed values are Timeserie, Trajectory, Profile.");
        }

        // Construction du measureStringBuilder à partir des données collectées dans le hashmap
        MeasureStringBuilder msb = ob.getResults();
        final int currentCount   = ob.getResultsCount();

        final Procedure proc = new Procedure(ob.procedureId, ob.procedureName, ob.procedureDesc, null);
        Map<String, Object> properties = new HashMap<>();
        properties.put("type", ob.observationType);

        ComplexResult resultO = new ComplexResult(fields, DEFAULT_ENCODING, msb.getString(), currentCount);
        result.observations.add(new Observation(oid,
                                                oid,
                                                null, null,
                                                "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_ComplexObservation",
                                                proc,
                                                ob.getTimeObject(),
                                                sp,
                                                phenomenon,
                                                null,
                                                resultO,
                                                properties));
        result.addFeatureOfInterest(sp);

        if (!result.phenomenons.contains(phenomenon)) {
            result.phenomenons.add(phenomenon);
        }

        // build procedure tree
        final ProcedureDataset procedure = getOrCreateProcedureTree(result, ob.procedureId, ob.procedureName, ob.procedureDesc, PROCEDURE_TREE_TYPE, ob.observationType.toLowerCase());
        for (Map.Entry<Long, List<Coordinate>> entry : ob.getHistoricalPositions()) {
            procedure.spatialBound.addLocation(new Date(entry.getKey()), buildGeom(entry.getValue()));
        }
        procedure.spatialBound.merge(ob.currentSpaBound);
    }

    @Override
    public TemporalGeometricPrimitive getTemporalBounds() throws DataStoreException {

        try (final DataFileReader reader = getDataFileReader()) {

            // prepare time column indices
            List<Integer> dateIndexes = getColumnIndexes(dateColumns, reader, directColumnIndex);

            if (dateIndexes.isEmpty()) return null;

            final Iterator<Object[]> it = reader.iterator(!noHeader);

            final GeoSpatialBound result = new GeoSpatialBound();
            final SimpleDateFormat sdf = new SimpleDateFormat(this.dateFormat);

            while (it.hasNext()) {
                final Object[] line = it.next();

                Optional<Long> d = parseDate(line, null, dateIndexes, sdf, -1);
                if (d.isPresent()) {
                    result.addDate(d.get());
                }
            }
            return result.getTimeObject();
            
        } catch (IOException ex) {
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
    protected double[] extractLinePosition(int latitudeIndex, int longitudeIndex, String procedure, Object[] line) throws ParseException, NumberFormatException {
        if (latitudeIndex != -1 && longitudeIndex != -1) {
            final double latitude = parseDouble(line[latitudeIndex]);
            final double longitude = parseDouble(line[longitudeIndex]);
            return new double[] {latitude, longitude};
        }
        return new double[0];
    }

    protected Optional<Long> parseDate(Object[] line, final Long preComputeValue, List<Integer> dateIndexes, final DateFormat sdf, int lineNumber) {
        if (preComputeValue != null) return Optional.of(preComputeValue);

        if (dateIndexes.isEmpty()) {
            return Optional.empty();

        } else if (dateIndexes.size() == 1) {
            Object value = line[dateIndexes.get(0)];
            try {
                return  Optional.of(parseObjectDate(value, sdf));
            } catch (ParseException ex) {
                LOGGER.fine(String.format("Problem parsing date for date field at line %d (value='%s'). skipping line...", lineNumber, value));
                return Optional.empty();
            }

        // composite dates are only supported for string column
        } else {
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
    }

    protected Optional<? extends Number> parseMain(Object[] line, final Long preComputeDateValue, List<Integer> mainIndexes, final DateFormat sdf, int lineNumber, String currentObsType) throws DataStoreException {
        
        // assume that for profile main field is a double
        if ("Profile".equals(currentObsType)) {
            if (mainIndexes.size() > 1) {
                throw new DataStoreException("Multiple main columns is not yet supported for Profile");
            }
            Object value = line[mainIndexes.get(0)];
            if (value instanceof String strValue) {
                try {
                    return Optional.of(parseDouble(strValue));
                } catch (ParseException | NumberFormatException ex) {
                    LOGGER.fine(String.format("Problem parsing double for main field at line %d (value='%s'). skipping line...", lineNumber, value));
                    return Optional.empty();
                }
            } else if (value instanceof Number num) {
                return Optional.of(num);
            }
            LOGGER.fine(String.format("Unexpected type for main field at line %d (value='%s') expecting double. skipping line...", lineNumber, value));
            return Optional.empty();

        // assume that is a date otherwise
        } else {
            return parseDate(line, preComputeDateValue, mainIndexes, sdf, lineNumber);
        }
    }
    
    protected DataFileReader getDataFileReader() throws IOException {
        return FileParsingUtils.getDataFileReader(mimeType, dataFile, delimiter, quotechar);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Path[] getComponentFiles() throws DataStoreException {
        return new Path[]{dataFile};
    }

    /*@Override
    public FeatureType getType() throws DataStoreException {
        return ft;
    }

    @Override
    public Stream<Feature> features(boolean parallel) throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Optional<Envelope> getEnvelope() throws DataStoreException {
        return Optional.empty();
    }*/
}
