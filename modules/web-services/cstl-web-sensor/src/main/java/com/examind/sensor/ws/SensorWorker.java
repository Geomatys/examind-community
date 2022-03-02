/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2019 Geomatys.
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
package com.examind.sensor.ws;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.sis.storage.FeatureQuery;
import org.constellation.api.ServiceDef;
import org.constellation.api.WorkerState;
import org.constellation.business.ISensorBusiness;
import org.constellation.dto.service.config.sos.SOSConfiguration;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.provider.DataProvider;
import org.constellation.provider.DataProviders;
import org.constellation.provider.ObservationProvider;
import org.constellation.provider.SensorProvider;
import org.constellation.dto.service.config.sos.SOSProviderCapabilities;
import org.constellation.ws.AbstractWorker;
import org.geotoolkit.filter.FilterUtilities;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.ResourceId;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.observation.Phenomenon;
import org.opengis.observation.Process;
import org.opengis.observation.sampling.SamplingFeature;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class SensorWorker extends AbstractWorker<SOSConfiguration> {

    /**
     * The sensor business
     */
    @Autowired
    protected ISensorBusiness sensorBusiness;

    /**
     * The sensorML provider identifier (to be removed)
     */
    protected Integer smlProviderID;

    /**
     * The Observation provider
     */
    protected ObservationProvider omProvider;

    protected final FilterFactory ff = FilterUtilities.FF;

    public SensorWorker(final String id, final ServiceDef.Specification specification) {
        super(id, specification);
        if (getState().equals(WorkerState.ERROR)) return;
        try {
            final List<Integer> providers = serviceBusiness.getLinkedProviders(getServiceId());

            // we initialize the reader/writer
            for (Integer providerID : providers) {
                DataProvider p = DataProviders.getProvider(providerID);
                if (p != null) {
                    // TODO for now we only take one provider by type
                    if (p instanceof SensorProvider) {
                        smlProviderID = providerID;
                    }
                    // provider may implements the 2 interface
                    if (p instanceof ObservationProvider) {
                        omProvider = (ObservationProvider) p;
                    }
                } else {
                    startError("Unable to instanciate the provider:" + providerID, null);
                }
            }
        } catch (ConfigurationException ex) {
            startError(ex.getMessage(), ex);
        }
    }

    /**
     * Extract the transactional profile of the service.
     * 
     * @return {@code true} if the transactional profile is activated.
     */
    @Override
    protected boolean getTransactionalProperty() {
        // look into deprecated configuration attribute.
        if (configuration != null && "transactional".equals(configuration.getProfileValue())) {
            return true;
        }
        return super.getTransactionalProperty();
    }

    protected Phenomenon getPhenomenon(String phenName, String version) throws ConstellationStoreException {
        final FeatureQuery subquery = new FeatureQuery();
        final ResourceId filter = ff.resourceId(phenName);
        subquery.setSelection(filter);
        Collection<Phenomenon> sps = omProvider.getPhenomenon(subquery, Collections.singletonMap("version", version));
        if (sps.isEmpty()) {
            return null;
        } else {
            if (sps.size() > 1) {
                LOGGER.warning("Multiple phenomenon found for one identifier");
            }
            return sps.iterator().next();
        }
    }

    protected SamplingFeature getFeatureOfInterest(String featureName, String version) throws ConstellationStoreException {
        final FeatureQuery subquery = new FeatureQuery();
        final ResourceId filter = ff.resourceId(featureName);
        subquery.setSelection(filter);
        List<SamplingFeature> sps = omProvider.getFeatureOfInterest(subquery, Collections.singletonMap("version", version));
        if (sps.isEmpty()) {
            return null;
        } else {
            return sps.get(0);
        }
    }

    protected Process getProcess(String procName, String version) throws ConstellationStoreException {
        final FeatureQuery subquery = new FeatureQuery();
        final ResourceId filter = ff.resourceId(procName);
        subquery.setSelection(filter);
        Collection<Process> sps = omProvider.getProcedures(subquery, Collections.singletonMap("version", version));
        if (sps.isEmpty()) {
            return null;
        } else {
            return sps.iterator().next();
        }
    }

    protected SOSProviderCapabilities getProviderCapabilities() throws ConstellationStoreException {
        return omProvider.getCapabilities();
    }

    protected List<Process> getProcedureForOffering(String offname, String version) throws ConstellationStoreException {
        final FeatureQuery subquery = new FeatureQuery();
        final BinaryComparisonOperator filter = ff.equal(ff.property("offering"), ff.literal(offname));
        subquery.setSelection(filter);
        return omProvider.getProcedures(subquery, Collections.singletonMap("version", version));
    }

    protected boolean isLinkedSensor(String sensorId) {
        return sensorBusiness.isLinkedSensor(getServiceId(), sensorId);
    }
}
