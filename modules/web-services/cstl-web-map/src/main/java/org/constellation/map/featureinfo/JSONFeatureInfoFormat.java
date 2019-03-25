/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
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
package org.constellation.map.featureinfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.Resource;
import org.apache.sis.util.logging.Logging;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.map.featureinfo.dto.CoverageInfo;
import org.constellation.provider.Data;
import org.constellation.ws.MimeType;
import org.geotoolkit.display.PortrayalException;
import org.geotoolkit.display2d.canvas.RenderingContext2D;
import org.geotoolkit.display2d.primitive.ProjectedCoverage;
import org.geotoolkit.display2d.primitive.ProjectedFeature;
import org.geotoolkit.display2d.primitive.SearchAreaJ2D;
import org.geotoolkit.display2d.service.CanvasDef;
import org.geotoolkit.display2d.service.SceneDef;
import org.geotoolkit.display2d.service.ViewDef;
import org.geotoolkit.feature.FeatureExt;
import org.geotoolkit.map.FeatureMapLayer;
import org.geotoolkit.ows.xml.GetFeatureInfo;
import org.geotoolkit.util.DateRange;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;
import org.opengis.util.GenericName;
import org.constellation.api.DataType;

import java.awt.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A generic FeatureInfoFormat that produce JSON output for Features and Coverages.
 * Supported mimeTypes are :
 * <ul>
 *     <li>text/html</li>
 * </ul>
 *
 * TODO: Make proper Jackson bindings, do not extend text format which is too constraining.
 *
 * @author Guilhem Legal (Geomatys)
 * @author Johann Sorel (Geomatys)
 */
public class JSONFeatureInfoFormat extends AbstractTextFeatureInfoFormat {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.map.featureinfo");

    private static final ObjectWriter COVERAGE_WRITER = new ObjectMapper()
            .disable(SerializationFeature.INDENT_OUTPUT)
            .writerFor(CoverageInfo.class);

    private GetFeatureInfo gfi;

    public JSONFeatureInfoFormat() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void nextProjectedCoverage(ProjectedCoverage coverage, RenderingContext2D context, SearchAreaJ2D queryArea) {
        final List<Map.Entry<SampleDimension,Object>> results =
                FeatureInfoUtilities.getCoverageValues(coverage, context, queryArea);

        if (results == null) {
            return;
        }

        final Resource ref = coverage.getLayer().getResource();
        final GenericName fullLayerName;
        try {
            fullLayerName = ref.getIdentifier();
        } catch (DataStoreException e) {
            throw new RuntimeException(e);      // TODO
        }
        String layerName = fullLayerName.tip().toString();
        // TODO: Jackson binding
        StringBuilder builder = new StringBuilder();

        builder.append(coverageToJSON(fullLayerName, results, gfi, getLayersDetails()));

        if (builder.length() > 0) {
            List<String> strs = coverages.get(layerName);
            if (strs == null) {
                strs = new ArrayList<>();
                coverages.put(layerName, strs);
            }
            strs.add(builder.toString());
        }

    }

    protected static String coverageToJSON(final GenericName fullLayerName,
            final List<Map.Entry<SampleDimension,Object>> results, final GetFeatureInfo gfi, final List<Data> layerDetailsList) {

        Data layerPostgrid = null;

        for (Data layer : layerDetailsList) {
            if (layer.getDataType().equals(DataType.COVERAGE) && layer.getName().equals(fullLayerName)) {
                layerPostgrid = layer;
            }
        }

        List<Date> time;
        Double elevation;
        if (gfi != null && gfi instanceof org.geotoolkit.wms.xml.GetFeatureInfo) {
            org.geotoolkit.wms.xml.GetFeatureInfo wmsGFI = (org.geotoolkit.wms.xml.GetFeatureInfo) gfi;
            time = wmsGFI.getTime();
            elevation = wmsGFI.getElevation();
        } else {
            time = null;
            elevation = null;
        }

        if (time == null) {
            /*
             * Get the date of the last slice in this layer. Don't invoke
             * layerPostgrid.getAvailableTimes().last() because getAvailableTimes() is very
             * costly. The layerPostgrid.getEnvelope() method is much cheaper, since it can
             * leverage the database index.
             */
            DateRange dates = null;
            if (layerPostgrid != null) {
                try {
                    dates = layerPostgrid.getDateRange();
                } catch (ConstellationStoreException ex) {
                    LOGGER.log(Level.INFO, ex.getLocalizedMessage(), ex);
                }
            }
            if (dates != null && !(dates.isEmpty())) {
                if (dates.getMaxValue() != null) {
                    time = Collections.singletonList(dates.getMaxValue());
                }
            }
        }

        if (elevation == null) {
            SortedSet<Number> elevs = null;
            if (layerPostgrid != null) {
                try {
                    elevs = layerPostgrid.getAvailableElevations();
                } catch (ConstellationStoreException ex) {
                    LOGGER.log(Level.INFO, ex.getLocalizedMessage(), ex);
                    elevs = null;
                }
            }
            if (elevs != null && !(elevs.isEmpty())) {
                elevation = elevs.first().doubleValue();
            }
        }

        final Instant javaTime = time == null || time.isEmpty() ? null : time.get(time.size() - 1).toInstant();
        final CoverageInfo info = new CoverageInfo(fullLayerName.toString(), javaTime, elevation);

        final List<CoverageInfo.Sample> outValues = info.getValues();
        for (final Map.Entry<SampleDimension,Object> entry : results) {
            final Object value = entry.getValue();
            if (value instanceof Number) {
                final Number nValue = (Number) value;
                if (Double.isFinite(nValue.doubleValue())) {

                    final SampleDimension sd = entry.getKey();
                    outValues.add(new CoverageInfo.Sample(
                            sd.getName().tip().toString(),
                            nValue,
                            sd.getUnits().orElse(null)
                    ));
                } else {
                    // Could no-data reach this point ?
                    LOGGER.log(Level.FINE, "Ignoring non finite value");
                }
            } else {
                LOGGER.log(Level.FINE, "Ignoring unsupported data type: {0}", value == null? "null" : value.getClass().getCanonicalName());
            }
        }

        if (outValues.isEmpty()) {
            return "";
        }

        try {
            return COVERAGE_WRITER.writeValueAsString(info);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write JSON for GetFeatureInfo of coverage: "+fullLayerName, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void nextProjectedFeature(ProjectedFeature graphic, RenderingContext2D context, SearchAreaJ2D queryArea) {

        ///           TODO
        final StringBuilder builder   = new StringBuilder();
        final FeatureMapLayer layer   = graphic.getLayer();
        final Feature feature         = graphic.getCandidate();
        final FeatureType featureType = feature.getType();

        // feature member  mark
        builder.append("{");

        // featureType mark
        if (featureType != null) {
            String ftLocal = featureType.getName().tip().toString();

            builder.append("\"Layer\":\"").append(layer.getName()).append("\",");
            builder.append("\"Name\":\"").append(ftLocal).append("\",");
            builder.append("\"ID\":\"").append(FeatureExt.getId(feature).getID()).append("\",");

            builder.append("\"feature\":");
            complexAttributetoJSON(builder, feature);
            builder.append("}");
        } else {
            LOGGER.warning("The feature type is null");
        }

        final String result = builder.toString();
        if (builder.length() > 0) {
            final String layerName = layer.getName();
            List<String> strs = features.get(layerName);
            if (strs == null) {
                strs = new ArrayList<>();
                features.put(layerName, strs);
            }
            strs.add(result.substring(0, result.length()));
        }
    }

    protected static void complexAttributetoJSON(final StringBuilder builder, final Feature feature) {
        builder.append('{');
        Collection<? extends PropertyType> attributes = feature.getType().getProperties(true);
        for (PropertyType pt : attributes) {

            if (pt instanceof AttributeType) {
                final AttributeType attType = (AttributeType) pt;
                final GenericName propName = pt.getName();
                String pLocal = propName.tip().toString();
                if (pLocal.startsWith("@")) {
                    pLocal = pLocal.substring(1);
                }

                final Object value = feature.getPropertyValue(propName.toString());

                if (Geometry.class.isAssignableFrom(attType.getValueClass())) {
                    builder.append('"').append(pLocal).append("\":");
                    Geometry geom = (Geometry)value;
                    builder.append('"').append(geom.toText()).append("\",");
                } else {

                    if (value instanceof Feature) {
                        final Feature complex = (Feature) value;
                        builder.append('"').append(pLocal).append("\":");
                        complexAttributetoJSON(builder, complex);
                        builder.append(",");

                    } else {
                        //simple
                        if (value instanceof List) {
                            builder.append('"').append(pLocal).append("\":[");

                            List valueList = (List) value;
                            for (Object v : valueList) {
                                if (v != null) {
                                    final String strValue = value.toString();
                                    builder.append(strValue).append(",");
                                }
                            }
                            if (!valueList.isEmpty()) {
                                builder.deleteCharAt(builder.length() - 1);
                            }
                            builder.append("],");

                        } else if (value != null) {
                            final String strValue = value.toString();
                            builder.append('"').append(pLocal).append("\":\"")
                                   .append(strValue)
                                   .append("\",");
                        }
                    }
                }
            }
        }
        if (!attributes.isEmpty()) {
            builder.deleteCharAt(builder.length() - 1);
        }
        builder.append('}');
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getFeatureInfo(SceneDef sdef, ViewDef vdef, CanvasDef cdef, Rectangle searchArea, GetFeatureInfo getFI) throws PortrayalException {

        this.gfi = getFI;

        //fill coverages and features maps
        getCandidates(sdef, vdef, cdef, searchArea, -1);

        // optimization move this filter to getCandidates
        Integer maxValue = getFeatureCount(getFI);
        if (maxValue == null) {
            maxValue = 1;
        }

        // TODO: Proper Jackson binding. Here, I don't think we're making a proper JSON array.

        final StringBuilder builder = new StringBuilder();

        final Map<String, List<String>> values = new HashMap<>();
        values.putAll(features);
        values.putAll(coverages);

        int cpt = 0;
        String comma = "";
        for (String layerName : values.keySet()) {
            for (final String record : values.get(layerName)) {
                builder.append(comma).append(record);
                comma = ",";
                cpt++;
                if (cpt >= maxValue) break;
            }
        }

        String result =  builder.toString();
        if (cpt > 1) {
            // add brackets
            result = "[" + result + "]";
        }
        return result;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getSupportedMimeTypes() {
        final List<String> mimes = new ArrayList<>();

        //will return map server GML
        mimes.add(MimeType.APP_JSON);
        mimes.add(MimeType.APP_JSON_UTF8);
        return mimes;
    }
}
