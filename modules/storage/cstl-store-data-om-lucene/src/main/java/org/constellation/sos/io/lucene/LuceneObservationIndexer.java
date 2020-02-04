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


package org.constellation.sos.io.lucene;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.*;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.sis.storage.DataStoreException;
import org.constellation.dto.service.config.generic.Automatic;
import static org.constellation.sos.io.lucene.LuceneObervationUtils.getLuceneTimeValue;

import org.geotoolkit.gml.xml.AbstractGML;
import org.geotoolkit.index.IndexingException;
import org.geotoolkit.lucene.index.AbstractIndexer;
import org.geotoolkit.observation.xml.Process;
import org.geotoolkit.observation.xml.v100.MeasurementType;
import org.geotoolkit.sos.xml.SOSMarshallerPool;
import org.geotoolkit.swe.xml.Phenomenon;
import org.geotoolkit.swe.xml.CompositePhenomenon;
import org.opengis.observation.Observation;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.opengis.temporal.TemporalObject;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.geotoolkit.sampling.xml.SamplingFeature;
import org.geotoolkit.sos.xml.ObservationOffering;
import org.geotoolkit.swe.xml.PhenomenonProperty;
import org.geotoolkit.observation.xml.Process;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class LuceneObservationIndexer extends AbstractIndexer<Object> {

    private Path offeringDirectoryv200;
    private Path offeringDirectoryv100;

    private Path observationDirectory;

    private Path foiDirectory;

    private Path phenDirectory;

    private Path observationTemplateDirectory;

    private boolean template = false;

    /**
     * Creates a new SOS indexer for a FileSystem reader.
     *
     * @param configuration A configuration object containing the database informations.Must not be null.
     * @param serviceID  The identifier, if there is one, of the index/service.
     * @param create
     */
    public LuceneObservationIndexer(final Automatic configuration, final String serviceID, final boolean create) throws IndexingException {
        this(configuration.getDataDirectory(), configuration.getConfigurationDirectory(), serviceID, create);
    }

    public LuceneObservationIndexer(final Path dataDirectory, final Path configDirectory, final String serviceID, final boolean create) throws IndexingException {
        super(serviceID, configDirectory, new WhitespaceAnalyzer());
        if (Files.isDirectory(dataDirectory)) {
            try {
                observationDirectory = dataDirectory.resolve("observations");
                if (!Files.exists(observationDirectory)) {
                    Files.createDirectories(observationDirectory);
                }

                phenDirectory = dataDirectory.resolve("phenomenons");
                if (!Files.exists(phenDirectory)) {
                    Files.createDirectories(phenDirectory);
                }

                observationTemplateDirectory = dataDirectory.resolve("observationTemplates");
                if (!Files.exists(observationTemplateDirectory)) {
                    Files.createDirectories(observationTemplateDirectory);
                }
                foiDirectory = dataDirectory.resolve("features");
                if (!Files.exists(foiDirectory)) {
                    Files.createDirectories(foiDirectory);
                }
                Path offeringParentDirectory = dataDirectory.resolve("offerings");
                if (!Files.exists(offeringParentDirectory)) {
                    Files.createDirectories(offeringParentDirectory);
                }
                offeringDirectoryv200 = offeringParentDirectory.resolve("2.0.0");
                if (!Files.exists(offeringDirectoryv200)) {
                    Files.createDirectories(offeringDirectoryv200);
                }
                offeringDirectoryv100 = offeringParentDirectory.resolve("1.0.0");
                if (!Files.exists(offeringDirectoryv100)) {
                    Files.createDirectories(offeringDirectoryv100);
                }
            } catch (IOException e) {
                throw new IndexingException("Unable to create observation directories", e);
            }
        } else {
            throw new IndexingException("The data directory does not exist: ");
        }
        if (create && needCreation()) {
            createIndex();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<String> getAllIdentifiers() throws IndexingException {
        throw new UnsupportedOperationException("not used in this implementation");
    }

    @Override
    protected Iterator<String> getIdentifierIterator() throws IndexingException {
        throw new UnsupportedOperationException("not used in this implementation");
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected Observation getEntry(final String identifier) throws IndexingException {
        throw new UnsupportedOperationException("not used in this implementation");
    }

    @Override
    protected Iterator<Object> getEntryIterator() throws IndexingException {
        throw new UnsupportedOperationException("not used in this implementation");
    }

    @Override
    protected boolean useEntryIterator() {
        return false;
    }

     /**
     * {@inheritDoc}
     */
    @Override
    public void createIndex() throws IndexingException {
        LOGGER.info("Creating lucene index for Filesystem observations please wait...");

        final long time = System.currentTimeMillis();
        int nbObservation = 0;
        int nbTemplate    = 0;
        int nbfoi         = 0;
        int nbPhen        = 0;
        try {
            final Unmarshaller unmarshaller = SOSMarshallerPool.getInstance().acquireUnmarshaller();
            final IndexWriterConfig conf = new IndexWriterConfig(analyzer);
            final IndexWriter writer = new IndexWriter(new SimpleFSDirectory(getFileDirectory()), conf);

            Map<String, ObjAndOffering> procs = new HashMap<>();

            // getting the objects list and index avery item in the IndexWriter.
            nbObservation = indexDirectory(observationDirectory, nbObservation, unmarshaller, writer, "observation", procs);
            template = true;
            nbTemplate = indexDirectory(observationTemplateDirectory, nbTemplate, unmarshaller, writer, "template", procs);
            template = false;

            nbfoi = indexDirectory(foiDirectory, nbfoi, unmarshaller, writer, "foi", procs);

            nbPhen = indexDirectory(phenDirectory, nbPhen, unmarshaller, writer, "phenomenon", procs);

            indexDirectory(offeringDirectoryv200, 0, unmarshaller, writer, "offering", procs);
            indexDirectory(offeringDirectoryv100, 0, unmarshaller, writer, "offering", procs);

            for (ObjAndOffering po : procs.values()) {
                indexDocument(writer, po);
            }

            SOSMarshallerPool.getInstance().recycle(unmarshaller);

            // writer.optimize(); no longer justified
            writer.close();

        } catch (CorruptIndexException ex) {
            LOGGER.log(Level.SEVERE,CORRUPTED_SINGLE_MSG + "{0}", ex.getMessage());
            throw new IndexingException(CORRUPTED_MULTI_MSG, ex);
        } catch (LockObtainFailedException ex) {
            LOGGER.log(Level.SEVERE,LOCK_SINGLE_MSG + "{0}", ex.getMessage());
            throw new IndexingException(LOCK_MULTI_MSG, ex);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE,IO_SINGLE_MSG + "{0}", ex.getMessage());
            throw new IndexingException("IOException while indexing documents.", ex);
        } catch (JAXBException ex) {
            String msg = ex.getMessage();
            if (msg == null && ex.getCause() != null) {
                msg = ex.getCause().getMessage();
            }
            LOGGER.log(Level.SEVERE, "JAXB Exception while indexing: {0}", msg);
            throw new IndexingException("JAXBException while indexing documents.", ex);
        }
        LOGGER.info("Index creation process in " + (System.currentTimeMillis() - time) + " ms\nObservations indexed: "
                + nbObservation + ". Template indexed:" + nbTemplate + ".");
    }

    private int indexDirectory(Path directory, int nbObservation, Unmarshaller unmarshaller, IndexWriter writer, String type, Map<String, ObjAndOffering> procs)
            throws JAXBException, IOException, IndexingException {

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path observationFile : stream) {
                try (InputStream inputStream = Files.newInputStream(observationFile)) {
                    Object obj = unmarshaller.unmarshal(inputStream);
                    if (obj instanceof JAXBElement) {
                        obj = ((JAXBElement) obj).getValue();
                    }
                    if (obj instanceof Observation) {
                        Observation obs = (Observation) obj;
                        Process procedure = (Process) obs.getProcedure();
                        if (!procs.containsKey(procedure.getHref())) {
                            procs.put(procedure.getHref(), new ProcAndOffering(procedure));
                        }

                        indexDocument(writer, obs);
                        nbObservation++;
                    } else if (obj instanceof SamplingFeature) {
                        SamplingFeature feat = (SamplingFeature) obj;
                        procs.put(feat.getId(), new FoiAndOffering(feat));

                        //indexDocument(writer, (SamplingFeature) obj);
                        //nbObservation++;

                    } else if (obj instanceof ObservationOffering) {

                        ObservationOffering off = (ObservationOffering) obj;
                        indexDocument(writer, off);
                        for (String procedure : off.getProcedures()) {
                            if (procs.containsKey(procedure)) {
                                procs.get(procedure).offering.add(off.getId());
                            }
                        }
                        for (String foid : off.getFeatureOfInterestIds()) {
                            if (procs.containsKey(foid)) {
                                procs.get(foid).offering.add(off.getId());
                            }
                        }

                        nbObservation++;

                    } else if (obj instanceof Phenomenon) {
                        indexDocument(writer, obj);


                    } else {
                        LOGGER.info("The "+type+" file " + observationFile.getFileName().toString()
                                + " does not contains an observation:" + obj);
                    }
                }
            }
        }
        return nbObservation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Document createDocument(final Object obj, final int docid) {
        // make a new, empty document
        final Document doc = new Document();

        final FieldType ft = new FieldType();
        ft.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        ft.setStored(true);

        final FieldType sft = new FieldType();
        sft.setTokenized(false);
        sft.setStored(false);
        sft.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        sft.setDocValuesType(DocValuesType.SORTED);

        if (obj instanceof Observation) {
            Observation observation = (Observation) obj;

            doc.add(new Field("id", observation.getName().getCode(), ft));
            if (observation instanceof MeasurementType) {
                doc.add(new Field("type", "measurement" , ft));
            } else {
                doc.add(new Field("type", "observation" , ft));
            }
            doc.add(new Field("procedure", ((Process)observation.getProcedure()).getHref(), ft));

            Phenomenon phen = (Phenomenon)observation.getObservedProperty();
            if (phen instanceof CompositePhenomenon) {
                CompositePhenomenon composite = (CompositePhenomenon) phen;
                for (PhenomenonProperty component : composite.getRealComponent()) {
                    if (component.getPhenomenon() != null && component.getPhenomenon().getName() != null) {
                        doc.add(new Field("observed_property",   component.getPhenomenon().getName().getCode(), ft));
                    } else if (component.getHref() != null) {
                        doc.add(new Field("observed_property",   component.getHref(), ft));
                    } else {
                        LOGGER.warning("Composite phenomenon component is empty");
                    }
                }

                // add the composite name
                doc.add(new Field("observed_property",   phen.getName().getCode(), ft));

            } else if (phen != null) {
                doc.add(new Field("observed_property",   phen.getName().getCode(), ft));
            }

            doc.add(new Field("feature_of_interest", ((AbstractGML)observation.getFeatureOfInterest()).getId(), ft));

            try {
                final TemporalObject time = observation.getSamplingTime();
                if (time instanceof Period) {
                    final Period period = (Period) time;
                    doc.add(new Field("sampling_time_begin", getLuceneTimeValue(period.getBeginning().getDate()), ft));
                    doc.add(new Field("sampling_time_end",   getLuceneTimeValue(period.getEnding().getDate()), ft));
                    doc.add(new Field("sampling_time_begin_sort", new BytesRef(getLuceneTimeValue(period.getBeginning().getDate()).getBytes()), sft));
                    doc.add(new Field("sampling_time_end_sort",   new BytesRef(getLuceneTimeValue(period.getEnding().getDate()).getBytes()), sft));

                } else if (time instanceof Instant) {
                    final Instant instant = (Instant) time;
                    doc.add(new Field("sampling_time_begin",   getLuceneTimeValue(instant.getDate()), ft));
                    doc.add(new Field("sampling_time_end",    "NULL", ft));
                    doc.add(new Field("sampling_time_begin_sort", new BytesRef(getLuceneTimeValue(instant.getDate()).getBytes()), sft));
                    doc.add(new Field("sampling_time_end_sort", new BytesRef("NULL".getBytes()), sft));

                } else if (time != null) {
                    LOGGER.log(Level.WARNING, "unrecognized sampling time type:{0}", time);
                }
            } catch(DataStoreException ex) {
                LOGGER.severe("error while indexing sampling time.");
            }
            if (template) {
                doc.add(new Field("template", "TRUE", ft));
            } else {
                doc.add(new Field("template", "FALSE", ft));
            }
            // add a default meta field to make searching all documents easy
            doc.add(new Field("metafile", "doc", ft));

        } else if (obj instanceof SamplingFeature) {
            SamplingFeature feat = (SamplingFeature) obj;
             doc.add(new Field("id", feat.getId(), ft));
             doc.add(new Field("type", "foi" , ft));

            // add a default meta field to make searching all documents easy
            doc.add(new Field("metafile", "doc", ft));

        } else if (obj instanceof FoiAndOffering) {
            FoiAndOffering fooff = ((FoiAndOffering) obj);
            SamplingFeature feat = fooff.foi;
            doc.add(new Field("id", feat.getId(), ft));
            doc.add(new Field("type", "foi" , ft));
            for (String off : fooff.offering) {
               doc.add(new Field("offering", off , ft));
            }
            // add a default meta field to make searching all documents easy
            doc.add(new Field("metafile", "doc", ft));

        } else if (obj instanceof ObservationOffering) {

            ObservationOffering feat = (ObservationOffering) obj;
            doc.add(new Field("id", feat.getId(), ft));
            doc.add(new Field("type", "offering" , ft));

            // add a default meta field to make searching all documents easy
            doc.add(new Field("metafile", "doc", ft));

        } else if (obj instanceof ProcAndOffering) {

            ProcAndOffering feat = (ProcAndOffering) obj;
            doc.add(new Field("id", feat.proc.getHref(), ft));
            doc.add(new Field("type", "procedure" , ft));
            for (String off : feat.offering) {
               doc.add(new Field("offering", off , ft));
            }

            // add a default meta field to make searching all documents easy
            doc.add(new Field("metafile", "doc", ft));

        } else if (obj instanceof Phenomenon) {

            Phenomenon feat = (Phenomenon) obj;
            doc.add(new Field("id", feat.getId(), ft));
            doc.add(new Field("type", "phenomenon" , ft));
            
            // add a default meta field to make searching all documents easy
            doc.add(new Field("metafile", "doc", ft));
        } else {
            throw new IllegalArgumentException("Unepxected object type to index");
        }

        return doc;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getIdentifier(Object obj) {
        if (obj instanceof Observation) {
            return ((Observation)obj).getName().getCode();
        } else if (obj instanceof SamplingFeature) {
            return ((SamplingFeature)obj).getName().getCode();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
        super.destroy();
    }

    private class ObjAndOffering {
        public List<String> offering = new ArrayList<>();
    }

    private class ProcAndOffering extends ObjAndOffering {
        public Process proc;

        public ProcAndOffering(Process proc) {
            this.proc = proc;
        }
    }

     private class FoiAndOffering extends ObjAndOffering {
        public SamplingFeature foi;

        public FoiAndOffering(SamplingFeature foi) {
            this.foi = foi;
        }
    }
}
