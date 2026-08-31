/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2025 Geomatys.
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
package com.examind.dggs.core;

import com.examind.dggs.ws.rs.DGGSService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.geometry.S2Polygon;
import jakarta.xml.bind.JAXBException;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;
import javax.imageio.ImageIO;
import javax.measure.IncommensurableException;
import javax.measure.Quantity;
import javax.measure.Unit;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridOrientation;
import org.apache.sis.coverage.grid.GridRoundingMode;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.cql.CQL;
import org.apache.sis.cql.CQLException;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.internal.shared.AttributeConvention;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.filter.FunctionRegister;
import org.apache.sis.geometries.BBox;
import org.apache.sis.geometries.math.Array;
import org.apache.sis.geometries.math.NDArrays;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.wrapper.jts.JTS;
import org.apache.sis.image.PixelIterator;
import org.apache.sis.image.WritablePixelIterator;
import org.apache.sis.images.ImageBuilder;
import org.apache.sis.math.Statistics;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.crs.DefaultTemporalCRS;
import org.apache.sis.referencing.datum.DatumOrEnsemble;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.FeatureQuery;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.MemoryGridCoverageResource;
import org.apache.sis.storage.NoSuchDataException;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.coveragejson.CoverageJsonStore;
import org.apache.sis.storage.geotiff.GeoTiffStore;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.iso.Names;
import org.apache.sis.xml.XML;
import org.constellation.api.ServiceDef;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.provider.Data;
import org.constellation.ws.CstlServiceException;
import org.constellation.ws.LayerCache;
import org.constellation.ws.LayerWorker;
import org.constellation.ws.rs.ResponseObject;
import org.geotoolkit.dggs.healpix.HealpixDggrs;
import org.geotoolkit.feature.FeatureExt;
import org.geotoolkit.ogcapi.client.dggs.DggsApi;
import org.geotoolkit.ogcapi.dto.LinkRelations;
import org.geotoolkit.ogcapi.dto.common.CollectionDescription;
import org.geotoolkit.ogcapi.dto.common.Collections;
import org.geotoolkit.ogcapi.dto.common.Crs;
import org.geotoolkit.ogcapi.dto.common.Extent;
import org.geotoolkit.ogcapi.dto.common.Grid;
import org.geotoolkit.ogcapi.dto.common.LandingPage;
import org.geotoolkit.ogcapi.dto.common.Link;
import org.geotoolkit.ogcapi.dto.common.OtherDimension;
import org.geotoolkit.ogcapi.dto.common.SpatialExtent;
import org.geotoolkit.ogcapi.dto.common.TemporalExtent;
import org.geotoolkit.ogcapi.dto.dggs.Dggrs;
import org.geotoolkit.ogcapi.dto.dggs.DggrsData;
import org.geotoolkit.ogcapi.dto.dggs.DggrsDataValue;
import org.geotoolkit.ogcapi.dto.dggs.DggrsDataValueShape;
import org.geotoolkit.ogcapi.dto.dggs.DggrsDefinition;
import org.geotoolkit.ogcapi.dto.dggs.DggrsItem;
import org.geotoolkit.ogcapi.dto.dggs.DggrsLinkTemplatesInner;
import org.geotoolkit.ogcapi.dto.dggs.DggrsListResponse;
import org.geotoolkit.ogcapi.dto.dggs.DggrsZonesResponse;
import org.geotoolkit.ogcapi.dto.dggs.Dggs;
import org.geotoolkit.ogcapi.dto.dggs.DggsConstraints;
import org.geotoolkit.ogcapi.dto.dggs.DggsDefinition;
import org.geotoolkit.ogcapi.dto.dggs.DggsOrientation;
import org.geotoolkit.ogcapi.dto.dggs.DggsParameters;
import org.geotoolkit.ogcapi.dto.dggs.Dimension;
import org.geotoolkit.ogcapi.dto.dggs.MediaTypes;
import org.geotoolkit.ogcapi.dto.dggs.SchemaProperty;
import org.geotoolkit.ogcapi.dto.dggs.SubZoneOrder;
import org.geotoolkit.ogcapi.dto.dggs.Zirs;
import org.geotoolkit.ogcapi.dto.dggs.ZirsText;
import org.geotoolkit.ogcapi.dto.dggs.ZirsUInt64;
import org.geotoolkit.ogcapi.dto.dggs.ZoneInfo;
import org.geotoolkit.ogcapi.dto.dggs.ZoneInfoStatisticsValue;
import org.geotoolkit.ogcapi.dto.feature.Argument;
import org.geotoolkit.ogcapi.dto.feature.Function;
import org.geotoolkit.ogcapi.dto.feature.Functions;
import org.geotoolkit.ogcapi.dto.feature.ValueType;
import org.geotoolkit.ogcapi.dto.geojson.GeoJSONFeature;
import org.geotoolkit.ogcapi.dto.geojson.GeoJSONFeatureCollection;
import org.geotoolkit.ogcapi.dto.geojson.GeoJSONMapper;
import org.geotoolkit.ogcapi.dto.jsonschema.JSONSchema;
import org.geotoolkit.ogcapi.dto.jsonschema.JSONType;
import org.geotoolkit.ogcapi.request.common.GetCollection;
import org.geotoolkit.ogcapi.request.common.GetCollectionList;
import org.geotoolkit.ogcapi.request.common.GetCollectionMetadata;
import org.geotoolkit.ogcapi.request.common.GetCollectionQueryables;
import org.geotoolkit.ogcapi.request.common.GetCollectionSchema;
import org.geotoolkit.ogcapi.request.dggs.GetDggrs;
import org.geotoolkit.ogcapi.request.dggs.GetDggrsDefinition;
import org.geotoolkit.ogcapi.request.dggs.GetDggrsList;
import org.geotoolkit.ogcapi.request.dggs.GetZone;
import org.geotoolkit.ogcapi.request.dggs.GetZoneData;
import org.geotoolkit.ogcapi.request.dggs.GetZoneList;
import org.geotoolkit.referencing.ReferencingUtilities;
import org.apache.sis.referencing.dggs.DiscreteGlobalGrid;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridHierarchy;
import org.geotoolkit.storage.coverage.FeatureSetToCoverageTileGenerator;
import org.apache.sis.storage.dggs.DiscreteGlobalGridGeometry;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystems;
import org.apache.sis.storage.dggs.DiscreteGlobalGridResource;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridSystem;
import org.apache.sis.referencing.dggs.GridConstraints;
import org.apache.sis.storage.dggs.DiscreteGlobalGridSystems;
import org.apache.sis.referencing.dggs.PolyhedronParameters;
import org.apache.sis.referencing.dggs.RefinementStrategy;
import org.apache.sis.referencing.dggs.Zone;
import org.geotoolkit.storage.geojson.GeoJSONStreamWriter;
import org.apache.sis.referencing.rs.Code;
import org.apache.sis.storage.rs.CodeIterator;
import org.apache.sis.storage.rs.CodedCoverage;
import org.apache.sis.storage.rs.CodedGeometry;
import org.apache.sis.storage.rs.CodedResource;
import org.apache.sis.storage.rs.CodeTransform;
import org.apache.sis.referencing.rs.ReferenceSystems;
import org.apache.sis.storage.dggs.DiscreteGlobalGridCoverageProcessor;
import org.apache.sis.storage.dggs.internal.shared.ArrayDiscreteGlobalGridCoverage;
import org.apache.sis.storage.dggs.internal.shared.FeatureSetAsDiscreteGlobalGridResource;
import org.geotoolkit.storage.feature.query.Query;
import org.apache.sis.storage.rs.CodedCoverages;
import org.apache.sis.storage.rs.WritableCodeIterator;
import org.apache.sis.storage.rs.internal.shared.ArrayCodedCoverage;
import org.apache.sis.storage.rs.internal.shared.CodeTransforms;
import org.apache.sis.storage.rs.internal.shared.CodedCoverageAsFeatureSet;
import org.apache.sis.storage.rs.internal.shared.CompoundCodedResource;
import org.apache.sis.storage.rs.internal.shared.s2.S2;
import org.geotoolkit.ubjson.UBJsonMapper;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.ValueReference;
import org.opengis.filter.capability.AvailableFunction;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.citation.Organisation;
import org.opengis.metadata.citation.Party;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.metadata.identification.BrowseGraphic;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;
import org.opengis.util.TypeName;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
@Component("DGGSWorker")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DefaultDGGSWorker extends LayerWorker implements DGGSWorker {

    private static final String LOCAL_UTM = "COMPUTED:UTM";
    private static final String LOCAL_ORTHOGRAPHIC = "COMPUTED:ORTHOGRAPHIC";
    private static final GeographicCRS CRS84 = CommonCRS.WGS84.normalizedGeographic();
    private static final CoordinateReferenceSystem CRS84H;

    private static final Functions ALL_FUNCTIONS;

    static {
        try {
            CRS84H = CRS.compound(CommonCRS.WGS84.normalizedGeographic(), CommonCRS.Vertical.ELLIPSOIDAL.crs());
        } catch (FactoryException ex) {
            throw new IllegalStateException("Failed to create CRS84+H");
        }

        ALL_FUNCTIONS = new Functions();
        for (FunctionRegister fr : org.geotoolkit.filter.function.Functions.getFactories()) {
            for (String name : fr.getNames()) {
                final AvailableFunction af = fr.describe(name);
                final Function fdto = new Function();
                fdto.setName(af.getName().toString());
                fdto.addReturnsItem(asType(af.getReturnType()));

                for (ParameterDescriptor pd : af.getArguments()) {
                    final Argument argument = new Argument();
                    argument.setTitle(pd.getName().toString());
                    argument.setDescription(pd.getDescription().map(Object::toString).orElse(null));
                    argument.addTypeItem(asType(pd.getValueType()));
                    fdto.addArgumentsItem(argument);
                }
                ALL_FUNCTIONS.addFunctionsItem(fdto);
            }
        }
    }

    private final AtomicReference<LandingPage> cacheLandingPage = new AtomicReference<>();
    private final AtomicReference<Collections> cacheCollections = new AtomicReference<>();


    public DefaultDGGSWorker(String id) {
        super(id, ServiceDef.Specification.DGGS);
        started();
    }

    /**
     * Set the current date to the updateSequence parameter
     */
    @Override
    public synchronized void refreshUpdateSequence() {
        cacheLandingPage.set(null);
        cacheCollections.set(null);
    }

    /**
     * Reset work capabilities cache
     */
    @Override
    public synchronized void clearCapabilitiesCache() {
        cacheLandingPage.set(null);
        cacheCollections.set(null);
    }

    private final String getServicePath() {
        final String serviceUrl = getServiceUrl();
        return serviceUrl.replace("?", "");
        //return "/WS/" + specification.toString().toLowerCase() + "/" + id;
    }

    private static List<Link> buildAlternates(String base) {
        return List.of(
            new Link(base + "?f=html", LinkRelations.ALTERNATE, MediaType.TEXT_HTML_VALUE, null, "Html", null),
            new Link(base + "?f=json", LinkRelations.ALTERNATE, MediaType.APPLICATION_JSON_VALUE, null, "Json", null),
            new Link(base + "?f=yaml", LinkRelations.ALTERNATE, MediaType.APPLICATION_YAML_VALUE, null, "Yaml", null),
            new Link(base + "?f=xml", LinkRelations.ALTERNATE, MediaType.APPLICATION_XML_VALUE, null, "Xml", null)
        );
    }

    @Override
    public LandingPage getLandingPage() throws CstlServiceException {

        LandingPage dto = cacheLandingPage.get();
        if (dto != null) return dto;
        dto = new LandingPage();
        dto.setTitle("OGC-API DGGS by Examind");

        //alternates
        dto.getLinks().addAll(buildAlternates(getServicePath()+"/"));

        //conformance
        dto.addLinksItem(new Link(getServicePath() + "/conformance",LinkRelations.OGC_DGGRS_LIST,null,null,"Conformance",null));
        //API description
        dto.addLinksItem(new Link(getServicePath() + "/api?f=json",LinkRelations.SERVICE_DESC,null,null,"Service description (OpenAPI 3.0)",null));
        //API doc
        dto.addLinksItem(new Link(getServicePath() + "/api?f=html",LinkRelations.SERVICE_DOC,null,null,"Service documentation (Swagger)",null));
        //collections
        dto.addLinksItem(new Link(getServicePath() + "/collections",LinkRelations.OGC_DGGRS_LIST,null,null,"Collections",null));
        //dggs
        dto.addLinksItem(new Link(getServicePath() + "/dggs",LinkRelations.OGC_DGGRS_LIST,null,null,"Discrete Global Grid Reference Systems",null));
        //functions
        dto.addLinksItem(new Link(getServicePath() + "/functions","OGC Feature functions",null,null,"CQL2 functions",null));

        //add a tracking value to the landing page for client to check if service has been updated
        dto.setOtherField("updateSequence", Instant.now().toEpochMilli());

        cacheLandingPage.set(dto);
        return dto;
    }

    @Override
    public String getAPI() throws CstlServiceException {

        String json;
        try (InputStream in = DefaultDGGSWorker.class.getResourceAsStream("/com/examind/dggs/openapi.json")) {
            json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new CstlServiceException(ex.getMessage(), ex);
        }

        final Collections collectionList = getCollectionList(new GetCollectionList());
        final StringJoiner coljoiner = new StringJoiner(",");
        for (CollectionDescription desc : collectionList.getCollections()) {
            coljoiner.add("\"" + desc.getId() + "\"");
        }

        final StringJoiner dggrsjoiner = new StringJoiner(",");
        for (String entry : DiscreteGlobalGridReferenceSystems.listDggrs()) {
            dggrsjoiner.add("\"" + entry + "\"");
        }

        json = json.replace("$${collection}", coljoiner.toString());
        json = json.replace("$${dggrs}", dggrsjoiner.toString());
        json = json.replace("$${serviceUrl}", getServiceUrl());

        return json;
    }

    @Override
    public Collections getCollectionList(GetCollectionList parameters) throws CstlServiceException {
        final String userLogin  = getUserLogin();

        Collections dto = cacheCollections.get();
        if (dto != null) return dto;
        dto = new Collections();

        final List<LayerCache> layers = getLayerCaches(userLogin);

        //alternates
        dto.getLinks().addAll(buildAlternates(getServicePath()+"/collections"));

        final List<CollectionDescription> items = new ArrayList<>();

        for (LayerCache layer : layers) {
            items.add(describe(layer, false));
        }

        dto.setCollections(items);
        dto.setNumberMatched(dto.getCollections().size());
        dto.setNumberReturned(dto.getCollections().size());
        cacheCollections.set(dto);
        return dto;
    }

    @Override
    public CollectionDescription getCollection(GetCollection parameters) throws CstlServiceException {
        final String userLogin  = getUserLogin();
        final LayerCache layer = getLayerCache(userLogin, decodeCollectionId(parameters.getCollectionId()));

        return describe(layer, true);
    }

    @Override
    public JSONSchema getCollectionSchema(GetCollectionSchema parameters) throws CstlServiceException {
        final String collectionId = decodeCollectionId(parameters.getCollectionId());

        final String userLogin  = getUserLogin();
        final LayerCache layer = getLayerCache(userLogin, collectionId);
        final Resource resource = layer.getData().getOrigin();

        final CodedResource dgr;
        try {
            final DiscreteGlobalGridReferenceSystem dggrs = new HealpixDggrs();
            if (resource instanceof CodedResource d) {
                dgr = d;
            } else if (resource instanceof GridCoverageResource gcr) {
                dgr = CodedCoverages.viewAsDggrs(gcr.getIdentifier().get(), gcr, dggrs);
            } else if (resource instanceof FeatureSet fs) {
                dgr = DiscreteGlobalGridSystems.viewAsDggrs(fs, dggrs, new DiscreteGlobalGridCoverageProcessor());
            }  else {
                throw new CstlServiceException("Resource not supported");
            }

            return toJsonSchema(dgr.getSampleDimensions());
        } catch (DataStoreException | IncommensurableException | TransformException | FactoryException ex) {
            throw new CstlServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public JSONSchema getCollectionQueryables(GetCollectionQueryables parameters) throws CstlServiceException {
        //same as base schema
        return getCollectionSchema(new GetCollectionSchema().collectionId(parameters.getCollectionId()));
    }

    @Override
    public String getCollectionMetadata(GetCollectionMetadata parameters) throws CstlServiceException {
        final String collectionId = decodeCollectionId(parameters.getCollectionId());

        final String userLogin  = getUserLogin();
        final LayerCache layer = getLayerCache(userLogin, collectionId);
        try {
            final Metadata metadata = layer.getData().getOrigin().getMetadata();

            if ("dublin".equals(parameters.getFormat())) {
                //TODO : extract conversion methods from other services and make a DUBLIN utiliy class
                return XML.marshal(metadata);
            } else {
                //fallback on ISO-19115
                return XML.marshal(metadata);
            }

        } catch (DataStoreException | JAXBException ex) {
            throw new CstlServiceException("Failed to get metadata for " + collectionId, ex);
        }
    }

    @Override
    public Functions getFunctions() throws CstlServiceException {
        return ALL_FUNCTIONS;
    }

    private static ValueType asType(TypeName tn) {
        //todo
        return ValueType.STRING;
    }

    private CollectionDescription describe(LayerCache layer, boolean full) {

        final Data data = layer.getData();
        final Resource resource = data.getOrigin();

        final CollectionDescription dto = new CollectionDescription();
        final String id = encodeCollectionId(layer.getName().getLocalPart());
        dto.id(id);
        dto.title(layer.getName().getLocalPart());

        if (resource instanceof GridCoverageResource) {
            dto.setDataType("coverage");
        } else if (resource instanceof FeatureSet) {
            dto.setDataType("vector");
        } else {
            dto.setDataType("undefined");
        }

        //extract the extent
        try {
            final Extent extent = new Extent();
            dto.setExtent(extent);

            if (resource instanceof GridCoverageResource gcr) {
                try {
                    GridGeometry gridGeometry = gcr.getGridGeometry();
                    fillExtent(extent, gridGeometry);
                } catch (DataStoreException | FactoryException | TransformException ex) {
                    fillExtent(extent, data.getEnvelope());
                    fillExtent(extent, data.getDateRange());
                }

            } else {
                //use the envelope
                fillExtent(extent, data.getEnvelope());
                fillExtent(extent, data.getDateRange());
            }

        } catch (ConstellationStoreException ex) {
            LOGGER.log(Level.FINE, ex.getMessage(), ex);
        }

        if (full) {

            //alternates
            dto.getLinks().addAll(buildAlternates(getServicePath()+"/collections/"+id));

            //dggs
            dto.addLinksItem(new Link(getServicePath()+ "/collections/" + id + "/dggs", LinkRelations.OGC_DGGRS_LIST, null, null, "Discrete Global Grid Systems", null));

            //schema
            dto.addLinksItem(new Link(getServicePath()+ "/collections/" + id + "/schema", LinkRelations.OGC_FEATURE_SCHEMA, null, null, "Schema", null));

            //queryables
            dto.addLinksItem(new Link(getServicePath()+ "/collections/" + id + "/queryables", LinkRelations.OGC_FEATURE_QUERYABLES, null, null, "Queryables", null));

            //metadata
            dto.addLinksItem(new Link(getServicePath()+ "/collections/" + id + "/metadata", LinkRelations.OGC_DATA_META, null, null, "Metadata", null));
        }

        return dto;
    }

    @Override
    public DggrsListResponse getDggrsList(GetDggrsList parameters) throws CstlServiceException {
        final DggrsListResponse dto = new DggrsListResponse();
        final String collectionId = decodeCollectionId(parameters.getCollectionId());

        final String basePath;
        if (collectionId == null) {
            //general description list
            basePath = getServicePath()+"/dggs";
        } else {
            //description for a single data collection
            basePath = getServicePath()+ "/collections/" + encodeCollectionId(collectionId) + "/dggs";
        }

        //alternates
        dto.getLinks().addAll(buildAlternates(basePath));

        //link to root
        dto.addLinksItem(new Link(getServicePath() + "/", LinkRelations.OGC_DATASET, null, null, "Root OGC Web API", null));

        Set<String> listDggrs = DiscreteGlobalGridReferenceSystems.listDggrs();

        //link to base geodata
        if (collectionId != null) {
            dto.addLinksItem(new Link(getServicePath()+ "/collections/" + encodeCollectionId(collectionId), LinkRelations.OGC_GEODATA, null, null, collectionId + " dataset", null));

            final String userLogin  = getUserLogin();
            final LayerCache layer = getLayerCache(userLogin, collectionId);
            final Resource resource = layer.getData().getOrigin();

            if (resource instanceof CodedResource cr) {
                listDggrs = new TreeSet<>();
                try {
                    for (CodedGeometry cg : cr.getAlternateGridGeometry()) {
                        List<ReferenceSystem> rss = ReferenceSystems.getSingleComponents(cg.getReferenceSystem(), false);
                        for (ReferenceSystem rs : rss) {
                            if (rs instanceof DiscreteGlobalGridReferenceSystem dggrs) {
                                listDggrs.add(dggrs.getName().getCode());
                            }
                        }
                    }
                } catch (DataStoreException ex) {
                    throw new CstlServiceException(ex);
                }
            }
        }

        for (String dggrsId : listDggrs) {
            final DiscreteGlobalGridReferenceSystem dggrs;
            try {
                dggrs = DiscreteGlobalGridReferenceSystems.forCode(dggrsId);
            } catch (FactoryException ex) {
                throw new CstlServiceException(ex);
            }
            final DggrsItem item = new DggrsItem();
            item.setId(dggrsId);
            item.setTitle(dggrs.getName() == null ? dggrsId : dggrs.getName().toString());
            item.setUri(dggrs.getUri());

            final Party owner = dggrs.getOverallOwner();
            if (owner instanceof Organisation org && !org.getLogo().isEmpty()) {
                BrowseGraphic logo = org.getLogo().iterator().next();
                if (logo.getFileName() != null) {
                    item.setOtherField("logo", logo.getFileName().toString());
                }
            }

            //link to zone queries
            item.addLinksItem(new Link(basePath + "/" + dggrsId + "/zones", LinkRelations.OGC_DGGRS_ZONE_QUERY, null, null, dggrsId +" zone querying", null));

            //link to dggrs
            item.addLinksItem(new Link(basePath + "/" + dggrsId, LinkRelations.SELF, null, null, dggrsId + " DGGRS", null));

            //link to dggrs definition
            item.addLinksItem(new Link(basePath + "/" + dggrsId + "/definition", LinkRelations.OGC_DGGRS_DEFINITION, null, null, dggrsId +" DGGRS definition", null));

            dto.addDggrsItem(item);
        }
        return dto;
    }

    @Override
    public Dggrs getDggrs(GetDggrs parameters) throws CstlServiceException {
        final String dggrsId = parameters.getDggrsId();
        final DiscreteGlobalGridReferenceSystem dggrs;
        try {
            dggrs = DiscreteGlobalGridReferenceSystems.forCode(dggrsId);
        } catch (FactoryException ex) {
            throw new CstlServiceException("DGGRS not found", ex);
        }
        final String collectionId = decodeCollectionId(parameters.getCollectionId());


        final String basePath;
        if (collectionId == null) {
            //general description list
            basePath = getServicePath()+"/dggs";
        } else {
            //description for a single data collection
            basePath = getServicePath()+ "/collections/" + encodeCollectionId(collectionId) + "/dggs";
        }

        final String crsId = IdentifiedObjects.getIdentifierOrName(dggrs.getGridSystem().getCrs());

        final Dggrs dto = new Dggrs();
        dto.setId(dggrsId);
        dto.setTitle(dggrs.getName() == null ? dggrsId : dggrs.getName().toString());
        dto.setDescription(dggrs.getDescription().map(i -> i.toString()).orElse(null));
        dto.setKeywords(dggrs.getKeywords());
        dto.setUri(dggrs.getUri());
        dto.setCrs(new Crs().plain(crsId));
        dto.setDefaultDepth(getDefaultDepth(dggrs));
        dto.setMaxRelativeDepth(getMaxRelativeDepth(dggrs));
        dto.setMaxRefinementLevel(dggrs.getGridSystem().getHierarchy().getGrids().size()-1);

        final Party owner = dggrs.getOverallOwner();
        if (owner instanceof Organisation org && !org.getLogo().isEmpty()) {
            BrowseGraphic logo = org.getLogo().iterator().next();
            if (logo.getFileName() != null) {
                dto.setOtherField("logo", logo.getFileName().toString());
            }
        }

        //alternates
        dto.getLinks().addAll(buildAlternates(basePath+"/"+dggrsId));

        //link to root
        dto.addLinksItem(new Link(getServicePath() + "/", LinkRelations.OGC_DATASET, null, null, "Root OGC Web API", null));

        //link to dggrs
        dto.addLinksItem(new Link(basePath + "/" + dggrsId, LinkRelations.SELF, null, null, dggrsId + " DGGRS", null));

        //link to dggrs definition
        dto.addLinksItem(new Link(getServicePath() + "/dggs/" + dggrsId + "/definition", LinkRelations.OGC_DGGRS_DEFINITION, MediaType.APPLICATION_JSON_VALUE, null, dggrsId +" DGGRS definition", null));

        //link to zone queries
        dto.addLinksItem(new Link(basePath + "/" + dggrsId + "/zones", LinkRelations.OGC_DGGRS_ZONE_QUERY, null, null, dggrsId +" zone querying", null));

        //link to base geodata
        if (collectionId != null) {
            dto.addLinksItem(new Link(getServicePath()+ "/collections/" + encodeCollectionId(collectionId), LinkRelations.OGC_GEODATA, null, null, collectionId + " dataset", null));
            //metadata
            dto.addLinksItem(new Link(getServicePath()+ "/collections/" + encodeCollectionId(collectionId) + "/metadata", LinkRelations.OGC_DATA_META, null, null, collectionId + " metadata", null));
        }

        //link templates
        final DggrsLinkTemplatesInner templateZone = new DggrsLinkTemplatesInner();
        templateZone.setRel(LinkRelations.OGC_DGGRS_ZONE_INFO);
        templateZone.setTitle("DGGS zone information");
        templateZone.setUriTemplate(basePath + "/" + dggrsId + "/zones/{zoneId}");
        dto.addLinkTemplatesItem(templateZone);

        final DggrsLinkTemplatesInner templateZoneData = new DggrsLinkTemplatesInner();
        templateZoneData.setRel(LinkRelations.OGC_DGGRS_ZONE_DATA);
        templateZoneData.setTitle("DGGS zone data");
        templateZoneData.setUriTemplate(basePath + "/" + dggrsId + "/zones/{zoneId}/data");
        dto.addLinkTemplatesItem(templateZoneData);

        return dto;
    }

    @Override
    public DggrsDefinition getDggrsDefinition(GetDggrsDefinition parameters) throws CstlServiceException {
        final String dggrsId = parameters.getDggrsId();
        final DiscreteGlobalGridReferenceSystem dggrs;
        try {
            dggrs = DiscreteGlobalGridReferenceSystems.forCode(dggrsId);
        } catch (FactoryException ex) {
            throw new CstlServiceException("DGGRS not found", ex);
        }

        final DiscreteGlobalGridSystem dggs = dggrs.getGridSystem();
        final List<GridConstraints> cellConstraints = dggs.getGridConstraints();
        final PolyhedronParameters params = dggs.getParameters();
        final Ellipsoid ellipsoid = DatumOrEnsemble.asDatum((GeographicCRS)dggs.getCrs()).getEllipsoid();

        final org.apache.sis.referencing.dggs.SubZoneOrder subZoneOrder = dggrs.getSubZoneOrder();
        final SubZoneOrder szo = new SubZoneOrder();
        szo.setDescription(subZoneOrder.getDescription());
        szo.setType(subZoneOrder.name());

        final DggsOrientation orientation = new DggsOrientation();
        orientation.setAzimuth(params.getOrientation().getAzimuth());
        orientation.setDescription(params.getOrientation().getDescription());
        orientation.setLatitude(params.getOrientation().getLatitude());
        orientation.setLongitude(params.getOrientation().getLongitude());

        final DggsParameters dggsparameters = new DggsParameters();
        try {
            dggsparameters.setEllipsoid(new URI(IdentifiedObjects.lookupURN(ellipsoid, null)));
        } catch (FactoryException | URISyntaxException ex) {
            throw new CstlServiceException(ex.getMessage(), ex);
        }
        dggsparameters.setOrientation(orientation);

        final DggsConstraints constraints = new DggsConstraints();
        constraints.setCellAxisAligned(cellConstraints.contains(GridConstraints.cellAxisAligned));
        constraints.setCellConformal(cellConstraints.contains(GridConstraints.cellConformal));
        constraints.setCellEqualSized(cellConstraints.contains(GridConstraints.cellEquiSized));
        constraints.setCellEquiAngular(cellConstraints.contains(GridConstraints.cellEquiAngular));
        constraints.setCellEquiDistant(cellConstraints.contains(GridConstraints.cellEquiDistant));


        final DggsDefinition definition = new DggsDefinition();
        definition.setBasePolyhedron(dggs.getBasePolyhedron());
        definition.setConstraints(constraints);
        try {
            definition.setCrs(new Crs().uri(new URI(IdentifiedObjects.lookupURN(dggs.getCrs(), null))));
        } catch (FactoryException | URISyntaxException ex) {
            throw new CstlServiceException(ex.getMessage(), ex);
        }
        final List<DggsDefinition.RefinementStrategyEnum> strats = new ArrayList<>();
        for (RefinementStrategy rs : dggs.getRefinementStrategy()) {
            if (RefinementStrategy.centredChildCell.equals(rs))      strats.add(DggsDefinition.RefinementStrategyEnum.CENTRED_CHILD_CELL);
            else if (RefinementStrategy.edgeCentredChildCell.equals(rs))  strats.add(DggsDefinition.RefinementStrategyEnum.EDGE_CENTRED_CHILD_CELL);
            else if (RefinementStrategy.faceCentredChildCell.equals(rs))  strats.add(DggsDefinition.RefinementStrategyEnum.FACE_CENTRED_CHILD_CELL);
            else if (RefinementStrategy.nestedChildCell.equals(rs))       strats.add(DggsDefinition.RefinementStrategyEnum.NESTED_CHILD_CELL);
            else if (RefinementStrategy.nodeCentredChildCell.equals(rs))  strats.add(DggsDefinition.RefinementStrategyEnum.NODE_CENTRED_CHILD_CELL);
            else if (RefinementStrategy.solidCentredChildCell.equals(rs)) strats.add(DggsDefinition.RefinementStrategyEnum.SOLID_CENTRED_CHILD_CELL);
        }
        definition.setRefinementRatio(dggs.getRefinementRatio());
        definition.setRefinementStrategy(strats);
        definition.setSpatialDimensions(dggs.getSpatialDimensions());
        definition.setTemporalDimensions(dggs.getTemporalDimensions());
        definition.setZoneTypes(dggs.getZoneTypes());

        final Dggs dtoDggs = new Dggs();
        dtoDggs.setDefinition(definition);
        dtoDggs.setParameters(dggsparameters);

        final ZirsText textZirs = new ZirsText();
        //TODO fill ZIRS, but we don't have those yet in the DGGRS Java API
        //textZirs.setDescription(null);
        //textZirs.setType(null);

        final Zirs zirs = new Zirs();
        zirs.setTextZIRS(textZirs);

        if (dggrs.getZonalSystem().supportUInt64Form()) {
            final ZirsUInt64 uint64Zirs = new ZirsUInt64();
            uint64Zirs.setDescription(null);
            uint64Zirs.setType(null);
            zirs.setUint64ZIRS(uint64Zirs);
        }

        final DggrsDefinition dto = new DggrsDefinition();
        dto.setTitle(dggrs.getName() == null ? dggrsId : dggrs.getName().toString());
        dto.setDescription(dggrs.getDescription().map(i -> i.toString()).orElse(null));
        dto.setUri(dggrs.getUri());
        dto.setSubZoneOrder(szo);
        dto.setDggh(dtoDggs);
        dto.setZirs(zirs);

        return dto;
    }

    @Override
    public ResponseObject getDggrsZoneList(GetZoneList parameters) throws CstlServiceException {

        Integer offset = parameters.getOffset();
        Integer limit = parameters.getLimit();
        List<String> subsets = parameters.getSubset();
        String filterLang = parameters.getFilterLang();
        String dggrsId = parameters.getDggrsId();
        String outputCrsTxt = parameters.getCrs();
        String collectionId = (parameters.getCollectionId() == null || parameters.getCollectionId().isEmpty()) ? null : parameters.getCollectionId().get(0);
        String subsetCrsStr = parameters.getSubsetCrs();
        Envelope bbox = parameters.getBbox();
        String datetime = parameters.getDatetime();
        String parentZone = parameters.getParentZone();
        Integer zoneLevel = parameters.getZoneLevel();
        String filterCql = parameters.getFilter();
        Boolean compactZones = parameters.getCompactZones();
        String mediaType = parameters.getFormat();
        String profile = parameters.getProfile();
        String geometry = parameters.getGeometry();

        if (offset == null) offset = 0;
        if (limit == null) limit = 10000;
        if (subsets == null) subsets = java.util.Collections.EMPTY_LIST;

        if (filterLang != null) throw new CstlServiceException("filterLang parameter not supported yet");

        final DiscreteGlobalGridReferenceSystem dggrs;
        try {
            dggrs = DiscreteGlobalGridReferenceSystems.forCode(dggrsId);
        } catch (FactoryException ex) {
            throw new CstlServiceException("DGGRS not found", ex);
        }

        if (outputCrsTxt == null || outputCrsTxt.isBlank()) outputCrsTxt = "CRS:84";

        final DggrsZonesResponse dto = new DggrsZonesResponse();

        //alternates
        dto.getLinks().addAll(buildAlternates(getServicePath()+"/dggs/"+dggrsId+"/zones"));

        //link to root
        dto.addLinksItem(new Link(getServicePath() + "/", LinkRelations.OGC_DATASET, null, null, "Root OGC Web API", null));

        //link to dggrs
        if (collectionId == null) {
            dto.addLinksItem(new Link(getServicePath() + "/dggs/" + dggrsId, LinkRelations.SELF, null, null, dggrsId + " DGGRS", null));
        } else {
            dto.addLinksItem(new Link(getServicePath() + "/dggs/" + dggrsId, LinkRelations.OGC_DGGRS, null, null, dggrsId + " DGGRS", null));
        }

        //link to dggrs definition
        dto.addLinksItem(new Link(getServicePath() + "/dggs/" + dggrsId + "/definition", LinkRelations.OGC_DGGRS_DEFINITION, null, null, dggrsId +" DGGRS definition", null));

        //map each subset parameter to a bbox
        final CoordinateReferenceSystem subsetcrs = DGGSService.parseCrs(subsetCrsStr);

        //extract the data envelope to narrow the search
        if (collectionId != null) {
            try {
                collectionId = decodeCollectionId(collectionId);
                final String userLogin  = getUserLogin();
                final LayerCache layer = getLayerCache(userLogin, collectionId);

                GeneralEnvelope env;
                if (bbox == null) {
                    final Envelope lenv = layer.getEnvelope();
                    if (lenv != null) {
                        env = new GeneralEnvelope(lenv);
                    } else {
                        env = new GeneralEnvelope(CRS.getDomainOfValidity(CRS84));
                    }
                } else {
                    final Envelope resourceEnv = layer.getEnvelope(bbox.getCoordinateReferenceSystem());

                    if (resourceEnv != null) {
                        //intersect with searched envelope
                        final GeneralEnvelope bb = new GeneralEnvelope(bbox);
                        bb.intersect(resourceEnv);
                        env = bb;
                    } else {
                        env = new GeneralEnvelope(bbox);
                    }
                }

                if (!env.isFinite()) {
                    env = new GeneralEnvelope(CRS.getDomainOfValidity(CRS84));
                }

                //see if we add some subset parameters
                if (subsetcrs == null) {
                    for (String subset : subsets) {
                        final Entry<String, NumberRange<Double>> entry = parseSubsetRange(subset);
                        final String axisName = entry.getKey();
                        final NumberRange<Double> range = entry.getValue();

                        //subset must not be part of the user query bbox
                        if (bbox != null) {
                            Integer idx = null;
                            try {
                                idx = getAxisIndex(bbox.getCoordinateReferenceSystem(), axisName);
                            } catch (IllegalArgumentException ex) {
                                //ok not found
                            }
                            if (idx != null) {
                                throw new DGGSService.BadParameterException("Subset " + axisName + " can not be used with bbox parameter");
                            }
                        }
                        final int idx = getAxisIndex(env.getCoordinateReferenceSystem(), axisName);
                        final GeneralEnvelope clip = new GeneralEnvelope(env);
                        clip.setRange(idx, range.getMinDouble(), range.getMaxDouble());
                        env.intersect(clip);
                    }
                } else {
                    //convert the subset to a bbox
                    GeneralEnvelope clip;
                    if (subsetCrsStr == null) {
                        clip = new GeneralEnvelope(CRS84H);
                        clip.setRange(0, -180, 180);
                        clip.setRange(1, -90, 90);
                        clip.setRange(2, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
                    } else {
                        clip = new GeneralEnvelope(subsetcrs);
                        clip.setToInfinite();
                    }

                    for (String subset : subsets) {
                        final Entry<String, NumberRange<Double>> entry = parseSubsetRange(subset);
                        final String axisName = entry.getKey();
                        final NumberRange<Double> range = entry.getValue();
                        final int idx = getAxisIndex(subsetcrs, axisName);
                        clip.setRange(idx, range.getMinDouble(), range.getMaxDouble());
                    }
                    try {
                        env = ReferencingUtilities.intersectEnvelopes(env, clip);
                    } catch (TransformException ex) {
                        throw new CstlServiceException(ex);
                    }
                }

                //add the datetime as subset
                final Optional<String> datetimeToSubset = datetimeToSubset(datetime);
                if (datetimeToSubset.isPresent()) {
                    final Entry<String, NumberRange<Double>> entry = parseSubsetRange(datetimeToSubset.get());
                    final String axisName = entry.getKey();
                    final NumberRange<Double> range = entry.getValue();
                    final int idx = getAxisIndex(env.getCoordinateReferenceSystem(), axisName);
                    final GeneralEnvelope clip = new GeneralEnvelope(env);
                    clip.setRange(idx, range.getMinDouble(), range.getMaxDouble());
                    env.intersect(clip);
                }

                bbox = env;

            } catch (ConstellationStoreException ex) {
                throw new CstlServiceException(ex);
            }
        } else {
            //convert subset to a bbox
            if (!subsets.isEmpty() && bbox != null) {
                throw new DGGSService.BadParameterException("Can not combine subset parameter with bbox parameter when no collection has been selected");
            }

            if (!subsets.isEmpty()) {
                //convert the subset to a bbox
                final GeneralEnvelope env;
                if (subsetCrsStr == null) {
                    env = new GeneralEnvelope(CRS84H);
                    env.setRange(0, -180, 180);
                    env.setRange(1, -90, 90);
                    env.setRange(2, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
                } else {
                    env = new GeneralEnvelope(subsetcrs);
                }

                for (String subset : subsets) {
                    final Entry<String, NumberRange<Double>> entry = parseSubsetRange(subset);
                    final String axisName = entry.getKey();
                    final NumberRange<Double> range = entry.getValue();
                    final int idx = getAxisIndex(subsetcrs, axisName);
                    env.setRange(idx, range.getMinDouble(), range.getMaxDouble());
                }
                bbox = env;
            }
        }


        //extract zones
        Stream<Zone> result;
        try {
            Stream<Zone> cdts;

            if (bbox != null && (!new GeneralEnvelope(bbox).isFinite())) {
                //no intersection or empty request
                cdts = Stream.empty();
            } else {
                final DiscreteGlobalGridHierarchy hierarchy = dggrs.getGridSystem().getHierarchy();
                if (parentZone != null) {
                    final Zone root = hierarchy.getZone(parentZone);
                    if (zoneLevel == null) {
                        //arbitrary precision
                        cdts = root.getChildrenAtRelativeDepth(root.getLocationType().getRefinementLevel() - 8);
                    } else {
                        cdts = root.getChildrenAtRelativeDepth(zoneLevel - root.getLocationType().getRefinementLevel());
                    }

                    if (bbox != null) {
                        final S2Polygon intersection = S2.toS2Polygon(Envelopes.transform(bbox, CRS84));
                        cdts = cdts.filter((Zone t) -> {
                            final S2Polygon p = DiscreteGlobalGridSystems.toS2Polygon(t.getGeographicExtent());
                            return p == null || p.intersects(intersection);
                        });
                    }

                } else {
                    if (bbox == null) bbox = CRS.getDomainOfValidity(CRS84);
                    if (zoneLevel == null) {
                        cdts = Stream.empty();
                        for (DiscreteGlobalGrid dgg : hierarchy.getGrids()) {
                            List<Zone> zones = dgg.getZones(bbox).toList();
                            cdts = zones.stream();
                            if (zones.size() > dggrs.getGridSystem().getRefinementRatio()) {
                                break;
                            }
                        }

//                        cdts = DiscreteGlobalGridSystems.firstIntersect(dggrs, bbox).stream();
                    } else {
                        cdts = dggrs.getGridSystem().getHierarchy().getGrids().get(zoneLevel).getZones(bbox);
                    }
                }
            }
            result = cdts;

        } catch (TransformException ex) {
            throw new CstlServiceException(ex);
        }

        //we must read the datas to check the filter
        if (filterCql != null && !filterCql.isBlank()) {
            if (collectionId == null) {
                throw new DGGSService.BadParameterException("Collection id must be provided to use CQL filter");
            }

            final Filter filter;
            try {
                filter = CQL.parseFilter(filterCql);
            } catch (CQLException ex) {
                throw new DGGSService.BadParameterException("Invalid CQL filter : " + filterCql);
            }
            final CodedResource rgr = getCodedResource(collectionId, dggrs);

            final List<Zone> zones = result.toList();
            final List<Object> zids = zones.stream().map(Zone::getIdentifier).toList();
            result.close();
            final CodedGeometry horizontal = DiscreteGlobalGridGeometry.unstructured(dggrs, zids, null);

            final CodedGeometry query = CodedGeometry.compound(horizontal);

            try {
                final CodedCoverage coverage = rgr.read(query);

                final Set<Zone> filtered = new HashSet<>();
                final CodedCoverageAsFeatureSet fs = new CodedCoverageAsFeatureSet(coverage, false, geometry);

                final CodeIterator iterator = coverage.createIterator();
                while (iterator.next()) {
                    final Feature feature = fs.viewAsFeature(iterator);
                    if (filter.test(feature)) {
                        int i = iterator.getPosition()[0];
                        filtered.add(zones.get(i));
                    }
                }

                result = filtered.stream().sorted();

            } catch (DataStoreException ex) {
                throw new CstlServiceException("Failed to read datas", ex);
            }
        }


        final Stream<Zone> candidates;
        {
            //apply offset and limit
            result = result.skip(offset);
            result = result.limit(limit);

            //compact zones
            if (compactZones == null || Boolean.TRUE.equals(compactZones)) {
                List<Zone> zones = result.toList();
                result.close();
                zones = DiscreteGlobalGridSystems.compact(zones, zoneLevel == null ? 0 : 0);
                candidates = zones.stream();
            } else {
                candidates = result;
            }
        }


        if (MediaTypes.ZONELIST_UINT64.equals(mediaType)) {

            try (candidates) {
                long[] array = candidates.mapToLong(Zone::getLongIdentifier).toArray();
                final ByteBuffer bb = ByteBuffer.allocate((array.length+1) * 8);
                final LongBuffer lb = bb.order(ByteOrder.LITTLE_ENDIAN).asLongBuffer();
                lb.put((long)array.length);
                lb.put(array);
                return new ResponseObject(bb.array(), mediaType);
            }

        } else if (MediaTypes.ZONELIST_JSON.equals(mediaType)
                || MediaTypes.ZONELIST_HTML.equals(mediaType)) {

            double area = 0;
            try (candidates) {
                final Iterator<Zone> ite = candidates.iterator();
                while (ite.hasNext()) {
                    final Zone zone = ite.next();
                    final String zoneId = zone.getTextIdentifier().toString();
                    dto.addZonesItem(zoneId);
                    Double cdt = zone.getAreaMetersSquare();
                    if (cdt != null) area += cdt;
                }
            }

            dto.setReturnedAreaMetersSquare(area);

            //todo need a ND data to compute this
            dto.setReturnedHyperVolumeMetersCubeSeconds(null);
            dto.setReturnedVolumeMetersCube(null);
            dto.setReturnedVolumeMetersSquareSeconds(null);

            //add best preview sub-level, used in UI
            final int linkLevel;
            final int previewLevel;
            switch (dggrs.getGridSystem().getRefinementRatio()) {
                case 2  : linkLevel = 7; previewLevel = 9; break;
                case 3  : linkLevel = 7; previewLevel = 9; break;
                case 4  : linkLevel = 6; previewLevel = 8; break;
                case 5  : linkLevel = 5; previewLevel = 7; break;
                case 6  : linkLevel = 5; previewLevel = 7; break;
                case 7  : linkLevel = 4; previewLevel = 5; break;
                case 8  : linkLevel = 4; previewLevel = 5; break;
                case 9  : linkLevel = 4; previewLevel = 5; break;
                default : linkLevel = 3; previewLevel = 4; break;
            }
            dto.setOtherField("_linkZoneDepth", linkLevel);
            dto.setOtherField("_previewZoneDepth", previewLevel);


            return new ResponseObject(dto, MediaType.APPLICATION_JSON);

        } else if (MediaTypes.ZONELIST_GEOJSON.equals(mediaType)) {

            final List<Object> zids;
            try (candidates) {
                zids = candidates.map(Zone::getIdentifier).toList();
            }

            final DiscreteGlobalGridGeometry gridGeometry = DiscreteGlobalGridGeometry.unstructured(dggrs, zids, null);
            final CodedCoverage dggsCoverage = new ArrayDiscreteGlobalGridCoverage(Names.createLocalName(null, null, "result"), gridGeometry, new ArrayList());

            final Envelope dataEnv = dggsCoverage.getEnvelope().get();
            final double lon = dataEnv.getMedian(0);
            final double lat = dataEnv.getMedian(1);
            //parse the crs
            final CoordinateReferenceSystem crs;
            try {
                crs = toCRS(new DirectPosition2D(lon, lat), outputCrsTxt);
            } catch (FactoryException ex) {
                return new ResponseObject(ex.getMessage(), MediaType.TEXT_PLAIN, 406);
            }

            try {
                final byte[] data = toGeojson(geometry, profile, crs, dggsCoverage);
                return new ResponseObject(data, MediaType.APPLICATION_JSON);
            } catch (IOException | DataStoreException ex) {
                throw new CstlServiceException(ex);
            }

        } else if (MediaTypes.ZONELIST_GEOTIFF.equals(mediaType)) {

            final List<Object> zids;
            try (candidates) {
                zids = candidates.map(Zone::getIdentifier).toList();
            }

            final DiscreteGlobalGridGeometry dggsGeometry = DiscreteGlobalGridGeometry.unstructured(dggrs, zids, null);
            final CodedCoverage dggsCoverage = new ArrayDiscreteGlobalGridCoverage(Names.createLocalName(null, null, "result"),dggsGeometry, new ArrayList());

            final Envelope dataEnv = dggsCoverage.getEnvelope().get();
            final double lon = dataEnv.getMedian(0);
            final double lat = dataEnv.getMedian(1);

            //parse the crs
            final CoordinateReferenceSystem crs;
            try {
                crs = toCRS(new DirectPosition2D(lon, lat), outputCrsTxt);
            } catch (FactoryException ex) {
                return new ResponseObject(406);
            }
            try {
                final Envelope envelope = dggsCoverage.getGeometry().getEnvelope(crs);

                //commpute a grid size based on the number of zones we have
                int width = (int) Math.sqrt(((DiscreteGlobalGridGeometry)dggsCoverage.getGeometry()).getZoneIds().size()*1.5);
                if (width > 512) width = 512;
                if (width < 32) width = 32;

                final GridGeometry gridGeometry = new GridGeometry(new GridExtent(width, width), envelope, GridOrientation.REFLECTION_Y);

                final FeatureSet fs = DiscreteGlobalGridSystems.viewAsFeatureSet(dggsCoverage, true, geometry);
                final FeatureSetToCoverageTileGenerator tg = new FeatureSetToCoverageTileGenerator();
                tg.setAntialiasing(false);
                tg.setFeatureSet(fs);
                tg.setFeatureToSamples(new BiFunction<Feature, Integer, double[]>() {
                    @Override
                    public double[] apply(Feature t, Integer u) {
                        if (t == null) return new double[]{0};
                        long l = (long) t.getPropertyValue(AttributeConvention.IDENTIFIER);
                        return new double[]{Double.longBitsToDouble(l)};
                    }
                });
                final SampleDimension sd = new SampleDimension.Builder().setName("zid").build();
                tg.setSampleDimensions(List.of(sd));
                tg.setTemplate(new ImageBuilder().setSize(1, 1).setNumBands(1).setDataType(DataBuffer.TYPE_DOUBLE).createBufferedImage());

                final GridCoverage gridCoverage = tg.generate(gridGeometry);
                final byte[] data = toGeotiff(gridCoverage);
                return new ResponseObject(data, MediaTypes.DATA_GEOTIFF);
            } catch (IOException | DataStoreException | TransformException ex) {
                throw new CstlServiceException(ex);
            }

        } else {
            candidates.close();
            return new ResponseObject("Format not supported", MediaType.TEXT_PLAIN, 406);
        }

    }

    private static String encodeCollectionId(String name) {
        if (name == null) return name;
        return name.replace("/", "%5E").replace(":", "%3A");
    }

    private static String decodeCollectionId(String name) {
        if (name == null) return name;
        return name.replace("^", "/");
    }

    private static List<String> decodeCollectionId(List<String> names) {
        if (names == null) return names;
        final List<String> clean = new ArrayList<>();
        for (String s : names) {
            clean.add(decodeCollectionId(s));
        }
        return clean;
    }

    @Override
    public ZoneInfo getDggrsZone(GetZone parameters) throws CstlServiceException {
        final ZoneInfo dto = new ZoneInfo();
        final String collectionId = decodeCollectionId(parameters.getCollectionId());

        //convert datetime to subset
        final List<String> subset = new ArrayList<>();
        datetimeToSubset(parameters.getDatetime()).ifPresent(subset::add);

        if (parameters.getCollections() != null) throw new CstlServiceException("collections parameter not supported yet");

        final String dggrsId = parameters.getDggrsId();
        final DiscreteGlobalGridReferenceSystem dggrs;
        try {
            dggrs = DiscreteGlobalGridReferenceSystems.forCode(dggrsId);
        } catch (FactoryException ex) {
            throw new CstlServiceException("DGGRS not found", ex);
        }


        final String zoneId = parameters.getZoneId();

        final String basePath;
        if (collectionId == null) {
            //general description list
            basePath = getServicePath()+"/dggs";
        } else {
            //description for a single data collection
            basePath = getServicePath()+ "/collections/" + encodeCollectionId(collectionId) + "/dggs";
        }

        final DiscreteGlobalGridReferenceSystem.Coder coder = dggrs.createCoder();
        final Zone zone;
        try {
            zone = coder.decode(zoneId);
        } catch (TransformException ex) {
            throw new CstlServiceException("Zone " + zoneId + " not found");
        }

        final DirectPosition position = zone.getPosition();
        final Envelope geographicExtent = zone.getEnvelope();

        dto.setId(zone.getGeographicIdentifier().toString());
        dto.setLevel(zone.getLocationType().getRefinementLevel());
        dto.setCentroid((List)List.of(position.getCoordinate(0), position.getCoordinate(1)));
        dto.setBbox((List)List.of(geographicExtent.getMinimum(0), geographicExtent.getMinimum(1), geographicExtent.getMaximum(0), geographicExtent.getMaximum(1)));
        dto.setShapeType(zone.getShapeType());
        Double areaMetersSquare = zone.getAreaMetersSquare();
        if (areaMetersSquare != null) dto.setAreaMetersSquare(areaMetersSquare);

        try {
            dto.setCrs(new URI(IdentifiedObjects.lookupURN(dggrs.getGridSystem().getCrs(), null)));
        } catch (FactoryException | URISyntaxException ex) {
            LOGGER.log(Level.FINE, ex.getMessage(), ex);
        }

        try {
            final GeographicExtent extent = zone.getGeographicExtent();
            final Polygon polygon = DiscreteGlobalGridSystems.toJTSPolygon(extent);
            final GeoJSONFeature feature = new GeoJSONFeature();
            feature.setGeometry(new GeoJSONMapper().transform(polygon));
            dto.setGeometry(feature);
        } catch (DataStoreException ex) {
            LOGGER.log(Level.FINE, ex.getMessage(), ex);
        }

        if (collectionId != null) {
            //todo
            dto.setTemporalDurationSeconds(null);
            dto.setTemporalInterval(null);
            dto.setVolumeMetersCube(null);

            //compute statistics

            //ensure we do not go under the max level of the dggrs
            final int zoneLevel = zone.getLocationType().getRefinementLevel();
            int queryLevel = zoneLevel + 4;
            final int maxLevel = dggrs.getGridSystem().getHierarchy().getGrids().size()-1;
            if (queryLevel >= maxLevel) {
                queryLevel = maxLevel;
            }

            final CodedCoverage dggrsCoverage = getDggsCoverage(getCodedResource(collectionId, dggrs), dggrs, zoneId, null, (queryLevel - zoneLevel), null, null, subset);
            final Map<String, ZoneInfoStatisticsValue> stats = new LinkedHashMap<>();
            final CodeIterator ite = dggrsCoverage.createIterator();

            final List<SampleDimension> sampleDimensions = dggrsCoverage.getSampleDimensions();
            final String[] propertyNames = sampleDimensions.stream().map(SampleDimension::getName).map(GenericName::toString).toArray(String[]::new);

            final Statistics[] comps = new Statistics[propertyNames.length];
            for (int i = 0; i < comps.length; i++) {
                comps[i] = new Statistics(propertyNames[i]);
            }
            while (ite.next()) {
                for (int i = 0; i < comps.length; i++) {
                    comps[i].accept(ite.getSampleDouble(i));
                }
            }
            for (int i = 0; i < comps.length; i++) {
                final ZoneInfoStatisticsValue stat = new ZoneInfoStatisticsValue();
                stat.setAverage(noNaN(comps[i].mean()));
                stat.setMaximum(noNaN(comps[i].maximum()));
                stat.setMinimum(noNaN(comps[i].minimum()));
                stat.setStdDev(noNaN(comps[i].standardDeviation(false)));
                stat.setOtherField("rms", noNaN(comps[i].rms()));
                stat.setOtherField("span", noNaN(comps[i].span()));
                stat.setOtherField("count", comps[i].count());
                stat.setOtherField("countNaN", comps[i].countNaN());
                stats.put(comps[i].name().toString(), stat);
            }
            dto.setStatistics(stats);
        }


        //Build all the links
        //alternates
        dto.getLinks().addAll(buildAlternates(basePath + "/" + dggrsId+"/zones/"+zoneId));
        //link to root
        dto.addLinksItem(new Link(getServicePath() + "/", LinkRelations.OGC_DATASET, null, null, "Root OGC Web API", null));
        //link to dggrs
        dto.addLinksItem(new Link(basePath + "/" + dggrsId, LinkRelations.OGC_DGGRS, null, null, dggrsId + " DGGRS", null));
        //link to dggrs definition
        dto.addLinksItem(new Link(basePath + "/" + dggrsId + "/definition", LinkRelations.OGC_DGGRS_DEFINITION, null, null, dggrsId +" DGGRS definition", null));

        if (collectionId != null) {
            //data link
            dto.addLinksItem(new Link(basePath + "/" + dggrsId + "/zones/" + zoneId + "/data", LinkRelations.OGC_DGGRS_ZONE_DATA, null, null, zoneId +" data", null));
        }

        //parent zone
        final Collection<? extends Zone> parents = zone.getParents();
        for (Zone parent : parents) {
            final String cellId = parent.getGeographicIdentifier().toString();
            final Link link = new Link(basePath + "/" + dggrsId + "/zones/" + cellId, LinkRelations.OGC_DGGRS_ZONE_PARENT, null, null, cellId, null);
            dto.addLinksItem(link);
        }

        //child zone
        final Collection<? extends Zone> children = zone.getChildren();
        for (Zone child : children) {
            final String cellId = child.getGeographicIdentifier().toString();
            final Link link = new Link(basePath + "/" + dggrsId + "/zones/" + cellId, LinkRelations.OGC_DGGRS_ZONE_CHILD, null, null, cellId, null);
            dto.addLinksItem(link);
        }

        //neighbor zone
        final Collection<? extends Zone> neighbors = zone.getNeighbors();
        for (Zone neighbor : neighbors) {
            final String cellId = neighbor.getGeographicIdentifier().toString();
            final Link link = new Link(basePath + "/" + dggrsId + "/zones/" + cellId, LinkRelations.OGC_DGGRS_ZONE_NEIGHBOR, null, null, cellId, null);
            dto.addLinksItem(link);
        }

        //add best preview sub-level, used in UI
        final int linkLevel;
        final int previewLevel;
        switch (dggrs.getGridSystem().getRefinementRatio()) {
            case 2  : linkLevel = 7; previewLevel = 9; break;
            case 3  : linkLevel = 7; previewLevel = 9; break;
            case 4  : linkLevel = 6; previewLevel = 8; break;
            case 5  : linkLevel = 5; previewLevel = 7; break;
            case 6  : linkLevel = 5; previewLevel = 7; break;
            case 7  : linkLevel = 4; previewLevel = 5; break;
            case 8  : linkLevel = 4; previewLevel = 5; break;
            case 9  : linkLevel = 4; previewLevel = 5; break;
            default : linkLevel = 3; previewLevel = 4; break;
        }
        dto.setOtherField("_linkZoneDepth", linkLevel);
        dto.setOtherField("_previewZoneDepth", previewLevel);

        return dto;
    }

    @Override
    public ResponseObject getDggrsZoneData(GetZoneData parameters) throws CstlServiceException {
        if (isTransactional) assertTransactionnal("Transaction");

        List<String> colIds = parameters.getCollectionId();
        String dggrsId = parameters.getDggrsId();
        String zoneId = parameters.getZoneId();
        String crsStr = parameters.getCrs();
        String excludeProperties = parameters.getExcludeProperties();
        String filterCql = parameters.getFilter();
        String geometry = parameters.getGeometry();
        String profile = parameters.getProfile();
        String properties = parameters.getProperties();
        List<String> subset = parameters.getSubset();
        String datetime = parameters.getDatetime();
        Double valuesOffset = parameters.getValuesOffset();
        Double valuesScale = parameters.getValuesScale();
        String zoneDepthStr = parameters.getZoneDepth();
        String mediaType = parameters.getFormat();
        boolean gzip = parameters.isGzip();

        if (colIds == null || colIds.isEmpty()) {
            throw new DGGSService.BadParameterException("Collections is empty");
        }

        final DiscreteGlobalGridReferenceSystem dggrs;
        try {
            dggrs = DiscreteGlobalGridReferenceSystems.forCode(dggrsId);
        } catch (FactoryException ex) {
            throw new CstlServiceException("DGGRS not found", ex);
        }

        if (crsStr == null || crsStr.isBlank()) crsStr = "CRS:84";

        //parse the relative zone depths
        if (zoneDepthStr == null || zoneDepthStr.isBlank()) {
            int cdt = getDefaultDepth(dggrs);
            zoneDepthStr = ""+cdt;
        }

        final List<Integer> relativeZoneDepths = new ArrayList<>();
        for (String part : zoneDepthStr.split(",")) {
            part = part.trim();
            if (part.isBlank()) continue;
            String[] range = part.split("-");
            if (range.length == 1) {
                //single value
                relativeZoneDepths.add(Integer.valueOf(range[0]));
            } else {
                int start = Integer.valueOf(range[0]);
                int end = Integer.valueOf(range[1]);
                for (int i = start; i <=end; i++) {
                    relativeZoneDepths.add(i);
                }
            }
        }

        //check relative zone depth is not more then maxRelativeDepth
        final int maxRelativeDepth = getMaxRelativeDepth(dggrs);
        for (Integer rzd : relativeZoneDepths) {
            if (rzd > maxRelativeDepth) {
                throw new DGGSService.BadParameterException("Relative depth greater then maximum relative depth : " + maxRelativeDepth);
            }
        }

        //parse filter parameter
        Filter filter = Filter.include();
        if (filterCql != null && !filterCql.isBlank()) {
            try {
                filter = CQL.parseFilter(filterCql);
            } catch (CQLException ex) {
                throw new DGGSService.BadParameterException("Invalid CQL filter : " + filterCql);
            }
        }

        //add datetime to subset
        if (subset == null) subset = new ArrayList<>();
        datetimeToSubset(datetime).ifPresent(subset::add);

        //read requested zone
        final Zone rootZone;
        final DirectPosition zonePosition;
        try {
            rootZone = dggrs.getGridSystem().getHierarchy().getZone(zoneId);
            zonePosition = rootZone.getPosition();
        } catch (IllegalArgumentException ex) {
            return new ResponseObject(ex.getMessage(), MediaType.TEXT_PLAIN, HttpStatus.BAD_REQUEST);
        }

        //open resource
        colIds = decodeCollectionId(colIds);
        final CodedResource resource;
        if (colIds.size() == 1) {
            resource = getCodedResource(colIds.get(0), dggrs);
        } else {
            resource = getCodedResource(dggrs, colIds.toArray(String[]::new));
        }


        //special case if we are dealing with vector data and requesting vectorized
        vectorCase:
        if (DggsApi.GEOMETRY_ZONE_VECTORIZED.equals(geometry)) {
            if (!MediaTypes.DATA_GEOJSON.equalsIgnoreCase(mediaType)) {
                throw new DGGSService.BadParameterException("Only geojson format support the vectorized value for geometry parameter.");
            }
            if (!(resource instanceof FeatureSetAsDiscreteGlobalGridResource fsd)) {
                break vectorCase;
            }
            FeatureSet featureSet = fsd.getOrigin();

            try {
                //reproject data
                FeatureQuery query = Query.reproject(featureSet.getType(), dggrs.getGridSystem().getCrs());
                featureSet = featureSet.subset(query);
                //clip to cell
                final Polygon polygon = DiscreteGlobalGridSystems.toJTSPolygon(rootZone.getGeographicExtent());
                final FeatureQuery clipQuery = new FeatureQuery();
                final FilterFactory<Feature, Object, Object> ff = DefaultFilterFactory.forFeatures();
                clipQuery.setSelection(ff.intersects(ff.property(AttributeConvention.GEOMETRY), ff.literal(polygon)));
                featureSet = featureSet.subset(clipQuery);

                final FeatureType type = featureSet.getType();
                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                try (final GeoJSONStreamWriter writer = new GeoJSONStreamWriter(out, type, 12)) {
                    try (Stream<Feature> stream = featureSet.features(false)) {
                        final Iterator<Feature> iterator = stream.iterator();
                        while (iterator.hasNext()) {
                            final Feature feature = iterator.next();
                            Feature cp = writer.next();
                            FeatureExt.copy(feature, cp, false);
                            writer.write();
                        }
                    }
                }
                out.flush();
                final byte[] array = out.toByteArray();
                return new ResponseObject(array, MediaType.APPLICATION_JSON);

            } catch (DataStoreException | IOException ex) {
                throw new CstlServiceException(ex);
            }
        }

        //read wanted datas
        final CodedCoverage[] dggrsCoverages = new CodedCoverage[relativeZoneDepths.size()];
        for (int i = 0, n = relativeZoneDepths.size(); i < n ; i++) {
            dggrsCoverages[i] = getDggsCoverage(resource, dggrs,
                    zoneId, null, relativeZoneDepths.get(i),
                    excludeProperties, properties, subset);
            dggrsCoverages[i] = applyFilter(dggrsCoverages[i], filter);
            dggrsCoverages[i] = applyScaleOffset(dggrsCoverages[i], valuesScale, valuesOffset);
        }

        //parse the crs
        final CoordinateReferenceSystem crs;
        try {
            crs = toCRS(zonePosition, crsStr);
        } catch (FactoryException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            return new ResponseObject(406);
        }


        if (MediaTypes.DATA_PNG.equalsIgnoreCase(mediaType)) {
            if (dggrsCoverages.length > 1) throw new CstlServiceException("PNG output can only support 1 zone depth");
            final CodedCoverage dggrsCoverage = dggrsCoverages[0];
            try {
                final GridCoverage gridCoverage = toCoverage(dggrsCoverage, crs, zonePosition);
                final GridGeometry gridGeometry = gridCoverage.getGridGeometry();
                final RenderedImage cimage = gridCoverage.render(gridGeometry.getExtent());

                //try to preserve a correct rendering if no scaling has been set
                double scale = 1;
                double offset = 0;
                if (valuesScale == null && valuesOffset == null && dggrsCoverage.getSampleDimensions().size() == 1) {
                    if (dggrsCoverage instanceof ArrayDiscreteGlobalGridCoverage a) {
                        BBox range = NDArrays.computeRange(a.getSamples().get(0));
                        if (range.getSpan(0) > 0) {
                            scale = 255.0 / range.getSpan(0);
                            offset = range.getMinimum(0);
                        }
                    } else if (dggrsCoverage instanceof ArrayCodedCoverage b) {
                        final Array array = b.getSamples().get(0);
                        BBox range = NDArrays.computeRange(array);
                        if (range.getSpan(0) > 0) {
                            scale = 255.0 / range.getSpan(0);
                            offset = range.getMinimum(0);
                        }
                    }
                }

                //force RGBA
                final BufferedImage bimage = new BufferedImage(cimage.getWidth(), cimage.getHeight(), BufferedImage.TYPE_INT_ARGB);

                final WritablePixelIterator wite = WritablePixelIterator.create(bimage);
                final PixelIterator rite = PixelIterator.create(cimage);

                int[] rgba = new int[4];
                while (rite.next()) {
                    Point position = rite.getPosition();
                    wite.moveTo(position.x, position.y);
                    double[] pixel = rite.getPixel((double[])null);

                    //apply scale and offset
                    for (int i = 0; i < pixel.length; i++) {
                        pixel[i] = pixel[i] * scale + offset;
                    }

                    if (pixel.length > 4) {
                        //we can't represent this, let's create a bit mask like image to show where datas are
                        boolean isEmpty = true;
                        for (int i = 0; i< pixel.length; i++) {
                            if (!Double.isNaN(pixel[i])) {
                                isEmpty = false;
                            }
                        }
                        rgba[0] = isEmpty ? 0 : 220;
                        rgba[1] = isEmpty ? 0 : 220;
                        rgba[2] = isEmpty ? 0 : 220;
                        rgba[3] = isEmpty ? 0 : 255;
                        wite.setSample(0, rgba[0]);
                        wite.setSample(1, rgba[1]);
                        wite.setSample(2, rgba[2]);
                        wite.setSample(3, rgba[3]);

                    } else if (pixel.length == 4) {
                        //treat it as rgba
                        rgba[0] = (int) pixel[0];
                        rgba[1] = (int) pixel[1];
                        rgba[2] = (int) pixel[2];
                        rgba[3] = (int) pixel[3];
                        wite.setSample(0, rgba[0]);
                        wite.setSample(1, rgba[1]);
                        wite.setSample(2, rgba[2]);
                        wite.setSample(3, rgba[3]);

                    } else if (pixel.length == 3) {
                        //treat it as rgb
                        boolean isEmpty = Double.isNaN(pixel[0]) && Double.isNaN(pixel[1]) && Double.isNaN(pixel[2]);
                        rgba[0] = (int) pixel[0];
                        rgba[1] = (int) pixel[1];
                        rgba[2] = (int) pixel[2];
                        rgba[3] = isEmpty ? 0 : 255;
                        wite.setSample(0, rgba[0]);
                        wite.setSample(1, rgba[1]);
                        wite.setSample(2, rgba[2]);
                        wite.setSample(3, rgba[3]);

                    } else if (pixel.length == 2) {
                        //treat it as grayscale + alpha
                        rgba[0] = (int) pixel[0];
                        rgba[1] = (int) pixel[0];
                        rgba[2] = (int) pixel[0];
                        rgba[3] = (int) pixel[1];
                        wite.setSample(0, rgba[0]);
                        wite.setSample(1, rgba[1]);
                        wite.setSample(2, rgba[2]);
                        wite.setSample(3, rgba[3]);

                    } else if (pixel.length == 1) {
                        //treat it as grayscale
                        boolean isEmpty = Double.isNaN(pixel[0]);
                        rgba[0] = (int) pixel[0];
                        rgba[1] = (int) pixel[0];
                        rgba[2] = (int) pixel[0];
                        rgba[3] = isEmpty ? 0 : 255;
                        wite.setSample(0, rgba[0]);
                        wite.setSample(1, rgba[1]);
                        wite.setSample(2, rgba[2]);
                        wite.setSample(3, rgba[3]);
                    }
                }

                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                ImageIO.write(bimage, "png", out);
                final byte[] datas = out.toByteArray();

                return new ResponseObject(datas, MediaType.IMAGE_PNG);
            } catch (IOException | TransformException ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                return new ResponseObject(406);
            }
        } else if (MediaTypes.DATA_GEOJSON.equalsIgnoreCase(mediaType)) {
            try {
                final byte[] data = toGeojson(geometry, profile, crs, dggrsCoverages);
                if (gzip) {
                    return new ResponseObject(compressGzip(data), MediaType.APPLICATION_JSON, HttpStatus.OK, Map.of("Content-Encoding", "gzip"));
                } else {
                    return new ResponseObject(data, MediaType.APPLICATION_JSON);
                }
            } catch (IOException | DataStoreException ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                return new ResponseObject(406);
            }
        } else if (MediaTypes.DATA_GEOTIFF.equalsIgnoreCase(mediaType)) {
            if (dggrsCoverages.length > 1) throw new CstlServiceException("GEOTIFF output can only support 1 zone depth for the moment");
            final CodedCoverage dggrsCoverage = dggrsCoverages[0];
            try {
                final GridCoverage gridCoverage = toCoverage(dggrsCoverage, crs, zonePosition);
                final byte[] data = toGeotiff(gridCoverage);

                return new ResponseObject(data, MediaTypes.DATA_GEOTIFF);
            } catch (IOException | TransformException | DataStoreException ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                return new ResponseObject(406);
            }
        } else if (MediaTypes.DATA_COVERAGEJSON.equalsIgnoreCase(mediaType)) {
            if (profile != null) {
                if (!"covjson".equals(profile)) throw new DGGSService.BadParameterException(profile + " profile not supported yet");
                //todo profiles : covjson-dggs and covjson-dggs-zoneids
            }
            if (dggrsCoverages.length > 1) throw new CstlServiceException("CoverageJson output can only support 1 zone depth for the moment");
            final CodedCoverage dggrsCoverage = dggrsCoverages[0];
            try {
                final GridCoverage gridCoverage = toCoverage(dggrsCoverage, crs, zonePosition);
                final byte[] data = toCoverageJson(resource.getIdentifier().get(), gridCoverage);

                return new ResponseObject(data, MediaTypes.DATA_GEOTIFF);
            } catch (IOException | TransformException | DataStoreException ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                return new ResponseObject(406);
            }
        } else if (MediaTypes.DATA_JSON.equalsIgnoreCase(mediaType)) {

            final DggrsData json = toDggrsData(dggrs, dggrsCoverages, false);
            json.setZoneId(zoneId);

            final ObjectMapper mapper = JsonMapper.builder()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .addModule(new JavaTimeModule())
                .build();
            try {
                final byte[] data = mapper.writeValueAsBytes(json);
                if (gzip) {
                    return new ResponseObject(compressGzip(data), MediaType.APPLICATION_JSON, HttpStatus.OK, Map.of("Content-Encoding", "gzip"));
                } else {
                    return new ResponseObject(data, MediaType.APPLICATION_JSON);
                }
            } catch (IOException ex) {
                throw new CstlServiceException(ex);
            }

        } else if (MediaTypes.DATA_UBJSON.equalsIgnoreCase(mediaType)) {

            final DggrsData json = toDggrsData(dggrs, dggrsCoverages, false);
            json.setZoneId(zoneId);

            final ObjectMapper mapper = UBJsonMapper.builder()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .addModule(new JavaTimeModule())
                .build();
            try {
                final byte[] data = mapper.writeValueAsBytes(json);
                if (gzip) {
                    return new ResponseObject(compressGzip(data), MediaType.parseMediaType(MediaTypes.DATA_UBJSON), HttpStatus.OK, Map.of("Content-Encoding", "gzip"));
                } else {
                    return new ResponseObject(data, MediaTypes.DATA_UBJSON);
                }
            } catch (IOException ex) {
                throw new CstlServiceException(ex);
            }
        } else if (MediaTypes.DATA_CBOR.equalsIgnoreCase(mediaType)) {

            final DggrsData json = toDggrsData(dggrs, dggrsCoverages, false);
            json.setZoneId(zoneId);

            final ObjectMapper mapper = CBORMapper.builder()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .addModule(new JavaTimeModule())
                .build();
            try {
                final byte[] data = mapper.writeValueAsBytes(json);
                if (gzip) {
                    return new ResponseObject(compressGzip(data), MediaType.parseMediaType(MediaTypes.DATA_CBOR), HttpStatus.OK, Map.of("Content-Encoding", "gzip"));
                } else {
                    return new ResponseObject(data, MediaTypes.DATA_CBOR);
                }
            } catch (IOException ex) {
                throw new CstlServiceException(ex);
            }
        } else if (MediaTypes.DATA_ZARR.equalsIgnoreCase(mediaType)) {
            if (dggrsCoverages.length > 1) throw new CstlServiceException("ZARR output can only support 1 zone depth for the moment");

            if (profile == null) profile = DggsApi.PROFILE_ZARR3_DGGS;

            final CodedCoverage dggrsCoverage = dggrsCoverages[0];
            try {

                if (DggsApi.PROFILE_ZARR3_DGGS.equals(profile)) {
                    final byte[] data = toDggrsZarr(dggrsCoverage);
                    return new ResponseObject(data, MediaType.APPLICATION_OCTET_STREAM);
                } else if (DggsApi.PROFILE_ZARR3.equals(profile)) {
                    final GridCoverage gridCoverage = toCoverage(dggrsCoverage, crs, zonePosition);
                    final byte[] data = toGeoZarr(gridCoverage);
                    return new ResponseObject(data, MediaType.APPLICATION_OCTET_STREAM);
                } else {
                    throw new DGGSService.BadParameterException(profile + " profile not supported yet");
                }

            } catch (IOException | TransformException | DataStoreException ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                return new ResponseObject(406);
            }
        } else {
            return new ResponseObject(406);
        }

    }


    @Override
    public ResponseObject getDggrsData(GetZoneData parameters) throws CstlServiceException {
        if (isTransactional) assertTransactionnal("Transaction");

        List<String> colIds = parameters.getCollectionId();
        String dggrsId = parameters.getDggrsId();
        String zoneId = parameters.getZoneId();
        String crsStr = parameters.getCrs();
        String excludeProperties = parameters.getExcludeProperties();
        String filterCql = parameters.getFilter();
        String geometry = parameters.getGeometry();
        String profile = parameters.getProfile();
        String properties = parameters.getProperties();
        List<String> subset = parameters.getSubset();
        String datetime = parameters.getDatetime();
        Double valuesOffset = parameters.getValuesOffset();
        Double valuesScale = parameters.getValuesScale();
        String zoneDepthStr = parameters.getZoneDepth();
        String mediaType = parameters.getFormat();
        String zoneAccuracyStr = parameters.getZoneAccuracy();
        String geom = parameters.getArea();
        String geomCrs = parameters.getAreaCrs();
        boolean gzip = parameters.isGzip();

        final DiscreteGlobalGridReferenceSystem dggrs;
        try {
            dggrs = DiscreteGlobalGridReferenceSystems.forCode(dggrsId);
        } catch (FactoryException ex) {
            throw new CstlServiceException("DGGRS not found", ex);
        }

        if (crsStr == null || crsStr.isBlank()) crsStr = "CRS:84";

        //parse the zone depth
        if (zoneAccuracyStr != null && !zoneAccuracyStr.isBlank()) {
            String[] parts = zoneAccuracyStr.split(" ");
            if (parts.length != 2) {
                throw new DGGSService.BadParameterException("Invalid accuracy : " + zoneAccuracyStr);
            }
            final double acc = Double.parseDouble(parts[0]);
            final Unit<?> unit = Units.valueOf(parts[1]);
            final Quantity<?> q = Quantities.create(acc, unit);
            try {
                zoneDepthStr = "" + dggrs.getGridSystem().getHierarchy().getGrid(q).getRefinementLevel();
            } catch (IncommensurableException ex) {
                throw new CstlServiceException(ex.getMessage(), ex);
            }
        }
        if (zoneDepthStr == null || zoneDepthStr.isBlank()) {
            throw new DGGSService.BadParameterException("Missing depth parameter");
        }

        final List<Integer> zoneDepths = new ArrayList<>();
        for (String part : zoneDepthStr.split(",")) {
            part = part.trim();
            if (part.isBlank()) continue;
            String[] range = part.split("-");
            if (range.length == 1) {
                //single value
                zoneDepths.add(Integer.valueOf(range[0]));
            } else {
                int start = Integer.valueOf(range[0]);
                int end = Integer.valueOf(range[1]);
                for (int i = start; i <=end; i++) {
                    zoneDepths.add(i);
                }
            }
        }

        //parse filter parameter
        Filter filter = Filter.include();
        if (filterCql != null && !filterCql.isBlank()) {
            try {
                filter = CQL.parseFilter(filterCql);
            } catch (CQLException ex) {
                throw new DGGSService.BadParameterException("Invalid CQL filter : " + filterCql);
            }
        }

        //add datetime to subset
        if (subset == null) subset = new ArrayList<>();
        datetimeToSubset(datetime).ifPresent(subset::add);

        //read requested zone
        final GeographicExtent rootArea;
        final DirectPosition zonePosition;
        try {
            Geometry g = new WKTReader().read(geom);
            g.setUserData(CRS.forCode(geomCrs));
            g = JTS.transform(g, CRS84);
            rootArea =  S2.toGeographicExtent(S2.toS2Polygon((Polygon)g));
            org.locationtech.jts.geom.Point centroid = g.getCentroid();
            zonePosition = new DirectPosition2D(CRS84, centroid.getX(), centroid.getY());
        } catch (IllegalArgumentException ex) {
            return new ResponseObject(ex.getMessage(), MediaType.TEXT_PLAIN, HttpStatus.BAD_REQUEST);
        } catch (ParseException ex) {
            return new ResponseObject(ex.getMessage(), MediaType.TEXT_PLAIN, HttpStatus.BAD_REQUEST);
        } catch (FactoryException | TransformException ex) {
            return new ResponseObject(ex.getMessage(), MediaType.TEXT_PLAIN, HttpStatus.BAD_REQUEST);
        }

        //open resource
        colIds = decodeCollectionId(colIds);
        final CodedResource resource;
        if (colIds.size() == 1) {
            resource = getCodedResource(colIds.get(0), dggrs);
        } else {
            resource = getCodedResource(dggrs, colIds.toArray(String[]::new));
        }

        //special case if we are dealing with vector data and requesting vectorized
        vectorCase:
        if (DggsApi.GEOMETRY_ZONE_VECTORIZED.equals(geometry)) {
            if (!MediaTypes.DATA_GEOJSON.equalsIgnoreCase(mediaType)) {
                throw new DGGSService.BadParameterException("Only geojson format support the vectorized value for geometry parameter.");
            }
            if (!(resource instanceof FeatureSetAsDiscreteGlobalGridResource fsd)) {
                break vectorCase;
            }
            FeatureSet featureSet = fsd.getOrigin();

            try {
                //reproject data
                FeatureQuery query = Query.reproject(featureSet.getType(), dggrs.getGridSystem().getCrs());
                featureSet = featureSet.subset(query);
                //clip to cell
                final Polygon polygon = DiscreteGlobalGridSystems.toJTSPolygon(rootArea);
                final FeatureQuery clipQuery = new FeatureQuery();
                final FilterFactory<Feature, Object, Object> ff = DefaultFilterFactory.forFeatures();
                clipQuery.setSelection(ff.intersects(ff.property(AttributeConvention.GEOMETRY), ff.literal(polygon)));
                featureSet = featureSet.subset(clipQuery);

                final FeatureType type = featureSet.getType();
                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                try (final GeoJSONStreamWriter writer = new GeoJSONStreamWriter(out, type, 12)) {
                    try (Stream<Feature> stream = featureSet.features(false)) {
                        final Iterator<Feature> iterator = stream.iterator();
                        while (iterator.hasNext()) {
                            final Feature feature = iterator.next();
                            Feature cp = writer.next();
                            FeatureExt.copy(feature, cp, false);
                            writer.write();
                        }
                    }
                }
                out.flush();
                final byte[] array = out.toByteArray();
                return new ResponseObject(array, MediaType.APPLICATION_JSON);

            } catch (DataStoreException | IOException ex) {
                throw new CstlServiceException(ex);
            }
        }

        //read wanted datas
        final CodedCoverage[] dggrsCoverages = new CodedCoverage[zoneDepths.size()];
        for (int i = 0, n = zoneDepths.size(); i < n ; i++) {
            dggrsCoverages[i] = getDggsCoverage(resource, dggrs,
                    null, rootArea, zoneDepths.get(i),
                    excludeProperties, properties, subset);
            dggrsCoverages[i] = applyFilter(dggrsCoverages[i], filter);
            dggrsCoverages[i] = applyScaleOffset(dggrsCoverages[i], valuesScale, valuesOffset);
        }

        //parse the crs
        final CoordinateReferenceSystem crs;
        try {
            crs = toCRS(zonePosition, crsStr);
        } catch (FactoryException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            return new ResponseObject(406);
        }


        if (MediaTypes.DATA_PNG.equalsIgnoreCase(mediaType)) {
            if (dggrsCoverages.length > 1) throw new CstlServiceException("PNG output can only support 1 zone depth");
            final CodedCoverage dggrsCoverage = dggrsCoverages[0];
            try {
                final GridCoverage gridCoverage = toCoverage(dggrsCoverage, crs, zonePosition);
                final GridGeometry gridGeometry = gridCoverage.getGridGeometry();
                final RenderedImage cimage = gridCoverage.render(gridGeometry.getExtent());

                //try to preserve a correct rendering if no scaling has been set
                double scale = 1;
                double offset = 0;
                if (valuesScale == null && valuesOffset == null && dggrsCoverage.getSampleDimensions().size() == 1) {
                    if (dggrsCoverage instanceof ArrayDiscreteGlobalGridCoverage a) {
                        BBox range = NDArrays.computeRange(a.getSamples().get(0));
                        if (range.getSpan(0) > 0) {
                            scale = 255.0 / range.getSpan(0);
                            offset = range.getMinimum(0);
                        }
                    } else if (dggrsCoverage instanceof ArrayCodedCoverage b) {
                        final Array array = b.getSamples().get(0);
                        BBox range = NDArrays.computeRange(array);
                        if (range.getSpan(0) > 0) {
                            scale = 255.0 / range.getSpan(0);
                            offset = range.getMinimum(0);
                        }
                    }
                }

                //force RGBA
                final BufferedImage bimage = new BufferedImage(cimage.getWidth(), cimage.getHeight(), BufferedImage.TYPE_INT_ARGB);

                final WritablePixelIterator wite = WritablePixelIterator.create(bimage);
                final PixelIterator rite = PixelIterator.create(cimage);

                int[] rgba = new int[4];
                while (rite.next()) {
                    Point position = rite.getPosition();
                    wite.moveTo(position.x, position.y);
                    double[] pixel = rite.getPixel((double[])null);

                    //apply scale and offset
                    for (int i = 0; i < pixel.length; i++) {
                        pixel[i] = pixel[i] * scale + offset;
                    }

                    if (pixel.length > 4) {
                        //we can't represent this, let's create a bit mask like image to show where datas are
                        boolean isEmpty = true;
                        for (int i = 0; i< pixel.length; i++) {
                            if (!Double.isNaN(pixel[i])) {
                                isEmpty = false;
                            }
                        }
                        rgba[0] = isEmpty ? 0 : 220;
                        rgba[1] = isEmpty ? 0 : 220;
                        rgba[2] = isEmpty ? 0 : 220;
                        rgba[3] = isEmpty ? 0 : 255;
                        wite.setSample(0, rgba[0]);
                        wite.setSample(1, rgba[1]);
                        wite.setSample(2, rgba[2]);
                        wite.setSample(3, rgba[3]);

                    } else if (pixel.length == 4) {
                        //treat it as rgba
                        rgba[0] = (int) pixel[0];
                        rgba[1] = (int) pixel[1];
                        rgba[2] = (int) pixel[2];
                        rgba[3] = (int) pixel[3];
                        wite.setSample(0, rgba[0]);
                        wite.setSample(1, rgba[1]);
                        wite.setSample(2, rgba[2]);
                        wite.setSample(3, rgba[3]);

                    } else if (pixel.length == 3) {
                        //treat it as rgb
                        boolean isEmpty = Double.isNaN(pixel[0]) && Double.isNaN(pixel[1]) && Double.isNaN(pixel[2]);
                        rgba[0] = (int) pixel[0];
                        rgba[1] = (int) pixel[1];
                        rgba[2] = (int) pixel[2];
                        rgba[3] = isEmpty ? 0 : 255;
                        wite.setSample(0, rgba[0]);
                        wite.setSample(1, rgba[1]);
                        wite.setSample(2, rgba[2]);
                        wite.setSample(3, rgba[3]);

                    } else if (pixel.length == 2) {
                        //treat it as grayscale + alpha
                        rgba[0] = (int) pixel[0];
                        rgba[1] = (int) pixel[0];
                        rgba[2] = (int) pixel[0];
                        rgba[3] = (int) pixel[1];
                        wite.setSample(0, rgba[0]);
                        wite.setSample(1, rgba[1]);
                        wite.setSample(2, rgba[2]);
                        wite.setSample(3, rgba[3]);

                    } else if (pixel.length == 1) {
                        //treat it as grayscale
                        boolean isEmpty = Double.isNaN(pixel[0]);
                        rgba[0] = (int) pixel[0];
                        rgba[1] = (int) pixel[0];
                        rgba[2] = (int) pixel[0];
                        rgba[3] = isEmpty ? 0 : 255;
                        wite.setSample(0, rgba[0]);
                        wite.setSample(1, rgba[1]);
                        wite.setSample(2, rgba[2]);
                        wite.setSample(3, rgba[3]);
                    }
                }

                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                ImageIO.write(bimage, "png", out);
                final byte[] datas = out.toByteArray();

                return new ResponseObject(datas, MediaType.IMAGE_PNG);
            } catch (IOException | TransformException ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                return new ResponseObject(406);
            }
        } else if (MediaTypes.DATA_GEOJSON.equalsIgnoreCase(mediaType)) {
            try {
                final byte[] data = toGeojson(geometry, profile, crs, dggrsCoverages);
                return new ResponseObject(data, MediaType.APPLICATION_JSON);
            } catch (IOException | DataStoreException ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                return new ResponseObject(406);
            }
        } else if (MediaTypes.DATA_GEOTIFF.equalsIgnoreCase(mediaType)) {
            if (dggrsCoverages.length > 1) throw new CstlServiceException("GEOTIFF output can only support 1 zone depth for the moment");
            final CodedCoverage dggrsCoverage = dggrsCoverages[0];
            try {
                final GridCoverage gridCoverage = toCoverage(dggrsCoverage, crs, zonePosition);
                final byte[] data = toGeotiff(gridCoverage);

                return new ResponseObject(data, MediaTypes.DATA_GEOTIFF);
            } catch (IOException | TransformException | DataStoreException ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                return new ResponseObject(406);
            }
        } else if (MediaTypes.DATA_COVERAGEJSON.equalsIgnoreCase(mediaType)) {
            if (profile != null) {
                if (!"covjson".equals(profile)) throw new DGGSService.BadParameterException(profile + " profile not supported yet");
                //todo profiles : covjson-dggs and covjson-dggs-zoneids
            }
            if (dggrsCoverages.length > 1) throw new CstlServiceException("CoverageJson output can only support 1 zone depth for the moment");
            final CodedCoverage dggrsCoverage = dggrsCoverages[0];
            try {
                final GridCoverage gridCoverage = toCoverage(dggrsCoverage, crs, zonePosition);
                final byte[] data = toCoverageJson(resource.getIdentifier().get(), gridCoverage);

                return new ResponseObject(data, MediaTypes.DATA_GEOTIFF);
            } catch (IOException | TransformException | DataStoreException ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                return new ResponseObject(406);
            }
        } else if (MediaTypes.DATA_JSON.equalsIgnoreCase(mediaType)) {

            final DggrsData json = toDggrsData(dggrs, dggrsCoverages, true);

            final ObjectMapper mapper = JsonMapper.builder()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .addModule(new JavaTimeModule())
                .build();
            try {
                return new ResponseObject(mapper.writeValueAsString(json), MediaType.APPLICATION_JSON);
            } catch (JsonProcessingException ex) {
                throw new CstlServiceException(ex);
            }

        } else if (MediaTypes.DATA_UBJSON.equalsIgnoreCase(mediaType)) {

            final DggrsData json = toDggrsData(dggrs, dggrsCoverages, true);

            final ObjectMapper mapper = UBJsonMapper.builder()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .addModule(new JavaTimeModule())
                .build();
            try {
                return new ResponseObject(mapper.writeValueAsBytes(json), MediaTypes.DATA_UBJSON);
            } catch (JsonProcessingException ex) {
                throw new CstlServiceException(ex);
            }
        } else if (MediaTypes.DATA_ZARR.equalsIgnoreCase(mediaType)) {
            if (dggrsCoverages.length > 1) throw new CstlServiceException("ZARR output can only support 1 zone depth for the moment");

            if (profile == null) profile = DggsApi.PROFILE_ZARR3_DGGS;

            final CodedCoverage dggrsCoverage = dggrsCoverages[0];
            try {

                if (DggsApi.PROFILE_ZARR3_DGGS.equals(profile)) {
                    final byte[] data = toDggrsZarr(dggrsCoverage);
                    return new ResponseObject(data, MediaType.APPLICATION_OCTET_STREAM);
                } else if (DggsApi.PROFILE_ZARR3.equals(profile)) {
                    final GridCoverage gridCoverage = toCoverage(dggrsCoverage, crs, zonePosition);
                    final byte[] data = toGeoZarr(gridCoverage);
                    return new ResponseObject(data, MediaType.APPLICATION_OCTET_STREAM);
                } else {
                    throw new DGGSService.BadParameterException(profile + " profile not supported yet");
                }

            } catch (IOException | TransformException | DataStoreException ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                return new ResponseObject(406);
            }
        } else {
            return new ResponseObject(406);
        }
    }

    private static JSONSchema toJsonSchema(FeatureType type, CodedCoverage coverage, boolean onlyBands) {

        final JSONSchema schema = new JSONSchema();
        schema.setType(JSONType.OBJECT);
        int inc = 1;
        for (PropertyType pt : type.getProperties(true)) {
            if (pt instanceof AttributeType at) {

                if (onlyBands && AttributeConvention.contains(at.getName())) continue;

                final Class valueClass = at.getValueClass();
                final JSONType jsonType;
                if (Integer.class.equals(valueClass)) {
                    jsonType = JSONType.INTEGER;
                } else if (Float.class.equals(valueClass)
                        || Double.class.equals(valueClass)) {
                    jsonType = JSONType.NUMBER;
                } else if (String.class.equals(valueClass)) {
                    jsonType = JSONType.STRING;
                } else if (Boolean.class.equals(valueClass)) {
                    jsonType = JSONType.BOOLEAN;
                } else {
                    jsonType = JSONType.OBJECT;
                }

                //fill the schema
                final String name = at.getName().toString();
                final SchemaProperty prop = new SchemaProperty();
                prop.setType(jsonType);
                prop.setTitle(name);
                prop.setxOgcPropertySeq(inc);
                schema.putPropertiesItem(name, prop);
                inc++;
            }
        }
        return schema;
    }

    private static JSONSchema toJsonSchema(List<SampleDimension> sampleDimensions) {

        final JSONSchema schema = new JSONSchema();
        schema.setType(JSONType.OBJECT);
        int inc = 1;
        for (SampleDimension sd : sampleDimensions) {
            final String name = sd.getName().toString();
            final SchemaProperty prop = new SchemaProperty();
            prop.setType(JSONType.NUMBER);
            prop.setTitle(name);
            prop.setxOgcPropertySeq(inc);
            schema.putPropertiesItem(name, prop);
            inc++;
        }

        return schema;
    }

    private static GridCoverage toCoverage(CodedCoverage dggrsCoverage, CoordinateReferenceSystem crs, DirectPosition zonePosition) throws TransformException {

        final CodedGeometry grid = dggrsCoverage.getGeometry();
        final Envelope envelope = dggrsCoverage.getGeometry().getEnvelope(crs);
        final long nbZones = grid.getExtent().getSize(0);

        //commpute a grid size based on the number of zones we have
        int width = (int) Math.sqrt(nbZones*2);
        if (width > 1024) width = 1024;
        if (width < 128) width = 128;

        final GridGeometry gridGeometry = new GridGeometry(new GridExtent(width, width), envelope, GridOrientation.REFLECTION_Y);
        final GridCoverage gridCoverage = dggrsCoverage.sample(gridGeometry, gridGeometry);

        return gridCoverage;
    }

    private CodedResource getCodedResource(String collectionId, DiscreteGlobalGridReferenceSystem dggrs) throws CstlServiceException {

        final String userLogin  = getUserLogin();
        final LayerCache layer = getLayerCache(userLogin, collectionId);
        final Resource resource = layer.getData().getOrigin();

        CodedResource dgr;
        try {
            if (resource instanceof CodedResource d) {
                dgr = d;
            } else if (resource instanceof GridCoverageResource gcr) {
                try {
                    dgr = CodedCoverages.viewAsDggrs(gcr.getIdentifier().get(), gcr, dggrs);
                } catch (FactoryException ex) {
                    throw new CstlServiceException(ex.getMessage(), ex);
                }
            } else if (resource instanceof FeatureSet fs) {
                dgr = DiscreteGlobalGridSystems.viewAsDggrs(fs, dggrs, new DiscreteGlobalGridCoverageProcessor());
            }  else {
                throw new CstlServiceException("Resource not supported");
            }
        } catch (DataStoreException | IncommensurableException | TransformException ex) {
            throw new CstlServiceException(ex);
        }

        Optional<GenericName> identifier;
        try {
            identifier = dgr.getIdentifier();
        } catch (DataStoreException ex) {
            throw new CstlServiceException(ex.getMessage(), ex);
        }

//        if (identifier.isEmpty() || !identifier.get().tip().toString().equals(collectionId)) {
            final CodedResource base = dgr;

            dgr = new CodedResource() {
                @Override
                public Optional<GenericName> getIdentifier() throws DataStoreException {
                    return Optional.of(Names.createLocalName(null, null, collectionId));
                }

                @Override
                public CodedGeometry getGridGeometry() throws DataStoreException {
                    return base.getGridGeometry();
                }

                @Override
                public CodedCoverage read(CodedGeometry geometry, int... range) throws DataStoreException {
                    CodedCoverage gc = base.read(geometry, range);
                    //force resampling
                    if (!gc.getGeometry().equals(geometry)) {
                        try {
                            gc = new DiscreteGlobalGridCoverageProcessor().resample(gc, geometry);
                        } catch (FactoryException | TransformException ex) {
                            throw new DataStoreException(ex.getMessage(), ex);
                        }
                    }

                    return gc;
                }

                @Override
                public List<SampleDimension> getSampleDimensions() throws DataStoreException {
                    return base.getSampleDimensions();
                }

                @Override
                public Optional<Envelope> getEnvelope() throws DataStoreException {
                    return base.getEnvelope();
                }

                @Override
                public Metadata getMetadata() throws DataStoreException {
                    return base.getMetadata();
                }
            };
//        }

        return dgr;
    }

    private CodedResource getCodedResource(DiscreteGlobalGridReferenceSystem dggrs, String ... collectionIds) throws CstlServiceException {

        if (collectionIds.length == 1) return getCodedResource(id, dggrs);

        final CodedResource[] resources = new CodedResource[collectionIds.length];
        for (int i = 0; i < resources.length; i++) {
            resources[i] = getCodedResource(collectionIds[i], dggrs);
        }

        try {
            return new CompoundCodedResource(Names.createLocalName(null, null, "Compound"), Arrays.asList(resources));
        } catch (DataStoreException ex) {
            throw new CstlServiceException(ex.getMessage(), ex);
        }
    }

    private CodedCoverage getDggsCoverage(CodedResource resource, DiscreteGlobalGridReferenceSystem dggrs,
            String zoneId, GeographicExtent geoExtent, int relativeZoneDepth,
            String excludeProperties, String properties, List<String> subset) throws CstlServiceException {

        CodedCoverage dggrsCoverage;
        try {
            final CodedGeometry horizontalGrid;
            if (zoneId != null) {
                horizontalGrid = DiscreteGlobalGridGeometry.subZone(dggrs, zoneId, relativeZoneDepth);
            } else {
                final DiscreteGlobalGrid dgg = dggrs.getGridSystem().getHierarchy().getGrids().get(relativeZoneDepth);
                List zoneIds;
                try (Stream<Zone> stream = dgg.getZones(geoExtent)) {
                    zoneIds = stream.map(Zone::getTextIdentifier).toList();
                }
                horizontalGrid = DiscreteGlobalGridGeometry.unstructured(dggrs, zoneIds, null);
            }
            final List<CodedGeometry> sliceQuery = new ArrayList<>();
            sliceQuery.add(horizontalGrid);
            final CodedGeometry resourceGeometry = resource.getGridGeometry();


            //choose the bands to read
            final List<SampleDimension> sampleDimensions = resource.getSampleDimensions();
            final String[] propertyNames = sampleDimensions.stream().map(SampleDimension::getName).map(GenericName::toString).toArray(String[]::new);
            int[] bands = null;
            if (properties != null && excludeProperties != null) {
                throw new DGGSService.BadParameterException("properties and excludeProperties can not be set at the same time");
            } else if (properties != null) {
                final Set<String> names = new LinkedHashSet<>(List.of(properties.split(",")));
                for (int i = 0; i < propertyNames.length; i++) {
                    if (names.contains(propertyNames[i])) {
                        bands = (bands == null) ? new int[]{i} : ArraysExt.concatenate(bands, new int[]{i});
                    }
                }
                if (bands == null || (names.size() != bands.length)) throw new DGGSService.BadParameterException("Some properties do not exist in the requested coverage");
            } else if (excludeProperties != null) {
                final Set<String> names = new LinkedHashSet<>(List.of(excludeProperties.split(",")));
                int found = 0;
                for (int i = 0; i < propertyNames.length; i++) {
                    if (names.contains(propertyNames[i])) {
                        found++;
                    } else {
                        bands = (bands == null) ? new int[]{i} : ArraysExt.concatenate(bands, new int[]{i});
                    }
                }
                if (names.size() != found) throw new DGGSService.BadParameterException("Some excludeProperties do not exist in the requested coverage");
            }


            //extract subset grids
            if (subset != null) {
                for (String sub : subset) {
                    if (sub.isBlank()) continue;

                    final Entry<String, NumberRange<Double>> entry = parseSubsetRange(sub);
                    final String axisName = entry.getKey();
                    final NumberRange<Double> interval = entry.getValue();

                    if (DggsApi.SUBSET_TIME.equals(axisName)) {
                        final Optional<TemporalCRS> temporalComponent = ReferenceSystems.getTemporalComponent(resourceGeometry.getReferenceSystem());
                        if (temporalComponent.isEmpty()) throw new DGGSService.BadParameterException("No temporal axis on this resource");
                        final CodedGeometry slice = resourceGeometry.slice(temporalComponent.get()).get();
                        final GridGeometry sliceGrid = slice.isRegularGrid().get();
                        final GeneralEnvelope queryEnv = new GeneralEnvelope(CommonCRS.Temporal.JAVA.crs());
                        queryEnv.setRange(0, interval.getMinDouble(), interval.getMaxDouble());
                        final GridGeometry result = sliceGrid.derive().rounding(GridRoundingMode.ENCLOSING).subgrid(queryEnv).build();
                        sliceQuery.add(new CodedGeometry(result));
                    } else if (DggsApi.SUBSET_H.equals(axisName) || DggsApi.SUBSET_Z.equals(axisName)) {
                        //todo differenciate between z and h
                        final Optional<VerticalCRS> verticalComponent = ReferenceSystems.getVerticalComponent(resourceGeometry.getReferenceSystem());
                        if (verticalComponent.isEmpty()) throw new DGGSService.BadParameterException("No vertical axis on this resource");
                        final CodedGeometry slice = resourceGeometry.slice(verticalComponent.get()).get();
                        final GridGeometry sliceGrid = slice.isRegularGrid().get();
                        final GeneralEnvelope queryEnv = new GeneralEnvelope(sliceGrid.getCoordinateReferenceSystem());
                        queryEnv.setRange(0, interval.getMinDouble(), interval.getMaxDouble());
                        final GridGeometry result = sliceGrid.derive().rounding(GridRoundingMode.ENCLOSING).subgrid(queryEnv).build();
                        sliceQuery.add(new CodedGeometry(result));
                    } else {
                        ReferenceSystem found = null;
                        for (ReferenceSystem rs : ReferenceSystems.getSingleComponents(resourceGeometry.getReferenceSystem(), true)) {
                            if (rs.getName().toString().equals(axisName)) {
                                found = rs;
                                break;
                            }
                        }
                        if (found == null) throw new DGGSService.BadParameterException("No axis on this resource named " + axisName);
                        final CodedGeometry slice = resourceGeometry.slice(found).get();
                        final GridGeometry sliceGrid = slice.isRegularGrid().get();
                        final GeneralEnvelope queryEnv = new GeneralEnvelope(sliceGrid.getCoordinateReferenceSystem());
                        queryEnv.setRange(0, interval.getMinDouble(), interval.getMaxDouble());
                        final GridGeometry result = sliceGrid.derive().rounding(GridRoundingMode.ENCLOSING).subgrid(queryEnv).build();
                        sliceQuery.add(new CodedGeometry(result));
                    }

                }
            }

            //ensure we extract a single slice on axes where no range has been defined
            final List<ReferenceSystem> singleComponents = ReferenceSystems.getSingleComponents(resourceGeometry.getReferenceSystem(), true);
            loop:
            for (ReferenceSystem rs : singleComponents) {
                if (rs instanceof DiscreteGlobalGridReferenceSystem) continue;
                for (CodedGeometry slicegeom : sliceQuery) {
                    if (slicegeom.getReferenceSystem().equals(rs)) {
                        continue loop;
                    }
                }
                //not found
                final Optional<CodedGeometry> slice = resourceGeometry.slice(rs);
                if (!slice.isPresent()) continue;
                final CodedGeometry slicegeom = slice.get();
                final Optional<GridGeometry> regularGrid = slicegeom.isRegularGrid();
                if (!regularGrid.isPresent()) continue;
                double ratio = 1.0;
                if (rs instanceof VerticalCRS) {
                    ratio = 0.0; //prefer the lowest level
                }
                GridGeometry sliceGrid = regularGrid.get();
                GridGeometry build = sliceGrid.derive().sliceByRatio(ratio).build();
                sliceQuery.add(new CodedGeometry(build));
            }


            //place RS in order : horiontal > vertical > others
            sliceQuery.sort(new Comparator<CodedGeometry>() {
                @Override
                public int compare(CodedGeometry o1, CodedGeometry o2) {
                    final ReferenceSystem rs1 = o1.getReferenceSystem();
                    final ReferenceSystem rs2 = o2.getReferenceSystem();
                    if (rs1 instanceof DiscreteGlobalGridReferenceSystem) return -1;
                    if (rs2 instanceof DiscreteGlobalGridReferenceSystem) return +1;

                    final CoordinateReferenceSystem crs1 = (CoordinateReferenceSystem) rs1;
                    final CoordinateReferenceSystem crs2 = (CoordinateReferenceSystem) rs2;
                    if (CRS.isHorizontalCRS(crs1)) return -1;
                    if (CRS.isHorizontalCRS(crs2)) return +1;
                    if (crs1 instanceof VerticalCRS) return -1;
                    if (crs2 instanceof VerticalCRS) return +1;
                    return 0;
                }
            });
            final CodedGeometry query = CodedGeometry.compound(sliceQuery.toArray(CodedGeometry[]::new));

            try {
                dggrsCoverage = resource.read(query, bands);
            } catch (NoSuchDataException ex) {
                //create an empty one
                dggrsCoverage = new ArrayCodedCoverage(Names.createLocalName(null, null, "empty coverage"), query, resource.getSampleDimensions().toArray(SampleDimension[]::new));
            }

        } catch (TransformException | DataStoreException | FactoryException ex) {
            throw new CstlServiceException(ex.getMessage(), ex);
        }

        return dggrsCoverage;
    }

    private static CodedCoverage applyFilter(CodedCoverage coverage, Filter filter) throws CstlServiceException {
        if (filter == null || filter.equals(Filter.include())) return coverage;

        final List<SampleDimension> sampleDimensions = coverage.getSampleDimensions();
        final String[] propertyNames = sampleDimensions.stream().map(SampleDimension::getName).map(GenericName::toString).toArray(String[]::new);
        final CodedCoverageAsFeatureSet fs = new CodedCoverageAsFeatureSet(coverage, false, CodedCoverageAsFeatureSet.GEOMETRY_ZONE_NONE);

        try (Stream<Feature> stream = fs.features(false)) {
            final Iterator<Feature> iterator = stream.iterator();
            while (iterator.hasNext()) {
                final Feature sample = iterator.next();
                if (!filter.test(sample)) {
                    for (String pname : propertyNames) {
                        sample.setPropertyValue(pname, Double.NaN);
                    }
                }
            }
        } catch (DataStoreException ex) {
            throw new CstlServiceException(ex.getMessage(), ex);
        }
        return coverage;
    }

    /**
     * Apply scale and translate to coverag sample values.
     *
     * @param coverage
     * @param valuesScale
     * @param valuesOffset
     * @return same coverage as input
     */
    private static CodedCoverage applyScaleOffset(CodedCoverage coverage, Double valuesScale, Double valuesOffset) {
        final List<SampleDimension> sampleDimensions = coverage.getSampleDimensions();
        final String[] propertyNames = sampleDimensions.stream().map(SampleDimension::getName).map(GenericName::toString).toArray(String[]::new);

        //apply scale and translate
        final double scale = valuesScale != null ? valuesScale : 1;
        final double offset = valuesOffset != null ? valuesOffset : 0;
        if (scale != 1 || offset != 0) {
            final WritableCodeIterator iterator = coverage.createWritableIterator();
            double[] arr = new double[propertyNames.length];
            while (iterator.next()) {
                arr = iterator.getCell(arr);
                for (int i = 0; i < propertyNames.length; i++) {
                    arr[i] = arr[i] * scale + offset;
                }
                iterator.setCell(arr);
            }
        }

        return coverage;
    }

    /**
     * JSON do not have support for NaN.
     * replace those by null.
     */
    private static Double noNaN(Double value) {
        if (value == null || Double.isNaN(value)) return null;
        return value;
    }

    private static int getDefaultDepth(DiscreteGlobalGridReferenceSystem dggrs) {
        return dggrs.getGridSystem().getRefinementRatio() > 4 ? 3 : 4;
    }

    private static int getMaxRelativeDepth(DiscreteGlobalGridReferenceSystem dggrs) {
        final int refinementRatio = dggrs.getGridSystem().getRefinementRatio();
        switch (refinementRatio) {
            case 1 :
            case 2 :
            case 3 : return 10;
            case 4 : return 8;
            default : return 6;
        }
    }

    private static byte[] toGeojson(String geometry, String profile, CoordinateReferenceSystem crs, CodedCoverage ... coverages) throws IOException, DataStoreException {
        final FeatureSet fs = CodedCoverages.viewAsFeatureSet(coverages[0], false, geometry);
        final FeatureType type = fs.getType();

        //we do not want cells where all values are NaN
        // this is specified in : https://docs.ogc.org/DRAFTS/21-038r1.html#conf_data-cql2-filter_filter
        final FeatureType sampleType = new CodedCoverageAsFeatureSet(coverages[0], false, geometry).getType();
        final FilterFactory<Feature, Object, Object> ff = DefaultFilterFactory.forFeatures();
        Filter filter = Filter.include();
        for (PropertyType pt : sampleType.getProperties(true)) {
            if (pt instanceof AttributeType at) {
                final boolean isNumeric = Number.class.isAssignableFrom(at.getValueClass());
                final Filter dimFilter;
                final ValueReference<Feature, ?> property = ff.property(at.getName().tip().toString());
                if (isNumeric) {
                    dimFilter =
                            ff.or(
                                ff.isNull(property),
                                ff.or(
                                    ff.equal(property, ff.literal(Double.NaN)),
                                    ff.equal(property, ff.literal(null))
                                )
                            )
                            ;
                } else {
                    dimFilter =
                            ff.or(
                                ff.isNull(property),
                                ff.equal(property, ff.literal(null))

                    );
                }
                if (filter == Filter.include()) {
                    filter = dimFilter;
                } else {
                    filter = ff.and(filter, dimFilter);
                }
            }
        }
        final FeatureQuery query = new FeatureQuery();
        if (!Filter.include().equals(filter)) {
            query.setSelection(ff.not(filter));
        }

        final GeoJSONMapper mapper = new GeoJSONMapper();
        mapper.setIncludeFeatureId(true);
        mapper.setBboxOnCollection(false);
        if (DggsApi.PROFILE_JSONFG.equals(profile)) {
            mapper.setIncludeCoordRefSysOnCollection(true);
            mapper.setIncludeTypeOnCollection(true);
        } else {
            //produce a default geojson
        }

        GeoJSONFeatureCollection res = null;
        for (CodedCoverage dggrsCoverage : coverages) {
            FeatureSet cfs = CodedCoverages.viewAsFeatureSet(dggrsCoverage, false, geometry);
            if (crs != null && !Utilities.equalsIgnoreMetadata(FeatureExt.getCRS(cfs.getType()), crs)) {
                final FeatureQuery projQuery = Query.reproject(cfs.getType(), crs);
                cfs = cfs.subset(projQuery);
            }
            GeoJSONFeatureCollection col = mapper.transform(cfs);
            if (res == null) {
                res = col;
            } else {
                res.getFeatures().addAll(col.getFeatures());
            }
        }

        /* TODO does not work List<Double>, find a solution
        final SimpleModule doubleSer = new SimpleModule("DoubleModule");
        final NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);
        numberFormat.setGroupingUsed(false);
        numberFormat.setMaximumFractionDigits(7);
        doubleSer.addSerializer(Double.class, new JsonSerializer<Double>() {
            @Override
            public void serialize(Double value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeRawValue(numberFormat.format(value));
            }
        });
        jsmapper.registerModule(doubleSer);
        */

        final JsonMapper jsmapper = new JsonMapper();
        return jsmapper.writeValueAsBytes(res);
    }

    private static byte[] toGeotiff(GridCoverage gridCoverage) throws IOException, DataStoreException {
         // TIFF writer do no support writing in output stream currently, we have to write in a file before
        final Path temp = Files.createTempFile("data", ".tiff");
        try (final GeoTiffStore iowriter = (GeoTiffStore) DataStores.openWritable(temp, "GeoTIFF")) {
            iowriter.append(gridCoverage, null);
        }
        byte[] data = Files.readAllBytes(temp);
        Files.delete(temp);
        return data;
    }

    private static byte[] toDggrsZarr(CodedCoverage gridCoverage) throws IOException, DataStoreException {
        throw new UnsupportedOperationException("Zarr module not available");
//        final Path temp = Files.createTempDirectory("zarr");
//        final Path folder = temp.resolve("zarr");
//        Files.createDirectory(folder);
//
//        final CodedResource rgr = new MemoryCodedResource(gridCoverage);
//        ZarrDggsDataStoreProvider provider = new ZarrDggsDataStoreProvider();
//        try (ZarrDggsDataStore store = (ZarrDggsDataStore) provider.open(new StorageConnector(folder))) {
//            store.add(rgr);
//        }
//
//        final Path zip = Files.createTempFile("zarr", ".zip");
//        ZipUtilities.zipNIO(zip, folder);
//
//        byte[] data = Files.readAllBytes(zip);
//        Files.delete(zip);
//        IOUtilities.deleteRecursively(temp);
//        return data;
    }

    private static byte[] toGeoZarr(GridCoverage gridCoverage) throws IOException, DataStoreException {
        throw new UnsupportedOperationException("Zarr module not available");
//        final Path temp = Files.createTempDirectory("zarr");
//        final Path folder = temp.resolve("zarr");
//        Files.createDirectory(folder);
//
//        ZarrDggsDataStoreProvider provider = new ZarrDggsDataStoreProvider();
//        try (ZarrDggsDataStore store = (ZarrDggsDataStore) provider.open(new StorageConnector(folder))) {
//            store.add(new MemoryGridCoverageResource(null, Names.createLocalName(null, null, "zarr"), gridCoverage, null));
//        }
//
//        final Path zip = Files.createTempFile("zarr", ".zip");
//        ZipUtilities.zipNIO(zip, folder);
//
//        byte[] data = Files.readAllBytes(zip);
//        Files.delete(zip);
//        IOUtilities.deleteRecursively(temp);
//        return data;
    }

    private static byte[] toCoverageJson(GenericName name, GridCoverage gridCoverage) throws IOException, DataStoreException {
        final Path temp = Files.createTempFile("data", ".covjson");
        Files.delete(temp);
        try (final CoverageJsonStore store = (CoverageJsonStore) DataStores.openWritable(temp, "CoverageJSON")) {
            store.add(new MemoryGridCoverageResource(null, name, gridCoverage, null));
        }
        byte[] data = Files.readAllBytes(temp);
        Files.delete(temp);
        return data;
    }

    private static DggrsData toDggrsData(DiscreteGlobalGridReferenceSystem dggrs, CodedCoverage[] coverages, boolean writeZoneIds) throws CstlServiceException {

        //create schema
        final List<SampleDimension> sampleDimensions = coverages[0].getSampleDimensions();
        final String[] propertyNames = sampleDimensions.stream().map(SampleDimension::getName).map(GenericName::toString).toArray(String[]::new);

        final FeatureType sampleType;
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("rs");
        CodedCoverageAsFeatureSet.toFeatureType(ftb, sampleDimensions);
        sampleType = ftb.build();
        final JSONSchema schema = toJsonSchema(sampleType, coverages[0], true);

        //create value objects
        final Map<String, List<DggrsDataValue>> values = new LinkedHashMap<>();
        final int nbSample = propertyNames.length;
        for (int b = 0; b < nbSample; b++) {
            values.put(propertyNames[b], new ArrayList<>());
        }

        //create dimensions
        final CodedGeometry geometry = coverages[0].getGeometry();
        final ReferenceSystem referenceSystem = geometry.getReferenceSystem();
        final List<ReferenceSystem> singleComponents = ReferenceSystems.getSingleComponents(referenceSystem, true);
        final List<Dimension> dimensions = new ArrayList<>();
        final Map<String,Integer> dataDimension = new LinkedHashMap<>();
        int dimCount = 1;
        for (ReferenceSystem rs : singleComponents) {
            if (rs instanceof DiscreteGlobalGridReferenceSystem) continue;

            final CodedGeometry slice = geometry.slice(rs).get();
            final CodeTransform trs = slice.getGridToRS();
            final GridExtent extent = slice.getExtent();
            final long low = extent.getLow(0);
            final long high = extent.getHigh(0);
            final int size = Math.toIntExact(high-low+1);
            Object resolution = null;

            final List<Object> gridValues = new ArrayList<>();
            if (rs instanceof TemporalCRS tcrs) {
                final DefaultTemporalCRS dtcrs = DefaultTemporalCRS.castOrCopy(tcrs);
                try {
                    for (long i = low; i <= high; i++) {
                        Code address = trs.toCode(new int[]{(int)i});
                        Object ordinate = address.getOrdinate(0);
                        ordinate = dtcrs.toInstant(((Number)ordinate).doubleValue());
                        gridValues.add(ordinate);
                    }
                } catch (TransformException ex) {
                    throw new CstlServiceException(ex.getMessage(), ex);
                }
                final double[] sliceRes = slice.getResolution(true);
                if (sliceRes != null && !Double.isNaN(sliceRes[0])) {
                    final Duration duration = dtcrs.toDuration(sliceRes[0]);
                    if (duration != null) {
                        resolution = duration.toString();
                    }
                }

            } else {
                for (long i = low; i <= high; i++) {
                    try {
                        gridValues.add(trs.toCode(new int[]{(int)i}).getOrdinate(0));
                    } catch (TransformException ex) {
                        throw new CstlServiceException(ex.getMessage(), ex);
                    }
                }

                final double[] sliceRes = slice.getResolution(true);
                if (sliceRes != null && !Double.isNaN(sliceRes[0])) {
                    resolution = sliceRes[0];
                }
            }

            final String name;
            if (rs instanceof TemporalCRS) {
                name = "time";
            } else if (rs instanceof VerticalCRS) {
                name = "z";
            } else {
                name = rs.getName().toString();
            }

            final Grid grid = new Grid();
            grid.setBoundsCoordinates(null);
            grid.setCellsCount(size);
            grid.setCoordinates(gridValues);
            grid.setFirstCoordinate(gridValues.get(0));
            grid.setRelativeBounds(null);
            grid.setResolution(resolution);

            final Dimension dimension = new Dimension();
            dimension.setDefinition(null);
            dimension.setGrid(grid);
            dimension.setInterval(List.of(gridValues.get(0), gridValues.get(gridValues.size()-1)));
            dimension.setName(name);
            dimension.setUnit(null);
            dimension.setUnitLang(null);
            dimensions.add(dimension);

            dataDimension.put(name, size);
            dimCount *= size;
        }


        final List<Integer> depths = new ArrayList<>();

        for (CodedCoverage dggrsCoverage : coverages) {

            final DiscreteGlobalGridGeometry dggrsGeom = (DiscreteGlobalGridGeometry) dggrsCoverage.getGeometry().slice(dggrs).get();
            final int depth = dggrsGeom.getRefinementLevel();
            depths.add(depth);

            final List<Object> zoneIds = writeZoneIds ? dggrsGeom.getZoneIds() : null;

            final DggrsDataValue[] ddvs = new DggrsDataValue[nbSample];
            for (int b = 0; b < nbSample; b++) {
                final DggrsDataValue ddv = new DggrsDataValue();
                ddv.setDepth(depth);
                ddv.setData(new ArrayList<>());
                final DggrsDataValueShape shape = new DggrsDataValueShape();
                shape.setCount(0);
                shape.setSubZones(0);
                shape.setDimensions(dataDimension);
                ddv.setShape(shape);
                ddvs[b] = ddv;
                values.get(propertyNames[b]).add(ddv);
            }

            final CodeIterator ite = dggrsCoverage.createIterator();
            while (ite.next()) {
                for (int b = 0; b < nbSample; b++) {
                    final DggrsDataValue ddv = ddvs[b];
                    final DggrsDataValueShape shape = ddv.getShape();
                    shape.setCount(shape.getCount()+1);
                    Object value = ite.getSampleDouble(b);
                    if (value instanceof Number n) {
                        ddv.addDataItem(Double.isNaN(n.doubleValue()) ? null : n.doubleValue());
                    } else {
                        ddv.addDataItem(value);
                    }
                }
            }

            for (DggrsDataValue ddv : ddvs) {
                DggrsDataValueShape shape = ddv.getShape();
                shape.setSubZones(shape.getCount() / dimCount);
                //next line is not in the standard, but experimental
                if (writeZoneIds) shape.setOtherField("zoneIds", zoneIds);
            }
        }

        final DggrsData json = new DggrsData();
        json.setDepths(depths);
        json.setDggrs(String.valueOf(dggrs.getUri()));
        json.setDimensions(dimensions);
        json.setSchema(schema);
        json.setValues(values);
        return json;
    }

    private static CoordinateReferenceSystem toCRS(DirectPosition zonePosition, String crsStr) throws FactoryException {
        final CoordinateReferenceSystem crs;
        switch (crsStr) {
            case LOCAL_UTM : crs = CommonCRS.WGS84.universal(zonePosition.getCoordinate(1), zonePosition.getCoordinate(0)); break;
            case LOCAL_ORTHOGRAPHIC : crs = DiscreteGlobalGridSystems.createOrthographicCRS(CRS84, zonePosition.getCoordinate(1), zonePosition.getCoordinate(0)); break;
            default : crs = CRS.forCode(crsStr);
        }
        return crs;
    }

    /**
     * Convert datetime parameter to a subset parameter.
     */
    private static Optional<String> datetimeToSubset(String datetime) {

        if (datetime != null && !datetime.isBlank()) {
            int split = datetime.indexOf("/");
            if (split < 0) {
                //single value
                return Optional.of("time(\"" + datetime +"\")");
            } else {
                String start = datetime.substring(0, split).trim();
                String end = datetime.substring(split+1).trim();
                if ("..".equals(start)) start = Instant.ofEpochMilli(Long.MIN_VALUE).toString();
                if ("..".equals(end)) end = Instant.ofEpochMilli(Long.MAX_VALUE).toString();
                return Optional.of("time(\"" + start +"\" : \"" + end + "\")");
            }
        }
        return Optional.empty();
    }

    private static Entry<String,NumberRange<Double>> parseSubsetRange(String sub) throws DGGSService.BadParameterException {

        final int start = sub.indexOf('(');
        final int end = sub.indexOf(')');
        if (start < 1 || end < 1 || end < start) throw new DGGSService.BadParameterException("Incorrect subset format : " + sub);
        final String axisName = sub.substring(0, start);
        String parameters = sub.substring(start+1, end).trim();
        final double intervalStart;
        final double intervalEnd;
        if (parameters.startsWith("\"")) {
            int se = parameters.indexOf("\"", 1);
            if (se < 0) throw new DGGSService.BadParameterException("Incorrect subset format : " + sub);
            intervalStart = Instant.parse(parameters.substring(1, se)).toEpochMilli();
            int s = parameters.indexOf(":", se);
            if (s < 0) {
                // value
                intervalEnd = intervalStart;
            } else {
                // start : end
                int ns = parameters.indexOf("\"", se+1);
                int ne = parameters.indexOf("\"", ns+1);
                if (ns < s) throw new DGGSService.BadParameterException("Incorrect subset format : " + sub);
                if (ne < 0) throw new DGGSService.BadParameterException("Incorrect subset format : " + sub);
                intervalEnd = Instant.parse(parameters.substring(ns+1, ne)).toEpochMilli();
            }

        } else {
            int s = parameters.indexOf(":");
            if (s < 0) {
                // value
                intervalStart = intervalEnd = Double.parseDouble(parameters.trim());
            } else {
                // start : end
                intervalStart = Double.parseDouble(parameters.substring(0, s).trim());
                intervalEnd = Double.parseDouble(parameters.substring(s+1).trim());
            }
        }

        return new AbstractMap.SimpleEntry<>(axisName, NumberRange.create(intervalStart, true, intervalEnd, true));
    }

    private static int getAxisIndex(CoordinateReferenceSystem crs, String name) throws DGGSService.BadParameterException {

        final AxisDirection searched;
        switch (name) {
            case DggsApi.SUBSET_E :
            case DggsApi.SUBSET_LON : searched = AxisDirection.EAST; break;
            case DggsApi.SUBSET_N :
            case DggsApi.SUBSET_LAT : searched = AxisDirection.NORTH; break;
            case DggsApi.SUBSET_H :
            case DggsApi.SUBSET_Z : searched = AxisDirection.UP; break;
            case DggsApi.SUBSET_TIME : searched = AxisDirection.FUTURE; break;
            default: throw new DGGSService.BadParameterException("Unknown subset " + name);
        }

        final CoordinateSystem cs = crs.getCoordinateSystem();
        for (int i = 0, n = cs.getDimension(); i < n; i++) {
            final CoordinateSystemAxis axis = cs.getAxis(i);
            final AxisDirection direction = simplify(axis.getDirection());
            if (searched.equals(direction)) return i;
        }

        throw new DGGSService.BadParameterException("Could not find a matching crs axis for subset " + name);
    }

    /**
     * Simplify direction to EAST,NORTH,UP,FUTURE,UNSPECIFIED whatever the orientation.
     */
    private static AxisDirection simplify(AxisDirection direction) {
             if (AxisDirection.NORTH            .equals(direction)) return AxisDirection.NORTH;
        else if (AxisDirection.NORTH_NORTH_EAST .equals(direction)) return AxisDirection.NORTH;
        else if (AxisDirection.NORTH_EAST       .equals(direction)) return AxisDirection.NORTH;
        else if (AxisDirection.EAST_NORTH_EAST  .equals(direction)) return AxisDirection.EAST;
        else if (AxisDirection.EAST             .equals(direction)) return AxisDirection.EAST;
        else if (AxisDirection.EAST_SOUTH_EAST  .equals(direction)) return AxisDirection.EAST;
        else if (AxisDirection.SOUTH_EAST       .equals(direction)) return AxisDirection.NORTH;
        else if (AxisDirection.SOUTH_SOUTH_EAST .equals(direction)) return AxisDirection.NORTH;
        else if (AxisDirection.SOUTH            .equals(direction)) return AxisDirection.NORTH;
        else if (AxisDirection.SOUTH_SOUTH_WEST .equals(direction)) return AxisDirection.NORTH;
        else if (AxisDirection.SOUTH_WEST       .equals(direction)) return AxisDirection.NORTH;
        else if (AxisDirection.WEST_SOUTH_WEST  .equals(direction)) return AxisDirection.EAST;
        else if (AxisDirection.WEST             .equals(direction)) return AxisDirection.EAST;
        else if (AxisDirection.WEST_NORTH_WEST  .equals(direction)) return AxisDirection.EAST;
        else if (AxisDirection.NORTH_WEST       .equals(direction)) return AxisDirection.NORTH;
        else if (AxisDirection.NORTH_NORTH_WEST .equals(direction)) return AxisDirection.NORTH;
        else if (AxisDirection.UP               .equals(direction)) return AxisDirection.UP;
        else if (AxisDirection.DOWN             .equals(direction)) return AxisDirection.UP;
        else if (AxisDirection.GEOCENTRIC_X     .equals(direction)) return AxisDirection.NORTH;
        else if (AxisDirection.GEOCENTRIC_Y     .equals(direction)) return AxisDirection.EAST;
        else if (AxisDirection.GEOCENTRIC_Z     .equals(direction)) return AxisDirection.UP;
        else if (AxisDirection.COLUMN_POSITIVE  .equals(direction)) return AxisDirection.NORTH;
        else if (AxisDirection.COLUMN_NEGATIVE  .equals(direction)) return AxisDirection.NORTH;
        else if (AxisDirection.ROW_POSITIVE     .equals(direction)) return AxisDirection.EAST;
        else if (AxisDirection.ROW_NEGATIVE     .equals(direction)) return AxisDirection.EAST;
        else if (AxisDirection.DISPLAY_RIGHT    .equals(direction)) return AxisDirection.EAST;
        else if (AxisDirection.DISPLAY_LEFT     .equals(direction)) return AxisDirection.EAST;
        else if (AxisDirection.DISPLAY_UP       .equals(direction)) return AxisDirection.NORTH;
        else if (AxisDirection.DISPLAY_DOWN     .equals(direction)) return AxisDirection.NORTH;
        else if (AxisDirection.FUTURE           .equals(direction)) return AxisDirection.FUTURE;
        else if (AxisDirection.PAST             .equals(direction)) return AxisDirection.FUTURE;
        else return AxisDirection.UNSPECIFIED;
    }

    private static Extent fillExtent(Extent extent, Envelope env) {
        if (env == null) return extent;
        final SpatialExtent spatial = new SpatialExtent();

        Envelope e3d = null;
        if (CRS.getVerticalComponent(env.getCoordinateReferenceSystem(), true) != null) {
            try {
                e3d = Envelopes.transform(env, CRS84H);
            } catch (TransformException te) {
                //do nothing
            }
        }
        Envelope e2d = null;
        try {
            e2d = Envelopes.transform(env, CRS84);
        } catch (TransformException ex) {
                //do nothing
        }

        if (e3d != null) {
            spatial.setCrs(SpatialExtent.CRS84H);
            spatial.setBbox(new double[][]{{
                    e3d.getMinimum(0), e3d.getMinimum(1), e3d.getMinimum(2),
                    e3d.getMaximum(0), e3d.getMaximum(1), e3d.getMaximum(2)}});
            extent.setSpatial(spatial);
        } else if (e2d != null) {
            spatial.setCrs(SpatialExtent.CRS84);
            spatial.setBbox(new double[][]{{
                    e2d.getMinimum(0), e2d.getMinimum(1),
                    e2d.getMaximum(0), e2d.getMaximum(1)}});
            extent.setSpatial(spatial);
        }

        final TemporalCRS temporalComponent = CRS.getTemporalComponent(env.getCoordinateReferenceSystem());
        if (temporalComponent != null) {
            try {
                Envelope e = Envelopes.transform(env, temporalComponent);
                //TODO
            } catch (TransformException te) {
                //do nothing
            }
        }

        return extent;
    }

    private static Extent fillExtent(Extent extent, SortedSet<Date> dates) {
        if (dates == null || dates.isEmpty()) return extent;
        final TemporalExtent temporal = new TemporalExtent();
        final OffsetDateTime temporalStart = dates.first().toInstant().atOffset(ZoneOffset.UTC);
        final OffsetDateTime temporalEnd = dates.last().toInstant().atOffset(ZoneOffset.UTC);
        temporal.setInterval(new OffsetDateTime[][]{{temporalStart, temporalEnd}});
        extent.setTemporal(temporal);
        return extent;
    }

    private static Extent fillExtent(Extent extent, Instant[] range) {
        if (range == null || range.length == 0) return extent;
        final TemporalExtent temporal = new TemporalExtent();
        final OffsetDateTime temporalStart = range[0].atOffset(ZoneOffset.UTC);
        final OffsetDateTime temporalEnd = range[range.length-1].atOffset(ZoneOffset.UTC);
        temporal.setInterval(new OffsetDateTime[][]{{temporalStart, temporalEnd}});
        extent.setTemporal(temporal);
        return extent;
    }

    private static Extent fillExtent(Extent extent, GridGeometry gridGeom) throws FactoryException, TransformException {
        if (gridGeom == null) return extent;

        if (gridGeom.isDefined(GridGeometry.EXTENT | GridGeometry.CRS | GridGeometry.GRID_TO_CRS)) {

            final CoordinateReferenceSystem crs = gridGeom.getCoordinateReferenceSystem();
            final CoordinateReferenceSystem horizontal = CRS.getHorizontalComponent(crs);
            final CoordinateReferenceSystem temporal = CRS.getTemporalComponent(crs);

            if (horizontal != null) {
                final GridGeometry slice = CodeTransforms.slice(gridGeom, horizontal);
                final GridExtent hext = slice.getExtent();
                fillExtent(extent, slice.getEnvelope());
                final SpatialExtent spatial = extent.getSpatial();

                for (int i = 0, n = hext.getDimension(); i < n; i++) {
                    final GridExtent xExt = hext.selectDimensions(i);
                    final GridGeometry gg = new GridGeometry(xExt, PixelInCell.CELL_CENTER, MathTransforms.linear(1, 0), null);
                    //todo to big, may contain millions of values
                    //spatial.addGridItem(createGrid(new CodedGeometry(gg)));
                    final Grid grid = new Grid();
                    grid.setCellsCount(Math.toIntExact(xExt.getSize(0)));
                    spatial.addGridItem(grid);
                }
            }

            if (temporal != null) {
                final GridGeometry slice = CodeTransforms.slice(gridGeom, temporal);
                fillExtent(extent, slice.getTemporalExtent());
                final TemporalExtent text = extent.getTemporal();
                text.setGrid(createGrid(new CodedGeometry(slice)));
            }

            final List<SingleCRS> singleComponents = CRS.getSingleComponents(crs);
            for (SingleCRS scrs : singleComponents) {
                if (Utilities.equalsIgnoreMetadata(scrs, horizontal)) continue;
                if (Utilities.equalsIgnoreMetadata(scrs, temporal)) continue;

                try {
                    if (scrs.getCoordinateSystem().getDimension() == 1) {
                        final GridGeometry slice = CodeTransforms.slice(gridGeom, scrs);
                        final Envelope senv = slice.getEnvelope();
                        final GridExtent hext = slice.getExtent();
                        final OtherDimension od = new OtherDimension();

                        final List<Object> interval = new ArrayList<>();
                        interval.add(senv.getMinimum(0));
                        interval.add(senv.getMaximum(0));
                        od.setInterval(List.of(interval));

                        for (int i = 0, n = hext.getDimension(); i < n; i++) {
                            final GridExtent xExt = hext.selectDimensions(i);
                            final GridGeometry gg = new GridGeometry(xExt, PixelInCell.CELL_CENTER, MathTransforms.linear(1, 0), null);
                            final Grid grid = createGrid(new CodedGeometry(gg));
                            od.setGrid(grid);
                        }
                        String dimName = scrs.getName().getCode();
                        extent.setOtherField(dimName, od);
                    }

                } catch (FactoryException fe) {
                    LOGGER.log(Level.WARNING, fe.getMessage(), fe);
                }
            }

        } else if (gridGeom.isDefined(GridGeometry.ENVELOPE)) {
            fillExtent(extent, gridGeom.getEnvelope());
        }

        return extent;
    }

    private static Grid createGrid(CodedGeometry slice) throws TransformException {
        final ReferenceSystem rs = slice.getReferenceSystem();
        final CodeTransform trs = slice.getGridToRS();
        final GridExtent extent = slice.getExtent();
        final long low = extent.getLow(0);
        final long high = extent.getHigh(0);
        final int size = Math.toIntExact(high-low+1);
        Object resolution;

        final List<Object> gridValues = new ArrayList<>();
        if (rs instanceof TemporalCRS tcrs) {
            final DefaultTemporalCRS dtcrs = DefaultTemporalCRS.castOrCopy(tcrs);
            for (long i = low; i <= high; i++) {
                Code address = trs.toCode(new int[]{(int)i});
                Object ordinate = address.getOrdinate(0);
                ordinate = dtcrs.toInstant(((Number)ordinate).doubleValue());
                gridValues.add(ordinate);
            }
            Duration duration = dtcrs.toDuration(slice.getResolution(true)[0]);
            if (duration == null) {
                duration = Duration.ZERO;
            }
            resolution = duration.toString();

        } else {
            for (long i = low; i <= high; i++) {
                gridValues.add(trs.toCode(new int[]{(int)i}).getOrdinate(0));
            }
            double[] res = slice.getResolution(true);
            resolution = res != null ? res[0] : null;
        }

        final Grid grid = new Grid();
        grid.setBoundsCoordinates(null);
        grid.setCellsCount(size);
        grid.setCoordinates(gridValues);
        grid.setFirstCoordinate(gridValues.get(0));
        grid.setRelativeBounds(null);
        grid.setResolution(resolution);
        return grid;
    }

    private static byte[] compressGzip(byte[] data) throws IOException {
        final ByteArrayOutputStream bo = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(bo)) {
            gz.write(data);
            gz.flush();
        }
        return bo.toByteArray();
    }

}
