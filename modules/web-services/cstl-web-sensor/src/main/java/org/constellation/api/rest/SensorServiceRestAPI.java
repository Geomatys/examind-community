/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2017 Geomatys.
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
package org.constellation.api.rest;

import com.examind.sensor.component.SensorServiceBusiness;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import org.constellation.business.IDataBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.ISensorBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.dto.Sensor;
import org.constellation.dto.AcknowlegementType;
import org.constellation.dto.service.config.sos.ObservationFilter;
import org.constellation.dto.SimpleValue;
import org.constellation.dto.service.config.sos.ExtractionResult;
import org.constellation.dto.service.config.sos.ProcedureTree;
import org.constellation.exception.ConfigurationException;
import org.constellation.provider.DataProvider;
import org.constellation.provider.DataProviders;
import org.constellation.provider.ObservationProvider;
import org.constellation.provider.SensorProvider;
import org.geotoolkit.gml.xml.AbstractGeometry;
import org.opengis.geometry.Geometry;
import org.opengis.observation.Observation;
import org.springframework.http.HttpStatus;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@RestController
public class SensorServiceRestAPI {

    @Inject
    private IProviderBusiness providerBusiness;

    @Inject
    private IDataBusiness dataBusiness;

    @Inject
    private IServiceBusiness serviceBusiness;

    @Inject
    private ISensorBusiness sensorBusiness;

    @Inject
    private SensorServiceBusiness sensorServiceBusiness;

    @RequestMapping(value="/SensorService/{id}/link/{providerID}", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity linkSXSProvider(final @PathVariable("id") Integer serviceId, final @PathVariable("providerID") String providerID) throws Exception {
        final Integer providerId = providerBusiness.getIDFromIdentifier(providerID);
        serviceBusiness.linkServiceAndProvider(serviceId, providerId);
        return new ResponseEntity(AcknowlegementType.success("Provider correctly linked"), OK);
    }

    @RequestMapping(value="/SensorService/{id}/sensors", method = PUT, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity importSensorMetadata(final @PathVariable("id") Integer serviceId, @RequestBody final File sensor) throws Exception {
        AcknowlegementType response;
        if (sensorServiceBusiness.importSensor(serviceId, sensor.toPath(), "xml")) {
            response = new AcknowlegementType("Success", "The specified sensor have been imported in the Sensor service");
        } else {
            response = new AcknowlegementType("Error", "An error occurs during the process");
        }
        return new ResponseEntity(response, OK);
    }

    @RequestMapping(value="/SensorService/{id}/sensor/{sensorID:.+}", method = DELETE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity removeSensor(final @PathVariable("id") Integer serviceId, final @PathVariable("sensorID") String sensorID) throws Exception {
         AcknowlegementType response;
        if (sensorServiceBusiness.removeSensor(serviceId, sensorID)) {
            response = new AcknowlegementType("Success", "The specified sensor have been removed from the Sensor service");
        } else {
            response = new AcknowlegementType("Error", "The specified sensor fail to be removed from the Sensor service");
        }
        return new ResponseEntity(response, OK);
    }

    @RequestMapping(value="/SensorService/{id}/sensors", method = DELETE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity removeAllSensor(final @PathVariable("id") Integer serviceId) throws Exception {
        AcknowlegementType response;
        if (sensorServiceBusiness.removeAllSensors(serviceId)) {
            response = new AcknowlegementType("Success", "The specified sensors have been removed in the Sensor service");
        } else {
            response = new AcknowlegementType("Error", "Unable to remove all the sensors from SML datasource.");
        }
        return new ResponseEntity(response, OK);
    }

    @RequestMapping(value="/SensorService/{id}/sensor/{sensorID:.+}", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getSensorMetadata(final @PathVariable("id") Integer serviceId, final @PathVariable("sensorID") String sensorID) throws Exception {
        if (sensorBusiness.isLinkedSensor(serviceId, sensorID)) {
            return new ResponseEntity(sensorBusiness.getSensorMetadata(sensorID), OK);
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value="/SensorService/{id}/sensors", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getSensorTree(final @PathVariable("id") Integer serviceId) throws Exception {
        return new ResponseEntity(sensorServiceBusiness.getSensorTree(serviceId), OK);
    }

    @RequestMapping(value="/SensorService/{id}/sensors/identifiers", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getSensorIds(final @PathVariable("id") Integer serviceId) throws Exception {
        return new ResponseEntity(sensorServiceBusiness.getSensorIds(serviceId), OK);
    }

    @RequestMapping(value="/SensorService/{id}/sensors/identifiers/id", method = POST, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getSensorIdsForObservedProperty(final @PathVariable("id") Integer serviceId, final @RequestParam("observedProperty") String observedProperty) throws Exception {
        return new ResponseEntity(sensorServiceBusiness.getSensorIdsForObservedProperty(serviceId, observedProperty), OK);
    }

    @RequestMapping(value="/SensorService/{id}/sensors/count", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getSensortCount(final @PathVariable("id") Integer serviceId) throws Exception {
        return new ResponseEntity(new SimpleValue(sensorBusiness.getCountByServiceId(serviceId)), OK);
    }

    @RequestMapping(value="/SensorService/{id}/sensor/location/{sensorID:.+}", method = PUT, consumes = APPLICATION_XML_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity updateSensorLocation(final @PathVariable("id") Integer serviceId, final @PathVariable("sensorID") String sensorID, final @RequestBody AbstractGeometry gmlLocation) throws Exception {
        AcknowlegementType response;
        Geometry location = null;
        if (gmlLocation instanceof Geometry) {
            location = (Geometry) gmlLocation;
            if (sensorServiceBusiness.updateSensorLocation(serviceId, sensorID, location)) {
                response =  new AcknowlegementType("Success", "The sensor location have been updated in the Sensor service");
            } else {
                response =  new AcknowlegementType("Success", "The sensor location fail to be updated in the Sensor service");
            }
        } else if (gmlLocation != null) {
            response =  new AcknowlegementType("Failure", "GML Geometry can not be casted as Opengis one: " + gmlLocation);
        } else {
            response =  new AcknowlegementType("Failure", "GML Geometry is null");
        }
        return new ResponseEntity(response, OK);
    }

    @RequestMapping(value="/SensorService/{id}/sensor/location/{sensorID:.+}", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getWKTSensorLocation(final @PathVariable("id") Integer serviceId, final @PathVariable("sensorID") String sensorID) throws Exception {
        return new ResponseEntity(sensorServiceBusiness.getWKTSensorLocation(serviceId, sensorID), OK);
    }

    @RequestMapping(value="/SensorService/{id}/observedProperty/identifiers/{sensorID:.+}", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getObservedPropertiesForSensor(final @PathVariable("id") Integer serviceId, final @PathVariable("sensorID") String sensorID) throws Exception {
        return new ResponseEntity(sensorServiceBusiness.getObservedPropertiesForSensorId(serviceId, sensorID, true), OK);
    }

    @RequestMapping(value="/SensorService/{id}/time/{sensorID:.+}", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getTimeForSensor(final @PathVariable("id") Integer serviceId, final @PathVariable("sensorID") String sensorID) throws Exception {
        return new ResponseEntity(sensorServiceBusiness.getTimeForSensorId(serviceId, sensorID), OK);
    }

    @RequestMapping(value="/SensorService/{id}/observations", method = POST, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getDecimatedObservations(final @PathVariable("id") Integer serviceId, final @RequestBody ObservationFilter filter) throws Exception {
        return new ResponseEntity(sensorServiceBusiness.getObservationsCsv(serviceId, filter.getSensorID(), filter.getObservedProperty(), filter.getFoi(), filter.getStart(), filter.getEnd(), filter.getWidth()), OK);
    }

    @RequestMapping(value="/SensorService/{id}/observations/raw", method = POST, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getObservations(final @PathVariable("id") Integer serviceId, final @RequestBody ObservationFilter filter) throws Exception {
        return new ResponseEntity(sensorServiceBusiness.getObservationsCsv(serviceId, filter.getSensorID(), filter.getObservedProperty(), filter.getFoi(), filter.getStart(), filter.getEnd(), null), OK);
    }

    @RequestMapping(value="/SensorService/{id}/observations", method = PUT, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity importObservation(final @PathVariable("id") Integer serviceId, final File obs) throws Exception {
        AcknowlegementType response;
        if (sensorServiceBusiness.importObservations(serviceId, obs.toPath())) {
            response = new AcknowlegementType("Success", "The specified observation have been imported in the Sensor service");
        } else {
            response = new AcknowlegementType("Failure", "Unexpected object type for observation file");
        }
        return new ResponseEntity(response, OK);
    }

    @RequestMapping(value="/SensorService/{id}/observation/{observationID:.+}", method = DELETE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity removeObservation(final @PathVariable("id") Integer serviceId, final @PathVariable("observationID") String observationID) throws Exception {
        AcknowlegementType response;
        if (sensorServiceBusiness.removeSingleObservation(serviceId, observationID)) {
            response = new AcknowlegementType("Success", "The specified observation have been removed from the Sensor service");
        } else {
            response = new AcknowlegementType("Failure", "The specified observation fail to be removed from the Sensor service");
        }
        return new ResponseEntity(response, OK);
    }

    @RequestMapping(value="/SensorService/{id}/observedProperties/identifiers", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getObservedPropertiesIds(final @PathVariable("id") Integer serviceId) throws Exception {
        return new ResponseEntity(sensorServiceBusiness.getObservedPropertiesIds(serviceId), OK);
    }

    @RequestMapping(value="/SensorService/{id}/data/{dataID}", method = PUT, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity importSensorFromData(final @PathVariable("id") Integer serviceId, final @PathVariable("dataID") Integer dataID) throws Exception {
        final Integer providerId = dataBusiness.getDataProvider(dataID);

        if (providerId != null) {

            final DataProvider provider = DataProviders.getProvider(providerId);
            if (provider instanceof ObservationProvider) {
                final ObservationProvider omProvider = (ObservationProvider) provider;
                final ExtractionResult result = omProvider.extractResults();

                // import in O&M database
                sensorServiceBusiness.importObservations(serviceId, result.getObservations(), result.getPhenomenons());

                // SensorML generation
                for (ProcedureTree process : result.getProcedures()) {
                    sensorBusiness.generateSensorForData(dataID, process,  getSensorProviderId(serviceId), null);
                    updateSensorLocation(serviceId, process);
                }
            } else {
                return new ResponseEntity(new AcknowlegementType("Failure", "Available only on Observation provider for now"), OK);
            }

            return new ResponseEntity(new AcknowlegementType("Success", "The specified observations have been imported in the Sensor Service"), OK);
        } else {
            return new ResponseEntity(new AcknowlegementType("Failure", "The specified data does not exist"), OK);
        }
    }

    private void updateSensorLocation(final Integer serviceId, final ProcedureTree process) throws ConfigurationException {
        //record location
        final Geometry geom = process.getGeom();
        if (geom != null) {
            sensorServiceBusiness.updateSensorLocation(serviceId, process.getId(), geom);
        }
        for (ProcedureTree child : process.getChildren()) {
            updateSensorLocation(serviceId, child);
        }
    }

    @RequestMapping(value="/SensorService/{id}/data/{dataID}", method = DELETE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity removeDataFromSXS(final @PathVariable("id") Integer serviceId, final @PathVariable("dataID") Integer dataID) throws Exception {
        final Integer providerId = dataBusiness.getDataProvider(dataID);

        if (providerId != null) {
            final DataProvider provider = DataProviders.getProvider(providerId);
            if (provider instanceof ObservationProvider) {
                final ObservationProvider omProvider = (ObservationProvider) provider;
                final ExtractionResult result = omProvider.extractResults();

                // remove from O&M database
                for (Observation obs : result.getObservations()) {
                    if (obs.getName() != null) {
                        sensorServiceBusiness.removeSingleObservation(serviceId, obs.getName().getCode());
                    }
                }
            } else {
                return new ResponseEntity(new AcknowlegementType("Failure", "Available only on Observation provider for now"), OK);
            }

            return new ResponseEntity(new AcknowlegementType("Success", "The specified observations have been removed from the Sensor Service"), OK);
        } else {
            return new ResponseEntity(new AcknowlegementType("Failure", "The specified data does not exist"), OK);
        }
    }

    @RequestMapping(value="/SensorService/{id}/sensor/import/{sensorID:.+}", method = PUT, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity importSensor(final @PathVariable("id") Integer sid, final @PathVariable("sensorID") String sensorID) throws Exception {
        final Sensor sensor               = sensorBusiness.getSensor(sensorID);
        final List<Sensor> sensorChildren = sensorBusiness.getChildren(sensor.getIdentifier());
        final Collection<String> previous = sensorServiceBusiness.getSensorIds(sid);
        final List<String> sensorIds      = new ArrayList<>();
        final List<Integer> dataProviders = new ArrayList<>();

        // hack for not importing already inserted sensor. must be reviewed when the SOS/STS service will share an O&M provider
        if (!previous.contains(sensorID)) {
            dataProviders.addAll(sensorBusiness.getLinkedDataProviderIds(sensor.getId()));
        }

        sensorBusiness.addSensorToService(sid, sensor.getId());
        sensorIds.add(sensorID);

        //import sensor children
        for (Sensor child : sensorChildren) {
            // hack for not importing already inserted sensor. must be reviewed when the SOS/STS service will share an O&M provider
            if (!previous.contains(child.getIdentifier())) {
                dataProviders.addAll(sensorBusiness.getLinkedDataProviderIds(child.getId()));
            }
            sensorBusiness.addSensorToService(sid, child.getId());
            sensorIds.add(child.getIdentifier());
        }

        // look for provider ids (remove doublon)
        final Set<Integer> providerIDs = new HashSet<>();
        for (Integer dataProvider : dataProviders) {
            providerIDs.add(dataProvider);
        }

        // import observations
        for (Integer providerId : providerIDs) {
            final DataProvider provider = DataProviders.getProvider(providerId);
            if (provider instanceof ObservationProvider) {
                final ObservationProvider omProvider = (ObservationProvider) provider;
                final ExtractionResult result = omProvider.extractResults(sensorID, sensorIds);

                // update sensor location
                for (ProcedureTree process : result.getProcedures()) {
                    sensorServiceBusiness.writeProcedure(sid, process);
                }

                // import in O&M database
                sensorServiceBusiness.importObservations(sid, result.getObservations(), result.getPhenomenons());
            } else {
                return new ResponseEntity(new AcknowlegementType("Failure", "Available only on Observation provider (and netCDF coverage) for now"), OK);
            }
        }
        return new ResponseEntity(new AcknowlegementType("Success", "The specified sensor has been imported in the Sensor Service"), OK);
    }

    private Integer getSensorProviderId(final Integer serviceID) throws ConfigurationException {
        final List<Integer> providers = serviceBusiness.getLinkedProviders(serviceID);
        for (Integer providerID : providers) {
            final DataProvider p = DataProviders.getProvider(providerID);
            if (p instanceof SensorProvider){
                // TODO for now we only take one provider by type
                return providerID;
            }
        }
        throw new ConfigurationException("there is no sensor provider linked to this ID:" + serviceID);
    }
}
