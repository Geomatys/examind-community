/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2016 Geomatys.
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
package org.constellation.provider.sensorstore;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.GenericName;

import org.apache.sis.storage.DataStoreException;

import org.geotoolkit.sensor.AbstractSensorStore;
import org.geotoolkit.sml.xml.AbstractSensorML;
import org.geotoolkit.util.NamesExt;

import org.constellation.exception.ConstellationStoreException;
import org.constellation.provider.IndexedNameDataProvider;
import org.constellation.provider.Data;
import org.constellation.provider.DataProviderFactory;
import org.constellation.provider.SensorProvider;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class SensorStoreProvider extends IndexedNameDataProvider<AbstractSensorStore> implements SensorProvider {

    public SensorStoreProvider(String providerId, DataProviderFactory service, ParameterValueGroup param) throws DataStoreException{
        super(providerId,service,param);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Data computeData(GenericName key) throws ConstellationStoreException {
        final AbstractSensorStore store = getMainStore();
        try {
            final AbstractSensorML metadata = store.getSensorML(key.toString());
            return  (metadata != null) ? new DefaultSensorData(key, store, metadata) : null;
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    protected Set<GenericName> computeKeys() {
        final Set<GenericName> results = new LinkedHashSet<>();
        final AbstractSensorStore store = getMainStore();
        if (store != null) {
            try {
                for (final String sensorId : getMainStore().getSensorNames()) {
                    results.add(NamesExt.create(sensorId));
                }
            } catch (DataStoreException ex) {
                LOGGER.log(Level.SEVERE, "Failed to retrieve list of available sensor names.", ex);
            }
        }
        return results;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    protected Class getStoreClass() {
        return AbstractSensorStore.class;
    }

    @Override
    public synchronized boolean remove(GenericName key) {
        final AbstractSensorStore store = getMainStore();
        boolean result = false;
        try {
            result = store.deleteSensor(key.toString());
            if (result) {
                reload();
            }
        } catch (DataStoreException ex) {
            LOGGER.log(Level.INFO, "Unable to remove " + key.toString() + " from provider.", ex);
        }
        return result;
    }

    @Override
    public String getNewSensorId() throws ConstellationStoreException {
        final AbstractSensorStore store = getMainStore();
        try {
            return store.getNewSensorId();
        } catch (DataStoreException ex) {
            LOGGER.log(Level.INFO, "Unable to acquire a new sensorID from provider.", ex);
        }
        return null;
    }

    @Override
    public boolean writeSensor(String id, Object sensor) throws ConstellationStoreException {
        final AbstractSensorStore store = getMainStore();
        boolean result = false;
        try {
            result = store.writeSensor(id, sensor);
            if (result) {
                reload();
            }
        } catch (DataStoreException ex) {
            LOGGER.log(Level.INFO, "Unable to insert a new sensor in provider:" + id, ex);
        }
        return result;
    }

    @Override
    public Map<String, List<String>> getAcceptedSensorMLFormats() {
        final AbstractSensorStore store = getMainStore();
        return store.getAcceptedSensorMLFormats();
    }
}
