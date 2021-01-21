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

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.constellation.admin.SpringHelper;
import org.constellation.business.IInternalMetadataBusiness;
import static org.constellation.metadata.CSWQueryable.ALL_PREFIX_MAPPING;
import org.geotoolkit.metadata.AbstractMetadataWriter;
import org.geotoolkit.metadata.MetadataIoException;
import org.constellation.metadata.utils.Utils;
import org.constellation.util.NodeUtilities;
import static org.constellation.util.NodeUtilities.extractNodes;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.NO_APPLICABLE_CODE;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import static org.constellation.util.NodeUtilities.getDocumentFromString;
import static org.constellation.util.NodeUtilities.updateObjects;
import org.xml.sax.SAXException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class InternalMetadataWriter extends AbstractMetadataWriter {

    @Autowired
    protected IInternalMetadataBusiness internalMetadataBusiness;

    protected final DocumentBuilderFactory dbf;

    protected final XMLInputFactory xif = XMLInputFactory.newFactory();

    public InternalMetadataWriter(final Map configuration) throws MetadataIoException {
        SpringHelper.injectDependencies(this);
        dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        try {
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (ParserConfigurationException ex) {
            throw new MetadataIoException(ex);
        }
    }

    @Override
    public boolean storeMetadata(Node original) throws MetadataIoException {
        final String identifier = Utils.findIdentifier(original);
        if (!internalMetadataBusiness.existMetadata(identifier)) {
            String xml = getStringFromNode(original);
            internalMetadataBusiness.storeMetadata(identifier, xml);
            return true;
        } else {
            return replaceMetadata(identifier, original);
        }
    }

    @Override
    public boolean deleteMetadata(String metadataID) throws MetadataIoException {
        return internalMetadataBusiness.deleteMetadata(metadataID);
    }

    @Override
    public boolean isAlreadyUsedIdentifier(String metadataID) throws MetadataIoException {
        return internalMetadataBusiness.existMetadata(metadataID);
    }

    @Override
    public boolean replaceMetadata(String metadataID, Node original) throws MetadataIoException {
        String xml = getStringFromNode(original);
        String newIdentifier = Utils.findIdentifierNode(original);
        internalMetadataBusiness.updateMetadata(metadataID, newIdentifier, xml);
        return true;
    }

    private String getStringFromNode(Node n) throws MetadataIoException {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            NodeUtilities.secureFactory(tf);//NOSONAR
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            StringWriter sw = new StringWriter();
            StreamResult sr = new StreamResult(sw);
            transformer.transform(new DOMSource(n),sr);
            return sw.toString();
        } catch (TransformerException ex) {
            throw new MetadataIoException("Unable to transform node.into XML string", ex, NO_APPLICABLE_CODE);
        }
    }

    @Override
    public boolean deleteSupported() {
        return true;
    }

    @Override
    public boolean updateSupported() {
        return true;
    }

    @Override
    public void destroy() {
        // do nothing
    }

    @Override
    public boolean updateMetadata(String metadataID, Map<String, Object> properties) throws MetadataIoException {
        String xml = internalMetadataBusiness.getMetadata(metadataID);
        if (xml != null) {

            final Document metadataDoc;
            try {
                metadataDoc = getDocumentFromString(xml);
            } catch (ParserConfigurationException | SAXException | IOException ex) {
                throw new MetadataIoException(ex);
            }

            for (Map.Entry<String, Object> property : properties.entrySet()) {
                String xpath = property.getKey();

                if (xpath.indexOf('/', 1) != -1) {
                    Node parent = metadataDoc.getDocumentElement();
                    NodeUtilities.NodeAndOrdinal nao = extractNodes(parent, xpath, true);

                    // we verify that the metadata to update has the same type that the Xpath type
                    if (nao == null) {
                        throw new MetadataIoException("The metadata :" + metadataID + " is not of the same type that the one describe in Xpath expression", INVALID_PARAMETER_VALUE);
                    }

                    // we update the metadata
                    final Node value = (Node) property.getValue();

                    String propertyName;
                    String propertyNmsp;
                    final int separatorIndex = nao.propertyName.indexOf(':');
                    if (separatorIndex != -1) {
                        String prefix = nao.propertyName.substring(0, separatorIndex);
                        propertyNmsp = ALL_PREFIX_MAPPING.get(prefix); // TODO the mapping should be provided by the parameters.
                        propertyName = nao.propertyName.substring(separatorIndex + 1);
                    } else {
                        propertyNmsp = null;
                        propertyName = nao.propertyName;
                    }

                    List<Node> nodes = new ArrayList<>();
                    for (Node n : nao.nodes) {
                        nodes.add(n.getParentNode());
                    }

                    updateObjects(nodes, propertyName, propertyNmsp, value);

                    // we finish by updating the metadata.
                    deleteMetadata(metadataID);
                    storeMetadata(metadataDoc.getDocumentElement());
                    return true;

                }
            }
        }
        return false;
    }

    @Override
    public boolean canImportInternalData() {
        return false;
    }
}
