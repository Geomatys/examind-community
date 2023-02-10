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
package org.constellation.json.component;

import java.util.ArrayList;
import java.util.List;
import org.constellation.business.IStyleConverterBusiness;
import org.constellation.exception.ConfigurationException;
import org.constellation.json.binding.CellSymbolizer;
import org.constellation.json.binding.DynamicRangeSymbolizer;
import org.constellation.json.binding.IsolineSymbolizer;
import org.constellation.json.binding.LineSymbolizer;
import org.constellation.json.binding.PieSymbolizer;
import org.constellation.json.binding.PointSymbolizer;
import org.constellation.json.binding.PolygonSymbolizer;
import org.constellation.json.binding.RasterSymbolizer;
import org.constellation.json.binding.Rule;
import org.constellation.json.binding.Style;
import org.constellation.json.binding.TextSymbolizer;
import org.constellation.json.util.StyleUtilities;
import org.geotoolkit.style.MutableFeatureTypeStyle;
import org.geotoolkit.style.MutableRule;
import org.geotoolkit.style.MutableStyle;
import org.opengis.filter.LikeOperator;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 *
 * @author guilhem
 */
@Component("cstlStyleConverterBusiness")
@Primary
public class StyleConverterBusiness implements IStyleConverterBusiness {

    @Override
    public Style getJsonStyle(org.opengis.style.Style style) throws ConfigurationException {
        if (style instanceof MutableStyle mStyle) {
            final Style result = new Style();
            final List<MutableRule> mutableRules = new ArrayList<>(0);
            Boolean multiStyle = false;
            if (!style.featureTypeStyles().isEmpty()) {
                /**
                 * During SIGeoS development we introduced the possibility to have more than one MutableFeatureTypeStyle
                 * per style. So, instead of fetching the rules from the first of the list only, we will iterate on the entire list
                 */
                for (MutableFeatureTypeStyle fts : mStyle.featureTypeStyles()) {
                    mutableRules.addAll(fts.rules());
                }
                if (mStyle.featureTypeStyles().size() > 1) {
                    multiStyle = true;
                }
            }

            result.setName(mStyle.getName());
            result.setMultiStyle(multiStyle);
            for (final MutableRule mutableRule : mutableRules) {
                result.getRules().add(getJsonRule(mutableRule));
            }
            return result;
        } else {
            throw new ConfigurationException(" Supplied style is not a Mutable style.");
        }
    }

    @Override
    public Rule getJsonRule(org.opengis.style.Rule orule) throws ConfigurationException {
        if (orule instanceof MutableRule rule) {
            Rule result = new Rule();
            result.setName(rule.getName());
            if (rule.getDescription() != null) {
                if (rule.getDescription().getTitle() != null) {
                    result.setTitle(rule.getDescription().getTitle().toString());
                }
                if (rule.getDescription().getAbstract() != null) {
                    result.setDescription(rule.getDescription().getAbstract().toString());
                }
            }
            result.setMinScale(rule.getMinScaleDenominator());
            result.setMaxScale(rule.getMaxScaleDenominator());
            for (final org.opengis.style.Symbolizer symbolizer : rule.symbolizers()) {
                if (symbolizer instanceof org.opengis.style.PointSymbolizer ps) {
                    result.getSymbolizers().add(new PointSymbolizer(ps));
                } else if (symbolizer instanceof org.opengis.style.LineSymbolizer ls) {
                    result.getSymbolizers().add(new LineSymbolizer(ls));
                } else if (symbolizer instanceof org.opengis.style.PolygonSymbolizer ps) {
                    result.getSymbolizers().add(new PolygonSymbolizer(ps));
                } else if (symbolizer instanceof org.opengis.style.TextSymbolizer ts) {
                    result.getSymbolizers().add(new TextSymbolizer(ts));
                } else if (symbolizer instanceof org.opengis.style.RasterSymbolizer rs) {
                    result.getSymbolizers().add(new RasterSymbolizer(rs));
                } else if (symbolizer instanceof org.geotoolkit.display2d.ext.cellular.CellSymbolizer cs) {
                    result.getSymbolizers().add(getCellSymbolizer(cs));
                } else if (symbolizer instanceof org.geotoolkit.display2d.ext.pie.PieSymbolizer ps) {
                    result.getSymbolizers().add(new PieSymbolizer(ps));
                } else if (symbolizer instanceof org.geotoolkit.display2d.ext.dynamicrange.DynamicRangeSymbolizer ds){
                    result.getSymbolizers().add(new DynamicRangeSymbolizer(ds));
                } else if (symbolizer instanceof org.geotoolkit.display2d.ext.isoline.symbolizer.IsolineSymbolizer is){
                    result.getSymbolizers().add(new IsolineSymbolizer(is));
                }
            }
            if (rule.getFilter() != null) {
                result.setFilter(StyleUtilities.toCQL(rule.getFilter()));

                //for generated rules auto unique values we need to escape quotes.
                if(result.getFilter().contains("''") && !result.getFilter().endsWith("''") && rule.getFilter() instanceof LikeOperator){
                    result.setFilter(result.getFilter().replaceAll("''","\\\\'"));
                }
            }
            return result;
        } else {
            throw new ConfigurationException(" Supplied rule is not a Mutable rule.");
        }
    }

    protected CellSymbolizer getCellSymbolizer(final org.geotoolkit.display2d.ext.cellular.CellSymbolizer symbolizer) throws ConfigurationException {
        CellSymbolizer result = new CellSymbolizer();

        result.setName(symbolizer.getName());
        result.setCellSize(symbolizer.getCellSize());
        final MutableRule mutableRule = (MutableRule)symbolizer.getRule();
        result.setRule(getJsonRule(mutableRule));
        return result;
    }
}
