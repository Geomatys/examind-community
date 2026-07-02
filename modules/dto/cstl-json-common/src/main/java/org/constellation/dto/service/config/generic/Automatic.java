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

package org.constellation.dto.service.config.generic;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import org.constellation.dto.service.config.AbstractConfigurationObject;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "automatic")
@JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
public class Automatic extends AbstractConfigurationObject {

    /**
     * The directory where is stored the configuration file.
     * must be set by java, not in the xml file because it is transient.
     */
    @XmlTransient
    private Path configurationDirectory;

    /**
     * The specific type of implementation.
     * could be one of the static flag declared up there.
     * DEFAULT, CSR, FILESYSTEM, PRODLINE, ....
     *
     * @see org.constellation.dto.DataSourceType#VALUES
     */
    @XmlAttribute
    @Deprecated
    private String format;

    /**
     * A name to the object.
     * could be used in a case of multiple automatic in the same file.
     */
    @XmlAttribute
    private String name;

    /**
     * In the case of a fileSystem implementation,
     * this attribute contains the path of the directory containing the data.
     *  -- FileSystem specific flag
     */
    private String dataDirectory;

    private HashMap<String, String> customparameters = new HashMap<>();

    /**
     * Constructor used by JAXB
     */
    public Automatic() {
    }

    /**
     * Build an configuration object for file system dataSource.
     *
     * @param format type of the implementation.
     * @param dataDirectory Directory containing the data file.
     */
    public Automatic(final String format, final String dataDirectory) {
        this.format        = format;
        this.dataDirectory = dataDirectory;
    }

    /**
     * return the type of implementation.
     * @return
     */
    @Deprecated
    public String getFormat() {
        return format;
    }

    /**
     * set the type of implementation.
     *
     * @param format
     */
    @Deprecated
    public void setFormat(final String format) {
        this.format = format;
    }

    /**
     * Return the directory containing the data files.
     * @return
     */
    public Path getDataDirectory() {
        Path result = null;
        if (dataDirectory != null) {
            result = Paths.get(dataDirectory);
            if (!Files.exists(result)){
                // TODO find a way for windows
                if (dataDirectory.startsWith("/")) {
                    return result;
                } else if (configurationDirectory != null && Files.exists(configurationDirectory)){
                    result = configurationDirectory.resolve(dataDirectory);
                }
            }
        }
        return result;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return the customparameters
     */
    public HashMap<String, String> getCustomparameters() {
        if (customparameters == null) {
            customparameters = new HashMap<>();
        }
        return customparameters;
    }

    public void putParameter(final String key, final String value) {
        if (customparameters == null) {
            customparameters = new HashMap<>();
        }
        this.customparameters.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProperty(String key) {
        return getParameter(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setProperty(String key, String value) {
        putParameter(key, value);
    }
    
    public String getParameter(final String key) {
        if (customparameters == null) {
            customparameters = new HashMap<>();
        }
        return customparameters.get(key);
    }
    
    public String getParameter(final String key, String defaultValue) {
        if (customparameters == null) {
            customparameters = new HashMap<>();
        }
        return customparameters.getOrDefault(key, defaultValue);
    }

    public boolean getBooleanParameter(final String key, final boolean defaultValue) {
        if (customparameters == null) {
            customparameters = new HashMap<>();
        }
        if (customparameters.containsKey(key)) {
            return Boolean.parseBoolean(customparameters.get(key));
        }
        return defaultValue;
    }

    public List<String> getParameterList(final String key) {
        final List<String> result = new ArrayList<>();
        if (customparameters == null) {
            customparameters = new HashMap<>();
        }
        final String value = customparameters.get(key);
        if (value != null) {
            final String[] parts = value.split(",");
            result.addAll(Arrays.asList(parts));
        }
        return result;
    }

    public void setParameterList(final String key, List<String> list) {
        if (customparameters == null) {
            customparameters = new HashMap<>();
        }
        String s = ",";
        for (String l : list) {
            s = s + ',' + l;
        }
        s = s.substring(1);
        customparameters.put(key, s);
    }

    public void removeParameter(final String key) {
        if (customparameters == null) {
            customparameters = new HashMap<>();
        }
        customparameters.remove(key);
    }

    /**
     * @param customparameters the customparameters to set
     */
    public void setCustomparameters(final HashMap<String, String> customparameters) {
        this.customparameters = customparameters;
    }

    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder("[Automatic]");
        if (name != null) {
            s.append("name: ").append(name).append('\n');
        }
        if (format != null) {
            s.append("format: ").append(format).append('\n');
        }
        if (dataDirectory != null) {
            s.append("dataDirectory:").append(dataDirectory).append('\n');
        }
        if (configurationDirectory != null) {
            s.append("configurationDirectory:").append(configurationDirectory).append('\n');
        }
        if (customparameters != null) {
            s.append("custom parameters:\n");
            for (Entry entry : customparameters.entrySet()) {
                s.append(entry.getKey()).append(" = ").append(entry.getValue()).append('\n');
            }
        }
        return s.toString();
    }

    /**
     * Verify if this entry is identical to the specified object.
     * @param object The object to compare with.
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (object == null) {
            return false;
        }
        if (this.getClass() == object.getClass()) {
            final Automatic that = (Automatic) object;

            return Objects.equals(this.name  ,           that.name)             &&
                   Objects.equals(this.format  ,         that.format)           &&
                   Objects.equals(this.dataDirectory,    that.dataDirectory)    &&
                   Objects.equals(this.customparameters, that.customparameters);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + (this.format != null ? this.format.hashCode() : 0);
        hash = 37 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 37 * hash + (this.dataDirectory != null ? this.dataDirectory.hashCode() : 0);
        return hash;
    }

}
