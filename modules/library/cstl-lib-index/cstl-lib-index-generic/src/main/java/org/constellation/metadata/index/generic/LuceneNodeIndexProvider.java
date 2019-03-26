package org.constellation.metadata.index.generic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.exception.ConfigurationException;
import org.constellation.filter.FilterParser;
import org.constellation.filter.LuceneFilterParser;
import org.constellation.filter.SQLFilterParser;
import org.constellation.dto.service.config.generic.Automatic;
import org.constellation.metadata.index.IndexProvider;
import org.constellation.metadata.index.IndexSearcher;
import org.constellation.metadata.index.Indexer;
import org.constellation.store.metadata.CSWMetadataReader;
import org.geotoolkit.index.IndexingException;
import org.geotoolkit.metadata.MetadataStore;
import org.springframework.stereotype.Component;

/**
 * @author Quentin Boileau (Geomatys)
 */
@Component(value = "lucene-node")
public class LuceneNodeIndexProvider implements IndexProvider {

    public static final String INDEX_TYPE = "lucene-node";

    @Override
    public String indexType() {
        return INDEX_TYPE;
    }

    @Override
    public Indexer getIndexer(Automatic configuration, MetadataStore mdStore, String serviceID) throws IndexingException, ConfigurationException {
        return new NodeIndexer(mdStore, configuration.getConfigurationDirectory(), "", ((CSWMetadataReader)mdStore.getReader()).getAdditionalQueryablePathMap(), false);
    }

    @Override
    public IndexSearcher getIndexSearcher(Automatic configuration, String serviceID) throws IndexingException, ConfigurationException {
        return new LuceneIndexSearcher(configuration.getConfigurationDirectory(), "", null, true);
    }

    @Override
    public FilterParser getFilterParser(Automatic configuration) throws ConfigurationException {
        return new LuceneFilterParser();
    }

    @Override
    public FilterParser getSQLFilterParser(Automatic configuration) throws ConfigurationException {
        return new SQLFilterParser();
    }

    @Override
    public boolean refreshIndex(Automatic configuration, String serviceID, Indexer indexer, boolean asynchrone) throws ConfigurationException {
        final Path instanceDir = ConfigDirectory.getInstanceDirectory("csw", serviceID);
        if (Files.exists(instanceDir)) {
            if (asynchrone) {
                final Path nexIndexDir = instanceDir.resolve("index-" + System.currentTimeMillis());
                try {
                    if (indexer != null) {
                        try {
                            Files.createDirectories(nexIndexDir);
                        } catch (IOException e) {
                            throw new ConfigurationException("Unable to create a directory nextIndex for  the id:" + serviceID);
                        }
                        indexer.setFileDirectory(nexIndexDir);
                        indexer.createIndex();
                    } else {
                        throw new ConfigurationException("Unable to create an indexer for the id:" + serviceID);
                    }
                } catch (IllegalArgumentException | IndexingException ex) {
                    throw new ConfigurationException("An exception occurs while creating the index!\ncause:" + ex.getMessage());
                }
                return true;
            } else {
                try {
                    return indexer.destroyIndex();
                } catch (IndexingException ex) {
                    throw new ConfigurationException(ex);
                }
            }
        }
        return false;
    }
}
