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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.DataStoreException;
import org.constellation.util.FilterSQLRequest;
import org.geotoolkit.geometry.jts.SRIDGenerator;
import org.geotoolkit.observation.OMUtils;
import org.geotoolkit.observation.model.ComplexResult;
import org.geotoolkit.observation.model.CompositePhenomenon;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.FieldType;
import org.geotoolkit.observation.model.MeasureResult;
import org.geotoolkit.observation.model.Observation;
import org.geotoolkit.observation.model.Phenomenon;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBWriter;
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

    public static byte[] getGeometryBytes(Geometry pt) {
        final WKBWriter writer = new WKBWriter();
        return writer.write(pt);
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

    /**
     * TODO remove when corrected in geotk.
     */
    public static List<Field> getPhenomenonsFields(final Phenomenon phen) {
        final List<Field> results = new ArrayList<>();
         if (phen instanceof CompositePhenomenon comp) {

            for (int i = 0; i < comp.getComponent().size(); i++) {
                Phenomenon component = comp.getComponent().get(i);
                results.add(new Field(i + 2, FieldType.QUANTITY, component.getId(), component.getName(), component.getDefinition(), null));
            }
        } else if (phen != null) {
            results.add(new Field(2, FieldType.QUANTITY, phen.getId(), phen.getName(), phen.getDefinition(), null));
        }
        return results;
    }
}
