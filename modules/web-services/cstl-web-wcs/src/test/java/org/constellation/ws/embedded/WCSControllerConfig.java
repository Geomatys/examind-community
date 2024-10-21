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
package org.constellation.ws.embedded;

import java.util.List;

import com.examind.ogc.api.rest.common.converter.CommonResponseConverter;
import org.constellation.coverage.ws.rs.GridCoverageNCWriter;
import org.constellation.coverage.ws.rs.GridCoverageWriter;
import org.constellation.coverage.ws.rs.WCSResponseWriter;
import org.constellation.ws.rs.provider.ExceptionReportWriter;
import org.constellation.api.rest.converter.PortrayalMessageConverter;
import org.constellation.api.rest.converter.ProfileMessageConverter;
import org.constellation.ws.rs.provider.RenderedImageWriter;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

/**
 *
 * @author guilhem
 */
@Configuration
public class WCSControllerConfig  extends WebMvcConfigurationSupport {

    public WCSControllerConfig() {

    }

    @Override
    protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new MappingJackson2HttpMessageConverter());
        converters.add(new WCSResponseWriter());
        converters.add(new GridCoverageNCWriter());
        converters.add(new GridCoverageWriter());
        converters.add(new PortrayalMessageConverter());
        converters.add(new ProfileMessageConverter());
        converters.add(new RenderedImageWriter());
        converters.add(new ExceptionReportWriter());
        converters.add(new StringHttpMessageConverter());
        converters.add(new CommonResponseConverter());
    }
}
