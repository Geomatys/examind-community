/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014-2017 Geomatys.
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

package org.constellation.json.metadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.constellation.dto.metadata.JsonMetadataConstants;
import org.constellation.dto.metadata.RootObj;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class TemplateTreeTest {
    
    
    @Test
    public void testTreeFromEmptyTemplate() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        final InputStream stream = TemplateTreeTest.class.getResourceAsStream("profile_default_raster.json");
        final RootObj root       =  objectMapper.readValue(stream, RootObj.class);
        final TemplateTree tree  = TemplateTree.getTreeFromRootObj(root);
        final ValueNode result   = tree.getRoot();
        
        final ValueNode expresult   = new ValueNode("metadata", null, 0, null, null, false);
        
        final ValueNode dataQuality = new ValueNode("metadata.dataQualityInfo", null, 0, expresult, null, false);
        final ValueNode report      = new ValueNode("metadata.dataQualityInfo.report", "org.opengis.metadata.quality.DomainConsistency", 0, dataQuality, null, false);
        final ValueNode resultN     = new ValueNode("metadata.dataQualityInfo.report.result", "org.opengis.metadata.quality.ConformanceResult", 0, report, null, false);
        final ValueNode spec        = new ValueNode("metadata.dataQualityInfo.report.result.specification", null, 0, resultN, null, false);
        final ValueNode specTitle   = new ValueNode("metadata.dataQualityInfo.report.result.specification.title", null, null, "Specification.codelist", 0, null, spec, "metadata.field.report.specification.title", "elementary");
        final ValueNode specDate    = new ValueNode("metadata.dataQualityInfo.report.result.specification.date", null, 0, spec, null, false);
        final ValueNode specDateD   = new ValueNode("metadata.dataQualityInfo.report.result.specification.date.date", null, null, "DATE.readonly", 0, null, specDate, "metadata.field.report.specification.date", "elementary");
        final ValueNode specDateT   = new ValueNode("metadata.dataQualityInfo.report.result.specification.date.dateType", null, "CI_DateTypeCode.publication", "CODELIST.readonly", 0, null, specDate,  "metadata.field.dateType", "elementary");
        
        final ValueNode scope       = new ValueNode("metadata.dataQualityInfo.scope", null, 0, dataQuality, null, false);
        final ValueNode scopeLvl    = new ValueNode("metadata.dataQualityInfo.scope.level", null, "MD_ScopeCode.dataset", "CODELIST.readonly", 0, null, scope, "metadata.field.scopeLevel", "extended");
        
        final ValueNode pass        = new ValueNode("metadata.dataQualityInfo.report.result.pass", null, "nilReason:unknown", "ResultPass.codelist", 0, null, resultN, "metadata.field.resultPass", "extended");
        final ValueNode explanation = new ValueNode("metadata.dataQualityInfo.report.result.explanation", null, "See the referenced specification", "textarea", 0, null, resultN, "metadata.field.resultExplanation", "extended");
        
        final ValueNode ident       = new ValueNode("metadata.identificationInfo", null, 0, expresult, null, false);
        final ValueNode dkey        = new ValueNode("metadata.identificationInfo.descriptiveKeywords", null, 0, ident, "metadata.block.descriptiveKeyword", false);
        final ValueNode keyword     = new ValueNode("metadata.identificationInfo.descriptiveKeywords.keyword", null, null, "KEYWORD.text", 0, null, dkey, "metadata.field.keyword", "elementary");
        final ValueNode thesau      = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName", null, 0, dkey, null, false);
        final ValueNode thesauTitle = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.title", null, null, "text", 0, null, thesau, "metadata.field.thesaurusTitle", "extended");
        final ValueNode thesauDate  = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date", null, 0, thesau, null, false);
        final ValueNode thesauDateD = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date.date", null, null, "DATE.text", 0, null, thesauDate, "metadata.field.thesaurusDate", "extended");
        final ValueNode thesauDateT = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date.dateType", null, null, "DATE.codelist", 0, null, thesauDate, "metadata.field.thesaurusDateType", "extended");
        
        final ValueNode fid         = new ValueNode("metadata.fileIdentifier", null, null, "readonly", 0, null, expresult, "metadata.field.identifier", "elementary");
        final ValueNode date        = new ValueNode("metadata.dateStamp", null, null, "DATE.readonly", 0, null, expresult, "metadata.field.dateStamp", "elementary");
        final ValueNode stanName    = new ValueNode("metadata.metadataStandardName", null, "x-urn:schema:ISO19115:INSPIRE:dataset:geo-raster", "readonly", 0, null, expresult, "metadata.field.metadataStandardName", "elementary");
        final ValueNode stanVers    = new ValueNode("metadata.metadataStandardVersion", null, "2011.03", "readonly", 0, null, expresult, "metadata.field.metadataStandardVersion", "elementary");
        final ValueNode charac      = new ValueNode("metadata.characterSet", null, "UTF-8", "CharacterSet.codelist", 0, null, expresult, "metadata.field.characterSet", "elementary");
        final ValueNode lang        = new ValueNode("metadata.language", null, "LanguageCode.fra", "Language.codelist", 0, null, expresult, "metadata.field.metadatalanguage", "elementary");
        final ValueNode hierar      = new ValueNode("metadata.hierarchyLevel", null, "MD_ScopeCode.dataset", "CODELIST.readonly", 0, null, expresult, "metadata.field.hierarchyLevel", "elementary");
        
        valueNodeEquals(expresult, result);
        
    }
    
    @Test
    public void testTreeFromFilledTemplate() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        final InputStream stream  = TemplateTreeTest.class.getResourceAsStream("result.json");
        final RootObj root        =  objectMapper.readValue(stream, RootObj.class);
        final TemplateTree tree   = TemplateTree.getTreeFromRootObj(root);
        final ValueNode result    = tree.getRoot();
                
        final ValueNode expresult   = new ValueNode("metadata", null, 0, null, null, false);
        
        final ValueNode dataQuality = new ValueNode("metadata.dataQualityInfo", null, 0, expresult, null, false);
        final ValueNode report      = new ValueNode("metadata.dataQualityInfo.report", "org.opengis.metadata.quality.DomainConsistency", 0, dataQuality, null, false);
        final ValueNode resultN     = new ValueNode("metadata.dataQualityInfo.report.result", "org.opengis.metadata.quality.ConformanceResult", 0, report, null, false);
        final ValueNode spec        = new ValueNode("metadata.dataQualityInfo.report.result.specification", null, 0, resultN, null, false);
        final ValueNode specTitle   = new ValueNode("metadata.dataQualityInfo.report.result.specification.title", null, null, "Specification.codelist", 0, "some title", spec, "metadata.field.report.specification.title", "elementary");
        final ValueNode specDate    = new ValueNode("metadata.dataQualityInfo.report.result.specification.date", null, 0, spec, null, false);
        final ValueNode specDateD   = new ValueNode("metadata.dataQualityInfo.report.result.specification.date.date", null, null, "DATE.readonly", 0, "1970-05-10 00:00:03", specDate, "metadata.field.report.specification.date", "elementary");
        final ValueNode specDateT   = new ValueNode("metadata.dataQualityInfo.report.result.specification.date.dateType", null, "CI_DateTypeCode.publication", "CODELIST.readonly", 0, "CI_DateTypeCode.creation", specDate,  "metadata.field.dateType", "elementary");
        
        final ValueNode scope       = new ValueNode("metadata.dataQualityInfo.scope", null, 0, dataQuality, null, false);
        final ValueNode scopeLvl    = new ValueNode("metadata.dataQualityInfo.scope.level", null, "MD_ScopeCode.dataset", "CODELIST.readonly", 0, "MD_ScopeCode.dataset", scope, "metadata.field.scopeLevel", "extended");
        
        final ValueNode pass        = new ValueNode("metadata.dataQualityInfo.report.result.pass", null, "nilReason:unknown", "ResultPass.codelist", 0, "true", resultN, "metadata.field.resultPass", "extended");
        final ValueNode explanation = new ValueNode("metadata.dataQualityInfo.report.result.explanation", null, "See the referenced specification", "textarea", 0, "some explanation", resultN, "metadata.field.resultExplanation", "extended");
        
        final ValueNode ident       = new ValueNode("metadata.identificationInfo", null, 0, expresult, null, false);
        
        final ValueNode dkey        = new ValueNode("metadata.identificationInfo.descriptiveKeywords", null, 0, ident, "metadata.block.descriptiveKeyword", false);
        final ValueNode keyword11   = new ValueNode("metadata.identificationInfo.descriptiveKeywords.keyword", null, null, "KEYWORD.text", 0, "hello", dkey, "metadata.field.keyword", "elementary");
        final ValueNode keyword12   = new ValueNode("metadata.identificationInfo.descriptiveKeywords.keyword", null, null, "KEYWORD.text", 1, "world", dkey, "metadata.field.keyword", "elementary");
        final ValueNode thesau      = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName", null, 0, dkey, null, false);
        final ValueNode thesauTitle = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.title", null, null, "text", 0, null, thesau, "metadata.field.thesaurusTitle", "extended");
        final ValueNode thesauDate  = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date", null, 0, thesau, null, false);
        final ValueNode thesauDateD = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date.date", null, null, "DATE.text", 0, null, thesauDate, "metadata.field.thesaurusDate", "extended");
        final ValueNode thesauDateT = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date.dateType", null, null, "DATE.codelist", 0, null, thesauDate, "metadata.field.thesaurusDateType", "extended");
        
        final ValueNode dkey2        = new ValueNode("metadata.identificationInfo.descriptiveKeywords", null, 1, ident, "metadata.block.descriptiveKeyword", false);
        final ValueNode keyword21    = new ValueNode("metadata.identificationInfo.descriptiveKeywords.keyword", null, null, "KEYWORD.text", 0, "this", dkey2, "metadata.field.keyword", "elementary");
        final ValueNode keyword22    = new ValueNode("metadata.identificationInfo.descriptiveKeywords.keyword", null, null, "KEYWORD.text", 1, "is", dkey2, "metadata.field.keyword", "elementary");
        final ValueNode thesau2      = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName", null, 0, dkey2, null, false);
        final ValueNode thesauTitle2 = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.title", null, null, "text", 0, null, thesau2, "metadata.field.thesaurusTitle", "extended");
        final ValueNode thesauDate2  = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date", null, 0, thesau2, null, false);
        final ValueNode thesauDateD2 = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date.date", null, null, "DATE.text", 0, null, thesauDate2, "metadata.field.thesaurusDate", "extended");
        final ValueNode thesauDateT2 = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date.dateType", null, null, "DATE.codelist", 0, null, thesauDate2, "metadata.field.thesaurusDateType", "extended");
        
        final ValueNode fid         = new ValueNode("metadata.fileIdentifier", null, null, "readonly", 0, "metadata-id-0007", expresult, "metadata.field.identifier", "elementary");
        final ValueNode date        = new ValueNode("metadata.dateStamp", null, null, "DATE.readonly", 0, null, expresult, "metadata.field.dateStamp", "elementary");
        final ValueNode stanName    = new ValueNode("metadata.metadataStandardName", null, "x-urn:schema:ISO19115:INSPIRE:dataset:geo-raster", "readonly", 0, "x-urn:schema:ISO19115:INSPIRE:dataset:geo-raster", expresult, "metadata.field.metadataStandardName", "elementary");
        final ValueNode stanVers    = new ValueNode("metadata.metadataStandardVersion", null, "2011.03", "readonly", 0, "2011.03", expresult, "metadata.field.metadataStandardVersion", "elementary");
        final ValueNode charac      = new ValueNode("metadata.characterSet", null, "UTF-8", "CharacterSet.codelist", 0, "UTF-8", expresult, "metadata.field.characterSet", "elementary");
        final ValueNode lang        = new ValueNode("metadata.language", null, "LanguageCode.fra", "Language.codelist", 0, "LanguageCode.fra", expresult, "metadata.field.metadatalanguage", "elementary");
        final ValueNode hierar      = new ValueNode("metadata.hierarchyLevel", null, "MD_ScopeCode.dataset", "CODELIST.readonly", 0, "MD_ScopeCode.dataset", expresult, "metadata.field.hierarchyLevel", "elementary");
        
        valueNodeEquals(expresult, result);
    }
    
    @Test
    public void testTreeFromFilledTemplate2() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        final InputStream stream  = TemplateTreeTest.class.getResourceAsStream("result2.json");
        final RootObj root        =  objectMapper.readValue(stream, RootObj.class);
        final TemplateTree tree   = TemplateTree.getTreeFromRootObj(root);
        final ValueNode result    = tree.getRoot();
                
        final ValueNode expresult   = new ValueNode("metadata", null, 0, null, null, false);
        
        final ValueNode dataQuality = new ValueNode("metadata.dataQualityInfo", null, 0, expresult, null, false);
        final ValueNode report      = new ValueNode("metadata.dataQualityInfo.report", null, 0, dataQuality, null, false);
        final ValueNode resultN     = new ValueNode("metadata.dataQualityInfo.report.result", null, 0, report, null, false);
        final ValueNode spec        = new ValueNode("metadata.dataQualityInfo.report.result.specification", null, 0, resultN, null, false);
        final ValueNode specTitle   = new ValueNode("metadata.dataQualityInfo.report.result.specification.title", null, null, "Specification.codelist", 0, "some title", spec, "metadata.field.report.specification.title", "elementary");
        final ValueNode specDate    = new ValueNode("metadata.dataQualityInfo.report.result.specification.date", null, 0, spec, null, false);
        final ValueNode specDateD   = new ValueNode("metadata.dataQualityInfo.report.result.specification.date.date", null, null, "DATE.text", 0, "1970-05-10", specDate, "metadata.field.report.specification.date", "elementary");
        final ValueNode specDateT   = new ValueNode("metadata.dataQualityInfo.report.result.specification.date.dateType", null, "CI_DateTypeCode.publication", "CODELIST.readonly", 0, "CI_DateTypeCode.creation", specDate,  "metadata.field.dateType", "elementary");
        
        final ValueNode scope       = new ValueNode("metadata.dataQualityInfo.scope", null, 0, dataQuality, null, false);
        final ValueNode scopeLvl    = new ValueNode("metadata.dataQualityInfo.scope.level", null, "MD_ScopeCode.dataset", "CODELIST.readonly", 0, "MD_ScopeCode.dataset", scope, "metadata.field.scopeLevel", "extended");
        
        final ValueNode pass        = new ValueNode("metadata.dataQualityInfo.report.result.pass", null, "nilReason:unknown", "ResultPass.codelist", 0, "true", resultN, "metadata.field.resultPass", "extended");
        final ValueNode explanation = new ValueNode("metadata.dataQualityInfo.report.result.explanation", null, "See the referenced specification", "textarea", 0, "some explanation", resultN, "metadata.field.resultExplanation", "extended");
        
        final ValueNode ident       = new ValueNode("metadata.identificationInfo", null, 0, expresult, null, false);
        
        final ValueNode dkey        = new ValueNode("metadata.identificationInfo.descriptiveKeywords", null, 0, ident, "metadata.block.descriptiveKeyword", false);
        final ValueNode keyword11   = new ValueNode("metadata.identificationInfo.descriptiveKeywords.keyword", null, null, "KEYWORD.text", 0, "hello", dkey, "metadata.field.keyword", "elementary");
        final ValueNode keyword12   = new ValueNode("metadata.identificationInfo.descriptiveKeywords.keyword", null, null, "KEYWORD.text", 1, "world", dkey, "metadata.field.keyword", "elementary");
        final ValueNode thesau      = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName", null, 0, dkey, null, false);
        final ValueNode thesauTitle = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.title", null, null, "text", 0, null, thesau, "metadata.field.thesaurusTitle", "extended");
        final ValueNode thesauDate  = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date", null, 0, thesau, null, false);
        final ValueNode thesauDateD = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date.date", null, null, "DATE.text", 0, null, thesauDate, "metadata.field.thesaurusDate", "extended");
        final ValueNode thesauDateT = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date.dateType", null, null, "DATE.codelist", 0, null, thesauDate, "metadata.field.thesaurusDateType", "extended");
        
        final ValueNode dkey2        = new ValueNode("metadata.identificationInfo.descriptiveKeywords", null, 1, ident, "metadata.block.descriptiveKeyword", false);
        final ValueNode keyword21    = new ValueNode("metadata.identificationInfo.descriptiveKeywords.keyword", null, null, "KEYWORD.text", 0, "this", dkey2, "metadata.field.keyword", "elementary");
        final ValueNode keyword22    = new ValueNode("metadata.identificationInfo.descriptiveKeywords.keyword", null, null, "KEYWORD.text", 1, "is", dkey2, "metadata.field.keyword", "elementary");
        final ValueNode thesau2      = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName", null, 0, dkey2, null, false);
        final ValueNode thesauTitle2 = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.title", null, null, "text", 0, null, thesau2, "metadata.field.thesaurusTitle", "extended");
        final ValueNode thesauDate2  = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date", null, 0, thesau2, null, false);
        final ValueNode thesauDateD2 = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date.date", null, null, "DATE.text", 0, null, thesauDate2, "metadata.field.thesaurusDate", "extended");
        final ValueNode thesauDateT2 = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date.dateType", null, null, "DATE.codelist", 0, null, thesauDate2, "metadata.field.thesaurusDateType", "extended");
        
        final ValueNode constr       = new ValueNode("metadata.identificationInfo.resourceConstraints", "org.opengis.metadata.constraint.LegalConstraints", 0, ident, "metadata.block.legalconstraints", false);
        final ValueNode useLim       = new ValueNode("metadata.identificationInfo.resourceConstraints.useLimitation", null, null, "textarea", 0, null, constr, "metadata.field.useLimitation", "elementary");
        final ValueNode access       = new ValueNode("metadata.identificationInfo.resourceConstraints.accessConstraints", null, "MD_RestrictionCode.licence", "Restriction.codelist", 0, "MD_RestrictionCode.licence", constr, "metadata.field.accessConstraints", "extended");
        final ValueNode other        = new ValueNode("metadata.identificationInfo.resourceConstraints.otherConstraints", null, null, "textarea", 0, null, constr, "metadata.field.otherConstraints", "extended");
        
        final ValueNode constr2      = new ValueNode("metadata.identificationInfo.resourceConstraints", "org.opengis.metadata.constraint.SecurityConstraints", 1, ident, "metadata.block.securityconstraints", false);
        final ValueNode useLim2      = new ValueNode("metadata.identificationInfo.resourceConstraints.useLimitation", null, null, "textarea", 0, "some limitations", constr2, "metadata.field.useLimitation", "elementary");
        final ValueNode classif      = new ValueNode("metadata.identificationInfo.resourceConstraints.classification", null, "MD_ClassificationCode.unclassified", "Classification.codelist", 0, "MD_ClassificationCode.unclassified", constr2, "metadata.field.classification", "extended");
        final ValueNode useNote      = new ValueNode("metadata.identificationInfo.resourceConstraints.userNote", null, null, "textarea", 0, null, constr2, "metadata.field.userNote", "extended");
        
        final ValueNode fid         = new ValueNode("metadata.fileIdentifier", null, null, "readonly", 0, "metadata-id-0007", expresult, "metadata.field.identifier", "elementary");
        final ValueNode date        = new ValueNode("metadata.dateStamp", null, null, "DATE.readonly", 0, null, expresult, "metadata.field.dateStamp", "elementary");
        final ValueNode stanName    = new ValueNode("metadata.metadataStandardName", null, "x-urn:schema:ISO19115:INSPIRE:dataset:geo-raster", "readonly", 0, "x-urn:schema:ISO19115:INSPIRE:dataset:geo-raster", expresult, "metadata.field.metadataStandardName", "elementary");
        final ValueNode stanVers    = new ValueNode("metadata.metadataStandardVersion", null, "2011.03", "readonly", 0, "2011.03", expresult, "metadata.field.metadataStandardVersion", "elementary");
        final ValueNode charac      = new ValueNode("metadata.characterSet", null, "UTF-8", "CharacterSet.codelist", 0, "UTF-8", expresult, "metadata.field.characterSet", "extended");
        final ValueNode lang        = new ValueNode("metadata.language", null, "LanguageCode.fra", "Language.codelist", 0, "LanguageCode.fra", expresult, "metadata.field.metadatalanguage", "extended");
        final ValueNode hierar      = new ValueNode("metadata.hierarchyLevel", null, "MD_ScopeCode.dataset", "CODELIST.readonly", 0, "MD_ScopeCode.dataset", expresult, "metadata.field.hierarchyLevel", "extended");
        
        valueNodeEquals(expresult, result);
    }
    
    @Test
    public void testTreeFromFilledTemplate3() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        final InputStream stream  = TemplateTreeTest.class.getResourceAsStream("result3.json");
        final RootObj root        =  objectMapper.readValue(stream, RootObj.class);
        final TemplateTree tree   = TemplateTree.getTreeFromRootObj(root);
        final ValueNode result    = tree.getRoot();
                
        final ValueNode expresult   = new ValueNode("metadata", null, 0, null, null, false);
        
        final ValueNode dataQuality = new ValueNode("metadata.dataQualityInfo", null, 0, expresult, null, false);
        final ValueNode report      = new ValueNode("metadata.dataQualityInfo.report", null, 0, dataQuality, null, false);
        final ValueNode resultN     = new ValueNode("metadata.dataQualityInfo.report.result", null, 0, report, null, false);
        final ValueNode spec        = new ValueNode("metadata.dataQualityInfo.report.result.specification", null, 0, resultN, null, false);
        final ValueNode specTitle   = new ValueNode("metadata.dataQualityInfo.report.result.specification.title", null, null, "Specification.codelist", 0, "some title", spec, "metadata.field.report.specification.title", "elementary");
        final ValueNode specDate    = new ValueNode("metadata.dataQualityInfo.report.result.specification.date", null, 0, spec, null, false);
        final ValueNode specDateD   = new ValueNode("metadata.dataQualityInfo.report.result.specification.date.date", null, null, "DATE.text", 0, "1970-05-10", specDate, "metadata.field.report.specification.date", "elementary");
        final ValueNode specDateT   = new ValueNode("metadata.dataQualityInfo.report.result.specification.date.dateType", null, "CI_DateTypeCode.publication", "CODELIST.readonly", 0, "CI_DateTypeCode.creation", specDate,  "metadata.field.dateType", "elementary");
        
        final ValueNode scope       = new ValueNode("metadata.dataQualityInfo.scope", null, 0, dataQuality, null, false);
        final ValueNode scopeLvl    = new ValueNode("metadata.dataQualityInfo.scope.level", null, "MD_ScopeCode.dataset", "CODELIST.readonly", 0, "MD_ScopeCode.dataset", scope, "metadata.field.scopeLevel", "extended");
        
        final ValueNode pass        = new ValueNode("metadata.dataQualityInfo.report.result.pass", null, "nilReason:unknown", "ResultPass.codelist", 0, "true", resultN, "metadata.field.resultPass", "extended");
        final ValueNode explanation = new ValueNode("metadata.dataQualityInfo.report.result.explanation", null, "See the referenced specification", "textarea", 0, "some explanation", resultN, "metadata.field.resultExplanation", "extended");
        
        final ValueNode ident       = new ValueNode("metadata.identificationInfo", null, 0, expresult, null, false);
        
        final ValueNode dkey        = new ValueNode("metadata.identificationInfo.descriptiveKeywords", null, 0, ident, "metadata.block.descriptiveKeyword", false);
        final ValueNode keyword11   = new ValueNode("metadata.identificationInfo.descriptiveKeywords.keyword", null, null, "KEYWORD.text", 0, "hello", dkey, "metadata.field.keyword", "elementary");
        final ValueNode keyword12   = new ValueNode("metadata.identificationInfo.descriptiveKeywords.keyword", null, null, "KEYWORD.text", 1, "world", dkey, "metadata.field.keyword", "elementary");
        final ValueNode thesau      = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName", null, 0, dkey, null, false);
        final ValueNode thesauTitle = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.title", null, null, "text", 0, null, thesau, "metadata.field.thesaurusTitle", "extended");
        final ValueNode thesauDate  = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date", null, 0, thesau, null, false);
        final ValueNode thesauDateD = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date.date", null, null, "DATE.text", 0, null, thesauDate, "metadata.field.thesaurusDate", "extended");
        final ValueNode thesauDateT = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date.dateType", null, null, "DATE.codelist", 0, null, thesauDate, "metadata.field.thesaurusDateType", "extended");
        
        final ValueNode dkey2        = new ValueNode("metadata.identificationInfo.descriptiveKeywords", null, 1, ident, "metadata.block.descriptiveKeyword", false);
        final ValueNode keyword21    = new ValueNode("metadata.identificationInfo.descriptiveKeywords.keyword", null, null, "KEYWORD.text", 0, "this", dkey2, "metadata.field.keyword", "elementary");
        final ValueNode keyword22    = new ValueNode("metadata.identificationInfo.descriptiveKeywords.keyword", null, null, "KEYWORD.text", 1, "is", dkey2, "metadata.field.keyword", "elementary");
        final ValueNode thesau2      = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName", null, 0, dkey2, null, false);
        final ValueNode thesauTitle2 = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.title", null, null, "text", 0, null, thesau2, "metadata.field.thesaurusTitle", "extended");
        final ValueNode thesauDate2  = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date", null, 0, thesau2, null, false);
        final ValueNode thesauDateD2 = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date.date", null, null, "DATE.text", 0, null, thesauDate2, "metadata.field.thesaurusDate", "extended");
        final ValueNode thesauDateT2 = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date.dateType", null, null, "DATE.codelist", 0, null, thesauDate2, "metadata.field.thesaurusDateType", "extended");
        
        final ValueNode constr       = new ValueNode("metadata.identificationInfo.resourceConstraints", "org.opengis.metadata.constraint.LegalConstraints", 0, ident, "metadata.block.legalconstraints", false);
        final ValueNode useLim       = new ValueNode("metadata.identificationInfo.resourceConstraints.useLimitation", null, null, "textarea", 0, "legal limitations", constr, "metadata.field.useLimitation", "elementary");
        final ValueNode access       = new ValueNode("metadata.identificationInfo.resourceConstraints.accessConstraints", null, "MD_RestrictionCode.licence", "Restriction.codelist", 0, "MD_RestrictionCode.licence", constr, "metadata.field.accessConstraints", "extended");
        final ValueNode other        = new ValueNode("metadata.identificationInfo.resourceConstraints.otherConstraints", null, null, "textarea", 0, null, constr, "metadata.field.otherConstraints", "extended");
        
        final ValueNode constr2      = new ValueNode("metadata.identificationInfo.resourceConstraints", "org.opengis.metadata.constraint.SecurityConstraints", 1, ident, "metadata.block.securityconstraints", false);
        final ValueNode useLim2      = new ValueNode("metadata.identificationInfo.resourceConstraints.useLimitation", null, null, "textarea", 0, "some limitations", constr2, "metadata.field.useLimitation", "elementary");
        final ValueNode classif      = new ValueNode("metadata.identificationInfo.resourceConstraints.classification", null, "MD_ClassificationCode.unclassified", "Classification.codelist", 0, "MD_ClassificationCode.unclassified", constr2, "metadata.field.classification", "extended");
        final ValueNode useNote      = new ValueNode("metadata.identificationInfo.resourceConstraints.userNote", null, null, "textarea", 0, null, constr2, "metadata.field.userNote", "extended");
        
        final ValueNode fid         = new ValueNode("metadata.fileIdentifier", null, null, "readonly", 0, "metadata-id-0007", expresult, "metadata.field.identifier", "elementary");
        final ValueNode date        = new ValueNode("metadata.dateStamp", null, null, "DATE.readonly", 0, null, expresult, "metadata.field.dateStamp", "elementary");
        final ValueNode stanName    = new ValueNode("metadata.metadataStandardName", null, "x-urn:schema:ISO19115:INSPIRE:dataset:geo-raster", "readonly", 0, "x-urn:schema:ISO19115:INSPIRE:dataset:geo-raster", expresult, "metadata.field.metadataStandardName", "elementary");
        final ValueNode stanVers    = new ValueNode("metadata.metadataStandardVersion", null, "2011.03", "readonly", 0, "2011.03", expresult, "metadata.field.metadataStandardVersion", "elementary");
        final ValueNode charac      = new ValueNode("metadata.characterSet", null, "UTF-8", "CharacterSet.codelist", 0, "UTF-8", expresult, "metadata.field.characterSet", "extended");
        final ValueNode lang        = new ValueNode("metadata.language", null, "LanguageCode.fra", "Language.codelist", 0, "LanguageCode.fra", expresult, "metadata.field.metadatalanguage", "extended");
        final ValueNode hierar      = new ValueNode("metadata.hierarchyLevel", null, "MD_ScopeCode.dataset", "CODELIST.readonly", 0, "MD_ScopeCode.dataset", expresult, "metadata.field.hierarchyLevel", "extended");
        
        valueNodeEquals(expresult, result);
    }
    
    
    @Test
    public void testTreeFromFilledTemplateKeywords() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        final InputStream stream  = TemplateTreeTest.class.getResourceAsStream("result_keywords.json");
        final RootObj root        =  objectMapper.readValue(stream, RootObj.class);
        final TemplateTree tree   = TemplateTree.getTreeFromRootObj(root);
        final ValueNode result    = tree.getRoot();
                
        final ValueNode expresult   = new ValueNode("metadata", null, 0, null, null, false);
        
        final ValueNode ident       = new ValueNode("metadata.identificationInfo", null, 0, expresult, null, false);
        
        final ValueNode dkey        = new ValueNode("metadata.identificationInfo.descriptiveKeywords", null, 0, ident, "metadata.block.descriptiveKeyword", false);
        final ValueNode keyword11   = new ValueNode("metadata.identificationInfo.descriptiveKeywords.keyword", null, null, "KEYWORD.text", 0, "hello", dkey, "metadata.field.keyword", "elementary");
        final ValueNode keyword12   = new ValueNode("metadata.identificationInfo.descriptiveKeywords.keyword", null, null, "KEYWORD.text", 1, "world", dkey, "metadata.field.keyword", "elementary");
        
        final ValueNode dkey2        = new ValueNode("metadata.identificationInfo.descriptiveKeywords", null, 1, ident, "metadata.block.descriptiveKeyword", false);
        final ValueNode keyword21    = new ValueNode("metadata.identificationInfo.descriptiveKeywords.keyword", null, null, "KEYWORD.text", 0, "this", dkey2, "metadata.field.keyword", "elementary");
        final ValueNode keyword22    = new ValueNode("metadata.identificationInfo.descriptiveKeywords.keyword", null, null, "KEYWORD.text", 1, "is", dkey2, "metadata.field.keyword", "elementary");
        
        valueNodeEquals(expresult, result);
    }
    
    @Test
    public void testTreeFromEmptyTemplateKeywords3() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        final InputStream stream  = TemplateTreeTest.class.getResourceAsStream("profile_keywords3.json");
        final RootObj root        =  objectMapper.readValue(stream, RootObj.class);
        final TemplateTree tree   = TemplateTree.getTreeFromRootObj(root);
        final ValueNode result    = tree.getRoot();
                
        final ValueNode expresult   = new ValueNode("metadata", null, 0, null, null, false);
        
        final ValueNode ident       = new ValueNode("metadata.identificationInfo", null, 0, expresult, null, false);
        
        final ValueNode dkey        = new ValueNode("metadata.identificationInfo.descriptiveKeywords", null, 0, ident, null, false);
        final ValueNode keyword1    = new ValueNode("metadata.identificationInfo.descriptiveKeywords.keyword", null, null, "KEYWORD.text", 0, null, dkey, "metadata.field.keywordReserved", "elementary");
        keyword1.strict = true;
        keyword1.getPredefinedValues().add("value1");
        keyword1.getPredefinedValues().add("value2");
        final ValueNode keyword2    = new ValueNode("metadata.identificationInfo.descriptiveKeywords.keyword", null, null, "KEYWORD.text", 1, null, dkey, "metadata.field.keywordFree", "elementary");
        
        valueNodeEquals(expresult, result);
    }
    
    @Test
    public void testTreeFromFilledTemplateKeywordsUI() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        final InputStream stream  = TemplateTreeTest.class.getResourceAsStream("result_keywords_UI.json");
        final RootObj root        =  objectMapper.readValue(stream, RootObj.class);
        final TemplateTree tree   = TemplateTree.getTreeFromRootObj(root);
        final ValueNode result    = tree.getRoot();
                
        final ValueNode expresult   = new ValueNode("metadata", null, 0, null, null, false);
        
        final ValueNode ident       = new ValueNode("metadata.identificationInfo", null, 0, expresult, null, false);
        
        final ValueNode dkey        = new ValueNode("metadata.identificationInfo.descriptiveKeywords", null, 0, ident, "metadata.block.descriptiveKeyword", false);
        final ValueNode keyword11   = new ValueNode("metadata.identificationInfo.descriptiveKeywords.keyword", null, null, "KEYWORD.text", 0, "hello", dkey, "metadata.field.keyword", "elementary");
        final ValueNode keyword12   = new ValueNode("metadata.identificationInfo.descriptiveKeywords.keyword", null, null, "KEYWORD.text", 1, "world", dkey, "metadata.field.keyword", "elementary");
        
        final ValueNode dkey2        = new ValueNode("metadata.identificationInfo.descriptiveKeywords", null, 1, ident, "metadata.block.descriptiveKeyword", false);
        final ValueNode keyword21    = new ValueNode("metadata.identificationInfo.descriptiveKeywords.keyword", null, null, "KEYWORD.text", 0, "this", dkey2, "metadata.field.keyword", "elementary");
        final ValueNode keyword22    = new ValueNode("metadata.identificationInfo.descriptiveKeywords.keyword", null, null, "KEYWORD.text", 1, "is", dkey2, "metadata.field.keyword", "elementary");
        
        final ValueNode dkey3        = new ValueNode("metadata.identificationInfo.descriptiveKeywords", null, 2, ident, "metadata.block.descriptiveKeyword", false);
        final ValueNode keyword31    = new ValueNode("metadata.identificationInfo.descriptiveKeywords.keyword", null, null, "KEYWORD.text", 0, "my", dkey3, "metadata.field.keyword", "elementary");
        final ValueNode keyword32    = new ValueNode("metadata.identificationInfo.descriptiveKeywords.keyword", null, null, "KEYWORD.text", 1, "test", dkey3, "metadata.field.keyword", "elementary");
        
        valueNodeEquals(expresult, result);
    }
    
    @Test
    public void testTreeFromFilledTemplateKeywordsUI2() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        final InputStream stream  = TemplateTreeTest.class.getResourceAsStream("result_keywords2_UI.json");
        final RootObj root        =  objectMapper.readValue(stream, RootObj.class);
        final TemplateTree tree   = TemplateTree.getTreeFromRootObj(root);
        final ValueNode result    = tree.getRoot();
                
        final ValueNode expresult   = new ValueNode("metadata", null, 0, null, null, false);
        
        final ValueNode ident       = new ValueNode("metadata.identificationInfo", null, 0, expresult, null, false);
        
        final ValueNode dkey        = new ValueNode("metadata.identificationInfo.descriptiveKeywords", null, 0, ident, "metadata.block.descriptiveKeyword_inspire", true);
        final ValueNode keyword11   = new ValueNode("metadata.identificationInfo.descriptiveKeywords.keyword", null, null, "KEYWORD.text", 0, "hello", dkey, "metadata.field.keyword", "elementary");
        final ValueNode keyword12   = new ValueNode("metadata.identificationInfo.descriptiveKeywords.keyword", null, null, "KEYWORD.text", 1, "world", dkey, "metadata.field.keyword", "elementary");
        final ValueNode thesau      = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName", null, 0, dkey, null, false);
        final ValueNode thesauTitle = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.title", null, "GEMET", "readonly", 0, "GEMET", thesau, "metadata.field.thesaurusTitle", "elementary");
        final ValueNode thesauDate  = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date", null, 0, thesau, null, false);
        final ValueNode thesauDateD = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date.date", null, "2012-01-01", "DATE.text", 0, "2012-01-01", thesauDate, "metadata.field.thesaurusDate", "elementary");
        final ValueNode thesauDateT = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date.dateType", null, "CI_DateTypeCode.publication", "CODELIST.readonly", 0, "CI_DateTypeCode.publication", thesauDate, "metadata.field.thesaurusDateType", "elementary");
        
        final ValueNode dkey2        = new ValueNode("metadata.identificationInfo.descriptiveKeywords", null, 1, ident, "metadata.block.descriptiveKeyword_inspire", true);
        final ValueNode keyword21    = new ValueNode("metadata.identificationInfo.descriptiveKeywords.keyword", null, null, "KEYWORD.text", 0, "hey", dkey2, "metadata.field.keyword", "elementary");
        final ValueNode keyword22    = new ValueNode("metadata.identificationInfo.descriptiveKeywords.keyword", null, null, "KEYWORD.text", 1, "you", dkey2, "metadata.field.keyword", "elementary");
        final ValueNode thesau2      = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName", null, 0, dkey2, null, false);
        final ValueNode thesauTitle2 = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.title", null, "GEMET", "readonly", 0, "GEMET", thesau2, "metadata.field.thesaurusTitle", "elementary");
        final ValueNode thesauDate2  = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date", null, 0, thesau2, null, false);
        final ValueNode thesauDateD2 = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date.date", null, "2012-01-01", "DATE.text", 0, "2012-01-01", thesauDate2, "metadata.field.thesaurusDate", "elementary");
        final ValueNode thesauDateT2 = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date.dateType", null, "CI_DateTypeCode.publication", "CODELIST.readonly", 0, "CI_DateTypeCode.publication", thesauDate2, "metadata.field.thesaurusDateType", "elementary");
        
        final ValueNode dkey3        = new ValueNode("metadata.identificationInfo.descriptiveKeywords", null, 2, ident, "metadata.block.descriptiveKeyword", false);
        final ValueNode keyword31    = new ValueNode("metadata.identificationInfo.descriptiveKeywords.keyword", null, null, "KEYWORD.text", 0, "this", dkey3, "metadata.field.keyword", "elementary");
        final ValueNode keyword32    = new ValueNode("metadata.identificationInfo.descriptiveKeywords.keyword", null, null, "KEYWORD.text", 1, "is", dkey3, "metadata.field.keyword", "elementary");
        final ValueNode thesau3      = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName", null, 0, dkey3, null, false);
        final ValueNode thesauTitle3 = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.title", null, null, "text", 0, null, thesau3, "metadata.field.thesaurusTitle", "elementary");
        final ValueNode thesauDate3  = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date", null, 0, thesau3, null, false);
        final ValueNode thesauDateD3 = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date.date", null, null, "DATE.text", 0, null, thesauDate3, "metadata.field.thesaurusDate", "elementary");
        final ValueNode thesauDateT3 = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date.dateType", null, null, "DATE.codelist", 0, null, thesauDate3, "metadata.field.thesaurusDateType", "elementary");
        
        valueNodeEquals(expresult, result);
    }
    
    @Test
    public void testTreeFromFilledTemplateKeywords3() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        final InputStream stream  = TemplateTreeTest.class.getResourceAsStream("result_keywords3.json");
        final RootObj root        =  objectMapper.readValue(stream, RootObj.class);
        final TemplateTree tree   = TemplateTree.getTreeFromRootObj(root);
        final ValueNode result    = tree.getRoot();
                
        final ValueNode expresult   = new ValueNode("metadata", null, 0, null, null, false);
        
        final ValueNode ident       = new ValueNode("metadata.identificationInfo", null, 0, expresult, null, false);
        
        final ValueNode dkey        = new ValueNode("metadata.identificationInfo.descriptiveKeywords", null, 0, ident, "metadata.block.descriptiveKeyword_inspire", true);
        final ValueNode keyword11   = new ValueNode("metadata.identificationInfo.descriptiveKeywords.keyword", null, null, "KEYWORD.text", 0, "hello", dkey, "metadata.field.keyword", "elementary");
        final ValueNode keyword12   = new ValueNode("metadata.identificationInfo.descriptiveKeywords.keyword", null, null, "KEYWORD.text", 1, "world", dkey, "metadata.field.keyword", "elementary");
        final ValueNode thesau      = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName", null, 0, dkey, null, false);
        final ValueNode thesauTitle = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.title", null, "GEMET", "readonly", 0, "GEMET", thesau, "metadata.field.thesaurusTitle", "elementary");
        final ValueNode thesauDate  = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date", null, 0, thesau, null, false);
        final ValueNode thesauDateD = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date.date", null, "2012-01-01", "DATE.text", 0, "2012-01-01", thesauDate, "metadata.field.thesaurusDate", "elementary");
        final ValueNode thesauDateT = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date.dateType", null, "CI_DateTypeCode.publication", "CODELIST.readonly", 0, "CI_DateTypeCode.publication", thesauDate, "metadata.field.thesaurusDateType", "elementary");
        
        final ValueNode dkey2        = new ValueNode("metadata.identificationInfo.descriptiveKeywords", null, 1, ident, "metadata.block.descriptiveKeyword_inspire", true);
        final ValueNode keyword21    = new ValueNode("metadata.identificationInfo.descriptiveKeywords.keyword", null, null, "KEYWORD.text", 0, "this", dkey2, "metadata.field.keyword", "elementary");
        final ValueNode keyword22    = new ValueNode("metadata.identificationInfo.descriptiveKeywords.keyword", null, null, "KEYWORD.text", 1, "is", dkey2, "metadata.field.keyword", "elementary");
        final ValueNode thesau2      = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName", null, 0, dkey2, null, false);
        final ValueNode thesauTitle2 = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.title", null, "GEMET", "readonly", 0, "GEMET", thesau2, "metadata.field.thesaurusTitle", "elementary");
        final ValueNode thesauDate2  = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date", null, 0, thesau2, null, false);
        final ValueNode thesauDateD2 = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date.date", null, "2012-01-01", "DATE.text", 0, "2012-01-01", thesauDate2, "metadata.field.thesaurusDate", "elementary");
        final ValueNode thesauDateT2 = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date.dateType", null, "CI_DateTypeCode.publication", "CODELIST.readonly", 0, "CI_DateTypeCode.publication", thesauDate2, "metadata.field.thesaurusDateType", "elementary");
        
        final ValueNode dkey3        = new ValueNode("metadata.identificationInfo.descriptiveKeywords", null, 2, ident, "metadata.block.descriptiveKeyword", false);
        final ValueNode keyword31    = new ValueNode("metadata.identificationInfo.descriptiveKeywords.keyword", null, null, "KEYWORD.text", 0, null, dkey3, "metadata.field.keyword", "elementary");
        final ValueNode thesau3      = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName", null, 0, dkey3, null, false);
        final ValueNode thesauTitle3 = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.title", null, null, "text", 0, null, thesau3, "metadata.field.thesaurusTitle", "elementary");
        final ValueNode thesauDate3  = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date", null, 0, thesau3, null, false);
        final ValueNode thesauDateD3 = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date.date", null, null, "DATE.text", 0, null, thesauDate3, "metadata.field.thesaurusDate", "elementary");
        final ValueNode thesauDateT3 = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date.dateType", null, null, "DATE.codelist", 0, null, thesauDate3, "metadata.field.thesaurusDateType", "elementary");
        
        valueNodeEquals(expresult, result);
    }
    
    @Test
    public void testTreeFromEmptyTemplateMultipleBlock() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        final InputStream stream  = TemplateTreeTest.class.getResourceAsStream("profile_multiple_block.json");
        final RootObj root        =  objectMapper.readValue(stream, RootObj.class);
        final TemplateTree tree   = TemplateTree.getTreeFromRootObj(root);
        final ValueNode result    = tree.getRoot();
                
        final ValueNode expresult   = new ValueNode("metadata", null, 0, null, null, false);
        
        final ValueNode ident       = new ValueNode("metadata.identificationInfo", null, 0, expresult, null, false);
        
        final ValueNode dkey        = new ValueNode("metadata.identificationInfo.descriptiveKeywords", null, 0, ident, "metadata.block.descriptiveKeyword", false);
        final ValueNode keyword11   = new ValueNode("metadata.identificationInfo.descriptiveKeywords.keyword", null, null, "KEYWORD.text", 0, null, dkey, "metadata.field.keyword", "elementary");
        final ValueNode thesau      = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName", null, 0, dkey, null, false);
        final ValueNode thesauTitle = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.title", null, null, "text", 0, null, thesau, "metadata.field.thesaurusTitle", "elementary");
        final ValueNode thesauDate  = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date", null, 0, thesau, "metadata.block.descriptiveKeyword_thesaurus_date", false);
        final ValueNode thesauDateD = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date.date", null, null, "DATE.text", 0, null, thesauDate, "metadata.field.thesaurusDate", "elementary");
        final ValueNode thesauDateT = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date.dateType", null, null, "DATE.codelist", 0, null, thesauDate, "metadata.field.thesaurusDateType", "elementary");
        
        
        
        valueNodeEquals(expresult, result);
    }
    
    @Test
    public void testTreeFromFilledTemplateMultipleBlock() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        final InputStream stream  = TemplateTreeTest.class.getResourceAsStream("result_multiple_block.json");
        final RootObj root        =  objectMapper.readValue(stream, RootObj.class);
        final TemplateTree tree   = TemplateTree.getTreeFromRootObj(root);
        final ValueNode result    = tree.getRoot();
                
        final ValueNode expresult   = new ValueNode("metadata", null, 0, null, null, false);
        
        final ValueNode ident       = new ValueNode("metadata.identificationInfo", null, 0, expresult, null, false);
        
        final ValueNode dkey        = new ValueNode("metadata.identificationInfo.descriptiveKeywords", null, 0, ident, "metadata.block.descriptiveKeyword", false);
        final ValueNode keyword11   = new ValueNode("metadata.identificationInfo.descriptiveKeywords.keyword", null, null, "KEYWORD.text", 0, "hello", dkey, "metadata.field.keyword", "elementary");
        final ValueNode keyword12   = new ValueNode("metadata.identificationInfo.descriptiveKeywords.keyword", null, null, "KEYWORD.text", 1, "world", dkey, "metadata.field.keyword", "elementary");
        final ValueNode thesau      = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName", null, 0, dkey, null, false);
        final ValueNode thesauTitle = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.title", null, null, "text", 0, "GEMET", thesau, "metadata.field.thesaurusTitle", "elementary");
        final ValueNode thesauDate  = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date", null, 0, thesau, "metadata.block.descriptiveKeyword_thesaurus_date", false);
        final ValueNode thesauDateD = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date.date", null, null, "DATE.text", 0, "2012-01-01", thesauDate, "metadata.field.thesaurusDate", "elementary");
        final ValueNode thesauDateT = new ValueNode("metadata.identificationInfo.descriptiveKeywords.thesaurusName.date.dateType", null, null, "DATE.codelist", 0, "CI_DateTypeCode.publication", thesauDate, "metadata.field.thesaurusDateType", "elementary");
        
        
        
        valueNodeEquals(expresult, result);
    }
    
    @Test
    public void buildNumeratedPath() {
        String orig      =  "metadata.identificationInfo.descriptiveKeywords";
        String result    = JsonMetadataConstants.buildNumeratedPath(orig, 0);
        String expResult =  "metadata[0].identificationInfo[0].descriptiveKeywords[0]";
        
        Assert.assertEquals(expResult, result);
        
        result = JsonMetadataConstants.buildNumeratedPath(expResult, 0);
        
        Assert.assertNull(result);
        
    }
    private static void valueNodeEquals(final ValueNode expected, final ValueNode result) {
        if (expected.children.size() == result.children.size()) {
            for (int i = 0 ; i < expected.children.size(); i++) {
                ValueNode expChild = expected.children.get(i);
                ValueNode resChild = result.children.get(i);
                valueNodeEquals(expChild, resChild);
            } 
        }
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testTreeMultiQUalityReport() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        final InputStream stream = TemplateTreeTest.class.getResourceAsStream("multi_quality_report.json");
        final RootObj root       =  objectMapper.readValue(stream, RootObj.class);
        final TemplateTree tree  = TemplateTree.getTreeFromRootObj(root);
        final ValueNode result   = tree.getRoot();

        List<String> titles = new ArrayList<>();
        titles.add("Data Specification on Agricultural and Aquaculture Facilities");
        titles.add("Data Specification on Area management / restriction / regulation zones and reporting units");
        titles.add("Data Specification on Atmospheric Conditions- Meteorological geographical features");
        titles.add("Data Specification on Bio-geographical regions");
        titles.add("Data Specification on Buildings");
        titles.add("Data Specification on Elevation");
        titles.add("Data Specification on Energy Resources");
        titles.add("Data Specification on Environmental monitoring Facilities");
        titles.add("Data Specification on Geology");
        titles.add("Data Specification on Habitats and biotopes");
        titles.add("Data Specification on Human health and safety");
        titles.add("Data Specification on Land cover");
        titles.add("Data Specification on Land use");
        titles.add("Data Specification on Mineral Resources");
        titles.add("Data Specification on Natural risk zones");
        titles.add("Data Specification on Oceanographic geographical features");
        titles.add("Data Specification on Orthoimagery");
        titles.add("Data Specification on Population distribution - demography");
        titles.add("Data Specification on Production and Industrial Facilities");
        titles.add("Data Specification on Sea regions");
        titles.add("Data Specification on Soil");
        titles.add("Data Specification on Species distribution");
        titles.add("Data Specification on Statistical units");
        titles.add("Data Specification on Utility and Government Services");
        titles.add("INSPIRE Data Specification on Administrative Units");
        titles.add("INSPIRE Data Specification on Cadastral Parcels");
        titles.add("INSPIRE Data Specification on Geographical Names");
        titles.add("INSPIRE Data Specification on Hydrography");
        titles.add("INSPIRE Data Specification on Protected Sites");
        titles.add("INSPIRE Data Specification on Transport Networks");
        titles.add("INSPIRE Data Specifications on Addresses");
        titles.add("INSPIRE Specification on Coordinate Reference Systems");
        titles.add("INSPIRE Specification on Geographical Grid Systems");
        titles.add("No INSPIRE Data Specification");
        titles.add("RÈGLEMENT (UE) N°1089/2010");

        List<String> resultPass = new ArrayList<>();
        resultPass.add("nilReason:unknown");
        resultPass.add("false");
        resultPass.add("true");
        
        final ValueNode expresult   = new ValueNode("metadata", null, 0, null, null, false);

        final ValueNode dataQuality = new ValueNode("metadata.dataQualityInfo", null, 0, expresult, null, false);
        
        final ValueNode scope       = new ValueNode("metadata.dataQualityInfo.scope", null, 0, dataQuality, null, false);
        final ValueNode scopeLvl    = new ValueNode("metadata.dataQualityInfo.scope.level", null, "MD_ScopeCode.dataset", "CODELIST.readonly", 0, "MD_ScopeCode.dataset", scope, "INSPIRE.cg2a_dataset_geo.dataQuality.report.scopeLevel", "ELEMENTARY");

        
        final ValueNode report      = new ValueNode("metadata.dataQualityInfo.report", null, 0, dataQuality, "INSPIRE.cg2a_dataset_geo.dataQuality.report", false);
        final ValueNode resultN     = new ValueNode("metadata.dataQualityInfo.report.result",  null, 0, report, null, false);
        final ValueNode spec        = new ValueNode("metadata.dataQualityInfo.report.result.specification", null, 0, resultN, null, false);
        final ValueNode specTitle   = new ValueNode("metadata.dataQualityInfo.report.result.specification.title", null, null, "predefinedValues.select", 0, "Data Specification on Buildings", spec, "INSPIRE.cg2a_dataset_geo.dataQuality.report.title", "ELEMENTARY");
        specTitle.predefinedValues = titles;
        specTitle.strict = true;
        final ValueNode specDate    = new ValueNode("metadata.dataQualityInfo.report.result.specification.date", null, 0, spec, null, false);
        final ValueNode specDateD   = new ValueNode("metadata.dataQualityInfo.report.result.specification.date.date", null, "2010-05-03", "readonly", 0, "2010-05-03", specDate, "INSPIRE.cg2a_dataset_geo.dataQuality.report.date", "ELEMENTARY");
        final ValueNode specDateT   = new ValueNode("metadata.dataQualityInfo.report.result.specification.date.dateType", null, "CI_DateTypeCode.publication", "CODELIST.readonly", 0, "CI_DateTypeCode.publication", specDate,  "INSPIRE.cg2a_dataset_geo.dataQuality.report.dateType", "ELEMENTARY");

        final ValueNode pass        = new ValueNode("metadata.dataQualityInfo.report.result.pass", null, "nilReason:unknown", "predefinedValues.select.ResultPass", 0, "nilReason:unknown", resultN, "INSPIRE.cg2a_dataset_geo.dataQuality.report.resultPass", "ELEMENTARY");
        pass.predefinedValues = resultPass;
        final ValueNode explanation = new ValueNode("metadata.dataQualityInfo.report.result.explanation", null, "See the referenced specification", "textarea", 0, "See the referenced specification", resultN,  "INSPIRE.cg2a_dataset_geo.dataQuality.report.resultExplanation", "ELEMENTARY");

        final ValueNode report2      = new ValueNode("metadata.dataQualityInfo.report", null, 1, dataQuality, "INSPIRE.cg2a_dataset_geo.dataQuality.report", false);
        final ValueNode resultN2     = new ValueNode("metadata.dataQualityInfo.report.result", null, 0, report2, null, false);
        final ValueNode spec2        = new ValueNode("metadata.dataQualityInfo.report.result.specification", null, 0, resultN2, null, false);
        final ValueNode specTitle2   = new ValueNode("metadata.dataQualityInfo.report.result.specification.title", null, null, "predefinedValues.select", 0, "Data Specification on Elevation", spec2, "INSPIRE.cg2a_dataset_geo.dataQuality.report.title", "ELEMENTARY");
        specTitle2.predefinedValues = titles;
        specTitle2.strict = true;
        final ValueNode specDate2    = new ValueNode("metadata.dataQualityInfo.report.result.specification.date", null, 0, spec2, null, false);
        final ValueNode specDateD2   = new ValueNode("metadata.dataQualityInfo.report.result.specification.date.date", null, "2010-05-03", "readonly", 0, "2010-05-03", specDate2, "INSPIRE.cg2a_dataset_geo.dataQuality.report.date", "ELEMENTARY");
        final ValueNode specDateT2   = new ValueNode("metadata.dataQualityInfo.report.result.specification.date.dateType", null, "CI_DateTypeCode.publication", "CODELIST.readonly", 0, "CI_DateTypeCode.publication", specDate2,  "INSPIRE.cg2a_dataset_geo.dataQuality.report.dateType", "ELEMENTARY");

        final ValueNode pass2        = new ValueNode("metadata.dataQualityInfo.report.result.pass", null, "nilReason:unknown", "predefinedValues.select.ResultPass", 0, "nilReason:unknown", resultN2, "INSPIRE.cg2a_dataset_geo.dataQuality.report.resultPass", "ELEMENTARY");
        pass2.predefinedValues = resultPass;
        final ValueNode explanation2 = new ValueNode("metadata.dataQualityInfo.report.result.explanation", null, "See the referenced specification", "textarea", 0, "See the referenced specification", resultN2, "INSPIRE.cg2a_dataset_geo.dataQuality.report.resultExplanation", "ELEMENTARY");

        

        valueNodeEquals(expresult, result);

    }

    @Test
    public void testTreeInspireKeywordsTopicCategory() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        final InputStream stream = TemplateTreeTest.class.getResourceAsStream("result_inspire_keywords_topic.json");
        final RootObj root       =  objectMapper.readValue(stream, RootObj.class);
        final TemplateTree tree  = TemplateTree.getTreeFromRootObj(root);
        final ValueNode result   = tree.getRoot();
        System.out.println(result.treeRepresentation());
        // TODO
    }
}
