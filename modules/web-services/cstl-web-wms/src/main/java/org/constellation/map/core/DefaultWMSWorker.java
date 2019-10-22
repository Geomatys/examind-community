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
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.measure.Range;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.crs.DefaultEngineeringCRS;
import org.apache.sis.referencing.cs.AbstractCS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis;
import org.apache.sis.referencing.datum.DefaultEngineeringDatum;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.xml.MarshallerPool;

import org.constellation.api.ServiceDef;
import org.constellation.exception.ConstellationException;
import org.constellation.dto.service.config.wxs.AttributionType;
import org.constellation.exception.ConfigurationException;
import org.constellation.dto.service.config.wxs.DimensionDefinition;
import org.constellation.dto.service.config.wxs.FormatURL;
import org.constellation.dto.service.config.wxs.Layer;
import org.constellation.dto.service.config.wxs.LayerContext;
import org.constellation.dto.Reference;
import org.constellation.dto.portrayal.WMSPortrayal;
import org.constellation.dto.contact.Details;
import org.constellation.map.featureinfo.FeatureInfoFormat;
import org.constellation.map.featureinfo.FeatureInfoUtilities;
import org.constellation.portrayal.PortrayalUtil;
import org.constellation.portrayal.PortrayalResponse;
import org.constellation.provider.CoverageData;
import org.constellation.provider.Data;
import org.constellation.util.DataReference;
import org.constellation.ws.CstlServiceException;
import org.constellation.ws.LayerWorker;
import org.constellation.ws.MimeType;
import org.geotoolkit.cql.CQL;
import org.geotoolkit.cql.CQLException;
import org.geotoolkit.data.query.QueryBuilder;
import org.geotoolkit.display.PortrayalException;
import org.geotoolkit.display2d.ext.legend.LegendTemplate;
import org.geotoolkit.display2d.service.CanvasDef;
import org.geotoolkit.display2d.service.OutputDef;
import org.geotoolkit.display2d.service.SceneDef;
import org.geotoolkit.display2d.service.ViewDef;
import org.geotoolkit.factory.Hints;
import org.geotoolkit.inspire.xml.vs.ExtendedCapabilitiesType;
import org.geotoolkit.inspire.xml.vs.LanguageType;
import org.geotoolkit.inspire.xml.vs.LanguagesType;
import org.geotoolkit.map.FeatureMapLayer;
import org.geotoolkit.map.MapContext;
import org.geotoolkit.map.MapItem;
import org.geotoolkit.map.MapLayer;
import org.geotoolkit.ows.xml.OWSExceptionCode;
import org.geotoolkit.referencing.ReferencingUtilities;
import org.geotoolkit.se.xml.v110.OnlineResourceType;
import org.geotoolkit.sld.MutableLayer;
import org.geotoolkit.sld.MutableLayerStyle;
import org.geotoolkit.sld.MutableNamedLayer;
import org.geotoolkit.sld.MutableNamedStyle;
import org.geotoolkit.sld.MutableStyledLayerDescriptor;
import org.geotoolkit.sld.xml.GetLegendGraphic;
import org.geotoolkit.sld.xml.StyleXmlIO;
import org.geotoolkit.sld.xml.v110.DescribeLayerResponseType;
import org.geotoolkit.sld.xml.v110.LayerDescriptionType;
import org.geotoolkit.sld.xml.v110.TypeNameType;
import org.geotoolkit.style.MutableStyle;
import org.geotoolkit.temporal.util.PeriodUtilities;
import org.geotoolkit.wms.xml.AbstractBoundingBox;
import org.geotoolkit.wms.xml.AbstractDimension;
import org.geotoolkit.wms.xml.AbstractGeographicBoundingBox;
import org.geotoolkit.wms.xml.AbstractLayer;
import org.geotoolkit.wms.xml.AbstractLegendURL;
import org.geotoolkit.wms.xml.AbstractLogoURL;
import org.geotoolkit.wms.xml.AbstractOnlineResource;
import org.geotoolkit.wms.xml.AbstractRequest;
import org.geotoolkit.wms.xml.AbstractWMSCapabilities;
import org.geotoolkit.wms.xml.DescribeLayer;
import org.geotoolkit.wms.xml.GetCapabilities;
import org.geotoolkit.wms.xml.GetFeatureInfo;
import org.geotoolkit.wms.xml.GetMap;
import org.geotoolkit.wms.xml.WMSMarshallerPool;
import org.geotoolkit.wms.xml.v111.LatLonBoundingBox;
import org.geotoolkit.wms.xml.v130.Capability;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.datum.EngineeringDatum;
import org.opengis.referencing.operation.TransformException;
import org.opengis.sld.StyledLayerDescriptor;
import org.opengis.util.FactoryException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import javax.inject.Named;
import javax.measure.Unit;
import javax.xml.bind.JAXBException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.internal.referencing.GeodeticObjectBuilder;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.IdentifiedObjects;

import static org.constellation.api.CommonConstants.DEFAULT_CRS;
import static org.constellation.map.core.WMSConstant.EXCEPTION_111_BLANK;
import static org.constellation.map.core.WMSConstant.EXCEPTION_111_INIMAGE;
import static org.constellation.map.core.WMSConstant.EXCEPTION_130_BLANK;
import static org.constellation.map.core.WMSConstant.EXCEPTION_130_INIMAGE;
import static org.constellation.map.core.WMSConstant.KEY_BBOX;
import static org.constellation.map.core.WMSConstant.KEY_ELEVATION;
import static org.constellation.map.core.WMSConstant.KEY_EXTRA_PARAMETERS;
import static org.constellation.map.core.WMSConstant.KEY_LAYER;
import static org.constellation.map.core.WMSConstant.KEY_LAYERS;
import static org.constellation.map.core.WMSConstant.KEY_TIME;
import static org.geotoolkit.ows.xml.OWSExceptionCode.CURRENT_UPDATE_SEQUENCE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_POINT;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_FORMAT;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_UPDATE_SEQUENCE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.LAYER_NOT_DEFINED;
import static org.geotoolkit.ows.xml.OWSExceptionCode.LAYER_NOT_QUERYABLE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.NO_APPLICABLE_CODE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.STYLE_NOT_DEFINED;
import static org.geotoolkit.wms.xml.WmsXmlFactory.createBoundingBox;
import static org.geotoolkit.wms.xml.WmsXmlFactory.createDimension;
import static org.geotoolkit.wms.xml.WmsXmlFactory.createGeographicBoundingBox;
import static org.geotoolkit.wms.xml.WmsXmlFactory.createLayer;
import static org.geotoolkit.wms.xml.WmsXmlFactory.createLegendURL;
import static org.geotoolkit.wms.xml.WmsXmlFactory.createLogoURL;
import static org.geotoolkit.wms.xml.WmsXmlFactory.createOnlineResource;
import static org.geotoolkit.wms.xml.WmsXmlFactory.createStyle;
import org.opengis.util.GenericName;
import org.constellation.dto.StyleReference;
import org.constellation.portrayal.CstlPortrayalService;
import org.constellation.util.DtoToOGCFilterTransformer;
import org.constellation.util.Util;
import org.constellation.dto.service.config.wxs.FilterAndDimension;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.provider.GeoData;
import org.geotoolkit.display2d.ext.legend.DefaultLegendService;
import org.geotoolkit.filter.FilterFactoryImpl;
import org.opengis.feature.FeatureType;
import org.opengis.referencing.crs.VerticalCRS;

/**
 * A WMS worker for a local WMS service which handles requests from either REST
 * or SOAP facades and issues appropriate responses.
 * <p>
 * The classes implementing the REST or SOAP facades to this service will have
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

    /**
     * Only Elevation dimension.
     */
    private static final List<String> VERTICAL_DIM = UnmodifiableArrayList.wrap(new String[] {"UP", "DOWN"});

    /**
     * List of FeatureInfo mimeTypes
     */
    private final List<String> GFI_MIME_TYPES = new ArrayList<>();

    private WMSPortrayal mapPortrayal;
    public DefaultWMSWorker(final String id) {
        super(id, ServiceDef.Specification.WMS);

        //get all supported GetFeatureInfo mimetypes
        try {
            GFI_MIME_TYPES.clear();
            final LayerContext config = (LayerContext)getConfiguration();
            GFI_MIME_TYPES.addAll(FeatureInfoUtilities.allSupportedMimeTypes(config));
        } catch (ConfigurationException | ClassNotFoundException ex) {
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

        if (isStarted) {
            LOGGER.log(Level.INFO, "WMS worker {0} running", id);
        }
    }

    @Override
    protected MarshallerPool getMarshallerPool() {
        return WMSMarshallerPool.getInstance();
    }

    /**
     * Return a description of layers specified in the user's request.
     *
     * TODO: Does this actually do anything? why does this never access LayerDetails?
     * TODO: Is this broken?
     *
     * @param descLayer The {@linkplain DescribeLayer describe layer} request.
     * @return a description of layers specified in the user's request.
     *
     * @throws CstlServiceException
     */
    @Override
    public DescribeLayerResponseType describeLayer(final DescribeLayer descLayer) throws CstlServiceException {
        final OnlineResourceType or = new OnlineResourceType(getServiceUrl());

        final List<LayerDescriptionType> layerDescriptions = new ArrayList<>();
        final List<String> layerNames = descLayer.getLayers();
        for (String layerName : layerNames) {
            final TypeNameType t = new TypeNameType(layerName.trim());
            final LayerDescriptionType outputLayer = new LayerDescriptionType(or, t);
            layerDescriptions.add(outputLayer);
        }
        return new DescribeLayerResponseType("1.1.1", layerDescriptions);
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
        } else if (requestedLanguage == null && defaultLanguage != null) {
            currentLanguage = defaultLanguage;
        } else {
            currentLanguage = null;
        }

        final Object cachedCapabilities = getCapabilitiesFromCache(queryVersion, currentLanguage);
        if (cachedCapabilities != null) {
            return (AbstractWMSCapabilities) cachedCapabilities;
        }

        final Details skeleton = getStaticCapabilitiesObject("wms", currentLanguage);
        final AbstractWMSCapabilities inCapabilities = WMSConstant.createCapabilities(queryVersion, skeleton);

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

        //set the current updateSequence parameter
        final boolean returnUS = returnUpdateSequenceDocument(getCapab.getUpdateSequence());
        if (returnUS) {
            throw new CstlServiceException("the update sequence parameter is equal to the current", CURRENT_UPDATE_SEQUENCE, "updateSequence");
        }

        //Build the list of layers
        final List<AbstractLayer> outputLayers = new ArrayList<>();
        final List<Layer> layers = getConfigurationLayers(userLogin);

       for (Layer configLayer : layers) {
            final Data layer = getLayerReference(configLayer);

            if (layer == null) {
                LOGGER.log(Level.WARNING, "Unable to find a provider data correspounding to layer:{0}", configLayer.getIdentifier());
                continue;
            }

            if (!layer.isQueryable(ServiceDef.Query.WMS_ALL)) {
                continue;
            }

            // Get default CRS for the layer supported crs.
            final Envelope layerNativeEnv;
            try {
                layerNativeEnv = layer.getEnvelope();
                if (layerNativeEnv == null) {
                    LOGGER.log(Level.WARNING, "Cannot get envelope for layer {0}  (null)", layer);
                    continue;
                }
            } catch (ConstellationStoreException ex) {
                LOGGER.log(Level.WARNING, ex, () -> "Cannot get envelope for layer "+layer);
                continue;
            }

            String nativeCrs = null;
            try {
               CoordinateReferenceSystem crs = layerNativeEnv.getCoordinateReferenceSystem();
               if (crs != null) {
                   final Integer epsgCode = IdentifiedObjects.lookupEPSG(crs);
                   if (epsgCode != null) {
                       nativeCrs = "EPSG:" + epsgCode;
                   }
               }
           } catch (FactoryException ex) {
               LOGGER.log(Level.INFO, "Error retrieving data crs for the layer :" + layer.getName(), ex);
           }

            GeographicBoundingBox inputGeoBox;
            try {
                inputGeoBox = layer.getGeographicBoundingBox();
            } catch (ConstellationStoreException exception) {
                throw new CstlServiceException(exception, NO_APPLICABLE_CODE);
            }

            if (inputGeoBox == null) {
                // The layer does not contain geometric information, we do not want this layer
                // in the capabilities response.
                continue;
            }

            // We ensure that the data envelope is not empty. It can occurs with vector data, on a single point.
            final double width  = inputGeoBox.getEastBoundLongitude() - inputGeoBox.getWestBoundLongitude();
            final double height = inputGeoBox.getNorthBoundLatitude() - inputGeoBox.getSouthBoundLatitude();
            if (width == 0 && height == 0) {
                final double diffWidth = Math.nextUp(inputGeoBox.getEastBoundLongitude()) - inputGeoBox.getEastBoundLongitude();
                final double diffHeight = Math.nextUp(inputGeoBox.getNorthBoundLatitude()) - inputGeoBox.getNorthBoundLatitude();
                inputGeoBox = new LatLonBoundingBox(inputGeoBox.getWestBoundLongitude() - diffWidth,
                                                    inputGeoBox.getSouthBoundLatitude() - diffHeight,
                                                    Math.nextUp(inputGeoBox.getEastBoundLongitude()),
                                                    Math.nextUp(inputGeoBox.getNorthBoundLatitude()));
            }
            if (width == 0) {
                final double diffWidth = Math.nextUp(inputGeoBox.getEastBoundLongitude()) - inputGeoBox.getEastBoundLongitude();
                inputGeoBox = new LatLonBoundingBox(inputGeoBox.getWestBoundLongitude() - diffWidth, inputGeoBox.getSouthBoundLatitude(),
                        Math.nextUp(inputGeoBox.getEastBoundLongitude()), inputGeoBox.getNorthBoundLatitude());
            }
            if (height == 0) {
                final double diffHeight = Math.nextUp(inputGeoBox.getNorthBoundLatitude()) - inputGeoBox.getNorthBoundLatitude();
                inputGeoBox = new LatLonBoundingBox(inputGeoBox.getWestBoundLongitude(), inputGeoBox.getSouthBoundLatitude() - diffHeight,
                        inputGeoBox.getEastBoundLongitude(), Math.nextUp(inputGeoBox.getNorthBoundLatitude()));
            }
            // fix for overlapping box
            if (inputGeoBox.getWestBoundLongitude() > inputGeoBox.getEastBoundLongitude()) {
                inputGeoBox = new LatLonBoundingBox(-180, inputGeoBox.getSouthBoundLatitude(),
                                                     180, inputGeoBox.getNorthBoundLatitude());
            }

            // List of elevations, times and dim_range values.
            final List<AbstractDimension> dimensions = new ArrayList<>();

            /*
             * Dimension: the available date
             */
            try {
                final SortedSet<Date> dates = layer.getAvailableTimes();
                if (!dates.isEmpty()) {
                    final PeriodUtilities periodFormatter = new PeriodUtilities(ISO8601_FORMAT);
                    final String defaut = ISO8601_FORMAT.format(dates.last());
                    AbstractDimension dim = createDimension(queryVersion, "time", "ISO8601", defaut, null);
                    dim.setValue(periodFormatter.getDatesRespresentation(dates));
                    dimensions.add(dim);
                }
            } catch (ConstellationStoreException ex) {
                LOGGER.log(Level.WARNING, "Error retrieving dates values for the layer :"+ layer.getName(), ex);
            }

            /*
             * Dimension: the available elevation
             */
            try {
               final SortedSet<Number> elevations = layer.getAvailableElevations();
               if (!elevations.isEmpty()) {
                   // Define elevation unit as a CRS identifier. See Annex C.2
                   String unit = null;
                   try {
                       final VerticalCRS vCrs = CRS.getVerticalComponent(layerNativeEnv.getCoordinateReferenceSystem(), true);
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
             * Dimension: the dimension range
             */
            final MeasurementRange<?>[] ranges = layer.getSampleValueRanges();
            /* If the layer has only one sample dimension, then we can apply the dim_range
             * parameter. Otherwise it can be a multiple sample dimensions layer, and we
             * don't apply the dim_range.
             */
            if (ranges != null && ranges.length == 1 && ranges[0] != null) {
                final MeasurementRange<?> firstRange = ranges[0];
                final double minRange = firstRange.getMinDouble();
                final double maxRange = firstRange.getMaxDouble();
                final String defaut = minRange + "," + maxRange;
                final Unit<?> u = firstRange.unit();
                final String unit = (u != null) ? u.toString() : null;
                String unitSymbol;
                try {
                    unitSymbol = new org.apache.sis.measure.UnitFormat(Locale.UK).format(u);
                } catch (IllegalArgumentException e) {
                    // Workaround for one more bug in javax.measure...
                    unitSymbol = unit;
                }
                AbstractDimension dim = createDimension(queryVersion, minRange + "," + maxRange, "dim_range", unit,unitSymbol, defaut, null, null, null);
                dimensions.add(dim);
            }

            //-- execute only if it is a CoverageData
            if (layer instanceof CoverageData) {
                final CoverageData covdata = (CoverageData) layer;
                try {
                    for (org.constellation.dto.Dimension d : covdata.getSpecialDimensions()) {
                        dimensions.add(createDimension(queryVersion, d.getValue(), d.getName(), d.getUnits(),
                                d.getUnitSymbol(), d.getDefault(), null, null, null));
                    }
                } catch (ConstellationStoreException ex) {
                    LOGGER.log(Level.INFO, ex.getMessage(), ex);
                    break;
                }
            }

            // Verify extra dimensions
            if (!configLayer.getDimensions().isEmpty()) {
               if (layer instanceof GeoData) {
                   final GeoData geoLayer = (GeoData) layer;
                   try {
                       final MapItem mi = geoLayer.getMapLayer(null, null);
                       applyLayerFiltersAndDims(mi, userLogin);

                       if (mi instanceof MapContext) {
                           final MapContext mc = (MapContext) mi;
                           final List<AbstractDimension> dimensionsToAdd = new ArrayList<>();
                           for (final MapLayer candidateLayer : mc.layers()) {
                               if (candidateLayer instanceof FeatureMapLayer) {
                                   final FeatureMapLayer fml = (FeatureMapLayer) candidateLayer;
                                   final List<AbstractDimension> extraDimsToAdd = getExtraDimensions(fml, queryVersion);
                                   for (AbstractDimension newExtraDim : extraDimsToAdd) {
                                       boolean exist = false;
                                       for (AbstractDimension oldExtraDim : dimensionsToAdd) {
                                           if (oldExtraDim.getName().equalsIgnoreCase(newExtraDim.getName())) {
                                               mergeValues(oldExtraDim, newExtraDim);
                                               exist = true;
                                               break;
                                           }
                                       }
                                       if (!exist) {
                                           dimensionsToAdd.add(newExtraDim);
                                       }
                                   }
                               }
                           }

                           if (!dimensionsToAdd.isEmpty()) {
                               dimensions.addAll(dimensionsToAdd);
                           }
                       }

                       if (mi instanceof FeatureMapLayer) {
                           final FeatureMapLayer fml = (FeatureMapLayer) mi;
                           dimensions.addAll(getExtraDimensions(fml, queryVersion));
                       }

                   } catch (ConstellationStoreException | DataStoreException ex) {
                       LOGGER.log(Level.INFO, ex.getMessage(), ex);
                       break;
                   }
               }
           }

            /*
             * LegendUrl generation
             * TODO: Use a StringBuilder or two
             */
            final String layerName;

            //Use layer alias from config if exist.
            if (configLayer.getAlias() != null && !configLayer.getAlias().isEmpty()) {
                layerName = configLayer.getAlias();
            } else {
                // If not, we fallback on layer strict name.
                final QName fullName = configLayer.getName();
                final String ns = fullName.getNamespaceURI().trim();
                if (ns != null && !ns.isEmpty()) {
                    layerName = ns + ':' + fullName.getLocalPart();
                } else {
                    layerName = fullName.getLocalPart();
                }
            }
            final String beginLegendUrl = getServiceUrl() + "REQUEST=GetLegendGraphic&VERSION=1.1.1&FORMAT=";
            final String legendUrlGif   = beginLegendUrl + MimeType.IMAGE_GIF + "&LAYER=" + layerName;
            final String legendUrlPng   = beginLegendUrl + MimeType.IMAGE_PNG + "&LAYER=" + layerName;
            final String queryable      = (layer.isQueryable(ServiceDef.Query.WMS_GETINFO)) ? "1" : "0";
            final String _abstract;
            final String keyword;
            if (layer instanceof CoverageData) {
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
                        inputGeoBox.getNorthBoundLatitude(), 0.0, 0.0);

                if (nativeCrs != null && layerNativeEnv.getCoordinateReferenceSystem() != null) {
                    try {
                        final Envelope rightHanded = Envelopes.transform(layerNativeEnv,
                                AbstractCRS.castOrCopy(layerNativeEnv.getCoordinateReferenceSystem()).forConvention(AxesConvention.RIGHT_HANDED));
                        nativeBBox = createBoundingBox(queryVersion,
                            nativeCrs,
                            rightHanded.getMinimum(0),
                            rightHanded.getMinimum(1),
                            rightHanded.getMaximum(0),
                            rightHanded.getMaximum(1), 0.0, 0.0);
                    } catch (TransformException ex) {
                        LOGGER.log(Level.INFO, "Error retrieving data crs for the layer :"+ layer.getName(), ex);
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
                            inputGeoBox.getEastBoundLongitude(), 0.0, 0.0);

                if(nativeCrs!=null){
                    nativeBBox = createBoundingBox(queryVersion,
                        nativeCrs,
                        layerNativeEnv.getMinimum(0),
                        layerNativeEnv.getMinimum(1),
                        layerNativeEnv.getMaximum(0),
                        layerNativeEnv.getMaximum(1), 0.0, 0.0);
                }

            }
            // we build a Style Object
            final List<StyleReference> stylesName = configLayer.getStyles();
            final List<org.geotoolkit.wms.xml.Style> styles = new ArrayList<>();
            if (stylesName != null && !stylesName.isEmpty()) {
                // For each styles defined for the layer, get the dimension of the getLegendGraphic response.
                for (StyleReference styleName : stylesName) {
                    final MutableStyle ms = getStyle(styleName);
                    String legendUrlPng2 =  legendUrlPng+"&STYLE="+ styleName.getName();
                    String legendUrlGif2 =  legendUrlGif+"&STYLE="+ styleName.getName();
                    final org.geotoolkit.wms.xml.Style style = convertMutableStyleToWmsStyle(queryVersion, ms, layer, legendUrlPng2, legendUrlGif2);
                    styles.add(style);
                }
            }

            //list supported crs
            final List<String> supportedCrs;
            if(nativeCrs!=null && DEFAULT_CRS.indexOf(nativeCrs)!=0){
                //we add or move to first position the native crs
                supportedCrs = new ArrayList<>(DEFAULT_CRS);
                supportedCrs.remove(nativeCrs);
                supportedCrs.add(0, nativeCrs);
            }else{
                supportedCrs = DEFAULT_CRS;
            }

            final AbstractGeographicBoundingBox bbox = createGeographicBoundingBox(queryVersion, inputGeoBox);
            final AbstractLayer outputLayerO = createLayer(queryVersion, layerName,
                    _abstract, keyword,
                    supportedCrs, bbox, outputBBox, queryable, dimensions, styles);
            if(nativeBBox!=null && !nativeBBox.getCRSCode().equals(outputBBox.getCRSCode())){
                ((List)outputLayerO.getBoundingBox()).add(0, nativeBBox);
            }

            final AbstractLayer outputLayer = customizeLayer(queryVersion, outputLayerO, configLayer, currentLanguage);
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
                    boolean isDefault = language.equals(defaultLanguage);
                    languageList.add(new LanguageType(language, isDefault));
                }
                LanguagesType languages = new LanguagesType(languageList);
                inspireExtension.setLanguages(languages);
                if (currentLanguage == null) {
                    inspireExtension.setCurrentLanguage(defaultLanguage);
                } else {
                    inspireExtension.setCurrentLanguage(currentLanguage);
                }
            }

        }
        putCapabilitiesInCache(queryVersion, currentLanguage, inCapabilities);
        return inCapabilities;
    }

    private String sortValues(final String... vals) {
        final List<String> finalVals = new ArrayList<>();
        for (final String s : vals) {
            finalVals.add(s);
        }

        boolean isDoubleValues = false;
        List<Double> finalValsDouble = null;
        try {
            Double.valueOf(finalVals.get(0));
            // It is a double!
            isDoubleValues = true;
            finalValsDouble = new ArrayList<>();
            for (String s : finalVals) {
                finalValsDouble.add(Double.valueOf(s));
            }
        } catch (NumberFormatException ex) {
        }

        if (isDoubleValues) {
            Collections.sort(finalValsDouble);
            finalVals.clear();
            for (Double d : finalValsDouble) {
                finalVals.add(String.valueOf(d));
            }
        } else {
            Collections.sort(finalVals);
        }

        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String val : finalVals) {
            if (!first) {
                sb.append(",");
            }
            sb.append(val);
            first = false;
        }

        return sb.toString();
    }

    /**
     * Merge old and new values in the old dimension. Try to sort its values.
     *
     * @param oldExtraDim
     * @param newExtraDim
     */
    private void mergeValues(final AbstractDimension oldExtraDim, final AbstractDimension newExtraDim) {
        final Set<String> valsSet = new HashSet<>();
        final String oldVals = oldExtraDim.getValue();
        final String[] oldValsSplit = oldVals.split(",");
        for (final String o : oldValsSplit) {
            valsSet.add(o);
        }

        final String newVals = newExtraDim.getValue();
        final String[] newValsSplit = newVals.split(",");
        for (final String n : newValsSplit) {
            valsSet.add(n);
        }

        final List<String> finalVals = new ArrayList<>();
        finalVals.addAll(valsSet);

        if (finalVals.isEmpty()) {
            return;
        }

        final String finalValSorted = sortValues(finalVals.toArray(new String[0]));
        oldExtraDim.setValue(finalValSorted);
    }

    /**
     * Get extra dimensions from a {@link FeatureMapLayer}.
     *
     * @param fml {@link FeatureMapLayer}
     * @param queryVersion Version of the request.
     * @return A list of extra dimensions, never {@code null}
     * @throws DataStoreException
     */
    private List<AbstractDimension> getExtraDimensions(final FeatureMapLayer fml, final String queryVersion) throws DataStoreException {
        final List<AbstractDimension> dimensions = new ArrayList<>();
        for(FeatureMapLayer.DimensionDef ddef : fml.getExtraDimensions()){
            final Collection<Range> collRefs = fml.getDimensionRange(ddef);
            // Transform it to a set in order to filter same values
            final Set<Range> refs = new HashSet<>();
            for (Range ref : collRefs) {
                refs.add(ref);
            }

            if (refs.isEmpty()) {
                // Dimension applied on a layer which has no values: just skip this dimension
                continue;
            }

            final StringBuilder values = new StringBuilder();
            int index = 0;
            for (final Range val : refs) {
                values.append(val.getMinValue());
                if(val.getMinValue().compareTo(val.getMaxValue()) != 0){
                    values.append('-');
                    values.append(val.getMaxValue());
                }
                if (index++ < refs.size()-1) {
                    values.append(",");
                }
            }

            final String sortedValues = sortValues(values.toString().split(","));
            final String unitSymbol = ddef.getCrs().getCoordinateSystem().getAxis(0).getUnit().toString();
            final String unit = unitSymbol;
            final String axisName = ddef.getCrs().getCoordinateSystem().getAxis(0).getName().getCode();
            final String defaut = "";

            final AbstractDimension dim = (queryVersion.equals(ServiceDef.WMS_1_1_1_SLD.version.toString())) ?
                new org.geotoolkit.wms.xml.v111.Dimension(sortedValues, axisName, unit,
                    unitSymbol, defaut, null, null, null) :
                new org.geotoolkit.wms.xml.v130.Dimension(sortedValues, axisName, unit,
                    unitSymbol, defaut, null, null, null);

            dimensions.add(dim);
        }
        return dimensions;
    }

    /**
     * Apply the layer customization extracted from the configuration.
     *
     * @param version
     * @param outputLayer
     * @param configLayer
     * @param layerDetails
     * @param legendUrlPng
     * @param legendUrlGif
     * @return
     * @throws CstlServiceException
     */
    private AbstractLayer customizeLayer(final String version, final AbstractLayer outputLayer, final Layer configLayer, String language) throws CstlServiceException
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
     * @param layerDetails
     * @param legendUrlPng
     * @param legendUrlGif
     * @return
     */
    private org.geotoolkit.wms.xml.Style convertMutableStyleToWmsStyle(final String currentVersion, final MutableStyle ms, final Data layerDetails,
            final String legendUrlPng, final String legendUrlGif)
    {
        if (!(layerDetails instanceof GeoData)) {
            return null;
        }
        AbstractOnlineResource or = createOnlineResource(currentVersion, legendUrlPng);
        final LegendTemplate lt = mapPortrayal.getDefaultLegendTemplate();
        final Dimension dimension;
        try {
            dimension = DefaultLegendService.legendPreferredSize(lt, ((GeoData)layerDetails).getMapLayer(ms, null));
        } catch (ConstellationStoreException ex) {
            LOGGER.log(Level.INFO, ex.getLocalizedMessage(), ex);
            return null;
        }

        final AbstractLegendURL legendURL1 = createLegendURL(currentVersion, MimeType.IMAGE_PNG, or, dimension.width, dimension.height);

        or = createOnlineResource(currentVersion, legendUrlGif);
        final AbstractLegendURL legendURL2 = createLegendURL(currentVersion, MimeType.IMAGE_GIF, or, dimension.width, dimension.height);

        String styleName = ms.getName();
        if (styleName != null && !styleName.isEmpty() && styleName.startsWith("${")) {
            final DataReference dataRef = new DataReference(styleName);
            styleName = Util.getLayerId(dataRef).tip().toString();
        }
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
        // 1. SCENE
        //       -- get the List of layer references
        final String userLogin             = getUserLogin();
        final List<GenericName> layerNames        = getFI.getQueryLayers();
        final List<Data> layerRefs;
        final List<Layer> layerConfig;
        final Map<GenericName, List<StyleReference>> layerStyles;
        try {
            layerRefs = getLayerReferences(userLogin, layerNames);
            layerConfig = getConfigurationLayers(userLogin, layerNames);
            layerStyles = getLayersStyles(userLogin, layerNames);
        } catch (CstlServiceException ex) {
            throw new CstlServiceException(ex, LAYER_NOT_DEFINED, KEY_LAYERS.toLowerCase());
        }

        for (Data layer : layerRefs) {
            if (!layer.isQueryable(ServiceDef.Query.WMS_GETINFO)) {
                throw new CstlServiceException("You are not allowed to request the layer \""+
                        layer.getName() +"\".", LAYER_NOT_QUERYABLE, KEY_LAYERS.toLowerCase());
            }
        }
        //       -- build an equivalent style List
        //TODO: clean up the SLD vs. style logic
        final List<String> styleNames   = getFI.getStyles();
        final StyledLayerDescriptor sld = getFI.getSld();

        final List<MutableStyle> styles        = getStyles(layerStyles, sld, styleNames);
        //       -- create the rendering parameter Map
        final Double elevation                 = getFI.getElevation();
        final List<Date> time                  = getFI.getTime();
        final Map<String, Object> params       = new HashMap<>();
        params.put(KEY_ELEVATION, elevation);
        params.put(KEY_TIME, time);
        params.put(KEY_EXTRA_PARAMETERS, getFI.getParameters());
        final SceneDef sdef = new SceneDef();

        try {
            final MapContext context = PortrayalUtil.createContext(layerRefs, styles, params);
            sdef.setContext(context);
            //apply layercontext filters
            applyLayerFiltersAndDims(context, userLogin);
        } catch (ConstellationStoreException | DataStoreException ex) {
            throw new CstlServiceException(ex, NO_APPLICABLE_CODE);
        }

        // 2. VIEW
        final Envelope refEnv = buildRequestedViewEnvelope(getFI, layerRefs);
        final double azimuth = getFI.getAzimuth();
        final ViewDef vdef   = new ViewDef(refEnv,azimuth);
        try {
            //force longitude first
            vdef.setLongitudeFirst();
        } catch (TransformException | FactoryException ex) {
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
        final CanvasDef cdef = new CanvasDef(canvasDimension,background);

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
        Layer config = null;
        if (layerRefs.size() == 1) {
            config = layerConfig.get(0);
        }

        FeatureInfoFormat featureInfo = null;
        try {
            featureInfo = FeatureInfoUtilities.getFeatureInfoFormat(getConfiguration(), config, infoFormat);
        } catch (ClassNotFoundException | ConfigurationException ex) {
            throw new CstlServiceException(ex, NO_APPLICABLE_CODE);
        }

        if (featureInfo == null) {
            throw new CstlServiceException("INFO_FORMAT="+infoFormat+" not supported for layers : "+layerNames, INVALID_FORMAT);
        }

        try {
            //give the layerRef list used by some FIF
            featureInfo.setLayersDetails(layerRefs);
            final Object result = featureInfo.getFeatureInfo(sdef, vdef, cdef, selectionArea, getFI);
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
        final String userLogin   = getUserLogin();
        final Data data = getLayerReference(userLogin, getLegend.getLayer());
        final List<StyleReference> layerStyles = getLayerStyles(userLogin, getLegend.getLayer());
        final String layerName = data.getName().toString();
        if (!data.isQueryable(ServiceDef.Query.WMS_ALL)) {
            throw new CstlServiceException("You are not allowed to request the layer \""+
                    layerName +"\".", LAYER_NOT_QUERYABLE, KEY_LAYER.toLowerCase());
        }

        if (!(data instanceof GeoData)) {
            throw new CstlServiceException("Unable to extract legend from a non GeoData");
        }
        final GeoData layer = (GeoData) data;

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
        final String sld = getLegend.getSld();
        final String style = getLegend.getStyle();
        try {
            MutableStyle ms = null;
            // If a sld file is given, extracts the style from it.
            if (sld != null && !sld.isEmpty()) {
                final StyleXmlIO utils = new StyleXmlIO();
                final MutableStyledLayerDescriptor mutableSLD;

                try {
                    mutableSLD = utils.readSLD(new URL(sld), getLegend.getSldVersion());
                } catch (JAXBException ex) {
                    final String message;
                    if (ex.getLinkedException() instanceof FileNotFoundException) {
                        message = "The given url \""+ sld +"\" points to an non-existing file.";
                    } else {
                        message = ex.getLocalizedMessage();
                    }
                    throw new PortrayalException(message, ex);
                } catch (FactoryException ex) {
                    throw new PortrayalException(ex);
                } catch (MalformedURLException ex) {
                    throw new PortrayalException("The given SLD url \""+ sld +"\" is not a valid url", ex);
                }

                final List<MutableLayer> emptyNameMutableLayers = new ArrayList<>();
                for (final MutableLayer mutableLayer : mutableSLD.layers()) {
                    final String mutableLayerName = mutableLayer.getName();
                    if (mutableLayerName == null || mutableLayerName.isEmpty()) {
                        emptyNameMutableLayers.add(mutableLayer);
                        continue;
                    }
                    if (layerName.equals(mutableLayerName)) {
                        ms = (MutableStyle) mutableLayer.styles().get(0);
                        break;
                    }
                }
                if (ms == null) {
                    LOGGER.log(Level.INFO, "No layer {0} found for the given SLD. Continue with the first style found.", layerName);
                    ms = (MutableStyle) emptyNameMutableLayers.get(0).styles().get(0);
                }
            } else if (style != null && !style.isEmpty()) {
                for (StyleReference ref : layerStyles) {
                    if(style.equals(ref.getName())) {
                        ms = getStyle(ref);
                        break;
                    }
                }
            } else {
                // No sld given, we use the style.
                if (!layerStyles.isEmpty()) {
                    final StyleReference styleRef = layerStyles.get(0);
                    ms = getStyle(styleRef);
                }
            }
            image = WMSUtilities.getLegendGraphic(layer.getMapLayer(ms, null), dims, mapPortrayal.getDefaultLegendTemplate(), ms, rule, scale);
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


        // 1. SCENE
        //       -- get the List of layer references
        final List<GenericName> layerNames = getMap.getLayers();

        //check layer limit
        final Details skeleton = getStaticCapabilitiesObject("wms", defaultLanguage);
        if (skeleton.getServiceConstraints()!=null) {
            final int layerLimit = skeleton.getServiceConstraints().getLayerLimit();
            if(layerLimit>0 && layerLimit<layerNames.size()) {
                throw new CstlServiceException("Too many layers requested, limit is "+layerLimit);
            }
        }
        final List<Data> layerRefs;
        final Map<GenericName, List<StyleReference>> layerStyles;
        try{
            layerRefs = getLayerReferences(userLogin, layerNames);
            layerStyles = getLayersStyles(userLogin, layerNames);
        } catch (CstlServiceException ex) {
            return handleExceptions(getMap, errorInImage, errorBlank, ex, LAYER_NOT_DEFINED,  KEY_LAYERS.toLowerCase());
        }
        for (Data layer : layerRefs) {
            if (!layer.isQueryable(ServiceDef.Query.WMS_ALL)) {
                throw new CstlServiceException("You are not allowed to request the layer \""+
                        layer.getName() +"\".", LAYER_NOT_QUERYABLE, KEY_LAYERS.toLowerCase());
            }
        }
        //       -- build an equivalent style List
        //TODO: clean up the SLD vs. style logic
        final List<String> styleNames = getMap.getStyles();
        final StyledLayerDescriptor sld = getMap.getSld();

        List<MutableStyle> styles;
        try {
            styles = getStyles(layerStyles, sld, styleNames);
        } catch (CstlServiceException ex) {
            return handleExceptions(getMap, errorInImage, errorBlank, ex, STYLE_NOT_DEFINED, null);
        }
        //       -- create the rendering parameter Map
        final Map<String, Object> params = new HashMap<>();
        params.put(KEY_EXTRA_PARAMETERS, getMap.getParameters());
        final SceneDef sdef = new SceneDef();
        sdef.extensions().add(mapPortrayal.getExtension());
        final Hints hints = mapPortrayal.getHints();
        if (hints != null) {
            /*
             * HACK we set anti-aliasing to false for gif
             */
            if ("image/gif".equals(getMap.getFormat())) {
                hints.put(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);
            }
            sdef.getHints().putAll(hints);
        }

        try {
            final MapContext context = PortrayalUtil.createContext(layerRefs, styles, params);
            //apply layercontext filters
            applyLayerFiltersAndDims(context, userLogin);

            sdef.setContext(context);
        } catch (ConstellationStoreException | DataStoreException ex) {
            return handleExceptions(getMap, errorInImage, errorBlank, ex, NO_APPLICABLE_CODE, null);
        }

        // 2. VIEW
        final Envelope refEnv = buildRequestedViewEnvelope(getMap, layerRefs);
        final double azimuth = getMap.getAzimuth();
        final ViewDef vdef = new ViewDef(refEnv,azimuth);

        // 3. CANVAS
        final Dimension canvasDimension = getMap.getSize();
        final Color background;
        if (getMap.getTransparent() && !MimeType.IMAGE_JPEG.equalsIgnoreCase(getMap.getFormat())) {
            background = null;
        } else {
            final Color color = getMap.getBackground();
            background = (color == null) ? Color.WHITE : color;
        }
        final CanvasDef cdef = new CanvasDef(canvasDimension,background);

        // 4. IMAGE
        final String mime = getMap.getFormat();
        final OutputDef odef = mapPortrayal.getOutputDef(mime);

        try {
            //force longitude first
            vdef.setLongitudeFirst();
        } catch (TransformException | FactoryException ex) {
            throw new CstlServiceException(ex, NO_APPLICABLE_CODE);
        }

        final PortrayalResponse response = new PortrayalResponse(cdef, sdef, vdef, odef);
        if(!mapPortrayal.isCoverageWriter()){
            try {
                response.prepareNow();
            } catch (PortrayalException ex) {
                return handleExceptions(getMap, errorInImage, errorBlank, ex, NO_APPLICABLE_CODE, null);
            }
        }

        return response;
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
    public Envelope buildRequestedViewEnvelope(GetMap request, List<Data> layers) throws CstlServiceException {
        final Envelope refEnv;
        try {
            //check envelope has positive span only if not a GetFeatureInfo request.
            if (!(request instanceof GetFeatureInfo)) {
                if (request.getEnvelope2D().getLowerCorner().getOrdinate(0) > request.getEnvelope2D().getUpperCorner().getOrdinate(0) ||
                        request.getEnvelope2D().getLowerCorner().getOrdinate(1) > request.getEnvelope2D().getUpperCorner().getOrdinate(1)) {
                    throw new CstlServiceException("BBOX parameter minimum is greater than the maximum", INVALID_PARAMETER_VALUE, KEY_BBOX.toLowerCase());
                }
            }

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
                for (Data layer : layers) {
                    final SortedSet<Date> layerTimes = layer.getAvailableTimes();
                    if (!layerTimes.isEmpty()) {
                        time[0] = time[1] = layerTimes.first();
                        break;
                    }
                }
            }

            final Double[] vertical = new Double[2];
            final Double requestElevation = request.getElevation();
            VerticalCRS  vCrs;
            if (requestElevation != null) {
                vertical[0] = vertical[1] = requestElevation;
                vCrs = CRS.getVerticalComponent(layers.get(0).getEnvelope().getCoordinateReferenceSystem(), true);
            } else {
                vCrs = null;
                /*
                    By default, select default elevation of first layer. Maybe we should
                    not create a 3D envelope, and let renderer get default slices.
                 */
                for (Data layer : layers) {
                    final SortedSet<Number> elevations = layer.getAvailableElevations();
                    if (!elevations.isEmpty()) {
                        vertical[0] = vertical[1] = elevations.first().doubleValue();
                        vCrs = CRS.getVerticalComponent(layer.getEnvelope().getCoordinateReferenceSystem(), true);
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

    private Envelope combine(Envelope env, Date[] temporal, Double[] elevation, VerticalCRS vCrs) throws FactoryException {
        List<CoordinateReferenceSystem> crss = new ArrayList<>();
        crss.add(env.getCoordinateReferenceSystem());
        final boolean hasTime = temporal != null && (temporal[0] != null || temporal[1] != null);
        if(hasTime){
            crss.add(CommonCRS.Temporal.JAVA.crs());
        }

        final boolean hasElevation = elevation != null && (elevation[0] != null || elevation[1] != null) && vCrs != null;
        if (hasElevation) {
            crss.add(vCrs);
        }

        if (!hasTime && !hasElevation)
            return env;

        final CoordinateReferenceSystem outputCrs = new GeodeticObjectBuilder()
                    .addName("Composite")
                    .createCompoundCRS(crss.toArray(new CoordinateReferenceSystem[crss.size()]));
        GeneralEnvelope result = new GeneralEnvelope(outputCrs);
        int dim = env.getDimension();
        result.subEnvelope(0, dim).setEnvelope(env);
        if (hasTime) {
            result.setRange(dim++, (double)temporal[0].getTime(), (double)temporal[1].getTime());
        }
        if (hasElevation) {
            result.setRange(dim++, elevation[0], elevation[1]);
        }
        return result;
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

    private MutableStyle extractStyle(final GenericName layerName, final List<StyleReference> layerStyles, final StyledLayerDescriptor sld) throws CstlServiceException{
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
                    if (mls instanceof MutableNamedStyle) {
                        final MutableNamedStyle mns = (MutableNamedStyle) mls;
                        final String namedStyle = mns.getName();
                        final StyleReference styleRef = Util.findStyleReference(namedStyle, layerStyles);
                        return getStyle(styleRef);
                    } else if (mls instanceof MutableStyle) {
                        return (MutableStyle) mls;
                    }

                }
            }
        }

        //no valid style found, returns the first one that do not specify a layer on which to apply.
        LOGGER.log(Level.INFO, "No layer {0} found for the styles defined in the given SLD file.", layerName);
        if (!emptyNameSLDLayers.isEmpty()) {
            LOGGER.info("Continue with the first style read in the SLD, that do not specify any layer on which to apply.");
            return (MutableStyle) ((MutableNamedLayer)sld.layers().get(0)).styles().get(0);
        }
        return null;
    }

    private List<MutableStyle> getStyles(final Map<GenericName, List<StyleReference>> layersStyles, final StyledLayerDescriptor sld, final List<String> styleNames) throws CstlServiceException {
        final List<MutableStyle> results = new ArrayList<>();
        int i = 0;
        for (Entry<GenericName, List<StyleReference>> layerStyles : layersStyles.entrySet()) {
            final List<StyleReference> styles = layerStyles.getValue();
            final MutableStyle style;
            if (sld != null) {
                //try to use the provided SLD
                style = extractStyle(layerStyles.getKey(), styles, sld);
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
     * Apply and transform recursively configuration {@link org.opengis.filter.Filter} and
     * {@link org.constellation.dto.service.config.wxs.DimensionDefinition} to all {@link org.geotoolkit.map.FeatureMapLayer} in
     * input {@link org.geotoolkit.map.MapItem}.
     * This method will only work on Feature layers.
     *
     * @param item root mapItem
     * @param userLogin login used to get configuration.
     */
    private void applyLayerFiltersAndDims(final MapItem item, final String userLogin) throws DataStoreException {

        if(item instanceof FeatureMapLayer){
            final FeatureMapLayer fml = (FeatureMapLayer)item;
            final FeatureType type = fml.getResource().getType();
            final FilterAndDimension layerFnD = getLayerFilterDimensions(type.getName(), userLogin);
            if (layerFnD.getFilter() != null) {
                Filter filterGt = Filter.INCLUDE;
                try {
                    filterGt = new DtoToOGCFilterTransformer(new FilterFactoryImpl()).visitFilter(layerFnD.getFilter());
                } catch (FactoryException e) {
                    LOGGER.log(Level.INFO, e.getLocalizedMessage(), e);
                }
                fml.setQuery(QueryBuilder.filtered(type.getName().toString(), filterGt));
            }

            for(DimensionDefinition ddef : layerFnD.getDimensions()){

                try {
                    final String crsname = ddef.getCrs();
                    final Expression lower = CQL.parseExpression(ddef.getLower());
                    final Expression upper = CQL.parseExpression(ddef.getUpper());
                    final CoordinateReferenceSystem crs;

                    if("elevation".equalsIgnoreCase(crsname)){
                        crs = CommonCRS.Vertical.ELLIPSOIDAL.crs();
                    }else if("temporal".equalsIgnoreCase(crsname)){
                        crs = CommonCRS.Temporal.JAVA.crs();
                    }else{
                        final EngineeringDatum customDatum = new DefaultEngineeringDatum(Collections.singletonMap("name", crsname));
                        final CoordinateSystemAxis csAxis = new DefaultCoordinateSystemAxis(Collections.singletonMap("name", crsname), "u", AxisDirection.valueOf(crsname), Units.UNITY);
                        final AbstractCS customCs = new AbstractCS(Collections.singletonMap("name", crsname), csAxis);
                        crs = new DefaultEngineeringCRS(Collections.singletonMap("name", crsname), customDatum, customCs);
                    }

                    final FeatureMapLayer.DimensionDef fdef = new FeatureMapLayer.DimensionDef(crs, lower, upper);
                    fml.getExtraDimensions().add(fdef);

                } catch (CQLException ex) {
                    LOGGER.log(Level.WARNING, null, ex);
                }
            }

        }

        for(MapItem layer : item.items()){
            applyLayerFiltersAndDims(layer, userLogin);
        }

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
