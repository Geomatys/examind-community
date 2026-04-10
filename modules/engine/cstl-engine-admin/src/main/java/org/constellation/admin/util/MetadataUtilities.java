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
import java.io.UncheckedIOException;
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
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.citation.DefaultAddress;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultCitationDate;
import org.apache.sis.metadata.iso.citation.DefaultContact;
import org.apache.sis.metadata.iso.citation.DefaultOnlineResource;
import org.apache.sis.metadata.iso.citation.DefaultResponsibleParty;
import org.apache.sis.metadata.iso.citation.DefaultTelephone;
import org.apache.sis.metadata.iso.distribution.DefaultDigitalTransferOptions;
import org.apache.sis.metadata.iso.distribution.DefaultDistribution;
import org.apache.sis.metadata.iso.distribution.DefaultDistributor;
import org.apache.sis.metadata.iso.distribution.DefaultFormat;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;
import org.apache.sis.metadata.iso.identification.DefaultKeywords;
import org.apache.sis.metadata.iso.identification.DefaultRepresentativeFraction;
import org.apache.sis.metadata.iso.identification.DefaultResolution;
import org.apache.sis.metadata.iso.maintenance.DefaultScope;
import org.apache.sis.metadata.iso.quality.DefaultCompletenessCommission;
import org.apache.sis.metadata.iso.quality.DefaultDataQuality;
import org.apache.sis.metadata.iso.quality.DefaultQuantitativeResult;
import org.apache.sis.referencing.internal.shared.NilReferencingObject;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.xml.bind.metadata.replace.ReferenceSystemMetadata;
import org.constellation.dto.metadata.MetadataBbox;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.metadata.identification.Identification;
import org.constellation.dto.CstlUser;
import org.constellation.metadata.utils.MetadataFeeder;
import org.constellation.metadata.utils.Utils;
import org.geotoolkit.nio.IOUtilities;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.citation.Role;
import org.opengis.metadata.distribution.DigitalTransferOptions;
import org.opengis.metadata.distribution.Distribution;
import org.opengis.metadata.identification.CharacterSet;
import org.opengis.metadata.identification.TopicCategory;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.metadata.quality.Element;
import org.opengis.metadata.spatial.SpatialRepresentationType;
import org.opengis.referencing.ReferenceSystem;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;


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

    public static @Nullable String getTemplateMetadata(@NonNull final Properties prop, @NonNull final String templatePath) {
        try {
            final TemplateEngine templateEngine = TemplateEngineFactory.getInstance(TemplateEngineFactory.GROOVY_TEMPLATE_ENGINE);
            try (InputStream stream = Util.getResourceAsStream(templatePath)) {
                return templateEngine.apply(stream, prop);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
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

    public static DefaultMetadata buildMetadataFromProperties(final Properties prop, final String dataType, final String metadataID, final String title, final String crsName,
            final Optional<CstlUser> optUser, final List<String> keywords) {
        
        final CstlUser user  = optUser.orElse(null);
        
        DefaultMetadata templateMetadata = new DefaultMetadata();
        templateMetadata.setFileIdentifier(metadataID);
        templateMetadata.setLanguage(Locale.ENGLISH);
        templateMetadata.setCharacterSet(CharacterSet.UTF_8);
        
        // does this property is fill a some point??
        if (prop.contains("parentId")) {
            templateMetadata.setParentIdentifier(prop.getProperty("parentId"));
        }
        DefaultResponsibleParty contact = new DefaultResponsibleParty();
        
        if (user != null) {
            contact.setIndividualName(user.getFirstname() + " " + user.getLastname());
        }
        if (prop.contains("organisationName")) {
            contact.setOrganisationName(new SimpleInternationalString(prop.getProperty("organisationName")));
        }
        if (prop.contains("position")) {
            contact.setPositionName(new SimpleInternationalString(prop.getProperty("position")));
        }
        
        DefaultContact conInfo = new DefaultContact();
        if (prop.contains("phone") || prop.contains("fax")) {
            DefaultTelephone phone = new DefaultTelephone();
            if (prop.contains("phone")) {
                phone.setNumber(prop.getProperty("phone"));
            }
            if (prop.contains("fax")) {
                phone.setFacsimiles(List.of(prop.getProperty("fax")));
            }
            conInfo.setPhones(List.of(phone));
        }
        DefaultAddress address = new DefaultAddress();
        if (prop.contains("address")) {
            address.setDeliveryPoints(List.of(new SimpleInternationalString(prop.getProperty("address"))));
        }
        if (prop.contains("city")) {
            address.setCity(new SimpleInternationalString(prop.getProperty("city")));
        }
        if (prop.contains("postalCode")) {
            address.setPostalCode(prop.getProperty("postalCode"));
        }
        if (prop.contains("country")) {
            address.setCountry(new SimpleInternationalString(prop.getProperty("country")));
        }
        if (user != null) {
            address.setElectronicMailAddresses(List.of(user.getEmail()));
        }
        conInfo.setAddresses(List.of(address));
        contact.setContactInfo(conInfo);
        
        if (prop.contains("role")) {
            contact.setRole(Role.valueOf(prop.getProperty("role")));
        }
        templateMetadata.setContacts(List.of(contact));
        
        templateMetadata.setDateStamp(new Date(System.currentTimeMillis()));
        
        templateMetadata.setMetadataStandardName("ISO19115");
        templateMetadata.setMetadataStandardVersion("2003/Cor.1:2006");

        ReferenceSystem refSystem;
        if (crsName != null) {
            refSystem = new ReferenceSystemMetadata(new DefaultIdentifier(crsName));
        } else {
            refSystem = NilReferencingObject.INSTANCE;
        }
        templateMetadata.setReferenceSystemInfo(List.of(refSystem));
        
        DefaultDataIdentification dataIdent = new DefaultDataIdentification();
        
        DefaultCitation citation = new DefaultCitation();
        citation.setTitle(new SimpleInternationalString(title));
        
        List<CitationDate> dates = new ArrayList<>();
        if (prop.contains("publicationDate")) {
            DefaultCitationDate cd = new DefaultCitationDate(Instant.parse(prop.getProperty("publicationDate")), DateType.PUBLICATION);
            dates.add(cd);
        }
        dates.add(new DefaultCitationDate(Instant.now(), DateType.CREATION));
        if (prop.contains("revisionDate")) {
            DefaultCitationDate cd = new DefaultCitationDate(Instant.parse(prop.getProperty("revisionDate")), DateType.REVISION);
            dates.add(cd);
        }
        citation.setDates(dates);
        dataIdent.setCitation(citation);
        dataIdent.setAbstract(new SimpleInternationalString(""));
        
        if (keywords != null && !keywords.isEmpty()) {
            DefaultKeywords kw = new DefaultKeywords();
            List<SimpleInternationalString> kws = new ArrayList<>();
            for (String keyword : keywords) {
                kws.add(new SimpleInternationalString(keyword));
            }
            kw.setKeywords(kws);
            dataIdent.setDescriptiveKeywords(List.of(kw));
        }
        
        if ("raster".equalsIgnoreCase(dataType)) {
            dataIdent.setSpatialRepresentationTypes(List.of(SpatialRepresentationType.GRID));
        } else if("vector".equalsIgnoreCase(dataType)) {
            dataIdent.setSpatialRepresentationTypes(List.of(SpatialRepresentationType.VECTOR));
        }
        
        if (prop.contains("groundResolution")) {
            DefaultResolution resolution = new DefaultResolution(new DefaultRepresentativeFraction(Long.parseLong(prop.getProperty("groundResolution"))));
            dataIdent.setSpatialResolutions(List.of(resolution));
        }
        
        if (prop.contains("dataLocale")) {
            dataIdent.setLanguages(List.of(Locale.of(prop.getProperty("dataLocale"))));
        }
        
        if (prop.contains("topicCategory")) {
            dataIdent.setTopicCategories(List.of(TopicCategory.valueOf(prop.getProperty("topicCategory"))));
        }
        templateMetadata.setIdentificationInfo(List.of(dataIdent));
        
        DefaultDistribution distribInfo = new DefaultDistribution();
        if (prop.contains("distributionFormat")) {
            DefaultFormat format = new DefaultFormat();
            format.setName(new SimpleInternationalString(prop.getProperty("distributionFormat")));
            distribInfo.setDistributionFormats(List.of(format));
        }
        DefaultDistributor distributor = new DefaultDistributor();
        distributor.setDistributorContact(contact);
        distribInfo.setDistributors(List.of(distributor));

        templateMetadata.setDistributionInfo(List.of(distribInfo));
        
        DefaultDataQuality qualityInfo = new DefaultDataQuality();
        qualityInfo.setScope(new DefaultScope(ScopeCode.DATASET));
        
        List<Element> reports = new ArrayList();
        if (prop.contains("acquisitionQualityValue")) {
            DefaultCompletenessCommission cc = new DefaultCompletenessCommission();
            cc.setNamesOfMeasure(List.of(new SimpleInternationalString("Quality percent value")));
            cc.setMeasureDescription(new SimpleInternationalString("Overall quality of the data"));
            DefaultQuantitativeResult result = new DefaultQuantitativeResult();
            // TODO result.setValues(newValues);
            cc.setResults(List.of(result));
        }
        
        if (prop.contains("percentCloudCover")) {
            DefaultCompletenessCommission cc = new DefaultCompletenessCommission();
            cc.setNamesOfMeasure(List.of(new SimpleInternationalString(prop.getProperty("measureName"))));
            cc.setMeasureDescription(new SimpleInternationalString("Percent of Missing Data (Cloud Coverage)"));
            DefaultQuantitativeResult result = new DefaultQuantitativeResult();
            // TODO result.setValues(newValues);
            cc.setResults(List.of(result));
        }
        qualityInfo.setReports(reports);
        
        templateMetadata.setDataQualityInfo(List.of(qualityInfo));
        
        return templateMetadata;
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
