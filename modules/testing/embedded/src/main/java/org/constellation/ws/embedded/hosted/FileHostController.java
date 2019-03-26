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
package org.constellation.ws.embedded.hosted;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.logging.Level;
import javax.servlet.http.HttpServletResponse;
import org.constellation.api.rest.ErrorMessage;
import org.constellation.configuration.ConfigDirectory;
import org.geotoolkit.nio.IOUtilities;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@RestController
public class FileHostController {

    @RequestMapping(value="/{fileName}",method=GET)
    public void getDataMetadata(final @PathVariable("fileName") String fileName, HttpServletResponse response) throws Exception {

        Path configDirectory = ConfigDirectory.getConfigDirectory();
        Path HostedDirectory = configDirectory.resolve("hosted");
        Path requestFile     = HostedDirectory.resolve(fileName);

        InputStream inputStream = new FileInputStream(requestFile.toFile());
        response.setContentType("application/xml");
        response.setHeader("Content-Disposition", "attachment; filename="+fileName+".xml");
        IOUtilities.copy(inputStream, response.getOutputStream());
        response.flushBuffer();
        inputStream.close();

    }
}
