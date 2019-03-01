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

import org.apache.sis.internal.xml.LegacyNamespaces;
import org.constellation.store.metadata.CSWMetadataReader;
import org.constellation.metadata.io.DomMetadataReader;
import org.geotoolkit.metadata.ElementSetType;
import org.geotoolkit.metadata.MetadataIoException;
import org.geotoolkit.metadata.MetadataType;
import org.constellation.store.metadata.filesystem.sql.IdentifierIterator;
import org.constellation.store.metadata.filesystem.sql.MetadataDatasource;
import org.constellation.store.metadata.filesystem.sql.RecordIterator;
import org.constellation.store.metadata.filesystem.sql.Session;
import org.geotoolkit.csw.xml.DomainValues;
import org.geotoolkit.csw.xml.v202.DomainValuesType;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.util.collection.CloseableIterator;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import org.apache.sis.xml.Namespaces;
import org.constellation.api.PathType;

import static org.constellation.api.CommonConstants.XML_EXT;
import static org.constellation.metadata.CSWQueryable.DUBLIN_CORE_QUERYABLE;
import static org.constellation.util.NodeUtilities.getNodeFromPath;
import static org.geotoolkit.metadata.TypeNames.METADATA_QNAME;
import org.geotoolkit.metadata.RecordInfo;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.NO_APPLICABLE_CODE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.OPERATION_NOT_SUPPORTED;
import org.xml.sax.SAXException;


/**
 * A CSW Metadata Reader. This reader does not require a database.
 * The CSW records are stored XML file in a directory .
 *
 * This reader can be used for test purpose or in case of small amount of record.
 *
 * @author Guilhem Legal (Geomatys)
 */
public class FileMetadataReader extends DomMetadataReader implements CSWMetadataReader {

    /**
     * The directory containing the data XML files.
     */
    private final Path dataDirectory;

    private final MetadataDatasource source;

    /**
     * Build a new CSW File Reader.
     *
     * @param configuration A generic configuration object containing a directory path
     * in the configuration.dataDirectory field.
     * @param additionalQueryable
     * @param dataDirectory
     * @param source
     *
     * @throws MetadataIoException If the configuration object does
     * not contains an existing directory path in the configuration.dataDirectory field.
     * If the creation of a MarshallerPool throw a JAXBException.
     */
    public FileMetadataReader(final Map configuration, final Map<String, PathType> additionalQueryable, final Path dataDirectory, final MetadataDatasource source) throws MetadataIoException {
        super(true, false, additionalQueryable);
        this.dataDirectory = dataDirectory;
        this.source = source;
        if (dataDirectory == null) {
            throw new MetadataIoException("cause: unable to find the data directory", NO_APPLICABLE_CODE);
        } else if (!Files.exists(dataDirectory)) {
            try {
                Files.createDirectories(dataDirectory);
            } catch (IOException e) {
                throw new MetadataIoException("cause: unable to create the data directory", NO_APPLICABLE_CODE);
            }
        }
        if (configuration.get("enable-thread") != null) {
            final boolean t = Boolean.parseBoolean((String) configuration.get("enable-thread"));
            if (t) {
                LOGGER.info("parrallele treatment enabled");
            }
            setIsThreadEnabled(t);
        }
        if (configuration.get("enable-cache") != null) {
            final boolean c = Boolean.parseBoolean((String) configuration.get("enable-cache"));
            if (!c) {
                LOGGER.info("cache system have been disabled");
            }
            setIsCacheEnabled(c);
        }
        analyzeFileSystem(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RecordInfo getMetadata(final String identifier, final MetadataType mode) throws MetadataIoException {
        return getMetadata(identifier, mode, ElementSetType.FULL, new ArrayList<>());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RecordInfo getMetadata(String identifier, MetadataType mode, ElementSetType type, List<QName> elementName) throws MetadataIoException {

        final Path metadataFile = getFileFromIdentifier(identifier);
        if (metadataFile != null) {
            final MetadataType metadataMode;
            try (InputStream in = Files.newInputStream(metadataFile)) {
                metadataMode = getMetadataType(in, false);
            } catch (IOException | XMLStreamException ex) {
                throw new MetadataIoException(ex);
            }
            final Node metadataNode;
            try {
                metadataNode = getNodeFromPath(metadataFile);
            } catch (SAXException | IOException | ParserConfigurationException ex) {
                throw new MetadataIoException("Error while reading file: " + metadataFile.getFileName(), ex, null);
            }

            final Node n;

            // ISO TO CSW2
            if (metadataMode ==  MetadataType.ISO_19115 && mode == MetadataType.DUBLINCORE_CSW202) {
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
            return new RecordInfo(identifier, n, metadataMode, mode);
        }
        return null;
    }

    @Override
    public boolean existMetadata(final String identifier) throws MetadataIoException {
        try (Session session = source.createSession()) {
            return session.existRecord(identifier);
        } catch (SQLException ex) {
            throw new MetadataIoException("SQL Exception while analyzing the file system", ex, NO_APPLICABLE_CODE);
        }
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

    /**
     * {@inheritDoc}
     */
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
        //do nothing
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
    public List<RecordInfo> getAllEntries() throws MetadataIoException {
        final List<RecordInfo> results = new ArrayList<>();
        try (CloseableIterator<String> iterator = getIdentifierIterator()) {
            while (iterator.hasNext()) {
                results.add(getMetadata(iterator.next(), MetadataType.NATIVE));
            }
        }
        return results;
    }

    @Override
    public CloseableIterator<String> getIdentifierIterator() throws MetadataIoException {
        try {
            final Session session = source.createSession();
            return new IdentifierIterator(session);
        } catch (SQLException ex) {
            throw new MetadataIoException("SQL Exception while building identifier iterator", ex, NO_APPLICABLE_CODE);
        }
    }

    @Override
    public CloseableIterator<RecordInfo> getEntryIterator() throws MetadataIoException {
        try {
            final Session session = source.createSession();
            return new RecordIterator(session);
        } catch (SQLException ex) {
            throw new MetadataIoException("SQL Exception while building identifier iterator", ex, NO_APPLICABLE_CODE);
        }
    }

    @Override
    public boolean useEntryIterator() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getAllIdentifiers() throws MetadataIoException {
        final List<String> results = new ArrayList<>();
        try (Session session = source.createSession()) {
            session.setReadOnly(true);
            results.addAll(session.getRecordList());
        } catch (SQLException ex) {
            throw new MetadataIoException("SQL Exception while analyzing the file system", ex, NO_APPLICABLE_CODE);
        }
        return results;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<MetadataType> getSupportedDataTypes() {
        return Arrays.asList(MetadataType.ISO_19115,
                             MetadataType.DUBLINCORE_CSW202,
                             MetadataType.DUBLINCORE_CSW300,
                             MetadataType.EBRIM_250,
                             MetadataType.EBRIM_300,
                             MetadataType.ISO_19110,
                             MetadataType.DIF);
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

    public final void analyzeFileSystem(final boolean force) throws MetadataIoException {
        if (dataDirectory != null) {
            try (Session session = source.createSession()) {
                if (force || session.needAnalyze()) {
                    LOGGER.info("Launching file system analyze");
                    session.setAutoCommit(false);
                    session.clear();
                    final long start = System.currentTimeMillis();
                    analyzeFileSystem(dataDirectory, session);
                    session.commit();
                    LOGGER.log(Level.INFO, "fileSystem analyze done in :{0} ms", (System.currentTimeMillis() - start));
                }
            } catch (SQLException ex) {
                throw new MetadataIoException("SQL Exception while analyzing the file system", ex, NO_APPLICABLE_CODE);
            }
        }
    }

    private void analyzeFileSystem(final Path directory, final Session session) throws MetadataIoException {
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    final String fileName = file.getFileName().toString();
                    if (fileName.endsWith(XML_EXT)) {
                        try (InputStream is = Files.newInputStream(file)) {
                            final String identifier = getMetadataIdentifier(is);
                            if (identifier != null) {
                                if (!session.existRecord(identifier)) {
                                    session.putRecord(identifier, file.toString());
                                } else {
                                    LOGGER.warning("File: " + fileName + " excluded, cause: identifier already used");
                                }
                            } else {
                                LOGGER.warning("File: " + fileName + " excluded, cause: unable to extract an identifier");
                            }
                        } catch (SQLException | XMLStreamException ex) {
                            throw new IOException(ex);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

            });

        } catch (IOException  e) {
            LOGGER.log(Level.WARNING, "Error while walking through file system", e);
        }
    }

    private Path getFileFromIdentifier(final String identifier) throws MetadataIoException {
        try (Session session = source.createSession()) {
            final String path = session.getPathForRecord(identifier);
            return IOUtilities.toPath(path);
        } catch (SQLException ex) {
            throw new MetadataIoException("SQL Exception while reading path for record", ex, NO_APPLICABLE_CODE);
        } catch (IOException ex) {
            throw new MetadataIoException("IO Exception while analyzing the file system", ex, NO_APPLICABLE_CODE);
        }
    }

    @Override
    public int getEntryCount() throws MetadataIoException {
        try (Session session = source.createSession()) {
            return session.getCount();

        } catch (SQLException ex) {
            throw new MetadataIoException("SQL Exception while getting records count", ex, NO_APPLICABLE_CODE);
        }
    }

    protected String getMetadataIdentifier(final InputStream metadataStream) throws IOException, XMLStreamException {
        final List<String> identifierPaths = DUBLIN_CORE_QUERYABLE.get("identifier").paths;
        final List<String[]> paths = new ArrayList<>();
        for (String identifierPath : identifierPaths) {
            identifierPath = identifierPath.substring(1); // remove the first '/'
            final String[] path = identifierPath.split("/");
            for (int i = 0; i < path.length; i++) {
                int sep = path[i].indexOf(':');
                if (sep != -1) {
                    path[i] = path[i].substring(sep + 1);
                }
            }
            paths.add(path);
        }
        XMLStreamReader xsr = null;
        try {
            xsr = xif.createXMLStreamReader(metadataStream);
            int i = 0;
            while (xsr.hasNext()) {
                xsr.next();
                if (xsr.isStartElement()) {
                    String nodeName = xsr.getLocalName();
                    final List<String[]> toRemove = new ArrayList<>();
                    for (String [] path : paths) {
                        String currentName = path[i];
                        if (i == path.length -2 && path[i + 1].startsWith("@")) {
                            final String value = xsr.getAttributeValue(null, path[i + 1].substring(1));
                            if (value != null) {
                                return value;
                            } else {
                                toRemove.add(path);
                            }
                        } else if (!currentName.equals("*") && !currentName.equals(nodeName)) {
                            toRemove.add(path);
                        } else if (i  == path.length -1) {
                            return xsr.getElementText();
                        }
                    }
                    paths.removeAll(toRemove);
                    i++;
                }
            }
        } finally {
            if (xsr != null) {
                xsr.close();
            }
            metadataStream.close();
        }
        return null;
    }
}
