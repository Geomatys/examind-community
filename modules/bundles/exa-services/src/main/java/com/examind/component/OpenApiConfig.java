package com.examind.component;

import com.github.therapi.runtimejavadoc.ClassJavadoc;
import com.github.therapi.runtimejavadoc.RuntimeJavadoc;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:springdoc.properties")
@Import({
    SpringDocConfiguration.class,
    SpringDocWebMvcConfiguration.class,
    org.springdoc.webmvc.ui.SwaggerConfig.class,
    SwaggerUiConfigProperties.class,
    SwaggerUiOAuthProperties.class,
    SpringDocConfigProperties.class,
    JacksonAutoConfiguration.class,
    org.springdoc.core.configuration.SpringDocJavadocConfiguration.class
})
public class OpenApiConfig {


    @Bean
    public OpenAPI openAPI() {
        
        return new OpenAPI()
                .info(new Info()
                        .title("Examind Community API")
                        .version("1.0-SNAPSHOT")
                        .description("Javadoc generated documentation"));
    }
}
