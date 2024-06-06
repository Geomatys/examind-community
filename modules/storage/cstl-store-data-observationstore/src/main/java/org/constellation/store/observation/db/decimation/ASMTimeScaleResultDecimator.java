/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com/
 *
 * Copyright 2023 Geomatys.
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
package org.constellation.store.observation.db.decimation;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.constellation.store.observation.db.TimeScaleResultDecimator;
import org.constellation.store.observation.db.model.ProcedureInfo;
import org.constellation.util.FilterSQLRequest;
import org.geotoolkit.observation.model.Field;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ASMTimeScaleResultDecimator extends TimeScaleResultDecimator {

    public ASMTimeScaleResultDecimator(List<Field> fields, boolean includeId, int width, List<Integer> fieldFilters,  boolean includeTimeInProfile, ProcedureInfo procedure) {
        super(fields, includeId, width, fieldFilters, includeTimeInProfile, procedure);
    }

    @Override
    public void computeRequest(FilterSQLRequest sqlRequest, int offset, boolean firstFilter, Connection c) throws SQLException {
        // extract value field
        Field f = fields.get(mainFieldIndex + 1);

        StringBuilder select  = new StringBuilder();
        select.append("time as \"step\", value as \"").append(f.name);
        select.append("\" FROM unnest ((SELECT asap_smooth(");
        select.append("\"").append(procedure.mainField.name).append("\",");
        select.append("\"").append(fields.get(offset).name).append("\",");
        select.append(width).append(")");

        // will not work
        if (profile) {
            select.append(", o.\"id\" as \"oid\" ");
        }
        // will not work
        if (profile && includeTimeInProfile) {
            select.append(", o.\"time_begin\" ");
        }
        
        String mainFieldSelect = "m.\"" + procedure.mainField.name + "\"";
        sqlRequest.replaceFirst(mainFieldSelect, select.toString());
        if (firstFilter) {
            sqlRequest.replaceFirst("WHERE", "");
        }
        sqlRequest.append("))"); // close unnest
    }

}
