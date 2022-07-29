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
import java.util.logging.Logger;
import org.apache.sis.storage.DataStoreException;
import static org.geotoolkit.observation.OMUtils.dateFromTS;
import org.geotoolkit.observation.ResultBuilder;
import org.geotoolkit.observation.model.Field;

/**
 *
 * @author guilhem
 */
public class ResultProcessor {

    protected static final Logger LOGGER = Logger.getLogger("org.constellation.store.observation.db");
    
    protected final ResultBuilder values;
    protected final List<Field> fields;
    protected final boolean profileWithTime;
    protected final boolean profile;

    public ResultProcessor(ResultBuilder values, List<Field> fields, boolean profileWithTime, boolean profile) {
        this.values = values;
        this.fields = fields;
        this.profileWithTime = profileWithTime;
        this.profile = profile;
    }

    public void processResults(ResultSet rs) throws SQLException, DataStoreException {
        while (rs.next()) {
            values.newBlock();
            if (profileWithTime) {
                Date t = dateFromTS(rs.getTimestamp("time_begin"));
                values.appendTime(t);
            }
            for (int i = 0; i < fields.size(); i++) {
                Field field = fields.get(i);
                String value;
                switch (field.type) {
                    case TIME:
                        Date t = dateFromTS(rs.getTimestamp(field.name));
                        values.appendTime(t);
                        break;
                    case QUANTITY:
                        value = rs.getString(field.name); // we need to kown if the value is null (rs.getDouble return 0 if so).
                        Double d = Double.NaN;
                        if (value != null && !value.isEmpty()) {
                            d = rs.getDouble(field.name);
                        }
                        values.appendDouble(d);
                        break;
                    case BOOLEAN:
                        boolean bvalue = rs.getBoolean(field.name);
                        values.appendBoolean(bvalue);
                        break;
                    default:
                        values.appendString(rs.getString(field.name));
                        break;
                }
            }
            values.endBlock();
        }
    }
}
