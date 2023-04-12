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
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import org.apache.sis.storage.DataStoreException;
import org.constellation.store.observation.db.OM2ObservationWriter.ObservationInfos;
import org.geotoolkit.observation.model.ComplexResult;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.MeasureResult;
import org.geotoolkit.observation.model.Observation;
import org.geotoolkit.observation.model.TextEncoderProperties;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;

/**
 * Remove SQL measure one by one if they match the time filter.
 * Only work on timeseries observation.
 *
 *  @author Guilhem Legal (Geomatys)
 */
public class OM2MeasureRemover extends OM2MeasureHandler {

    private final ObservationInfos obsInfo;
    private final Field mainField;

    // calculated
    private final Collection<String> deleteRequests;

    public OM2MeasureRemover(ObservationInfos obsInfo, Field mainField, String schemaPrefix) {
        super(obsInfo.pi, schemaPrefix);
        this.mainField = mainField;
        this.obsInfo = obsInfo;
        this.deleteRequests = buildDeleteRequests();
    }

    /**
     * Build the measure removal requests.
     *
     * @return A SQL request.
     * @throws DataStoreException If a field contains forbidden characters.
     */
    private List<String> buildDeleteRequests() {
        List<String> results = new ArrayList<>();
        for (int i = 0 ; i < pi.nbTable; i++) {
            final String sql;
            if (i > 1) {
                String suffix = "_" + (i + 1);;
                sql = "DELETE FROM \"" + schemaPrefix + "mesures\".\"" + baseTableName + suffix + "\" WHERE  \"id\" = " +
                      "(SELECT \"id\" FROM \"" + schemaPrefix + "mesures\".\"" + baseTableName + "\" WHERE \"" + mainField.name + "\" = ? AND \"id_observation\" = " + obsInfo.id + ")";
            } else {
                sql = "DELETE FROM \"" + schemaPrefix + "mesures\".\"" + baseTableName + "\" WHERE  \"" + mainField.name + "\" = ? AND \"id_observation\" = " + obsInfo.id + "";
            }
            
            results.add(sql);
        }
        return results;
    }

    private List<PreparedStatement> prepareStatements(final Connection c) throws SQLException {
        List<PreparedStatement> results = new ArrayList<>();
        for (String sql : deleteRequests) {
            results.add(c.prepareStatement(sql));
        }
        return results;
    }

    public void removeMeasures(final Connection c, final Observation obs) throws SQLException {
        final List<PreparedStatement> stmts  = prepareStatements(c);
        try {
            if (obs.getResult() instanceof MeasureResult) {
                if (obs.getSamplingTime() instanceof Instant inst) {
                    final Timestamp t = new Timestamp(inst.getDate().getTime());
                    for (PreparedStatement stmt : stmts) {
                        stmt.setTimestamp(1, t);
                        stmt.executeUpdate();
                    }
                }
            } else if (obs.getResult() instanceof ComplexResult cr) {
                // do we need to handle dataObject mode?
                if (cr.getValues() == null) throw new UnsupportedOperationException("Not supported for now. or never");

                Timestamp boundBegin;
                Timestamp boundEnd;
                if (obsInfo.time instanceof Period p) {
                    boundBegin = new Timestamp(p.getBeginning().getDate().getTime());
                    boundEnd   = new Timestamp(p.getEnding().getDate().getTime());
                } else if (obsInfo.time instanceof Instant i) {
                    boundBegin = new Timestamp(i.getDate().getTime());
                    boundEnd = null;
                } else if (obsInfo.time != null) {
                    throw new IllegalArgumentException("Unexpected observation time bounds type:" + obsInfo.time.getClass().getName());
                } else {
                    throw new IllegalArgumentException("A time filter must be supplied");
                }

                final TextEncoderProperties encoding = cr.getTextEncodingProperties();
                final StringTokenizer tokenizer      = new StringTokenizer(cr.getValues(), encoding.getBlockSeparator());

                int rmCount = 0;
                while (tokenizer.hasMoreTokens()) {
                    String block = tokenizer.nextToken().trim();
                    if (block.isEmpty()) {
                        continue;
                    }
                    final String timeValue = block.substring(0, block.indexOf(encoding.getTokenSeparator()));

                    Date d = null;
                    try {
                        d = dateParser.parseToDate(timeValue);
                    } catch (NumberFormatException ex) {
                        LOGGER.log(Level.FINER, "unable to parse the value: {0}", timeValue);
                    }
                    if (d == null) {
                        LOGGER.log(Level.WARNING, "unable to parse the value: {0}", timeValue);
                        continue;
                    }
                    final Timestamp t = new Timestamp(d.getTime());

                    if (t.equals(boundBegin) || (boundEnd != null && t.equals(boundEnd)) || (boundEnd != null && t.after(boundBegin) && t.before(boundEnd) )) {
                        for (PreparedStatement stmt : stmts) {
                            stmt.setTimestamp(1, t);
                            int removed = stmt.executeUpdate();
                            rmCount += removed;
                        }
                    }
                }
                LOGGER.finer("measure removed:" + rmCount);
            }
        
        } finally {
            for (PreparedStatement stmt : stmts) {
                try {stmt.close();} catch (SQLException ex) {LOGGER.log(Level.FINER, "Error while closing delete statement", ex);}
            }
        }
    }
}
