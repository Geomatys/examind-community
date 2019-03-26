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
package org.constellation.metadata.index;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A path filter to retrieve all the index directory in a specified directory.
 * Path implementation of geotoolkit {@link org.geotoolkit.lucene.index.IndexDirectoryFilter}
 *
 * @author Quentin Boileau (Geomatys)
 */
public class IndexDirectoryPathVisitor implements DirectoryStream.Filter<Path> {


    /**
     * The service ID.
     */
    private final String prefix;

    public IndexDirectoryPathVisitor(final String id) {
        if (id != null) {
            prefix = id;
        } else {
            prefix = "";
        }
    }

    /**
     * Return true if the specified file is a directory and if its name start
     * with the serviceID + 'index-'.
     *
     * @param entry The current path explored.
     * @return True if the specified file in the current directory match the
     * conditions.
     */
    @Override
    public boolean accept(Path entry) throws IOException {
        String fileName = entry.getFileName().toString();
        if ("all".equals(prefix)) {
            return (fileName.contains("index-") && Files.isDirectory(entry));
        } else {
            return (fileName.startsWith(prefix + "index-") && Files.isDirectory(entry));
        }
    }
}