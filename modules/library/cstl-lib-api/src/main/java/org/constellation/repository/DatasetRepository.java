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

package org.constellation.repository;

import org.constellation.dto.DataSet;

import java.util.List;
import java.util.Map;

/**
 *
 * @author Guilhem Legal
 */
public interface DatasetRepository extends AbstractRepository {

    List<DataSet> findAll();

    Integer create(DataSet dataset);

    int update(DataSet dataset);

    DataSet findByMetadataId(String metadataId);

    DataSet findByIdentifier(String datasetIdentifier);

    Integer findIdForIdentifier(String datasetIdentifier);

    DataSet findByIdentifierWithEmptyMetadata(String datasetIdentifier);

    DataSet findById(int datasetId);

    List<DataSet> getCswLinkedDataset(final int cswId);

    void addDatasetToCSW(final int serviceID, final int datasetID);

    void removeDatasetFromCSW(final int serviceID, final int datasetID);

    void removeAllDatasetFromCSW(final int serviceID);

    boolean existsByName(String datasetName);

    Map.Entry<Integer, List<DataSet>> filterAndGet(Map<String, Object> filterMap, Map.Entry<String, String> sortEntry, int pageNumber, int rowsPerPage);

    List<Integer> getAllIds();

    Integer getDataCount(int datasetId);
}
