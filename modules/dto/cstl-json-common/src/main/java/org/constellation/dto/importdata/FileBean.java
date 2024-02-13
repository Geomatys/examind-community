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
package org.constellation.dto.importdata;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Bean for file information : name and boolean to define folder
 *
 * @author Benjamin Garcia (Geomatys)
 * @author Cédric Briançon (Geomatys)
 * @version 0.9
 * @since 0.9
 */
public class FileBean implements Serializable,Comparable<FileBean> {

    private String name;

    private Boolean folder;

    private String path;

    private String parentPath;

    private Long lastModified;

    private long size = 0;

    private List<StoreFormat> types;

    private List<FileBean> children;

    public FileBean() {

    }

    public FileBean(final String name, final Boolean folder, final String path,
            final String parentPath, final long size, final Map<String, String> types) {
        this.name = name;
        this.folder = folder;
        this.path = path;
        this.parentPath = parentPath;
        this.size = size;
        final List<StoreFormat> sf = new ArrayList<>();
        for (Map.Entry<String, String> entry : types.entrySet()) {
            sf.add(new StoreFormat(entry.getKey(), entry.getValue()));
        }
        this.types = sf;
    }

    public FileBean(final String name, final Boolean folder, final String path,
            final String parentPath, final long size, final List<StoreFormat> types) {
        this.name = name;
        this.folder = folder;
        this.path = path;
        this.parentPath = parentPath;
        this.size = size;
        this.types = types;
    }

    public FileBean(final String name, final Boolean folder, final String path,
            final String parentPath, final long size, final Long lastModified) {
        this.name = name;
        this.folder = folder;
        this.path = path;
        this.parentPath = parentPath;
        this.size = size;
        this.lastModified = lastModified;
    }

    public FileBean(Path path, boolean isLocal) {
        this.name = path.getFileName().toString();
        this.folder = Files.isDirectory(path);
        Path absPath = path.toAbsolutePath();
        if (isLocal) {
            //remove uri schema if local file "file:/...."
            this.path = absPath.toString();
            this.parentPath = absPath.getParent().toString();
        } else {
            this.path = absPath.toUri().toASCIIString();
            this.parentPath = absPath.getParent().toUri().toASCIIString();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Boolean isFolder() {
        return folder;
    }

    public void setFolder(final Boolean folder) {
        this.folder = folder;
    }

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public String getParentPath() {
        return parentPath;
    }

    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }

    @Override
    public int compareTo(FileBean o) {
        return path.compareTo(o.getPath());
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public List<StoreFormat> getTypes() {
        return types;
    }

    public void setTypes(List<StoreFormat> types) {
        this.types = types;
    }

    public Long getLastModified() {
        return lastModified;
    }

    public void setLastModified(Long lastModified) {
        this.lastModified = lastModified;
    }

    public List<FileBean> getChildren() {
        return children;
    }

    public void setChildren(List<FileBean> children) {
        this.children = children;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[FileBean]");
        sb.append(" name:").append(name);
        sb.append(" lastModified:").append(lastModified);
        sb.append(" size:").append(size);
        sb.append(" folder:").append(folder);
        sb.append(" path:").append(path);
        if (children != null && !children.isEmpty()) {
            for (FileBean child : children) {
                sb.append('\n').append(child.toString("---"));
            }
        }
        return sb.toString();
    }

    private String toString(String margin) {
        StringBuilder sb = new StringBuilder(margin);
        sb.append(" name:").append(name);
        sb.append(" lastModified:").append(lastModified);
        sb.append(" size:").append(size);
        sb.append(" folder:").append(folder);
        sb.append(" path:").append(path);
        if (children != null && !children.isEmpty()) {
            for (FileBean child : children) {
                sb.append('\n').append(child.toString(margin + "---"));
            }
        }
        return sb.toString();
    }
}
