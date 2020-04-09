package org.constellation.metadata.index.elasticsearch;

import java.util.Map;
import java.util.logging.Logger;
import org.apache.sis.util.logging.Logging;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.constellation.exception.ConfigurationException;
import org.constellation.filter.ElasticSearchFilterParser;
import org.constellation.filter.FilterParser;
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
@Component(value = "es-node")
public class ElasticSearchIndexProvider implements IndexProvider {

    private static final Logger LOGGER = Logging.getLogger("ElasticSearchIndexProvider");

    public static final String INDEX_TYPE = "es-node";

    public static final String ES_URL_PARAM = "es-url";
    public static final String ES_PORT_PARAM = "es-port";

    @Override
    public String indexType() {
        return INDEX_TYPE;
    }

    @Override
    public Indexer getIndexer(Automatic configuration, MetadataStore mdStore, String serviceID) throws IndexingException, ConfigurationException {
        String host = configuration.getParameter(ES_URL_PARAM);
        String clusterName;
        if (host == null || host.isEmpty()) {
            host = Application.getProperty(AppProperty.ES_MASTER_NAME);
            clusterName = Application.getProperty(AppProperty.ES_CLUSTER_NAME);
        } else {
            Map<String, Object> infos = ElasticSearchClient.getServerInfo("http://" + host + ":9200");
            clusterName = (String) infos.get("cluster_name");
        }
        return new ElasticSearchNodeIndexer(mdStore, host, clusterName, serviceID, ((CSWMetadataReader)mdStore.getReader()).getAdditionalQueryablePathMap(), true);
    }

    @Override
    public IndexSearcher getIndexSearcher(Automatic configuration, String serviceID) throws IndexingException, ConfigurationException {
        String host = configuration.getParameter(ES_URL_PARAM);
        String clusterName;
        if (host == null || host.isEmpty()) {
            host = Application.getProperty(AppProperty.ES_MASTER_NAME);
            clusterName = Application.getProperty(AppProperty.ES_CLUSTER_NAME);
        } else {
            Map<String, Object> infos = ElasticSearchClient.getServerInfo("http://" + host + ":9200");
            clusterName = (String) infos.get("cluster_name");
        }
        return new ElasticSearchIndexSearcher(host, clusterName, serviceID);
    }

    @Override
    public FilterParser getFilterParser(Automatic configuration) throws ConfigurationException {
        return new ElasticSearchFilterParser();
    }

    @Override
    public FilterParser getSQLFilterParser(Automatic configuration) throws ConfigurationException {
        return new SQLFilterParser();
    }

    @Override
    public boolean refreshIndex(Automatic configuration, String serviceID, Indexer indexer, boolean asynchrone) throws ConfigurationException {
        try {
            // TODO asynchrone refresh can't be performed for now
            if (asynchrone) {
                LOGGER.warning("asynchrone refresh can't be performed for now. performing in synchroneous way");
            }
            if (indexer.destroyIndex()) {
                indexer.createIndex();
                return true;
            } else {
                throw new ConfigurationException("Unable to destroy the ES index");
            }
        } catch (IndexingException ex) {
            throw new ConfigurationException(ex);
        }
    }
}
