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
import org.constellation.dto.service.config.generic.Automatic;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.observation.ObservationReader;
import org.opengis.temporal.TemporalPrimitive;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.geotoolkit.observation.model.OMEntity;
import org.geotoolkit.observation.model.Observation;
import org.geotoolkit.observation.model.Offering;
import org.geotoolkit.observation.model.Phenomenon;
import org.geotoolkit.observation.model.Procedure;
import org.geotoolkit.observation.model.ResponseMode;
import org.geotoolkit.observation.model.SamplingFeature;
import org.geotoolkit.observation.query.IdentifierQuery;
import org.locationtech.jts.geom.Geometry;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class FileObservationReader extends FileObservationHandler implements ObservationReader {

    public FileObservationReader(final Automatic configuration, final Map<String, Object> properties) throws DataStoreException {
        this(configuration.getDataDirectory(), properties);
    }

    public FileObservationReader(final Path dataDirectory, final Map<String, Object> properties) throws DataStoreException {
        super(dataDirectory, properties);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> getEntityNames(final OMEntity entityType) throws DataStoreException {
        if (entityType == null) {
            throw new DataStoreException("Missing entity type parameter");
        }
        switch (entityType) {
            case FEATURE_OF_INTEREST: return getFeatureOfInterestIds();
            case OBSERVED_PROPERTY:   return getPhenomenonIds();
            case PROCEDURE:           return getProcedureNames();
            case LOCATION:            throw new DataStoreException("not implemented yet.");
            case HISTORICAL_LOCATION: throw new DataStoreException("not implemented yet.");
            case OFFERING:            return getOfferingNames();
            case OBSERVATION:         throw new DataStoreException("not implemented yet.");
            case RESULT:              throw new DataStoreException("not implemented yet.");
            default: throw new DataStoreException("unexpected entity type:" + entityType);
        }
    }

    private Collection<String> getOfferingNames() throws DataStoreException {
        // TODO filter on sensor type
        final List<String> offeringNames = new ArrayList<>();
        if (Files.isDirectory(offeringDirectory)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(offeringDirectory)) {
                for (Path offeringFile : stream) {

                    String ext = IOUtilities.extension(offeringFile);
                    if (!ext.equals(FILE_EXTENSION_JS)) continue;

                    String offeringName = IOUtilities.filenameWithoutExtension(offeringFile);
                    offeringName = offeringName.replace('µ', ':');
                    offeringNames.add(offeringName);
                }
            } catch (IOException e) {
                throw new DataStoreException(e.getMessage(), e);
            }
        }
        return offeringNames;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existEntity(final IdentifierQuery query) throws DataStoreException {
        OMEntity entityType = query.getEntityType();
        if (entityType == null) {
            throw new DataStoreException("Missing entity type parameter");
        }
        String identifier   = query.getIdentifier();
        switch (entityType) {
            case FEATURE_OF_INTEREST: return getFeatureOfInterestIds().contains(identifier);
            case OBSERVED_PROPERTY:   return existPhenomenon(identifier);
            case PROCEDURE:           return existProcedure(identifier);
            case LOCATION:            throw new DataStoreException("not implemented yet.");
            case HISTORICAL_LOCATION: throw new DataStoreException("not implemented yet.");
            case OFFERING:            return getOfferingNames().contains(identifier);
            case OBSERVATION:         throw new DataStoreException("not implemented yet.");
            case RESULT:              throw new DataStoreException("not implemented yet.");
            default: throw new DataStoreException("unexpected entity type:" + entityType);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Offering getObservationOffering(final String identifier) throws DataStoreException {
        if (Files.isDirectory(offeringDirectory)) {
            String fileName = identifier.replace(':', 'µ');
            final Path offeringFile = offeringDirectory.resolve(fileName + '.' + FILE_EXTENSION_JS);
            if (Files.exists(offeringFile)) {
                try (InputStream is = Files.newInputStream(offeringFile)) {
                    return mapper.readValue(is, Offering.class);
                } catch (IOException ex) {
                    throw new DataStoreException("Unable to read the file " + offeringFile, ex);
                }
            }
        }
        return null;
    }

    private Collection<String> getProcedureNames() throws DataStoreException {
        // TODO filter on sensor type
        final List<String> sensorNames = new ArrayList<>();
        if (Files.isDirectory(sensorDirectory)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(sensorDirectory)) {
                for (Path sensorFile : stream) {
                    String sensorName = IOUtilities.filenameWithoutExtension(sensorFile);
                    sensorName = sensorName.replace('µ', ':');
                    sensorNames.add(sensorName);
                }
            } catch (IOException e) {
               throw new DataStoreException("Error during sensor directory scanning", e);
            }
        }
        return sensorNames;
    }

    private Collection<String> getPhenomenonIds() throws DataStoreException {
        final List<String> phenomenonNames = new ArrayList<>();
        if (Files.isDirectory(phenomenonDirectory)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(phenomenonDirectory)) {
                for (Path phenomenonFile : stream) {
                    String ext = IOUtilities.extension(phenomenonFile);
                    if (!ext.equals(FILE_EXTENSION_JS)) continue;

                    String phenId = IOUtilities.filenameWithoutExtension(phenomenonFile);
                    phenId = phenId.replace('µ', ':');
                    phenomenonNames.add(phenId);
                }
            } catch (IOException e) {
                throw new DataStoreException("Error during phenomenon directory scanning", e);
            }
        }
        return phenomenonNames;
    }

    @Override
    public Phenomenon getPhenomenon(String identifier) throws DataStoreException {
        if (Files.isDirectory(phenomenonDirectory)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(phenomenonDirectory)) {
                for (Path phenomenonFile : stream) {
                    String fileName = IOUtilities.filenameWithoutExtension(phenomenonFile);
                    fileName = fileName.replace('µ', ':');
                    // we remove the phenomenon id base
                    if (identifier.contains(phenomenonIdBase)) {
                        identifier = identifier.replace(phenomenonIdBase, "");
                    }
                    if (identifier.equals(fileName)) {
                        try (InputStream is = Files.newInputStream(phenomenonFile)) {
                            return mapper.readValue(is, Phenomenon.class);
                        } catch (IOException e) {
                            throw new DataStoreException("Error during phenomenon reading", e);
                        }
                    }
                }
            } catch (IOException e) {
                throw new DataStoreException("Error during phenomenon directory scanning", e);
            }
        }
        return null;
    }

    @Override
    public Procedure getProcess(String identifier) throws DataStoreException {
        // todo from file
        return new Procedure(identifier);
    }

    private boolean existPhenomenon(String phenomenoniId) throws DataStoreException {
        // we remove the phenomenon id base
        if (phenomenoniId.contains(phenomenonIdBase)) {
            phenomenoniId = phenomenoniId.replace(phenomenonIdBase, "");
        }
        String fileName = phenomenoniId.replace(':', 'µ');
        final Path phenomenonFile = phenomenonDirectory.resolve(fileName + '.' + FILE_EXTENSION_JS);
        return Files.exists(phenomenonFile);
    }

    private Collection<String> getFeatureOfInterestIds() throws DataStoreException {
        final List<String> foiNames = new ArrayList<>();
        if (Files.isDirectory(foiDirectory)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(foiDirectory)) {
                for (Path foiFile : stream) {
                    String ext = IOUtilities.extension(foiFile);
                    if (!ext.equals(FILE_EXTENSION_JS)) continue;

                    String foiName = IOUtilities.filenameWithoutExtension(foiFile);
                    foiName = foiName.replace('µ', ':');
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
    public SamplingFeature getFeatureOfInterest(final String samplingFeatureId) throws DataStoreException {
        String fileName = samplingFeatureId.replace(':', 'µ');
        if (Files.isDirectory(foiDirectory)) {
            final Path samplingFeatureFile = foiDirectory.resolve(fileName + '.' + FILE_EXTENSION_JS);
            if (Files.exists(samplingFeatureFile)) {
                try (InputStream is = Files.newInputStream(samplingFeatureFile)) {
                    return mapper.readValue(is, SamplingFeature.class);
                } catch (IOException ex) {
                    throw new DataStoreException("Unable to read The file " + samplingFeatureFile, ex);
                }
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Observation getObservation(final String identifier, final QName resultModel, final ResponseMode mode) throws DataStoreException {
        Path directory;
        if (mode == ResponseMode.INLINE) {
            directory = observationDirectory;
        } else if (mode == ResponseMode.RESULT_TEMPLATE) {
            directory = observationTemplateDirectory;
        } else {
            throw new DataStoreException("Unsupported responde mode: " + mode);
        }

        if (Files.isDirectory(directory)) {
            String fileName = identifier.replace(':', 'µ');
            Path observationFile = directory.resolve(fileName + '.' + FILE_EXTENSION_JS);
            if (Files.exists(observationFile)) {
                try (InputStream is = Files.newInputStream(observationFile)) {
                    return mapper.readValue(is, Observation.class);
                } catch (IOException ex) {
                    throw new DataStoreException("Unable to read The file " + observationFile, ex);
                }
            }
            throw new DataStoreException("The file " + observationFile + " does not exist");
        }
        throw new DataStoreException("The directory " + observationDirectory + " does not exist");
    }

    /**
     * {@inheritDoc}
     */
    private boolean existProcedure(final String href) throws DataStoreException {
        if (Files.isDirectory(sensorDirectory)) {

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(sensorDirectory)) {
                for (Path sensorFile : stream) {
                    String sensorName = IOUtilities.filenameWithoutExtension(sensorFile);
                    sensorName = sensorName.replace('µ', ':');
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
    public TemporalPrimitive getEventTime() throws DataStoreException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TemporalPrimitive getFeatureOfInterestTime(final String samplingFeatureName) throws DataStoreException {
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
    public Geometry getSensorLocation(String sensorID) throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet in this implementation.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Date, Geometry> getSensorLocations(String sensorID) throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet in this implementation.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TemporalPrimitive getProcedureTime(final String sensorID) throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet in this implementation.");
    }

    @Override
    public Observation getTemplateForProcedure(String procedure) throws DataStoreException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(observationTemplateDirectory)) {
            for (Path templateFile : stream) {
                String ext = IOUtilities.extension(templateFile);
                if (!ext.equals(FILE_EXTENSION_JS)) continue;
                try (InputStream is = Files.newInputStream(templateFile)) {
                    Observation obs = mapper.readValue(is, Observation.class);
                    final String processID = obs.getProcedure().getId();
                    if (processID.equals(procedure)) {
                        return obs;
                    }
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
