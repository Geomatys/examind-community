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

import org.locationtech.jts.geom.Geometry;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.CRS;
import org.apache.sis.util.logging.Logging;
import org.constellation.dto.service.config.sos.SensorMLTree;
import org.constellation.util.ReflectionUtilities;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.gml.GeometrytoJTS;
import org.geotoolkit.gml.xml.AbstractFeature;
import org.geotoolkit.gml.xml.AbstractGeometry;
import org.geotoolkit.gml.xml.BoundingShape;
import org.geotoolkit.gml.xml.Envelope;
import org.geotoolkit.sml.xml.AbstractComponents;
import org.geotoolkit.sml.xml.AbstractProcess;
import org.geotoolkit.sml.xml.AbstractProcessChain;
import org.geotoolkit.sml.xml.AbstractSensorML;
import org.geotoolkit.sml.xml.ComponentProperty;
import org.geotoolkit.sml.xml.SMLMember;
import org.geotoolkit.sml.xml.System;
import org.geotoolkit.sos.xml.SOSMarshallerPool;
import org.geotoolkit.sos.xml.SOSXmlFactory;
import org.geotoolkit.swe.xml.AbstractEncoding;
import org.geotoolkit.swe.xml.TextBlock;
import org.opengis.geometry.primitive.Point;
import org.opengis.observation.Observation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.temporal.Period;
import org.opengis.util.FactoryException;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.internal.storage.query.SimpleQuery;
import org.apache.sis.internal.system.DefaultFactories;
import org.geotoolkit.gml.xml.FeatureProperty;
import org.geotoolkit.observation.xml.AbstractObservation;

import static org.geotoolkit.sml.xml.SensorMLUtilities.getSensorMLType;
import static org.geotoolkit.sml.xml.SensorMLUtilities.getSmlID;
import org.apache.sis.util.Utilities;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.provider.ObservationProvider;
import org.opengis.filter.FilterFactory;
import org.opengis.observation.CompositePhenomenon;
import org.opengis.observation.Phenomenon;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public final class SensorUtils {

    /**
     * use for debugging purpose
     */
    private static final Logger LOGGER = Logging.getLogger("com.examind.sensor.ws");

    private SensorUtils() {}

    /**
     * depracted by org.geotoolkit.observation.Utils.getTimeValue
     */
    public static Envelope getCollectionBound(final String version, final List<Observation> observations, final String srsName) {
        double minx = Double.MAX_VALUE;
        double miny = Double.MAX_VALUE;
        double maxx = -Double.MAX_VALUE;
        double maxy = -Double.MAX_VALUE;

        for (Observation observation: observations) {
            final AbstractFeature feature = (AbstractFeature) observation.getFeatureOfInterest();
            if (feature != null) {
                if (feature.getBoundedBy() != null) {
                    final BoundingShape bound = feature.getBoundedBy();
                    if (bound.getEnvelope() != null) {
                        if (bound.getEnvelope().getLowerCorner() != null
                            && bound.getEnvelope().getLowerCorner().getCoordinate() != null
                            && bound.getEnvelope().getLowerCorner().getCoordinate().length == 2 ) {
                            final double[] lower = bound.getEnvelope().getLowerCorner().getCoordinate();
                            if (lower[0] < minx) {
                                minx = lower[0];
                            }
                            if (lower[1] < miny) {
                                miny = lower[1];
                            }
                        }
                        if (bound.getEnvelope().getUpperCorner() != null
                            && bound.getEnvelope().getUpperCorner().getCoordinate() != null
                            && bound.getEnvelope().getUpperCorner().getCoordinate().length == 2 ) {
                            final double[] upper = bound.getEnvelope().getUpperCorner().getCoordinate();
                            if (upper[0] > maxx) {
                                maxx = upper[0];
                            }
                            if (upper[1] > maxy) {
                                maxy = upper[1];
                            }
                        }
                    }
                }
            }
        }

        if (minx == Double.MAX_VALUE) {
            minx = -180.0;
        }
        if (miny == Double.MAX_VALUE) {
            miny = -90.0;
        }
        if (maxx == (-Double.MAX_VALUE)) {
            maxx = 180.0;
        }
        if (maxy == (-Double.MAX_VALUE)) {
            maxy = 90.0;
        }

        final Envelope env = SOSXmlFactory.buildEnvelope(version, null, minx, miny, maxx, maxy, srsName);
        env.setSrsDimension(2);
        env.setAxisLabels(Arrays.asList("Y X"));
        return env;
    }

    /**
     * Used for CSV encoding, while iterating on a resultSet.
     *
     * if the round on the current date is over, and some field data are not present,
     * we have to add empty token before to start the next date round.
     *
     * example : we are iterating on some date with temperature an salinity
     *
     * date       |  phenomenon | value
     * 2010-01-01    TEMP          1
     * 2010-01-01    SAL           202
     * 2010-01-02    TEMP          3
     * 2010-01-02    SAL           201
     * 2010-01-03    TEMP          4
     * 2010-01-04    TEMP          2
     * 2010-01-04    SAL           210
     *
     * CSV encoding will be : @@2010-01-01,1,202@@2010-01-02,3,201@@2010-01-03,4,@@2010-01-04,2,210
     *
     * @param value the datablock builder.
     * @param currentIndex the current object index.
     */
    public static void fillEndingDataHoles(final Appendable value, int currentIndex, final List<String> fieldList, final TextBlock encoding, final int nbBlockByHole) throws IOException {
        while (currentIndex < fieldList.size()) {
            if (value != null) {
                for (int i = 0; i < nbBlockByHole; i++) {
                    value.append(encoding.getTokenSeparator());
                }
            }
            currentIndex++;
        }
    }

    /**
     * Used for CSV encoding, while iterating on a resultSet.
     *
     * if some field data are not present in the middle of a date round,
     * we have to add empty token until we got the next phenomenon data.
     *
     * @param value the datablock builder.
     * @param currentIndex the current phenomenon index.
     * @param searchedField the name of the current phenomenon.
     *
     * @return the updated phenomenon index.
     */
    public static int fillDataHoles(final Appendable value, int currentIndex, final String searchedField, final List<String> fieldList, final TextBlock encoding, final int nbBlockByHole) throws IOException {
        while (currentIndex < fieldList.size() && !fieldList.get(currentIndex).equals(searchedField)) {
            if (value != null) {
                for (int i = 0; i < nbBlockByHole; i++) {
                    value.append(encoding.getTokenSeparator());
                }
            }
            currentIndex++;
        }
        return currentIndex;
    }

    public static String getIDFromObject(final Object obj) {
        if (obj != null) {
            final Method idGetter = ReflectionUtilities.getGetterFromName("id", obj.getClass());
            if (idGetter != null) {
                return (String) ReflectionUtilities.invokeMethod(obj, idGetter);
            }
        }
        return null;
    }

    public static Period extractTimeBounds(final String version, final String brutValues, final AbstractEncoding abstractEncoding) {
        final String[] result = new String[2];
        if (abstractEncoding instanceof TextBlock) {
            final TextBlock encoding        = (TextBlock) abstractEncoding;
            final StringTokenizer tokenizer = new StringTokenizer(brutValues, encoding.getBlockSeparator());
            boolean first = true;
            while (tokenizer.hasMoreTokens()) {
                final String block = tokenizer.nextToken();
                final int tokenEnd = block.indexOf(encoding.getTokenSeparator());
                String samplingTimeValue;
                if (tokenEnd != -1) {
                    samplingTimeValue = block.substring(0, block.indexOf(encoding.getTokenSeparator()));
                // only one field
                } else {
                    samplingTimeValue = block;
                }
                if (first) {
                    result[0] = samplingTimeValue;
                    first = false;
                } else if (!tokenizer.hasMoreTokens()) {
                    result[1] = samplingTimeValue;
                }
            }
        } else {
            LOGGER.warning("unable to parse datablock unknown encoding");
        }
        return SOSXmlFactory.buildTimePeriod(version, null, result[0], result[1]);
    }

    /**
     * Return true if the samplingPoint entry is strictly inside the specified envelope.
     *
     * @param sp A sampling point (2D) station.
     * @param e An envelope (2D).
     * @return True if the sampling point is strictly inside the specified envelope.
     */
    public static boolean samplingPointMatchEnvelope(final Point sp, final Envelope e) throws ConstellationStoreException {
        if (sp.getDirectPosition() != null) {
            org.opengis.geometry.Envelope reproj;
            final CoordinateReferenceSystem spCRS = sp.getCoordinateReferenceSystem();
            if (Utilities.equalsIgnoreMetadata(spCRS, e.getCoordinateReferenceSystem())) {
                reproj = e;
            } else {
                try {
                    reproj = new GeneralEnvelope(e);
                    reproj = Envelopes.transform(reproj, spCRS);
                } catch (TransformException ex) {
                    throw new ConstellationStoreException(ex);
                }
            }

            final double stationX = sp.getDirectPosition().getOrdinate(0);
            final double stationY = sp.getDirectPosition().getOrdinate(1);
            final double minx     = reproj.getLowerCorner().getOrdinate(0);
            final double maxx     = reproj.getUpperCorner().getOrdinate(0);
            final double miny     = reproj.getLowerCorner().getOrdinate(1);
            final double maxy     = reproj.getUpperCorner().getOrdinate(1);

            // we look if the station if contained in the BBOX
            return stationX < maxx && stationX > minx && stationY < maxy && stationY > miny;
        }
        LOGGER.log(Level.WARNING, " the feature of interest does not have proper position");
        return false;
    }

    public static boolean BoundMatchEnvelope(final AbstractFeature sc, final Envelope e) {
         if (sc.getBoundedBy() != null &&
            sc.getBoundedBy().getEnvelope() != null &&
            sc.getBoundedBy().getEnvelope().getLowerCorner() != null &&
            sc.getBoundedBy().getEnvelope().getUpperCorner() != null &&
            sc.getBoundedBy().getEnvelope().getLowerCorner().getCoordinate().length > 1 &&
            sc.getBoundedBy().getEnvelope().getUpperCorner().getCoordinate().length > 1) {

            final double stationMinX  = sc.getBoundedBy().getEnvelope().getLowerCorner().getOrdinate(0);
            final double stationMaxX  = sc.getBoundedBy().getEnvelope().getUpperCorner().getOrdinate(0);
            final double stationMinY  = sc.getBoundedBy().getEnvelope().getLowerCorner().getOrdinate(1);
            final double stationMaxY  = sc.getBoundedBy().getEnvelope().getUpperCorner().getOrdinate(1);
            final double minx         = e.getLowerCorner().getOrdinate(0);
            final double maxx         = e.getUpperCorner().getOrdinate(0);
            final double miny         = e.getLowerCorner().getOrdinate(1);
            final double maxy         = e.getUpperCorner().getOrdinate(1);

            // we look if the station if contained in the BBOX
            if (stationMaxX < maxx && stationMinX > minx &&
                stationMaxY < maxy && stationMinY > miny) {
                return true;
            } else {
                LOGGER.log(Level.FINER, " the feature of interest {0} is not in the BBOX", sc.getId());
            }
        } else {
            LOGGER.log(Level.WARNING, " the feature of interest (samplingCurve){0} does not have proper bounds", sc.getId());
        }
        return false;
    }

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

    public static List<SensorMLTree> getChildren(final AbstractProcess process) {
        final List<SensorMLTree> results = new ArrayList<>();
        if (process instanceof System) {
            final System s = (System) process;
            final AbstractComponents compos = s.getComponents();
            if (compos != null && compos.getComponentList() != null) {
                for (ComponentProperty cp : compos.getComponentList().getComponent()){
                    if (cp.getHref() != null) {
                        results.add(new SensorMLTree(null, cp.getHref(), "unknown", null, null));
                    } else if (cp.getAbstractProcess()!= null) {
                        results.add(new SensorMLTree(null, getSmlID(cp.getAbstractProcess()), getSensorMLType(cp.getAbstractProcess()), null, null));
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
                        results.add(new SensorMLTree(null, cp.getHref(), "unknown", null, null));
                    } else if (cp.getAbstractProcess()!= null) {
                        results.add(new SensorMLTree(null, getSmlID(cp.getAbstractProcess()), getSensorMLType(cp.getAbstractProcess()),null, null));
                    } else {
                        LOGGER.warning("SML system component has no href or embedded object");
                    }
                }
            }
        }
        return results;
    }

    public static List<Geometry> getJTSGeometryFromSensor(final SensorMLTree sensor, final ObservationProvider omProvider) throws ConstellationStoreException, FactoryException, TransformException {
        if ("Component".equals(sensor.getType())) {
            final AbstractGeometry geom = (AbstractGeometry) omProvider.getSensorLocation(sensor.getIdentifier(), "2.0.0");
            if (geom != null) {
                Geometry jtsGeometry = GeometrytoJTS.toJTS(geom);
                // reproject to CRS:84
                final MathTransform mt = CRS.findOperation(geom.getCoordinateReferenceSystem(true), CommonCRS.defaultGeographic(), null).getMathTransform();
                return Arrays.asList(JTS.transform(jtsGeometry, mt));
            }
        } else {
            final List<Geometry> geometries = new ArrayList<>();

            // add the root geometry if there is one
            final AbstractGeometry geom = (AbstractGeometry) omProvider.getSensorLocation(sensor.getIdentifier(), "2.0.0");
            if (geom != null) {
                Geometry jtsGeometry = GeometrytoJTS.toJTS(geom);
                // reproject to CRS:84
                final MathTransform mt = CRS.findOperation(geom.getCoordinateReferenceSystem(true), CommonCRS.defaultGeographic(), null).getMathTransform();
                geometries.add(JTS.transform(jtsGeometry, mt));
            }
            for (SensorMLTree child : sensor.getChildren()) {
                geometries.addAll(getJTSGeometryFromSensor(child, omProvider));
            }
            return geometries;
        }
        return new ArrayList<>();
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

    public static Set<String> getPhenomenonFromSensor(final String sensorID, final ObservationProvider provider, boolean decompose) throws ConstellationStoreException {
        final FilterFactory ff = DefaultFactories.forBuildin(FilterFactory.class);
        final Set<String> phenomenons = new HashSet<>();

        SimpleQuery query = new SimpleQuery();
        query.setFilter(ff.equals(ff.property("procedure"), ff.literal(sensorID)));
        Collection<Phenomenon> phenos = provider.getPhenomenon(query, Collections.emptyMap());
        phenos.forEach(p -> {
            org.geotoolkit.swe.xml.Phenomenon phen = (org.geotoolkit.swe.xml.Phenomenon)p;
            if (decompose && phen instanceof CompositePhenomenon) {
                CompositePhenomenon cp = (CompositePhenomenon)phen;
                for (Phenomenon c : cp.getComponent()) {
                    phenomenons.add(((org.geotoolkit.swe.xml.Phenomenon)c).getName().getCode());
                }
            } else {
                phenomenons.add(phen.getName().getCode());
            }
        });
        return phenomenons;
    }

    public static Object unmarshallObservationFile(final Path f) throws JAXBException, ConstellationStoreException, IOException {
        try (InputStream stream = Files.newInputStream(f)){
            final Unmarshaller um = SOSMarshallerPool.getInstance().acquireUnmarshaller();
            Object obj = um.unmarshal(stream);
            if (obj instanceof JAXBElement) {
                obj = ((JAXBElement) obj).getValue();
            }
            if (obj != null) {
                return obj;
            }
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
}