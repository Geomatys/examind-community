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
package org.constellation.coverage.core;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.logging.Level;

import com.examind.ogc.api.rest.common.dto.Collection;
import com.examind.ogc.api.rest.common.dto.Extent;
import com.examind.ogc.api.rest.common.dto.SpatialCRS;
import com.examind.ogc.api.rest.coverages.dto.DataRecord;
import com.examind.ogc.api.rest.coverages.dto.Axis;
import com.examind.ogc.api.rest.coverages.dto.DataRecordField;
import com.examind.ogc.api.rest.coverages.dto.DomainSet;
import com.examind.ogc.api.rest.coverages.dto.EncodingInfo;
import com.examind.ogc.api.rest.coverages.dto.GeneralGrid;
import com.examind.ogc.api.rest.coverages.dto.GridLimits;
import com.examind.ogc.api.rest.coverages.dto.IndexAxis;
import com.examind.ogc.api.rest.coverages.dto.RegularAxis;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import org.apache.sis.coverage.Category;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverageBuilder;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.image.ImageProcessor;
import org.apache.sis.image.Interpolation;
import org.apache.sis.math.Statistics;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.Units;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.crs.DefaultTemporalCRS;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.util.Utilities;
import org.constellation.api.DataType;
import org.constellation.api.QueryConstants;
import org.constellation.api.ServiceDef;

import static org.constellation.coverage.core.AtomLinkBuilder.BuildCoverageLink;
import static org.constellation.coverage.core.WCSConstant.ASCII_GRID;
import static org.constellation.coverage.core.WCSConstant.GEOTIFF;
import static org.constellation.coverage.core.WCSConstant.INTERPOLATION_V100;
import static org.constellation.coverage.core.WCSConstant.INTERPOLATION_V111;
import static org.constellation.coverage.core.WCSConstant.KEY_BBOX;
import static org.constellation.coverage.core.WCSConstant.KEY_COVERAGE;
import static org.constellation.coverage.core.WCSConstant.KEY_CRS;
import static org.constellation.coverage.core.WCSConstant.KEY_FORMAT;
import static org.constellation.coverage.core.WCSConstant.KEY_IDENTIFIER;
import static org.constellation.coverage.core.WCSConstant.KEY_INTERPOLATION;
import static org.constellation.coverage.core.WCSConstant.KEY_RESPONSE_CRS;
import static org.constellation.coverage.core.WCSConstant.KEY_SECTION;
import static org.constellation.coverage.core.WCSConstant.KEY_TIME;
import static org.constellation.coverage.core.WCSConstant.MATRIX;
import static org.constellation.coverage.core.WCSConstant.NETCDF;
import static org.constellation.coverage.core.WCSConstant.SUPPORTED_FORMATS_100;
import static org.constellation.coverage.core.WCSConstant.SUPPORTED_FORMATS_111;
import static org.constellation.coverage.core.WCSConstant.SUPPORTED_INTERPOLATIONS_V100;
import static org.constellation.coverage.core.WCSConstant.TIF;
import static org.constellation.coverage.core.WCSConstant.TIFF;
import static org.constellation.coverage.core.WCSConstant.getOperationMetadata;
import org.constellation.coverage.ws.rs.GeotiffResponse;
import org.constellation.coverage.ws.rs.GridCoverageNCWriter;
import org.constellation.coverage.ws.rs.GridCoverageWriter;
import org.constellation.dto.StyleReference;
import org.constellation.dto.contact.Details;
import org.constellation.dto.service.config.wxs.FormatURL;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.portrayal.CstlPortrayalService;
import org.constellation.provider.CoverageData;
import org.constellation.provider.Data;
import org.constellation.util.WCSUtils;
import org.constellation.ws.CstlServiceException;
import org.constellation.ws.ExceptionCode;
import static org.constellation.ws.ExceptionCode.AXIS_LABEL_INVALID;
import static org.constellation.ws.ExceptionCode.INVALID_SUBSETTING;
import org.constellation.ws.LayerCache;
import org.constellation.ws.LayerWorker;
import org.constellation.ws.MimeType;
import org.constellation.ws.rs.MultiPart;
import org.geotoolkit.atom.xml.Link;
import org.geotoolkit.display.PortrayalException;
import org.geotoolkit.display2d.service.CanvasDef;
import org.geotoolkit.display2d.service.SceneDef;
import org.geotoolkit.geometry.jts.JTSEnvelope2D;
import org.geotoolkit.gml.xml.v311.DirectPositionType;
import org.geotoolkit.gml.xml.v311.EnvelopeType;
import org.geotoolkit.gml.xml.v311.GridType;
import org.geotoolkit.gml.xml.v311.RectifiedGridType;
import org.geotoolkit.gml.xml.v321.AssociationRoleType;
import org.geotoolkit.gml.xml.v321.FileType;
import org.geotoolkit.gmlcov.geotiff.xml.v100.CompressionType;
import org.geotoolkit.gmlcov.geotiff.xml.v100.ParametersType;
import org.geotoolkit.gmlcov.xml.v100.AbstractDiscreteCoverageType;
import org.geotoolkit.gmlcov.xml.v100.ObjectFactory;
import org.geotoolkit.image.io.metadata.SpatialMetadata;
import org.apache.sis.map.MapLayers;
import static org.constellation.map.util.MapUtils.combine;
import static org.geotoolkit.ows.xml.OWSExceptionCode.CURRENT_UPDATE_SEQUENCE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.FILE_SIZE_EXCEEDED;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_CRS;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_DIMENSION_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_FORMAT;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_UPDATE_SEQUENCE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.LAYER_NOT_DEFINED;
import static org.geotoolkit.ows.xml.OWSExceptionCode.LAYER_NOT_QUERYABLE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.MISSING_PARAMETER_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.NO_APPLICABLE_CODE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.VERSION_NEGOTIATION_FAILED;

import org.constellation.util.Util;
import org.constellation.util.CRSUtilities;
import org.geotoolkit.ows.xml.AbstractCapabilitiesCore;
import org.geotoolkit.ows.xml.AbstractOperationsMetadata;
import org.geotoolkit.ows.xml.AbstractServiceIdentification;
import org.geotoolkit.ows.xml.AbstractServiceProvider;
import org.geotoolkit.ows.xml.AcceptFormats;
import org.geotoolkit.ows.xml.AcceptVersions;
import org.geotoolkit.ows.xml.BoundingBox;
import org.geotoolkit.ows.xml.Sections;
import org.geotoolkit.ows.xml.v110.BoundingBoxType;
import org.geotoolkit.ows.xml.v110.CodeType;
import org.geotoolkit.ows.xml.v110.SectionsType;
import org.geotoolkit.ows.xml.v110.WGS84BoundingBoxType;
import org.geotoolkit.process.ProcessException;
import org.geotoolkit.processing.coverage.bandselect.BandSelectProcess;
import org.geotoolkit.resources.Errors;
import org.geotoolkit.storage.ResourceProcessor;
import org.geotoolkit.style.MutableStyle;
import org.geotoolkit.swe.xml.v200.AllowedValuesPropertyType;
import org.geotoolkit.swe.xml.v200.AllowedValuesType;
import org.geotoolkit.swe.xml.v200.DataRecordPropertyType;
import org.geotoolkit.swe.xml.v200.DataRecordType;
import org.geotoolkit.swe.xml.v200.Field;
import org.geotoolkit.swe.xml.v200.QuantityType;
import org.geotoolkit.swe.xml.v200.UnitReference;
import org.geotoolkit.temporal.object.TemporalUtilities;
import org.geotoolkit.temporal.util.TimeParser;
import org.geotoolkit.wcs.xml.Content;
import org.geotoolkit.wcs.xml.CoverageInfo;
import org.geotoolkit.wcs.xml.DescribeCoverage;
import org.geotoolkit.wcs.xml.DescribeCoverageResponse;
import org.geotoolkit.wcs.xml.DomainSubset;
import org.geotoolkit.wcs.xml.GetCapabilities;
import org.geotoolkit.wcs.xml.GetCapabilitiesResponse;
import org.geotoolkit.wcs.xml.GetCoverage;
import org.geotoolkit.wcs.xml.ServiceMetadata;
import org.geotoolkit.wcs.xml.WCSMarshallerPool;
import org.geotoolkit.wcs.xml.WCSXmlFactory;
import org.geotoolkit.wcs.xml.v100.CoverageOfferingType;
import org.geotoolkit.wcs.xml.v100.DomainSetType;
import org.geotoolkit.wcs.xml.v100.InterpolationMethod;
import org.geotoolkit.wcs.xml.v100.LonLatEnvelopeType;
import org.geotoolkit.wcs.xml.v100.RangeSetType;
import org.geotoolkit.wcs.xml.v100.SpatialDomainType;
import org.geotoolkit.wcs.xml.v100.SupportedCRSsType;
import org.geotoolkit.wcs.xml.v100.SupportedFormatsType;
import org.geotoolkit.wcs.xml.v100.SupportedInterpolationsType;
import org.geotoolkit.wcs.xml.v111.CoverageDescriptionType;
import org.geotoolkit.wcs.xml.v111.CoverageDomainType;
import org.geotoolkit.wcs.xml.v111.FieldType;
import org.geotoolkit.wcs.xml.v111.GridCrsType;
import org.geotoolkit.wcs.xml.v111.InterpolationMethods;
import org.geotoolkit.wcs.xml.v111.RangeType;
import org.geotoolkit.wcs.xml.v200.DimensionSliceType;
import org.geotoolkit.wcs.xml.v200.DimensionTrimType;
import org.geotoolkit.wcs.xml.v200.ExtensionType;
import org.geotoolkit.wcs.xml.v200.ServiceParametersType;
import org.opengis.coverage.grid.RectifiedGrid;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Worker for the WCS services in Constellation which services both the REST
 * facades by issuing appropriate responses.
 * <p>
 * The classes implementing the REST facades to this service will have
 * processed the requests sufficiently to ensure that all the information
 * conveyed by the HTTP request is in one of the fields of the object passed
 * to the worker methods as a parameter.
 * </p>
 *
 * @version 0.9
 *
 * @author Cédric Briançon (Geomatys)
 * @author Guilhem Legal (Geomatys)
 * @since 0.3
 */
@Component("WCSWorker")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public final class DefaultWCSWorker extends LayerWorker implements WCSWorker {

    public DefaultWCSWorker(final String id) {
        super(id, ServiceDef.Specification.WCS);
        started();
    }

    /**
     * The DescribeCoverage operation returns an XML file, containing the
     * complete description of the specific coverages requested.
     * <p>
     * This method extends the definition of each coverage given in the
     * Capabilities document with supplementary information.
     * </p>
     *
     * @param request A {@linkplain DescribeCoverage request} with the
     *                parameters of the user message.
     * @return An XML document giving the full description of the requested coverages.
     * @throws CstlServiceException
     */
    @Override
    public DescribeCoverageResponse describeCoverage(final DescribeCoverage request) throws CstlServiceException {
        isWorking();
        final String version = request.getVersion().toString();
        final String userLogin = getUserLogin();
        if (version.isEmpty()) {
            throw new CstlServiceException("The parameter VERSION must be specified.",
                    MISSING_PARAMETER_VALUE, QueryConstants.VERSION_PARAMETER.toLowerCase());
        }

        if (request.getIdentifier().isEmpty()) {
            throw new CstlServiceException("The parameter IDENTIFIER must be specified",
                    MISSING_PARAMETER_VALUE, KEY_IDENTIFIER.toLowerCase());
        }

        if (!"WCS".equalsIgnoreCase(request.getService())) {
            throw new CstlServiceException("The parameter SERVICE must be specified as WCS",
                    MISSING_PARAMETER_VALUE, QueryConstants.SERVICE_PARAMETER.toLowerCase());
        }

        final List<CoverageInfo> coverageOfferings = new ArrayList<>();
        for (String coverage : request.getIdentifier()) {

            final QName name = Util.parseQName(coverage);
            final LayerCache layer = getLayerCache(userLogin, name);
            if (layer.getDataType().equals(DataType.VECTOR)) {
                throw new CstlServiceException("The requested layer is vectorial. WCS is not able to handle it.",
                        LAYER_NOT_DEFINED, KEY_COVERAGE.toLowerCase());
            }
            if (!(layer.getData() instanceof CoverageData)) {
                // Should not occurs, since we have previously verified the type of layer.
                throw new CstlServiceException("The requested layer is not a coverage. WCS is not able to handle it.",
                        LAYER_NOT_DEFINED, KEY_COVERAGE.toLowerCase());
            }

            if (!layer.isQueryable(ServiceDef.Query.WCS_ALL)) {
                throw new CstlServiceException("You are not allowed to request the layer \"" +
                        coverage + "\".", LAYER_NOT_QUERYABLE, KEY_COVERAGE.toLowerCase());
            }


            if (version.equals("1.0.0")) {
                coverageOfferings.add(describeCoverage100(layer));
            } else if (version.equals("1.1.1")) {
                coverageOfferings.add(describeCoverage111(layer));
            } else if (version.equals("2.0.1")) {
                coverageOfferings.add(describeCoverage200(layer));
            } else {
                throw new CstlServiceException("The version number specified for this GetCoverage request " +
                        "is not handled.", NO_APPLICABLE_CODE, QueryConstants.VERSION_PARAMETER.toLowerCase());
            }
        }
        return WCSXmlFactory.createDescribeCoverageResponse(version, coverageOfferings);
    }

    /**
     * Returns the description of the coverage requested in version 1.0.0 of WCS standard.
     *
     * @param request a {@linkplain org.geotoolkit.wcs.xml.v100.DescribeCoverage describe coverage}
     *                request done by the user.
     * @return an XML document giving the full description of a coverage, in version 1.0.0.
     *
     * @throws CstlServiceException
     */
    private CoverageInfo describeCoverage100(final LayerCache layer) throws CstlServiceException {
        try {
            final String identifier = identifier(layer);
            final CoverageData data = (CoverageData) layer.getData();
            final GeographicBoundingBox inputGeoBox = layer.getGeographicBoundingBox();

            final List<EnvelopeType> envelopes = new ArrayList<>();

            final LonLatEnvelopeType llenvelope;
            final EnvelopeType envelope;
            if (inputGeoBox != null) {
                final SortedSet<Number> elevations = layer.getAvailableElevations();
                final List<DirectPositionType> pos = WCSUtils.buildPositions(inputGeoBox, elevations);
                llenvelope = new LonLatEnvelopeType(pos, "urn:ogc:def:crs:OGC:1.3:CRS84");
                envelope = new EnvelopeType(pos, "urn:ogc:def:crs:EPSG::4326");
                envelopes.add(envelope);
            } else {
                throw new CstlServiceException("The geographic bbox for the layer is null !",
                        NO_APPLICABLE_CODE);
            }
            final List<String> keywords = Arrays.asList("WCS", identifier);

            /*
             * Spatial metadata
             */
            final EnvelopeType nativeEnvelope = new EnvelopeType(layer.getEnvelope());
            if (!envelopes.contains(nativeEnvelope)) {
                envelopes.add(nativeEnvelope);
            }

            GridType grid = null;
            try {
                SpatialMetadata meta = data.getSpatialMetadata();
                if (meta != null) {
                    RectifiedGrid brutGrid = meta.getInstanceForType(RectifiedGrid.class);
                    if (brutGrid != null) {
                        grid = new RectifiedGridType(brutGrid);
                        /*
                         * UGLY PATCH : remove it when geotk will fill this data
                         */
                        if (grid.getDimension() == 0) {
                            int dimension = brutGrid.getOffsetVectors().size();
                            grid.setDimension(dimension);
                        }
                        if (grid.getAxisName().isEmpty()) {
                            if (grid.getDimension() == 2) {
                                grid.setAxisName(Arrays.asList("x", "y"));
                            } else if (grid.getDimension() == 3) {
                                grid.setAxisName(Arrays.asList("x", "y", "z"));
                            }
                        }
                    }
                }
            } catch (ConstellationStoreException ex) {
                LOGGER.log(Level.WARNING, "Unable to get coverage spatial metadata", ex);
            }

            final SpatialDomainType spatialDomain =
                new SpatialDomainType(envelopes, Arrays.asList(grid));

            // temporal metadata
            final List<Object> times = WCSUtils.formatDateList(layer.getAvailableTimes());
            final DomainSetType domainSet = new DomainSetType(spatialDomain, times);
            //TODO complete
            final RangeSetType rangeSet = new RangeSetType(null, identifier, identifier, null, null, null, null);
            //supported CRS
            final SupportedCRSsType supCRS = new SupportedCRSsType("urn:ogc:def:crs:EPSG::4326");
            supCRS.addNativeCRSs(nativeEnvelope.getSrsName());

            // supported formats
            final String nativeFormat = toWcsFormat(data.getImageFormat().orElse("unknown"));
            final SupportedFormatsType supForm = new SupportedFormatsType(nativeFormat, SUPPORTED_FORMATS_100);

            //supported interpolations
            final SupportedInterpolationsType supInt = INTERPOLATION_V100;

            //we build the coverage offering for this layer/coverage
            return new CoverageOfferingType(null, identifier,
                    identifier, "", llenvelope,
                    keywords, domainSet, rangeSet, supCRS, supForm, supInt);
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex, NO_APPLICABLE_CODE);
        }
    }

    private static String toWcsFormat(String mimeType) {
        if (mimeType != null && mimeType.contains("/")) {
            return mimeType.substring(mimeType.lastIndexOf('/') + 1);
        }
        return mimeType;
    }

    /**
     * Returns the description of the coverage requested in version 1.1.1 of WCS standard.
     *
     * @param request a {@linkplain org.geotoolkit.wcs.xml.v111.DescribeCoverage describe coverage}
     *                request done by the user.
     * @return an XML document giving the full description of a coverage, in version 1.1.1.
     *
     * @throws CstlServiceException
     */
    private CoverageInfo describeCoverage111(final LayerCache layer) throws CstlServiceException {
        try {
            final String identifier = identifier(layer);
            final CoverageData data = (CoverageData) layer.getData();
            final GeographicBoundingBox inputGeoBox = layer.getGeographicBoundingBox();

            WGS84BoundingBoxType outputBBox = null;
            if (inputGeoBox != null) {
                outputBBox = new WGS84BoundingBoxType(inputGeoBox);
            }
            /*
             * Spatial metadata
             */
            final BoundingBoxType nativeEnvelope = new BoundingBoxType(layer.getEnvelope());

            GridCrsType grid = null;
            try {
                SpatialMetadata meta = WCSUtils.adapt(
                        data.getSpatialMetadata(),
                        data.getGeometry(),
                        data.getSampleDimensions().toArray(new SampleDimension[0])
                );
                RectifiedGrid brutGrid = meta.getInstanceForType(RectifiedGrid.class);
                if (brutGrid != null) {
                    grid = new GridCrsType(brutGrid);
                }
            } catch (ConstellationStoreException ex) {
                LOGGER.log(Level.WARNING, "Unable to get coverage spatial metadata", ex);
            }

            // spatial metadata
            final org.geotoolkit.wcs.xml.v111.SpatialDomainType spatial =
                    new org.geotoolkit.wcs.xml.v111.SpatialDomainType(outputBBox,nativeEnvelope, grid, null, null, null);

            //general metadata
            final String title = identifier;
            final String abstractt = "";
            final List<String> keywords = Arrays.asList("WCS", identifier);

            // temporal metadata
            final List<Object> times = WCSUtils.formatDateList(layer.getAvailableTimes());
            final CoverageDomainType domain = new CoverageDomainType(spatial, times);

            //supported interpolations
            final InterpolationMethods interpolations = INTERPOLATION_V111;

            final String thematic = "";
            final RangeType range = new RangeType(new FieldType(thematic,
                    null, new CodeType("0.0"), interpolations));

            //supported CRS
            final List<String> supportedCRS = Arrays.asList("urn:ogc:def:crs:EPSG::4326");

            return new CoverageDescriptionType(title, abstractt,
                    keywords, identifier, domain, range, supportedCRS, SUPPORTED_FORMATS_111);
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex, NO_APPLICABLE_CODE);
        }
    }

    /**
     * Returns the description of the coverage requested in version 2.0.1 of WCS standard.
     *
     * TODO : do not use metadata to build description. All can be set from grid geometry and sample dimensions.
     *
     * @param request a {@linkplain org.geotoolkit.wcs.xml.v200.DescribeCoverage describe coverage}
     *                request done by the user.
     * @return an XML document giving the full description of a coverage, in version 2.0.1.
     *
     * @throws CstlServiceException
     */
    private org.geotoolkit.wcs.xml.v200.CoverageDescriptionType describeCoverage200(final LayerCache layer) throws CstlServiceException {
        try {
            final String identifier = identifier(layer);
            final CoverageData data = (CoverageData) layer.getData();
            /*
             * Spatial metadata
             */
            final org.geotoolkit.gml.xml.v321.EnvelopeType nativeEnvelope = new org.geotoolkit.gml.xml.v321.EnvelopeType(layer.getEnvelope());

            org.geotoolkit.gml.xml.v321.GridType grid = null;
            try {
                SpatialMetadata meta = WCSUtils.adapt(
                        data.getSpatialMetadata(),
                        data.getGeometry(),
                        data.getSampleDimensions().toArray(new SampleDimension[0])
                );

                RectifiedGrid brutGrid = meta.getInstanceForType(RectifiedGrid.class);
                if (brutGrid.getExtent() != null) {
                    CoordinateReferenceSystem crs = meta.getInstanceForType(CoordinateReferenceSystem.class);
                    grid = new org.geotoolkit.gml.xml.v321.RectifiedGridType(brutGrid, crs);
                    grid.setId("grid-" + identifier.replace(':', '_')); // gml id does not like ':'

                    // update native envelope axis labels
                    if (brutGrid.getAxisNames() != null) {
                        nativeEnvelope.setAxisLabels(brutGrid.getAxisNames());
                    } else if (crs != null) {
                        final List<String> axisNames = new ArrayList<>();
                        for (int i = 0; i < crs.getCoordinateSystem().getDimension(); i++) {
                            axisNames.add(crs.getCoordinateSystem().getAxis(i).getAbbreviation());
                        }
                        nativeEnvelope.setAxisLabels(axisNames);
                    }
                }

            } catch (ConstellationStoreException ex) {
                LOGGER.log(Level.WARNING, "Unable to get coverage spatial metadata", ex);
            }

            // spatial metadata
            final org.geotoolkit.gml.xml.v321.DomainSetType domain = new org.geotoolkit.gml.xml.v321.DomainSetType(grid);

            final List<SampleDimension> bands = data.getSampleDimensions();
            final List<Field> fields = new ArrayList<>();
            if (bands != null) {
                for (SampleDimension band : bands) {
                    final QuantityType quantity = new QuantityType();
                    band.getUnits().ifPresent(s -> quantity.setUom(new UnitReference(s.toString())));

                    // TODO select only one category => which one?
                    if (band.getCategories() != null) {
                        for (Category cat : band.getCategories()) {
                            if (cat.getName().toString(Locale.ENGLISH).equalsIgnoreCase("no data")) {
                                //ignore this category
                                continue;
                            }

                            final AllowedValuesType av = new AllowedValuesType();
                            if (cat.getName() != null) {
                                av.setId(cat.getName().toString());
                            }
                            final NumberRange<?> range =  cat.getMeasurementRange().orElse(null);
                            if (range != null) {
                                av.setMin(range.getMinDouble());
                                av.setMax(range.getMaxDouble());
                            }
                            quantity.setConstraint(new AllowedValuesPropertyType(av));
                        }
                    }
                    final Field f = new Field(band.getName().toString(), quantity);
                    fields.add(f);
                }
            }
            final DataRecordType dataRecord;
            if (!fields.isEmpty()) {
                dataRecord = new DataRecordType(null, null, false, fields);
            } else {
                dataRecord = null;
            }
            final DataRecordPropertyType rangeType = new DataRecordPropertyType(dataRecord);
            final ServiceParametersType serviceParametersType = new ServiceParametersType(new QName("GridCoverage"), toWcsFormat(data.getImageFormat().orElse(null)));
            org.geotoolkit.wcs.xml.v200.CoverageDescriptionType result = new org.geotoolkit.wcs.xml.v200.CoverageDescriptionType(identifier, nativeEnvelope, domain, rangeType, serviceParametersType);
            result.setId(identifier);
            return result;
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex, NO_APPLICABLE_CODE);
        }
    }

    /**
     * Describe the capabilities and the layers available for the WCS service.
     *
     * @param request The request done by the user.
     * @return a WCSCapabilities XML document describing the capabilities of this service.
     *
     * @throws CstlServiceException
     */
    @Override
    public GetCapabilitiesResponse getCapabilities(final GetCapabilities request) throws CstlServiceException {
        isWorking();
        //we begin by extract the base attribute
        String version = request.getVersion().toString();
        final String userLogin = getUserLogin();
        if (version.isEmpty()) {
            // For the moment the only version that we really support is this one.
            version = "1.0.0";
        }

        if (!"WCS".equalsIgnoreCase(request.getService())) {
            throw new CstlServiceException("The parameter SERVICE must be specified as WCS",
                    MISSING_PARAMETER_VALUE, QueryConstants.SERVICE_PARAMETER.toLowerCase());
        }

        //set the current updateSequence parameter
        final boolean returnUS = returnUpdateSequenceDocument(request.getUpdateSequence(), version);
        if (returnUS) {
            return WCSXmlFactory.createCapabilitiesResponse(version, getCurrentUpdateSequence());
        }

        /*
         * In WCS 1.0.0 the user can request only one section
         * ( or all by omitting the parameter section)
         */
        final Sections sections = request.getSections();
        if (sections != null && !sections.getSection().isEmpty()) {
            for (String sec : sections.getSection()) {
                if (!SectionsType.getExistingSections(version).contains(sec)) {
                    throw new CstlServiceException("This sections " + sec + " is not allowed", INVALID_PARAMETER_VALUE, KEY_SECTION.toLowerCase());
                }
            }
        }

        // if the user have specified one format accepted (only one for now != spec)
        final String format;
        if (version.equals("1.1.1")) {
            final AcceptFormats formats = request.getAcceptFormats();
            if (formats == null || formats.getOutputFormat().isEmpty()) {
                format = MimeType.TEXT_XML;
            } else {
                format = formats.getOutputFormat().get(0);
                if (!format.equals(MimeType.TEXT_XML) && !format.equals(MimeType.APP_XML)) {
                    throw new CstlServiceException("This format " + format + " is not allowed",
                            INVALID_FORMAT, KEY_FORMAT.toLowerCase());
                }
            }
        }

        // If the getCapabilities response is in cache, we just return it.
        final AbstractCapabilitiesCore cachedCapabilities = getCapabilitiesFromCache(version, null);
        if (cachedCapabilities != null) {
            return (GetCapabilitiesResponse) cachedCapabilities.applySections(sections);
        }

        // We unmarshall the static capabilities document.
        final Details skeleton = getStaticCapabilitiesObject("WCS", null);
        final GetCapabilitiesResponse staticCapabilities = WCSConstant.createCapabilities(version, skeleton);
        final AbstractServiceIdentification si = staticCapabilities.getServiceIdentification();
        final AbstractServiceProvider sp = staticCapabilities.getServiceProvider();
        final AbstractOperationsMetadata om = getOperationMetadata(version);
        om.updateURL(getServiceUrl());

        WCSConstant.applyProfile(version, si);

        final List<CoverageInfo> offBrief = new ArrayList<>();
        final List<LayerCache> layers = getLayerCaches(userLogin);
        try {
            for (LayerCache layer : layers) {
                final Data data = layer.getData();

                if (data == null) {
                    throw new CstlServiceException("There is no existing data for the layer:" + layer.getName());
                }

                if (layer.getDataType().equals(DataType.VECTOR)) {
                    continue;
                }
                if (!layer.isQueryable(ServiceDef.Query.WCS_ALL)) {
                    continue;
                }
                if (layer.getGeographicBoundingBox() == null) {
                    // The coverage does not contain geometric information, we do not want this coverage
                    // in the capabilities response.
                    continue;
                }

                final CoverageInfo co;
                if (version.equals("1.0.0")) {
                    co = getCoverageInfo100(layer);
                } else {
                    co = getCoverageInfo(version, layer);
                }
                /*
                * coverage brief customisation
                 */
                final String title = layer.getConfiguration().getTitle();
                if (title != null) {
                    co.setTitle(title);
                }
                final String abs = layer.getConfiguration().getAbstrac();
                if (abs != null) {
                    co.setAbstract(abs);
                }
                final List<String> kws = layer.getConfiguration().getKeywords();
                if (kws != null && !kws.isEmpty()) {
                    co.setKeywordValues(kws);
                }
                final List<FormatURL> metadataUrls = layer.getConfiguration().getMetadataURL();
                if (metadataUrls != null && !metadataUrls.isEmpty() && metadataUrls.get(0).getOnlineResource() != null) {
                    // TODO handle multiple metadata URL
                    co.setMetadata(metadataUrls.get(0).getOnlineResource().getValue());
                }
                offBrief.add(co);
            }
        } catch (ConstellationStoreException exception) {
            throw new CstlServiceException(exception, NO_APPLICABLE_CODE);
        }
        final Content contents = WCSXmlFactory.createContent(version, offBrief);
        final ServiceMetadata sm = WCSConstant.getServiceMetadata(version);
        final GetCapabilitiesResponse response = WCSXmlFactory.createCapabilitiesResponse(version, si, sp, om, contents, getCurrentUpdateSequence(), sm);
        putCapabilitiesInCache(version, null, response);
        return (GetCapabilitiesResponse) response.applySections(sections);
    }

    /**
     * Returns the {@linkplain GetCapabilitiesResponse GetCapabilities} response of the request
     * given by parameter, in version 1.0.0 of WCS.
     *
     * @param request The request done by the user, in version 1.0.0.
     * @return a WCSCapabilities XML document describing the capabilities of this service.
     *
     * @throws CstlServiceException
     * @throws JAXBException when unmarshalling the default GetCapabilities file.
     */
    private CoverageInfo getCoverageInfo100(final LayerCache layer) throws ConstellationStoreException {
        final String identifier = identifier(layer);
        final GeographicBoundingBox inputGeoBox = layer.getGeographicBoundingBox();
        final List<DirectPositionType> pos = WCSUtils.buildPositions(inputGeoBox, layer.getAvailableElevations());
        final LonLatEnvelopeType outputBBox = new LonLatEnvelopeType(pos, "urn:ogc:def:crs:OGC:1.3:CRS84");

        final SortedSet<Date> dates = layer.getAvailableTimes();
        if (dates != null && dates.size() >= 2) {
            /*
             * Adds the first and last date available, since in the WCS GetCapabilities,
             * it is a brief description of the capabilities.
             * To get the whole available values, the describeCoverage request has to be
             * done on a specific coverage.
             */
            final Date firstDate = dates.first();
            final Date lastDate = dates.last();
            synchronized (WCSUtils.FORMATTER) {
                outputBBox.addTimePosition(WCSUtils.FORMATTER.format(firstDate), WCSUtils.FORMATTER.format(lastDate));
            }
        }

        return WCSXmlFactory.createCoverageInfo("1.0.0", identifier, identifier, null, outputBBox, null);
    }

    /**
     * Returns the {@linkplain GetCapabilitiesResponse GetCapabilities} response of the request given
     * by parameter, in version 1.1.1 of WCS.
     *
     * @param request The request done by the user, in version 1.1.1.
     * @return a WCSCapabilities XML document describing the capabilities of this service.
     *
     * @throws CstlServiceException
     */
    private CoverageInfo getCoverageInfo(final String version, final LayerCache layer) throws ConstellationStoreException {
        final String identifier = identifier(layer);
        final String title      = identifier;
        final String remark     = "";

        final GeographicBoundingBox inputGeoBox = layer.getGeographicBoundingBox();
        final BoundingBox outputBBox = WCSXmlFactory.buildWGS84BoundingBox(version, inputGeoBox);
        final String coverageSubType = "GridCoverage";
        return WCSXmlFactory.createCoverageInfo(version, identifier, title,
                remark, outputBBox, coverageSubType);
    }

    /**
     * Get the coverage values for a specific coverage specified.
     * According to the output format chosen, the response could be an
     * {@linkplain RenderedImage image} or data representation.
     *
     * @param request The request done by the user.
     * @return An {@linkplain RenderedImage image}, or a data representation.
     *
     * @throws CstlServiceException
     */
    @Override
    public Object getCoverage(final GetCoverage request) throws CstlServiceException {
        isWorking();
        final String inputVersion = request.getVersion().toString();
        final String userLogin = getUserLogin();
        if (inputVersion == null) {
            throw new CstlServiceException("The parameter version must be specified",
                    MISSING_PARAMETER_VALUE, QueryConstants.VERSION_PARAMETER.toLowerCase());
        } else if (!"1.0.0".equals(inputVersion) &&
                   !"2.0.1".equals(inputVersion) &&
                   !"1.1.1".equals(inputVersion)) {
            throw new CstlServiceException("The version number specified for this request " + inputVersion +
                    " is not handled.", VERSION_NEGOTIATION_FAILED, QueryConstants.VERSION_PARAMETER.toLowerCase());
        }

        if (!"WCS".equalsIgnoreCase(request.getService())) {
            throw new CstlServiceException("The parameter SERVICE must be specified as WCS",
                    MISSING_PARAMETER_VALUE, QueryConstants.SERVICE_PARAMETER.toLowerCase());
        }

        final String coverageName = request.getCoverage();
        if (coverageName == null) {
            throw new CstlServiceException("You must specify the parameter: COVERAGE", INVALID_PARAMETER_VALUE,
                    KEY_COVERAGE.toLowerCase());
        }
        final QName tmpName = Util.parseQName(request.getCoverage());
        final LayerCache layer = getLayerCache(userLogin, tmpName);
        if (!layer.isQueryable(ServiceDef.Query.WCS_ALL) || layer.getDataType().equals(DataType.VECTOR)) {
            throw new CstlServiceException("You are not allowed to request the layer \"" +
                    layer.getName() + "\".", INVALID_PARAMETER_VALUE, KEY_COVERAGE.toLowerCase());
        }
        if (!(layer.getData() instanceof CoverageData)) {
            // Should not occurs, since we have previously verified the type of layer.
            throw new CstlServiceException("The requested layer is not a coverage. WCS is not able to handle it.",
                    LAYER_NOT_DEFINED, KEY_COVERAGE.toLowerCase());
        }
        final CoverageData data = (CoverageData) layer.getData();

        // TODO : if no subsetting is done, and queried output format is the same as queried data, we could directly
        // send back original data file. It would be far less complex, and far more optimized.
        if ("2.0.1".equals(inputVersion)) {
            return getCoverage200(request, layer);
        }

        Date[] dates = null;
        try {
            // another parser should be used allowing to parse multiple date.
            Date d = TimeParser.toDate(request.getTime());
            dates = new Date[] {d,d};
        } catch (ParseException ex) {
            throw new CstlServiceException("Parsing of the date failed. Please verify that the specified" +
                    " date is compliant with the ISO-8601 standard.", ex, INVALID_PARAMETER_VALUE,
                    KEY_TIME.toLowerCase());
        }

        // we verify the interpolation method even if we don't use it
        try {
            if (request.getInterpolationMethod() != null) {
                final InterpolationMethod interpolation = (InterpolationMethod) request.getInterpolationMethod();
                if (!SUPPORTED_INTERPOLATIONS_V100.contains(interpolation)) {
                    throw new CstlServiceException("Unsupported interpolation: " + request.getInterpolationMethod(), INVALID_PARAMETER_VALUE, KEY_INTERPOLATION.toLowerCase());
                }
            }
        } catch (IllegalArgumentException ex) {
            throw new CstlServiceException(ex.getMessage(), INVALID_PARAMETER_VALUE, KEY_INTERPOLATION.toLowerCase());
        }

        Envelope envelope;
        try {
            envelope = request.getEnvelope();
        } catch (FactoryException ex) {
            throw new CstlServiceException(ex, INVALID_PARAMETER_VALUE, KEY_BBOX.toLowerCase());
        }
        /*
         * Here the envelope can be null, if we have specified a TIME parameter. In this case we
         * do not have to test whether the bbox parameter are into the CRS axes definition.
         */
        if (envelope != null) {
            // Ensures the bbox specified is inside the range of the CRS.
            final CoordinateReferenceSystem objectiveCrs;
            try {
                objectiveCrs = request.getCRS();
            } catch (FactoryException ex) {
                throw new CstlServiceException(ex, INVALID_CRS, KEY_CRS.toLowerCase());
            }
            for (int i = 0; i < objectiveCrs.getCoordinateSystem().getDimension(); i++) {
                final CoordinateSystemAxis axis = objectiveCrs.getCoordinateSystem().getAxis(i);
                if (envelope.getMaximum(i) < axis.getMinimumValue() ||
                    envelope.getMinimum(i) > axis.getMaximumValue())
                {
                    throw new CstlServiceException(Errors.format(Errors.Keys.IllegalRange_2,
                            envelope.getMinimum(i), envelope.getMaximum(i)),
                            INVALID_DIMENSION_VALUE, KEY_BBOX.toLowerCase());
                }
            }
            // Ensures the requested envelope has, at least, a part that intersects the valid envelope
            // for the coverage.
            try {
                final GeographicBoundingBox geoBbox = layer.getGeographicBoundingBox();
                if (geoBbox == null) {
                    throw new CstlServiceException("The request coverage \""+ layer.getName() +"\" has" +
                                                   " no geometric information.", NO_APPLICABLE_CODE);
                }
                final GeneralEnvelope validGeoEnv = new GeneralEnvelope(geoBbox);
                Envelope requestGeoEnv = envelope;
                // We have to transform the objective envelope into an envelope that uses a geographic CRS,
                // in order to be able to verify the intersection between those two envelopes.
                if (!Utilities.equalsIgnoreMetadata(envelope.getCoordinateReferenceSystem(), CommonCRS.WGS84.normalizedGeographic())) {
                    try {
                        requestGeoEnv = Envelopes.transform(envelope, CommonCRS.WGS84.normalizedGeographic());
                    } catch (TransformException ex) {
                        throw new CstlServiceException(ex, NO_APPLICABLE_CODE, KEY_BBOX.toLowerCase());
                    }
                }
                if (!(validGeoEnv.intersects(requestGeoEnv, false))) {
                    throw new CstlServiceException("The requested bbox is outside the domain of validity " +
                            "for this coverage", NO_APPLICABLE_CODE, KEY_BBOX.toLowerCase());
                }
            } catch (ConstellationStoreException ex) {
                throw new CstlServiceException(ex, NO_APPLICABLE_CODE, KEY_BBOX.toLowerCase());
            }
        } else if (dates == null) {

            throw new CstlServiceException("One of Time or Envelope has to be specified", MISSING_PARAMETER_VALUE);

        } else {
            // We take the envelope from the data provider. That envelope can be a little bit imprecise.
            try {
                final GeographicBoundingBox geoBbox = layer.getGeographicBoundingBox();
                if (geoBbox == null) {
                    throw new CstlServiceException("The request coverage \""+ layer.getName() +"\" has" +
                                                   " no geometric information.", NO_APPLICABLE_CODE);
                }
                envelope = new JTSEnvelope2D(geoBbox.getWestBoundLongitude(), geoBbox.getEastBoundLongitude(),
                        geoBbox.getSouthBoundLatitude(), geoBbox.getNorthBoundLatitude(),
                        CommonCRS.WGS84.normalizedGeographic());
            } catch (ConstellationStoreException ex) {
                throw new CstlServiceException(ex, NO_APPLICABLE_CODE, KEY_BBOX.toLowerCase());
            }
        }
        Envelope refEnvel;
        try {
            final CoordinateReferenceSystem responseCRS = request.getResponseCRS();
            if (responseCRS != null && !Utilities.equalsIgnoreMetadata(responseCRS, envelope.getCoordinateReferenceSystem())) {
                final Envelope responseEnv = Envelopes.transform(envelope, responseCRS);
                refEnvel = new GeneralEnvelope(responseEnv);
            } else {
                refEnvel = envelope;
            }
        } catch (FactoryException ex) {
            throw new CstlServiceException(ex, INVALID_CRS, KEY_CRS.toLowerCase());
        } catch (TransformException ex) {
            throw new CstlServiceException(ex, INVALID_CRS, KEY_RESPONSE_CRS.toLowerCase());
        }

        Dimension size = request.getSize();
        if (size == null) {
            // Try with resx/resy, those parameters should be filled.
            final List<Double> resolutions = request.getResolutions();
            if (resolutions == null || resolutions.isEmpty()) {
                // Should not occurs since it is already tested
                throw new CstlServiceException("If width/height are not specified, you have to give resx/resy");
            }
            final double resx = resolutions.get(0);
            final double resy = resolutions.get(1);
            final double envWidth = refEnvel.getSpan(0);
            final double envHeight = refEnvel.getSpan(1);
            // Assume that the resolution is in unit per px -> unit / (unit/pixel) -> px
            // For example to obtain an image whose width is 1024 pixels, representing 360 degrees,
            // the resolution on the x axis is 360 / 1024 = 0,3515625 degrees/pixels.
            // In our case, we want to know the image width using the size of the envelope and the
            // given resolution on that axis, so: image_width = envelope_width / resx
            final int newWidth = (int) Math.round(envWidth / resx);
            final int newHeight = (int) Math.round(envHeight / resy);
            size = new Dimension(newWidth, newHeight);
        }

        // TODO this values is not used
        //final Double elevation = (envelope.getDimension() > 2) ? envelope.getMedian(2) : null;
        Double[] elevations = new Double[] {null, null};

        /*
         * Generating the response.
         * It can be a text one (format MATRIX) or an image one (png, gif ...).
         */
        final String format = request.getFormat();
        if (format.equalsIgnoreCase(MATRIX) || format.equalsIgnoreCase(ASCII_GRID)) {

            //NOTE ADRIAN HACKED HERE
            final RenderedImage image;
            try {
                if (dates != null) {
                    refEnvel = combine(refEnvel, dates, elevations, null);
                }
                final GridCoverage gridCov = data.getCoverage(refEnvel, size);
                image = gridCov.render(null);
            } catch (ConstellationStoreException | FactoryException ex) {
                throw new CstlServiceException(ex, NO_APPLICABLE_CODE);
            }

            return image;

        } else if (format.equalsIgnoreCase(NETCDF)) {

            throw new CstlServiceException(new IllegalArgumentException(
                    "Examind does not support netcdf writing in WCS 1.0.0."),
                    INVALID_FORMAT, KEY_FORMAT.toLowerCase());

        } else if (format.equalsIgnoreCase(GEOTIFF) || format.equalsIgnoreCase(TIFF) || format.equalsIgnoreCase(TIF)) {
            try {
                if (dates != null) {
                    refEnvel = combine(refEnvel, dates, elevations, null);
                }
                GeotiffResponse response = new GeotiffResponse();
                response.metadata = data.getSpatialMetadata();
                response.coverage = data.getCoverage(refEnvel, size);
                return response;
            } catch (ConstellationStoreException | FactoryException ex) {
                throw new CstlServiceException(ex, NO_APPLICABLE_CODE);
            }

        } else {
            // We are in the case of an image format requested.
            //NOTE: ADRIAN HACKED HERE
            final SceneDef sdef = new SceneDef();

            final MutableStyle style;
            if (!layer.getStyles().isEmpty()) {
                final StyleReference styleName = layer.getStyles().get(0);
                final MutableStyle incomingStyle = (MutableStyle) getStyle(styleName);
                style = WCSUtils.filterStyle(incomingStyle, request.getRangeSubset());
            } else {
                style = null;
            }
            try {
                final MapLayers context = mapBusiness.createContext(layer, style);
                sdef.setContext(context);
            } catch (ConstellationStoreException ex) {
                throw new CstlServiceException(ex, NO_APPLICABLE_CODE);
            }

            // VIEW
            final Double azimuth = 0.0; //HARD CODED SINCE PROTOCOL DOES NOT ALLOW

            // CANVAS
            Color background = null;
            if (MimeType.IMAGE_JPEG.equalsIgnoreCase(format)) {
                background = Color.WHITE;
            }

            // build an 2D envelope
            refEnvel = new JTSEnvelope2D(envelope);
            final CanvasDef cdef = new CanvasDef(size, refEnvel);
            cdef.setBackground(background);
            cdef.setAzimuth(azimuth);

            // IMAGE
            final RenderedImage img;
            try {
                img = CstlPortrayalService.getInstance().portray(sdef, cdef);
            } catch (PortrayalException ex) {
                /*
                 * TODO: the binding xml for WCS and GML do not support the exceptions format,
                 * consequently we can't extract the exception output mime-type information from
                 * the request. Maybe a more recent version of the GML 3 spec has fixed this bug ...
                 */
                //if (exceptions != null && exceptions.equalsIgnoreCase(EXCEPTIONS_INIMAGE)) {
                //    img = Cstl.Portrayal.writeInImage(ex, abstractRequest.getSize());
                //} else {
                throw new CstlServiceException(ex, NO_APPLICABLE_CODE);
                //}
            }

            return img;
        }
    }

    private Object getCoverage200(final GetCoverage request, final LayerCache layer) throws CstlServiceException {
        boolean isMultiPart = false;
        if (request.getMediaType() != null) {
            if (request.getMediaType().equals("multipart/mixed")) {
                isMultiPart = true;
            } else {
                throw new CstlServiceException("Only multipart/mixed is supported for mediaType parameter", INVALID_PARAMETER_VALUE);
            }
        }
        final CoverageData data = (CoverageData) layer.getData();
        final SpatialMetadata metadata;
        final GridCoverageResource ref = (GridCoverageResource) data.getOrigin();
        GeneralEnvelope readEnv;
        CoordinateReferenceSystem crs;
        try {
            Optional<Envelope> refEnv = ref.getEnvelope();
            if (refEnv.isPresent()) {
                readEnv = new GeneralEnvelope(refEnv.get());
                crs = readEnv.getCoordinateReferenceSystem();
            } else {
                final GridGeometry gridGeometry = ref.getGridGeometry();
                readEnv = new GeneralEnvelope(gridGeometry.getEnvelope());
            }
            if (request.getCRS() != null) {
                crs = request.getCRS();
                if (!Utilities.equalsIgnoreMetadata(crs, readEnv.getCoordinateReferenceSystem())) {
                    readEnv = CRSUtilities.reprojectWithNoInfinity(readEnv, crs);
                }
            } else {
                crs = readEnv.getCoordinateReferenceSystem();
            }
            metadata = data.getSpatialMetadata();
        } catch (ConstellationStoreException | DataStoreException | FactoryException  | TransformException ex) {
            throw new CstlServiceException(ex, NO_APPLICABLE_CODE);
        }

        //combine envelope with domain subset
        if (!request.getDomainSubset().isEmpty()) {

            // trim / slice the envelope
            Set<String> usedDimension = new HashSet<>();
            for (DomainSubset subset : request.getDomainSubset()) {
                String dimension = null;
                if (subset instanceof DimensionTrimType trim) {
                    dimension = trim.getDimension();
                    final int dimIdx = dimensionIndex(trim.getDimension(), crs);
                    if (dimIdx == -1) {
                        throw new CstlServiceException("There is no such dimension: " + trim.getDimension(), AXIS_LABEL_INVALID);
                    } else {

                        double minVal = readEnv.getLower(dimIdx);
                        double maxVal = readEnv.getUpper(dimIdx);
                        double low = toAxisValue(trim.getTrimLow(), crs, dimIdx, minVal);
                        double high = toAxisValue(trim.getTrimHigh(), crs, dimIdx, maxVal);

                        //verif that trim value does not overlap envelope
                        if (low < minVal || low > maxVal || high > maxVal || high < minVal) {
                            throw new CstlServiceException("Subsetting params overlap the envelope extent",
                                    INVALID_SUBSETTING);
                        }

                        //verif that trim value is correct (low < high)
                        /*  we want to allow images crossing anti-meridian
                        if (low > high) {
                            throw new CstlServiceException("Subsetting params are invalid (low > high)",
                                    INVALID_SUBSETTING);
                        }*/

                        readEnv.setRange(dimIdx, low, high);
                    }

                } else if (subset instanceof DimensionSliceType slice) {
                    dimension = slice.getDimension();
                    final int dimIdx = dimensionIndex(slice.getDimension(), crs);
                    if (dimIdx == -1) {
                        throw new CstlServiceException("There is no such dimension: " + slice.getDimension(), AXIS_LABEL_INVALID);
                    } else {
                        double minVal = readEnv.getMinimum(dimIdx);
                        double maxVal = readEnv.getMaximum(dimIdx);
                        double pt = toAxisValue(slice.getSlicePoint(), crs, dimIdx, readEnv.getMedian(dimIdx));

                        //verif that trim value does not overlap envelope
                        if (pt < minVal || pt > maxVal) {
                            throw new CstlServiceException("Subsetting params overlap the envelope extent",
                                    INVALID_SUBSETTING);
                        }

                        readEnv.setRange(dimIdx, pt, pt);
                    }
                }
                // verify that multiple operations are not applied on the same dimension
                if (dimension != null) {
                    if (usedDimension.contains(dimension)) {
                        throw new CstlServiceException("A GetCoverage request shall contain at most one subsetting operation for each of the dimensions of the coverage addressed",
                                AXIS_LABEL_INVALID);
                    } else {
                        usedDimension.add(dimension);
                    }
                }
            }
        }

        /*
         * Generating the response.
         * It can be a text one (format MATRIX) or an image one (png, gif ...).
         */
        final String format = request.getFormat();
        if (format.equalsIgnoreCase(MATRIX) || format.equalsIgnoreCase(ASCII_GRID)) {

            //NOTE ADRIAN HACKED HERE
            final RenderedImage image;
            try {
                final GridCoverage coverage = data.getCoverage(readEnv, null);
                image = coverage.render(null);
            } catch (ConstellationStoreException ex) {
                throw new CstlServiceException(ex, NO_APPLICABLE_CODE);
            }

            return image;

        } else if (format.equalsIgnoreCase(MimeType.NETCDF)) {

            try {
                final GridCoverage coverage = data.getCoverage(readEnv, null);
                final SimpleEntry response = new SimpleEntry(coverage, metadata);
                if (isMultiPart) {
                    final File img = File.createTempFile(layer.getName().getLocalPart(), ".nc");
                    GridCoverageNCWriter.writeInStream(response, new FileOutputStream(img));
                    final String xml = buildXmlPart(describeCoverage200(layer), format);
                    final MultiPart multiPart = new MultiPart();
                    multiPart.bodyPart("application/xml", xml)
                            .bodyPart(format, img);
                    return multiPart;
                } else {
                    return response;
                }

            } catch (IOException | JAXBException | ConstellationStoreException ex) {
                throw new CstlServiceException(ex, NO_APPLICABLE_CODE);
            }

        } else if (format.equalsIgnoreCase(MimeType.IMAGE_TIFF)) {
            try {
                final GeotiffResponse response = new GeotiffResponse();
                response.coverage = data.getCoverage(readEnv, null);
                response.metadata = metadata;
                response.outputCRS = request.getResponseCRS();
                if (request.getExtension() instanceof ExtensionType ext) {
                    final ParametersType geoExt = ext.getForClass(ParametersType.class);
                    if (geoExt != null) {
                        if (geoExt.getCompression() != null) {
                            if (geoExt.getCompression() == CompressionType.LZW ||
                                geoExt.getCompression() == CompressionType.PACK_BITS ||
                                geoExt.getCompression() == CompressionType.NONE) {
                                response.compression = geoExt.getCompression().value();
                            } else {
                                throw new CstlServiceException("Server does not support the requested compression.", ExceptionCode.COMPRESSION_NOT_SUPPORTED, geoExt.getCompression().value());
                            }
                        }
                        if (geoExt.getInterleave() != null) {
                            throw new CstlServiceException("Server does not support interleaving.", ExceptionCode.INTERLEAVING_NOT_SUPPORTED, geoExt.getInterleave().value());
                        }
                        if (geoExt.getPredictor() != null) {
                            throw new CstlServiceException("Server does not support predictor.", ExceptionCode.PREDICTOR_NOT_SUPPORTED, geoExt.getPredictor().value());
                        }
                        if (geoExt.isTiling()) {
                            if ("PackBits".equals(response.compression)) {
                                throw new CstlServiceException("Server does not support Tiling for packbit compression.", ExceptionCode.TILING_NOT_SUPPORTED, "tiling");
                            }
                            if (geoExt.getTileheight() != null && geoExt.getTilewidth() != null &&
                                geoExt.getTileheight() > 0 && geoExt.getTilewidth() > 0 &&
                                (geoExt.getTileheight() % 16 == 0) &&
                                (geoExt.getTilewidth() % 16 == 0)) {
                                response.tiling = true;
                                response.tileHeight = geoExt.getTileheight();
                                response.tileWidth = geoExt.getTilewidth();
                            } else {
                                throw new CstlServiceException("Server does not support predictor.", ExceptionCode.TILING_INVALID, geoExt.getPredictor().value());
                            }
                        }
                    }
                }
                if (isMultiPart) {
                    final File img = GridCoverageWriter.writeInFile(response);
                    final String xml = buildXmlPart(describeCoverage200(layer), format);
                    final MultiPart multiPart = new MultiPart();
                    multiPart.bodyPart("application/xml", xml)
                            .bodyPart(format, img);
                    return multiPart;
                } else {
                    return response;
                }
            } catch (Exception ex) {
                throw new CstlServiceException(ex, NO_APPLICABLE_CODE);
            }

        } else {
            // We are in the case of an image format requested.
            //NOTE: ADRIAN HACKED HERE

            // SCENE
            final SceneDef sdef = new SceneDef();

            final MutableStyle style;
            if (!layer.getStyles().isEmpty()) {
                final StyleReference styleName = layer.getStyles().get(0);
                final MutableStyle incomingStyle = (MutableStyle) getStyle(styleName);
                style = WCSUtils.filterStyle(incomingStyle, request.getRangeSubset());
            } else {
                style = null;
            }
            try {
                final MapLayers context = mapBusiness.createContext(layer, style);
                sdef.setContext(context);
            } catch (ConstellationStoreException ex) {
                throw new CstlServiceException(ex, NO_APPLICABLE_CODE);
            }

            // CANVAS
            Color background = null;
            if (MimeType.IMAGE_JPEG.equalsIgnoreCase(format)) {
                background = Color.WHITE;
            }
            final CanvasDef cdef = new CanvasDef(new Dimension(500, 500), readEnv);
            cdef.setBackground(background);

            // IMAGE
            final RenderedImage img;
            try {
                img = CstlPortrayalService.getInstance().portray(sdef, cdef);
            } catch (PortrayalException ex) {
                /*
                 * TODO: the binding xml for WCS and GML do not support the exceptions format,
                 * consequently we can't extract the exception output mime-type information from
                 * the request. Maybe a more recent version of the GML 3 spec has fixed this bug ...
                 */
                //if (exceptions != null && exceptions.equalsIgnoreCase(EXCEPTIONS_INIMAGE)) {
                //    img = Cstl.Portrayal.writeInImage(ex, abstractRequest.getSize());
                //} else {
                throw new CstlServiceException(ex, NO_APPLICABLE_CODE);
                //}
            }

            return img;
        }
    }

    @Override
    public Object getCoverage(String collectionId, String format, List<Double> bbox, String scaleData, List<String> subsetData, List<String> properties) throws CstlServiceException {
        try {
            final CoverageData data = getCoverageData(collectionId);
            GeneralEnvelope readEnv = getGeneralEnvelope(data);

            CoordinateReferenceSystem crs = readEnv.getCoordinateReferenceSystem();
            final SpatialMetadata metadata;
            try {
                metadata = data.getSpatialMetadata();
            } catch (ConstellationStoreException ex) {
                throw new CstlServiceException(ex, NO_APPLICABLE_CODE);
            }

            processBbox(readEnv, bbox);
            GridGeometry originGeometry = data.getGeometry();
            DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

            for(String subsetString : subsetData) {
                String[] split = subsetString.split("\\(",2);
                String axisName = split[0];
                String[] values = split[1].replace(")", "").split(":",2);

                if (values.length >= 3) { //Not supported
                    throw new CstlServiceException("This subset is not valid (only support slicing [value] or simple subset [value1:value2].", INVALID_SUBSETTING);
                }

                Integer axisID = null;
                for (int i = 0; i < crs.getCoordinateSystem().getDimension(); i++) {
                    CoordinateSystemAxis axis = crs.getCoordinateSystem().getAxis(i);
                    if (axisName.equalsIgnoreCase(axis.getAbbreviation())) {
                        axisID = i;
                        break;
                    }
                }
                if (axisID == null) {
                    throw new CstlServiceException("This subset is not valid, axis doesn't exist : " + axisName, INVALID_SUBSETTING);
                }

                DimensionNameType axisType = originGeometry.getExtent().getAxisType(axisID).orElse(null);
                double minVal = readEnv.getMinimum(axisID);
                double maxVal = readEnv.getMaximum(axisID);
                double firstValue = 0.0;
                double secondValue = 0.0;

                if (axisType == DimensionNameType.TIME) {
                    if (values.length == 1) { //In case of slice
                        LocalDateTime dateTime = LocalDateTime.parse(values[0], dateFormat);
                        firstValue = dateTime.toEpochSecond(java.time.ZoneOffset.UTC);
                        secondValue = firstValue;
                    } else if (values.length == 2) { //In case of subset
                        LocalDateTime dateTime = LocalDateTime.parse(values[0], dateFormat);
                        firstValue = dateTime.toEpochSecond(java.time.ZoneOffset.UTC);
                        dateTime = LocalDateTime.parse(values[1], dateFormat);
                        secondValue = dateTime.toEpochSecond(java.time.ZoneOffset.UTC);
                    }
                } else {
                    if (values.length == 1) { //In case of slice
                        firstValue = Double.parseDouble(values[0]);
                        secondValue = firstValue;
                    } else if (values.length == 2) { //In case of subset
                        firstValue = Double.parseDouble(values[0]);
                        secondValue = Double.parseDouble(values[1]);
                    }
                }

                if ((firstValue < minVal || firstValue > maxVal) && (secondValue < minVal || secondValue > maxVal)) {
                    throw new CstlServiceException("Subsetting params overlap the envelope extent",
                            INVALID_SUBSETTING);
                }
                readEnv.setRange(axisID, firstValue, secondValue);
            }

            /////////////////////////////////////////////////////////////////////////////////

            //Get time and vertical dimensions
            int timeDimensionId = -1;
            int verticalDimensionId = -1;
            int dimensionSize = originGeometry.getDimension();
            for (int dimIdx = 0; dimIdx < dimensionSize; dimIdx++) {
                CoordinateSystemAxis csa = crs.getCoordinateSystem().getAxis(dimIdx);
                String abbreviation = csa.getAbbreviation().toLowerCase();
                DimensionNameType axisType = originGeometry.getExtent().getAxisType(dimIdx).orElse(null);

                if (timeDimensionId == -1 && (axisType == DimensionNameType.TIME || abbreviation.equals("t") || abbreviation.equals("time"))) {
                    timeDimensionId = dimIdx;
                }
                if (verticalDimensionId == -1 && (axisType == DimensionNameType.VERTICAL)) {
                    verticalDimensionId = dimIdx;
                }
            }

            boolean timeAlreadySliced = false;
            boolean verticalAlreadySliced = false;
            if(timeDimensionId == -1) timeAlreadySliced = true;
            else {
                double min = readEnv.getMinimum(timeDimensionId);
                double max = readEnv.getMaximum(timeDimensionId);
                if (min == max) {
                    timeAlreadySliced = true;
                }
            }
            if(verticalDimensionId == -1) verticalAlreadySliced = true;
            else {
                double min = readEnv.getMinimum(verticalDimensionId);
                double max = readEnv.getMaximum(verticalDimensionId);
                if (min == max) {
                    verticalAlreadySliced = true;
                }
            }
            if (!format.equalsIgnoreCase(MimeType.NETCDF) && !timeAlreadySliced && !verticalAlreadySliced) {
                //Slice over time and vertical axis
                if (timeDimensionId != -1) {
                    //Slice time :
                    double minVal = readEnv.getMinimum(timeDimensionId);
                    double maxVal = readEnv.getMaximum(timeDimensionId);
                    double pt = readEnv.getLower(timeDimensionId);

                    //verif that trim value does not overlap envelope
                    if (pt < minVal || pt > maxVal) {
                        throw new CstlServiceException("Subsetting params overlap the envelope extent",
                                INVALID_SUBSETTING);
                    }
                    readEnv.setRange(timeDimensionId, pt, pt);
                }
                if (verticalDimensionId != -1) {
                    //Slice vertical dimension :
                    double minVal = readEnv.getMinimum(verticalDimensionId);
                    double maxVal = readEnv.getMaximum(verticalDimensionId);
                    double pt = readEnv.getLower(verticalDimensionId);

                    //verif that trim value does not overlap envelope
                    if (pt < minVal || pt > maxVal) {
                        throw new CstlServiceException("Subsetting params overlap the envelope extent",
                                INVALID_SUBSETTING);
                    }
                    readEnv.setRange(verticalDimensionId, pt, pt);
                }
            }

            GridGeometry readGg = getGridGeometry(data, readEnv, scaleData);
            GridCoverage gridCoverageSource = null;
            try {
                var processor = new ResourceProcessor();
                processor.getProcessor().setInterpolation(Interpolation.BILINEAR);
                GridCoverageResource coverageResourceResampled = processor.resample(data.getOrigin(), readGg, data.getName());
                gridCoverageSource = coverageResourceResampled.read(readGg);
            } catch (Exception ex) {
                throw new CstlServiceException(ex, NO_APPLICABLE_CODE);
            }

            if (properties != null && !properties.isEmpty()) {
                List<Integer> ids = new ArrayList<>();
                int i = 0;
                for(SampleDimension sm : gridCoverageSource.getSampleDimensions()) {
                    if(properties.contains(sm.getName().toString())) {
                        ids.add(i);
                    }
                    i++;
                }

                int[] idsArray = new int[ids.size()];
                for (i = 0; i < ids.size(); i++) {
                    idsArray[i] = ids.get(i);
                }

                if (!ids.isEmpty()) {
                    BandSelectProcess bandSelectProcess = new BandSelectProcess(gridCoverageSource, idsArray);
                    gridCoverageSource = bandSelectProcess.executeNow();
                }
            }

            if (format.equalsIgnoreCase(MimeType.NETCDF)) {
//                try {
                    //TODO : Reuse data.getCoverage(readEnv,null) when we will use Apache SIS Tiff writer
                    final GridCoverage coverage = gridCoverageSource; //data.getCoverage(readEnv, null);
                    final SimpleEntry response = new SimpleEntry(coverage, metadata);
                    return response;

//                } catch (ConstellationStoreException ex) {
//                    throw new CstlServiceException(ex, NO_APPLICABLE_CODE);
//                }
            }
            else if (format.equalsIgnoreCase(MimeType.IMAGE_TIFF)) {
                try {
                    final GeotiffResponse response = new GeotiffResponse();

                    ////////////////////////////////////////////////
                    // QUICK FIX : SOLVE COLORMODEL = NULL EXCEPTION
                    ////////////////////////////////////////////////
                    RenderedImage imageStatistics = new ImageProcessor().statistics(gridCoverageSource.render(null), null, null);
                    Statistics[] statistics = (Statistics[]) imageStatistics.getProperty("org.apache.sis.Statistics");

                    int i = 0;
                    List<SampleDimension> resultSdList = new ArrayList<>();
                    for(SampleDimension sourceSD : gridCoverageSource.getSampleDimensions()) {
                        SampleDimension.Builder sdb = new SampleDimension.Builder();
                        sdb.addQuantitative(sourceSD.getName().toString(), statistics[i].minimum(), statistics[i].maximum(), sourceSD.getUnits().orElse(Units.UNITY));
                        resultSdList.add(sdb.build());
                    }

                    GridCoverageBuilder gcb = new GridCoverageBuilder();
                    gcb.setDomain(gridCoverageSource.getGridGeometry());
                    gcb.setValues(gridCoverageSource.render(null).getData()); //TODO : Delete getData() when we will use Apache SIS Tiff writer
                    gcb.setRanges(resultSdList);
                    response.coverage = gcb.build();
                    response.metadata = metadata;
                    response.outputCRS = crs;
                    return response;

                } catch (Exception ex) {
                    throw new CstlServiceException(ex, NO_APPLICABLE_CODE);
                }

            } else {
                throw new CstlServiceException("The format is not valid, we only support geotiff",
                        INVALID_PARAMETER_VALUE, KEY_COVERAGE.toLowerCase());
            }
        } catch (OutOfMemoryError ex) {
            throw new CstlServiceException("OutOfMemory : the coverage is too big with these parameters",
                    FILE_SIZE_EXCEEDED, KEY_COVERAGE.toLowerCase());
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException("Error : Coverage geometry is not found",
                    NO_APPLICABLE_CODE, KEY_COVERAGE.toLowerCase());
        } catch (ProcessException e) {
            throw new CstlServiceException("Error : Coverage band selection (properties) as an error",
                    NO_APPLICABLE_CODE, KEY_COVERAGE.toLowerCase());
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public GetCapabilitiesResponse getCapabilities(String version) throws CstlServiceException {
       AcceptVersions versions = WCSXmlFactory.buildAcceptVersion(version, Arrays.asList(version));
       GetCapabilities request = WCSXmlFactory.createGetCapabilities(version, versions, null, null, null, "WCS");
       return getCapabilities(request);
    }

    @Override
    public List<Collection> getCollections(List<String> names) throws CstlServiceException {
        final String userLogin  = getUserLogin();
        final List<LayerCache> layers;
        if (names.isEmpty()) {
            // return all layers
            layers = getLayerCaches(userLogin, true);
        } else {
            layers = new ArrayList<>();
            for (String name : names) {
                final QName collName = Util.parseQName(name);
                layers.add(getLayerCache(userLogin, collName));
            }
        }
        return layers.stream().map(r -> dataToCollection(r)).collect(Collectors.toList());
    }

    private Collection dataToCollection(LayerCache layer) {
        final Data data = layer.getData();
        final List<Link> links = new ArrayList<>();
        final Extent extent = new Extent();

        Envelope envelope = null;
        try {
            envelope = data.getEnvelope();
            extent.setFromCoordinateReferenceSystem(envelope.getCoordinateReferenceSystem());
        } catch (ConstellationStoreException ex) {
            LOGGER.log(Level.WARNING, "No envelope - " + ex.getLocalizedMessage(), ex);
        }

        //TODO : change with data crs + dynamically get the crs
        List<String> crs = Collections.singletonList(extent.getCrs());
        String storageCrs = extent.getCrs();

        try {
            final DefaultGeographicBoundingBox gbox = new DefaultGeographicBoundingBox();
            gbox.setBounds(data.getEnvelope());

            SpatialCRS spatialCRS = new SpatialCRS();
            double[] boundingBox = {gbox.getWestBoundLongitude(), gbox.getSouthBoundLatitude(), gbox.getEastBoundLongitude(), gbox.getNorthBoundLatitude()};
            double[][] globalBoundingBox = new double[1][4];
            System.arraycopy(boundingBox, 0, globalBoundingBox[0], 0, boundingBox.length);
            spatialCRS.setBbox(globalBoundingBox);
            extent.setSpatial(spatialCRS);

        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Cannot set spatial extent of data " + data.getName(), ex);
            extent.setSpatial(null);
        }

        final DateFormat ISO8601_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        try {
            SortedSet<Date> dates = data.getDateRange();
            if (dates != null) {
                com.examind.ogc.api.rest.common.dto.TemporalCRS temporalCRS = new com.examind.ogc.api.rest.common.dto.TemporalCRS();
                String[][] temporalInterval = new String[1][2];
                temporalInterval[0][0] = ISO8601_FORMAT.format(dates.first());
                temporalInterval[0][1] = ISO8601_FORMAT.format(dates.last());
                temporalCRS.setInterval(temporalInterval);
                extent.setTemporal(temporalCRS);
            } else {
                extent.setTemporal(null);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Cannot set temporal extent of data " + data.getName(), e);
            extent.setTemporal(null);
        }
        String identifier = identifier(layer);
        String title      = layer.getConfiguration().getTitle();
        if (title == null) {
            title = identifier;
        }
        String wfsUrl = getServiceUrl();
        String url    = wfsUrl.replace("/wcs", "/coverage").replace("?", "");
        BuildCoverageLink(url, identifier, title, links);
        // String itemType = "VECTOR" set item type to null for now as it does not appears in official xsd at http://schemas.opengis.net/ogcapi/features/part1/1.0/xml/core.xsd
        // but it appears at https://github.com/opengeospatial/ogcapi-features/blob/master/core/xml/core.xsd
        return new Collection(identifier, links, title, null, title, extent, crs, storageCrs);
    }

    @Override
    public DomainSet getDomainSet(String collectionId, List<Double> bbox) throws CstlServiceException {
        CoverageData data = getCoverageData(collectionId);

        final Extent extent = new Extent();

        GeneralEnvelope readEnv = getGeneralEnvelope(data);
        CoordinateReferenceSystem crs = readEnv.getCoordinateReferenceSystem();

        extent.setFromCoordinateReferenceSystem(crs);

        processBbox(readEnv, bbox);
        GridGeometry readGg = getGridGeometry(data, readEnv);

        List<Axis> axisList = new ArrayList<>();
        List<IndexAxis> indexAxisList = new ArrayList<>();
        char firstLetter = 'i';

        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        int dimensionSize = crs.getCoordinateSystem().getDimension();
        for(int dimIdx=0; dimIdx<dimensionSize; dimIdx++) {
            CoordinateSystemAxis csa = crs.getCoordinateSystem().getAxis(dimIdx);
            String abbreviation = csa.getAbbreviation().toLowerCase();
            DimensionNameType axisType = readGg.getExtent().getAxisType(dimIdx).orElse(null);

            Object lower = readEnv.getLower(dimIdx);
            Object upper = readEnv.getUpper(dimIdx);
            if (axisType == DimensionNameType.TIME || abbreviation.equals("t") || abbreviation.equals("time")) {
                Instant[] instants = readGg.getTemporalExtent();

                LocalDateTime dateTime = LocalDateTime.ofInstant(instants[0], ZoneOffset.UTC);
                lower = dateTime.format(dateFormat);

                dateTime = LocalDateTime.ofInstant(instants[1], ZoneOffset.UTC);
                upper = dateTime.format(dateFormat);
            }

            RegularAxis regularAxis = new RegularAxis(abbreviation, lower,
                    upper, csa.getUnit().getName(), readGg.getResolution(true)[dimIdx]);
            axisList.add(regularAxis);
            try {
                char letter = (char) (firstLetter + dimIdx);
                String letterStr = String.valueOf(Character.valueOf(letter));
                IndexAxis indexAxis = new IndexAxis(letterStr, data.getGeometry().getExtent().getLow(dimIdx), data.getGeometry().getExtent().getHigh(dimIdx));
                indexAxisList.add(indexAxis);
            } catch (ConstellationStoreException e) {
                throw new CstlServiceException(e, NO_APPLICABLE_CODE);
            }
        }

        //In the case of some dimensions are not in the crs (TIME, ROW, COLUMN are in the crs, others dimensions sometimes not)
        int extentDimensionSize = readGg.getDimension();
        int effectiveIndex = 0;
        if(dimensionSize < extentDimensionSize) {
            for(int dimIdx=0; dimIdx<extentDimensionSize; dimIdx++) {
                Optional<DimensionNameType> dimensionNameType = readGg.getExtent().getAxisType(dimIdx);
                if (dimensionNameType.isPresent()) {
                    if (!dimensionNameType.get().equals(DimensionNameType.ROW) && !dimensionNameType.get().equals(DimensionNameType.COLUMN) &&
                        !dimensionNameType.get().equals(DimensionNameType.TIME)) {

                        RegularAxis regularAxis = new RegularAxis(dimensionNameType.get().name(), readGg.getExtent().getLow(dimIdx),
                                readGg.getExtent().getHigh(dimIdx), "no unit specified", 1.0);
                        axisList.add(regularAxis);
                        char letter = (char) (firstLetter + (effectiveIndex + dimensionSize));
                        String letterStr = String.valueOf(Character.valueOf(letter));
                        IndexAxis indexAxis = new IndexAxis(letterStr, readGg.getExtent().getLow(dimIdx), readGg.getExtent().getHigh(dimIdx));
                        indexAxisList.add(indexAxis);
                        effectiveIndex++;
                    }
                }
            }
        }

        //TODO : change with data crs + dynamically get the crs
        String srsName = extent.getSrs();

        GeneralGrid generalGrid = new GeneralGrid(srsName, axisList, new GridLimits(indexAxisList));
        return new DomainSet(generalGrid);
    }

    @Override
    public DataRecord getDataRecord(String collectionId) throws CstlServiceException {
        CoverageData data = getCoverageData(collectionId);

        List<DataRecordField> dataRecordFields = new ArrayList<>();

        try {
            List<SampleDimension> sampleDimensions = data.getSampleDimensions();

            for (SampleDimension smp : sampleDimensions) {
                Class<?> type;
                if(smp.getBackground().isPresent()) {
                    type = smp.getBackground().get().getClass();
                } else {
                    type = null;
                }

                EncodingInfo encodingInfo = new EncodingInfo(EncodingInfo.getOpenGisLink(type));
                dataRecordFields.add(new DataRecordField(smp.getName().toString(), smp.getName().toString(), encodingInfo));
            }

        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex, NO_APPLICABLE_CODE);
        }

        return new DataRecord(dataRecordFields);
    }

    public List<String> getDimensionsNames(String collectionId) throws CstlServiceException {
        CoverageData data = getCoverageData(collectionId);

        GeneralEnvelope readEnv = getGeneralEnvelope(data);
        CoordinateReferenceSystem crs = readEnv.getCoordinateReferenceSystem();

        List<String> dimensionsNames = new ArrayList<>();

        int dimensionSize = crs.getCoordinateSystem().getDimension();
        for(int dimIdx=0; dimIdx<dimensionSize; dimIdx++) {
            CoordinateSystemAxis csa = crs.getCoordinateSystem().getAxis(dimIdx);
            dimensionsNames.add(csa.getAbbreviation());
        }

        return dimensionsNames;
    }

    private CoverageData getCoverageData(String collectionId) throws CstlServiceException {
        final String userLogin = getUserLogin();
        final QName tmpName = Util.parseQName(collectionId);
        final LayerCache layer = getLayerCache(userLogin, tmpName);

        if (!layer.isQueryable(ServiceDef.Query.WCS_ALL) || layer.getDataType().equals(DataType.VECTOR)) {
            throw new CstlServiceException("You are not allowed to request the layer \"" +
                    layer.getName() + "\".", INVALID_PARAMETER_VALUE, KEY_COVERAGE.toLowerCase());
        }

        if (!(layer.getData() instanceof CoverageData)) {
            // Should not occurs, since we have previously verified the type of layer.
            throw new CstlServiceException("The requested layer is not a coverage. WCS is not able to handle it.",
                    LAYER_NOT_DEFINED, KEY_COVERAGE.toLowerCase());
        }

        CoverageData data = (CoverageData) layer.getData();
        return data;
    }

    private GeneralEnvelope getGeneralEnvelope(CoverageData data) throws CstlServiceException {
        final GridCoverageResource ref = data.getOrigin();
        GeneralEnvelope readEnv;
        try {
            Optional<Envelope> refEnv = ref.getEnvelope();
            if (refEnv.isPresent()) {
                readEnv = new GeneralEnvelope(refEnv.get());
            } else {
                final GridGeometry gridGeometry = ref.getGridGeometry();
                readEnv = new GeneralEnvelope(gridGeometry.getEnvelope());
            }

            final CoordinateReferenceSystem epsg4326 = CRS.forCode("urn:ogc:def:crs:OGC:2:84");
            final CoordinateReferenceSystem temporalCRS = CRS.getTemporalComponent(readEnv.getCoordinateReferenceSystem());
            final CoordinateReferenceSystem verticalCRS = CRS.getVerticalComponent(readEnv.getCoordinateReferenceSystem(), true);

            final CoordinateReferenceSystem finalCRS;
            if(temporalCRS != null && verticalCRS == null) finalCRS = CRS.compound(epsg4326, temporalCRS);
            else if(temporalCRS == null && verticalCRS != null) finalCRS = CRS.compound(epsg4326, verticalCRS);
            else if(temporalCRS != null && verticalCRS != null) finalCRS = CRS.compound(epsg4326, verticalCRS, temporalCRS);
            else finalCRS = epsg4326;

            if (!Utilities.equalsIgnoreMetadata(epsg4326, readEnv.getCoordinateReferenceSystem())) {
                readEnv = CRSUtilities.reprojectWithNoInfinity(readEnv, finalCRS);
            }

        } catch (DataStoreException | FactoryException | TransformException ex) {
            throw new CstlServiceException(ex, NO_APPLICABLE_CODE);
        }

        return readEnv;
    }

    private Map<String,Double> parseResolutions(String stringResolutions) {
        Map<String,Double> resolutions = new HashMap<>();

        if(stringResolutions != null) {
            Pattern pattern = Pattern.compile("(\\w+)\\((\\d+\\.\\d+)\\)");
            Matcher matcher = pattern.matcher(stringResolutions);

            while (matcher.find()) {
                String axisName = matcher.group(1);
                double scaleValue = Double.parseDouble(matcher.group(2));

                resolutions.put(axisName.toLowerCase(), scaleValue);
            }
        }

        return resolutions;
    }

    private GridGeometry getGridGeometry(CoverageData data, GeneralEnvelope env, String resolution) throws CstlServiceException {
        Map<String,Double> resolutionsParsed = parseResolutions(resolution);

        CoordinateSystem cs = env.getCoordinateReferenceSystem().getCoordinateSystem();

        int dimensionSize = cs.getDimension();
        double[] resolutionsArr = new double[dimensionSize];

        for(int dimIdx=0; dimIdx<dimensionSize; dimIdx++) {
            CoordinateSystemAxis csa = cs.getAxis(dimIdx);
            String abbreviation = csa.getAbbreviation().toLowerCase();

                //We can't change the resolution on time axis
            if (!abbreviation.equalsIgnoreCase("t") && !abbreviation.equalsIgnoreCase("time") && resolutionsParsed.containsKey(abbreviation.toLowerCase())) {
                resolutionsArr[dimIdx] = resolutionsParsed.get(abbreviation);
            } else {
                resolutionsArr[dimIdx] = -1.0; //A resolution can't be negative, so -1 in our case is for the default value
            }
        }

        return getGridGeometry(data, env, resolutionsArr);
    }

    private GridGeometry getGridGeometry(CoverageData data, GeneralEnvelope env, double... resolution) throws CstlServiceException {
        try {
            if (resolution == null || resolution.length < 1) return data.getGeometry().derive().subgrid(env).build();

            var base = data.getGeometry();
            double [] targetResolution = base.getResolution(true);
            for (int i = 0 ; i < resolution.length && i < targetResolution.length ; i++) {
                var target = resolution[i];
                if (Double.isFinite(target) && target > 0) targetResolution[i] = target;
            }

            return base.derive().subgrid(env, targetResolution).build();
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex, NO_APPLICABLE_CODE);
        }
    }

    private void processBbox(GeneralEnvelope readEnv, List<Double> bbox) throws CstlServiceException {
         if (bbox != null) {
            CoordinateReferenceSystem crs = readEnv.getCoordinateReferenceSystem();
            int dimensionSize = crs.getCoordinateSystem().getDimension();
            if (bbox.size() == 4 || bbox.size() == 6) {
                for(int dimIdx=0; dimIdx<dimensionSize; dimIdx++) {
                    double minVal = readEnv.getLower(dimIdx);
                    double maxVal = readEnv.getUpper(dimIdx);

                    //double low = toAxisValue(trim.getTrimLow(), crs, dimIdx, minVal);
                    //double high = toAxisValue(trim.getTrimHigh(), crs, dimIdx, maxVal);
                    if(dimIdx < bbox.size()/2) {
                        double low = bbox.get(dimIdx);
                        double high = bbox.get(dimIdx + bbox.size()/2);

                        //verif that trim value does not overlap envelope
                        if (low < minVal || low > maxVal || high > maxVal || high < minVal) {
                            throw new CstlServiceException("Subsetting params overlap the envelope extent",
                                    INVALID_SUBSETTING, KEY_COVERAGE.toLowerCase());
                        }
                        readEnv.setRange(dimIdx, low, high);
                    }
                }
            } else if (!bbox.isEmpty()) {
                throw new CstlServiceException("bbox specified is not correct (only accept 4 or 6 values)",
                        INVALID_PARAMETER_VALUE, KEY_COVERAGE.toLowerCase());
            }
        }
    }

    private static int dimensionIndex(final String dimension, final CoordinateReferenceSystem crs) {
        for (int i = 0; i < crs.getCoordinateSystem().getDimension(); i++) {
            if (dimension.equals(crs.getCoordinateSystem().getAxis(i).getAbbreviation())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Convert string value to a double matching given axis unit.
     */
    private static double toAxisValue(String value, CoordinateReferenceSystem crs, int index, double fallback) throws CstlServiceException {
        if(value==null || value.isEmpty() || "*".equals(value)) return fallback;

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            try {
                //check if it's a date
                final CoordinateReferenceSystem subcrs = CRS.getComponentAt(crs, index, index+1);
                if (subcrs instanceof TemporalCRS tempCrs) {
                    final Calendar cal = TemporalUtilities.parseDateCal(value);
                    return DefaultTemporalCRS.castOrCopy(tempCrs).toValue(cal.getTime());
                }
            } catch (Exception ex1) {
                LOGGER.log(Level.FINE, ex1.getMessage(), ex1);
            }
            throw new CstlServiceException("Unsupported subset value " + value, INVALID_SUBSETTING);
        }
    }

    /**
     * Overriden from AbstractWorker because in version 1.0.0 the behaviour is different when the request updateSequence
     * is equal to the current.
     *
     * @param updateSequence
     * @param version
     * @return
     * @throws CstlServiceException
     */
    private boolean returnUpdateSequenceDocument(final String updateSequence, final String version) throws CstlServiceException {
        if (updateSequence == null) {
            return false;
        }
        if ("1.0.0".equals(version)) {
            try {
                final long sequenceNumber = Long.parseLong(updateSequence);
                final long currentUpdateSequence = Long.parseLong(getCurrentUpdateSequence());
                if (sequenceNumber == currentUpdateSequence) {
                    throw new CstlServiceException("The update sequence parameter is equal to the current", CURRENT_UPDATE_SEQUENCE, "updateSequence");
                } else if (sequenceNumber > currentUpdateSequence) {
                    throw new CstlServiceException("The update sequence parameter is invalid (higher value than the current)", INVALID_UPDATE_SEQUENCE, "updateSequence");
                }
                return false;
            } catch (NumberFormatException ex) {
                throw new CstlServiceException("The update sequence must be an integer", ex, INVALID_PARAMETER_VALUE, "updateSequence");
            }
        } else {
            return returnUpdateSequenceDocument(updateSequence);
        }
    }

    private String buildXmlPart(org.geotoolkit.wcs.xml.v200.CoverageDescriptionType describeCoverage200, String mime) throws JAXBException {
        final org.geotoolkit.gml.xml.v321.RangeSetType rangeSet = new org.geotoolkit.gml.xml.v321.RangeSetType();
        final FileType ft = new FileType();
        ft.setMimeType(mime);
        final String ext = WCSUtils.getExtension(mime);
        ft.setRangeParameters(new AssociationRoleType("cid:" + describeCoverage200.getCoverageId() + ext,
                "http://www.opengis.net/spec/GMLCOV_geotiff-coverages/1.0/conf/geotiff-coverage",
                "fileReference"));
        ft.setFileReference("cid:" + describeCoverage200.getCoverageId() + ext);
        rangeSet.setFile(ft);
        final AbstractDiscreteCoverageType cov = new AbstractDiscreteCoverageType(describeCoverage200, rangeSet);
        final ObjectFactory factory = new ObjectFactory();
        Object obj = factory.createGridCoverage(cov);

        Marshaller m = WCSMarshallerPool.getInstance().acquireMarshaller();
        final StringWriter sw = new StringWriter();
        m.marshal(obj, sw);
        WCSMarshallerPool.getInstance().recycle(m);
        return sw.toString();
    }
}
