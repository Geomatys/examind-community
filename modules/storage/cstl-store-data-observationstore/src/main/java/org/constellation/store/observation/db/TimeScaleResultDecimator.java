/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com
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

import org.constellation.store.observation.db.model.DbField;
import org.constellation.store.observation.db.decimation.AbstractResultDecimator;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import org.apache.sis.storage.DataStoreException;
import org.constellation.store.observation.db.model.ProcedureInfo;
import org.constellation.util.SQLResult;
import static org.geotoolkit.observation.OMUtils.dateFromTS;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.FieldType;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class TimeScaleResultDecimator extends AbstractResultDecimator {

    public TimeScaleResultDecimator(List<Field> fields, boolean includeId, int width, List<Integer> fieldFilters, boolean includeTimeInProfile, ProcedureInfo procedure) {
        super(fields, includeId, width, fieldFilters, includeTimeInProfile, procedure);
    }

    @Override
    public void processResults(SQLResult rs, int fieldOffset) throws SQLException, DataStoreException {
        if (values == null) {
            throw new DataStoreException("initResultBuilder(...) must be called before processing the results");
        }
        int cpt = 0;
        while (rs.nextOnField("step")) {
            values.newBlock();
            for (int i = 0; i < fields.size(); i++) {
                DbField field = (DbField) fields.get(i);
                String fieldName = field.name;
                int rsIndex = field.tableNumber;

                // main field
                if (i == mainFieldIndex) {
                    fieldName = "step";

                    // special case for profile + datastream on another phenomenon that the main field.
                    // we do not include the main field in the result
                    if (skipProfileMain) {
                        continue;
                    }
                // id field
                } else if (i < fieldOffset && field.type == FieldType.TEXT) {
                    values.appendString(procedure.id + "-dec-" + cpt, false, field);
                    cpt++;
                    continue;
                }
                switch (field.type) {
                    case TIME -> {
                        boolean measureField = i >= fieldOffset;
                        Date t;
                        // time for profile
                        if (!measureField) {
                            t = dateFromTS(rs.getTimestamp(fieldName));
                        } else {
                            t = dateFromTS(rs.getTimestamp(fieldName, rsIndex));
                        }
                        values.appendTime(t, measureField, field);
                    }
                    case QUANTITY -> {
                        double dvalue = rs.getDouble(fieldName, rsIndex);
                        if (rs.wasNull(rsIndex)) {
                            dvalue = Double.NaN;
                        }
                        values.appendDouble(dvalue, true, field);
                    }
                    default-> {
                        values.appendString(rs.getString(fieldName, rsIndex), true, field);
                    }
                }
            }
            values.endBlock();
        }
    }
}
