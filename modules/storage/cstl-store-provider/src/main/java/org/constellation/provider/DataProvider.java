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
package org.constellation.provider;

import java.nio.file.Path;
import java.util.Date;
import java.util.Set;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.constellation.api.DataType;
import org.constellation.api.ProviderType;
import org.apache.sis.storage.DataStore;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationStoreException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.GenericName;

/**
 *
 * @version $Id$
 *
 * @author Johann Sorel (Geomatys)
 */
public interface DataProvider{

    final String RELOAD_TIME_PROPERTY = "updateTime";

    String getId();

    /**
     * @return the factory which created this provider.
     */
    DataProviderFactory getFactory();

    /**
     * Use this method if you need the complete list of entries in this data provider.
     * If you are just searching if a special key exists than you should use the contains method.
     */
    Set<GenericName> getKeys();

    /**
     * If you want to intend to get the related data, you should use the
     * get method directly and test if the result is not null.
     *
     * @param key Data name to be removed from this provider.
     * @return true if the given key data is in this data provider .
     */
    boolean contains(GenericName key);

    /**
     * Get the data related to the given key.
     * 
     * @param key Data name to be removed from this provider.
     * @return V object if it is in the data provider, or null if not.
     */
    Data get(GenericName key);

    /**
     * Get the data related to the given key built from the namespace and name.
     *
     * @param namespace Namespace or {@code null}
     * @param name Name of the data in the provider
     *
     * @return V object if it is in the data provider, or null if not.
     */
    Data get(String namespace, String name);

    /**
     * Reload data provider. this may be useful if new entries on disk have been
     * added after creation.
     */
    void reload();

    /**
     * Clear every caches, this data provider should not be used after a call
     * to this method.
     */
    void dispose();

    /**
     * The configuration of this provider. Can be null if the provider
     * is hard coded.
     */
    ParameterValueGroup getSource();

    /**
     * Remove all data from this provider.
     */
    void removeAll();

    /**
     * Remove a data from this provider.
     *
     * @param key Data name to be removed from this provider.
     */
    boolean remove(GenericName key);

    /**
     * Remove a data from this provider.
     *
     * @param namespace Namespace or {@code null}
     * @param name Name of the data in the provider
     */
    boolean remove(String namespace, String name);

    ProviderType getProviderType();

    @Deprecated
    DataType getDataType();

    boolean isSensorAffectable();

    /**
     * Original data store.
     * @return
     */
    DataStore getMainStore();

    /**
     * Get the data related to the given key in given version.
     * @return LayerDetails if it is in the data provider, or null if not.
     */
    Data get(GenericName key, Date version);

    Path[] getFiles() throws ConstellationException;

    DefaultMetadata getStoreMetadata() throws ConstellationStoreException;

    String getCRSName() throws ConstellationStoreException;

}
