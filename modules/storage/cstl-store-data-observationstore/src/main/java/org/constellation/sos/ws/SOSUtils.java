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

package org.constellation.sos.ws;

import org.locationtech.jts.geom.Geometry;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.CRS;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.logging.Logging;
import org.constellation.dto.service.config.sos.SensorMLTree;
import org.constellation.util.ReflectionUtilities;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.gml.GeometrytoJTS;
import org.geotoolkit.gml.xml.AbstractFeature;
import org.geotoolkit.gml.xml.AbstractGeometry;
import org.geotoolkit.gml.xml.BoundingShape;
import org.geotoolkit.gml.xml.Envelope;
import org.geotoolkit.observation.ObservationReader;
import org.geotoolkit.observation.ObservationStoreException;
import org.geotoolkit.sml.xml.AbstractClassification;
import org.geotoolkit.sml.xml.AbstractClassifier;
import org.geotoolkit.sml.xml.AbstractComponents;
import org.geotoolkit.sml.xml.AbstractDerivableComponent;
import org.geotoolkit.sml.xml.AbstractIdentification;
import org.geotoolkit.sml.xml.AbstractIdentifier;
import org.geotoolkit.sml.xml.AbstractProcess;
import org.geotoolkit.sml.xml.AbstractProcessChain;
import org.geotoolkit.sml.xml.AbstractSensorML;
import org.geotoolkit.sml.xml.ComponentProperty;
import org.geotoolkit.sml.xml.SMLMember;
import org.geotoolkit.sml.xml.SensorMLMarshallerPool;
import org.geotoolkit.sml.xml.System;
import org.geotoolkit.sos.xml.SOSMarshallerPool;
import org.geotoolkit.sos.xml.SOSXmlFactory;
import org.geotoolkit.swe.xml.AbstractEncoding;
import org.geotoolkit.swe.xml.TextBlock;
import org.geotoolkit.temporal.object.ISODateParser;
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
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.internal.storage.ResourceOnFileSystem;
import org.constellation.provider.DataProvider;
import org.geotoolkit.gml.xml.FeatureProperty;
import org.geotoolkit.observation.ObservationStore;
import org.geotoolkit.observation.xml.AbstractObservation;

import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.MISSING_PARAMETER_VALUE;
import static org.geotoolkit.sml.xml.SensorMLUtilities.getSensorMLType;
import static org.geotoolkit.sml.xml.SensorMLUtilities.getSmlID;
import org.apache.sis.storage.DataStore;
import org.geotoolkit.data.om.netcdf.NetcdfObservationStore;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public final class SOSUtils {

    /**
     * use for debugging purpose
     */
    private static final Logger LOGGER = Logging.getLogger("org.constellation.sos");

    private SOSUtils() {}

    /**
     * depracted by org.geotoolkit.observation.Utils.getPhysicalID
     */
    @Deprecated
    public static String getPhysicalID(final AbstractSensorML sensor) {
        if (sensor != null && sensor.getMember().size() > 0) {
            final AbstractProcess process = sensor.getMember().get(0).getRealProcess();
            final List<? extends AbstractIdentification> idents = process.getIdentification();

            for(AbstractIdentification ident : idents) {
                if (ident.getIdentifierList() != null) {
                    for (AbstractIdentifier identifier: ident.getIdentifierList().getIdentifier()) {
                        if ("supervisorCode".equals(identifier.getName()) && identifier.getTerm() != null) {
                            return identifier.getTerm().getValue();
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Return the networks names binded to this sensor.
     *
     * * Those names are found into "Classifier" marks with the name 'network'
     * @param sensor
     * @return
     */
    @Deprecated
    public static List<String> getNetworkNames(final AbstractSensorML sensor) {
        final List<String> results = new ArrayList<>();
        if (sensor != null && sensor.getMember().size() == 1) {
            final AbstractProcess component = sensor.getMember().get(0).getRealProcess();
            if (component != null) {
                for (AbstractClassification cl : component.getClassification()) {
                    if (cl.getClassifierList() != null) {
                        for (AbstractClassifier classifier : cl.getClassifierList().getClassifier()) {
                            if (classifier.getName().equals("network") && classifier.getTerm() != null) {
                                results.add(classifier.getTerm().getValue());
                            }
                        }
                    }
                }
            }
        }
        return results;
    }

    /**
     * depracted by org.geotoolkit.observation.Utils.getSensorPosition
     */
    @Deprecated
    public static AbstractGeometry getSensorPosition(final AbstractSensorML sensor) {
        if (sensor.getMember().size() == 1) {
            if (sensor.getMember().get(0).getRealProcess() instanceof AbstractDerivableComponent) {
                final AbstractDerivableComponent component = (AbstractDerivableComponent) sensor.getMember().get(0).getRealProcess();
                if (component.getSMLLocation() != null && component.getSMLLocation().getGeometry()!= null) {
                    return component.getSMLLocation().getGeometry();
                } else if (component.getPosition() != null && component.getPosition().getPosition() != null &&
                           component.getPosition().getPosition().getLocation() != null && component.getPosition().getPosition().getLocation().getVector() != null) {
                    final URI crs = component.getPosition().getPosition().getReferenceFrame();
                    return component.getPosition().getPosition().getLocation().getVector().getGeometry(crs);
                }
            }
        }
        LOGGER.severe("there is no piezo location");
        return null;
    }

    /**
     * depracted by org.geotoolkit.observation.Utils.getTimeValue
     */
    public static String getTimeValue(final Date time) throws ObservationStoreException {
        if (time != null) {
             try {
                 DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SS");
                 final String value = df.format(time);

                 //here t is not used but it allow to verify the syntax of the timestamp
                 final ISODateParser parser = new ISODateParser();
                 final Date d = parser.parseToDate(value);
                 final Timestamp t = new Timestamp(d.getTime());
                 return t.toString();

             } catch(IllegalArgumentException e) {
                throw new ObservationStoreException("Unable to parse the value: " + time.toString() + '\n' +
                                               "Bad format of timestamp:\n" + e.getMessage(),
                                               INVALID_PARAMETER_VALUE, "eventTime");
             }
          } else {
            String locator;
            if (time == null) {
                locator = "Timeposition";
            } else {
                locator = "TimePosition value";
            }
            throw new  ObservationStoreException("bad format of time, " + locator + " mustn't be null",
                                              MISSING_PARAMETER_VALUE, "eventTime");
          }
    }

    public static Timestamp getTimestampValue(final Date time) throws ObservationStoreException {
        return Timestamp.valueOf(getTimeValue(time));
    }

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
    public static boolean samplingPointMatchEnvelope(final Point sp, final Envelope e) {
        if (sp.getDirectPosition() != null) {

            final double stationX = sp.getDirectPosition().getOrdinate(0);
            final double stationY = sp.getDirectPosition().getOrdinate(1);
            final double minx     = e.getLowerCorner().getOrdinate(0);
            final double maxx     = e.getUpperCorner().getOrdinate(0);
            final double miny     = e.getLowerCorner().getOrdinate(1);
            final double maxy     = e.getUpperCorner().getOrdinate(1);

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
        if (sml.getMember() != null)  {
            //assume only one member
            for (SMLMember member : sml.getMember()) {
                final AbstractProcess process = member.getRealProcess();
                return getChildren(process);
            }
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

    public static List<Geometry> getJTSGeometryFromSensor(final SensorMLTree sensor, final ObservationReader reader) throws DataStoreException, FactoryException, TransformException {
        if ("Component".equals(sensor.getType())) {
            final AbstractGeometry geom = reader.getSensorLocation(sensor.getIdentifier(), "2.0.0");
            if (geom != null) {
                Geometry jtsGeometry = GeometrytoJTS.toJTS(geom);
                // reproject to CRS:84
                final MathTransform mt = CRS.findOperation(geom.getCoordinateReferenceSystem(true), CommonCRS.defaultGeographic(), null).getMathTransform();
                return Arrays.asList(JTS.transform(jtsGeometry, mt));
            }
        } else {
            final List<Geometry> geometries = new ArrayList<>();

            // add the root geometry if there is one
            final AbstractGeometry geom = reader.getSensorLocation(sensor.getIdentifier(), "2.0.0");
            if (geom != null) {
                Geometry jtsGeometry = GeometrytoJTS.toJTS(geom);
                // reproject to CRS:84
                final MathTransform mt = CRS.findOperation(geom.getCoordinateReferenceSystem(true), CommonCRS.defaultGeographic(), null).getMathTransform();
                geometries.add(JTS.transform(jtsGeometry, mt));
            }
            for (SensorMLTree child : sensor.getChildren()) {
                geometries.addAll(getJTSGeometryFromSensor(child, reader));
            }
            return geometries;
        }
        return new ArrayList<>();
    }

    public static Collection<String> getPhenomenonFromSensor(final SensorMLTree sensor, final ObservationReader reader) throws DataStoreException {
        if ("Component".equals(sensor.getType())) {
            return reader.getPhenomenonsForProcedure(sensor.getIdentifier());

        } else {
            final Set<String> phenomenons = new HashSet<>();

            // add the root phenomenon if there is one
            phenomenons.addAll(reader.getPhenomenonsForProcedure(sensor.getIdentifier()));
            for (SensorMLTree child : sensor.getChildren()) {
                phenomenons.addAll(getPhenomenonFromSensor(child, reader));
            }
            return phenomenons;
        }
    }

    public static AbstractSensorML unmarshallSensor(final Path f) throws JAXBException, DataStoreException {
        try (InputStream stream = Files.newInputStream(f)){
            return unmarshallSensor(stream);
        } catch (FileNotFoundException e) {
            throw new DataStoreException("File to unmarhall not found", e);
        } catch (IOException e) {
            throw new DataStoreException("Unable to read file", e);
        }
    }

    /**
     *
     * @param f
     * @return
     * @throws JAXBException
     * @throws DataStoreException
     *
     * deprecated use {@link #unmarshallSensor(Path)} instead
     */
    @Deprecated
    public static AbstractSensorML unmarshallSensor(final File f) throws JAXBException, DataStoreException {
        try (InputStream stream = new FileInputStream(f)){
            return unmarshallSensor(stream);
        } catch (FileNotFoundException e) {
            throw new DataStoreException("File to unmarhall not found", e);
        } catch (IOException e) {
            throw new DataStoreException("Unable to read file", e);
        }
    }

    public static AbstractSensorML unmarshallSensor(final String xml) throws JAXBException, DataStoreException {
        return unmarshallSensor(new ByteArrayInputStream(xml.getBytes(Charset.forName("UTF-8"))));
    }

    public static AbstractSensorML unmarshallSensor(final InputStream is) throws JAXBException, DataStoreException {
        final Unmarshaller um = SensorMLMarshallerPool.getInstance().acquireUnmarshaller();
        Object obj = um.unmarshal(is);
        if (obj instanceof JAXBElement) {
            obj = ((JAXBElement)obj).getValue();
        }
        if (obj instanceof AbstractSensorML) {
            return (AbstractSensorML)obj;
        }
        throw new DataStoreException("the sensorML file does not contain a valid sensorML object");
    }

    public static Object unmarshallObservationFile(final Path f) throws JAXBException, DataStoreException, IOException {
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
        throw new DataStoreException("the observation file does not contain a valid O&M object");
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

    public static void setLogLevel(DataProvider provider, final Level logLevel) {
        final DataStore store = provider.getMainStore();
        if(store instanceof ObservationStore){
            if (((ObservationStore)store).getFilter() != null) {
                ((ObservationStore)store).getFilter().setLoglevel(logLevel);
            }
        }
    }

    public static String getInfos(DataProvider provider) {
        final DataStore store = provider.getMainStore();
        if(store instanceof ObservationStore){
            final ObservationStore obsStore = (ObservationStore) store;
            return getInfos(obsStore);
        }else{
            return "";
        }
    }

    public static String getInfos(ObservationStore obsStore) {
        final StringBuilder infos = new StringBuilder();
        if (obsStore.getReader() != null) {
            infos.append(obsStore.getReader().getInfos()).append(" loaded.\n");
        } else {
            infos.append("No O&M reader loaded.\n");
        }
        if (obsStore.getFilter() != null) {
            infos.append(obsStore.getFilter().getInfos()).append(" loaded.\n");
        } else {
            infos.append("No O&M filter loaded.\n");
        }
        if (obsStore.getWriter() != null) {
            infos.append(obsStore.getWriter().getInfos()).append(" loaded.\n");
        } else {
            infos.append("No O&M writer loaded.\n");
        }
        return infos.toString();
    }

    public static ObservationStore getObservationStore(final DataProvider omProvider) {
        if (omProvider.isSensorAffectable()) {

            if (omProvider.getMainStore() instanceof ObservationStore) {
                return (ObservationStore) omProvider.getMainStore();

            } else if (omProvider.getMainStore() instanceof ResourceOnFileSystem) {
                try {
                    final ResourceOnFileSystem dfStore = (ResourceOnFileSystem) omProvider.getMainStore();
                    final Path[] files = dfStore.getComponentFiles();
                    //for now handle only one file
                    if (files.length > 0) {
                        final Path f = files[0];
                        return new NetcdfObservationStore(f);
                    }
                } catch (DataStoreException ex) {
                    LOGGER.log(Level.WARNING, "Error while retrieving file from datastore:" + omProvider.getId(), ex);
                }
            }
        }
        return null;
    }
}
