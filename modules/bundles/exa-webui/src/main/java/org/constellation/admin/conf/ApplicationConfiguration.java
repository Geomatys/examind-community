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
package org.constellation.admin.conf;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

@Configuration
@ComponentScan(basePackages = {
        "org.constellation.admin.service",
        "org.constellation.admin.security"})
public class ApplicationConfiguration {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.admin.conf");

    @Autowired
    private Environment env;


    /**
     * Initializes exa-webui.
     * Spring profiles can be configured with a system property -Dspring.profiles.active=your-active-profile
     */
    @PostConstruct
    public void initApplication() throws IOException {
        LOGGER.finer("Looking for Spring profiles...");
        if (env.getActiveProfiles().length == 0) {
            LOGGER.finer("No Spring profile configured, running with default configuration");
        } else {
            for (String profile : env.getActiveProfiles()) {
                LOGGER.log(Level.FINER, "Detected Spring profile : {0}", profile);
            }
        }
    }

    @Bean(name = "build")
    public Properties getBuildProperties() throws IOException {
        Properties properties = new Properties();
        try(InputStream inputStream = getClass().getResourceAsStream("/META-INF/constellation-build.properties")){
            properties.load(inputStream);
        }
        return properties;
    }
}
