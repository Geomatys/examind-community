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
import org.constellation.exception.ConfigurationException;
import org.constellation.provider.DataProvider;
import org.constellation.provider.DataProviders;
import org.constellation.provider.ObservationProvider;
import org.geotoolkit.gml.xml.v321.AbstractGeometryType;
import org.geotoolkit.observation.ObservationStore;
import org.geotoolkit.sensor.SensorStore;
import org.geotoolkit.sos.netcdf.ExtractionResult;
import org.geotoolkit.sos.netcdf.ExtractionResult.ProcedureTree;
import org.constellation.sos.ws.SOSUtils;
import org.geotoolkit.sos.netcdf.GeoSpatialBound;
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

    @RequestMapping(value="/SensorService/{id}/{schema}/build", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity buildDatasourceOM(final @PathVariable("id") Integer serviceId, final @PathVariable("schema") String schema) throws Exception {
        final AcknowlegementType ack;
        if (sensorServiceBusiness.buildDatasource(serviceId, schema)) {
            ack = AcknowlegementType.success("O&M datasource created");
        } else {
            ack = AcknowlegementType.failure("error while creating O&M datasource");
        }
        return new ResponseEntity(ack, OK);
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

    @RequestMapping(value="/SensorService/{id}/sensor/{sensorID}", method = DELETE, produces = APPLICATION_JSON_VALUE)
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

    @RequestMapping(value="/SensorService/{id}/sensor/{sensorID}", method = GET, produces = APPLICATION_JSON_VALUE)
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

    @RequestMapping(value="/SensorService/{id}/sensor/location/{sensorID}", method = PUT, consumes = APPLICATION_XML_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity updateSensorLocation(final @PathVariable("id") Integer serviceId, final @PathVariable("sensorID") String sensorID, final @RequestBody AbstractGeometryType location) throws Exception {
        AcknowlegementType response;
        if (sensorServiceBusiness.updateSensorLocation(serviceId, sensorID, location)) {
            response =  new AcknowlegementType("Success", "The sensor location have been updated in the Sensor service");
        } else {
            response =  new AcknowlegementType("Success", "The sensor location fail to be updated in the Sensor service");
        }
        return new ResponseEntity(response, OK);
    }

    @RequestMapping(value="/SensorService/{id}/sensor/location/{sensorID}", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getWKTSensorLocation(final @PathVariable("id") Integer serviceId, final @PathVariable("sensorID") String sensorID) throws Exception {
        return new ResponseEntity(sensorServiceBusiness.getWKTSensorLocation(serviceId, sensorID), OK);
    }

    @RequestMapping(value="/SensorService/{id}/observedProperty/identifiers/{sensorID}", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getObservedPropertiesForSensor(final @PathVariable("id") Integer serviceId, final @PathVariable("sensorID") String sensorID) throws Exception {
        return new ResponseEntity(sensorServiceBusiness.getObservedPropertiesForSensorId(serviceId, sensorID), OK);
    }

    @RequestMapping(value="/SensorService/{id}/time/{sensorID}", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getTimeForSensor(final @PathVariable("id") Integer serviceId, final @PathVariable("sensorID") String sensorID) throws Exception {
        return new ResponseEntity(sensorServiceBusiness.getTimeForSensorId(serviceId, sensorID), OK);
    }

    @RequestMapping(value="/SensorService/{id}/observations", method = POST, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getDecimatedObservations(final @PathVariable("id") Integer serviceId, final @RequestBody ObservationFilter filter) throws Exception {
        return new ResponseEntity(sensorServiceBusiness.getDecimatedObservationsCsv(serviceId, filter.getSensorID(), filter.getObservedProperty(), filter.getFoi(), filter.getStart(), filter.getEnd(), filter.getWidth()), OK);
    }

    @RequestMapping(value="/SensorService/{id}/observations/raw", method = POST, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getObservations(final @PathVariable("id") Integer serviceId, final @RequestBody ObservationFilter filter) throws Exception {
        return new ResponseEntity(sensorServiceBusiness.getObservationsCsv(serviceId, filter.getSensorID(), filter.getObservedProperty(), filter.getFoi(), filter.getStart(), filter.getEnd()), OK);
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

    @RequestMapping(value="/SensorService/{id}/observation/{observationID}", method = DELETE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity removeObservation(final @PathVariable("id") Integer serviceId, final @PathVariable("observationID") String observationID) throws Exception {
        AcknowlegementType response;
        if (sensorServiceBusiness.removeSingleObservation(serviceId, observationID)) {
            response = new AcknowlegementType("Success", "The specified observation have been removed from the Sensor service");
        } else {
            response = new AcknowlegementType("Failure", "The specified observation fail to be removed from the Sensor service");
        }
        return new ResponseEntity(response, OK);
    }

    @RequestMapping(value="/SensorService/{id}/observation/procedure/{procedureID}", method = DELETE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity removeObservationForProcedure(final @PathVariable("id") Integer serviceId, final @PathVariable("procedureID") String procedureID) throws Exception {
        AcknowlegementType response;
        if (sensorServiceBusiness.removeObservationForProcedure(serviceId, procedureID)) {
            response = new AcknowlegementType("Success", "The specified observations have been removed from the Sensor service");
        } else {
            response = new AcknowlegementType("Failure", "The specified observations fail to be removed from the Sensor service");
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
            final ObservationStore store = SOSUtils.getObservationStore(provider);
            final ExtractionResult result;
            if (store != null) {
                result = store.getResults();
            } else {
                return new ResponseEntity(new AcknowlegementType("Failure", "Available only on Observation provider (and netCDF coverage) for now"), OK);
            }

            // import in O&M database
            sensorServiceBusiness.importObservations(serviceId, result.observations, result.phenomenons);

            // SensorML generation
            for (ProcedureTree process : result.procedures) {
                sensorBusiness.generateSensorForData(dataID, toDto(process),  getSensorProviderId(serviceId), null);
                updateSensorLocation(serviceId, process);
            }

            return new ResponseEntity(new AcknowlegementType("Success", "The specified observations have been imported in the Sensor Service"), OK);
        } else {
            return new ResponseEntity(new AcknowlegementType("Failure", "The specified data does not exist"), OK);
        }
    }

    private void updateSensorLocation(final Integer serviceId, final ProcedureTree process) throws ConfigurationException {
        //record location
        final AbstractGeometryType geom = (AbstractGeometryType) process.spatialBound.getGeometry("2.0.0");
        if (geom != null) {
            sensorServiceBusiness.updateSensorLocation(serviceId, process.id, geom);
        }
        for (ProcedureTree child : process.children) {
            updateSensorLocation(serviceId, child);
        }
    }

    private org.constellation.dto.service.config.sos.ProcedureTree toDto(ExtractionResult.ProcedureTree pt) {
        GeoSpatialBound bound = pt.spatialBound;
        org.constellation.dto.service.config.sos.ProcedureTree result  = new org.constellation.dto.service.config.sos.ProcedureTree(pt.id,
                                                  pt.type,
                                                  bound.dateStart,
                                                  bound.dateEnd,
                                                  bound.minx,
                                                  bound.maxx,
                                                  bound.miny,
                                                  bound.maxy,
                                                  pt.fields);
        for (ExtractionResult.ProcedureTree child: pt.children) {
            result.getChildren().add(toDto(child));
        }
        return result;
    }

    @RequestMapping(value="/SensorService/{id}/data/{dataID}", method = DELETE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity removeDataFromSXS(final @PathVariable("id") Integer serviceId, final @PathVariable("dataID") Integer dataID) throws Exception {
        final Integer providerId = dataBusiness.getDataProvider(dataID);

        if (providerId != null) {
            final DataProvider provider = DataProviders.getProvider(providerId);
            final ObservationStore store = SOSUtils.getObservationStore(provider);
            final ExtractionResult result;
            if (store != null) {
                result = store.getResults();
            } else {
                return new ResponseEntity(new AcknowlegementType("Failure", "Available only on Observation provider (and netCDF coverage) for now"), OK);
            }

            // remove from O&M database
            for (Observation obs : result.observations) {
                if (obs.getName() != null) {
                    sensorServiceBusiness.removeSingleObservation(serviceId, obs.getName().getCode());
                }
            }

            return new ResponseEntity(new AcknowlegementType("Success", "The specified observations have been removed from the Sensor Service"), OK);
        } else {
            return new ResponseEntity(new AcknowlegementType("Failure", "The specified data does not exist"), OK);
        }
    }

    private void writeProcedures(final Integer id, final ProcedureTree process, final String parent) throws ConfigurationException {
        final AbstractGeometryType geom = (AbstractGeometryType) process.spatialBound.getGeometry("2.0.0");
        sensorServiceBusiness.writeProcedure(id, process.id, geom, parent, process.type);
        for (ProcedureTree child : process.children) {
            writeProcedures(id, child, process.id);
        }
    }

    @RequestMapping(value="/SensorService/{id}/sensor/import/{sensorID}", method = PUT, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity importSensor(final @PathVariable("id") Integer sid, final @PathVariable("sensorID") String sensorID) throws Exception {
        final Sensor sensor               = sensorBusiness.getSensor(sensorID);
        final List<Sensor> sensorChildren = sensorBusiness.getChildren(sensor.getIdentifier());
        final List<Integer> dataProviders = sensorBusiness.getLinkedDataProviderIds(sensor.getId());
        final List<String> sensorIds      = new ArrayList<>();

        sensorBusiness.addSensorToService(sid, sensor.getId());
        sensorIds.add(sensorID);

        //import sensor children
        for (Sensor child : sensorChildren) {
            dataProviders.addAll(sensorBusiness.getLinkedDataProviderIds(child.getId()));
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
                final ObservationStore store = (ObservationStore) ((ObservationProvider) provider).getMainStore();
                final ExtractionResult result;
                if (store != null) {
                    result = store.getResults(sensorID, sensorIds);
                } else {
                    return new ResponseEntity(new AcknowlegementType("Failure", "Available only on Observation provider (and netCDF coverage) for now"), OK);
                }

                // update sensor location
                for (ProcedureTree process : result.procedures) {
                    writeProcedures(sid, process, null);
                }

                // import in O&M database
                sensorServiceBusiness.importObservations(sid, result.observations, result.phenomenons);
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
            if(p.getMainStore() instanceof SensorStore){
                // TODO for now we only take one provider by type
                return providerID;
            }
        }
        throw new ConfigurationException("there is no sensor provider linked to this ID:" + serviceID);
    }
}
