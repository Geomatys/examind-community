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
package org.constellation.webservice.map.component;

import java.awt.Dimension;
import java.awt.RenderingHints;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.xml.bind.JAXBException;
import org.apache.sis.cql.CQL;
import org.apache.sis.cql.CQLException;

import org.opengis.style.Style;
import org.opengis.util.FactoryException;

import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CRS;

import org.geotoolkit.display.canvas.control.NeverFailMonitor;
import org.geotoolkit.display2d.service.CanvasDef;
import org.geotoolkit.display2d.service.OutputDef;
import org.geotoolkit.display2d.service.SceneDef;
import org.geotoolkit.factory.Hints;
import org.geotoolkit.map.MapBuilder;
import org.apache.sis.portrayal.MapLayers;
import org.apache.sis.portrayal.MapItem;
import org.apache.sis.portrayal.MapLayer;
import org.apache.sis.storage.FeatureQuery;
import org.geotoolkit.sld.xml.Specification;
import org.geotoolkit.sld.xml.StyleXmlIO;
import org.geotoolkit.style.MutableStyle;

import org.constellation.business.IDataBusiness;
import org.constellation.business.IStyleBusiness;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.exception.TargetNotFoundException;
import org.constellation.portrayal.PortrayalResponse;
import org.constellation.provider.Data;
import org.constellation.provider.DataProviders;
import org.constellation.ws.CstlServiceException;
import org.springframework.stereotype.Component;

import static org.apache.sis.util.ArgumentChecks.ensureDimensionMatches;
import static org.apache.sis.util.ArgumentChecks.ensureExpectedCount;
import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import org.constellation.business.IMapBusiness;
import org.constellation.ws.LayerCache;
import org.geotoolkit.util.StringUtilities;
import org.opengis.filter.Filter;
import org.opengis.geometry.Envelope;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component("exaMapBusiness")
public class MapBusiness implements IMapBusiness {

    /**
     * Default rendering options.
     */
    private static final NeverFailMonitor DEFAULT_MONITOR = new NeverFailMonitor();
    private static final Hints DEFAULT_HINTS = new Hints(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON,
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BILINEAR);

    @Inject
    private IStyleBusiness styleBusiness;


    @Inject
    private IDataBusiness dataBusiness;

    /**
     * {@inheritDoc }
     */
    @Override
    public PortrayalResponse portray(final Integer dataId, final String crsCode,
                                     final String bbox, final int width, final int height,
                                     final String sldProvider, final String styleName, final String filter)
                                     throws ConstellationException {
        Style style = null;
        if (sldProvider != null && styleName != null) {
            style = styleBusiness.getStyle(sldProvider, styleName);
            if (style == null){
                throw new CstlServiceException("a problem occurred while retrieving the style from the database, styleid : "+styleName+" on provider : "+sldProvider);
            }
        }
    	return portray(dataId, crsCode, bbox, width, height, style, filter);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PortrayalResponse portray(final Integer dataId, final String crsCode,
                                     final String bbox, final int width, final int height,
                                     final Integer styleId, final String filter)
                                     throws ConstellationException {
        Style style = null;
        if (styleId != null) {
            style = styleBusiness.getStyle(styleId);
            if (style == null){
                throw new CstlServiceException("a problem occurred while retrieving the style from the database, styleid : "+styleId);
            }
        }
    	return portray(dataId, crsCode, bbox, width, height, style, filter);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PortrayalResponse portraySLD(final Integer dataId, final String crsCode,
                                      final String bbox, final int width, final int height, final String sldBody,
                                      final String sldVersion, final String filter) throws ConstellationException {
        final MutableStyle style;
        try {
            // Style.
            if (sldBody != null) {
                // Use specified style.
                Specification.SymbologyEncoding version;
                if ("1.1.0".equals(sldVersion)) {
                    version = Specification.SymbologyEncoding.V_1_1_0;
                } else {
                    version = Specification.SymbologyEncoding.SLD_1_0_0;
                }
                final StringReader reader = new StringReader(sldBody);
                style = new StyleXmlIO().readStyle(reader, version);
            } else {
                //let portrayal process to apply its own style
                style= null;
            }
        } catch (FactoryException | JAXBException ex) {
            throw new CstlServiceException(String.format(
                    "Rendering failed for parameters:%nData: %d%nBbox: %s%nWidth: %d%nHeight: %d%nFilter: %s%n(Style ommitted)",
                    dataId, bbox, width, height, filter
            ), ex);
        }
        return portray(dataId, crsCode, bbox, width, height, style, filter);
    }

    @Override
    public PortrayalResponse portray(final List<Integer> dataIds, final List<Integer> styleIds, final String crsCode,
                                     final String bbox, final int width, final int height,
                                     final String filter) throws ConstellationException {
        ensureNonNull("dataId", dataIds);
        List<Style> styles = new ArrayList<>();
        if (styleIds != null) {
            ensureExpectedCount("data/style size", dataIds.size(), styles.size());
            for (int i = 0; i < dataIds.size(); i++) {
                Integer sid = styleIds.get(i);
                Style s = null;
                if (sid != null) {
                    s = styleBusiness.getStyle(sid);
                }
                styles.add(null);
            }
        } else {
            // create a list full of null
            for (int i = 0; i < dataIds.size(); i++) {
                styles.add(null);
            }
        }
        return portray(dataIds, crsCode, bbox, width, height, styles, filter);
    }

    private PortrayalResponse portray(final Integer dataId, final String crsCode,
                                     final String bbox, final int width, final int height,
                                     final Style style, final String filter) throws ConstellationException {
        ensureNonNull("dataId", dataId);
        return portray(Arrays.asList(dataId), crsCode, bbox, width, height, Arrays.asList(style), filter);
    }

    private PortrayalResponse portray(final List<Integer> dataIds, final String crsCode,
                                     final String bbox, final int width, final int height,
                                     final List<Style> styles, final String filter) throws ConstellationException {

        ensureExpectedCount("data/style size", dataIds.size(), styles.size());
        try {
            final MapLayers mapContext = MapBuilder.createContext();

            for (int j = 0; j < dataIds.size(); j++) {

                // Get the data (throws exception if doesn't exist).
                Integer dataId = dataIds.get(j);
                Style style    = styles.get(j);

                final org.constellation.dto.Data data  = dataBusiness.getData(dataId);
                if (data == null) throw new TargetNotFoundException("Unexisting data: " + dataId);

                final Data d = DataProviders.getProviderData(data.getProviderId(), data.getNamespace(), data.getName());;

                if (d == null) throw new ConstellationStoreException("Unable to find a provider data for name:{" + data.getNamespace() +  "}:" +  data.getName());

                // Map item.
                MapItem mapItem = d.getMapLayer(style);
                if (filter != null && !filter.isEmpty() && mapItem instanceof MapLayer ml) {
                    try {
                        Filter f = CQL.parseFilter(filter);
                        FeatureQuery query = new FeatureQuery();
                        query.setSelection(f);
                        ml.setQuery(query);
                    } catch (CQLException e) {
                        throw new CstlServiceException("Error while parsing cql filter", e);
                    }
                }
                mapContext.getComponents().add(mapItem);
            }

            // Envelope.
            final String[] bboxSplit = bbox.split(",");
            final GeneralEnvelope envelope = new GeneralEnvelope(CRS.forCode(crsCode));
            final int bboxDim = bboxSplit.length / 2;
            ensureDimensionMatches("BBOX dimension", bboxDim, envelope);
            for (int i = 0 ; i < bboxDim ; i++) {
                final double lower = Double.parseDouble(bboxSplit[i].trim());
                final double upper = Double.parseDouble(bboxSplit[i + bboxDim].trim());
                envelope.setRange(i, lower, upper);
            }

            // Dimension.
            final Dimension dimension = new Dimension(width, height);

            // Inputs.
            final SceneDef sceneDef = new SceneDef(mapContext, DEFAULT_HINTS);
            final CanvasDef canvasDef = new CanvasDef(dimension, envelope);
            canvasDef.setMonitor(DEFAULT_MONITOR);
            final OutputDef outputDef = new OutputDef("image/png", new Object());

            // Create response.
            return new PortrayalResponse(canvasDef, sceneDef, outputDef);

        } catch (FactoryException | ConstellationStoreException ex) {

            throw new CstlServiceException(String.format(
                    "Rendering failed for parameters:%nData: %s%nBbox: %s%nWidth: %d%nHeight: %d%nFilter: %s%n(Style ommitted)",
                    StringUtilities.toCommaSeparatedValues(dataIds), bbox, width, height, filter
            ), ex);
        }
    }

    @Override
    public MapLayers createContext(LayerCache layerRef, MutableStyle styleRef) throws ConstellationStoreException {
        return createContext(
                 Collections.singletonList(layerRef),
                 Collections.singletonList(styleRef),
                 Collections.EMPTY_LIST,
                 Collections.EMPTY_LIST,
                 null,
                 Collections.EMPTY_MAP);

    }

    @Override
    public MapLayers createContext(List<LayerCache> layers, List<MutableStyle> styles, List<List<String>> propertiess, List<Filter> extraFilters, Envelope env, Map<String, Object> extraParams) throws ConstellationStoreException {
        final MapLayers context = MapBuilder.createContext();

        for (int i = 0; i < layers.size(); i++) {
            final LayerCache layer = layers.get(i);
            if (layer.getData() != null) {
                final Data data = layer.getData();
                MutableStyle style = null;
                if (i < styles.size()) {
                    style = styles.get(i);
                }
                Filter extraFilter = null;
                if (i < extraFilters.size()) {
                    extraFilter = extraFilters.get(i);
                }
                final List<String> propertiesFilter = new ArrayList<>();
                if (i < propertiess.size()) {
                    propertiesFilter.addAll(propertiess.get(i));
                }

                final MapItem mapItem = data.getMapLayer(style);
                if (mapItem == null) {
                    throw new ConstellationStoreException("Could not create a mapLayer for layer: " + layer.getName());
                }
                mapItem.setVisible(true);
                final Map<String, Object> userData = mapItem.getUserProperties();
                userData.put("layerId", layer.getId());
                userData.put("layerName", layer.getName());
                layer.getAlias().ifPresent(a -> userData.put("alias", a));
                if (mapItem instanceof MapLayer mapLayer) {

                    // extra filters
                    Optional<Filter> filter = layer.getLayerFilter(env, extraFilter);
                    List<String> properties = layer.getLayerProperties(propertiesFilter);
                    if (filter.isPresent() || !properties.isEmpty()) {
                        final FeatureQuery query = new FeatureQuery();
                        if (!properties.isEmpty()) {
                            query.setProjection(properties.toArray(String[]::new));
                        }
                        if (filter.isPresent()) {
                            query.setSelection(filter.get());
                        }
                        mapLayer.setQuery(query);
                    }
                }
                context.getComponents().add(mapItem);
            } else {
                throw new ConstellationStoreException("Could not create a Context for a non Geo data: " + layers.get(i).getName());
            }
        }
        return context;
    }
}
