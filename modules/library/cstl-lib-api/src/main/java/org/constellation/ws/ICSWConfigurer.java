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
package org.constellation.ws;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import org.constellation.dto.service.config.csw.BriefNode;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.TargetNotFoundException;
import org.w3c.dom.Node;

/**
 * OGC service utility class regrouping methods to configure/operate on a CSW service.
 *
 * @author Guilhem Legal (Geomatys)
 */
public interface ICSWConfigurer extends IOGCConfigurer {

    /**
     * Rebuild the index of a CSW service.
     * it can build a new index which will be used after the next restart of the service if the flag
     * "asynchrone" is set to {@code true}. Otherwise, the refresh will be immediate,
     * and the service will be unavailable during this process.
     * If the service is already re-indexing this methos will not launch another indexation process
     * unless the flag "forced" is set to {@code true}.
     *
     * @param id identifier of the CSW service.
     * @param asynchrone Prepare a new index for the next start of the service.
     * @param forced Force the re-indexation even if another one is already running.
     *
     * @return {@code true} if the indexation succeed.
     *
     * @throws ConfigurationException If an indexation id already running and the flag "forced" is set to false.
     * @throws TargetNotFoundException If the csw service does not exist.
     * @throws ConstellationException if a problem occurs during the indexation.
     */
    boolean refreshIndex(final String id, final boolean asynchrone, final boolean forced) throws ConstellationException;

    /**
     * Add some CSW record to the index.
     * These metadata must be already present in the examind datasource.
     *
     * @param id identifier of the CSW service.
     * @param identifierList list of metadata identifier to add into the index.
     *
     * @return {@code true} if the record has been added to the service.
     * @throws ConstellationException if a problem occurs during the indexation.
     */
    boolean addToIndex(final String id, final List<String> identifierList) throws ConstellationException;

    /**
     * Remove some CSW record to the index.
     *
     * @param id identifier of the CSW service.
     * @param identifierList list of metadata identifier to remove from the index.
     *
     * @return {@code true} if the record has been removed to the service.
     * @throws ConfigurationException if a problem occurs during the indexation.
     */
    boolean removeFromIndex(final String id, final List<String> identifierList) throws ConfigurationException;

    /**
     * Remove the index for the specified CSW service.
     *
     * @param id identifier of the CSW service.
     *
     * @return {@code true} if the index has been destroyed.
     * @throws ConfigurationException if a problem occurs during the removal.
     */
    boolean removeIndex(final String id) throws ConfigurationException;

    /**
     * Stop all the indexation going on.
     *
     * @param id identifier of the CSW service.
     * @return {@code true} if the indexation has been stopped or if there was no indexation going on.
     */
    boolean stopIndexation(final String id);

    /**
     * Import the specified record into the CSW service.
     *
     * @param id identifier of the CSW service.
     * @param metadataId identifier of the metadata.
     * 
     * @return {@code true} if the record has been imported into the service.
     *
     * @throws TargetNotFoundException If the csw service or the metadata does not exist.
     * @throws ConstellationException If something went wrong during the import.
     */
    boolean importRecord(final String id, final String metadataId) throws ConstellationException;

    /**
     * Import a list of records into the CSW service.
     *
     * @param id identifier of the CSW service.
     * @param metadataIds identifiers of the metadata.
     *
     * @return {@code true} if the records has been imported into the service.
     * 
     * @throws TargetNotFoundException If the csw service or the metadata does not exist.
     * @throws ConstellationException If something went wrong during the import.
     */
    boolean importRecords(final String id, final Collection<String> metadataIds) throws ConstellationException ;

    /**
     * Import the supplied record (Extracted from a file) into the CSW service.
     *
     * @param id identifier of the CSW service.
     * @param f File containing a metadata.
     * @param fileName name of the file.
     *
     * @return {@code true} if the record has been imported into the service.
     *
     * @throws TargetNotFoundException If the csw service does not exist.
     * @throws ConstellationException  If something went wrong during the import.
     */
    boolean importRecords(final String id, final Path f, final String fileName) throws ConstellationException ;

    /**
     * Import the supplied record (Extracted from a DOM node) into the CSW service.
     *
     * @param id identifier of the CSW service.
     * @param n DOM node containing a metadata.
     * 
     * @return {@code true} if the record has been imported into the service.
     *
     * @throws TargetNotFoundException If the csw service does not exist.
     * @throws ConstellationException  If something went wrong during the import.
     */
    boolean importRecord(final String id, final Node n) throws ConstellationException;

    /**
     * Remove the specified record from the CSW service.
     *
     * @param id identifier of the CSW service.
     * @param metadataId identifier of the metadata to remove.
     * 
     * @return {@code true} if the record has been removed from the service.
     * 
     * @throws TargetNotFoundException If the csw service or the metadata does not exist.
     * @throws ConstellationException If something went wrong during the removal.
     */
    boolean removeRecord(final String id, final String metadataId) throws ConstellationException;

    /**
     * Remove the specified records from the CSW service.
     *
     * @param id identifier of the CSW service.
     * @param metadataIds identifiers of the metadata to remove.
     *
     * @throws TargetNotFoundException If the csw service or the metadata does not exist.
     * @throws ConstellationException If something went wrong during the removal.
     */
    void removeRecords(final String id, final Collection<String> metadataIds) throws ConstellationException;

    /**
     * Remove all the records from the CSW service.
     *
     * @param id identifier of the CSW service.
     * 
     * @return  {@code true} if all the records has been removed from the service.
     *
     * @throws TargetNotFoundException If the csw service does not exist.
     * @throws ConstellationException If something went wrong during the removal.
     */
    boolean removeAllRecords(final String id) throws ConstellationException;

    /**
     * Return true if the specified metadata exist in the CSW service.
     *
     * @param id identifier of the CSW service.
     * @param metadataID identifier of the metadata.
     * 
     * @return {@code true} if the metadata exist.
     *
     * @throws TargetNotFoundException If the csw service does not exist.
     * @throws ConfigurationException If the CSW service is badly configured
     */
    boolean metadataExist(final String id, final String metadataID) throws ConfigurationException;

    List<BriefNode> getMetadataList(final String id, final int count, final int startIndex) throws ConfigurationException;

    List<Node> getFullMetadataList(final String id, final int count, final int startIndex, String type) throws ConfigurationException;

    Node getMetadata(final String id, final String metadataID) throws ConstellationException;

    /**
     * Return the metadata count in the CSW service.
     * 
     * @param id identifier of the CSW service.
     *
     * @return the number of metadata in the service.
     *
     * @throws TargetNotFoundException If the csw service does not exist.
     * @throws ConfigurationException If the CSW service is badly configured.
     */
    int getMetadataCount(final String id) throws ConfigurationException;

    /**
     * Return the difference datasource type implementation available on the server.
     * 
     * @return A list of datasource type names.
     */
    List<String> getAvailableCSWDataSourceType();
}
