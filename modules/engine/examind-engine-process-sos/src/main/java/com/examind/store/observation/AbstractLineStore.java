/*
 *     Examind Community - An open source and standard compliant SDI
 *     https://community.examind.com/
 *
 *  Copyright 2026 Geomatys.
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

import static com.examind.store.observation.FileParsingObservationStoreFactory.OBS_PROP_COLUMN;
import static com.examind.store.observation.FileParsingObservationStoreFactory.OBS_PROP_DESC_COLUMN;
import static com.examind.store.observation.FileParsingObservationStoreFactory.OBS_PROP_FILTER_COLUMN;
import static com.examind.store.observation.FileParsingObservationStoreFactory.OBS_PROP_NAME_COLUMN;
import static com.examind.store.observation.FileParsingObservationStoreFactory.OBS_PROP_PROPERTIES_COLUMN;
import static com.examind.store.observation.FileParsingObservationStoreFactory.OBS_PROP_PROPERTIES_MAP_COLUMN;
import static com.examind.store.observation.FileParsingObservationStoreFactory.RESULT_COLUMN;
import static com.examind.store.observation.FileParsingObservationStoreFactory.TYPE_COLUMN;
import static com.examind.store.observation.FileParsingObservationStoreFactory.UOM_COLUMN;
import static com.examind.store.observation.FileParsingObservationStoreFactory.getMultipleValues;
import static com.examind.store.observation.FileParsingObservationStoreFactory.getMultipleValuesList;
import static com.examind.store.observation.FileParsingUtils.*;
import static com.examind.store.observation.csvflat.CsvFlatUtils.extractCodes;
import java.io.IOException;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import org.apache.sis.storage.DataStoreException;
import org.constellation.exception.ConstellationStoreException;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.FieldDataType;
import org.geotoolkit.observation.model.FieldType;
import org.geotoolkit.observation.model.ObservationType;
import static org.geotoolkit.observation.model.ObservationType.PROFILE;
import static org.geotoolkit.observation.model.ObservationType.TIMESERIES;
import static org.geotoolkit.observation.model.ObservationType.TRAJECTORY;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class AbstractLineStore extends FileParsingObservationStore {
    
    protected final String valueColumn;
    protected final Set<String> csvFlatobsPropColumns;
    protected final List<String> obsPropNameColumns;
    protected final List<String> obsPropDescColumns;
    protected final String typeColumn;
    protected final String uomColumn;
    
    protected final String obsPropPropertiesMapColumn;
    protected final Set<String> obsPropPropertieColumns;
    
    /**
     * use to avoid loading obsPropColumns at store creation.
     */
    private boolean obsPropColumnsLoaded = false;
    protected final Set<String> obsPropFilterColumns;
    
    
    private final boolean applyObsPropFilterOnprocedure;
    
    public AbstractLineStore(ParameterValueGroup params, boolean applyObsPropFilterOnprocedure) throws IOException, DataStoreException {
        super(params);
        
        this.valueColumn = (String) params.parameter(RESULT_COLUMN.getName().toString()).getValue();
        this.csvFlatobsPropColumns = getMultipleValues(params, OBS_PROP_COLUMN.getName().toString());
        this.obsPropNameColumns = getMultipleValuesList(params, OBS_PROP_NAME_COLUMN.getName().toString());
        this.obsPropDescColumns = getMultipleValuesList(params, OBS_PROP_DESC_COLUMN.getName().toString());
        
        this.typeColumn = (String) params.parameter(TYPE_COLUMN.getName().toString()).getValue();
        this.uomColumn = (String) params.parameter(UOM_COLUMN.getName().toString()).getValue();

        this.obsPropFilterColumns = getMultipleValues(params, OBS_PROP_FILTER_COLUMN.getName().toString());
        this.obsPropPropertiesMapColumn = (String) params.parameter(OBS_PROP_PROPERTIES_MAP_COLUMN.getName().toString()).getValue();
        this.obsPropPropertieColumns = getMultipleValues(params, OBS_PROP_PROPERTIES_COLUMN.getName().toString());
        
        
        this.applyObsPropFilterOnprocedure = applyObsPropFilterOnprocedure;
    }
    
     /**
     * Load obsPropColumns.
     * 
     * @return
     * @throws DataStoreException
     */
    protected synchronized  Set<String> getObsPropColumns() throws DataStoreException {
        if (!obsPropColumnsLoaded) {
            // special case for hard coded observed property
            // in flat mode, only one is accepted
            if (!obsPropIds.isEmpty()) {
                this.obsPropColumns = new HashSet(obsPropIds);
            // special case for * measure columns
            // if the store is open with missing mime type we skip this part.
            } else if (obsPropFilterColumns.isEmpty() && mimeType != null) {
                try (final DataFileReader reader = getDataFileReader(getObservationFile())) {
                    this.obsPropColumns = extractCodes(reader, csvFlatobsPropColumns, noHeader, directColumnIndex, obsPropRegex);
                } catch (ConstellationStoreException ex) {
                    throw new DataStoreException(ex.getMessage(), ex);
                } catch (IOException | IndexOutOfBoundsException | InterruptedException ex) {
                    throw new DataStoreException("problem reading variables csv file", ex);
                }
            } else {
                 this.obsPropColumns = obsPropFilterColumns;
            }
            obsPropColumnsLoaded = true;
        }
        return this.obsPropColumns;
    }
    
    protected abstract Path getObservationFile();
    
    protected abstract Path geProcedureFile();
    
    @Override
    public Set<String> extractPhenomenonIds() throws DataStoreException {
        // TODO verify existence?
        return getObsPropColumns();
    }
    
    protected static Object[] parseExtraFields(Object[] line, List<Integer> indexes, String observedProperty, FieldInfos measureColums, FieldType type, DateFormat sdf ) {
        Object[] values = new Object[indexes.size()];
        for (int i = 0; i < indexes.size(); i++) {
            Integer qIndex = indexes.get(i);
            MeasureField mf = measureColums.getExtraField(observedProperty, i, type);
            Object value = line[qIndex];
            try {
                values[i] = parseFieldValue(value, mf.dataType, sdf);
            } catch (ParseException | NumberFormatException ex) {
                LOGGER.fine(String.format("Problem parsing value for extra field  at column %d (value='%s').", qIndex, line[qIndex]));
            }
        }
        return values;
    }
    
    
    @Override
    protected Set<String> extractProcedureIds() throws DataStoreException {
        if (procedureColumn == null) return Collections.singleton(getProcedureID());

        // pre-load the obsProp colmuns has we don't want to open twice the file
        // some DataFileReader are not concurrent (like xlsx) ans this will cause issue
        final Set<String> obspropColumns = getObsPropColumns();

        final Set<String> result = new HashSet();
        // open csv file
        try (final DataFileReader reader = getDataFileReader(geProcedureFile())) {

            String[] headers = null;
            if (!noHeader) {
                headers = reader.getHeaders();
            }
            int lineNumber = 1;
            final AtomicInteger maxIndex  = new AtomicInteger();
            
            // prepare procedure/type column indices
            int procIndex       = getColumnIndex(procedureColumn, headers, directColumnIndex, laxHeader, maxIndex);
            int typeColumnIndex = getColumnIndex(typeColumn,      headers, directColumnIndex, laxHeader, maxIndex);
            
            final List<Integer> obsPropColumnIndexes  = new ArrayList<>();
            if (applyObsPropFilterOnprocedure) {
                obsPropColumnIndexes.addAll(getColumnIndexes(csvFlatobsPropColumns, headers, directColumnIndex, laxHeader, maxIndex, OBS_PROP_QUALIFIER));
            }

            if (procIndex == -1) throw new DataStoreException("Unable to find the procedure column: " + procedureColumn);
            String fixedObsId   = obsPropIds.isEmpty()  ? null  : obsPropIds.get(0);

            final Iterator<Object[]> it = reader.iterator(!noHeader);

            final List<String> obsTypeCodes = getObsTypeCodes();
            while (it.hasNext()) {
                lineNumber++;
                final Object[] line = it.next();

                // verify that the line is complete (meaning that the line is at least as long as the last index we look for)
                if (verifyLineCompletion(line, lineNumber, headers, maxIndex)) {
                    LOGGER.log(Level.FINER, "skipping empty line {0}", lineNumber);
                    continue;
                }
                
                // to be perfectly correct we should look for empty measure
                if (verifyEmptyLineStr(line, lineNumber, Arrays.asList(procIndex))) {
                    LOGGER.fine("skipping line due to empty procedure column.");
                    continue;
                }

                // checks if row matches the observed data types
                if (typeColumnIndex != -1) {
                    if (!obsTypeCodes.contains(asString(line[typeColumnIndex]))) continue;
                }

                // checks if row matches the observed properties filter
                if (applyObsPropFilterOnprocedure) {
                    String observedProperty = getMultiOrFixedValue(line, fixedObsId, obsPropColumnIndexes, obsPropRegex);
                    if (!obspropColumns.contains(observedProperty)) {
                        continue;
                    }
                }

                String procId = extractWithRegex(procRegex, asString(line[procIndex]));
                result.add(procedureId + procId);
            }
            return result;

        } catch (IOException | InterruptedException ex) {
            LOGGER.log(Level.WARNING, "problem reading csv file", ex);
            throw new DataStoreException(ex);
        }
    }
    
    
    /**
     * return the allowed values for the "typeColumn".
     * Dependending if the parameter ObservationType is null or not,
     *
     * @return
     */
    protected List<String> getObsTypeCodes() {
        if (observationType == null) {
            return Arrays.asList("TS", "TR", "PR");
        }
        return switch (observationType) {
            case TIMESERIES ->  Arrays.asList("TS");
            case TRAJECTORY ->  Arrays.asList("TR");
            case PROFILE    ->  Arrays.asList("PR");
            default -> throw new IllegalArgumentException("Unexpected observation type:" + observationType + ". Allowed values are Timeserie, Trajectory, Profile.");
        };
    }

    protected ObservationType getObservationTypeFromCode(String code) {
        return switch (code) {
            case "TS" -> ObservationType.TIMESERIES;
            case "TR" -> ObservationType.TRAJECTORY;
            case "PR" -> ObservationType.PROFILE;
            default-> throw new IllegalArgumentException("Unexpected observation type code:" + code + ". Allowed values are TS, TR, PR.");
        };
    }
    
    protected FieldInfos buildFields(ObservationType currentObstType, final List<String> currentMainColumns, final List<String> sortedMeasureColumns, List<MeasureField> qualityFields, List<MeasureField> parameterFields) {
        List<MeasureField> measureFields = new ArrayList<>();
        if (PROFILE.equals(currentObstType)) {
            if (currentMainColumns.size() > 1) {
                throw new IllegalArgumentException("Multiple main columns is not yet supported for Profile");
            }
            measureFields.add(new MeasureField(-1, currentMainColumns.get(0), FieldDataType.QUANTITY, FieldType.MAIN));
        } else {
            measureFields.add(new MeasureField(-1, "TIME", FieldDataType.TIME, FieldType.MAIN));
        }
        for (int j = 0; j < sortedMeasureColumns.size(); j++) {
            String mc = sortedMeasureColumns.get(j);
            measureFields.add(new MeasureField(-1, mc, FieldDataType.QUANTITY, qualityFields, parameterFields, FieldType.MEASURE));
        }
        return new FieldInfos(measureFields, currentObstType);
    }
    
    protected Map<String, Field> buildFieldMaps(ObservationType currentObstType, final List<String> sortedMeasureColumns, List<Field> qualityFields, List<Field> parameterFields) {
        Map<String, Field> measureFields = new HashMap<>();
         // initialize description
        int offset = PROFILE.equals(currentObstType) ? 1 : 0;
        for (int j = 0, k = offset; j < sortedMeasureColumns.size(); j++, k++) {
            String mc = sortedMeasureColumns.get(j);
            FieldDataType dataType = FieldDataType.QUANTITY;
            FieldType type = FieldType.MEASURE;
            measureFields.put(mc, new Field(k, dataType, mc, mc, null, null, type, qualityFields, parameterFields));
        }
        return measureFields;
    }
}
