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

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.geotoolkit.index.IndexingException;
import org.geotoolkit.index.tree.manager.NamedEnvelope;

/**
 * TODO move to cstl-lib-api when there will be no more dependency to geotk-index-lucene
 *
 * @author Guilhem Legal (Geomatys)
 * @param <E>
 */
public interface Indexer<E extends Object> {

    boolean needCreation();

    void createIndex() throws IndexingException;

    boolean destroyIndex() throws IndexingException;

    void indexDocument(E document);

    void indexDocuments(List<E> documents);

    void removeDocument(String id);

    String getTreeRepresentation();

    Map<Integer, NamedEnvelope> getMapperContent() throws IOException;

    @Deprecated
    void setFileDirectory(Path aFileDirectory);

    void destroy();
}
