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
package org.constellation.map.core;

import com.codahale.metrics.annotation.Timed;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.inject.Named;
import org.apache.sis.cql.CQL;
import org.apache.sis.cql.CQLException;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.measure.Range;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import static org.constellation.api.CommonConstants.DEFAULT_CRS;
import org.constellation.api.ServiceDef;
import org.constellation.dto.Reference;
import org.constellation.dto.StyleReference;
import org.constellation.dto.contact.Details;
import org.constellation.dto.portrayal.WMSPortrayal;
import org.constellation.dto.service.config.wxs.AttributionType;
import org.constellation.dto.service.config.wxs.FormatURL;
import org.constellation.dto.service.config.wxs.LayerConfig;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationStoreException;
import static org.constellation.map.core.WMSConstant.EXCEPTION_111_BLANK;
import static org.constellation.map.core.WMSConstant.EXCEPTION_111_INIMAGE;
import static org.constellation.map.core.WMSConstant.EXCEPTION_130_BLANK;
import static org.constellation.map.core.WMSConstant.EXCEPTION_130_INIMAGE;
import static org.constellation.map.core.WMSConstant.KEY_LAYER;
import static org.constellation.map.core.WMSConstant.KEY_LAYERS;
import static org.constellation.map.core.WMSUtilities.*;
import org.constellation.map.featureinfo.FeatureInfoFormat;
import org.constellation.map.featureinfo.FeatureInfoUtilities;
import org.constellation.portrayal.CstlPortrayalService;
import org.constellation.portrayal.PortrayalResponse;
import org.constellation.provider.CoverageData;
import org.constellation.provider.Data;
import org.constellation.util.Util;
import org.constellation.ws.CstlServiceException;
import org.constellation.ws.LayerCache;
import org.constellation.ws.LayerWorker;
import org.constellation.ws.MimeType;
import org.geotoolkit.display.PortrayalException;
import org.geotoolkit.display2d.ext.legend.DefaultLegendService;
import org.geotoolkit.display2d.ext.legend.LegendTemplate;
import org.geotoolkit.display2d.service.CanvasDef;
import org.geotoolkit.display2d.service.DefaultPortrayalService;
import org.geotoolkit.display2d.service.OutputDef;
import org.geotoolkit.display2d.service.SceneDef;
import org.geotoolkit.factory.Hints;
import org.geotoolkit.inspire.xml.vs.ExtendedCapabilitiesType;
import org.geotoolkit.inspire.xml.vs.LanguageType;
import org.geotoolkit.inspire.xml.vs.LanguagesType;
import org.apache.sis.portrayal.MapLayers;
import org.apache.sis.util.Version;
import org.constellation.api.DataType;
import org.constellation.api.WorkerState;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.constellation.dto.DimensionRange;
import static org.constellation.map.core.WMSConstant.KEY_PROPERTYNAME;
import static org.constellation.map.core.WMSConstant.NO_TRANSPARENT_FORMAT;
import org.constellation.map.util.DimensionDef;
import org.constellation.map.util.MapUtils;
import static org.constellation.map.util.MapUtils.combine;
import static org.constellation.map.util.MapUtils.transformJAXBFilter;
import org.constellation.provider.FeatureData;
import org.geotoolkit.ows.xml.OWSExceptionCode;
import static org.geotoolkit.ows.xml.OWSExceptionCode.CURRENT_UPDATE_SEQUENCE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_POINT;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_UPDATE_SEQUENCE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.LAYER_NOT_DEFINED;
import static org.geotoolkit.ows.xml.OWSExceptionCode.LAYER_NOT_QUERYABLE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.NO_APPLICABLE_CODE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.STYLE_NOT_DEFINED;
import org.geotoolkit.referencing.ReferencingUtilities;
import org.geotoolkit.sld.MutableLayer;
import org.geotoolkit.sld.MutableLayerStyle;
import org.geotoolkit.sld.MutableNamedLayer;
import org.geotoolkit.sld.MutableNamedStyle;
import org.geotoolkit.sld.MutableStyledLayerDescriptor;
import org.geotoolkit.sld.xml.GetLegendGraphic;
import org.geotoolkit.sld.xml.v110.DescribeLayerResponseType;
import org.geotoolkit.sld.xml.v110.LayerDescriptionType;
import org.geotoolkit.temporal.util.PeriodUtilities;
import org.geotoolkit.wms.xml.*;
import static org.geotoolkit.wms.xml.WmsXmlFactory.createBoundingBox;
import static org.geotoolkit.wms.xml.WmsXmlFactory.createDimension;
import static org.geotoolkit.wms.xml.WmsXmlFactory.createGeographicBoundingBox;
import static org.geotoolkit.wms.xml.WmsXmlFactory.createLayer;
import static org.geotoolkit.wms.xml.WmsXmlFactory.createLegendURL;
import static org.geotoolkit.wms.xml.WmsXmlFactory.createLogoURL;
import static org.geotoolkit.wms.xml.WmsXmlFactory.createOnlineResource;
import static org.geotoolkit.wms.xml.WmsXmlFactory.createStyle;
import org.geotoolkit.wms.xml.v130.Capability;
import org.opengis.filter.Filter;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.operation.TransformException;
import org.opengis.sld.StyledLayerDescriptor;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.lang.NonNull;

/**
 * A WMS worker for a local WMS service which handles requests from REST
 * facades and issues appropriate responses.
 * <p>
 * The classes implementing the REST facade to this service will have
 * processed the requests sufficiently to ensure that all the information
 * conveyed by the HTTP request is either in the method call parameters or is
 * in one of the fields of the parent class which holds instances of the
 * injectable interface {@code Context} objects created by the JEE container.
 * </p>
 *
 * @author Cédric Briançon (Geomatys)
 * @author Johann Sorel (Geomatys)
 * @author Guilhem Legal (Geomatys)
 * @since 0.3
 */

@Named("WMSWorker")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DefaultWMSWorker extends LayerWorker implements WMSWorker {

    /**
     * Temporal formatting for layer with TemporalCRS.
     */
    private static final DateFormat ISO8601_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    static {
        ISO8601_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private static final DateFormat ISO8601_NO_MS_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    static {
        ISO8601_NO_MS_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * List of FeatureInfo mimeTypes
     */
    private final List<String> GFI_MIME_TYPES = new ArrayList<>();

    private WMSPortrayal mapPortrayal;
    public DefaultWMSWorker(final String id) {
        super(id, ServiceDef.Specification.WMS);
        if (getState().equals(WorkerState.ERROR)) return;

        //get all supported GetFeatureInfo mimetypes
        try {
            GFI_MIME_TYPES.clear();
            GFI_MIME_TYPES.addAll(FeatureInfoUtilities.allSupportedMimeTypes(configuration));
        } catch (ConfigurationException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
        }

        mapPortrayal = new WMSPortrayal();
        try {
            WMSPortrayal candidate = (WMSPortrayal) serviceBusiness.getExtraConfiguration("WMS", id, "WMSPortrayal.xml");
            if (candidate != null) {
                mapPortrayal = candidate;
            }
        } catch (ConstellationException ex) {
            LOGGER.log(Level.WARNING, null, ex);
        }
        started();
    }

    /**
     * Return a description of layers specified in the user's request.
     *
     * TODO: Does this actually do anything? why does this never access Data?
     * TODO: Is this broken?
     *
     * @param descLayer The {@linkplain DescribeLayer describe layer} request.
     * @return a description of layers specified in the user's request.
     *
     * @throws CstlServiceException
     */
    @Override
    public DescribeLayerResponseType describeLayer(final DescribeLayer descLayer) throws CstlServiceException {
        final String serviceUrl = getServiceUrl();

        final List<LayerDescriptionType> layerDescriptions = new ArrayList<>();
        final List<String> layerNames = descLayer.getLayers();
        for (String layerName : layerNames) {
            final LayerDescriptionType outputLayer = new LayerDescriptionType(serviceUrl, layerName);
            layerDescriptions.add(outputLayer);
        }
        return new DescribeLayerResponseType("1.1.1", layerDescriptions);
    }


    /**
     * {@inheritDoc }
     */
    @Override
    public AbstractWMSCapabilities getCapabilities(String version) throws CstlServiceException {
        return getCapabilities(new GetCapabilities(new Version(version)));
    }

    /**
     * return a date formmatter depending on configuration flags.
     * @return 
     */
    private DateFormat getDateFormatter() {
        boolean noMs = Application.getBooleanProperty(AppProperty.EXA_WMS_NO_MS, false);
        if (noMs) {
            return ISO8601_NO_MS_FORMAT;
        }
        return ISO8601_FORMAT;
    }

    /**
     * Describe the capabilities and the layers available of this service.
     *
     * @param getCapab       The {@linkplain GetCapabilities get capabilities} request.
     * @return a WMSCapabilities XML document describing the capabilities of the service.
     *
     * @throws CstlServiceException
     */
    @Override
    @Timed
    public AbstractWMSCapabilities getCapabilities(final GetCapabilities getCapab) throws CstlServiceException {
        isWorking();
        final String queryVersion      = getCapab.getVersion().toString();
        final String requestedLanguage = getCapab.getLanguage();
        final String userLogin         = getUserLogin();
        // we get the request language, if its not set we get the default "eng"
        final String currentLanguage;
        if (requestedLanguage != null && supportedLanguages.contains(requestedLanguage)) {
            currentLanguage = requestedLanguage;
        } else {
            currentLanguage = getDefaultLanguage();
        }

        //set the current updateSequence parameter
        final boolean returnUS = returnUpdateSequenceDocument(getCapab.getUpdateSequence());
        if (returnUS) {
            throw new CstlServiceException("the update sequence parameter is equal to the current", CURRENT_UPDATE_SEQUENCE, "updateSequence");
        }

        final Object cachedCapabilities = getCapabilitiesFromCache(queryVersion, currentLanguage);
        if (cachedCapabilities != null) {
            return (AbstractWMSCapabilities) cachedCapabilities;
        }

        final Details skeleton = getStaticCapabilitiesObject("wms", currentLanguage);
        final AbstractWMSCapabilities inCapabilities = WMSConstant.createCapabilities(queryVersion, skeleton, getCurrentUpdateSequence());

        // temporary sort in order to fix cite test
        final AbstractRequest request;
        final List<String> exceptionFormats;
        if (queryVersion.equals(ServiceDef.WMS_1_1_1_SLD.version.toString())) {
            request          = WMSConstant.createRequest111(GFI_MIME_TYPES).clone();
            exceptionFormats = WMSConstant.EXCEPTION_111;
        } else {
            request          = WMSConstant.createRequest130(GFI_MIME_TYPES).clone();
            exceptionFormats = WMSConstant.EXCEPTION_130;
        }
        request.updateURL(getServiceUrl());
        inCapabilities.getCapability().setRequest(request);
        inCapabilities.getCapability().setExceptionFormats(exceptionFormats);

        //Build the list of layers
        final List<AbstractLayer> outputLayers = new ArrayList<>();
        final List<LayerCache> layers = getLayerCaches(userLogin);

       for (LayerCache layer : layers) {
            final Data data = layer.getData();

            if (data == null) {
                LOGGER.log(Level.WARNING, "Unable to find a provider data correspounding to layer:{0}", layer.getName());
                continue;
            }

            if (!layer.isQueryable(ServiceDef.Query.WMS_ALL)) {
                continue;
            }

            // Get default CRS for the layer supported crs.
            final Envelope nativeEnv;
            try {
                nativeEnv = layer.getEnvelope();
                if (nativeEnv == null) {
                    LOGGER.log(Level.WARNING, "Cannot get envelope for layer {0}  (null)", layer.getName());
                    continue;
                }
            } catch (ConstellationStoreException ex) {
                LOGGER.log(Level.WARNING, ex, () -> "Cannot get envelope for layer " + layer.getName());
                continue;
            }

            CoordinateReferenceSystem nativeCRS = nativeEnv.getCoordinateReferenceSystem();
            String nativeCrsCode = null;
            try {
               if (nativeCRS != null) {
                   final Integer epsgCode = IdentifiedObjects.lookupEPSG(nativeCRS);
                   if (epsgCode != null) {
                       nativeCrsCode = "EPSG:" + epsgCode;
                   }
               }
           } catch (FactoryException ex) {
               LOGGER.log(Level.INFO, "Error retrieving data crs for the layer :" + layer.getName(), ex);
           }

            GeographicBoundingBox inputGeoBox = null;
            try {
                inputGeoBox = layer.getGeographicBoundingBox();
            } catch (ConstellationStoreException ex) {
                LOGGER.log(Level.WARNING, "Error retrieving bouding box values for the layer :"+ layer.getName(), ex);
            }

            if (inputGeoBox == null) {
                // The layer does not contain geometric information, we do not want this layer
                // in the capabilities response.
                continue;
            }

            // We ensure that the data envelope is not empty. It can occurs with vector data, on a single point.
            inputGeoBox = notEmptyBBOX(inputGeoBox);

            // List of elevations, times and dim_range values.
            final List<AbstractDimension> dimensions = new ArrayList<>();

            /*
             * Dimension: the available dates
             */
            try {
                final SortedSet<Date> dates = layer.getAvailableTimes();
                if (!dates.isEmpty()) {
                    final DateFormat df = getDateFormatter();
                    synchronized (df) {
                        final PeriodUtilities periodFormatter = new PeriodUtilities(df);
                        final String defaut = df.format(dates.last());
                        AbstractDimension dim = createDimension(queryVersion, "time", "ISO8601", defaut, null);
                        dim.setValue(periodFormatter.getDatesRespresentation(dates));
                        dimensions.add(dim);
                    }
                }
            } catch (ConstellationStoreException ex) {
                LOGGER.log(Level.WARNING, "Error retrieving dates values for the layer :" + layer.getName(), ex);
            }

            /*
             * Dimension: the available elevations
             */
            try {
               final SortedSet<Number> elevations = layer.getAvailableElevations();
               if (!elevations.isEmpty()) {
                   // Define elevation unit as a CRS identifier. See Annex C.2
                   String unit = null;
                   try {
                       final VerticalCRS vCrs = CRS.getVerticalComponent(nativeCRS, true);
                       unit = ReferencingUtilities.lookupIdentifier(vCrs, true);
                   } catch (Exception e) {
                       LOGGER.log(Level.WARNING, "Cannot find any valid identifier for vertical CRS.", e);
                   }
                   final String values = elevations.stream()
                           .map(Number::toString)
                           .collect(Collectors.joining(","));
                   AbstractDimension dim = createDimension(queryVersion, "elevation", unit, elevations.first().toString(), values);
                   dimensions.add(dim);
               }
           } catch (ConstellationStoreException ex) {
               LOGGER.log(Level.WARNING, "Error retrieving elevation values for the layer :" + layer.getName(), ex);
           }

            /*
             * Dimension: the dimension range.
             * TODO: why this block ignore the dimension after the first?
             */
            try {
               final SortedSet<DimensionRange> ranges = layer.getSampleValueRanges();
               /* If the layer has only one sample dimension, then we can apply the dim_range
                * parameter. Otherwise it can be a multiple sample dimensions layer, and we
                * don't apply the dim_range.
                */
               if (ranges.size() == 1 && ranges.first() != null) {
                   final DimensionRange firstRange = ranges.first();
                   final double minRange = firstRange.getMin();
                   final double maxRange = firstRange.getMax();
                   final String defaut = minRange + "," + maxRange;
                   final String unit = firstRange.getUnit();
                   String unitSymbol = firstRange.getUnitsymbol();
                   AbstractDimension dim = createDimension(queryVersion, minRange + "," + maxRange, "dim_range", unit, unitSymbol, defaut, null, null, null);
                   dimensions.add(dim);
               }
            } catch (ConstellationStoreException ex) {
               LOGGER.log(Level.WARNING, "Error retrieving range values for the layer :" + layer.getName(), ex);
            }

            // get resolution
            Double[] nativeResolution = new Double[2];
            try {
                nativeResolution = layer.getResolution();
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            }

            // Verify extra dimensions
            try {
                dimensions.addAll(getExtraDimensions(layer, queryVersion));
            } catch (ConstellationStoreException ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                break;
            }

            /*
             * LegendUrl generation
             * TODO: Use a StringBuilder or two
             */
            final String layerName      = identifier(layer);
            final String beginLegendUrl = getServiceUrl() + "REQUEST=GetLegendGraphic&VERSION=1.1.1&FORMAT=";
            final String legendUrlGif   = beginLegendUrl + MimeType.IMAGE_GIF + "&LAYER=" + layerName;
            final String legendUrlPng   = beginLegendUrl + MimeType.IMAGE_PNG + "&LAYER=" + layerName;
            final String queryable      = (layer.isQueryable(ServiceDef.Query.WMS_GETINFO)) ? "1" : "0";
            final String _abstract;
            final String keyword;
            if (DataType.COVERAGE.equals(layer.getDataType())) {
                _abstract = "Coverage data";
                keyword   = "Coverage data";
            } else {
                _abstract = "Vector data";
                keyword   = "Vector data";
            }

            final AbstractBoundingBox outputBBox;
            AbstractBoundingBox nativeBBox = null;
            if (queryVersion.equals(ServiceDef.WMS_1_1_1_SLD.version.toString())) {
                /*
                 * TODO
                 * do we have to use the same order as WMS 1.3.0 (SOUTH WEST NORTH EAST) ???
                 */
                outputBBox = createBoundingBox(queryVersion,
                        "EPSG:4326",
                        inputGeoBox.getWestBoundLongitude(),
                        inputGeoBox.getSouthBoundLatitude(),
                        inputGeoBox.getEastBoundLongitude(),
                        inputGeoBox.getNorthBoundLatitude(), null, null);

                if (nativeCrsCode != null && nativeCRS != null) {
                    try {
                        final Envelope rightHanded = Envelopes.transform(nativeEnv, AbstractCRS.castOrCopy(nativeCRS).forConvention(AxesConvention.RIGHT_HANDED));
                        nativeBBox = createBoundingBox(queryVersion,
                            nativeCrsCode,
                            rightHanded.getMinimum(0),
                            rightHanded.getMinimum(1),
                            rightHanded.getMaximum(0),
                            rightHanded.getMaximum(1), nativeResolution[0], nativeResolution[1]);
                    } catch (TransformException ex) {
                        LOGGER.log(Level.INFO, "Error retrieving data crs for the layer :" + layer.getName(), ex);
                    }
                }

            } else {
                /*
                 * TODO
                 * Envelope inputBox = inputLayer.getCoverage().getEnvelope();
                 */
                outputBBox = createBoundingBox(queryVersion,
                            "EPSG:4326",
                            inputGeoBox.getSouthBoundLatitude(),
                            inputGeoBox.getWestBoundLongitude(),
                            inputGeoBox.getNorthBoundLatitude(),
                            inputGeoBox.getEastBoundLongitude(), null, null);

                if (nativeCrsCode != null) {
                    nativeBBox = createBoundingBox(queryVersion,
                        nativeCrsCode,
                        nativeEnv.getMinimum(0),
                        nativeEnv.getMinimum(1),
                        nativeEnv.getMaximum(0),
                        nativeEnv.getMaximum(1), nativeResolution[0], nativeResolution[1]);
                }

            }
            // we build a Style Object
            final List<StyleReference> stylesName = layer.getStyles();
            final List<org.geotoolkit.wms.xml.Style> styles = new ArrayList<>();
            if (stylesName != null && !stylesName.isEmpty()) {
                // For each styles defined for the layer, get the dimension of the getLegendGraphic response.
                for (StyleReference styleName : stylesName) {
                    final org.opengis.style.Style ms = getStyle(styleName);
                    String legendUrlPng2 =  legendUrlPng+"&STYLE="+ styleName.getName();
                    String legendUrlGif2 =  legendUrlGif+"&STYLE="+ styleName.getName();
                    final org.geotoolkit.wms.xml.Style style = convertStyleToWmsStyle(queryVersion, ms, data, legendUrlPng2, legendUrlGif2);
                    styles.add(style);
                }
            }

            //list supported crs
            final List<String> supportedCrs;
            if (nativeCrsCode != null && DEFAULT_CRS.indexOf(nativeCrsCode) != 0) {
                //we add or move to first position the native crs
                supportedCrs = new ArrayList<>(DEFAULT_CRS);
                supportedCrs.remove(nativeCrsCode);
                supportedCrs.add(0, nativeCrsCode);
            } else {
                supportedCrs = DEFAULT_CRS;
            }

            final AbstractGeographicBoundingBox bbox = createGeographicBoundingBox(queryVersion, inputGeoBox);
            final AbstractLayer outputLayerO = createLayer(queryVersion, layerName, _abstract, keyword, supportedCrs, bbox, outputBBox, queryable, dimensions, styles);
            if (nativeBBox != null && !nativeBBox.getCRSCode().equals(outputBBox.getCRSCode())) {
                ((List) outputLayerO.getBoundingBox()).add(0, nativeBBox);
            }

            final AbstractLayer outputLayer = customizeLayer(queryVersion, outputLayerO, layer.getConfiguration(), currentLanguage);
            outputLayers.add(outputLayer);
        }

        //we build the general layer and add it to the document
        final AbstractLayer mainLayer = customizeLayer(queryVersion, createLayer(queryVersion, "Examind Web Map Layer",
                    "description of the service(need to be fill)", DEFAULT_CRS,
                    createGeographicBoundingBox(queryVersion, -180.0, -90.0, 180.0, 90.0), outputLayers), getMainLayer(), currentLanguage);

        inCapabilities.getCapability().setLayer(mainLayer);


        /*
         * INSPIRE PART
         */
        if (queryVersion.equals(ServiceDef.WMS_1_3_0.version.toString()) || queryVersion.equals(ServiceDef.WMS_1_3_0_SLD.version.toString()) ) {

            final Capability capa = (Capability) inCapabilities.getCapability();
            final ExtendedCapabilitiesType inspireExtension =  capa.getInspireExtendedCapabilities();

            if (inspireExtension != null) {
                inspireExtension.setMetadataDate(new Date(System.currentTimeMillis()));

                List<LanguageType> languageList = new ArrayList<>();
                for (String language : supportedLanguages) {
                    boolean isDefault = language.equals(getDefaultLanguage());
                    languageList.add(new LanguageType(language, isDefault));
                }
                LanguagesType languages = new LanguagesType(languageList);
                inspireExtension.setLanguages(languages);
                if (currentLanguage == null) {
                    inspireExtension.setCurrentLanguage(getDefaultLanguage());
                } else {
                    inspireExtension.setCurrentLanguage(currentLanguage);
                }
            }

        }
        putCapabilitiesInCache(queryVersion, currentLanguage, inCapabilities);
        return inCapabilities;
    }

    /**
     * Get extra dimensions from a layer.
     *
     * @param layer A layer.
     * @param queryVersion Version of the request.
     * @return A list of extra dimensions, never {@code null}
     * @throws DataStoreException
     */
    private List<AbstractDimension> getExtraDimensions(final LayerCache layer, final String queryVersion) throws ConstellationStoreException {
        // TODO handle the mapcontxt data case where we should merge the dimensions values
        final List<AbstractDimension> dimensions = new ArrayList<>();
        if (layer.getData() instanceof CoverageData covData) {
            for (org.constellation.dto.Dimension d : covData.getSpecialDimensions()) {
                dimensions.add(createDimension(queryVersion, d.getValue(), d.getName(), d.getUnits(), d.getUnitSymbol(), d.getDefault(), null, null, null));
            }
        } else if (layer.getData() instanceof FeatureData fdata) {

            if (layer.hasFilterAndDimension()) {
                final FeatureSet fs = fdata.getOrigin();
                final List<DimensionDef> dims = layer.getDimensiondefinition();
                for (DimensionDef ddef : dims) {
                    final Collection<Range> collRefs = getDimensionRange(fs, ddef.lower, ddef.upper);

                    // Transform it to a set in order to filter same values
                    final Set<Range> refs = new HashSet<>();
                    for (Range ref : collRefs) {
                        refs.add(ref);
                    }

                    if (refs.isEmpty()) {
                        // Dimension applied on a layer which has no values: just skip this dimension
                        continue;
                    }

                    final String sortedValues = printValues(refs);
                    final String unitSymbol = ddef.crs.getCoordinateSystem().getAxis(0).getUnit().toString();
                    final String unit = unitSymbol;
                    final String axisName = ddef.crs.getCoordinateSystem().getAxis(0).getName().getCode();
                    final String defaut = "";

                    final AbstractDimension adim = (queryVersion.equals(ServiceDef.WMS_1_1_1_SLD.version.toString())) ?
                        new org.geotoolkit.wms.xml.v111.Dimension(sortedValues, axisName, unit,
                            unitSymbol, defaut, null, null, null) :
                        new org.geotoolkit.wms.xml.v130.Dimension(sortedValues, axisName, unit,
                            unitSymbol, defaut, null, null, null);

                    dimensions.add(adim);
                }
            }
        }
        return dimensions;
    }

    /**
     * Apply the layer customization extracted from the configuration.
     *
     * @param version
     * @param outputLayer
     * @param configLayer
     * @param language
     * @return
     * @throws CstlServiceException
     */
    private AbstractLayer customizeLayer(final String version, final AbstractLayer outputLayer, final LayerConfig configLayer, String language) throws CstlServiceException
    {
        if (configLayer == null) {
            return outputLayer;
        }

        if (language != null && configLayer.getMultiLangTitle().containsKey(language)) {
            outputLayer.setTitle(configLayer.getMultiLangTitle().get(language));

        // fallback
        } else if (configLayer.getTitle() != null) {
            outputLayer.setTitle(configLayer.getTitle());
        }

        if (language != null && configLayer.getMultiLangAbstract().containsKey(language)) {
            outputLayer.setAbstract(configLayer.getMultiLangAbstract().get(language));

        // fallback
        } else if (configLayer.getAbstrac() != null) {
            outputLayer.setAbstract(configLayer.getAbstrac());
        }

        if (language != null && configLayer.getMultiLangKeywords().containsKey(language)) {
            outputLayer.setKeywordList(new ArrayList<>(configLayer.getMultiLangKeywords().get(language).getList()));

        // fallback
        } else if (configLayer.getKeywords() != null && !configLayer.getKeywords().isEmpty()) {
            outputLayer.setKeywordList(configLayer.getKeywords());
        }

        if (configLayer.getMetadataURL() != null) {
            final List<FormatURL> metadataURLs = configLayer.getMetadataURL();
            for (FormatURL metadataURL : metadataURLs) {
                // add not override (not a mistake)
                outputLayer.setMetadataURL(metadataURL.getFormat(),
                                           metadataURL.getOnlineResource().getHref(),
                                           metadataURL.getType());
            }
        }
        if (configLayer.getDataURL() != null) {
            final FormatURL dataURL = configLayer.getDataURL();
            outputLayer.setDataURL(dataURL.getFormat(),
                                      dataURL.getOnlineResource().getHref());
        }
        if (configLayer.getAuthorityURL() != null) {
            final FormatURL authorityURL = configLayer.getAuthorityURL();
            outputLayer.setAuthorityURL(authorityURL.getName(),
                                           authorityURL.getOnlineResource().getHref());
        }
        if (configLayer.getIdentifier() != null) {
            final Reference identifier = configLayer.getIdentifier();
            outputLayer.setIdentifier(identifier.getAuthority(), identifier.getValue());
        }
        if (configLayer.getAttribution() != null) {
            final AttributionType attribution = configLayer.getAttribution();
            final FormatURL fUrl = attribution.getLogoURL();
            final AbstractLogoURL logoUrl;
            if (fUrl != null) {
                logoUrl = createLogoURL(version, fUrl.getFormat(), fUrl.getOnlineResource().getHref(), fUrl.getWidth(), fUrl.getHeight());
            } else {
                logoUrl = null;
            }
            outputLayer.setAttribution(attribution.getTitle(),
                                          attribution.getOnlineResource().getHref(),
                                          logoUrl);
        }
        if (configLayer.getOpaque() != null) {
            int opaque = 0;
            if (configLayer.getOpaque()) {
                opaque = 1;
            }
            outputLayer.setOpaque(opaque);
        }
        if (!configLayer.getCrs().isEmpty()) {
            outputLayer.setCrs(configLayer.getCrs());
        }
        return outputLayer;
    }


    /**
     *
     * @param currentVersion
     * @param ms
     * @param data
     * @param legendUrlPng
     * @param legendUrlGif
     * @return
     */
    private org.geotoolkit.wms.xml.Style convertStyleToWmsStyle(final String currentVersion, final org.opengis.style.Style ms, final Data data,
            final String legendUrlPng, final String legendUrlGif)
    {
        AbstractOnlineResource or = createOnlineResource(currentVersion, legendUrlPng);
        final LegendTemplate lt = mapPortrayal.getDefaultLegendTemplate();
        final Dimension dimension;
        try {
            dimension = DefaultLegendService.legendPreferredSize(lt, data.getMapLayer(ms));
        } catch (ConstellationStoreException ex) {
            LOGGER.log(Level.INFO, ex.getLocalizedMessage(), ex);
            return null;
        }

        final AbstractLegendURL legendURL1 = createLegendURL(currentVersion, MimeType.IMAGE_PNG, or, dimension.width, dimension.height);

        or = createOnlineResource(currentVersion, legendUrlGif);
        final AbstractLegendURL legendURL2 = createLegendURL(currentVersion, MimeType.IMAGE_GIF, or, dimension.width, dimension.height);

        String styleName = ms.getName();
        return createStyle(currentVersion, styleName, styleName, null, legendURL1, legendURL2);
    }

    /**
     * Return the value of a point in a map.
     *
     * @param getFI The {@linkplain GetFeatureInfo get feature info} request.
     * @return text, HTML , XML or GML code.
     *
     * @throws CstlServiceException
     */
    @Override
    public Map.Entry<String, Object> getFeatureInfo(final GetFeatureInfo getFI) throws CstlServiceException {
        isWorking();
        //
        // Note this is almost the same logic as in getMap
        //

        //       -- get the List of layer references
        final String userLogin             = getUserLogin();
        final List<GenericName> layerNames = getFI.getQueryLayers();
        final List<LayerCache> layersCache;
        try {
            layersCache = getLayerCaches(userLogin, layerNames);
        } catch (CstlServiceException ex) {
            throw new CstlServiceException(ex, LAYER_NOT_DEFINED, KEY_LAYERS.toLowerCase());
        }

        for (LayerCache layer : layersCache) {
            if (!layer.isQueryable(ServiceDef.Query.WMS_GETINFO)) {
                throw new CstlServiceException("You are not allowed to request the layer \""+
                        layer.getName() +"\".", LAYER_NOT_QUERYABLE, KEY_LAYERS.toLowerCase());
            }
        }

        // 1. VIEW
        final Envelope refEnv = buildRequestedViewEnvelope(getFI, layersCache);
        final double azimuth = getFI.getAzimuth();

        // 2. SCENE
        //       -- build an equivalent style List
        //TODO: clean up the SLD vs. style logic
        final List<String> styleNames   = getFI.getStyles();
        final StyledLayerDescriptor sld = getFI.getSld();

        final List<org.opengis.style.Style> styles = getStyles(layersCache, sld, styleNames);
        List<List<String>> propertyNames = Collections.EMPTY_LIST;
        if (getFI.getParameters().containsKey(KEY_PROPERTYNAME)) {
            if (getFI.getParameters().get(KEY_PROPERTYNAME) instanceof List pname) {
                propertyNames = pname;
            } else {
                throw new CstlServiceException("Expecting a list of propertyName", INVALID_VALUE, KEY_PROPERTYNAME);
            }
        }
        final Map<String, Object> extraParams = getFI.getParameters();
        
        // Build additional filters
        List<Filter> extraFilters = extractAdditionalFilters(getFI);

        final SceneDef sdef = new SceneDef();

        try {
            final MapLayers context = mapBusiness.createContext(layersCache, styles, propertyNames, extraFilters, refEnv, extraParams);
            sdef.setContext(context);
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex, NO_APPLICABLE_CODE);
        }

        // 3. CANVAS
        final Dimension canvasDimension = getFI.getSize();
        final Color background;
        if (getFI.getTransparent()) {
            background = null;
        } else {
            final Color color = getFI.getBackground();
            background = (color == null) ? Color.WHITE : color;
        }
        CanvasDef cdef = new CanvasDef(canvasDimension,null);
        cdef.setBackground(background);
        cdef.setEnvelope(refEnv);
        cdef.setAzimuth(azimuth);
        cdef = adaptToVersion(cdef, WMSVersion.getVersion(getFI.getVersion().toString()));

        // 4. SHAPE
        final int pixelTolerance = 3;
        final int x = getFI.getX();
        final int y = getFI.getY();
        if (x < 0 || x > canvasDimension.width) {
            throw new CstlServiceException("The requested point has an invalid X coordinate.", INVALID_POINT);
        }
        if (y < 0 || y > canvasDimension.height) {
            throw new CstlServiceException("The requested point has an invalid Y coordinate.", INVALID_POINT);
        }
        final Rectangle selectionArea = new Rectangle( getFI.getX()-pixelTolerance,
                                               getFI.getY()-pixelTolerance,
                                               pixelTolerance*2,
                                               pixelTolerance*2);

        // 5. VISITOR
        String infoFormat = getFI.getInfoFormat();
        if (infoFormat == null) {
            //Should not happen since the info format parameter is mandatory for the GetFeatureInfo request.
            infoFormat = MimeType.TEXT_PLAIN;
        }

        //search custom FeatureInfoFormat
        LayerConfig config = null;
        if (layersCache.size() == 1) {
            config = layersCache.get(0).getConfiguration();
        }

        final FeatureInfoFormat featureInfo =  getFeatureInfo(config, infoFormat);
        try {
            //give the layerRef list used by some FIF
            featureInfo.setLayers(layersCache);
            final Object result = featureInfo.getFeatureInfo(sdef, cdef, selectionArea, getFI);
            return new AbstractMap.SimpleEntry<>(infoFormat, result);
        } catch (PortrayalException ex) {
            throw new CstlServiceException(ex, NO_APPLICABLE_CODE);
        }
    }

    /**
     * Return the legend graphic for the current layer.
     * <p>If no width or height have been specified, a default output
     * size is adopted, the size will depend on the symbolizer used.</p>
     *
     * @param getLegend The {@linkplain GetLegendGraphic get legend graphic} request.
     * @return a file containing the legend graphic image.
     *
     * @throws CstlServiceException if the layer does not support GetLegendGraphic requests.
     */
    @Override
    public PortrayalResponse getLegendGraphic(final GetLegendGraphic getLegend) throws CstlServiceException {
        isWorking();
        final String userLogin  = getUserLogin();
        final LayerCache layer  = getLayerCache(userLogin, getLegend.getLayer());
        final String layerName = layer.getName().toString();
        if (!layer.isQueryable(ServiceDef.Query.WMS_ALL)) {
            throw new CstlServiceException("You are not allowed to request the layer \""+
                    layerName +"\".", LAYER_NOT_QUERYABLE, KEY_LAYER.toLowerCase());
        }

        final Data data      = layer.getData();
        final Integer width  = getLegend.getWidth();
        final Integer height = getLegend.getHeight();

        final Dimension dims;
        if (width != null && height != null) {
            dims = new Dimension(width, height);
        } else {
            //layers will calculate the best size
            dims = null;
        }
        final BufferedImage image;
        final String rule = getLegend.getRule();
        final Double scale = getLegend.getScale();
        final String sldUrl = getLegend.getSld();
        final String style = getLegend.getStyle();
        try {
            org.opengis.style.Style ms = null;
            // If a sld file is given, extracts the style from it.
            if (sldUrl != null && !sldUrl.isEmpty()) {
                if (getLegend.getSldVersion() == null) {
                    throw new PortrayalException("SLD version must be specified");
                }
                final MutableStyledLayerDescriptor mutableSLD;
                try {
                    mutableSLD = (MutableStyledLayerDescriptor) styleBusiness.readSLD(new URL(sldUrl), getLegend.getSldVersion().name());
                } catch (ConstellationException ex) {
                    final String message;
                    if (ex.getCause() instanceof FileNotFoundException) {
                        message = "The given url \""+ sldUrl +"\" points to an non-existing file.";
                    } else {
                        message = ex.getLocalizedMessage();
                    }
                    throw new PortrayalException(message, ex);
                }  catch (MalformedURLException ex) {
                    throw new PortrayalException("The given SLD url \""+ sldUrl +"\" is not a valid url", ex);
                }

                final List<MutableLayer> emptyNameMutableLayers = new ArrayList<>();
                for (final MutableLayer mutableLayer : mutableSLD.layers()) {
                    final String mutableLayerName = mutableLayer.getName();
                    if (mutableLayerName == null || mutableLayerName.isEmpty()) {
                        emptyNameMutableLayers.add(mutableLayer);
                        continue;
                    }
                    if (layerName.equals(mutableLayerName)) {
                        ms = (org.opengis.style.Style) mutableLayer.styles().get(0);
                        break;
                    }
                }
                if (ms == null) {
                    LOGGER.log(Level.INFO, "No layer {0} found for the given SLD. Continue with the first style found.", layerName);
                    ms = (org.opengis.style.Style) emptyNameMutableLayers.get(0).styles().get(0);
                }
            } else if (style != null && !style.isEmpty()) {
                for (StyleReference ref : layer.getStyles()) {
                    if(style.equals(ref.getName())) {
                        ms = getStyle(ref);
                        break;
                    }
                }
            } else {
                // No sld given, we use the style.
                if (!layer.getStyles().isEmpty()) {
                    final StyleReference styleRef = layer.getStyles().get(0);
                    ms = getStyle(styleRef);
                }
            }
            image = getLegendGraphicImg(data.getMapLayer(ms), dims, mapPortrayal.getDefaultLegendTemplate(), ms, rule, scale);
        } catch (PortrayalException | ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
        if (image == null) {
            throw new CstlServiceException("The requested layer \""+ layerName +"\" does not support "
                    + "GetLegendGraphic request", NO_APPLICABLE_CODE, KEY_LAYER.toLowerCase());
        }
        return new PortrayalResponse(image);
    }

    /**
     * Return a map for the specified parameters in the query.
     *
     * @param getMap The {@linkplain GetMap get map} request.
     * @return The map requested, or an error.
     *
     * @throws CstlServiceException
     */
    @Override
    @Timed
    public PortrayalResponse getMap(final GetMap getMap) throws CstlServiceException {
        isWorking();
        final String queryVersion = getMap.getVersion().toString();
        final String userLogin    = getUserLogin();
    	//
    	// Note this is almost the same logic as in getFeatureInfo
    	//
        // TODO support BLANK exception format for WMS1.1.1 and WMS1.3.0
        final String errorType = getMap.getExceptionFormat();
        final boolean errorInImage;
        final boolean errorBlank;
        if (queryVersion.equals(ServiceDef.WMS_1_3_0.version.toString())) {
            errorInImage = EXCEPTION_130_INIMAGE.equalsIgnoreCase(errorType);
            errorBlank = EXCEPTION_130_BLANK.equalsIgnoreCase(errorType);
        } else {
            errorInImage = EXCEPTION_111_INIMAGE.equalsIgnoreCase(errorType);
            errorBlank = EXCEPTION_111_BLANK.equalsIgnoreCase(errorType);
        }

        // get the List of layer references
        final List<GenericName> layerNames = getMap.getLayers();

        //check layer limit
        final Details skeleton = getStaticCapabilitiesObject("wms", getDefaultLanguage());
        if (skeleton.getServiceConstraints()!=null) {
            final int layerLimit = skeleton.getServiceConstraints().getLayerLimit();
            if(layerLimit>0 && layerLimit<layerNames.size()) {
                throw new CstlServiceException("Too many layers requested, limit is "+layerLimit, INVALID_PARAMETER_VALUE);
            }
        }
        final List<LayerCache> layersCache;
        try{
            layersCache = getLayerCaches(userLogin, layerNames);
        } catch (CstlServiceException ex) {
            return handleExceptions(getMap, errorInImage, errorBlank, ex, LAYER_NOT_DEFINED,  KEY_LAYERS.toLowerCase());
        }
        for (LayerCache layer : layersCache) {
            if (!layer.isQueryable(ServiceDef.Query.WMS_ALL)) {
                throw new CstlServiceException("You are not allowed to request the layer \""+
                        layer.getName() +"\".", LAYER_NOT_QUERYABLE, KEY_LAYERS.toLowerCase());
            }
        }

        // 1. VIEW
        final Envelope refEnv = buildRequestedViewEnvelope(getMap, layersCache);
        final double azimuth = getMap.getAzimuth();

        // 1. SCENE
        //       -- build an equivalent style List
        //TODO: clean up the SLD vs. style logic
        final List<String> styleNames = getMap.getStyles();
        final StyledLayerDescriptor sld = getMap.getSld();

        List<org.opengis.style.Style> styles;
        try {
            styles = getStyles(layersCache, sld, styleNames);
        } catch (CstlServiceException ex) {
            return handleExceptions(getMap, errorInImage, errorBlank, ex, STYLE_NOT_DEFINED, null);
        }

        final Map<String, Object> extraParams = getMap.getParameters();
        
        // Build additional filters
        List<Filter> extraFilters = extractAdditionalFilters(getMap);

        final SceneDef sdef = new SceneDef();
        sdef.extensions().add(mapPortrayal.getExtension());
        final Hints hints = mapPortrayal.getHints();
        if (hints != null) {
            /*
             * HACK we set anti-aliasing to false for gif
             */
            if (MimeType.IMAGE_GIF.equals(getMap.getFormat())) {
                hints.put(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);
            }
            sdef.getHints().putAll(hints);
        }

        try {
            final MapLayers context = mapBusiness.createContext(layersCache, styles, Collections.EMPTY_LIST, extraFilters, refEnv, extraParams);
            sdef.setContext(context);
        } catch (ConstellationStoreException ex) {
            return handleExceptions(getMap, errorInImage, errorBlank, ex, NO_APPLICABLE_CODE, null);
        }
        
        // 3. CANVAS
        final Dimension canvasDimension = getMap.getSize();
        final Color background;
        if (getMap.getTransparent() && !NO_TRANSPARENT_FORMAT.contains(getMap.getFormat())) {
            background = null;
        } else {
            final Color color = getMap.getBackground();
            background = (color == null) ? Color.WHITE : color;
        }

        CanvasDef cdef = new CanvasDef(canvasDimension,refEnv);
        cdef.setBackground(background);
        cdef.setAzimuth(azimuth);
        cdef = adaptToVersion(cdef, WMSVersion.getVersion(queryVersion));

        // 4. IMAGE
        final String mime = getMap.getFormat();
        final OutputDef odef = mapPortrayal.getOutputDef(mime);

        final PortrayalResponse response = new PortrayalResponse(cdef, sdef, odef);
        if (!mapPortrayal.isCoverageWriter() && DefaultPortrayalService.isImageFormat(odef.getMime())) {
            try {
                response.prepareNow();
            } catch (PortrayalException ex) {
                return handleExceptions(getMap, errorInImage, errorBlank, ex, NO_APPLICABLE_CODE, null);
            }
        }

        return response;
    }

    /**
     * Try to force easting/northing orientation on older WMS versions (< 1.3.0).
     * In 1.3.0, the standard clearly states that the service must interpret CRS codes according to their definition,
     * i.e. it must respect axis order.
     * However, on older WMS versions, (1.1.0), the standard document enforce east/north order on read systems.
     */
    private CanvasDef adaptToVersion(@NonNull CanvasDef canvas, @NonNull WMSVersion version) throws CstlServiceException {
        if (version.compareTo(WMSVersion.v130) >= 0) return canvas;

        try {
            //force longitude first
            return canvas.setLongitudeFirst();
        } catch (TransformException | FactoryException ex) {
            throw new CstlServiceException(ex, NO_APPLICABLE_CODE);
        }
    }

    /**
     * Extract additional filters contained in the request.
     * It will extract the filters in 3 way:
     * - extra dimension filter passed through parameters starting with "dim_"
     * - filter passed in XML the "filter" parameter
     * - filter passed in CQL the "cql_filter" parameter
     *
     * @param getMap A getMap/getFeatureInfo request.
     *
     * @return A filter list.
     * @throws CstlServiceException If an error occurs in filter parsing.
     */
    private List<Filter> extractAdditionalFilters(GetMap getMap) throws CstlServiceException {
        // Build additional filters
        List<Filter> extraFilters = new ArrayList<>();

        Map<String, Object> extraParams = getMap.getParameters();
        extraFilters.addAll(MapUtils.resolveExtraFilters(extraParams));

        // - extract  either cql_filter or filter (mutually exclusive)
        List<Filter> filters = (List<Filter>) extraParams.getOrDefault(WMSConstant.KEY_FILTER, Collections.EMPTY_LIST);
        for (Filter f : filters) {
            f = transformJAXBFilter(f, null, null, "1.1.0");
            if (f != null) {
                extraFilters.add(f);
            }
        }
        List<String> cqlFilters = (List<String>) extraParams.getOrDefault(WMSConstant.KEY_CQL_FILTER, Collections.EMPTY_LIST);
        for (String cqlFilter : cqlFilters) {
            try {
                extraFilters.add(CQL.parseFilter(cqlFilter));
            } catch (CQLException e) {
                throw new CstlServiceException("Error while parsing CQL filter", e);
            }
        }
        return extraFilters;
    }

    /**
     * Build request view envelope from request parameters and requested layers.
     * Limitation : generate an envelope only with TIME and ELEVATION dimensions, all layers default values
     * ar merge in one range instead of request each layers with his default value.
     *
     * TODO support cases defined in WMS spec. Annexe C and D. See CSTL-1245.
     *
     * @param request GetMap based request (GetMap and GetFeatureInfo)
     * @param layers all layers requested
     * @return view Envelope 2D, 3D or 4D depending of dimensions of layers and request.
     * @throws CstlServiceException
     */
    private Envelope buildRequestedViewEnvelope(GetMap request, List<LayerCache> layers) throws CstlServiceException {
        final Envelope refEnv;
        try {
            /*check envelope has positive span only if not a GetFeatureInfo request.
            if (!(request instanceof GetFeatureInfo)) {
                if (request.getEnvelope2D().getLowerCorner().getOrdinate(0) > request.getEnvelope2D().getUpperCorner().getOrdinate(0) ||
                        request.getEnvelope2D().getLowerCorner().getOrdinate(1) > request.getEnvelope2D().getUpperCorner().getOrdinate(1)) {
                    throw new CstlServiceException("BBOX parameter minimum is greater than the maximum", INVALID_PARAMETER_VALUE, KEY_BBOX.toLowerCase());
                }
            }*/

            final Date[] time = new Date[2];
            final List<Date> times = request.getTime();
            if (times != null && !times.isEmpty()) {
                time[0] = times.get(0);
                time[1] = times.get(times.size()-1);
            } else {
                /*
                    By default, select default time of first layer. Maybe we should
                    not create a 3D envelope, and let renderer get default slices.
                 */
                for (LayerCache layer : layers) {
                    try {
                        SortedSet<Date> dates = layer.getDateRange();
                        final Date last = (dates != null && !dates.isEmpty()) ? dates.last() : null;
                        if (last != null) {
                            time[0] = time[1] = last;
                            break;
                        }
                    } catch(ConstellationStoreException ex) {
                        LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                    }
                }
            }

            final Double[] vertical = new Double[2];
            final Double requestElevation = request.getElevation();
            VerticalCRS  vCrs;
            if (requestElevation != null) {
                vertical[0] = vertical[1] = requestElevation;
                vCrs = CRS.getVerticalComponent(layers.get(0).getCoordinateReferenceSystem(), true);
            } else {
                vCrs = null;
                /*
                    By default, select default elevation of first layer. Maybe we should
                    not create a 3D envelope, and let renderer get default slices.
                 */
                for (LayerCache layer : layers) {
                    final Number first = layer.getFirstElevation();
                    if (first != null) {
                        vertical[0] = vertical[1] = first.doubleValue();
                        vCrs = CRS.getVerticalComponent(layer.getCoordinateReferenceSystem(), true);
                        break;
                    }
                }
            }

            // generate view envelope with 2D, time and vertical values.
            // TODO add other dimensions (see CSTL-1245).
            refEnv = combine(request.getEnvelope2D(), time, vertical, vCrs);
        } catch (ConstellationStoreException | FactoryException ex) {
            throw new CstlServiceException(ex);
        }
        return refEnv;
    }

    private PortrayalResponse handleExceptions(GetMap getMap, boolean errorInImage, boolean errorBlank,
                                               Exception ex, OWSExceptionCode expCode, String locator) throws CstlServiceException {
        if (errorInImage) {
            if (!OWSExceptionCode.LAYER_NOT_DEFINED.equals(expCode)) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            }
            BufferedImage img = CstlPortrayalService.getInstance().writeInImage(ex, getMap.getSize());
            Boolean trs = getMap.getTransparent();
            if (Boolean.FALSE.equals(trs)) {
                //force background
                final BufferedImage buffer = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
                final Color exColor = getMap.getBackground() != null ? getMap.getBackground() : Color.WHITE;
                final Graphics2D g = buffer.createGraphics();
                g.setColor(exColor);
                g.fillRect(0, 0, buffer.getWidth(), buffer.getHeight());
                g.drawImage(img, 0, 0, null);
                img = buffer;
            }
            return new PortrayalResponse(img);

        } else if (errorBlank) {
            Color exColor = getMap.getBackground() != null ? getMap.getBackground() : Color.WHITE;
            if (getMap.getTransparent()) {
                exColor = new Color(0x00FFFFFF & exColor.getRGB(), true); //mark alpha bit as 0 to make color transparent
            }
            if (!OWSExceptionCode.LAYER_NOT_DEFINED.equals(expCode)) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            }
            return new PortrayalResponse(CstlPortrayalService.getInstance().writeBlankImage(exColor, getMap.getSize()));
        } else {
            if (locator != null) {
                throw new CstlServiceException(ex, expCode, locator);
            } else {
                throw new CstlServiceException(ex, expCode);
            }
        }
    }

    private org.opengis.style.Style extractStyle(final GenericName layerName, final List<StyleReference> layerStyles, final StyledLayerDescriptor sld) throws CstlServiceException{
        if(sld == null){
            throw new IllegalArgumentException("SLD should not be null");
        }

        final List<MutableNamedLayer> emptyNameSLDLayers = new ArrayList<>();
        for(final org.opengis.sld.Layer sldLayer : sld.layers()){
            // We can't do anything if it is not a MutableNamedLayer.
            if (!(sldLayer instanceof MutableNamedLayer)) {
                continue;
            }
            final MutableNamedLayer mnl = (MutableNamedLayer) sldLayer;
            final String sldLayerName = mnl.getName();
            // We store this sld layer, for the case all styles defined in the sld would
            // be associated to no layer.
            if (sldLayerName == null || sldLayerName.isEmpty()) {
                emptyNameSLDLayers.add(mnl);
                continue;
            }
            // If it matches, then we return it.
            if (layerName.tip().toString().equals(sldLayerName)) {
                for (final MutableLayerStyle mls : mnl.styles()) {
                    if (mls instanceof MutableNamedStyle mns) {
                        final String namedStyle = mns.getName();
                        final StyleReference styleRef = Util.findStyleReference(namedStyle, layerStyles);
                        return getStyle(styleRef);
                    } else if (mls instanceof org.opengis.style.Style ms) {
                        return ms;
                    }

                }
            }
        }

        //no valid style found, returns the first one that do not specify a layer on which to apply.
        LOGGER.log(Level.INFO, "No layer {0} found for the styles defined in the given SLD file.", layerName);
        if (!emptyNameSLDLayers.isEmpty()) {
            LOGGER.info("Continue with the first style read in the SLD, that do not specify any layer on which to apply.");
            return (org.opengis.style.Style) ((MutableNamedLayer)sld.layers().get(0)).styles().get(0);
        }
        return null;
    }

    private List<org.opengis.style.Style> getStyles(final List<LayerCache> layers, final StyledLayerDescriptor sld, final List<String> styleNames) throws CstlServiceException {
        final List<org.opengis.style.Style> results = new ArrayList<>();
        int i = 0;
        for (LayerCache layer : layers) {
            final List<StyleReference> styles = layer.getStyles();
            final org.opengis.style.Style style;
            if (sld != null) {
                //try to use the provided SLD
                style = extractStyle(layer.getName(), styles, sld);
            } else if (styleNames != null && styleNames.size() > i && styleNames.get(i) != null && !styleNames.get(i).isEmpty()) {
                //try to grab the style if provided
                //a style has been given for this layer, try to use it
                final String namedStyle = styleNames.get(i);
                final StyleReference styleRef = Util.findStyleReference(namedStyle, styles);
                if (styleRef == null) {
                    style = null;
                } else {
                    style = getStyle(styleRef);
                }
                if (style == null) {
                    throw new CstlServiceException("Style provided not found.", STYLE_NOT_DEFINED);
                }
            } else {
                //no defined styles, use the favorite one, let the layer get it himself.
                if (!styles.isEmpty()) {
                    final StyleReference styleRef = styles.get(0);
                    style = getStyle(styleRef);
                } else {
                    style = null;
                }
            }
            results.add(style);

            i++;
        }
        return results;
    }

    /**
     * Overriden from AbstractWorker because the behaviour is different when the request updateSequence
     * is equal to the current.
     *
     * @param updateSequence
     * @return
     * @throws CstlServiceException
     */
    @Override
    protected boolean returnUpdateSequenceDocument(final String updateSequence) throws CstlServiceException {
        if (updateSequence == null) {
            return false;
        }
        try {
            final long sequenceNumber = Long.parseLong(updateSequence);
            final long currentUpdateSequence = Long.parseLong(getCurrentUpdateSequence());
            if (sequenceNumber == currentUpdateSequence) {
                throw new CstlServiceException("The update sequence parameter is equal to the current", CURRENT_UPDATE_SEQUENCE, "updateSequence");
            } else if (sequenceNumber > currentUpdateSequence) {
                throw new CstlServiceException("The update sequence parameter is invalid (higher value than the current)", INVALID_UPDATE_SEQUENCE, "updateSequence");
            }
            return false;
        } catch(NumberFormatException ex) {
            throw new CstlServiceException("The update sequence must be an integer", ex, INVALID_PARAMETER_VALUE, "updateSequence");
        }
    }
}
