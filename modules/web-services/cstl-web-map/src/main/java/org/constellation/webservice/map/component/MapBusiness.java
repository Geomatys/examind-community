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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.RenderingHints;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.measure.Unit;
import javax.xml.bind.JAXBException;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.internal.storage.StoreResource;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CRS;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import org.constellation.admin.util.ImageStatisticDeserializer;
import org.constellation.business.IDataBusiness;
import org.constellation.business.IStyleBusiness;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.exception.TargetNotFoundException;
import org.constellation.portrayal.PortrayalResponse;
import org.constellation.provider.Data;
import org.constellation.provider.DataProvider;
import org.constellation.provider.DataProviders;
import org.constellation.provider.GeoData;
import org.constellation.ws.CstlServiceException;
import org.apache.sis.coverage.Category;
import org.apache.sis.coverage.SampleDimension;
import org.geotoolkit.coverage.amended.AmendedCoverageResource;
import org.geotoolkit.display.canvas.control.NeverFailMonitor;
import org.geotoolkit.display2d.service.CanvasDef;
import org.geotoolkit.display2d.service.OutputDef;
import org.geotoolkit.display2d.service.SceneDef;
import org.geotoolkit.display2d.service.ViewDef;
import org.geotoolkit.factory.Hints;
import org.geotoolkit.map.CoverageMapLayer;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.map.MapContext;
import org.geotoolkit.map.MapItem;
import org.geotoolkit.metadata.ImageStatistics;
import org.geotoolkit.sld.xml.Specification;
import org.geotoolkit.sld.xml.StyleXmlIO;
import org.geotoolkit.style.MutableStyle;
import org.geotoolkit.util.NamesExt;
import org.opengis.style.Style;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;
import org.springframework.stereotype.Component;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component
public class MapBusiness {

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
     * Produces a {@link PortrayalResponse} from the specified parameters.
     * <br>
     * This method allows to perform data rendering without WMS layer.
     *
     * @param dataId      the data identifier
     * @param crsCode     the projection code
     * @param bbox        the bounding box
     * @param width       the image width
     * @param height      the image height
     * @param sldVersion  the SLD version
     * @param sldProvider the SLD provider name
     * @param styleId     the style identifier in the provider
     * @param filter      the filter on data
     *
     * @return a {@link PortrayalResponse} instance
     * @throws ConstellationException if the {@link PortrayalResponse} can't be produced for
     * any reason
     * @throws TargetNotFoundException
     * @throws JAXBException
     */
    public PortrayalResponse portray(final Integer dataId, final String crsCode,
                                     final String bbox, final int width, final int height, final String sldVersion,
                                     final String sldProvider, final String styleId, final String filter)
                                     throws ConstellationException, TargetNotFoundException, JAXBException {
        if (sldProvider == null || styleId == null) {
            return portray(dataId, crsCode, bbox, width, height, null, sldVersion, filter);
        }
    	Style style = styleBusiness.getStyle(sldProvider, styleId);
        if (style == null){
            throw new CstlServiceException("a problem occurred while retrieving the style from the database, styleid : "+styleId+" on provider : "+sldProvider);
        }
    	StyleXmlIO styleXmlIO = new StyleXmlIO();
    	final StringWriter sw = new StringWriter();
    	styleXmlIO.writeStyle(sw, style, Specification.StyledLayerDescriptor.V_1_1_0);
    	return portray(dataId, crsCode, bbox, width, height, sw.toString(), sldVersion, filter);
    }

    /**
     * Produces a {@link PortrayalResponse} from the specified parameters.
     * <br>
     * This method allows to perform data rendering without WMS layer.
     *
     * @param dataId      the data identifier
     * @param crsCode     the projection code
     * @param bbox        the bounding box
     * @param width       the image width
     * @param height      the image height
     * @param sldVersion  the SLD version
     * @param styleId     the style identifier in the provider
     * @param filter      the filter on data
     *
     * @return a {@link PortrayalResponse} instance
     * @throws ConstellationException if the {@link PortrayalResponse} can't be produced for
     * any reason
     * @throws TargetNotFoundException
     * @throws JAXBException
     */
    public PortrayalResponse portray(final Integer dataId, final String crsCode,
                                     final String bbox, final int width, final int height, final String sldVersion,
                                     final Integer styleId, final String filter)
                                     throws ConstellationException, TargetNotFoundException, JAXBException {
        if (styleId == null) {
            return portray(dataId, crsCode, bbox, width, height, null, sldVersion, filter);
        }
    	Style style = styleBusiness.getStyle(styleId);
        if (style == null){
            throw new CstlServiceException("a problem occurred while retrieving the style from the database, styleid : "+styleId);
        }
    	StyleXmlIO styleXmlIO = new StyleXmlIO();
    	final StringWriter sw = new StringWriter();
    	styleXmlIO.writeStyle(sw, style, Specification.StyledLayerDescriptor.V_1_1_0);
    	return portray(dataId, crsCode, bbox, width, height, sw.toString(), sldVersion, filter);
    }

    /**
     * Produces a {@link PortrayalResponse} from the specified parameters.
     * <br>
     * This method allows to perform data rendering without WMS layer.
     *
     * @param dataId      the data identifier
     * @param crsCode    the projection code
     * @param bbox       the bounding box
     * @param width      the image width
     * @param height     the image height
     * @param sldBody    the style to apply
     * @param sldVersion the style version
     * @return a {@link PortrayalResponse} instance
     * @throws ConstellationException if the {@link PortrayalResponse} can't be produced for
     * any reason
     */
    public PortrayalResponse portray(final Integer dataId, final String crsCode,
                                      final String bbox, final int width, final int height, final String sldBody,
                                      final String sldVersion, final String filter) throws ConstellationException {
        ensureNonNull("dataId", dataId);

        // Get the layer (throws exception if doesn't exist).
        final org.constellation.dto.Data data  = dataBusiness.getDataBrief(dataId);
        if (data != null) {

            final DataProvider provider = DataProviders.getProvider(data.getProviderId());
            final GenericName name = NamesExt.valueOf(data.getName());
            final Data d = provider.get(name);

            if (d instanceof GeoData) {
                final GeoData layer = (GeoData) d;

                try {
                    // Envelope.
                    final String[] bboxSplit = bbox.split(",");
                    final GeneralEnvelope envelope = new GeneralEnvelope(CRS.forCode(crsCode));
                    envelope.setRange(0, Double.valueOf(bboxSplit[0]), Double.valueOf(bboxSplit[2]));
                    envelope.setRange(1, Double.valueOf(bboxSplit[1]), Double.valueOf(bboxSplit[3]));

                    // Dimension.
                    final Dimension dimension = new Dimension(width, height);

                    // Style.
                    final MutableStyle style;
                    if (sldBody != null) {
                        // Use specified style.
                        final StringReader reader = new StringReader(sldBody);
                        if ("1.1.0".equals(sldVersion)) {
                            style = new StyleXmlIO().readStyle(reader, Specification.SymbologyEncoding.V_1_1_0);
                        } else {
                            style = new StyleXmlIO().readStyle(reader, Specification.SymbologyEncoding.SLD_1_0_0);
                        }
                    } else {
                        //let portrayal process to apply its own style
                        style= null;
                    }

                // Map context.
                final MapContext mapContext = MapBuilder.createContext();
                MapItem mapItem;
                if (filter != null && !filter.isEmpty()) {
                    final Map<String,Object> params = new HashMap<>();
                    params.put("CQL_FILTER", filter);
                    final Map<String,Object> extraParams = new HashMap<>();
                    extraParams.put(Data.KEY_EXTRA_PARAMETERS, params);
                    mapItem = layer.getMapLayer(style, extraParams);
                } else {
                    mapItem = layer.getMapLayer(style, null);
                }

                //append statistics to coverage resource
                //used for palette creation when colors are not defined
                replaceResource:
                if (mapItem instanceof CoverageMapLayer) {
                    final CoverageMapLayer cl = (CoverageMapLayer)mapItem;
                    final MutableStyle covStyle = cl.getStyle();
                    final GridCoverageResource res = cl.getResource();
                    final String state = data.getStatsState();

                    if (!"COMPLETED".equalsIgnoreCase(state)) {
                        //if stats are not complete don't replace
                        break replaceResource;
                    }

                    //Note : we don't check the style, do many cases to take in consideration
                    //We could check for a RasterSymbolizer with a palette, but requires to check also filters, properties ... and so on.

                    //parse statistics
                    final ImageStatistics stats;
                    try {
                        final String result = data.getStatsResult();
                        final ObjectMapper mapper = new ObjectMapper();
                        final SimpleModule module = new SimpleModule();
                        module.addDeserializer(ImageStatistics.class, new ImageStatisticDeserializer()); //custom deserializer
                        mapper.registerModule(module);
                        stats = mapper.readValue(result, ImageStatistics.class);
                    } catch (IOException ex) {
                        //stats bad formatting.
                        throw new ConfigurationException("Coverage statistics for data "+dataId+" are incorrect, "+ex.getMessage(), ex);
                    }

                    if (stats.getBands() == null || stats.getBands().length == 0) {
                        //if stats are not complete don't replace
                        break replaceResource;
                    }

                    //get original sample dimensions
                    final List<SampleDimension> oldSampleDimensions;
                    try {
                        oldSampleDimensions = res.getSampleDimensions();
                    } catch (DataStoreException ex) {
                        throw new ConfigurationException(ex.getMessage(), ex);
                    }

                    //override sample dimensions
                    final List<SampleDimension> newSampleDimensions = new ArrayList<>();
                    final ImageStatistics.Band[] bands = stats.getBands();
                    for (int i=0; i<bands.length; i++) {

                        String description = ""+i;
                        Unit unit = Units.UNITY;

                        final List<Category> categories = new ArrayList<>();
                        if (oldSampleDimensions != null && oldSampleDimensions.size() > i) {
                            final SampleDimension gsd = oldSampleDimensions.get(i);
                            description = String.valueOf(gsd.getName());
                            unit = gsd.getUnits().orElse(Units.UNITY);

                            //preserve no-data categories
                            final List<Category> oldCategories = gsd.getCategories();
                            gsd.getNoDataValues();
                            if (oldCategories != null) {
                                for (Category cat : oldCategories) {
                                    if (!cat.isQuantitative()) {
                                        categories.add(cat);
                                    }
                                }
                            }
                        }

                        //min-max from statistics
                        int min = (int) Math.floor(bands[i].getMin());
                        int max = (int) Math.ceil(bands[i].getMax());
                        if (min == max) min = max-1;

                        final SampleDimension gsd = new SampleDimension.Builder()
                                .setName(description)
                                .addQuantitative("statdata", min, max, unit)
                                .build();
                        newSampleDimensions.add(gsd);
                    }

                    //create amended resource
                    GridCoverageResource resource = res;
                    if (res instanceof org.geotoolkit.storage.coverage.GridCoverageResource) {
                        final AmendedCoverageResource r = new AmendedCoverageResource((org.geotoolkit.storage.coverage.GridCoverageResource) res, ((StoreResource) res).getOriginator());
                        r.setOverrideDims(newSampleDimensions);
                        resource = r;
                    }

                    final CoverageMapLayer cml = MapBuilder.createCoverageLayer(resource, covStyle);
                    cml.setDescription(cl.getDescription());
                    cml.setElevationModel(cl.getElevationModel());
                    cml.setOpacity(cl.getOpacity());
                    cml.setName(cl.getName());
                    cml.setSelectable(cl.isSelectable());
                    cml.setVisible(cl.isVisible());
                    mapItem = cml;
                }

                mapContext.items().add(mapItem);

                    // Inputs.
                    final SceneDef sceneDef = new SceneDef(mapContext, DEFAULT_HINTS);
                    final CanvasDef canvasDef = new CanvasDef(dimension, null);
                    final ViewDef viewDef = new ViewDef(envelope, 0, DEFAULT_MONITOR);
                    final OutputDef outputDef = new OutputDef("image/png", new Object());

                    // Create response.
                    return new PortrayalResponse(canvasDef, sceneDef, viewDef, outputDef);

                } catch (FactoryException | JAXBException | ConstellationStoreException ex) {
                    throw new CstlServiceException(ex.getLocalizedMessage());
                }
            } else {
                throw new ConstellationStoreException("Unable to portray a non GeoData");
            }
        } else {
            throw new TargetNotFoundException("Unexisting data: " + dataId);
        }
    }

}
