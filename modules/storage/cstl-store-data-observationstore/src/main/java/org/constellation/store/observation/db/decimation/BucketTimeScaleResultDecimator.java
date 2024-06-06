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
import java.util.Map;
import org.constellation.store.observation.db.OM2Utils;
import org.constellation.store.observation.db.TimeScaleResultDecimator;
import static org.constellation.store.observation.db.OM2Utils.getTimeScalePeriod;
import org.constellation.store.observation.db.model.ProcedureInfo;
import org.constellation.util.FilterSQLRequest;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.OMEntity;

/**
 *
 * @author guilhem
 */
public class BucketTimeScaleResultDecimator extends TimeScaleResultDecimator {

    public BucketTimeScaleResultDecimator(List<Field> fields, boolean includeId, int width, List<Integer> fieldFilters,  boolean includeTimeInProfile, ProcedureInfo procedure) {
        super(fields, includeId, width, fieldFilters, includeTimeInProfile, procedure);
    }

    @Override
    public void computeRequest(FilterSQLRequest sqlRequest, int offset, boolean firstFilter, Connection c) throws SQLException {
        // calculate step
        final Map<Object, long[]> times = OM2Utils.getMainFieldStep(sqlRequest.clone(), fields, c, width, OMEntity.RESULT, procedure);
        final long step;
        if (profile) {
            // choose the first step
            // (may be replaced by one request by observation, maybe by looking if the step is uniform)
            step = times.values().iterator().next()[1];
        } else {
            step = times.get(1)[1];
        }

        StringBuilder select  = new StringBuilder();
        if (profile) {
            // need review for integer overflow
            select.append("time_bucket('").append(step).append("', \"");
        } else {
            select.append("time_bucket('").append(getTimeScalePeriod(step)).append("', \"");
        }
        select.append(procedure.mainField.name).append("\") AS \"step\"");
        for (int i = offset; i < fields.size(); i++) {
             select.append(", avg(\"").append(fields.get(i).name).append("\") AS \"").append(fields.get(i).name).append("\"");
        }
        
        if (profile) {
            select.append(", o.\"id\" as \"oid\" ");
            if (includeTimeInProfile) {
                select.append(", o.\"time_begin\" ");
            }
        }
        
        String mainFieldSelect = "m.\"" + procedure.mainField.name + "\"";
        sqlRequest.replaceFirst(mainFieldSelect, select.toString());

        if (profile) {
            sqlRequest.append(" GROUP BY step, \"oid\" ORDER BY \"oid\", step");
        } else {
            sqlRequest.append(" GROUP BY step ORDER BY step");
        }

        if (firstFilter) {
            sqlRequest.replaceFirst("WHERE", "");
        }
    }
}
