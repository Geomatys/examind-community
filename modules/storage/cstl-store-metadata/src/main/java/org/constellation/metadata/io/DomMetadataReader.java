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
import static org.constellation.metadata.CSWQueryable.DIF_DISTRIBUTION_COND_PATH;
import static org.constellation.metadata.CSWQueryable.DIF_DISTRIBUTION_COND_VALUE;
import static org.constellation.metadata.CSWQueryable.DIF_DISTRIBUTION_LINK;
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
        FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        FORMATTER.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private static final String GMD = "http://www.isotc211.org/2005/gmd";
    private static final String GMI = "http://www.isotc211.org/2005/gmi";
    private static final String GMX = "http://www.isotc211.org/2005/gmx";
    private static final String GML = "http://www.opengis.net/gml/3.2";
    private static final String GCO = "http://www.isotc211.org/2005/gco";

    private static enum DateLabel {
        UNKNOW("unknown"),
        PRESENT("present"),
        UNBOUNDED("unbounded"),
        FUTURE("future"),
        NOT_PROVIDED("Not provided");

        private final String label;

        DateLabel(String label) {
            this.label = label;
        }

        static DateLabel getFromLabel(String label) {
            switch(label) {
                case "unknown"      : return UNKNOW;
                case "present"      : return PRESENT;
                case "unbounded"    : return UNBOUNDED;
                case "future"       : return FUTURE;
                case "Not provided" : return NOT_PROVIDED;
            }
            return null;
        }

        public String getLabelDate() {
            switch(label) {
                case "present"      :   String dateValue;
                                        synchronized (FORMATTER) {
                                            dateValue = FORMATTER.format(new Date(System.currentTimeMillis()));
                                        }
                                        dateValue = dateValue.substring(0, dateValue.length() - 2);
                                        dateValue = dateValue + ":00";
                                        return dateValue;
                case "unbounded"    : 
                case "future"       : 
                case "Not provided" :
                case "unknown"      : return "1970-01-01T00:00:00Z";
            }
            throw new IllegalArgumentException("Unxpected label:" + label);
        }
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
            final Date d = TemporalUtilities.parseDateCal(modValue).getTime();
            String dateValue;
            synchronized (FORMATTER) {
                dateValue = FORMATTER.format(d);
            }
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

            final List<String> relValues  = new ArrayList<>();
            final List<Node> uris         = new ArrayList<>();
            final List<Node> transferOpts = NodeUtilities.getNodeFromPath(metadata, "/gmd:distributionInfo/gmd:MD_Distribution/gmd:transferOptions/gmd:MD_DigitalTransferOptions/gmd:onLine/gmd:CI_OnlineResource");
            for (Node transferOpt : transferOpts) {
                String link = NodeUtilities.getFirstValueFromPath(transferOpt, "/gmd:CI_OnlineResource/gmd:linkage/gmd:URL");
                if (link != null) {
                    relValues.add(link);

                    // URI
                    final Node n = document.createElementNS("http://purl.org/dc/elements/1.1/", "URI");
                    n.setTextContent(link);

                    final String protocol = NodeUtilities.getFirstValueFromPath(transferOpt, "/gmd:CI_OnlineResource/gmd:protocol/gco:CharacterString");
                    if (protocol != null) {
                        ((Element)n).setAttribute("protocol", protocol);
                    }
                    final String description = NodeUtilities.getFirstValueFromPath(transferOpt, "/gmd:CI_OnlineResource/gmd:description/gco:CharacterString");
                    if (description != null) {
                        ((Element)n).setAttribute("description", description);
                    }
                    final String name = NodeUtilities.getFirstValueFromPath(transferOpt, "/gmd:CI_OnlineResource/gmd:name/gco:CharacterString");
                    if (name != null) {
                        ((Element)n).setAttribute("name", name);
                    }
                    uris.add(n);
                }
            }

            final List<Node> references = NodeUtilities.buildNodes(document, "http://purl.org/dc/elements/1.1/", "references", relValues, false);

            /* TODO
            final SimpleLiteral spatial = null;*/

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
                NodeUtilities.appendChilds(recRoot, references);
                NodeUtilities.appendChilds(recRoot, bboxes);
                NodeUtilities.appendChilds(recRoot, uris);
                //NodeUtilities.appendChilds(recRoot, spatials);

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

            final List<String> relValues = NodeUtilities.getValuesFromConditionalPath(metadata, DIF_DISTRIBUTION_LINK, DIF_DISTRIBUTION_COND_PATH, DIF_DISTRIBUTION_COND_VALUE);
            final List<Node> references = NodeUtilities.buildNodes(document, "http://purl.org/dc/elements/1.1/", "references", relValues, false);

            /* TODO
            final SimpleLiteral spatial = null;*/

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
                NodeUtilities.appendChilds(recRoot, references);
                NodeUtilities.appendChilds(recRoot, bboxes);
                //NodeUtilities.appendChilds(recRoot, spatials);
                return recRoot;
            }

            document.appendChild(root);
            return root;
        }
        return null;
    }

    protected Node translateDIFtoISONode(final Node metadata, final ElementSetType type, final List<QName> elementName) throws MetadataIoException {
        if (metadata != null) {

            final DocumentBuilder docBuilder;
            try {
                docBuilder = dbf.newDocumentBuilder();
            } catch (ParserConfigurationException ex) {
                throw new MetadataIoException(ex);
            }
            final Document doc = docBuilder.newDocument();

            final Element root;

            List<Node> platformNodes = NodeUtilities.getNodeFromPath(metadata, "/dif:Platform");
            if (platformNodes.isEmpty()) {
                root = doc.createElementNS(GMD, "MD_Metadata");
            } else {
                root = doc.createElementNS(GMI, "MI_Metadata");
            }

            final String identifierValue = NodeUtilities.getFirstValueFromPath(metadata, "/dif:DIF/dif:Entry_ID/dif:Short_Name");
            addCharacterStringNode(doc, root, "fileIdentifier", identifierValue);

            addCodelistNode(doc, root, "language", "LanguageCode", "eng");
            addCodelistNode(doc, root, "hierarchyLevel", "MD_ScopeCode", "series");

            final List<Node> contactNodes = NodeUtilities.getNodeFromPath(metadata, "/dif:Personnel");
            boolean hasContact = !contactNodes.isEmpty();
            if (hasContact) {
                for (Node contactNode : contactNodes) {
                    String role = NodeUtilities.getFirstValueFromPath(contactNode, "/dif:Personnel/dif:Role");
                    if ("METADATA AUTHOR".equals(role)) {

                        List<Node> groups = NodeUtilities.getNodeFromPath(contactNode, "/dif:Contact_Group");
                        for (Node groupNode : groups) {
                            final Node contact = doc.createElementNS(GMD, "contact");
                            root.appendChild(contact);
                            final Node ciResp = doc.createElementNS(GMD, "CI_ResponsibleParty");
                            contact.appendChild(ciResp);

                            addCharacterStringNode(doc, ciResp, "organisationName", NodeUtilities.getFirstValueFromPath(groupNode, "/dif:Contact_Group/dif:Name"));
                            addCharacterStringNode(doc, ciResp, "positionName", "METADATA AUTHOR");

                            buildDiffAddress(doc, ciResp, groupNode, null, "/dif:Contact_Group");

                            addCodelistNode(doc, ciResp, "role", "CI_RoleCode", "author");
                        }

                        List<Node> persons = NodeUtilities.getNodeFromPath(contactNode, "/dif:Contact_Person");
                        for (Node personNode : persons) {
                            final Node contact = doc.createElementNS(GMD, "contact");
                            root.appendChild(contact);
                            final Node ciResp = doc.createElementNS(GMD, "CI_ResponsibleParty");
                            contact.appendChild(ciResp);

                            String uuid  = NodeUtilities.getFirstValueFromPath(personNode, "/dif:Contact_Person/@uuid");
                            addAttribute(ciResp, "uuid", uuid);

                            String fName = NodeUtilities.getFirstValueFromPath(personNode, "/dif:Contact_Person/dif:First_Name");
                            String mName = NodeUtilities.getFirstValueFromPath(personNode, "/dif:Contact_Person/dif:Middle_Name");
                            String lName = NodeUtilities.getFirstValueFromPath(personNode, "/dif:Contact_Person/dif:Last_Name");
                            if (fName != null || mName != null || lName != null) {
                                StringBuilder indName = new StringBuilder();
                                if (fName != null) {
                                    indName.append(fName).append(" ");
                                }
                                if (mName != null) {
                                    indName.append(mName).append(" ");
                                }
                                if (lName != null) {
                                    indName.append(lName);
                                }
                                addCharacterStringNode(doc, ciResp, "organisationName", indName.toString());
                            }
                            addCharacterStringNode(doc, ciResp, "positionName", "METADATA AUTHOR");

                            buildDiffAddress(doc, ciResp, personNode, null, "/dif:Contact_Person");

                            addCodelistNode(doc, ciResp, "role", "CI_RoleCode", "author");
                        }
                    }
                }
            } else {
                addNullNode(doc, root, GMD, "contact");
            }

            final String metaCreation = NodeUtilities.getFirstValueFromPath(metadata, "/dif:DIF/dif:Metadata_Dates/dif:Metadata_Creation");
            final String metaLastUpda = NodeUtilities.getFirstValueFromPath(metadata, "/dif:DIF/dif:Metadata_Dates/dif:Metadata_Last_Revision");
            final String dataCreation = NodeUtilities.getFirstValueFromPath(metadata, "/dif:DIF/dif:Metadata_Dates/dif:Data_Creation");
            final String dataLastUpda = NodeUtilities.getFirstValueFromPath(metadata, "/dif:DIF/dif:Metadata_Dates/dif:Data_Last_Revision");
            if (notNullDate(metaLastUpda)) {
                addDateTimeNode(doc, root, "dateStamp", metaLastUpda);
            } else {
                addDateTimeNode(doc, root, "dateStamp", metaCreation);
            }

            final Node identInfo = doc.createElementNS(GMD, "identificationInfo");
            root.appendChild(identInfo);
            final Node dIdent = doc.createElementNS(GMD, "MD_DataIdentification");
            identInfo.appendChild(dIdent);
            final Node cit = doc.createElementNS(GMD, "citation");
            dIdent.appendChild(cit);
            final Node citType = doc.createElementNS(GMD, "CI_Citation");
            cit.appendChild(citType);

            final String titleValue = NodeUtilities.getFirstValueFromPath(metadata, "/dif:DIF/dif:Entry_Title");
            addCharacterStringNode(doc, citType, "title", titleValue);
            if (notNullDate(dataCreation)) {
                final Node date = doc.createElementNS(GMD, "date");
                citType.appendChild(date);
                final Node dateType = doc.createElementNS(GMD, "CI_Date");
                date.appendChild(dateType);
                addDateTimeNode(doc, dateType, "date", dataCreation);
                addCodelistNode(doc, dateType, "dateType", "CI_DateTypeCode", "creation");
            }
            if (notNullDate(dataLastUpda)) {
                final Node date = doc.createElementNS(GMD, "date");
                citType.appendChild(date);
                final Node dateType = doc.createElementNS(GMD, "CI_Date");
                date.appendChild(dateType);
                addDateTimeNode(doc, dateType, "date", dataLastUpda);
                addCodelistNode(doc, dateType, "dateType", "CI_DateTypeCode", "revision");
            }

            final String abstractValue = NodeUtilities.getFirstValueFromPath(metadata, "/dif:DIF/dif:Summary/dif:Abstract");
            addCharacterStringNode(doc, dIdent, "abstract", abstractValue);

            final String purposeValue = NodeUtilities.getFirstValueFromPath(metadata, "/dif:DIF/dif:Summary/dif:Purpose");
            addCharacterStringNode(doc, dIdent, "purpose", purposeValue);

            final String progressValue = NodeUtilities.getFirstValueFromPath(metadata, "/dif:DIF/dif:Dataset_Progress");
            addCodelistNode(doc, dIdent, "status", "MD_ProgressCode", progressValue);

            List<Node> orgNodes = NodeUtilities.getNodeFromPath(metadata, "/dif:Organization");
            for (Node orgNode : orgNodes) {
                String orgType = NodeUtilities.getFirstValueFromPath(orgNode, "/dif:Organization/dif:Organization_Type");
                String orgUrl  = NodeUtilities.getFirstValueFromPath(orgNode, "/dif:Organization/dif:Organization_URL");
                String orgName  = NodeUtilities.getFirstValueFromPath(orgNode, "/dif:Organization/dif:Organization_Name/dif:Short_Name");
                String orguuid  = NodeUtilities.getFirstValueFromPath(orgNode, "/dif:Organization/dif:Organization_Name/@uuid");

                if (!"DISTRIBUTOR".equals(orgType)) {

                    List<Node> contacts = NodeUtilities.getNodeFromPath(orgNode, "/dif:Personnel");
                    if (!contacts.isEmpty()) {

                        for (Node contactNode : contacts) {
                            List<Node> groups = NodeUtilities.getNodeFromPath(contactNode, "/dif:Contact_Group");
                            for (Node groupNode : groups) {
                                final Node poc = doc.createElementNS(GMD, "pointOfContact");
                                dIdent.appendChild(poc);
                                final Node ciResp = doc.createElementNS(GMD, "CI_ResponsibleParty");
                                poc.appendChild(ciResp);
                                addAttribute(ciResp, "uuid", orguuid);
                                addCharacterStringNode(doc, ciResp, "organisationName", orgName);
                                addCharacterStringNode(doc, ciResp, "positionName", "DATA CENTER CONTACT");

                                buildDiffAddress(doc, ciResp, groupNode, orgUrl, "/dif:Contact_Group");

                                addCodelistNode(doc, ciResp, "role", "CI_RoleCode", orgType);
                            }

                            List<Node> persons = NodeUtilities.getNodeFromPath(contactNode, "/dif:Contact_Person");
                            for (Node personNode : persons) {
                                final Node poc = doc.createElementNS(GMD, "pointOfContact");
                                dIdent.appendChild(poc);
                                final Node ciResp = doc.createElementNS(GMD, "CI_ResponsibleParty");
                                poc.appendChild(ciResp);
                                addAttribute(ciResp, "uuid", orguuid);
                                String fName = NodeUtilities.getFirstValueFromPath(personNode, "/dif:Contact_Person/dif:First_Name");
                                String mName = NodeUtilities.getFirstValueFromPath(personNode, "/dif:Contact_Person/dif:Middle_Name");
                                String lName = NodeUtilities.getFirstValueFromPath(personNode, "/dif:Contact_Person/dif:Last_Name");
                                if (fName != null || mName != null || lName != null) {
                                    StringBuilder indName = new StringBuilder();
                                    if (fName != null) {
                                        indName.append(fName).append(" ");
                                    }
                                    if (mName != null) {
                                        indName.append(mName).append(" ");
                                    }
                                    if (lName != null) {
                                        indName.append(lName);
                                    }
                                    addCharacterStringNode(doc, ciResp, "individualName", indName.toString());
                                }
                                addCharacterStringNode(doc, ciResp, "organisationName", orgName);
                                addCharacterStringNode(doc, ciResp, "positionName", "DATA CENTER CONTACT");

                                buildDiffAddress(doc, ciResp, personNode, orgUrl, "/dif:Contact_Person");

                                addCodelistNode(doc, ciResp, "role", "CI_RoleCode", orgType);
                            }
                        }
                    }
                }
            }

            final List<Node> multiMedias = NodeUtilities.getNodeFromPath(metadata, "/dif:Multimedia_Sample");
            for (Node multiMedia : multiMedias) {
                Node graOvNode = doc.createElementNS(GMD, "graphicOverview");
                dIdent.appendChild(graOvNode);
                Node browNode = doc.createElementNS(GMD, "MD_BrowseGraphic");
                graOvNode.appendChild(browNode);

                String fileValue = NodeUtilities.getFirstValueFromPath(multiMedia, "/dif:Multimedia_Sample/dif:File");
                if (fileValue != null) {
                    Node fileNNode = doc.createElementNS(GMD, "fileName");
                    browNode.appendChild(fileNNode);
                    Node fileXNode = doc.createElementNS(GMX, "FileName");
                    fileXNode.setTextContent(fileValue);
                    ((Element)fileXNode).setAttribute("src", fileValue);
                    fileNNode.appendChild(fileXNode);
                } else {
                     addNullNode(doc, browNode, GMD, "fileName", "inapplicable");
                }

                String descValue = NodeUtilities.getFirstValueFromPath(multiMedia, "/dif:Multimedia_Sample/dif:Description");
                addCharacterStringNode(doc, browNode, "fileDescription", descValue);

                String formatValue = NodeUtilities.getFirstValueFromPath(multiMedia, "/dif:Multimedia_Sample/dif:Format");
                if (formatValue != null) {
                    final Node node = doc.createElementNS(GMD, "fileType");
                    final Node val = doc.createElementNS(GMX, "MimeFileType");
                    val.setTextContent(formatValue);
                    ((Element)val).setAttribute("type", formatValue);
                    node.appendChild(val);
                    browNode.appendChild(node);
                }
            }

            final List<String> kw1Value = NodeUtilities.getValuesFromPath(metadata, "/dif:DIF/dif:Science_Keywords/dif:Category");
            final List<String> kw2Value = NodeUtilities.getValuesFromPath(metadata, "/dif:DIF/dif:Science_Keywords/dif:Topic");
            final List<String> kw3Value = NodeUtilities.getValuesFromPath(metadata, "/dif:DIF/dif:Science_Keywords/dif:Term");
            kw1Value.addAll(kw2Value);
            kw1Value.addAll(kw3Value);
            if (!kw1Value.isEmpty()) {
                final Node descKw = doc.createElementNS(GMD, "descriptiveKeywords");
                dIdent.appendChild(descKw);
                final Node mdDesc = doc.createElementNS(GMD, "MD_Keywords");
                descKw.appendChild(mdDesc);

                for (String kw : kw1Value) {
                    addCharacterStringNode(doc, mdDesc, "keyword", kw);
                }
                addCodelistNode(doc, mdDesc, "type", "MD_KeywordTypeCode", "theme");

                final Node thName = doc.createElementNS(GMD, "thesaurusName");
                mdDesc.appendChild(thName);
                final Node thCit = doc.createElementNS(GMD, "CI_Citation");
                thName.appendChild(thCit);
                addCharacterStringNode(doc, thCit, "title", "NASA/GCMD Science Keywords");
                addCharacterStringNode(doc, thCit, "alternateTitle", "Science Keywords");
                final Node date = doc.createElementNS(GMD, "date");
                thCit.appendChild(date);
                final Node dateType = doc.createElementNS(GMD, "CI_Date");
                date.appendChild(dateType);
                addDateTimeNode(doc, dateType, "date", "2016-08-10T00:00:00");
                addCodelistNode(doc, dateType, "dateType", "CI_DateTypeCode", "revision");
                addCharacterStringNode(doc, thCit, "collectiveTitle", "Global Change Master Directory (GCMD). 2016. GCMD Keywords, Version 8.4. Greenbelt, MD: Global Change Data Center, Science and Exploration Directorate, Goddard Space Flight Center (GSFC) National Aeronautics and Space Administration (NASA). URL (GCMD Keyword Forum Page)");
            }

            final List<String> kw4Value = NodeUtilities.getValuesFromPath(metadata, "/dif:DIF/dif:Ancillary_Keyword");
            if (!kw4Value.isEmpty()) {
                final Node descKw = doc.createElementNS(GMD, "descriptiveKeywords");
                dIdent.appendChild(descKw);
                final Node mdDesc = doc.createElementNS(GMD, "MD_Keywords");
                descKw.appendChild(mdDesc);

                for (String kw : kw4Value) {
                    addCharacterStringNode(doc, mdDesc, "keyword", kw);
                }
            }

            String accValue = NodeUtilities.getFirstValueFromPath(metadata, "/dif:DIF/dif:Access_Constraints");
            String useValue = NodeUtilities.getFirstValueFromPath(metadata, "/dif:DIF/dif:Use_Constraints");

            if (useValue != null || accValue != null) {
                final Node resCst = doc.createElementNS(GMD, "resourceConstraints");
                dIdent.appendChild(resCst);
                final Node legCst = doc.createElementNS(GMD, "MD_LegalConstraints");
                resCst.appendChild(legCst);
                addCharacterStringNode(doc, legCst, "useLimitation", useValue);
                addCharacterStringNode(doc, legCst, "otherConstraints", accValue);
            }

            final String langValue = NodeUtilities.getFirstValueFromPath(metadata, "/dif:DIF/dif:Dataset_Language");
            if (langValue != null) {
                addCodelistNode(doc, dIdent, "language", "LanguageCode", langValue); // TODO ISO code
            } else {
                addCodelistNode(doc, dIdent, "language", "LanguageCode", "eng");
            }

            final List<String> categoryValues = NodeUtilities.getValuesFromPath(metadata, "/dif:DIF/dif:ISO_Topic_Category");
            for (String categoryValue : categoryValues) {
                categoryValue = convertISOTopicValue(categoryValue);
                if (categoryValue != null) {
                    addGCONode(doc, dIdent, "topicCategory", categoryValue, GMD, "MD_TopicCategoryCode");
                }
            }


            final String locationValue = NodeUtilities.getFirstValueFromPath(metadata, "/dif:DIF/dif:Location/dif:Detailed_Location");
            if (locationValue != null) {
                final Node extent = doc.createElementNS(GMD, "extent");
                dIdent.appendChild(extent);
                Node exExtent = doc.createElementNS(GMD, "EX_Extent");
                extent.appendChild(exExtent);

                final Node geoElem = doc.createElementNS(GMD, "geographicElement");
                exExtent.appendChild(geoElem);
                final Node geoDesc = doc.createElementNS(GMD, "EX_GeographicDescription");
                geoElem.appendChild(geoDesc);
                final Node geoId = doc.createElementNS(GMD, "geographicIdentifier");
                geoDesc.appendChild(geoId);
                final Node mdId = doc.createElementNS(GMD, "MD_Identifier");
                geoId.appendChild(mdId);
                addCharacterStringNode(doc, mdId, "code", locationValue);
            }

            final List<Node> spaCovNodes = NodeUtilities.getNodeFromPath(metadata, "/dif:Spatial_Coverage");

            for (Node spaCovNode : spaCovNodes) {
                final Node extent = doc.createElementNS(GMD, "extent");
                dIdent.appendChild(extent);
                Node exExtent = doc.createElementNS(GMD, "EX_Extent");
                extent.appendChild(exExtent);

                final String crsType = NodeUtilities.getFirstValueFromPath(spaCovNode, "/dif:Spatial_Coverage/dif:Granule_Spatial_Representation");
                if (crsType != null) {
                    addCharacterStringNode(doc, exExtent, "description", "SpatialGranuleSpatialRepresentation=" + crsType);
                }

                final List<Node> rectNodes = NodeUtilities.getNodeFromPath(spaCovNode, "/dif:Geometry/dif:Bounding_Rectangle");
                for (Node rectNode : rectNodes) {
                    String wests = NodeUtilities.getFirstValueFromPath(rectNode, "/dif:Bounding_Rectangle/dif:Westernmost_Longitude");
                    String easts = NodeUtilities.getFirstValueFromPath(rectNode, "/dif:Bounding_Rectangle/dif:Easternmost_Longitude");
                    String norths = NodeUtilities.getFirstValueFromPath(rectNode, "/dif:Bounding_Rectangle/dif:Northernmost_Latitude");
                    String souths = NodeUtilities.getFirstValueFromPath(rectNode, "/dif:Bounding_Rectangle/dif:Southernmost_Latitude");
                    if (wests != null && easts != null && norths != null && souths != null) {
                        final Node geoElem = doc.createElementNS(GMD, "geographicElement");
                        exExtent.appendChild(geoElem);
                        final Node geoBox = doc.createElementNS(GMD, "EX_GeographicBoundingBox");
                        geoElem.appendChild(geoBox);

                        addDecimalNode(doc, geoBox, "westBoundLongitude", wests);
                        addDecimalNode(doc, geoBox, "eastBoundLongitude", easts);
                        addDecimalNode(doc, geoBox, "southBoundLatitude", souths);
                        addDecimalNode(doc, geoBox, "northBoundLatitude", norths);
                    }
                    String minAltValue = NodeUtilities.getFirstValueFromPath(rectNode, "/dif:Bounding_Rectangle/dif:Minimum_Altitude");
                    String maxAltValue = NodeUtilities.getFirstValueFromPath(rectNode, "/dif:Bounding_Rectangle/dif:Maximum_Altitude");

                    if (minAltValue != null || maxAltValue != null) {
                        final Node vertElem = doc.createElementNS(GMD, "verticalElement");
                        exExtent.appendChild(vertElem);
                        final Node vertEx = doc.createElementNS(GMD, "EX_VerticalExtent");
                        vertElem.appendChild(vertEx);

                        addRealNode(doc, vertEx, "minimumValue", minAltValue);
                        addRealNode(doc, vertEx, "maximumValue", maxAltValue);
                    }
                }
            }

            final String beginValue = NodeUtilities.getFirstValueFromPath(metadata, "/dif:DIF/dif:Temporal_Coverage/dif:Range_DateTime/dif:Beginning_Date_Time");
            final String endValue = NodeUtilities.getFirstValueFromPath(metadata, "/dif:DIF/dif:Temporal_Coverage/dif:Range_DateTime/dif:Ending_Date_Time");
            if (beginValue != null || endValue != null) {

                final Node extent = doc.createElementNS(GMD, "extent");
                dIdent.appendChild(extent);
                Node exExtent = doc.createElementNS(GMD, "EX_Extent");
                extent.appendChild(exExtent);

                final Node tempElem = doc.createElementNS(GMD, "temporalElement");
                exExtent.appendChild(tempElem);
                final Node tempEx = doc.createElementNS(GMD, "EX_TemporalExtent");
                tempElem.appendChild(tempEx);
                final Node ext = doc.createElementNS(GMD, "extent");
                tempEx.appendChild(ext);
                final Node tp = doc.createElementNS(GML, "TimePeriod");
                addAttributeNS(tp, "http://www.opengis.net/gml/3.2", "id", "timeperiod1");
                ext.appendChild(tp);

                final Node beg = doc.createElementNS(GML, "beginPosition");
                beg.setTextContent(beginValue);
                final Node end = doc.createElementNS(GML, "endPosition");
                end.setTextContent(endValue);
                tp.appendChild(beg);
                tp.appendChild(end);
            }

            final Node distInfo = doc.createElementNS(GMD, "distributionInfo");
            root.appendChild(distInfo);
            final Node mdDIst = doc.createElementNS(GMD, "MD_Distribution");
            distInfo.appendChild(mdDIst);

            List<Node> distNodes = NodeUtilities.getNodeFromPath(metadata, "/dif:Distribution");
            for (Node distNode : distNodes) {
                String format = NodeUtilities.getFirstValueFromPath(distNode, "/dif:Distribution/dif:Distribution_Format");
                if (format != null) {
                    final Node distFormat = doc.createElementNS(GMD, "distributionFormat");
                    mdDIst.appendChild(distFormat);
                    final Node mdFormat = doc.createElementNS(GMD, "MD_Format");
                    distFormat.appendChild(mdFormat);
                    addCharacterStringNode(doc, mdFormat, "name", format);
                    addNullNode(doc, mdFormat, GMD, "version", "inapplicable");
                }
            }

            Node mdDistributor = null;
            for (Node orgNode : orgNodes) {
                String orgType  = NodeUtilities.getFirstValueFromPath(orgNode, "/dif:Organization/dif:Organization_Type");
                String orgUrl   = NodeUtilities.getFirstValueFromPath(orgNode, "/dif:Organization/dif:Organization_URL");
                String orgName  = NodeUtilities.getFirstValueFromPath(orgNode, "/dif:Organization/dif:Organization_Name/dif:Short_Name");
                String orguuid  = NodeUtilities.getFirstValueFromPath(orgNode, "/dif:Organization/dif:Organization_Name/@uuid");

                if ("DISTRIBUTOR".equals(orgType)) {

                    List<Node> distributors = NodeUtilities.getNodeFromPath(orgNode, "/dif:Personnel");
                    if (!distributors.isEmpty()) {
                        final Node distributor = doc.createElementNS(GMD, "distributor");
                        mdDIst.appendChild(distributor);
                        mdDistributor = doc.createElementNS(GMD, "MD_Distributor");
                        distributor.appendChild(mdDistributor);

                        for (Node distributorNode : distributors) {
                            String role = NodeUtilities.getFirstValueFromPath(distributorNode, "/dif:Personnel/dif:Role");
                            if ("DATA CENTER CONTACT".equals(role)) {

                                List<Node> groups = NodeUtilities.getNodeFromPath(distributorNode, "/dif:Contact_Group");
                                for (Node groupNode : groups) {
                                    final Node contact = doc.createElementNS(GMD, "distributorContact");
                                    mdDistributor.appendChild(contact);
                                    final Node ciResp = doc.createElementNS(GMD, "CI_ResponsibleParty");
                                    contact.appendChild(ciResp);

                                    addAttribute(ciResp, "uuid", orguuid);

                                    //addCharacterStringNode(doc, ciResp, "organisationName", NodeUtilities.getFirstValueFromPath(groupNode, "dif:Name"));
                                    addCharacterStringNode(doc, ciResp, "organisationName", orgName);
                                    addCharacterStringNode(doc, ciResp, "positionName", "DATA CENTER CONTACT");

                                    buildDiffAddress(doc, ciResp, groupNode, orgUrl, "/dif:Contact_Group");

                                    addCodelistNode(doc, ciResp, "role", "CI_RoleCode", "distributor");
                                }

                                List<Node> persons = NodeUtilities.getNodeFromPath(distributorNode, "/dif:Contact_Person");
                                for (Node personNode : persons) {
                                    final Node contact = doc.createElementNS(GMD, "distributorContact");
                                    mdDistributor.appendChild(contact);
                                    final Node ciResp = doc.createElementNS(GMD, "CI_ResponsibleParty");
                                    contact.appendChild(ciResp);

                                    addAttribute(ciResp, "uuid", orguuid);

                                    String fName = NodeUtilities.getFirstValueFromPath(personNode, "/dif:Contact_Person/dif:First_Name");
                                    String mName = NodeUtilities.getFirstValueFromPath(personNode, "/dif:Contact_Person/dif:Middle_Name");
                                    String lName = NodeUtilities.getFirstValueFromPath(personNode, "/dif:Contact_Person/dif:Last_Name");
                                    if (fName != null || mName != null || lName != null) {
                                        StringBuilder indName = new StringBuilder();
                                        if (fName != null) {
                                            indName.append(fName).append(" ");
                                        }
                                        if (mName != null) {
                                            indName.append(mName).append(" ");
                                        }
                                        if (lName != null) {
                                            indName.append(lName);
                                        }
                                        addCharacterStringNode(doc, ciResp, "individualName", indName.toString());
                                    }
                                    addCharacterStringNode(doc, ciResp, "organisationName", orgName);
                                    addCharacterStringNode(doc, ciResp, "positionName", "DATA CENTER CONTACT");

                                    buildDiffAddress(doc, ciResp, personNode, orgUrl, "/dif:Contact_Person");

                                    addCodelistNode(doc, ciResp, "role", "CI_RoleCode", "distributor");
                                }
                            }
                        }
                    }
                }
            }

            List<Node> urlNodes = NodeUtilities.getNodeFromPath(metadata, "/dif:Related_URL");

            for (Node urlNode : urlNodes) {
                String urlType = NodeUtilities.getFirstValueFromPath(urlNode, "/dif:Related_URL/dif:URL_Content_Type/dif:Type");
                String proto = NodeUtilities.getFirstValueFromPath(urlNode, "/dif:Related_URL/dif:Protocol");
                String url = NodeUtilities.getFirstValueFromPath(urlNode, "/dif:Related_URL/dif:URL");
                String title = NodeUtilities.getFirstValueFromPath(urlNode, "/dif:Related_URL/dif:Title");
                String desc = NodeUtilities.getFirstValueFromPath(urlNode, "/dif:Related_URL/dif:Description");

                if (urlType != null || proto != null || url != null || title != null || desc != null) {
                    final Node distTransfer = doc.createElementNS(GMD, "transferOptions");
                    mdDIst.appendChild(distTransfer);
                    final Node mdTransfer = doc.createElementNS(GMD, "MD_DigitalTransferOptions");
                    distTransfer.appendChild(mdTransfer);
                    final Node online = doc.createElementNS(GMD, "onLine");
                    mdTransfer.appendChild(online);
                    final Node ciOnline = doc.createElementNS(GMD, "CI_OnlineResource");
                    online.appendChild(ciOnline);
                    addGCONode(doc, ciOnline, "linkage", url, GMD, "URL");
                    addCharacterStringNode(doc, ciOnline, "protocol", proto);
                    addCharacterStringNode(doc, ciOnline, "name", title);
                    addCharacterStringNode(doc, ciOnline, "description", desc);
                    addCodelistNode(doc, ciOnline, "function", "CI_OnLineFunctionCode", urlType);
                }
            }

            for (Node distNode : distNodes) {
                String size = NodeUtilities.getFirstValueFromPath(distNode, "/dif:Distribution/dif:Distribution_Size");
                String media = NodeUtilities.getFirstValueFromPath(distNode, "/dif:Distribution/dif:Distribution_Media");

                if (size != null || media != null) {
                    final Node distTransfer = doc.createElementNS(GMD, "transferOptions");
                    mdDIst.appendChild(distTransfer);
                    final Node mdTransfer = doc.createElementNS(GMD, "MD_DigitalTransferOptions");
                    distTransfer.appendChild(mdTransfer);
                    addRealNode(doc, mdTransfer, "transferSize", size);
                }

                String fees = NodeUtilities.getFirstValueFromPath(distNode, "/dif:Distribution//dif:Fees");

                if (fees != null) {
                    if (mdDistributor == null) {
                        final Node distributor = doc.createElementNS(GMD, "distributor");
                        mdDIst.appendChild(distributor);
                        mdDistributor = doc.createElementNS(GMD, "MD_Distributor");
                        distributor.appendChild(mdDistributor);
                    }
                    final Node distOrder = doc.createElementNS(GMD, "distributionOrderProcess");
                    mdDistributor.appendChild(distOrder);
                    final Node mdOrder = doc.createElementNS(GMD, "MD_StandardOrderProcess");
                    distOrder.appendChild(mdOrder);
                    addCharacterStringNode(doc, mdOrder, "fees", fees);
                }
            }

            if (!platformNodes.isEmpty()) {
                final Node acquiInfo = doc.createElementNS(GMI, "acquisitionInformation");
                root.appendChild(acquiInfo);
                final Node miAcqui = doc.createElementNS(GMI, "MI_AcquisitionInformation");
                acquiInfo.appendChild(miAcqui);

                for (Node platformNode : platformNodes) {
                    final Node platf = doc.createElementNS(GMI, "platform");
                    miAcqui.appendChild(platf);
                    final Node miPlatf = doc.createElementNS(GMI, "MI_Platform");
                    platf.appendChild(miPlatf);

                    String shortName = NodeUtilities.getFirstValueFromPath(platformNode, "/dif:Platform/dif:Short_Name");
                    String longName  = NodeUtilities.getFirstValueFromPath(platformNode, "/dif:Platform/dif:Long_Name");
                    if (longName != null) {
                        final Node platCit = doc.createElementNS(GMI, "citation");
                        miPlatf.appendChild(platCit);
                        final Node ciPlatCit = doc.createElementNS(GMD, "CI_Citation");
                        platCit.appendChild(ciPlatCit);
                        addCharacterStringNode(doc, ciPlatCit, "title", longName);
                        addNullNode(doc, ciPlatCit, GMD, "date");

                    }
                    if (shortName != null) {
                        final Node platId = doc.createElementNS(GMI, "identifier");
                        miPlatf.appendChild(platId);
                        final Node mdPlatId = doc.createElementNS(GMD, "MD_Identifier");
                        platId.appendChild(mdPlatId);
                        addCharacterStringNode(doc, mdPlatId, "code", shortName);
                    }
                    addCharacterStringNode(doc, miPlatf, GMI, "description", longName);
                    List<Node> instrumentNodes = NodeUtilities.getNodeFromPath(platformNode, "/dif:Instrument");
                    for (Node instrumentNode : instrumentNodes) {
                        final Node inst = doc.createElementNS(GMI, "instrument");
                        miPlatf.appendChild(inst);
                        final Node miInst = doc.createElementNS(GMI, "MI_Instrument");
                        inst.appendChild(miInst);

                        String instShortName = NodeUtilities.getFirstValueFromPath(instrumentNode, "/dif:Instrument/dif:Short_Name");
                        String instLongName  = NodeUtilities.getFirstValueFromPath(instrumentNode, "/dif:Instrument/dif:Long_Name");
                        if (longName != null || shortName != null) {
                            final Node instCit = doc.createElementNS(GMI, "citation");
                            miInst.appendChild(instCit);
                            final Node ciInstCit = doc.createElementNS(GMD, "CI_Citation");
                            instCit.appendChild(ciInstCit);
                            if (longName != null) {
                                addCharacterStringNode(doc, ciInstCit, "title", instLongName);
                            }
                            addNullNode(doc, ciInstCit, GMD, "date");

                             if (shortName != null) {
                                final Node instId = doc.createElementNS(GMD, "identifier");
                                ciInstCit.appendChild(instId);
                                final Node mdInstId = doc.createElementNS(GMD, "MD_Identifier");
                                instId.appendChild(mdInstId);
                                addCharacterStringNode(doc, mdInstId, "code", instShortName);
                            }
                        }
                       
                        //final Node instType = doc.createElementNS(GMI, "type");
                        //final Node instTypeCode = doc.createElementNS(GMI, "MI_SensorTypeCode");
                        //instType.appendChild(instTypeCode);
                        //miInst.appendChild(instType);
                        addNullNode(doc, miInst, GMI, "type");

                    }


                }
            }


            doc.appendChild(root);
            return root;
        }
        return null;
    }

    private static final String[] TOPICS = new String[]{"farming" , "biota" , "boundaries" , "climatologyMeteorologyAtmosphere" , "economy" , "elevation" , "environment" , "geoscientificInformation" , "health" , "imageryBaseMapsEarthCover" , "intelligenceMilitary" , "inlandWaters" , "location" , "oceans" , "planningCadastre" , "society" , "structure" , "transportation" , "utilitiesCommunication"};
    private String convertISOTopicValue(String categoryValue) {
        if (categoryValue != null) {
            categoryValue = categoryValue.replace(" ", "");
            categoryValue = categoryValue.replace("/", "");
            for (String topic : TOPICS) {
                if (categoryValue.equalsIgnoreCase(topic) ||
                   (categoryValue + "s").equalsIgnoreCase(topic)) {  // special case for ocean/oceans
                    return topic;
                }
            }
            LOGGER.info("ignore unknow iso topic category:" + categoryValue);
        }
        return null;
    }

    private void addDateTimeNode(Document doc, Node root, String nodeName, String value) {
        DateLabel dl = DateLabel.getFromLabel(value);
        if (dl != null) {
            value = dl.getLabelDate();
        } else {
            value = addTimeToDate(value);
        }
        addGCONode(doc, root, nodeName, value, GCO, "DateTime");
    }

    private void addDateNode(Document doc, Node root, String nodeName, String value) {
        addGCONode(doc, root, nodeName, value, GCO, "Date");
    }

    private void addCharacterStringNode(Document doc, Node root, String nodeNmsp, String nodeName, String value) {
        addGCONode(doc, root, nodeNmsp, nodeName, value, GCO, "CharacterString");
    }

    private void addCharacterStringNode(Document doc, Node root, String nodeName, String value) {
        addGCONode(doc, root, nodeName, value, GCO, "CharacterString");
    }

    private void addDecimalNode(Document doc, Node root, String nodeName, String value) {
        addGCONode(doc, root, nodeName, value, GCO, "Decimal");
    }

    private void addRealNode(Document doc, Node root, String nodeName, String value) {
        addGCONode(doc, root, nodeName, value, GCO, "Real");
    }

    private void addGCONode(Document doc, Node root, String nodeName, String value, String gcoNmsp, String gcoType) {
        addGCONode(doc, root, GMD, nodeName, value, gcoNmsp, gcoType);
    }
    private void addGCONode(Document doc, Node root, String nodeNmsp, String nodeName, String value, String gcoNmsp, String gcoType) {
        if (value != null) {
            final Node node = doc.createElementNS(nodeNmsp, nodeName);
            final Node val = doc.createElementNS(gcoNmsp, gcoType);
            val.setTextContent(value);
            node.appendChild(val);
            root.appendChild(node);
        }
    }

    private void addNullNode(Document doc, Node root, String nmsp, String nodeName) {
        addNullNode(doc, root, nmsp, nodeName, "unknown");
    }

    private void addNullNode(Document doc, Node root, String nmsp, String nodeName, String value) {
        final Node node = doc.createElementNS(nmsp, nodeName);
        addAttributeNS(node, GCO, "nilReason", value);
        root.appendChild(node);
    }

    private void addAttribute(Node root, String attName, String value) {
        if (value != null) {
            ((Element)root).setAttribute(attName, value);
        }
    }

    private void addAttributeNS(Node root, String nmsp, String attName, String value) {
        if (value != null) {
            ((Element)root).setAttributeNS(nmsp, attName, value);
        }
    }

    private void addCodelistNode(Document doc, Node root, String nodeName, String codelistName, String value) {
        if (value != null) {
            final Node node = doc.createElementNS(GMD, nodeName);
            final Node val = doc.createElementNS(GMD, codelistName);

            ((Element) val).setAttribute("codeList", "http://standards.iso.org/ittf/PubliclyAvailableStandards/ISO_19139_Schemas/resources/Codelist/ML_gmxCodelists.xml#" + codelistName);
            ((Element) val).setAttribute("codeListValue", value);
            val.setTextContent(value);
            node.appendChild(val);
            root.appendChild(node);
        }
    }

    private boolean notNullDate(String dateValue) {
        return dateValue != null && !"unknown".equalsIgnoreCase(dateValue);
    }

    private String addTimeToDate(String date) {
        if (notNullDate(date) && !date.contains("T")) {
            return date + "T00:00:00";
        }
        return date;
    }

    private void buildDiffAddress(Document doc, Node ciResp, Node difNode, String url, String parent) {
        final Node contactInfo = doc.createElementNS(GMD, "contactInfo");
        ciResp.appendChild(contactInfo);
        final Node ciContact = doc.createElementNS(GMD, "CI_Contact");
        contactInfo.appendChild(ciContact);

        String phoneNum = NodeUtilities.getFirstValueFromPath(difNode, parent + "/dif:Phone/dif:Number");
        if (phoneNum != null) {
            final Node phone = doc.createElementNS(GMD, "phone");
            ciContact.appendChild(phone);
            final Node ciPhone = doc.createElementNS(GMD, "CI_Telephone");
            phone.appendChild(ciPhone);
            addCharacterStringNode(doc, ciPhone, "voice", phoneNum);
        }

        final Node address = doc.createElementNS(GMD, "address");
        ciContact.appendChild(address);
        final Node ciAddress = doc.createElementNS(GMD, "CI_Address");
        address.appendChild(ciAddress);

        addCharacterStringNode(doc, ciAddress, "deliveryPoint", NodeUtilities.getFirstValueFromPath(difNode, parent + "/dif:Address/dif:Street_Address"));
        addCharacterStringNode(doc, ciAddress, "city", NodeUtilities.getFirstValueFromPath(difNode, parent + "/dif:Address/dif:City"));
        addCharacterStringNode(doc, ciAddress, "postalCode", NodeUtilities.getFirstValueFromPath(difNode, parent + "/dif:Address/dif:Postal_Code"));
        addCharacterStringNode(doc, ciAddress, "country", NodeUtilities.getFirstValueFromPath(difNode, parent + "/dif:Address/dif:Country"));
        addCharacterStringNode(doc, ciAddress, "electronicMailAddress", NodeUtilities.getFirstValueFromPath(difNode, parent + "/dif:Email"));

        if (url != null) {
            final Node onlineResource = doc.createElementNS(GMD, "onlineResource");
            ciContact.appendChild(onlineResource);
            final Node ciOnline = doc.createElementNS(GMD, "CI_OnlineResource");
            onlineResource.appendChild(ciOnline);
            addGCONode(doc, ciOnline, "linkage", url, GMD, "URL");
        }
    }



     protected Node convertAndApplyElementSet(MetadataType metadataMode, MetadataType mode, ElementSetType type, List<QName> elementName, Node metadataNode) throws MetadataIoException {
         final Node n;

        // DIF TO CSW2
        if (metadataMode ==  MetadataType.DIF && mode == MetadataType.DUBLINCORE_CSW202) {
            n = translateDIFtoDCNode(metadataNode, type, elementName, LegacyNamespaces.CSW);

        // DIF TO CSW3
        } else if (metadataMode ==  MetadataType.DIF && mode == MetadataType.DUBLINCORE_CSW300) {
            n = translateDIFtoDCNode(metadataNode, type, elementName, Namespaces.CSW);

        // DIF TO ISO
        } else if (metadataMode ==  MetadataType.DIF && mode == MetadataType.ISO_19115) {
            n = translateDIFtoISONode(metadataNode, type, elementName);

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
}
