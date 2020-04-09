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

package org.constellation.store.metadata.filesystem.sql;

import org.apache.sis.util.logging.Logging;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.util.collection.CloseableIterator;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.XMLConstants;
import org.geotoolkit.metadata.MetadataType;
import org.geotoolkit.metadata.RecordInfo;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class RecordIterator implements CloseableIterator<RecordInfo> {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.metadata.io.filesystem.sql");

    private final DocumentBuilder docBuilder;
    private final Session session;

    private final ResultSet rs;

    private RecordInfo current = null;

    public RecordIterator(final Session session) {
        this.session = session;
        try {
            session.setReadOnly(true);
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "error while setting connection to read only", ex);
        }
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);

        DocumentBuilder candidate = null;
        try {
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            candidate = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            LOGGER.log(Level.WARNING, "error while building XML DocumentBuilder", ex);
        }
        docBuilder = candidate;
        this.rs = session.getIterator();
    }

    @Override
    public boolean hasNext() {
        findNext();
        return current != null;
    }

    @Override
    public RecordInfo next() {
       findNext();
       final RecordInfo entry = current;
       current = null;
       return entry;
    }

    private void findNext(){
        if (current!=null) return;
        try {
            if (rs.next()) {
                final String path = rs.getString(2);
                final Path metadataFile = IOUtilities.toPath(path);
                current = new RecordInfo(rs.getString(2), getNodeFromFile(metadataFile), MetadataType.NATIVE, MetadataType.NATIVE);
            }
        } catch (IOException | SQLException e) {
            LOGGER.log(Level.WARNING, "error while iterating resultSet", e);
        }
    }

    private Node getNodeFromFile(final Path metadataFile) throws SQLException {
        try {
            final Document document = docBuilder.parse(Files.newInputStream(metadataFile));
            return document.getDocumentElement();
        } catch (SAXException | IOException ex) {
            throw new SQLException("The metadata file : " + metadataFile.getFileName() + " can not be read", null, ex);
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not supported in this iterator.");
    }

    @Override
    public void close() {
        try {
            rs.close();
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "error while closing resultSet", ex);
        }
        session.close();
    }
}
