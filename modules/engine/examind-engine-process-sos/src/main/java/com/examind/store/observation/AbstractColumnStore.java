/*
 *     Examind Community - An open source and standard compliant SDI
 *     https://community.examind.com/
 *
 *  Copyright 2024 Geomatys.
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

import static com.examind.store.observation.FileParsingObservationStoreFactory.OBS_PROP_COLUMN_TYPE;
import static com.examind.store.observation.FileParsingObservationStoreFactory.UOM_ID;
import static com.examind.store.observation.FileParsingObservationStoreFactory.getMultipleValuesList;
import static com.examind.store.observation.FileParsingUtils.extractWithRegex;
import static com.examind.store.observation.FileParsingUtils.normalizeFieldName;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.FieldDataType;
import org.geotoolkit.observation.model.FieldType;
import org.opengis.parameter.ParameterValueGroup;

/**
 * Abstract classfor CSV / DBF stores.
 * 
 * @author Guilhem Legal (Geomatys)
 */
public abstract class AbstractColumnStore extends FileParsingObservationStore {
    
    protected final List<String> obsPropColumnsTypes;

    protected final List<String> uomIds;

    public AbstractColumnStore(final ParameterValueGroup params) throws DataStoreException,IOException {
        super(params);
        this.obsPropColumnsTypes = getMultipleValuesList(params, OBS_PROP_COLUMN_TYPE.getName().getCode());
        this.uomIds = getMultipleValuesList(params, UOM_ID.getName().getCode());
    }
    
    protected List<CsvField> getObsPropFields(List<Integer> obsPropIndexes, List<Integer> qualityIndexes, List<Integer> parameterIndexes, String[] headers) {
        final List<CsvField> results = new ArrayList<>();
        for (int i = 0; i < obsPropIndexes.size(); i++) {
            int index = obsPropIndexes.get(i);
            FieldDataType ft = FieldDataType.QUANTITY;
            if (i < obsPropColumnsTypes.size()) {
                ft = FieldDataType.valueOf(obsPropColumnsTypes.get(i));
            }
            // for now we handle only one quality/parameter field by field
            CsvField qField = parseExtraField(i, qualityIndexes, qualityColumnsIds, qualityColumnsTypes, headers, FieldType.QUALITY);
            List<Field> qualityFields = qField != null ? List.of(qField) : List.of();
            
            CsvField pField = parseExtraField(i, parameterIndexes, parameterColumnsIds, parameterColumnsTypes, headers, FieldType.PARAMETER);
            List<Field> parameterFields = pField != null ? List.of(pField) : List.of();
            
            String name;
            if (i < obsPropIds.size()) {
                name = obsPropIds.get(i);
            } else {
                name = headers[index];
            }

            String uom  = extractWithRegex(uomRegex, name, null);
            name        = extractWithRegex(obsPropRegex, name);
            CsvField mf = new CsvField(index, name, ft, uom, FieldType.MEASURE, qualityFields, parameterFields);
            results.add(mf);
        }
        return results;
    }
    
    private static CsvField parseExtraField(int i, List<Integer> indexes, List<String> columnIds, List<String> columnTypes, String[] headers, FieldType type) {
        if (i < indexes.size()) {
            int qIndex = indexes.get(i);
            String qName = headers[qIndex];
            if (i < columnIds.size()) {
                qName = columnIds.get(i);
            }
            qName = normalizeFieldName(qName);
            FieldDataType qtype = FieldDataType.TEXT;
            if (i < columnTypes.size()) {
                qtype = FieldDataType.valueOf(columnTypes.get(i));
            }
            return new CsvField(qIndex, qName, qtype, type);
        }
        return null;
    }
    protected List<ObservedProperty> getObservedProperties(List<String> measureFields) {
        List<ObservedProperty> fixedObsProperties = new ArrayList<>();
        if (!obsPropIds.isEmpty()) {
            for (int i = 0; i < obsPropIds.size(); i++) {
                String id = obsPropIds.get(i);
                String name = (obsPropNames.size() > i) ? obsPropNames.get(i) : id;
                String uom = (uomIds.size() > i) ? uomIds.get(i) : null;
                String desc = (obsPropDescs.size() > i) ? obsPropDescs.get(i) : null;
                fixedObsProperties.add(createFixedObservedProperty(id, name, uom, desc, new HashMap<>()));
                measureFields.add(id);
            }
        }
        return fixedObsProperties;
    }
    
    // for overriding store
    protected ObservedProperty createFixedObservedProperty(String id, String name, String uom, String description, Map<String, Object> properties) {
        return new ObservedProperty(id, name, uom, description, properties);
    }
}
