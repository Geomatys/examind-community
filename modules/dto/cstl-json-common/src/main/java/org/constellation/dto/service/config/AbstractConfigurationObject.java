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
package org.constellation.dto.service.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.constellation.dto.service.config.generic.Automatic;

import javax.xml.bind.annotation.XmlSeeAlso;
import org.constellation.dto.service.config.sos.SOSConfiguration;
import org.constellation.dto.service.config.webdav.WebdavContext;
import org.constellation.dto.service.config.wps.ProcessContext;
import org.constellation.dto.service.config.wxs.LayerContext;

/**
 * @author Cédric Briançon (Geomatys)
 */
@XmlSeeAlso({LayerContext.class,ProcessContext.class,Automatic.class,SOSConfiguration.class,WebdavContext.class})
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = LayerContext.class, name = "LayerContext"),
    @JsonSubTypes.Type(value = ProcessContext.class, name = "ProcessContext"),
    @JsonSubTypes.Type(value = Automatic.class, name = "Automatic"),
    @JsonSubTypes.Type(value = SOSConfiguration.class, name = "SOSConfiguration"),
    @JsonSubTypes.Type(value = WebdavContext.class, name = "WebdavContext")
})
public abstract class AbstractConfigurationObject {
}
