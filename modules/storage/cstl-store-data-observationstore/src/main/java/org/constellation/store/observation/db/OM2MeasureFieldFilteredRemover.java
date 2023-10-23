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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.apache.sis.storage.DataStoreException;
import org.constellation.store.observation.db.OM2ObservationWriter.ObservationInfos;
import org.constellation.store.observation.db.ResultValuesIterator.DataLine;
import org.constellation.util.Util;
import org.geotoolkit.observation.model.ComplexResult;
import org.geotoolkit.observation.model.MeasureResult;
import org.geotoolkit.observation.model.Observation;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;

/**
 * Update measure by emptying one or more fields inside an observation by looking for matching main values.
 * Work only for time series.
 *
 * This will possibly leave some empty row.
 *
 *  @author Guilhem Legal (Geomatys)
 */
public class OM2MeasureFieldFilteredRemover extends OM2MeasureHandler {

    private final ObservationInfos obsInfo;

    private final List<InsertDbField> fields;
    
     // calculated
    private final Collection<String> emptyFieldRequests;

    public OM2MeasureFieldFilteredRemover(ObservationInfos obsInfo, String schemaPrefix, final OMSQLDialect dialect, final List<InsertDbField> fields) throws DataStoreException {
        super(obsInfo.pi, schemaPrefix, dialect);
        this.obsInfo = obsInfo;
        this.fields = fields;
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

        final String mainFieldName = pi.mainField.name;
        Map<Integer, String> results = new HashMap<>();
        for (Map.Entry<Integer, StringBuilder> builder : builders.entrySet()) {
            int tableNum      = builder.getKey();
            StringBuilder sql = builder.getValue();
            sql.deleteCharAt(sql.length() - 1);

            if (tableNum > 1) {
                sql.append(" WHERE  \"id\" = (SELECT \"id\" FROM \"" + schemaPrefix + "mesures\".\"" + baseTableName + "\" WHERE \"" + mainFieldName + "\" = ? )");
            } else {
                sql.append(" WHERE  \"" + mainFieldName + "\" = ?");
            }

            results.put(builder.getKey(), sql.toString());
        }
        return results.values();
    }

    private List<PreparedStatement> prepareStatements(final Connection c) throws SQLException {
        List<PreparedStatement> results = new ArrayList<>();
        for (String sql : emptyFieldRequests) {
            results.add(c.prepareStatement(sql));
        }
        return results;
    }

    public int removeMeasures(final Connection c, final Observation obs) throws SQLException {
        final List<PreparedStatement> stmts = prepareStatements(c);
        int cptRemoved = 0;
        try {
            if (obs.getResult() instanceof MeasureResult) {
                if (obs.getSamplingTime() instanceof Instant inst) {
                    final Timestamp t = new Timestamp(inst.getDate().getTime());
                    boolean removed = false;
                    for (PreparedStatement stmt : stmts) {
                        stmt.setTimestamp(1, t);
                        // if one table match all the other table should match also
                        removed = stmt.executeUpdate() == 1;
                    }
                    if (removed) cptRemoved++;
                }
            } else if (obs.getResult() instanceof ComplexResult cr) {

                boolean hasFilter = false;
                Timestamp boundBegin = null;
                Timestamp boundEnd = null;
                if (obsInfo.time instanceof Period p) {
                    boundBegin = new Timestamp(p.getBeginning().getDate().getTime());
                    boundEnd   = new Timestamp(p.getEnding().getDate().getTime());
                    hasFilter  = true;
                } else if (obsInfo.time != null) {
                    throw new IllegalArgumentException("Unexpected observation time bounds type:" + obsInfo.time.getClass().getName());
                }

                final ResultValuesIterator vi = new ResultValuesIterator(cr, dialect);
                final List<DataLine> blocks = vi.getDataLines();

                for (DataLine block : blocks) {
                    Date d = block.getMainValue();
                    if (d == null) {
                        continue;
                    }
                    final Timestamp t = new Timestamp(d.getTime());

                    if (!hasFilter || (t.after(boundBegin) && t.before(boundEnd) || t.equals(boundBegin) || t.equals(boundEnd))) {
                        boolean removed = false;
                        for (PreparedStatement stmt : stmts) {
                            stmt.setTimestamp(1, t);
                            // if one table match all the other table should match also
                            removed = stmt.executeUpdate() == 1;
                        }
                        if (removed) cptRemoved++;
                    }
                }

            } else {
                throw new IllegalArgumentException("Unexpected observation result type");
            }
        } finally {
            for (PreparedStatement stmt : stmts) {
                try {stmt.close();} catch (SQLException ex) {LOGGER.log(Level.FINER, "Error while closing delete statement", ex);}
            }
        }
        return cptRemoved;
    }
}
