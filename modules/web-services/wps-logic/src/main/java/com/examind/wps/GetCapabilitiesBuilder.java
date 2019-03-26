/**
 * Copyright 2018 Geomatys.
 *
 * TODO : license
 */
package com.examind.wps;

import com.examind.wps.util.WPSConstants;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.sis.util.ArgumentChecks;
import org.constellation.dto.contact.AccessConstraint;
import org.constellation.dto.contact.Contact;
import org.constellation.dto.contact.Details;
import org.geotoolkit.ows.xml.v200.CapabilitiesBaseType;
import org.geotoolkit.ows.xml.v200.CodeType;
import org.geotoolkit.ows.xml.v200.ContactType;
import org.geotoolkit.ows.xml.v200.KeywordsType;
import org.geotoolkit.ows.xml.v200.LanguageStringType;
import org.geotoolkit.ows.xml.v200.OnlineResourceType;
import org.geotoolkit.ows.xml.v200.OperationsMetadata;
import org.geotoolkit.ows.xml.v200.ResponsiblePartySubsetType;
import org.geotoolkit.ows.xml.v200.ServiceIdentification;
import org.geotoolkit.ows.xml.v200.ServiceProvider;
import org.geotoolkit.wps.xml.v200.ProcessOffering;
import org.geotoolkit.wps.xml.v200.ProcessSummary;
import org.geotoolkit.wps.xml.v200.Capabilities;
import org.geotoolkit.wps.xml.v200.Contents;
import org.geotoolkit.wps.client.WPSVersion;

/**
 * Defines routines to build a WPS capabilities document.
 *
 * TODO:
 * <ol>
 * <li>change how process summary are given by user, it's not a very clear
 * procedure right now. We should make use of {@link ProcessOffering} API.</li>
 * <li>Activate internationalization</li>
 * </ol>
 *
 *  Not thread-safe.
 *
 * @author Alexis Manin
 */
public class GetCapabilitiesBuilder {

    WPSVersion version;
    String updateSequence;
    String serviceUrl;
    Details serviceDetails;
    Stream<ProcessSummary> processes;
    OperationsMetadata operationMetadata;
    String defaultLanguage;
    String[] supportedLanguages;

    public Capabilities build() {
        checkParameters();
        final ServiceIdentification ident = serviceDetails == null?
                null : createServiceIdentification(serviceDetails);
        final ServiceProvider sp = createServiceProvider(version, serviceDetails);

        // TODO: make proper management of process offerings through ProcessOffering API.
        final List<ProcessSummary> summaries;
        if (this.processes == null) {
            summaries = Collections.EMPTY_LIST;
        } else {
            summaries = (List) this.processes.collect(Collectors.toList());
        }

        final Contents processOffering = new Contents(summaries);

        OperationsMetadata opMeta;
        if (operationMetadata == null) {
            opMeta = WPSConstants.OPERATIONS_METADATA.get(version);
            if (opMeta != null) {
                opMeta = (OperationsMetadata) opMeta.clone();
                opMeta.updateURL(serviceUrl);
            }
        } else {
            opMeta = operationMetadata;
        }

        final String defaultLang;
        final boolean hasDefaultLang = defaultLanguage != null && !defaultLanguage.trim().isEmpty();
        final boolean hasSupportedLangs = supportedLanguages != null && supportedLanguages.length > 0;
        if (hasDefaultLang) {
            defaultLang = defaultLanguage;
        } else {
            if (hasSupportedLangs) {
                defaultLang = supportedLanguages[0];
            } else {
                defaultLang = WPSConstants.WPS_LANG_EN;
            }
        }

        final List<String> supportedLangs;
        if (hasSupportedLangs) {
            supportedLangs = Arrays.asList(supportedLanguages);
        } else {
            supportedLangs = Collections.singletonList(defaultLang);
        }

        final CapabilitiesBaseType.Languages languages = new CapabilitiesBaseType.Languages(supportedLangs);

        final Capabilities capa = new Capabilities(
                ident,
                sp,
                opMeta,
                version.getCode(),
                updateSequence,
                processOffering,
                languages,
                null, // ext
                defaultLanguage
        );

        return capa;
    }

    private void checkParameters() {
        ArgumentChecks.ensureNonNull("WPS version", version);

        final boolean hasDefaultLang = defaultLanguage != null && !defaultLanguage.trim().isEmpty();
        final boolean hasSupportedLangs = supportedLanguages != null && supportedLanguages.length > 0;
        if (hasDefaultLang && hasSupportedLangs) {
            if (!Arrays.asList(supportedLanguages).contains(defaultLanguage)) {
                throw new IllegalArgumentException("Given default language ["+defaultLanguage+"] cannot be found in supported languages");
            }
        }
    }

    public WPSVersion getVersion() {
        return version;
    }

    public GetCapabilitiesBuilder setVersion(WPSVersion version) {
        this.version = version;
        return this;
    }

    public String getUpdateSequence() {
        return updateSequence;
    }

    public GetCapabilitiesBuilder setUpdateSequence(String updateSequence) {
        this.updateSequence = updateSequence;
        return this;
    }

    public String getServiceUrl() {
        return serviceUrl;
    }

    public GetCapabilitiesBuilder setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
        return this;
    }

    public Details getServiceDetails() {
        return serviceDetails;
    }

    public GetCapabilitiesBuilder setServiceDetails(Details serviceDetails) {
        this.serviceDetails = serviceDetails;
        return this;
    }

    public Stream<ProcessSummary> getProcesses() {
        return processes;
    }

    public GetCapabilitiesBuilder setProcesses(Stream<ProcessSummary> descriptions) {
        this.processes = descriptions;
        return this;
    }

    public OperationsMetadata getOperationMetadata() {
        return operationMetadata;
    }

    public GetCapabilitiesBuilder setOperationMetadata(OperationsMetadata operationMetadata) {
        this.operationMetadata = operationMetadata;
        return this;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public GetCapabilitiesBuilder setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
        return this;
    }

    public String[] getSupportedLanguages() {
        return supportedLanguages;
    }

    public GetCapabilitiesBuilder setSupportedLanguages(String... supportedLanguages) {
        if (supportedLanguages != null) {
            supportedLanguages = Stream.of(supportedLanguages)
                    .filter(Objects::nonNull)
                    .toArray(s -> new String[s]);
        }
        this.supportedLanguages = supportedLanguages;
        return this;
    }

    private static ServiceIdentification createServiceIdentification(final Details metadata) {
        ArgumentChecks.ensureNonNull("Detail metadata", metadata);

        final AccessConstraint constraint = metadata.getServiceConstraints();

        final String fees;
        final List<String> accessConstraints;
        if (constraint == null) {
            fees = null;
            accessConstraints = Collections.EMPTY_LIST;
        } else {
            fees = constraint.getFees();
            accessConstraints = Collections.singletonList(constraint.getAccessConstraint());
        }

        return new ServiceIdentification(
                new LanguageStringType(metadata.getName()),
                new LanguageStringType(metadata.getDescription()),
                new KeywordsType(metadata.getKeywords()),
                new CodeType(WPSConstants.WPS_SERVICE),
                metadata.getVersions(),
                fees,
                accessConstraints
        );
    }

    private static ServiceProvider createServiceProvider(final WPSVersion version, final Details metadata) {
        ArgumentChecks.ensureNonNull("WPS version", version);

        Contact serviceContact = metadata == null? null : metadata.getServiceContact();

        final String organization;
        final OnlineResourceType organizationUrl;
        final ResponsiblePartySubsetType responsible;
        if (serviceContact != null) {
            organization = serviceContact.getOrganisation();

            final ContactType contact = new ContactType(
                    serviceContact.getPhone(),
                    serviceContact.getFax(),
                    serviceContact.getEmail(),
                    serviceContact.getAddress(),
                    serviceContact.getCity(),
                    serviceContact.getState(),
                    serviceContact.getZipCode(),
                    serviceContact.getCountry(),
                    serviceContact.getHoursOfService(),
                    serviceContact.getContactInstructions()
            );

            responsible = new ResponsiblePartySubsetType(
                    serviceContact.getFullname(),
                    serviceContact.getPosition(),
                    contact,
                    null
            );

            final String contactUrl = serviceContact.getUrl();
            organizationUrl = contactUrl == null?
                    null : new OnlineResourceType(contactUrl);

        } else {
            organization = null;
            organizationUrl = null;
            responsible = new ResponsiblePartySubsetType(null, null, null, null);
        }

        return new ServiceProvider(
                organization,
                organizationUrl,
                responsible
        );
    }
}