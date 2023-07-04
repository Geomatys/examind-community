/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014-2017 Geomatys.
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
package org.constellation.api.rest;

import ch.qos.logback.classic.LoggerContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;
import org.apache.commons.lang3.StringUtils;
import org.constellation.business.IConfigurationBusiness;
import org.constellation.repository.PropertyRepository;
import org.constellation.dto.AcknowlegementType;
import org.constellation.dto.SimpleValue;
import org.constellation.dto.StringList;
import org.constellation.util.json.JsonUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import static org.springframework.http.HttpStatus.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import static org.springframework.web.bind.annotation.RequestMethod.*;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
@RestController
public class AdminRestAPI extends AbstractRestAPI {

    @Autowired
    private PropertyRepository propertyRepository;
    @Autowired
    private IConfigurationBusiness configurationBusiness;


    /**
     * Get server configuration folder.
     *
     * @return configuration path
     */
    @RequestMapping(value="/admin/configurationLocation", method=GET, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity configurationPath() {
        try {
            final String path = configurationBusiness.getConfigurationDirectory().toString();
            return new ResponseEntity(new AcknowlegementType(true, path), OK);
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Get configuration properties values.
     *
     * @return the value of the constellation property
     */
    @RequestMapping(value="/admin/properties", method=GET, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getProperties() {
        try {
            Map<String, Object> results = configurationBusiness.getProperties(false);
            return new ResponseEntity(results,OK);
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Get configuration property value.
     *
     * @param key property key
     * @return the value of the constellation property
     */
    @RequestMapping(value="/admin/property/{key:.+}", method=GET, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getProperty(@PathVariable("key") String key) {
        try {
            Object obj = configurationBusiness.getProperty(key, null, false);
            if (obj instanceof List) {
                return new ResponseEntity(new StringList((List<String>) obj),OK);
            }
            return new ResponseEntity(new SimpleValue(obj),OK);
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Set configuration property value.
     * <br>
     * Note that restarting the instance may still be required.
     *
     * @param key property key
     * @param value property value
     */
    @RequestMapping(value="/admin/property/{key:.+}", method=POST, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity setProperty(@PathVariable("key") String key,
            @RequestBody SimpleValue value) {
        try {
            configurationBusiness.setProperty(key, value.getValue());
            return new ResponseEntity(OK);
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/admin/contact", method=GET, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getContact() {
        try {
            final Map<String, String> properties = propertyRepository.startWith("contact.%");
            final Properties javaProperties = new Properties();
            for (Entry<String, String> property : properties.entrySet()) {
                javaProperties.put(property.getKey(), property.getValue());
            }

            return new ResponseEntity(JsonUtils.toJSon(javaProperties),OK);
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value = "/admin/contact", method=POST, consumes=MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity updateContact(@RequestBody HashMap<String, Object> contact) {
        try {
            final Properties properties = JsonUtils.toProperties(contact);
            final Map<String, String> propertiesDB = propertyRepository.startWith("contact.%");

            for (Entry<String, String> entry : propertiesDB.entrySet()) {
                final String posted = properties.getProperty(entry.getKey());
                if (StringUtils.isNotBlank(posted)) {
                    entry.setValue(posted);
                    properties.remove(entry.getKey());
                } else {
                    propertyRepository.delete(entry.getKey());
                }
            }

            for (Entry<Object, Object> entry : properties.entrySet()) {
                propertyRepository.update((String) entry.getKey(), (String) entry.getValue());
            }
            return new ResponseEntity(OK);
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value = "admin/rungc", method = GET)
    public ResponseEntity runGC() {
        System.gc();
        return new ResponseEntity(NO_CONTENT);
    }

    @RequestMapping(value = "admin/loggers", method = GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getLoggers() {
        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        final List<org.constellation.dto.Logger> loggers = new ArrayList<>();
        for (ch.qos.logback.classic.Logger logger : context.getLoggerList()) {
            loggers.add(new org.constellation.dto.Logger(logger.getName(), logger.getEffectiveLevel().toString()));
        }
        return new ResponseEntity(loggers,OK);
    }

    @RequestMapping(value = "admin/loggers", method = PUT)
    public ResponseEntity updateLogger(@RequestBody org.constellation.dto.Logger jsonLogger) {
        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        final ch.qos.logback.classic.Level level = ch.qos.logback.classic.Level.valueOf(jsonLogger.getLevel());
        context.getLogger(jsonLogger.getName()).setLevel(level);
        return new ResponseEntity(NO_CONTENT);
    }

}
