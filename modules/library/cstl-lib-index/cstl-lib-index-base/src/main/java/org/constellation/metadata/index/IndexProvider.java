/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2021 Geomatys.
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

package org.constellation.metadata.index;

import org.constellation.exception.ConfigurationException;
import org.constellation.filter.FilterParser;
import org.constellation.dto.service.config.generic.Automatic;
import org.constellation.exception.ConstellationException;
import org.geotoolkit.metadata.MetadataStore;

/**
 * @author Quentin Boileau (Geomatys)
 * @author Guilhem Legal (Geomatys)
 */
public interface IndexProvider  {

    String indexType();

    Indexer getIndexer(final Automatic configuration, final MetadataStore mdStore, final String serviceID) throws ConstellationException;

    IndexSearcher getIndexSearcher(final Automatic configuration, final String serviceID) throws ConstellationException;

    FilterParser getFilterParser(final Automatic configuration) throws ConfigurationException;

    boolean refreshIndex(final Automatic configuration, String serviceID, Indexer indexer, boolean asynchrone) throws ConfigurationException;
}
