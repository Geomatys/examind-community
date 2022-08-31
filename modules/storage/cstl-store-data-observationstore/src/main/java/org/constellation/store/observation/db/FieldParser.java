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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import static org.geotoolkit.observation.OMUtils.dateFromTS;
import org.geotoolkit.observation.ResultBuilder;
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
    private String name;

    private boolean first = false;

    public FieldParser(List<Field> fields, boolean profileWithTime, boolean includeID, String name) {
        this.profileWithTime = profileWithTime;
        this.fields = fields;
        this.includeID = includeID;
        this.name = name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void parseLine(ResultBuilder values, ResultSet rs,  int offset) throws SQLException {

        values.newBlock();
        for (int i = 0; i < fields.size(); i++) {

            Field field = fields.get(i);
            switch (field.type) {
                case TIME:
                    // profile with time field
                    if (profileWithTime && i <= offset) {
                        values.appendTime(firstTime);
                    } else {
                        Date t = dateFromTS(rs.getTimestamp(field.name));
                        values.appendTime(t);
                        if (first) {
                            firstTime = t;
                            first = false;
                        }
                        lastTime = t;
                    }
                    break;
                case QUANTITY:
                    Double d =  rs.getDouble(field.name);
                    if (rs.wasNull()) {
                        d = Double.NaN;
                    }
                    values.appendDouble(d);
                    break;
                case BOOLEAN:
                    boolean bvalue = rs.getBoolean(field.name);
                    values.appendBoolean(bvalue);
                    break;
                default:
                    String svalue = rs.getString(field.name);
                    if (includeID && field.name.equals("id")) {
                        svalue = name + '-' + svalue;
                    }
                    values.appendString(svalue);
                    break;
            }
        }
        nbValue = values.endBlock();
    }

}
