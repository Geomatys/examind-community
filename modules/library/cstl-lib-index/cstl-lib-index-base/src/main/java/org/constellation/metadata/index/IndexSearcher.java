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
package org.constellation.metadata.index;

import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.geotoolkit.index.IndexingException;
import org.geotoolkit.index.SearchingException;
import org.geotoolkit.index.SpatialQuery;

/**
 * TODO move to cstl-lib-api when there will be no more dependency to geotk-index-lucene
 * 
 * @author Guilhem Legal (Geomatys)
 */
public interface IndexSearcher {
    
    Set<String> doSearch(final SpatialQuery spatialQuery) throws SearchingException;
    
    Map<String, Character> getNumericFields();
    
    String identifierQuery(final String id) throws SearchingException;
    
    void refresh() throws IndexingException;
    
    void setLogLevel(Level logLevel);
    
    void destroy();
}
