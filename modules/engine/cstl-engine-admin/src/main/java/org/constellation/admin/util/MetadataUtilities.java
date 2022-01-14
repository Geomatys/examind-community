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

import java.io.IOException;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.constellation.engine.template.TemplateEngine;
import org.constellation.engine.template.TemplateEngineException;
import org.constellation.engine.template.TemplateEngineFactory;
import org.constellation.util.Util;
import org.opengis.metadata.Metadata;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.metadata.iso.citation.DefaultOnlineResource;
import org.apache.sis.metadata.iso.distribution.DefaultDigitalTransferOptions;
import org.apache.sis.metadata.iso.distribution.DefaultDistribution;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.SimpleInternationalString;
import org.constellation.dto.metadata.MetadataBbox;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.metadata.identification.Identification;
import org.constellation.dto.CstlUser;
import org.constellation.metadata.utils.MetadataFeeder;
import org.constellation.metadata.utils.Utils;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.temporal.object.TemporalUtilities;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.distribution.DigitalTransferOptions;
import org.opengis.metadata.distribution.Distribution;


/**
 * Utility class to do some operation on metadata file (generate, revover, ...)
 *
 *  TODO look for redundance with {@link org.constellation.metadata.utils.Utils}
 *
 * @author bgarcia
 * @version 0.9
 * @since 0.9
 */
public final class MetadataUtilities {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.admin.util");

    public static String getTemplateSensorMLString(final Properties prop, final String type) {
        try {
            final TemplateEngine templateEngine = TemplateEngineFactory.getInstance(TemplateEngineFactory.GROOVY_TEMPLATE_ENGINE);
            final InputStream stream;
            if ("Component".equals(type)) {
                stream = Util.getResourceAsStream("org/constellation/engine/template/smlComponentTemplate.xml");
            } else if ("System".equals(type)) {
                stream = Util.getResourceAsStream("org/constellation/engine/template/smlSystemTemplate.xml");
            } else {
                throw new IllegalArgumentException("unexpected sml type");
            }

            //apply props
            final String templateApplied = templateEngine.apply(stream, prop);

            //write to file
            Path templateFile = Files.createTempFile("smlTemplate", ".xml");
            IOUtilities.writeString(templateApplied, templateFile);

            return templateApplied;
        } catch (TemplateEngineException | IOException ex) {
           LOGGER.log(Level.WARNING, null, ex);
        }
        return null;
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
                           if (!results.contains(bbox)) {
                               results.add(bbox);
                           }
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

    public static String getMetadataIdForData(final String providerId, final String namespace, final String name){
        ArgumentChecks.ensureNonNull("dataName", name);
        ArgumentChecks.ensureNonNull("providerId", providerId);
        String nmsp = namespace;
        if (nmsp == null) {
            nmsp = "";
        }
        return  providerId + '_' + nmsp + name;
    }

    public static String getMetadataIdForDataset(final String providerId){
        ArgumentChecks.ensureNonNull("providerId", providerId);
        return  providerId;
    }

    public static void updateServiceMetadataURL(final String serviceIdentifier, final String serviceType, final String cstlURL, final DefaultMetadata metadata) {
        final MetadataFeeder feeder = new MetadataFeeder(metadata);
        final String serviceURL = cstlURL + "/WS/" + serviceType.toLowerCase() + '/' + serviceIdentifier;
        feeder.updateServiceURL(serviceURL);
    }

    public static String fillMetadataFromProperties(final Properties prop, final String dataType, final String metadataID, final String title, final String crsName,
            final Optional<CstlUser> optUser, final List<String> keywords) {
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

        if (optUser.isPresent()) {
            final CstlUser user = optUser.get();
            prop.put("contactName", user.getFirstname()+" "+user.getLastname());
            prop.put("contactEmail", user.getEmail());
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
