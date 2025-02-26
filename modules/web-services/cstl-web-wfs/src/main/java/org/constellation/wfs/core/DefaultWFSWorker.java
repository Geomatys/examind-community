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

package org.constellation.wfs.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import org.apache.sis.feature.privy.AttributeConvention;
import org.apache.sis.geometry.wrapper.jts.JTS;
import org.apache.sis.storage.aggregate.ConcatenatedFeatureSet;
import org.apache.sis.storage.FeatureQuery;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.WritableFeatureSet;
import org.apache.sis.storage.event.StoreListener;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.Version;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.xml.Namespaces;
import static org.constellation.api.CommonConstants.OUTPUT_FORMAT;
import org.constellation.api.ServiceDef;
import org.constellation.api.WorkerState;
import org.constellation.dto.contact.Details;
import org.constellation.dto.service.config.wxs.FormatURL;
import org.constellation.dto.service.config.wxs.LayerConfig;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationStoreException;
import static org.constellation.map.util.MapUtils.transformJAXBFilter;
import static org.constellation.map.util.MapUtils.visitJaxbSortBy;
import org.constellation.provider.Data;
import org.constellation.provider.FeatureData;
import org.constellation.util.QNameComparator;
import org.constellation.util.Util;
import org.constellation.wfs.NameOverride;
import static org.constellation.wfs.core.AtomLinkBuilder.BuildItemsLink;
import static org.constellation.wfs.core.AtomLinkBuilder.buildDescribedByLink;
import org.constellation.wfs.core.WFSConstants.GetXSD;
import static org.constellation.wfs.core.WFSConstants.IDENTIFIER_FILTER;
import static org.constellation.wfs.core.WFSConstants.IDENTIFIER_PARAM;
import static org.constellation.wfs.core.WFSConstants.OPERATIONS_METADATA_V110;
import static org.constellation.wfs.core.WFSConstants.OPERATIONS_METADATA_V200;
import static org.constellation.wfs.core.WFSConstants.TYPE_PARAM;
import static org.constellation.wfs.core.WFSConstants.UNKNOW_TYPENAME;
import org.constellation.ws.CstlServiceException;
import org.constellation.ws.LayerCache;
import org.constellation.ws.LayerWorker;
import org.constellation.ws.MimeType;
import static org.constellation.ws.MimeType.APP_GML32_XML;
import static org.constellation.ws.MimeType.TEXT_GML31_XML;
import org.geotoolkit.storage.feature.FeatureStore;
import org.geotoolkit.storage.feature.FeatureStoreRuntimeException;
import org.geotoolkit.storage.feature.FeatureStoreUtilities;
import org.geotoolkit.storage.memory.InMemoryFeatureSet;
import org.geotoolkit.feature.FeatureExt;
import org.geotoolkit.feature.FeatureTypeExt;
import org.geotoolkit.feature.xml.BoundingBox;
import org.geotoolkit.feature.xml.Extent;
import org.geotoolkit.atom.xml.Link;
import org.geotoolkit.feature.model.FeatureSetWrapper;
import org.geotoolkit.feature.xml.Utils;
import org.geotoolkit.feature.xml.XmlFeatureSet;
import org.geotoolkit.feature.xml.jaxb.JAXBFeatureTypeWriter;
import org.geotoolkit.feature.xml.jaxp.JAXPStreamFeatureReader;
import org.geotoolkit.filter.FilterUtilities;
import org.geotoolkit.filter.binding.Binding;
import org.geotoolkit.filter.binding.Bindings;
import org.geotoolkit.filter.visitor.FillCrsVisitor;
import org.geotoolkit.filter.visitor.IsValidSpatialFilterVisitor;
import org.geotoolkit.filter.visitor.ListingPropertyVisitor;
import org.geotoolkit.gml.GeometrytoJTS;
import org.geotoolkit.gml.xml.AbstractGML;
import org.geotoolkit.gml.xml.DirectPosition;
import org.geotoolkit.gml.xml.v311.AbstractGeometryType;
import org.geotoolkit.gml.xml.v311.FeaturePropertyType;
import org.geotoolkit.ogc.xml.XMLFilter;
import org.geotoolkit.ogc.xml.XMLLiteral;
import org.geotoolkit.ogc.xml.v200.BBOXType;
import org.geotoolkit.ows.xml.AbstractCapabilitiesBase;
import org.geotoolkit.ows.xml.AbstractDomain;
import org.geotoolkit.ows.xml.AbstractOperationsMetadata;
import org.geotoolkit.ows.xml.AbstractServiceIdentification;
import org.geotoolkit.ows.xml.AbstractServiceProvider;
import org.geotoolkit.ows.xml.AcceptVersions;
import static org.geotoolkit.ows.xml.OWSExceptionCode.DUPLICATE_STORED_QUERY_ID_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.MISSING_PARAMETER_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.NO_APPLICABLE_CODE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.VERSION_NEGOTIATION_FAILED;
import org.geotoolkit.ows.xml.RequestBase;
import org.geotoolkit.ows.xml.Sections;
import org.geotoolkit.storage.event.FeatureStoreContentEvent;
import org.geotoolkit.util.NamesExt;
import org.geotoolkit.wfs.xml.CreateStoredQuery;
import org.geotoolkit.wfs.xml.CreateStoredQueryResponse;
import org.geotoolkit.wfs.xml.DeleteElement;
import org.geotoolkit.wfs.xml.DescribeFeatureType;
import org.geotoolkit.wfs.xml.DescribeStoredQueries;
import org.geotoolkit.wfs.xml.DescribeStoredQueriesResponse;
import org.geotoolkit.wfs.xml.DropStoredQuery;
import org.geotoolkit.wfs.xml.DropStoredQueryResponse;
import org.geotoolkit.wfs.xml.FeatureRequest;
import org.geotoolkit.wfs.xml.FeatureTypeList;
import org.geotoolkit.wfs.xml.GetCapabilities;
import org.geotoolkit.wfs.xml.GetFeature;
import org.geotoolkit.wfs.xml.GetGmlObject;
import org.geotoolkit.wfs.xml.GetPropertyValue;
import org.geotoolkit.wfs.xml.IdentifierGenerationOptionType;
import org.geotoolkit.wfs.xml.InsertElement;
import org.geotoolkit.wfs.xml.ListStoredQueries;
import org.geotoolkit.wfs.xml.ListStoredQueriesResponse;
import org.geotoolkit.wfs.xml.LockFeature;
import org.geotoolkit.wfs.xml.LockFeatureResponse;
import org.geotoolkit.wfs.xml.Parameter;
import org.geotoolkit.wfs.xml.ParameterExpression;
import org.geotoolkit.wfs.xml.Property;
import org.geotoolkit.wfs.xml.Query;
import org.geotoolkit.wfs.xml.QueryExpressionText;
import org.geotoolkit.wfs.xml.ReplaceElement;
import org.geotoolkit.wfs.xml.ResultTypeType;
import org.geotoolkit.wfs.xml.StoredQueries;
import org.geotoolkit.wfs.xml.StoredQuery;
import org.geotoolkit.wfs.xml.StoredQueryDescription;
import org.geotoolkit.wfs.xml.Transaction;
import org.geotoolkit.wfs.xml.TransactionResponse;
import org.geotoolkit.wfs.xml.UpdateElement;
import org.geotoolkit.wfs.xml.WFSCapabilities;
import org.geotoolkit.wfs.xml.WFSMarshallerPool;
import org.geotoolkit.wfs.xml.WFSXmlFactory;
import static org.geotoolkit.wfs.xml.WFSXmlFactory.buildBBOX;
import static org.geotoolkit.wfs.xml.WFSXmlFactory.buildCreateStoredQueryResponse;
import static org.geotoolkit.wfs.xml.WFSXmlFactory.buildDescribeStoredQueriesResponse;
import static org.geotoolkit.wfs.xml.WFSXmlFactory.buildDropStoredQueryResponse;
import static org.geotoolkit.wfs.xml.WFSXmlFactory.buildFeatureType;
import static org.geotoolkit.wfs.xml.WFSXmlFactory.buildFeatureTypeList;
import static org.geotoolkit.wfs.xml.WFSXmlFactory.buildListStoredQueriesResponse;
import static org.geotoolkit.wfs.xml.WFSXmlFactory.buildSections;
import static org.geotoolkit.wfs.xml.WFSXmlFactory.buildTransactionResponse;
import static org.geotoolkit.wfs.xml.WFSXmlFactory.buildWFSCapabilities;
import org.geotoolkit.wfs.xml.v110.FeatureCollectionType;
import org.geotoolkit.wfs.xml.v200.ObjectFactory;
import org.geotoolkit.wfs.xml.v200.PropertyName;
import org.geotoolkit.wfs.xml.v200.QueryExpressionTextType;
import org.geotoolkit.wfs.xml.v200.QueryType;
import org.geotoolkit.wfs.xml.v200.StoredQueryDescriptionType;
import org.geotoolkit.xsd.xml.v2001.FormChoice;
import org.geotoolkit.xsd.xml.v2001.Import;
import org.geotoolkit.xsd.xml.v2001.Schema;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureAssociationRole;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyNotFoundException;
import org.opengis.feature.PropertyType;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.LogicalOperator;
import org.opengis.filter.LogicalOperatorName;
import org.opengis.filter.Filter;
import org.geotoolkit.filter.capability.FilterCapabilities;
import org.opengis.filter.ResourceId;
import org.opengis.filter.SortProperty;
import org.opengis.filter.BinarySpatialOperator;
import org.opengis.filter.FilterFactory;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.CodeList;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component("WFSWorker")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DefaultWFSWorker extends LayerWorker implements WFSWorker {

    /**
     * Base known CRS.
     */
    private final static List<String> DEFAULT_CRS;
    static {
        final String[] codes = {"EPSG:4326", "EPSG:3395", "CRS:84"};
        final List<String> tmpUrns = new ArrayList<>();
        for (final String code : codes) {
            try {
                final CoordinateReferenceSystem crs = CRS.forCode(code);
                String urn = IdentifiedObjects.lookupURN(crs, null);
                if (urn != null) {
                    tmpUrns.add(urn);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Cannot initialize default coordinate reference system", e);
            }
        }
        DEFAULT_CRS = Collections.unmodifiableList(tmpUrns);
    }

    private List<StoredQueryDescription> storedQueries = new ArrayList<>();

    public DefaultWFSWorker(final String id) {
        super(id, ServiceDef.Specification.WFS);
        if (getState().equals(WorkerState.ERROR)) return;
        // loading stored queries
        loadStoredQueries();
        started();
    }

    private void loadStoredQueries() {
        try {
            final Object obj = serviceBusiness.getExtraConfiguration("WFS", getId(), "StoredQueries.xml",  WFSMarshallerPool.getInstance());
            if (obj instanceof StoredQueries sq) {
                this.storedQueries = sq.getStoredQuery();
            } else if (obj != null) {
                LOGGER.log(Level.WARNING, "The storedQueries File does not contains proper object");
            }
        } catch (ConstellationException ex) {
            LOGGER.log(Level.WARNING, "ConfigurationException while unmarshalling the stored queries File", ex);
        }

        // we verify if the identifier query is loaded (if not we load it)
       boolean foundID = false;
       for (StoredQueryDescription squery : storedQueries) {
           if ("urn:ogc:def:query:OGC-WFS::GetFeatureById".equals(squery.getId())) {
               foundID = true;
               break;
           }
       }
       if (!foundID) {
           final List<QName> typeNames = new ArrayList<>(getTypeNames(null));
           Collections.sort(typeNames, new QNameComparator());
           final QueryType query = new QueryType(IDENTIFIER_FILTER, typeNames, "2.0.0");
           final QueryExpressionTextType queryEx = new QueryExpressionTextType("urn:ogc:def:queryLanguage:OGC-WFS::WFS_QueryExpression", null, typeNames);
           final ObjectFactory factory = new ObjectFactory();
           queryEx.getContent().add(factory.createQuery(query));
           final StoredQueryDescriptionType idQ = new StoredQueryDescriptionType("urn:ogc:def:query:OGC-WFS::GetFeatureById", "Identifier query" , "filter on feature identifier", IDENTIFIER_PARAM, queryEx);
           storedQueries.add(idQ);
       }

        // we verify if the type query is loaded (if not we load it)
       boolean foundT = false;
       for (StoredQueryDescription squery : storedQueries) {
           if ("urn:ogc:def:storedQuery:OGC-WFS::GetFeatureByType".equals(squery.getId())) {
               foundT = true;
               break;
           }
       }
       if (!foundT) {
           final List<QName> returnTypeNames = new ArrayList<>();
           returnTypeNames.add(new QName("http://www.opengis.net/gml/3.2", "AbstractFeatureType"));
           final List<QName> typeNames = new ArrayList<>();
           typeNames.add(new QName("$typeName"));
           final QueryType query = new QueryType(null, typeNames, "2.0.0");
           final QueryExpressionTextType queryEx = new QueryExpressionTextType("urn:ogc:def:queryLanguage:OGC-WFS::WFS_QueryExpression", null, returnTypeNames);
           final ObjectFactory factory = new ObjectFactory();
           queryEx.getContent().add(factory.createQuery(query));
           final StoredQueryDescriptionType idQ = new StoredQueryDescriptionType("urn:ogc:def:storedQuery:OGC-WFS::GetFeatureByType", "By type query" , "filter on feature type", TYPE_PARAM, queryEx);
           storedQueries.add(idQ);
       }

    }

    private void storedQueries() {
        try {
            serviceBusiness.setExtraConfiguration("WFS", getId(), "StoredQueries.xml", new StoredQueries(storedQueries),  WFSMarshallerPool.getInstance());
        } catch (ConstellationException ex) {
            LOGGER.log(Level.WARNING, "Error while writing stored queries", ex);
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public WFSCapabilities getCapabilities(final GetCapabilities request) throws CstlServiceException {
        LOGGER.log(Level.FINE, "GetCapabilities request proccesing");
        final long start = System.currentTimeMillis();

        final String userLogin  = getUserLogin();
        //choose the best version from acceptVersion
        final AcceptVersions versions = request.getAcceptVersions();
        if (versions != null) {
            Version max = null;
            for (String v : versions.getVersion()) {
                final Version vv = new Version(v);
                if (isSupportedVersion(v)) {
                    if (max == null || vv.compareTo(max) > 1) {
                        max = vv;
                    }
                }
            }
            if (max != null) {
                request.setVersion(max.toString());
            }
        }

        // we verify the base attribute
        verifyBaseRequest(request, false, true);
        final String currentVersion = request.getVersion().toString();

        //set the current updateSequence parameter
        final boolean returnUS = returnUpdateSequenceDocument(request.getUpdateSequence());
        if (returnUS) {
            return WFSXmlFactory.buildWFSCapabilities(currentVersion, getCurrentUpdateSequence());
        }

        Sections sections = request.getSections();
        if (sections == null) {
            sections = buildSections(currentVersion, Arrays.asList("All"));
        }

        final AbstractCapabilitiesBase cachedCapabilities = (WFSCapabilities) getCapabilitiesFromCache(currentVersion, null);
        if (cachedCapabilities != null) {
            return (WFSCapabilities) cachedCapabilities.applySections(sections);
        }

        final Details skeleton = getStaticCapabilitiesObject("WFS", null);
        final WFSCapabilities inCapabilities = WFSConstants.createCapabilities(currentVersion, skeleton);

        final FeatureTypeList ftl = buildFeatureTypeList(currentVersion);
        /*
         *  layer providers
         */
        final List<LayerCache> layers = getLayerCaches(userLogin);
        final QNameComparator comparator = new QNameComparator();
        Collections.sort(layers, (l1, l2) -> comparator.compare(l1.getName(), l2.getName()));
        for (final LayerCache layer : layers) {
            final Data data = layer.getData();

            if (data instanceof FeatureData fld) {
                final FeatureType type;
                try {
                    type  = fld.getType();
                } catch (ConstellationStoreException ex) {
                    LOGGER.log(Level.WARNING, "Error while getting featureType for:{0}\ncause:{1}", new Object[]{layer.getName(), ex.getMessage()});
                    continue;
                }
                final LayerConfig confLayer = layer.getConfiguration();
                final org.geotoolkit.wfs.xml.FeatureType ftt;
                try {

                    final QName typeName = layer.getName();
                    final String defaultCRS = getCRSCode(type);
                    String title = confLayer.getTitle();
                    if (title == null) {
                        title = layer.getName().getLocalPart();
                    }

                    List<String> others = DEFAULT_CRS;
                    if (DEFAULT_CRS.contains(defaultCRS)) {
                        others = new ArrayList<>(DEFAULT_CRS);
                        others.remove(defaultCRS);
                    }

                    ftt = buildFeatureType(
                            currentVersion,
                            typeName,
                            title,
                            defaultCRS,
                            others,
                            toBBox(layer, currentVersion));

                    /*
                     * we apply the layer customization
                     */
                    ftt.setAbstract(confLayer.getAbstrac());
                    if (!confLayer.getKeywords().isEmpty()) {
                        ftt.addKeywords(confLayer.getKeywords());
                    }
                    List<FormatURL> metadataURLs = confLayer.getMetadataURL();
                    if (metadataURLs != null) {
                        for (FormatURL metadataURL : metadataURLs) {
                            ftt.addMetadataURL(metadataURL.getOnlineResource().getValue(),
                                               metadataURL.getType(),
                                               metadataURL.getFormat());
                        }
                    }
                    if (!confLayer.getCrs().isEmpty()) {
                        ftt.setOtherCRS(confLayer.getCrs());
                    }

                    // we add the feature type description to the list
                    ftl.addFeatureType(ftt);
                } catch (FactoryException ex) {
                    Logging.unexpectedException(LOGGER,DefaultWFSWorker.class,"getCapabilities",ex);
                }

            } else {
                LOGGER.log(Level.WARNING, "The layer:{0} is not a feature layer", layer.getName());
            }
        }

        final AbstractOperationsMetadata om;
        if (currentVersion.equals("2.0.0")) {
            om = OPERATIONS_METADATA_V200.clone();
        } else {
            om = OPERATIONS_METADATA_V110.clone();
        }
        om.updateURL(getServiceUrl());

        if (!isTransactional) {
            om.removeOperation("Transaction");
            final AbstractDomain cst = om.getConstraint("ImplementsTransactionalWFS");
            if (cst != null) {
                cst.setDefaultValue("FALSE");
            }
        }

        final AbstractServiceProvider sp       = inCapabilities.getServiceProvider();
        final AbstractServiceIdentification si = inCapabilities.getServiceIdentification();
        final FilterCapabilities fc;
        if (currentVersion.equals("2.0.0")) {
            fc = WFSConstants.FILTER_CAPABILITIES_V200;
        } else {
            fc = WFSConstants.FILTER_CAPABILITIES_V110;
        }
        final WFSCapabilities result = buildWFSCapabilities(currentVersion, getCurrentUpdateSequence(), si, sp, om, ftl, fc);
        putCapabilitiesInCache(currentVersion, null, result);
        LOGGER.log(Level.FINE, "GetCapabilities treated in {0}ms", (System.currentTimeMillis() - start));
        return (WFSCapabilities) result.applySections(sections);
    }

    private String getCRSCode(FeatureType type) throws FactoryException {
        final CoordinateReferenceSystem crs = getCRS(type);

        String defaultCRS = IdentifiedObjects.lookupURN(crs, null);
        if (defaultCRS == null) {
            defaultCRS = IdentifiedObjects.toURN(crs.getClass(), IdentifiedObjects.getIdentifier(crs, null));
        }

        if (defaultCRS == null) {
            /* If we reach here, we're in a very frightening situation : SIS did
             * not even succeed to give an URN for the most basic CRS. It means
             * that a big problem lies between SIS and EPSG database access.
             * Worst, in this case, SIS should have thrown an error already.
             */
            LOGGER.log(Level.WARNING, "No URN can be created for given CRS : "+crs);
            return IdentifiedObjects.lookupURN(CommonCRS.defaultGeographic(), null);
        }

        return defaultCRS;
    }


    private CoordinateReferenceSystem getCRS(FeatureType type) throws FactoryException {
        PropertyType geomAtt = null;
        try {
            geomAtt = FeatureExt.getDefaultGeometry(type);
        } catch (PropertyNotFoundException|IllegalStateException e) {
            LOGGER.log(Level.FINE, "Cannot determine a primary geometry in given feature type.", e);
        }

        if (geomAtt != null) {
            final CoordinateReferenceSystem crs = FeatureExt.getCRS(geomAtt);
            if (crs!=null) return crs;
        }

        // We fallback on CRS:84
        return CommonCRS.defaultGeographic();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Object describeFeatureType(final DescribeFeatureType request) throws CstlServiceException {
        LOGGER.log(Level.FINE, "DecribeFeatureType request proccesing");
        final long start = System.currentTimeMillis();

        // we verify the base attribute
        verifyBaseRequest(request, false, false);
        final String currentVersion = request.getVersion().toString();
        final String userLogin      = getUserLogin();

        final String gmlVersion;
        if ("2.0.0".equals(currentVersion)) {
            gmlVersion = "3.2.1";
        } else {
            gmlVersion = "3.1.1";
        }
        final JAXBFeatureTypeWriter writer  = new JAXBFeatureTypeWriter(gmlVersion);
        final List<QName> names             = request.getTypeName();
        final List<FeatureType> types       = new ArrayList<>();
        final Map<String, String> locations = new HashMap<>();

        //XmlFeatureSet may provide the xsd themselves
        final Map<FeatureType,Schema> declaredSchema = new HashMap<>();
        if (names.isEmpty()) {
            //search all types
            for (final LayerCache layer : getLayerCaches(userLogin)) {
                if (!(layer.getData() instanceof FeatureData)) {continue;}

                FeatureData data = (FeatureData) layer.getData();

                try {
                    FeatureSet featureset = data.getOrigin();
                    FeatureType ftType = data.getType();

                    if (featureset instanceof XmlFeatureSet xfs) {
                        final Map params = xfs.getSchema();
                        if (params.size() == 1 && params.get(params.keySet().iterator().next()) instanceof Schema) {
                            final FeatureType ft = NameOverride.wrap(ftType, layer.getName());
                            declaredSchema.put(ft, (Schema) params.get(params.keySet().iterator().next()));
                            types.add(ft);
                        } else {
                            locations.putAll(params);
                        }
                    } else {
                        types.add(NameOverride.wrap(ftType, layer.getName()));
                    }
                } catch (ConstellationStoreException | DataStoreException ex) {
                    LOGGER.log(Level.WARNING, "error while getting featureType for:{0}", layer.getName());
                }
            }
        } else {

            //search only the given list
            for (final QName name : names) {
                if (name == null) {continue;}
                final LayerCache layer;
                try {
                    layer = getLayerCache(userLogin, name);
                } catch (CstlServiceException ex) {
                    throw new CstlServiceException(UNKNOW_TYPENAME + name, INVALID_PARAMETER_VALUE, "typenames");
                }
                if (!(layer.getData() instanceof FeatureData)) {
                    throw new CstlServiceException(UNKNOW_TYPENAME + name, INVALID_PARAMETER_VALUE, "typenames");
                }

                final FeatureData fLayer = (FeatureData) layer.getData();
                try {
                    FeatureSet featureset = fLayer.getOrigin();
                    FeatureType ftType = fLayer.getType();

                    if (featureset instanceof XmlFeatureSet) {
                        final Map params = ((XmlFeatureSet) featureset).getSchema();
                        if (params.size()==1 && params.get(params.keySet().iterator().next()) instanceof Schema) {
                            FeatureType renamed = NameOverride.wrap(ftType, layer.getName());
                            declaredSchema.put(renamed, (Schema) params.get(params.keySet().iterator().next()));
                            types.add(renamed);
                        } else {
                            locations.putAll(params);
                        }
                    } else {
                        types.add(NameOverride.wrap(ftType, layer.getName()));
                    }
                } catch (ConstellationStoreException | DataStoreException ex) {
                    LOGGER.log(Level.WARNING, "error while getting featureType for:{0}", layer.getName());
                }
            }
        }

        final int size = types.size();

        if (request.getOutputFormat().equals(MimeType.APP_JSON_SCHEMA)) {
            return new org.constellation.wfs.ws.rs.FeatureTypeList(types);
        }

        /*
         * Most simple case. we have only one feature type
         */
        final Schema schema;
        if (size == 1) {
            final FeatureType type = types.get(0);
            final String tn        = type.getName().tip().toString();
            final String tnmsp     = NamesExt.getNamespace(type.getName());
            if (declaredSchema.containsKey(type)) {
                schema = declaredSchema.get(type);
            } else {
                schema = writer.getSchemaFromFeatureType(type);
                final Set<String> nmsps = Utils.listAllNamespaces(type);
                nmsps.remove(tnmsp);
                nmsps.remove(Namespaces.GML);
                nmsps.remove("http://www.opengis.net/gml");
                for (String nmsp : nmsps) {
                    schema.addImport(new Import(nmsp, getServiceUrl() + "request=xsd&version=" + currentVersion + "&targetNamespace=" + nmsp + "&typename=ns:" + tn + "&namespace=xmlns(ns=" + tnmsp + ")"));
                }
            }
        /*
         * Second case. we have many feature type in the same namespace
         */
        } else if (AllInSameNamespace(types)) {

            //shortcut if all types have the same schema defined, return only this schema
            Schema commonSchema = null;
            for (FeatureType ft : types) {
                Schema sc = declaredSchema.get(ft);
                if (commonSchema == null) {
                    commonSchema = sc;
                } else if ( commonSchema != sc) {
                    commonSchema = null;
                    break;
                }
            }
            if (commonSchema != null) {
                return commonSchema;
            }


            schema = writer.getSchemaFromFeatureType(types);
            final Set<String> nmsps = new HashSet<>();
            for (FeatureType type : types) {
                nmsps.addAll(Utils.listAllNamespaces(type));
            }
            nmsps.remove(NamesExt.getNamespace(types.get(0).getName()));
            nmsps.remove(Namespaces.GML);
            nmsps.remove("http://www.opengis.net/gml");
            for (String nmsp : nmsps) {
                schema.addImport(new Import(nmsp, getServiceUrl() + "request=xsd&version=" + currentVersion + "&targetNamespace=" + nmsp));
            }

        /*
         * third case. we have many feature type in the many namespace.
         * send an xsd pointing on various describeFeatureRequest
         */
        } else {
            Map<String, List<FeatureType>> typeMap = splitByNamespace(types);
            if (typeMap.containsKey(null)) {
                final List<FeatureType> fts = typeMap.get(null);
                schema = writer.getSchemaFromFeatureType(fts);
                typeMap.remove(null);
            } else {
                schema = new Schema(FormChoice.QUALIFIED, null);
            }
            for (String nmsp : typeMap.keySet()) {
                final List<FeatureType> fts = typeMap.get(nmsp);
                StringBuilder sb = new StringBuilder();
                for (FeatureType ft : fts) {
                    sb.append("ns:").append(ft.getName().tip().toString()).append(',');
                }
                sb.delete(sb.length() -1, sb.length());
                final String schemaLocation = getServiceUrl() + "request=DescribeFeatureType&service=WFS&version=" + currentVersion + "&typename=" + sb.toString() + "&namespace=xmlns(ns=" + nmsp + ")";
                schema.addImport(new Import(nmsp, schemaLocation));
            }
        }

        for (Entry<String, String> location : locations.entrySet()) {
            schema.addImport(new Import(location.getKey(), location.getValue()));
        }

        LOGGER.log(Level.FINE, "DescribeFeatureType treated in {0}ms", (System.currentTimeMillis() - start));
        return schema;
    }

    private boolean AllInSameNamespace(final List<FeatureType> types) {
        if (types == null || types.isEmpty()) {
            return false;
        }

        final String firstNmsp = NamesExt.getNamespace(types.get(0).getName());
        for (int i = 1; i < types.size(); i++) {
            FeatureType type = types.get(i);
            final String currentNmsp = NamesExt.getNamespace(type.getName());
            // Objects.equals checks null pointers.
            if (!Objects.equals(firstNmsp, currentNmsp)) {
                return false;
            }
        }

        return true;
    }

    private Map<String, List<FeatureType>> splitByNamespace(final List<FeatureType> types) {
        Map<String, List<FeatureType>> results = new HashMap<>();
        for (FeatureType type : types) {
            final String nmsp = NamesExt.getNamespace(type.getName());
            if (results.containsKey(nmsp)) {
                results.get(nmsp).add(type);
            } else {
                final List<FeatureType> ft = new ArrayList<>();
                ft.add(type);
                results.put(nmsp, ft);
            }
        }
        return results;
    }

    @Override
    public Schema getXsd(final GetXSD request) throws CstlServiceException {
        final String userLogin = getUserLogin();

        final String gmlVersion;
        if ("2.0.0".equals(request.version)) {
            gmlVersion = "3.2.1";
        } else {
            gmlVersion = "3.1.1";
        }
        final JAXBFeatureTypeWriter writer  = new JAXBFeatureTypeWriter(gmlVersion);
        final List<FeatureType> types = new ArrayList<>();
        final String suffix;
        if (request.featureType == null) {
            //search all types
            for (final LayerCache layer : getLayerCaches(userLogin)) {
                if (!(layer.getData() instanceof FeatureData)) {continue;}
                final FeatureData data = (FeatureData) layer.getData();
                try {
                    types.add(NameOverride.wrap(data.getType(), layer.getName()));
                } catch (ConstellationStoreException ex) {
                    LOGGER.log(Level.WARNING, "error while getting featureType for:{0}", layer.getName());
                }
            }
            suffix = "";
        } else {
            final LayerCache layer;
            try {
                layer = getLayerCache(userLogin, request.featureType);
            } catch (CstlServiceException ex) {
                throw new CstlServiceException(UNKNOW_TYPENAME + request.featureType, INVALID_PARAMETER_VALUE, "typenames");
            }
            if(!(layer.getData() instanceof FeatureData)) {
                throw new CstlServiceException(UNKNOW_TYPENAME + request.featureType, INVALID_PARAMETER_VALUE, "typenames");
            }
            final FeatureData data = (FeatureData) layer.getData();

            try {
                types.add(NameOverride.wrap(data.getType(), layer.getName()));
            } catch (ConstellationStoreException ex) {
                LOGGER.log(Level.WARNING, "error while getting featureType for:"+ layer.getName(), ex);
            }
            suffix = "&typename=ns:" + request.featureType.getLocalPart() + "&namespace=xmlns(ns=" + request.featureType.getNamespaceURI() + ")";
        }
        Schema schema = writer.getExternalSchemaFromFeatureType(request.namespace, types);
        final Set<String> nmsps = new HashSet<>();
        for (FeatureType type : types) {
            nmsps.addAll(Utils.listAllSubNamespaces(type, request.namespace));
        }
        nmsps.remove(request.namespace);
        nmsps.remove(Namespaces.GML);
        nmsps.remove("http://www.opengis.net/gml");

        for (String nmsp : nmsps) {
            schema.addImport(new Import(nmsp, getServiceUrl() + "request=xsd&version=" + request.version + "&targetNamespace=" + nmsp + suffix));
        }
        return schema;

    }

    private LinkedHashMap<String,? extends Query> extractStoredQueries(final FeatureRequest request) throws CstlServiceException {
        final List<? extends Query> queries = request.getQuery();
        final LinkedHashMap<String,Query> result = new LinkedHashMap<>();
        for(int i=0,n=queries.size();i<n;i++){
            result.put(""+i, queries.get(i));
        }

        for (StoredQuery storedQuery : request.getStoredQuery()) {
            StoredQueryDescription description = null;
            final List<? extends Parameter> parameters = storedQuery.getParameter();
            for (StoredQueryDescription desc : storedQueries) {
                if (desc.getId().equals(storedQuery.getId())) {
                    description = desc;
                    break;
                }
            }
            if (description == null) {
                throw new CstlServiceException("Unknow stored query: " + storedQuery.getId(), INVALID_PARAMETER_VALUE, "storedQuery");
            } else {
                for (QueryExpressionText queryEx : description.getQueryExpressionText()) {
                    for (Object content : queryEx.getContent()) {
                        if (content instanceof JAXBElement jb) {
                            content = jb.getValue();
                        }
                        if (content instanceof Query q) {
                            final Query query = WFSXmlFactory.cloneQuery(q);
                            applyParameterOnQuery(query, parameters);
                            result.put(description.getId(), query);
                        } else {
                            throw new CstlServiceException("unexpected query object: " + content, INVALID_PARAMETER_VALUE, "storedQuery");
                        }
                    }
                }
            }
        }
        return result;
    }

    private List<String> extractPropertyNames(final List<Object> properties, Map<String, String> prefixMapping) {
        final List<String> requestPropNames = new ArrayList<>();
        for (Object obj : properties) {
            if (obj instanceof JAXBElement jb) {
                obj = jb.getValue();
            }
            if (obj instanceof String pName) {
                int pos = pName.indexOf(':');
                if (pos != -1) {
                    String prefix = pName.substring(0, pos);
                    prefix = correctGMLNamespace(prefixMapping.getOrDefault(prefix, prefix));
                    pName = prefix + pName.substring(pos);
                }
                requestPropNames.add(correctGMLNamespace(pName));
            } else if (obj instanceof PropertyName pName) {
                if (pName.getValue() != null) {
                    requestPropNames.add(correctGMLNamespace(Util.toFeaturePropertyFormat(pName.getValue())));
                }
            }
        }
        return requestPropNames;
    }

    private static String correctGMLNamespace(String propName) {
        // hack for GML 3.2 namespace
        if (propName.contains("http://www.opengis.net/gml/3.2") ||
            propName.contains("http://www.opengis.net/gml/3.2.1")) {
            propName = propName.replace("http://www.opengis.net/gml/3.2", "http://www.opengis.net/gml");
            propName = propName.replace("http://www.opengis.net/gml/3.2.1", "http://www.opengis.net/gml");
        }
        return propName;
    }

    private void putSchemaLocation(final QName typeName, final Map<String, String> schemaLocations, final String version) {
        final String namespace = typeName.getNamespaceURI();
        if (schemaLocations.containsKey(namespace)) {
            LOGGER.severe("TODO multiple typeName schemaLocation");

        } else {
            String prefix = null;// typeName.getPrefix(); issue here. can we add a prfix into GenericName?
            if (prefix == null || prefix.isEmpty()) {
                prefix = "ns1";
            }
            final String url    = getServiceUrl();
            if (url != null) {
                String describeRequest = url + "request=DescribeFeatureType&version=" + version + "&service=WFS";
                describeRequest        = describeRequest + "&namespace=xmlns(" + prefix + "=" + namespace + ")";
                final String tnParameter;
                if (version.equals("2.0.0")) {
                    tnParameter = "typenames";
                } else {
                    tnParameter = "typename";
                }
                describeRequest        = describeRequest + "&" + tnParameter + "=" + prefix + ':' + typeName.getLocalPart();
                schemaLocations.put(namespace, describeRequest);
            }
        }
    }

    private String[] verifyPropertyNames(final QName typeName, final FeatureType ft, final List<String> requestPropNames) throws CstlServiceException {

        if (!requestPropNames.isEmpty()) {

            final Set<GenericName> selected = new LinkedHashSet<>();
            //select mandatory properties
            for (PropertyType pdesc : ft.getProperties(true)) {
                final GenericName propName = pdesc.getName();
                if ((pdesc instanceof AttributeType && ((AttributeType)pdesc).getMinimumOccurs() > 0) ||
                   (AttributeConvention.IDENTIFIER_PROPERTY.equals(propName))) {
                    selected.add(propName);
                }

                //check requested properties
                for (int i=requestPropNames.size()-1;i>=0;i--) {
                    final String cdt = requestPropNames.get(i);
                    try {
                        if (ft.getProperty(cdt)==pdesc) {
                            selected.add(propName);
                            requestPropNames.remove(cdt);
                        }
                    } catch (PropertyNotFoundException ex) {
                        throw new CstlServiceException("The feature Type " + typeName + " has no such property:" + cdt, INVALID_PARAMETER_VALUE);
                    }
                }
            }

            final String[] array = new String[selected.size()];
            final Iterator<GenericName> ite = selected.iterator();
            for (int i=0;i<array.length;i++) array[i] = Util.toXPathFormat(ite.next());
            return array;

        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public FeatureSetWrapper getFeature(final GetFeature request) throws CstlServiceException {
        LOGGER.log(Level.FINE, "GetFeature request proccesing");
        final long start = System.currentTimeMillis();

        // we verify the base attribute
        verifyBaseRequest(request, false, false);

        Integer nbMatched                          = 0;
        Integer nbReturned                         = 0;
        final String userLogin                     = getUserLogin();
        final String currentVersion                = request.getVersion().toString();
        final int maxFeatures                      = request.getCount();
        final Integer startIndex                   = request.getStartIndex();
        final List<FeatureSet> collections         = new ArrayList<>();
        final Map<String, String> schemaLocations  = new HashMap<>();
        final Map<String, String> namespaceMapping = request.getPrefixMapping();

        if ((request.getQuery() == null || request.getQuery().isEmpty()) && (request.getStoredQuery() == null || request.getStoredQuery().isEmpty())) {
            throw new CstlServiceException("You must specify a query!", MISSING_PARAMETER_VALUE);
        }
        final Map<String, ? extends Query> queries = extractStoredQueries(request);

        final Map<QName, LayerCache> layers = getLayerCaches(userLogin).stream()
                .collect(
                        Collectors.toMap(
                                LayerCache::getName,
                                Function.identity(),
                                (LayerCache l1, LayerCache l2) -> {
                                    throw new IllegalStateException(
                                            String.format("There's 2 layers with the same name on service %s.%nFirst :%s%nSecond:%s", getId(), l1, l2)
                                    );
                                }
                        ));

        for (final Query query : queries.values()) {
            final Map<String, QName> aliases = new HashMap<>();
            final List<QName> typeNames = new ArrayList<>();
            if (isAllFeatureTypes(query.getTypeNames())) {
                typeNames.addAll(layers.keySet());
            } else {
                final List<QName> queryNames = query.getTypeNames();
                for (int i = 0; i < queryNames.size(); i++) {
                    final QName queryName = queryNames.get(i);
                    String namespace = queryNames.get(i).getNamespaceURI();
                    QName typeName = null;
                    if (layers.containsKey(queryName)) {
                        typeName = queryName;
                    } else if (namespace == null || namespace.trim().isEmpty()) {
                        for (final QName n : layers.keySet()) {
                            if (n.getLocalPart().equals(queryName.getLocalPart())) {
                                typeName = n;
                                break;
                            }
                        }
                    }

                    if (typeName == null) {
                        throw new CstlServiceException(UNKNOW_TYPENAME + queryName, INVALID_PARAMETER_VALUE, "typenames");
                    }

                    typeNames.add(typeName);
                    if (query.getAliases().size() > i) {
                        aliases.put(query.getAliases().get(i), typeName);
                    }
                }
            }

            //decode filter-----------------------------------------------------
            final Filter filter = transformJAXBFilter(query.getFilter(), Filter.include(), namespaceMapping, currentVersion);

            //decode crs--------------------------------------------------------
            final CoordinateReferenceSystem queryCRS = extractCRS(query.getSrsName());

            //decode property names---------------------------------------------
            final List<String> requestPropNames = extractPropertyNames(query.getPropertyNames(), namespaceMapping);

            //decode sort by----------------------------------------------------
            final List<SortProperty> sortBys = visitJaxbSortBy(query.getSortBy(), namespaceMapping, currentVersion);

            boolean singleCollectionRequested = typeNames.size() == 1;
            for (QName typeName : typeNames) {
                final LayerCache layer = layers.get(typeName);

                if (!(layer.getData() instanceof FeatureData)) {
                    LOGGER.log(Level.WARNING, "The requested layer is not a feature:{0}", layer.getName());
                    continue;
                }

                final FeatureData data = (FeatureData) layer.getData();

                final FeatureType ft;
                try {
                    ft = data.getType();
                } catch (ConstellationStoreException ex) {
                    throw new CstlServiceException(ex);
                }

                final FeatureSet origin = data.getOrigin();
                final FeatureQuery subquery = new FeatureQuery();
                if (!sortBys.isEmpty()) {
                    subquery.setSortBy(sortBys.toArray(SortProperty[]::new));
                }
                final Filter cleanFilter = processFilter(ft, filter, aliases);
                subquery.setSelection(cleanFilter);

                // we ensure that the property names are contained in the feature type and add the mandatory attribute to the list
                String[] properties = verifyPropertyNames(typeName, ft, requestPropNames);
                if (properties != null) {
                    // here we can't use subquery.setProjection(String[]) because the filterFactory of sis does not support Xpath
                    final FilterFactory ff = FilterUtilities.FF;
                    FeatureQuery.NamedExpression[] columns = new FeatureQuery.NamedExpression[properties.length];
                    for (int i=0; i<properties.length; i++) {
                        final String t = properties[i];
                        columns[i] = new FeatureQuery.NamedExpression(ff.property(t));
                    }
                    subquery.setProjection(columns);
                 }

                // look for matching count before pagination
                try {
                    Long colMatch = FeatureStoreUtilities.getCount(origin.subset(subquery));
                    nbMatched = nbMatched + colMatch.intValue();
                } catch (DataStoreException ex) {
                    throw new CstlServiceException(ex);
                }

                if (startIndex != 0) {
                    subquery.setOffset(startIndex);
                }
                if (request.getResultType() == ResultTypeType.HITS) {
                    subquery.setLimit(0);
                } else if (maxFeatures != 0 ) {
                    subquery.setLimit(maxFeatures);
                }

                FeatureSet collection;
                try {
                    collection = origin.subset(subquery);
                    if (queryCRS != null) {
                        final FeatureQuery reproject = org.geotoolkit.storage.feature.query.Query.reproject(collection.getType(), queryCRS);
                        collection = collection.subset(reproject);
                    }
                } catch (DataStoreException ex) {
                    throw new CstlServiceException(ex);
                }


                // we verify that all the properties contained in the filter are known by the feature type.
                verifyFilterProperty(NameOverride.wrap(ft, typeName), cleanFilter, aliases);

                Long colSize;
                try {
                    colSize = FeatureStoreUtilities.getCount(collection);
                    nbReturned = nbReturned + colSize.intValue();
                } catch (DataStoreException ex) {
                    throw new CstlServiceException(ex);
                }

                if (colSize > 0 || singleCollectionRequested) {
                    if (queryCRS == null) {
                        try {
                            //ensure axes are in the declared order, since we use urn epsg, we must comply
                            //to proper epsg axis order
                            final String defaultCRS = getCRSCode(ft);
                            final CoordinateReferenceSystem rcrs = CRS.forCode(defaultCRS);
                            final CoordinateReferenceSystem dataCrs = FeatureExt.getCRS(ft);
                            if (!Utilities.equalsIgnoreMetadata(rcrs, dataCrs)) {
                                final FeatureQuery reproject = org.geotoolkit.storage.feature.query.Query.reproject(collection.getType(), CRS.forCode(defaultCRS));
                                collection = collection.subset(reproject);
                            }
                        } catch (FactoryException|PropertyNotFoundException|IllegalStateException|DataStoreException ex) {
                            // If we cannot extract coordinate system information, we send back brut data.
                            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                        }
                    }

                    // Ensure exposed name is compliant with service capabilities
                    try {
                        if (!NamesExt.create(typeName).equals(ft.getName()) || !collection.getIdentifier().isPresent()) {
                            try {
                                //TODO : test cases expect the collection with identifier 'id', we should change this behavior
                                collection = NameOverride.wrap(collection, typeName, NamesExt.create("id"));
                            } catch (DataStoreException ex) {
                                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                            }
                        }
                    } catch (DataStoreException ex) {
                        // If we cannot extract coordinate system information, we send back brut data.
                        LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                    }

                    collections.add(collection);
                    // we write The SchemaLocation
                    putSchemaLocation(typeName, schemaLocations, currentVersion);
                }
            }
        }
        final String gmlVersion;
        if (TEXT_GML31_XML.equals(request.getOutputFormat()) ||
            "text/gml; subtype=\"gml/3.1.1\"".equals(request.getOutputFormat())) {
            gmlVersion = "3.1.1";
        } else if ("text/xml; subtype=\"gml/3.2.1\"".equals(request.getOutputFormat()) ||
                   "text/xml; subtype=\"gml/3.2\"".equals(request.getOutputFormat())   ||
                   APP_GML32_XML.equals(request.getOutputFormat())) {
            gmlVersion = "3.2.1";
        } else if (MimeType.APP_JSON.equals(request.getOutputFormat())) {
            gmlVersion = null;
        } else {
            throw new CstlServiceException("invalid outputFormat:" + request.getOutputFormat(), INVALID_PARAMETER_VALUE, OUTPUT_FORMAT);
        }


        /**
         * 3 possibilities here :
         *    1) return a collection of collection.
         *    2) return an empty collection
         *    3) if there is only one feature we return (change the return type in object)
         *
         * result TODO find an id and a member type
         */
        if (collections.isEmpty()) {
            collections.add(FeatureStoreUtilities.collection("collection-1", null));
        }
        if (request.getResultType() == ResultTypeType.HITS && "1.1.0".equals(currentVersion)) {
            nbReturned =  nbMatched;
            nbMatched = null;
        }
        LOGGER.log(Level.FINE, "GetFeature treated in {0}ms", (System.currentTimeMillis() - start));

        boolean singleFeature = queries.size() == 1 && queries.containsKey("urn:ogc:def:query:OGC-WFS::GetFeatureById");
        return new FeatureSetWrapper(collections, schemaLocations, gmlVersion, currentVersion, nbMatched, nbReturned, singleFeature);
    }

    private boolean isAllFeatureTypes(List<QName> typeNames) {
        if (typeNames.isEmpty()) {
            return true;
        } else if (typeNames.size() == 1 && "http://www.opengis.net/gml/3.2".equals(typeNames.get(0).getNamespaceURI())
                                         && "AbstractFeatureType".equals(typeNames.get(0).getLocalPart())) {
            return true;

        }
        return false;
    }

    @Override
    public FeatureSetWrapper getPropertyValue(final GetPropertyValue request) throws CstlServiceException {
        LOGGER.log(Level.FINE, "GetPropertyValue request processing\n");
        final long startTime = System.currentTimeMillis();
        verifyBaseRequest(request, true, false);

        final String valueReference                = request.getValueReference();
        if (valueReference == null) {
            throw new CstlServiceException("ValueReference must be specified", MISSING_PARAMETER_VALUE, "valueReference");
        } else if (valueReference.isEmpty()) {
            throw new CstlServiceException("ValueReference must not be empty", INVALID_PARAMETER_VALUE, "valueReference");
        }

        Integer nbMatched                          = 0;
        Integer nbReturned                         = 0;
        final String userLogin                     = getUserLogin();
        final Map<String, String> namespaceMapping = request.getPrefixMapping();
        final String currentVersion                = request.getVersion().toString();
        final Collection<? extends Query> queries  = extractStoredQueries(request).values();
        final Integer maxFeatures                  = request.getCount();
        final Integer startIndex                   = request.getStartIndex();
        final Map<String, String> schemaLocations  = new HashMap<>();
        final List<FeatureSet> collections  = new ArrayList<>();

        for (final Query query : queries) {

            final List<QName> typeNames;
            final Map<String, QName> aliases = new HashMap<>();
            if (query.getTypeNames().isEmpty()) {
                typeNames = getTypeNames(userLogin);
            } else {
                typeNames = query.getTypeNames();
                if (!query.getAliases().isEmpty()) {
                    for (int i = 0; i < typeNames.size() && i < query.getAliases().size(); i++) {
                        aliases.put(query.getAliases().get(i), typeNames.get(i));
                    }
                }
            }

            //decode filter-----------------------------------------------------
            final Filter filter = transformJAXBFilter(query.getFilter(), Filter.include(), namespaceMapping, currentVersion);

            //decode crs--------------------------------------------------------
            final CoordinateReferenceSystem crs = extractCRS(query.getSrsName());

            //decode property names---------------------------------------------
            final List<String> requestPropNames = extractPropertyNames(query.getPropertyNames(), namespaceMapping);

            //decode sort by----------------------------------------------------
            final List<SortProperty> sortBys = visitJaxbSortBy(query.getSortBy(), namespaceMapping, currentVersion);

            for (QName typeName : typeNames) {

                final LayerCache layer;
                try {
                    layer = getLayerCache(userLogin, typeName);
                } catch (CstlServiceException ex) {
                    throw new CstlServiceException(UNKNOW_TYPENAME + typeName, INVALID_PARAMETER_VALUE, "typenames");
                }
                if (!(layer.getData() instanceof FeatureData)) {continue;}

                final FeatureData data = (FeatureData) layer.getData();
                final FeatureSet origin = data.getOrigin();

                final FeatureType ft;
                try {
                    ft = data.getType();
                } catch (ConstellationStoreException ex) {
                    throw new CstlServiceException(ex);
                }
                final Filter cleanFilter = processFilter(ft, filter, aliases);

                final FeatureQuery subquery = new FeatureQuery();
                subquery.setSelection(cleanFilter);

                // look for matching count before pagination
                try {
                    Long colMatch = FeatureStoreUtilities.getCount(origin.subset(subquery));
                    nbMatched = nbMatched + colMatch.intValue();
                } catch (DataStoreException ex) {
                    throw new CstlServiceException(ex);
                }

                if (startIndex != 0) {
                    subquery.setOffset(startIndex);
                }
                if (request.getResultType() == ResultTypeType.HITS) {
                    subquery.setLimit(0);
                } else if (maxFeatures != 0 ) {
                    subquery.setLimit(maxFeatures);
                }

                if (!sortBys.isEmpty()) {
                    subquery.setSortBy(sortBys.toArray(SortProperty[]::new));
                }

                // we ensure that the property names are contained in the feature type and add the mandatory attribute to the list
                String[] properties = verifyPropertyNames(typeName, ft, requestPropNames);
                if (properties != null) {
                    // here we can't use subquery.setProjection because the filterFactory of sis does not support Xpath
                    final FilterFactory ff = FilterUtilities.FF;
                    FeatureQuery.NamedExpression[] columns = new FeatureQuery.NamedExpression[properties.length];
                    for (int i=0; i<properties.length; i++) {
                        final String t = properties[i];
                        columns[i] = new FeatureQuery.NamedExpression(ff.property(t));
                    }
                    subquery.setProjection(columns);
                 }

                // we verify that all the properties contained in the filter are known by the feature type.
                verifyFilterProperty(NameOverride.wrap(ft, typeName), cleanFilter, aliases);

                try {
                    FeatureSet col = origin.subset(subquery);

                    Long colSize = FeatureStoreUtilities.getCount(col);
                    nbReturned = nbReturned + colSize.intValue();

                    if (crs != null) {
                        col = col.subset(org.geotoolkit.storage.feature.query.Query.reproject(col.getType(), crs));
                    }

                    col = NameOverride.wrap(col, typeName, NamesExt.create("id"));
                    collections.add(col);
                } catch (DataStoreException ex) {
                    throw new CstlServiceException(ex.getMessage(), ex);
                }

                // we write The SchemaLocation
                putSchemaLocation(typeName, schemaLocations, currentVersion);
            }
        }
        /*
         * 3 possibility here :
         *    1) merge the collections
         *    2) return a collection of collection.
         *    3) if there is only one feature we return (change the return type in object)
         *
         * result TODO find an id and a member type
         */
        final FeatureSet featureCollection;
        if (collections.size() > 1) {
            try {
                featureCollection = ConcatenatedFeatureSet.create(collections);
            } catch (DataStoreException ex) {
                throw new CstlServiceException(ex.getMessage(), ex);
            }
        } else if (collections.size() == 1) {
            featureCollection = collections.get(0);
        } else {
            featureCollection = FeatureStoreUtilities.collection("collection-1", null);
        }

        LOGGER.log(Level.FINE, "GetPropertyValue request processed in {0} ms", (System.currentTimeMillis() - startTime));
        return new FeatureSetWrapper(featureCollection, request.getValueReference(), nbMatched, nbReturned, "3.2.1");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public AbstractGML getGMLObject(final GetGmlObject grbi) throws CstlServiceException {
        throw new CstlServiceException("WFS get GML Object is not supported on this Constellation version.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public LockFeatureResponse lockFeature(final LockFeature gr) throws CstlServiceException {
        throw new CstlServiceException("WFS Lock is not supported on this Constellation version.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public TransactionResponse transaction(final Transaction request) throws CstlServiceException {
        LOGGER.log(Level.FINE, "Transaction request processing\n");
        final long startTime = System.currentTimeMillis();
        assertTransactionnal("Transaction");
        verifyBaseRequest(request, true, false);

        // we prepare the report
        final String userLogin                      = getUserLogin();
        final String currentVersion                 = request.getVersion().toString();
        int totalInserted                           = 0;
        int totalUpdated                            = 0;
        int totalDeleted                            = 0;
        int totalReplaced                           = 0;
        final List<Object> transactions             = request.getTransactionAction();
        final Map<String, String> inserted          = new TreeMap<>(); //have a consistant order, necessary for tests, TODO it should not be required
        final Map<String, String> replaced          = new TreeMap<>(); //have a consistant order, necessary for tests, TODO it should not be required
        final Map<String, String> namespaceMapping  = request.getPrefixMapping();
        final JAXPStreamFeatureReader featureReader = new JAXPStreamFeatureReader(getFeatureTypes(userLogin));
        featureReader.getProperties().put(JAXPStreamFeatureReader.BINDING_PACKAGE, "GML");

        for (Object transaction: transactions) {
            /*
             * Features insertion.
             */
            if (transaction instanceof InsertElement insertRequest) {

                final String handle = insertRequest.getHandle();

                // we verify the input format
                if (insertRequest.getInputFormat() != null && !(insertRequest.getInputFormat().equals(TEXT_GML31_XML)
                                                           ||   insertRequest.getInputFormat().equals(APP_GML32_XML))) {
                    throw new CstlServiceException("This only input format supported are: text/xml; subtype=\"gml/3.1.1\" and " + APP_GML32_XML,
                            INVALID_PARAMETER_VALUE, "inputFormat");
                }

                // what to do with the CRS ?
                final CoordinateReferenceSystem insertCRS = extractCRS(insertRequest.getSrsName());

                // what to do with that, which ones are supported ??
                final IdentifierGenerationOptionType idGen = insertRequest.getIdgen();

                for (Object featureObject : insertRequest.getFeature()) {
                    if (featureObject instanceof JAXBElement) {
                        featureObject = ((JAXBElement)featureObject).getValue();
                    }

                    FeatureType ft = null;
                    try {
                        if (featureObject instanceof Node) {
                            featureObject = featureReader.read(featureObject);
                        } else if (featureObject instanceof FeatureCollectionType) {
                            final FeatureCollectionType xmlCollection = (FeatureCollectionType) featureObject;
                            final String id = xmlCollection.getId();
                            final List<Feature> features = new ArrayList<>();
                            for (FeaturePropertyType fprop : xmlCollection.getFeatureMember()) {
                                Feature feat = (Feature)featureReader.read(fprop.getUnknowFeature());
                                ft = feat.getType();
                                features.add(feat);
                            }
                            featureObject = features;
                        }
                    } catch (IllegalArgumentException ex) {
                        throw new CstlServiceException(ex.getMessage(), ex, INVALID_VALUE);
                    } catch (IOException | XMLStreamException ex) {
                        throw new CstlServiceException(ex);
                    }
                    Collection<Feature> featureCollection;

                    if (featureObject instanceof Feature feature) {
                        ft = feature.getType();
                        featureCollection = Arrays.asList(feature);
                    } else if (featureObject instanceof List ls) {
                        featureCollection = ls;
                    } else if (featureObject instanceof FeatureSet fs) {
                        try {
                            featureCollection = fs.features(false).collect(Collectors.toList());
                            ft = ((FeatureSet) featureObject).getType();
                        } catch (DataStoreException ex) {
                            throw new CstlServiceException(ex);
                        }

                    } else {
                        final String featureType;
                        if (featureObject == null) {
                            featureType = "null";
                        } else {
                            if (featureObject instanceof JAXBElement jb) {
                                featureType = "JAXBElement<" + jb.getValue().getClass().getName() + ">";
                            } else {
                                featureType = featureObject.getClass().getName();
                            }
                        }
                        throw new CstlServiceException("Unexpected Object to insert:" + featureType);
                    }
                    final QName typeName = Utils.getQnameFromName(ft.getName());
                    final LayerCache layer;
                    try {
                        layer = getLayerCache(userLogin, typeName);
                    } catch (Exception ex) {
                        throw new CstlServiceException(UNKNOW_TYPENAME + typeName);
                    }
                    final FeatureData data = (FeatureData) layer.getData();
                    try {
                        final FeatureType type = data.getType();
                        final CoordinateReferenceSystem trueCrs = FeatureExt.getCRS(type);
                        if (trueCrs != null && !Utilities.equalsIgnoreMetadata(trueCrs, FeatureExt.getCRS(ft))) {
                            final FeatureSet collection = new InMemoryFeatureSet(type, featureCollection);
                            final FeatureQuery reproject = org.geotoolkit.storage.feature.query.Query.reproject(collection.getType(), trueCrs);
                            featureCollection = collection.subset(reproject).features(false).collect(Collectors.toList());
                        }

                        DataStore store = data.getStore();
                        if (store instanceof FeatureStore) {
                            final List<ResourceId> features = ((FeatureStore)store).addFeatures(typeName.toString(), featureCollection);

                            for (ResourceId fid : features) {
                                inserted.put(fid.getIdentifier(), handle);// get the id of the inserted feature
                                totalInserted++;
                                LOGGER.log(Level.FINER, "fid inserted: {0} total:{1}", new Object[]{fid, totalInserted});
                            }
                        } else {
                            FeatureSet origin = data.getOrigin();
                            if (origin instanceof WritableFeatureSet wOrigin) {

                                //todo we do not have the created ids, use a listener, not 100% safe but better then nothing
                                final AtomicInteger acc = new AtomicInteger();
                                final StoreListener<FeatureStoreContentEvent> listener = new StoreListener<>() {
                                    @Override
                                    public void eventOccured(FeatureStoreContentEvent event) {
                                        if (event.getType() == FeatureStoreContentEvent.Type.ADD) {
                                            Set<ResourceId> identifiers = new HashSet<>();
                                            separate(event.getIds(), identifiers);
                                            for (ResourceId id : identifiers) {
                                                inserted.put(id.getIdentifier(), handle);
                                            }
                                            acc.addAndGet(identifiers.size());
                                        }
                                    }
                                };
                                origin.addListener(FeatureStoreContentEvent.class, listener);
                                wOrigin.add(featureCollection.iterator());
                                origin.removeListener(FeatureStoreContentEvent.class, listener);
                                totalInserted += acc.get();
                            } else {
                                throw new CstlServiceException("The specified FeatureSet does not suport the write operations.");
                            }
                        }
                    } catch (ConstellationStoreException | DataStoreException ex) {
                        Logging.unexpectedException(LOGGER,DefaultWFSWorker.class,"transaction", ex);
                    } catch (ClassCastException ex) {
                        Logging.unexpectedException(LOGGER,DefaultWFSWorker.class,"transaction", ex);
                        throw new CstlServiceException("The specified Datastore does not suport the write operations.");
                    }
                }

            /*
             * Features remove.
             */
            } else if (transaction instanceof DeleteElement deleteRequest) {

                //decode filter-----------------------------------------------------
                if (deleteRequest.getFilter() == null) {
                    throw new CstlServiceException("The filter must be specified.", MISSING_PARAMETER_VALUE, "filter");
                }
                final Filter filter = transformJAXBFilter(deleteRequest.getFilter(), Filter.exclude(), namespaceMapping, currentVersion);

                final LayerCache layer;
                try {
                    layer = getLayerCache(userLogin, deleteRequest.getTypeName());
                } catch (CstlServiceException ex) {
                    throw new CstlServiceException(UNKNOW_TYPENAME + deleteRequest.getTypeName(), INVALID_PARAMETER_VALUE, "typename");
                }
                final FeatureData data = (FeatureData) layer.getData();
                try {
                    final FeatureType ft = data.getType();
                    final Filter cleanFilter = processFilter(ft, filter, null);
                    FeatureSet fs = (FeatureSet) data.getOrigin();

                    // we verify that all the properties contained in the filter are known by the feature type.
                    verifyFilterProperty(NameOverride.wrap(ft, layer.getName()), cleanFilter, null);

                    // we extract the number of feature deleted
                    final FeatureQuery query = new FeatureQuery();
                    query.setSelection(cleanFilter);

                    totalDeleted = totalDeleted + (int) FeatureStoreUtilities.getCount(fs.subset(query)).intValue();

                    if (fs instanceof WritableFeatureSet wfs) {
                        wfs.removeIf((f)-> filter.test(f));
                    } else {
                        throw new CstlServiceException("This feature set is not Writable");
                    }
                } catch (ConstellationStoreException | DataStoreException ex) {
                    throw new CstlServiceException(ex);
                } catch (ClassCastException ex) {
                    Logging.unexpectedException(LOGGER,DefaultWFSWorker.class,"transaction", ex);
                    throw new CstlServiceException("The specified Datastore does not suport the delete operations.");
                }

            /*
             * Features updates.
             */
            } else if (transaction instanceof UpdateElement updateRequest) {

                // we verify the input format
                if (updateRequest.getInputFormat() != null && !(updateRequest.getInputFormat().equals(TEXT_GML31_XML)
                                                           ||   updateRequest.getInputFormat().equals(APP_GML32_XML))) {
                    throw new CstlServiceException("This only input format supported are: text/xml; subtype=\"gml/3.1.1\" and " + APP_GML32_XML,
                            INVALID_PARAMETER_VALUE, "inputFormat");
                }

                //decode filter-----------------------------------------------------
                final Filter filter = transformJAXBFilter(updateRequest.getFilter(),Filter.exclude(), namespaceMapping, currentVersion);

                //decode crs--------------------------------------------------------
                final CoordinateReferenceSystem crs = extractCRS(updateRequest.getSrsName());

                final LayerCache layer;
                try {
                    layer = getLayerCache(userLogin, updateRequest.getTypeName());
                } catch (CstlServiceException ex) {
                    throw new CstlServiceException(UNKNOW_TYPENAME + updateRequest.getTypeName(), INVALID_PARAMETER_VALUE, "typename");
                }
                final FeatureData data = (FeatureData) layer.getData();
                try {
                    final FeatureType ft = data.getType();
                    if (ft == null) {
                        throw new CstlServiceException("Unable to find the featuretype:" + layer.getName());
                    }
                    final FeatureSet fs = (FeatureSet) data.getOrigin();

                    final Map<String,Object> values = new HashMap<>();

                    // we verify that the update property are contained in the feature type
                    for (final Property updateProperty : updateRequest.getProperty()) {
                        String updatePropertyName = updateProperty.getLocalName();
                        Binding pa = Bindings.getBinding(FeatureType.class, updatePropertyName);
                        if (pa == null || pa.get(ft, updatePropertyName, null) == null) {
                            throw new CstlServiceException("The feature Type " + updateRequest.getTypeName() + " has no such property: " + updatePropertyName, INVALID_VALUE);
                        }
                        PropertyType propertyType = (PropertyType) pa.get(ft, updatePropertyName, null);
                        if (propertyType instanceof FeatureAssociationRole && updateProperty.getValue() != null) {
                            FeatureAssociationRole ct = (FeatureAssociationRole) propertyType;
                            try {
                                ct.getValueType().getProperty("_value");
                                updatePropertyName = "/" + updatePropertyName + "/_value";
                                propertyType = ct.getValueType().getProperty("_value");
                            } catch(PropertyNotFoundException ex) {
                                //do nothing
                            }
                        }

                        Object value;
                        if (updateProperty.getValue() instanceof Element elem) {
                            final String strValue = getXMLFromElementNSImpl(elem);
                            value = null;
                            LOGGER.log(Level.FINER, ">> updating : {0}   => {1}", new Object[]{updatePropertyName, strValue});
                        } else {
                            value = updateProperty.getValue();
                            if (value instanceof AbstractGeometryType geom) {
                                try {
                                    final String defaultCRS = getCRSCode(ft);
                                    final CoordinateReferenceSystem exposedCrs = CRS.forCode(defaultCRS);
                                    final CoordinateReferenceSystem trueCrs = FeatureExt.getCRS(propertyType);

                                    value = GeometrytoJTS.toJTS(geom);
                                    if(trueCrs != null && !Utilities.equalsIgnoreMetadata(exposedCrs, trueCrs)){
                                        value = JTS.transform((Geometry)value, CRS.findOperation(exposedCrs, trueCrs, null).getMathTransform());
                                    }

                                } catch (TransformException | FactoryException ex) {
                                    Logging.unexpectedException(LOGGER,DefaultWFSWorker.class,"transaction", ex);
                                } catch (IllegalArgumentException ex) {
                                    throw new CstlServiceException(ex);
                                }
                            } else if (value instanceof DirectPosition dp) {
                                value = new GeometryFactory().createPoint(new Coordinate(dp.getCoordinate(0), dp.getCoordinate(1)));
                            } else if (value instanceof String sval) {
                                value = JAXPStreamFeatureReader.readValue(sval, (AttributeType) propertyType);
                            }
                            LOGGER.log(Level.FINER, ">> updating : {0} => {1}", new Object[]{updatePropertyName, value});
                            if (value != null) {
                                LOGGER.log(Level.FINER, "type : {0}", value.getClass());
                            }
                        }
                        values.put(updatePropertyName, value);

                    }

                    final Filter cleanFilter = processFilter(ft, filter, null);
                    // we verify that all the properties contained in the filter are known by the feature type.
                    verifyFilterProperty(NameOverride.wrap(ft, layer.getName()), cleanFilter, null);

                    // we extract the number of feature update
                    final FeatureQuery query = new FeatureQuery();
                    query.setSelection(cleanFilter);
                    totalUpdated = totalUpdated + (int) FeatureStoreUtilities.getCount(fs.subset(query)).intValue();

                    final FeatureSet origin = data.getOrigin();
                    if (origin instanceof WritableFeatureSet wfs) {
                        wfs.replaceIf(filter, new UnaryOperator<Feature>() {
                            @Override
                            public Feature apply(Feature feat) {
                                for (Entry<String, Object> entry : values.entrySet()) {
                                    Binding pa = Bindings.getBinding(Feature.class, entry.getKey());
                                    pa.set(feat, entry.getKey(), entry.getValue());
                                }
                                return feat;
                            }
                        });
                    }
                } catch (ConstellationStoreException | DataStoreException ex) {
                    throw new CstlServiceException(ex);
                }


            } else if (transaction instanceof ReplaceElement replaceRequest) {

                final String handle = replaceRequest.getHandle();

                // we verify the input format
                if (replaceRequest.getInputFormat() != null && !(replaceRequest.getInputFormat().equals(TEXT_GML31_XML)
                                                            ||   replaceRequest.getInputFormat().equals(APP_GML32_XML))) {
                    throw new CstlServiceException("This only input format supported are: " + TEXT_GML31_XML + " and " + APP_GML32_XML,
                            INVALID_PARAMETER_VALUE, "inputFormat");
                }

                //decode filter-----------------------------------------------------
                final Filter filter = transformJAXBFilter(replaceRequest.getFilter(),Filter.exclude(), namespaceMapping, currentVersion);

                //decode crs--------------------------------------------------------
                final CoordinateReferenceSystem crs = extractCRS(replaceRequest.getSrsName());

                // extract replacement feature
                Object featureObject = replaceRequest.getFeature();
                if (featureObject instanceof JAXBElement jb) {
                    featureObject = jb.getValue();
                }
                try {
                    if (featureObject instanceof Node) {

                        featureObject = featureReader.read(featureObject);

                    } else if (featureObject instanceof FeatureCollectionType xmlCollection) {
                        final String collId = xmlCollection.getId();
                        final List<Feature> features = new ArrayList<>();
                        FeatureType ft = null;
                        for (FeaturePropertyType fprop : xmlCollection.getFeatureMember()) {
                            Feature feat = (Feature) featureReader.read(fprop.getUnknowFeature());
                            ft = feat.getType();
                            features.add(feat);
                        }
                        featureObject = new InMemoryFeatureSet(NamesExt.create(collId), ft, features, true);
                    }
                } catch (IllegalArgumentException ex) {
                    throw new CstlServiceException(ex.getMessage(), ex, INVALID_PARAMETER_VALUE);
                } catch (IOException | XMLStreamException ex) {
                    throw new CstlServiceException(ex);
                }
                final GenericName typeName;
                FeatureSet featureCollection;

                if (featureObject instanceof Feature feature) {
                    final FeatureType fType = feature.getType();
                    typeName = fType.getName();
                    featureCollection = new InMemoryFeatureSet(fType, List.of(feature));
                } else if (featureObject instanceof FeatureSet fs) {
                    try {
                        typeName = fs.getType().getName();
                        featureCollection = (FeatureSet) featureObject;
                    } catch (DataStoreException ex) {
                        throw new CstlServiceException(ex);
                    }
                } else {
                    final String featureType;
                    if (featureObject == null) {
                        featureType = "null";
                    } else {
                        if (featureObject instanceof JAXBElement jb) {
                            featureType = "JAXBElement<" + jb.getValue().getClass().getName() + ">";
                        } else {
                            featureType = featureObject.getClass().getName();
                        }
                    }
                    throw new CstlServiceException("Unexpected replacement object:" + featureType);
                }

                final LayerCache layer;
                try {
                    layer = getLayerCache(userLogin, Utils.getQnameFromName(typeName));
                } catch (CstlServiceException ex) {
                    throw new CstlServiceException(UNKNOW_TYPENAME + typeName);
                }

                try {
                    final FeatureData data = (FeatureData) layer.getData();
                    final FeatureType ft    = data.getType();
                    final WritableFeatureSet fs = (WritableFeatureSet) data.getOrigin();
                    final String layerName  = data.getName().toString();

                    // we extract the number of feature to replace
                    final FeatureQuery query = new FeatureQuery();
                    query.setSelection(processFilter(ft, filter, null));
                    totalReplaced = totalReplaced + (int) FeatureStoreUtilities.getCount(fs.subset(query)).intValue();

                    // first remove the feature to replace
                    fs.removeIf(filter::test);

                    // then add the new one
                    final CoordinateReferenceSystem trueCrs = FeatureExt.getCRS(ft);
                    if (trueCrs != null && !Utilities.equalsIgnoreMetadata(trueCrs, FeatureExt.getCRS(featureCollection.getType()))) {
                        final FeatureQuery reproject = org.geotoolkit.storage.feature.query.Query.reproject(featureCollection.getType(), trueCrs);
                        featureCollection = featureCollection.subset(reproject);
                    }

                    DataStore store = data.getStore();
                    if (store instanceof FeatureStore fst) {
                        try (Stream<Feature> stream = featureCollection.features(false)) {
                            List<Feature> collected = stream.collect(Collectors.toList());
                            final List<ResourceId> features = fst.addFeatures(layerName, collected);

                            for (ResourceId fid : features) {
                                replaced.put(fid.getIdentifier(), handle);// get the id of the replaced feature
                                LOGGER.log(Level.FINER, "fid inserted: {0} total:{1}", new Object[]{fid, totalInserted});
                            }
                        }
                    } else {

                        //todo we do not have the created ids, use a listener, not 100% safe but better then nothing
                        final AtomicInteger acc = new AtomicInteger();
                        final StoreListener<FeatureStoreContentEvent> listener = new StoreListener<>() {
                            @Override
                            public void eventOccured(FeatureStoreContentEvent event) {
                                if (event.getType() == FeatureStoreContentEvent.Type.ADD) {
                                    Set<ResourceId> identifiers = new HashSet<>();
                                    separate(event.getIds(), identifiers);
                                    for (ResourceId id : identifiers) {
                                        replaced.put(id.getIdentifier(), handle);// get the id of the replaced feature
                                    }
                                    acc.addAndGet(identifiers.size());
                                }
                            }
                        };
                        fs.addListener(FeatureStoreContentEvent.class, listener);
                        try (Stream<Feature> stream = featureCollection.features(false)) {
                            fs.add(stream.iterator());
                        }
                        fs.removeListener(FeatureStoreContentEvent.class, listener);
                    }

                } catch (ConstellationStoreException | DataStoreException | FeatureStoreRuntimeException ex) {
                    throw new CstlServiceException(ex);
                }

            } else {
                String className = " null object";
                if (transaction != null) {
                    className = transaction.getClass().getName();
                }
                throw new CstlServiceException("This kind of transaction is not supported by the service: " + className,
                                              INVALID_PARAMETER_VALUE, "transaction");
            }

        }

        final TransactionResponse response = buildTransactionResponse(currentVersion,
                                                                      totalInserted,
                                                                      totalUpdated,
                                                                      totalDeleted,
                                                                      totalReplaced,
                                                                      inserted,
                                                                      replaced);
        LOGGER.log(Level.FINE, "Transaction request processed in {0} ms", (System.currentTimeMillis() - startTime));

        return response;
    }

    private static void separate(final Filter f, Set<ResourceId> addTo) {
        if (f.getOperatorType() == LogicalOperatorName.OR) {
            for (final Filter c : ((LogicalOperator<?>) f).getOperands()) {
                separate(c, addTo);
            }
        } else {
            addTo.add((ResourceId) f);
        }
    }

    /**
     * Extract the a XML string from a W3C Element.
     *
     * @param elt An W3c Xml Element.
     *
     * @return a string containing the xml representation.
     */
    private  String getXMLFromElementNSImpl(final Element elt) {
        final StringBuilder s = new StringBuilder();
        s.append('<').append(elt.getLocalName()).append('>');
        final Node node = elt.getFirstChild();
        s.append(getXMLFromNode(node));

        s.append("</").append(elt.getLocalName()).append('>');
        return s.toString();
    }

    /**
     * Extract the a XML string from a W3C node.
     *
     * @param node An W3c Xml node.
     *
     * @return a string builder containing the xml.
     */
    private  StringBuilder getXMLFromNode(final Node node) {
        final StringBuilder temp = new StringBuilder();
        if (!node.getNodeName().equals("#text")){
            temp.append("<").append(node.getNodeName());
            final NamedNodeMap attrs = node.getAttributes();
            for(int i=0;i<attrs.getLength();i++){
                temp.append(" ").append(attrs.item(i).getNodeName()).append("=\"").append(attrs.item(i).getTextContent()).append("\" ");
            }
            temp.append(">");
        }
        if (node.hasChildNodes()) {
            final NodeList nodes = node.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                temp.append(getXMLFromNode(nodes.item(i)));
            }
        }
        else{
            temp.append(node.getTextContent());
        }
        if (!node.getNodeName().equals("#text")) {temp.append("</").append(node.getNodeName()).append(">");}
        return temp;
    }

    /**
     * Return a coordinate reference system from an identifier.
     *
     * @param srsName a CRS identifier.
     * @return
     * @throws CstlServiceException
     */
    private CoordinateReferenceSystem extractCRS(final String srsName) throws CstlServiceException {
        final CoordinateReferenceSystem crs;
        if (srsName != null) {
            try {
                crs = CRS.forCode(srsName);
                //todo use other properties to filter properly
            } catch (NoSuchAuthorityCodeException ex) {
                throw new CstlServiceException(ex, INVALID_PARAMETER_VALUE, "srsName");
            } catch (FactoryException ex) {
                throw new CstlServiceException(ex, INVALID_PARAMETER_VALUE, "srsName");
            }
        } else {
            crs = null;
        }
        return crs;
    }

    /**
     * Verify that all the property contained in the filter are known by the featureType
     *
     * @param ft A featureType.
     * @param filter An OGC filter.
     *
     * @throws CstlServiceException if one of the propertyName in the filter is not present in the featureType.
     */
    private void verifyFilterProperty(final FeatureType ft, final Filter filter, final Map<String, QName> aliases) throws CstlServiceException {
        final Collection<String> filterProperties = new ArrayList<>();
        ListingPropertyVisitor.VISITOR.visit(filter, filterProperties);
        for (String filterProperty : filterProperties) {

            if (filterProperty.startsWith("@")){
                //this property in an id property, we won't find it in the feature type
                //but it always exist on the features
                continue;
            }

            // look to remove featureType prefix
            String ftName = "";
            if (NamesExt.getNamespace(ft.getName()) != null) {
                ftName = "{" + NamesExt.getNamespace(ft.getName()) + "}";
            }
            ftName = ftName + ft.getName().tip().toString();
            if (filterProperty.startsWith(ftName)) {
                filterProperty = filterProperty.substring(ftName.length());
            }
            if (aliases != null) {
                for (String entry : aliases.keySet()) {
                    if (filterProperty.startsWith(entry + "/")) {
                        filterProperty =  filterProperty.substring(entry.length());
                    }
                }
            }
            // remove the XPath QName format
            filterProperty = filterProperty.replace("Q{", "{");

            final Binding pa = Bindings.getBinding(FeatureType.class, filterProperty);
            if (pa == null || pa.get(ft, filterProperty, null) == null) {
                String s = "";
                if (NamesExt.getNamespace(ft.getName()) != null) {
                    s = "{" + NamesExt.getNamespace(ft.getName()) + "}";
                }
                s = s + ft.getName().tip().toString();
                throw new CstlServiceException("The feature Type " + s + " has no such property: " + filterProperty, INVALID_PARAMETER_VALUE, "filter");
            }
        }

        if (!new IsValidSpatialFilterVisitor(ft).visit(filter)) {
            throw new CstlServiceException("The filter try to apply spatial operators on non-spatial property", INVALID_PARAMETER_VALUE, "filter");
        }
    }

    /**
     * Ensure crs is set on all geometric elements and with correct crs.
     * replace Aliases by correct feature type names.
     * remove feature type name prefixing propertyName.
     */
    private Filter processFilter(final FeatureType ft, Filter filter, final Map<String, QName> aliases) {
        try {
            if (aliases!= null && !aliases.isEmpty()) {
                filter = (Filter) new AliasFilterVisitor(aliases)   .visit(filter);
            }
            filter = (Filter) new UnprefixerFilterVisitor(ft)   .visit(filter);
            filter = (Filter) new DefaultGeomPropertyVisitor(ft).visit(filter);
            filter = (Filter) new GMLNamespaceVisitor()         .visit(filter);
            filter = (Filter) new LiteralCorrectionVisitor(ft)  .visit(filter);

            final String defaultCRS = getCRSCode(ft);
            final CoordinateReferenceSystem exposedCrs = CRS.forCode(defaultCRS);
            final CoordinateReferenceSystem trueCrs = getCRS(ft);
            if (exposedCrs != null && trueCrs != null && !Utilities.equalsIgnoreMetadata(trueCrs, exposedCrs)) {
                filter = (Filter) new FillCrsVisitor(exposedCrs).visit(filter);
                filter = (Filter) new CrsAdjustFilterVisitor(exposedCrs, trueCrs).visit(filter);
            }

        } catch (FactoryException|PropertyNotFoundException|IllegalStateException ex) {
            /* In case we cannot analyze CRS (no geometric property, or multiple
             * ones, or if a problem occurs with referencing engine), we simply
             * ignore the filter.
             */
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
        }
        return filter;
    }

    /**
     * Extract the WGS84 BBOx from a featureSource.
     * what ? may not be wgs84 exactly ? why is there a CRS attribute on a wgs84 bbox ?
     */
    private static Object toBBox(final LayerCache layer, final String version) throws CstlServiceException {
        try {
            final CoordinateReferenceSystem epsg4326 = CRS.forCode("urn:ogc:def:crs:OGC:2:84");
            Envelope env = layer.getEnvelope(epsg4326);
            if (env != null) {
                return buildBBOX(version,
                       "urn:ogc:def:crs:OGC:2:84",
                       env.getMinimum(0),
                       env.getMinimum(1),
                       env.getMaximum(0),
                       env.getMaximum(1));
            }
        } catch (ConstellationStoreException | FactoryException ex) {
            throw new CstlServiceException(ex);
        }
        // return default full BBOX
        return buildBBOX(version,"urn:ogc:def:crs:OGC:2:84", -180, -90, 180, 90);
    }

    private static void applyParameterOnQuery(final Query query, final List<? extends Parameter> parameters) throws CstlServiceException {
        applyParameterOnFilter(query.getFilter(), parameters);
        final List<QName> toRemove = new ArrayList<>();
        final List<QName> toAdd    = new ArrayList<>();
        if (query.getTypeNames().isEmpty()) {
            for (Parameter param : parameters) {
                if (!param.getContent().isEmpty() && param.getContent().get(0) instanceof QName && param.getName().equalsIgnoreCase("typeName")) {
                    toAdd.add((QName)param.getContent().get(0));
                }
            }
        } else {
            for (QName q : query.getTypeNames()) {
                for (Parameter param : parameters) {
                    if (q.getLocalPart().contains("$" +  param.getName())) {
                        toRemove.add(q);
                        if (!param.getContent().isEmpty() && param.getContent().get(0) instanceof QName) {
                            toAdd.add((QName)param.getContent().get(0));
                        } else {
                            LOGGER.warning("bad type or empty parameter content");
                        }
                    }
                }
            }
        }
        query.getTypeNames().removeAll(toRemove);
        query.getTypeNames().addAll(toAdd);
    }

    private static void applyParameterOnFilter(final Filter filter, final List<? extends Parameter> parameters) throws CstlServiceException {
        final Object filterObject;
        if (filter instanceof XMLFilter xmf) {
            filterObject = xmf.getFilterObject();
        } else {
            filterObject = filter;
        }

        if (filterObject instanceof BBOXType bb) {
           if (bb.getAny() != null && bb.getAny() instanceof String s) {
               for (Parameter param : parameters) {
                   if (s.contains("${" + param.getName() + '}')) {
                       bb.setAny(param.getContent().get(0));
                   }
               }
           }

       } else if (filterObject instanceof BinarySpatialOperator binary) {
           if (binary.getOperand2() != null && binary.getOperand2() instanceof XMLLiteral lit) {
               if (lit.getValue() instanceof String s) {
                   for (Parameter param : parameters) {
                       if (s.contains("${" + param.getName() + '}')) {
                           s = s.replace("${" + param.getName()+ '}', (String)param.getContent().get(0));
                       }
                   }
                   lit.getContent().clear();
                   lit.setContent(s);
               }
           }

       } else if (filterObject instanceof BinaryComparisonOperator binary) {
           if (binary.getOperand2() != null && binary.getOperand2() instanceof XMLLiteral lit) {
               if (lit.getValue() instanceof String s) {
                   for (Parameter param : parameters) {
                       if (s.contains("${"  + param.getName()+ '}')) {
                           s = s.replace("${"  + param.getName()+ '}', (String)param.getContent().get(0));
                       }
                   }
                   lit.getContent().clear();
                   lit.setContent(s);
               }
           }

       } else if (filterObject instanceof LogicalOperator) {
           final LogicalOperator<Object> binary = (LogicalOperator) filterObject;
           for (Filter child : binary.getOperands()) {
               applyParameterOnFilter(child, parameters);
           }
       } else  if (filterObject != null) {
           throw new CstlServiceException("Unimplemented filter implementation:" + filterObject.getClass().getName(), NO_APPLICABLE_CODE);
       }
    }

    /**
     * Verify that the bases request attributes are correct.
     *
     * @param request an object request with the base attribute (all except GetCapabilities request);
     */
    private void verifyBaseRequest(final RequestBase request, final boolean versionMandatory, final boolean getCapabilities) throws CstlServiceException {
        isWorking();
        if (request != null) {
            if (request.getService() != null) {
                if (request.getService().isEmpty()) {
                  // we let pass (CITE test)
                } else if (!request.getService().equalsIgnoreCase("WFS"))  {
                    throw new CstlServiceException("service must be \"WFS\"!",
                                                  INVALID_PARAMETER_VALUE, "service");
                }
            } else {
                throw new CstlServiceException("service must be specified!",
                                              MISSING_PARAMETER_VALUE, "service");
            }
            if (request.getVersion() != null) {
                if (isSupportedVersion(request.getVersion().toString())) {
                    request.setVersion(request.getVersion().toString());

                // for the CITE test
                } else if (request.getVersion().toString().isEmpty()) {
                    request.setVersion(ServiceDef.WFS_1_1_0.version.toString());

                } else {
                    final CodeList code;
                    if (getCapabilities) {
                        code = VERSION_NEGOTIATION_FAILED;
                    } else {
                        code = INVALID_PARAMETER_VALUE;
                    }
                    throw new CstlServiceException("version must be \"1.1.0\" or \"2.0.0\"!", code, "version");
                }
            } else {
                if (versionMandatory) {
                    throw new CstlServiceException("version must be specified!", MISSING_PARAMETER_VALUE, "version");
                } else {
                    request.setVersion(ServiceDef.WFS_1_1_0.version.toString());
                }
            }
         } else {
            throw new CstlServiceException("The request is null!", NO_APPLICABLE_CODE);
         }
    }

    private List<FeatureType> getFeatureTypes(final String userLogin) throws CstlServiceException {
        final List<FeatureType> types = new ArrayList<>();

        //search all types
        for (final LayerCache layer : getLayerCaches(userLogin)) {
            if (!(layer.getData() instanceof FeatureData)) {continue;}
            final FeatureData data = (FeatureData) layer.getData();
            try {
                //fix feature type to define the exposed crs : true EPSG axis order
                final FeatureType baseType = NameOverride.wrap(data.getType(), layer.getName());
                final String crsCode = getCRSCode(baseType);
                final CoordinateReferenceSystem exposedCrs = CRS.forCode(crsCode);
                final FeatureType exposedType = FeatureTypeExt.createSubType(baseType, null, exposedCrs);
                types.add(exposedType);
            } catch (Exception ex) {
                LOGGER.severe("DataStore exception while getting featureType");
            }
        }
        return types;
    }

    @Override
    public ListStoredQueriesResponse listStoredQueries(final ListStoredQueries request) throws CstlServiceException {
        LOGGER.log(Level.FINE, "ListStoredQueries request processing\n");
        final long startTime = System.currentTimeMillis();
        verifyBaseRequest(request, true, false);

        final String currentVersion = request.getVersion().toString();

        final ListStoredQueriesResponse response = buildListStoredQueriesResponse(currentVersion, storedQueries);
        LOGGER.log(Level.FINE, "ListStoredQueries request processed in {0} ms", (System.currentTimeMillis() - startTime));
        return response;

    }

    @Override
    public DescribeStoredQueriesResponse describeStoredQueries(final DescribeStoredQueries request) throws CstlServiceException {
        LOGGER.log(Level.FINE, "DescribeStoredQueries request processing\n");
        final long startTime = System.currentTimeMillis();
        verifyBaseRequest(request, true, false);
        final String currentVersion = request.getVersion().toString();

        final List<StoredQueryDescription> storedQueryList;
        if (request.getStoredQueryId() != null && !request.getStoredQueryId().isEmpty()) {
            storedQueryList = new ArrayList<>();
            for (String sqId : request.getStoredQueryId()) {
                StoredQueryDescription description = getStoredQueryById(sqId);
                if (description != null) {
                    storedQueryList.add(description);
                }
            }
        } else {
            storedQueryList = storedQueries;
        }
        final DescribeStoredQueriesResponse response = buildDescribeStoredQueriesResponse(currentVersion, storedQueryList);
        LOGGER.log(Level.FINE, "DescribeStoredQueries request processed in {0} ms", (System.currentTimeMillis() - startTime));
        return response;
    }

    private StoredQueryDescription getStoredQueryById(String id) {
        for (StoredQueryDescription description : storedQueries) {
            if (description.getId().equals(id)) {
                return description;
            }
        }
        return null;
    }

    @Override
    public CreateStoredQueryResponse createStoredQuery(final CreateStoredQuery request) throws CstlServiceException {
        LOGGER.log(Level.FINE, "CreateStoredQuery request processing\n");
        final long startTime = System.currentTimeMillis();
        verifyBaseRequest(request, true, false);
        final String currentVersion  = request.getVersion().toString();

        for (StoredQueryDescription query : request.getStoredQueryDefinition()) {
            if (getStoredQueryById(query.getId()) != null) {
                throw new CstlServiceException("Stored query:" + query.getId() + " already exist",
                            DUPLICATE_STORED_QUERY_ID_VALUE, query.getId());
            }
            for (QueryExpressionText qet :query.getQueryExpressionText()) {
                if (!"urn:ogc:def:queryLanguage:OGC-WFS::WFS_QueryExpression".equals(qet.getLanguage()) &&
                    !"urn:ogc:def:queryLanguage:OGC-WFS::WFSQueryExpression".equals(qet.getLanguage())) { // error in CITE test
                    throw new CstlServiceException("Invalid language query. Accepted values are:{urn:ogc:def:queryLanguage:OGC-WFS::WFS_QueryExpression, urn:ogc:def:queryLanguage:OGC-WFS::WFSQueryExpression}",
                            INVALID_PARAMETER_VALUE, "language");
                }
            }
        }

        storedQueries.addAll(request.getStoredQueryDefinition());
        storedQueries();

        final CreateStoredQueryResponse response = buildCreateStoredQueryResponse(currentVersion, "OK");
        LOGGER.log(Level.FINE, "CreateStoredQuery request processed in {0} ms", (System.currentTimeMillis() - startTime));
        return response;
    }

    @Override
    public DropStoredQueryResponse dropStoredQuery(final DropStoredQuery request) throws CstlServiceException {
        LOGGER.log(Level.FINE, "dropStoredQuery request processing\n");
        final long startTime = System.currentTimeMillis();
        verifyBaseRequest(request, true, false);
        final String currentVersion  = request.getVersion().toString();

        StoredQueryDescription candidate = null;
        for (StoredQueryDescription sq : storedQueries) {
            if (sq.getId().equals(request.getId())) {
                candidate = sq;
            }
        }
        if (candidate == null) {
            throw new CstlServiceException("Unexisting Stored query: " + request.getId(), INVALID_PARAMETER_VALUE, "id");
        } else  {
            storedQueries.remove(candidate);
        }
        storedQueries();

        final DropStoredQueryResponse response = buildDropStoredQueryResponse(currentVersion, "OK");
        LOGGER.log(Level.FINE, "dropStoredQuery request processed in {0} ms", (System.currentTimeMillis() - startTime));
        return response;
    }

    @Override
    public List<ParameterExpression> getParameterForStoredQuery(final String queryId) {
        final List<ParameterExpression> results = new ArrayList<>();
        for (StoredQueryDescription description : storedQueries) {
            if (description.getId().equals(queryId)) {
                results.addAll(description.getParameter());
            }
        }
        return results;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public WFSCapabilities getCapabilities(String version) throws CstlServiceException {
       return getCapabilities(WFSXmlFactory.buildGetCapabilities(version, "WFS"));
    }

    @Override
    public List<org.geotoolkit.feature.xml.Collection> getCollections(List<String> names) throws CstlServiceException {
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

    private org.geotoolkit.feature.xml.Collection dataToCollection(LayerCache layer) {
        final Data data = layer.getData();
        final List<Link> links = new ArrayList<>();
        final Extent extent = new Extent();

        try {
            final DefaultGeographicBoundingBox gbox = new DefaultGeographicBoundingBox();
            gbox.setBounds(data.getEnvelope());
            BoundingBox box = new BoundingBox();
            box.setMinx(gbox.getWestBoundLongitude());
            box.setMiny(gbox.getSouthBoundLatitude());
            box.setMaxx(gbox.getEastBoundLongitude());
            box.setMaxy(gbox.getNorthBoundLatitude());
            extent.getSpatial().addBox(box);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Cannot set spatial extent of data " + data.getName(), ex);
            extent.setSpatial(null);
        }

        try {
            SortedSet<Date> dates = data.getDateRange();
            if (dates != null) {
                extent.getTemporal().addInterval(dates.first());
                extent.getTemporal().addInterval(dates.last());
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
        String url    = wfsUrl.replace("/wfs", "/feature").replace("?", "");
        BuildItemsLink(url, identifier, title, links);
        // add schema decription
        buildDescribedByLink(wfsUrl, links, identifier);
        // String itemType = "VECTOR" set item type to null for now as it does not appears in official xsd at http://schemas.opengis.net/ogcapi/features/part1/1.0/xml/core.xsd
        // but it appears at https://github.com/opengeospatial/ogcapi-features/blob/master/core/xml/core.xsd
        return new org.geotoolkit.feature.xml.Collection(identifier, title, null, links, extent, null);
    }

    @Override
    public FeatureSetWrapper getCollectionItems(String collectionId, Filter filter, int limit, int offset, boolean includedMatched) throws CstlServiceException {
        try {
            final String userLogin = getUserLogin();
            final LayerCache layer = getLayerCache(userLogin, Util.parseQName(collectionId));
            final FeatureData data = (FeatureData) layer.getData();
            FeatureSet fs          = data.getOrigin();
            FeatureQuery query     = null;
            /*
            * always reproject to CRS:84 if not already
            */
            final CoordinateReferenceSystem crs = FeatureExt.getCRS(fs.getType());
            if (!Utilities.equalsApproximately(crs, CommonCRS.defaultGeographic())) {
                query = org.geotoolkit.storage.feature.query.Query.reproject(fs.getType(), CommonCRS.defaultGeographic());
            }

            /*
            * Apply filters
            */
            if (filter != null) {
                filter = processFilter(fs.getType(), filter, new HashMap<>());
                if (query == null) query = new FeatureQuery();
                query.setSelection(filter);
            }
            if (query != null) {
                fs = fs.subset(query);
            }
            Integer nbMatched =  includedMatched ? FeatureStoreUtilities.getCount(fs).intValue() : null;

           /*
            * Apply paging
            */
            final FeatureQuery reduced = new FeatureQuery();
            reduced.setLimit(limit);
            reduced.setOffset(offset);
            fs = fs.subset(reduced);

            Integer nbReturned = includedMatched ? FeatureStoreUtilities.getCount(fs).intValue() : null;

            return new FeatureSetWrapper(fs, new ArrayList<>(), new HashMap<>(), nbMatched, nbReturned);
        } catch (DataStoreException ex) {
            throw new CstlServiceException(ex);
        }
    }
}
