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
import org.apache.sis.internal.storage.query.SimpleQuery;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.util.logging.Logging;
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
import org.constellation.dto.service.config.sos.ProcedureTree;
import org.constellation.util.NamedId;
import org.constellation.ws.CstlServiceException;
import org.geotoolkit.gml.xml.v321.TimeInstantType;
import org.geotoolkit.gml.xml.v321.TimePeriodType;
import org.geotoolkit.nio.ZipUtilities;
import static org.geotoolkit.ows.xml.OWSExceptionCode.OPERATION_NOT_SUPPORTED;
import org.geotoolkit.sml.xml.AbstractSensorML;
import org.geotoolkit.sml.xml.SensorMLUtilities;
import static org.geotoolkit.sml.xml.SensorMLUtilities.getSensorMLType;
import static org.geotoolkit.sml.xml.SensorMLUtilities.getSmlID;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKTWriter;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.spatial.BBOX;
import org.opengis.filter.temporal.After;
import org.opengis.filter.temporal.Before;
import org.opengis.filter.temporal.Begins;
import org.opengis.filter.temporal.BegunBy;
import org.opengis.filter.temporal.During;
import org.opengis.filter.temporal.EndedBy;
import org.opengis.filter.temporal.Ends;
import org.opengis.filter.temporal.Meets;
import org.opengis.filter.temporal.OverlappedBy;
import org.opengis.filter.temporal.TContains;
import org.opengis.filter.temporal.TEquals;
import org.opengis.filter.temporal.TOverlaps;
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

    private static final Logger LOGGER = Logging.getLogger("com.examind.sensor.component");

    @Autowired
    protected ISensorBusiness sensorBusiness;

    @Autowired
    protected IServiceBusiness serviceBusiness;
    
    protected final FilterFactory ff = DefaultFactories.forBuildin(FilterFactory.class);

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
            for (Path importedFile: files) {
                if (importedFile != null) {
                    final Object sensor = sensorBusiness.unmarshallSensor(importedFile);
                    if (sensor instanceof AbstractSensorML) {
                        final String sensorID = getSmlID((AbstractSensorML)sensor);
                        final String smlType = SensorMLUtilities.getSensorMLType((AbstractSensorML)sensor);
                        sensorBusiness.create(sensorID, smlType, null, null, sensor, System.currentTimeMillis(), getSensorProviderId(serviceID));
                    } else {
                        throw new ConfigurationException("Only handle SensorML for now");
                    }
                } else {
                    throw new ConfigurationException("An imported file is null");
                }
            }
            return true;
        } catch (JAXBException ex) {
            LOGGER.log(Level.WARNING, "Exception while unmarshalling imported file", ex);
        } catch (IOException ex) {
            throw new ConfigurationException(ex);
        }
        return false;
    }

    public boolean removeSensor(final Integer id, final String sensorID) throws ConfigurationException {
        final ObservationProvider pr = getOMProvider(id);
        try {
            final SensorMLTree root = getSensorTree(id);
            final SensorMLTree tree = root.find(sensorID);

            // for a System sensor, we delete also his components
            final List<NamedId> toRemove = new ArrayList<>();
            if (tree != null) {
                toRemove.addAll(tree.getAllChildrenNamedIds());
            } else {
                // tree should no be null
                toRemove.add(new NamedId(-1, sensorID));
            }
            for (NamedId sid : toRemove) {
                sensorBusiness.removeSensorFromService(id, sid.getId());
                if (sensorBusiness.getLinkedServiceIds(sid.getId()).isEmpty()) {
                    pr.removeProcedure(sid.getIdentifier());
                }
            }

            // if the sensor has a System parent, we must update his component list
            if (tree != null && tree.getParent() != null) {
                final String parentID = tree.getParent().getIdentifier();
                if (!"root".equals(parentID)) {
                    final AbstractSensorML sml = (AbstractSensorML) sensorBusiness.getSensorMetadata(parentID);
                    SensorUtils.removeComponent(sml, sensorID);
                    sensorBusiness.updateSensorMetadata(parentID, sml);
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
                boolean sucess = true; // TODO
                if (sucess) {
                    pr.removeProcedure(sensor.getIdentifier());
                } else {
                    return false;
                }
            }

            return true;
        } catch (ConstellationStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public SensorMLTree getSensorTree(Integer id) throws ConfigurationException {
        final Collection<Sensor> sensors = sensorBusiness.getByServiceId(id);
        final List<SensorMLTree> values = new ArrayList<>();
        for (Sensor sensor : sensors) {
            final AbstractSensorML sml = (AbstractSensorML) sensorBusiness.getSensorMetadata(sensor.getIdentifier());
            final SensorMLTree t;
            if (sml != null) {
                final String smlType  = getSensorMLType(sml);
                final String smlID    = getSmlID(sml);
                t                     = new SensorMLTree(sensor.getId(), smlID, smlType, null, null);
                final List<SensorMLTree> children = SensorUtils.getChildren(sml);
                t.setChildren(children);
            } else {
                LOGGER.log(Level.WARNING, "Unable to retrieve Sensor Metadata for:{0}", sensor.getIdentifier());
                t = new SensorMLTree(sensor.getId(), sensor.getIdentifier(), null, null, null);
            }
            values.add(t);
        }
        return SensorMLTree.buildTree(values);
    }

    public Collection<String> getSensorIds(final Integer id) throws ConfigurationException {
        final ObservationProvider pr = getOMProvider(id);
        try {
            return pr.getProcedureNames(null, Collections.EMPTY_MAP);
        } catch (ConstellationStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public Collection<String> getSensorIdsForObservedProperty(final Integer id, final String observedProperty) throws ConfigurationException {
        final ObservationProvider pr = getOMProvider(id);
        try {
            SimpleQuery query = new SimpleQuery();
            final FilterFactory ff = DefaultFactories.forBuildin(FilterFactory.class);
            query.setFilter(ff.equals(ff.property("observedProperty"), ff.literal(observedProperty)));
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
            final SensorMLTree root          = getSensorTree(serviceId);
            final SensorMLTree current       = root.find(sensorID);
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
            final SensorMLTree root          = getSensorTree(id);
            final SensorMLTree current       = root.find(sensorID);
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

    public String getObservationsCsv(final Integer id, final String sensorID, final List<String> observedProperties, final List<String> foi, final Date start, final Date end, final Integer width) throws ConfigurationException {
        try {
            final ObservationProvider pr = getOMProvider(id);
            SimpleQuery query = new SimpleQuery();
            query.setFilter(buildFilter(start, end, observedProperties, foi));
            Map<String, String> hints = new HashMap<>();
            if (width != null) {
                hints.put("decimSize", Integer.toString(width));
            }
            return pr.getResults(sensorID, CommonConstants.OBSERVATION_QNAME, query, "text/csv", hints);
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
                filters.add(ff.equals(ff.property("observedProperty"), ff.literal(observedProperty)));
            }
        }
        if (featuresOfInterest != null) {
            for (String featureOfInterest : featuresOfInterest) {
                filters.add(ff.equals(ff.property("featureOfInterest"), ff.literal(featureOfInterest)));
            }
        }
        if (filters.size() == 1) {
            return filters.get(0);
        } else if (filters.size() > 1) {
            return ff.and(filters);
        } else {
            return Filter.INCLUDE;
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
