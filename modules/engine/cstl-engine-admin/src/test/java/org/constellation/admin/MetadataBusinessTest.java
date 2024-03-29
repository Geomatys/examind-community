/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com/
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
package org.constellation.admin;

import org.junit.Assert;
import org.junit.Test;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.constellation.dto.metadata.MetadataLightBrief;
import org.constellation.exception.ConfigurationException;
import org.constellation.util.NodeUtilities;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class MetadataBusinessTest extends AbstractBusinessTest {

    @Test
    public void createMetadata() throws Exception {
        int providerID = metadataBusiness.getDefaultInternalProviderID();

        final MetadataLightBrief metadata = metadataBusiness.updateMetadata("42292_9s_19900610041000", NodeUtilities.getNodeFromString(BIG_XML), null, null, null, null, providerID, "DOC");

        final DefaultMetadata result  = (DefaultMetadata) metadataBusiness.getMetadata(metadata.getId());

        final DefaultMetadata expResult = (DefaultMetadata) metadataBusiness.unmarshallMetadata(BIG_XML);

        Assert.assertEquals(expResult, result);
    }

    @Test
    public void createMetadataError() throws Exception {
        int providerID = metadataBusiness.getDefaultInternalProviderID();
        boolean exlanched = false;
        try {
            metadataBusiness.updateMetadata("whatever", NodeUtilities.getNodeFromString(ERROR_XML), null, null, null, null, providerID, "DOC");
        }catch (ConfigurationException e) {
            exlanched = true;
        }
        Assert.assertTrue(exlanched);

    }


     private static final String ERROR_XML =
    """
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <gmd:MD_ERROR xmlns:gco="http://www.isotc211.org/2005/gco"
                     xmlns:gmd="http://www.isotc211.org/2005/gmd"
                     xmlns:fra="http://www.cnig.gouv.fr/2005/fra"
                     xmlns:gmx="http://www.isotc211.org/2005/gmx"
                     xmlns:xlink="http://www.w3.org/1999/xlink"
                     xmlns:gml="http://www.opengis.net/gml">
        <gmd:fileIdentifier>
            <gco:CharacterString>error</gco:CharacterString>
        </gmd:fileIdentifier>
    </gmd:MD_ERROR>""";


    private static final String BIG_XML =
    """
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <gmd:MD_Metadata xmlns:gco="http://www.isotc211.org/2005/gco"
                     xmlns:gmd="http://www.isotc211.org/2005/gmd"
                     xmlns:fra="http://www.cnig.gouv.fr/2005/fra"
                     xmlns:gmx="http://www.isotc211.org/2005/gmx"
                     xmlns:xlink="http://www.w3.org/1999/xlink"
                     xmlns:gml="http://www.opengis.net/gml">
        <gmd:fileIdentifier>
            <gco:CharacterString>42292_9s_19900610041000</gco:CharacterString>
        </gmd:fileIdentifier>
        <gmd:language>
            <gmd:LanguageCode codeList="http://schemas.opengis.net/iso/19139/20070417/resources/Codelist/ML_gmxCodelists.xml#LanguageCode" codeListValue="eng">eng</gmd:LanguageCode>
        </gmd:language>
        <gmd:characterSet>
            <gmd:MD_CharacterSetCode codeList="http://www.tc211.org/ISO19139/resources/codeList.xml#utf8" codeListValue="utf8"/>
        </gmd:characterSet>
        <gmd:hierarchyLevel>
            <gmd:MD_ScopeCode codeList="http://www.tc211.org/ISO19139/resources/codeList.xml#dataset" codeListValue="dataset"/>
        </gmd:hierarchyLevel>
        <gmd:hierarchyLevelName>
            <gmx:Anchor xlink:href="SDN:L231:3:CDI">Common Data Index record</gmx:Anchor>
        </gmd:hierarchyLevelName>
        <gmd:contact>
            <gmd:CI_ResponsibleParty>
                <gmd:organisationName>
                    <gco:CharacterString>IFREMER / IDM/SISMER</gco:CharacterString>
                </gmd:organisationName>
                <gmd:contactInfo>
                    <gmd:CI_Contact>
                        <gmd:phone>
                            <gmd:CI_Telephone>
                                <gmd:voice>
                                    <gco:CharacterString>+33 (0)2 98.22.49.16</gco:CharacterString>
                                </gmd:voice>
                                <gmd:facsimile>
                                    <gco:CharacterString>+33 (0)2 98.22.46.44</gco:CharacterString>
                                </gmd:facsimile>
                            </gmd:CI_Telephone>
                        </gmd:phone>
                        <gmd:address>
                            <gmd:CI_Address>
                                <gmd:deliveryPoint>
                                    <gco:CharacterString>Centre IFREMER de Brest BP 70</gco:CharacterString>
                                </gmd:deliveryPoint>
                                <gmd:city>
                                    <gco:CharacterString>PLOUZANE</gco:CharacterString>
                                </gmd:city>
                                <gmd:postalCode>
                                    <gco:CharacterString>29280</gco:CharacterString>
                                </gmd:postalCode>
                                <gmd:country>
                                    <gmx:Anchor xlink:href="SDN:C320:2:FR">France</gmx:Anchor>
                                </gmd:country>
                                <gmd:electronicMailAddress>
                                    <gco:CharacterString>sismer@ifremer.fr</gco:CharacterString>
                                </gmd:electronicMailAddress>
                            </gmd:CI_Address>
                        </gmd:address>
                        <gmd:onlineResource>
                            <gmd:CI_OnlineResource>
                                <gmd:linkage>
                                    <gmd:URL>http://www.ifremer.fr/sismer/</gmd:URL>
                                </gmd:linkage>
                                <gmd:protocol>
                                    <gco:CharacterString>http</gco:CharacterString>
                                </gmd:protocol>
                            </gmd:CI_OnlineResource>
                        </gmd:onlineResource>
                    </gmd:CI_Contact>
                </gmd:contactInfo>
                <gmd:role>
                    <gmd:CI_RoleCode codeList="http://www.tc211.org/ISO19139/resources/codeList.xml#author" codeListValue="author"/>
                </gmd:role>
            </gmd:CI_ResponsibleParty>
        </gmd:contact>
        <gmd:dateStamp>
            <gco:DateTime>2009-01-26T13:00:00+02:00</gco:DateTime>
        </gmd:dateStamp>
        <gmd:spatialRepresentationInfo>
            <gmd:MD_VectorSpatialRepresentation>
                <gmd:geometricObjects>
                    <gmd:MD_GeometricObjects>
                        <gmd:geometricObjectType>
                            <gmd:MD_GeometricObjectTypeCode codeList="http://www.tc211.org/ISO19139/resources/codeList.xml#point" codeListValue="point"/>
                        </gmd:geometricObjectType>
                    </gmd:MD_GeometricObjects>
                </gmd:geometricObjects>
            </gmd:MD_VectorSpatialRepresentation>
        </gmd:spatialRepresentationInfo>
        <gmd:referenceSystemInfo>
            <gmd:MD_ReferenceSystem>
                <gmd:referenceSystemIdentifier>
                    <gmd:RS_Identifier>
                        <gmd:authority>
                            <gmd:CI_Citation>
                                <gmd:title>
                                    <gco:CharacterString>SeaDataNet geographic co-ordinate reference frames</gco:CharacterString>
                                </gmd:title>
                                <gmd:alternateTitle>
                                    <gco:CharacterString>L101</gco:CharacterString>
                                </gmd:alternateTitle>
                                <gmd:edition>
                                    <gmx:Anchor xlink:href="SDN:C371:1:2">2</gmx:Anchor>
                                </gmd:edition>
                                <gmd:identifier>
                                    <gmd:RS_Identifier>
                                        <gmd:code>
                                            <gco:CharacterString>http://www.seadatanet.org/urnurl/</gco:CharacterString>
                                        </gmd:code>
                                    </gmd:RS_Identifier>
                                </gmd:identifier>
                            </gmd:CI_Citation>
                        </gmd:authority>
                        <gmd:code>
                            <gmx:Anchor xlink:href="SDN:L101:2:4326">World Geodetic System 84</gmx:Anchor>
                        </gmd:code>
                    </gmd:RS_Identifier>
                </gmd:referenceSystemIdentifier>
            </gmd:MD_ReferenceSystem>
        </gmd:referenceSystemInfo>
        <gmd:metadataExtensionInfo>
            <gmd:MD_MetadataExtensionInformation>
                <gmd:extendedElementInformation>
                    <gmd:MD_ExtendedElementInformation>
                        <gmd:name>
                            <gco:CharacterString>SDN:EDMO::</gco:CharacterString>
                        </gmd:name>
                        <gmd:definition>
                            <gco:CharacterString>http://www.seadatanet.org/urnurl/</gco:CharacterString>
                        </gmd:definition>
                        <gmd:dataType>
                            <gmd:MD_DatatypeCode codeList="http://www.tc211.org/ISO19139/resources/codeList.xml#codelist" codeListValue="codelist"/>
                        </gmd:dataType>
                        <gmd:parentEntity>
                            <gco:CharacterString>SeaDataNet</gco:CharacterString>
                        </gmd:parentEntity>
                    </gmd:MD_ExtendedElementInformation>
                </gmd:extendedElementInformation>
                <gmd:extendedElementInformation>
                    <gmd:MD_ExtendedElementInformation>
                        <gmd:name>
                            <gco:CharacterString>SDN:L021:1:</gco:CharacterString>
                        </gmd:name>
                        <gmd:definition>
                            <gco:CharacterString>http://www.seadatanet.org/urnurl/</gco:CharacterString>
                        </gmd:definition>
                        <gmd:dataType>
                            <gmd:MD_DatatypeCode codeList="http://www.tc211.org/ISO19139/resources/codeList.xml#codelist" codeListValue="codelist"/>
                        </gmd:dataType>
                        <gmd:parentEntity>
                            <gco:CharacterString>SeaDataNet</gco:CharacterString>
                        </gmd:parentEntity>
                    </gmd:MD_ExtendedElementInformation>
                </gmd:extendedElementInformation>
                <gmd:extendedElementInformation>
                    <gmd:MD_ExtendedElementInformation>
                        <gmd:name>
                            <gco:CharacterString>SDN:L031:2:</gco:CharacterString>
                        </gmd:name>
                        <gmd:definition>
                            <gco:CharacterString>http://www.seadatanet.org/urnurl/</gco:CharacterString>
                        </gmd:definition>
                        <gmd:dataType>
                            <gmd:MD_DatatypeCode codeList="http://www.tc211.org/ISO19139/resources/codeList.xml#codelist" codeListValue="codelist"/>
                        </gmd:dataType>
                        <gmd:parentEntity>
                            <gco:CharacterString>SeaDataNet</gco:CharacterString>
                        </gmd:parentEntity>
                    </gmd:MD_ExtendedElementInformation>
                </gmd:extendedElementInformation>
                <gmd:extendedElementInformation>
                    <gmd:MD_ExtendedElementInformation>
                        <gmd:name>
                            <gco:CharacterString>SDN:L071:1:</gco:CharacterString>
                        </gmd:name>
                        <gmd:definition>
                            <gco:CharacterString>http://www.seadatanet.org/urnurl/</gco:CharacterString>
                        </gmd:definition>
                        <gmd:dataType>
                            <gmd:MD_DatatypeCode codeList="http://www.tc211.org/ISO19139/resources/codeList.xml#codelist" codeListValue="codelist"/>
                        </gmd:dataType>
                        <gmd:parentEntity>
                            <gco:CharacterString>SeaDataNet</gco:CharacterString>
                        </gmd:parentEntity>
                    </gmd:MD_ExtendedElementInformation>
                </gmd:extendedElementInformation>
                <gmd:extendedElementInformation>
                    <gmd:MD_ExtendedElementInformation>
                        <gmd:name>
                            <gco:CharacterString>SDN:L081:1:</gco:CharacterString>
                        </gmd:name>
                        <gmd:definition>
                            <gco:CharacterString>http://www.seadatanet.org/urnurl/</gco:CharacterString>
                        </gmd:definition>
                        <gmd:dataType>
                            <gmd:MD_DatatypeCode codeList="http://www.tc211.org/ISO19139/resources/codeList.xml#codelist" codeListValue="codelist"/>
                        </gmd:dataType>
                        <gmd:parentEntity>
                            <gco:CharacterString>SeaDataNet</gco:CharacterString>
                        </gmd:parentEntity>
                    </gmd:MD_ExtendedElementInformation>
                </gmd:extendedElementInformation>
                <gmd:extendedElementInformation>
                    <gmd:MD_ExtendedElementInformation>
                        <gmd:name>
                            <gco:CharacterString>SDN:L231:3:</gco:CharacterString>
                        </gmd:name>
                        <gmd:definition>
                            <gco:CharacterString>http://www.seadatanet.org/urnurl/</gco:CharacterString>
                        </gmd:definition>
                        <gmd:dataType>
                            <gmd:MD_DatatypeCode codeList="http://www.tc211.org/ISO19139/resources/codeList.xml#codelist" codeListValue="codelist"/>
                        </gmd:dataType>
                        <gmd:parentEntity>
                            <gco:CharacterString>SeaDataNet</gco:CharacterString>
                        </gmd:parentEntity>
                    </gmd:MD_ExtendedElementInformation>
                </gmd:extendedElementInformation>
                <gmd:extendedElementInformation>
                    <gmd:MD_ExtendedElementInformation>
                        <gmd:name>
                            <gco:CharacterString>SDN:L241:1:</gco:CharacterString>
                        </gmd:name>
                        <gmd:definition>
                            <gco:CharacterString>http://www.seadatanet.org/urnurl/</gco:CharacterString>
                        </gmd:definition>
                        <gmd:dataType>
                            <gmd:MD_DatatypeCode codeList="http://www.tc211.org/ISO19139/resources/codeList.xml#codelist" codeListValue="codelist"/>
                        </gmd:dataType>
                        <gmd:parentEntity>
                            <gco:CharacterString>SeaDataNet</gco:CharacterString>
                        </gmd:parentEntity>
                    </gmd:MD_ExtendedElementInformation>
                </gmd:extendedElementInformation>
            </gmd:MD_MetadataExtensionInformation>
        </gmd:metadataExtensionInfo>
        <gmd:identificationInfo>
            <gmd:MD_DataIdentification>
                <gmd:citation>
                    <gmd:CI_Citation>
                        <gmd:title>
                            <gco:CharacterString>90008411-2.ctd</gco:CharacterString>
                        </gmd:title>
                        <gmd:alternateTitle>
                            <gco:CharacterString>42292_9s_19900610041000</gco:CharacterString>
                        </gmd:alternateTitle>
                        <gmd:date>
                            <gmd:CI_Date>
                                <gmd:date>
                                    <gco:DateTime>1990-06-05T00:00:00+02:00</gco:DateTime>
                                </gmd:date>
                                <gmd:dateType>
                                    <gmd:CI_DateTypeCode codeList="http://www.tc211.org/ISO19139/resources/codeList.xml#revision" codeListValue="revision"/>
                                </gmd:dateType>
                            </gmd:CI_Date>
                        </gmd:date>
                        <gmd:date>
                            <gmd:CI_Date>
                                <gmd:date>
                                    <gco:Date>1970-02-04T03:04:26+02:00</gco:Date>
                                </gmd:date>
                                <gmd:dateType>
                                    <gmd:CI_DateTypeCode codeList="http://schemas.opengis.net/iso/19139/20070417/resources/Codelist/gmxCodelists.xml#CI_DateTypeCode" codeListValue="creation" codeSpace="eng">Creation</gmd:CI_DateTypeCode>
                                </gmd:dateType>
                            </gmd:CI_Date>
                        </gmd:date>
                        <gmd:citedResponsibleParty>
                            <gmd:CI_ResponsibleParty>
                                <gmd:organisationName>
                                    <gco:CharacterString>UNIVERSITE DE LA MEDITERRANNEE (U2) / COM - LAB. OCEANOG. &amp; BIOGEOCHIMIE - LUMINY</gco:CharacterString>
                                </gmd:organisationName>
                                <gmd:contactInfo>
                                    <gmd:CI_Contact>
                                        <gmd:phone>
                                            <gmd:CI_Telephone>
                                                <gmd:voice>
                                                    <gco:CharacterString>+33(0)4 91 82 91 15</gco:CharacterString>
                                                </gmd:voice>
                                                <gmd:facsimile>
                                                    <gco:CharacterString>+33(0)4 91.82.65.48</gco:CharacterString>
                                                </gmd:facsimile>
                                            </gmd:CI_Telephone>
                                        </gmd:phone>
                                        <gmd:address>
                                            <gmd:CI_Address>
                                                <gmd:deliveryPoint>
                                                    <gco:CharacterString>UFR Centre Oceanologique de Marseille Campus de Luminy Case 901</gco:CharacterString>
                                                </gmd:deliveryPoint>
                                                <gmd:city>
                                                    <gco:CharacterString>Marseille cedex 9</gco:CharacterString>
                                                </gmd:city>
                                                <gmd:postalCode>
                                                    <gco:CharacterString>13288</gco:CharacterString>
                                                </gmd:postalCode>
                                                <gmd:country>
                                                    <gmx:Anchor xlink:href="SDN:C320:2:FR">France</gmx:Anchor>
                                                </gmd:country>
                                                <gmd:electronicMailAddress>
                                                    <gmx:Anchor xlink:href="SDN:EDMERP::10680"/>
                                                </gmd:electronicMailAddress>
                                            </gmd:CI_Address>
                                        </gmd:address>
                                        <gmd:onlineResource>
                                            <gmd:CI_OnlineResource>
                                                <gmd:linkage>
                                                    <gmd:URL>http://www.com.univ-mrs.fr/LOB/</gmd:URL>
                                                </gmd:linkage>
                                                <gmd:protocol>
                                                    <gco:CharacterString>http</gco:CharacterString>
                                                </gmd:protocol>
                                            </gmd:CI_OnlineResource>
                                        </gmd:onlineResource>
                                    </gmd:CI_Contact>
                                </gmd:contactInfo>
                                <gmd:role>
                                    <gmd:CI_RoleCode codeList="http://www.tc211.org/ISO19139/resources/codeList.xml#originator" codeListValue="originator"/>
                                </gmd:role>
                            </gmd:CI_ResponsibleParty>
                        </gmd:citedResponsibleParty>
                    </gmd:CI_Citation>
                </gmd:citation>
                <gmd:abstract>
                    <gco:CharacterString>Donnees CTD MEDIPROD VI 120</gco:CharacterString>
                </gmd:abstract>
                <gmd:pointOfContact>
                    <gmd:CI_ResponsibleParty>
                        <gmd:organisationName>
                            <gco:CharacterString>IFREMER / IDM/SISMER</gco:CharacterString>
                        </gmd:organisationName>
                        <gmd:contactInfo>
                            <gmd:CI_Contact>
                                <gmd:phone>
                                    <gmd:CI_Telephone>
                                        <gmd:voice>
                                            <gco:CharacterString>+33 (0)2 98.22.49.16</gco:CharacterString>
                                        </gmd:voice>
                                        <gmd:facsimile>
                                            <gco:CharacterString>+33 (0)2 98.22.46.44</gco:CharacterString>
                                        </gmd:facsimile>
                                    </gmd:CI_Telephone>
                                </gmd:phone>
                                <gmd:address>
                                    <gmd:CI_Address>
                                        <gmd:deliveryPoint>
                                            <gco:CharacterString>Centre IFREMER de Brest BP 70</gco:CharacterString>
                                        </gmd:deliveryPoint>
                                        <gmd:city>
                                            <gco:CharacterString>PLOUZANE</gco:CharacterString>
                                        </gmd:city>
                                        <gmd:postalCode>
                                            <gco:CharacterString>29280</gco:CharacterString>
                                        </gmd:postalCode>
                                        <gmd:country>
                                            <gmx:Anchor xlink:href="SDN:C320:2:FR">France</gmx:Anchor>
                                        </gmd:country>
                                        <gmd:electronicMailAddress>
                                            <gco:CharacterString>sismer@ifremer.fr</gco:CharacterString>
                                        </gmd:electronicMailAddress>
                                    </gmd:CI_Address>
                                </gmd:address>
                                <gmd:onlineResource>
                                    <gmd:CI_OnlineResource>
                                        <gmd:linkage>
                                            <gmd:URL>http://www.ifremer.fr/sismer/</gmd:URL>
                                        </gmd:linkage>
                                        <gmd:protocol>
                                            <gco:CharacterString>http</gco:CharacterString>
                                        </gmd:protocol>
                                    </gmd:CI_OnlineResource>
                                </gmd:onlineResource>
                            </gmd:CI_Contact>
                        </gmd:contactInfo>
                        <gmd:role>
                            <gmd:CI_RoleCode codeList="http://www.tc211.org/ISO19139/resources/codeList.xml#custodian" codeListValue="custodian"/>
                        </gmd:role>
                    </gmd:CI_ResponsibleParty>
                </gmd:pointOfContact>
                <gmd:descriptiveKeywords>
                    <gmd:MD_Keywords>
                        <gmd:keyword>
                            <gmx:Anchor xlink:href="SDN:P021:35:ATTN">Transmittance and attenuance of the water column</gmx:Anchor>
                        </gmd:keyword>
                        <gmd:keyword>
                            <gmx:Anchor xlink:href="SDN:P021:35:CNDC">Electrical conductivity of the water column</gmx:Anchor>
                        </gmd:keyword>
                        <gmd:keyword>
                            <gmx:Anchor xlink:href="SDN:P021:35:DOXY">Dissolved oxygen parameters in the water column</gmx:Anchor>
                        </gmd:keyword>
                        <gmd:keyword>
                            <gmx:Anchor xlink:href="SDN:P021:35:EXCO">Light extinction and diffusion coefficients</gmx:Anchor>
                        </gmd:keyword>
                        <gmd:keyword>
                            <gmx:Anchor xlink:href="SDN:P021:35:HEXC">Dissolved noble gas concentration parameters in the water column</gmx:Anchor>
                        </gmd:keyword>
                        <gmd:keyword>
                            <gmx:Anchor xlink:href="SDN:P021:35:OPBS">Optical backscatter</gmx:Anchor>
                        </gmd:keyword>
                        <gmd:keyword>
                            <gmx:Anchor xlink:href="SDN:P021:35:PSAL">Salinity of the water column</gmx:Anchor>
                        </gmd:keyword>
                        <gmd:keyword>
                            <gmx:Anchor xlink:href="SDN:P021:35:SCOX">Dissolved concentration parameters for 'other' gases in the water column</gmx:Anchor>
                        </gmd:keyword>
                        <gmd:keyword>
                            <gmx:Anchor xlink:href="SDN:P021:35:TEMP">Temperature of the water column</gmx:Anchor>
                        </gmd:keyword>
                        <gmd:keyword>
                            <gmx:Anchor xlink:href="SDN:P021:35:VSRA">Visible waveband radiance and irradiance measurements in the atmosphere</gmx:Anchor>
                        </gmd:keyword>
                        <gmd:keyword>
                            <gmx:Anchor xlink:href="SDN:P021:35:VSRW">Visible waveband radiance and irradiance measurements in the water column</gmx:Anchor>
                        </gmd:keyword>
                        <gmd:type>
                            <gmd:MD_KeywordTypeCode codeList="http://www.tc211.org/ISO19139/resources/codeList.xml#parameter" codeListValue="parameter"/>
                        </gmd:type>
                        <gmd:thesaurusName>
                            <gmd:CI_Citation>
                                <gmd:title>
                                    <gco:CharacterString>BODC Parameter Discovery Vocabulary</gco:CharacterString>
                                </gmd:title>
                                <gmd:alternateTitle>
                                    <gco:CharacterString>P021</gco:CharacterString>
                                </gmd:alternateTitle>
                                <gmd:date>
                                    <gmd:CI_Date>
                                        <gmd:date>
                                            <gco:DateTime>2008-11-26T02:00:04+01:00</gco:DateTime>
                                        </gmd:date>
                                        <gmd:dateType>
                                            <gmd:CI_DateTypeCode codeList="http://www.tc211.org/ISO19139/resources/codeList.xml#revision" codeListValue="revision"/>
                                        </gmd:dateType>
                                    </gmd:CI_Date>
                                </gmd:date>
                                <gmd:edition>
                                    <gmx:Anchor xlink:href="SDN:C371:1:35">35</gmx:Anchor>
                                </gmd:edition>
                                <gmd:identifier>
                                    <gmd:RS_Identifier>
                                        <gmd:code>
                                            <gco:CharacterString>http://www.seadatanet.org/urnurl/</gco:CharacterString>
                                        </gmd:code>
                                    </gmd:RS_Identifier>
                                </gmd:identifier>
                            </gmd:CI_Citation>
                        </gmd:thesaurusName>
                    </gmd:MD_Keywords>
                </gmd:descriptiveKeywords>
                <gmd:descriptiveKeywords>
                    <gmd:MD_Keywords>
                        <gmd:keyword>
                            <gmx:Anchor xlink:href="SDN:L054:2:130">CTD profilers</gmx:Anchor>
                        </gmd:keyword>
                        <gmd:type>
                            <gmd:MD_KeywordTypeCode codeList="http://www.tc211.org/ISO19139/resources/codeList.xml#instrument" codeListValue="instrument"/>
                        </gmd:type>
                        <gmd:thesaurusName>
                            <gmd:CI_Citation>
                                <gmd:title>
                                    <gco:CharacterString>SeaDataNet device categories</gco:CharacterString>
                                </gmd:title>
                                <gmd:alternateTitle>
                                    <gco:CharacterString>L05</gco:CharacterString>
                                </gmd:alternateTitle>
                                <gmd:date>
                                    <gmd:CI_Date>
                                        <gmd:date>
                                            <gco:DateTime>2008-01-11T02:00:04+01:00</gco:DateTime>
                                        </gmd:date>
                                        <gmd:dateType>
                                            <gmd:CI_DateTypeCode codeList="http://www.tc211.org/ISO19139/resources/codeList.xml#revision" codeListValue="revision"/>
                                        </gmd:dateType>
                                    </gmd:CI_Date>
                                </gmd:date>
                                <gmd:edition>
                                    <gmx:Anchor xlink:href="SDN:C371:1:4">4</gmx:Anchor>
                                </gmd:edition>
                                <gmd:identifier>
                                    <gmd:RS_Identifier>
                                        <gmd:code>
                                            <gco:CharacterString>http://www.seadatanet.org/urnurl/</gco:CharacterString>
                                        </gmd:code>
                                    </gmd:RS_Identifier>
                                </gmd:identifier>
                            </gmd:CI_Citation>
                        </gmd:thesaurusName>
                    </gmd:MD_Keywords>
                </gmd:descriptiveKeywords>
                <gmd:descriptiveKeywords>
                    <gmd:MD_Keywords>
                        <gmd:keyword>
                            <gmx:Anchor xlink:href="SDN:L061:6:31">research vessel</gmx:Anchor>
                        </gmd:keyword>
                        <gmd:type>
                            <gmd:MD_KeywordTypeCode codeList="http://www.tc211.org/ISO19139/resources/codeList.xml#platform_class" codeListValue="platform_class"/>
                        </gmd:type>
                        <gmd:thesaurusName>
                            <gmd:CI_Citation>
                                <gmd:title>
                                    <gco:CharacterString>SeaDataNet Platform Classes</gco:CharacterString>
                                </gmd:title>
                                <gmd:alternateTitle>
                                    <gco:CharacterString>L061</gco:CharacterString>
                                </gmd:alternateTitle>
                                <gmd:date>
                                    <gmd:CI_Date>
                                        <gmd:date>
                                            <gco:DateTime>2008-02-21T10:55:40+01:00</gco:DateTime>
                                        </gmd:date>
                                        <gmd:dateType>
                                            <gmd:CI_DateTypeCode codeList="http://www.tc211.org/ISO19139/resources/codeList.xml#revision" codeListValue="revision"/>
                                        </gmd:dateType>
                                    </gmd:CI_Date>
                                </gmd:date>
                                <gmd:edition>
                                    <gmx:Anchor xlink:href="SDN:C371:1:6">6</gmx:Anchor>
                                </gmd:edition>
                                <gmd:identifier>
                                    <gmd:RS_Identifier>
                                        <gmd:code>
                                            <gco:CharacterString>http://www.seadatanet.org/urnurl/</gco:CharacterString>
                                        </gmd:code>
                                    </gmd:RS_Identifier>
                                </gmd:identifier>
                            </gmd:CI_Citation>
                        </gmd:thesaurusName>
                    </gmd:MD_Keywords>
                </gmd:descriptiveKeywords>
                <gmd:resourceConstraints>
                    <gmd:MD_LegalConstraints>
                        <gmd:accessConstraints>
                            <gmd:MD_RestrictionCode codeList="http://www.tc211.org/ISO19139/resources/codeList.xml#license" codeListValue="license"/>
                        </gmd:accessConstraints>
                    </gmd:MD_LegalConstraints>
                </gmd:resourceConstraints>
                <gmd:aggregationInfo>
                    <gmd:MD_AggregateInformation>
                        <gmd:aggregateDataSetName>
                            <gmd:CI_Citation>
                                <gmd:title>
                                    <gco:CharacterString>MEDIPROD VI</gco:CharacterString>
                                </gmd:title>
                                <gmd:alternateTitle>
                                    <gco:CharacterString>90008411</gco:CharacterString>
                                </gmd:alternateTitle>
                                <gmd:date>
                                    <gmd:CI_Date>
                                        <gmd:date>
                                            <gco:DateTime>1990-06-05T00:00:00+02:00</gco:DateTime>
                                        </gmd:date>
                                        <gmd:dateType>
                                            <gmd:CI_DateTypeCode codeList="http://www.tc211.org/ISO19139/resources/codeList.xml#revision" codeListValue="revision"/>
                                        </gmd:dateType>
                                    </gmd:CI_Date>
                                </gmd:date>
                            </gmd:CI_Citation>
                        </gmd:aggregateDataSetName>
                        <gmd:associationType>
                            <gmd:DS_AssociationTypeCode codeList="http://www.tc211.org/ISO19139/resources/codeList.xml#largerWorkCitation" codeListValue="largerWorkCitation"/>
                        </gmd:associationType>
                        <gmd:initiativeType>
                            <gmd:DS_InitiativeTypeCode codeList="http://www.tc211.org/ISO19139/resources/codeList.xml#campaign" codeListValue="campaign"/>
                        </gmd:initiativeType>
                    </gmd:MD_AggregateInformation>
                </gmd:aggregationInfo>
                <gmd:aggregationInfo>
                    <gmd:MD_AggregateInformation>
                        <gmd:aggregateDataSetName>
                            <gmd:CI_Citation>
                                <gmd:title>
                                    <gco:CharacterString>9s</gco:CharacterString>
                                </gmd:title>
                                <gmd:alternateTitle>
                                    <gco:CharacterString>9s</gco:CharacterString>
                                </gmd:alternateTitle>
                                <gmd:date>
                                    <gmd:CI_Date>
                                        <gmd:date>
                                            <gco:DateTime>1990-06-10T00:00:00+02:00</gco:DateTime>
                                        </gmd:date>
                                        <gmd:dateType>
                                            <gmd:CI_DateTypeCode codeList="http://www.tc211.org/ISO19139/resources/codeList.xml#revision" codeListValue="revision"/>
                                        </gmd:dateType>
                                    </gmd:CI_Date>
                                </gmd:date>
                            </gmd:CI_Citation>
                        </gmd:aggregateDataSetName>
                        <gmd:associationType>
                            <gmd:DS_AssociationTypeCode codeList="http://www.tc211.org/ISO19139/resources/codeList.xml#largerWorkCitation" codeListValue="largerWorkCitation"/>
                        </gmd:associationType>
                        <gmd:initiativeType>
                            <gmd:DS_InitiativeTypeCode codeList="http://www.tc211.org/ISO19139/resources/codeList.xml#campaign" codeListValue="campaign"/>
                        </gmd:initiativeType>
                    </gmd:MD_AggregateInformation>
                </gmd:aggregationInfo>
                <gmd:language>
                    <gco:CharacterString>eng</gco:CharacterString>
                </gmd:language>
                <gmd:topicCategory>
                    <gmd:MD_TopicCategoryCode>oceans</gmd:MD_TopicCategoryCode>
                </gmd:topicCategory>
                <gmd:extent>
                    <gmd:EX_Extent>
                        <gmd:geographicElement>
                            <gmd:EX_GeographicBoundingBox>
                                <gmd:extentTypeCode>
                                    <gco:Boolean>true</gco:Boolean>
                                </gmd:extentTypeCode>
                                <gmd:westBoundLongitude>
                                    <gco:Decimal>1.3667</gco:Decimal>
                                </gmd:westBoundLongitude>
                                <gmd:eastBoundLongitude>
                                    <gco:Decimal>1.3667</gco:Decimal>
                                </gmd:eastBoundLongitude>
                                <gmd:southBoundLatitude>
                                    <gco:Decimal>36.6</gco:Decimal>
                                </gmd:southBoundLatitude>
                                <gmd:northBoundLatitude>
                                    <gco:Decimal>36.6</gco:Decimal>
                                </gmd:northBoundLatitude>
                            </gmd:EX_GeographicBoundingBox>
                        </gmd:geographicElement>
                        <gmd:geographicElement>
                            <gmd:EX_GeographicBoundingBox>
                                <gmd:extentTypeCode>
                                    <gco:Boolean>true</gco:Boolean>
                                </gmd:extentTypeCode>
                                <gmd:westBoundLongitude>
                                    <gco:Decimal>12.1</gco:Decimal>
                                </gmd:westBoundLongitude>
                                <gmd:eastBoundLongitude>
                                    <gco:Decimal>12.1</gco:Decimal>
                                </gmd:eastBoundLongitude>
                                <gmd:southBoundLatitude>
                                    <gco:Decimal>31.2</gco:Decimal>
                                </gmd:southBoundLatitude>
                                <gmd:northBoundLatitude>
                                    <gco:Decimal>31.2</gco:Decimal>
                                </gmd:northBoundLatitude>
                            </gmd:EX_GeographicBoundingBox>
                        </gmd:geographicElement>
                        <gmd:temporalElement>
                            <gmd:EX_TemporalExtent>
                                <gmd:extent>
                                    <gml:TimePeriod gml:id="extent">
                                        <gml:beginPosition>1990-06-05T00:00:00+02:00</gml:beginPosition>
                                        <gml:endPosition>1990-07-02T00:00:00+02:00</gml:endPosition>
                                    </gml:TimePeriod>
                                </gmd:extent>
                            </gmd:EX_TemporalExtent>
                        </gmd:temporalElement>
                        <gmd:verticalElement>
                            <gmd:EX_VerticalExtent>
                                <gmd:verticalCRS>
                                    <gml:VerticalCRS gml:id="coordinate-reference-system">
                                        <gml:identifier codeSpace="">idvertCRS</gml:identifier>
                                        <gml:scope/>
                                        <gml:verticalCS>
                                            <gml:VerticalCS gml:id="coordinate-system">
                                                <gml:identifier codeSpace="">meters</gml:identifier>
                                                <gml:axis>
                                                    <gml:CoordinateSystemAxis gml:uom="m" gml:id="coordinate-system-axis">
                                                        <gml:identifier codeSpace="">meters</gml:identifier>
                                                        <gml:axisAbbrev>meters</gml:axisAbbrev>
                                                        <gml:axisDirection codeSpace="">down</gml:axisDirection>
                                                    </gml:CoordinateSystemAxis>
                                                </gml:axis>
                                            </gml:VerticalCS>
                                        </gml:verticalCS>
                                        <gml:verticalDatum>
                                            <gml:VerticalDatum gml:id="datum">
                                                <gml:identifier codeSpace="">D28</gml:identifier>
                                                <gml:scope/>
                                            </gml:VerticalDatum>
                                        </gml:verticalDatum>
                                    </gml:VerticalCRS>
                                </gmd:verticalCRS>
                            </gmd:EX_VerticalExtent>
                        </gmd:verticalElement>
                    </gmd:EX_Extent>
                </gmd:extent>
            </gmd:MD_DataIdentification>
        </gmd:identificationInfo>
        <gmd:contentInfo>
            <gmd:MD_ImageDescription>
                <gmd:cloudCoverPercentage>
                    <gco:Real>21.0</gco:Real>
                </gmd:cloudCoverPercentage>
            </gmd:MD_ImageDescription>
        </gmd:contentInfo>
        <gmd:distributionInfo>
            <gmd:MD_Distribution>
                <gmd:distributionFormat>
                    <gmd:MD_Format>
                        <gmd:name>
                            <gmx:Anchor xlink:href="SDN:L241:1:MEDATLAS">MEDATLAS ASCII</gmx:Anchor>
                        </gmd:name>
                        <gmd:version>
                            <gco:CharacterString>1.0</gco:CharacterString>
                        </gmd:version>
                    </gmd:MD_Format>
                </gmd:distributionFormat>
                <gmd:distributor>
                    <gmd:MD_Distributor>
                        <gmd:distributorContact>
                            <gmd:CI_ResponsibleParty>
                                <gmd:organisationName>
                                    <gco:CharacterString>IFREMER / IDM/SISMER</gco:CharacterString>
                                </gmd:organisationName>
                                <gmd:contactInfo>
                                    <gmd:CI_Contact>
                                        <gmd:phone>
                                            <gmd:CI_Telephone>
                                                <gmd:voice>
                                                    <gco:CharacterString>+33 (0)2 98.22.49.16</gco:CharacterString>
                                                </gmd:voice>
                                                <gmd:facsimile>
                                                    <gco:CharacterString>+33 (0)2 98.22.46.44</gco:CharacterString>
                                                </gmd:facsimile>
                                            </gmd:CI_Telephone>
                                        </gmd:phone>
                                        <gmd:address>
                                            <gmd:CI_Address>
                                                <gmd:deliveryPoint>
                                                    <gco:CharacterString>Centre IFREMER de Brest BP 70</gco:CharacterString>
                                                </gmd:deliveryPoint>
                                                <gmd:city>
                                                    <gco:CharacterString>PLOUZANE</gco:CharacterString>
                                                </gmd:city>
                                                <gmd:postalCode>
                                                    <gco:CharacterString>29280</gco:CharacterString>
                                                </gmd:postalCode>
                                                <gmd:country>
                                                    <gmx:Anchor xlink:href="SDN:C320:2:FR">France</gmx:Anchor>
                                                </gmd:country>
                                                <gmd:electronicMailAddress>
                                                    <gco:CharacterString>sismer@ifremer.fr</gco:CharacterString>
                                                </gmd:electronicMailAddress>
                                            </gmd:CI_Address>
                                        </gmd:address>
                                        <gmd:onlineResource>
                                            <gmd:CI_OnlineResource>
                                                <gmd:linkage>
                                                    <gmd:URL>http://www.ifremer.fr/sismer/</gmd:URL>
                                                </gmd:linkage>
                                                <gmd:protocol>
                                                    <gco:CharacterString>http</gco:CharacterString>
                                                </gmd:protocol>
                                            </gmd:CI_OnlineResource>
                                        </gmd:onlineResource>
                                    </gmd:CI_Contact>
                                </gmd:contactInfo>
                                <gmd:role>
                                    <gmd:CI_RoleCode codeList="http://www.tc211.org/ISO19139/resources/codeList.xml#distributor" codeListValue="distributor"/>
                                </gmd:role>
                            </gmd:CI_ResponsibleParty>
                        </gmd:distributorContact>
                    </gmd:MD_Distributor>
                </gmd:distributor>
                <gmd:transferOptions>
                    <gmd:MD_DigitalTransferOptions>
                        <gmd:transferSize>
                            <gco:Real>2.431640625</gco:Real>
                        </gmd:transferSize>
                        <gmd:onLine>
                            <gmd:CI_OnlineResource>
                                <gmd:linkage>
                                    <gmd:URL>http://www.ifremer.fr/sismerData/jsp/visualisationMetadata3.jsp?langue=EN&amp;pageOrigine=CS&amp;cle1=42292_1&amp;cle2=CTDF02</gmd:URL>
                                </gmd:linkage>
                                <gmd:protocol>
                                    <gco:CharacterString>http</gco:CharacterString>
                                </gmd:protocol>
                                <gmd:description>
                                    <gco:CharacterString>CTDF02</gco:CharacterString>
                                </gmd:description>
                                <gmd:function>
                                    <gmd:CI_OnLineFunctionCode codeList="http://www.tc211.org/ISO19139/resources/codeList.xml#download" codeListValue="download"/>
                                </gmd:function>
                            </gmd:CI_OnlineResource>
                        </gmd:onLine>
                    </gmd:MD_DigitalTransferOptions>
                </gmd:transferOptions>
            </gmd:MD_Distribution>
        </gmd:distributionInfo>
    </gmd:MD_Metadata>
    """;

}
