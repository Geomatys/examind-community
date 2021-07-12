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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import static org.constellation.api.CommonConstants.RESPONSE_FORMAT_V100_XML;
import static org.constellation.api.CommonConstants.RESPONSE_FORMAT_V200_XML;
import static org.geotoolkit.observation.AbstractObservationStoreFactory.OBSERVATION_ID_BASE_NAME;
import static org.geotoolkit.observation.AbstractObservationStoreFactory.PHENOMENON_ID_BASE_NAME;
import org.geotoolkit.observation.OMEntity;
import static org.geotoolkit.observation.ObservationReader.ENTITY_TYPE;
import static org.geotoolkit.observation.ObservationReader.SENSOR_TYPE;
import static org.geotoolkit.observation.ObservationReader.SOS_VERSION;
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
        this.observationIdBase = (String) properties.get(OBSERVATION_ID_BASE_NAME);
        this.phenomenonIdBase  = (String) properties.get(PHENOMENON_ID_BASE_NAME);
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
    public Collection<String> getEntityNames(final Map<String, Object> hints) throws DataStoreException {
        OMEntity entityType = (OMEntity) hints.get(ENTITY_TYPE);
        if (entityType == null) {
            throw new DataStoreException("Missing entity type parameter");
        }
        String sensorType   = (String) hints.get(SENSOR_TYPE);
        String version      = (String) hints.get(SOS_VERSION);
        switch (entityType) {
            case FEATURE_OF_INTEREST: return getFeatureOfInterestNames();
            case OBSERVED_PROPERTY:   return getPhenomenonNames();
            case PROCEDURE:           return getProcedureNames(sensorType);
            case LOCATION:            throw new DataStoreException("not implemented yet.");
            case OFFERING:            return getOfferingNames(version, sensorType);
            case OBSERVATION:         throw new DataStoreException("not implemented yet.");
            case RESULT:              throw new DataStoreException("not implemented yet.");
            default: throw new DataStoreException("unexpected entity type:" + entityType);
        }
    }

    private Collection<String> getOfferingNames(final String version, String sensorType) throws DataStoreException {
        // TODO filter on sensor type
        final List<String> offeringNames = new ArrayList<>();
        if (Files.isDirectory(offeringDirectory)) {
            final Path offeringVersionDir = offeringDirectory.resolve(version);
            if (Files.isDirectory(offeringVersionDir)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(offeringVersionDir)) {
                    for (Path offeringFile : stream) {
                        String offeringName = offeringFile.getFileName().toString();
                        offeringName = offeringName.replace('µ', ':');
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

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existEntity(final Map<String, Object> hints) throws DataStoreException {
        OMEntity entityType = (OMEntity) hints.get(ENTITY_TYPE);
        if (entityType == null) {
            throw new DataStoreException("Missing entity type parameter");
        }
        String identifier   = (String) hints.get(IDENTIFIER);
        String sensorType   = (String) hints.get(SENSOR_TYPE);
        String version      = (String) hints.get(SOS_VERSION);
        switch (entityType) {
            case FEATURE_OF_INTEREST: return getFeatureOfInterestNames().contains(identifier);
            case OBSERVED_PROPERTY:   return existPhenomenon(identifier);
            case PROCEDURE:           return existProcedure(identifier);
            case LOCATION:            throw new DataStoreException("not implemented yet.");
            case OFFERING:            return getOfferingNames(version, sensorType).contains(identifier);
            case OBSERVATION:         throw new DataStoreException("not implemented yet.");
            case RESULT:              throw new DataStoreException("not implemented yet.");
            default: throw new DataStoreException("unexpected entity type:" + entityType);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ObservationOffering> getObservationOfferings(final Map<String, Object> hints) throws DataStoreException {
        String sensorType   = (String) hints.get(SENSOR_TYPE);
        String version      = (String) hints.get(SOS_VERSION);
        Object identifierVal = hints.get(IDENTIFIER);
        List<String> identifiers = new ArrayList<>();
        if (identifierVal instanceof Collection) {
            identifiers.addAll((Collection<? extends String>) identifierVal);
        } else if (identifierVal instanceof String) {
            identifiers.add((String) identifierVal);
        } else if (identifierVal == null) {
            identifiers.addAll(getOfferingNames(version, sensorType));
        }
        final List<ObservationOffering> offerings = new ArrayList<>();
        for (String offeringName : identifiers) {
            ObservationOffering off = getObservationOffering(offeringName, version);
            if (off != null) {
                offerings.add(off);
            }
        }
        return offerings;
    }

    private ObservationOffering getObservationOffering(final String offeringName, final String version) throws DataStoreException {
        final Path offeringVersionDir = offeringDirectory.resolve(version);
        if (Files.isDirectory(offeringVersionDir)) {
            String fileName = offeringName.replace(':', 'µ');
            final Path offeringFile = offeringVersionDir.resolve(fileName + FILE_EXTENSION);
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

    private Collection<String> getProcedureNames(String sensorType) throws DataStoreException {
        // TODO filter on sensor type
        final List<String> sensorNames = new ArrayList<>();
        if (Files.isDirectory(sensorDirectory)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(sensorDirectory)) {
                for (Path sensorFile : stream) {
                    String sensorName = sensorFile.getFileName().toString();
                    sensorName = sensorName.replace('µ', ':');
                    sensorName = sensorName.substring(0, sensorName.indexOf(FILE_EXTENSION));
                    sensorNames.add(sensorName);
                }
            } catch (IOException e) {
               throw new DataStoreException("Error during sensor directory scanning", e);
            }
        }
        return sensorNames;
    }

    private Collection<String> getPhenomenonNames() throws DataStoreException {
        final List<String> phenomenonNames = new ArrayList<>();
        if (Files.isDirectory(phenomenonDirectory)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(phenomenonDirectory)) {
                for (Path phenomenonFile : stream) {
                    String phenomenonName = phenomenonFile.getFileName().toString();
                    phenomenonName = phenomenonName.replace('µ', ':');
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
    public Collection<Phenomenon> getPhenomenons(final Map<String, Object> hints) throws DataStoreException {
        String version       = (String) hints.get(SOS_VERSION);
        Object identifierVal = hints.get(IDENTIFIER);
        List<String> identifiers = new ArrayList<>();
        if (identifierVal instanceof Collection) {
            identifiers.addAll((Collection<? extends String>) identifierVal);
        } else if (identifierVal instanceof String) {
            identifiers.add((String) identifierVal);
        } 
        final List<Phenomenon> results = new ArrayList<>();
        if (Files.isDirectory(phenomenonDirectory)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(phenomenonDirectory)) {
                for (Path phenomenonFile : stream) {
                    String fileName = IOUtilities.filenameWithoutExtension(phenomenonFile);
                    fileName = fileName.replace('µ', ':');
                    if (identifiers.isEmpty() || identifiers.contains(fileName)) {
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
                }
            } catch (IOException e) {
                throw new DataStoreException("Error during phenomenon directory scanning", e);
            }
        }
        return results;
    }

    @Override
    public Process getProcess(String identifier, String version) throws DataStoreException {
        return SOSXmlFactory.buildProcess(version, identifier);
    }

    private boolean existPhenomenon(String phenomenonName) throws DataStoreException {
        if (phenomenonName.equals(phenomenonIdBase + "ALL")) {
            return true;
        }
        // we remove the phenomenon id base
        if (phenomenonName.contains(phenomenonIdBase)) {
            phenomenonName = phenomenonName.replace(phenomenonIdBase, "");
        }
        String fileName = phenomenonName.replace(':', 'µ');
        final Path phenomenonFile = phenomenonDirectory.resolve(fileName + FILE_EXTENSION);
        return Files.exists(phenomenonFile);
    }

    private Collection<String> getFeatureOfInterestNames() throws DataStoreException {
        final List<String> foiNames = new ArrayList<>();
        if (Files.isDirectory(foiDirectory)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(foiDirectory)) {
                for (Path foiFile : stream) {
                    String foiName = foiFile.getFileName().toString();
                    foiName = foiName.replace('µ', ':');
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
        String fileName = samplingFeatureName.replace(':', 'µ');
        final Path samplingFeatureFile =foiDirectory.resolve(fileName + FILE_EXTENSION);
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
        String fileName = identifier.replace(':', 'µ');
        Path observationFile = observationDirectory.resolve(fileName + FILE_EXTENSION);
        if (!Files.exists(observationFile)) {
            observationFile = observationTemplateDirectory.resolve(fileName + FILE_EXTENSION);
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
        String fileName = identifier.replace(':', 'µ');
        final Path anyResultFile = observationDirectory.resolve(fileName + FILE_EXTENSION);
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
    private boolean existProcedure(final String href) throws DataStoreException {
        if (Files.isDirectory(sensorDirectory)) {

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(sensorDirectory)) {
                for (Path sensorFile : stream) {
                    String sensorName = sensorFile.getFileName().toString();
                    sensorName = sensorName.replace('µ', ':');
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
                String fileName = obsID.replace(':', 'µ');
                final Path newFile = observationDirectory.resolve(fileName);
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
    public TemporalPrimitive getEventTime(String version) throws DataStoreException {
        return null;
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
    public Map<String, List<String>> getResponseFormats() throws DataStoreException {
        final Map<String, List<String>> results = new HashMap<>();
        results.put("1.0.0", Arrays.asList(RESPONSE_FORMAT_V100_XML));
        results.put("2.0.0", Arrays.asList(RESPONSE_FORMAT_V200_XML));
        return results;
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
    public Map<Date, AbstractGeometry> getSensorLocations(String sensorID, String version) throws DataStoreException {
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
}
