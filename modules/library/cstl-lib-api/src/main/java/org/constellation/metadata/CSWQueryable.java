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
package org.constellation.metadata;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.sis.internal.xml.LegacyNamespaces;
import org.apache.sis.xml.Namespaces;
import org.constellation.api.PathType;


/**
 * A container for list of queryable elements in different schemas used in CSW.
 *
 * @author Guilhem Legal
 */
public final class CSWQueryable {

     public static final String INSPIRE  = "http://www.inspire.org";
     public static final String INSPIRE_PREFIX  = "ins";

     public static final QName DEGREE_QNAME                               = new QName(INSPIRE, "Degree",                          INSPIRE_PREFIX);
     public static final QName ACCESS_CONSTRAINTS_QNAME                   = new QName(INSPIRE, "AccessConstraints",               INSPIRE_PREFIX);
     public static final QName OTHER_CONSTRAINTS_QNAME                    = new QName(INSPIRE, "OtherConstraints",                INSPIRE_PREFIX);
     public static final QName INS_CLASSIFICATION_QNAME                   = new QName(INSPIRE, "Classification",                  INSPIRE_PREFIX);
     public static final QName CONDITION_APPLYING_TO_ACCESS_AND_USE_QNAME = new QName(INSPIRE, "ConditionApplyingToAccessAndUse", INSPIRE_PREFIX);
     public static final QName METADATA_POINT_OF_CONTACT_QNAME            = new QName(INSPIRE, "MetadataPointOfContact",          INSPIRE_PREFIX);
     public static final QName LINEAGE_QNAME                              = new QName(INSPIRE, "Lineage",                         INSPIRE_PREFIX);
     public static final QName SPECIFICATION_TITLE_QNAME                  = new QName(INSPIRE, "SpecificationTitle",              INSPIRE_PREFIX);
     public static final QName SPECIFICATION_DATE_QNAME                   = new QName(INSPIRE, "SpecificationDate",               INSPIRE_PREFIX);
     public static final QName SPECIFICATION_DATETYPE_QNAME               = new QName(INSPIRE, "SpecificationDateType",           INSPIRE_PREFIX);

     public static final Map<String , String> ISO_PREFIX_MAPPING = new HashMap<>();
     static {
         ISO_PREFIX_MAPPING.put("gfc", LegacyNamespaces.GFC);
         ISO_PREFIX_MAPPING.put("gmd", LegacyNamespaces.GMD);
         ISO_PREFIX_MAPPING.put("gco", LegacyNamespaces.GCO);
         ISO_PREFIX_MAPPING.put("gmx", LegacyNamespaces.GMX);
         ISO_PREFIX_MAPPING.put("gmi", LegacyNamespaces.GMI_ALIAS);
         ISO_PREFIX_MAPPING.put("gml", Namespaces.GML);
         ISO_PREFIX_MAPPING.put("srv", LegacyNamespaces.SRV);
     }

     public static final Map<String , String> DIF_PREFIX_MAPPING = new HashMap<>();
     static {
         DIF_PREFIX_MAPPING.put("dif", "http://gcmd.gsfc.nasa.gov/Aboutus/xml/dif/");
     }

     public static final Map<String , String> EBRIM_PREFIX_MAPPING = new HashMap<>();
     static {
         EBRIM_PREFIX_MAPPING.put("eb3", "urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0");
         EBRIM_PREFIX_MAPPING.put("eb2", "urn:oasis:names:tc:ebxml-regrep:rim:xsd:2.5");
         EBRIM_PREFIX_MAPPING.put("wr",  "http://www.opengis.net/cat/wrs");
         EBRIM_PREFIX_MAPPING.put("wrs", "http://www.opengis.net/cat/wrs/1.0");
     }

     public static final Map<String , String> CSW_PREFIX_MAPPING = new HashMap<>();
     static {
         CSW_PREFIX_MAPPING.put("csw2", LegacyNamespaces.CSW);
         CSW_PREFIX_MAPPING.put("csw3", Namespaces.CSW);
         CSW_PREFIX_MAPPING.put("dc",   "http://purl.org/dc/elements/1.1/");
         CSW_PREFIX_MAPPING.put("dct",  "http://purl.org/dc/terms/");
     }

     public static final Map<String , String> ALL_PREFIX_MAPPING = new HashMap<>();
     static {
         ALL_PREFIX_MAPPING.putAll(ISO_PREFIX_MAPPING);
         ALL_PREFIX_MAPPING.putAll(DIF_PREFIX_MAPPING);
         ALL_PREFIX_MAPPING.putAll(EBRIM_PREFIX_MAPPING);
         ALL_PREFIX_MAPPING.putAll(CSW_PREFIX_MAPPING);
     }

     private CSWQueryable() {}

     /**
     * The queryable element from ISO 19110 and their path id.
     */
    public static final Map<String, PathType> ISO_FC_QUERYABLE = new HashMap<>();
    static {
        List<String> paths;

        /*
         * The core queryable of ISO 19115
         */
        paths = new ArrayList<>();
        paths.add("/gfc:FC_FeatureCatalogue/gfc:featureType/gfc:FC_FeatureType/gfc:carrierOfCharacteristics/gfc:FC_FeatureAttribute/gfc:memberName/gco:LocalName");
        ISO_FC_QUERYABLE.put("attributeName", new PathType(String.class, paths, ISO_PREFIX_MAPPING));
    }

    /**
     * The queryable element from ISO 19115 and their path id.
     */
    public static final Map<String, PathType> ISO_QUERYABLE = new HashMap<>();
    static {
        List<String> paths;

        /*
         * The core queryable of ISO 19115
         */
        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:descriptiveKeywords/gmd:MD_Keywords/gmd:keyword/gco:CharacterString");
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:descriptiveKeywords/gmd:MD_Keywords/gmd:keyword/gmx:Anchor");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:descriptiveKeywords/gmd:MD_Keywords/gmd:keyword/gco:CharacterString");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:descriptiveKeywords/gmd:MD_Keywords/gmd:keyword/gmx:Anchor");
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:topicCategory/gmd:MD_TopicCategoryCode");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:topicCategory/gmd:MD_TopicCategoryCode");
        ISO_QUERYABLE.put("Subject", new PathType(String.class, paths, ISO_PREFIX_MAPPING));

        //MANDATORY
        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString");
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:citation/gmd:CI_Citation/gmd:title/gmx:Anchor");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:citation/gmd:CI_Citation/gmd:title/gmx:Anchor");
        ISO_QUERYABLE.put("Title", new PathType(String.class, paths, ISO_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:abstract/gco:CharacterString");
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:abstract/gmx:Anchor");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:abstract/gco:CharacterString");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:abstract/gmx:Anchor");
        ISO_QUERYABLE.put("Abstract", new PathType(String.class, paths, ISO_PREFIX_MAPPING));

        /*MANDATORY
        paths = new ArrayList<>();
        ISO_QUERYABLE.put("AnyText", new PathType(String.class, paths));*/

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:distributionInfo/gmd:MD_Distribution/gmd:distributionFormat/gmd:MD_Format/gmd:name/gco:CharacterString");
        paths.add("/gmd:MD_Metadata/gmd:distributionInfo/gmd:MD_Distribution/gmd:distributionFormat/gmd:MD_Format/gmd:name/gmx:Anchor");
        paths.add("/gmi:MI_Metadata/gmd:distributionInfo/gmd:MD_Distribution/gmd:distributionFormat/gmd:MD_Format/gmd:name/gco:CharacterString");
        paths.add("/gmi:MI_Metadata/gmd:distributionInfo/gmd:MD_Distribution/gmd:distributionFormat/gmd:MD_Format/gmd:name/gmx:Anchor");
        ISO_QUERYABLE.put("Format", new PathType(String.class, paths, ISO_PREFIX_MAPPING));

        //MANDATORY
        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:fileIdentifier/gco:CharacterString");
        paths.add("/gmi:MI_Metadata/gmd:fileIdentifier/gco:CharacterString");
        ISO_QUERYABLE.put("Identifier", new PathType(String.class, paths, ISO_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:dateStamp/gco:DateTime");
        paths.add("/gmd:MD_Metadata/gmd:dateStamp/gco:Date");
        paths.add("/gmi:MI_Metadata/gmd:dateStamp/gco:DateTime");
        paths.add("/gmi:MI_Metadata/gmd:dateStamp/gco:Date");
        ISO_QUERYABLE.put("Modified", new PathType(Date.class, paths, ISO_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:hierarchyLevel/gmd:MD_ScopeCode/@codeListValue");
        paths.add("/gmi:MI_Metadata/gmd:hierarchyLevel/gmd:MD_ScopeCode/@codeListValue");
        ISO_QUERYABLE.put("Type", new PathType(String.class, paths, ISO_PREFIX_MAPPING));

        /*
         * Bounding box
         */
        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:geographicElement/gmd:EX_GeographicBoundingBox/gmd:westBoundLongitude/gco:Decimal");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:geographicElement/gmd:EX_GeographicBoundingBox/gmd:westBoundLongitude/gco:Decimal");
        ISO_QUERYABLE.put("WestBoundLongitude",     new PathType(Double.class, paths, ISO_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:geographicElement/gmd:EX_GeographicBoundingBox/gmd:eastBoundLongitude/gco:Decimal");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:geographicElement/gmd:EX_GeographicBoundingBox/gmd:eastBoundLongitude/gco:Decimal");
        ISO_QUERYABLE.put("EastBoundLongitude",     new PathType(Double.class, paths, ISO_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:geographicElement/gmd:EX_GeographicBoundingBox/gmd:northBoundLatitude/gco:Decimal");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:geographicElement/gmd:EX_GeographicBoundingBox/gmd:northBoundLatitude/gco:Decimal");
        ISO_QUERYABLE.put("NorthBoundLatitude",     new PathType(Double.class, paths, ISO_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:geographicElement/gmd:EX_GeographicBoundingBox/gmd:southBoundLatitude/gco:Decimal");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:geographicElement/gmd:EX_GeographicBoundingBox/gmd:southBoundLatitude/gco:Decimal");
        ISO_QUERYABLE.put("SouthBoundLatitude",     new PathType(Double.class, paths, ISO_PREFIX_MAPPING));

        /*
         * CRS
         */
        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:referenceSystemInfo/gmd:MD_ReferenceSystem/gmd:referenceSystemIdentifier/gmd:RS_Identifier/gmd:codeSpace/gco:CharacterString");
        paths.add("/gmi:MI_Metadata/gmd:referenceSystemInfo/gmd:MD_ReferenceSystem/gmd:referenceSystemIdentifier/gmd:RS_Identifier/gmd:codeSpace/gco:CharacterString");
        ISO_QUERYABLE.put("Authority",     new PathType(String.class, paths, ISO_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:referenceSystemInfo/gmd:MD_ReferenceSystem/gmd:referenceSystemIdentifier/gmd:RS_Identifier/gmd:code/gco:CharacterString");
        paths.add("/gmi:MI_Metadata/gmd:referenceSystemInfo/gmd:MD_ReferenceSystem/gmd:referenceSystemIdentifier/gmd:RS_Identifier/gmd:code/gco:CharacterString");
        ISO_QUERYABLE.put("ID",     new PathType(String.class, paths, ISO_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:referenceSystemInfo/gmd:MD_ReferenceSystem/gmd:referenceSystemIdentifier/gmd:RS_Identifier/gmd:version/gco:CharacterString");
        paths.add("/gmi:MI_Metadata/gmd:referenceSystemInfo/gmd:MD_ReferenceSystem/gmd:referenceSystemIdentifier/gmd:RS_Identifier/gmd:version/gco:CharacterString");
        ISO_QUERYABLE.put("Version",     new PathType(String.class, paths, ISO_PREFIX_MAPPING));

        /*
         * Additional queryable Element
         */
        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:citation/gmd:CI_Citation/gmd:alternateTitle/gco:CharacterString");
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:citation/gmd:CI_Citation/gmd:alternateTitle/gmx:Anchor");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:citation/gmd:CI_Citation/gmd:alternateTitle/gco:CharacterString");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:citation/gmd:CI_Citation/gmd:alternateTitle/gmx:Anchor");
        ISO_QUERYABLE.put("AlternateTitle",   new PathType(String.class, paths, ISO_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:citation/gmd:CI_Citation/gmd:date/gmd:CI_Date#gmd:dateType/gmd:CI_DateTypeCode/@codeListValue=revision/gmd:date/gco:Date");
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:citation/gmd:CI_Citation/gmd:date/gmd:CI_Date#gmd:dateType/gmd:CI_DateTypeCode/@codeListValue=revision/gmd:date/gco:DateTime");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:citation/gmd:CI_Citation/gmd:date/gmd:CI_Date#gmd:dateType/gmd:CI_DateTypeCode/@codeListValue=revision/gmd:date/gco:Date");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:citation/gmd:CI_Citation/gmd:date/gmd:CI_Date#gmd:dateType/gmd:CI_DateTypeCode/@codeListValue=revision/gmd:date/gco:DateTime");
        ISO_QUERYABLE.put("RevisionDate",  new PathType(Date.class, paths, ISO_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:citation/gmd:CI_Citation/gmd:date/gmd:CI_Date#gmd:dateType/gmd:CI_DateTypeCode/@codeListValue=creation/gmd:date/gco:Date");
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:citation/gmd:CI_Citation/gmd:date/gmd:CI_Date#gmd:dateType/gmd:CI_DateTypeCode/@codeListValue=creation/gmd:date/gco:DateTime");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:citation/gmd:CI_Citation/gmd:date/gmd:CI_Date#gmd:dateType/gmd:CI_DateTypeCode/@codeListValue=creation/gmd:date/gco:Date");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:citation/gmd:CI_Citation/gmd:date/gmd:CI_Date#gmd:dateType/gmd:CI_DateTypeCode/@codeListValue=creation/gmd:date/gco:DateTime");
        ISO_QUERYABLE.put("CreationDate",  new PathType(Date.class, paths, ISO_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:citation/gmd:CI_Citation/gmd:date/gmd:CI_Date#gmd:dateType/gmd:CI_DateTypeCode/@codeListValue=publication/gmd:date/gco:Date");
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:citation/gmd:CI_Citation/gmd:date/gmd:CI_Date#gmd:dateType/gmd:CI_DateTypeCode/@codeListValue=publication/gmd:date/gco:DateTime");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:citation/gmd:CI_Citation/gmd:date/gmd:CI_Date#gmd:dateType/gmd:CI_DateTypeCode/@codeListValue=publication/gmd:date/gco:Date");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:citation/gmd:CI_Citation/gmd:date/gmd:CI_Date#gmd:dateType/gmd:CI_DateTypeCode/@codeListValue=publication/gmd:date/gco:DateTime");
        ISO_QUERYABLE.put("PublicationDate",  new PathType(Date.class, paths, ISO_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:pointOfContact/gmd:CI_ResponsibleParty/gmd:organisationName/gco:CharacterString");
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:pointOfContact/gmd:CI_ResponsibleParty/gmd:organisationName/gmx:Anchor");
        // TODO remove the following path are not normalized
        paths.add("/gmd:MD_Metadata/gmd:contact/gmd:CI_ResponsibleParty/gmd:organisationName/gco:CharacterString");
        paths.add("/gmd:MD_Metadata/gmd:contact/gmd:CI_ResponsibleParty/gmd:organisationName/gmx:Anchor");
        paths.add("/gmd:MD_Metadata/gmd:distributionInfo/gmd:MD_Distribution/gmd:distributor/gmd:MD_Distributor/gmd:distributorContact/gmd:CI_ResponsibleParty/gmd:organisationName/gco:CharacterString");
        paths.add("/gmd:MD_Metadata/gmd:distributionInfo/gmd:MD_Distribution/gmd:distributor/gmd:MD_Distributor/gmd:distributorContact/gmd:CI_ResponsibleParty/gmd:organisationName/gmx:Anchor");
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:citation/gmd:CI_Citation/gmd:citedResponsibleParty/gmd:CI_ResponsibleParty/gmd:organisationName/gco:CharacterString");
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:citation/gmd:CI_Citation/gmd:citedResponsibleParty/gmd:CI_ResponsibleParty/gmd:organisationName/gmx:Anchor");

        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:pointOfContact/gmd:CI_ResponsibleParty/gmd:organisationName/gco:CharacterString");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:pointOfContact/gmd:CI_ResponsibleParty/gmd:organisationName/gmx:Anchor");
        // TODO remove the following path are not normalized
        paths.add("/gmi:MI_Metadata/gmd:contact/gmd:CI_ResponsibleParty/gmd:organisationName/gco:CharacterString");
        paths.add("/gmi:MI_Metadata/gmd:contact/gmd:CI_ResponsibleParty/gmd:organisationName/gmx:Anchor");
        paths.add("/gmi:MI_Metadata/gmd:distributionInfo/gmd:MD_Distribution/gmd:distributor/gmd:MD_Distributor/gmd:distributorContact/gmd:CI_ResponsibleParty/gmd:organisationName/gco:CharacterString");
        paths.add("/gmi:MI_Metadata/gmd:distributionInfo/gmd:MD_Distribution/gmd:distributor/gmd:MD_Distributor/gmd:distributorContact/gmd:CI_ResponsibleParty/gmd:organisationName/gmx:Anchor");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:citation/gmd:CI_Citation/gmd:citedResponsibleParty/gmd:CI_ResponsibleParty/gmd:organisationName/gco:CharacterString");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:citation/gmd:CI_Citation/gmd:citedResponsibleParty/gmd:CI_ResponsibleParty/gmd:organisationName/gmx:Anchor");
        ISO_QUERYABLE.put("OrganisationName", new PathType(String.class, paths, ISO_PREFIX_MAPPING));

        //TODO If an instance of the class MD_SecurityConstraint exists for a resource, the “HasSecurityConstraints” is “true”, otherwise “false”
        //paths = new ArrayList<>();
        //ISO_QUERYABLE.put("HasSecurityConstraints", new PathType(String.class, paths));

        //TODO MD_FeatureCatalogueDescription
        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:language/gmd:LanguageCode/@codeListValue");
        paths.add("/gmi:MI_Metadata/gmd:language/gmd:LanguageCode/@codeListValue");
        ISO_QUERYABLE.put("Language", new PathType(String.class, paths, ISO_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:citation/gmd:CI_Citation/gmd:identifier/gmd:RS_Identifier/gmd:code/gco:CharacterString");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:citation/gmd:CI_Citation/gmd:identifier/gmd:RS_Identifier/gmd:code/gco:CharacterString");
        ISO_QUERYABLE.put("ResourceIdentifier", new PathType(String.class, paths, ISO_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:parentIdentifier/gco:CharacterString");
        paths.add("/gmi:MI_Metadata/gmd:parentIdentifier/gco:CharacterString");
        ISO_QUERYABLE.put("ParentIdentifier", new PathType(String.class, paths, ISO_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:descriptiveKeywords/gmd:MD_Keywords/gmd:type/gmd:MD_KeywordTypeCode");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:descriptiveKeywords/gmd:MD_Keywords/gmd:type/gmd:MD_KeywordTypeCode");
        ISO_QUERYABLE.put("KeywordType", new PathType(String.class, paths, ISO_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:topicCategory/gmd:MD_TopicCategoryCode");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:topicCategory/gmd:MD_TopicCategoryCode");
        ISO_QUERYABLE.put("TopicCategory", new PathType(String.class, paths, ISO_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:language/gmd:LanguageCode");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:language/gmd:LanguageCode");
        ISO_QUERYABLE.put("ResourceLanguage", new PathType(String.class, paths, ISO_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:geographicElement/gmd:EX_GeographicDescription/gmd:geographicIdentifier/gmd:RS_Identifier/gmd:code/gco:CharacterString");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:geographicElement/gmd:EX_GeographicDescription/gmd:geographicIdentifier/gmd:RS_Identifier/gmd:code/gco:CharacterString");
        ISO_QUERYABLE.put("GeographicDescriptionCode", new PathType(String.class, paths, ISO_PREFIX_MAPPING));

        /*
         * spatial resolution
         */
        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:spatialResolution/gmd:MD_Resolution/gmd:equivalentScale/gmd:MD_RepresentativeFraction/gmd:denominator/gco:Integer");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:spatialResolution/gmd:MD_Resolution/gmd:equivalentScale/gmd:MD_RepresentativeFraction/gmd:denominator/gco:Integer");
        ISO_QUERYABLE.put("Denominator", new PathType(Integer.class, paths, ISO_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:spatialResolution/gmd:MD_Resolution/gmd:distance/gco:Distance");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:spatialResolution/gmd:MD_Resolution/gmd:distance/gco:Distance");

        ISO_QUERYABLE.put("DistanceValue", new PathType(Double.class, paths, ISO_PREFIX_MAPPING));

        //TODO not existing path in geotoolkit (Distance is treated as a primitive type)
        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:spatialResolution/gmd:MD_Resolution/gmd:distance/gco:Distance@uom");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:spatialResolution/gmd:MD_Resolution/gmd:distance/gco:Distance@uom");
        ISO_QUERYABLE.put("DistanceUOM", new PathType(String.class, paths, ISO_PREFIX_MAPPING));

        /*
         * Temporal Extent
         */
        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:temporalElement/gmd:EX_TemporalExtent/gmd:extent/gml:TimePeriod/gml:beginPosition/gmx:Anchor");
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:temporalElement/gmd:EX_TemporalExtent/gmd:extent/gml:TimePeriod/gml:beginPosition");
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:temporalElement/gmd:EX_TemporalExtent/gmd:extent/gml:TimeInstant/gml:position");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:temporalElement/gmd:EX_TemporalExtent/gmd:extent/gml:TimePeriod/gml:beginPosition/gmx:Anchor");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:temporalElement/gmd:EX_TemporalExtent/gmd:extent/gml:TimePeriod/gml:beginPosition");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:temporalElement/gmd:EX_TemporalExtent/gmd:extent/gml:TimeInstant/gml:position");
        ISO_QUERYABLE.put("TempExtent_begin", new PathType(Date.class, paths, ISO_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:temporalElement/gmd:EX_TemporalExtent/gmd:extent/gml:TimePeriod/gml:endPosition/gmx:Anchor");
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:temporalElement/gmd:EX_TemporalExtent/gmd:extent/gml:TimePeriod/gml:endPosition");
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:temporalElement/gmd:EX_TemporalExtent/gmd:extent/gml:TimeInstant/gml:position");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:temporalElement/gmd:EX_TemporalExtent/gmd:extent/gml:TimePeriod/gml:endPosition/gmx:Anchor");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:temporalElement/gmd:EX_TemporalExtent/gmd:extent/gml:TimePeriod/gml:endPosition");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:temporalElement/gmd:EX_TemporalExtent/gmd:extent/gml:TimeInstant/gml:position");
        ISO_QUERYABLE.put("TempExtent_end", new PathType(Date.class, paths, ISO_PREFIX_MAPPING));

        /*
         *  cloud cover percentage
         */
        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:contentInfo/gmd:MD_ImageDescription/gmd:cloudCoverPercentage/gco:Real");
        paths.add("/gmi:MI_Metadata/gmd:contentInfo/gmd:MD_ImageDescription/gmd:cloudCoverPercentage/gco:Real");
        ISO_QUERYABLE.put("CloudCover", new PathType(Double.class, paths, ISO_PREFIX_MAPPING));

        /*
         *  illuminationElevationAngle
         */
        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:contentInfo/gmd:MD_ImageDescription/gmd:illuminationElevationAngle/gco:Real");
        paths.add("/gmi:MI_Metadata/gmd:contentInfo/gmd:MD_ImageDescription/gmd:illuminationElevationAngle/gco:Real");
        ISO_QUERYABLE.put("IlluminationElevation", new PathType(Double.class, paths, ISO_PREFIX_MAPPING));

        /*
         *  processing level
         */
        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:contentInfo/gmd:MD_ImageDescription/gmd:processingLevelCode/gmd:RS_Identifier/gmd:code/gco:CharacterString");
        paths.add("/gmi:MI_Metadata/gmd:contentInfo/gmd:MD_ImageDescription/gmd:processingLevelCode/gmd:RS_Identifier/gmd:code/gco:CharacterString");
        ISO_QUERYABLE.put("ProcessingLevel", new PathType(String.class, paths, ISO_PREFIX_MAPPING));


        /**
         * ISO 19119 specific queryable
         */
        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/srv:SV_ServiceIdentification/srv:serviceType/gco:LocalName");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/srv:SV_ServiceIdentification/srv:serviceType/gco:LocalName");
        ISO_QUERYABLE.put("ServiceType", new PathType(String.class, paths, ISO_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/srv:SV_ServiceIdentification/srv:couplingType/srv:SV_CouplingType");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/srv:SV_ServiceIdentification/srv:couplingType/srv:SV_CouplingType");
        ISO_QUERYABLE.put("CouplingType", new PathType(String.class, paths, ISO_PREFIX_MAPPING));

        //TODO  the following element are described in Service part of ISO 19139 not yet used.
        paths = new ArrayList<>();
        ISO_QUERYABLE.put("ServiceTypeVersion",   new PathType(String.class, paths, ISO_PREFIX_MAPPING));
        ISO_QUERYABLE.put("OperatesOn",           new PathType(String.class, paths, ISO_PREFIX_MAPPING));
        ISO_QUERYABLE.put("OperatesOnIdentifier", new PathType(String.class, paths, ISO_PREFIX_MAPPING));
        ISO_QUERYABLE.put("OperatesOnWithOpName", new PathType(String.class, paths, ISO_PREFIX_MAPPING));

        /**
         * ISO 19115-2 specific queryable
         */
        paths = new ArrayList<>();
        paths.add("/gmi:MI_Metadata/gmd:acquisitionInformation/gmi:MI_AcquisitionInformation/gmi:platform/gmi:MI_Platform/gmi:citation/gmd:CI_Citation/gmd:title/gco:CharacterString");
        paths.add("/gmi:MI_Metadata/gmd:acquisitionInformation/gmi:MI_AcquisitionInformation/gmi:instrument/gmi:MI_Instrument/gmi:mountedOn/gmi:MI_Platform/gmi:citation/gmd:CI_Citation/gmd:title/gco:CharacterString");
        paths.add("/gmi:MI_Metadata/gmd:acquisitionInformation/gmi:MI_AcquisitionInformation/gmi:operation/gmi:MI_Operation/gmi:platform/gmi:MI_Platform/gmi:citation/gmd:CI_Citation/gmd:title/gco:CharacterString");
        paths.add("/gmi:MI_Metadata/gmd:acquisitionInformation/gmi:MI_AcquisitionInformation/gmi:operation/gmi:MI_Operation/gmi:parentOperation/gmi:MI_Operation/gmi:platform/gmi:MI_Platform/gmi:citation/gmd:CI_Citation/gmd:title/gco:CharacterString");
        paths.add("/gmi:MI_Metadata/gmd:acquisitionInformation/gmi:MI_AcquisitionInformation/gmi:operation/gmi:MI_Operation/gmi:childOperation/gmi:MI_Operation/gmi:platform/gmi:MI_Platform/gmi:citation/gmd:CI_Citation/gmd:title/gco:CharacterString");
        ISO_QUERYABLE.put("Platform", new PathType(String.class, paths, ISO_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmi:MI_Metadata/gmd:acquisitionInformation/gmi:MI_AcquisitionInformation/gmi:instrument/gmi:MI_Instrument/gmi:citation/gmd:CI_Citation/gmd:title/gco:CharacterString");
        paths.add("/gmi:MI_Metadata/gmd:acquisitionInformation/gmi:MI_AcquisitionInformation/gmi:platform/gmi:MI_Platform/gmi:instrument/gmi:MI_Instrument/gmi:citation/gmd:CI_Citation/gmd:title/gco:CharacterString");
        paths.add("/gmi:MI_Metadata/gmd:acquisitionInformation/gmi:MI_AcquisitionInformation/gmi:operation/gmi:MI_Operation/gmi:platform/gmi:MI_Platform/gmi:instrument/gmi:MI_Instrument/gmi:citation/gmd:CI_Citation/gmd:title/gco:CharacterString");
        paths.add("/gmi:MI_Metadata/gmd:acquisitionInformation/gmi:MI_AcquisitionInformation/gmi:operation/gmi:MI_Operation/gmi:parentOperation/gmi:MI_Operation/gmi:platform/gmi:MI_Platform/gmi:instrument/gmi:MI_Instrument/gmi:citation/gmd:CI_Citation/gmd:title/gco:CharacterString");
        paths.add("/gmi:MI_Metadata/gmd:acquisitionInformation/gmi:MI_AcquisitionInformation/gmi:operation/gmi:MI_Operation/gmi:childOperation/gmi:MI_Operation/gmi:platform/gmi:MI_Platform/gmi:instrument/gmi:MI_Instrument/gmi:citation/gmd:CI_Citation/gmd:title/gco:CharacterString");
        ISO_QUERYABLE.put("Instrument", new PathType(String.class, paths, ISO_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmi:MI_Metadata/gmd:acquisitionInformation/gmi:MI_AcquisitionInformation/gmi:operation/gmi:MI_Operation/gmi:citation/gmd:CI_Citation/gmd:title/gco:CharacterString");
        paths.add("/gmi:MI_Metadata/gmd:acquisitionInformation/gmi:MI_AcquisitionInformation/gmi:operation/gmi:MI_Operation/gmi:childOperation/gmi:MI_Operation/gmi:citation/gmd:CI_Citation/gmd:title/gco:CharacterString");
        paths.add("/gmi:MI_Metadata/gmd:acquisitionInformation/gmi:MI_AcquisitionInformation/gmi:operation/gmi:MI_Operation/gmi:parentOperation/gmi:MI_Operation/gmi:citation/gmd:CI_Citation/gmd:title/gco:CharacterString");
        ISO_QUERYABLE.put("Operation", new PathType(String.class, paths, ISO_PREFIX_MAPPING));
    }


    /**
     * The queryable element from DublinCore and their path id.
     */
    public static final Map<String, PathType> DIF_QUERYABLE = new HashMap<>();
    static {
        List<String> paths;
        /*
         * Bounding box
         */
        paths = new ArrayList<>();
        paths.add("/dif:DIF/dif:Spatial_Coverage/dif:Geometry/dif:Bounding_Rectangle/dif:Westernmost_Longitude");
        DIF_QUERYABLE.put("WestBoundLongitude",     new PathType(Double.class, paths, DIF_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/dif:DIF/dif:Spatial_Coverage/dif:Geometry/dif:Bounding_Rectangle/dif:Easternmost_Longitude");
        DIF_QUERYABLE.put("EastBoundLongitude",     new PathType(Double.class, paths, DIF_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/dif:DIF/dif:Spatial_Coverage/dif:Geometry/dif:Bounding_Rectangle/dif:Northernmost_Latitude");
        DIF_QUERYABLE.put("NorthBoundLatitude",     new PathType(Double.class, paths, DIF_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/dif:DIF/dif:Spatial_Coverage/dif:Geometry/dif:Bounding_Rectangle/dif:Southernmost_Latitude");
        DIF_QUERYABLE.put("SouthBoundLatitude",     new PathType(Double.class, paths, DIF_PREFIX_MAPPING));
    }

    /**
     * The queryable element from DublinCore and their path id.
     */
    public static final Map<String, PathType> DUBLIN_CORE_QUERYABLE = new HashMap<>();
    static {
        List<String> paths;

        /*
         * The core queryable of DublinCore
         */
        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString");
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:citation/gmd:CI_Citation/gmd:title/gmx:Anchor");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:citation/gmd:CI_Citation/gmd:title/gmx:Anchor");
        paths.add("/csw2:Record/dc:title");
        paths.add("/csw3:Record/dc:title");
        paths.add("/eb3:*/eb3:Name/eb3:LocalizedString/@value");
        paths.add("/eb2:*/eb2:Name/eb2:LocalizedString/@value");
        paths.add("/dif:DIF/dif:Entry_Title");
        DUBLIN_CORE_QUERYABLE.put("title", new PathType(String.class, paths, ALL_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:pointOfContact/gmd:CI_ResponsibleParty#gmd:role/gmd:CI_RoleCode/@codeListValue=originator/gmd:organisationName/gmx:Anchor");
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:pointOfContact/gmd:CI_ResponsibleParty#gmd:role/gmd:CI_RoleCode/@codeListValue=originator/gmd:organisationName/gco:CharacterString");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:pointOfContact/gmd:CI_ResponsibleParty#gmd:role/gmd:CI_RoleCode/@codeListValue=originator/gmd:organisationName/gmx:Anchor");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:pointOfContact/gmd:CI_ResponsibleParty#gmd:role/gmd:CI_RoleCode/@codeListValue=originator/gmd:organisationName/gco:CharacterString");
        paths.add("/csw2:Record/dc:creator");
        paths.add("/csw3:Record/dc:creator");
        paths.add("/dif:DIF/dif:Organization/dif:Organization_Name/dif:Short_Name");
        DUBLIN_CORE_QUERYABLE.put("creator", new PathType(String.class, paths, ALL_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:descriptiveKeywords/gmd:MD_Keywords/gmd:keyword/gco:CharacterString");
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:descriptiveKeywords/gmd:MD_Keywords/gmd:keyword/gmx:Anchor");
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:topicCategory/gmd:MD_TopicCategoryCode");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:descriptiveKeywords/gmd:MD_Keywords/gmd:keyword/gco:CharacterString");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:descriptiveKeywords/gmd:MD_Keywords/gmd:keyword/gmx:Anchor");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:topicCategory/gmd:MD_TopicCategoryCode");
        paths.add("/csw2:Record/dc:subject");
        paths.add("/csw3:Record/dc:subject");
        //TODO @name = “http://purl.org/dc/elements/1.1/subject”
        paths.add("/eb3:*/eb3:slot/eb3:valueList/eb3:Value");
        paths.add("/eb2:*/eb2:slot/eb2:valueList/eb2:Value");

        paths.add("/dif:DIF/dif:ISO_Topic_Category");
        paths.add("/dif:DIF/dif:Ancillary_Keyword");
        // DIF Science keywords?
        DUBLIN_CORE_QUERYABLE.put("description", new PathType(String.class, paths, ALL_PREFIX_MAPPING));
        DUBLIN_CORE_QUERYABLE.put("subject",     new PathType(String.class, paths, ALL_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:abstract/gco:CharacterString");
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:abstract/gmx:Anchor");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:abstract/gco:CharacterString");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:abstract/gmx:Anchor");
        paths.add("/csw2:Record/dc:abstract");
        paths.add("/csw3:Record/dc:abstract");
        paths.add("/eb3:*/eb3:Description/eb3:LocalizedString/@value");
        paths.add("/eb2:*/eb2:Description/eb2:LocalizedString/@value");
        paths.add("/dif:DIF/dif:Summary/dif:Abstract");
        DUBLIN_CORE_QUERYABLE.put("abstract", new PathType(String.class, paths, ALL_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:pointOfContact/gmd:CI_ResponsibleParty#gmd:role/gmd:CI_RoleCode/@codeListValue=publisher/gmd:organisationName/gco:CharacterString");
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:pointOfContact/gmd:CI_ResponsibleParty#gmd:role/gmd:CI_RoleCode/@codeListValue=publisher/gmd:organisationName/gmx:Anchor");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:pointOfContact/gmd:CI_ResponsibleParty#gmd:role/gmd:CI_RoleCode/@codeListValue=publisher/gmd:organisationName/gco:CharacterString");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:pointOfContact/gmd:CI_ResponsibleParty#gmd:role/gmd:CI_RoleCode/@codeListValue=publisher/gmd:organisationName/gmx:Anchor");
        paths.add("/csw2:Record/dc:publisher");
        paths.add("/csw3:Record/dc:publisher");
        DUBLIN_CORE_QUERYABLE.put("publisher", new PathType(String.class, paths, ALL_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:pointOfContact/gmd:CI_ResponsibleParty#gmd:role/gmd:CI_RoleCode/@codeListValue=author/gmd:organisationName/gco:CharacterString");
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:pointOfContact/gmd:CI_ResponsibleParty#gmd:role/gmd:CI_RoleCode/@codeListValue=author/gmd:organisationName/gmx:Anchor");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:pointOfContact/gmd:CI_ResponsibleParty#gmd:role/gmd:CI_RoleCode/@codeListValue=author/gmd:organisationName/gco:CharacterString");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:pointOfContact/gmd:CI_ResponsibleParty#gmd:role/gmd:CI_RoleCode/@codeListValue=author/gmd:organisationName/gmx:Anchor");
        paths.add("/csw2:Record/dc:contributor");
        paths.add("/csw3:Record/dc:contributor");
        DUBLIN_CORE_QUERYABLE.put("contributor", new PathType(String.class, paths, ALL_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:dateStamp/gco:DateTime");
        paths.add("/gmd:MD_Metadata/gmd:dateStamp/gco:Date");
        paths.add("/gmi:MI_Metadata/gmd:dateStamp/gco:DateTime");
        paths.add("/gmi:MI_Metadata/gmd:dateStamp/gco:Date");
        paths.add("/csw2:Record/dc:date");
        paths.add("/csw3:Record/dc:date");
        paths.add("/dif:DIF/dif:Metadata_Dates/dif:Metadata_Last_Revision");
        DUBLIN_CORE_QUERYABLE.put("date", new PathType(Date.class, paths, ALL_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:hierarchyLevel/gmd:MD_ScopeCode");
        paths.add("/gmi:MI_Metadata/gmd:hierarchyLevel/gmd:MD_ScopeCode");
        paths.add("/csw2:Record/dc:type");
        paths.add("/csw3:Record/dc:type");
        paths.add("/eb3:*/@objectType");
        paths.add("/eb2:*/@objectType");
        // DIF ?
        DUBLIN_CORE_QUERYABLE.put("type", new PathType(String.class, paths, ALL_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:distributionInfo/gmd:MD_Distribution/gmd:distributionFormat/gmd:MD_Format/gmd:name/gco:CharacterString");
        paths.add("/gmd:MD_Metadata/gmd:distributionInfo/gmd:MD_Distribution/gmd:distributionFormat/gmd:MD_Format/gmd:name/gmx:Anchor");
        paths.add("/gmi:MI_Metadata/gmd:distributionInfo/gmd:MD_Distribution/gmd:distributionFormat/gmd:MD_Format/gmd:name/gco:CharacterString");
        paths.add("/gmi:MI_Metadata/gmd:distributionInfo/gmd:MD_Distribution/gmd:distributionFormat/gmd:MD_Format/gmd:name/gmx:Anchor");
        paths.add("/csw2:Record/dc:format");
        paths.add("/csw3:Record/dc:format");
        paths.add("/eb3:*/@mimeType");
        paths.add("/eb2:*/@mimeType");
        // DIF ?
        DUBLIN_CORE_QUERYABLE.put("format", new PathType(String.class, paths, ALL_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:fileIdentifier/gco:CharacterString");
        paths.add("/gmi:MI_Metadata/gmd:fileIdentifier/gco:CharacterString");

        paths.add("/csw2:Record/dc:identifier");
        paths.add("/csw3:Record/dc:identifier");

        paths.add("/gfc:FC_FeatureCatalogue/@id");

        paths.add("/eb3:*/@id");
        paths.add("/wrs:ExtrinsicObject/@id");

        paths.add("/eb2:*/@id");
        paths.add("/wr:*/@id");

        paths.add("/dif:DIF/dif:Entry_ID/dif:Short_Name");
        DUBLIN_CORE_QUERYABLE.put("identifier", new PathType(String.class, paths, ALL_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/csw2:Record/dc:source");
        paths.add("/csw3:Record/dc:source");
        DUBLIN_CORE_QUERYABLE.put("source", new PathType(String.class, paths, ALL_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:language/gmd:LanguageCode");
        paths.add("/gmi:MI_Metadata/gmd:language/gmd:LanguageCode");
        paths.add("/csw2:Record/dc:language");
        paths.add("/csw3:Record/dc:language");
        DUBLIN_CORE_QUERYABLE.put("language", new PathType(String.class, paths, ALL_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:aggregationInfo/gmd:MD_AggregateInformation/gmd:aggregateDataSetName/gmd:CI_Citation/gmd:title/gco:CharacterString");
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:aggregationInfo/gmd:MD_AggregateInformation/gmd:aggregateDataSetName/gmd:CI_Citation/gmd:title/gmx:Anchor");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:aggregationInfo/gmd:MD_AggregateInformation/gmd:aggregateDataSetName/gmd:CI_Citation/gmd:title/gco:CharacterString");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:aggregationInfo/gmd:MD_AggregateInformation/gmd:aggregateDataSetName/gmd:CI_Citation/gmd:title/gmx:Anchor");
        paths.add("/csw3:Record/dc:relation");
        paths.add("/csw2:Record/dc:relation");
        DUBLIN_CORE_QUERYABLE.put("relation", new PathType(String.class, paths, ALL_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:resourceConstraints/*/gmd:accessConstraints/gmd:MD_RestrictionCode");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:resourceConstraints/*/gmd:accessConstraints/gmd:MD_RestrictionCode");
        paths.add("/csw2:Record/dc:rights");
        paths.add("/csw3:Record/dc:rights");
        DUBLIN_CORE_QUERYABLE.put("rights", new PathType(String.class, paths, ALL_PREFIX_MAPPING));

        /*
         * Bounding box
         */
        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:geographicElement/gmd:EX_GeographicBoundingBox/gmd:westBoundLongitude/gco:Decimal");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:geographicElement/gmd:EX_GeographicBoundingBox/gmd:westBoundLongitude/gco:Decimal");
        paths.add("/csw2:Record/ows:BoundingBox/ows:LowerCorner[0]");
        paths.add("/csw3:Record/ows:BoundingBox/ows:LowerCorner[0]");
        paths.add("/dif:DIF/dif:Spatial_Coverage/dif:Geometry/dif:Bounding_Rectangle/dif:Westernmost_Longitude");
        DUBLIN_CORE_QUERYABLE.put("WestBoundLongitude",     new PathType(Double.class, paths, ALL_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:geographicElement/gmd:EX_GeographicBoundingBox/gmd:eastBoundLongitude/gco:Decimal");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:geographicElement/gmd:EX_GeographicBoundingBox/gmd:eastBoundLongitude/gco:Decimal");
        paths.add("/csw2:Record/ows:BoundingBox/ows:UpperCorner[0]");
        paths.add("/csw3:Record/ows:BoundingBox/ows:UpperCorner[0]");
        paths.add("/dif:DIF/dif:Spatial_Coverage/dif:Geometry/dif:Bounding_Rectangle/dif:Easternmost_Longitude");
        DUBLIN_CORE_QUERYABLE.put("EastBoundLongitude",     new PathType(Double.class, paths, ALL_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:geographicElement/gmd:EX_GeographicBoundingBox/gmd:northBoundLatitude/gco:Decimal");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:geographicElement/gmd:EX_GeographicBoundingBox/gmd:northBoundLatitude/gco:Decimal");
        paths.add("/csw2:Record/ows:BoundingBox/ows:UpperCorner[1]");
        paths.add("/csw3:Record/ows:BoundingBox/ows:UpperCorner[1]");
        paths.add("/dif:DIF/dif:Spatial_Coverage/dif:Geometry/dif:Bounding_Rectangle/dif:Northernmost_Latitude");
        DUBLIN_CORE_QUERYABLE.put("NorthBoundLatitude",     new PathType(Double.class, paths, ALL_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:geographicElement/gmd:EX_GeographicBoundingBox/gmd:southBoundLatitude/gco:Decimal");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:geographicElement/gmd:EX_GeographicBoundingBox/gmd:southBoundLatitude/gco:Decimal");
        paths.add("/csw2:Record/ows:BoundingBox/ows:LowerCorner[1]");
        paths.add("/csw3:Record/ows:BoundingBox/ows:LowerCorner[1]");
        paths.add("/dif:DIF/dif:Spatial_Coverage/dif:Geometry/dif:Bounding_Rectangle/dif:Southernmost_Latitude");
        DUBLIN_CORE_QUERYABLE.put("SouthBoundLatitude",     new PathType(Double.class, paths, ALL_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/csw2:Record/ows:BoundingBox/@crs");
        paths.add("/csw3:Record/ows:BoundingBox/@crs");
        paths.add("/dif:DIF/dif:Spatial_Coverage/dif:Geometry/dif:Coordinate_System");
        DUBLIN_CORE_QUERYABLE.put("CRS",     new PathType(String.class, paths, ALL_PREFIX_MAPPING));

        /*
         * Temporal Extent
         */
        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:temporalElement/gmd:EX_TemporalExtent/gmd:extent/gml:TimePeriod/gml:beginPosition/gmx:Anchor");
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:temporalElement/gmd:EX_TemporalExtent/gmd:extent/gml:TimePeriod/gml:beginPosition");
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:temporalElement/gmd:EX_TemporalExtent/gmd:extent/gml:TimeInstant/gml:position");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:temporalElement/gmd:EX_TemporalExtent/gmd:extent/gml:TimePeriod/gml:beginPosition/gmx:Anchor");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:temporalElement/gmd:EX_TemporalExtent/gmd:extent/gml:TimePeriod/gml:beginPosition");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:temporalElement/gmd:EX_TemporalExtent/gmd:extent/gml:TimeInstant/gml:position");
        paths.add("/csw3:Record/csw3:TemporalExtent/csw3:begin");
        paths.add("/dif:DIF/dif:Temporal_Coverage/dif:Range_DateTime/dif:Beginning_Date_Time");
        DUBLIN_CORE_QUERYABLE.put("TemporalExtent_begin", new PathType(Date.class, paths, ALL_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:temporalElement/gmd:EX_TemporalExtent/gmd:extent/gml:TimePeriod/gml:endPosition/gmx:Anchor");
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:temporalElement/gmd:EX_TemporalExtent/gmd:extent/gml:TimePeriod/gml:endPosition");
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:temporalElement/gmd:EX_TemporalExtent/gmd:extent/gml:TimeInstant/gml:position");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:temporalElement/gmd:EX_TemporalExtent/gmd:extent/gml:TimePeriod/gml:endPosition/gmx:Anchor");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:temporalElement/gmd:EX_TemporalExtent/gmd:extent/gml:TimePeriod/gml:endPosition");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:extent/gmd:EX_Extent/gmd:temporalElement/gmd:EX_TemporalExtent/gmd:extent/gml:TimeInstant/gml:position");
        paths.add("/csw3:Record/csw3:TemporalExtent/csw3:end");
        paths.add("/dif:DIF/dif:Temporal_Coverage/dif:Range_DateTime/dif:Ending_Date_Time");
        DUBLIN_CORE_QUERYABLE.put("TemporalExtent_end", new PathType(Date.class, paths, ALL_PREFIX_MAPPING));
    }

    /**
     * The queryable element from ebrim and their path id.
     * @deprecated
     */
    @Deprecated
    public static final Map<String, PathType> EBRIM_QUERYABLE = new HashMap<>();
    static {
        List<String> paths;

        /*
         * The core queryable of DublinCore
         */
        paths = new ArrayList<>();
        paths.add("/eb3:RegistryObject/eb3:Name/eb3:LocalizedString/@value");
        paths.add("/eb3:RegistryPackage/eb3:Name/eb3:LocalizedString/@value");
        EBRIM_QUERYABLE.put("name", new PathType(String.class, paths, EBRIM_PREFIX_MAPPING));

        //TODO verify codelist=originator
        paths = new ArrayList<>();
        EBRIM_QUERYABLE.put("creator", new PathType(String.class, paths, EBRIM_PREFIX_MAPPING));

        paths = new ArrayList<>();
        //TODO @name = “http://purl.org/dc/elements/1.1/subject”
        paths.add("/eb3:RegistryObject/eb3:slot/eb3:valueList/eb3:Value");
        paths.add("/eb3:RegistryPackage/eb3:slot/eb3:valueList/eb3:Value");
        EBRIM_QUERYABLE.put("description", new PathType(String.class, paths, EBRIM_PREFIX_MAPPING));
        EBRIM_QUERYABLE.put("subject", new PathType(String.class, paths, EBRIM_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/eb3:RegistryObject/eb3:Description/eb3:LocalizedString/@value");
        paths.add("/eb3:RegistryPackage/eb3:Description/eb3:LocalizedString/@value");
        EBRIM_QUERYABLE.put("abstract", new PathType(String.class, paths, EBRIM_PREFIX_MAPPING));

        //TODO verify codelist=publisher
        paths = new ArrayList<>();
        EBRIM_QUERYABLE.put("publisher", new PathType(String.class, paths, EBRIM_PREFIX_MAPPING));

        //TODO verify codelist=contributor
        paths = new ArrayList<>();
        EBRIM_QUERYABLE.put("contributor", new PathType(String.class, paths, EBRIM_PREFIX_MAPPING));

        paths = new ArrayList<>();
        EBRIM_QUERYABLE.put("date", new PathType(String.class, paths, EBRIM_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/eb3:RegistryObject/@objectType");
        paths.add("/eb3:RegistryPackage/@objectType");
        EBRIM_QUERYABLE.put("type", new PathType(String.class, paths, EBRIM_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/eb3:ExtrinsicObject/@mimeType");
        EBRIM_QUERYABLE.put("format", new PathType(String.class, paths, EBRIM_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/eb3:RegistryObject/@id");
        paths.add("/eb3:RegistryPackage/@id");
        EBRIM_QUERYABLE.put("identifier", new PathType(String.class, paths, EBRIM_PREFIX_MAPPING));

        paths = new ArrayList<>();
        EBRIM_QUERYABLE.put("source", new PathType(String.class, paths, EBRIM_PREFIX_MAPPING));

        paths = new ArrayList<>();
        EBRIM_QUERYABLE.put("language", new PathType(String.class, paths, EBRIM_PREFIX_MAPPING));

        paths = new ArrayList<>();
        EBRIM_QUERYABLE.put("relation", new PathType(String.class, paths, EBRIM_PREFIX_MAPPING));

        paths = new ArrayList<>();
        EBRIM_QUERYABLE.put("rigths", new PathType(String.class, paths, EBRIM_PREFIX_MAPPING));

        /*
         * Bounding box
         */
        paths = new ArrayList<>();
        EBRIM_QUERYABLE.put("WestBoundLongitude", new PathType(Double.class, paths, EBRIM_PREFIX_MAPPING));

        paths = new ArrayList<>();
        EBRIM_QUERYABLE.put("EastBoundLongitude", new PathType(Double.class, paths, EBRIM_PREFIX_MAPPING));

        paths = new ArrayList<>();
        EBRIM_QUERYABLE.put("NorthBoundLatitude", new PathType(Double.class, paths, EBRIM_PREFIX_MAPPING));

        paths = new ArrayList<>();
        EBRIM_QUERYABLE.put("SouthBoundLatitude", new PathType(Double.class, paths, EBRIM_PREFIX_MAPPING));

        paths = new ArrayList<>();
        EBRIM_QUERYABLE.put("CRS", new PathType(String.class, paths, EBRIM_PREFIX_MAPPING));
    }

     /**
     * The queryable element from DublinCore and their path id.
     */
    public static final Map<String, PathType> INSPIRE_QUERYABLE = new HashMap<>();
    static {
        List<String> paths;

        /*
         * The core queryable of DublinCore
         */
        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:dataQualityInfo/gmd:DQ_DataQuality/gmd:report/*/gmd:result/*/gmd:pass/gco:Boolean");
        paths.add("/gmi:MI_Metadata/gmd:dataQualityInfo/gmd:DQ_DataQuality/gmd:report/*/gmd:result/*/gmd:pass/gco:Boolean");
        INSPIRE_QUERYABLE.put("Degree", new PathType(Boolean.class, paths, ISO_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:resourceConstraints/*/gmd:accessConstraints/gmd:MD_RestrictionCode");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:resourceConstraints/*/gmd:accessConstraints/gmd:MD_RestrictionCode");
        INSPIRE_QUERYABLE.put("AccessConstraints", new PathType(String.class, paths, ISO_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:resourceConstraints/*/gmd:otherConstraints/gco:CharacterString");
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:resourceConstraints/*/gmd:otherConstraints/gmx:Anchor");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:resourceConstraints/*/gmd:otherConstraints/gco:CharacterString");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:resourceConstraints/*/gmd:otherConstraints/gmx:Anchor");
        INSPIRE_QUERYABLE.put("OtherConstraints", new PathType(String.class, paths, ISO_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:resourceConstraints/*/gmd:classification/gmd:MD_ClassificationCode");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:resourceConstraints/*/gmd:classification/gmd:MD_ClassificationCode");
        INSPIRE_QUERYABLE.put("Classification", new PathType(String.class, paths, ISO_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:resourceConstraints/*/gmd:useLimitation/gco:CharacterString");
        paths.add("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:resourceConstraints/*/gmd:useLimitation/gmx:Anchor");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:resourceConstraints/*/gmd:useLimitation/gco:CharacterString");
        paths.add("/gmi:MI_Metadata/gmd:identificationInfo/*/gmd:resourceConstraints/*/gmd:useLimitation/gmx:Anchor");
        INSPIRE_QUERYABLE.put("ConditionApplyingToAccessAndUse", new PathType(String.class, paths, ISO_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:contact/gmd:CI_ResponsibleParty/gmd:organisationName/gco:CharacterString");
        paths.add("/gmd:MD_Metadata/gmd:contact/gmd:CI_ResponsibleParty/gmd:organisationName/gmx:Anchor");
        paths.add("/gmi:MI_Metadata/gmd:contact/gmd:CI_ResponsibleParty/gmd:organisationName/gco:CharacterString");
        paths.add("/gmi:MI_Metadata/gmd:contact/gmd:CI_ResponsibleParty/gmd:organisationName/gmx:Anchor");
        INSPIRE_QUERYABLE.put("MetadataPointOfContact", new PathType(String.class, paths, ISO_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:dataQualityInfo/gmd:DQ_DataQuality/gmd:lineage/*/gmd:statement/gco:CharacterString");
        paths.add("/gmd:MD_Metadata/gmd:dataQualityInfo/gmd:DQ_DataQuality/gmd:lineage/*/gmd:statement/gmx:Anchor");
        paths.add("/gmi:MI_Metadata/gmd:dataQualityInfo/gmd:DQ_DataQuality/gmd:lineage/*/gmd:statement/gco:CharacterString");
        paths.add("/gmi:MI_Metadata/gmd:dataQualityInfo/gmd:DQ_DataQuality/gmd:lineage/*/gmd:statement/gmx:Anchor");
        INSPIRE_QUERYABLE.put("Lineage", new PathType(String.class, paths, ISO_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:dataQualityInfo/gmd:DQ_DataQuality/gmd:report/*/gmd:result/*/gmd:specification/gmd:CI_Citation/gmd:title/gco:CharacterString");
        paths.add("/gmd:MD_Metadata/gmd:dataQualityInfo/gmd:DQ_DataQuality/gmd:report/*/gmd:result/*/gmd:specification/gmd:CI_Citation/gmd:title/gmx:Anchor");
        paths.add("/gmi:MI_Metadata/gmd:dataQualityInfo/gmd:DQ_DataQuality/gmd:report/*/gmd:result/*/gmd:specification/gmd:CI_Citation/gmd:title/gco:CharacterString");
        paths.add("/gmi:MI_Metadata/gmd:dataQualityInfo/gmd:DQ_DataQuality/gmd:report/*/gmd:result/*/gmd:specification/gmd:CI_Citation/gmd:title/gmx:Anchor");
        INSPIRE_QUERYABLE.put("SpecificationTitle", new PathType(String.class, paths, ISO_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:dataQualityInfo/gmd:DQ_DataQuality/gmd:report/*/gmd:result/*/gmd:specification/gmd:CI_Citation/gmd:date/gmd:CI_Date/gmd:date/gco:Date");
        paths.add("/gmd:MD_Metadata/gmd:dataQualityInfo/gmd:DQ_DataQuality/gmd:report/*/gmd:result/*/gmd:specification/gmd:CI_Citation/gmd:date/gmd:CI_Date/gmd:date/gco:DateTime");
        paths.add("/gmi:MI_Metadata/gmd:dataQualityInfo/gmd:DQ_DataQuality/gmd:report/*/gmd:result/*/gmd:specification/gmd:CI_Citation/gmd:date/gmd:CI_Date/gmd:date/gco:Date");
        paths.add("/gmi:MI_Metadata/gmd:dataQualityInfo/gmd:DQ_DataQuality/gmd:report/*/gmd:result/*/gmd:specification/gmd:CI_Citation/gmd:date/gmd:CI_Date/gmd:date/gco:DateTime");
        INSPIRE_QUERYABLE.put("SpecificationDate", new PathType(Date.class, paths, ISO_PREFIX_MAPPING));

        paths = new ArrayList<>();
        paths.add("/gmd:MD_Metadata/gmd:dataQualityInfo/gmd:DQ_DataQuality/gmd:report/*/gmd:result/*/gmd:specification/gmd:CI_Citation/gmd:date/gmd:CI_Date/gmd:dateType/gmd:CI_DateTypeCode");
        paths.add("/gmi:MI_Metadata/gmd:dataQualityInfo/gmd:DQ_DataQuality/gmd:report/*/gmd:result/*/gmd:specification/gmd:CI_Citation/gmd:date/gmd:CI_Date/gmd:dateType/gmd:CI_DateTypeCode");
        INSPIRE_QUERYABLE.put("SpecificationDateType", new PathType(String.class, paths, ISO_PREFIX_MAPPING));
    }
}
