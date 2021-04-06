/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2015 Geomatys.
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
package org.constellation.repository;

import java.util.List;
import java.util.Map;

import org.constellation.dto.metadata.MetadataComplete;
import org.constellation.dto.metadata.Metadata;
import org.constellation.dto.metadata.MetadataBbox;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public interface MetadataRepository extends AbstractRepository {

    int create(MetadataComplete metadata);

    Metadata update(MetadataComplete metadata);

    List<Metadata> findByDataId(int dataId);

    Metadata findByDatasetId(int id);

    Metadata findByServiceId(int serviceId);

    Metadata findByMapContextId(int mapContextId);

    Metadata findByMetadataId(String metadataId);

    Metadata findById(int id);

    List<Metadata> findByProviderId(final Integer providerId, final String type);

    List<Metadata> findAll(final boolean includeService, final boolean onlyPublished);

    List<Integer> findAllIds();

    List<MetadataBbox> getBboxes(int id);

    List<Metadata> findByCswId(Integer id);

    /**
     * Look for a numerated title like 'title(%)'.
     *
     * @param title
     * @return
     */
    List<String> findByTitlePrefix(String title);

    List<String> findMetadataIDByCswId(final Integer id, final boolean includeService, final boolean onlyPublished, final String type, final Boolean hidden);

    List<String> findMetadataIDByProviderId(final Integer id, final boolean includeService, final boolean onlyPublished, final String type, final Boolean hidden);

    int countMetadataByCswId(final Integer id, final boolean includeService, final boolean onlyPublished, final String type, final Boolean hidden);

    int countMetadataByProviderId(final Integer id, final boolean includeService, final boolean onlyPublished, final String type, final Boolean hidden);

    List<String> findMetadataID(final boolean includeService, final boolean onlyPublished, final Integer providerId, final String type);

    int countMetadata(final boolean includeService, final boolean onlyPublished, final Integer providerID, final String type);

    boolean isLinkedMetadata(Integer metadataID, Integer cswID);

    boolean isLinkedMetadata(String metadataID, String cswID);

    boolean isLinkedMetadata(String metadataID, String cswID, final boolean includeService, final boolean onlyPublished);

    boolean isLinkedMetadata(String metadataID, int providerID, final boolean includeService, final boolean onlyPublished);

    List<Metadata> findAll();

    Map.Entry<Integer, List<Metadata>> filterAndGet(final Map<String,Object> filterMap, final Map.Entry<String,String> sortEntry,final int pageNumber,final int rowsPerPage);

    List<Map<String, Object>> filterAndGetWithoutPagination(final Map<String,Object> filterMap);

    Map<String,Integer> getProfilesCount(final Map<String,Object> filterMap);

    void addMetadataToCSW(final String metadataID, final int cswID);

    void removeDataFromCSW(final String metadataID, final int cswID);

    void changeOwner(final int id, final int owner);

    void changeValidation(final int id, final boolean validated);

    void changePublication(final int id, final boolean published);

    void changeHidden(final int id, final boolean hidden);

    void changeSharedProperty(final int id, final boolean shared);

    void changeProfile(final int id, final String newProfile);

    int countTotalMetadata(final Map<String,Object> filterMap);

    int countValidated(final boolean status,final Map<String,Object> filterMap);

    int countPublished(final boolean status,final Map<String,Object> filterMap);

    int countInCompletionRange(final Map<String,Object> filterMap, final int minCompletion, final int maxCompletion);

    void setValidationRequired(final int id, final String state, final String validationState);

    void denyValidation(final int id, final String comment);

    boolean existInternalMetadata(final String metadataID, final boolean includeService, final boolean onlyPublished, final Integer providerID);

    boolean existMetadataTitle(final String title);

    void linkMetadataData(int metadataID, int dataId);

    void unlinkMetadataData(int metadataID);

    void linkMetadataMapContext(int metadataID, int contextId);

    void unlinkMetadataMapContext(int metadataID);

    public void linkMetadataDataset(int metadataID, int datasetId);

    public void unlinkMetadataDataset(int metadataID);
}
