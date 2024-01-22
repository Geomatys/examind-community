/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com
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

import org.constellation.store.observation.db.model.InsertDbField;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.constellation.store.observation.db.model.ProcedureInfo;
import org.constellation.util.FilterSQLRequest;
import org.constellation.util.SQLResult;
import org.geotoolkit.geometry.jts.SRIDGenerator;
import org.geotoolkit.observation.OMUtils;
import org.geotoolkit.observation.model.ComplexResult;
import org.geotoolkit.observation.model.CompositePhenomenon;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.FieldType;
import org.geotoolkit.observation.model.MeasureResult;
import org.geotoolkit.observation.model.OMEntity;
import org.geotoolkit.observation.model.Observation;
import org.geotoolkit.observation.model.Phenomenon;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.opengis.temporal.RelativePosition;
import org.opengis.temporal.TemporalObject;
import org.opengis.util.FactoryException;

/**
 * @author Guilhem Legal (Geomatys)
 */
public class OM2Utils {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.store.observation.db");

    private static Field DEFAULT_TIME_FIELD = new Field(-1, FieldType.TIME, "time", null, "http://www.opengis.net/def/property/OGC/0/SamplingTime", null);

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

    public static List<InsertDbField> flatFields(List<InsertDbField> fields) {
        final List<InsertDbField> results = new ArrayList<>();
        for (InsertDbField field : fields) {
            results.add(field);
            if (field.qualityFields != null && !field.qualityFields.isEmpty()) {
                for (Field qField : field.qualityFields) {
                    String name = field.name + "_quality_" + qField.name;
                    InsertDbField newField = new InsertDbField(null, qField.type, name, qField.label, qField.description, qField.uom, field.tableNumber);
                    results.add(newField);
                }
            }
        }
        return results;
    }

    public static CoordinateReferenceSystem parsePostgisCRS(int srid) throws FactoryException {
        if (srid == 0 || srid == 4326) {
            return CommonCRS.WGS84.normalizedGeographic();
        } else {
            return CRS.forCode(SRIDGenerator.toSRS(srid, SRIDGenerator.Version.V1));
        }
    }

    public static boolean containsField(List<Field> fields, Field field) {
        for (Field f : fields) {
            // only compare name and type
            boolean found = Objects.equals(f.name, field.name)
                        && Objects.equals(f.type, field.type);
            if (found) {
                return true;
            }
        }
        return false;
    }

    public static boolean isEqualsOrSubset(Phenomenon candidate, Phenomenon phenomenon) {
        if (Objects.equals(candidate, phenomenon)) {
            return true;
        } else if (phenomenon instanceof CompositePhenomenon composite) {
            if (candidate instanceof CompositePhenomenon compositeCdt) {
                return OMUtils.isACompositeSubSet(compositeCdt, composite);
            } else if (candidate != null) {
                return OMUtils.hasComponent(candidate.getId(), composite);
            }
        }
        return false;
    }

    public static boolean isPartOf(Phenomenon candidate, Phenomenon phenomenon) {
        if (candidate instanceof CompositePhenomenon compositeCdt) {
            if (phenomenon instanceof CompositePhenomenon composite) {
                return OMUtils.isACompositeSubSet(composite, compositeCdt);
            } else if (phenomenon != null) {
                return OMUtils.hasComponent(phenomenon.getId(), compositeCdt);
            }
        }
        return false;
    }

    public static boolean isTimeIntersect(RelativePosition pos) {
        return pos.equals(RelativePosition.BEGUN_BY) ||
              pos.equals(RelativePosition.ENDED_BY) ||
              pos.equals(RelativePosition.CONTAINS) ||
              pos.equals(RelativePosition.OVERLAPS) ||
              pos.equals(RelativePosition.DURING)   ||
              pos.equals(RelativePosition.BEGINS)   ||
              pos.equals(RelativePosition.ENDS)   ||
              pos.equals(RelativePosition.MET_BY)   ||
              pos.equals(RelativePosition.MEETS);
    }

    public static int getMeasureCount(Observation obs) {
        if (obs.getResult() instanceof ComplexResult cr) {
            return cr.getNbValues();
        } else if (obs.getResult() instanceof MeasureResult) {
            return 1;
        }
        return -1;
    }

    /**
     * Return a list of measure fields for the observation.
     * For a complex result, the main field at index 0 will be removed.
     *
     * @param obs An observation.
     * @return A liste of measure fields
     * 
     * @throws IllegalArgumentException if the observation result has no field or is of an unknown type.
     */
    public static List<Field> getMeasureFields(Observation obs) {
        final List<Field> fields;
        if (obs.getResult() instanceof ComplexResult cr && !cr.getFields().isEmpty()) {
            // remove main field at index O.
            fields = cr.getFields().subList(1, cr.getFields().size());

        } else if (obs.getResult() instanceof MeasureResult mr && mr.getField() != null) {
            fields = Arrays.asList(mr.getField());
        } else {
            throw new IllegalArgumentException("Unxexpected result type in observation");
        }
        return fields;
    }

    public static String getTimeScalePeriod(long millisecond) throws SQLException {
        if (millisecond > Integer.MAX_VALUE) {
            long second = millisecond / 1000;
            if (second > Integer.MAX_VALUE) {
                long minute = second / 60;
                if (minute > Integer.MAX_VALUE) {
                    long hour = minute / 60;
                    if (hour > Integer.MAX_VALUE) {
                        long day = hour / 24;
                        if (day > Integer.MAX_VALUE) {
                            long week = day / 7;
                            if (week > Integer.MAX_VALUE) {
                                // we stop it for now has with month its start to be variable
                                throw new SQLException("Interval is too high to be set has an integer field: " + millisecond + " ms");
                            }
                            return Long.toString(week) + " weeks";
                        }
                        return Long.toString(day) + " days";
                    }
                    return Long.toString(hour) + " hours";
                }
                return Long.toString(minute) + " minutes";
            }
            return Long.toString(second) + " s";
        }
        return Long.toString(millisecond) + " ms";
    }

    /**
     * extract the main field (time or other for profile observation) span and determine a step regarding the width parameter.
     *
     * return a Map with in the keys :
     *  - the procedure id for location retrieval
     *  - the observation id for profiles sensor.
     *  - a fixed value "1" for non profiles sensor (as in time series all observation are merged).
     *
     * and in the values, an array of a fixed size of 2 containing :
     *  - the mnimal value
     *  - the step value
     */
    public static Map<Object, long[]> getMainFieldStep(FilterSQLRequest request, final Connection c, final int width, OMEntity objectType, ProcedureInfo proc) throws SQLException {
        final boolean getLoc  = OMEntity.HISTORICAL_LOCATION.equals(objectType);
        final Field mainField = getLoc ? DEFAULT_TIME_FIELD :  proc.mainField;
        final Boolean profile = getLoc ? null : "profile".equals(proc.type);
        if (getLoc) {
            request.replaceSelect("MIN(\"" + mainField.name + "\") as tmin, MAX(\"" + mainField.name + "\") as tmax, hl.\"procedure\" ");
            request.append(" group by hl.\"procedure\" order by hl.\"procedure\"");

        } else {
            if (profile) {
                request.replaceSelect(" MIN(\"" + mainField.name + "\"), MAX(\"" + mainField.name + "\"), o.\"id\" ");
                request.append(" group by o.\"id\" order by o.\"id\"");
            } else {
                request.replaceSelect(" MIN(\"" + mainField.name + "\"), MAX(\"" + mainField.name + "\") ");
            }
        }
        LOGGER.fine(request.toString());
        try (final SQLResult rs = request.execute(c)) {
            Map<Object, long[]> results = new LinkedHashMap<>();
            while (rs.next()) {
                final long[] result = {-1L, -1L};
                if (FieldType.TIME.equals(mainField.type)) {
                    final Timestamp minT = rs.getTimestamp(1, 0);
                    final Timestamp maxT = rs.getTimestamp(2, 0);
                    if (minT != null && maxT != null) {
                        final long min = minT.getTime();
                        final long max = maxT.getTime();
                        result[0] = min;
                        long step = (max - min) / width;
                        /* step should always be positive
                        if (step <= 0) {
                            step = 1;
                        }*/
                        result[1] = step;
                    }
                } else if (FieldType.QUANTITY.equals(mainField.type)) {
                    final Double minT = rs.getDouble(1, 0);
                    final Double maxT = rs.getDouble(2, 0);
                    final long min    = minT.longValue();
                    final long max    = maxT.longValue();
                    result[0] = min;
                    long step = (max - min) / width;
                    /* step should always be positive
                    if (step <= 0) {
                        step = 1;
                    }*/
                    result[1] = step;

                } else {
                    throw new SQLException("unable to extract bound from a " + mainField.type + " main field.");
                }
                final Object key;
                if (getLoc) {
                    key = rs.getString(3);
                } else {
                    if (profile) {
                        key = rs.getInt(3);
                    } else {
                        key = 1; // single in time series
                    }
                }
                results.put(key, result);
            }
            return results;
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "SQLException while executing the query: {0}", request.toString());
            throw ex;
        }
    }
}
