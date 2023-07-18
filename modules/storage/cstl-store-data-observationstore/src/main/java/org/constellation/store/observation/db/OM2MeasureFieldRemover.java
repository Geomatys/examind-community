/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com
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
package org.constellation.store.observation.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.sis.storage.DataStoreException;
import org.constellation.store.observation.db.OM2ObservationWriter.ObservationInfos;
import org.constellation.util.Util;

/**
 * Update measure by emptying one or more fields for all the measure of an observation .
 *
 * This will possibly leave some empty row.
 * 
 * @author Guilhem Legal (Geomatys)
 */
public class OM2MeasureFieldRemover extends OM2MeasureHandler {

    private final ObservationInfos obsInfo;
    
    private final List<InsertDbField> fields;

     // calculated
    private final Collection<String> emptyFieldRequests;

    public OM2MeasureFieldRemover(ObservationInfos obsInfo, String schemaPrefix, final List<InsertDbField> fields) throws DataStoreException {
        super(obsInfo.pi, schemaPrefix);
        this.fields = fields;
        this.obsInfo = obsInfo;
        this.emptyFieldRequests = buildEmptyRequests();
    }

    /**
     * Build the measure removal requests.
     *
     * @return A SQL request.
     * @throws DataStoreException If a field contains forbidden characters.
     */
    private Collection<String> buildEmptyRequests() throws DataStoreException {
        Map<Integer, StringBuilder> builders = new HashMap<>();
        for (DbField field : fields) {
            if (Util.containsForbiddenCharacter(field.name)) {
                throw new DataStoreException("Invalid field name");
            }
            StringBuilder sql = builders.computeIfAbsent(field.tableNumber, tn ->
            {
                String suffix = "";
                if (tn > 1) {
                    suffix = "_" + tn;
                }
                return new StringBuilder("UPDATE \"" + schemaPrefix + "mesures\".\"" + baseTableName + suffix + "\" SET ");
            });
            sql.append('"').append(field.name).append("\" = NULL ,");
        }

        List<String> results = new ArrayList<>();
        for (StringBuilder builder : builders.values()) {
            builder.deleteCharAt(builder.length() - 1);
            builder.append(" WHERE \"id_observation\" = ?");
            results.add(builder.toString());
        }
        return results;
    }

    public void removeMeasures(final Connection c) throws SQLException {
        for (String sql : emptyFieldRequests) {
            try (final PreparedStatement stmtMes = c.prepareStatement(sql)){
                stmtMes.setInt(1, obsInfo.id);
                stmtMes.executeUpdate();
            }
        }
    }
}
