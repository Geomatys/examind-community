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
package org.constellation.metadata.core;

import java.math.BigInteger;
import org.constellation.dto.contact.AccessConstraint;
import org.constellation.dto.contact.Contact;
import org.constellation.dto.contact.Details;
import org.constellation.ws.MimeType;
import org.geotoolkit.csw.xml.AbstractCapabilities;
import org.geotoolkit.csw.xml.CswXmlFactory;
import org.geotoolkit.ogc.xml.v110.ComparisonOperatorsType;
import org.geotoolkit.ogc.xml.v110.SpatialOperatorType;
import org.geotoolkit.ogc.xml.v110.SpatialOperatorsType;
import org.geotoolkit.ows.xml.AbstractContact;
import org.geotoolkit.ows.xml.AbstractDCP;
import org.geotoolkit.ows.xml.AbstractDomain;
import org.geotoolkit.ows.xml.AbstractOnlineResourceType;
import org.geotoolkit.ows.xml.AbstractOperation;
import org.geotoolkit.ows.xml.AbstractOperationsMetadata;
import org.geotoolkit.ows.xml.AbstractResponsiblePartySubset;
import org.geotoolkit.ows.xml.AbstractServiceIdentification;
import org.geotoolkit.ows.xml.AbstractServiceProvider;
import org.geotoolkit.ows.xml.OWSXmlFactory;
import org.geotoolkit.filter.capability.Operator;
import org.geotoolkit.filter.capability.SpatialOperator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.constellation.api.ServiceConstants.GET_CAPABILITIES;
import static org.geotoolkit.gml.xml.v311.ObjectFactory._Envelope_QNAME;
import static org.geotoolkit.gml.xml.v311.ObjectFactory._LineString_QNAME;
import static org.geotoolkit.gml.xml.v311.ObjectFactory._Point_QNAME;
import static org.geotoolkit.gml.xml.v311.ObjectFactory._Polygon_QNAME;
import org.geotoolkit.ops.xml.v110.CompleteQueryType;
import org.geotoolkit.ops.xml.v110.OpenSearchDescription;
import org.geotoolkit.ops.xml.v110.Url;
import org.geotoolkit.opsp.xml.v100.Parameter;
import org.geotoolkit.filter.capability.FilterCapabilities;
import org.w3._2005.atom.DateTimeType;
import org.w3._2005.atom.FeedType;
import org.w3._2005.atom.PersonType;

/**
 * CSW constants.
 *
 * @version $Id$
 * @author Guilhem Legal (Geomatys)
 */
public abstract class CSWConstants {

    public static Logger LOGGER = Logger.getLogger("org.constellation.metadata.core");
    /**
     * Request parameters.
     */
    public static final String CSW_202_VERSION = "2.0.2";
    public static final String CSW = "CSW";

    public static final String REQUEST_ID = "REQUESTID";
    public static final String OUTPUT_FORMAT = "OUTPUTFORMAT";
    public static final String RESULT_TYPE = "RESULTTYPE";
    public static final String START_POSITION = "STARTPOSITION";
    public static final String MAX_RECORDS = "MAXRECORDS";
    public static final String CONSTRAINT = "CONSTRAINT";
    public static final String CONSTRAINT_LANGUAGE = "CONSTRAINTLANGUAGE";
    public static final String CONSTRAINT_LANGUAGE_VERSION = "CONSTRAINT_LANGUAGE_VERSION";

    public static final String OUTPUT_SCHEMA = "outputSchema";
    public static final String TYPENAMES = "TypeNames";
    public static final String FILTER_CAPABILITIES = "Filter_Capabilities";
    public static final String PARAMETERNAME = "parameterName";
    public static final String TRANSACTION_TYPE = "TransactionType";
    public static final String SOURCE = "Source";
    public static final String ALL = "All";
    public static final String NAMESPACE = "namespace";

    public static final String CSW_SCHEMA_LOCATION = "http://www.opengis.net/cat/csw/2.0.2 http://schemas.opengis.net/csw/2.0.2/csw.xsd";

    public static final String ISO_SCHEMA_LOCATION = "http://www.isotc211.org/2005/gmd http://schemas.opengis.net/iso/19139/20070417/gmd/gmd.xsd http://www.isotc211.org/2005/gmx http://schemas.opengis.net/iso/19139/20070417/gmx/gmx.xsd";

    // TODO those 3 namespace must move to geotk Namespace class
    public static final String EBRIM_25 = "urn:oasis:names:tc:ebxml-regrep:rim:xsd:2.5";
    public static final String EBRIM_30 = "urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0";

    /**
     * A list of supported MIME type.
     */
    public static final List<String> ACCEPTED_OUTPUT_FORMATS;
    static {
        ACCEPTED_OUTPUT_FORMATS = new ArrayList<>();
        ACCEPTED_OUTPUT_FORMATS.add(MimeType.TEXT_XML);
        ACCEPTED_OUTPUT_FORMATS.add(MimeType.APPLICATION_XML);
        ACCEPTED_OUTPUT_FORMATS.add(MimeType.TEXT_HTML);
        ACCEPTED_OUTPUT_FORMATS.add(MimeType.TEXT_PLAIN);
        ACCEPTED_OUTPUT_FORMATS.add(MimeType.APP_ATOM);
    }

    /**
     * Error message
     */

    public static final String NOT_EXIST = " does not exist";

    public static final String MALFORMED = " is malformed";

    public static final Map<String, AbstractOperationsMetadata> OPERATIONS_METADATA = new HashMap<>();
    static {
        final List<AbstractDCP> getAndPost = new ArrayList<>();
        getAndPost.add(OWSXmlFactory.buildDCP("1.0.0", "somURL", "someURL"));

        final List<AbstractDCP> onlyPost = new ArrayList<>();
        onlyPost.add(OWSXmlFactory.buildDCP("1.0.0", "somURL", "someURL"));

        final List<AbstractOperation> operations = new ArrayList<>();

        final List<AbstractDomain> gcParameters = new ArrayList<>();
        gcParameters.add(OWSXmlFactory.buildDomain("1.0.0", "sections", Arrays.asList("All", "ServiceIdentification", "ServiceProvider", "OperationsMetadata", "Filter_Capabilities")));
        gcParameters.add(OWSXmlFactory.buildDomain("1.0.0", "Version",  Arrays.asList("2.0.2")));
        gcParameters.add(OWSXmlFactory.buildDomain("1.0.0", "Service",  Arrays.asList("CSW")));

        final AbstractOperation getCapabilities = OWSXmlFactory.buildOperation("1.0.0", getAndPost, gcParameters, null, GET_CAPABILITIES);
        operations.add(getCapabilities);

        final List<AbstractDomain> grParameters = new ArrayList<>();
        grParameters.add(OWSXmlFactory.buildDomain("1.0.0", "Version", Arrays.asList("2.0.2")));
        grParameters.add(OWSXmlFactory.buildDomain("1.0.0", "Service", Arrays.asList("CSW")));
        grParameters.add(OWSXmlFactory.buildDomain("1.0.0", "TypeNames", Arrays.asList("gmd:MD_Metadata", "csw:Record")));
        grParameters.add(OWSXmlFactory.buildDomain("1.0.0", "outputFormat", Arrays.asList("text/xml", "application/xml")));
        grParameters.add(OWSXmlFactory.buildDomain("1.0.0", "outputSchema", Arrays.asList("http://www.opengis.net/cat/csw/2.0.2", "http://www.isotc211.org/2005/gmd")));
        grParameters.add(OWSXmlFactory.buildDomain("1.0.0", "resultType", Arrays.asList("hits", "results", "validate")));
        grParameters.add(OWSXmlFactory.buildDomain("1.0.0", "ElementSetName", Arrays.asList("brief", "summary", "full")));
        grParameters.add(OWSXmlFactory.buildDomain("1.0.0", "CONSTRAINTLANGUAGE", Arrays.asList("Filter", "CQL")));

        final List<AbstractDomain> grConstraints = new ArrayList<>();

        final List<String> supportedISOQueryable = new ArrayList<>();
        supportedISOQueryable.add("RevisionDate");
        supportedISOQueryable.add("AlternateTitle");
        supportedISOQueryable.add("CreationDate");
        supportedISOQueryable.add("PublicationDate");
        supportedISOQueryable.add("OrganisationName");
        supportedISOQueryable.add("HasSecurityConstraints");
        supportedISOQueryable.add("Language");
        supportedISOQueryable.add("ResourceIdentifier");
        supportedISOQueryable.add("ParentIdentifier");
        supportedISOQueryable.add("KeywordType");
        supportedISOQueryable.add("TopicCategory");
        supportedISOQueryable.add("ResourceLanguage");
        supportedISOQueryable.add("GeographicDescriptionCode");
        supportedISOQueryable.add("DistanceValue");
        supportedISOQueryable.add("DistanceUOM");
        supportedISOQueryable.add("TempExtent_begin");
        supportedISOQueryable.add("TempExtent_end");
        supportedISOQueryable.add("ServiceType");
        supportedISOQueryable.add("ServiceTypeVersion");
        supportedISOQueryable.add("Operation");
        supportedISOQueryable.add("CouplingType");
        supportedISOQueryable.add("OperatesOn");
        supportedISOQueryable.add("Denominator");
        supportedISOQueryable.add("OperatesOnIdentifier");
        supportedISOQueryable.add("OperatesOnWithOpName");

        grConstraints.add(OWSXmlFactory.buildDomain("1.0.0", "SupportedISOQueryables", supportedISOQueryable));
        grConstraints.add(OWSXmlFactory.buildDomain("1.0.0", "AdditionalQueryables", Arrays.asList("HierarchyLevelName")));

        final AbstractOperation getRecords = OWSXmlFactory.buildOperation("1.0.0", getAndPost, grParameters, grConstraints, "GetRecords");
        operations.add(getRecords);

        final List<AbstractDomain> grbParameters = new ArrayList<>();
        grbParameters.add(OWSXmlFactory.buildDomain("1.0.0", "Version", Arrays.asList("2.0.2")));
        grbParameters.add(OWSXmlFactory.buildDomain("1.0.0", "Service", Arrays.asList("CSW")));
        grbParameters.add(OWSXmlFactory.buildDomain("1.0.0", "ElementSetName", Arrays.asList("brief", "summary", "full")));
        grbParameters.add(OWSXmlFactory.buildDomain("1.0.0", "outputSchema", Arrays.asList("http://www.opengis.net/cat/csw/2.0.2", "http://www.isotc211.org/2005/gmd")));
        grbParameters.add(OWSXmlFactory.buildDomain("1.0.0", "outputFormat", Arrays.asList("text/xml", "application/xml")));

        final AbstractOperation getRecordById = OWSXmlFactory.buildOperation("1.0.0", getAndPost, grbParameters, null, "GetRecordById");
        operations.add(getRecordById);

        final List<AbstractDomain> drParameters = new ArrayList<>();
        drParameters.add(OWSXmlFactory.buildDomain("1.0.0", "Version", Arrays.asList("2.0.2")));
        drParameters.add(OWSXmlFactory.buildDomain("1.0.0", "Service", Arrays.asList("CSW")));
        drParameters.add(OWSXmlFactory.buildDomain("1.0.0", "TypeName", Arrays.asList("gmd:MD_Metadata", "csw:Record")));
        drParameters.add(OWSXmlFactory.buildDomain("1.0.0", "SchemaLanguage", Arrays.asList("http://www.w3.org/XML/Schema", "XMLSCHEMA")));
        drParameters.add(OWSXmlFactory.buildDomain("1.0.0", "outputFormat", Arrays.asList("text/xml", "application/xml")));

        final AbstractOperation describeRecord = OWSXmlFactory.buildOperation("1.0.0", getAndPost, drParameters, null, "DescribeRecord");
        operations.add(describeRecord);


        final List<AbstractDomain> gdParameters = new ArrayList<>();
        gdParameters.add(OWSXmlFactory.buildDomain("1.0.0", "Version", Arrays.asList("2.0.2")));
        gdParameters.add(OWSXmlFactory.buildDomain("1.0.0", "Service", Arrays.asList("CSW")));

        final AbstractOperation getDomain = OWSXmlFactory.buildOperation("1.0.0", getAndPost, gdParameters, null, "GetDomain");
        operations.add(getDomain);

        final List<AbstractDomain> tParameters = new ArrayList<>();
        tParameters.add(OWSXmlFactory.buildDomain("1.0.0", "Version", Arrays.asList("2.0.2")));
        tParameters.add(OWSXmlFactory.buildDomain("1.0.0", "Service", Arrays.asList("CSW")));
        tParameters.add(OWSXmlFactory.buildDomain("1.0.0", "ResourceType", Arrays.asList("toUpdate")));

        final AbstractOperation transaction = OWSXmlFactory.buildOperation("1.0.0", onlyPost, tParameters, null, "Transaction");
        operations.add(transaction);

        final List<AbstractDomain> hParameters = new ArrayList<>();
        hParameters.add(OWSXmlFactory.buildDomain("1.0.0", "Version", Arrays.asList("2.0.2")));
        hParameters.add(OWSXmlFactory.buildDomain("1.0.0", "Service", Arrays.asList("CSW")));
        hParameters.add(OWSXmlFactory.buildDomain("1.0.0", "ResourceType", Arrays.asList("toUpdate")));

        final AbstractOperation harvest = OWSXmlFactory.buildOperation("1.0.0", onlyPost, hParameters, null, "Harvest");
        operations.add(harvest);

        final List<AbstractDomain> parameters = new ArrayList<>();
        parameters.add(OWSXmlFactory.buildDomain("1.0.0", "service", Arrays.asList("CSW")));
        parameters.add(OWSXmlFactory.buildDomain("1.0.0", "version", Arrays.asList("2.0.2")));

        final List<AbstractDomain> constraints = new ArrayList<>();
        constraints.add(OWSXmlFactory.buildDomain("1.0.0", "PostEncoding", Arrays.asList("XML")));

        OPERATIONS_METADATA.put("2.0.2", OWSXmlFactory.buildOperationsMetadata("1.0.0", operations, parameters, constraints, null));
    }

    static {
        final List<AbstractDCP> getAndPost = new ArrayList<>();
        getAndPost.add(OWSXmlFactory.buildDCP("2.0.0", "somURL", "someURL"));

        final List<AbstractDCP> onlyPost = new ArrayList<>();
        onlyPost.add(OWSXmlFactory.buildDCP("2.0.0", "somURL", "someURL"));

        final List<AbstractOperation> operations = new ArrayList<>();

        final List<AbstractDomain> gcParameters = new ArrayList<>();
        gcParameters.add(OWSXmlFactory.buildDomain("2.0.0", "sections", Arrays.asList("All", "ServiceIdentification", "ServiceProvider", "OperationsMetadata", "Filter_Capabilities")));
        gcParameters.add(OWSXmlFactory.buildDomain("2.0.0", "Version",  Arrays.asList("3.0.0")));
        gcParameters.add(OWSXmlFactory.buildDomain("2.0.0", "Service",  Arrays.asList("CSW")));

        final AbstractOperation getCapabilities = OWSXmlFactory.buildOperation("2.0.0", getAndPost, gcParameters, null, GET_CAPABILITIES);
        operations.add(getCapabilities);

        final List<AbstractDomain> grParameters = new ArrayList<>();
        grParameters.add(OWSXmlFactory.buildDomain("2.0.0", "Version", Arrays.asList("3.0.0")));
        grParameters.add(OWSXmlFactory.buildDomain("2.0.0", "Service", Arrays.asList("CSW")));
        grParameters.add(OWSXmlFactory.buildDomain("2.0.0", "TypeNames", Arrays.asList("gmd:MD_Metadata", "csw:Record")));
        grParameters.add(OWSXmlFactory.buildDomain("2.0.0", "outputFormat", Arrays.asList("text/xml", "application/xml")));
        grParameters.add(OWSXmlFactory.buildDomain("2.0.0", "outputSchema", Arrays.asList("http://www.opengis.net/cat/csw/2.0.2", "http://www.isotc211.org/2005/gmd")));
        grParameters.add(OWSXmlFactory.buildDomain("2.0.0", "resultType", Arrays.asList("hits", "results", "validate")));
        grParameters.add(OWSXmlFactory.buildDomain("2.0.0", "ElementSetName", Arrays.asList("brief", "summary", "full")));
        grParameters.add(OWSXmlFactory.buildDomain("2.0.0", "CONSTRAINTLANGUAGE", Arrays.asList("Filter", "CQL")));

        final List<AbstractDomain> grConstraints = new ArrayList<>();
        grConstraints.add(OWSXmlFactory.buildDomain("2.0.0", "OpenSearchDescriptionDocument", Arrays.asList("{openSearchURL}")));

        final List<String> supportedISOQueryable = new ArrayList<>();
        supportedISOQueryable.add("RevisionDate");
        supportedISOQueryable.add("AlternateTitle");
        supportedISOQueryable.add("CreationDate");
        supportedISOQueryable.add("PublicationDate");
        supportedISOQueryable.add("OrganisationName");
        supportedISOQueryable.add("HasSecurityConstraints");
        supportedISOQueryable.add("Language");
        supportedISOQueryable.add("ResourceIdentifier");
        supportedISOQueryable.add("ParentIdentifier");
        supportedISOQueryable.add("KeywordType");
        supportedISOQueryable.add("TopicCategory");
        supportedISOQueryable.add("ResourceLanguage");
        supportedISOQueryable.add("GeographicDescriptionCode");
        supportedISOQueryable.add("DistanceValue");
        supportedISOQueryable.add("DistanceUOM");
        supportedISOQueryable.add("TempExtent_begin");
        supportedISOQueryable.add("TempExtent_end");
        supportedISOQueryable.add("ServiceType");
        supportedISOQueryable.add("ServiceTypeVersion");
        supportedISOQueryable.add("Operation");
        supportedISOQueryable.add("CouplingType");
        supportedISOQueryable.add("OperatesOn");
        supportedISOQueryable.add("Denominator");
        supportedISOQueryable.add("OperatesOnIdentifier");
        supportedISOQueryable.add("OperatesOnWithOpName");

        grConstraints.add(OWSXmlFactory.buildDomain("2.0.0", "SupportedISOQueryables", supportedISOQueryable));
        grConstraints.add(OWSXmlFactory.buildDomain("2.0.0", "AdditionalQueryables", Arrays.asList("HierarchyLevelName")));

        final AbstractOperation getRecords = OWSXmlFactory.buildOperation("2.0.0", getAndPost, grParameters, grConstraints, "GetRecords");
        operations.add(getRecords);

        final List<AbstractDomain> grbParameters = new ArrayList<>();
        grbParameters.add(OWSXmlFactory.buildDomain("2.0.0", "Version", Arrays.asList("3.0.0")));
        grbParameters.add(OWSXmlFactory.buildDomain("2.0.0", "Service", Arrays.asList("CSW")));
        grbParameters.add(OWSXmlFactory.buildDomain("2.0.0", "ElementSetName", Arrays.asList("brief", "summary", "full")));
        grbParameters.add(OWSXmlFactory.buildDomain("2.0.0", "outputSchema", Arrays.asList("http://www.opengis.net/cat/csw/2.0.2", "http://www.isotc211.org/2005/gmd")));
        grbParameters.add(OWSXmlFactory.buildDomain("2.0.0", "outputFormat", Arrays.asList("text/xml", "application/xml")));

        final AbstractOperation getRecordById = OWSXmlFactory.buildOperation("2.0.0", getAndPost, grbParameters, null, "GetRecordById");
        operations.add(getRecordById);

        final List<AbstractDomain> drParameters = new ArrayList<>();
        drParameters.add(OWSXmlFactory.buildDomain("2.0.0", "Version", Arrays.asList("3.0.0")));
        drParameters.add(OWSXmlFactory.buildDomain("2.0.0", "Service", Arrays.asList("CSW")));
        drParameters.add(OWSXmlFactory.buildDomain("2.0.0", "TypeName", Arrays.asList("gmd:MD_Metadata", "csw:Record")));
        drParameters.add(OWSXmlFactory.buildDomain("2.0.0", "SchemaLanguage", Arrays.asList("http://www.w3.org/XML/Schema", "XMLSCHEMA")));
        drParameters.add(OWSXmlFactory.buildDomain("2.0.0", "outputFormat", Arrays.asList("text/xml", "application/xml")));

        final AbstractOperation describeRecord = OWSXmlFactory.buildOperation("2.0.0", getAndPost, drParameters, null, "DescribeRecord");
        operations.add(describeRecord);


        final List<AbstractDomain> gdParameters = new ArrayList<>();
        gdParameters.add(OWSXmlFactory.buildDomain("2.0.0", "Version", Arrays.asList("3.0.0")));
        gdParameters.add(OWSXmlFactory.buildDomain("2.0.0", "Service", Arrays.asList("CSW")));

        final AbstractOperation getDomain = OWSXmlFactory.buildOperation("2.0.0", getAndPost, gdParameters, null, "GetDomain");
        operations.add(getDomain);

        final List<AbstractDomain> tParameters = new ArrayList<>();
        tParameters.add(OWSXmlFactory.buildDomain("2.0.0", "Version", Arrays.asList("3.0.0")));
        tParameters.add(OWSXmlFactory.buildDomain("2.0.0", "Service", Arrays.asList("CSW")));
        tParameters.add(OWSXmlFactory.buildDomain("2.0.0", "ResourceType", Arrays.asList("toUpdate")));

        final AbstractOperation transaction = OWSXmlFactory.buildOperation("2.0.0", onlyPost, tParameters, null, "Transaction");
        operations.add(transaction);

        final List<AbstractDomain> hParameters = new ArrayList<>();
        hParameters.add(OWSXmlFactory.buildDomain("2.0.0", "Version", Arrays.asList("3.0.0")));
        hParameters.add(OWSXmlFactory.buildDomain("2.0.0", "Service", Arrays.asList("CSW")));
        hParameters.add(OWSXmlFactory.buildDomain("2.0.0", "ResourceType", Arrays.asList("toUpdate")));

        final AbstractOperation harvest = OWSXmlFactory.buildOperation("2.0.0", onlyPost, hParameters, null, "Harvest");
        operations.add(harvest);

        final List<AbstractDomain> parameters = new ArrayList<>();
        parameters.add(OWSXmlFactory.buildDomain("2.0.0", "service", Arrays.asList("CSW")));
        parameters.add(OWSXmlFactory.buildDomain("2.0.0", "version", Arrays.asList("3.0.0")));

        final List<AbstractDomain> constraints = new ArrayList<>();
        constraints.add(OWSXmlFactory.buildDomain("2.0.0", "PostEncoding", Arrays.asList("XML")));

        OPERATIONS_METADATA.put("3.0.0", OWSXmlFactory.buildOperationsMetadata("2.0.0", operations, parameters, constraints, null));
    }

    public static final Map<String, FilterCapabilities> CSW_FILTER_CAPABILITIES = new HashMap<>();

    static {
        org.geotoolkit.ogc.xml.v110.FilterCapabilities csw202FC = new org.geotoolkit.ogc.xml.v110.FilterCapabilities();

        final org.geotoolkit.ogc.xml.v110.GeometryOperandsType geom = new org.geotoolkit.ogc.xml.v110.GeometryOperandsType(Arrays.asList(_Envelope_QNAME, _Point_QNAME, _LineString_QNAME, _Polygon_QNAME));
        final SpatialOperator[] spaOps = new SpatialOperator[11];
        spaOps[0]  = new SpatialOperatorType("BBOX", null);
        spaOps[1]  = new SpatialOperatorType("BEYOND", null);
        spaOps[2]  = new SpatialOperatorType("CONTAINS", null);
        spaOps[3]  = new SpatialOperatorType("CROSSES", null);
        spaOps[4]  = new SpatialOperatorType("DISJOINT", null);
        spaOps[5]  = new SpatialOperatorType("D_WITHIN", null);
        spaOps[6]  = new SpatialOperatorType("EQUALS", null);
        spaOps[7]  = new SpatialOperatorType("INTERSECTS", null);
        spaOps[8]  = new SpatialOperatorType("OVERLAPS", null);
        spaOps[9]  = new SpatialOperatorType("TOUCHES", null);
        spaOps[10] = new SpatialOperatorType("WITHIN", null);


        final SpatialOperatorsType spaOp = new SpatialOperatorsType(spaOps);
        final org.geotoolkit.ogc.xml.v110.SpatialCapabilitiesType spatial = new org.geotoolkit.ogc.xml.v110.SpatialCapabilitiesType(geom, spaOp);
        csw202FC.setSpatialCapabilities(spatial);

        final Operator[] compOps = new Operator[] {
            Operator.BETWEEN,
            Operator.EQUAL_TO,
            Operator.NOT_EQUAL_TO,
            Operator.LESS_THAN,
            Operator.LESS_THAN_EQUAL_TO,
            Operator.GREATER_THAN,
            Operator.GREATER_THAN_EQUAL_TO,
            Operator.LIKE,
            Operator.NULL_CHECK
        };
        final ComparisonOperatorsType compOp = new ComparisonOperatorsType(compOps);
        final org.geotoolkit.ogc.xml.v110.ScalarCapabilitiesType scalar = new org.geotoolkit.ogc.xml.v110.ScalarCapabilitiesType(compOp, null, true);

        csw202FC.setScalarCapabilities(scalar);

        final org.geotoolkit.ogc.xml.v110.IdCapabilitiesType id = new org.geotoolkit.ogc.xml.v110.IdCapabilitiesType(false, true);
        csw202FC.setIdCapabilities(id);

        CSW_FILTER_CAPABILITIES.put("2.0.2", csw202FC);
    }

    static {
        org.geotoolkit.ogc.xml.v200.FilterCapabilities csw300FC = new org.geotoolkit.ogc.xml.v200.FilterCapabilities();

        final org.geotoolkit.ogc.xml.v200.GeometryOperandsType geom = new org.geotoolkit.ogc.xml.v200.GeometryOperandsType(Arrays.asList(_Envelope_QNAME, _Point_QNAME, _LineString_QNAME, _Polygon_QNAME));
        final SpatialOperator[] spaOps = new SpatialOperator[11];
        spaOps[0]  = new SpatialOperatorType("BBOX", null);
        spaOps[1]  = new SpatialOperatorType("BEYOND", null);
        spaOps[2]  = new SpatialOperatorType("CONTAINS", null);
        spaOps[3]  = new SpatialOperatorType("CROSSES", null);
        spaOps[4]  = new SpatialOperatorType("DISJOINT", null);
        spaOps[5]  = new SpatialOperatorType("D_WITHIN", null);
        spaOps[6]  = new SpatialOperatorType("EQUALS", null);
        spaOps[7]  = new SpatialOperatorType("INTERSECTS", null);
        spaOps[8]  = new SpatialOperatorType("OVERLAPS", null);
        spaOps[9]  = new SpatialOperatorType("TOUCHES", null);
        spaOps[10] = new SpatialOperatorType("WITHIN", null);


        final org.geotoolkit.ogc.xml.v200.SpatialOperatorsType spaOp = new org.geotoolkit.ogc.xml.v200.SpatialOperatorsType(spaOps);
        final org.geotoolkit.ogc.xml.v200.SpatialCapabilitiesType spatial = new org.geotoolkit.ogc.xml.v200.SpatialCapabilitiesType(geom, spaOp);
        csw300FC.setSpatialCapabilities(spatial);

        final Operator[] compOps = new Operator[9];
        compOps[0] = new org.geotoolkit.ogc.xml.v200.ComparisonOperatorType("BETWEEN");
        compOps[1] = new org.geotoolkit.ogc.xml.v200.ComparisonOperatorType("EQUAL_TO");
        compOps[2] = new org.geotoolkit.ogc.xml.v200.ComparisonOperatorType("NOT_EQUAL_TO");
        compOps[3] = new org.geotoolkit.ogc.xml.v200.ComparisonOperatorType("LESS_THAN");
        compOps[4] = new org.geotoolkit.ogc.xml.v200.ComparisonOperatorType("LESS_THAN_EQUAL_TO");
        compOps[5] = new org.geotoolkit.ogc.xml.v200.ComparisonOperatorType("GREATER_THAN");
        compOps[6] = new org.geotoolkit.ogc.xml.v200.ComparisonOperatorType("GREATER_THAN_EQUAL_TO");
        compOps[7] = new org.geotoolkit.ogc.xml.v200.ComparisonOperatorType("LIKE");
        compOps[8] = new org.geotoolkit.ogc.xml.v200.ComparisonOperatorType("NULL_CHECK");
        final org.geotoolkit.ogc.xml.v200.ComparisonOperatorsType compOp = new org.geotoolkit.ogc.xml.v200.ComparisonOperatorsType(compOps);
        final org.geotoolkit.ogc.xml.v200.ScalarCapabilitiesType scalar = new org.geotoolkit.ogc.xml.v200.ScalarCapabilitiesType(compOp, true);

        csw300FC.setScalarCapabilities(scalar);

        //final org.geotoolkit.ogc.xml.v200.IdCapabilitiesType id = new org.geotoolkit.ogc.xml.v200.IdCapabilitiesType(false, true);
        //csw300FC.setIdCapabilities(id);

        CSW_FILTER_CAPABILITIES.put("3.0.0", csw300FC);
    }

    /**
     * Generates the base capabilities for a WMS from the service metadata.
     *
     * @param metadata the service metadata
     * @return the service base capabilities
     */
    public static AbstractCapabilities createCapabilities(final String version, final Details metadata) {
        ensureNonNull("metadata", metadata);
        ensureNonNull("version",  version);

        final Contact currentContact = metadata.getServiceContact();
        final AccessConstraint constraint = metadata.getServiceConstraints();

        final String owsVersion = CswXmlFactory.getOwsVersion(version);

        final AbstractServiceIdentification servIdent;
        if (constraint != null) {
            servIdent = OWSXmlFactory.buildServiceIdentification(owsVersion, metadata.getName(), metadata.getDescription(),
                    metadata.getKeywords(), "CSW", metadata.getVersions(), constraint.getFees(),
                    Arrays.asList(constraint.getAccessConstraint()));
        } else {
            servIdent = OWSXmlFactory.buildServiceIdentification(owsVersion, metadata.getName(), metadata.getDescription(),
                    metadata.getKeywords(), "CSW", metadata.getVersions(), null, new ArrayList<>());
        }

        final AbstractServiceProvider servProv;
        if (currentContact != null) {
            // Create provider part.
            final AbstractContact contact = OWSXmlFactory.buildContact(owsVersion, currentContact.getPhone(), currentContact.getFax(),
                    currentContact.getEmail(), currentContact.getAddress(), currentContact.getCity(), currentContact.getState(),
                    currentContact.getZipCode(), currentContact.getCountry(), currentContact.getHoursOfService(), currentContact.getContactInstructions());

            final AbstractResponsiblePartySubset responsible = OWSXmlFactory.buildResponsiblePartySubset(owsVersion, currentContact.getFullname(), currentContact.getPosition(), contact, null);


            AbstractOnlineResourceType orgUrl = null;
            if (currentContact.getUrl() != null) {
                orgUrl = OWSXmlFactory.buildOnlineResource(owsVersion, currentContact.getUrl());
            }
            servProv = OWSXmlFactory.buildServiceProvider(owsVersion, currentContact.getOrganisation(), orgUrl, responsible);
        } else {
            servProv = OWSXmlFactory.buildServiceProvider(owsVersion, "", null, null);
        }

        // Create capabilities base.
        return CswXmlFactory.createCapabilities(version, servIdent, servProv, null, null, null);
    }

    public static FeedType createFeed(final String serviceUrl, final Details metadata, String osUrl) {
        final Contact currentContact = metadata.getServiceContact();
        PersonType author = null;
        if (currentContact != null) {
            author = new PersonType(currentContact.getFullname(), currentContact.getEmail(), currentContact.getUrl());
        }
        FeedType feed = new FeedType(serviceUrl, "Examind Opensearch service", author, osUrl);
        try {
            feed.addUpdated(new DateTimeType(DatatypeFactory.newInstance().newXMLGregorianCalendar((GregorianCalendar)GregorianCalendar.getInstance())));
        } catch (DatatypeConfigurationException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage());
        }
        return feed;
    }

    public final static Map<String, List<String>> ISO_BRIEF_FIELDS = new HashMap<>();
    static {
        ISO_BRIEF_FIELDS.put("identifier", Arrays.asList("/gmd:MD_Metadata/gmd:fileIdentifier/gco:CharacterString",
                                                         "/csw:Record/dc:identifier",
                                                         "/dif:DIF/dif:Entry_ID/dif:Short_Name",
                                                         "/gfc:FC_FeatureCatalogue/@id"));
        ISO_BRIEF_FIELDS.put("title",      Arrays.asList("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString",
                                                         "/csw:Record/dc:title",
                                                         "/dif:DIF/dif:Summary/dif:Abstract",
                                                         "/gfc:FC_FeatureCatalogue/gfc:featureType/gfc:FC_FeatureType/gfc:typeName/gco:LocalName"));
        ISO_BRIEF_FIELDS.put("date",       Arrays.asList("/gmd:MD_Metadata/gmd:dateStamp/gco:DateTime", "/gmd:MD_Metadata/gmd:dateStamp/gco:Date",
                                                         "/csw:Record/dc:modified",
                                                         "/dif:DIF/dif:Metadata_Dates/dif:Metadata_Last_Revision"));
        ISO_BRIEF_FIELDS.put("creator",    Arrays.asList("/gmd:MD_Metadata/gmd:contact/gmd:CI_ResponsibleParty/gmd:organisationName/gco:CharacterString",
                                                         "/csw:Record/dc:creator",
                                                         "/dif:DIF/dif:Organization/dif:Organization_Name/dif:Short_Name"));
    }

    public static final OpenSearchDescription OS_DESCRIPTION;
    static {
        OS_DESCRIPTION = new OpenSearchDescription("Examind", "Provides interoperable access, following ISO/OGC interface guidelines, to various metadata.");
        // Search by bbox returning csw:Record (i.e. GetRecordsResponse)
        String type     = "application/xml";
        String template = "{cswUrl}/opensearch?service=CSW&version=3.0.0&"
                        + "q={searchTerms?}&"
                        + "maxRecords={count?}&"
                        + "startPosition={startIndex?}&"
                        + "bbox={geo:box?}&"
                        + "recordIds={geo:uid?}&"
                        + "geometry={geo:geometry?}&"
                        + "relation={geo:relation?}&"
                        + "lat={geo:lat?}&"
                        + "lon={geo:lon?}&"
                        + "radius={geo:radius?}&"
                        + "name={geo:name?}&"
                        + "startDate={time:start?}&"
                        + "endDate={time:end?}&"
                        + "trelation={time:relation?}&"
                        + "outputSchema={outputSchema?}&"
                        + "outputFormat=application/xml";
        Url cswURL = new Url(type, template);
        cswURL.setIndexOffset(new BigInteger("1"));
        cswURL.setPageOffset(new BigInteger("1"));

        List<Parameter> params = new ArrayList<>();
        Parameter param = new Parameter("q", "{searchTerms}",
                "Textual search in the title, abstract of keyword section of the metadata.  Surround with double quotes for exact match.");
        params.add(param);

        param = new Parameter("maxRecords", "{count}", "Number of results returned per page (default 10)");
        param.setMinInclusive(1);
        params.add(param);

        param = new Parameter("startPosition", "{startIndex}", null);
        param.setMinInclusive(1);
        params.add(param);

        param = new Parameter("bbox", "{geo:box}",
                "Region of Interest defined by 'west, south, east, north' coordinates of longitude, latitude, in decimal degrees (EPSG:4326)");
        params.add(param);

        param = new Parameter("recordIds", "{geo:uid}", "Metadata identifier");
        params.add(param);

        param = new Parameter("geometry", "{geo:geometry}", "Region of Interest defined in Well Known Text standard (WKT) with coordinates in decimal degrees (EPSG:4326)");
        params.add(param);

        param = new Parameter("relation", "{geo:relation}", "The spatial operator to apply using the value of the geometry parameter. (default value: Intersects)");
        param.addOption("Equals");
        param.addOption("Disjoint");
        param.addOption("Touches");
        param.addOption("Within");
        param.addOption("Overlaps");
        param.addOption("Crosses");
        param.addOption("Intersects");
        param.addOption("Contains");
        param.addOption("DWithin");
        param.addOption("Beyond");
        params.add(param);

        param = new Parameter("lat", "{geo:lat}", "Latitude expressed in decimal degrees (EPSG:4326) - should be used with geo:lon");
        param.setMinInclusive(-90);
        param.setMaxInclusive(90);
        params.add(param);

        param = new Parameter("lon", "{geo:lon}", "Longitude expressed in decimal degrees (EPSG:4326) - should be used with geo:lat");
        param.setMinInclusive(-90);
        param.setMaxInclusive(90);
        params.add(param);

        param = new Parameter("radius", "{geo:radius}", "Expressed in meters - should be used with geo:lon and geo:lat");
        param.setMinInclusive(1);
        params.add(param);

        param = new Parameter("name", "{geo:name}", "Location string e.g. Paris, France");
        params.add(param);

        param = new Parameter("startDate", "{time:start}", "Start date to be compared with the data acquisition time.");
        params.add(param);

        param = new Parameter("endDate", "{time:end}", "End date to be compared with the data acquisition time.");
        params.add(param);

        param = new Parameter("trelation", "{time:relation}", "The temporal operator to apply using the value of the time parameter. (default value: TEquals for single date, AnyInteracts for period)");
        param.addOption("After");
        param.addOption("Before");
        param.addOption("Begins");
        param.addOption("BegunBy");
        param.addOption("TContains");
        param.addOption("During");
        param.addOption("EndedBy");
        param.addOption("Ends");
        param.addOption("TEquals");
        param.addOption("Meets");
        param.addOption("MetBy");
        param.addOption("TOverlaps");
        param.addOption("OverlappedBy");
        param.addOption("AnyInteracts");
        params.add(param);

        Parameter outSchemParam = new Parameter("outputSchema", "{outputSchema}", "Used to indicate the schema of the output that is generated in response to a GetRecords request.");
        outSchemParam.addOption("http://www.opengis.net/cat/csw/3.0");
        outSchemParam.addOption("http://www.isotc211.org/2005/gmd");
        outSchemParam.addOption("http://www.opengis.net/cat/csw/2.0.2");
        outSchemParam.addOption("http://www.isotc211.org/2005/gfc");
        outSchemParam.addOption("http://gcmd.gsfc.nasa.gov/Aboutus/xml/dif/");
        params.add(outSchemParam);

        cswURL.setParameters(new ArrayList<>(params));

        OS_DESCRIPTION.getUrl().add(cswURL);

        // Search by bbox returning ATOM
        type     = "application/atom+xml";
        template = "{cswUrl}/opensearch?service=CSW&version=3.0.0&"
                 + "q={searchTerms?}&"
                 + "maxRecords={count?}&"
                 + "startPosition={startIndex?}"
                 + "&bbox={geo:box?}&"
                 + "recordIds={geo:uid?}&"
                 + "geometry={geo:geometry?}&"
                 + "relation={geo:relation?}&"
                 + "lat={geo:lat?}&"
                 + "lon={geo:lon?}&"
                 + "radius={geo:radius?}&"
                 + "name={geo:name?}&"
                 + "startDate={time:start?}&"
                 + "endDate={time:end?}&"
                 + "trelation={time:relation?}&"
                 + "outputFormat=application/atom%2Bxml";
        Url atURL = new Url(type, template);
        atURL.setIndexOffset(new BigInteger("1"));
        atURL.setPageOffset(new BigInteger("1"));
        atURL.setRel("collection");
        params.remove(outSchemParam);
        atURL.setParameters(new ArrayList<>(params));
        OS_DESCRIPTION.getUrl().add(atURL);

        CompleteQueryType query = new CompleteQueryType();
        query.setRole("example");
        query.setSearchTerms("L2");
        OS_DESCRIPTION.getQuery().add(query);
    }

    private CSWConstants() {}

}
