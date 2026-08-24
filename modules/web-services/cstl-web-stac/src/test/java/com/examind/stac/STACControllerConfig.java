package com.examind.stac;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

/**
 * Minimal Spring MVC config used by {@link STACAbstractTest} to bootstrap the
 * embedded servlet context for the STAC controller; message converters are left
 * at Spring Boot defaults (nothing to customize for plain JSON STAC responses).
 *
 * @author Quentin BIALOTA (Geomatys)
 */
@Configuration
public class STACControllerConfig extends WebMvcConfigurationSupport {

    public STACControllerConfig() {
    }

    @Override
    protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
    }
}