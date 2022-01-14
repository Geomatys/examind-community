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
package com.examind.sensor.component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;
import org.apache.sis.storage.FeatureQuery;
import org.constellation.business.ISensorBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.dto.Sensor;
import org.constellation.dto.service.config.sos.SensorMLTree;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.provider.DataProvider;
import org.constellation.provider.DataProviders;
import org.constellation.provider.ObservationProvider;
import org.constellation.provider.SensorProvider;
import com.examind.sensor.ws.SensorUtils;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import org.constellation.api.CommonConstants;
import static org.constellation.api.CommonConstants.OBJECT_TYPE;
import static org.constellation.api.CommonConstants.PROCEDURE;
import org.constellation.dto.service.config.sos.ProcedureTree;
import org.constellation.exception.ConstellationException;
import org.constellation.util.NamedId;
import org.geotoolkit.filter.FilterUtilities;
import org.geotoolkit.gml.xml.v321.TimeInstantType;
import org.geotoolkit.gml.xml.v321.TimePeriodType;
import org.geotoolkit.nio.ZipUtilities;
import org.geotoolkit.sml.xml.AbstractSensorML;
import org.geotoolkit.sml.xml.SensorMLUtilities;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKTWriter;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.observation.Process;
import org.opengis.observation.Observation;
import org.opengis.observation.ObservationCollection;
import org.opengis.observation.Phenomenon;
import org.opengis.referencing.operation.TransformException;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.opengis.temporal.TemporalGeometricPrimitive;
import org.opengis.util.FactoryException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component
public class SensorServiceBusiness {

    private static final Logger LOGGER = Logger.getLogger("com.examind.sensor.component");

    @Autowired
    protected ISensorBusiness sensorBusiness;

    @Autowired
    protected IServiceBusiness serviceBusiness;

    protected final FilterFactory ff = FilterUtilities.FF;

    public boolean importSensor(final Integer serviceID, final Path sensorFile, final String type) throws ConfigurationException {
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
            int smlProviderId = getSensorProviderId(serviceID);
            for (Path importedFile: files) {
                if (importedFile != null) {
                    final Object sensor = sensorBusiness.unmarshallSensor(importedFile);
                    if (sensor instanceof AbstractSensorML) {
                        final AbstractSensorML sml        = (AbstractSensorML) sensor;
                        final String sensorID             = SensorMLUtilities.getSmlID(sml);
                        final String smlType              = SensorMLUtilities.getSensorMLType(sml);
                        final String omType               = SensorMLUtilities.getOMType(sml);
                        final String name                 = sensorID; // TODO extract from sml
                        final String description          = null; // TODO extract from sml
                        final List<SensorMLTree> children = SensorUtils.getChildren(sml);
                        sensorBusiness.create(sensorID, name, description, smlType, omType, null, sensor, System.currentTimeMillis(), smlProviderId);
                        for (SensorMLTree child : children) {
                            importSensorChild(child, sensorID, smlProviderId);
                        }
                    } else {
                        throw new ConfigurationException("Only handle SensorML for now");
                    }
                } else {
                    throw new ConfigurationException("An imported file is null");
                }
            }
            return true;
        } catch (ConstellationException ex) {
            LOGGER.log(Level.WARNING, "Exception while unmarshalling imported file", ex);
        }
        return false;
    }

    private void importSensorChild(SensorMLTree sensor, String parentId, int providerId) throws ConfigurationException {
        sensorBusiness.create(sensor.getIdentifier(), sensor.getName(), sensor.getDescription(), sensor.getType(), null, parentId, sensor.getSml(), System.currentTimeMillis(), providerId);
        for (SensorMLTree child : sensor.getChildren()) {
            importSensorChild(child, sensor.getIdentifier(), providerId);
        }
    }

    public boolean removeSensor(final Integer id, final String sensorID) throws ConstellationException {
        final ObservationProvider pr = getOMProvider(id);
        try {
            final SensorMLTree tree = sensorBusiness.getSensorMLTree(sensorID);

            // for a System sensor, we delete also his components
            final List<NamedId> toRemove = new ArrayList<>();
            if (tree != null) {
                toRemove.addAll(tree.getAllChildrenNamedIds());
            } else {
                // tree should no be null
                toRemove.add(new NamedId(-1, sensorID));
            }
            final String datasourceKey = pr.getDatasourceKey();
            for (NamedId sid : toRemove) {
                sensorBusiness.removeSensorFromService(id, sid.getId());
               /*
                * if another service, using the SAME datasource, use this sensor, we don't remove it from the datasource
                */
                boolean rmFromDatasource = true;
                List<Integer> serviceIds = sensorBusiness.getLinkedServiceIds(sid.getId());
                for (Integer serviceId : serviceIds) {
                    ObservationProvider servProv  = getOMProvider(serviceId);
                    if (servProv.getDatasourceKey().equals(datasourceKey)) {
                        rmFromDatasource = false;
                        break;
                    }
                }
                if (rmFromDatasource) {
                    pr.removeProcedure(sid.getIdentifier());
                }
            }

            // if the sensor has a System parent, we must update his component list
            if (tree != null && tree.getParent() != null) {
                final String parentID = tree.getParent().getIdentifier();
                if (!"root".equals(parentID)) {
                    try {
                        final AbstractSensorML sml = (AbstractSensorML) sensorBusiness.getSensorMetadata(parentID);
                        SensorUtils.removeComponent(sml, sensorID);
                        sensorBusiness.updateSensorMetadata(parentID, sml);
                    } catch (Exception ex) {
                        LOGGER.warning("Unable to read/update parent sensor metadata");
                    }
                }
            }
            return true;
        } catch (ConstellationStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public boolean removeAllSensors(final Integer id) throws ConfigurationException {
        final ObservationProvider pr = getOMProvider(id);
        try {
            final Collection<Sensor> sensors = sensorBusiness.getByServiceId(id);
            for (Sensor sensor : sensors) {
                sensorBusiness.removeSensorFromService(id, sensor.getId());
                pr.removeProcedure(sensor.getIdentifier());
            }
            serviceBusiness.restart(id);
            return true;
        } catch (ConstellationStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public Collection<String> getSensorIds(final Integer id) throws ConfigurationException {
        final ObservationProvider pr = getOMProvider(id);
        try {
            return pr.getProcedureNames(null, Collections.EMPTY_MAP);
        } catch (ConstellationStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public long getSensorCount(final Integer id) throws ConfigurationException {
        final ObservationProvider pr = getOMProvider(id);
        try {
            return pr.getCount(null, Collections.singletonMap(OBJECT_TYPE, PROCEDURE));
        } catch (ConstellationStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public Collection<String> getSensorIdsForObservedProperty(final Integer id, final String observedProperty) throws ConfigurationException {
        final ObservationProvider pr = getOMProvider(id);
        try {
            FeatureQuery query = new FeatureQuery();
            final FilterFactory ff = FilterUtilities.FF;
            query.setSelection(ff.equal(ff.property("observedProperty"), ff.literal(observedProperty)));
            List<Process> processes = pr.getProcedures(query, Collections.emptyMap());
            List<String> results = new ArrayList<>();
            processes.forEach(p -> results.add(((org.geotoolkit.observation.xml.Process)p).getHref()));
            return results;
        } catch (ConstellationStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public Collection<String> getObservedPropertiesForSensorId(final Integer serviceId, final String sensorID, final boolean decompose) throws ConfigurationException {
        final ObservationProvider pr = getOMProvider(serviceId);
        try {
            final SensorMLTree current = sensorBusiness.getSensorMLTree(sensorID);
            if (current != null) {
                return SensorUtils.getPhenomenonFromSensor(current, pr, decompose);
            } else {
                return SensorUtils.getPhenomenonFromSensor(sensorID, pr, decompose);
            }
        } catch (ConstellationStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public TemporalGeometricPrimitive getTimeForSensorId(final Integer id, final String sensorID) throws ConfigurationException {
        final ObservationProvider pr = getOMProvider(id);
        try {
            return pr.getTimeForProcedure("2.0.0", sensorID);
        } catch (ConstellationStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public boolean importObservations(final Integer id, final Path observationFile) throws ConfigurationException {
        final ObservationProvider writer = getOMProvider(id);
        try {
            final Object objectFile = SensorUtils.unmarshallObservationFile(observationFile);
            if (objectFile instanceof Observation) {
                writer.writeObservation((Observation)objectFile);
            } else if (objectFile instanceof ObservationCollection) {
                importObservations(id, (ObservationCollection)objectFile);
            } else {
                return false;
            }
            return true;
        } catch (IOException | JAXBException | ConstellationStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public void importObservations(final Integer id, final ObservationCollection collection) throws ConfigurationException {
        final ObservationProvider writer = getOMProvider(id);
        try {
            final long start = System.currentTimeMillis();
            for (Observation obs : collection.getMember()) {
                writer.writeObservation(obs);
            }
            LOGGER.log(Level.INFO, "observations imported in :{0} ms", (System.currentTimeMillis() - start));
        } catch (ConstellationStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public void importObservations(final Integer id, final List<Observation> observations, final List<Phenomenon> phenomenons) throws ConfigurationException {
        final ObservationProvider writer = getOMProvider(id);
        try {
            final long start = System.currentTimeMillis();
            writer.writePhenomenons(phenomenons);
            for (Observation obs : observations) {
                writer.writeObservation(obs);
            }
            LOGGER.log(Level.INFO, "observations imported in :{0} ms", (System.currentTimeMillis() - start));
        } catch (ConstellationStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public boolean removeSingleObservation(final Integer id, final String observationID) throws ConfigurationException {
        final ObservationProvider pr = getOMProvider(id);
        try {
            pr.removeObservation(observationID);
            return true;
        } catch (ConstellationStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public Collection<String> getObservedPropertiesIds(Integer id) throws ConfigurationException {
        final ObservationProvider pr = getOMProvider(id);
        try {
            return pr.getPhenomenonNames(null, Collections.EMPTY_MAP);
        } catch (ConstellationStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public void writeProcedure(final Integer id, final ProcedureTree procedure) throws ConfigurationException {
        final ObservationProvider pr = getOMProvider(id);
        try {
            pr.writeProcedure(procedure);
        } catch (ConstellationStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public boolean updateSensorLocation(final Integer serviceId, final String sensorID, final org.opengis.geometry.Geometry location) throws ConfigurationException {
        final ObservationProvider pr = getOMProvider(serviceId);
        try {
            pr.updateProcedureLocation(sensorID, location);
            return true;
        } catch (ConstellationStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public String getWKTSensorLocation(final Integer id, final String sensorID) throws ConfigurationException {
        final ObservationProvider provider = getOMProvider(id);
        try {
            final SensorMLTree current = sensorBusiness.getSensorMLTree(sensorID);
            if (current != null) {
                final List<Geometry> jtsGeometries = SensorUtils.getJTSGeometryFromSensor(current, provider);
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
        } catch (ConstellationStoreException | FactoryException | TransformException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public Object getResultsCsv(final Integer id, final String sensorID, final List<String> observedProperties, final List<String> foi, final Date start, final Date end, final Integer width, final String resultFormat) throws ConfigurationException {
        try {
            final ObservationProvider pr = getOMProvider(id);
            FeatureQuery query = new FeatureQuery();
            query.setSelection(buildFilter(start, end, observedProperties, foi));
            Map<String, Object> hints = new HashMap<>();
            if (width != null) {
                hints.put("decimSize", Integer.toString(width));
            }
            return pr.getResults(sensorID, CommonConstants.OBSERVATION_QNAME, "inline", query, resultFormat, hints);
        } catch (ConstellationStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    private Filter buildFilter(final Date start, final Date end, List<String> observedProperties, List<String> featuresOfInterest)  {
        final List<Filter> filters = new ArrayList<>();
        if (start != null && end != null) {
            final Period period = new TimePeriodType(null, new Timestamp(start.getTime()), new Timestamp(end.getTime()));
            filters.add(ff.during(ff.property("resultTime"), ff.literal(period)));
        } else if (start != null) {
            final Instant time = new TimeInstantType(new Timestamp(start.getTime()));
            filters.add(ff.after(ff.property("resultTime"), ff.literal(time)));
        } else if (end != null) {
            final Instant time = new TimeInstantType(new Timestamp(end.getTime()));
            filters.add(ff.before(ff.property("resultTime"), ff.literal(time)));
        }

        if (observedProperties != null) {
            for (String observedProperty : observedProperties) {
                filters.add(ff.equal(ff.property("observedProperty"), ff.literal(observedProperty)));
            }
        }
        if (featuresOfInterest != null) {
            for (String featureOfInterest : featuresOfInterest) {
                filters.add(ff.equal(ff.property("featureOfInterest"), ff.literal(featureOfInterest)));
            }
        }
        if (filters.size() == 1) {
            return filters.get(0);
        } else if (filters.size() > 1) {
            return ff.and(filters);
        } else {
            return Filter.include();
        }
    }

    protected Integer getSensorProviderId(final Integer serviceID) throws ConfigurationException {
        final List<Integer> providers = serviceBusiness.getLinkedProviders(serviceID);
        for (Integer providerID : providers) {
            final DataProvider p = DataProviders.getProvider(providerID);
            if(p instanceof SensorProvider){
                // TODO for now we only take one provider by type
                return providerID;
            }
        }
        throw new ConfigurationException("there is no sensor provider linked to this ID:" + serviceID);
    }

    protected ObservationProvider getOMProvider(final Integer serviceID) throws ConfigurationException {
        final List<Integer> providers = serviceBusiness.getLinkedProviders(serviceID);
        for (Integer providerID : providers) {
            final DataProvider p = DataProviders.getProvider(providerID);
            if(p instanceof ObservationProvider){
                // TODO for now we only take one provider by type
                return (ObservationProvider) p;
            }
        }
        throw new ConfigurationException("there is no OM provider linked to this ID:" + serviceID);
    }
}
