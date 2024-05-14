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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.*;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.sis.storage.DataStoreException;
import static org.constellation.sos.io.lucene.LuceneObervationUtils.getLuceneTimeValue;

import org.geotoolkit.index.IndexingException;
import org.geotoolkit.lucene.index.AbstractIndexer;
import org.opengis.temporal.Period;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import org.geotoolkit.observation.json.ObservationJsonUtils;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.observation.model.CompositePhenomenon;
import org.geotoolkit.observation.model.Observation;
import org.geotoolkit.observation.model.Offering;
import org.geotoolkit.observation.model.Phenomenon;
import org.geotoolkit.observation.model.Procedure;
import org.geotoolkit.observation.model.SamplingFeature;
import org.geotoolkit.temporal.object.TemporalUtilities;
import org.opengis.temporal.TemporalPrimitive;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class LuceneObservationIndexer extends AbstractIndexer<Object> {

    private static final String FILE_EXTENSION_JS = "json";

    private ObjectMapper mapper;

    private Path offeringDirectory;

    private Path observationDirectory;

    private Path foiDirectory;

    private Path phenDirectory;

    private Path observationTemplateDirectory;

    private boolean template = false;

    /**
     * Creates a new SOS indexer for a FileSystem reader.
     *
     * @param dataDirectory The directory containing the data files.
     * @param configDirectory The directory where the index will be created.
     * @param serviceID  The identifier, if there is one, of the index/service.
     * @param create
     */
    public LuceneObservationIndexer(final Path dataDirectory, final Path configDirectory, final String serviceID, final boolean create) throws IndexingException {
        super(serviceID, configDirectory, new WhitespaceAnalyzer());
        mapper = ObservationJsonUtils.getMapper();

        if (Files.isDirectory(dataDirectory)) {
            try {
                observationDirectory = dataDirectory.resolve("observations");
                Files.createDirectories(observationDirectory);

                phenDirectory = dataDirectory.resolve("phenomenons");
                Files.createDirectories(phenDirectory);

                observationTemplateDirectory = dataDirectory.resolve("observationTemplates");
                Files.createDirectories(observationTemplateDirectory);

                foiDirectory = dataDirectory.resolve("features");
                Files.createDirectories(foiDirectory);

                offeringDirectory = dataDirectory.resolve("offerings");
                Files.createDirectories(offeringDirectory);
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
            final IndexWriterConfig conf = new IndexWriterConfig(analyzer);
            try (IndexWriter writer = new IndexWriter(new SimpleFSDirectory(getFileDirectory()), conf)) {
                Map<String, ObjAndOffering> procs = new HashMap<>();
                // getting the objects list and index avery item in the IndexWriter.
                nbObservation = indexJsonDirectory(observationDirectory, nbObservation, writer, "observation", procs, Observation.class);
                template = true;
                nbTemplate = indexJsonDirectory(observationTemplateDirectory, nbTemplate, writer, "template", procs, Observation.class);
                template = false;
                nbfoi = indexJsonDirectory(foiDirectory, nbfoi, writer, "foi", procs, SamplingFeature.class);
                nbPhen = indexJsonDirectory(phenDirectory, nbPhen, writer, "phenomenon", procs, Phenomenon.class);
                indexJsonDirectory(offeringDirectory, 0, writer, "offering", procs, Offering.class);
                for (ObjAndOffering po : procs.values()) {
                    indexDocument(writer, po);
                }
            }

        } catch (CorruptIndexException ex) {
            LOGGER.log(Level.SEVERE,CORRUPTED_SINGLE_MSG + "{0}", ex.getMessage());
            throw new IndexingException(CORRUPTED_MULTI_MSG, ex);
        } catch (LockObtainFailedException ex) {
            LOGGER.log(Level.SEVERE,LOCK_SINGLE_MSG + "{0}", ex.getMessage());
            throw new IndexingException(LOCK_MULTI_MSG, ex);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE,IO_SINGLE_MSG + "{0}", ex.getMessage());
            throw new IndexingException("IOException while indexing documents.", ex);
        }
        LOGGER.info("Index creation process in " + (System.currentTimeMillis() - time) + " ms\nObservations indexed: "
                + nbObservation + ". Template indexed:" + nbTemplate + ".");
    }

    private int indexJsonDirectory(Path directory, int nbObservation, IndexWriter writer, String type, Map<String, ObjAndOffering> procs, Class entityClass)
            throws IOException, IndexingException {

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path entityFile : stream) {
                if (!Files.isDirectory(entityFile)) {
                    String ext = IOUtilities.extension(entityFile);
                    if (!ext.equals(FILE_EXTENSION_JS)) continue;

                    try (InputStream inputStream = Files.newInputStream(entityFile)) {
                        Object obj = mapper.readValue(inputStream, entityClass);

                        if (obj instanceof Observation obs) {
                            Procedure procedure = obs.getProcedure();
                            if (!procs.containsKey(procedure.getId())) {
                                procs.put(procedure.getId(), new ProcAndOffering(procedure));
                            }

                            indexDocument(writer, obs);
                            nbObservation++;
                        } else if (obj instanceof SamplingFeature feat) {
                            procs.put(feat.getId(), new FoiAndOffering(feat));

                            //indexDocument(writer, (SamplingFeature) obj);
                            //nbObservation++;

                        } else if (obj instanceof Offering off) {

                            indexDocument(writer, off);
                            String procedure = off.getProcedure();
                            if (procs.containsKey(procedure)) {
                                procs.get(procedure).offering.add(off.getId());
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
                            LOGGER.info("The " + type + " file " + entityFile.getFileName() + " does not contains an observation:" + obj);
                        }
                    } catch (IOException ex) {
                        LOGGER.log(Level.WARNING, "Unable read json file:" + entityFile.getFileName(), ex);
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

        if (obj instanceof Observation observation) {

            doc.add(new Field("id", observation.getName().getCode(), ft));
            doc.add(new Field("type", "observation" , ft));
            doc.add(new Field("procedure", observation.getProcedure().getId(), ft));

            Phenomenon phen = observation.getObservedProperty();
            if (phen instanceof CompositePhenomenon composite) {
                for (Phenomenon component : composite.getComponent()) {
                    doc.add(new Field("observed_property", component.getId(), ft));
                }
                // add the composite id
                doc.add(new Field("observed_property",   phen.getId(), ft));

            } else if (phen != null) {
                doc.add(new Field("observed_property",   phen.getId(), ft));
            }
            SamplingFeature foi = observation.getFeatureOfInterest();
            if (foi != null) {
                doc.add(new Field("feature_of_interest", foi.getId(), ft));
            }
            Optional<Temporal> instant;
            try {
                final TemporalPrimitive time = observation.getSamplingTime();
                if (time instanceof Period period) {
                    doc.add(new Field("sampling_time_begin", getLuceneTimeValue(period.getBeginning()), ft));
                    doc.add(new Field("sampling_time_end",   getLuceneTimeValue(period.getEnding()), ft));
                    doc.add(new Field("sampling_time_begin_sort", new BytesRef(getLuceneTimeValue(period.getBeginning()).getBytes()), sft));
                    doc.add(new Field("sampling_time_end_sort",   new BytesRef(getLuceneTimeValue(period.getEnding()).getBytes()), sft));

                } else if ((instant = TemporalUtilities.toTemporal(time)).isPresent()) {
                    doc.add(new Field("sampling_time_begin",   getLuceneTimeValue(instant.get()), ft));
                    doc.add(new Field("sampling_time_end",    "NULL", ft));
                    doc.add(new Field("sampling_time_begin_sort", new BytesRef(getLuceneTimeValue(instant.get()).getBytes()), sft));
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

        } else if (obj instanceof SamplingFeature feat) {
             doc.add(new Field("id", feat.getId(), ft));
             doc.add(new Field("type", "foi" , ft));

            // add a default meta field to make searching all documents easy
            doc.add(new Field("metafile", "doc", ft));

        } else if (obj instanceof FoiAndOffering fooff) {
            SamplingFeature feat = fooff.foi;
            doc.add(new Field("id", feat.getId(), ft));
            doc.add(new Field("type", "foi" , ft));
            for (String off : fooff.offering) {
               doc.add(new Field("offering", off , ft));
            }
            // add a default meta field to make searching all documents easy
            doc.add(new Field("metafile", "doc", ft));

        } else if (obj instanceof Offering oof) {

            doc.add(new Field("id", oof.getId(), ft));
            doc.add(new Field("type", "offering" , ft));

            // add a default meta field to make searching all documents easy
            doc.add(new Field("metafile", "doc", ft));

        } else if (obj instanceof ProcAndOffering pao) {

            doc.add(new Field("id", pao.proc.getId(), ft));
            doc.add(new Field("type", "procedure" , ft));
            for (String off : pao.offering) {
               doc.add(new Field("offering", off , ft));
            }

            // add a default meta field to make searching all documents easy
            doc.add(new Field("metafile", "doc", ft));

        } else if (obj instanceof Phenomenon phen) {

            doc.add(new Field("id", phen.getId(), ft));
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
        if (obj instanceof Observation obs) {
            return obs.getName().getCode();
        } else if (obj instanceof SamplingFeature sp) {
            return sp.getName();
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
        public Procedure proc;

        public ProcAndOffering(Procedure proc) {
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
