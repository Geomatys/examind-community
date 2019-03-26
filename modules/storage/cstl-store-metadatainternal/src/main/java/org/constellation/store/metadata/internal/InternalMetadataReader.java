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
package org.constellation.store.metadata.internal;

import org.apache.sis.internal.xml.LegacyNamespaces;
import org.constellation.admin.SpringHelper;
import org.constellation.store.metadata.CSWMetadataReader;
import org.constellation.metadata.io.DomMetadataReader;
import org.geotoolkit.metadata.ElementSetType;
import org.geotoolkit.metadata.MetadataIoException;
import org.geotoolkit.metadata.MetadataType;
import org.constellation.util.NodeUtilities;
import org.geotoolkit.csw.xml.DomainValues;
import org.geotoolkit.csw.xml.v202.DomainValuesType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.constellation.api.PathType;
import org.constellation.business.IInternalMetadataBusiness;

import static org.constellation.metadata.CSWQueryable.DUBLIN_CORE_QUERYABLE;
import static org.constellation.metadata.CSWQueryable.ISO_QUERYABLE;
import static org.constellation.util.NodeUtilities.getNodeFromReader;
import static org.geotoolkit.csw.xml.TypeNames.METADATA_QNAME;
import static org.geotoolkit.dublincore.xml.v2.elements.ObjectFactory._Creator_QNAME;
import static org.geotoolkit.dublincore.xml.v2.elements.ObjectFactory._Date_QNAME;
import static org.geotoolkit.dublincore.xml.v2.elements.ObjectFactory._Description_QNAME;
import static org.geotoolkit.dublincore.xml.v2.elements.ObjectFactory._Format_QNAME;
import static org.geotoolkit.dublincore.xml.v2.elements.ObjectFactory._Identifier_QNAME;
import static org.geotoolkit.dublincore.xml.v2.elements.ObjectFactory._Language_QNAME;
import static org.geotoolkit.dublincore.xml.v2.elements.ObjectFactory._Publisher_QNAME;
import static org.geotoolkit.dublincore.xml.v2.elements.ObjectFactory._Subject_QNAME;
import static org.geotoolkit.dublincore.xml.v2.elements.ObjectFactory._Title_QNAME;
import static org.geotoolkit.dublincore.xml.v2.elements.ObjectFactory._Type_QNAME;
import static org.geotoolkit.dublincore.xml.v2.terms.ObjectFactory._Abstract_QNAME;
import static org.geotoolkit.dublincore.xml.v2.terms.ObjectFactory._Modified_QNAME;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.OPERATION_NOT_SUPPORTED;
import static org.geotoolkit.ows.xml.v100.ObjectFactory._BoundingBox_QNAME;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.SAXException;


/**
 * A CSW Metadata Reader. This reader does not require a database.
 * The CSW records are stored XML file in a directory .
 *
 * This reader can be used for test purpose or in case of small amount of record.
 *
 * @author Guilhem Legal (Geomatys)
 */
public class InternalMetadataReader extends DomMetadataReader implements CSWMetadataReader {

    @Autowired
    private IInternalMetadataBusiness internalMetadataBusiness;

    /**
     * Build a new CSW File Reader.
     *
     * @param configuration A map of configuration flag
     * @param additionalQueryable
     *
     * @throws MetadataIoException If the configuration object does
     * not contains an existing directory path in the configuration.dataDirectory field.
     * If the creation of a MarshallerPool throw a JAXBException.
     */
    public InternalMetadataReader(final Map configuration, final Map<String, PathType> additionalQueryable) throws MetadataIoException {
        super(true, false, additionalQueryable);
        SpringHelper.injectDependencies(this);
        if (configuration != null && configuration.get("enable-thread") != null) {
            final boolean t = Boolean.parseBoolean((String) configuration.get("enable-thread"));
            if (t) {
                LOGGER.info("parrallele treatment enabled");
            }
            setIsThreadEnabled(t);
        }
        if (configuration != null && configuration.get("enable-cache") != null) {
            final boolean c = Boolean.parseBoolean((String) configuration.get("enable-cache"));
            if (!c) {
                LOGGER.info("cache system have been disabled");
            }
            setIsCacheEnabled(c);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Node getMetadata(final String identifier, final MetadataType mode) throws MetadataIoException {
        return getMetadata(identifier, mode, ElementSetType.FULL, new ArrayList<QName>());
    }

     /**
     * {@inheritDoc}
     */
    @Override
    public Node getMetadata(final String identifier, final MetadataType mode, final ElementSetType type, final List<QName> elementName) throws MetadataIoException {
        final String metadataString = internalMetadataBusiness.getMetadata(identifier);
        if (metadataString != null) {
            final MetadataType metadataMode;
            final Node metadataNode;
            try {
                metadataMode = getMetadataType(new StringReader(metadataString), false);
                metadataNode = getNodeFromReader(new StringReader(metadataString));
            } catch (IOException | XMLStreamException | ParserConfigurationException | SAXException ex) {
                throw new MetadataIoException(ex);
            }

            if (metadataMode ==  MetadataType.ISO_19115 && mode == MetadataType.DUBLINCORE) {
                return translateISOtoDCNode(metadataNode, type, elementName);
            } else if (mode == MetadataType.DUBLINCORE && metadataMode == MetadataType.DUBLINCORE) {
                return  applyElementSetNode(metadataNode, type, elementName);
            } else {
               return metadataNode;
            }
        }
        return null;
    }

    @Override
    public boolean existMetadata(final String identifier) throws MetadataIoException {
        return internalMetadataBusiness.existMetadata(identifier);
    }

    private Node translateISOtoDCNode(final Node metadata, final ElementSetType type, final List<QName> elementName) throws MetadataIoException  {
        if (metadata != null) {

            final DocumentBuilder docBuilder;
            try {
                dbf.setNamespaceAware(true);
                docBuilder = dbf.newDocumentBuilder();
            } catch (ParserConfigurationException ex) {
                throw new MetadataIoException(ex);
            }
            final Document document = docBuilder.newDocument();

            final Element root = document.createElementNS(LegacyNamespaces.CSW, "Record");

            /*
             * BRIEF part
             */
            final List<String> identifierValues = NodeUtilities.getValuesFromPaths(metadata, ISO_QUERYABLE.get("Identifier").paths);
            final List<Node> identifiers = NodeUtilities.buildNodes(document, "http://purl.org/dc/elements/1.1/", "identifier", identifierValues, true);

            if (elementName != null && elementName.contains(_Identifier_QNAME)) {
                NodeUtilities.appendChilds(root, identifiers);
            }

            final List<String> titleValues = NodeUtilities.getValuesFromPaths(metadata, ISO_QUERYABLE.get("Title").paths);
            final List<Node> titles = NodeUtilities.buildNodes(document, "http://purl.org/dc/elements/1.1/", "title", titleValues, true);

            if (elementName != null && elementName.contains(_Title_QNAME)) {
                NodeUtilities.appendChilds(root, titles);
            }

            final List<String> dataTypeValues = NodeUtilities.getValuesFromPaths(metadata, ISO_QUERYABLE.get("Type").paths);
            final List<Node> dataTypes = NodeUtilities.buildNodes(document, "http://purl.org/dc/elements/1.1/", "type", dataTypeValues, false);

            if (elementName != null && elementName.contains(_Type_QNAME)) {
                NodeUtilities.appendChilds(root, dataTypes);
            }

            final List<String> westValues  = NodeUtilities.getValuesFromPaths(metadata, ISO_QUERYABLE.get("WestBoundLongitude").paths);
            final List<String> eastValues  = NodeUtilities.getValuesFromPaths(metadata, ISO_QUERYABLE.get("EastBoundLongitude").paths);
            final List<String> northValues = NodeUtilities.getValuesFromPaths(metadata, ISO_QUERYABLE.get("NorthBoundLatitude").paths);
            final List<String> southValues = NodeUtilities.getValuesFromPaths(metadata, ISO_QUERYABLE.get("SouthBoundLatitude").paths);

            final List<Node> bboxes = new ArrayList<>();
            if (westValues.size()  == eastValues.size()  &&
                eastValues.size()  == northValues.size() &&
                northValues.size() == southValues.size()) {

                for (int i = 0; i < westValues.size(); i++) {
                    final Node bboxNode = document.createElementNS("http://www.opengis.net/ows", "BoundingBox");
                    final Node crsAtt   = document.createAttribute("crs");
                    crsAtt.setTextContent("EPSG:4326");
                    bboxNode.getAttributes().setNamedItem(crsAtt);
                    final Node dimAtt   = document.createAttribute("dimensions");
                    dimAtt.setTextContent("2");
                    bboxNode.getAttributes().setNamedItem(dimAtt);
                    final Node lower    = document.createElementNS("http://www.opengis.net/ows", "LowerCorner");
                    lower.setTextContent(westValues.get(i) + " " + southValues.get(i));
                    bboxNode.appendChild(lower);
                    final Node upper    = document.createElementNS("http://www.opengis.net/ows", "UpperCorner");
                    upper.setTextContent(eastValues.get(i) + " " + northValues.get(i));
                    bboxNode.appendChild(upper);
                    bboxes.add(bboxNode);
                }
            } else {
                LOGGER.warning("incoherent bboxes coordinate");
            }

            if (ElementSetType.BRIEF.equals(type)) {
                final Element briefRoot = document.createElementNS(LegacyNamespaces.CSW, "BriefRecord");
                NodeUtilities.appendChilds(briefRoot, identifiers);
                NodeUtilities.appendChilds(briefRoot, titles);
                NodeUtilities.appendChilds(briefRoot, dataTypes);
                NodeUtilities.appendChilds(briefRoot, bboxes);
                return briefRoot;
            }

            /*
             *  SUMMARY part
             */
            final List<String> abstractValues = NodeUtilities.getValuesFromPath(metadata, "/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:abstract/gco:CharacterString");
            final List<Node> abstracts = NodeUtilities.buildNodes(document, "http://purl.org/dc/terms/", "abstract", abstractValues, false);

            if (elementName != null && elementName.contains(_Abstract_QNAME)) {
                NodeUtilities.appendChilds(root, abstracts);
            }

            final List<String> kwValues = NodeUtilities.getValuesFromPaths(metadata, ISO_QUERYABLE.get("Subject").paths);
            final List<Node> subjects = NodeUtilities.buildNodes(document, "http://purl.org/dc/elements/1.1/", "subject", kwValues, false);

            if (elementName != null && elementName.contains(_Subject_QNAME)) {
                NodeUtilities.appendChilds(root, subjects);
            }

            final List<String> formValues = NodeUtilities.getValuesFromPaths(metadata, ISO_QUERYABLE.get("Format").paths);
            final List<Node> formats = NodeUtilities.buildNodes(document, "http://purl.org/dc/elements/1.1/", "format", formValues, false);

            if (elementName != null && elementName.contains(_Format_QNAME)) {
                 NodeUtilities.appendChilds(root, formats);
            }

            final List<String> modValues = NodeUtilities.getValuesFromPaths(metadata, ISO_QUERYABLE.get("Modified").paths);
            final List<String> dateValues = new ArrayList<>();
            for (String modValue : modValues) {
                dateValues.add(formatDate(modValue));
            }
            final List<Node> modifieds = NodeUtilities.buildNodes(document, "http://purl.org/dc/terms/", "modified", dateValues, false);

            if (elementName != null && elementName.contains(_Modified_QNAME)) {
                NodeUtilities.appendChilds(root, modifieds);
            }

            if (ElementSetType.SUMMARY.equals(type)) {
                final Element sumRoot = document.createElementNS(LegacyNamespaces.CSW, "SummaryRecord");
                NodeUtilities.appendChilds(sumRoot, identifiers);
                NodeUtilities.appendChilds(sumRoot, titles);
                NodeUtilities.appendChilds(sumRoot, dataTypes);
                NodeUtilities.appendChilds(sumRoot, subjects);
                NodeUtilities.appendChilds(sumRoot, formats);
                NodeUtilities.appendChilds(sumRoot, modifieds);
                NodeUtilities.appendChilds(sumRoot, abstracts);
                NodeUtilities.appendChilds(sumRoot, bboxes);
                return sumRoot;
            }

            final List<Node> dates = NodeUtilities.buildNodes(document, "http://purl.org/dc/elements/1.1/", "date", dateValues, false);

            if (elementName != null && elementName.contains(_Date_QNAME)) {
                NodeUtilities.appendChilds(root, dates);
            }

            final List<String> creaValues = NodeUtilities.getValuesFromPaths(metadata, DUBLIN_CORE_QUERYABLE.get("creator").paths);
            final List<Node> creators = NodeUtilities.buildNodes(document, "http://purl.org/dc/elements/1.1/", "creator", creaValues, false);

            if (elementName != null && elementName.contains(_Creator_QNAME)) {
                NodeUtilities.appendChilds(root, creators);
            }

            final List<String> desValues = NodeUtilities.getValuesFromPath(metadata, "/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:graphicOverview/gmd:MD_BrowseGraphic/gmd:fileName/gmx:FileName/@src");
            final List<Node> descriptions = NodeUtilities.buildNodes(document, "http://purl.org/dc/elements/1.1/", "description", desValues, false);

            if (!descriptions.isEmpty() && elementName != null && elementName.contains(_Description_QNAME)) {
                NodeUtilities.appendChilds(root, descriptions);
            }

            final List<String> paths = new ArrayList<>();
            paths.add("/gmd:MD_Metadata/gmd:distributionInfo/gmd:MD_Distribution/gmd:distributor/gmd:MD_Distributor/gmd:distributorContact/gmd:CI_ResponsibleParty/gmd:organisationName/gco:CharacterString");
            paths.add("/gmd:MD_Metadata/gmd:distributionInfo/gmd:MD_Distribution/gmd:distributor/gmd:MD_Distributor/gmd:distributorContact/gmd:CI_ResponsibleParty/gmd:organisationName/gmx:Anchor");
            paths.add("/gmi:MI_Metadata/gmd:distributionInfo/gmd:MD_Distribution/gmd:distributor/gmd:MD_Distributor/gmd:distributorContact/gmd:CI_ResponsibleParty/gmd:organisationName/gco:CharacterString");
            paths.add("/gmi:MI_Metadata/gmd:distributionInfo/gmd:MD_Distribution/gmd:distributor/gmd:MD_Distributor/gmd:distributorContact/gmd:CI_ResponsibleParty/gmd:organisationName/gmx:Anchor");
            final List<String> distValues = NodeUtilities.getValuesFromPaths(metadata, paths);
            final List<Node> distributors = NodeUtilities.buildNodes(document, "http://purl.org/dc/elements/1.1/", "publisher", distValues, false);

            if (elementName != null && elementName.contains(_Publisher_QNAME)) {
                NodeUtilities.appendChilds(root, distributors);
            }

            final List<String> langValues = NodeUtilities.getValuesFromPaths(metadata, ISO_QUERYABLE.get("Language").paths);
            final List<Node> languages = NodeUtilities.buildNodes(document, "http://purl.org/dc/elements/1.1/", "language", langValues, false);

            if (elementName != null && elementName.contains(_Language_QNAME)) {
                NodeUtilities.appendChilds(root, languages);
            }

            if (elementName != null && elementName.contains(_BoundingBox_QNAME)) {
                NodeUtilities.appendChilds(root, bboxes);
            }

            /* TODO
            final SimpleLiteral spatial = null;
            final SimpleLiteral references = null;*/

            if (ElementSetType.FULL.equals(type)) {
                final Element recRoot = document.createElementNS(LegacyNamespaces.CSW, "Record");
                NodeUtilities.appendChilds(recRoot, identifiers);
                NodeUtilities.appendChilds(recRoot, titles);
                NodeUtilities.appendChilds(recRoot, dataTypes);
                NodeUtilities.appendChilds(recRoot, subjects);
                NodeUtilities.appendChilds(recRoot, formats);
                NodeUtilities.appendChilds(recRoot, languages);
                NodeUtilities.appendChilds(recRoot, creators);
                NodeUtilities.appendChilds(recRoot, modifieds);
                NodeUtilities.appendChilds(recRoot, dates);
                NodeUtilities.appendChilds(recRoot, abstracts);
                NodeUtilities.appendChilds(recRoot, distributors);
                NodeUtilities.appendChilds(recRoot, descriptions);
                NodeUtilities.appendChilds(recRoot, bboxes);
                //NodeUtilities.appendChilds(recRoot, spatials);
                //NodeUtilities.appendChilds(recRoot, references);
                return recRoot;
            }

            document.appendChild(root);
            return root;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DomainValues> getFieldDomainofValues(final String propertyNames) throws MetadataIoException {
        final List<DomainValues> responseList = new ArrayList<>();
        final StringTokenizer tokens          = new StringTokenizer(propertyNames, ",");

        while (tokens.hasMoreTokens()) {
            final String token       = tokens.nextToken().trim();
            final List<String> paths = getPathForQueryable(token);

            if (!paths.isEmpty()) {
                final List<String> values         = getAllValuesFromPaths(paths);
                final DomainValuesType value      = new DomainValuesType(null, token, values, METADATA_QNAME);
                responseList.add(value);

            } else {
                throw new MetadataIoException("The property " + token + " is not queryable for now",
                        INVALID_PARAMETER_VALUE, "propertyName");
            }

        }
        return responseList;
    }

    @Override
    public List<String> getFieldDomainofValuesForMetadata(String token, String identifier) throws MetadataIoException {
        final List<String> paths = getPathForQueryable(token);
        if (!paths.isEmpty()) {
            return getAllValuesFromPaths(identifier, paths);
        } else {
            throw new MetadataIoException("The property " + token + " is not queryable for now",
                    INVALID_PARAMETER_VALUE, "propertyName");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] executeEbrimSQLQuery(final String sqlQuery) throws MetadataIoException {
        throw new MetadataIoException("Ebrim query are not supported int the FILESYSTEM mode.", OPERATION_NOT_SUPPORTED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Node> getAllEntries() throws MetadataIoException {
        final List<Node> result = new ArrayList<>();
        final List<String> metadataIds = internalMetadataBusiness.getInternalMetadataIds();
        for (String metadataID : metadataIds) {
            final String meta = internalMetadataBusiness.getMetadata(metadataID);
            try {
                result.add(getNodeFromReader(new StringReader(meta)));
            } catch (ParserConfigurationException | SAXException | IOException ex) {
                throw new MetadataIoException(ex);
            }
        }
        return result;
    }

   @Override
    public int getEntryCount() throws MetadataIoException {
        return internalMetadataBusiness.getInternalMetadataCount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getAllIdentifiers() throws MetadataIoException {
        return internalMetadataBusiness.getInternalMetadataIds();
    }

    @Override
    public Iterator<String> getIdentifierIterator() throws MetadataIoException {
        return internalMetadataBusiness.getInternalMetadataIds().iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<MetadataType> getSupportedDataTypes() {
        return Arrays.asList(MetadataType.ISO_19115, MetadataType.DUBLINCORE, MetadataType.EBRIM, MetadataType.ISO_19110);
    }

    /**
     * Return the list of Additional queryable element.
     */
    @Override
    public List<QName> getAdditionalQueryableQName() {
        List<QName> addQnames = new ArrayList<>();
        for (Object addQname : additionalQueryable.keySet()) {
            addQnames.add(new QName((String)addQname));
        }
        return addQnames;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, PathType> getAdditionalQueryablePathMap() {
        return additionalQueryable;
    }
}
