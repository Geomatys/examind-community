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

package org.constellation.admin;

import java.util.AbstractMap;
import org.constellation.exception.ConstellationException;
import org.constellation.business.IDatasetBusiness;
import org.constellation.business.IMetadataBusiness;
import org.constellation.exception.ConfigurationException;
import org.constellation.dto.DataBrief;
import org.constellation.dto.DataSetBrief;
import org.constellation.exception.TargetNotFoundException;
import org.constellation.dto.CstlUser;
import org.constellation.dto.Data;
import org.constellation.dto.DataSet;
import org.constellation.repository.DataRepository;
import org.constellation.repository.DatasetRepository;
import org.constellation.repository.ProviderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import org.constellation.business.IDataBusiness;
import org.constellation.business.IUserBusiness;
import org.constellation.dto.metadata.MetadataLightBrief;
import org.constellation.metadata.utils.Utils;
import org.constellation.dto.process.DatasetProcessReference;

/**
 *
 * Business facade for dataset.
 *
 * @author Guilhem Legal (Geomatys)
 * @author Mehdi Sidhoum (Geomatys).
 * @since 0.9
 */
@Component("cstlDatasetBusiness")
@Primary
public class DatasetBusiness implements IDatasetBusiness {

    /**
     * Used for debugging purposes.
     */
    protected static final Logger LOGGER = Logger.getLogger("org.constellation.admin");

    /**
     * Injected user business.
     */
    @Autowired
    protected IUserBusiness userBusiness;

    /**
     * Injected dataset repository.
     */
    @Autowired
    protected DatasetRepository datasetRepository;
    /**
     * Injected data repository.
     */
    @Autowired
    protected DataRepository dataRepository;

    /**
     * Injected data business.
     */
    @Autowired
    protected IDataBusiness dataBusiness;

    /**
     * Injected provider repository.
     */
    @Autowired
    private ProviderRepository providerRepository;
    /**
     * Injected metadata repository.
     */
    @Autowired
    protected IMetadataBusiness metadataBusiness;

    /**
     * Creates a new instance of {@link DatasetBusiness}.
     */
    public DatasetBusiness() {

    }

    @Override
    public Integer getDatasetId(final String identifier) {
        return datasetRepository.findIdForIdentifier(identifier);
    }

    @Override
    public List<Integer> getAllDatasetIds() {
        List<Integer> result = datasetRepository.getAllIds();
        if (result == null) {
            return new ArrayList<>();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DataSet> getAllDataset() {
        List<DataSet> result = datasetRepository.findAll();
        if (result == null) {
            return new ArrayList<>();
        }
        return result;
     }


    /**
     * {@inheritDoc}
     */
    @Override
    public DataSet getDataset(final int id) {
        return datasetRepository.findById(id);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public int createDataset(final String identifier, final Integer owner, final String type) throws ConstellationException {
        if (datasetRepository.existsByName(identifier)) {
            throw new ConfigurationException("Dataset with name" + identifier + " alsready exist");
        }
        DataSet ds = new DataSet();
        ds.setIdentifier(identifier);
        ds.setOwnerId(owner);
        ds.setDate(System.currentTimeMillis());
        ds.setType(type);
        return datasetRepository.create(ds);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getMetadata(final String datasetIdentifier) throws ConstellationException {
        final Integer dsId = getDatasetId(datasetIdentifier);
        if (dsId != null) {
            return metadataBusiness.getIsoMetadataForDataset(dsId);
        }
        return null;
    }

    @Override
    public Object getMetadata(final int datasetId) throws ConstellationException {
        return metadataBusiness.getIsoMetadataForDataset(datasetId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void updateMetadata(final String datasetIdentifier, final Object metadata)
            throws ConstellationException {
        final Integer dataset = datasetRepository.findIdForIdentifier(datasetIdentifier);
        if (dataset != null) {
            Integer internalProviderID = metadataBusiness.getDefaultInternalProviderID();
            if (internalProviderID != null) {
                final String metadataID = Utils.findIdentifier(metadata);
                metadataBusiness.updateMetadata(metadataID, metadata, null, dataset, null, null, internalProviderID, "DOC");
            } else {
                LOGGER.warning("No metadata provider available");
            }
        } else {
            throw new TargetNotFoundException("Dataset :" + datasetIdentifier + " not found");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void updateMetadata(final int datasetId, final Object metadata, boolean hidden) throws ConstellationException {
        Integer internalProviderID = metadataBusiness.getDefaultInternalProviderID();
        if (internalProviderID != null) {
            final String metadataID = Utils.findIdentifier(metadata);
            metadataBusiness.updateMetadata(metadataID, metadata, null, datasetId, null, null, internalProviderID, "DOC", null, hidden);
        } else {
            LOGGER.warning("No metadata provider available");
        }
    }

    @Override
    @Transactional
    public void addProviderDataToDataset(final String datasetId, final String providerId) throws ConfigurationException {
        final Integer ds = datasetRepository.findIdForIdentifier(datasetId);
        if (ds != null) {
            final Integer p = providerRepository.findIdForIdentifier(providerId);
            if (p != null) {
                final List<Data> datas = dataRepository.findByProviderId(p);
                for (Data data : datas) {
                    data.setDatasetId(ds);
                    dataRepository.update(data);
                }
            } else {
                throw new TargetNotFoundException("Unable to find a profile: " + providerId);
            }
        } else {
            throw new TargetNotFoundException("Unable to find a dataset: " + datasetId);
        }
    }

    @Transactional
    @Override
    public void removeDataset(Integer datasetId) throws ConstellationException {
        if (datasetId != null) {
            final Set<Data> linkedData = new HashSet<>();

            // 1. List dataset data
            linkedData.addAll(dataRepository.findAllByDatasetId(datasetId));

            // 2. delete data
            for (Data data : linkedData) {
                dataBusiness.removeData(data.getId(), false);
            }

            // 4. remove dataset metadata
            metadataBusiness.deleteDatasetMetadata(datasetId);

            // 5. remove dataset
            datasetRepository.delete(datasetId);
        }
    }

    @Transactional
    @Override
    public void removeDataset(String datasetIdentifier) throws ConstellationException {
        removeDataset(datasetRepository.findIdForIdentifier(datasetIdentifier));
    }

    @Transactional
    @Override
    public void removeAllDatasets() throws ConstellationException {
        final List<Integer> all = datasetRepository.getAllIds();
        for (Integer dataset : all) {
            removeDataset(dataset);
        }
    }



    @Override
    public DataSetBrief getDatasetBrief(Integer dataSetId, List<DataBrief> children) {
        final DataSet dataset = datasetRepository.findById(dataSetId);
        return convertToBrief(dataset, children, children.size());
    }

    private DataSetBrief convertToBrief(DataSet dataset, List<DataBrief> children, int dataCount) {
        Integer completion = metadataBusiness.getCompletionForDataset(dataset.getId());
        final Optional<CstlUser> optUser = userBusiness.findById(dataset.getOwnerId());
        Integer ownerId = null;
        String owner = null;
        if(optUser.isPresent()){
            final CstlUser user = optUser.get();
            ownerId = user.getId();
            owner = user.getLogin();
        }
        final List<MetadataLightBrief> metadatas = metadataBusiness.getMetadataBriefForDataset(dataset.getId());

        // TODO : Get BBOX of this datasets byt the union of all data bbox

        final DataSetBrief dsb = new DataSetBrief(dataset.getId(),
                dataset.getIdentifier(),
                dataset.getType(), ownerId, owner, children,
                dataset.getDate(),
                completion,
                dataCount,
                metadatas);

        return dsb;
    }

    @Override
    public boolean existsById(int datasetId) {
        return datasetRepository.existsById(datasetId);
    }

    @Override
    public boolean existsByName(String datasetName) {
        return datasetRepository.existsByName(datasetName);
    }

    @Override
    public DataSetBrief getSingletonDatasetBrief(DataSetBrief dsItem, List<DataBrief> items) {
        return new DataSetBrief(dsItem, items);
    }

    @Override
    public Map.Entry<Integer, List<DataSetBrief>> filterAndGetBrief(Map<String, Object> filterMap, Map.Entry<String, String> sortEntry, int pageNumber, int rowsPerPage) {
        final Map.Entry<Integer, List<DataSet>> entry = datasetRepository.filterAndGet(filterMap, sortEntry, pageNumber, rowsPerPage);
        final List<DataSet> datasetList = entry.getValue();
        final List<DataSetBrief> results = new ArrayList<>();
        for (DataSet ds : datasetList) {
            results.add(convertToBrief(ds, new ArrayList<>(), datasetRepository.getDataCount(ds.getId())));
        }
        return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), results);
    }

    @Override
    public List<DatasetProcessReference> getAllDatasetReference() {
        final List<DatasetProcessReference> datasetPRef = new ArrayList<>();
        final List<DataSet> datasets = datasetRepository.findAll();
        for (final DataSet ds : datasets) {
            final DatasetProcessReference ref = new DatasetProcessReference();
            ref.setId(ds.getId());
            ref.setIdentifier(ds.getIdentifier());
            datasetPRef.add(ref);
        }
        return datasetPRef;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void linkDataTodataset(final DataSet dataset, final List<Data> datas) {
        for (final Data data : datas) {
            data.setDatasetId(dataset.getId());
            dataRepository.update(data);
        }
    }
}
