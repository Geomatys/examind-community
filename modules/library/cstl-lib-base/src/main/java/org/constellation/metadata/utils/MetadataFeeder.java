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
package org.constellation.metadata.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.opengis.feature.AttributeType;
import org.opengis.feature.PropertyType;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.citation.Role;
import org.opengis.metadata.content.AttributeGroup;
import org.opengis.metadata.content.ContentInformation;
import org.opengis.metadata.content.CoverageDescription;
import org.opengis.metadata.content.RangeDimension;
import org.opengis.metadata.distribution.DigitalTransferOptions;
import org.opengis.metadata.distribution.Distribution;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.extent.GeographicDescription;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.metadata.identification.BrowseGraphic;
import org.opengis.metadata.identification.CouplingType;
import org.opengis.metadata.identification.DataIdentification;
import org.opengis.metadata.identification.DistributedComputingPlatform;
import org.opengis.metadata.identification.Identification;
import org.opengis.metadata.identification.KeywordType;
import org.opengis.metadata.identification.Keywords;
import org.opengis.metadata.identification.OperationMetadata;
import org.opengis.metadata.identification.TopicCategory;
import org.opengis.metadata.spatial.GeometricObjectType;
import org.opengis.metadata.spatial.SpatialRepresentation;
import org.apache.sis.coverage.grid.PixelInCell;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.InternationalString;
import org.opengis.util.NameFactory;

import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.feature.Features;
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.citation.DefaultAddress;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultCitationDate;
import org.apache.sis.metadata.iso.citation.DefaultContact;
import org.apache.sis.metadata.iso.citation.DefaultOnlineResource;
import org.apache.sis.metadata.iso.citation.DefaultResponsibleParty;
import org.apache.sis.metadata.iso.citation.DefaultTelephone;
import org.apache.sis.metadata.iso.constraint.DefaultLegalConstraints;
import org.apache.sis.metadata.iso.content.DefaultCoverageDescription;
import org.apache.sis.metadata.iso.distribution.DefaultDigitalTransferOptions;
import org.apache.sis.metadata.iso.distribution.DefaultDistribution;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.identification.AbstractIdentification;
import org.apache.sis.metadata.iso.identification.DefaultBrowseGraphic;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;
import org.apache.sis.metadata.iso.identification.DefaultKeywords;
import org.apache.sis.metadata.iso.identification.DefaultOperationMetadata;
import org.apache.sis.metadata.iso.identification.DefaultServiceIdentification;
import org.apache.sis.metadata.iso.spatial.DefaultGeometricObjects;
import org.apache.sis.metadata.iso.spatial.DefaultVectorSpatialRepresentation;
import org.apache.sis.storage.DataSet;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.DefaultInternationalString;
import org.apache.sis.util.iso.DefaultNameFactory;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.xml.IdentifierSpace;

import org.constellation.dto.contact.AccessConstraint;
import org.constellation.dto.contact.Contact;
import org.constellation.dto.contact.Details;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import static org.constellation.api.ServiceConstants.DELETE_SENSOR;
import static org.constellation.api.ServiceConstants.DESCRIBE_SENSOR;
import static org.constellation.api.ServiceConstants.GET_CAPABILITIES;
import static org.constellation.api.ServiceConstants.GET_FEATURE_OF_INTEREST;
import static org.constellation.api.ServiceConstants.GET_FEATURE_OF_INTEREST_TIME;
import static org.constellation.api.ServiceConstants.GET_OBSERVATION;
import static org.constellation.api.ServiceConstants.GET_OBSERVATION_BY_ID;
import static org.constellation.api.ServiceConstants.GET_RESULT_TEMPLATE;
import static org.constellation.api.ServiceConstants.INSERT_OBSERVATION;
import static org.constellation.api.ServiceConstants.INSERT_RESULT;
import static org.constellation.api.ServiceConstants.INSERT_RESULT_TEMPLATE;
import static org.constellation.api.ServiceConstants.INSERT_SENSOR;
import org.opengis.metadata.extent.TemporalExtent;
import org.opengis.temporal.TemporalPrimitive;


/**
 * Class which add some part on a metadata.
 *
 * @author bgarcia
 * @author Cédric Briançon (Geomatys)
 * @version 0.9
 * @since 0.9
 */
public class MetadataFeeder {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.utils");

    /**
     * metadata target
     */
    protected final DefaultMetadata eater;

    /**
     * Constructor
     *
     * @param eater {@link org.apache.sis.metadata.iso.DefaultMetadata} which contain new informations.
     */
    public MetadataFeeder(final DefaultMetadata eater) {
        this.eater = eater;
    }

    public void feedService(Details serviceInfo) {
        setAbstract(serviceInfo.getDescription());
        setTitle(serviceInfo.getName());
        setKeywordsNoType(serviceInfo.getKeywords());
        feedServiceContraint(serviceInfo.getServiceConstraints());
        feedServiceContact(serviceInfo.getServiceContact());
    }

    protected void feedServiceContraint(final AccessConstraint constraint) {
        if (constraint != null) {
            final AbstractIdentification identification = (AbstractIdentification) getIdentification(eater);
            final DefaultLegalConstraints legalConstraint = new DefaultLegalConstraints();
            if (constraint.getAccessConstraint() != null && !constraint.getAccessConstraint().isEmpty()) {
                final SimpleInternationalString useLim = new SimpleInternationalString(constraint.getAccessConstraint());
                legalConstraint.setUseLimitations(Arrays.asList(useLim));
            }
            if (constraint.getFees() != null && !constraint.getFees().isEmpty()) {
                final SimpleInternationalString fees = new SimpleInternationalString("Fees:" + constraint.getFees());
                legalConstraint.setOtherConstraints(Arrays.asList(fees));
            }
            identification.setResourceConstraints(Arrays.asList(legalConstraint));
        }
    }

    protected void feedServiceContact(final Contact contact) {
        if (contact != null) {
            final AbstractIdentification identification = (AbstractIdentification) getIdentification(eater);
            final DefaultResponsibleParty ct = new DefaultResponsibleParty(Role.POINT_OF_CONTACT);
            final DefaultContact cont = new DefaultContact();

            boolean hasAddress = false;
            final DefaultAddress adr = new DefaultAddress();
            if (contact.getAddress() != null && !contact.getAddress().isEmpty()) {
                adr.setDeliveryPoints(Arrays.asList(new SimpleInternationalString(contact.getAddress())));
                hasAddress = true;
            }
            if (contact.getCity()!= null && !contact.getCity().isEmpty()) {
                adr.setCity(new SimpleInternationalString(contact.getCity()));
                hasAddress = true;
            }
            if (contact.getCountry()!= null && !contact.getCountry().isEmpty()) {
                adr.setCountry(new SimpleInternationalString(contact.getCountry()));
                hasAddress = true;
            }
            if (contact.getState()!= null && !contact.getState().isEmpty()) {
                adr.setAdministrativeArea(new SimpleInternationalString(contact.getState()));
                hasAddress = true;
            }
            if (contact.getZipCode()!= null && !contact.getZipCode().isEmpty()) {
                adr.setPostalCode(contact.getZipCode());
                hasAddress = true;
            }
            if (contact.getEmail()!= null && !contact.getEmail().isEmpty()) {
                adr.setElectronicMailAddresses(Arrays.asList(contact.getEmail()));
                hasAddress = true;
            }
            if (hasAddress) {
                cont.setAddress(adr);
            }

            if (contact.getContactInstructions() != null && !contact.getContactInstructions().isEmpty()) {
                cont.setContactInstructions(new SimpleInternationalString(contact.getContactInstructions()));
            }
            if (contact.getHoursOfService() != null && !contact.getHoursOfService().isEmpty()) {
                cont.setHoursOfService(Collections.singletonList(new SimpleInternationalString(contact.getHoursOfService())));
            }

            final DefaultTelephone phone = new DefaultTelephone();
            boolean hasPhone = false;
            if (contact.getPhone() != null && !contact.getPhone().isEmpty()) {
                phone.setVoices(Arrays.asList(contact.getPhone()));
                hasPhone = true;
            }
            if (contact.getFax() != null && !contact.getFax().isEmpty()) {
                phone.setFacsimiles(Arrays.asList(contact.getFax()));
                hasPhone = true;
            }
            if (hasPhone) {
                cont.setPhone(phone);
            }

            if (contact.getUrl() != null && !contact.getUrl().isEmpty()) {
                try {
                    final DefaultOnlineResource or = new DefaultOnlineResource(new URI(contact.getUrl()));
                    cont.setOnlineResource(or);
                } catch (URISyntaxException ex) {
                    LOGGER.log(Level.WARNING, "unvalid URL in service contact", ex);
                }
            }
            ct.setContactInfo(cont);
            String fullName = "";
            if (contact.getFirstname()!= null && !contact.getFirstname().isEmpty()) {
                fullName = contact.getFirstname();
            }

            if (contact.getLastname()!= null && !contact.getLastname().isEmpty()) {
                fullName = fullName + " " + contact.getLastname();
            }

            if (!fullName.isEmpty()) {
                ct.setIndividualName(fullName);
            }

            if (contact.getOrganisation()!= null && !contact.getOrganisation().isEmpty()) {
                ct.setOrganisationName(new SimpleInternationalString(contact.getOrganisation()));
            }
            if (contact.getPosition()!= null && !contact.getPosition().isEmpty()) {
                ct.setPositionName(new SimpleInternationalString(contact.getPosition()));
            }

            identification.setPointOfContacts(Arrays.asList(ct));
        }
    }

    /**
     * Get IdentifiationInformation from metadata
     *
     * @param metadata {@link org.apache.sis.metadata.iso.DefaultMetadata} where we can found Identification
     * @return an {@link org.opengis.metadata.identification.Identification}
     */
    protected Identification getIdentification(DefaultMetadata metadata) {
        if (metadata.getIdentificationInfo() == null || metadata.getIdentificationInfo().isEmpty()) {
            metadata.getIdentificationInfo().add(new DefaultDataIdentification());
        }

        return metadata.getIdentificationInfo().iterator().next();
    }

    protected CoverageDescription getCoverageDescription(DefaultMetadata metadata, boolean create) {
        for (ContentInformation ci : metadata.getContentInfo()) {
            if (ci instanceof CoverageDescription) {
                return (CoverageDescription) ci;
            }
        }
        DefaultCoverageDescription cd = null;
        if (create) {
            cd = new DefaultCoverageDescription();
            metadata.setContentInfo(Arrays.asList(cd));
        }
        return cd;
    }

     protected Identification getServiceIdentification(DefaultMetadata metadata) {
        if (metadata.getIdentificationInfo().isEmpty()) {
            metadata.getIdentificationInfo().add(new DefaultServiceIdentification());
        }

        return metadata.getIdentificationInfo().iterator().next();
    }

    public Identification getServiceIdentification() {
        return getServiceIdentification(eater);
    }

    public String getTitle() {
        final AbstractIdentification identification = (AbstractIdentification) getIdentification(eater);
        if (identification != null && identification.getCitation() != null) {
            final Citation citation = identification.getCitation();
            if (citation != null) {
                final InternationalString is = citation.getTitle();
                if (is != null) {
                    return is.toString();
                }
            }
        }
        return null;
    }

    /**
     * Add title on metadata
     *
     * @param title title  we want add
     */
    public void setTitle(final String title) {
        final AbstractIdentification identification = (AbstractIdentification) getIdentification(eater);
        final InternationalString internationalizeTitle = new DefaultInternationalString(title);
        if (identification.getCitation() == null) {
            final DefaultCitation citation = new DefaultCitation();
            citation.setTitle(internationalizeTitle);
            identification.setCitation(citation);
        } else {
            final DefaultCitation citation = (DefaultCitation) identification.getCitation();
            citation.setTitle(internationalizeTitle);
        }
    }

    protected CitationDate getCitationDate() {
        final AbstractIdentification identification = (AbstractIdentification) getIdentification(eater);
        if (identification.getCitation() != null) {
            final Citation citation = identification.getCitation();
            if (!citation.getDates().isEmpty()) {
                return citation.getDates().iterator().next();
            }
        }
        return null;
    }

    /**
     * Add data date on metadata
     *
     * @param date     {@link java.util.Date} need to be inserted
     * @param dateType {@link org.opengis.metadata.citation.DateType} to define the type of the date inserted
     */
    protected void setCitationDate(final Date date, final DateType dateType) {
        final AbstractIdentification identification = (AbstractIdentification) getIdentification(eater);
        final DefaultCitationDate citDate = new DefaultCitationDate(date, dateType);
        if (identification.getCitation() == null) {
            final DefaultCitation citation = new DefaultCitation();
            citation.setDates(Collections.singletonList(citDate));
            identification.setCitation(citation);
        }

        final DefaultCitation citation = (DefaultCitation) identification.getCitation();
        final List<CitationDate> dates = new ArrayList<>(0);
        for (CitationDate d : citation.getDates()) {
            dates.add(d);
        }
        dates.add(citDate);
        citation.setDates(dates);
    }

    public void setCreationDate(final Date date) {
        final DefaultCitationDate creationDate = new DefaultCitationDate(date, DateType.CREATION);
        final AbstractIdentification ident = (AbstractIdentification)getIdentification(eater);
        DefaultCitation citation = (DefaultCitation) ident.getCitation();
        if (citation == null) {
            citation = new DefaultCitation();
            citation.setDates(Collections.singletonList(creationDate));
            ident.setCitation(citation);
            return;
        }
        //remove old creationDate
        final List<CitationDate> dates = new ArrayList<>();
        for (CitationDate cd : citation.getDates()) {
            if (DateType.CREATION.equals(cd.getDateType())) {
                dates.add(cd);
            }
        }
        citation.getDates().removeAll(dates);
        //add the new creation date
        citation.getDates().add(creationDate);
    }

    public void setCitationIdentifier(final String fileIdentifier) {
        final AbstractIdentification id = (AbstractIdentification) getIdentification(eater);
        DefaultCitation citation = (DefaultCitation) id.getCitation();
        if (citation == null) {
            citation = new DefaultCitation();
            citation.setIdentifiers(Collections.singleton(new DefaultIdentifier(fileIdentifier)));
            id.setCitation(citation);
            return;
        }
        citation.setIdentifiers(Collections.singleton(new DefaultIdentifier(fileIdentifier)));
    }

    public String getAbstract() {
        final AbstractIdentification identification = (AbstractIdentification) getIdentification(eater);
        final InternationalString internationalizeAbstract = identification.getAbstract();
        if (internationalizeAbstract != null) {
            return internationalizeAbstract.toString();
        }
        return null;
    }

    /**
     * Add abstract on metadata
     *
     * @param _abstract abstract we want add.
     */
    protected void setAbstract(String _abstract) {
        final AbstractIdentification identification = (AbstractIdentification) getIdentification(eater);
        final InternationalString internationalizeAbstract;
        if (_abstract != null) {
            internationalizeAbstract = new DefaultInternationalString(_abstract);
        } else {
            internationalizeAbstract = null;
        }
        identification.setAbstract(internationalizeAbstract);
    }

    /**
     * Returns all keywords of metadata.
     * called by lucene indexer to index all metadata keywords.
     * @return {@code List} of string keywords.
     */
    public final List<String> getKeywords() {
        final List<String> keywords = new ArrayList<>();
        final AbstractIdentification ident = (AbstractIdentification) getIdentification(eater);
        if(ident != null) {
            for (final Keywords descKeywords : ident.getDescriptiveKeywords()) {
                for (final InternationalString is : descKeywords.getKeywords()) {
                    keywords.add(is.toString());
                }
            }
        }
        return keywords;
    }

    public final List<String> getKeywordsNoType() {
        final List<String> keywords = new ArrayList<>();
        final AbstractIdentification ident = (AbstractIdentification) getIdentification(eater);
        for (Keywords descKeywords : ident.getDescriptiveKeywords()) {
            if (descKeywords.getType() == null) {
                for (InternationalString is : descKeywords.getKeywords()) {
                    keywords.add(is.toString());
                }
            }
        }
        return keywords;
    }

    /**
     * Add keywords on metadata
     *
     * @param keywords a Keyword {@link java.util.List}
     */
    public void AddKeywordsNoType(final List<String> keywords) {
        if (keywords == null) {
            return;
        }
        final List<InternationalString> kw = new ArrayList<>();
        for (String k : keywords) {
            kw.add(new SimpleInternationalString(k));
        }
        final AbstractIdentification ident = (AbstractIdentification) getIdentification(eater);
        for (Keywords descKeywords : ident.getDescriptiveKeywords()) {
            if (descKeywords.getType() == null) {
                if (descKeywords instanceof DefaultKeywords) {
                    DefaultKeywords kwnt = (DefaultKeywords)descKeywords;
                    kwnt.getKeywords().addAll(kw);
                    return;
                }
            }
        }
        final DefaultKeywords keywordsNoType = new DefaultKeywords();
        keywordsNoType.setKeywords(kw);
        ident.getDescriptiveKeywords().add(keywordsNoType);
    }

    /**
     * Set keywords on metadata
     *
     * @param keywords a Keyword {@link java.util.List}
     */
    public void setKeywordsNoType(final List<String> keywords) {
        if (keywords == null) {
            return;
        }
        final List<InternationalString> kw = new ArrayList<>();
        for (String k : keywords) {
            kw.add(new SimpleInternationalString(k));
        }
        final AbstractIdentification ident = (AbstractIdentification) getIdentification(eater);
        List<Keywords> toRemove = new ArrayList<>();
        for (Keywords descKeywords : ident.getDescriptiveKeywords()) {
            if (descKeywords.getType() == null) {
                toRemove.add(descKeywords);
            }
        }
        ident.getDescriptiveKeywords().removeAll(toRemove);
        final DefaultKeywords keywordsNoType = new DefaultKeywords();
        keywordsNoType.setKeywords(kw);
        ident.getDescriptiveKeywords().add(keywordsNoType);
    }

    public void addKeywords(final String type, final List<String> keywords) {
        if (keywords == null) {
            return;
        }
        final KeywordType kwType = KeywordType.valueOf(type);
        final List<InternationalString> kw = new ArrayList<>();
        for (String k : keywords) {
            kw.add(new SimpleInternationalString(k));
        }
        final AbstractIdentification ident = (AbstractIdentification) getIdentification(eater);
        for (Keywords descKeywords : ident.getDescriptiveKeywords()) {
            if (kwType.equals(descKeywords.getType())) {
                if (descKeywords instanceof DefaultKeywords) {
                    DefaultKeywords kwnt = (DefaultKeywords)descKeywords;
                    kwnt.getKeywords().addAll(kw);
                    return;
                }
            }
        }
        final DefaultKeywords keywordWithType = new DefaultKeywords();
        keywordWithType.setKeywords(kw);
        keywordWithType.setType(kwType);
        ident.getDescriptiveKeywords().add(keywordWithType);
    }


    public void setKeywords(final String type, final List<String> keywords) {
        if (keywords == null) {
            return;
        }
        final KeywordType kwType = KeywordType.valueOf(type);
        final List<InternationalString> kw = new ArrayList<>();
        for (String k : keywords) {
            kw.add(new SimpleInternationalString(k));
        }
        final AbstractIdentification ident = (AbstractIdentification) getIdentification(eater);
        List<Keywords> toRemove = new ArrayList<>();
        for (Keywords descKeywords : ident.getDescriptiveKeywords()) {
            if (kwType.equals(descKeywords.getType())) {
                toRemove.add(descKeywords);
            }
        }
        ident.getDescriptiveKeywords().removeAll(toRemove);
        final DefaultKeywords keywordWithType = new DefaultKeywords();
        keywordWithType.setKeywords(kw);
        keywordWithType.setType(kwType);
        ident.getDescriptiveKeywords().add(keywordWithType);
    }

    protected String getDataLanguage() {
        final Identification identificationI = getIdentification(eater);
        if (identificationI instanceof DataIdentification) {
            final DataIdentification identification = (DataIdentification) getIdentification(eater);
            if (!identification.getLanguages().isEmpty()) {
                return identification.getLanguages().iterator().next().getLanguage();
            }
        }
        return null;
    }

    /**
     * Add data locale on metadata
     *
     * @param dataLocale {@link java.util.Locale} for data locale
     */
    public void addDataLanguage(final Locale dataLocale) {
        if (dataLocale == null) {
            return;
        }
        final DefaultDataIdentification identification = (DefaultDataIdentification) getIdentification(eater);
        if (identification.getLanguages() == null) {
            identification.setLanguages(Collections.singletonList(dataLocale));
        } else {
            identification.getLanguages().add(dataLocale);
        }
    }

    public void setDataLanguage(final Locale dataLocale) {
        if (dataLocale == null) {
            return;
        }
        final Identification identificationI = getIdentification(eater);
        if (identificationI instanceof DefaultDataIdentification) {
            final DefaultDataIdentification identification = (DefaultDataIdentification) identificationI;
            if (identification.getLanguages() == null) {
                identification.setLanguages(Collections.singletonList(dataLocale));
            } else {
                identification.getLanguages().clear();
                identification.getLanguages().add(dataLocale);
            }
        }
    }

    public String getTopicCategory() {
        final Identification identification = getIdentification(eater);
        if (!identification.getTopicCategories().isEmpty()) {
            return identification.getTopicCategories().iterator().next().identifier().orElse(null);
        }
        return null;
    }

    public List<String> getAllTopicCategory() {
        List<String> result = new ArrayList<>();
        final Identification identification = getIdentification(eater);
        if (!identification.getTopicCategories().isEmpty()) {
            final Collection<TopicCategory> topicCategories = identification.getTopicCategories();//.iterator().next().identifier();
            for (TopicCategory topicCategory : topicCategories){
                result.add(topicCategory.identifier().orElseThrow());
            }
        }

        return result;
    }

    /**
     * Add a topicCategory on metadata
     *
     * @param topicCategoryName topic code to found right {@link org.opengis.metadata.identification.TopicCategory}
     */
    public void addTopicCategory(final String topicCategoryName) {
        if (topicCategoryName == null) {
            return;
        }
        final TopicCategory topic = TopicCategory.valueOf(topicCategoryName);
        final Identification identificationI = getIdentification(eater);
        if (identificationI instanceof DefaultDataIdentification) {
            final DefaultDataIdentification identification = (DefaultDataIdentification) identificationI;
            if (identification.getTopicCategories() == null) {
                identification.setTopicCategories(Collections.singletonList(topic));
            } else {
                identification.getTopicCategories().add(topic);
            }
        } else if (identificationI instanceof DefaultServiceIdentification) {
            final DefaultServiceIdentification identification = (DefaultServiceIdentification) identificationI;
            if (identification.getTopicCategories() == null) {
                identification.setTopicCategories(Collections.singletonList(topic));
            } else {
                identification.getTopicCategories().add(topic);
            }
        }
    }

    public void setTopicCategory(final String topicCategoryName) {
        if (topicCategoryName == null) {
            return;
        }
        final TopicCategory topic = TopicCategory.valueOf(topicCategoryName);
        final Identification identificationI = getIdentification(eater);
        if (identificationI instanceof DefaultDataIdentification) {
            final DefaultDataIdentification identification = (DefaultDataIdentification) identificationI;
            if (identification.getTopicCategories() == null) {
                identification.setTopicCategories(Collections.singletonList(topic));
            } else {
                identification.getTopicCategories().clear();
                identification.getTopicCategories().add(topic);
            }
        } else if (identificationI instanceof DefaultServiceIdentification) {
            final DefaultServiceIdentification identification = (DefaultServiceIdentification) identificationI;
            if (identification.getTopicCategories() == null) {
                identification.setTopicCategories(Collections.singletonList(topic));
            } else {
                identification.getTopicCategories().clear();
                identification.getTopicCategories().add(topic);
            }
        }
    }

    /**
     * Add fileIdentifier on metadata
     *
     * @param identifier the fileIdentifier
     */
    protected void setIdentifier(final String identifier) {
        eater.setFileIdentifier(identifier);
    }

    public String getIdentifier() {
        return eater.getFileIdentifier();
    }

    /**
     * Add dateStamp on metadata
     *
     * @param dateStamp
     */
    protected void addDateStamp(final Date dateStamp) {
        eater.setDateStamp(dateStamp);
    }

    protected String getMetadataLocale() {
        if (!eater.getLocales().isEmpty()) {
            return eater.getLocales().iterator().next().getLanguage();
        }
        return null;
    }

    /**
     * Add locale on metadata
     *
     * @param metadataLocale
     */
    protected void setMetadataLocale(final Locale metadataLocale, final Charset chars) {
        eater.setLocalesAndCharsets(Collections.singletonMap(metadataLocale, chars));
    }

    /**
     * Add a contact on metadata
     *
     * @param individualName   data user name
     * @param organisationName data organisation name
     * @param userRole         user role
     */
    protected void addContact(final String individualName, final String organisationName, final String userRole) {
        DefaultResponsibleParty newContact = new DefaultResponsibleParty();
        newContact.setIndividualName(individualName);
        final InternationalString internationalizeOrganisation = new DefaultInternationalString(organisationName);
        newContact.setOrganisationName(internationalizeOrganisation);
        Role currentRole = Role.valueOf(userRole);
        newContact.setRole(currentRole);
        eater.getContacts().add(newContact);
    }

    protected void setContact(final String individualName, final String organisationName, final String userRole) {
        DefaultResponsibleParty newContact = new DefaultResponsibleParty();
        newContact.setIndividualName(individualName);
        final InternationalString internationalizeOrganisation = new DefaultInternationalString(organisationName);
        newContact.setOrganisationName(internationalizeOrganisation);
        Role currentRole = Role.valueOf(userRole);
        newContact.setRole(currentRole);
        eater.getContacts().clear();
        eater.getContacts().add(newContact);
    }

    protected String getOrganisationName() {
        if (!eater.getContacts().isEmpty()) {
            final InternationalString is = DefaultResponsibleParty.castOrCopy(eater.getContacts().iterator().next()).getOrganisationName();
            if (is != null) {
                return is.toString();
            }
        }
        return null;
    }

    protected String getIndividualName() {
        if (!eater.getContacts().isEmpty()) {
            return DefaultResponsibleParty.castOrCopy(eater.getContacts().iterator().next()).getIndividualName();
        }
        return null;
    }

    protected String getRole() {
        if (!eater.getContacts().isEmpty()) {
            final Role is = eater.getContacts().iterator().next().getRole();
            if (is != null) {
                return is.name();
            }
        }
        return null;
    }

    public String getServiceType() {
        final Collection<Identification> idents = eater.getIdentificationInfo();
        DefaultServiceIdentification servIdent = null;
        for (Identification ident : idents) {
            if (ident instanceof DefaultServiceIdentification) {
                servIdent = (DefaultServiceIdentification) ident;
            }
        }
        if (servIdent != null && servIdent.getServiceType() != null) {
            return servIdent.getServiceType().toString();
        }
        return null;
    }

    public String getServiceInstanceName() {
        final Identification servIdent = getIdentification(eater);
        if (servIdent != null) {
            final Citation cit = servIdent.getCitation();
            if (cit != null && cit.getOtherCitationDetails() != null) {
                return cit.getOtherCitationDetails().toString();
            }
        }
        return null;
    }

    public void setServiceInstanceName(final String serviceInstance) {
        final Identification servIdent = getServiceIdentification();
        if (servIdent != null) {
           DefaultCitation cit = (DefaultCitation) servIdent.getCitation();
           if (cit != null) {
               cit.setOtherCitationDetails(Collections.singleton(new SimpleInternationalString(serviceInstance)));
           } else {
               cit = new DefaultCitation();
               cit.setOtherCitationDetails(Collections.singleton(new SimpleInternationalString(serviceInstance)));
               ((AbstractIdentification)servIdent).setCitation(cit);
           }
        } else {
            final DefaultServiceIdentification ident = new DefaultServiceIdentification();
            final DefaultCitation cit = new DefaultCitation();
            cit.setOtherCitationDetails(Collections.singleton(new SimpleInternationalString(serviceInstance)));
            ident.setCitation(cit);
            eater.setIdentificationInfo(Collections.singletonList(ident));
        }

    }

    public void addServiceInformation(final String serviceType, final String url) {
        final Collection<Identification> idents = eater.getIdentificationInfo();
        DefaultServiceIdentification servIdent = null;
        for (Identification ident : idents) {
            if (ident instanceof DefaultServiceIdentification) {
                servIdent = (DefaultServiceIdentification) ident;
            }
        }
        if (servIdent == null) {
            servIdent = new DefaultServiceIdentification();
            eater.getIdentificationInfo().add(servIdent);
        }
        final NameFactory nameFacto = new DefaultNameFactory();
        servIdent.setCouplingType(CouplingType.LOOSE);
        servIdent.setServiceType(nameFacto.createLocalName(null, serviceType));
        servIdent.setContainsOperations(getOperation(serviceType, url));

        try {
            Distribution dist;
            Collection<? extends Distribution> dists = eater.getDistributionInfo();
            if (dists.isEmpty()) {
                dist = new DefaultDistribution();
                eater.setDistributionInfo(Collections.singleton(dist));
            } else {
                dist = dists.iterator().next();
            }

            DefaultDigitalTransferOptions dto = new DefaultDigitalTransferOptions();
            dto.setOnLines(Collections.singleton(new DefaultOnlineResource(new URI(url))));
            addWithoutDoublon(dist.getTransferOptions(), Collections.singleton(dto));
        } catch (URISyntaxException e) {
            LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
        }
    }

    public void updateServiceURL(final String url) {
        final Collection<Identification> idents = eater.getIdentificationInfo();
        DefaultServiceIdentification servIdent = null;
        for (Identification ident : idents) {
            if (ident instanceof DefaultServiceIdentification) {
                servIdent = (DefaultServiceIdentification) ident;
            }
        }
        if (servIdent != null) {
            for (OperationMetadata om : servIdent.getContainsOperations()) {
                for (OnlineResource or : om.getConnectPoints()) {
                    final DefaultOnlineResource resource = (DefaultOnlineResource) or;
                    try {
                        resource.setLinkage(new URI(url));
                    } catch (URISyntaxException ex) {
                        LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
                    }
                }
            }
        }
        Collection<? extends Distribution> dist = eater.getDistributionInfo();
        if (!dist.isEmpty()) {
            for (DigitalTransferOptions dto : dist.iterator().next().getTransferOptions()) {
                for (OnlineResource or : dto.getOnLines()) {
                    final DefaultOnlineResource resource = (DefaultOnlineResource) or;
                    try {
                        resource.setLinkage(new URI(url));
                    } catch (URISyntaxException ex) {
                        LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
                    }
                }
            }
        }
    }

    protected List<OperationMetadata> getOperation(final String serviceType, final String url) {
        final List<OperationMetadata> operations = new ArrayList<>();
        OnlineResource resource;
        try {
            resource = new DefaultOnlineResource(new URI(url));
        } catch (URISyntaxException ex) {
            LOGGER.log(Level.WARNING, "unvalid URL:" + url, ex);  // TODO: NON!!!!!
            resource = null;
        }
        operations.add(buildOperation(GET_CAPABILITIES, resource));

        switch (serviceType) {
            case "WMS":
                operations.add(buildOperation("GetMap", resource));
                operations.add(buildOperation("GetFeatureInfo", resource));
                operations.add(buildOperation("DescribeLayer", resource));
                operations.add(buildOperation("GetLegendGraphic", resource));
                break;
            case "WFS":
                operations.add(buildOperation("GetFeature", resource));
                operations.add(buildOperation("DescribeFeatureType", resource));
                operations.add(buildOperation("Transaction", resource));
                break;
            case "WCS":
                operations.add(buildOperation("DescribeCoverage", resource));
                operations.add(buildOperation("GetCoverage", resource));
                break;
            case "WMTS":
                operations.add(buildOperation("GetTile", resource));
                operations.add(buildOperation("GetFeatureInfo", resource));
                break;
            case "CSW":
                operations.add(buildOperation("GetRecords", resource));
                operations.add(buildOperation("GetRecordById", resource));
                operations.add(buildOperation("DescribeRecord", resource));
                operations.add(buildOperation("GetDomain", resource));
                operations.add(buildOperation("Transaction", resource));
                operations.add(buildOperation("Harvest", resource));
                break;
            case "SOS":
                operations.add(buildOperation(GET_OBSERVATION, resource));
                operations.add(buildOperation(GET_OBSERVATION_BY_ID, resource));
                operations.add(buildOperation(DESCRIBE_SENSOR, resource));
                operations.add(buildOperation(GET_FEATURE_OF_INTEREST, resource));
                operations.add(buildOperation(INSERT_OBSERVATION, resource));
                operations.add(buildOperation(INSERT_SENSOR, resource));
                operations.add(buildOperation(DELETE_SENSOR, resource));
                operations.add(buildOperation(INSERT_RESULT, resource));
                operations.add(buildOperation(INSERT_RESULT_TEMPLATE, resource));
                operations.add(buildOperation(GET_RESULT_TEMPLATE, resource));
                operations.add(buildOperation(GET_FEATURE_OF_INTEREST_TIME, resource));
                break;
        }
        // TODO other service
        return operations;
    }

    protected OperationMetadata buildOperation(final String operationName, final OnlineResource url) {
        return new DefaultOperationMetadata(operationName, DistributedComputingPlatform.WEB_SERVICES, url);
    }

    public void setServiceMetadataIdForData(final List<String> layerIds) {
        final Collection<Identification> idents = eater.getIdentificationInfo();
        DefaultServiceIdentification servIdent = null;
        for (Identification ident : idents) {
            if (ident instanceof DefaultServiceIdentification) {
                servIdent = (DefaultServiceIdentification) ident;
            }
        }
        if (servIdent == null) {
            servIdent = new DefaultServiceIdentification();
            eater.getIdentificationInfo().add(servIdent);
        }

        final List<DataIdentification> resources = new ArrayList<>();
        for (String layerId : layerIds) {
            final DefaultDataIdentification dataIdent = new DefaultDataIdentification();
            dataIdent.getIdentifierMap().put(IdentifierSpace.HREF, layerId);
            resources.add(dataIdent);
        }
        servIdent.setOperatesOn(resources);
    }

    public void addServiceMetadataIdForData(final String layerId) {
        final Collection<Identification> idents = eater.getIdentificationInfo();
        DefaultServiceIdentification servIdent = null;
        for (Identification ident : idents) {
            if (ident instanceof DefaultServiceIdentification) {
                servIdent = (DefaultServiceIdentification) ident;
            }
        }
        if (servIdent == null) {
            servIdent = new DefaultServiceIdentification();
            eater.getIdentificationInfo().add(servIdent);
        }

        final Collection<DataIdentification> resources = servIdent.getOperatesOn();
        for (DataIdentification did : resources) {
            final DefaultDataIdentification dataIdent = (DefaultDataIdentification) did;
            if (layerId.equals(dataIdent.getIdentifierMap().getSpecialized(IdentifierSpace.HREF).toString())) {
                return;
            }
        }
        // add new resouce
        final DefaultDataIdentification dataIdent = new DefaultDataIdentification();
        dataIdent.getIdentifierMap().put(IdentifierSpace.HREF, layerId);
        servIdent.getOperatesOn().add(dataIdent);
    }

    /**
     * Copy the elements of a source into a destination collection, without adding the elements
     * which are already present in the destination collection.
     * @param destination The collection to copy data to.
     * @param source The collection to get data from.
     */
    public static void addWithoutDoublon(Collection destination, Collection source) {
        if (source == null || source.isEmpty()) {
            return;
        }

        if (destination.isEmpty()) {
            destination.addAll(source);
        } else {
            for (Object object : source) {
                if (!destination.contains(object)) {
                    destination.add(object);
                }
            }
        }
    }

    public List<String> getAllSequenceIdentifier() {
        List<String> result = new ArrayList<>();
        for (ContentInformation contentInformation : eater.getContentInfo()){
            if (contentInformation != null && contentInformation instanceof  CoverageDescription){
                for (AttributeGroup attributeGroup : ((CoverageDescription) contentInformation).getAttributeGroups()){
                    if (attributeGroup != null) {
                        for (RangeDimension rangeDimension : attributeGroup.getAttributes()) {
                            if (rangeDimension != null && rangeDimension.getSequenceIdentifier() != null) {
                                result.add(rangeDimension.getSequenceIdentifier().toString());
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    public String getProcessingLevel() {
        final Identification identification = getIdentification(eater);
        if (identification !=null && identification.getProcessingLevel() != null) {
            return identification.getProcessingLevel().toString();
        }
        return null;
    }

    public List<String> getAllGeographicIdentifier() {
        List<String> result = new ArrayList<>();
        final Identification identification = getIdentification(eater);
        if (identification != null) {
            for (Extent extent : identification.getExtents()) {
                if (extent != null) {
                    for (GeographicExtent geographicExtent : extent.getGeographicElements()) {
                        if (geographicExtent instanceof GeographicDescription &&
                           ((GeographicDescription) geographicExtent).getGeographicIdentifier() != null) {
                            result.add(((GeographicDescription) geographicExtent).getGeographicIdentifier().toString());
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     *
     * @return All found Geographic bounding boxes from current metadata. Note that bounding polygons are ignored.
     */
    public Stream<GeographicBoundingBox> getGeographicBBoxes() {
        return getExtents(eater)
                .flatMap(extent -> extent.getGeographicElements().stream())
                .filter(GeographicBoundingBox.class::isInstance)
                .map(GeographicBoundingBox.class::cast);
    }

    /**
     *
     * @return All found Temporal Extent, extent from current metadata.
     */
    public Stream<TemporalPrimitive> getTemporalExtent() {
        return getExtents(eater)
                .flatMap(extent -> extent.getTemporalElements().stream())
                .map(TemporalExtent::getExtent);
    }
    /**
     *
     * @return Any Geographic bbox found, or null.
     * @deprecated return only one of possibly many boxes defined. Please consider using {@link #getGeographicBBoxes()}
     * instead. For exact same behavior, you can use {@code getGeographicBBoxes().findAny().orElse(null); }
     */
    @Deprecated
    public GeographicBoundingBox getGeographicBoundingBox() {
        final Identification identification = getIdentification(eater);
        if (identification != null) {
            for (Extent extent : identification.getExtents()) {
                if (extent != null) {
                    for (GeographicExtent geographicExtent : extent.getGeographicElements()) {
                        if (geographicExtent instanceof GeographicBoundingBox) {
                            return (GeographicBoundingBox) geographicExtent;
                        }
                    }
                }
            }
        }
        return null;
    }

    public URI getQuickLookUrl() {
        final Identification identification = getIdentification(eater);
        if (identification != null) {
            for (BrowseGraphic bg : identification.getGraphicOverviews()) {
                if (bg != null) {
                    return bg.getFileName();
                }
            }
        }
        return null;
    }

    public void setQuickLookUrl(URI url) {
        final Identification identification = getIdentification(eater);
        if (identification != null) {
            if (!identification.getGraphicOverviews().isEmpty()) {
                for (BrowseGraphic bg : identification.getGraphicOverviews()) {
                    if (bg instanceof DefaultBrowseGraphic) {
                        ((DefaultBrowseGraphic)bg).setFileName(url);
                    }
                }
            } else if (identification instanceof DefaultDataIdentification){
                DefaultBrowseGraphic bg = new DefaultBrowseGraphic(url);
                ((DefaultDataIdentification)identification).setGraphicOverviews(Arrays.asList(bg));
            }
        }
    }

    public void setCoverageDescription(CoverageDescription newValue, WriteOption copyBehavior) {
        final Runnable mdInit;
        final Collection contentInfo = eater.getContentInfo();
        switch (copyBehavior) {
            case REPLACE_EXISTING:
                mdInit = () -> contentInfo.removeIf(info -> info instanceof CoverageDescription);
                break;
            case CREATE_NEW:
                if (contentInfo.stream().anyMatch(CoverageDescription.class::isInstance)) return;
                // Clean metadata: initial action is identity
                mdInit = () -> {};
                break;
            case APPEND:
            default:
                // NOTHING TO DO, we'll add them after existing ones
                mdInit = () -> {};
        }
        mdInit.run();
        contentInfo.add(newValue);
    }

    /**
     *
     * @param datasource Dataset to use for envelope extraction.
     * @param copyBehavior How to manage information overlap between newly added envelope and possibly already existing
     *                     information in target metadata.
     * @return Extents effectively written in target metadata. Cannot be null, but can be empty. If empty, it means
     * metadata has not been modified.
     *
     * @throws DataStoreException If we cannot extract envelope from datasource.
     * @throws TransformException DataSet has provided an envelope, but we cannot set it in metadata due to incompatible
     * reference system (ISO-19115 defines geographic spatial extents, for example).
     */
    public Collection<? extends Extent> setExtent(final DataSet datasource, WriteOption copyBehavior) throws DataStoreException, TransformException {
        try {
            final Supplier<Optional<Envelope>> envelope = () -> {
                try {
                    return datasource.getEnvelope();
                } catch (DataStoreException e) {
                    throw new BackingStoreException(e);
                }
            };
            return setExtent(envelope, copyBehavior);
        } catch (BackingStoreException e) {
            throw new DataStoreException(e);
        }
    }

    /**
     * See {@link #setExtent(Supplier, WriteOption)}.
     */
    public Collection<? extends Extent> setExtent(final Envelope newExtent, WriteOption copyBehavior) throws TransformException {
        return setExtent(() -> Optional.ofNullable(newExtent), copyBehavior);
    }

    /**
     *
     * @param datasource Component providing envelope to set.
     * @param copyBehavior How to manage information overlap between newly added envelope and possibly already existing
     *                     information in target metadata.
     * @return Extents effectively written in target metadata. Cannot be null, but can be empty. If empty, it means
     * metadata has not been modified.
     *
     * @throws TransformException DataSet has provided an envelope, but we cannot set it in metadata due to incompatible
     * reference system (ISO-19115 defines geographic spatial extents, for example).
     */
    public Collection<? extends Extent> setExtent(final Supplier<Optional<Envelope>> datasource, WriteOption copyBehavior) throws TransformException {
        final Consumer<DefaultMetadata> mdInit;
        switch (copyBehavior) {
            case REPLACE_EXISTING:
                mdInit = md -> {
                    for (Identification ident : md.getIdentificationInfo()) {
                        ident.getExtents().clear();
                    }
                };
                break;
            case CREATE_NEW:
                if (hasExtent(eater)) return Collections.EMPTY_LIST;
                // Clean metadata: initial action is identity
                mdInit = md -> {};
                break;
            case APPEND:
            default:
                // NOTHING TO DO, we'll add them after existing ones
                mdInit = md -> {};
        }

        final Optional<Envelope> optEnv = datasource.get();
        if (optEnv.isPresent()) {
            final MetadataBuilder builder = new MetadataBuilder();
            builder.addExtent(optEnv.get(), null);
            final DefaultMetadata md = builder.build();
            final List<? extends Extent> newExtents = getExtents(md).collect(Collectors.toList());
            if (!newExtents.isEmpty()) {
                mdInit.accept(eater);
                getIdentification(eater).getExtents().addAll((List) newExtents);
                return newExtents;
            }
        }

        return Collections.EMPTY_LIST;
    }

    public Collection<? extends Extent> setExtent(final Extent newExtent, WriteOption copyBehavior) {
        return setFullExtent(() -> Optional.ofNullable(newExtent), copyBehavior);
    }

    public Collection<? extends Extent> setFullExtent(final Supplier<Optional<Extent>> datasource, WriteOption copyBehavior) {
        final Consumer<DefaultMetadata> mdInit;
        switch (copyBehavior) {
            case REPLACE_EXISTING:
                mdInit = md -> {
                    for (Identification ident : md.getIdentificationInfo()) {
                        ident.getExtents().clear();
                    }
                };
                break;
            case CREATE_NEW:
                if (hasExtent(eater)) return Collections.EMPTY_LIST;
                // Clean metadata: initial action is identity
                mdInit = md -> {};
                break;
            case APPEND:
            default:
                // NOTHING TO DO, we'll add them after existing ones
                mdInit = md -> {};
        }

        final Optional<Extent> optExt = datasource.get();
        if (optExt.isPresent()) {
                mdInit.accept(eater);
                Identification aid = getIdentification(eater);
                List<Extent> newExtents = new ArrayList<>(aid.getExtents());
                newExtents.add(optExt.get());
                if (aid instanceof AbstractIdentification) {
                    ((AbstractIdentification)aid).setExtents(newExtents);
                } else {
                    throw new IllegalArgumentException("identification info is not modifiable");
                }
                return newExtents;
        }

        return Collections.EMPTY_LIST;
    }

    public void setGeographicBoundingBox(final Optional<GeographicBoundingBox> optBox, WriteOption copyBehavior) {
        final Consumer<DefaultMetadata> mdInit;
        switch (copyBehavior) {
            case REPLACE_EXISTING:
                mdInit = md -> {
                    for (Identification ident : md.getIdentificationInfo()) {
                        for (Extent ex : ident.getExtents()) {
                            List<GeographicExtent> toRemove = new ArrayList<>();
                            for (GeographicExtent geoEx : ex.getGeographicElements()) {
                                if (geoEx instanceof GeographicBoundingBox) {
                                    toRemove.add(geoEx);
                                }
                            }
                            ex.getGeographicElements().removeAll(toRemove);
                        }
                    }
                };
                break;
            case CREATE_NEW:
                if (hasGeographicBoundingBox(eater)) return;
                // Clean metadata: initial action is identity
                mdInit = md -> {};
                break;
            case APPEND:
            default:
                // NOTHING TO DO, we'll add them after existing ones
                mdInit = md -> {};
        }

        if (optBox.isPresent()) {
            mdInit.accept(eater);
            AbstractIdentification ident = (AbstractIdentification) getIdentification(eater);
            if (hasExtent(eater)) {
                List<Extent> newExtents = new ArrayList<>(ident.getExtents());
                DefaultExtent ex = (DefaultExtent) newExtents.iterator().next();
                List<GeographicExtent> newGeoElems = new ArrayList<>(ex.getGeographicElements());
                newGeoElems.add(optBox.get());
                ex.setGeographicElements(newGeoElems);
                ident.setExtents(newExtents);
            } else {
                 DefaultExtent ex = new DefaultExtent();
                 ex.setGeographicElements(Arrays.asList(optBox.get()));
                 ident.setExtents(Arrays.asList(ex));
            }
        }
    }

    public void setCoverageDescriptionAttributeGroups(final Collection<? extends AttributeGroup> newValues, WriteOption copyBehavior) {
        final Consumer<DefaultMetadata> mdInit;
        switch (copyBehavior) {
            case REPLACE_EXISTING:
                mdInit = md -> {
                    for (ContentInformation content : md.getContentInfo()) {
                        if (content instanceof CoverageDescription) {
                            CoverageDescription covDesc = (CoverageDescription) content;
                            covDesc.getAttributeGroups().clear();
                        }
                    }
                };
                break;
            case CREATE_NEW:
                if (hasCoverageDescriptionAttributeGroups(eater)) return;
                // Clean metadata: initial action is identity
            case APPEND:
            default:
                // NOTHING TO DO, we'll add them after existing ones
                mdInit = md -> {};
        }

        if (!newValues.isEmpty()) {
            mdInit.accept(eater);
            CoverageDescription cd = getCoverageDescription(eater, true);
            if (cd instanceof DefaultCoverageDescription) {
                ((DefaultCoverageDescription)cd).setAttributeGroups(newValues);
            } else {
                throw new IllegalArgumentException("coverage description is not modifiable");
            }
        }
    }

    /**
     *
     * @param datasource Spatial resource to build a representation for.
     * @param copyBehavior What to do if spatial representation is already present in target metadata.
     * @return Affected spatial representation, if any.
     * @throws DataStoreException If accessing given datasource fails.
     */
    public Optional<SpatialRepresentation> setSpatialRepresentation(DataSet datasource, WriteOption copyBehavior) throws DataStoreException {
        final Consumer<DefaultMetadata> mdInit;
        switch (copyBehavior) {
            case REPLACE_EXISTING:
                mdInit = md -> md.getSpatialRepresentationInfo().clear();
                break;
            case CREATE_NEW:
                if (hasExtent(eater)) return Optional.empty();
                // Clean metadata: initial action is identity
            case APPEND:
            default:
                // NOTHING TO DO, we'll add them after existing ones
                mdInit = md -> {};
        }
        final Optional<SpatialRepresentation> newValue;
        if (datasource instanceof FeatureSet) {
            newValue = buildSpatialRepresentation((FeatureSet) datasource);
        } else if (datasource instanceof GridCoverageResource) {
            newValue = buildSpatialRepresentation((GridCoverageResource) datasource);
        } else return Optional.empty();

        newValue.ifPresent(spatialRepresentation -> {
            mdInit.accept(eater);
            eater.getSpatialRepresentationInfo().add(spatialRepresentation);
        });

        return newValue;
    }

    private static Optional<SpatialRepresentation> buildSpatialRepresentation(FeatureSet datasource) throws DataStoreException {
        final List<DefaultGeometricObjects> geoms = datasource.getType().getProperties(true).stream()
                .filter(p -> !isALink(p))
                .flatMap(property -> Features.toAttribute(property).map(Stream::of).orElseGet(Stream::empty))
                .flatMap(
                        attribute -> getGeomTypeFromJTS(attribute)
                                .map(DefaultGeometricObjects::new)
                                .map(Stream::of)
                                .orElseGet(Stream::empty)
                )
                .collect(Collectors.toList());
        if (geoms.isEmpty()) return Optional.empty();
        final DefaultVectorSpatialRepresentation vsr = new DefaultVectorSpatialRepresentation();
        vsr.getGeometricObjects().addAll(geoms);
        return Optional.of(vsr);
    }

    private static Optional<SpatialRepresentation> buildSpatialRepresentation(GridCoverageResource datasource) throws DataStoreException {
        final GridGeometry geometry = datasource.getGridGeometry();
        final String description = (geometry.isDefined(GridGeometry.GRID_TO_CRS)) ?
                geometry.getGridToCRS(PixelInCell.CELL_CENTER).toWKT() : null;
        final MetadataBuilder builder = new MetadataBuilder();
        builder.addSpatialRepresentation(description, geometry, geometry.isDefined(GridGeometry.RESOLUTION));
        return builder.build().getSpatialRepresentationInfo().stream()
                .findAny();
    }

    public Stream<? extends Extent> getExtents() {
        return getExtents(eater);
    }

    private Stream<? extends Extent> getExtents(Metadata md) {
        return md.getIdentificationInfo().stream()
                .flatMap(ident -> ident.getExtents().stream());
    }

    private boolean hasExtent(Metadata md) {
        return getExtents(md)
                .findAny()
                .isPresent();
    }

    public Collection<? extends AttributeGroup> getCoverageDescriptionAttributeGroups() {
        return getCoverageDescriptionAttributeGroups(eater);
    }

    private Collection<? extends AttributeGroup> getCoverageDescriptionAttributeGroups(DefaultMetadata md) {
        CoverageDescription cd = getCoverageDescription(md, false);
        if (cd != null) {
            return cd.getAttributeGroups();
        }
        return new ArrayList<>();
    }

    private boolean hasCoverageDescriptionAttributeGroups(Metadata md) {
        CoverageDescription cd = getCoverageDescription(eater, false);
        return cd !=null && cd.getAttributeGroups().isEmpty();
    }

    private boolean hasGeographicBoundingBox(Metadata md) {
        return getGeographicBBoxes()
                .findAny()
                .isPresent();
    }

    private static Optional<GeometricObjectType> getGeomTypeFromJTS(AttributeType property) {
        Class binding = property.getValueClass();
        if (Point.class.isAssignableFrom(binding)) {
            return Optional.of(GeometricObjectType.POINT);
        } else if (LineString.class.isAssignableFrom(binding)) {
            return Optional.of(GeometricObjectType.CURVE);
        } else if (Polygon.class.isAssignableFrom(binding)) {
            return Optional.of(GeometricObjectType.SURFACE);
        } else if (Geometry.class.isAssignableFrom(binding)) return Optional.of(GeometricObjectType.COMPLEX);
        return Optional.empty();
    }

    private static boolean isALink(final PropertyType target) {
        return Features.getLinkTarget(target).isPresent();
    }

    /**
     * Specify what behavior to adopt when adding any information into an existing metadata.
     */
    public enum WriteOption {
        /**
         * Write information only if it does not already exists into target metadata. Otherwise, no operation is done.
         */
        CREATE_NEW,
        /**
         * If the information to add/set already exists, it will be deleted before-hand. It should only be deleted if
         * the new information is present/not empty.
         */
        REPLACE_EXISTING,
        /**
         * Do not check if the information already exists. Simply add the new one at the end of the corresponding set/category.
         */
        APPEND;
    }
}
