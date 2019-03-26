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
package org.constellation.webdav.ws;

import org.constellation.api.ServiceDef;
import org.constellation.ws.CstlServiceException;
import org.constellation.ws.Worker;
import org.constellation.ws.rs.OGCWebService;

import java.util.logging.Level;
import com.examind.webdav.WebdavWorker;
import org.constellation.ws.rs.ResponseObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Controller
@RequestMapping("webdav/{serviceId}")
public class AdminWebdavService extends OGCWebService<WebdavWorker> {

    /**
     * Build a new Restful CSW service.
     */
    public AdminWebdavService() {
        super(ServiceDef.Specification.WEBDAV);
        LOGGER.log(Level.INFO, "Webdav (Admin) REST service running");
    }

    @Override
    protected ResponseObject treatIncomingRequest(Object objectRequest, WebdavWorker worker) {
        return new ResponseObject(HttpStatus.NOT_FOUND);
    }

    @Override
    protected ResponseObject processExceptionResponse(CstlServiceException ex, ServiceDef serviceDef, Worker w) {
        return new ResponseObject(HttpStatus.NOT_FOUND);
    }
}
