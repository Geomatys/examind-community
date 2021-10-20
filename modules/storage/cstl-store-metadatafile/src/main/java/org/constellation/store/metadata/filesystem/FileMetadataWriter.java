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
package org.constellation.store.metadata.filesystem;

import org.geotoolkit.metadata.AbstractMetadataWriter;
import org.geotoolkit.metadata.MetadataIoException;
import org.constellation.store.metadata.filesystem.sql.MetadataDatasource;
import org.constellation.store.metadata.filesystem.sql.Session;
import org.constellation.metadata.utils.Utils;
import org.constellation.util.NodeUtilities;
import org.geotoolkit.nio.IOUtilities;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import javax.xml.stream.XMLInputFactory;
import org.constellation.admin.SpringHelper;

import static java.nio.file.StandardOpenOption.*;
import javax.xml.XMLConstants;
import static org.constellation.metadata.CSWQueryable.ALL_PREFIX_MAPPING;

import static org.constellation.util.NodeUtilities.updateObjects;
import static org.constellation.util.NodeUtilities.extractNodes;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.NO_APPLICABLE_CODE;
import org.w3c.dom.Attr;

/**
 * A CSW Metadata Writer. This writer does not require a database.
 * The CSW records are stored XML file in a directory .
 *
 * @author Guilhem Legal (Geomatys)
 */
public class FileMetadataWriter extends AbstractMetadataWriter {

    /**
     * A directory in witch the metadata files are stored.
     */
    protected final Path dataDirectory;

    protected final DocumentBuilderFactory dbf;

    protected final XMLInputFactory xif = XMLInputFactory.newFactory();

    private final MetadataDatasource source;

    /**
     * Build a new File metadata writer, with the specified indexer.
     *
     * @param configuration An object containing all the dataSource informations (in this case the data directory).
     * @param dataDirectory
     * @param source
     *
     * @throws org.geotoolkit.metadata.MetadataIoException
     */
    public FileMetadataWriter(final Map configuration, final Path dataDirectory, final MetadataDatasource source) throws MetadataIoException {
        SpringHelper.injectDependencies(this);
        this.dataDirectory = dataDirectory;
        this.source = source;
        if (dataDirectory == null || !Files.isDirectory(dataDirectory)) {
            throw new MetadataIoException("Unable to find the data directory", NO_APPLICABLE_CODE);
        }
        dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        try {
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (ParserConfigurationException ex) {
            throw new MetadataIoException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean storeMetadata(final Node original) throws MetadataIoException {
        final String identifier = Utils.findIdentifier(original);
        try (Session session = source.createSession()) {

            String path = session.getPathForRecord(identifier);

            final Path f;

           /*
            * New record, stored in the root directory
            */
            if (path == null) {
                // for windows we avoid to create file with ':'
                if (System.getProperty("os.name", "").startsWith("Windows")) {
                    final String windowsIdentifier = identifier.replace(':', '-');
                    f = dataDirectory.resolve(windowsIdentifier + ".xml");
                } else {
                    f = dataDirectory.resolve(identifier + ".xml");
                }
            /*
            * Update record, stored in his original location
            */
            } else {
                f = IOUtilities.toPath(path);
            }

            TransformerFactory tf = TransformerFactory.newInstance();
            NodeUtilities.secureFactory(tf);//NOSONAR
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            try (Writer writer = Files.newBufferedWriter(f, Charset.forName("UTF-8"), CREATE, WRITE, TRUNCATE_EXISTING)) {
                StreamResult sr = new StreamResult(writer);
                transformer.transform(new DOMSource(original), sr);
            }

            if (path == null) {
                session.putRecord(identifier, f.toUri().toString());
            } else {
                session.updateRecord(identifier, f.toUri().toString());
            }

        } catch (SQLException| IOException | TransformerException ex) {
            throw new MetadataIoException("Unable to write the file.", ex, NO_APPLICABLE_CODE);
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean deleteSupported() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean updateSupported() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean deleteMetadata(final String metadataID) throws MetadataIoException {
        final Path metadataFile = getFileFromIdentifier(metadataID);
        if (Files.exists(metadataFile)) {
           boolean suceed = false;
           try{
               // see http://bugs.java.com/bugdatabase/view_bug.do?bug_id=4715154
                System.gc();
                suceed =  Files.deleteIfExists(metadataFile);
           } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "IO exception while deleting file: {0}", ex.getMessage());
           }
           if (suceed) {
                try (Session session = source.createSession()) {
                    session.removeRecord(metadataID);
                } catch (SQLException ex) {
                    throw new MetadataIoException("SQL Exception while reading path for record", ex, NO_APPLICABLE_CODE);
                }
           } else {
               LOGGER.warning("unable to delete the matadata file");
           }
           return suceed;
        } else {
            throw new MetadataIoException("The metadataFile : " + metadataID + ".xml is not present", INVALID_PARAMETER_VALUE);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean replaceMetadata(final String metadataID, final Node any) throws MetadataIoException {
        final boolean succeed = deleteMetadata(metadataID);
        if (!succeed) {
            return false;
        }
        return storeMetadata(any);
    }

    @Override
    public boolean isAlreadyUsedIdentifier(String metadataID) throws MetadataIoException {
        return getFileFromIdentifier(metadataID) != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean updateMetadata(final String metadataID, final Map<String , Object> properties) throws MetadataIoException {
        final Document metadataDoc = getDocumentFromFile(metadataID);
        for (Entry<String, Object> property : properties.entrySet()) {
            String xpath = property.getKey();

            if (xpath.indexOf('/', 1) != -1) {

                Node parent = metadataDoc.getDocumentElement();
                NodeUtilities.NodeAndOrdinal nao = extractNodes(parent, xpath, ALL_PREFIX_MAPPING, true);

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
                    if (n instanceof Attr) {
                        nodes.add(((Attr)n).getOwnerElement());
                    } else if (n != null && n.getParentNode() != null) {
                        nodes.add(n.getParentNode());
                    }
                }

                updateObjects(nodes, propertyName, propertyNmsp, value);

                // we finish by updating the metadata.
                deleteMetadata(metadataID);
                storeMetadata(metadataDoc.getDocumentElement());
                return true;
            }
        }
        return false;
    }

    private Document getDocumentFromFile(String identifier) throws MetadataIoException {
        final Path metadataFile = getFileFromIdentifier(identifier);
        if (Files.exists(metadataFile)) {
            try {
                return NodeUtilities.getDocumentFromPath(metadataFile);
            } catch (SAXException | IOException | ParserConfigurationException ex) {
                throw new MetadataIoException("The metadataFile : " + identifier + ".xml can not be read\ncause: " + ex.getMessage(), ex, INVALID_PARAMETER_VALUE);
            }
        } else {
            throw new MetadataIoException("The metadataFile : " + identifier + ".xml is not present", INVALID_PARAMETER_VALUE);
        }
    }

    private Path getFileFromIdentifier(final String identifier) throws MetadataIoException {
        try (Session session = source.createSession()) {
            final String path = session.getPathForRecord(identifier);
            if (path != null) {
                return Paths.get(URI.create(path));
            } else {
                throw new MetadataIoException("Null path value for identifier:" + identifier, NO_APPLICABLE_CODE);
            }
        } catch (SQLException ex) {
            throw new MetadataIoException("SQL Exception while reading path for record", ex, NO_APPLICABLE_CODE);
        }
    }

    /**
     * Destoy all the resource and close connection.
     */
    @Override
    public void destroy() {
        // do nothing
    }

    @Override
    public boolean canImportInternalData() {
        return true;
    }
}
