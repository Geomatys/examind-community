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

import org.geotoolkit.metadata.MetadataIoException;
import org.constellation.ws.CstlServiceException;
import org.geotoolkit.csw.xml.GetRecordsRequest;
import org.geotoolkit.nio.IOUtilities;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.logging.Level;
import org.geotoolkit.csw.xml.FederatedSearchResultBase;
import org.geotoolkit.metadata.MetadataStore;

import static org.geotoolkit.ows.xml.OWSExceptionCode.NO_APPLICABLE_CODE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.OPERATION_NOT_SUPPORTED;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class FileSystemHarvester extends CatalogueHarvester {

    /**
     * Build a new catalogue harvester able to harvest a fileSystem.
     *
     * @param store The Writer allowing to store the metadata in the datasource.
     *
     */
    public FileSystemHarvester(MetadataStore store) {
        super(store);

    }

    @Override
    public int[] harvestCatalogue(String sourceURL) throws CstlServiceException {
        if (!store.writeSupported()) {
            throw new CstlServiceException("The Service can not write into the database",
                                          OPERATION_NOT_SUPPORTED, "Harvest");
        }
        try {
            final Path dataDirectory = IOUtilities.toPath(sourceURL);
            if (!Files.isDirectory(dataDirectory)) {
                throw new CstlServiceException("The supplied source is not a valid directory",
                                              INVALID_PARAMETER_VALUE, "sourceURL");
            }
            //we initialize the getRecords request
            final int nbRecordInserted = harvestDirectory(dataDirectory);
            // TODO
            final int nbRecordUpdated  = 0;

            final int[] result = new int [3];
            result[0]    = nbRecordInserted;
            result[1]    = nbRecordUpdated;
            result[2]    = 0;

            return result;
        } catch (IOException ex) {
            throw new CstlServiceException("The service can't open the connection to the source",
                                              INVALID_PARAMETER_VALUE, "sourceURL");
        }
    }


    /**
     * Harvest recursively a directy and its children.
     *
     * @param dataDirectory
     * @return
     * @throws CstlServiceException
     */
    private int harvestDirectory(Path dataDirectory) throws CstlServiceException {
        try {
            final Unmarshaller unmarshaller = marshallerPool.acquireUnmarshaller();
            HarvestVisitor visitor = new HarvestVisitor(unmarshaller);
            Files.walkFileTree(dataDirectory, visitor);
            marshallerPool.recycle(unmarshaller);

            return visitor.getNbRecordInserted();

        } catch (JAXBException | IOException ex) {
            throw new CstlServiceException(ex, NO_APPLICABLE_CODE);
        }
    }

    @Override
    protected InputStream getSingleMetadata(String sourceURL) throws CstlServiceException{
        try {
            return Files.newInputStream(IOUtilities.toPath(sourceURL));
        } catch (IOException ex) {
            throw new CstlServiceException(ex);
        }
    }

    @Override
    public List<FederatedSearchResultBase> transferGetRecordsRequest(GetRecordsRequest request, List<String> distributedServers, int startPosition, int maxRecords) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private class HarvestVisitor extends SimpleFileVisitor<Path> {

        private final Unmarshaller unmarshaller;
        private int nbRecordInserted;

        public HarvestVisitor(Unmarshaller unmarshaller) {
            this.unmarshaller = unmarshaller;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

            LOGGER.log(Level.INFO, "current file:{0}", file.toString());
            try (InputStream in = Files.newInputStream(file)) {
                Object harvested = unmarshaller.unmarshal(in);

                // if the file is storable
                if (harvested instanceof Node) {

                    //Temporary ugly patch TODO handle update in CSW
                    try {
                        if (store.storeMetadata((Node) harvested)) {
                            nbRecordInserted++;
                        } else {
                            LOGGER.log(Level.INFO, "The file:{0} has not been recorded", file.toString());
                        }
                    } catch (IllegalArgumentException ex) {
                        LOGGER.log(Level.WARNING, "Illegal argument while storing the file:" + file.toString(), ex);
                    } catch (MetadataIoException ex) {
                        throw new IOException(ex.getMessage(), ex);
                    }

                    // the file does not contain a storable metadata
                } else {
                    String type = "null";
                    if (harvested != null) {
                        type = harvested.getClass().getSimpleName();
                    }
                    throw new IOException("The file does not contain an expected metadata type: " + type);
                }
            } catch (JAXBException ex) {
                throw new IOException(ex.getMessage(), ex);
            }

            return FileVisitResult.CONTINUE;
        }

        public int getNbRecordInserted() {
            return nbRecordInserted;
        }
    }
}
