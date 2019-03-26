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
package org.constellation.util.nio;

import org.geotoolkit.nio.IOUtilities;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * NIO FileVisitor that map matching extensions Paths from a root visited Path.
 * Extensions are in lower cases
 *
 * @author Quentin Boileau (Geomatys)
 */
public class PathExtensionVisitor extends SimpleFileVisitor<Path> {

    final Map<String,SortedSet<Path>> extensions = new HashMap<>();

    /**
     * @return found extensions in lowercase with matched files path
     */
    public Map<String, SortedSet<Path>> getExtensions() {
        return extensions;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        String ext = IOUtilities.extension(file);
        if (ext != null) {
            ext = ext.toLowerCase();
            if (!extensions.containsKey(ext)) {
                extensions.put(ext, new TreeSet<Path>());
            }
            extensions.get(ext).add(file);
        }
        return FileVisitResult.CONTINUE;
    }
}
