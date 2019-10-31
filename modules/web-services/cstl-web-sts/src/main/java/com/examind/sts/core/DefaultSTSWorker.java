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

import java.util.logging.Level;
import javax.inject.Named;
import org.apache.sis.xml.MarshallerPool;
import org.constellation.api.ServiceDef;
import org.constellation.dto.contact.Details;
import org.constellation.dto.service.config.sos.SOSConfiguration;
import org.constellation.exception.ConstellationException;
import org.constellation.security.SecurityManagerHolder;
import org.constellation.ws.AbstractWorker;
import org.constellation.ws.CstlServiceException;
import org.constellation.ws.UnauthorizedException;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
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
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;


/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Named("IOTWorker")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DefaultSTSWorker extends AbstractWorker implements STSWorker {

    private final boolean isTransactionnal;

    private SOSConfiguration configuration;

    public DefaultSTSWorker(final String id) {
        super(id, ServiceDef.Specification.STS);
        if (isStarted) {
            LOGGER.log(Level.INFO, "STS worker {0} running", id);
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
    public ThingsResponse getThings() throws CstlServiceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addThing(Thing thing) throws CstlServiceException {
        assertTransactionnal();
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ObservationsResponse getObservations() throws CstlServiceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addObservation(Observation observation) throws CstlServiceException {
        assertTransactionnal();
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public DatastreamsResponse getDatastreams() throws CstlServiceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addDatastream(Datastream datastream) throws CstlServiceException {
        assertTransactionnal();
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ObservedPropertiesResponse getObservedProperties() throws CstlServiceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addObservedProperty(ObservedProperty observedProperty) throws CstlServiceException {
        assertTransactionnal();
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public LocationsResponse getLocations() throws CstlServiceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addLocation(Location location) throws CstlServiceException {
        assertTransactionnal();
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public SensorsResponse getSensors() throws CstlServiceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addSensor(Sensor sensor) throws CstlServiceException {
        assertTransactionnal();
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public FeatureOfInterestsResponse getFeatureOfInterests() throws CstlServiceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addFeatureOfInterest(FeatureOfInterest foi) throws CstlServiceException {
        assertTransactionnal();
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public HistoricalLocationsResponse getHistoricalLocations() throws CstlServiceException {
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
