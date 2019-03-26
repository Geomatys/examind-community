package org.constellation.metadata.index;

import org.constellation.exception.ConfigurationException;
import org.constellation.filter.FilterParser;
import org.constellation.dto.service.config.generic.Automatic;
import org.geotoolkit.index.IndexingException;
import org.geotoolkit.metadata.MetadataStore;

/**
 * @author Quentin Boileau (Geomatys)
 */
public interface IndexProvider  {

    String indexType();

    Indexer getIndexer(final Automatic configuration, final MetadataStore mdStore, final String serviceID) throws IndexingException, ConfigurationException;

    IndexSearcher getIndexSearcher(final Automatic configuration, final String serviceID) throws IndexingException, ConfigurationException;

    FilterParser getFilterParser(final Automatic configuration) throws ConfigurationException;

    FilterParser getSQLFilterParser(final Automatic configuration) throws ConfigurationException;

    boolean refreshIndex(final Automatic configuration, String serviceID, Indexer indexer, boolean asynchrone) throws ConfigurationException;
}
