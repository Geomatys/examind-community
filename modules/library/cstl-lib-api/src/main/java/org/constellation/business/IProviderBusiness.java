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

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.constellation.api.ProviderType;
import org.constellation.dto.DataBrief;
import org.constellation.dto.ProviderBrief;
import org.constellation.dto.ProviderConfiguration;
import org.constellation.dto.ProviderPyramidChoiceList;
import org.constellation.dto.Style;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.GenericName;

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
     * @throws ConfigurationException If a provider already exists with the given name, or if the configuration is invalid.
     *
     * @deprecated : Following procedure will be removed once the new DataStoreSource system will be created.
     */
    Integer create(String id, ProviderConfiguration config) throws ConfigurationException;

    /**
     * Create and save a provider object with given identifier. Input spi and configuration must be org.apache.sis.storage.DataStoreProvider
     * and its proper configuration filled from org.apache.sis.storage.DataStoreProvider#getParametersDescriptor().
     *
     * @param id The identifier (name) to give to the created provider.
     * @param spiConfiguration The configuration needed for spi parameter to open a valid data source.
     * @return  A new Provider ID.
     *
     * @throws ConfigurationException If a provider already exists with the given name, or if the configuration is invalid.
     *
     * @deprecated : Following procedure will be removed once the new DataStoreSource system will be created.
     */
    Integer create(final String id, ParameterValueGroup spiConfiguration) throws ConfigurationException;

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

    /**
     * Create and save a provider object with given identifier. Input spi and configuration must be DataProviderFactory
     * and its proper configuration filled from org.constellation.provider.DataProviderFactory#getProviderDescriptor().
     *
     * @param id The identifier (name) to give to the created provider.
     * @param providerSPIName Name of the org.constellation.provider.DataProviderFactory to identify underlying data source type.
     * @param providerConfig The configuration needed for providerSPI parameter to open a valid data source.
     *
     * @return A new Provider ID
     * @throws ConfigurationException If a provider already exists with the given name, or if the configuration is invalid.
     *
     * @deprecated : Following procedure will be removed once the new DataStoreSource system will be created.
     */
    Integer create(final String id, final String providerSPIName, final ParameterValueGroup providerConfig) throws ConfigurationException;

    Set<GenericName> test(String providerIdentifier, ProviderConfiguration configuration) throws ConfigurationException;

    void update(final Integer id, String providerConfig) throws ConfigurationException;

    void update(final String id, String providerConfig) throws ConfigurationException;

    void update(String id, ProviderConfiguration config) throws ConfigurationException;

    void update(final String id, SPI_NAMES spiName, ParameterValueGroup spiConfiguration) throws ConfigurationException;

    Integer storeProvider(String providerId, String o, ProviderType type, String factoryName, GeneralParameterValue config) throws ConfigurationException;

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
     * @return
     *
     * @throws org.constellation.exception.ConstellationException
     */
    List<DataBrief> getDataBriefsFromProviderId(Integer id, String dataType, boolean included, boolean hidden) throws ConstellationException;

    List<Style> getStylesFromProviderId(Integer id);

    void removeProvider(int providerId) throws ConfigurationException;

    @Deprecated
    void removeProvider(String providerId) throws ConfigurationException;

    void removeAll() throws ConfigurationException;

    void updateParent(String id, String providerId);

    ProviderPyramidChoiceList listPyramids(final String id, final String dataName) throws ConfigurationException;


    /**
     * Generates a pyramid conform for each data of the provider.
     * N.B : Generated pyramid contains coverage real values, it's not styled for rendering.
     *
     * @param providerId Provider identifier of the data to tile.
     *
     */
    void createAllPyramidConformForProvider(final int providerId) throws ConstellationException;

    int createZXYPyramidProvider(String providerId, String pyramidProviderId) throws ConstellationException;

    /**
     * Generates a pyramid conform for data.
     * N.B : Generated pyramid contains coverage real values, it's not styled for rendering.
     *
     * @param providerId Provider identifier of the data to tile.
     * @param dataName the given data name.
     * @param namespace the given data namespace.
     * @param userId The pyramids owner.
     * @return {@link DataBrief}
     */
    DataBrief createPyramidConform(final String providerId,final String dataName, final String namespace,final int userId) throws ConstellationException;

    /**
     * Generates a pyramid conform for data.
     * N.B : Generated pyramid contains coverage real values, it's not styled for rendering.
     *
     * @param dataId The given data identifier.
     * @param userId The pyramids owner.
     * @return {@link DataBrief}
     */
    DataBrief createPyramidConform(final int dataId, final int userId) throws ConstellationException;

    List<Integer> getProviderIdsAsInt();

    List<Integer> getProviderIdsAsInt(boolean noParent);

    /**
     *
     * @param providerId given provider identifier
     * @param datasetId given dataset identifier to attach to data.
     * @param createDatasetIfNull flag that indicates if a dataset will be created in case of given datasetId is null.
     *
     * @return The asssigned dataset id if createDatasetIfNull is set to true, or if a datasetId is specified.
     * return {@code null} else.
     * @throws IOException
     * @throws org.constellation.exception.ConfigurationException
     */
    Integer createOrUpdateData(final int providerId, Integer datasetId, final boolean createDatasetIfNull) throws IOException, ConstellationException;

    Integer createOrUpdateData(final int providerId, Integer datasetId, final boolean createDatasetIfNull, final boolean hideNewData, Integer owner) throws IOException, ConstellationException;

    void reload(int providerId) throws ConstellationException;

    void reload(String providerId) throws ConstellationException;

    int createPyramidProvider(String providerId, String pyramidProviderId) throws ConstellationException;

    int createPyramidProvider(String providerId, String pyramidProviderId, boolean cacheTileState) throws ConstellationException;
}
