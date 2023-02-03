/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2021 Geomatys.
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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.apache.sis.storage.DataStoreException;
import org.constellation.util.FilterSQLRequest;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.swe.xml.DataArray;
import org.geotoolkit.swe.xml.DataRecord;
import org.geotoolkit.swe.xml.SimpleDataRecord;
import org.geotoolkit.swe.xml.TextBlock;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBWriter;
import org.opengis.observation.Measure;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.opengis.temporal.TemporalObject;

/**
 * @author Guilhem Legal (Geomatys)
 */
public class OM2Utils {

    public static Long getInstantTime(Instant inst) {
        if (inst != null && inst.getDate() != null) {
            return inst.getDate().getTime();
        }
        return null;
    }

    public static Timestamp getInstantTimestamp(Instant inst) {
        if (inst != null && inst.getDate() != null) {
            return new Timestamp(inst.getDate().getTime());
        }
        return null;
    }

    public static void addtimeDuringSQLFilter(FilterSQLRequest sqlRequest, TemporalObject time, String tableAlias) {
        if (time instanceof Period tp) {
            final Timestamp begin = new Timestamp(tp.getBeginning().getDate().getTime());
            final Timestamp end   = new Timestamp(tp.getEnding().getDate().getTime());

            // 1.1 the multiple observations included in the period
            sqlRequest.append(" (").append(tableAlias).append(".\"time_begin\">=").appendValue(begin).append(" AND ").append(tableAlias).append(".\"time_end\"<=").appendValue(end).append(")");
            sqlRequest.append("OR");
            // 1.2 the single observations included in the period
            sqlRequest.append(" (").append(tableAlias).append(".\"time_begin\">=").appendValue(begin).append(" AND ").append(tableAlias).append(".\"time_begin\"<=").appendValue(end).append(" AND ").append(tableAlias).append(".\"time_end\" IS NULL)");
            sqlRequest.append("OR");
            // 2. the multiple observations which overlaps the first bound
            sqlRequest.append(" (").append(tableAlias).append(".\"time_begin\"<=").appendValue(begin).append(" AND ").append(tableAlias).append(".\"time_end\"<=").appendValue(end).append(" AND ").append(tableAlias).append(".\"time_end\">=").appendValue(begin).append(")");
            sqlRequest.append("OR");
            // 3. the multiple observations which overlaps the second bound
            sqlRequest.append(" (").append(tableAlias).append(".\"time_begin\">=").appendValue(begin).append(" AND ").append(tableAlias).append(".\"time_end\">=").appendValue(end).append(" AND ").append(tableAlias).append(".\"time_begin\"<=").appendValue(end).append(")");
            sqlRequest.append("OR");
            // 4. the multiple observations which overlaps the whole period
            sqlRequest.append(" (").append(tableAlias).append(".\"time_begin\"<=").appendValue(begin).append(" AND ").append(tableAlias).append(".\"time_end\">=").appendValue(end).append(")");
        
        } else if (time instanceof Instant inst) {
            final Timestamp instTime = new Timestamp(inst.getDate().getTime());

            sqlRequest.append(" (").append(tableAlias).append(".\"time_begin\"<=").appendValue(instTime).append(" AND ").append(tableAlias).append(".\"time_end\">=").appendValue(instTime).append(")");
            sqlRequest.append("OR");
            sqlRequest.append(" (").append(tableAlias).append(".\"time_begin\"=").appendValue(instTime).append(" AND ").append(tableAlias).append(".\"time_end\" IS NULL)");
        }
    }

    public static void addTimeContainsSQLFilter(FilterSQLRequest sqlRequest, Period tp) {
        final Timestamp begin = new Timestamp(tp.getBeginning().getDate().getTime());
        final Timestamp end   = new Timestamp(tp.getEnding().getDate().getTime());

        // the multiple observations which overlaps the whole period
        sqlRequest.append(" (\"time_begin\"<=").appendValue(begin).append(" AND \"time_end\">=").appendValue(end).append(")");
    }

    public static TextBlock verifyDataArray(final DataArray array) throws DataStoreException {
        if (!(array.getEncoding() instanceof TextBlock encoding)) {
            throw new DataStoreException("Only TextEncoding is supported");
        }
        if (!(array.getPropertyElementType().getAbstractRecord() instanceof DataRecord) &&
            !(array.getPropertyElementType().getAbstractRecord() instanceof SimpleDataRecord)) {
            throw new DataStoreException("Only DataRecord/SimpleDataRecord is supported");
        }
        return encoding;
    }

    public static double getMeasureValue(Object result) {
        double value;
        if (result instanceof org.apache.sis.internal.jaxb.gml.Measure meas) {
            value = meas.value;
        } else {
            value = ((Measure) result).getValue();
        }
        return value;
    }

    public static byte[] getGeometryBytes(Geometry pt) {
        final WKBWriter writer = new WKBWriter();
        return writer.write(pt);
    }

    public static List<DbField> flatFields(List<DbField> fields) {
        final List<DbField> results = new ArrayList<>();
        for (DbField field : fields) {
            results.add(field);
            if (field.qualityFields != null && !field.qualityFields.isEmpty()) {
                for (Field qField : field.qualityFields) {
                    String name = field.name + "_quality_" + qField.name;
                    DbField newField = new DbField(null, qField.type, name, qField.label, qField.description, qField.uom, field.tableNumber);
                    results.add(newField);
                }
            }
        }
        return results;
    }
}
