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

package org.constellation.dto.service.config.sos;

import org.constellation.dto.service.config.AbstractConfigurationObject;
import org.constellation.dto.service.config.generic.Automatic;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import org.constellation.dto.service.config.DataSourceType;

/**
 * A XML binding object for SOS configuration.
 *
 * @author Guilhem Legal (Geomatys)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "SOSConfiguration")
public class SOSConfiguration extends AbstractConfigurationObject {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.dto");

    /**
     * Informations about SensorML Datasource.
     */
    @Deprecated
    @XmlElement(name="SMLConfiguration")
    private Automatic smlConfiguration;

    /**
     * Implementation type for observation filter.
     */
    @Deprecated
    private DataSourceType observationFilterType;

    /**
     * Implementation type for observation reader.
     */
    @Deprecated
    private DataSourceType observationReaderType;

    /**
     * Implementation type for observation writer.
     */
    @Deprecated
    private DataSourceType observationWriterType;

    /**
     * type of the datasource for SensorML Datasource.
     */
    @Deprecated
    @XmlElement(name="SMLType")
    private DataSourceType smlType;

    /**
     * Informations about O&M Datasource.
     */
    @Deprecated
    @XmlElement(name="OMConfiguration")
    private Automatic omConfiguration;

    /**
     * Other datasource informations (used by sub-implmentations).
     */
    private List<Automatic> extensions;

    private HashMap<String, String> parameters = new HashMap<>();

    /**
     * prefix for observations id (example: urn:ogc:object:observation:orgName:)
     * @deprecated move to parameters map
     */
    @Deprecated
    private String observationIdBase;

    /**
     * prefix for phenomenons id (example: urn:ogc:def:phenomenon:orgName:)
     * @deprecated move to parameters map
     */
    @Deprecated
    private String phenomenonIdBase;

    /**
     * prefix for observation templates id (example: urn:ogc:object:observationTemplate:orgName:)
    * @deprecated move to parameters map
     */
    @Deprecated
    private String observationTemplateIdBase;

    /**
     * prefix for sensorML id (example: urn:ogc:object:sensor:orgName:)
     * @deprecated move to parameters map
     */
    @Deprecated
    private String sensorIdBase;

    /**
     * profile of the SOS (discovery / transactional)
     * @deprecated move to parameters map
     */
    @Deprecated
    private String profile;

    /**
     * Empty constructor used by JAXB.
     */
    public SOSConfiguration() {

    }

    /**
     * @return the SMLConfiguration
     */
    @Deprecated
    public Automatic getSMLConfiguration() {
        return smlConfiguration;
    }

    /**
     * @return the OMConfiguration
     */
    @Deprecated
    public Automatic getOMConfiguration() {
        return omConfiguration;
    }

    /**
     * @return the observationReaderType
     */
    @Deprecated
    public DataSourceType getObservationReaderType() {
        return observationReaderType;
    }

    /**
     * @return the SMLType
     */
    @Deprecated
    public DataSourceType getSMLType() {
        return smlType;
    }

    public void clearSMLDeprecatedAttibute() {
        this.smlType = null;
        this.smlConfiguration = null;
    }

    public void clearOMDeprecatedAttibute() {
        this.smlConfiguration = null;
        this.observationReaderType = null;
        this.observationWriterType = null;
    }

    /**
     * @return the observationIdBase
     */
    @Deprecated
    public String getObservationIdBase() {
        return observationIdBase;
    }

    /**
     * return the phenomenon id prefix.
     * @return
     */
    @Deprecated
    public String getPhenomenonIdBase() {
        return phenomenonIdBase;
    }

    /**
     * @return the observationTemplateIdBase
     */
    @Deprecated
    public String getObservationTemplateIdBase() {
        return observationTemplateIdBase;
    }

    /**
     * @return the sensorIdBase
     */
    @Deprecated
    public String getSensorIdBase() {
        return sensorIdBase;
    }

    /**
     * @return the observationWriterType
     */
    public DataSourceType getObservationWriterType() {
        return observationWriterType;
    }

    /**
     * Return a flag for the SOS profile (discovery/transactional)
     * @return
     */
    @Deprecated
    public int getProfile() {
        if ("transactional".equalsIgnoreCase(profile)) {
            return 1;
        }
        return 0;
    }

    @Deprecated
    public String getProfileValue() {
        return profile;
    }

    /**
     * set the flag for the SOS profile (discovery/transactional)
     * @param profile
     */
    @Deprecated
    public void setProfile(final String profile) {
        this.profile = profile;
    }

    /**
     * @return the extensions
     */
    public List<Automatic> getExtensions() {
        if (extensions == null) {
            extensions = new ArrayList<>();
        }
        return extensions;
    }

    /**
     * @param extensions the extensions to set
     */
    public void setExtensions(final List<Automatic> extensions) {
        this.extensions = extensions;
    }

    /**
     * Replace all the password in this object by '****'
     */
    public void hideSensibleField() {
        for (Automatic aut: getExtensions()) {
            aut.hideSensibleField();
        }
        if (omConfiguration != null) {
            omConfiguration.hideSensibleField();
        }
        if (omConfiguration != null) {
            smlConfiguration.hideSensibleField();
        }
    }

    /**
     * @return the parameters
     */
    public HashMap<String, String> getParameters() {
        if (parameters == null) {
            this.parameters = new HashMap<>();
        }
        return parameters;
    }

    public String getParameter(final String name) {
        if (parameters != null) {
            return parameters.get(name);
        }
        return null;
    }

    public boolean getBooleanParameter(final String name, boolean defaultValue) {
        if (parameters != null) {
            final String value = parameters.get(name);
            if (value != null) {
                return Boolean.parseBoolean(value);
            }
        }
        return defaultValue;
    }

    /**
     * @param parameters the parameters to set
     */
    public void setParameters(HashMap<String, String> parameters) {
        this.parameters = parameters;
    }
}
