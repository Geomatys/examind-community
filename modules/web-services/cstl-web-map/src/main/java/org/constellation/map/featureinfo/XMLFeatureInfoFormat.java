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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.measure.Unit;
import javax.xml.namespace.QName;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.storage.GridCoverageResource;
import org.constellation.api.DataType;
import org.constellation.map.featureinfo.FeatureInfoUtilities.Sample;
import org.constellation.ws.LayerCache;
import org.constellation.ws.MimeType;
import org.geotoolkit.display.PortrayalException;
import org.geotoolkit.display2d.canvas.RenderingContext2D;
import org.geotoolkit.display2d.primitive.SearchAreaJ2D;
import org.geotoolkit.display2d.service.CanvasDef;
import org.geotoolkit.display2d.service.SceneDef;
import org.geotoolkit.feature.FeatureExt;
import static org.constellation.metadata.utils.Utils.encodeXML;
import static org.constellation.metadata.utils.Utils.encodeXMLMark;
import org.geotoolkit.ows.xml.GetFeatureInfo;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureAssociationRole;
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

    private GetFeatureInfo gfi;

    public XMLFeatureInfoFormat() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void nextProjectedCoverage(QName layerName, final GridCoverageResource resource, RenderingContext2D context, SearchAreaJ2D queryArea) {
        final List<Sample> results = FeatureInfoUtilities.getCoverageValues(resource, context, queryArea);

        if (results == null) {
            return;
        }

        String layerNameStr = layerName.getLocalPart();

        StringBuilder builder = new StringBuilder();
        String margin = "\t";

        builder.append(margin).append("<Coverage>\n");
        margin += "\t";
        builder.append(margin).append("<Layer>").append(encodeXML(layerNameStr)).append("</Layer>\n");

        builder.append(coverageToXML(results, margin, gfi, getLayer(layerName, DataType.COVERAGE)));

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

    protected String coverageToXML(final List<Sample> results, String margin, final GetFeatureInfo gfi, final Optional<LayerCache> layerO) {

        StringBuilder builder = new StringBuilder();
        LayerCache layer = layerO.orElse(null);

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

        if (time == null || time.isEmpty()) {
            final Date lastTime = searchLastTime(layer);
            if (lastTime != null) time = Collections.singletonList(lastTime);
        }

        if (elevation == null) elevation = searchElevation(layer);

        if (time != null && !time.isEmpty()) {
            synchronized (DATE_FORMAT) {
                builder.append(margin).append("<time>").append(encodeXML(DATE_FORMAT.format(time.get(time.size()-1)))).append("</time>").append("\n");
            }
        }

        if (elevation != null) {
            builder.append(margin).append("<elevation>").append(elevation).append("</elevation>").append("\n");
        }

        builder.append(margin).append("<values>").append("\n");
        margin += "\t";
        int index = 0;
        for (final Sample entry : results) {
            final SampleDimension dim = entry.description();
            String bandName;
            if (dim.getName() != null) {
                String name = dim.getName().toString();
                // we don't want the dimension name to be a simple number
                try {
                     double d = Double.parseDouble(name);
                     bandName = "band_" + name;
                } catch (NumberFormatException ex) {
                    bandName = name;
                }
            } else {
                bandName = "band_" + index;
            }

            final Unit unit = dim.getUnits().orElse(null);
            if (unit != null) {
                builder.append(margin).append("<").append(encodeXMLMark(bandName)).append(" unit =\"").append(unit.toString()).append("\">");
            } else  {
                builder.append(margin).append("<").append(encodeXMLMark(bandName)).append(">");
            }
            builder.append(entry.value()).append("</").append(encodeXMLMark(bandName)).append(">").append("\n");
            index++;
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
    protected void nextProjectedFeature(QName layerName, final Feature feature, RenderingContext2D context, SearchAreaJ2D queryArea) {

        final StringBuilder builder   = new StringBuilder();
        String margin                 = "\t";

        // feature member  mark
        builder.append(margin).append("<Feature>\n");
        margin += "\t";

        // featureType mark
        if (layerName != null) {
            String ftLocal = layerName.getLocalPart();

            builder.append(margin).append("<Layer>").append(encodeXML(ftLocal)).append("</Layer>\n");
            builder.append(margin).append("<Name>").append(encodeXML(ftLocal)).append("</Name>\n");
            builder.append(margin).append("<ID>").append(encodeXML(FeatureExt.getId(feature).getIdentifier())).append("</ID>\n");

            complexAttributetoXML(builder, feature, margin);
        } else {
            LOGGER.warning("The feature type is null");
        }

        // end feature member mark
        margin = margin.substring(1);
        builder.append(margin).append("</Feature>\n");

        final String result = builder.toString();
        if (builder.length() > 0) {
            List<String> strs = features.get(layerName);
            if (strs == null) {
                strs = new ArrayList<>();
                features.put(layerName, strs);
            }
            strs.add(result);
        }
    }

    protected static void complexAttributetoXML(final StringBuilder builder, final Feature feature, String margin) {

        for (PropertyType pt : feature.getType().getProperties(true)) {

            final GenericName propName = pt.getName();
            String pLocal = propName.tip().toString();
            if (pLocal.startsWith("@")) {
                pLocal = pLocal.substring(1);
            }
            pLocal = encodeXMLMark(pLocal);

            final Object value = feature.getPropertyValue(propName.toString());

            if (pt instanceof FeatureAssociationRole) {
                Collection values;
                if (!(value instanceof Collection))  {
                    values = Arrays.asList(value);
                } else {
                    values = (Collection) value;
                }

                for (Object v : values) {
                    if (v instanceof Feature) {
                        builder.append(margin).append('<').append(pLocal).append(">\n");
                        margin += "\t";
                        final Feature complex = (Feature) v;
                        complexAttributetoXML(builder, complex, margin);
                        margin = margin.substring(1);
                        builder.append(margin).append("</").append(pLocal).append(">\n");
                    }
                }
            } else if (pt instanceof AttributeType) {
                final AttributeType attType = (AttributeType) pt;
                
                if (Geometry.class.isAssignableFrom(attType.getValueClass())) {
                    builder.append(margin).append('<').append(pLocal).append(">\n");
                    Geometry geom = (Geometry)value;
                    builder.append(encodeXML(geom.toText()));
                    builder.append("\n").append(margin).append("</").append(pLocal).append(">\n");
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
    public Object getFeatureInfo(SceneDef sdef, CanvasDef cdef, Rectangle searchArea, GetFeatureInfo getFI) throws PortrayalException {

        this.gfi = getFI;
        final StringBuilder builder = new StringBuilder();

        final String mimeType = getFI.getInfoFormat();

        //fill coverages and features maps
        getCandidates(sdef, cdef, searchArea, -1);

        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append("\n")
                .append("<FeatureInfo>").append("\n");

        final Map<QName, List<String>> values = new HashMap<>();
        values.putAll(features);
        values.putAll(coverages);

        // optimization move this filter to getCandidates
        Integer maxValue = getFeatureCount(getFI);
        if (maxValue == null) {
            maxValue = 1;
        }

        int cpt = 0;
        for (QName layerName : values.keySet()) {
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
