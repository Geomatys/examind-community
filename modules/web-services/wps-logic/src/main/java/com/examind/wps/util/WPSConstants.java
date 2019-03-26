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
package com.examind.wps.util;

import org.constellation.dto.contact.AccessConstraint;
import org.constellation.dto.contact.Contact;
import org.constellation.dto.contact.Details;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.constellation.api.CommonConstants.DEFAULT_CRS;

import org.geotoolkit.ows.xml.v200.Operation;
import org.geotoolkit.ows.xml.v200.CodeType;
import org.geotoolkit.ows.xml.v200.ContactType;
import org.geotoolkit.ows.xml.v200.DCP;
import org.geotoolkit.ows.xml.v200.KeywordsType;
import org.geotoolkit.ows.xml.v200.LanguageStringType;
import org.geotoolkit.ows.xml.v200.OnlineResourceType;
import org.geotoolkit.ows.xml.v200.OperationsMetadata;
import org.geotoolkit.ows.xml.v200.ResponsiblePartySubsetType;
import org.geotoolkit.ows.xml.v200.ServiceIdentification;
import org.geotoolkit.ows.xml.v200.ServiceProvider;
import org.geotoolkit.ows.xml.v200.DomainType;
import org.geotoolkit.wps.xml.v200.Capabilities;
import org.geotoolkit.wps.xml.v200.BoundingBoxData;
import org.geotoolkit.wps.xml.v200.DataDescription;
import org.geotoolkit.wps.xml.v200.Format;
import org.geotoolkit.wps.xml.v200.SupportedCRS;
import org.geotoolkit.wps.client.WPSVersion;
import org.geotoolkit.wps.io.WPSMimeType;

/**
 *  WPS Constants
 *
 * @author Quentin Boileau (Geomatys)
 */
public final class WPSConstants {

    private WPSConstants() {}

    /**
     * WPS Query service
     */
    public static final String WPS_SERVICE = "WPS";

    /**
     * Lang
     */
    public static final String WPS_LANG_EN = "en-EN";
    public static final String WPS_LANG_FR = "fr-FR";
    /**
     * Languages supported by the service. From WPS 2, we should support language without locale. For more information,
     * see OGC specification, table 51: "DescribeProcess request KVP encoding"
     */
    public static final List<String> WPS_SUPPORTED_LANG = Arrays.asList(WPS_LANG_EN, WPS_LANG_FR, "en", "fr");
    public static final Locale WPS_EN_LOC = Locale.forLanguageTag(WPS_LANG_EN);

    /**
     * Request parameters.
     */
    public static final String GETSTATUS = "GetStatus";
    public static final String GETRESULT = "GetResult";
    public static final String GETCAPABILITIES = "GetCapabilities";
    public static final String DESCRIBEPROCESS = "DescribeProcess";
    public static final String EXECUTE = "Execute";
    public static final String DISMISS = "Dismiss";


    public static final String JOBID_PARAMETER = "JOBID";

    public static final String IDENTIFIER_PARAMETER = "IDENTIFIER";
    public static final String LANGUAGE_PARAMETER = "LANGUAGE";

    public static final String DATA_INPUTS_PARAMETER = "DATAINPUTS";
    public static final String RESPONSE_DOCUMENT_PARAMETER = "RESPONSEDOCUMENT";
    public static final String RAW_DATA_OUTPUT_PARAMETER = "RAWDATAOUTPUT";

    // Input/output parameters
    public static final String MIME_TYPE_PARAMETER = "MIMETYPE";
    public static final String ENCODING_PARAMETER = "ENCODING";
    public static final String SCHEMA_PARAMETER = "SCHEMA";
    public static final String HREF_PARAMETER = "HREF";
    // The WPS specification seems to accept either the attribute mimeType or format to set a mime type
    public static final String FORMAT_PARAMETER = "FORMAT";
    public static final String METHOD_PARAMETER = "METHOD";
    public static final String HEADER_PARAMETER = "HEADER";
    public static final String BODY_PARAMETER = "BODY";
    public static final String BODY_REFERENCE_PARAMETER = "BODYREFERENCE";
    public static final String DATA_TYPE_PARAMETER = "DATATYPE";
    public static final String UOM_PARAMETER = "UOM";
    public static final String AS_REFERENCE_PARAMETER = "asReference";

    // CRS and BoundingBox identifier
    public static final String BOUNDING_BOX_IDENTIFIER_PARAMETER = "ows:boundingbox";
    public static final String CRS_IDENTIFIER_PARAMETER = "urn:ogc:def:crs";

    // ResponseDocument options
    public static final String LINEAGE_PARAMETER = "lineage";
    public static final String STATUS_PARAMETER = "status";
    public static final String STORE_EXECUTE_RESPONSE_PARAMETER = "storeExecuteResponse";

    public static final String WMS_SUPPORTED = "WMS_SUPPORTED";

    /* Maximum size in megabytes for a complex input */
    public static final int MAX_MB_INPUT_COMPLEX = 100;


   /**
     * Separator to use for building process URNs.
     */
    public static final String URN_SEPARATOR = ":";

    /**
     * Process identifier prefix to uniquely identifiy process using URN code.
     */
    public static final String PROCESS_PREFIX = "urn"+URN_SEPARATOR+"exa"+URN_SEPARATOR+"wps"+URN_SEPARATOR;

    public static final Map<WPSVersion, OperationsMetadata> OPERATIONS_METADATA = new EnumMap<>(WPSVersion.class);

    static {
        final List<DCP> getAndPost = new ArrayList<>();
        getAndPost.add(new DCP("somURL", "someURL"));

        final List<DCP> onlyPost = new ArrayList<>();
        onlyPost.add(new DCP(null, "someURL"));

        final List<Operation> operations = new ArrayList<>();

        final List<DomainType> gcParameters = new ArrayList<>();
        gcParameters.add(new DomainType("service", Arrays.asList(WPS_SERVICE)));
        gcParameters.add(new DomainType("Acceptversions", Arrays.asList(WPSVersion.v100.getCode())));
        gcParameters.add(new DomainType("AcceptFormats", Arrays.asList("text/xml")));
        final Operation getCapabilities = new Operation(getAndPost, gcParameters, null, null, "GetCapabilities");
        operations.add(getCapabilities);

        final List<DomainType> dpParameters = new ArrayList<>();
        dpParameters.add(new DomainType("service", Arrays.asList(WPS_SERVICE)));
        dpParameters.add(new DomainType("version", Arrays.asList(WPSVersion.v100.getCode())));
        final Operation describeProcess = new Operation(getAndPost, dpParameters, null, null, "DescribeProcess");
        operations.add(describeProcess);

        final List<DomainType> eParameters = new ArrayList<>();
        eParameters.add(new DomainType("service", Arrays.asList(WPS_SERVICE)));
        eParameters.add(new DomainType("version", Arrays.asList(WPSVersion.v100.getCode())));
        final Operation execute = new Operation(onlyPost, eParameters, null, null, "Execute");
        operations.add(execute);

        final List<DomainType> constraints = new ArrayList<>();
        constraints.add(new DomainType("PostEncoding", Arrays.asList("XML")));

        OPERATIONS_METADATA.put(WPSVersion.v100, new OperationsMetadata(operations, null, constraints, null));
    }

    static {
        final List<DCP> getAndPost = new ArrayList<>();
        getAndPost.add(new DCP("somURL", "someURL"));

        final List<DCP> onlyPost = new ArrayList<>();
        onlyPost.add(new DCP(null, "someURL"));

        final List<Operation> operations = new ArrayList<>();

        final List<DomainType> gcParameters = new ArrayList<>();
        gcParameters.add(new DomainType("service", Arrays.asList(WPS_SERVICE)));
        gcParameters.add(new DomainType("Acceptversions", Arrays.asList(WPSVersion.v200.getCode())));
        gcParameters.add(new DomainType("AcceptFormats", Arrays.asList("text/xml")));
        final Operation getCapabilities = new Operation(getAndPost, gcParameters, null, null, "GetCapabilities");
        operations.add(getCapabilities);

        final List<DomainType> dpParameters = new ArrayList<>();
        dpParameters.add(new DomainType("service", Arrays.asList(WPS_SERVICE)));
        dpParameters.add(new DomainType("version", Arrays.asList(WPSVersion.v200.getCode())));
        final Operation describeProcess =  new Operation(getAndPost, dpParameters, null, null, "DescribeProcess");
        operations.add(describeProcess);

        final List<DomainType> eParameters = new ArrayList<>();
        eParameters.add(new DomainType("service", Arrays.asList(WPS_SERVICE)));
        eParameters.add(new DomainType("version", Arrays.asList(WPSVersion.v200.getCode())));
        final Operation execute = new Operation(onlyPost, eParameters, null, null, "Execute");
        operations.add(execute);

        final List<DomainType> dsParameters = new ArrayList<>();
        dsParameters.add(new DomainType("service", Arrays.asList(WPS_SERVICE)));
        dsParameters.add(new DomainType("version", Arrays.asList(WPSVersion.v200.getCode())));
        final Operation dismiss = new Operation(getAndPost, dsParameters, null, null, "Dismiss");
        operations.add(dismiss);

        final List<DomainType> constraints = new ArrayList<>();
        constraints.add(new DomainType("PostEncoding", Arrays.asList("XML")));

        OPERATIONS_METADATA.put(WPSVersion.v200, new OperationsMetadata(operations, null, constraints, null));
    }

    /**
     * Supported CRS.
     */
    public static final DataDescription WPS_SUPPORTED_CRS ;

    static {
        List<SupportedCRS> supportedcrs = new ArrayList<>();
        supportedcrs.add(new SupportedCRS(DEFAULT_CRS.get(0), true));
        for (int i = 1; i < DEFAULT_CRS.size(); i++) {
            supportedcrs.add(new SupportedCRS(DEFAULT_CRS.get(i)));
        }
        Format xml = new Format(WPSMimeType.TEXT_XML.val(), true);
        WPS_SUPPORTED_CRS = new BoundingBoxData(Arrays.asList(xml), supportedcrs);
    }

    /**
     * GML VERSION CRS.
     */
    public static final Map<String, String> GML_VERSION = new HashMap<>();

    static {
        GML_VERSION.put("1.0.0", "3.1.1");
        GML_VERSION.put("2.0.0", "3.2.1");
    }

    /**
     * Generates the base capabilities for a WMS from the service metadata.
     *
     * @param metadata the service metadata
     * @return the service base capabilities
     */
    public static Capabilities createCapabilities(final String version, final Details metadata) {
        ensureNonNull("metadata", metadata);
        ensureNonNull("version",  version);

        final Contact currentContact = metadata.getServiceContact();
        final AccessConstraint constraint = metadata.getServiceConstraints();

        final ServiceIdentification servIdent;
        if (constraint == null) {
            servIdent = new ServiceIdentification(
                    new LanguageStringType(metadata.getName()),
                    new LanguageStringType(metadata.getDescription()),
                    new KeywordsType(metadata.getKeywords()),
                    new CodeType(WPS_SERVICE),
                    metadata.getVersions(),
                    null, new ArrayList<>());
        } else {
            servIdent = new ServiceIdentification(
                    new LanguageStringType(metadata.getName()),
                    new LanguageStringType(metadata.getDescription()),
                    new KeywordsType(metadata.getKeywords()),
                    new CodeType(WPS_SERVICE),
                    metadata.getVersions(),
                    constraint.getFees(),
                    Arrays.asList(constraint.getAccessConstraint()));
        }

        // Create provider part.
        OnlineResourceType orgUrl = null;
        final ResponsiblePartySubsetType responsible;
        final ServiceProvider servProv;
        if (currentContact != null) {
            ContactType contact = new ContactType(currentContact.getPhone(), currentContact.getFax(),
                    currentContact.getEmail(), currentContact.getAddress(), currentContact.getCity(), currentContact.getState(),
                    currentContact.getZipCode(), currentContact.getCountry(), currentContact.getHoursOfService(), currentContact.getContactInstructions());
            responsible = new ResponsiblePartySubsetType(currentContact.getFullname(), currentContact.getPosition(), contact, null);

            if (currentContact.getUrl() != null) {
                orgUrl = new OnlineResourceType(currentContact.getUrl());
            }
            servProv = new ServiceProvider(currentContact.getOrganisation(), orgUrl, responsible);
        } else {
            responsible =new ResponsiblePartySubsetType(null, null, null, null);
            servProv = new ServiceProvider(null, orgUrl, responsible);
        }

        // Create capabilities base.
        return new Capabilities(servIdent, servProv, null, null, null, null, null);
    }
}
