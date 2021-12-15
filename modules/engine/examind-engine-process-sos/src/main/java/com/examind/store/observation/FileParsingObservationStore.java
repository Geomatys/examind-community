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
import static com.examind.store.observation.FileParsingUtils.getDataRecordProfile;
import static com.examind.store.observation.FileParsingUtils.getDataRecordTrajectory;
import static com.examind.store.observation.FileParsingUtils.parseDouble;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.logging.Logging;
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
import org.geotoolkit.sos.xml.SOSXmlFactory;
import org.geotoolkit.swe.xml.AbstractDataRecord;
import org.geotoolkit.swe.xml.Phenomenon;
import org.geotoolkit.util.NamesExt;
import org.opengis.observation.Process;
import org.opengis.feature.FeatureType;
import org.opengis.geometry.DirectPosition;
import org.opengis.util.GenericName;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class FileParsingObservationStore extends CSVStore implements ObservationStore {

    protected static final Logger LOGGER = Logging.getLogger("com.examind.store.observation");

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

    protected final boolean extractUom;

    public FileParsingObservationStore(final Path f, final char separator, final char quotechar, FeatureType ft, 
            final String mainColumn, final String dateColumn, final String dateTimeformat, final String longitudeColumn,
            final String latitudeColumn, final Set<String> measureColumns, String observationType, String foiColumn,
            final String procedureId, final String procedureColumn, final String procedureNameColumn, final String procedureDescColumn, final String zColumn, final boolean extractUom) throws MalformedURLException, DataStoreException{
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
    public Set<GenericName> getProcedureNames() {
        final Set<GenericName> names = new HashSet<>();
        if (procedureColumn == null) {
            names.add(NamesExt.create(getProcedureID()));
        } else {
            try {
                names.addAll(extractProcedures());
            } catch (DataStoreException ex) {
                // TODO change the signature of getProcedureNames to throw DataStoreException
                LOGGER.log(Level.WARNING, "Error while getting procedure names", ex);
            }
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

    protected boolean verifyEmptyLine(String[] line, int lineNumber, List<Integer> doubleFields) {
        boolean empty = true;
        for (int i : doubleFields) {
            try {
                parseDouble(line[i]);
                empty = false;
                break;
            } catch (NumberFormatException | ParseException ex) {
                if (!line[i].isEmpty()) {
                    LOGGER.fine(String.format("Problem parsing double value at line %d and column %d (value='%s')", lineNumber, i, line[i]));
                }
            }
        }
        return empty;
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
            String label = field.getValue().label != null ? field.getValue().label : name;
            String uom   = field.getValue().uom;
            int b = name.indexOf('(');
            int o = name.indexOf(')');

            // if extract uom is set, we are in csv mode, so there is no observed properties name column
            if (extractUom && b != -1 && o != -1 && b < o) {
                name  = field.getKey().substring(0, b).trim();
                uom   = field.getKey().substring(b + 1, o);
                label = name;
            }
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
}
