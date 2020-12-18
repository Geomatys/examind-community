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
package org.constellation.business;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.constellation.dto.metadata.GroupStatBrief;
import org.constellation.dto.metadata.OwnerStatBrief;
import org.constellation.dto.metadata.User;
import org.constellation.exception.ConfigurationException;
import org.constellation.dto.metadata.MetadataLists;
import org.constellation.dto.metadata.MetadataWithState;
import org.constellation.dto.metadata.Attachment;
import org.constellation.dto.metadata.MetadataBrief;
import org.constellation.dto.metadata.MetadataLightBrief;
import org.constellation.dto.metadata.RootObj;
import org.constellation.exception.ConstellationException;
import org.w3c.dom.Node;

/**
 * @author Cédric Briançon (Geomatys)
 */
public interface IMetadataBusiness {

    /**
     * Returns the metadata Pojo for given metadata identifier.
     *
     * @param metadataId given metadata identifier
     * @param includeService flag that indicates if service repository will be requested.
     * @param onlyPublished flag that indicates if it will return the unpublished metadata.
     * @param providerID if set, look for only the metadata associated with tha specified provider. Otherwise it search on all metadata.
     * @return String representation of metadata in xml.
     */
    MetadataBrief searchFullMetadata(final String metadataId, final boolean includeService, final boolean onlyPublished, final Integer providerID);

    /**
     * Returns {@code true} if the xml metadata exists for given metadata identifier.
     *
     * @param metadataID given metadata identifier.
     * @param includeService flag that indicates if service repository will be requested.
     * @param onlyPublished flag that indicates if it will return the unpublished metadata.
     * @param providerID
     * @return boolean to indicates if metadata is present or not.
     */
    boolean existInternalMetadata(final String metadataID, final boolean includeService, final boolean onlyPublished, final Integer providerID);

    /**
     * Returns {@code true} if the xml metadata exists for given title.
     *
     * @param title given metadata Title.
     *
     * @return boolean to indicates if metadata with the specified title is present or not.
     */
    boolean existMetadataTitle(final String title);

    /**
     * Returns a cout of all metadata wuth the specified filters.
     *
     * @param includeService flag that indicates if service repository will be requested.
     * @param onlyPublished flag that indicates if it will return the unpublished metadata.
     * @param providerID if set, look for only the metadata associated with the specified provider. Otherwise it search on all metadata.
     * @param type if set, look for only the metadata with the specified type. Otherwise it search on all metadata types.
     *
     * @return List of string identifiers.
     */
    List<String> getMetadataIds(final boolean includeService, final boolean onlyPublished, final Integer providerID, final String type);

    /**
     * Returns a list of all metadata identifiers.
     *
     * @param includeService flag that indicates if service repository will be requested.
     * @param onlyPublished flag that indicates if it will return the unpublished metadata.
     * @param providerID if set, look for only the metadata associated with the specified provider. Otherwise it search on all metadata.
     * @param type if set, look for only the metadata with the specified type. Otherwise it search on all metadata types.
     *
     * @return List of string identifiers.
     */
    int getMetadataCount(final boolean includeService, final boolean onlyPublished, final Integer providerID, final String type);

    /**
     * Returns all metadata for the specified provider ID.
     *
     * @param providerID
     * @param type if set, look for only the metadata with the specified type. Otherwise it search on all metadata types.
     * @return List of all metadata as string xml he specified provider.
     */
    List<MetadataBrief> getByProviderId(final int providerID, final String type);

    /**
     * Update or create a new Metadata pojo.
     *
     * @param metadataId identifier of the metadata.
     * @param metadataObj Node or geotk representation of the metadata.
     * @param dataID Identifier of the linked data (can be {@code null}).
     * @param datasetID Identifier of the linked dataset (can be {@code null}).
     * @param mapcontextID Identifier of the linked map-context (can be {@code null}).
     * @param owner User who owes the metadata.
     * @param providerId identifier of the metadata provider.
     * @param type The type of the document (DOC, MODEL, CONTACT) (can be {@code null} will be assumed at DOC for a new metadata or at it previous type).
     *
     * @return The created/update Metadata pojo.
     * @throws org.constellation.exception.ConfigurationException
     */
    MetadataLightBrief updateMetadata(final String metadataId, final Object metadataObj, final Integer dataID, final Integer datasetID, final Integer mapcontextID, final Integer owner, final Integer providerId, String type) throws ConstellationException;

    /**
     * Update or create a new Metadata pojo.
     *
     * @param metadataId identifier of the metadata.
     * @param metadataObj Node or geotk representation of the metadata.
     * @param dataID Identifier of the linked data (can be {@code null}).
     * @param datasetID Identifier of the linked dataset (can be {@code null}).
     * @param mapcontextID Identifier of the linked map-context (can be {@code null}).
     * @param owner User who owes the metadata.
     * @param providerId identifier of the metadata provider.
     * @param type The type of the document (DOC, MODEL, CONTACT) (can be {@code null} will be assumed at DOC for a new metadata or at it previous type).
     * @param templateName force the metadata profile (can be {@code null}).
     *
     * @return The created/update Metadata pojo.
     * @throws org.constellation.exception.ConfigurationException
     */
    MetadataLightBrief updateMetadata(final String metadataId, final Object metadataObj, final Integer dataID, final Integer datasetID, final Integer mapcontextID, final Integer owner, final Integer providerId, String type, String templateName, boolean hidden) throws ConstellationException;

    boolean updatePartialMetadata(final String metadataId, Map<String, Object> properties, final Integer providerId) throws ConstellationException;

    /**
     * Returns all the metadata identifier associated with a csw service.
     *
     * @param cswIdentifier identifer of the CSW instance.
     * @param partial if {@code true} return only the metadata linked to the service, else all the metadata provider linked ones.
     * @param includeService given flag to include service's metadata
     * @param onlyPublished flag that indicates if it will return the unpublished metadata.
     * @param type The type of the document (DOC, MODEL, CONTACT) (can be {@code null} will be assumed at DOC for a new metadata or at it previous type).
     *
     * @return List of all metadata identifiers stored in database.
     */
    List<String> getLinkedMetadataIDs(final String cswIdentifier, final boolean partial, final boolean includeService, final boolean onlyPublished, final String type);

    /**
     * Return the number of metadata linked to this CSW.
     *
     * @param cswIdentifier identifer of the CSW instance.
     * @param partial if {@code true} return only the metadata linked to the service, else all the metadata provider linked ones.
     * @param includeService given flag to include service's metadata
     * @param onlyPublished flag that indicates if it will return the unpublished metadata.
     * @param type The type of the document (DOC, MODEL, CONTACT) (can be {@code null} will be assumed at DOC for a new metadata or at it previous type).
     *
     * @return The number of metadata linked to this CSW.
     */
    int getLinkedMetadataCount(final String cswIdentifier, final boolean partial, final boolean includeService, final boolean onlyPublished, final String type);

    /**
     * Build a link beetween a CSW service and a metadata.
     *
     * @param metadataId Identifier of the geotk metadata object.
     * @param cswIdentifier identifer of the CSW instance.
     * @throws org.constellation.exception.ConfigurationException
     */
    void linkMetadataIDToCSW(final String metadataId, final String cswIdentifier) throws ConfigurationException;

    /**
     * Remove the link beetween a CSW service and a metadata.
     *
     * @param metadataId Identifier of the geotk metadata object.
     * @param cswIdentifier identifer of the CSW instance.
     */
    void unlinkMetadataIDToCSW(final String metadataId, final String cswIdentifier);

    /**
     * Return {@code true} if the specified metadata is linked to the specified CSW service.
     * @param metadataID Identifier of the metadata pojo.
     * @param cswID identifer of the CSW instance.
     *
     * @return {@code true} if the specified metadata is linked to the specified CSW service.
     */
    boolean isLinkedMetadataToCSW(final int metadataID, final int cswID);

    /**
     * Return {@code true} if the specified metadata is linked to the specified CSW service.
     * @param metadataID Identifier of the metadata pojo.
     * @param cswID identifer of the CSW instance.
     * @param partial CSW integrate partially the metadata of the provider (each metadata has to be linked explicitly to the CSW).
     * @param includeService given flag to include service's metadata
     * @param onlyPublished flag that indicates if it will return the unpublished metadata.
     *
     * @return {@code true} if the specified metadata is linked to the specified CSW service.
     */
    boolean isLinkedMetadataToCSW(final String metadataID, final String cswID, final boolean partial, final boolean includeService, final boolean onlyPublished);

    /**
     * Return {@code true} if the specified metadata is linked to the specified CSW service.
     * @param metadataID Identifier of the geotk metadata object.
     * @param cswID  identifer of the CSW instance.
     *
     * @return {@code true} if the specified metadata is linked to the specified CSW service.
     */
    boolean isLinkedMetadataToCSW(final String metadataID, final String cswID);

    MetadataLists getMetadataCodeLists();

    /**
     * Return the geotk metadata object the specified pojo identifier.
     *
     * @param id identifier of the metadata pojo.
     *
     * @return The geotk metadata object or {@code null} .
     * @throws org.constellation.exception.ConfigurationException
     */
    Object getMetadata(final int id) throws ConfigurationException;

    /**
     * Return the geotk metadata object the specified pojo identifier.
     *
     * @param metadataId identifier of the metadata object.
     *
     * @return The geotk metadata object or {@code null} .
     * @throws org.constellation.exception.ConfigurationException
     */
    Object getMetadata(final String metadataId) throws ConfigurationException;

    MetadataLightBrief getMetadataPojo(final String metadataId) throws ConfigurationException;

    MetadataBrief getMetadataPojo(final int metadataId) throws ConfigurationException;

    /**
     * Return the metadata node the specified pojo identifier.
     *
     * @param metadataId identifier of the metadata object.
     *
     * @return The geotk metadata object or {@code null} .
     * @throws org.constellation.exception.ConfigurationException
     */
    Node getMetadataNode(final String metadataId) throws ConfigurationException;

    /**
     * Return the geotk metadata object the specified pojo identifier.
     *
     * @param id identifier of the metadata pojo.
     *
     * @return The geotk metadata object or {@code null} .
     * @throws org.constellation.exception.ConfigurationException
     */
    String getMetadataXml(final int id) throws ConfigurationException;

    /**
     * Return the metadat pojo for the specified identifier.
     *
     * @param id identifier of the metadata pojo.
     *
     * @return The metadat pojo or {@code null}.
     */
    MetadataBrief getMetadataById(final int id);

    /**
     * Update the publication flag of a metadata.
     *
     * @param id identifier of the metadata pojo.
     * @param newStatus new publication status to set.
     *
     * @throws org.constellation.exception.ConfigurationException
     */
    void updatePublication(final int id, final boolean newStatus) throws ConstellationException;

    /**
     * Update the publication flag for a list of metadata pojo.
     *
     * @param ids List of metadata pojo identifier.
     * @param newStatus new publication status to set.
     *
     * @throws org.constellation.exception.ConfigurationException
     */
    void updatePublication(final List<Integer> ids, final boolean newStatus) throws ConstellationException;

    /**
     * Update the hidden flag of a metadata.
     *
     * @param id identifier of the metadata pojo.
     * @param newStatus new hidden status to set.
     *
     * @throws org.constellation.exception.ConfigurationException
     */
    void updateHidden(final int id, final boolean newStatus) throws ConstellationException;

    /**
     * Update the hidden flag for a list of metadata pojo.
     *
     * @param ids List of metadata pojo identifier.
     * @param newStatus new hidden status to set.
     *
     * @throws org.constellation.exception.ConfigurationException
     */
    void updateHidden(final List<Integer> ids, final boolean newStatus) throws ConstellationException;

    /**
     * Update the profile for a metadata pojo.
     *
     * @param id metadata pojo identifier.
     * @param newProfile new profile to set.
     *
     * @throws org.constellation.exception.ConfigurationException
     */
    void updateProfile(final Integer id, final String newProfile) throws ConfigurationException;

    /**
     * Update the validation flag of a metadata.
     *
     * @param id identifier of the metadata pojo.
     * @param newStatus new validation status to set.
     *
     */
    void updateValidation(final int id, final boolean newStatus);

    /**
     * Update the owner of a metadata.
     *
     * @param id identifier of the metadata pojo.
     * @param newOwner new owner identifier to set.
     *
     */
    void updateOwner(final int id, final int newOwner);

    /**
     * Update the owner for a list of metadata.
     *
     * @param ids list of metadata identifiers to change.
     * @param newOwner new owner identifier to set.
     *
     */
    void updateOwner(final List<Integer> ids, final int newOwner);

    /**
     * Delete a metadata pojo.
     *
     * @param id identifier of the metadata pojo.
     * @throws org.constellation.exception.ConfigurationException
     */
    void deleteMetadata(final int id) throws ConstellationException;

    /**
     * Delete a metadata pojo.
     *
     * @param metadataID identifier of the metadata Object.
     * @return {@code true} if the operation succeed.
     *
     * @throws org.constellation.exception.ConfigurationException
     */
    boolean deleteMetadata(final String metadataID) throws ConstellationException;

    /**
     * Delete the linked metadata pojo for the specified data.
     *
     * @param dataId identifier of the data pojo.
     * @throws org.constellation.exception.ConfigurationException
     */
    void deleteDataMetadata(final int dataId) throws ConstellationException;

    /**
     * Delete the linked metadata pojo for the specified dataszt.
     *
     * @param datasetId identifier of the dataset pojo.
     * @throws org.constellation.exception.ConfigurationException
     */
    void deleteDatasetMetadata(final int datasetId) throws ConstellationException;

    /**
     * Delete the linked metadata pojo for the specified map context.
     *
     * @param mapContextId identifier of the mapContext pojo.
     * @throws org.constellation.exception.ConfigurationException
     */
    void deleteMapContextMetadata(final int mapContextId) throws ConstellationException;

    /**
     * Delete a list of metadata pojo.
     *
     * @param ids List of metadata pojo identifiers.
     * @throws org.constellation.exception.ConfigurationException
     */
    void deleteMetadata(final List<Integer> ids) throws ConstellationException;

    /**
     * Delete all metadata in database
     * @throws ConfigurationException
     */
    void deleteAllMetadata() throws ConstellationException;

    /**
     * Return a percentage of the metadata completion (related to the profile linked to the metadata pojo).
     * The metadata pojo is retrieve from the linked specified dataset.
     *
     * @param datasetId identifier of the dataset.
     *
     * @return an integer representing the percentage of completion or {@code null} if the dataset has no linked metadata.
     */
    Integer getCompletionForDataset(final int datasetId);

    /**
     * Return the geotk metadata object linked with the specified data.
     *
     * @param dataId identifier of the data.
     *
     * @return The geotk metadata object or {@code null} if there is no metadata linked to the specified data.
     * @throws org.constellation.exception.ConfigurationException
     *
     * @deprecated A data could have multiple metadatas.
     */
    @Deprecated
    Object getIsoMetadataForData(final int dataId) throws ConfigurationException;

    /**
     * Return a list of geotk metadata object linked with the specified data.
     *
     * @param dataId identifier of the data.
     *
     * @return The geotk metadata object or {@code null} if there is no metadata linked to the specified data.
     * @throws org.constellation.exception.ConfigurationException
     */
    List<Object> getIsoMetadatasForData(final int dataId) throws ConfigurationException;

    /**
     * Return the geotk metadata object linked with the specified service.
     *
     * @param serviceId identifier of the service.
     *
     * @return The geotk metadata object or {@code null} if there is no metadata linked to the specified data.
     * @throws org.constellation.exception.ConfigurationException
     */
    Object getIsoMetadataForService(final int serviceId) throws ConfigurationException;

    /**
     * Return the geotk metadata object linked with the specified dataset.
     *
     * @param datasetId identifier of the dataset.
     *
     * @return The geotk metadata object or {@code null} if there is no metadata linked to the specified dataset.
     * @throws org.constellation.exception.ConfigurationException
     */
    Object getIsoMetadataForDataset(final int datasetId) throws ConfigurationException;

    /**
     * Update the CSW services index linked with the specified metadata pojos.
     *
     * @param metadatas List of metadata pojos.
     * @param update If {@code false} indicates that the metadata must be removed from the indexes.
     *
     * @throws org.constellation.exception.ConfigurationException
     */
    void updateCSWIndex(final List<MetadataWithState> metadatas, final boolean update) throws ConstellationException;

    /**
     * Duplicate a metadata pojo. Update the fileIdentifier and title of the geotk metadata.
     * if (the specified newTitle is null, the new title of the metadata will be "old title" + "(1)".
     *
     * @param id Identifier of the metadata pojo.
     * @param newTitle The new title to apply to the metadata object (can be {@code null}).
     * @param newType The new title to apply to the metadata object (can be {@code null} it will keep the type od the original metadata).
     *
     * @return the new pojo created.
     * @throws org.constellation.exception.ConfigurationException
     */
    MetadataLightBrief duplicateMetadata(final int id, final String newTitle, final String newType) throws ConfigurationException;

    /**
     * Count the number of metadata stored in the database.
     *
     * @param filterMap Filters which is optional.
     *
     * @return The total count of metadata.
     */
    int countTotal(final Map<String,Object> filterMap);

    int[] countInCompletionRange(final Map<String,Object> filterMap);

    /**
     * Count the number of metadata stored in the database whith the specified publication flag.
     *
     * @param status Publication flag value.
     * @param filterMap Filters which is optional.
     *
     * @return The total count of metadata with the specified publication flag.
     */
    int countPublished(final boolean status,final Map<String,Object> filterMap);

    /**
     * Returns map of distribution of used profiles.
     * @param filterMap optional filters
     *
     * @return A map of profile name / number of documents
     */
    Map<String,Integer> getProfilesCount(final Map<String,Object> filterMap);

    /**
     * Returns map of distribution of used profiles.
     *
     * @param filterMap optional filters
     * @param dataType The profile data type.
     *
     * @return A map of profile name / number of documents
     * @throws org.constellation.exception.ConfigurationException
     */
    Map<String,Integer> getProfilesCount(final Map<String,Object> filterMap, String dataType) throws ConfigurationException;

    /**
     * Return all profiles.
     * @return List of string profile names
     */
    List<String> getAllProfiles();

    /**
     * Return all profiles matching the specified data type.
     *
     * @param dataType The profile data type.
     *
     * @return List of string profile names
     * @throws org.constellation.exception.ConfigurationException
     */
    List<String> getProfilesMatchingType(String dataType) throws ConfigurationException;

    /**
     * Count the number of metadata stored in the database whith the specified validation flag.
     *
     * @param status Validation flag value.
     * @param filterMap Filters which is optional.
     *
     * @return The total count of metadata with the specified validation flag.
     */
    int countValidated(final boolean status,final Map<String,Object> filterMap);

    /**
     * Unmarshall an xml metadata into geotk object.
     *
     * @param metadata
     * @return
     * @throws org.constellation.exception.ConfigurationException
     */
    Object unmarshallMetadata(final String metadata) throws ConfigurationException;

    /**
     * Unmarshall metadata from a File
     * @param metadata
     * @return
     * @throws ConfigurationException
     * @deprecated use {@link #unmarshallMetadata(Path)} instead
     */
    @Deprecated
    Object unmarshallMetadata(final File metadata) throws ConfigurationException;

    /**
     * Unmarshall metadata from a Path
     * @param metadata
     * @return
     * @throws ConfigurationException
     */
    Object unmarshallMetadata(final Path metadata) throws ConfigurationException;

    /**
     * Marshall a geotk metadata object into a String.
     *
     * @param metadata
     * @return
     * @throws org.constellation.exception.ConfigurationException
     */
    String marshallMetadata(final Object metadata) throws ConfigurationException;

    void askForValidation(final int metadataID) throws ConfigurationException;

    void askForValidation(final List<Integer> ids, final String metadataLink, final boolean sendEmails) throws ConfigurationException;

    void denyValidation(final int metadataID, final String comment);

    void denyValidation(final MetadataBrief metadata, final String comment, final String metadataLink);

    void acceptValidation(final int metadataID) throws ConfigurationException;

    void acceptValidation(final MetadataBrief metadata, final String metadataLink) throws ConfigurationException;

    Map.Entry<Integer, List<MetadataBrief>> filterAndGetBrief(final Map<String,Object> filterMap, final Map.Entry<String,String> sortEntry,final int pageNumber,final int rowsPerPage);

    List<MetadataLightBrief> filterAndGetWithoutPagination(final Map<String,Object> filterMap);

    List<OwnerStatBrief> getOwnerStatBriefs(final Map<String, Object> filter);

    List<GroupStatBrief> getGroupStatBriefs(final Map<String, Object> filter);

    List<User> getUsers();

    User getUser(int id);

    /**
     * Extract the metadata from a file, first by tryng the special metadata format :
     * (DIMAP, ...)
     * then by unmarshalling it directly
     * @param metadataFile
     * @return
     */
    Object getMetadataFromFile(Path metadataFile) throws ConfigurationException;

    boolean isSpecialMetadataFormat(Path metadataFile);

    Object getMetadataFromSpecialFormat(Path metadataFile) throws ConfigurationException;

    /**
     * Retrieve an attachment with the specifed identifier.
     *
     * @param attachmentID attachment identifier.
     * @return
     */
    Attachment getMetadataAttachment(final int attachmentID);

    /**
     * Retrieve all the attachments with the specifed file name.
     *
     * @param fileName file name.
     * @return
     */
    List<Attachment> getMetadataAttachmentByFileName(final String fileName);

    /**
     * Proceed to upload  a file and create an attachment object.
     * (used bye example for metadata quicklook image in graphicOverview field)
     *
     * @param stream given stream of the file
     * @param fileName The original file name (allow to keep the extension).
     *
     * @return created attachment id
     * @throws ConfigurationException
     */
    int createMetadataAttachment(final InputStream stream, final String fileName) throws ConfigurationException;

    /**
     * Create a link betwen an attachment file and a metadata.
     *
     * @param metadataID Metadata identifier.
     * @param attchmentId Attachment identifier.
     */
    void linkMetadataAtachment(int metadataID, int attchmentId);

    /**
     * Remove a link betwen an attachment file and a metadata.
     *
     * @param metadataID Metadata identifier.
     * @param attchmentId Attachment identifier.
     */
    void unlinkMetadataAtachment(int metadataID, int attchmentId);

    /**
     * Create a link betwen a data and a metadata.
     *
     * @param metadataID Metadata identifier.
     * @param dataId Data identifier.
     */
    void linkMetadataData(int metadataID, int dataId);

    /**
     * Remove a link betwen a data and a metadata.
     *
     * @param metadataID Metadata identifier.
     * @param dataId Data identifier.
     */
    void unlinkMetadataData(int metadataID, int dataId);

    /**
     * Create a link betwen a dataset and a metadata.
     *
     * @param metadataID Metadata identifier.
     * @param datasetId Dataset identifier.
     */
    void linkMetadataDataset(int metadataID, int datasetId);

    /**
     * Remove a link betwen a dataset and a metadata.
     *
     * @param metadataID Metadata identifier.
     * @param datasetId Dataset identifier.
     */
    void unlinkMetadataDataset(int metadataID, int datasetId);

    /**
     * Create a link betwen a MapContext and a metadata.
     *
     * @param metadataID Metadata identifier.
     * @param contextId MapContext identifier.
     */
    void linkMetadataMapContext(int metadataID, int contextId);

    /**
     * Remove a link betwen a MapContext file and a metadata.
     *
     * @param metadataID Metadata identifier.
     * @param contextId MapContext identifier.
     */
    void unlinkMetadataMapContext(int metadataID, int contextId);

    /**
     * Create a new attachment file and a link it to a metadata.
     *
     * @param metadataID Metadata identifier.
     * @param path Path of the file on the server.
     * @param fileName The original file name
     *
     * @return The newly created attachment identifier.
     */
    int addMetadataAtachment(final int metadataID, final URI path, String fileName);

    /**
     * Create a new attachment file and a link it to a metadata.
     *
     * @param metadataID Metadata identifier.
     * @param content Content of the file to copy in the database.
     * @param fileName The original file name
     *
     * @return The newly created attachment identifier.
     *
     * @throws org.constellation.exception.ConfigurationException
     */
    int addMetadataAtachment(final int metadataID, final InputStream content, String fileName) throws ConfigurationException;

    void deleteFromProvider(int identifier) throws ConstellationException;

    Integer getDefaultInternalProviderID() throws ConfigurationException;

    String getJsonDatasetMetadata(final int datasetId, boolean prune, boolean override) throws ConstellationException;

    String getJsonDataMetadata(final int data, boolean prune, boolean override) throws ConstellationException;

    void mergeDatasetMetadata(final int datasetId, final RootObj metadataValues) throws ConstellationException;

    void mergeDataMetadata(final int dataId, final RootObj metadataValues) throws ConstellationException;

    Map<String,Integer> getStats(Map<String, Object> filter);

    void updateSharedProperty(final List<Integer> ids, final boolean shared) throws ConfigurationException;

    void updateSharedProperty(final int id, final boolean shared) throws ConfigurationException;

    List<MetadataLightBrief> getMetadataBriefForData(final int dataId);

    List<MetadataLightBrief> getMetadataBriefForDataset(final int datasetId);
}
