/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2022 Geomatys.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.constellation.store.observation.db;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import org.apache.sis.storage.DataStoreException;
import org.constellation.store.observation.db.OM2BaseReader.ProcedureInfo;
import org.constellation.util.SQLResult;
import static org.geotoolkit.observation.OMUtils.dateFromTS;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.FieldType;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class TimeScaleResultDecimator extends ResultDecimator {

    public TimeScaleResultDecimator(List<Field> fields, boolean includeId, int width, List<Integer> fieldFilters, Field mainField, ProcedureInfo procedure) {
        super(fields, includeId, width, fieldFilters, mainField, procedure);
    }

    @Override
    public void processResults(SQLResult rs) throws SQLException, DataStoreException {
        if (values == null) {
            throw new DataStoreException("initResultBuilder(...) must be called before processing the results");
        }
        int cpt = 0;
        while (rs.nextOnField(mainField.name)) {
            values.newBlock();
            for (int i = 0; i < fields.size(); i++) {
                DbField field = (DbField) fields.get(i);
                String fieldName = field.name;
                int rsIndex = field.tableNumber -1;
                
                // id field
                if (i < mainFieldIndex && field.type == FieldType.TEXT) {
                    values.appendString(procedure.procedureId + "-dec-" + cpt);
                    cpt++;
                    continue;
                // main field
                } else if (i == mainFieldIndex) {
                    fieldName = "step";

                    // special case for profile + datastream on another phenomenon that the main field.
                    // we do not include the main field in the result
                    if (profile && !fieldFilters.isEmpty() && !fieldFilters.contains(1)) {
                        continue;
                    }
                }
                String value;
                switch (field.type) {
                    case TIME:
                        Date t = dateFromTS(rs.getTimestamp(fieldName, rsIndex));
                        values.appendTime(t);
                        break;
                    case QUANTITY:
                        value = rs.getString(fieldName, rsIndex); // we need to kown if the value is null (rs.getDouble return 0 if so).
                        Double d = Double.NaN;
                        if (value != null && !value.isEmpty()) {
                            d = rs.getDouble(fieldName, rsIndex);
                        }
                        values.appendDouble(d);
                        break;
                    default:
                        values.appendString(rs.getString(fieldName, rsIndex));
                        break;
                }
            }
            values.endBlock();
        }
    }
}
