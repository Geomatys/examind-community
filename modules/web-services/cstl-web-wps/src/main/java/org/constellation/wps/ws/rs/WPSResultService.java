/*
 * Copyright 2018 Geomatys.
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
package org.constellation.wps.ws.rs;

import com.examind.wps.DefaultWPSWorker;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.sis.util.logging.Logging;
import org.constellation.api.rest.ErrorMessage;
import org.constellation.business.IServiceBusiness;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.service.config.wps.ProcessContext;
import org.constellation.exception.ConfigurationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service to access job products datas.
 *
 * @author Johann Sorel (Geomatys)
 */
@RestController
@RequestMapping("wps/{serviceId}/products")
public class WPSResultService {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.wps.ws.rs");

    @Inject
    protected IServiceBusiness serviceBusiness;

    public WPSResultService() {
    }

    @RequestMapping(path = "/**", method = RequestMethod.GET)
    public ResponseEntity getProduct(@PathVariable("serviceId") String serviceId, HttpServletRequest request, HttpServletResponse response) {
        String uri = request.getRequestURI();
        final String split = "/WS/wps/"+serviceId+"/products/";
        uri = uri.substring(uri.indexOf(split)+split.length());

        //search for the requested file
        Path path = getOutputDirectory(serviceId);
        path = path.resolve(DefaultWPSWorker.PATH_PRODUCTS_NAME);
        for (String part : uri.split("/")) {
            path = path.resolve(part);
        }

        if (Files.exists(path)) {
            try {
                response.addHeader("Content-Disposition", "attachment; filename="+path.getFileName().toString());
                Files.copy(path,response.getOutputStream());
                response.flushBuffer();
                return new ResponseEntity(HttpStatus.OK);
            } catch (IOException ex) {
                return new ErrorMessage(ex).build();
            }
        } else {
            return new ErrorMessage(HttpStatus.NOT_FOUND).build();
        }

    }

    private Path getOutputDirectory(String serviceId) {
        try {
            final Object obj = serviceBusiness.getConfiguration("WPS", serviceId);
            if (obj instanceof ProcessContext) {
                final ProcessContext ctx = (ProcessContext) obj;
                if (ctx.getOutputDirectory() != null && !ctx.getOutputDirectory().isEmpty()) {
                    try {
                        return Paths.get(new URI(ctx.getOutputDirectory()));
                    } catch (URISyntaxException ex) {
                        LOGGER.log(Level.WARNING, "Error while reading custom output directory", ex);
                    }
                }
            }

        } catch (ConfigurationException ex) {
            LOGGER.log(Level.WARNING, "Error while reading wps " + serviceId + " configuration", ex);
        }
        return ConfigDirectory.getInstanceDirectory("wps", serviceId);
    }

}
