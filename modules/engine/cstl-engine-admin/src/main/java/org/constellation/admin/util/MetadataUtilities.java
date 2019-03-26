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
 * limitations under the License..
 */

package org.constellation.admin.util;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.constellation.engine.template.TemplateEngine;
import org.constellation.engine.template.TemplateEngineException;
import org.constellation.engine.template.TemplateEngineFactory;
import org.constellation.provider.DataProvider;
import org.constellation.util.Util;
import org.geotoolkit.coverage.io.GridCoverageReader;
import org.geotoolkit.process.ProcessException;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.GenericName;
import org.opengis.util.NoSuchIdentifierException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.internal.metadata.Merger;
import org.apache.sis.metadata.ModifiableMetadata;
import org.apache.sis.metadata.iso.citation.DefaultOnlineResource;
import org.apache.sis.metadata.iso.distribution.DefaultDigitalTransferOptions;
import org.apache.sis.metadata.iso.distribution.DefaultDistribution;
import org.apache.sis.metadata.iso.spatial.DefaultGeometricObjects;
import org.apache.sis.metadata.iso.spatial.DefaultVectorSpatialRepresentation;
import org.apache.sis.storage.DataSet;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.Resource;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.constellation.dto.metadata.MetadataBbox;
import org.geotoolkit.storage.coverage.CoverageResource;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.metadata.identification.Identification;
import org.apache.sis.util.logging.Logging;
import org.constellation.dto.CstlUser;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.metadata.utils.Utils;
import org.constellation.util.StoreUtilities;
import org.geotoolkit.data.FeatureStoreUtilities;
import static org.geotoolkit.feature.FeatureExt.IS_NOT_CONVENTION;
import org.geotoolkit.referencing.ReferencingUtilities;
import org.geotoolkit.storage.DataStores;
import org.geotoolkit.temporal.object.TemporalUtilities;
import org.opengis.feature.AttributeType;
import org.opengis.feature.Operation;
import org.opengis.feature.PropertyNotFoundException;
import org.opengis.feature.PropertyType;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.distribution.DigitalTransferOptions;
import org.opengis.metadata.distribution.Distribution;
import org.opengis.metadata.spatial.GeometricObjectType;


/**
 * Utility class to do some operation on metadata file (generate, revover, ...)
 *
 * @author bgarcia
 * @version 0.9
 * @since 0.9
 */
public final class MetadataUtilities {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.admin.util");

    private static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");

    public static DefaultMetadata getRasterMetadata(final DataProvider dataProvider, final GenericName dataName) throws DataStoreException {

    	final DataStore dataStore = dataProvider.getMainStore();
    	final Resource ref = StoreUtilities.findResource(dataStore, dataName.toString());
        if (ref instanceof CoverageResource) {
            final CoverageResource cref = (CoverageResource) ref;
            final GridCoverageReader coverageReader = (GridCoverageReader) cref.acquireReader();
            try {
                return (DefaultMetadata) coverageReader.getMetadata();
            } finally {
                cref.recycle(coverageReader);
            }
        }
        return null;
    }

    /**
     * Returns the raster metadata for entire dataset referenced by given provider.
     * @param dataProvider the given data provider
     * @return {@code DefaultMetadata}
     * @throws DataStoreException
     */
    public static DefaultMetadata getRasterMetadata(final DataProvider dataProvider) throws DataStoreException {

        final DataStore dataStore = dataProvider.getMainStore();
        DefaultMetadata coverageMetadata =  (DefaultMetadata) dataStore.getMetadata();
        if (coverageMetadata != null) return coverageMetadata;

        //if the coverage metadata still null that means it is not implemented yet
        // so we return the metadata iso from the reader
        DefaultMetadata metadata = new DefaultMetadata();
        for (Resource resource : DataStores.flatten(dataStore, true)) {
            if (resource instanceof CoverageResource) {
                final CoverageResource cr = (CoverageResource) resource;
                final GridCoverageReader reader = (GridCoverageReader) cr.acquireReader();
                try {
                    final Metadata meta = reader.getMetadata();
                    //@FIXME
                    // this merge is bad here to build a fully dataset
                    // metadata that should contains all data children information
                    //see issue JIRA CSTL-1151
                    metadata = mergeMetadata(metadata,(DefaultMetadata)meta);
                }catch(Exception ex){
                    LOGGER.log(Level.WARNING,ex.getLocalizedMessage(),ex);
                } finally{
                    cr.recycle(reader);
                }
            }
        }
        return metadata;
    }

     /**
     * Returns crs name if possible for a provider.
     * @param dataProvider
     * @return
     * @throws DataStoreException
     */
    public static String getProviderCRSName(final DataProvider dataProvider) throws DataStoreException {
        final DataStore dataStore = dataProvider.getMainStore();
        CoordinateReferenceSystem candidat = null;

        for (Resource resource : DataStores.flatten(dataStore, true)) {
            if (resource instanceof DataSet) {
                try {
                    final DataSet cr = (DataSet) resource;
                    Envelope env = FeatureStoreUtilities.getEnvelope(cr);
                    if (env != null) {
                        final CoordinateReferenceSystem crs = env.getCoordinateReferenceSystem();
                        if (candidat == null && crs != null){
                            candidat = crs;
                        }
                        final String crsIdentifier = ReferencingUtilities.lookupIdentifier(crs,true);
                        if (crsIdentifier != null) {
                            return crsIdentifier;
                        }
                    }
                } catch(Exception ex) {
                    LOGGER.finer(ex.getMessage());
                }
            }
        }

        if (candidat != null && candidat.getName() != null) {
            return candidat.getName().toString();
        }
        return null;
    }

    /**
     * Returns crs name if possible for resource.
     */
    public static String getResourceCRSName(final DataProvider dataProvider, final GenericName name) throws DataStoreException {
        final DataStore dataStore = dataProvider.getMainStore();
        final Resource resource = StoreUtilities.findResource(dataStore, name.toString());
        if (resource instanceof DataSet) {
            try {
                final DataSet cr = (DataSet) resource;
                Envelope env = FeatureStoreUtilities.getEnvelope(cr);
                if (env != null) {
                    final CoordinateReferenceSystem crs = env.getCoordinateReferenceSystem();
                    if (crs != null) {
                        final String crsIdentifier = ReferencingUtilities.lookupIdentifier(crs, true);
                        if (crsIdentifier != null) {
                            return crsIdentifier;
                        }
                    }
                }
            } catch(Exception ex) {
                LOGGER.finer(ex.getMessage());
            }
        }
        return null;
    }

    public static DefaultMetadata getVectorMetadata(final DataProvider dataProvider, final GenericName dataName) throws DataStoreException, TransformException {

    	final DataStore dataStore = dataProvider.getMainStore();
        final Resource ft = StoreUtilities.findResource(dataStore, dataName.toString());

        final DefaultMetadata md = new DefaultMetadata();
        final DefaultDataIdentification ident = new DefaultDataIdentification();
        md.getIdentificationInfo().add(ident);

        if (ft instanceof DataSet) {
            DataSet ds = (DataSet) ft;

            // envelope extraction
            Envelope env = FeatureStoreUtilities.getEnvelope(ds);
            if (env != null) {
                env = Envelopes.transform(env, CommonCRS.WGS84.normalizedGeographic());
                final DefaultGeographicBoundingBox bbox = new DefaultGeographicBoundingBox(
                        env.getMinimum(0), env.getMaximum(0), env.getMinimum(1), env.getMaximum(1)
                );
                final DefaultExtent extent = new DefaultExtent("", bbox, null, null);
                ident.getExtents().add(extent);
            }

            // geometry type extraction
            if (ft instanceof FeatureSet) {
                FeatureSet fs = (FeatureSet) ft;
                try {
                    final List<? extends PropertyType> geometries = fs.getType().getProperties(true).stream()
                                    .filter(IS_NOT_CONVENTION)
                                    .filter(AttributeConvention::isGeometryAttribute)
                                    .collect(Collectors.toList());
                    for (PropertyType geometry : geometries) {
                        final GeometricObjectType geomType = getGeomTypeFromJTS(geometry);
                        if (geomType != null) {
                            DefaultVectorSpatialRepresentation sr = new DefaultVectorSpatialRepresentation();
                            sr.getGeometricObjects().add(new DefaultGeometricObjects(geomType));
                            md.getSpatialRepresentationInfo().add(sr);
                        }
                    }
                } catch (PropertyNotFoundException ex) {
                    LOGGER.log(Level.WARNING, "No default Geometry in vector data:{0}", dataName);
                }
            }
        }
        return md;
    }

    public static DefaultMetadata getVectorMetadata(final DataProvider dataProvider) throws DataStoreException, TransformException {

    	final DataStore dataStore = dataProvider.getMainStore();
        final DefaultMetadata md = new DefaultMetadata();
        final DefaultDataIdentification ident = new DefaultDataIdentification();
        md.getIdentificationInfo().add(ident);
        DefaultGeographicBoundingBox bbox = null;

        for (Resource resource : DataStores.flatten(dataStore, true)) {
            if (resource instanceof DataSet) {
                DataSet ds = (DataSet) resource;
                Envelope env = FeatureStoreUtilities.getEnvelope(ds);
                if (env == null) {
                    continue;
                }
                final DefaultGeographicBoundingBox databbox = new DefaultGeographicBoundingBox();
                databbox.setBounds(env);
                if (bbox == null) {
                    bbox = databbox;
                } else {
                    bbox.add(databbox);
                }
            }
        }
        final DefaultExtent extent = new DefaultExtent("", bbox, null, null);
        ident.getExtents().add(extent);
        return md;
    }

    private static GeometricObjectType getGeomTypeFromJTS(PropertyType defaultGeometry) {
        if (defaultGeometry != null) {
            while (defaultGeometry instanceof Operation) {
                defaultGeometry = (PropertyType) ((Operation) defaultGeometry).getResult();
            }
            Class binding = ((AttributeType)defaultGeometry).getValueClass();
            if (Point.class.equals(binding)) {
                return GeometricObjectType.POINT;
            } else if (LineString.class.equals(binding)) {
                return GeometricObjectType.CURVE;
            } else if (Polygon.class.equals(binding)) {
                return GeometricObjectType.SURFACE;
            } else if (GeometryCollection.class.equals(binding) ||
                       MultiLineString.class.equals(binding) ||
                       MultiPoint.class.equals(binding) ||
                       MultiPolygon.class.equals(binding)) {
                return GeometricObjectType.COMPLEX;
            } else if (Geometry.class.equals(binding)) {
                return GeometricObjectType.COMPLEX;
            } else if (binding != null) {
                LOGGER.info("Unexpected default geometry type:" + binding.getName());
            }
        }
        return null;
    }

    /**
     * @param fileMetadata
     * @param metadataToMerge
     *
     */
    public static DefaultMetadata mergeMetadata(final DefaultMetadata fileMetadata, final DefaultMetadata metadataToMerge) {
        final Metadata first = fileMetadata;
        final Metadata second = metadataToMerge;

        final DefaultMetadata merged = new DefaultMetadata(first);
        final Merger merger = new Merger(null) {
            @Override
            protected void merge(ModifiableMetadata target, String propertyName, Object sourceValue, Object targetValue) {
                // Ignore (TODO: we should probably emit some kind of warnings).
            }
        };
        merger.copy(second, merged);
        return merged;
    }

    public static String getTemplateMetadata(final Properties prop, final String templatePath) {
        try {
            final TemplateEngine templateEngine = TemplateEngineFactory.getInstance(TemplateEngineFactory.GROOVY_TEMPLATE_ENGINE);
            final InputStream stream = Util.getResourceAsStream(templatePath);
            return templateEngine.apply(stream, prop);
        } catch (TemplateEngineException ex) {
           LOGGER.log(Level.WARNING, null, ex);
        }
        return null;
    }

    public static Long extractDatestamp(final Object obj){
        if (obj instanceof DefaultMetadata) {
            final DefaultMetadata metadata = (DefaultMetadata) obj;
            if (metadata.getDateStamp() != null) {
                return metadata.getDateStamp().getTime();
            }
        }
        // generic way of finding resume
        return null;
    }

    public static String extractTitle(final Object obj){
        if (obj instanceof DefaultMetadata) {
            final DefaultMetadata metadata = (DefaultMetadata) obj;
            if (metadata.getIdentificationInfo() != null && !metadata.getIdentificationInfo().isEmpty()) {
                final Identification id = metadata.getIdentificationInfo().iterator().next();
                if (id.getCitation() != null && id.getCitation().getTitle() != null) {
                    return id.getCitation().getTitle().toString();
                }
            }
        } else {
            return Utils.findTitle(obj);
        }
        return null;
    }

    public static String extractResume(final Object obj){
        if (obj instanceof DefaultMetadata) {
            final DefaultMetadata metadata = (DefaultMetadata) obj;
            if (metadata.getIdentificationInfo() != null && !metadata.getIdentificationInfo().isEmpty()) {
                final Identification id = metadata.getIdentificationInfo().iterator().next();
                if (id.getAbstract() != null) {
                    return id.getAbstract().toString();
                }
            }
        }
        // generic way of finding resume
        return null;
    }

    public static List<MetadataBbox> extractBbox(final Object obj){
        final List<MetadataBbox> results = new ArrayList<>();
        if (obj instanceof DefaultMetadata) {
           final DefaultMetadata metadata = (DefaultMetadata) obj;
           if (metadata.getIdentificationInfo() != null && !metadata.getIdentificationInfo().isEmpty()) {
               final Identification id = metadata.getIdentificationInfo().iterator().next();
               for (Extent ex : id.getExtents()) {
                   for (GeographicExtent geoEx : ex.getGeographicElements()) {
                       if (geoEx instanceof GeographicBoundingBox) {
                           GeographicBoundingBox geobox = (GeographicBoundingBox) geoEx;
                           final MetadataBbox bbox = new MetadataBbox(null, geobox.getEastBoundLongitude(),
                                                                            geobox.getWestBoundLongitude(),
                                                                            geobox.getNorthBoundLatitude(),
                                                                            geobox.getSouthBoundLatitude());
                           results.add(bbox);
                       }
                   }
               }
           }
        }
        return results;
    }

    public static String extractParent(final Object obj){
        if (obj instanceof DefaultMetadata) {
            final DefaultMetadata metadata = (DefaultMetadata) obj;
            return metadata.getParentIdentifier();
        }
        return null;
    }

    public static String fillMetadataFromProperties(final String dataType, final String metadataID, final String title, final String crsName,
            final Optional<CstlUser> optUser, final List<String> keywords) {
        final Properties prop = ConfigDirectory.getMetadataTemplateProperties();
        prop.put("fileId", metadataID);
        prop.put("dataTitle", title);
        prop.put("dataAbstract", "");
        final String dateIso = TemporalUtilities.toISO8601Z(new Date(), TimeZone.getTimeZone("UTC"));
        prop.put("isoCreationDate", dateIso);
        prop.put("creationDate", dateIso);
        if ("raster".equalsIgnoreCase(dataType)) {
            prop.put("dataType", "grid");
        } else if("vector".equalsIgnoreCase(dataType)) {
            prop.put("dataType", "vector");
        }

        if (crsName != null) {
            prop.put("srs", crsName);
        }

        if (optUser!=null && optUser.isPresent()) {
            final CstlUser user = optUser.get();
            if (user != null) {
                prop.put("contactName", user.getFirstname()+" "+user.getLastname());
                prop.put("contactEmail", user.getEmail());
            }
        }
        if (keywords != null && !keywords.isEmpty()) {
            prop.put("keywords",keywords);
        }

        return MetadataUtilities.getTemplateMetadata(prop, "org/constellation/engine/template/mdTemplDataset.xml");
    }

    private static final Map<String, String> PROTOCOL_MAP = new HashMap<>();
    static {
        PROTOCOL_MAP.put("wcs",  "OGC :WCS :-1.0.0-http-get-coverage");
        PROTOCOL_MAP.put("wms",  "OGC :WMS :-1.1.1-http-get-map");
        PROTOCOL_MAP.put("wmts", "OGC:WMTS-1.0.0-http-get-tile");
    }

    private static final Map<String, String> DESCRIPTION_MAP = new HashMap<>();
    static {
        DESCRIPTION_MAP.put("wcs",  "download");
        DESCRIPTION_MAP.put("wms",  "vizualisation");
        DESCRIPTION_MAP.put("wmts", "vizualisation");
    }

    /**
     * Return true if the specified metadata contain a distribution block with the protocol correspounding to the service specification
     * and an url ending with the specified service instance if not {@code null}.
     *
     * @param metadataObj The metadata object to inspect.
     * @param spec Service specification (WCS, WMS or WMTS)
     * @param serviceInstance service identifier or {@code null}.
     *
     * @return
     */
    public static boolean hasServiceLink(final Object metadataObj, final String spec, final String serviceInstance) {
        String protocol = PROTOCOL_MAP.get(spec);
        if (protocol == null) {
            throw new IllegalArgumentException("Unsupported value for spec parameter:" + spec);
        }
        if (metadataObj instanceof Metadata) {
            final Metadata metadata = (Metadata) metadataObj;
            if (metadata.getDistributionInfo() == null || metadata.getDistributionInfo().isEmpty()) {
               return false;
            } else {
                final Distribution distributionInfoI = metadata.getDistributionInfo().iterator().next();
                final DefaultDistribution distributionInfo = (DefaultDistribution) distributionInfoI;
                for (DigitalTransferOptions dt : distributionInfo.getTransferOptions()) {
                    for (OnlineResource or : dt.getOnLines()) {
                        if (or.getProtocol().equals(protocol) &&
                            (serviceInstance == null || (or.getLinkage() != null && or.getLinkage().toString().endsWith('/' + serviceInstance)))) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

     /**
     * Remove the distribution/transferOptions block with the protocol correspounding to the service specification.
     *
     * @param metadataObj The metadata object to modifiy.
     * @param spec Service specification (WCS, WMS or WMTS).
     * @param serviceInstance service identifier or {@code null}.
     *
     */
    public static void removeServiceLink(final Object metadataObj, final String spec, final String serviceInstance) {
        String protocol = PROTOCOL_MAP.get(spec);
        if (protocol == null) {
            throw new IllegalArgumentException("Unsupported value for spec parameter:" + spec);
        }
        if (metadataObj instanceof Metadata) {
            final Metadata metadata = (Metadata) metadataObj;
            if (metadata.getDistributionInfo() != null && !metadata.getDistributionInfo().isEmpty()) {
                final Distribution distributionInfoI = metadata.getDistributionInfo().iterator().next();
                final DefaultDistribution distributionInfo = (DefaultDistribution) distributionInfoI;
                List<DigitalTransferOptions> toRemove = new ArrayList<>();
                for (DigitalTransferOptions dt : distributionInfo.getTransferOptions()) {
                    for (OnlineResource or : dt.getOnLines()) {
                        if (or.getProtocol().equals(protocol) &&
                            (serviceInstance == null || (or.getLinkage() != null && or.getLinkage().toString().endsWith('/' + serviceInstance)))) {
                            toRemove.add(dt);
                        }
                    }
                }
                distributionInfo.getTransferOptions().removeAll(toRemove);
            }
        }
    }




    public static void addServiceLink(final Object metadataObj, final String spec, final String serviceURL, String serviceInstance, String layerName) {
        String description = DESCRIPTION_MAP.get(spec);
        String protocol = PROTOCOL_MAP.get(spec);
        if (protocol == null) {
            throw new IllegalArgumentException("Unsupported value for spec parameter:" + spec);
        }
        if (metadataObj instanceof DefaultMetadata) {
            final DefaultMetadata metadata = (DefaultMetadata) metadataObj;

            final String link = serviceURL + "/" + spec.toLowerCase() + "/" + serviceInstance;
            if (metadata.getDistributionInfo() == null || metadata.getDistributionInfo().isEmpty()) {
                final DefaultDistribution distributionInfo = new DefaultDistribution();

                final DefaultDigitalTransferOptions transferOption = new DefaultDigitalTransferOptions();
                try {
                    final DefaultOnlineResource or = new DefaultOnlineResource(new URI(link));
                    or.setProtocol(protocol);
                    or.setName(new SimpleInternationalString(layerName));
                    or.setDescription(new SimpleInternationalString(description));
                    transferOption.setOnLines(Arrays.asList(or));
                } catch (URISyntaxException ex) {
                    LOGGER.log(Level.WARNING, "URI syntax exception when parsing service url:" + link, ex);
                }

                distributionInfo.setTransferOptions(Arrays.asList(transferOption));
                metadata.setDistributionInfo(Arrays.asList(distributionInfo));

            } else {
                final Distribution distributionInfoI = metadata.getDistributionInfo().iterator().next();
                if (distributionInfoI instanceof DefaultDistribution) {
                    final DefaultDistribution distributionInfo = (DefaultDistribution) distributionInfoI;
                    final DefaultDigitalTransferOptions transferOption = new DefaultDigitalTransferOptions();
                    try {
                        final DefaultOnlineResource or = new DefaultOnlineResource(new URI(link));
                        or.setProtocol(protocol);
                        or.setName(new SimpleInternationalString(layerName));
                        or.setDescription(new SimpleInternationalString(description));
                        transferOption.setOnLines(Arrays.asList(or));
                    } catch (URISyntaxException ex) {
                        LOGGER.log(Level.WARNING, "URI syntax exception when parsing service url:" + link, ex);
                    }
                    distributionInfo.getTransferOptions().add(transferOption);
                }
            }
        }
    }
}
