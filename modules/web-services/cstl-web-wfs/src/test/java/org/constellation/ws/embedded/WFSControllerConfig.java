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
import org.constellation.wfs.ws.rs.FeatureSetWriter;
import org.constellation.wfs.ws.rs.FeatureTypeGJSWriter;
import org.constellation.ws.rs.provider.NodeReader;
import org.constellation.wfs.ws.rs.SchemaWriter;
import org.constellation.wfs.ws.rs.WFSResponseWriter;
import org.constellation.ws.rs.provider.ExceptionReportWriter;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Configuration
public class WFSControllerConfig  extends WebMvcConfigurationSupport {
    
    public WFSControllerConfig() {
        
    }

    @Override
    protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new FeatureSetWriter());
        converters.add(new FeatureTypeGJSWriter());
        converters.add(new WFSResponseWriter());
        converters.add(new NodeReader());
        converters.add(new SchemaWriter());
        converters.add(new ExceptionReportWriter());
        converters.add(new MappingJackson2HttpMessageConverter());
        converters.add(new StringHttpMessageConverter()); 
    }
}
