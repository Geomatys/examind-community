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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.measure.Unit;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.display2d.canvas.RenderingContext2D;
import org.geotoolkit.display2d.primitive.ProjectedCoverage;
import org.geotoolkit.display2d.primitive.ProjectedFeature;
import org.geotoolkit.display2d.primitive.SearchAreaJ2D;
import org.geotoolkit.map.FeatureMapLayer;
import org.geotoolkit.storage.coverage.CoverageResource;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Feature;
import org.opengis.feature.PropertyNotFoundException;
import org.opengis.feature.PropertyType;

/**
 * @author Quentin Boileau (Geomatys)
 */
public abstract class AbstractTextFeatureInfoFormat extends AbstractFeatureInfoFormat {

    /**
     * Contains the values for all coverage layers requested.
     */
    protected final Map<String, List<String>> coverages = new HashMap<>();


    protected final Map<String, List<String>> features = new HashMap<>();

    /**
     * {@inheritDoc}
     */
    @Override
    protected void nextProjectedCoverage(ProjectedCoverage graphic, RenderingContext2D context, SearchAreaJ2D queryArea) {
        final List<Map.Entry<SampleDimension,Object>> results =
                FeatureInfoUtilities.getCoverageValues(graphic, context, queryArea);

        if (results == null) {
            return;
        }
        final CoverageResource ref = graphic.getLayer().getCoverageReference();
        final String layerName;
        try {
            layerName = ref.getIdentifier().tip().toString();
        } catch (DataStoreException e) {
            throw new RuntimeException(e);      // TODO
        }
        List<String> strs = coverages.get(layerName);
        if (strs == null) {
            strs = new ArrayList<>();
            coverages.put(layerName, strs);
        }

        final StringBuilder builder = new StringBuilder();
        for (final Map.Entry<SampleDimension,Object> entry : results) {
            final Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            builder.append(value);
            final Unit unit = entry.getKey().getUnits().orElse(null);
            if (unit != null) {
                builder.append(" ").append(unit.toString());
            }
        }

        final String result = builder.toString();
        strs.add(result.substring(0, result.length() - 2));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void nextProjectedFeature(ProjectedFeature graphic, RenderingContext2D context, SearchAreaJ2D queryArea) {
        final StringBuilder builder = new StringBuilder();
        final FeatureMapLayer layer = graphic.getLayer();
        final Feature feature = graphic.getCandidate();

        for (PropertyType pt : feature.getType().getProperties(true)) {
            try {
                final Object value = feature.getPropertyValue(pt.getName().toString());
                if (value instanceof Geometry) {
                    builder.append(pt.getName().tip().toString()).append(':').append(value.getClass().getSimpleName()).append(';');
                } else {
                    builder.append(pt.getName().tip().toString()).append(':').append(value).append(';');
                }
            } catch (PropertyNotFoundException ex) {
                //do nothing
            }
        }

        final String result = builder.toString();
        if (builder.length() > 0 && result.endsWith(";")) {
            final String layerName = layer.getName();
            List<String> strs = features.get(layerName);
            if (strs == null) {
                strs = new ArrayList<>();
                features.put(layerName, strs);
            }
            strs.add(result.substring(0, result.length() - 1));
        }
    }

    protected static String encodeXMLMark(String str) {
        if (str != null && !str.isEmpty()) {
            str = str.trim();
            // XML mark does not support no space or %20
            str = str.replace("%20", "_");
            str = str.replace(" ", "_");
            return encodeXML(str);
        }
        return str;
    }
    /**
     * Escapes the characters in a String.
     *
     * @param str
     * @return String
     */
    protected static String encodeXML(String str) {
        if (str != null && !str.isEmpty()) {
            str = str.trim();
            final StringBuffer buf = new StringBuffer(str.length() * 2);
            int i;
            for (i = 0; i < str.length(); ++i) {
                char ch = str.charAt(i);
                int intValue = (int)ch;
                String entityName = null;

                switch (intValue) {
                    case 34 : entityName = "quot"; break;
                    case 39 : entityName = "apos"; break;
                    case 38 : entityName = "amp"; break;
                    case 60 : entityName = "lt"; break;
                    case 62 : entityName = "gt"; break;
                    case 65533 : ch = '_'; break; // fallback value when the character has not been correctly interpretted.
                }

                if (entityName == null) {
                    if (ch > 0x7F) {
                        buf.append("&#");
                        buf.append(intValue);
                        buf.append(';');
                    } else {
                        buf.append(ch);
                    }
                } else {
                    buf.append('&');
                    buf.append(entityName);
                    buf.append(';');
                }
            }
            return buf.toString();
        }
        return str;
    }
}
