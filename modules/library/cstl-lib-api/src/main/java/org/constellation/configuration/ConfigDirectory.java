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
package org.constellation.configuration;

import java.io.File;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.constellation.exception.ConfigurationRuntimeException;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;

/**
 * Temporary copy of static methods from the WebService class (in module
 * web-base), in order to retrieve the configuration directory of Constellation.
 *
 * TODO: this implementation should probably been handled by the server
 * registry, so move it there.
 *
 * @author Cédric Briançon (Geomatys)
 * @author Guilhem Legal (Geomatys)
 * @version $Id$
 */
public final class ConfigDirectory {

    private static class Config {

        public Config(Builder builder) {
            this.home = builder.home;
            this.data = builder.data;
            this.dataIntegrated = builder.dataIntegrated;
            this.dataUserUploads = builder.dataUserUploads;
            this.dataServices = builder.dataServices;
            this.testing = builder.testing;
            this.process = builder.process;
        }

        private static class Builder {
            private Path home;
            private Path data;
            private Path dataIntegrated;
            private Path dataServices;
            private Path process;

            private URI homeLocation;
            private URI dataLocation;
            private URI processLocation;
            private Path dataUserUploads;
            private boolean testing;

            public Builder() {
                String exaHome = Application.getProperty(AppProperty.CSTL_HOME, System.getProperty("user.home") + File.separator + ".constellation");
                this.homeLocation = Paths.get(exaHome).toUri();
                String exaData = Application.getProperty(AppProperty.CSTL_DATA, exaHome + File.separator +  "data");
                this.dataLocation = Paths.get(exaData).toUri();
                String exaProcess = Application.getProperty(AppProperty.CSTL_PROCESS, null);
                this.processLocation = exaProcess != null ? Paths.get(exaProcess).toUri() : null;
            }

            Config build() {
                this.home = initFolder(homeLocation);
                this.data = initFolder(dataLocation);
                if (processLocation != null) {
                    this.process = initFolder(processLocation);
                } else {
                    this.process = initDataSubFolder("process");
                }
                this.dataIntegrated = initDataSubFolder("integrated");
                this.dataUserUploads = initDataSubFolder("user", "uploads");
                this.dataServices = initDataSubFolder("services");
                return new Config(this);
            }

            private Path initFolder(URI absLocation) {
                try {
                    Path location;
                    if (absLocation.getScheme() == null) {
                        //scheme null, consider as Path on default FileSystem
                        return Paths.get(absLocation.toString());
                    } else {
                        location = Paths.get(absLocation);
                    }
                    return ConfigDirectory.initFolder(location);
                } catch (IllegalArgumentException | FileSystemNotFoundException ex) {
                    throw new RuntimeException(ex);
                }
            }

            private Path initDataSubFolder(String sub, String... subs) {
                Path subPath = data.resolve(sub);
                if (subs != null) {
                    for (String s : subs) {
                        subPath = subPath.resolve(s);
                    }
                }
                return ConfigDirectory.initFolder(subPath);
            }

            public Builder forTest(String first, String... subPath) {
                ensureNonNull("At least first path part must be given", first);
                Path home = Paths.get(first, subPath);
                Path data = home.resolve("data");
                this.homeLocation = home.toUri();
                this.dataLocation = data.toUri();
                this.testing = true;
                return this;
            }
        }

        final Path home;
        final Path data;
        final Path dataIntegrated;
        final Path dataUserUploads;
        final boolean testing;
        final Path dataServices;
        final Path process;
    }

    /**
     * The default debugging logger.
     */
    private static final Logger LOGGER = Logger.getLogger("org.constellation.configuration");

    private static Config config;

    private ConfigDirectory() {
    }

    static Path initFolder(Path path) {
        if (Files.notExists(path)) {
            try {
                Files.createDirectories(path);
                LOGGER.log(Level.FINE, "{0} created.", path.toUri().toString());
            } catch (IOException e) {
                throw new ConfigurationRuntimeException("Could not create: " + path.toString(), e);
            }
        }
        return path;
    }

    /**
     * Give a data directory {@link java.nio.file.Path} defined on
     * constellation.properties or by default on .constellation-data from user
     * home directory
     *
     * @return data directory as {@link java.nio.file.Path}
     */
    public static Path getDataDirectory() {
        return config.data;
    }

    public static Path getDataPath() {
        return config.data;
    }

    /**
     * Give a integrated data directory {@link java.nio.file.Path} defined on
     * constellation.properties or by default on .constellation-data/integrated/
     * from user home directory
     *
     * @return providers directory as {@link java.nio.file.Path}
     */
    public static Path getDataIntegratedDirectory() {
        return config.dataIntegrated;
    }
    
    public static Path getProcessDirectory() {
        return config.process;
    }

    /**
     * Give a integrated data directory {@link java.nio.file.Path} defined on
     * constellation.properties or by default on .constellation-data/integrated/
     * from user home directory for given provider.
     *
     * @param providerId The provider identifier. will be used as the directory name.
     * @param create if {@code true} the directory will be created if it don't already exist.
     * @return providers directory as {@link java.nio.file.Path}
     * @throws IOException if provider directory creation failed
     */
    public static Path getDataIntegratedDirectory(String providerId, boolean create) throws IOException {
        if (config == null) return null;
        final Path rootFolder = getDataIntegratedDirectory().normalize();
        final Path f = rootFolder.resolve(providerId).normalize();
        if (!f.startsWith(rootFolder)) throw new IllegalArgumentException("Invalid provider ID: "+providerId);
        if (create && !Files.isDirectory(f)) {
            Files.createDirectories(f);
        }
        return f;
    }

    private static void deleteDir(Path folder) {
        if (Files.exists(folder)) {
            try {
                deleteRecursively(folder);
                LOGGER.log(Level.FINE, "{0} deleted.", folder.toString());
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
            }
        }
    }

    private static Path resolveUserUploads(String userName) {
        return config.dataUserUploads.resolve(userName);
    }

    private static Path resolveInstanceDirectory(String type, String id) {
        Path typeService = resolveInstanceServiceDirectoryByType(type);
        return initFolder(typeService.resolve(id));
    }

    private static Path resolveInstanceServiceDirectoryByType(String type) {
        Path typeService = config.dataServices.resolve(type);
        ConfigDirectory.initFolder(typeService);
        return typeService;
    }


    public static Path getUploadDirectory() {
        return config.dataUserUploads;
    }
    /**
     * Give a upload directory {@link java.nio.file.Path} defined on
     * constellation.properties or by default on
     * .constellation-data/upload/userName from user home directory
     *
     * @param userName
     *
     * @return providers directory as {@link java.nio.file.Path}
     */
    public static Path getUploadDirectory(String userName) throws IOException {
        Path uploadDirectory = resolveUserUploads(userName);
        if (!Files.exists(uploadDirectory)) {
            Files.createDirectories(uploadDirectory);
        }
        return uploadDirectory;
    }

    public static Path getConfigDirectory() {
        return config.home;
    }

    public static Path setupTestEnvironement(String filename) {
        ensureNonNull("File name", filename);
        config = new Config.Builder().forTest("target", filename).build();
        return config.home;
    }

    public static void shutdownTestEnvironement() {
        if (config.testing) {
            deleteDir(config.home);
        }
    }

    public static void init() {
        if (config == null) {
            config = new Config.Builder().build();
        }
    }

    public static Path getInstanceDirectory(String type, String id) {
        return resolveInstanceDirectory(type.toLowerCase(), id);
    }

    public static List<Path> getInstanceDirectories(String typeService) throws IOException {
        Path instancesDirectory = resolveInstanceServiceDirectoryByType(typeService);
        return listChildren(instancesDirectory);
    }

    public static Properties getMetadataTemplateProperties() {
        final Path cstlDir = ConfigDirectory.getConfigDirectory();
        final Path propFile = cstlDir.resolve("metadataTemplate.properties");
        final Properties prop = new Properties();
        if (Files.exists(propFile)) {
            try (InputStream in = Files.newInputStream(propFile)) {
                prop.load(in);
                return prop;
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "IOException while loading metadata template properties file", ex);
            }
        }
        return prop;
    }

    /**
     * This method delete recursively a file or a folder.
     *
     * @param root The File or directory to delete.
     */
    private static void deleteRecursively(final Path root) throws IOException {
        if (Files.exists(root)) {
            if (!Files.isDirectory(root)) {
                Files.deleteIfExists(root);
            } else {
                Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (!Files.isDirectory(file)) {
                            Files.deleteIfExists(file);
                        } else {
                            deleteRecursively(file);
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.deleteIfExists(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        }
    }

    /**
     * Traverse a directory an return children files with depth 0.
     * Result of this method can result of an OutOfMemory if scanned
     * folder contains very large number of files.
     *
     * @param directory input Path, should be a directory
     * @return children Path
     * @throws IllegalArgumentException if input Path is not a directory
     * @throws IOException if an error occurs during directory scanning
     */
    private static List<Path> listChildren(Path directory) throws IllegalArgumentException, IOException {
        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException("Input Path is not a directory or doesn't exist");
        }
        final List<Path> children = new LinkedList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*")) {
            for (Path child : stream) {
                children.add(child);
            }
        }
        //asc sort
        Collections.sort(children);
        return children;
    }

}
