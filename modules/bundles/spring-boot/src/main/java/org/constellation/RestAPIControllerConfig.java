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
import org.constellation.api.rest.converter.JsonStyleMessageConverter;
import org.constellation.api.rest.converter.JsonWrapperIntervalMessageConverter;
import org.constellation.api.rest.converter.PortrayalMessageConverter;
import org.constellation.api.rest.converter.ProfileMessageConverter;
import org.constellation.api.rest.converter.StyleMessageConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.MarshallingHttpMessageConverter;
import org.springframework.oxm.xstream.XStreamMarshaller;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Configuration
public class RestAPIControllerConfig extends WebMvcConfigurationSupport {

    @Override
    protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new ResourceHttpMessageConverter());
        converters.add(new StyleMessageConverter());
        converters.add(new JsonStyleMessageConverter());
        converters.add(new JsonWrapperIntervalMessageConverter());
        converters.add(new PortrayalMessageConverter());
        converters.add(new ProfileMessageConverter());
        converters.add(new ResourceHttpMessageConverter());
        converters.add(new MappingJackson2HttpMessageConverter());
        converters.add(new StringHttpMessageConverter(StandardCharsets.UTF_8));

        final XStreamMarshaller marshaller = new XStreamMarshaller();
        converters.add(new MarshallingHttpMessageConverter(marshaller,marshaller));
    }
}
