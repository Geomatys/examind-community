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
import org.constellation.util.SQLResult;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import static org.geotoolkit.observation.OMUtils.dateFromTS;
import org.geotoolkit.observation.result.ResultBuilder;
import org.geotoolkit.observation.model.Field;

/**
 *
 * @author guilhem
 */
public class FieldParser {

    public Date firstTime = null;
    public Date lastTime  = null;
    public int nbValue    = 0;
    
    private final List<Field> fields;
    private final boolean profileWithTime;
    private final boolean includeID;
    private final boolean includeQuality;
    private final ResultBuilder values;
    private String name;

    private boolean first = true;

    public FieldParser(List<Field> fields, ResultBuilder values, boolean profileWithTime, boolean includeID, boolean includeQuality, String name) {
        this.profileWithTime = profileWithTime;
        this.fields = fields;
        this.includeID = includeID;
        this.includeQuality = includeQuality;
        this.name = name;
        this.values = values;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void parseLine(SQLResult rs, int offset) throws SQLException {
        values.newBlock();
        for (int i = 0; i < fields.size(); i++) {

            DbField field = (DbField) fields.get(i);
            parseField(field, rs, i, offset, null);

            if (includeQuality && field.qualityFields != null) {
                for (Field qField : field.qualityFields) {
                    parseField((DbField) qField, rs, -1, -1, field);
                }
            }
        }
        nbValue = values.endBlock();
    }

    private void parseField(DbField field, SQLResult rs, int fieldIndex, int offset, Field parent) throws SQLException {
        String fieldName;
        if (parent != null) {
           fieldName = parent.name + "_quality_" + field.name;
        } else {
           fieldName = field.name;
        }
        int rsIndex = field.tableNumber - 1;
        switch (field.type) {
            case TIME:
                // profile with time field
                if (profileWithTime && fieldIndex < offset) {
                    values.appendTime(firstTime);
                } else {
                    Date t = dateFromTS(rs.getTimestamp(fieldName, rsIndex));
                    values.appendTime(t);
                    
                    if (fieldIndex < offset) {
                        if (first) {
                            firstTime = t;
                            first = false;
                        }
                        lastTime = t;
                    }
                }
                break;
            case QUANTITY:
                Double d =  rs.getDouble(fieldName, rsIndex);
                if (rs.wasNull(rsIndex)) {
                    d = Double.NaN;
                }
                values.appendDouble(d);
                break;
            case BOOLEAN:
                boolean bvalue = rs.getBoolean(fieldName, rsIndex);
                values.appendBoolean(bvalue);
                break;
            default:
                String svalue = rs.getString(fieldName, rsIndex);
                if (includeID && fieldName.equals("id")) {
                    svalue = name + '-' + svalue;
                }
                values.appendString(svalue);
                break;
        }
    }

}
