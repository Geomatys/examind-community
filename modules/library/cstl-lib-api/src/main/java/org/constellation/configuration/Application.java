/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2015 Geomatys.
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.util.logging.Logging;
import org.constellation.exception.ConfigurationRuntimeException;

/**
 * Utility class that gathers and merge configurations from embedded/external property files,
 * system properties and system environment variables.
 * The resolving order is :
 * <ol>
 *     <li>System environment variables. Evaluated at runtime and use env variable convention naming</li>
 *     <li>System property variables (with <code>-Dvar=value</code> or <code>System.setProperty("var", "value");</code>). Evaluated at runtime</li>
 *     <li>External property file (referenced with <code>-Dcstl.config=/path/to/config.properties</code> option). Evaluated once.</li>
 *     <li>Embedded property file in resources. Evaluated once</li>
 * </ol>
 *
 * Usage : <br>
 * <code>
 *     String cstlHome = Application.getProperty(Application.CSTL_HOME_KEY);
 * </code>
 * @author Quentin Boileau (Geomatys)
 */
public final class Application {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.dto");

    private static final Properties APP_PROPERTIES = new Properties();

    //lazy singleton to call initialization
    private static final Application INSTANCE = new Application();

    private Application() {

        //load embedded configuration file
        final String resourcePath = "/org/constellation/configuration/constellation.properties";
        try (final InputStream classLoaderSettings = Application.class.getResourceAsStream(resourcePath)) {
            LOGGER.info("Load default configuration.");
            APP_PROPERTIES.load(classLoaderSettings);
        } catch (IOException e) {
            throw new ConfigurationRuntimeException("Unable to load default configuration file from current class loader", e);
        }

        //search for external configuration file
        String extConfKey = AppProperty.CSTL_CONFIG.getKey();
        String envKey = toEnvConvention(extConfKey);
        String externalConf = System.getenv(envKey);
        if (externalConf == null) {
            externalConf = System.getProperty(extConfKey);
        }
        if (externalConf != null) {
            LOGGER.log(Level.INFO, "Load external configuration from {0}", externalConf);

            final Path externalFile = Paths.get(externalConf);
            if (!Files.isRegularFile(externalFile)) {
                LOGGER.warning("Unable to load external configuration because path is not a valid file.");
            }

            try (InputStream fin = Files.newInputStream(externalFile)) {
                APP_PROPERTIES.load(fin);
            } catch (IOException e) {
                //no need to crash application because of an invalid external configuration file.
                LOGGER.log(Level.WARNING, "Unable to load properties from external configuration file", e);
            }
        }

    }

    /**
     * Get all constellation settings.
     *
     * @return a merge of default, external and environment properties
     */
    private static Properties getProperties() {
        return (Properties) APP_PROPERTIES.clone();
    }

    /**
     * Search for the property with the specified key in {@link #getProperties()}
     * Look first in system environment variables and if nothing found, fallback
     * to application properties.
     *
     * @param key AppProperty
     * @return property value or <code>null</code> if not found.
     */
    public static String getProperty(AppProperty key) {
        return getProperty(key.getKey(), null);
    }

    /**
     * Search for the property with the specified key in {@link #getProperties()}
     * Look first in system environment variables and if nothing found, fallback
     * to application properties.
     *
     * @param key property key
     * @return property value or <code>null</code> if not found.
     */
    public static String getProperty(String key) {
        return getProperty(key, null);
    }

    /**
     * Search for the property with the specified key in {@link #getProperties()}
     * Look first in system environment variables and if nothing found, fallback
     * to application properties.
     *
     * @param key AppProperty
     * @param fallback fallback used if property not found.
     * @return property value or fallback value if not found.
     */
    public static String getProperty(AppProperty key, String fallback) {
        return getProperty(key.getKey(), fallback);
    }

    /**
     * Search for the property with the specified key in {@link #getProperties()}
     * Look first in system environment variables and if nothing found, fallback
     * to application properties.
     * Parse it into a boolean then.
     *
     * @param key AppProperty
     * @param fallback fallback used if property not found.
     * @return property value or fallback value if not found.
     */
    public static boolean getBooleanProperty(AppProperty key, boolean fallback) {
        String val = getProperty(key.getKey(), null);
        if (val != null) {
            return Boolean.parseBoolean(val);
        }
        return fallback;
    }

    public static int getIntegerProperty(AppProperty key, int fallback) {
        String val = getProperty(key.getKey(), null);
        if (val != null) {
            return Integer.parseInt(val);
        }
        return fallback;
    }

    public static Integer getIntegerProperty(AppProperty key) {
        String val = getProperty(key.getKey(), null);
        if (val != null) {
            return Integer.parseInt(val);
        }
        return null;
    }

    public static long getLongProperty(AppProperty key, long fallback) {
        String val = getProperty(key.getKey(), null);
        if (val != null) {
            return Long.parseLong(val);
        }
        return fallback;
    }

    /**
     * Search for the property with the specified key in {@link #getProperties()}
     * Look first in system environment variables and if nothing found, fallback
     * to application properties.
     *
     * @param key property key
     * @param fallback fallback used if property not found.
     * @return property value or fallback value if not found.
     */
    public static String getProperty(String key, String fallback) {
        //check env
        String envKey = toEnvConvention(key);
        final String envValue = System.getenv(envKey);
        if (envValue != null) {
            return envValue;
        }

        //check system properties
        final String propValue = System.getProperty(key);
        if (propValue != null) {
            return propValue;
        }

        //check conf
        return getProperties().getProperty(key, fallback);
    }

    /**
     * Translate a property key from java convention ("path.to.property") into an
     * environment variable convention ("PATH_TO_PROPERTY")
     * @param key in java convention
     * @return translated key
     */
    private static String toEnvConvention(String key) {
        return key.toUpperCase().replace('.', '_');
    }

    /**
     * Search for all application properties matching given prefix.
     *
     * @param prefix property key
     * @return all property that match given prefix. Output {@link Properties} can be
     * empty but never null.
     */
    public static Properties getProperties(String prefix) {
        Properties properties = new Properties();
        final Properties appProperties = getProperties();
        for (Map.Entry<Object, Object> appProp : appProperties.entrySet()) {
            if (((String)appProp.getKey()).startsWith(prefix)) {
                properties.put(appProp.getKey(), appProp.getValue());
            }
        }

        //override with system properties variables
        final Properties systemProperties = System.getProperties();
        for (Map.Entry<Object, Object> sysProp : systemProperties.entrySet()) {
            if (((String)sysProp.getKey()).startsWith(prefix)) {
                properties.put(sysProp.getKey(), sysProp.getValue());
            }
        }

        //override with system environment variables
        String envPrefix = toEnvConvention(prefix);
        final Map<String, String> envProps = System.getenv();
        for (Map.Entry<String, String> env : envProps.entrySet()) {
            if (env.getKey().startsWith(envPrefix)) {
                properties.put(env.getKey(), env.getValue());
            }
        }
        return properties;
    }
}
