/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2016 Geomatys.
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

import org.apache.lucene.analysis.Analyzer;
import org.constellation.metadata.index.IndexSearcher;
import org.geotoolkit.index.IndexingException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class LuceneIndexSearcher extends org.geotoolkit.lucene.index.LuceneIndexSearcher implements IndexSearcher {
    
    public LuceneIndexSearcher(final Path configDir, final String serviceID) throws IndexingException {
        super(configDir, serviceID);
    }

    public LuceneIndexSearcher(final Path configDir, final String serviceID, final Analyzer analyzer) throws IndexingException {
        super(configDir, serviceID, analyzer);
    }
    
    public LuceneIndexSearcher(final Path configDir, final String serviceID, final Analyzer analyzer, final boolean envelopeOnly) throws IndexingException {
        super(configDir, serviceID, analyzer, envelopeOnly);
    }
}
