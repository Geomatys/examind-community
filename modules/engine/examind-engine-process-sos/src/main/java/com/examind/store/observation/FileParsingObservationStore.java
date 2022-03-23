/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2021, Geomatys
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

package com.examind.store.observation;

import static com.examind.store.observation.FileParsingUtils.buildFOIByGeom;
import static com.examind.store.observation.FileParsingUtils.buildGeom;
import static com.examind.store.observation.FileParsingUtils.extractWithRegex;
import static com.examind.store.observation.FileParsingUtils.getDataRecordProfile;
import static com.examind.store.observation.FileParsingUtils.getDataRecordTrajectory;
import com.opencsv.CSVReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import org.apache.sis.storage.DataStoreException;
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
import org.geotoolkit.observation.model.FieldType;
import org.geotoolkit.observation.model.GeoSpatialBound;
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

    protected final String mainColumn;

    // date column expected header 
    protected final String dateColumn;
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

    // timeSeries / trajectory / profiles
    protected final String observationType;

    /**
     * Act as a single sensor ID if no procedureColumn is supplied.
     * Act as a prefix else.
     */
    protected final String procedureId;
    protected final String procedureColumn;
    protected final String procedureNameColumn;
    protected final String procedureDescColumn;

    protected final String uomRegex;
    protected final String obsPropRegex;

    public FileParsingObservationStore(final Path f, final char separator, final char quotechar, FeatureType ft, 
            final String mainColumn, final String dateColumn, final String dateTimeformat, final String longitudeColumn,
            final String latitudeColumn, final Set<String> measureColumns, String observationType, String foiColumn,
            final String procedureId, final String procedureColumn, final String procedureNameColumn, final String procedureDescColumn, 
            final String zColumn, final String uomRegex, final String obsPropRegex) throws MalformedURLException, DataStoreException{
        super(f, separator, ft);
        this.dataFile = f;
        this.delimiter = separator;
        this.quotechar = quotechar;
        this.mainColumn = mainColumn;
        this.dateColumn = dateColumn;
        this.dateFormat = dateTimeformat;
        this.longitudeColumn = longitudeColumn;
        this.latitudeColumn = latitudeColumn;
        this.measureColumns = measureColumns;
        this.observationType = observationType;
        this.foiColumn = foiColumn;
        this.procedureColumn = procedureColumn;
        this.procedureNameColumn = procedureNameColumn;
        this.procedureDescColumn = procedureDescColumn;
        this.zColumn = zColumn;
        this.uomRegex = uomRegex;
        this.obsPropRegex = obsPropRegex;

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

    /**
     * {@inheritDoc }
     */
    @Override
    public ObservationReader getReader() {
        return null;
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

    protected ObservationBlock getOrCreateObservationBlock(String procedureId, String procedureName, String procedureDesc, String foiID, Long time, List<String> measureColumns, String mainColumn, String observationType) {
        String key = procedureId + '-' + foiID + '-' + time;
        if (observationBlock.containsKey(key)) {
            return observationBlock.get(key);
        } else {
            MeasureBuilder cmb = new MeasureBuilder(observationType.equals("Profile"), measureColumns, mainColumn);
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
            name = extractWithRegex(obsPropRegex, name, name);

            String label = field.getValue().label != null ? field.getValue().label : name;
            fields.add(new Field(i, FieldType.QUANTITY, name, label, null, uom));
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

        try (final CSVReader reader = new CSVReader(Files.newBufferedReader(dataFile), delimiter, quotechar)) {

            final Iterator<String[]> it = reader.iterator();

            // at least one line is expected to contain headers information
            if (!it.hasNext()) throw new DataStoreException("csv headers not found");

            // prepare time column indices
            int dateIndex = -1;

            // read headers
            final String[] headers = it.next();
            for (int i = 0; i < headers.length; i++) {
                final String header = headers[i];
                if (dateColumn.equals(header)) {
                    dateIndex = i;
                    break;
                }
            }
            if (dateIndex == -1) return null;

            final GeoSpatialBound result = new GeoSpatialBound();
            final SimpleDateFormat sdf = new SimpleDateFormat(this.dateFormat);
            while (it.hasNext()) {
                final String[] line = it.next();
                String value = line[dateIndex];
                if (value != null && !(value = value.trim()).isEmpty()) {
                    result.addDate(sdf.parse(value));
                }
            }
            return result.getTimeObject("2.0.0");
            
        } catch (IOException | ParseException ex) {
            throw new DataStoreException("Failed extracting dates from input CSV file", ex);
        }
    }
}
