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
package org.constellation.ws.embedded.wps;

import java.util.List;
import org.constellation.coverage.ws.rs.GridCoverageWriter;
import org.constellation.coverage.ws.rs.WCSResponseWriter;
import org.constellation.wps.ws.rs.BoundingBoxWriter;
import org.constellation.wps.ws.rs.FeatureSetWriter;
import org.constellation.wps.ws.rs.FileWriter;
import org.constellation.wps.ws.rs.GeometryWriter;
import org.constellation.wps.ws.rs.WPSJSONResponseWriter;
import org.constellation.wps.ws.rs.WPSResponseWriter;
import org.constellation.ws.rs.provider.ExceptionReportWriter;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

/**
 *
 * @author guilhem
 */
@Configuration
public class WPSControllerConfig  extends WebMvcConfigurationSupport {

    public WPSControllerConfig() {

    }

    @Override
    protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new BoundingBoxWriter());
        converters.add(new FeatureSetWriter());
        converters.add(new FileWriter());
        converters.add(new GeometryWriter());
        converters.add(new WPSJSONResponseWriter());
        converters.add(new WPSResponseWriter());
        // <--openEO secondary-service tests publish real WCS instances and need to
        // parse their GetCapabilities XML response, alongside the WPS converters above.
        converters.add(new WCSResponseWriter());
        converters.add(new GridCoverageWriter());
        // openEO-->
        converters.add(new ExceptionReportWriter());
        converters.add(new MappingJackson2HttpMessageConverter());
    }
}
