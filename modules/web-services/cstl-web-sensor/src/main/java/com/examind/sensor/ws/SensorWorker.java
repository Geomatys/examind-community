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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import org.apache.sis.internal.storage.query.SimpleQuery;
import org.apache.sis.internal.system.DefaultFactories;
import org.constellation.api.ServiceDef;
import org.constellation.business.ISensorBusiness;
import org.constellation.dto.service.config.sos.Offering;
import org.constellation.dto.service.config.sos.SOSConfiguration;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.provider.DataProvider;
import org.constellation.provider.DataProviders;
import org.constellation.provider.ObservationProvider;
import org.constellation.provider.SensorProvider;
import static org.constellation.sos.ws.SOSUtils.BoundMatchEnvelope;
import static org.constellation.sos.ws.SOSUtils.getIDFromObject;
import static org.constellation.sos.ws.SOSUtils.samplingPointMatchEnvelope;
import org.constellation.ws.AbstractWorker;
import org.geotoolkit.filter.identity.DefaultFeatureId;
import org.geotoolkit.gml.xml.AbstractFeature;
import org.geotoolkit.gml.xml.Envelope;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.Id;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.geometry.primitive.Point;
import org.opengis.observation.Phenomenon;
import org.opengis.observation.Process;
import org.opengis.observation.sampling.SamplingFeature;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class SensorWorker extends AbstractWorker {

    /**
     * The sensor business
     */
    @Autowired
    protected ISensorBusiness sensorBusiness;

    protected SOSConfiguration configuration;

    /**
     * The sensorML provider identifier (to be removed)
     */
    protected Integer smlProviderID;

    /**
     * The Observation provider
     */
    protected ObservationProvider omProvider;

    /**
     * The profile of the SOS service (transational/discovery).
     */
    protected boolean isTransactionnal;

    protected final FilterFactory ff;

    public SensorWorker(final String id, final ServiceDef.Specification specification) {
        super(id, specification);
        this.ff = DefaultFactories.forBuildin(FilterFactory.class);
        try {
            final Object object = serviceBusiness.getConfiguration(specification.name().toLowerCase(), id);
            if (object instanceof SOSConfiguration) {
                configuration = (SOSConfiguration) object;
            } else {
                startError("The configuration object is malformed or null.", null);
                return;
            }

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

    @Override
    protected final String getProperty(final String propertyName) {
        if (configuration != null) {
            return configuration.getParameter(propertyName);
        }
        return null;
    }

    protected boolean getBooleanProperty(final String propertyName, boolean defaultValue) {
        if (configuration != null) {
            return configuration.getBooleanParameter(propertyName, defaultValue);
        }
        return defaultValue;
    }

    protected Phenomenon getPhenomenon(String phenName, String version) throws ConstellationStoreException {
        final SimpleQuery subquery = new SimpleQuery();
        final Id filter = ff.id(Collections.singleton(new DefaultFeatureId(phenName)));
        subquery.setFilter(filter);
        Collection<Phenomenon> sps = omProvider.getPhenomenon(subquery, Collections.singletonMap("version", version));
        if (sps.isEmpty()) {
            return null;
        } else {
            return sps.iterator().next();
        }
    }

    protected SamplingFeature getFeatureOfInterest(String featureName, String version) throws ConstellationStoreException {
        final SimpleQuery subquery = new SimpleQuery();
        final Id filter = ff.id(Collections.singleton(new DefaultFeatureId(featureName)));
        subquery.setFilter(filter);
        List<SamplingFeature> sps = omProvider.getFeatureOfInterest(subquery, Collections.singletonMap("version", version));
        if (sps.isEmpty()) {
            return null;
        } else {
            return sps.get(0);
        }
    }

    protected List<SamplingFeature> getFeaturesOfInterest(String version) throws ConstellationStoreException {
        final SimpleQuery subquery = new SimpleQuery();
        return omProvider.getFeatureOfInterest(subquery, Collections.singletonMap("version", version));
    }

    protected List<SamplingFeature> getFeaturesOfInterestForOffering(String offname, String version) throws ConstellationStoreException {
        final SimpleQuery subquery = new SimpleQuery();
        final PropertyIsEqualTo filter = ff.equals(ff.property("offering"), ff.literal(offname));
        subquery.setFilter(filter);
        return omProvider.getFeatureOfInterest(subquery, Collections.singletonMap("version", version));
    }

    protected List<String> getFeaturesOfInterestForBBOX(List<Offering> offerings, final Envelope e, String version) throws ConstellationStoreException {
        List<String> results = new ArrayList<>();
        for (Offering off : offerings) {
            results.addAll(getFeaturesOfInterestForBBOX(off.getId(), e, version));
        }
        return results;
    }

    protected List<String> getFeaturesOfInterestForBBOX(String offname, final Envelope e, String version) throws ConstellationStoreException {
        List<String> results = new ArrayList<>();
        final List<SamplingFeature> stations = new ArrayList<>();
        if (offname != null) {
            stations.addAll(getFeaturesOfInterestForOffering(offname, version));
        } else {
            stations.addAll(getFeaturesOfInterest(version));
        }
        for (SamplingFeature offStation : stations) {
            // TODO for SOS 2.0 use observed area
            final org.geotoolkit.sampling.xml.SamplingFeature station = (org.geotoolkit.sampling.xml.SamplingFeature) offStation;

            // should not happen
            if (station == null) {
                throw new ConstellationStoreException("the feature of interest is in offering list but not registered");
            }
            if (station.getGeometry() instanceof Point) {
                if (samplingPointMatchEnvelope((Point) station.getGeometry(), e)) {
                    results.add(getIDFromObject(station));
                } else {
                    LOGGER.log(Level.FINER, " the feature of interest {0} is not in the BBOX", getIDFromObject(station));
                }

            } else if (station instanceof AbstractFeature) {
                final AbstractFeature sc = (AbstractFeature) station;
                if (BoundMatchEnvelope(sc, e)) {
                    results.add(sc.getId());
                }
            } else {
                LOGGER.log(Level.WARNING, "unknow implementation:{0}", station.getClass().getName());
            }
        }
        return results;
    }

    protected List<Process> getProcedureForOffering(String offname, String version) throws ConstellationStoreException {
        final SimpleQuery subquery = new SimpleQuery();
        final PropertyIsEqualTo filter = ff.equals(ff.property("offering"), ff.literal(offname));
        subquery.setFilter(filter);
        return omProvider.getProcedures(subquery, Collections.singletonMap("version", version));
    }
}
