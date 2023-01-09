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
import org.constellation.sos.io.lucene.LuceneObservationIndexer;
import org.geotoolkit.index.IndexingException;
import org.geotoolkit.observation.ObservationWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import static java.nio.file.StandardOpenOption.*;
import org.geotoolkit.nio.IOUtilities;
import static org.geotoolkit.observation.AbstractObservationStoreFactory.OBSERVATION_TEMPLATE_ID_BASE_NAME;
import org.geotoolkit.observation.model.Observation;
import org.geotoolkit.observation.model.Offering;
import org.geotoolkit.observation.model.Phenomenon;
import org.geotoolkit.observation.model.ProcedureDataset;
import org.geotoolkit.observation.model.SamplingFeature;
import org.locationtech.jts.geom.Geometry;


/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class FileObservationWriter extends FileObservationHandler implements ObservationWriter {

    private LuceneObservationIndexer indexer;

    private final String observationTemplateIdBase;

    public FileObservationWriter(final Path dataDirectory, final Path configDirectory, final Map<String, Object> properties) throws DataStoreException {
        super(dataDirectory, properties);
        this.observationTemplateIdBase = (String) properties.get(OBSERVATION_TEMPLATE_ID_BASE_NAME);
        
        try {
            indexer        = new LuceneObservationIndexer(dataDirectory, configDirectory, "", true);
        } catch (IndexingException ex) {
            throw new DataStoreException("Indexing exception while initializing the file observation reader", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String writeObservation(final Observation observation) throws DataStoreException {
        if (observation.getName() == null) {
            observation.setName(getNewObservationId());
        }
        String fileName = observation.getName().getCode().replace(':', 'µ');
        final Path observationFile;
        if (observation.getName().getCode().startsWith(observationTemplateIdBase)) {
            observationFile = observationTemplateDirectory.resolve(fileName + '.' + FILE_EXTENSION_JS);
        } else {
            observationFile = observationDirectory.resolve(fileName + '.' + FILE_EXTENSION_JS);
        }
        writeJsonObject(observationFile, observation);
        writePhenomenon(observation.getObservedProperty());
        writeFeatureOfInterest(observation.getFeatureOfInterest());
        return observation.getName().getCode();
    }

    private String getNewObservationId() throws DataStoreException {
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
    public List<String> writeObservations(List<Observation> observations) throws DataStoreException {
        final List<String> results = new ArrayList<>();
        for (Observation observation : observations) {
            final String oid = writeObservation(observation);
            results.add(oid);
        }
        return results;
    }

    @Override
    public void removeObservation(final String observationID) throws DataStoreException {
        final Path observationFile;
        String fileName = observationID.replace(':', 'µ');
        if (observationID.startsWith(observationTemplateIdBase)) {
            observationFile = observationTemplateDirectory.resolve(fileName + '.' + FILE_EXTENSION_JS);
        } else {
            observationFile = observationDirectory.resolve(fileName + '.' + FILE_EXTENSION_JS);
        }
        try {
            Files.deleteIfExists(observationFile);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "unable to find the file to delete observation: "+ observationID, e);
        }
    }

    @Override
    public void removeObservationForProcedure(final String procedureID) throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet in this implementation.");
    }

    @Override
    public void removeProcedure(final String procedureID) throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet in this implementation.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writePhenomenons(final List<Phenomenon> phenomenons) throws DataStoreException {
        for (org.opengis.observation.Phenomenon phenomenon : phenomenons)  {
            if (phenomenon instanceof Phenomenon phen) {
                writePhenomenon(phen);
            } else if (phenomenon != null) {
                LOGGER.log(Level.WARNING, "Bad implementation of phenomenon:{0}", phenomenon.getClass().getName());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeOffering(final Offering offering) throws DataStoreException {
        if (offering == null) return;
        try {
            Files.createDirectories(offeringDirectory);
        } catch (IOException ex) {
            throw new DataStoreException("IO exception creating offering directory  "+offeringDirectory.toString(), ex);
        }
        String fileName = offering.getId().replace(':', 'µ');
        final Path offeringFile = offeringDirectory.resolve(fileName + '.' + FILE_EXTENSION_JS);
        writeJsonObject(offeringFile, offering);
    }

    private void writePhenomenon(final Phenomenon phenomenon) throws DataStoreException {
        if (phenomenon == null) return;
        try {
            Files.createDirectories(phenomenonDirectory);
        } catch (IOException ex) {
            throw new DataStoreException("IO exception creating phenomenon directory  "+phenomenonDirectory.toString(), ex);
        }
        String fileName = phenomenon.getId().replace(':', 'µ');
        final Path phenomenonFile = phenomenonDirectory.resolve(fileName + '.' + FILE_EXTENSION_JS);
        writeJsonObject(phenomenonFile, phenomenon);
    }

    private void writeFeatureOfInterest(final SamplingFeature foi) throws DataStoreException {
        if (foi == null) return;
        try {
            Files.createDirectories(foiDirectory);
        } catch (IOException ex) {
            throw new DataStoreException("IO exception creating foi directory  "+foiDirectory.toString(), ex);
        }
        String fileName = foi.getId().replace(':', 'µ');
        final Path foiFile = foiDirectory.resolve(fileName + '.' + FILE_EXTENSION_JS);
        writeJsonObject(foiFile, foi);
    }

    private void writeJsonObject(Path target, Object object) throws DataStoreException {
        try (OutputStream outputStream = Files.newOutputStream(target, CREATE, WRITE, TRUNCATE_EXISTING)) {
            mapper.writeValue(outputStream, object);
            indexer.indexDocument(object);
        } catch (IOException ex) {
            throw new DataStoreException("IO exception while marshalling the entity file.", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void recordProcedureLocation(final String physicalID, final Geometry position) throws DataStoreException {
        // do nothing
    }

    @Override
    public void writeProcedure(ProcedureDataset procedure) throws DataStoreException {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
        indexer.destroy();
    }

}
