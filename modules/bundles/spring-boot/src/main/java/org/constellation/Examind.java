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

import com.bradmcevoy.http.MiltonServlet;
import com.codahale.metrics.servlet.InstrumentedFilter;
import com.codahale.metrics.servlets.AdminServlet;
import com.sun.xml.ws.transport.http.servlet.WSSpringServlet;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.servlet.DispatcherType;
import org.apache.catalina.Context;
import org.constellation.admin.SessionListener;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.constellation.filter.CorsFilter;
import org.constellation.services.logger.MDCFilter;
import org.constellation.setup.CstlInstaller;
import org.constellation.webdav.ws.WebdavService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Configuration
@ServletComponentScan(basePackages = "org.constellation.admin.listener")
@ImportResource({"classpath*:spring/applicationContext.xml"})
public class Examind extends SpringBootServletInitializer {

    public static String[] CSTL_SPRING_PACKAGE = new String[] {
            "org.constellation.configuration.ws",
            "org.constellation.map.ws.rs",
            "org.constellation.metadata.ws.rs",
            "org.constellation.ws.rs.provider",
            "org.constellation.coverage.ws.rs",
            "org.constellation.wfs.ws.rs",
            "org.constellation.sos.ws.rs",
            "org.constellation.sos.ws.rs.provider",
            "org.constellation.wmts.ws.rs",
            "org.constellation.metadata.ws.rs.provider",
            "org.constellation.wps.ws.rs",
            "org.constellation.webdav",
            "org.constellation.thesaurus.ws.rs"};

    public static String CSTL_JERSEY_PACKAGE =
            "org.constellation.configuration.ws;\n" +
            "org.constellation.map.ws.rs;\n" +
            "org.constellation.metadata.ws.rs;\n" +
            "org.constellation.ws.rs.provider;\n" +
            "org.constellation.coverage.ws.rs;\n" +
            "org.constellation.wfs.ws.rs;\n" +
            "org.constellation.sos.ws.rs;\n" +
            "org.constellation.sos.ws.rs.provider;\n" +
            "org.constellation.wmts.ws.rs;\n" +
            "org.constellation.metadata.ws.rs.provider;\n" +
            "org.constellation.wps.ws.rs;\n" +
            "org.constellation.webdav;\n" +
            "org.constellation.thesaurus.ws.rs";


    public static void main(String[] args) {
        SpringApplication.run(Examind.class, args);
    }

    public Examind(){
        // in netbeans set this up to avoid polluting your .constellation
        //ConfigDirectory.setupTestEnvironement("boot");
    }

   /****************************************************************************/
   /*    The 3 beans below are used if you comment  @SpringBootApplication
    */
   /****************************************************************************/

    /**
     * needed to start the container
     *
     * @return ServletWebServerFactory
     */
    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessContext(Context context) {
                super.postProcessContext(context);

                context.setSessionTimeout((int)TimeUnit.MINUTES.toMillis(10));
                context.addApplicationListener("org.apache.tomcat.websocket.server.WsContextListener");
            }
        };

        tomcat.setPort(Application.getIntegerProperty(AppProperty.CSTL_PORT, 9000));

        return tomcat;
    }

    /**
     * Manually register a dispatcher servlet with the selected controllers
     * @return
     */
    @Bean
    public ServletRegistrationBean examindapi() {
        final DispatcherServlet servlet = new DispatcherServlet();
        final AnnotationConfigWebApplicationContext appContext = new AnnotationConfigWebApplicationContext();
        appContext.scan("org.constellation.api.rest");
        appContext.register(RestAPIControllerConfig.class);
        servlet.setApplicationContext(appContext);
        final ServletRegistrationBean servletBean = new ServletRegistrationBean(servlet, "/API/*");
        servletBean.setName("examindapi");
        servletBean.setLoadOnStartup(1);
        servletBean.setAsyncSupported(true);
        return servletBean;
    }

    /**
     * Manually register a dispatcher servlet with the selected controllers
     * @return
     */
    @Bean
    public ServletRegistrationBean ogcServiceServlet() {
        DispatcherServlet dispatcherServlet = new DispatcherServlet();
        AnnotationConfigWebApplicationContext applicationContext = new AnnotationConfigWebApplicationContext();
        applicationContext.scan(CSTL_SPRING_PACKAGE);
        applicationContext.register(OGCWSControllerConfig.class);
        dispatcherServlet.setApplicationContext(applicationContext);
        ServletRegistrationBean servletRegistrationBean = new ServletRegistrationBean(dispatcherServlet, "/WS/*");
        servletRegistrationBean.setName("ogc-WS");
        servletRegistrationBean.setLoadOnStartup(1);
        return servletRegistrationBean;
    }


    @Bean
    public FilterRegistrationBean corsFilter() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        CorsFilter corsFilter = new CorsFilter();
        registration.setFilter(corsFilter);
        registration.setName("CorsFilter");
        registration.addUrlPatterns("/*");
        Map<String,String> initParams = new HashMap<>();
        initParams.put("exclude", "/spring/ws/.*");
        registration.setInitParameters(initParams);
        return registration;
    }

    @Bean
    public FilterRegistrationBean logFilter() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        MDCFilter mdcFilter = new MDCFilter();
        registration.setFilter(mdcFilter);
        registration.setName("MDCFilter");
        registration.addUrlPatterns("/WS/*");
        return registration;
    }


    @Bean
    public GeotkInstaller geotkInstaller() {
        return new GeotkInstaller();
    }

    @Bean
    public CstlInstaller cstlInstaller() {
        return new CstlInstaller();
    }

    @Bean
    public SessionListener sessionListener() {
        return new SessionListener();
    }

    /**
     * Manually register a JAX WS servlet
     * @return
     */
    @Bean
    public ServletRegistrationBean ogcServiceSOAPServlet() {
        ServletRegistrationBean reg = new ServletRegistrationBean(new WSSpringServlet(),"/WS-SOAP/*");
        reg.setName("WS-SOAP");
        reg.setLoadOnStartup(1);
        return reg;
    }


    @Bean
    public ServletRegistrationBean webdavServlet() {
        ServletRegistrationBean registration = new ServletRegistrationBean(new MiltonServlet(), "/webdav/*");
        registration.addInitParameter("resource.factory.class", WebdavService.class.getName());
        registration.setName("milton");
        registration.setLoadOnStartup(99);
        return registration;
    }


    @Bean
    public FilterRegistrationBean metricsFilter() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        InstrumentedFilter instrumentFIlter = new InstrumentedFilter();
        registration.setFilter(instrumentFIlter);
        registration.setName("webappMetricsFilter");
        EnumSet<DispatcherType> disps = EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.ASYNC);
        registration.setMatchAfter(true);
        registration.setDispatcherTypes(disps);
        registration.addUrlPatterns("/*");
        registration.setAsyncSupported(true);

        return registration;
    }

    @Bean
    public ServletRegistrationBean metricsServlet() {
        ServletRegistrationBean registration = new ServletRegistrationBean(new AdminServlet(), "/metrics/*");
        registration.setName("metricsAdminServlet");
        registration.setLoadOnStartup(2);
        return registration;
    }

}