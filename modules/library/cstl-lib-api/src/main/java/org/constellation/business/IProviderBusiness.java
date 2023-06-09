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

import java.util.List;
import org.constellation.api.ProviderType;
import org.constellation.dto.DataBrief;
import org.constellation.dto.ProviderBrief;
import org.constellation.dto.ProviderConfiguration;
import org.constellation.dto.Style;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;

/**
 * @author Cédric Briançon (Geomatys)
 */
public interface IProviderBusiness {

    /**
     * Identifier of the possible provider types.
     * @deprecated Used for current provider management mechanism. Removed when providers will be simplified in
     * DataStoreSource.
     */
    public static enum SPI_NAMES {
        DATA_SPI_NAME("data-store"),
        SENSOR_SPI_NAME("sensor-store"),
        METADATA_SPI_NAME("metadata-store"),
        OBSERVATION_SPI_NAME("observation-store");

        public final String name;
        private SPI_NAMES(final String providerSPIName) {
            name = providerSPIName;
        }
    }

    ProviderBrief getProvider(Integer id);

    List<String> getProviderIds();

    List<ProviderBrief> getProviders();

    Integer getIDFromIdentifier(String providerId);

    /**
     * Create and save a provider object from input identifier and {@link org.constellation.dto.ProviderConfiguration} object.
     *
     * @param id The identifier (name) to give to the created provider.
     * @param config Serialized provider configuration (entire parameter group, as defined in the matching DataProviderFactory.
     * @return  A new Provider ID.
     * @throws ConstellationException If a provider already exists with the given name, or if the configuration is invalid.
     *
     * @deprecated : Following procedure will be removed once the new DataStoreSource system will be created.
     */
    Integer create(String id, ProviderConfiguration config) throws ConstellationException;

    /**
     * Create and save a provider object with given identifier. Input spi and configuration must be org.apache.sis.storage.DataStoreProvider
     * and its proper configuration filled from org.apache.sis.storage.DataStoreProvider#getParametersDescriptor().
     *
     * @param id The identifier (name) to give to the created provider.
     * @param spiConfiguration The configuration needed for spi parameter to open a valid data source.
     * @param spiName The name of spi (data-store, sensor-store, ...)
     * @return  A new Provider ID.
     *
     * @throws ConfigurationException If a provider already exists with the given name, or if the configuration is invalid.
     *
     * @deprecated : Following procedure will be removed once the new DataStoreSource system will be created.
     */
    Integer create(final String id, SPI_NAMES spiName, ParameterValueGroup spiConfiguration) throws ConfigurationException;

    boolean test(String providerIdentifier, ProviderConfiguration configuration) throws ConstellationException;

    void update(final Integer id, String providerConfig) throws ConfigurationException;

    void update(final String id, String providerConfig) throws ConfigurationException;

    void update(String id, ProviderConfiguration config) throws ConfigurationException;

    void update(final String id, SPI_NAMES spiName, ParameterValueGroup spiConfiguration) throws ConfigurationException;

    /**
     * Create and save a provider object with given identifier.Input spi and configuration must be DataProviderFactory
     * and its proper configuration filled from org.constellation.provider.DataProviderFactory#getProviderDescriptor().
     *
     * @param id The identifier (name) to give to the created provider.
     * @param type The provider type
     * @param factoryName Name of the org.constellation.provider.DataProviderFactory to identify underlying data source type.
     * @param config The configuration needed for providerSPI parameter to open a valid data source.
     *
     * @return A new Provider ID
     * @throws ConfigurationException If a provider already exists with the given name, or if the configuration is invalid.
     *
     */
    Integer storeProvider(String id, ProviderType type, String factoryName, GeneralParameterValue config) throws ConfigurationException;

    /**
     * Get all datas from the specified provider
     * @param id provider identifier
     * @return
     */
    List<Integer> getDataIdsFromProviderId(Integer id);

    /**
     * Get all datas from the specified provider, with filter on the data type, hidden and included flags.
     *
     * @param id provider identifier
     * @param dataType data type or {@code null} for no filter
     * @param included included flag filter
     * @param hidden hidden flag fliter.
     * @return
     */
    List<Integer> getDataIdsFromProviderId(Integer id, String dataType, boolean included, boolean hidden);

    /**
     * Get all data brief from the specified provider, with filter on the data type, hidden and included flags.
     *
     * @param id provider identifier
     * @param dataType data type or {@code null} for no filter
     * @param included included flag filter
     * @param hidden hidden flag fliter.
     * @param fetchDataDescription If true, will retriee the envelope, and specific data informations (like columns, bands, etc)
     * @param fetchAssociations If true, will retrieve the data linked object (like sensors, services, metadatas, etc)
     * @return
     *
     * @throws org.constellation.exception.ConstellationException
     */
    List<DataBrief> getDataBriefsFromProviderId(Integer id, String dataType, boolean included, boolean hidden, boolean fetchDataDescription, boolean fetchAssociations) throws ConstellationException;

    List<Style> getStylesFromProviderId(Integer id);

    void removeProvider(int providerId) throws ConstellationException;

    @Deprecated
    void removeProvider(String providerId) throws ConstellationException;

    void removeAll() throws ConstellationException;

    /**
     * Get all provider identifiers.
     * 
     * @return 
     */
    List<Integer> getProviderIdsAsInt();

    /**
     *
     * @param providerId given provider identifier
     * @param datasetId given dataset identifier to attach to data.
     * @param createDatasetIfNull flag that indicates if a dataset will be created in case of given datasetId is null.
     * @param hideNewData Flag used to create data as hidden until validated.
     * @param owner he owner of the dataset/datas created.
     *
     * @return The asssigned dataset id if createDatasetIfNull is set to true, or if a datasetId is specified.
     * return {@code null} else.
     * 
     * @throws org.constellation.exception.ConfigurationException
     */
    Integer createOrUpdateData(final int providerId, Integer datasetId, final boolean createDatasetIfNull, final boolean hideNewData, Integer owner) throws ConstellationException;

    void reload(int providerId) throws ConstellationException;

    void reload(String providerId) throws ConstellationException;

}
