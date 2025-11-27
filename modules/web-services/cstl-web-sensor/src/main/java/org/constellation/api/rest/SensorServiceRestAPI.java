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

import java.io.File;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.http.HttpServletResponse;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.ISensorServiceBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.dto.AcknowlegementType;
import org.constellation.dto.service.config.sos.ObservationFilter;
import org.constellation.dto.SimpleValue;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.TargetNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import static org.springframework.http.HttpStatus.OK;
import org.springframework.http.MediaType;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
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
public class SensorServiceRestAPI extends AbstractRestAPI {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.api.rest");
    private static final MediaType TEXT_PLAIN_UTF8 = new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8);

    @Autowired
    private IProviderBusiness providerBusiness;

    @Autowired
    private IServiceBusiness serviceBusiness;

    @Autowired
    private ISensorServiceBusiness sensorServiceBusiness;

    @RequestMapping(value="/SensorService/{id}/link/{providerID}", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity linkSXSProvider(final @PathVariable("id") Integer serviceId, final @PathVariable("providerID") String providerID,
            @RequestParam(name = "fullLink", defaultValue = "true") boolean fullLink) throws Exception {
        if (readOnlyAPI) return readOnlyModeActivated();
        final Integer providerId = providerBusiness.getIDFromIdentifier(providerID);
        if (providerID == null) {
            return new ResponseEntity(AcknowlegementType.failure("Unable to find the provider"), OK);
        }
        try {
            serviceBusiness.ensureExistingInstance(serviceId);
        } catch (TargetNotFoundException ex) {
            return new ResponseEntity(AcknowlegementType.failure("Unable to find the service"), OK);
        }
        serviceBusiness.linkServiceAndSensorProvider(serviceId, providerId, fullLink);
        return new ResponseEntity(AcknowlegementType.success("Provider correctly linked"), OK);
    }

    @RequestMapping(value="/SensorService/{id}/sensors/generate", method = PUT, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity generateServiceSensors(final @PathVariable("id") Integer serviceId) throws Exception {
        if (readOnlyAPI) return readOnlyModeActivated();
        try {
            sensorServiceBusiness.generateSensorFromOMProvider(serviceId);
            return new ResponseEntity(new AcknowlegementType("Success", "The sensors have been generated."), OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error while generating sensor from OM provider", ex);
            return new ResponseEntity(AcknowlegementType.failure(ex.getMessage()), OK);
        }
    }

    @RequestMapping(value="/SensorService/{id}/sensors", method = PUT, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity importSensorMetadata(final @PathVariable("id") Integer serviceId, @RequestBody final File sensor) throws Exception {
        if (readOnlyAPI) return readOnlyModeActivated();
        AcknowlegementType response;
        if (sensorServiceBusiness.importSensor(serviceId, sensor.toPath(), "xml")) {
            response = new AcknowlegementType("Success", "The specified sensor have been imported in the Sensor service");
        } else {
            response = new AcknowlegementType("Error", "An error occurs during the process");
        }
        return new ResponseEntity(response, OK);
    }

    @RequestMapping(value="/SensorService/{id}/sensor/{sensorID:.+}", method = DELETE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity removeSensorFromService(final @PathVariable("id") Integer serviceId, final @PathVariable("sensorID") String sensorID) throws Exception {
        if (readOnlyAPI) return readOnlyModeActivated();
         AcknowlegementType response;
        if (sensorServiceBusiness.removeSensor(serviceId, sensorID)) {
            response = new AcknowlegementType("Success", "The specified sensor have been removed from the Sensor service");
        } else {
            response = new AcknowlegementType("Error", "The specified sensor fail to be removed from the Sensor service");
        }
        return new ResponseEntity(response, OK);
    }

    @RequestMapping(value="/SensorService/{id}/sensors", method = DELETE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity removeAllSensorFromService(final @PathVariable("id") Integer serviceId) throws Exception {
        if (readOnlyAPI) return readOnlyModeActivated();
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
        Object sm = sensorServiceBusiness.getSensorMetadata(serviceId, sensorID);
        if (sm != null) {
            return new ResponseEntity(sm, OK);
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value="/SensorService/{id}/sensors", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getSensorTree(final @PathVariable("id") Integer serviceId) throws Exception {
        return new ResponseEntity(sensorServiceBusiness.getServiceSensorMLTree(serviceId), OK);
    }

    @RequestMapping(value="/SensorService/{id}/sensors/identifiers", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getSensorIds(final @PathVariable("id") Integer serviceId) throws Exception {
        return new ResponseEntity(sensorServiceBusiness.getSensorIds(serviceId), OK);
    }

    @RequestMapping(value="/SensorService/{id}/sensors/count", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getSensortCount(final @PathVariable("id") Integer serviceId) throws Exception {
        return new ResponseEntity(new SimpleValue(sensorServiceBusiness.getSensorCount(serviceId)), OK);
    }

    @RequestMapping(value="/SensorService/{id}/sensor/location/{sensorID:.+}", method = GET, produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getWKTSensorLocation(final @PathVariable("id") Integer serviceId, final @PathVariable("sensorID") String sensorID) throws Exception {
        String sensorWkt = sensorServiceBusiness.getWKTSensorLocation(serviceId, sensorID);
        return ResponseEntity.ok()
                             .contentType(TEXT_PLAIN_UTF8)
                             .body(sensorWkt);
    }

    @RequestMapping(value="/SensorService/{id}/observedProperty/identifiers/{sensorID:.+}", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getObservedPropertiesForSensor(final @PathVariable("id") Integer serviceId, final @PathVariable("sensorID") String sensorID) throws Exception {
        return new ResponseEntity(sensorServiceBusiness.getObservedPropertiesForSensorId(serviceId, sensorID, true), OK);
    }

    @RequestMapping(value="/SensorService/{id}/time/{sensorID:.+}", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getTimeForSensor(final @PathVariable("id") Integer serviceId, final @PathVariable("sensorID") String sensorID) throws Exception {
        return new ResponseEntity(sensorServiceBusiness.getTimeForSensorId(serviceId, sensorID), OK);
    }

    @RequestMapping(value="/SensorService/{id}/observations", method = POST)
    public ResponseEntity<?> getDecimatedObservations(final @PathVariable("id") Integer serviceId, final @RequestBody ObservationFilter filter) throws Exception {
        Object results = sensorServiceBusiness.getResultsCsv(serviceId, filter.getSensorID(), filter.getObservedProperty(), filter.getFoi(), filter.getStart(), filter.getEnd(), filter.getWidth(), "text/csv", false, false);
        if (results instanceof String strResults) {
            return ResponseEntity.ok()
                                 .contentType(TEXT_PLAIN_UTF8)
                                 .body(strResults);
        } else {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(results);
        }
    }

    @RequestMapping(value="/SensorService/{id}/observations/raw", method = POST)
    public ResponseEntity getObservations(final @PathVariable("id") Integer serviceId, final @RequestBody ObservationFilter filter) throws Exception {
        Object results = sensorServiceBusiness.getResultsCsv(serviceId, filter.getSensorID(), filter.getObservedProperty(), filter.getFoi(), filter.getStart(), filter.getEnd(), null, "text/csv", false, false);
        if (results instanceof String strResults) {
            return ResponseEntity.ok().contentType(TEXT_PLAIN_UTF8).body(strResults);
        } else {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(results);
        }
    }

    @RequestMapping(value="/SensorService/{id}/observedProperties/identifiers", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getObservedPropertiesIds(final @PathVariable("id") Integer serviceId) throws Exception {
        return new ResponseEntity(sensorServiceBusiness.getObservedPropertiesIds(serviceId), OK);
    }

    @RequestMapping(value="/SensorService/{id}/data/{dataID}", method = PUT, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity importSensorFromData(final @PathVariable("id") Integer serviceId, final @PathVariable("dataID") Integer dataID) {
        if (readOnlyAPI) return readOnlyModeActivated();
        try {
            sensorServiceBusiness.importObservationsFromData(serviceId, dataID);
            return new ResponseEntity(new AcknowlegementType("Success", "The specified observations have been imported in the Sensor Service"), OK);
        } catch (ConstellationException ex) {
            return new ResponseEntity(new AcknowlegementType("Failure", ex.getMessage()), OK);
        }
    }

    @RequestMapping(value="/SensorService/{id}/data/{dataID}", method = DELETE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity removeDataFromService(final @PathVariable("id") Integer serviceId, final @PathVariable("dataID") Integer dataID) {
        if (readOnlyAPI) return readOnlyModeActivated();
        try {
            sensorServiceBusiness.removeDataObservationsFromService(serviceId, dataID);
            return new ResponseEntity(new AcknowlegementType("Success", "The specified observations have been removed from the Sensor Service"), OK);
        } catch (ConstellationException ex) {
            return new ResponseEntity(new AcknowlegementType("Failure", "The specified data does not exist"), OK);
        }
    }

    @RequestMapping(value="/SensorService/{id}/sensor/import/{sensorID:.+}", method = PUT, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity addSensorInservice(final @PathVariable("id") Integer sid, final @PathVariable("sensorID") String sensorID) throws Exception {
        if (readOnlyAPI) return readOnlyModeActivated();
        boolean success = sensorServiceBusiness.importSensor(sid, sensorID);
        if (success) {
            return new ResponseEntity(new AcknowlegementType("Failure", "Available only on Observation provider (and netCDF coverage) for now"), OK);
        }
        return new ResponseEntity(new AcknowlegementType("Success", "The specified sensor has been imported in the Sensor Service"), OK);
    }
}
