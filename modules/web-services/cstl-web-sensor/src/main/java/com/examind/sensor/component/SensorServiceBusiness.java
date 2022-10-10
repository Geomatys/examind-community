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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.sis.util.collection.BackingStoreException;
import org.constellation.api.CommonConstants;
import static org.constellation.api.CommonConstants.OBJECT_TYPE;
import static org.constellation.api.CommonConstants.PROCEDURE;
import org.constellation.dto.service.config.sos.ExtractionResult;
import org.constellation.dto.service.config.sos.ProcedureTree;
import org.constellation.dto.service.config.sos.SOSConfiguration;
import org.constellation.exception.ConstellationException;
import org.constellation.provider.SensorData;
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
import org.opengis.observation.Observation;
import org.opengis.observation.ObservationCollection;
import org.opengis.observation.Phenomenon;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.opengis.temporal.TemporalGeometricPrimitive;
import org.opengis.util.GenericName;
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
            case "zip" -> {
                try  {
                    files = ZipUtilities.unzip(sensorFile, null);
                } catch (IOException ex) {
                    throw new ConfigurationException(ex);
                }
            }
            case "xml" -> files = Arrays.asList(sensorFile);
            default    -> throw new ConfigurationException("Unexpected file extension, accepting zip or xml");
        }

        try {
            int smlProviderId = getSensorProviderId(serviceID);
            for (Path importedFile: files) {
                if (importedFile != null) {
                    final Object sensor = sensorBusiness.unmarshallSensor(importedFile);
                    if (sensor instanceof AbstractSensorML sml) {
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
        final ObservationProvider pr = getSensorProvider(id, ObservationProvider.class);
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
                    ObservationProvider servProv  = getSensorProvider(serviceId, ObservationProvider.class);
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
        final ObservationProvider pr = getSensorProvider(id, ObservationProvider.class);
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
        if (isDirectProviderMode(id)) {
            final ObservationProvider pr = getSensorProvider(id, ObservationProvider.class);
            try {
                return pr.getProcedureNames(null, Collections.EMPTY_MAP);
            } catch (ConstellationStoreException ex) {
                throw new ConfigurationException(ex);
            }
        } else {
            return sensorBusiness.getLinkedSensorIdentifiers(id, null);
        }
    }

    public long getSensorCount(final Integer id) throws ConfigurationException {
        if (isDirectProviderMode(id)) {
            final ObservationProvider pr = getSensorProvider(id, ObservationProvider.class);
            try {
                return pr.getCount(null, Collections.singletonMap(OBJECT_TYPE, PROCEDURE));
            } catch (ConstellationStoreException ex) {
                throw new ConfigurationException(ex);
            }
        } else {
            return sensorBusiness.getCountByServiceId(id);
        }
    }

    public Collection<String> getSensorIdsForObservedProperty(final Integer id, final String observedProperty) throws ConfigurationException {
        final ObservationProvider pr = getSensorProvider(id, ObservationProvider.class);
        try {
            FeatureQuery query = new FeatureQuery();
            query.setSelection(buildFilter(null, null, Arrays.asList(observedProperty), new ArrayList<>()));
            Stream<String> processes = pr.getProcedures(query, Collections.emptyMap())
                                         .stream()
                                         .map(p -> ((org.geotoolkit.observation.xml.Process)p).getHref());

            if (!isDirectProviderMode(id)) {
                // filter on linked sensors
                processes = processes.filter(p -> sensorBusiness.isLinkedSensor(id, p));
            }
             return processes.collect(Collectors.toList());
        } catch (ConstellationStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public Collection<String> getObservedPropertiesForSensorId(final Integer id, final String sensorID, final boolean decompose) throws ConfigurationException {
        final ObservationProvider pr = getSensorProvider(id, ObservationProvider.class);
        try {
            final SensorMLTree current;
            if (isDirectProviderMode(id)) {
                final SensorProvider sp = getSensorProvider(id, SensorProvider.class);
                SensorData sd = (SensorData) sp.get(null, sensorID);
                Sensor s = SensorUtils.getSensorFromData(sd, null);
                current = new SensorMLTree(s);
            } else {
                current = sensorBusiness.getSensorMLTree(sensorID);
            }
            return SensorUtils.getPhenomenonFromSensor(current, pr, decompose);
            
        } catch (ConstellationStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public TemporalGeometricPrimitive getTimeForSensorId(final Integer id, final String sensorID) throws ConfigurationException {
        final ObservationProvider pr = getSensorProvider(id, ObservationProvider.class);
        try {
            return pr.getTimeForProcedure("2.0.0", sensorID);
        } catch (ConstellationStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public boolean importObservations(final Integer id, final Path observationFile) throws ConfigurationException {
        try {
            final Object objectFile = SensorUtils.unmarshallObservationFile(observationFile);
            if (objectFile instanceof Observation obs) {
                importObservations(id, Arrays.asList(obs), new ArrayList<>());
            } else if (objectFile instanceof ObservationCollection coll) {
                importObservations(id, coll);
            } else {
                return false;
            }
            return true;
        } catch (ConstellationStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public void importObservations(final Integer id, final ObservationCollection collection) throws ConfigurationException {
        importObservations(id, collection.getMember(), new ArrayList<>());
    }

    public void importObservations(final Integer id, final List<Observation> observations, final List<Phenomenon> phenomenons) throws ConfigurationException {
        final ObservationProvider writer = getSensorProvider(id, ObservationProvider.class);
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
        final ObservationProvider pr = getSensorProvider(id, ObservationProvider.class);
        try {
            pr.removeObservation(observationID);
            return true;
        } catch (ConstellationStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public Collection<String> getObservedPropertiesIds(Integer id) throws ConfigurationException {
        final ObservationProvider pr = getSensorProvider(id, ObservationProvider.class);
        try {
            // TODO this results are not filtered on linked sensors
            return pr.getPhenomenonNames(null, Collections.EMPTY_MAP);
        } catch (ConstellationStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public void writeProcedure(final Integer id, final ProcedureTree procedure) throws ConfigurationException {
        final ObservationProvider pr = getSensorProvider(id, ObservationProvider.class);
        try {
            pr.writeProcedure(procedure);
        } catch (ConstellationStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public boolean updateSensorLocation(final Integer id, final String sensorID, final org.opengis.geometry.Geometry location) throws ConfigurationException {
        final ObservationProvider pr = getSensorProvider(id, ObservationProvider.class);
        try {
            pr.updateProcedureLocation(sensorID, location);
            return true;
        } catch (ConstellationStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public String getWKTSensorLocation(final Integer id, final String sensorID) throws ConfigurationException {
        final ObservationProvider provider = getSensorProvider(id, ObservationProvider.class);
        try {
            final SensorMLTree current;
            if (isDirectProviderMode(id)) {
                final SensorProvider sp = getSensorProvider(id, SensorProvider.class);
                SensorData sd = (SensorData) sp.get(null, sensorID);
                Sensor s = SensorUtils.getSensorFromData(sd, null);
                current = new SensorMLTree(s);
            } else {
                current = sensorBusiness.getSensorMLTree(sensorID);
            }
            if (current != null) {
                final List<Geometry> jtsGeometries = provider.getJTSGeometryFromSensor(current);
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
        } catch (ConstellationStoreException | BackingStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public Object getResultsCsv(final Integer id, final String sensorID, final List<String> observedProperties, final List<String> foi, final Date start, final Date end, final Integer width, final String resultFormat, final boolean timeforProfile, final boolean includeIdInDatablock) throws ConfigurationException {
        try {
            final ObservationProvider pr = getSensorProvider(id, ObservationProvider.class);
            FeatureQuery query = new FeatureQuery();
            query.setSelection(buildFilter(start, end, observedProperties, foi));
            Map<String, Object> hints = new HashMap<>();
            if (width != null) {
                hints.put("decimSize", Integer.toString(width));
            }
            hints.put("includeTimeForProfile", timeforProfile);
            hints.put("includeIDInDataBlock", includeIdInDatablock);
            return pr.getResults(sensorID, CommonConstants.OBSERVATION_QNAME, "inline", query, resultFormat, hints);
        } catch (ConstellationStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    public Object getSensorMetadata(final Integer id, final String sensorID) throws ConstellationException {
        if (isDirectProviderMode(id)) {
            SensorProvider sp = getSensorProvider(id, SensorProvider.class);
            SensorData sd = (SensorData) sp.get(null, sensorID);
            return sd.getSensorMetadata();
        } else {
            return sensorBusiness.getSensorMetadata(sensorID);
        }
    }

    public Integer generateSensor(final ProcedureTree process, Integer serviceID, final String parentID, final Integer dataID) throws ConfigurationException {
        Integer smlId = getSensorProviderId(serviceID);
        return sensorBusiness.generateSensor(process, smlId, parentID, dataID);
    }

    public boolean importSensor(Integer id, String sensorID) throws ConstellationException {
        final Sensor sensor               = sensorBusiness.getSensor(sensorID);
        final List<Sensor> sensorChildren = sensorBusiness.getChildren(sensor.getId());
        final Collection<String> previous = getSensorIds(id);
        final List<String> sensorIds      = new ArrayList<>();
        final List<Integer> dataProviders = new ArrayList<>();

        // hack for not importing already inserted sensor. must be reviewed when the SOS/STS service will share an O&M provider
        if (!previous.contains(sensorID)) {
            dataProviders.addAll(sensorBusiness.getLinkedDataProviderIds(sensor.getId()));
        }

        sensorBusiness.addSensorToService(id, sensor.getId());
        sensorIds.add(sensorID);

        //import sensor children
        for (Sensor child : sensorChildren) {
            // hack for not importing already inserted sensor. must be reviewed when the SOS/STS service will share an O&M provider
            if (!previous.contains(child.getIdentifier())) {
                dataProviders.addAll(sensorBusiness.getLinkedDataProviderIds(child.getId()));
            }
            sensorBusiness.addSensorToService(id, child.getId());
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
            if (provider instanceof ObservationProvider omProvider) {
                final ExtractionResult result = omProvider.extractResults(sensorID, sensorIds);

                // update sensor location
                for (ProcedureTree process : result.getProcedures()) {
                    writeProcedure(id, process);
                }

                // import in O&M database
                importObservations(id, result.getObservations(), result.getPhenomenons());
            } else {
                return false;
            }
        }
        return true;
    }

    public SensorMLTree getServiceSensorMLTree(Integer id) throws ConstellationException {
        if (isDirectProviderMode(id)) {
            SensorProvider sp = getSensorProvider(id, SensorProvider.class);
            Integer sid       = getSensorProviderId(id);
            List<SensorMLTree> ss = sp.getKeys().stream()
                                .map(gn -> getData(sp, gn))
                                .map(sd -> SensorUtils.getSensorFromData(sd, sid))
                                .map(s -> new SensorMLTree(s))
                                .collect(Collectors.toList());
            return SensorMLTree.buildTree(ss, true);
        } else {
            return sensorBusiness.getServiceSensorMLTree(id);
        }
    }

    /**
     * Only here to use the SensorProvider#get in a stream.
     * Change the ConstellationStoreException into a BackingStoreException.
     */
    private static SensorData getData(SensorProvider sp, GenericName gn) {
        try {
            return (SensorData)sp.get(gn);
        } catch (ConstellationStoreException ex) {
            throw new BackingStoreException("Unable to get sensor data:" + gn);
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

    private Integer getSensorProviderId(final Integer serviceID) throws ConfigurationException {
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

    private <T> T getSensorProvider(final Integer serviceID, Class<T> c) throws ConfigurationException {
        final List<Integer> providers = serviceBusiness.getLinkedProviders(serviceID);
        for (Integer providerID : providers) {
            final DataProvider p = DataProviders.getProvider(providerID);
            if(c.isInstance(p)){
                // TODO for now we only take one provider by type
                return (T) p;
            }
        }
        throw new ConfigurationException("there is no OM provider linked to this ID:" + serviceID);
    }

    private boolean isDirectProviderMode(final Integer id) throws ConfigurationException {
        final SOSConfiguration conf = getServiceConfiguration(id);
        return conf.getBooleanParameter("directProvider", false);
    }

    private SOSConfiguration getServiceConfiguration(final Integer id) throws ConfigurationException {
        try {
            // we get the CSW configuration file
            return (SOSConfiguration) serviceBusiness.getConfiguration(id);
        } catch (Exception ex) {
            throw new ConfigurationException("Error while getting the Sensor service configuration for:" + id, ex);
        }
    }
}
