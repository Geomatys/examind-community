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

    @XmlTransient
    public static final int DEFAULT     = 0;
    @XmlTransient
    public static final int FILESYSTEM  = 1;
    @XmlTransient
    public static final int BYID        = 2;

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
     * The profile of the service (Discovery or transactional).
     */
    private String profile;

    /**
     * In the case of a fileSystem implementation,
     * this attribute contains the path of the directory containing the data.
     *  -- FileSystem specific flag
     */
    private String dataDirectory;

    /**
     * Enable the paralele execution.
     * @deprecated use getParameter("enableThread")
     */
    @Deprecated
    private String enableThread;

    /**
     * Enable the cache of metadata
     * (caution in case of large amount of data)
     * @deprecated use getParameter("enableCache")
     */
    @Deprecated
    private String enableCache;

    /**
     * Enable the indexation of published/all metadata.
     * @deprecated use getParameter("indexOnlyPublishedMetadata")
     */
    @Deprecated
    private Boolean indexOnlyPublishedMetadata;

    /**
     * Allow to disable the indexation part in of the metadataReader,
     * In the operation Harvest and transaction.
     * @deprecated use getParameter("noIndexation")
     */
    @Deprecated
    private String noIndexation;

    /**
     * In the case of a CSW configuration,
     * you can use this flag to substitute the Default catalog harvester,
     * by a ByIdHarvester or a fileSystemHarvester.
     * @deprecated use getParameter("harvester")
     */
    @Deprecated
    private String harvester;

    /**
     * In the case of CSW with a ByIdHarvester,
     * you must set this parameter to indicate
     * to the harvester where to find the file containing the identifiers.
     * @deprecated use getParameter("identifierDirectory")
     */
    @Deprecated
    private String identifierDirectory;

    private HashMap<String, String> customparameters = new HashMap<>();

    private String indexType = "lucene-node";

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
      * Return the type of harvester implementation as a flag.
     * @return
     */
    public int getHarvestType() {
        if ("filesystem".equalsIgnoreCase(harvester))
            return FILESYSTEM;
        else if ("byid".equalsIgnoreCase(harvester))
            return BYID;
        else
            return DEFAULT;
    }

    /**
     * @deprecated use getParameter("transactional")
     */
    @Deprecated
    public int getProfile() {
        if ("discovery".equalsIgnoreCase(profile))
            return 0;
        return 1;
    }

    public String getProfileValue() {
        return profile;
    }

    /**
     * Set the type of profile.
     * @param profile
     */
    public void setProfile(final String profile) {
        this.profile = profile;
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
     * @return the enableThread
     * @deprecated use getParameter("enableThread")
     */
    public String getEnableThread() {
        return enableThread;
    }

    /**
     * @param enableThread the enableThread to set
     * @deprecated use putParameter("enableThread", boolean)
     */
    public void setEnableThread(final String enableThread) {
        this.enableThread = enableThread;
    }

    /**
     * @return the enableCache flag.
     * @deprecated use getParameter("enableCache")
     */
    public String getEnableCache() {
        return enableCache;
    }

    /**
     * @param enableCache the enableCache flag to set
     * @deprecated use putParameter("enableCache", boolean)
     */
    public void setEnablecache(final String enableCache) {
        this.enableCache = enableCache;
    }

    /**
     * @return the noIndexation
     * @deprecated use getParameter("noIndexation")
     */
    public String getNoIndexation() {
        return noIndexation;
    }

    /**
     * @param noIndexation the noIndexation to set
     */
    public void setNoIndexation(final String noIndexation) {
        this.noIndexation = noIndexation;
    }

    /**
     * @return the byIdHarvester
     * @deprecated use getParameter("harvester")
     */
    public String getHarvester() {
        return harvester;
    }

    /**
     * @param harvester the Harvester type to use.
     */
    public void setHarvester(final String harvester) {
        this.harvester = harvester;
    }

    /**
     * @return the identifierDirectory
     * @deprecated use getParameter("identifierDirectory")
     */
    public String getIdentifierDirectory() {
        return identifierDirectory;
    }

    /**
     * @param identifierDirectory the identifierDirectory to set
     */
    public void setIdentifierDirectory(final String identifierDirectory) {
        this.identifierDirectory = identifierDirectory;
    }

    /**
     * @return the indexOnlyPublishedMetadata
     * @deprecated use getParameter("indexOnlyPublishedMetadata")
     */
    public boolean getIndexOnlyPublishedMetadata() {
        if (indexOnlyPublishedMetadata == null) {
            return true;
        }
        return indexOnlyPublishedMetadata;
    }

    /**
     * @param indexOnlyPublishedMetadata the indexOnlyPublishedMetadata to set
     */
    public void setIndexOnlyPublishedMetadata(final Boolean indexOnlyPublishedMetadata) {
        this.indexOnlyPublishedMetadata = indexOnlyPublishedMetadata;
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

    public String getParameter(final String key) {
        if (customparameters == null) {
            customparameters = new HashMap<>();
        }
        return customparameters.get(key);
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

    public String getIndexType() {
        return indexType;
    }

    public void setIndexType(String indexType) {
        this.indexType = indexType;
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
        if (profile != null) {
            s.append("profile:").append(profile).append('\n');
        }
        if (customparameters != null) {
            s.append("custom parameters:\n");
            for (Entry entry : customparameters.entrySet()) {
                s.append(entry.getKey()).append(" = ").append(entry.getValue()).append('\n');
            }
        }
        if (enableCache != null) {
            s.append("enableCache:").append(enableCache).append('\n');
        }
        if (enableThread != null) {
            s.append("enableThread:").append(enableThread).append('\n');
        }
        if (noIndexation != null) {
            s.append("noIndexation:").append(noIndexation).append('\n');
        }
        if (harvester != null) {
            s.append("harvester:").append(harvester).append('\n');
        }
        if (identifierDirectory != null) {
            s.append("identifierDirectory: ").append(identifierDirectory).append('\n');
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
                   Objects.equals(this.enableCache,      that.enableCache)      &&
                   Objects.equals(this.enableThread,     that.enableThread)     &&
                   Objects.equals(this.profile,          that.profile)          &&
                   Objects.equals(this.noIndexation,     that.noIndexation)     &&
                   Objects.equals(this.harvester,        that.harvester)        &&
                   Objects.equals(this.customparameters, that.customparameters);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + (this.format != null ? this.format.hashCode() : 0);
        hash = 37 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 37 * hash + (this.profile != null ? this.profile.hashCode() : 0);
        hash = 37 * hash + (this.enableThread != null ? this.enableThread.hashCode() : 0);
        hash = 37 * hash + (this.enableCache != null ? this.enableCache.hashCode() : 0);
        hash = 37 * hash + (this.dataDirectory != null ? this.dataDirectory.hashCode() : 0);
        hash = 37 * hash + (this.noIndexation != null ? this.noIndexation.hashCode() : 0);
        hash = 37 * hash + (this.harvester != null ? this.harvester.hashCode() : 0);
        return hash;
    }

}
