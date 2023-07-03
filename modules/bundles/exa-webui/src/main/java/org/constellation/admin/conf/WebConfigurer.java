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
package org.constellation.admin.conf;

import org.constellation.admin.web.filter.gzip.GZipServletFilter;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AbstractRefreshableWebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.DispatcherServlet;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletRegistration;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Configuration of web application with Servlet 3.0 APIs.
 */
public class WebConfigurer implements ServletContextListener {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.admin.conf");


    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext servletContext = sce.getServletContext();
        LOGGER.info("Web application configuration");

        LOGGER.finer("Configuring Spring root application context");

        Object rootContextObject = WebApplicationContextUtils.getWebApplicationContext(servletContext);
        AbstractRefreshableWebApplicationContext rootContext;
        if(rootContextObject==null) {
            AnnotationConfigWebApplicationContext annotationRootContext = new AnnotationConfigWebApplicationContext();
            annotationRootContext.register(ApplicationConfiguration.class);
            annotationRootContext.refresh();
            servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, annotationRootContext);
            rootContext = annotationRootContext;

        } else {
            if (rootContextObject instanceof AbstractRefreshableWebApplicationContext) {
                rootContext = (AbstractRefreshableWebApplicationContext) rootContextObject;
            } else {
                throw new IllegalStateException("Root web application context not an instance " +
                        "of AbstractRefreshableWebApplicationContext"+ rootContextObject);
            }
        }

        EnumSet<DispatcherType> disps = EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.ASYNC);

        initSpring(servletContext, rootContext);

        initGzipFilter(servletContext, disps);

        LOGGER.finer("Web application fully configured");
    }



    /**
     * Initializes the GZip filter.
     */
    private void initGzipFilter(ServletContext servletContext, EnumSet<DispatcherType> disps) {
        LOGGER.finer("Registering GZip Filter");

        FilterRegistration.Dynamic compressingFilter = servletContext.addFilter("gzipFilter", new GZipServletFilter());
        Map<String, String> parameters = new HashMap<>();

        compressingFilter.setInitParameters(parameters);

        compressingFilter.addMappingForUrlPatterns(disps, true, "*.css");
        compressingFilter.addMappingForUrlPatterns(disps, true, "*.json");
        compressingFilter.addMappingForUrlPatterns(disps, true, "*.html");
        compressingFilter.addMappingForUrlPatterns(disps, true, "*.js");
        compressingFilter.addMappingForUrlPatterns(disps, true, "*.svg");
        //@TODO add gzip filter for cstl-services war and cstl-bundle
        //compressingFilter.addMappingForUrlPatterns(disps, true, "/app/rest/*");
        //compressingFilter.addMappingForUrlPatterns(disps, true, "/metrics/*");

        compressingFilter.setAsyncSupported(true);
    }

    /**
     * Initializes Spring and Spring MVC.
     */
    private ServletRegistration.Dynamic initSpring(ServletContext servletContext, AbstractRefreshableWebApplicationContext rootContext) {
        LOGGER.finer("Configuring Spring Web application context");
        AnnotationConfigWebApplicationContext dispatcherServletConfiguration = new AnnotationConfigWebApplicationContext();
        dispatcherServletConfiguration.setParent(rootContext);
        dispatcherServletConfiguration.register(DispatcherServletConfiguration.class);

        LOGGER.finer("Registering Spring MVC Servlet");
        ServletRegistration.Dynamic dispatcherServlet = servletContext.addServlet("dispatcher", new DispatcherServlet(
                dispatcherServletConfiguration));
        dispatcherServlet.addMapping("/app/*");
        dispatcherServlet.setLoadOnStartup(1);
        dispatcherServlet.setAsyncSupported(true);
        return dispatcherServlet;
    }


    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOGGER.info("Destroying Web application");
        WebApplicationContext ac = WebApplicationContextUtils.getRequiredWebApplicationContext(sce.getServletContext());
        AbstractRefreshableWebApplicationContext gwac = (AbstractRefreshableWebApplicationContext) ac;
        gwac.close();
        LOGGER.finer("Web application destroyed");
    }
}
