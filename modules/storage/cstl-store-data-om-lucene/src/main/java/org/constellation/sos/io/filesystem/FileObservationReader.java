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

package org.constellation.sos.io.filesystem;


import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.xml.MarshallerPool;
import org.constellation.dto.service.config.generic.Automatic;
import org.geotoolkit.gml.xml.AbstractGeometry;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.observation.ObservationReader;
import org.geotoolkit.sos.xml.ObservationOffering;
import org.geotoolkit.sos.xml.ResponseModeType;
import org.geotoolkit.sos.xml.SOSMarshallerPool;
import org.geotoolkit.swe.xml.DataArrayProperty;
import org.opengis.observation.Observation;
import org.opengis.observation.sampling.SamplingFeature;
import org.opengis.temporal.TemporalGeometricPrimitive;
import org.opengis.temporal.TemporalPrimitive;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.constellation.api.CommonConstants;
import org.geotoolkit.sos.xml.SOSXmlFactory;
import org.opengis.observation.Phenomenon;
import org.opengis.observation.Process;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class FileObservationReader implements ObservationReader {

     /**
     * use for debugging purpose
     */
    protected static final Logger LOGGER = Logging.getLogger("org.constellation.sos");

    /**
     * The base for observation id.
     */
    protected final String observationIdBase;

    protected final String phenomenonIdBase;

    private Path offeringDirectory;

    private Path phenomenonDirectory;

    private Path observationDirectory;

    private Path observationTemplateDirectory;

    private Path sensorDirectory;

    private Path foiDirectory;

    private static final MarshallerPool MARSHALLER_POOL;
    static {
        MARSHALLER_POOL = SOSMarshallerPool.getInstance();
    }

    private static final String FILE_EXTENSION = ".xml";

    public FileObservationReader(final Automatic configuration, final Map<String, Object> properties) throws DataStoreException {
        this(configuration.getDataDirectory(), properties);
    }

    public FileObservationReader(final Path dataDirectory, final Map<String, Object> properties) throws DataStoreException {
        this.observationIdBase = (String) properties.get(CommonConstants.OBSERVATION_ID_BASE);
        this.phenomenonIdBase  = (String) properties.get(CommonConstants.PHENOMENON_ID_BASE);
        if (Files.isDirectory(dataDirectory)) {
            offeringDirectory            = dataDirectory.resolve("offerings");
            phenomenonDirectory          = dataDirectory.resolve("phenomenons");
            observationDirectory         = dataDirectory.resolve("observations");
            observationTemplateDirectory = dataDirectory.resolve("observationTemplates");
            sensorDirectory              = dataDirectory.resolve("sensors");
            foiDirectory                 = dataDirectory.resolve("features");
        } else {
            throw new DataStoreException("There is no data Directory");
        }
        if (MARSHALLER_POOL == null) {
            throw new DataStoreException("JAXB exception while initializing the file observation reader");
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> getOfferingNames(final String version) throws DataStoreException {
        final List<String> offeringNames = new ArrayList<>();
        if (Files.isDirectory(offeringDirectory)) {
            final Path offeringVersionDir = offeringDirectory.resolve(version);
            if (Files.isDirectory(offeringVersionDir)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(offeringVersionDir)) {
                    for (Path offeringFile : stream) {
                        String offeringName = offeringFile.getFileName().toString();
                        offeringName = offeringName.substring(0, offeringName.indexOf(FILE_EXTENSION));
                        offeringNames.add(offeringName);
                    }
                } catch (IOException e) {
                    throw new DataStoreException(e.getMessage(), e);
                }
            }
        }
        return offeringNames;
    }

    @Override
    public Collection<String> getOfferingNames(String version, String sensorType) throws DataStoreException {
        // no filter yet
        return getOfferingNames(version);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ObservationOffering> getObservationOfferings(final List<String> offeringNames, final String version) throws DataStoreException {
        final List<ObservationOffering> offerings = new ArrayList<>();
        for (String offeringName : offeringNames) {
            offerings.add(getObservationOffering(offeringName, version));
        }
        return offerings;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ObservationOffering getObservationOffering(final String offeringName, final String version) throws DataStoreException {
        final Path offeringVersionDir = offeringDirectory.resolve(version);
        if (Files.isDirectory(offeringVersionDir)) {
            final Path offeringFile = offeringVersionDir.resolve(offeringName + FILE_EXTENSION);
            if (Files.exists(offeringFile)) {
                try (InputStream is = Files.newInputStream(offeringFile)) {
                    final Unmarshaller unmarshaller = MARSHALLER_POOL.acquireUnmarshaller();
                    Object obj = unmarshaller.unmarshal(is);
                    MARSHALLER_POOL.recycle(unmarshaller);
                    if (obj instanceof JAXBElement) {
                        obj = ((JAXBElement)obj).getValue();
                    }
                    if (obj instanceof ObservationOffering) {
                        return (ObservationOffering) obj;
                    }
                    throw new DataStoreException("The file " + offeringFile + " does not contains an offering Object.");
                } catch (JAXBException | IOException ex) {
                    throw new DataStoreException("Unable to unmarshall The file " + offeringFile, ex);
                }
            }
        } else {
            throw new DataStoreException("Unsuported version:" + version);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ObservationOffering> getObservationOfferings(final String version) throws DataStoreException {
        final List<ObservationOffering> offerings = new ArrayList<>();
        if (Files.exists(offeringDirectory)) {
            final Path offeringVersionDir = offeringDirectory.resolve(version);
            if (Files.isDirectory(offeringVersionDir)) {

                try (DirectoryStream<Path> stream = Files.newDirectoryStream(offeringVersionDir)) {
                    for (Path offeringFile : stream) {

                        try (InputStream is = Files.newInputStream(offeringFile)) {
                            final Unmarshaller unmarshaller = MARSHALLER_POOL.acquireUnmarshaller();
                            Object obj = unmarshaller.unmarshal(is);
                            MARSHALLER_POOL.recycle(unmarshaller);
                            if (obj instanceof JAXBElement) {
                                obj = ((JAXBElement)obj).getValue();
                            }
                            if (obj instanceof ObservationOffering) {
                                offerings.add((ObservationOffering) obj);
                            } else {
                                throw new DataStoreException("The file " + offeringFile + " does not contains an offering Object.");
                            }
                        } catch (JAXBException ex) {
                            String msg = ex.getMessage();
                            if (msg == null && ex.getCause() != null) {
                                msg = ex.getCause().getMessage();
                            }
                            LOGGER.warning("Unable to unmarshall The file " + offeringFile + " cause:" + msg);
                        } catch (IOException ex) {
                            String msg = ex.getMessage();
                            if (msg == null && ex.getCause() != null) {
                                msg = ex.getCause().getMessage();
                            }
                            LOGGER.warning("Unable to read The file " + offeringFile + " cause:" + msg);
                        }
                    }
                } catch (IOException e) {
                    throw new DataStoreException("Error during scan of directory "+offeringVersionDir.toString(), e);
                }
            } else {
                throw new DataStoreException("Unsuported version:" + version);
            }
        }
        return offerings;
    }

    @Override
    public List<ObservationOffering> getObservationOfferings(String version, String sensorType) throws DataStoreException {
        // no filter yet
        return getObservationOfferings(version);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> getProcedureNames() throws DataStoreException {
        final List<String> sensorNames = new ArrayList<>();
        if (Files.isDirectory(sensorDirectory)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(sensorDirectory)) {
                for (Path sensorFile : stream) {
                    String sensorName = sensorFile.getFileName().toString();
                    sensorName = sensorName.substring(0, sensorName.indexOf(FILE_EXTENSION));
                    sensorNames.add(sensorName);
                }
            } catch (IOException e) {
               throw new DataStoreException("Error during sensor directory scanning", e);
            }
        }
        return sensorNames;
    }

    @Override
    public Collection<String> getProcedureNames(String sensorType) throws DataStoreException {
         // no filter yet
        return getProcedureNames();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> getPhenomenonNames() throws DataStoreException {
        final List<String> phenomenonNames = new ArrayList<>();
        if (Files.isDirectory(phenomenonDirectory)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(phenomenonDirectory)) {
                for (Path phenomenonFile : stream) {
                    String phenomenonName = phenomenonFile.getFileName().toString();
                    phenomenonName = phenomenonName.substring(0, phenomenonName.indexOf(FILE_EXTENSION));
                    phenomenonNames.add(phenomenonName);
                }
            } catch (IOException e) {
                throw new DataStoreException("Error during phenomenon directory scanning", e);
            }
        }
        return phenomenonNames;
    }

    @Override
    public Collection<Phenomenon> getPhenomenons(String version) throws DataStoreException {
        final List<Phenomenon> results = new ArrayList<>();
        if (Files.isDirectory(phenomenonDirectory)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(phenomenonDirectory)) {
                for (Path phenomenonFile : stream) {
                    try (InputStream is = Files.newInputStream(phenomenonFile)) {
                        final Unmarshaller unmarshaller = MARSHALLER_POOL.acquireUnmarshaller();
                        Object obj = unmarshaller.unmarshal(is);
                        MARSHALLER_POOL.recycle(unmarshaller);
                        if (obj instanceof JAXBElement) {
                            obj = ((JAXBElement)obj).getValue();
                        }
                        if (obj instanceof Phenomenon) {
                            results.add((Phenomenon) obj);
                        }
                    } catch (IOException | JAXBException e) {
                        throw new DataStoreException("Error during phenomenon umarshalling", e);
                    }
                }
            } catch (IOException e) {
                throw new DataStoreException("Error during phenomenon directory scanning", e);
            }
        }
        return results;
    }

    @Override
    public Phenomenon getPhenomenon(String identifier, String version) throws DataStoreException {
        if (Files.isDirectory(phenomenonDirectory)) {
            Path phenomenonFile = phenomenonDirectory.resolve(identifier + FILE_EXTENSION);
            if (Files.isRegularFile(phenomenonFile)) {
                try (InputStream is = Files.newInputStream(phenomenonFile)) {
                    final Unmarshaller unmarshaller = MARSHALLER_POOL.acquireUnmarshaller();
                    Object obj = unmarshaller.unmarshal(is);
                    MARSHALLER_POOL.recycle(unmarshaller);
                    if (obj instanceof JAXBElement) {
                        obj = ((JAXBElement)obj).getValue();
                    }
                    if (obj instanceof Phenomenon) {
                        return (Phenomenon) obj;
                    }
                } catch (IOException | JAXBException e) {
                    throw new DataStoreException("Error during phenomenon umarshalling", e);
                }
            }
        }
        return null;
    }

    @Override
    public Process getProcess(String identifier, String version) throws DataStoreException {
        return SOSXmlFactory.buildProcess(version, identifier);
    }

    @Override
    public Collection<String> getProceduresForPhenomenon(String observedProperty) throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet in this implementation.");
    }

    @Override
    public Collection<String> getPhenomenonsForProcedure(String sensorID) throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet in this implementation.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existPhenomenon(String phenomenonName) throws DataStoreException {
        if (phenomenonName.equals(phenomenonIdBase + "ALL")) {
            return true;
        }
        // we remove the phenomenon id base
        if (phenomenonName.contains(phenomenonIdBase)) {
            phenomenonName = phenomenonName.replace(phenomenonIdBase, "");
        }
        final Path phenomenonFile = phenomenonDirectory.resolve(phenomenonName + FILE_EXTENSION);
        return Files.exists(phenomenonFile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> getFeatureOfInterestNames() throws DataStoreException {
        final List<String> foiNames = new ArrayList<>();
        if (Files.isDirectory(foiDirectory)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(foiDirectory)) {
                for (Path foiFile : stream) {
                    String foiName = foiFile.getFileName().toString();
                    foiName = foiName.substring(0, foiName.indexOf(FILE_EXTENSION));
                    foiNames.add(foiName);
                }
            } catch (IOException e) {
                throw new DataStoreException("Error during foi directory scanning", e);
            }
        }
        return foiNames;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SamplingFeature getFeatureOfInterest(final String samplingFeatureName, final String version) throws DataStoreException {
        final Path samplingFeatureFile =foiDirectory.resolve(samplingFeatureName + FILE_EXTENSION);
        if (Files.exists(samplingFeatureFile)) {
            try (InputStream is = Files.newInputStream(samplingFeatureFile)) {
                final Unmarshaller unmarshaller = MARSHALLER_POOL.acquireUnmarshaller();
                Object obj = unmarshaller.unmarshal(is);
                MARSHALLER_POOL.recycle(unmarshaller);
                if (obj instanceof JAXBElement) {
                    obj = ((JAXBElement)obj).getValue();
                }
                if (obj instanceof SamplingFeature) {
                    return (SamplingFeature) obj;
                }
                throw new DataStoreException("The file " + samplingFeatureFile + " does not contains an foi Object.");
            } catch (JAXBException ex) {
                throw new DataStoreException("Unable to unmarshall The file " + samplingFeatureFile, ex);
            } catch (IOException ex) {
                throw new DataStoreException("Unable to read The file " + samplingFeatureFile, ex);
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Observation getObservation(final String identifier, final QName resultModel, final ResponseModeType mode, final String version) throws DataStoreException {
        Path observationFile = observationDirectory.resolve(identifier + FILE_EXTENSION);
        if (!Files.exists(observationFile)) {
            observationFile = observationTemplateDirectory.resolve(identifier + FILE_EXTENSION);
        }
        if (Files.exists(observationFile)) {
            try (InputStream is = Files.newInputStream(observationFile)) {
                final Unmarshaller unmarshaller = MARSHALLER_POOL.acquireUnmarshaller();
                Object obj = unmarshaller.unmarshal(is);
                MARSHALLER_POOL.recycle(unmarshaller);
                if (obj instanceof JAXBElement) {
                    obj = ((JAXBElement)obj).getValue();
                }
                if (obj instanceof Observation) {
                    return (Observation) obj;
                }
                throw new DataStoreException("The file " + observationFile + " does not contains an observation Object.");
            } catch (JAXBException ex) {
                throw new DataStoreException("Unable to unmarshall The file " + observationFile, ex);
            } catch (IOException ex) {
                throw new DataStoreException("Unable to read The file " + observationFile, ex);
            }
        }
        throw new DataStoreException("The file " + observationFile + " does not exist");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getResult(final String identifier, final QName resultModel, final String version) throws DataStoreException {
        final Path anyResultFile = observationDirectory.resolve(identifier + FILE_EXTENSION);
        if (Files.exists(anyResultFile)) {

            try (InputStream is = Files.newInputStream(anyResultFile)) {
                final Unmarshaller unmarshaller = MARSHALLER_POOL.acquireUnmarshaller();
                Object obj = unmarshaller.unmarshal(is);
                MARSHALLER_POOL.recycle(unmarshaller);
                if (obj instanceof JAXBElement) {
                    obj = ((JAXBElement)obj).getValue();
                }
                if (obj instanceof Observation) {
                    final Observation obs = (Observation) obj;
                    final DataArrayProperty arrayP = (DataArrayProperty) obs.getResult();
                    return arrayP.getDataArray();
                }
                throw new DataStoreException("The file " + anyResultFile + " does not contains an observation Object.");
            } catch (JAXBException ex) {
                throw new DataStoreException("Unable to unmarshall The file " + anyResultFile, ex);
            } catch (IOException ex) {
                throw new DataStoreException("Unable to read The file " + anyResultFile, ex);
            }
        }
        throw new DataStoreException("The file " + anyResultFile + " does not exist");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existProcedure(final String href) throws DataStoreException {
        if (Files.isDirectory(sensorDirectory)) {

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(sensorDirectory)) {
                for (Path sensorFile : stream) {
                    String sensorName = sensorFile.getFileName().toString();
                    sensorName = sensorName.substring(0, sensorName.indexOf(FILE_EXTENSION));
                    if (sensorName.equals(href)) {
                        return true;
                    }
                }
            } catch (IOException e) {
                throw new DataStoreException("Error while reading sensor directory", e);
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNewObservationId() throws DataStoreException {
        String obsID = null;
        boolean exist = true;
        try {
            long i = IOUtilities.listChildren(observationDirectory).size();
            while (exist) {
                obsID = observationIdBase + i;
                final Path newFile = observationDirectory.resolve(obsID);
                exist = Files.exists(newFile);
                i++;
            }
            return obsID;
        } catch (IOException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getEventTime() throws DataStoreException {
        return Arrays.asList("undefined", "now");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TemporalPrimitive getFeatureOfInterestTime(final String samplingFeatureName, final String version) throws DataStoreException {
        throw new DataStoreException("The Filesystem implementation of SOS does not support GetFeatureofInterestTime");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
        // nothing to destroy
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getInfos() {
        return "Constellation Filesystem O&M Reader 0.9";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ResponseModeType> getResponseModes() throws DataStoreException {
        return Arrays.asList(ResponseModeType.INLINE, ResponseModeType.RESULT_TEMPLATE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getResponseFormats() throws DataStoreException {
        return Arrays.asList("text/xml; subtype=\"om/1.0.0\"");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractGeometry getSensorLocation(String sensorID, String version) throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet in this implementation.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TemporalGeometricPrimitive getTimeForProcedure(final String version, final String sensorID) throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet in this implementation.");
    }

    @Override
    public Observation getTemplateForProcedure(String procedure, String version) throws DataStoreException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(observationTemplateDirectory)) {
            for (Path templateFile : stream) {
                try (InputStream is = Files.newInputStream(templateFile)){
                    final Unmarshaller unmarshaller = MARSHALLER_POOL.acquireUnmarshaller();
                    Object obj = unmarshaller.unmarshal(is);
                    MARSHALLER_POOL.recycle(unmarshaller);
                    if (obj instanceof JAXBElement) {
                        obj = ((JAXBElement)obj).getValue();
                    }
                    if (obj instanceof Observation) {
                        final Observation obs = (Observation) obj;
                        if (obs.getProcedure() instanceof org.geotoolkit.observation.xml.Process) {
                            final String processID = ((org.geotoolkit.observation.xml.Process)obs.getProcedure()).getHref();
                            if (processID.equals(procedure)) {
                                return obs;
                            }
                        }
                    }
                } catch (JAXBException ex) {
                    throw new DataStoreException("Unable to unmarshall The file " + templateFile, ex);
                } catch (IOException ex) {
                    throw new DataStoreException("Unable to read The file " + templateFile, ex);
                }
            }
        } catch (IOException e) {
            throw new DataStoreException("An error occurs while scanning observation template directory");
        }
        return null;
    }

    @Override
    public Collection<SamplingFeature> getFeatureOfInterestForProcedure(String sensorID, String version) throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
