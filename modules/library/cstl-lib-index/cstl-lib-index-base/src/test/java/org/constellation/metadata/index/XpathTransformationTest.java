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

package org.constellation.metadata.index;

import org.constellation.util.XpathUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.constellation.metadata.CSWQueryable.DUBLIN_CORE_QUERYABLE;
import static org.constellation.metadata.CSWQueryable.EBRIM_QUERYABLE;
import static org.constellation.metadata.CSWQueryable.INSPIRE_QUERYABLE;
import static org.constellation.metadata.CSWQueryable.ISO_FC_QUERYABLE;
import static org.constellation.metadata.CSWQueryable.ISO_QUERYABLE;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class XpathTransformationTest {


    @Test
    public void gfcQueryableTest() throws Exception {

        List<String> expResult = new ArrayList<>();
        expResult.add("ISO 19110:FC_FeatureCatalogue:featureType:carrierOfCharacteristics:memberName");

        List<String> result = XpathUtils.xpathToMDPath(ISO_FC_QUERYABLE.get("attributeName").paths);
        assertEquals(expResult, result);
    }

    @Test
    public void isoQueryableTest() throws Exception {
        /*
         * The core queryable of ISO 19115
         */
        List<String> expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:descriptiveKeywords:keyword");
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:descriptiveKeywords:keyword:value");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:descriptiveKeywords:keyword");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:descriptiveKeywords:keyword:value");
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:topicCategory");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:topicCategory");
        List<String> result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("Subject").paths);
        assertEquals(expResult, result);

        //MANDATORY
        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:citation:title");
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:citation:title:value");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:citation:title");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:citation:title:value");
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("Title").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:abstract");
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:abstract:value");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:abstract");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:abstract:value");
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("Abstract").paths);
        assertEquals(expResult, result);

        /*MANDATORY
        paths = new ArrayList<>();
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("AnyText", paths);*/

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:distributionInfo:distributionFormat:name");
        expResult.add("ISO 19115:MD_Metadata:distributionInfo:distributionFormat:name:value");
        expResult.add("ISO 19115-2:MI_Metadata:distributionInfo:distributionFormat:name");
        expResult.add("ISO 19115-2:MI_Metadata:distributionInfo:distributionFormat:name:value");
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("Format").paths);
        assertEquals(expResult, result);

        //MANDATORY
        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:fileIdentifier");
        expResult.add("ISO 19115-2:MI_Metadata:fileIdentifier");
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("Identifier").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:dateStamp");
        expResult.add("ISO 19115-2:MI_Metadata:dateStamp");
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("Modified").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:hierarchyLevel");
        expResult.add("ISO 19115-2:MI_Metadata:hierarchyLevel");
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("Type").paths);
        assertEquals(expResult, result);

        /*
         * Bounding box
         */
        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:extent:geographicElement2:westBoundLongitude");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:extent:geographicElement2:westBoundLongitude");
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("WestBoundLongitude").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:extent:geographicElement2:eastBoundLongitude");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:extent:geographicElement2:eastBoundLongitude");
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("EastBoundLongitude").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:extent:geographicElement2:northBoundLatitude");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:extent:geographicElement2:northBoundLatitude");
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("NorthBoundLatitude").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:extent:geographicElement2:southBoundLatitude");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:extent:geographicElement2:southBoundLatitude");
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("SouthBoundLatitude").paths);
        assertEquals(expResult, result);

        /*
         * CRS
         */
        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:referenceSystemInfo:referenceSystemIdentifier:codeSpace");
        expResult.add("ISO 19115-2:MI_Metadata:referenceSystemInfo:referenceSystemIdentifier:codeSpace");
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("Authority").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:referenceSystemInfo:referenceSystemIdentifier:code");
        expResult.add("ISO 19115-2:MI_Metadata:referenceSystemInfo:referenceSystemIdentifier:code");
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("ID").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:referenceSystemInfo:referenceSystemIdentifier:version");
        expResult.add("ISO 19115-2:MI_Metadata:referenceSystemInfo:referenceSystemIdentifier:version");
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("Version").paths);
        assertEquals(expResult, result);

        /*
         * Additional queryable Element
         */
        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:citation:alternateTitle");
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:citation:alternateTitle:value");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:citation:alternateTitle");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:citation:alternateTitle:value");
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("AlternateTitle").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:citation:date#dateType=revision:date");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:citation:date#dateType=revision:date");
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("RevisionDate").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:citation:date#dateType=creation:date");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:citation:date#dateType=creation:date");
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("CreationDate").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:citation:date#dateType=publication:date");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:citation:date#dateType=publication:date");
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("PublicationDate").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:pointOfContact:organisationName");
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:pointOfContact:organisationName:value");
        // TODO remove the following path are not normalized
        expResult.add("ISO 19115:MD_Metadata:contact:organisationName");
        expResult.add("ISO 19115:MD_Metadata:contact:organisationName:value");
        expResult.add("ISO 19115:MD_Metadata:distributionInfo:distributor:distributorContact:organisationName");
        expResult.add("ISO 19115:MD_Metadata:distributionInfo:distributor:distributorContact:organisationName:value");
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:citation:citedResponsibleParty:organisationName");
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:citation:citedResponsibleParty:organisationName:value");

        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:pointOfContact:organisationName");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:pointOfContact:organisationName:value");
        // TODO remove the following path are not normalized
        expResult.add("ISO 19115-2:MI_Metadata:contact:organisationName");
        expResult.add("ISO 19115-2:MI_Metadata:contact:organisationName:value");
        expResult.add("ISO 19115-2:MI_Metadata:distributionInfo:distributor:distributorContact:organisationName");
        expResult.add("ISO 19115-2:MI_Metadata:distributionInfo:distributor:distributorContact:organisationName:value");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:citation:citedResponsibleParty:organisationName");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:citation:citedResponsibleParty:organisationName:value");
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("OrganisationName").paths);
        assertEquals(expResult, result);

        //TODO If an instance of the class MD_SecurityConstraint exists for a resource, the “HasSecurityConstraints” is “true”, otherwise “false”
        //paths = new ArrayList<>();
        //result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("HasSecurityConstraints", paths);

        //TODO MD_FeatureCatalogueDescription
        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:language");
        expResult.add("ISO 19115-2:MI_Metadata:language");
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("Language").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:citation:identifier:code");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:citation:identifier:code");
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("ResourceIdentifier").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:parentIdentifier");
        expResult.add("ISO 19115-2:MI_Metadata:parentIdentifier");
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("ParentIdentifier").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:descriptiveKeywords:type");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:descriptiveKeywords:type");
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("KeywordType").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:topicCategory");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:topicCategory");
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("TopicCategory").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:language");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:language");
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("ResourceLanguage").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:extent:geographicElement3:geographicIdentifier:code");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:extent:geographicElement3:geographicIdentifier:code");
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("GeographicDescriptionCode").paths);
        assertEquals(expResult, result);

        /*
         * spatial resolution
         */

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:spatialResolution:equivalentScale:denominator");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:spatialResolution:equivalentScale:denominator");
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("Denominator").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:spatialResolution:distance");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:spatialResolution:distance");
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("DistanceValue").paths);
        assertEquals(expResult, result);

        /*
         * Temporal Extent
         */
        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:extent:temporalElement:extent:beginPosition:value");
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:extent:temporalElement:extent:beginPosition");
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:extent:temporalElement:extent:position");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:extent:temporalElement:extent:beginPosition:value");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:extent:temporalElement:extent:beginPosition");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:extent:temporalElement:extent:position");
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("TempExtent_begin").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:extent:temporalElement:extent:endPosition:value");
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:extent:temporalElement:extent:endPosition");
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:extent:temporalElement:extent:position");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:extent:temporalElement:extent:endPosition:value");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:extent:temporalElement:extent:endPosition");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:extent:temporalElement:extent:position");
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("TempExtent_end").paths);
        assertEquals(expResult, result);

        /*
         *  cloud cover percentage
         */
        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:contentInfo:cloudCoverPercentage");
        expResult.add("ISO 19115-2:MI_Metadata:contentInfo:cloudCoverPercentage");
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("CloudCover").paths);
        assertEquals(expResult, result);

        /*
         *  illuminationElevationAngle
         */
        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:contentInfo:illuminationElevationAngle");
        expResult.add("ISO 19115-2:MI_Metadata:contentInfo:illuminationElevationAngle");
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("IlluminationElevation").paths);
        assertEquals(expResult, result);

        /*
         *  processing level
         */
        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:contentInfo:processingLevelCode:code");
        expResult.add("ISO 19115-2:MI_Metadata:contentInfo:processingLevelCode:code");
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("ProcessingLevel").paths);
        assertEquals(expResult, result);


        /**
         * ISO 19119 specific queryable
         */
        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:serviceType");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:serviceType");
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("ServiceType").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:couplingType");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:couplingType");
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("CouplingType").paths);
        assertEquals(expResult, result);

        //TODO  the following element are described in Service part of ISO 19139 not yet used.
        expResult = new ArrayList<>();
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("ServiceTypeVersion").paths);
        assertEquals(expResult, result);
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("OperatesOn").paths);
        assertEquals(expResult, result);
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("OperatesOnIdentifier").paths);
        assertEquals(expResult, result);
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("OperatesOnWithOpName").paths);
        assertEquals(expResult, result);

        /**
         * ISO 19115-2 specific queryable
         */
        expResult = new ArrayList<>();
        expResult.add("ISO 19115-2:MI_Metadata:acquisitionInformation:platform:citation:title");
        expResult.add("ISO 19115-2:MI_Metadata:acquisitionInformation:instrument:mountedOn:citation:title");
        expResult.add("ISO 19115-2:MI_Metadata:acquisitionInformation:operation:platform:citation:title");
        expResult.add("ISO 19115-2:MI_Metadata:acquisitionInformation:operation:parentOperation:platform:citation:title");
        expResult.add("ISO 19115-2:MI_Metadata:acquisitionInformation:operation:childOperation:platform:citation:title");
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("Platform").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115-2:MI_Metadata:acquisitionInformation:instrument:citation:title");
        expResult.add("ISO 19115-2:MI_Metadata:acquisitionInformation:platform:instrument:citation:title");
        expResult.add("ISO 19115-2:MI_Metadata:acquisitionInformation:operation:platform:instrument:citation:title");
        expResult.add("ISO 19115-2:MI_Metadata:acquisitionInformation:operation:parentOperation:platform:instrument:citation:title");
        expResult.add("ISO 19115-2:MI_Metadata:acquisitionInformation:operation:childOperation:platform:instrument:citation:title");
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("Instrument").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115-2:MI_Metadata:acquisitionInformation:operation:citation:title");
        expResult.add("ISO 19115-2:MI_Metadata:acquisitionInformation:operation:childOperation:citation:title");
        expResult.add("ISO 19115-2:MI_Metadata:acquisitionInformation:operation:parentOperation:citation:title");
        result = XpathUtils.xpathToMDPath(ISO_QUERYABLE.get("Operation").paths);
        assertEquals(expResult, result);
    }



    @Test
    public void dcQueryableTest() throws Exception {
        /*
         * The core queryable of DublinCore
         */
        List<String> expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:citation:title");
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:citation:title:value");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:citation:title");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:citation:title:value");
        expResult.add("Catalog Web Service:Record:title:content");
        expResult.add("Ebrim v3.0:*:Name:LocalizedString:value");
        expResult.add("Ebrim v2.5:*:Name:LocalizedString:value");
        expResult.add("NASA Directory Interchange Format:DIF:Entry_Title");
        List<String> result = XpathUtils.xpathToMDPath(DUBLIN_CORE_QUERYABLE.get("title").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:pointOfContact#role=originator:organisationName:value");
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:pointOfContact#role=originator:organisationName");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:pointOfContact#role=originator:organisationName:value");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:pointOfContact#role=originator:organisationName");
        expResult.add("Catalog Web Service:Record:creator:content");
        expResult.add("NASA Directory Interchange Format:DIF:Organization:Short_Name");
        result = XpathUtils.xpathToMDPath(DUBLIN_CORE_QUERYABLE.get("creator").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:descriptiveKeywords:keyword");
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:descriptiveKeywords:keyword:value");
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:topicCategory");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:descriptiveKeywords:keyword");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:descriptiveKeywords:keyword:value");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:topicCategory");
        expResult.add("Catalog Web Service:Record:subject:content");
        //TODO @name = “http://purl.org/dc/elements/1.1/subject”
        expResult.add("Ebrim v3.0:*:slot:valueList:Value");
        expResult.add("Ebrim v2.5:*:slot:valueList:Value");
        expResult.add("NASA Directory Interchange Format:DIF:ISO_Topic_Category");
        expResult.add("NASA Directory Interchange Format:DIF:Ancillary_Keyword");
        result = XpathUtils.xpathToMDPath(DUBLIN_CORE_QUERYABLE.get("description").paths);
        assertEquals(expResult, result);
        result = XpathUtils.xpathToMDPath(DUBLIN_CORE_QUERYABLE.get("subject").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:abstract");
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:abstract:value");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:abstract");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:abstract:value");
        expResult.add("Catalog Web Service:Record:abstract:content");
        expResult.add("Ebrim v3.0:*:Description:LocalizedString:value");
        expResult.add("Ebrim v2.5:*:Description:LocalizedString:value");
        expResult.add("NASA Directory Interchange Format:DIF:Summary"); // erroned todo correct (but not used anymore)
        result = XpathUtils.xpathToMDPath(DUBLIN_CORE_QUERYABLE.get("abstract").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:pointOfContact#role=publisher:organisationName");
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:pointOfContact#role=publisher:organisationName:value");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:pointOfContact#role=publisher:organisationName");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:pointOfContact#role=publisher:organisationName:value");
        expResult.add("Catalog Web Service:Record:publisher:content");
        result = XpathUtils.xpathToMDPath(DUBLIN_CORE_QUERYABLE.get("publisher").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:pointOfContact#role=author:organisationName");
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:pointOfContact#role=author:organisationName:value");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:pointOfContact#role=author:organisationName");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:pointOfContact#role=author:organisationName:value");
        expResult.add("Catalog Web Service:Record:contributor:content");
        result = XpathUtils.xpathToMDPath(DUBLIN_CORE_QUERYABLE.get("contributor").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:dateStamp");
        expResult.add("ISO 19115-2:MI_Metadata:dateStamp");
        expResult.add("Catalog Web Service:Record:date:content");
        expResult.add("NASA Directory Interchange Format:DIF:Metadata_Dates"); // erroned todo correct (but not used anymore) // erroned todo correct (but not used anymore)
        result = XpathUtils.xpathToMDPath(DUBLIN_CORE_QUERYABLE.get("date").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:hierarchyLevel");
        expResult.add("ISO 19115-2:MI_Metadata:hierarchyLevel");
        expResult.add("Catalog Web Service:Record:type:content");
        expResult.add("Ebrim v3.0:*:objectType");
        expResult.add("Ebrim v2.5:*:objectType");
        result = XpathUtils.xpathToMDPath(DUBLIN_CORE_QUERYABLE.get("type").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:distributionInfo:distributionFormat:name");
        expResult.add("ISO 19115:MD_Metadata:distributionInfo:distributionFormat:name:value");
        expResult.add("ISO 19115-2:MI_Metadata:distributionInfo:distributionFormat:name");
        expResult.add("ISO 19115-2:MI_Metadata:distributionInfo:distributionFormat:name:value");
        expResult.add("Catalog Web Service:Record:format:content");
        expResult.add("Ebrim v3.0:*:mimeType");
        expResult.add("Ebrim v2.5:*:mimeType");
        result = XpathUtils.xpathToMDPath(DUBLIN_CORE_QUERYABLE.get("format").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:fileIdentifier");
        expResult.add("ISO 19115-2:MI_Metadata:fileIdentifier");
        expResult.add("Catalog Web Service:Record:identifier:content");
        expResult.add("ISO 19110:FC_FeatureCatalogue:id");
        expResult.add("Ebrim v3.0:*:id");
        expResult.add("Web Registry Service v1.0:ExtrinsicObject:id");
        expResult.add("Ebrim v2.5:*:id");
        expResult.add("Web Registry Service v0.9:*:id");
        expResult.add("NASA Directory Interchange Format:DIF:Entry_ID"); // erroned todo correct (but not used anymore)
        result = XpathUtils.xpathToMDPath(DUBLIN_CORE_QUERYABLE.get("identifier").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("Catalog Web Service:Record:source:content");
        result = XpathUtils.xpathToMDPath(DUBLIN_CORE_QUERYABLE.get("source").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:language");
        expResult.add("ISO 19115-2:MI_Metadata:language");
        expResult.add("Catalog Web Service:Record:language:content");
        result = XpathUtils.xpathToMDPath(DUBLIN_CORE_QUERYABLE.get("language").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:aggregationInfo:aggregateDataSetName:title");
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:aggregationInfo:aggregateDataSetName:title:value");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:aggregationInfo:aggregateDataSetName:title");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:aggregationInfo:aggregateDataSetName:title:value");
        expResult.add("Catalog Web Service:Record:relation:content");
        result = XpathUtils.xpathToMDPath(DUBLIN_CORE_QUERYABLE.get("relation").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:resourceConstraints:accessConstraints");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:resourceConstraints:accessConstraints");
        expResult.add("Catalog Web Service:Record:rights:content");
        result = XpathUtils.xpathToMDPath(DUBLIN_CORE_QUERYABLE.get("rights").paths);
        assertEquals(expResult, result);

        /*
         * Bounding box
         */
        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:extent:geographicElement2:westBoundLongitude");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:extent:geographicElement2:westBoundLongitude");
        expResult.add("Catalog Web Service:Record:BoundingBox:LowerCorner[0]");
        expResult.add("NASA Directory Interchange Format:DIF:Spatial_Coverage:Bounding_Rectangle"); // erroned todo correct (but not used anymore)
        result = XpathUtils.xpathToMDPath(DUBLIN_CORE_QUERYABLE.get("WestBoundLongitude").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:extent:geographicElement2:eastBoundLongitude");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:extent:geographicElement2:eastBoundLongitude");
        expResult.add("Catalog Web Service:Record:BoundingBox:UpperCorner[0]");
        expResult.add("NASA Directory Interchange Format:DIF:Spatial_Coverage:Bounding_Rectangle"); // erroned todo correct (but not used anymore)
        result = XpathUtils.xpathToMDPath(DUBLIN_CORE_QUERYABLE.get("EastBoundLongitude").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:extent:geographicElement2:northBoundLatitude");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:extent:geographicElement2:northBoundLatitude");
        expResult.add("Catalog Web Service:Record:BoundingBox:UpperCorner[1]");
        expResult.add("NASA Directory Interchange Format:DIF:Spatial_Coverage:Bounding_Rectangle"); // erroned todo correct (but not used anymore)
        result = XpathUtils.xpathToMDPath(DUBLIN_CORE_QUERYABLE.get("NorthBoundLatitude").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:extent:geographicElement2:southBoundLatitude");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:extent:geographicElement2:southBoundLatitude");
        expResult.add("Catalog Web Service:Record:BoundingBox:LowerCorner[1]");
        expResult.add("NASA Directory Interchange Format:DIF:Spatial_Coverage:Bounding_Rectangle"); // erroned todo correct (but not used anymore)
        result = XpathUtils.xpathToMDPath(DUBLIN_CORE_QUERYABLE.get("SouthBoundLatitude").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("Catalog Web Service:Record:BoundingBox:crs");
        expResult.add("NASA Directory Interchange Format:DIF:Spatial_Coverage:Coordinate_System");
        result = XpathUtils.xpathToMDPath(DUBLIN_CORE_QUERYABLE.get("CRS").paths);
        assertEquals(expResult, result);
    }

    @Test
    public void ebrimQueryableTest() throws Exception {
        /*
         * The core queryable of DublinCore
         */
        List<String> expResult = new ArrayList<>();
        expResult.add("Ebrim v3.0:RegistryObject:Name:LocalizedString:value");
        expResult.add("Ebrim v3.0:RegistryPackage:Name:LocalizedString:value");
        List<String> result = XpathUtils.xpathToMDPath(EBRIM_QUERYABLE.get("name").paths);
        assertEquals(expResult, result);

        //TODO verify codelist=originator
        expResult = new ArrayList<>();
        result = XpathUtils.xpathToMDPath(EBRIM_QUERYABLE.get("creator").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        //TODO @name = “http://purl.org/dc/elements/1.1/subject”
        expResult.add("Ebrim v3.0:RegistryObject:slot:valueList:Value");
        expResult.add("Ebrim v3.0:RegistryPackage:slot:valueList:Value");
        result = XpathUtils.xpathToMDPath(EBRIM_QUERYABLE.get("description").paths);
        assertEquals(expResult, result);
        result = XpathUtils.xpathToMDPath(EBRIM_QUERYABLE.get("subject").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("Ebrim v3.0:RegistryObject:Description:LocalizedString:value");
        expResult.add("Ebrim v3.0:RegistryPackage:Description:LocalizedString:value");
        result = XpathUtils.xpathToMDPath(EBRIM_QUERYABLE.get("abstract").paths);
        assertEquals(expResult, result);

        //TODO verify codelist=publisher
        expResult = new ArrayList<>();
        result = XpathUtils.xpathToMDPath(EBRIM_QUERYABLE.get("publisher").paths);
        assertEquals(expResult, result);

        //TODO verify codelist=contributor
        expResult = new ArrayList<>();
        result = XpathUtils.xpathToMDPath(EBRIM_QUERYABLE.get("contributor").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        result = XpathUtils.xpathToMDPath(EBRIM_QUERYABLE.get("date").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("Ebrim v3.0:RegistryObject:objectType");
        expResult.add("Ebrim v3.0:RegistryPackage:objectType");
        result = XpathUtils.xpathToMDPath(EBRIM_QUERYABLE.get("type").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("Ebrim v3.0:ExtrinsicObject:mimeType");
        result = XpathUtils.xpathToMDPath(EBRIM_QUERYABLE.get("format").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("Ebrim v3.0:RegistryObject:id");
        expResult.add("Ebrim v3.0:RegistryPackage:id");
        result = XpathUtils.xpathToMDPath(EBRIM_QUERYABLE.get("identifier").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        result = XpathUtils.xpathToMDPath(EBRIM_QUERYABLE.get("source").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        result = XpathUtils.xpathToMDPath(EBRIM_QUERYABLE.get("language").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        result = XpathUtils.xpathToMDPath(EBRIM_QUERYABLE.get("relation").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        result = XpathUtils.xpathToMDPath(EBRIM_QUERYABLE.get("rigths").paths);
        assertEquals(expResult, result);

        /*
         * Bounding box
         */
        expResult = new ArrayList<>();
        result = XpathUtils.xpathToMDPath(EBRIM_QUERYABLE.get("WestBoundLongitude").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        result = XpathUtils.xpathToMDPath(EBRIM_QUERYABLE.get("EastBoundLongitude").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        result = XpathUtils.xpathToMDPath(EBRIM_QUERYABLE.get("NorthBoundLatitude").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        result = XpathUtils.xpathToMDPath(EBRIM_QUERYABLE.get("SouthBoundLatitude").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        result = XpathUtils.xpathToMDPath(EBRIM_QUERYABLE.get("CRS").paths);
        assertEquals(expResult, result);
    }

    @Test
    public void inspireQueryableTest() throws Exception {
        /*
         * The core queryable of DublinCore
         */
        List<String> expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:dataQualityInfo:report:result:pass");
        expResult.add("ISO 19115-2:MI_Metadata:dataQualityInfo:report:result:pass");
        List<String> result = XpathUtils.xpathToMDPath(INSPIRE_QUERYABLE.get("Degree").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:resourceConstraints:accessConstraints");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:resourceConstraints:accessConstraints");
        result = XpathUtils.xpathToMDPath(INSPIRE_QUERYABLE.get("AccessConstraints").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:resourceConstraints:otherConstraints");
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:resourceConstraints:otherConstraints:value");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:resourceConstraints:otherConstraints");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:resourceConstraints:otherConstraints:value");
        result = XpathUtils.xpathToMDPath(INSPIRE_QUERYABLE.get("OtherConstraints").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:resourceConstraints:classification");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:resourceConstraints:classification");
        result = XpathUtils.xpathToMDPath(INSPIRE_QUERYABLE.get("Classification").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:resourceConstraints:useLimitation");
        expResult.add("ISO 19115:MD_Metadata:identificationInfo:resourceConstraints:useLimitation:value");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:resourceConstraints:useLimitation");
        expResult.add("ISO 19115-2:MI_Metadata:identificationInfo:resourceConstraints:useLimitation:value");
        result = XpathUtils.xpathToMDPath(INSPIRE_QUERYABLE.get("ConditionApplyingToAccessAndUse").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:contact:organisationName");
        expResult.add("ISO 19115:MD_Metadata:contact:organisationName:value");
        expResult.add("ISO 19115-2:MI_Metadata:contact:organisationName");
        expResult.add("ISO 19115-2:MI_Metadata:contact:organisationName:value");
        result = XpathUtils.xpathToMDPath(INSPIRE_QUERYABLE.get("MetadataPointOfContact").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:dataQualityInfo:lineage:statement");
        expResult.add("ISO 19115:MD_Metadata:dataQualityInfo:lineage:statement:value");
        expResult.add("ISO 19115-2:MI_Metadata:dataQualityInfo:lineage:statement");
        expResult.add("ISO 19115-2:MI_Metadata:dataQualityInfo:lineage:statement:value");
        result = XpathUtils.xpathToMDPath(INSPIRE_QUERYABLE.get("Lineage").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:dataQualityInfo:report:result:specification:title");
        expResult.add("ISO 19115:MD_Metadata:dataQualityInfo:report:result:specification:title:value");
        expResult.add("ISO 19115-2:MI_Metadata:dataQualityInfo:report:result:specification:title");
        expResult.add("ISO 19115-2:MI_Metadata:dataQualityInfo:report:result:specification:title:value");
        result = XpathUtils.xpathToMDPath(INSPIRE_QUERYABLE.get("SpecificationTitle").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:dataQualityInfo:report:result:specification:date:date");
        expResult.add("ISO 19115-2:MI_Metadata:dataQualityInfo:report:result:specification:date:date");
        result = XpathUtils.xpathToMDPath(INSPIRE_QUERYABLE.get("SpecificationDate").paths);
        assertEquals(expResult, result);

        expResult = new ArrayList<>();
        expResult.add("ISO 19115:MD_Metadata:dataQualityInfo:report:result:specification:date:dateType");
        expResult.add("ISO 19115-2:MI_Metadata:dataQualityInfo:report:result:specification:date:dateType");
        result = XpathUtils.xpathToMDPath(INSPIRE_QUERYABLE.get("SpecificationDateType").paths);
        assertEquals(expResult, result);
    }
}
