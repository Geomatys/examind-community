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
package org.constellation.api.rest;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.apache.sis.metadata.iso.DefaultMetadata;
import org.constellation.business.IMetadataBusiness;
import org.constellation.dto.metadata.MetadataBrief;
import org.constellation.dto.metadata.MetadataLists;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.constellation.json.metadata.Template;
import org.constellation.json.metadata.bean.TemplateResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@RestController
public class InternalMetadataRestAPI extends AbstractRestAPI {

    @Autowired
    private IMetadataBusiness metadataBusiness;

    @Autowired
    private TemplateResolver templateResolver;

    /**
     * Give metadata CodeLists (example {@link org.opengis.metadata.citation.Role} codes.
     * Used in metadata editor to fill the codelist como box.
     *
     * @return a List of codelist values
     */
    @RequestMapping(value="/internal/metadata/codeLists",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getMetadataCodeLists() {
        try {
            final MetadataLists mdList = metadataBusiness.getMetadataCodeLists();
            return new ResponseEntity(mdList, OK);
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * This method is used to upload image for metadata graphic overview field
     * Returns a JSON response with attachment ID.
     *
     * @param file
     * @return
     */
    @RequestMapping(value="/internal/metadata/image/upload",method=POST,consumes=MULTIPART_FORM_DATA_VALUE,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity uploadImage(@RequestParam("graphicOverviewFileInput") MultipartFile file) {
        if (readOnlyAPI) return readOnlyModeActivated();
        final int attId;
        final Map<String,Object> map = new HashMap<>();
        try (InputStream in = file.getInputStream()) {
            attId = metadataBusiness.createMetadataAttachment(in, file.getOriginalFilename());
            map.put("attachmentId",""+attId);
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING,ex.getLocalizedMessage(),ex);
            return new ErrorMessage(ex).build();
        }
        return new ResponseEntity(map, OK);
    }

    /**
     * Returns the json representation of metadata by using template for given metadata fileIdentifier .
     *
     * @param fileIdentifier given metadata ID
     * @return ResponseEntity that contains the metadata in json format.
     */
    @RequestMapping(value="/internal/metadata/{fileIdentifier}/json",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity resolveIsoMetadataJson(
            @PathVariable("fileIdentifier") final String fileIdentifier) {

        final StringWriter buffer = new StringWriter();
        try{
            //Resolve metadata by fileIdentifier
            final MetadataBrief md = metadataBusiness.searchFullMetadata(fileIdentifier, false, false, null);
            if(md != null) {
                final Object metadata = metadataBusiness.getMetadata(md.getId());
                if (metadata != null) {
                    if (metadata instanceof DefaultMetadata){
                        ((DefaultMetadata)metadata).prune();
                    }
                    //get template name
                    final String templateName = md.getType();
                    final Template template = templateResolver.getByName(templateName);
                    template.write(metadata,buffer,true, false);
                }
            }
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, "error while writing metadata json.", ex);
            return new ErrorMessage(ex).build();
        }
        return new ResponseEntity(buffer.toString(), OK);
    }
}
