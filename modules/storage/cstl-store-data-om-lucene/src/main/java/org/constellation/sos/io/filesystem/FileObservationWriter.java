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

// J2SE dependencies

import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.xml.MarshallerPool;
import org.constellation.sos.io.lucene.LuceneObservationIndexer;
import org.geotoolkit.gml.xml.AbstractGeometry;
import org.geotoolkit.index.IndexingException;
import org.geotoolkit.observation.ObservationWriter;
import org.geotoolkit.sampling.xml.SamplingFeature;
import org.geotoolkit.sos.xml.ObservationOffering;
import org.geotoolkit.sos.xml.SOSMarshallerPool;
import org.geotoolkit.swe.xml.Phenomenon;
import org.geotoolkit.observation.model.ObservationTemplate;
import org.opengis.observation.Observation;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.file.StandardOpenOption.*;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.geotoolkit.nio.IOUtilities;
import static org.geotoolkit.observation.AbstractObservationStoreFactory.OBSERVATION_ID_BASE_NAME;
import static org.geotoolkit.observation.AbstractObservationStoreFactory.OBSERVATION_TEMPLATE_ID_BASE_NAME;
import org.geotoolkit.observation.xml.AbstractObservation;
import org.geotoolkit.sos.netcdf.ExtractionResult;
import org.opengis.metadata.Identifier;


/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class FileObservationWriter implements ObservationWriter {

    private Path offeringDirectory;

    private Path phenomenonDirectory;

    private Path observationDirectory;

    private Path observationTemplateDirectory;

    //private File sensorDirectory;

    private Path foiDirectory;

    //private File resultDirectory;

    private static final MarshallerPool MARSHALLER_POOL;
    static {
        MARSHALLER_POOL = SOSMarshallerPool.getInstance();
    }

    private LuceneObservationIndexer indexer;

    private final String observationTemplateIdBase;

    private final String observationIdBase;

    private static final String FILE_EXTENSION = ".xml";

    private static final Logger LOGGER = Logging.getLogger("org.constellation.sos.io.filesystem");

    public FileObservationWriter(final Path dataDirectory, final Path configDirectory, final Map<String, Object> properties) throws DataStoreException {
        super();
        this.observationTemplateIdBase = (String) properties.get(OBSERVATION_TEMPLATE_ID_BASE_NAME);
        this.observationIdBase         = (String) properties.get(OBSERVATION_ID_BASE_NAME);
        if (Files.exists(dataDirectory)) {
            offeringDirectory    = dataDirectory.resolve("offerings");
            phenomenonDirectory  = dataDirectory.resolve("phenomenons");
            observationDirectory = dataDirectory.resolve("observations");
            //sensorDirectory      = dataDirectory.resolve("sensors");
            foiDirectory         = dataDirectory.resolve("features");
            //resultDirectory      = dataDirectory.resolve("results");
            observationTemplateDirectory = dataDirectory.resolve("observationTemplates");

        }
        if (MARSHALLER_POOL == null) {
            throw new DataStoreException("JAXB exception while initializing the file observation reader");
        }
        try {
            indexer        = new LuceneObservationIndexer(dataDirectory, configDirectory, "", true);
        } catch (IndexingException ex) {
            throw new DataStoreException("Indexing exception while initializing the file observation reader", ex);
        }
    }

    @Override
    public String writeObservationTemplate(final ObservationTemplate template) throws DataStoreException {
        final Observation observation = template.getObservation();
        if (observation == null) {
            return null;
        }
        String fileName = observation.getName().getCode().replace(':', 'µ');
        final Path observationFile = observationTemplateDirectory.resolve(fileName + FILE_EXTENSION);
        return writeObservationToFile(observation, observationFile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String writeObservation(final Observation observation) throws DataStoreException {
        final Path observationFile;
        if (observation instanceof AbstractObservation && observation.getName() == null) {
            ((AbstractObservation)observation).setName(getNewObservationId());
        }
        String fileName = observation.getName().getCode().replace(':', 'µ');
        if (observation.getName().getCode().startsWith(observationTemplateIdBase)) {
            observationFile = observationTemplateDirectory.resolve(fileName + FILE_EXTENSION);
        } else {
            observationFile = observationDirectory.resolve(fileName + FILE_EXTENSION);
        }
        return writeObservationToFile(observation, observationFile);
    }

    private String writeObservationToFile(Observation observation, Path observationFile) throws DataStoreException {
        if (Files.exists(observationFile)) {
            try {
                Files.createFile(observationFile);
            } catch (IOException e) {
                throw new DataStoreException("unable to create an observation file.");
            }
        } else {
            LOGGER.log(Level.WARNING, "we overwrite the file:{0}", observationFile.toString());
        }

        try (OutputStream os = Files.newOutputStream(observationFile, CREATE, WRITE, TRUNCATE_EXISTING)) {
            final Marshaller marshaller = MARSHALLER_POOL.acquireMarshaller();
            marshaller.marshal(observation, os);
            MARSHALLER_POOL.recycle(marshaller);

            writePhenomenon((Phenomenon) observation.getObservedProperty());
            if (observation.getFeatureOfInterest() != null) {
                writeFeatureOfInterest((SamplingFeature) observation.getFeatureOfInterest());
            }
            indexer.indexDocument(observation);
            return observation.getName().getCode();
        } catch (JAXBException | IOException ex) {
            throw new DataStoreException("Exception while marshalling the observation file.", ex);
        }
    }

    private Identifier getNewObservationId() throws DataStoreException {
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
            return new DefaultIdentifier(obsID);
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
            observationFile = observationTemplateDirectory.resolve(fileName + FILE_EXTENSION);
        } else {
            observationFile = observationDirectory.resolve(fileName + FILE_EXTENSION);
        }
        try {
            Files.deleteIfExists(observationFile);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "unable to find t he fiel to delete:"+ observationFile.toString(), e);
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

    private void writeObject(Path target, Object object, String objectType) throws DataStoreException {

        try (OutputStream outputStream = Files.newOutputStream(target, CREATE, WRITE, TRUNCATE_EXISTING)) {
            final Marshaller marshaller = MARSHALLER_POOL.acquireMarshaller();
            marshaller.marshal(object, outputStream);
            MARSHALLER_POOL.recycle(marshaller);
        } catch (JAXBException ex) {
            throw new DataStoreException("JAXB exception while marshalling the "+objectType+" file.", ex);
        } catch (IOException ex) {
            throw new DataStoreException("IO exception while marshalling the "+objectType+" file.", ex);
        }
    }

    private void writePhenomenon(final Phenomenon phenomenon) throws DataStoreException {
        try {
            if (!Files.exists(phenomenonDirectory)) {
                Files.createDirectories(phenomenonDirectory);
            }
        } catch (IOException ex) {
            throw new DataStoreException("IO exception creating phenomenon directory  "+phenomenonDirectory.toString(), ex);
        }
        String fileName = phenomenon.getName().getCode().replace(':', 'µ');
        final Path phenomenonFile = phenomenonDirectory.resolve(fileName + FILE_EXTENSION);
        writeObject(phenomenonFile, phenomenon, "phenomenon");
    }

    private void writeFeatureOfInterest(final SamplingFeature foi) throws DataStoreException {
        try {
            if (!Files.exists(foiDirectory)) {
                Files.createDirectories(foiDirectory);
            }
        } catch (IOException ex) {
            throw new DataStoreException("IO exception creating foi directory  "+foiDirectory.toString(), ex);
        }
        String fileName = foi.getId().replace(':', 'µ');
        final Path foiFile = foiDirectory.resolve(fileName + FILE_EXTENSION);
        writeObject(foiFile, foi, "foi");
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writePhenomenons(final List<org.opengis.observation.Phenomenon> phenomenons) throws DataStoreException {
        for (org.opengis.observation.Phenomenon phenomenon : phenomenons)  {
            if (phenomenon instanceof Phenomenon) {
                writePhenomenon((Phenomenon)phenomenon);
            } else if (phenomenon != null) {
                LOGGER.log(Level.WARNING, "Bad implementation of phenomenon:{0}", phenomenon.getClass().getName());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String writeOffering(final ObservationOffering offering) throws DataStoreException {
        try {
            if (!Files.exists(offeringDirectory)) {
                Files.createDirectories(offeringDirectory);
            }
        } catch (IOException ex) {
            throw new DataStoreException("IO exception creating offering directory  "+offeringDirectory.toString(), ex);
        }
        String fileName = offering.getId().replace(':', 'µ');
        final Path offeringFile = offeringDirectory.resolve(fileName + FILE_EXTENSION);
        writeObject(offeringFile, offering, "foi");
        return offering.getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateOffering(final String offeringID, final String offProc, final List<String> offPheno, final String offSF) throws DataStoreException {
        // TODO
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateOfferings() {
        //do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void recordProcedureLocation(final String physicalID, final AbstractGeometry position) throws DataStoreException {
        // do nothing
    }

    @Override
    public void writeProcedure(ExtractionResult.ProcedureTree procedure) throws DataStoreException {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getInfos() {
        return "Constellation Filesystem O&M Writer 0.9";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
        indexer.destroy();
    }

}
