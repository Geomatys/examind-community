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
package org.constellation.metadata.index.generic;

import java.nio.file.Path;
import org.constellation.exception.ConfigurationException;
import org.constellation.dto.service.config.generic.Automatic;
import org.constellation.metadata.index.Indexer;
import org.constellation.store.metadata.AbstractCstlMetadataStore;
import org.geotoolkit.index.IndexingException;
import org.geotoolkit.metadata.MetadataStore;
import org.springframework.stereotype.Component;

/**
 * @author Quentin Boileau (Geomatys)
 */
@Component(value = "lucene-generic")
public class LuceneGenericIndexProvider extends AbstractLuceneIndexProvider {

    @Override
    public String indexType() {
        return "lucene-generic";
    }

    @Override
    public Indexer getIndexer(Automatic configuration, MetadataStore mdStore, String serviceID) throws ConfigurationException {
        try {
            final Path instanceDirectory = configBusiness.getInstanceDirectory("csw", serviceID);
            return new GenericIndexer(mdStore, instanceDirectory, "", ((AbstractCstlMetadataStore)mdStore).getAdditionalQueryable(), false);
        } catch (IndexingException ex) {
            throw new ConfigurationException(ex);
        }
    }
}
