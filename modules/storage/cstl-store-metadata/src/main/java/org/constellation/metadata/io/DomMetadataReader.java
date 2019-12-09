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
package org.constellation.metadata.io;

import org.constellation.util.NodeUtilities;
import org.geotoolkit.temporal.object.TemporalUtilities;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.logging.Level;
import javax.xml.XMLConstants;
import org.apache.sis.internal.xml.LegacyNamespaces;
import org.apache.sis.xml.Namespaces;
import org.constellation.api.PathType;
import static org.constellation.metadata.CSWQueryable.DIF_QUERYABLE;
import static org.constellation.metadata.CSWQueryable.DUBLIN_CORE_QUERYABLE;
import static org.constellation.metadata.CSWQueryable.ISO_QUERYABLE;
import org.constellation.store.metadata.CSWMetadataReader;
import org.geotoolkit.csw.xml.DomainValues;
import org.geotoolkit.csw.xml.v202.DomainValuesType;
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
import org.geotoolkit.metadata.AbstractMetadataReader;
import org.geotoolkit.metadata.ElementSetType;
import org.geotoolkit.metadata.MetadataIoException;
import org.geotoolkit.metadata.MetadataType;
import org.geotoolkit.metadata.RecordInfo;
import static org.geotoolkit.metadata.TypeNames.METADATA_QNAME;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import static org.geotoolkit.ows.xml.v100.ObjectFactory._BoundingBox_QNAME;


/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class DomMetadataReader extends AbstractMetadataReader implements CSWMetadataReader {

    /**
     * A date formatter used to display the Date object for Dublin core translation.
     */
    private static final DateFormat FORMATTER;
    static {
        FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        FORMATTER.setTimeZone(TimeZone.getTimeZone("GMT+2"));
    }

    protected final DocumentBuilderFactory dbf;

    protected final XMLInputFactory xif = XMLInputFactory.newFactory();

    protected final Map<String, PathType> additionalQueryable;

    public DomMetadataReader(final boolean isCacheEnabled, final boolean isThreadEnabled, final Map<String, PathType> additionalQueryable) throws MetadataIoException {
        super(isCacheEnabled, isThreadEnabled);
        dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        try {
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (ParserConfigurationException ex) {
            throw new MetadataIoException(ex);
        }
        if (additionalQueryable != null) {
            this.additionalQueryable = additionalQueryable;
        } else {
            this.additionalQueryable = new HashMap<>();
        }
    }

    protected MetadataType getMetadataType(final InputStream metadataStream, final boolean reset) throws IOException, XMLStreamException {
        final QName rootName;
        if (reset){
            metadataStream.mark(0);
        }
        final XMLStreamReader xsr = xif.createXMLStreamReader(metadataStream);
        xsr.nextTag();
        rootName = xsr.getName();
        xsr.close();
        if (reset) {
            metadataStream.reset();
        }
        MetadataType result = MetadataType.getFromTypeName(rootName);
        if (result == null) {
            result = MetadataType.NATIVE;
        }
        return result;
    }

    protected MetadataType getMetadataType(final Reader metadataReader, final boolean reset) throws IOException, XMLStreamException {
        final QName rootName;
        if (reset){
            metadataReader.mark(0);
        }
        final XMLStreamReader xsr = xif.createXMLStreamReader(metadataReader);
        xsr.nextTag();
        rootName = xsr.getName();
        xsr.close();
        if (reset) {
            metadataReader.reset();
        }

        MetadataType result = MetadataType.getFromTypeName(rootName);
        if (result == null) {
            result = MetadataType.NATIVE;
        }
        return result;
    }

    private String formatDate(final String modValue) {
        try {
            final Date d = TemporalUtilities.parseDate(modValue);
            String dateValue;
            synchronized (FORMATTER) {
                dateValue = FORMATTER.format(d);
            }
            dateValue = dateValue.substring(0, dateValue.length() - 2);
            dateValue = dateValue + ":00";
            return dateValue;
        } catch (ParseException ex) {
            LOGGER.log(Level.WARNING, "unable to parse date: {0}", modValue);
        }
        return null;
    }

    /**
     * Apply the elementSet (Brief, Summary or full) or the custom elementSetName on the specified record.
     *
     * @param record A dublinCore record.
     * @param type The ElementSetType to apply on this record.
     * @param elementName A list of QName corresponding to the requested attribute. this parameter is ignored if type is not null.
     *
     * @return A record object.
     * @throws MetadataIoException If the type and the element name are null.
     */
    protected Node applyElementSetNode(final Node record, final ElementSetType type, final List<QName> elementName, String mainNmsp, boolean transform) throws MetadataIoException {
        final DocumentBuilder docBuilder;
        try {
            docBuilder = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            throw new MetadataIoException(ex);
        }
        String owsNmsp;
        if (mainNmsp.equals(Namespaces.CSW)) {
            owsNmsp = "http://www.opengis.net/ows/2.0";
        } else {
            owsNmsp = "http://www.opengis.net/ows";
        }
        final Document document = docBuilder.newDocument();
        if (type != null) {
            if (transform && type.equals(ElementSetType.FULL)) {
                final Element root = document.createElementNS(mainNmsp, "Record");
                for (int i = 0; i < record.getChildNodes().getLength(); i++) {
                    Node child = record.getChildNodes().item(i);
                    Node imported = document.importNode(child, true);

                    if (imported.getNodeType() == Node.ELEMENT_NODE && imported.getLocalName().equals("BoundingBox") && !imported.getNamespaceURI().equals(owsNmsp)) {
                        document.renameNode(imported, owsNmsp, imported.getLocalName());
                        for (int j = 0; j < imported.getChildNodes().getLength(); j++) {
                            Node childbbox = imported.getChildNodes().item(j);
                            if (childbbox.getNodeType() == Node.ELEMENT_NODE) {
                                document.renameNode(childbbox, owsNmsp, childbbox.getLocalName());
                            }
                        }
                    }
                    NodeUtilities.appendChilds(root, Arrays.asList(imported));
                }
                return root;

            } else if (type.equals(ElementSetType.SUMMARY)) {
                final Element sumRoot = document.createElementNS(mainNmsp, "SummaryRecord");
                final List<String> identifierValues = NodeUtilities.getValuesFromPath(record, "/csw:Record/dc:identifier");
                final List<Node> identifiers = NodeUtilities.buildNodes(document, "http://purl.org/dc/elements/1.1/", "identifier", identifierValues, true);
                NodeUtilities.appendChilds(sumRoot, identifiers);
                final List<String> titleValues = NodeUtilities.getValuesFromPath(record, "/csw:Record/dc:title");
                final List<Node> titles = NodeUtilities.buildNodes(document, "http://purl.org/dc/elements/1.1/", "title", titleValues, true);
                NodeUtilities.appendChilds(sumRoot, titles);
                final List<String> typeValues = NodeUtilities.getValuesFromPath(record, "/csw:Record/dc:type");
                final List<Node> types = NodeUtilities.buildNodes(document, "http://purl.org/dc/elements/1.1/", "type", typeValues, false);
                NodeUtilities.appendChilds(sumRoot, types);
                final List<String> subValues = NodeUtilities.getValuesFromPath(record, "/csw:Record/dc:subject");
                final List<Node> subjects = NodeUtilities.buildNodes(document, "http://purl.org/dc/elements/1.1/", "subject", subValues, false);
                NodeUtilities.appendChilds(sumRoot, subjects);
                final List<String> formValues = NodeUtilities.getValuesFromPath(record, "/csw:Record/dc:format");
                final List<Node> formats = NodeUtilities.buildNodes(document, "http://purl.org/dc/elements/1.1/", "format", formValues, false);
                NodeUtilities.appendChilds(sumRoot, formats);
                final List<String> modValues = NodeUtilities.getValuesFromPath(record, "/csw:Record/dc:modified");
                final List<Node> modifieds = NodeUtilities.buildNodes(document, "http://purl.org/dc/terms/", "modified", modValues, false);
                NodeUtilities.appendChilds(sumRoot, modifieds);
                final List<String> absValues = NodeUtilities.getValuesFromPath(record, "/csw:Record/dc:abstract");
                final List<Node> abstracts = NodeUtilities.buildNodes(document, "http://purl.org/dc/terms/", "abstract", absValues, false);
                NodeUtilities.appendChilds(sumRoot, abstracts);
                final List<Node> origBboxes = NodeUtilities.getNodeFromPath(record, "/ows:BoundingBox");
                for (Node origBbox : origBboxes) {
                    Node n = document.importNode(origBbox, true);
                    if (!n.getNamespaceURI().equals(owsNmsp)) {
                        document.renameNode(n, owsNmsp, n.getLocalName());
                        for (int i = 0; i < n.getChildNodes().getLength(); i++) {
                            Node child = n.getChildNodes().item(i);
                            if (child.getNodeType() == Node.ELEMENT_NODE) {
                                document.renameNode(child, owsNmsp, child.getLocalName());
                            }
                        }
                    }
                    NodeUtilities.appendChilds(sumRoot, Arrays.asList(n));
                }
                return sumRoot;
            } else if (type.equals(ElementSetType.BRIEF)) {
                final Element briefRoot = document.createElementNS(mainNmsp, "BriefRecord");
                final List<String> identifierValues = NodeUtilities.getValuesFromPath(record, "/csw:Record/dc:identifier");
                final List<Node> identifiers = NodeUtilities.buildNodes(document, "http://purl.org/dc/elements/1.1/", "identifier", identifierValues, true);
                NodeUtilities.appendChilds(briefRoot, identifiers);
                final List<String> titleValues = NodeUtilities.getValuesFromPath(record, "/csw:Record/dc:title");
                final List<Node> titles = NodeUtilities.buildNodes(document, "http://purl.org/dc/elements/1.1/", "title", titleValues, true);
                NodeUtilities.appendChilds(briefRoot, titles);
                final List<String> typeValues = NodeUtilities.getValuesFromPath(record, "/csw:Record/dc:type");
                final List<Node> types = NodeUtilities.buildNodes(document, "http://purl.org/dc/elements/1.1/", "type", typeValues, false);
                NodeUtilities.appendChilds(briefRoot, types);
                final List<Node> origBboxes = NodeUtilities.getNodeFromPath(record, "/csw:Record/ows:BoundingBox");
                for (Node origBbox : origBboxes) {
                    Node n = document.importNode(origBbox, true);
                    NodeUtilities.appendChilds(briefRoot, Arrays.asList(n));
                }
                return briefRoot;
            } else {
                return record;
            }
        } else if (elementName != null) {
            final Element recRoot = document.createElementNS(mainNmsp, "Record");
            for (QName qn : elementName) {
                if (qn != null) {
                    final List<Node> origs = NodeUtilities.getNodeFromPath(record, "/dc:" + qn.getLocalPart());
                    for (Node orig : origs) {
                        Node n = document.importNode(orig, true);
                        NodeUtilities.appendChilds(recRoot, Arrays.asList(n));
                    }
                } else {
                    LOGGER.warning("An elementName was null.");
                }
            }
            return recRoot;
        } else {
            throw new MetadataIoException("No ElementSet or Element name specified");
        }
    }

    protected Node translateISOtoDCNode(final Node metadata, final ElementSetType type, final List<QName> elementName, String mainNmsp) throws MetadataIoException  {
        if (metadata != null) {

            String owsNmsp;
            if (mainNmsp.equals(Namespaces.CSW)) {
                owsNmsp = "http://www.opengis.net/ows/2.0";
            } else {
                owsNmsp = "http://www.opengis.net/ows";
            }
            final DocumentBuilder docBuilder;
            try {
                docBuilder = dbf.newDocumentBuilder();
            } catch (ParserConfigurationException ex) {
                throw new MetadataIoException(ex);
            }
            final Document document = docBuilder.newDocument();

            final Element root = document.createElementNS(mainNmsp, "Record");

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
                    final Node bboxNode = document.createElementNS(owsNmsp, "BoundingBox");
                    final Node crsAtt   = document.createAttribute("crs");
                    crsAtt.setTextContent("EPSG:4326");
                    bboxNode.getAttributes().setNamedItem(crsAtt);
                    final Node dimAtt   = document.createAttribute("dimensions");
                    dimAtt.setTextContent("2");
                    bboxNode.getAttributes().setNamedItem(dimAtt);
                    final Node lower    = document.createElementNS(owsNmsp, "LowerCorner");
                    lower.setTextContent(southValues.get(i) + " " + westValues.get(i));
                    bboxNode.appendChild(lower);
                    final Node upper    = document.createElementNS(owsNmsp, "UpperCorner");
                    upper.setTextContent(northValues.get(i) + " " + eastValues.get(i));
                    bboxNode.appendChild(upper);
                    bboxes.add(bboxNode);
                }
            } else {
                LOGGER.warning("incoherent bboxes coordinate");
            }

            if (ElementSetType.BRIEF.equals(type)) {
                final Element briefRoot = document.createElementNS(mainNmsp, "BriefRecord");
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
                final Element sumRoot = document.createElementNS(mainNmsp, "SummaryRecord");
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

            if (elementName != null && (elementName.contains(_BoundingBox_QNAME) ||
                                        elementName.contains(org.geotoolkit.ows.xml.v200.ObjectFactory._BoundingBox_QNAME))) {
                NodeUtilities.appendChilds(root, bboxes);
            }

            /* TODO
            final SimpleLiteral spatial = null;
            final SimpleLiteral references = null;*/

            if (ElementSetType.FULL.equals(type)) {
                final Element recRoot = document.createElementNS(mainNmsp, "Record");
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

     protected Node translateDIFtoDCNode(final Node metadata, final ElementSetType type, final List<QName> elementName, String mainNmsp) throws MetadataIoException  {
        if (metadata != null) {

            String owsNmsp;
            if (mainNmsp.equals(Namespaces.CSW)) {
                owsNmsp = "http://www.opengis.net/ows/2.0";
            } else {
                owsNmsp = "http://www.opengis.net/ows";
            }
            final DocumentBuilder docBuilder;
            try {
                docBuilder = dbf.newDocumentBuilder();
            } catch (ParserConfigurationException ex) {
                throw new MetadataIoException(ex);
            }
            final Document document = docBuilder.newDocument();

            final Element root = document.createElementNS(mainNmsp, "Record");

            /*
             * BRIEF part
             */
            final List<String> identifierValues = NodeUtilities.getValuesFromPaths(metadata, DUBLIN_CORE_QUERYABLE.get("identifier").paths);
            final List<Node> identifiers = NodeUtilities.buildNodes(document, "http://purl.org/dc/elements/1.1/", "identifier", identifierValues, true);

            if (elementName != null && elementName.contains(_Identifier_QNAME)) {
                NodeUtilities.appendChilds(root, identifiers);
            }

            final List<String> titleValues = NodeUtilities.getValuesFromPaths(metadata, DUBLIN_CORE_QUERYABLE.get("title").paths);
            final List<Node> titles = NodeUtilities.buildNodes(document, "http://purl.org/dc/elements/1.1/", "title", titleValues, true);

            if (elementName != null && elementName.contains(_Title_QNAME)) {
                NodeUtilities.appendChilds(root, titles);
            }

            final List<String> dataTypeValues = NodeUtilities.getValuesFromPaths(metadata, DUBLIN_CORE_QUERYABLE.get("type").paths);
            final List<Node> dataTypes = NodeUtilities.buildNodes(document, "http://purl.org/dc/elements/1.1/", "type", dataTypeValues, false);

            if (elementName != null && elementName.contains(_Type_QNAME)) {
                NodeUtilities.appendChilds(root, dataTypes);
            }

            final List<String> westValues  = NodeUtilities.getValuesFromPaths(metadata, DIF_QUERYABLE.get("WestBoundLongitude").paths);
            final List<String> eastValues  = NodeUtilities.getValuesFromPaths(metadata, DIF_QUERYABLE.get("EastBoundLongitude").paths);
            final List<String> northValues = NodeUtilities.getValuesFromPaths(metadata, DIF_QUERYABLE.get("NorthBoundLatitude").paths);
            final List<String> southValues = NodeUtilities.getValuesFromPaths(metadata, DIF_QUERYABLE.get("SouthBoundLatitude").paths);

            final List<Node> bboxes = new ArrayList<>();
            if (westValues.size()  == eastValues.size()  &&
                eastValues.size()  == northValues.size() &&
                northValues.size() == southValues.size()) {

                for (int i = 0; i < westValues.size(); i++) {
                    final Node bboxNode = document.createElementNS(owsNmsp, "BoundingBox");
                    final Node crsAtt   = document.createAttribute("crs");
                    crsAtt.setTextContent("EPSG:4326");
                    bboxNode.getAttributes().setNamedItem(crsAtt);
                    final Node dimAtt   = document.createAttribute("dimensions");
                    dimAtt.setTextContent("2");
                    bboxNode.getAttributes().setNamedItem(dimAtt);
                    final Node lower    = document.createElementNS(owsNmsp, "LowerCorner");
                    lower.setTextContent(southValues.get(i) + " " + westValues.get(i));
                    bboxNode.appendChild(lower);
                    final Node upper    = document.createElementNS(owsNmsp, "UpperCorner");
                    upper.setTextContent(northValues.get(i) + " " + eastValues.get(i));
                    bboxNode.appendChild(upper);
                    bboxes.add(bboxNode);
                }
            } else {
                LOGGER.warning("incoherent bboxes coordinate");
            }

            if (ElementSetType.BRIEF.equals(type)) {
                final Element briefRoot = document.createElementNS(mainNmsp, "BriefRecord");
                NodeUtilities.appendChilds(briefRoot, identifiers);
                NodeUtilities.appendChilds(briefRoot, titles);
                NodeUtilities.appendChilds(briefRoot, dataTypes);
                NodeUtilities.appendChilds(briefRoot, bboxes);
                return briefRoot;
            }

            /*
             *  SUMMARY part
             */
            final List<String> abstractValues = NodeUtilities.getValuesFromPaths(metadata, DUBLIN_CORE_QUERYABLE.get("abstract").paths);
            final List<Node> abstracts = NodeUtilities.buildNodes(document, "http://purl.org/dc/terms/", "abstract", abstractValues, false);

            if (elementName != null && elementName.contains(_Abstract_QNAME)) {
                NodeUtilities.appendChilds(root, abstracts);
            }

            final List<String> kwValues = NodeUtilities.getValuesFromPaths(metadata, DUBLIN_CORE_QUERYABLE.get("subject").paths);
            final List<Node> subjects = NodeUtilities.buildNodes(document, "http://purl.org/dc/elements/1.1/", "subject", kwValues, false);

            if (elementName != null && elementName.contains(_Subject_QNAME)) {
                NodeUtilities.appendChilds(root, subjects);
            }

            final List<String> formValues = NodeUtilities.getValuesFromPaths(metadata, DUBLIN_CORE_QUERYABLE.get("format").paths);
            final List<Node> formats = NodeUtilities.buildNodes(document, "http://purl.org/dc/elements/1.1/", "format", formValues, false);

            if (elementName != null && elementName.contains(_Format_QNAME)) {
                 NodeUtilities.appendChilds(root, formats);
            }

            final List<String> modValues = NodeUtilities.getValuesFromPaths(metadata, DUBLIN_CORE_QUERYABLE.get("date").paths);
            final List<String> dateValues = new ArrayList<>();
            for (String modValue : modValues) {
                dateValues.add(formatDate(modValue));
            }
            final List<Node> modifieds = NodeUtilities.buildNodes(document, "http://purl.org/dc/terms/", "modified", dateValues, false);

            if (elementName != null && elementName.contains(_Modified_QNAME)) {
                NodeUtilities.appendChilds(root, modifieds);
            }

            if (ElementSetType.SUMMARY.equals(type)) {
                final Element sumRoot = document.createElementNS(mainNmsp, "SummaryRecord");
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

            final List<Node> descriptions = new ArrayList<>();
        //    final List<String> desValues = NodeUtilities.getValuesFromPath(metadata, "/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:graphicOverview/gmd:MD_BrowseGraphic/gmd:fileName/gmx:FileName/@src");
        //    final List<Node> descriptions = NodeUtilities.buildNodes(document, "http://purl.org/dc/elements/1.1/", "description", desValues, false);

        //    if (!descriptions.isEmpty() && elementName != null && elementName.contains(_Description_QNAME)) {
        //        NodeUtilities.appendChilds(root, descriptions);
        //    }

            final List<Node> distributors = new ArrayList<>();
        //    final List<String> paths = new ArrayList<>();
        //    paths.add("/gmd:MD_Metadata/gmd:distributionInfo/gmd:MD_Distribution/gmd:distributor/gmd:MD_Distributor/gmd:distributorContact/gmd:CI_ResponsibleParty/gmd:organisationName/gco:CharacterString");
        //    paths.add("/gmd:MD_Metadata/gmd:distributionInfo/gmd:MD_Distribution/gmd:distributor/gmd:MD_Distributor/gmd:distributorContact/gmd:CI_ResponsibleParty/gmd:organisationName/gmx:Anchor");
        //    paths.add("/gmi:MI_Metadata/gmd:distributionInfo/gmd:MD_Distribution/gmd:distributor/gmd:MD_Distributor/gmd:distributorContact/gmd:CI_ResponsibleParty/gmd:organisationName/gco:CharacterString");
        //    paths.add("/gmi:MI_Metadata/gmd:distributionInfo/gmd:MD_Distribution/gmd:distributor/gmd:MD_Distributor/gmd:distributorContact/gmd:CI_ResponsibleParty/gmd:organisationName/gmx:Anchor");
        //    final List<String> distValues = NodeUtilities.getValuesFromPaths(metadata, paths);
        //    final List<Node> distributors = NodeUtilities.buildNodes(document, "http://purl.org/dc/elements/1.1/", "publisher", distValues, false);

        //    if (elementName != null && elementName.contains(_Publisher_QNAME)) {
        //        NodeUtilities.appendChilds(root, distributors);
        //    }

            final List<Node> languages = new ArrayList<>();
        //    final List<String> langValues = NodeUtilities.getValuesFromPaths(metadata, ISO_QUERYABLE.get("Language").paths);
        //    final List<Node> languages = NodeUtilities.buildNodes(document, "http://purl.org/dc/elements/1.1/", "language", langValues, false);

        //    if (elementName != null && elementName.contains(_Language_QNAME)) {
        //        NodeUtilities.appendChilds(root, languages);
        //    }

            if (elementName != null && (elementName.contains(_BoundingBox_QNAME) ||
                                        elementName.contains(org.geotoolkit.ows.xml.v200.ObjectFactory._BoundingBox_QNAME))) {
                NodeUtilities.appendChilds(root, bboxes);
            }

            /* TODO
            final SimpleLiteral spatial = null;
            final SimpleLiteral references = null;*/

            if (ElementSetType.FULL.equals(type)) {
                final Element recRoot = document.createElementNS(mainNmsp, "Record");
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


     protected Node convertAndApplyElementSet(MetadataType metadataMode, MetadataType mode, ElementSetType type, List<QName> elementName, Node metadataNode) throws MetadataIoException {
         final Node n;

        // DIF TO CSW2
        if (metadataMode ==  MetadataType.DIF && mode == MetadataType.DUBLINCORE_CSW202) {
            n = translateDIFtoDCNode(metadataNode, type, elementName, LegacyNamespaces.CSW);

        // DIF TO CSW3
        } else if (metadataMode ==  MetadataType.DIF && mode == MetadataType.DUBLINCORE_CSW300) {
            n = translateDIFtoDCNode(metadataNode, type, elementName, Namespaces.CSW);

        // ISO TO CSW2
        } else if (metadataMode ==  MetadataType.ISO_19115 && mode == MetadataType.DUBLINCORE_CSW202) {
            n = translateISOtoDCNode(metadataNode, type, elementName, LegacyNamespaces.CSW);

        // ISO TO CSW3
        } else if (metadataMode ==  MetadataType.ISO_19115 && mode == MetadataType.DUBLINCORE_CSW300) {
            n = translateISOtoDCNode(metadataNode, type, elementName, Namespaces.CSW);

        // CSW3 (NO transform OR TO CSW3)
        } else if (mode == MetadataType.DUBLINCORE_CSW300 && (metadataMode == MetadataType.DUBLINCORE_CSW300 || metadataMode == MetadataType.DUBLINCORE_CSW202)) {
            n = applyElementSetNode(metadataNode, type, elementName, Namespaces.CSW, mode != metadataMode);

        // CSW2 (NO transform OR TO CSW2)
        } else if (mode == MetadataType.DUBLINCORE_CSW202 && (metadataMode == MetadataType.DUBLINCORE_CSW300 || metadataMode == MetadataType.DUBLINCORE_CSW202)) {
            n = applyElementSetNode(metadataNode, type, elementName, LegacyNamespaces.CSW, mode != metadataMode);

        // RETURN NATIVE
        } else {
           n = metadataNode;
        }
        return n;
     }

     /**
     * {@inheritDoc}
     */
    @Override
    public List<DomainValues> getFieldDomainofValues(final String propertyNames) throws MetadataIoException {
        final List<DomainValues> responseList = new ArrayList<>();
        final StringTokenizer tokens          = new StringTokenizer(propertyNames, ",");

        while (tokens.hasMoreTokens()) {
            final String token   = tokens.nextToken().trim();
            final PathType paths = getPathForQueryable(token);
            if (paths != null) {
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

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getFieldDomainofValuesForMetadata(String token, String identifier) throws MetadataIoException {
        final PathType paths = getPathForQueryable(token);
        if (paths != null) {
            return getAllValuesFromPaths(identifier, paths);
        } else {
            throw new MetadataIoException("The property " + token + " is not queryable for now",
                    INVALID_PARAMETER_VALUE, "propertyName");
        }
    }

    /**
     * Return all the String values corresponding to the specified list of path through all the metadatas.
     *
     * @param paths List of path within the xml.
     */
    private List<String> getAllValuesFromPaths(final PathType paths) throws MetadataIoException {
        final List<String> result = new ArrayList<>();
        final List<String> ids    = getAllIdentifiers();
        for (String metadataID : ids) {
            final RecordInfo metadata = getMetadata(metadataID, MetadataType.ISO_19115);
            final List<Object> value = NodeUtilities.extractValues(metadata.node, paths);
            for (Object obj : value){
                result.add(obj.toString());
            }

        }
        Collections.sort(result);
        return result;
    }

    /**
     * Return all the String values corresponding to the specified list of path through the specified
     * metadata.
     *
     * @param metadataID Metadata identifier.
     * @param paths List of path within the xml.
     */
    private List<String> getAllValuesFromPaths(final String metadataID, final PathType paths) throws MetadataIoException {
        final List<String> result = new ArrayList<>();
        final RecordInfo metadata = getMetadata(metadataID, MetadataType.ISO_19115);
        final List<Object> value = NodeUtilities.extractValues(metadata.node, paths);
        for (Object obj : value){
                result.add(obj.toString());
        }
        Collections.sort(result);
        return result;
    }

    /**
     * Return a list of metadata path for the specified queryable.
     *
     * @param token a queryable.
     */
    private PathType getPathForQueryable(String token) throws MetadataIoException {
        if (ISO_QUERYABLE.get(token) != null) {
            return ISO_QUERYABLE.get(token);
        } else if (DUBLIN_CORE_QUERYABLE.get(token) != null) {
            return DUBLIN_CORE_QUERYABLE.get(token);
        } else if (additionalQueryable.get(token) != null) {
            return additionalQueryable.get(token);
        } else {
            throw new MetadataIoException("The property " + token + " is not queryable",
                    INVALID_PARAMETER_VALUE, "propertyName");
        }
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
