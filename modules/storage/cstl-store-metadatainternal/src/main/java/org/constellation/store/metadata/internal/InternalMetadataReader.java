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

import org.constellation.admin.SpringHelper;
import org.constellation.metadata.io.DomMetadataReader;
import org.geotoolkit.metadata.ElementSetType;
import org.geotoolkit.metadata.MetadataIoException;
import org.geotoolkit.metadata.MetadataType;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.constellation.api.PathType;
import org.constellation.business.IInternalMetadataBusiness;

import static org.constellation.util.NodeUtilities.getNodeFromReader;
import org.geotoolkit.metadata.RecordInfo;
import static org.geotoolkit.ows.xml.OWSExceptionCode.OPERATION_NOT_SUPPORTED;
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
public class InternalMetadataReader extends DomMetadataReader {

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
    public RecordInfo getMetadata(String identifier, MetadataType mode, ElementSetType type, List<QName> elementName) throws MetadataIoException {
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

            final Node n = convertAndApplyElementSet(metadataMode, mode, type, elementName, metadataNode);
            return new RecordInfo(identifier, n, metadataMode, mode);
        }
        return null;
    }

    @Override
    public boolean existMetadata(final String identifier) throws MetadataIoException {
        return internalMetadataBusiness.existMetadata(identifier);
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
    public List<RecordInfo> getAllEntries() throws MetadataIoException {
        final List<RecordInfo> result = new ArrayList<>();
        final List<String> metadataIds = internalMetadataBusiness.getInternalMetadataIds();
        for (String metadataID : metadataIds) {
            result.add(getMetadata(metadataID, MetadataType.NATIVE));
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
        return Arrays.asList(MetadataType.ISO_19115,
                             MetadataType.DUBLINCORE_CSW202,
                             MetadataType.DUBLINCORE_CSW300,
                             MetadataType.EBRIM_250,
                             MetadataType.EBRIM_300,
                             MetadataType.ISO_19110,
                             MetadataType.DIF);
    }
}
