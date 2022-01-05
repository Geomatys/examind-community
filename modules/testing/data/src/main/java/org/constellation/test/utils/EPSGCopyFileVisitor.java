/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2020 Geomatys.
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
package org.constellation.test.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import static org.constellation.test.utils.TestEnvironment.EPSG_VERSION;
import org.geotoolkit.nio.IOUtilities;

/**
 * Java NIO FileVisitor that copy visited files and directory recursively into target {@link Path}.
 *
 * Usage example :
 * <code>
 *     Path sourcePath = Paths.get("/some/path")
 *     Path targetPath = Paths.get("/output/path")
 *     Files.walkFileTree(sourcePath, new CopyFileVisitor(targetPath));
 * </code>
 *
 *  Copied from org.geotoolkit.nio.CopyFileVisitor.
 * If an XML file is copied, the constant "EPSG_VERSION", will be replaced with the current EPSG database version.
 * 
 * @author GUilhem Legal (Geomatys)
 */
public class EPSGCopyFileVisitor extends SimpleFileVisitor<Path> {

    private final Path targetPath;
    private final CopyOption[] copyOption;
    private Path sourcePath = null;

    public EPSGCopyFileVisitor(Path targetPath, CopyOption... copyOption) {
        this.targetPath = targetPath;
        this.copyOption = copyOption;
    }

    @Override
    public FileVisitResult preVisitDirectory(final Path dir,
                                             final BasicFileAttributes attrs) throws IOException {
        if (sourcePath == null) {
            sourcePath = dir;
            // we check if target path is a directory because if it is a symbolic link to another directory
            // createDirectories will launch an exception
            if (!Files.isDirectory(targetPath)) {
                Files.createDirectories(targetPath);
            }
        } else {
            final Path relativize = sourcePath.relativize(dir);
            final Path p = targetPath.resolve(relativize.toString());
            // we check if target path is a directory because if it is a symbolic link to another directory
            // createDirectories will launch an exception
            if (!Files.isDirectory(p)) {
                Files.createDirectories(p);
            }
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(final Path file,
                                     final BasicFileAttributes attrs) throws IOException {
        if (sourcePath == null) {
            sourcePath = file.getParent();
        }
        final Path relativize = sourcePath.relativize(file);
        final Path target     = targetPath.resolve(relativize.toString());

        if ("xml".equals(IOUtilities.extension(file))) {
            try (OutputStream out = Files.newOutputStream(target);
                 OutputStreamWriter fw = new OutputStreamWriter(out);
                 InputStream in = Files.newInputStream(file)) {
                String content = IOUtilities.toString(in).replace("EPSG_VERSION", EPSG_VERSION);
                fw.write(content);
            }
        } else {
            Files.copy(file, targetPath.resolve(relativize.toString()), copyOption);
        }
        return FileVisitResult.CONTINUE;
    }
}
