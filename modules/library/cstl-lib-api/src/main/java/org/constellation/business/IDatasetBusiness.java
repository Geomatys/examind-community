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

import org.constellation.exception.ConstellationException;
import org.constellation.dto.DataBrief;
import org.constellation.dto.DataSetBrief;

import java.util.List;
import java.util.Map;
import org.constellation.dto.Data;
import org.constellation.dto.DataSet;
import org.constellation.dto.process.DatasetProcessReference;

/**
 * @author Cédric Briançon (Geomatys)
 */
public interface IDatasetBusiness {

    /**
     * Create and insert then returns a new dataset for given parameters.
     *
     * @param identifier dataset identifier.
     * @param owner
     * @return {@link Integer} Dataset id.
     * @throws ConstellationException
     */
    int createDataset(String identifier, Integer owner, String type) throws ConstellationException;

    /**
     * Proceed to update metadata for given dataset identifier.
     *
     * @param datasetIdentifier given dataset identifier.
     * @param metadata metadata as {@link org.apache.sis.metadata.iso.DefaultMetadata} to update.
     * @throws ConstellationException
     *
     * @deprecated use the updateMetadata method with the hidden flag
     */
    @Deprecated
    void updateMetadata(final String datasetIdentifier, final Object metadata) throws ConstellationException;

    /**
     * Proceed to update metadata for given dataset identifier.
     *
     * @param datasetId given dataset identifier.
     * @param metadata metadata as {@link org.apache.sis.metadata.iso.DefaultMetadata} to update.
     * @param hidden flag to indicate if the metadata should be hidden for now.
     *
     * @throws ConstellationException
     */
    void updateMetadata(final int datasetId, final Object metadata, boolean hidden) throws ConstellationException;

    /**
     * Get metadata for given dataset identifier.
     *
     * @param datasetIdentifier given dataset identifier.
     * @return {@link org.apache.sis.metadata.iso.DefaultMetadata}.
     * @throws ConstellationException for JAXBException
     */
    Object getMetadata(final String datasetIdentifier) throws ConstellationException;

    /**
     * Get metadata for given dataset id.
     *
     * @param datasetId given dataset id.
     * @return {@link org.apache.sis.metadata.iso.DefaultMetadata}.
     * @throws ConstellationException for JAXBException
     */
    Object getMetadata(final int datasetId) throws ConstellationException;

    void removeDataset(Integer datasetId) throws ConstellationException;

    void removeDataset(final String datasetIdentifier) throws ConstellationException;

    void removeAllDatasets() throws ConstellationException;

    /**
     * Get dataset for given id.
     *
     * @param id dataset id.
     * @return {@link DataSet}.
     */
    DataSet getDataset(int id);

    /**
     * Get all dataset from dataset table.
     * @return list of {@link DataSet}.
     */
    List<DataSet> getAllDataset();

    List<Integer> getAllDatasetIds();

    /**
     * Proceed to link data to dataset.
     *
     * @param dataset given dataset.
     * @param datas given data to link.
     */
    void linkDataTodataset(DataSet dataset, List<Data> datas);

    Integer getDatasetId(String identifier);

    List<DatasetProcessReference> getAllDatasetReference();

    void addProviderDataToDataset(final String datasetId, final String providerId) throws ConstellationException;

    DataSetBrief getDatasetBrief(final Integer dataSetId, List<DataBrief> children);

    boolean existsById(int datasetId);

    boolean existsByName(String datasetName);

    Map.Entry<Integer, List<DataSetBrief>> filterAndGetBrief(Map<String, Object> filterMap, Map.Entry<String, String> sortEntry, int pageNumber, int rowsPerPage);

    DataSetBrief getSingletonDatasetBrief(DataSetBrief dsItem, List<DataBrief> items);
    
    void updateDatasetIdentifier(int datasetId, String newIdentifier);
}
