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
package org.constellation.metadata.harvest;

import java.io.IOException;
import org.apache.sis.xml.MarshallerPool;
import org.apache.sis.internal.xml.LegacyNamespaces;
import org.geotoolkit.metadata.MetadataIoException;
import org.constellation.ws.CstlServiceException;
import org.geotoolkit.csw.xml.CSWMarshallerPool;
import org.geotoolkit.csw.xml.GetRecordsRequest;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.storage.DataStoreException;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.metadata.utils.CSWUtils;
import org.geotoolkit.csw.xml.FederatedSearchResultBase;
import org.geotoolkit.metadata.MetadataStore;

import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.NO_APPLICABLE_CODE;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class CatalogueHarvester {

    /**
     * use for debugging purpose
     */
    protected static final Logger LOGGER = Logger.getLogger("org.constellation.metadata");

    /**
     * A Marshaller / unMarshaller pool to send request to another CSW services / to get object from harvested resource.
     */
    protected final MarshallerPool marshallerPool;

    /**
     * A writer for the database
     */
    protected final MetadataStore store;

    /**
     * Build a new Catalog harvester with the specified metadataWriter.
     *
     * @param store A writer to store metadata in the dataSource.
     */
    public CatalogueHarvester(final MetadataStore store) {
        this.marshallerPool = CSWMarshallerPool.getInstanceCswOnly();
        this.store = store;
    }

    /**
     * Harvest another CSW service by getting all this records and storing it into the database
     *
     * @param sourceURL The URL of the distant CSW service
     *
     * @return An array containing: the number of inserted records, the number of updated records and the number of deleted records.
     */
    public abstract int[] harvestCatalogue(final String sourceURL) throws ConstellationStoreException, CstlServiceException;

    /**
     * Transfer The request to all the servers specified in distributedServers.
     *
     * @return A list of records harvesed in distributed servers.
     */
    public abstract List<FederatedSearchResultBase> transferGetRecordsRequest(final GetRecordsRequest request, final List<String> distributedServers,
            final int startPosition, final int maxRecords);

    /**
     * Harvest a single record and storing it into the database
     *
     * @param sourceURL The URL of the resource.
     * @param resourceType The record schema of the document to harvest.
     *
     * @return An array containing: the number of inserted records, the number of updated records and the number of deleted records.
     */
    public int[] harvestSingle(final String sourceURL, final String resourceType) throws CstlServiceException, ConstellationStoreException {
        final int[] result = new int[3];
        result[0] = 0;
        result[1] = 0;
        result[2] = 0;

        try {
            final Unmarshaller unmarshaller = marshallerPool.acquireUnmarshaller();

            if (LegacyNamespaces.GMD.equals(resourceType) ||
                LegacyNamespaces.CSW.equals(resourceType) ||
               "http://www.isotc211.org/2005/gfc".equals(resourceType)) {

                try (final InputStream in      = getSingleMetadata(sourceURL)) {
                    final Object harvestedObj = unmarshaller.unmarshal(in);
                    marshallerPool.recycle(unmarshaller);

                    if (harvestedObj == null) {
                        throw new CstlServiceException("The resource can not be parsed.",
                                INVALID_PARAMETER_VALUE, "Source");
                    }
                    final Node harvested = CSWUtils.transformToNode(harvestedObj, marshallerPool);
                    LOGGER.log(Level.INFO, "Object Type of the harvested Resource: {0}", harvested.getClass().getName());

                    // ugly patch TODO handle update
                    try {
                        if (store.storeMetadata(harvested)) {
                            result[0] = 1;
                        }
                    } catch (IllegalArgumentException e) {
                        result[1] = 1;
                    }  catch (MetadataIoException ex) {
                        throw new CstlServiceException(ex, NO_APPLICABLE_CODE);
                    }
                }
            } else {
                throw new CstlServiceException("unexpected resourceType: " + resourceType, NO_APPLICABLE_CODE);
            }
        } catch (JAXBException | IOException ex) {
            throw new ConstellationStoreException(ex);
        }
        return result;
    }

    protected abstract InputStream getSingleMetadata(final String sourceURL) throws CstlServiceException;

    public void destroy() throws DataStoreException {
        if (store != null) {
            store.close();
        }
    }
}
