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

package org.constellation.sos.configuration;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKTWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import javax.xml.bind.JAXBException;
import org.apache.sis.storage.DataStoreException;
import org.constellation.api.CommonConstants;
import org.constellation.business.ISensorBusiness;
import org.constellation.dto.AcknowlegementType;
import org.constellation.exception.ConfigurationException;
import org.constellation.dto.service.Instance;
import org.constellation.dto.service.config.sos.SensorMLTree;
import org.constellation.dto.Sensor;
import org.constellation.ogc.configuration.OGCConfigurer;
import org.constellation.provider.DataProvider;
import org.constellation.provider.DataProviders;
import org.constellation.sos.ws.SOSUtils;
import org.geotoolkit.sensor.SensorStore;
import org.geotoolkit.gml.xml.AbstractGeometry;
import org.geotoolkit.gml.xml.v321.TimeInstantType;
import org.geotoolkit.gml.xml.v321.TimePeriodType;
import org.geotoolkit.observation.ObservationFilter;
import org.geotoolkit.nio.ZipUtilities;
import org.geotoolkit.observation.ObservationFilterReader;
import org.geotoolkit.observation.ObservationReader;
import org.geotoolkit.observation.ObservationStore;
import org.geotoolkit.observation.ObservationWriter;
import org.constellation.store.observation.db.SOSDatabaseObservationStore;
import org.constellation.ws.ISOSConfigurer;
import org.geotoolkit.observation.xml.AbstractObservation;
import org.geotoolkit.sml.xml.AbstractSensorML;
import org.geotoolkit.sml.xml.SensorMLUtilities;
import static org.geotoolkit.sml.xml.SensorMLUtilities.getSensorMLType;
import static org.geotoolkit.sml.xml.SensorMLUtilities.getSmlID;
import org.opengis.observation.Observation;
import org.opengis.observation.ObservationCollection;
import org.opengis.observation.Phenomenon;
import org.opengis.referencing.operation.TransformException;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.opengis.temporal.TemporalGeometricPrimitive;
import org.opengis.util.FactoryException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * {@link OGCConfigurer} implementation for SOS service.
 *
 * TODO: implement specific configuration methods
 *
 * @author Fabien Bernard (Geomatys).
 * @version 0.9
 * @since 0.9
 */
public class SOSConfigurer extends OGCConfigurer implements ISOSConfigurer {

    @Autowired
    private ISensorBusiness sensorBusiness;

    @Override
    public Instance getInstance(final Integer id) throws ConfigurationException {
        final Instance instance = super.getInstance(id);
        try {
            instance.setLayersNumber(getSensorIds(instance.getIdentifier()).size());
        } catch (ConfigurationException ex) {
            LOGGER.log(Level.WARNING, "Error while getting sensor count on SOS instance:" + id, ex);
        }
        return instance;
    }

    public AcknowlegementType importSensor(final String id, final Path sensorFile, final String type) throws ConfigurationException {
        LOGGER.info("Importing sensor");

        final List<Path> files;
        switch (type) {
            case "zip":
                try  {
                    files = ZipUtilities.unzip(sensorFile, null);
                } catch (IOException ex) {
                    throw new ConfigurationException(ex);
                }   break;

            case "xml":
                files = Arrays.asList(sensorFile);
                break;

            default:
                throw new ConfigurationException("Unexpected file extension, accepting zip or xml");
        }

        try {
            for (Path importedFile: files) {
                if (importedFile != null) {
                    final Object sensor = sensorBusiness.unmarshallSensor(importedFile);
                    if (sensor instanceof AbstractSensorML) {
                        final String sensorID = getSmlID((AbstractSensorML)sensor);
                        final String smlType = SensorMLUtilities.getSensorMLType((AbstractSensorML)sensor);
                        sensorBusiness.create(sensorID, smlType, null, sensor, System.currentTimeMillis(), getSensorProviderId(id));
                    } else {
                        throw new ConfigurationException("Only handle SensorML for now");
                    }
                } else {
                    throw new ConfigurationException("An imported file is null");
                }
            }
            return new AcknowlegementType("Success", "The specified sensor have been imported in the SOS");
        } catch (JAXBException ex) {
            LOGGER.log(Level.WARNING, "Exception while unmarshalling imported file", ex);
        } catch (IOException ex) {
            throw new ConfigurationException(ex);
        }
        return new AcknowlegementType("Error", "An error occurs during the process");
    }

    @Override
    public AcknowlegementType removeSensor(final String id, final String sensorID) throws ConfigurationException {
        final ObservationWriter omWriter = getObservationWriter(id);
        try {
            final SensorMLTree root = getSensorTree(id);
            final SensorMLTree tree = root.find(sensorID);

            // for a System sensor, we delete also his components
            final List<String> toRemove = new ArrayList<>();
            if (tree != null) {
                toRemove.addAll(tree.getAllChildrenIds());
            } else {
                // tree should no be null
                toRemove.add(sensorID);
            }
            for (String sid : toRemove) {
                sensorBusiness.removeSensorFromSOS(id, sid);
                omWriter.removeProcedure(sid);
            }

            // if the sensor has a System parent, we must update his component list
            if (tree != null && tree.getParent() != null) {
                final String parentID = tree.getParent().getIdentifier();
                if (!"root".equals(parentID)) {
                    final AbstractSensorML sml = (AbstractSensorML) sensorBusiness.getSensorMetadata(parentID);
                    SOSUtils.removeComponent(sml, sensorID);
                    sensorBusiness.updateSensorMetadata(parentID, sml);
                }
            }

            return new AcknowlegementType("Success", "The specified sensor have been removed in the SOS");
        } catch (DataStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public AcknowlegementType removeAllSensors(final String id) throws ConfigurationException {
        final ObservationWriter omWriter = getObservationWriter(id);
        try {
            final Collection<Sensor> sensors = sensorBusiness.getByServiceId(id);
            for (Sensor sensor : sensors) {
                sensorBusiness.removeSensorFromSOS(id, sensor.getIdentifier());
                boolean sucess = true; // TODO
                if (sucess) {
                    omWriter.removeProcedure(sensor.getIdentifier());
                } else {
                    return new AcknowlegementType("Error", "Unable to remove the sensor from SML datasource:" + sensor.getIdentifier());
                }
            }
            return new AcknowlegementType("Success", "The specified sensor have been removed in the SOS");
        } catch (DataStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public SensorMLTree getSensorTree(String id) throws ConfigurationException {
        final Collection<Sensor> sensors = sensorBusiness.getByServiceId(id);
        final List<SensorMLTree> values = new ArrayList<>();
        for (Sensor sensor : sensors) {
            final AbstractSensorML sml = (AbstractSensorML) sensorBusiness.getSensorMetadata(sensor.getIdentifier());
            final SensorMLTree t;
            if (sml != null) {
                final String smlType  = getSensorMLType(sml);
                final String smlID    = getSmlID(sml);
                t                     = new SensorMLTree(sensor.getId(), smlID, smlType, null, null);
                final List<SensorMLTree> children = SOSUtils.getChildren(sml);
                t.setChildren(children);
            } else {
                LOGGER.warning("Unable to retrieve Sensor Metadata for:" + sensor.getIdentifier());
                t = new SensorMLTree(sensor.getId(), sensor.getIdentifier(), null, null, null);
            }
            values.add(t);
        }
        return SensorMLTree.buildTree(values);
    }

    public Collection<String> getSensorIds(final String id) throws ConfigurationException {
        final ObservationReader reader = getObservationReader(id);
        try {
            return reader.getProcedureNames();
        } catch (DataStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public Collection<String> getSensorIdsForObservedProperty(final String id, final String observedProperty) throws ConfigurationException {
        final ObservationReader reader = getObservationReader(id);
        try {
            return reader.getProceduresForPhenomenon(observedProperty);
        } catch (DataStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public Collection<String> getObservedPropertiesForSensorId(final String id, final String sensorID) throws ConfigurationException {
        final ObservationReader reader = getObservationReader(id);
        try {
            final SensorMLTree root          = getSensorTree(id);
            final SensorMLTree current       = root.find(sensorID);
            return SOSUtils.getPhenomenonFromSensor(current, reader);
        } catch (DataStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public TemporalGeometricPrimitive getTimeForSensorId(final String id, final String sensorID) throws ConfigurationException {
        final ObservationReader reader = getObservationReader(id);
        try {
            return reader.getTimeForProcedure("2.0.0", sensorID);
        } catch (DataStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public AcknowlegementType importObservations(final String id, final Path observationFile) throws ConfigurationException {
        final ObservationWriter writer = getObservationWriter(id);
        try {
            final Object objectFile = SOSUtils.unmarshallObservationFile(observationFile);
            if (objectFile instanceof AbstractObservation) {
                writer.writeObservation((AbstractObservation)objectFile);
            } else if (objectFile instanceof ObservationCollection) {
                importObservations(id, (ObservationCollection)objectFile);
            } else {
                return new AcknowlegementType("Failure", "Unexpected object type for observation file");
            }
            return new AcknowlegementType("Success", "The specified observation have been imported in the SOS");
        } catch (IOException | JAXBException | DataStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public AcknowlegementType importObservations(final String id, final ObservationCollection collection) throws ConfigurationException {
        final ObservationWriter writer = getObservationWriter(id);
        try {
            final long start = System.currentTimeMillis();
            writer.writeObservations(collection.getMember());
            LOGGER.log(Level.INFO, "observations imported in :{0} ms", (System.currentTimeMillis() - start));
            return new AcknowlegementType("Success", "The specified observations have been imported in the SOS");
        } catch (DataStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public AcknowlegementType importObservations(final String id, final List<Observation> observations, final List<Phenomenon> phenomenons) throws ConfigurationException {
        final ObservationWriter writer = getObservationWriter(id);
        try {
            final long start = System.currentTimeMillis();
            writer.writePhenomenons(phenomenons);
            writer.writeObservations(observations);
            LOGGER.log(Level.INFO, "observations imported in :{0} ms", (System.currentTimeMillis() - start));
            return new AcknowlegementType("Success", "The specified observations have been imported in the SOS");
        } catch (DataStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public AcknowlegementType removeSingleObservation(final String id, final String observationID) throws ConfigurationException {
        final ObservationWriter writer = getObservationWriter(id);
        try {
            writer.removeObservation(observationID);
            return new AcknowlegementType("Success", "The specified observation have been removed from the SOS");
        } catch (DataStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public AcknowlegementType removeObservationForProcedure(final String id, final String procedureID) throws ConfigurationException {
        final ObservationWriter writer = getObservationWriter(id);
        try {
            writer.removeObservationForProcedure(procedureID);
            return new AcknowlegementType("Success", "The specified observations have been removed from the SOS");
        } catch (DataStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public Collection<String> getObservedPropertiesIds(String id) throws ConfigurationException {
        final ObservationReader reader = getObservationReader(id);
        try {
            return reader.getPhenomenonNames();
        } catch (DataStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public AcknowlegementType writeProcedure(final String id, final String sensorID, final AbstractGeometry location, final String parent, final String type) throws ConfigurationException {
        final ObservationWriter writer = getObservationWriter(id);
        try {
            writer.writeProcedure(sensorID, location, parent, type);
            return new AcknowlegementType("Success", "The sensor have been recorded in the SOS");
        } catch (DataStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public AcknowlegementType updateSensorLocation(final String id, final String sensorID, final AbstractGeometry location) throws ConfigurationException {
        final ObservationWriter writer = getObservationWriter(id);
        try {
            writer.recordProcedureLocation(sensorID, location);
            return new AcknowlegementType("Success", "The sensor location have been updated in the SOS");
        } catch (DataStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public String getWKTSensorLocation(final String id, final String sensorID) throws ConfigurationException {
        final ObservationReader reader = getObservationReader(id);
        try {
            final SensorMLTree root          = getSensorTree(id);
            final SensorMLTree current       = root.find(sensorID);
            if (current != null) {
                final List<Geometry> jtsGeometries = SOSUtils.getJTSGeometryFromSensor(current, reader);
                if (jtsGeometries.size() == 1) {
                    final WKTWriter writer = new WKTWriter();
                    return writer.write(jtsGeometries.get(0));
                } else if (!jtsGeometries.isEmpty()) {
                    final Geometry[] geometries   = jtsGeometries.toArray(new Geometry[jtsGeometries.size()]);
                    final GeometryCollection coll = new GeometryCollection(geometries, new GeometryFactory());
                    final WKTWriter writer        = new WKTWriter();
                    return writer.write(coll);
                }
            }
            return "";
        } catch (DataStoreException | FactoryException | TransformException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public String getObservationsCsv(final String id, final String sensorID, final List<String> observedProperties, final List<String> foi, final Date start, final Date end) throws ConfigurationException {
        final ObservationFilterReader filter = (ObservationFilterReader) getObservationFilter(id); // TODO handle ObservationFilter
        try {
            filter.initFilterGetResult(sensorID, CommonConstants.OBSERVATION_QNAME);
            if (observedProperties.isEmpty()) {
                observedProperties.addAll(getObservedPropertiesForSensorId(id, sensorID));
            }
            filter.setObservedProperties(observedProperties);
            filter.setFeatureOfInterest(foi);
            filter.setResponseFormat("text/csv");

            if (start != null && end != null) {
                final Period period = new TimePeriodType(new Timestamp(start.getTime()), new Timestamp(end.getTime()));
                filter.setTimeDuring(period);
            } else if (start != null) {
                final Instant time = new TimeInstantType(new Timestamp(start.getTime()));
                filter.setTimeAfter(time);
            } else if (end != null) {
                final Instant time = new TimeInstantType(new Timestamp(end.getTime()));
                filter.setTimeBefore(time);
            }
            return filter.getResults();

        } catch (DataStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public String getDecimatedObservationsCsv(final String id, final String sensorID, final List<String> observedProperties, final List<String> foi, final Date start, final Date end, final int width) throws ConfigurationException {
        final ObservationFilterReader filter = (ObservationFilterReader) getObservationFilter(id); // TODO handle ObservationFilter
        try {
            filter.initFilterGetResult(sensorID, CommonConstants.OBSERVATION_QNAME);
            if (observedProperties.isEmpty()) {
                observedProperties.addAll(getObservedPropertiesForSensorId(id, sensorID));
            }
            filter.setObservedProperties(observedProperties);
            filter.setFeatureOfInterest(foi);
            filter.setResponseFormat("text/csv");

            if (start != null && end != null) {
                final Period period = new TimePeriodType(new Timestamp(start.getTime()), new Timestamp(end.getTime()));
                filter.setTimeDuring(period);
            } else if (start != null) {
                final Instant time = new TimeInstantType(new Timestamp(start.getTime()));
                filter.setTimeAfter(time);
            } else if (end != null) {
                final Instant time = new TimeInstantType(new Timestamp(end.getTime()));
                filter.setTimeBefore(time);
            }
            return filter.getDecimatedResults(width);

        } catch (DataStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public boolean buildDatasource(final String serviceID, final String schemaPrefix) throws ConfigurationException {
        final DataProvider omProvider = getOMProvider(serviceID);
        if (omProvider != null && omProvider.getMainStore() instanceof SOSDatabaseObservationStore) {
            return true;
        }
        return false;
    }

    protected Integer getSensorProviderId(final String serviceID) throws ConfigurationException {
        final List<Integer> providers = serviceBusiness.getSOSLinkedProviders(serviceID);
        for (Integer providerID : providers) {
            final DataProvider p = DataProviders.getProvider(providerID);
            if(p.getMainStore() instanceof SensorStore){
                // TODO for now we only take one provider by type
                return providerID;
            }
        }
        throw new ConfigurationException("there is no sensor provider linked to this ID:" + serviceID);
    }

    protected DataProvider getOMProvider(final String serviceID) throws ConfigurationException {
        final List<Integer> providers = serviceBusiness.getSOSLinkedProviders(serviceID);
        for (Integer providerID : providers) {
            final DataProvider p = DataProviders.getProvider(providerID);
            if(p.getMainStore() instanceof ObservationStore){
                // TODO for now we only take one provider by type
                return p;
            }
        }
        throw new ConfigurationException("there is no OM provider linked to this ID:" + serviceID);
    }

    /**
     * Build a new Observation writer for the specified service ID.
     *
     * @param serviceID the service identifier (form multiple SOS) default: ""
     *
     * @return An observation Writer.
     * @throws ConfigurationException
     */
    protected ObservationWriter getObservationWriter(final String serviceID) throws ConfigurationException {
        final DataProvider omProvider = getOMProvider(serviceID);
        if (omProvider != null) {
            return getObservationStore(serviceID).getWriter();
        } else {
            throw new ConfigurationException("there is no configuration file correspounding to this ID:" + serviceID);
        }
    }

    /**
     * Build a new Observation writer for the specified service ID.
     *
     * @param serviceID the service identifier (form multiple SOS) default: ""
     *
     * @return An observation Writer.
     * @throws ConfigurationException
     */
    protected ObservationReader getObservationReader(final String serviceID) throws ConfigurationException {
        final DataProvider omProvider = getOMProvider(serviceID);
        if (omProvider != null) {
            return getObservationStore(serviceID).getReader();
        } else {
            throw new ConfigurationException("there is no configuration file correspounding to this ID:" + serviceID);
        }
    }

    /**
     * Build a new Observation writer for the specified service ID.
     *
     * @param serviceID the service identifier (form multiple SOS) default: ""
     *
     * @return An observation Writer.
     * @throws ConfigurationException
     */
    protected ObservationFilter getObservationFilter(final String serviceID) throws ConfigurationException {
        final DataProvider omProvider = getOMProvider(serviceID);
        if (omProvider != null) {
            return getObservationStore(serviceID).getFilter();
        } else {
            throw new ConfigurationException("there is no configuration file correspounding to this ID:" + serviceID);
        }
    }

    private ObservationStore getObservationStore(final String serviceID) throws ConfigurationException {
        final DataProvider omProvider = getOMProvider(serviceID);
        return SOSUtils.getObservationStore(omProvider);
    }
}
