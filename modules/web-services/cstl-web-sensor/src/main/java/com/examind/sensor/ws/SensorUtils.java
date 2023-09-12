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

package com.examind.sensor.ws;

import org.constellation.dto.service.config.sos.SensorMLTree;
import org.geotoolkit.gml.xml.AbstractFeature;
import org.geotoolkit.gml.xml.Envelope;
import org.geotoolkit.sml.xml.AbstractComponents;
import org.geotoolkit.sml.xml.AbstractProcess;
import org.geotoolkit.sml.xml.AbstractProcessChain;
import org.geotoolkit.sml.xml.AbstractSensorML;
import org.geotoolkit.sml.xml.ComponentProperty;
import org.geotoolkit.sml.xml.SMLMember;
import org.geotoolkit.sml.xml.System;
import org.geotoolkit.sos.xml.SOSMarshallerPool;
import org.opengis.observation.Observation;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.apache.sis.util.ArgumentChecks;
import org.constellation.dto.Sensor;
import org.geotoolkit.gml.xml.FeatureProperty;
import org.geotoolkit.observation.xml.AbstractObservation;

import static org.geotoolkit.sml.xml.SensorMLUtilities.getSensorMLType;
import static org.geotoolkit.sml.xml.SensorMLUtilities.getSmlID;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.provider.ObservationProvider;
import org.constellation.provider.SensorData;
import org.geotoolkit.filter.FilterUtilities;
import org.geotoolkit.observation.query.ObservedPropertyQuery;
import static org.geotoolkit.sml.xml.SensorMLUtilities.getOMType;
import org.opengis.filter.FilterFactory;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public final class SensorUtils {

    /**
     * use for debugging purpose
     */
    private static final Logger LOGGER = Logger.getLogger("com.examind.sensor.ws");

    private SensorUtils() {}

    public static void removeComponent(final AbstractSensorML sml, final String component) {
        if (sml.getMember() != null)  {
            //assume only one member
            for (SMLMember member : sml.getMember()) {
                final AbstractProcess process = member.getRealProcess();
                if (process instanceof System) {
                    final System s = (System) process;
                    final AbstractComponents compos = s.getComponents();
                    if (compos != null && compos.getComponentList() != null) {
                        compos.getComponentList().removeComponent(component);
                    }
                }
            }
        }
    }

    public static List<SensorMLTree> getChildren(final AbstractSensorML sml) {
        if (sml.getMember() != null && !sml.getMember().isEmpty())  {
            //assume only one member
            SMLMember member = sml.getMember().get(0);
            final AbstractProcess process = member.getRealProcess();
            return getChildren(process);
        }
        return new ArrayList<>();
    }

    private static List<SensorMLTree> getChildren(final AbstractProcess process) {
        final List<SensorMLTree> results = new ArrayList<>();
        if (process instanceof System) {
            final System s = (System) process;
            final AbstractComponents compos = s.getComponents();
            if (compos != null && compos.getComponentList() != null) {
                for (ComponentProperty cp : compos.getComponentList().getComponent()){
                    if (cp.getHref() != null) {
                        results.add(new SensorMLTree(null, cp.getHref(), cp.getHref(), null, "unknown", null, null, null));
                    } else if (cp.getAbstractProcess()!= null) {
                        AbstractProcess ap = cp.getAbstractProcess();
                        String sensorId = getSmlID(ap);
                        String omType =  getOMType(ap).orElse(null);
                        results.add(new SensorMLTree(null, sensorId, sensorId, null, getSensorMLType(ap), omType, null, ap));
                    } else {
                        LOGGER.warning("SML system component has no href or embedded object");
                    }
                }
            }
        } else if (process instanceof AbstractProcessChain) {
            final AbstractProcessChain s = (AbstractProcessChain) process;
            final AbstractComponents compos = s.getComponents();
            if (compos != null && compos.getComponentList() != null) {
                for (ComponentProperty cp : compos.getComponentList().getComponent()){
                    if (cp.getHref() != null) {
                        results.add(new SensorMLTree(null, cp.getHref(), cp.getHref(), null, "unknown", null, null, null));
                    } else if (cp.getAbstractProcess()!= null) {
                        AbstractProcess ap = cp.getAbstractProcess();
                        String sensorId = getSmlID(ap);
                        String omType =  getOMType(ap).orElse(null);
                        results.add(new SensorMLTree(null, sensorId, sensorId, null, getSensorMLType(ap), omType, null, ap));
                    } else {
                        LOGGER.warning("SML system component has no href or embedded object");
                    }
                }
            }
        }
        return results;
    }

    public static Collection<String> getPhenomenonFromSensor(final SensorMLTree sensor, final ObservationProvider provider, boolean decompose) throws ConstellationStoreException {
        final Set<String> phenomenons = getPhenomenonFromSensor(sensor.getIdentifier(), provider, decompose);
        if (!"Component".equals(sensor.getType())) {
            for (SensorMLTree child : sensor.getChildren()) {
                phenomenons.addAll(getPhenomenonFromSensor(child, provider, decompose));
            }
        }
        return phenomenons;
    }

    private static Set<String> getPhenomenonFromSensor(final String sensorID, final ObservationProvider provider, boolean decompose) throws ConstellationStoreException {
        final FilterFactory ff = FilterUtilities.FF;
        ObservedPropertyQuery query = new ObservedPropertyQuery();
        query.setNoCompositePhenomenon(decompose);
        query.setSelection(ff.equal(ff.property("procedure"), ff.literal(sensorID)));
        return new HashSet(provider.getIdentifiers(query));
    }

    public static Object unmarshallObservationFile(final Path f) throws ConstellationStoreException {
        try (InputStream stream = Files.newInputStream(f)){
            final Unmarshaller um = SOSMarshallerPool.getInstance().acquireUnmarshaller();
            Object obj = um.unmarshal(stream);
            if (obj instanceof JAXBElement) {
                obj = ((JAXBElement) obj).getValue();
            }
            if (obj != null) {
                return obj;
            }
        } catch (JAXBException | IOException ex) {
            throw new ConstellationStoreException(ex);
        }
        throw new ConstellationStoreException("the observation file does not contain a valid O&M object");
    }

    public static boolean isCompleteEnvelope3D(Envelope e) {
        return e.getLowerCorner() != null && e.getUpperCorner() != null
                && e.getLowerCorner().getCoordinate().length == 3 && e.getUpperCorner().getCoordinate().length == 3;
    }

    public static String extractFOID(Observation obs) {
        if (obs.getFeatureOfInterest() instanceof AbstractFeature) {
            return ((AbstractFeature)obs.getFeatureOfInterest()).getId();
        } else if (obs instanceof AbstractObservation) {
            AbstractObservation aobs = (AbstractObservation) obs;
            FeatureProperty featProp = aobs.getPropertyFeatureOfInterest();
            return featProp.getHref();
        }
        return null;
    }

    public static Sensor getSensorFromData(SensorData sd, Integer providerId) {
        ArgumentChecks.ensureNonNull("Sensor Data", sd);
        final String sensorId    = sd.getName().toString();
        final String name        = sd.getSensorName();
        final String description = sd.getDescription();
        final String type        = sd.getSensorMLType();
        final String omType      = sd.getOMType();
        return new Sensor(null, sensorId, name, description, type, null, null, null, providerId, null, omType);
    }
}
