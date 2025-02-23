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

package org.constellation.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.XMLConstants;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import java.time.format.DateTimeParseException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.sis.temporal.LenientDateFormat;
import org.apache.sis.xml.privy.LegacyNamespaces;
import org.apache.sis.xml.MarshallerPool;
import org.apache.sis.xml.XML;
import static org.apache.sis.xml.XML.SCHEMAS;
import org.constellation.api.PathType;
import org.constellation.jaxb.MarshallWarnings;
import org.w3c.dom.Comment;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class NodeUtilities {

    private static final TimeZone TZ = TimeZone.getTimeZone("GMT+2:00");

    private static final String NULL_VALUE = "null";

    private static final Logger LOGGER = Logger.getLogger("org.constellation.util");

    public static List<Node> getNodes(final String propertyName, final List<Node> nodes, final int ordinal, final boolean create) {
        final List<Node> result = new ArrayList<>();
        for (Node e : nodes) {
            final List<Node> nl = getChilds(e, propertyName);
            // add new node
            if (nl.isEmpty() && create) {
                final Element newNode = e.getOwnerDocument().createElementNS("TODO", propertyName);
                e.appendChild(newNode);
                result.add(newNode);

            // Select the node to update
            } else {
                for (int i = 0 ; i < nl.size(); i++) {
                    if (ordinal == -1) {
                        result.add(nl.get(i));
                    } else if (i == ordinal) {
                        result.add(nl.get(i));
                    }
                }
            }
        }
        return result;
    }

    public static List<Node> getChilds(final Node n, final String propertyName) {
        final List<Node> results = new ArrayList<>();
        if (propertyName.startsWith("@")) {
            final Node att = n.getAttributes().getNamedItem(propertyName.substring(1));
            if (att != null) {
                results.add(att);
            }
        } else {
            final NodeList nl = n.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                final Node child = nl.item(i);
                if (propertyName.equals("*") || propertyName.equals(child.getLocalName())) {
                    results.add(child);
                }
            }
        }
        return results;
    }

    public static List<Node> getNodeFromConditionalPath(final String xpath, final String conditionalPath, final String conditionalValue, final Node metadata) {
        final List<Node> results = new ArrayList<>();
        final String[] xpart = xpath.split("/");
        final String[] cpart = conditionalPath.split("/");
        final int min = Math.min(xpart.length, cpart.length);
        int i = 0;
        String commonPath = "";
        while (i < min && xpart[i].equals(cpart[i])) {
            commonPath += "/" + xpart[i];
           i++;
        }
        commonPath = commonPath.substring(1);
        final List<Node> nodes = getNodeFromPath(metadata, commonPath);

        for (Node n : nodes) {
            final List<Node> conditionalNode = getNodeFromPath(n, conditionalPath.substring(commonPath.length()));
            boolean match = false;
            for (Node cNode : conditionalNode) {
                if (conditionalValue.equalsIgnoreCase(cNode.getTextContent())) {
                    match = true;
                }
            }
            if (match) {
                final List<Node> matchingNodes = getNodeFromPath(n, xpath.substring(commonPath.length()));
                results.addAll(matchingNodes);
            }
        }
        return results;
    }

    public static List<Node> getNodeFromPath(final Node parent, String xpath) {
        return getNodeFromPath(parent, xpath, false);
    }

    public static List<Node> getNodeFromPath(final Node parent, String xpath, final boolean create) {
        //we remove the type name from the xpath
        xpath = xpath.substring(xpath.indexOf('/') + 1);

        List<Node> nodes = Arrays.asList(parent);
        while (!xpath.isEmpty()) {

            //Then we get the next Property name
            int separator = xpath.indexOf('/');
            String propertyName;
            if (separator != -1) {
                propertyName = xpath.substring(0, separator);
            } else {
                propertyName = xpath;
            }
            final int ordinal = extractOrdinal(propertyName);
            final int braceIndex = propertyName.indexOf('[');
            if (braceIndex != -1) {
                propertyName = propertyName.substring(0, braceIndex);
            }

            //remove namespace on propertyName
            final int separatorIndex = propertyName.indexOf(':');
            if (separatorIndex != -1) {
                propertyName = propertyName.substring(separatorIndex + 1);
            }

            nodes = getNodes(propertyName, nodes, ordinal, create);
            if (nodes.isEmpty()) {
                return nodes;
            }

            separator = xpath.indexOf('/');
            if (separator != -1) {
                xpath = xpath.substring(separator + 1);
            } else {
                xpath = "";
            }
        }
        return nodes;
    }

    public static String getFirstValueFromPath(final Node parent, final String xpath) {
        return getFirstValueFromPaths(parent, Arrays.asList(xpath));
    }

    public static String getFirstValueFromPaths(final Node parent, final List<String> xpaths) {

        for (String xpath : xpaths) {
            // verify type
            xpath = xpath.substring(xpath.indexOf(':') + 1);
            final String pathType;
            if (xpath.indexOf('/') != -1) {
                pathType = xpath.substring(0, xpath.indexOf('/'));
            } else {
                pathType = xpath;
            }
            if (!pathType.equals("*") && !pathType.equals(parent.getLocalName())) {
                continue;
            }

            final List<Node> nodes = getNodeFromPath(parent, xpath);
            for (Node n : nodes) {
                if (n.getTextContent() != null && !n.getTextContent().isEmpty())
                return n.getTextContent();
            }
        }
        return null;
    }

    public static List<String> getValuesFromConditionalPath(final Node parent, String xpath, String conditionalPath, final String conditionalValue) {
        final List<String> results = new ArrayList<>();

        // verify type
        conditionalPath = conditionalPath.substring(xpath.indexOf(':') + 1);
        xpath           = xpath.substring(xpath.indexOf(':') + 1);
        final String pathType;
        if (xpath.indexOf('/') != -1) {
            pathType = xpath.substring(0, xpath.indexOf('/'));
        } else {
            pathType = xpath;
        }
        if (!pathType.equals("*") && !pathType.equals(parent.getLocalName())) {
            return results;
        }

        final List<Node> nodes = getNodeFromConditionalPath(xpath, conditionalPath, conditionalValue, parent);
        for (Node n : nodes) {
            results.add(n.getTextContent());
        }

        return results;
    }

    public static List<String> getValuesFromPath(final Node parent, final String xpath) {
        return getValuesFromPaths(parent, Arrays.asList(xpath));
    }

    public static List<String> getValuesFromPaths(final Node parent, final List<String> xpaths) {
        final List<String> results = new ArrayList<>();

        for (String xpath : xpaths) {
            // verify type
            xpath = xpath.substring(xpath.indexOf(':') + 1);
            final String pathType;
            if (xpath.indexOf('/') != -1) {
                pathType = xpath.substring(0, xpath.indexOf('/'));
            } else {
                pathType = xpath;
            }
            if (!pathType.equals("*") && !pathType.equals(parent.getLocalName())) {
                continue;
            }

            final List<Node> nodes = getNodeFromPath(parent, xpath);
            for (Node n : nodes) {
                results.add(n.getTextContent());
            }
        }
        return results;
    }

    public static void appendChilds(final Node parent, final List<Node> children) {
        for (Node child : children) {
            parent.appendChild(child);
        }
    }

    /**
     * Return an ordinal if there is one in the propertyName specified else return -1.
     * example : name[1] return  1
     *           name    return -1
     * @param propertyName A property name extract from an Xpath
     * @return an ordinal if there is one, -1 else.
     */
    public static int extractOrdinal(final String propertyName) {
        int ordinal = -1;

        //we extract the ordinal if there is one
        if (propertyName.indexOf('[') != -1) {
            if (propertyName.indexOf(']') != -1) {
                try {
                    final String ordinalValue = propertyName.substring(propertyName.indexOf('[') + 1, propertyName.indexOf(']'));
                    ordinal = Integer.parseInt(ordinalValue) - 1;
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("The xpath is malformed, the brackets value is not an integer");
                }
            } else {
                throw new IllegalArgumentException("The xpath is malformed, unclosed bracket");
            }
        }
        return ordinal;
    }

    public static List<Node> buildNodes(final Document doc, final String namespace, final String localName, final List<String> values, final boolean mandatory) {
        final List<Node> nodes = new ArrayList<>();
        if (mandatory && values.isEmpty()) {
            final Node n = doc.createElementNS(namespace, localName);
            nodes.add(n);
        }
        for (String value : values) {
            final Node n = doc.createElementNS(namespace, localName);
            n.setTextContent(value);
            nodes.add(n);
        }
        return nodes;
    }

    public static List<Object> extractValues(final Node metadata, final PathType path) {
        return extractValues(metadata, path, true);
    }

    /**
     * Extract the String values denoted by the specified paths
     * and return the values as a String values1,values2,....
     * if there is no values corresponding to the paths the method return "null" (the string)
     *
     * @param metadata
     * @param paths
     * @param parseDate if true, the Date will be parsed into a lucene format String
     * @return
     */
    public static List<Object> extractValues(final Node metadata, final PathType path, boolean parseDate) {
        final List<Object> response  = new ArrayList<>();

        if (path != null) {
            for (String fullPathID: path.paths) {

                NodeAndOrdinal nao = extractNodes(metadata, fullPathID, path.prefixMapping, false);
                if (nao == null) {
                    continue;
                }
                final List<Object> value = getStringValue(nao.nodes, nao.ordinal, path.type, parseDate);
                if (!value.isEmpty() && !value.equals(Arrays.asList(NULL_VALUE))) {
                    response.addAll(value);
                }
            }
        }
        return response;
    }

    /**
     * Extract the nodes from the document within the specified path.
     *
     * @param metadata The Document/Node to perform the extraction on.
     * @param xPath A path to the node to extract.
     * @param prefixMapping a map describing the namespace correspounding to the prefix used in the Xpath.
     * @param create if {@code true} the missing nodes while be created.
     *
     * @return An object containing The extract node, the ordinal and propertyName.
     */
    public static NodeAndOrdinal extractNodes(final Node metadata, String xPath, Map<String, String> prefixMapping, boolean create) {
        // remove Standard
        final String prefix    = xPath.substring(1, xPath.indexOf(':'));
        final String pathNmsp  = prefixMapping.get(prefix);
        xPath = xPath.substring(xPath.indexOf(':') + 1);
        final String pathType =  xPath.substring(0, xPath.indexOf('/'));
        if (!matchRootName(metadata, pathType, pathNmsp)) {
            return null;
        }
         String pathID;
         String conditionalPath  = null;
         String conditionalValue = null;

         // if the path ID contains a # we have a conditional value next to the searched value.
         final int separator = xPath.indexOf('#');
         if (separator != -1) {
             pathID               = xPath.substring(0, separator);
             conditionalPath      = pathID + '/' + xPath.substring(separator + 1, xPath.indexOf('='));
             conditionalValue     = xPath.substring(xPath.indexOf('=') + 1);
             int nextSeparator    = conditionalValue.indexOf('/');
             if (nextSeparator == -1) {
                 throw new IllegalArgumentException("A conditionnal path must be in the form ...start_path#conditional_path=value/endPath");
             } else {
                 pathID = pathID + conditionalValue.substring(nextSeparator);
                 conditionalValue = conditionalValue.substring(0, nextSeparator);
             }
         } else {
             pathID = xPath;
         }

         final String propertyName;
         int ordinal = -1;
         if (pathID.endsWith("]") && pathID.indexOf('[') != -1) {
             try {
                 ordinal = Integer.parseInt(pathID.substring(pathID.lastIndexOf('[') + 1, pathID.length() - 1));
             } catch (NumberFormatException ex) {
                 LOGGER.warning("Unable to parse last path ordinal");
             }
             propertyName = pathID.substring(pathID.lastIndexOf('/') + 1, pathID.indexOf('['));
         } else {
             propertyName = pathID.substring(pathID.lastIndexOf('/') + 1, pathID.length());
         }
         final List<Node> nodes;
         if (conditionalPath == null) {
             nodes = getNodeFromPath(metadata, pathID, create);
         } else {
             nodes  = getNodeFromConditionalPath(pathID, conditionalPath, conditionalValue, metadata); // create?
         }
         return new NodeAndOrdinal(ordinal, nodes, propertyName);
    }

    public static class NodeAndOrdinal {
        public final int ordinal;
        public final List<Node> nodes;
        public final String propertyName;

        public NodeAndOrdinal(int ordinal, List<Node> nodes, String propertyName) {
            this.ordinal = ordinal;
            this.nodes = nodes;
            this.propertyName = propertyName;
        }
    }

    /**
     * Return a String value from the specified Object.
     * Let the number object as Number
     */
    private static List<Object> getStringValue(final List<Node> nodes, final int ordinal, Class type, boolean parseDate) {
        final List<Object> result = new ArrayList<>();
        if (nodes != null && !nodes.isEmpty()) {
            for (Node n : nodes) {
                final String s = n.getTextContent();
                final String typeName = n.getLocalName();

                // special case for Double List => todo find a better way
                if (typeName.endsWith("Corner")) {
                    if (ordinal != -1) {
                        final String[] parts = s.split(" ");
                        if (ordinal < parts.length) {
                            try {
                                result.add(Double.parseDouble(parts[ordinal]));
                            } catch (NumberFormatException ex) {
                                LOGGER.log(Level.WARNING, "Unable to parse the real value:{0}", s);
                            }
                        }
                    } else {
                        result.add(s);
                    }
                } else if (type.equals(String.class)) {
                    result.add(s);
                } else if (type.equals(Double.class)) {
                    try {
                        result.add(Double.parseDouble(s));
                    } catch (NumberFormatException ex) {
                        LOGGER.log(Level.WARNING, "Unable to parse the real value:{0}", s);
                    }
                } else if (type.equals(Integer.class)) {
                    try {
                        result.add(Integer.parseInt(s));
                    } catch (NumberFormatException ex) {
                        LOGGER.log(Level.WARNING, "Unable to parse the integer value:{0}", s);
                    }
                } else if (type.equals(Date.class)) {
                    try {
                        final Date d = Date.from(LenientDateFormat.parseInstantUTC(s));
                        if (parseDate) {
                            synchronized (Util.LUCENE_DATE_FORMAT) {
                                result.add(Util.LUCENE_DATE_FORMAT.format(d));
                            }
                        } else {
                            result.add(d);
                        }
                    } catch (DateTimeParseException ex) {
                        LOGGER.log(Level.WARNING, "Unable to parse the date value:{0}", s);
                    }
                } else if (s != null) {
                    result.add(s);
                }
            }
        }
        return result;
    }

    /**
     * Return {@code true} if the candidate Node match the specified QName.
     * '*' can be used in namespace/local part for any match.
     *
     * @param n The candidate Node.
     * @param localPart Expected local part or '*' for any match.
     * @param namespace Expected namespace or '*' for any match.
     *
     * @return true if the candidate node match the namespace/localPart specified
     */
    private static boolean matchRootName(final Node n, final String localPart, final String namespace) {
        return (localPart.equals(n.getLocalName()) || localPart.equals("*")) && (namespace == null || namespace.equals(n.getNamespaceURI()) || namespace.equals("*"));
    }

    /**
     * Update an object by calling the setter of the specified property with the specified value.
     *
     * @param nodes The parent object on witch call the setters.
     * @param propertyName The name of the property to update on the parent (can contain an ordinal).
     * @param propNmsp The namespace of the property to update on the parent (can contain an ordinal).
     * @param value The new value to update.
     *
     */
    public static void updateObjects(List<Node> nodes, String propertyName, String propNmsp, Node value) {

        Class parameterType = value.getClass();
        LOGGER.log(Level.FINER, "parameter type:{0}", parameterType);

        final int ordinal             = NodeUtilities.extractOrdinal(propertyName);
        if (propertyName.indexOf('[') != -1) {
            propertyName = propertyName.substring(0, propertyName.indexOf('['));
        }

        for (Node e : nodes) {
            final List<Node> toUpdate = NodeUtilities.getChilds(e, propertyName);

            // ADD
            if (toUpdate.isEmpty()) {
                final Node newNode = e.getOwnerDocument().createElementNS(propNmsp, propertyName);
                final Node clone   = e.getOwnerDocument().importNode(value, true);
                newNode.appendChild(clone);
                e.appendChild(newNode);

            // UPDATE
            } else {
                for (int i = 0; i < toUpdate.size(); i++) {
                    if (ordinal == -1 || i == ordinal) {
                        Node n = toUpdate.get(i);
                        final Node firstChild = getFirstChild(n, value instanceof Text);
                        if (firstChild != null) {
                            final Node clone = n.getOwnerDocument().importNode(value, true);
                            n.replaceChild(clone, firstChild);
                        // add new text child
                        } else if (value instanceof Text){
                            final Node clone = n.getOwnerDocument().importNode(value, true);
                            n.appendChild(clone);
                        }
                    }
                }
            }
        }
    }

    private static Node getFirstChild(final Node n, final boolean isText) {
        final NodeList nl = n.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            final Node child = nl.item(i);
            if (isText || (!(child instanceof Text) && !(child instanceof Comment))) {
                return child;
            }
        }
        return null;
    }

    public static Node getNodeFromObject(final Object metadata, final MarshallerPool pool) throws JAXBException, ParserConfigurationException  {

        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        secureFactory(dbf);//NOSONAR
        dbf.setNamespaceAware(true);
        final DocumentBuilder docBuilder = dbf.newDocumentBuilder();
        final Document document = docBuilder.newDocument();
        final Marshaller marshaller = pool.acquireMarshaller();
        final MarshallWarnings warnings = new MarshallWarnings();
        marshaller.setProperty(XML.CONVERTER, warnings);
        marshaller.setProperty(XML.TIMEZONE, TZ);
        Map<String, String> gmd = new HashMap<>();
        gmd.put("gmd", "http://www.isotc211.org/2005/");
        gmd.put("gmi", "http://www.isotc211.org/2005/");
        marshaller.setProperty(SCHEMAS, gmd);
//      marshaller.setProperty(LegacyNamespaces.APPLY_NAMESPACE_REPLACEMENTS, true);
        marshaller.setProperty(XML.GML_VERSION, LegacyNamespaces.VERSION_3_2_1);
        marshaller.marshal(metadata, document);
        pool.recycle(marshaller);
        return document.getDocumentElement();
    }

    public static Node getNodeFromString(final String string) throws ParserConfigurationException, SAXException, IOException  {
        final InputSource source = new InputSource(new StringReader(string));
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        secureFactory(dbf);//NOSONAR
        dbf.setNamespaceAware(true);
        final DocumentBuilder docBuilder = dbf.newDocumentBuilder();
        final Document document = docBuilder.parse(source);
        return document.getDocumentElement();
    }

    public static Object getMetadataFromNode(final Node metadataNode, final MarshallerPool pool) throws JAXBException {
        final Unmarshaller um = pool.acquireUnmarshaller();
        Object obj;
        try {
            // We would like to use um.unmarshal(metadataNode) directly, but it currently causes
            // UnsupportedOperationException: Cannot create XMLEventReader from a DOMSource.
            obj = um.unmarshal(new StringReader(getStringFromNode(metadataNode)));
        } catch (TransformerException e) {
            throw new JAXBException(e);
        }
        pool.recycle(um);
        if (obj instanceof JAXBElement) {
            obj = ((JAXBElement)obj).getValue();
        }
        return obj;
    }

    public static Node getNodeFromPath(final Path metadataFile) throws SAXException, IOException, ParserConfigurationException {
        try (InputStream stream = Files.newInputStream(metadataFile)) {
            return getNodeFromStream(stream);
        }
    }

    public static Node getNodeFromStream(final InputStream stream) throws ParserConfigurationException, SAXException, IOException {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        secureFactory(dbf);//NOSONAR
        final DocumentBuilder docBuilder = dbf.newDocumentBuilder();
        final Document document = docBuilder.parse(stream);
        return document.getDocumentElement();
    }

    public static Node getNodeFromReader(final Reader reader) throws ParserConfigurationException, SAXException, IOException {
        final InputSource source = new InputSource(reader);
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        secureFactory(dbf);//NOSONAR
        final DocumentBuilder docBuilder = dbf.newDocumentBuilder();
        final Document document = docBuilder.parse(source);
        return document.getDocumentElement();
    }

    public static Document getDocumentFromPath(Path metadataFile) throws ParserConfigurationException, SAXException, IOException {
        try (InputStream stream = Files.newInputStream(metadataFile)) {
            return getDocumentFromStream(stream);
        }
    }

    public static Document getDocumentFromStream(InputStream metadataStream) throws ParserConfigurationException, SAXException, IOException {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        secureFactory(dbf);//NOSONAR
        final DocumentBuilder docBuilder = dbf.newDocumentBuilder();
        return docBuilder.parse(metadataStream);
    }

    public static Document getDocumentFromString(String xml) throws ParserConfigurationException, SAXException, IOException {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        secureFactory(dbf);//NOSONAR
        final DocumentBuilder docBuilder = dbf.newDocumentBuilder();
        final InputSource source = new InputSource(new StringReader(xml));
        return docBuilder.parse(source);
    }

     /**
     * Convert geotk metadata string xml to w3c document.
     *
     * @param metadata the given metadata xml as string.
     * @param pool
     *
     * @return {@link Node} that represents the metadata in w3c document format.
     */
    public static Node getNodeFromGeotkMetadata(final Object metadata, final MarshallerPool pool) throws JAXBException, ParserConfigurationException {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        secureFactory(dbf);//NOSONAR
        final DocumentBuilder docBuilder = dbf.newDocumentBuilder();
        final Document document = docBuilder.newDocument();
        final Marshaller marshaller = pool.acquireMarshaller();
        final MarshallWarnings warnings = new MarshallWarnings();
        marshaller.setProperty(XML.CONVERTER, warnings);
        marshaller.setProperty(XML.TIMEZONE, TZ);
//      marshaller.setProperty(LegacyNamespaces.APPLY_NAMESPACE_REPLACEMENTS, true);
        marshaller.setProperty(XML.GML_VERSION, LegacyNamespaces.VERSION_3_2_1);
        marshaller.marshal(metadata, document);
        pool.recycle(marshaller);

        return document.getDocumentElement();
    }

    public static void writerNode(Node n, Writer writer) throws TransformerConfigurationException, TransformerException {
        TransformerFactory tf = TransformerFactory.newInstance();
        secureFactory(tf);//NOSONAR
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        StreamResult sr = new StreamResult(writer);
        transformer.transform(new DOMSource(n), sr);
    }

    public static String getStringFromNode(final Node n) throws TransformerException  {
        TransformerFactory tf = TransformerFactory.newInstance();
        secureFactory(tf);//NOSONAR
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(n), new StreamResult(writer));
        String output = writer.getBuffer().toString().replaceAll("\n|\r", "");
        return output;
    }

    public static void secureFactory(final DocumentBuilderFactory dbf) {
        try {
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (Exception ex) {
            LOGGER.finer(ex.getMessage());
        }
    }

    public static void secureFactory(final TransformerFactory dbf) {
        try {
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (Exception ex) {
            LOGGER.finer(ex.getMessage());
        }
    }
}
