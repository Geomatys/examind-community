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
package org.constellation.business;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author Cédric Briançon (Geomatys)
 */
public interface IConfigurationBusiness {

    /**
     * Return the configuration directory (CSTL_HOME).
     *
     * @return never {@code null}.
     */
    Path getConfigurationDirectory();

    /**
     *
     * Return the data directory (CSTL_DATA).
     *
     * @return never {@code null}.
     */
    Path getDataDirectory();

    /**
     *
     * Return the asset directory (CSTL_DATA).
     *
     * @return never {@code null}.
     */
    Path getAssetsDirectory();

    /**
     * Return the specific directory for the specified OGC web service.
     * If the folder does not exist it will be created.
     *
     * @param type Service type (e.g : WMS, WFS, CSW, ...)
     * @param id Service identifier.
     *
     * @return never {@code null}.
     */
    Path getInstanceDirectory(String type, String id);

    /**
     * Remove the specific directory for the specified OGC web service.
     *
     * @param type Service type (e.g : WMS, WFS, CSW, ...)
     * @param id Service identifier.
     */
    void removeInstanceDirectory(String type, String id);

    /**
     * Return all the directories for the specified OGC web service type.
     *
     * @param type Service type (e.g : WMS, WFS, CSW, ...)
     * @return
     * @throws IOException
     */
    List<Path> getInstanceDirectories(String type)throws IOException;

    /**
     * Return the data integrated / provider directory.
     * If the parameter "providerId" is {@code null}, return the complete data integrated folder.
     * If set and if the provider folder does not exist, it will be created.
     *
     * @param providerId Provider identifier.
     * @return never {@code null}.
     *
     * @throws IOException
     */
    Path getDataIntegratedDirectory(String providerId) throws IOException;

    /**
     * Remove recusively the provider directory.
     *
     * @param providerId Provider identifier, must not be {@code null}.
     */
    void removeDataIntegratedDirectory(String providerId);

    /**
     * Return the assignated user upload directory.
     * If not exist, the folder will be created.
     *
     * @param userName User login.
     *
     * @return  never {@code null}.
     * @throws IOException
     */
    Path getUploadDirectory(String userName) throws IOException;

    /**
     * Return all the application properties set.
     * If showSecure is not set, and if the property is marked as "secure", the value returnd will be oblitered.
     *
     * @param showSecure return clear value of secure property.
     * @return all the application properties set.
     */
    Map<String, Object> getProperties(boolean showSecure);

    /**
     * Return an application property value :
     *  - from database
     *  - from environement variables
     *
     * If not set, return the fallback.
     * If showSecure is not set, and if the property is marked as "secure", the value returnd will be oblitered.
     *
     * @param key property key.
     * @param fallback default value if peroperty is not set.
     * @param showSecure return clear value of secure property.
     * 
     * @return an application property value.
     */
    Object getProperty(final String key, final Object fallback, boolean showSecure);

    /**
     * Override an application property, by storig it in the database.
     * 
     * @param key property key.
     * @param value property value.
     */
    void setProperty(final String key, final String value);

    void cleanupFileSystem();

    Properties getMetadataTemplateProperties();

    boolean allowedFilesystemAccess(String path);

}
