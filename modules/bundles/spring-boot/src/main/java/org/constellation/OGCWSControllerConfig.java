/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2017 Geomatys.
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
package org.constellation;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.constellation.coverage.ws.rs.GridCoverageNCWriter;
import org.constellation.coverage.ws.rs.GridCoverageWriter;
import org.constellation.coverage.ws.rs.WCSResponseWriter;
import org.constellation.map.ws.rs.WMSResponseWriter;
import org.constellation.metadata.ws.rs.provider.CSWResponseWriter;
import org.constellation.metadata.ws.rs.provider.NodeWriter;
import org.constellation.sos.ws.rs.provider.SOSResponseWriter;
import org.constellation.sos.ws.rs.provider.SensorMLWriter;
import org.constellation.wfs.ws.rs.FeatureTypeGJSWriter;
import org.constellation.ws.rs.provider.NodeReader;
import org.constellation.wfs.ws.rs.SchemaWriter;
import org.constellation.wfs.ws.rs.WFSResponseWriter;
import org.constellation.wps.ws.rs.BoundingBoxWriter;
import org.constellation.wps.ws.rs.FeatureSetWriter;
import org.constellation.wps.ws.rs.FileWriter;
import org.constellation.wps.ws.rs.GeometryWriter;
import org.constellation.wps.ws.rs.WPSResponseWriter;
import org.constellation.ws.rs.provider.ExceptionReportWriter;
import org.constellation.api.rest.converter.PortrayalMessageConverter;
import org.constellation.api.rest.converter.ProfileMessageConverter;
import org.constellation.ws.rs.provider.ByteArrayWriter;
import org.constellation.ws.rs.provider.RenderedImageWriter;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Configuration
public class OGCWSControllerConfig extends WebMvcConfigurationSupport {

    @Override
    protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new ResourceHttpMessageConverter());
        converters.add(new WMSResponseWriter());
        converters.add(new CSWResponseWriter());
        converters.add(new NodeWriter());
        converters.add(new org.constellation.wfs.ws.rs.FeatureSetWriter());
        converters.add(new FeatureTypeGJSWriter());
        converters.add(new WFSResponseWriter());
        converters.add(new NodeReader());
        converters.add(new SchemaWriter());
        converters.add(new BoundingBoxWriter());
        converters.add(new FeatureSetWriter());
        converters.add(new FileWriter());
        converters.add(new GeometryWriter());
        converters.add(new WPSResponseWriter());
        converters.add(new SOSResponseWriter());
        converters.add(new SensorMLWriter());
        converters.add(new WCSResponseWriter());
        converters.add(new GridCoverageNCWriter());
        converters.add(new GridCoverageWriter());
        converters.add(new PortrayalMessageConverter());
        converters.add(new ProfileMessageConverter());
        converters.add(new RenderedImageWriter());
        converters.add(new ExceptionReportWriter());
        converters.add(new ByteArrayWriter());
        converters.add(new StringHttpMessageConverter(StandardCharsets.UTF_8));
        converters.add(new MappingJackson2HttpMessageConverter());
    }

}
