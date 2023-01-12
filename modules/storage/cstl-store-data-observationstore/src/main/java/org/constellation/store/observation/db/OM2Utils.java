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

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.apache.sis.measure.Units;
import org.apache.sis.metadata.iso.quality.DefaultQuantitativeAttributeAccuracy;
import org.apache.sis.metadata.iso.quality.DefaultQuantitativeResult;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.util.iso.DefaultRecord;
import org.apache.sis.util.iso.DefaultRecordSchema;
import static org.constellation.store.observation.db.OM2BaseReader.LOGGER;
import org.constellation.util.FilterSQLRequest;
import org.geotoolkit.geometry.jts.transform.AbstractGeometryTransformer;
import org.geotoolkit.geometry.jts.transform.GeometryCSTransformer;
import org.geotoolkit.gml.JTStoGeometry;
import org.geotoolkit.gml.xml.Envelope;
import org.geotoolkit.gml.xml.FeatureProperty;
import org.geotoolkit.observation.result.ResultBuilder;
import org.geotoolkit.observation.model.Field;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildDataArrayProperty;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildFeatureProperty;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildSamplingCurve;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildSamplingFeature;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildSamplingPoint;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildSamplingPolygon;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildSimpleDatarecord;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildTimeInstant;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildTimePeriod;
import static org.geotoolkit.sos.xml.SOSXmlFactory.getGMLVersion;
import org.geotoolkit.swe.xml.AbstractDataComponent;
import org.geotoolkit.swe.xml.AbstractDataRecord;
import org.geotoolkit.swe.xml.AnyScalar;
import org.geotoolkit.swe.xml.DataArray;
import org.geotoolkit.swe.xml.DataArrayProperty;
import org.geotoolkit.swe.xml.DataComponentProperty;
import org.geotoolkit.swe.xml.DataRecord;
import org.geotoolkit.swe.xml.PhenomenonProperty;
import org.geotoolkit.swe.xml.SimpleDataRecord;
import org.geotoolkit.swe.xml.TextBlock;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKBWriter;
import org.opengis.metadata.quality.Element;
import org.opengis.observation.Measure;
import org.opengis.observation.sampling.SamplingFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.opengis.temporal.TemporalGeometricPrimitive;
import org.opengis.temporal.TemporalObject;
import org.opengis.util.FactoryException;
import org.opengis.util.RecordType;

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

    public static void addtimeDuringSQLFilter(FilterSQLRequest sqlRequest, TemporalObject time) {
        if (time instanceof Period tp) {
            final Timestamp begin = new Timestamp(tp.getBeginning().getDate().getTime());
            final Timestamp end   = new Timestamp(tp.getEnding().getDate().getTime());

            // 1.1 the multiple observations included in the period
            sqlRequest.append(" (\"time_begin\">=").appendValue(begin).append(" AND \"time_end\"<=").appendValue(end).append(")");
            sqlRequest.append("OR");
            // 1.2 the single observations included in the period
            sqlRequest.append(" (\"time_begin\">=").appendValue(begin).append(" AND \"time_begin\"<=").appendValue(end).append(" AND \"time_end\" IS NULL)");
            sqlRequest.append("OR");
            // 2. the multiple observations which overlaps the first bound
            sqlRequest.append(" (\"time_begin\"<=").appendValue(begin).append(" AND \"time_end\"<=").appendValue(end).append(" AND \"time_end\">=").appendValue(begin).append(")");
            sqlRequest.append("OR");
            // 3. the multiple observations which overlaps the second bound
            sqlRequest.append(" (\"time_begin\">=").appendValue(begin).append(" AND \"time_end\">=").appendValue(end).append(" AND \"time_begin\"<=").appendValue(end).append(")");
            sqlRequest.append("OR");
            // 4. the multiple observations which overlaps the whole period
            sqlRequest.append(" (\"time_begin\"<=").appendValue(begin).append(" AND \"time_end\">=").appendValue(end).append(")");
        
        } else if (time instanceof Instant inst) {
            final Timestamp instTime = new Timestamp(inst.getDate().getTime());

            sqlRequest.append(" (\"time_begin\"<=").appendValue(instTime).append(" AND \"time_end\">=").appendValue(instTime).append(")");
            sqlRequest.append("OR");
            sqlRequest.append(" (\"time_begin\"=").appendValue(instTime).append(" AND \"time_end\" IS NULL)");
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

    public static String getPhenomenonId(final PhenomenonProperty phenomenonP) {
        if (phenomenonP.getHref() != null) {
            return phenomenonP.getHref();
        }
        if (phenomenonP.getPhenomenon() != null) {
            org.geotoolkit.swe.xml.Phenomenon phen = phenomenonP.getPhenomenon();
            String id = phen.getId();
            if (id != null) {
                return id;
            }
            if (phen.getName() != null) {
                return phen.getName().getCode();
            }
        }
        return null;
    }

    public static byte[] getGeometryBytes(Geometry pt) throws DataStoreException {
        try {
            final WKBWriter writer = new WKBWriter();
            GeometryCSTransformer ts = new GeometryCSTransformer(new AbstractGeometryTransformer() {
                @Override
                public CoordinateSequence transform(CoordinateSequence cs, int i) throws TransformException {
                    for (int j = 0; j < cs.size(); j++) {
                        double x = cs.getX(j);
                        double y = cs.getY(j);
                        cs.setOrdinate(j, 0, y);
                        cs.setOrdinate(j, 1, x);
                    }
                    return cs;
                }
            });

            int srid = pt.getSRID();
            if (srid == 0) {
                srid = 4326;
            }
            if (srid == 4326) {
                pt = ts.transform(pt);
            }
            return writer.write(pt);
        } catch (TransformException ex) {
            throw new DataStoreException(ex);
        }
    }

    public static List<Field> getFieldList(AbstractDataRecord abstractRecord) throws SQLException {
        final List<Field> fields = new ArrayList<>();
        final Collection recordField;
        if (abstractRecord instanceof DataRecord record) {
            recordField =  record.getField();
        } else if (abstractRecord instanceof SimpleDataRecord record) {
            recordField =  record.getField();
        } else {
            throw new IllegalArgumentException("Unexpected record type: " + abstractRecord);
        }

        int i = 1;
        for (Object field : recordField) {
            String name;
            AbstractDataComponent value;
            if (field instanceof AnyScalar scal) {
                name  = scal.getName();
                value = scal.getValue();
            } else if (field instanceof DataComponentProperty compo) {
                name  = compo.getName();
                value = compo.getValue();
            } else if (field != null) {
                throw new SQLException("Unexpected field type:" + field.getClass());
            } else {
                throw new SQLException("Unexpected null field");
            }
            fields.add(new Field(i, name, null, value));
            i++;
        }
        return fields;
    }

    public static DataArrayProperty buildComplexResult(final String version, final Collection<AnyScalar> fields, final int nbValue,
            final TextBlock encoding, final ResultBuilder values, final int cpt) {
        final String arrayID     = "dataArray-" + cpt;
        final String recordID    = "datarecord-" + cpt;
        final AbstractDataRecord record = buildSimpleDatarecord(version, null, recordID, null, null, new ArrayList<>(fields));
        String stringValues = null;
        List<Object> dataValues = null;
        if (values != null) {
            stringValues = values.getStringValues();
            dataValues   = values.getDataArray();
        }
        return buildDataArrayProperty(version, arrayID, nbValue, arrayID, record, encoding, stringValues, dataValues);
    }

    public static TemporalGeometricPrimitive buildTime(String version, String timeID, Date startTime, Date endTime) {
        if (startTime == null && endTime == null) return null;
        else if (startTime == null)               return buildTimeInstant(version, timeID, endTime);
        else if (endTime == null)                 return buildTimeInstant(version, timeID, startTime);
        else if (startTime.equals(endTime))       return buildTimeInstant(version, timeID, startTime);
        else                                      return buildTimePeriod(version, timeID, startTime, endTime);
    }

    public static SamplingFeature buildFoi(final String version, final String id, final String name, final String description, final String sampledFeature,
            final Geometry geom, final CoordinateReferenceSystem crs) throws FactoryException {

        final String gmlVersion = getGMLVersion(version);
        // sampled feature is mandatory (even if its null, we build a property)
        final FeatureProperty prop = buildFeatureProperty(version, sampledFeature);
        if (geom instanceof Point pt) {
            final org.geotoolkit.gml.xml.Point point = JTStoGeometry.toGML(gmlVersion, pt, crs);
            // little hack fo unit test
            //point.setSrsName(null);
            point.setId("pt-" + id);
            return buildSamplingPoint(version, id, name, description, prop, point);
        } else if (geom instanceof LineString ls) {
            final org.geotoolkit.gml.xml.LineString line = JTStoGeometry.toGML(gmlVersion, ls, crs);
            line.emptySrsNameOnChild();
            line.setId("line-" + id);
            final Envelope bound = line.getBounds();
            return buildSamplingCurve(version, id, name, description, prop, line, null, null, bound);
        } else if (geom instanceof Polygon p) {
            final org.geotoolkit.gml.xml.Polygon poly = JTStoGeometry.toGML(gmlVersion, p, crs);
            poly.setId("polygon-" + id);
            return buildSamplingPolygon(version, id, name, description, prop, poly, null, null, null);
        } else if (geom != null) {
            LOGGER.log(Level.WARNING, "Unexpected geometry type:{0}", geom.getClass());
        }
        return buildSamplingFeature(version, id, name, description, prop);
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

    public static Element createQualityElement(Field field, Object value) {
        DefaultQuantitativeAttributeAccuracy element = new DefaultQuantitativeAttributeAccuracy();
        element.setNamesOfMeasure(Arrays.asList(new SimpleInternationalString(field.name)));
        if (value != null) {
            DefaultQuantitativeResult res      = new DefaultQuantitativeResult();
            DefaultRecordSchema schema         = new DefaultRecordSchema(null, null, "MySchema");
            Map<CharSequence,Class<?>> fieldss = new LinkedHashMap<>();
            fieldss.put("value",    field.type.getJavaType());
            RecordType rt = schema.createRecordType("MyRecordType", fieldss);

            DefaultRecord r = new DefaultRecord(rt);
            r.set(rt.getMembers().iterator().next(), value);
            res.setValues(Arrays.asList(r));
            if (field.uom != null) {
                res.setValueUnit(Units.valueOf(field.uom));
            }
            element.setResults(Arrays.asList(res));
        }
        return element;
    }
}
