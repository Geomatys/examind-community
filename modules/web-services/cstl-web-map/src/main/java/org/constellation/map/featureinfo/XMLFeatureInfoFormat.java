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

import java.awt.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.Resource;
import org.apache.sis.util.logging.Logging;
import org.constellation.api.DataType;
import org.constellation.exception.ConstellationStoreException;
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
import org.opengis.geometry.Envelope;
import org.opengis.util.GenericName;

/**
 * A generic FeatureInfoFormat that produce XML output for Features and Coverages.
 * Supported mimeTypes are :
 * <ul>
 *     <li>application/vnd.ogc.xml</li>
 *     <li>text/xml</li>
 * </ul>
 *
 * @author Quentin Boileau (Geomatys)
 */
public class XMLFeatureInfoFormat extends AbstractTextFeatureInfoFormat {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.map.featureinfo");

    private GetFeatureInfo gfi;

    public XMLFeatureInfoFormat() {
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
            if (ref.getIdentifier().isPresent()) {
                fullLayerName = ref.getIdentifier().get().tip();
            } else {
                throw new RuntimeException("resource identifier not present");
            }
        } catch (DataStoreException e) {
            throw new RuntimeException(e);      // TODO
        }
        String layerName = fullLayerName.tip().toString();

        StringBuilder builder = new StringBuilder();
        String margin = "\t";

        builder.append(margin).append("<Coverage>\n");
        margin += "\t";
        builder.append(margin).append("<Layer>").append(encodeXML(layerName)).append("</Layer>\n");

        builder.append(coverageToXML(coverage, results, margin, gfi, getLayersDetails()));

        margin = margin.substring(1);
        builder.append(margin).append("</Coverage>\n");

        if (builder.length() > 0) {
            List<String> strs = coverages.get(layerName);
            if (strs == null) {
                strs = new ArrayList<>();
                coverages.put(layerName, strs);
            }
            strs.add(builder.toString());
        }

    }

    protected static String coverageToXML(final ProjectedCoverage coverage, final List<Map.Entry<SampleDimension,Object>> results,
                                          String margin, final GetFeatureInfo gfi, final List<Data> layerDetailsList) {

        StringBuilder builder = new StringBuilder();
        final Resource ref = coverage.getLayer().getResource();
        final GenericName fullLayerName;
        try {
            if (ref.getIdentifier().isPresent()) {
                fullLayerName = ref.getIdentifier().get().tip();
            } else {
                throw new RuntimeException("resource identifier not present");
            }
        } catch (DataStoreException e) {
            throw new RuntimeException(e);      // TODO
        }

        Data layerPostgrid = null;

        for (Data layer : layerDetailsList) {
            if (layer.getDataType().equals(DataType.COVERAGE) && layer.getName().equals(fullLayerName)) {
                layerPostgrid = layer;
            }
        }

        final Envelope objEnv;
        List<Date> time;
        Double elevation;
        if (gfi != null && gfi instanceof org.geotoolkit.wms.xml.GetFeatureInfo) {
            org.geotoolkit.wms.xml.GetFeatureInfo wmsGFI = (org.geotoolkit.wms.xml.GetFeatureInfo) gfi;
            objEnv = wmsGFI.getEnvelope2D();
            time = wmsGFI.getTime();
            elevation = wmsGFI.getElevation();
        } else {
            objEnv = null;
            time = null;
            elevation = null;
        }

//        if (objEnv != null) {
//            final CoordinateReferenceSystem crs = objEnv.getCoordinateReferenceSystem();
//            final GeneralDirectPosition pos = getPixelCoordinates(gfi);
//            if (pos != null) {
//                builder.append("<gml:boundedBy>").append("\n");
//                String crsName;
//                try {
//                    crsName = IdentifiedObjects.lookupIdentifier(crs, true);
//                } catch (FactoryException ex) {
//                    LOGGER.log(Level.INFO, ex.getLocalizedMessage(), ex);
//                    crsName = crs.getName().getCode();
//                }
//                builder.append("\t\t\t\t<gml:Box srsName=\"").append(crsName).append("\">\n");
//                builder.append("\t\t\t\t\t<gml:coordinates>");
//                builder.append(pos.getOrdinate(0)).append(",").append(pos.getOrdinate(1)).append(" ")
//                        .append(pos.getOrdinate(0)).append(",").append(pos.getOrdinate(1));
//                builder.append("</gml:coordinates>").append("\n");
//                builder.append("\t\t\t\t</gml:Box>").append("\n");
//                builder.append("\t\t\t</gml:boundedBy>").append("\n");
//                builder.append("\t\t\t<x>").append(pos.getOrdinate(0)).append("</x>").append("\n")
//                        .append("\t\t\t<y>").append(pos.getOrdinate(1)).append("</y>").append("\n");
//            }
//        }

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

        if (time != null && !time.isEmpty()) {
            // TODO : Manage periods.
            final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            builder.append(margin).append("<time>").append(encodeXML(df.format(time.get(time.size()-1)))).append("</time>").append("\n");
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

        if (elevation != null) {
            builder.append(margin).append("<elevation>").append(elevation).append("</elevation>").append("\n");
        }

        builder.append(margin).append("<values>").append("\n");
        margin += "\t";
        int index = 0;
        for (final Map.Entry<SampleDimension,Object> entry : results) {
            final Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            String bandName = "band_"+index;
            String unit = entry.getKey().getUnits() != null ? entry.getKey().getUnits().toString() : null;
            if (unit != null) {
                builder.append(margin).append("<").append(encodeXMLMark(bandName)).append(" unit =\"").append(unit).append("\">");
            } else  {
                builder.append(margin).append("<").append(encodeXMLMark(bandName)).append(">");
            }
            builder.append(value)
                   .append("</").append(encodeXMLMark(bandName)).append(">").append("\n");
        }
        margin = margin.substring(1);
        builder.append(margin).append("</values>").append("\n");
//
//        if (!results.isEmpty()) {
//            builder.append("\t\t\t<variable>")
//                    .append(results.get(0).getKey().getDescription())
//                    .append("</variable>").append("\n");
//        }
//
//        MeasurementRange[] ranges = null;
//        if (layerPostgrid != null) {
//            ranges = layerPostgrid.getSampleValueRanges();
//        }
//        if (ranges != null && ranges.length > 0) {
//            final MeasurementRange range = ranges[0];
//            if (range != null) {
//                final Unit unit = range.getUnits();
//                if (unit != null && !unit.toString().isEmpty()) {
//                    builder.append("\t\t\t<unit>").append(unit.toString())
//                            .append("</unit>").append("\n");
//                }
//            }
//        }
//        builder.append("\t\t\t<value>").append(result)
//                .append("</value>").append("\n")
//                .append("\t\t</").append(layerName).append("_feature").append(endMark)
//                .append("\t</").append(layerName).append("_layer").append(endMark);


        return builder.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void nextProjectedFeature(ProjectedFeature graphic, RenderingContext2D context, SearchAreaJ2D queryArea) {

        final StringBuilder builder   = new StringBuilder();
        final FeatureMapLayer layer   = graphic.getLayer();
        final Feature feature         = graphic.getCandidate();
        final FeatureType featureType = feature.getType();
        String margin                 = "\t";

        // feature member  mark
        builder.append(margin).append("<Feature>\n");
        margin += "\t";

        // featureType mark
        if (featureType != null) {
            String ftLocal = featureType.getName().tip().toString();

            builder.append(margin).append("<Layer>").append(encodeXML(layer.getName())).append("</Layer>\n");
            builder.append(margin).append("<Name>").append(encodeXML(ftLocal)).append("</Name>\n");
            builder.append(margin).append("<ID>").append(encodeXML(FeatureExt.getId(feature).getID())).append("</ID>\n");

            complexAttributetoXML(builder, feature, margin);
        } else {
            LOGGER.warning("The feature type is null");
        }

        // end feature member mark
        margin = margin.substring(1);
        builder.append(margin).append("</Feature>\n");

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

    protected static void complexAttributetoXML(final StringBuilder builder, final Feature feature, String margin) {

        for (PropertyType pt : feature.getType().getProperties(true)) {

            if (pt instanceof AttributeType) {
                final AttributeType attType = (AttributeType) pt;
                final GenericName propName = pt.getName();
                String pLocal = propName.tip().toString();
                if (pLocal.startsWith("@")) {
                    pLocal = pLocal.substring(1);
                }
                pLocal = encodeXMLMark(pLocal);

                final Object value = feature.getPropertyValue(propName.toString());

                if (Geometry.class.isAssignableFrom(attType.getValueClass())) {
                    builder.append(margin).append('<').append(pLocal).append(">\n");
                    Geometry geom = (Geometry)value;
                    builder.append(encodeXML(geom.toText()));
                    builder.append(margin).append("</").append(pLocal).append(">\n");
                } else {

                    if (value instanceof Feature) {
                        final Feature complex = (Feature) value;
                        builder.append(margin).append('<').append(pLocal).append(">\n");
                        margin += "\t";

                        complexAttributetoXML(builder, complex, margin);

                        margin = margin.substring(1);
                        builder.append(margin).append("</").append(pLocal).append(">\n");
                    } else {
                        //simple
                        if (value instanceof List) {
                            List valueList = (List) value;
                            if (valueList.isEmpty()) {
                                builder.append(margin).append('<').append(pLocal).append("/>\n");
                            } else {
                                for (Object v : valueList) {
                                    if (v != null) {
                                        final String strValue = encodeXML(value.toString());
                                        builder.append(margin).append('<').append(pLocal).append(">")
                                                .append(strValue)
                                                .append("</").append(pLocal).append(">\n");
                                    } else {
                                        builder.append(margin).append('<').append(pLocal).append("/>\n");
                                    }
                                }
                            }
                        } else if (value != null) {
                            final String strValue = encodeXML(value.toString());
                            builder.append(margin).append('<').append(pLocal).append(">")
                                    .append(strValue)
                                    .append("</").append(pLocal).append(">\n");
                        } else {
                            builder.append(margin).append('<').append(pLocal).append("/>\n");
                        }
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getFeatureInfo(SceneDef sdef, ViewDef vdef, CanvasDef cdef, Rectangle searchArea, GetFeatureInfo getFI) throws PortrayalException {

        this.gfi = getFI;
        final StringBuilder builder = new StringBuilder();

        final String mimeType = getFI.getInfoFormat();

        //fill coverages and features maps
        getCandidates(sdef, vdef, cdef, searchArea, -1);

        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append("\n")
                .append("<FeatureInfo>").append("\n");

        final Map<String, List<String>> values = new HashMap<>();
        values.putAll(features);
        values.putAll(coverages);

        // optimization move this filter to getCandidates
        Integer maxValue = getFeatureCount(getFI);
        if (maxValue == null) {
            maxValue = 1;
        }

        int cpt = 0;
        for (String layerName : values.keySet()) {
            for (final String record : values.get(layerName)) {
                builder.append(record);
                cpt++;
                if (cpt >= maxValue) break;
            }
        }

        builder.append("</FeatureInfo>");

        return builder.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getSupportedMimeTypes() {
        final List<String> mimes = new ArrayList<>();

        //will return map server GML
        mimes.add(MimeType.APP_XML);
        mimes.add(MimeType.TEXT_XML);
        return mimes;
    }
}
