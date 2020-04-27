/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2018 Geomatys.
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
package org.constellation.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.util.logging.Logging;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.constellation.dto.importdata.FileBean;
import org.geotoolkit.nio.IOUtilities;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class FileSystemUtilities {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.util");

    private static final Map<String, FileSystemReference> USED_FILESYSTEMS = new HashMap<>();

    public static FileSystemReference getFileSystem(String type, String baseUrl, String userName, String password, Integer refId, boolean create) throws URISyntaxException, IOException {
        Map<String, Object> prop = new HashMap<>();
        String userUrl;
        URI baseURI;
        URI userURI;
        switch (type) {
            case "ftp":
                if (userName != null && !userName.isEmpty()) {
                    prop.put("username", userName);
                    userUrl = baseUrl.replace("ftp://", "ftp://" + userName + '@');
                    userUrl = userUrl.replace("ftps://", "ftps://" + userName + '@');
                } else {
                    userUrl = baseUrl;
                }
                if (password != null && !password.isEmpty()) {
                    char[] pwd = password.toCharArray();
                    prop.put("password", pwd);
                }
                prop.put("clientConnectionCount", Application.getIntegerProperty(AppProperty.CSTL_FTP_CLIENT_CONNECTION_COUNT, 5));
                prop.put("connectionMode",        Application.getProperty(AppProperty.CSTL_FTP_CLIENT_CONNECTION_MODE,         "PASSIVE"));
                prop.put("printCommand",          Application.getBooleanProperty(AppProperty.CSTL_FTP_VERBOSE_LOG,             false));
                prop.put("bufferSize",            Application.getIntegerProperty(AppProperty.CSTL_FTP_CLIENT_BUFFER_SIZE,      1024 * 1024));

                baseURI = new URI(baseUrl);
                userURI = new URI(userUrl);
                // remove path part for ftp
                if (baseURI.getPath() != null && !baseURI.getPath().isEmpty()) {
                    baseUrl = baseUrl.replace(baseURI.getPath(), "");
                    baseURI = new URI(baseUrl);
                }
                if (userURI.getPath() != null && !userURI.getPath().isEmpty()) {
                    userUrl = userUrl.replace(userURI.getPath(), "");
                    userURI = new URI(userUrl);
                }
                break;
            case "smb":
                if (userName != null && password != null) {
                    prop.put("username", userName);
                    prop.put("password", password);

                    userUrl = baseUrl.replace("smb://", "smb://" + userName + ':' + password + '@');
                } else {
                    userUrl = baseUrl;
                }
                baseURI = new URI(baseUrl);
                userURI = new URI(userUrl);
                break;
            case "s3":
                if (userName != null && !userName.isEmpty()
                        && password != null && !password.isEmpty()) {
                    userUrl = baseUrl.replace("s3://", "s3://" + userName + ':' + password + '@');
                } else if (userName != null) {
                    userUrl = baseUrl.replace("s3://", "s3://" + userName + '@');
                } else {
                    userUrl = baseUrl;
                }
                baseURI = new URI(baseUrl);
                userURI = new URI(userUrl);
                break;
            case "database":
                prop.put("username", userName);
                prop.put("password", password);
                baseURI = new URI(baseUrl);
                userURI = baseURI;
                userUrl = baseUrl;
                break;
            case "file":
                baseURI = new URI("file:///");
                userURI = baseURI;
                userUrl = "file:///";
                break;
            default: throw new IllegalArgumentException("No FileSystem available for datasource type: " + type);
        }
        synchronized (USED_FILESYSTEMS) {
            FileSystemReference result = USED_FILESYSTEMS.get(userUrl);
            if (result == null) {
                FileSystem fs = existFS(userURI);
                if (fs == null) {
                    if (create) {
                        fs = FileSystems.newFileSystem(baseURI, prop);
                    } else {
                        return null;
                    }
                }
                result = new FileSystemReference(userURI.getScheme(), userUrl, fs);
                USED_FILESYSTEMS.put(userUrl, result);
            }
            if (refId != null) {
                result.addDsRef(refId);
            } else {
                result.addUnknowRef();
            }
            return result;
        }
    }

    private static FileSystem existFS(URI uri) {
        try {
            return FileSystems.getFileSystem(uri);
        } catch(FileSystemNotFoundException ex) {
            return null;
        }
    }

    public static void closeFileSystem(String type, String baseUrl, String userName, String password, Integer refId) {
        switch (type) {
            case "smb":
            case "ftp":
            case "s3":
                if (baseUrl != null) {
                    try {
                        FileSystemReference fsr = getFileSystem(type, baseUrl, userName, password, refId, false);
                        if (fsr != null && fsr.closeFs(refId)) {
                            synchronized(USED_FILESYSTEMS) {
                                USED_FILESYSTEMS.remove(fsr.uri);
                            }
                        }
                    // do not throw exception on close method
                    } catch (IOException | URISyntaxException ex) {
                        LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                    }
                }
        }
    }
    public static void closeFileSystem(FileSystemReference fsr) {
        try {
            if (fsr != null && fsr.close()) {
                synchronized(USED_FILESYSTEMS) {
                    USED_FILESYSTEMS.remove(fsr.uri);
                }
            }
        // do not throw exception on close method
        } catch (UnsupportedOperationException ex) {
            LOGGER.log(Level.FINER, ex.getMessage(), ex);
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
        }
    }

    public static FileSystemReference createZipFileSystem(final Path zipPath) throws IOException {
        String url = "jar:" + zipPath.toUri();
        final URI uri = URI.create(url);
        synchronized (USED_FILESYSTEMS) {
            FileSystemReference result = USED_FILESYSTEMS.get(url);
            if (result == null) {
                FileSystem fs = existFS(uri);
                if (fs == null) {
                    fs = FileSystems.newFileSystem(uri, new HashMap());
                }
                result = new FileSystemReference(uri.getScheme(), url, fs);
                USED_FILESYSTEMS.put(url, result);
            }
            result.addUnknowRef();
            return result;
        }
    }

    public static void removeFileSystemFromCache(String uri) {
        synchronized (USED_FILESYSTEMS) {
            USED_FILESYSTEMS.remove(uri);
        }
    }

    public static List<FileBean> ListFiles(String parentPath, Path path, boolean recursive, boolean flat, boolean fullAnalyse) throws IOException {
        final List<FileBean> listBean = new ArrayList<>();

            if (!Files.exists(path)) {
                throw new IOException("path does not exists:" + path.toString());
            }

            List<Path> children = new ArrayList<>();
            // do not keep opened the stream for too long
            // because it can induce problem withe pooled client FileSystem (like ftp for example).
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                for (Path child : stream) {
                    children.add(child);
                }
            } catch (IOException e) {
                throw new IOException("Error occurs during directory browsing", e);
            }
            for (Path child : children) {
                FileBean fb = analysePath(parentPath, child, fullAnalyse);
                listBean.add(fb);
                if (fb.isFolder() && recursive) {
                    if (flat) {
                        listBean.addAll(ListFiles(fb.getPath(), child, recursive, flat, fullAnalyse));
                    } else {
                        fb.setChildren(ListFiles(fb.getPath(), child, recursive, flat, fullAnalyse));
                    }
                }
            }
            Collections.sort(listBean);
            return listBean;
    }

    public static FileBean analysePath(String parentPath, Path path) {
        return analysePath(parentPath, path, true);
    }

    public static FileBean analysePath(String parentPath, Path path, boolean fullAnalyze) {
        return analysePath(parentPath, path, true, fullAnalyze);
    }

    public static FileBean analysePath(String parentPath, Path path, boolean recursive, boolean fullAnalyze) {
        LOGGER.log(Level.FINER, "ANALYZING:{0}", path.toString());
        String fileName;
        boolean isDir = recursive && Files.isDirectory(path);
        Path fname    = path.getFileName();
        String localPath;
        if (parentPath != null) {
            fileName = fname.toString();
            localPath = parentPath + fileName;
            if (isDir) {
                localPath = localPath + '/';
            }
        } else {
            localPath = "/";
            if (fname != null) {
                fileName = fname.toString();
            } else {
                fileName = "/";
            }
        }
        int size = 0;
        Long lastModified = null;
        if (fullAnalyze && !isDir) {
            try {
                size = (int) Files.size(path);
                lastModified = Files.getLastModifiedTime(path).toMillis();
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "Error while trying get file size / last modified of the file:" + fileName, ex);
            }
        }
        return new FileBean(fileName, isDir, localPath, parentPath, size, lastModified);
    }

    public static Path getPath(final FileSystemReference fs, final String subPath) throws IOException {
        final String url;
        switch (fs.scheme) {
            case "smb":
            case "ftp":
            case "s3":

                url = fs.uri + subPath;
                break;
            case "file":

                url = "file://" + subPath;
                break;
            default:
                url = fs.uri + subPath; // a voir
                break;
        }
        try {
            String encodedUrl = url.replace(" ", "%20");
            encodedUrl = encodedUrl.replace("[", "%5B");
            encodedUrl = encodedUrl.replace("]", "%5D");
            URI u = new URI(encodedUrl);
            try {return new File(u).toPath();} catch (IllegalArgumentException ex){}
            return IOUtilities.toPath(u);
        } catch (Exception e) {
            throw new IOException("Invalid path :" + e.getMessage());
        }
    }
}
