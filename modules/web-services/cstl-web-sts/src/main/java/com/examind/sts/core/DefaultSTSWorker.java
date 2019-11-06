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

package com.examind.sts.core;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.inject.Named;
import org.apache.sis.storage.DataStore;
import org.apache.sis.xml.MarshallerPool;
import org.constellation.api.ServiceDef;
import org.constellation.business.ISensorBusiness;
import org.constellation.dto.contact.Details;
import org.constellation.dto.service.config.sos.SOSConfiguration;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.constellation.provider.DataProvider;
import org.constellation.provider.DataProviders;
import org.constellation.provider.ObservationProvider;
import org.constellation.security.SecurityManagerHolder;
import org.constellation.ws.AbstractWorker;
import org.constellation.ws.CstlServiceException;
import org.constellation.ws.UnauthorizedException;
import org.geotoolkit.observation.ObservationStore;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import org.geotoolkit.sensor.SensorStore;
import org.geotoolkit.sts.GetDatastreams;
import org.geotoolkit.sts.GetFeatureOfInterests;
import org.geotoolkit.sts.GetHistoricalLocations;
import org.geotoolkit.sts.GetLocations;
import org.geotoolkit.sts.GetObservations;
import org.geotoolkit.sts.GetObservedProperties;
import org.geotoolkit.sts.GetSensors;
import org.geotoolkit.sts.GetThings;
import org.geotoolkit.sts.json.Datastream;
import org.geotoolkit.sts.json.DatastreamsResponse;
import org.geotoolkit.sts.json.FeatureOfInterest;
import org.geotoolkit.sts.json.FeatureOfInterestsResponse;
import org.geotoolkit.sts.json.HistoricalLocation;
import org.geotoolkit.sts.json.HistoricalLocationsResponse;
import org.geotoolkit.sts.json.Location;
import org.geotoolkit.sts.json.LocationsResponse;
import org.geotoolkit.sts.json.Observation;
import org.geotoolkit.sts.json.ObservationsResponse;
import org.geotoolkit.sts.json.ObservedPropertiesResponse;
import org.geotoolkit.sts.json.ObservedProperty;
import org.geotoolkit.sts.json.Sensor;
import org.geotoolkit.sts.json.SensorsResponse;
import org.geotoolkit.sts.json.Thing;
import org.geotoolkit.sts.json.ThingsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;


/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Named("IOTWorker")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DefaultSTSWorker extends AbstractWorker implements STSWorker {

    /**
     * The sensor business
     */
    @Autowired
    private ISensorBusiness sensorBusiness;

    /**
     * The sensorML provider identifier (to be removed)
     */
    private Integer smlProviderID;

    private boolean isTransactionnal;

    private SOSConfiguration configuration;

    /**
     * The Observation provider
     * TODO; find a way to remove the omStore calls
     */
    private ObservationStore omStore;
    private ObservationProvider omProvider;


    public DefaultSTSWorker(final String id) {
        super(id, ServiceDef.Specification.STS);
        isStarted = true;
        try {

            final Object object = serviceBusiness.getConfiguration("sts", id);
            if (object instanceof SOSConfiguration) {
                configuration = (SOSConfiguration) object;
            } else {
                startError("The configuration object is malformed or null.", null);
                return;
            }

            final String isTransactionnalProp = getProperty("transactional");
            if (isTransactionnalProp != null) {
                isTransactionnal = Boolean.parseBoolean(isTransactionnalProp);
            } else {
                boolean t = false;
                try {
                    final Details details = serviceBusiness.getInstanceDetails("sts", id, null);
                    t = details.isTransactional();
                } catch (ConstellationException ex) {
                    LOGGER.log(Level.WARNING, null, ex);
                }
                isTransactionnal = t;
            }

            final List<Integer> providers = serviceBusiness.getLinkedProviders(getServiceId());

            // we initialize the reader/writer
            for (Integer providerID: providers) {
                DataProvider p = DataProviders.getProvider(providerID);
                if (p != null) {
                    final DataStore store = p.getMainStore();
                    // TODO for now we only take one provider by type
                    if (store instanceof SensorStore) {
                        smlProviderID  = providerID;
                    }
                    // store may implements the 2 interface
                    if (store instanceof ObservationStore) {
                        omStore     = (ObservationStore)store;
                        omProvider  = (ObservationProvider) p;
                    }
                } else {
                    throw new CstlServiceException("Unable to instanciate the provider:" + providerID);
                }
            }

        } catch (CstlServiceException ex) {
            startError(ex.getMessage(), ex);
        } catch (ConfigurationException ex) {
            startError("The configuration file can't be found.", ex);
        }
        if (isStarted) {
            LOGGER.log(Level.INFO, "STS worker {0} running", id);
        }
    }

    private void startError(final String msg, final Exception ex) {
        startError    = msg;
        isStarted     = false;
        LOGGER.log(Level.WARNING, "\nThe STS worker is not running!\ncause: {0}", startError);
        if (ex != null) {
            LOGGER.log(Level.FINER, "\nThe STS worker is not running!", ex);
        }
    }

    @Override
    protected MarshallerPool getMarshallerPool() {
        // NO XML binding for this service
        return null;
    }

    @Override
    protected final String getProperty(final String propertyName) {
        if (configuration != null) {
            return configuration.getParameter(propertyName);
        }
        return null;
    }

    @Override
    public ThingsResponse getThings(GetThings req) throws CstlServiceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addThing(Thing thing) throws CstlServiceException {
        assertTransactionnal();
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ObservationsResponse getObservations(GetObservations req) throws CstlServiceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addObservation(Observation observation) throws CstlServiceException {
        assertTransactionnal();
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public DatastreamsResponse getDatastreams(GetDatastreams req) throws CstlServiceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addDatastream(Datastream datastream) throws CstlServiceException {
        assertTransactionnal();
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ObservedPropertiesResponse getObservedProperties(GetObservedProperties req) throws CstlServiceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addObservedProperty(ObservedProperty observedProperty) throws CstlServiceException {
        assertTransactionnal();
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public LocationsResponse getLocations(GetLocations req) throws CstlServiceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addLocation(Location location) throws CstlServiceException {
        assertTransactionnal();
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public SensorsResponse getSensors(GetSensors req) throws CstlServiceException {
        final List<Sensor> sensors = new ArrayList<>();
        return new SensorsResponse(sensors);
    }

    @Override
    public void addSensor(Sensor sensor) throws CstlServiceException {
        assertTransactionnal();
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public FeatureOfInterestsResponse getFeatureOfInterests(GetFeatureOfInterests req) throws CstlServiceException {
        List<FeatureOfInterest> values = new ArrayList<>();
        //omStore.getReader().getFeatureOfInterest(startError, startError)
        return new FeatureOfInterestsResponse(values);
    }

    @Override
    public void addFeatureOfInterest(FeatureOfInterest foi) throws CstlServiceException {
        assertTransactionnal();
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public HistoricalLocationsResponse getHistoricalLocations(GetHistoricalLocations req) throws CstlServiceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addHistoricalLocation(HistoricalLocation HistoricalLocation) throws CstlServiceException {
        assertTransactionnal();
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void assertTransactionnal() throws CstlServiceException {
        if (!isTransactionnal) {
            throw new CstlServiceException("The service is not transactionnal", INVALID_PARAMETER_VALUE, "request");
        }
        if (isTransactionSecurized() && !SecurityManagerHolder.getInstance().isAuthenticated()) {
            throw new UnauthorizedException("You must be authentified to perform a transactionnal request.");
        }
    }
}
