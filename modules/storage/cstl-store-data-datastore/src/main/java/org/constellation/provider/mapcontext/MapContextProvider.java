/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2021 Geomatys.
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
package org.constellation.provider.mapcontext;

import com.examind.provider.component.ExaDataCreator;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.portrayal.MapLayers;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.constellation.admin.SpringHelper;
import org.constellation.business.IMapContextBusiness;
import org.constellation.dto.MapContextLayersDTO;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.exception.TargetNotFoundException;
import org.constellation.provider.AbstractDataProvider;
import org.constellation.provider.Data;
import org.constellation.provider.DataProviderFactory;
import org.constellation.repository.MapContextRepository;
import org.geotoolkit.util.NamesExt;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys).
 */
public class MapContextProvider extends AbstractDataProvider {

    @Autowired
    private ExaDataCreator dataCreator;

    private final Map<GenericName, Data> dataCache = new HashMap<>();

    public MapContextProvider(String providerId, DataProviderFactory service, ParameterValueGroup param) {
        super(providerId,service,param);
        SpringHelper.injectDependencies(this);
    }

    @Override
    public DataStore getMainStore() {
        // no store
        return null;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Set<GenericName> getKeys() throws ConstellationStoreException {
        final MapContextRepository repo = SpringHelper.getBean(MapContextRepository.class)
                                                      .orElseThrow(() -> new ConstellationStoreException("No spring context available"));
        return repo.findAll().stream().map(mc -> NamesExt.create(mc.getName())).collect(Collectors.toSet());
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Data get(final GenericName key) throws ConstellationStoreException {
        return get(key, null);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Data get(GenericName key, Date version) throws ConstellationStoreException {
        Data d = dataCache.get(key);
        if (d == null) {
            final IMapContextBusiness repo = SpringHelper.getBean(IMapContextBusiness.class)
                                                         .orElseThrow(() -> new ConstellationStoreException("No spring context available"));
            try {
                MapContextLayersDTO mc = repo.findByName(key.tip().toString());
                if (mc != null) {
                    MapLayers ml = MapContextUtils.getMapLayers(mc);
                    d = dataCreator.createMapContextData(ml);
                    // dataCache.put(key, d); do not put in cache because of sis/geotk issue with concurrent mapLayers
                }
            } catch (TargetNotFoundException ex) {
                LOGGER.log(Level.FINER, "mapcontext does not exist:" + key);
            } catch (ConstellationException | FactoryException | DataStoreException ex) {
                LOGGER.log(Level.WARNING, "Error while buildmap context data.", ex);
            }
        }
        return d;
    }

    /**
     * nothing to remove, you must remove the mapcontext itself and reload.
     * @param key ignored.
     */
    @Override
    public boolean remove(GenericName key) {
        return true;
    }

    /**
     * nothing to remove, you must remove the mapcontexts and reload.
     */
    @Override
    public void removeAll() {}

    /**
     * {@inheritDoc }
     */
    @Override
    public void reload() {
        dataCache.clear();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void dispose() {
        dataCache.clear();
    }

    @Override
    public boolean isSensorAffectable() {
        return false;
    }

    @Override
    public Path[] getFiles() throws ConstellationException {
        // do we want to export multiple files?
        return new Path[0];
    }

    @Override
    public DefaultMetadata getStoreMetadata() throws ConstellationStoreException {
        // do we want to get and merge resources metadata?
        DefaultMetadata metadata = new DefaultMetadata();
        return metadata;
    }
}
