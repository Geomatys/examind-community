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
import static com.examind.store.observation.FileParsingUtils.normalizeFieldName;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.observation.model.FieldType;
import org.opengis.parameter.ParameterValueGroup;

/**
 * Abstract classfor CSV / DBF stores.
 * 
 * @author Guilhem Legal (Geomatys)
 */
public abstract class AbstractCsvStore extends FileParsingObservationStore {
    
    protected final List<String> obsPropColumnsTypes;

    protected final List<String> uomIds;

    public AbstractCsvStore(final ParameterValueGroup params) throws DataStoreException,IOException {
        super(params);
        this.obsPropColumnsTypes = getMultipleValuesList(params, OBS_PROP_COLUMN_TYPE.getName().getCode());
        this.uomIds = getMultipleValuesList(params, UOM_ID.getName().getCode());
    }
    
    protected List<MeasureField> getObsPropFields(List<Integer> obsPropIndexes, List<Integer> qualityIndexes, String[] headers) {
        final List<MeasureField> results = new ArrayList<>();
        for (int i = 0; i < obsPropIndexes.size(); i++) {
            int index = obsPropIndexes.get(i);
            FieldType ft = FieldType.QUANTITY;
            if (i < obsPropColumnsTypes.size()) {
                ft = FieldType.valueOf(obsPropColumnsTypes.get(i));
            }
            // for now we handle only one quality field by field
            List<MeasureField> qualityFields = new ArrayList<>();
            if (i < qualityColumns.size()) {
                int qIndex = qualityIndexes.get(i);
                String qName = headers[qIndex];
                if (i < qualityColumnsIds.size()) {
                    qName = qualityColumnsIds.get(i);
                }
                qName = normalizeFieldName(qName);
                FieldType qtype = FieldType.TEXT;
                if (i < qualityColumnsTypes.size()) {
                    qtype = FieldType.valueOf(qualityColumnsTypes.get(i));
                }
                qualityFields.add(new MeasureField(qIndex, qName, qtype, List.of()));
            }
            String fieldName;
            if (i < obsPropIds.size()) {
                fieldName = obsPropIds.get(i);
            } else {
                fieldName = headers[index];
            }
            MeasureField mf = new MeasureField(index, fieldName, ft, qualityFields);
            results.add(mf);
        }
        return results;
    }
}
