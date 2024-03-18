/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2021 Geomatys.
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FilenameUtils;
import org.constellation.api.rest.dto.Resource;
import org.constellation.api.rest.dto.Resources;
import org.constellation.business.IConfigurationBusiness;
import org.constellation.business.IStyleBusiness;
import org.constellation.business.IStyleConverterBusiness;
import org.constellation.exception.ConstellationException;
import org.constellation.ws.WebServiceUtilities;
import org.geotoolkit.atom.xml.Link;
import org.geotoolkit.style.MutableStyle;
import org.opengis.style.Style;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Based on https://docs.opengeospatial.org/DRAFTS/20-009.html
 *
 * @author Hilmi BOUALLAGUE (Geomatys)
 * @author Johann Sorel (Geomatys)
 * @author Guilhem Legal (Geomatys)
 */
@RestController
@RequestMapping
public class OGCStyleAPI {
    private static final Logger LOGGER = Logger.getLogger("org.constellation.api.rest");
    private static final String[] CONFORMS = {
            "https://www.opengis.net/spec/ogcapi-styles-1/1.0/conf/core",
            "https://www.opengis.net/spec/ogcapi-styles-1/1.0/conf/json",
            "https://www.opengis.net/spec/ogcapi-styles-1/1.0/conf/manage-styles",
            "https://www.opengis.net/spec/ogcapi-styles-1/1.0/conf/style-validation",
            "https://www.opengis.net/spec/ogcapi-styles-1/1.0/conf/resources",
            "https://www.opengis.net/spec/ogcapi-styles- 1/1.0/conf/manage-resources",
            "https://www.opengis.net/spec/ogcapi-styles-1/1.0/conf/mapbox-styles",
            "https://www.opengis.net/spec/ogcapi-styles-1/1.0/conf/sld-10",
            "https://www.opengis.net/spec/ogcapi-styles-1/1.0/conf/sld-11"
    };

    @Autowired
    private IStyleBusiness styleBusiness;

    @Autowired
    private IStyleConverterBusiness styleConverterBusiness;

    @Autowired
    private IConfigurationBusiness configBusiness;

    /**
     * GET /styles/conformance
     * <p>
     * reference : https://docs.opengeospatial.org/DRAFTS/20-009.html#conformance_declaration
     *
     * @param f, "json" or "html"
     *           The optional f parameter indicates the output format which the server
     *           shall provide as part of the response document. It has preference
     *           over the HTTP Accept header. The default format is JSON
     */
    @RequestMapping(value = "/styles/conformance", method = GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity conformance(@RequestParam(value = "f", required = false, defaultValue = "json") String f) {
        return new ResponseEntity<>(Collections.singletonMap("conformsTo", CONFORMS), HttpStatus.OK);
    }

    /**
     * GET /styles
     * Liste des styles
     * <p>
     * reference : https://docs.opengeospatial.org/DRAFTS/20-009.html#get_styles
     *
     * @param f, "json" or "html"
     *           The optional f parameter indicates the output format which the server
     *           shall provide as part of the response document. It has preference
     *           over the HTTP Accept header. The default format is JSON.
     */
    @RequestMapping(value = "/styles", method = GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity listStyles(@RequestParam(value = "f", required = false, defaultValue = "json") String f,
                                     @RequestParam(value = "mode", required = false, defaultValue = "ref") String mode) {
        try {
            List response;
            if ("brief".equals(mode)) {
                response = styleBusiness.getAvailableStyles("sld",null);
            } else {
                response = styleBusiness.getAllStyleReferences("sld");
            }
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * POST /styles
     * Create or valide a style
     * <p>
     * reference :
     * - https://docs.opengeospatial.org/DRAFTS/20-009.html#create_style
     * - https://docs.opengeospatial.org/DRAFTS/20-009.html#style_validate
     *
     * @param validate, "yes" "no" "only"
     *                  'yes' creates a new style after successful validation and returns 400,
     *                  if validation fails, ’no' creates the style without validation and 'only'
     *                  just validates the style without creating a new style and returns 400,
     *                  if validation fails, otherwise 204.
     * @return
     */
    @RequestMapping(value = "/styles", method = POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity createOrValidateStyle(@RequestParam(value = "validate", required = false, defaultValue = "") String validate,
                                                @RequestParam(value = "name", required = false, defaultValue = "") String name,
                                                @RequestParam(value = "type", required = false, defaultValue = "sld") String type,
                                                @RequestParam("data") MultipartFile file) {
        if (file.isEmpty()) {
            return new ErrorMessage().message("SLD file to import is empty!").build();
        }

        String styleName = name.isEmpty() ? FilenameUtils.removeExtension(file.getOriginalFilename()) : name;

        //copy the file content in memory
        final byte[] buffer;
        try {
           buffer = WebServiceUtilities.getBufferFromFile(file);
        } catch(IOException ex) {
            LOGGER.log(Level.WARNING, "Error while retrieving SLD input", ex);
            return new ErrorMessage(ex).build();
        }

        //try to parse a style from various form and version
        MutableStyle style = (MutableStyle) styleBusiness.parseStyle(styleName, buffer, file.getOriginalFilename());

        if (style == null) {
            final String message = "Failed to import style from file, no UserStyle element defined";
            LOGGER.log(Level.WARNING, message);
            return new ErrorMessage().message(message).build();
        }

        try {
            final boolean exists = styleBusiness.existsStyle(type, style.getName());
            if (!exists) {
                final Integer styleId = styleBusiness.createStyle(type, style);
                Map<String, Object> response = new HashMap<>();
                response.put("styleId", styleId);
                response.put("message", String.format("Style with name %s successfully created", styleName));
                return new ResponseEntity<>(response, HttpStatus.CREATED);
            } else {
                return new ErrorMessage(HttpStatus.BAD_REQUEST).i18N(I18nCodes.Style.ALREADY_EXIST).build();
            }
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * GET /styles/{styleId}
     * Get a style
     * <p>
     * reference : https://docs.opengeospatial.org/DRAFTS/20-009.html#get_style
     *
     * @param styleId
     * @param f,      "mapbox" "sld10" "sld11", file
     *                The content type of the response.
     *                If no value is provided, the standard http rules apply, i.e.,
     *                the accept header will be used to determine the format.
     * @return
     */
    @RequestMapping(value = "/styles/{styleId}", method = GET)
    public ResponseEntity getStyle(@PathVariable(value = "styleId") int styleId, @RequestParam(value = "f", required = false, defaultValue = "json") String f) {
        try {
            final HttpHeaders headers = new HttpHeaders();
            final Object result;
            final HttpStatus status;
            final MediaType mType;
            if (!styleBusiness.existsStyle(styleId)) {
                mType = MediaType.APPLICATION_JSON;
                status = HttpStatus.NOT_FOUND;
                result = new ErrorMessage(status, "Cannot find style with this id : " + styleId);
            } else {
                org.apache.sis.style.Style style = styleBusiness.getStyle(styleId);
                if (style instanceof Style st) {
                    if (f.toLowerCase().contains("json")) {
                        org.constellation.json.binding.Style jstyle = styleConverterBusiness.getJsonStyle(st);
                        jstyle.setId(styleId);
                        mType = MediaType.APPLICATION_JSON;
                        result = jstyle;
                        status = HttpStatus.OK;
                    } else {
                        if (f.toLowerCase().equals("file")) {
                            final String name = st.getName();
                            headers.set("Content-Disposition", "attachment; filename=" + name + ".xml");
                        }
                        mType = MediaType.APPLICATION_XML;
                        result = style;
                        status = HttpStatus.OK;
                    }
                } else {
                    throw new IllegalArgumentException("Style " + style.getClass().getName() + " can not be converted to requested format.");
                }
            }
            headers.setContentType(mType);
            return new ResponseEntity<>(result, headers, status);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * PUT /styles/{styleId}
     * Replace or validate a style
     * <p>
     * reference :
     * - https://docs.opengeospatial.org/DRAFTS/20-009.html#replace_style
     * - https://docs.opengeospatial.org/DRAFTS/20-009.html#style_validate
     *
     * @param styleId
     * @param validate, "yes" "no" "only"
     *                  'yes' creates a new style after successful validation and returns 400,
     *                  if validation fails, ’no' creates the style without validation and 'only'
     *                  just validates the style without creating a new style and returns 400,
     *                  if validation fails, otherwise 204.
     * @return
     */
    @RequestMapping(value = "/styles/{styleId}", method = POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity updateOrValidateStyle(
            @PathVariable(value = "styleId") int styleId,
            @RequestParam(value = "validate", required = false, defaultValue = "") String validate,
            @RequestParam(value = "name", required = false, defaultValue = "") String name,
            @RequestParam(value = "type", required = false, defaultValue = "sld") String type,
            @RequestParam(value = "data", required = false) MultipartFile file) {

        if (file.isEmpty()) {
            return new ResponseEntity<>(new ErrorMessage(HttpStatus.BAD_REQUEST, "SLD file to import is empty!"), HttpStatus.BAD_REQUEST);
        }

        if (!styleBusiness.existsStyle(styleId)) {
            return new ResponseEntity<>(new ErrorMessage(HttpStatus.NOT_FOUND, "Cannot find style with this id : " + styleId), HttpStatus.NOT_FOUND);
        } else {
            //copy the file content in memory
            final byte[] buffer;
            try {
               buffer = WebServiceUtilities.getBufferFromFile(file);
            } catch(IOException ex) {
                return new ResponseEntity<>(new ErrorMessage(HttpStatus.NOT_FOUND, ex.getMessage()), HttpStatus.NOT_FOUND);
            }

            //try to parse a style from various form and version
            org.apache.sis.style.Style style;
            try {
                style = styleBusiness.parseStyle(name, buffer, file.getOriginalFilename());
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
                return new ResponseEntity<>(new ErrorMessage(HttpStatus.BAD_REQUEST, ex.getMessage()), HttpStatus.BAD_REQUEST);
            }

            if (style == null) {
                final String message = "Failed to import style from XML, no UserStyle element defined";
                LOGGER.log(Level.WARNING, message);
                return new ResponseEntity<>(new ErrorMessage(HttpStatus.BAD_REQUEST, message), HttpStatus.BAD_REQUEST);
            }

            try {
                styleBusiness.updateStyle(styleId, (Style) style);
                return new ResponseEntity<>(String.format("Style with id %d is successfully updated", styleId), HttpStatus.OK);
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
                return new ResponseEntity<>(new ErrorMessage(HttpStatus.BAD_REQUEST, ex.getMessage()), HttpStatus.BAD_REQUEST);
            }
        }
    }

    /**
     * DELETE /styles/{styleId}
     * Delete a style
     * <p>
     * reference : https://docs.opengeospatial.org/DRAFTS/20-009.html#delete_style
     *
     * @return
     */
    @RequestMapping(value = "/styles/{styleId}", method = DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity deleteStyle(@PathVariable(value = "styleId") int styleId) {
        try {
            int result = styleBusiness.deleteStyle(styleId);
            if (result == 0) {
                String errMsg = "Cannot find a style with id : " + styleId;
                LOGGER.info(errMsg);
                return new ResponseEntity<>(new ErrorMessage(HttpStatus.NOT_FOUND, errMsg), HttpStatus.NOT_FOUND);
            }
            return new ResponseEntity<>(String.format("Style with id %d has been deleted successfully", styleId), HttpStatus.NO_CONTENT);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * GET /styles/resources
     * Get resources list. (image, glyphes, models, fonts,...)
     * <p>
     * reference : https://opengeospatial.github.io/architecture-dwg/api-styles/index.html#tag/Fetch-resources
     *
     * @return
     */
    @RequestMapping(value = "/styles/resources", method = GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getResources() {
        final Path assetsFolder = configBusiness.getAssetsDirectory();
        try (Stream<Path> ds = Files.list(assetsFolder)) {
            List<Resource> resources = ds
                    .map(Path::getFileName)
                    .map(String::valueOf)
                    .map(new Function<String, Resource>() {
                        @Override
                        public Resource apply(String id) {
                            final Resource resource = new Resource();
                            resource.setId(id);
                            final Link link = new Link();
                            link.setHref(id);
                            link.setType("application/octet-stream");
                            resource.setLink(link);
                            return resource;
                        }
                    })
                    .collect(Collectors.toList());

            final Resources res = new Resources();
            res.setResources(resources);
            return new ResponseEntity<>(res, HttpStatus.OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * GET /styles/resources/{resourceId}
     * Get a resources.
     * <p>
     * reference : https://opengeospatial.github.io/architecture-dwg/api-styles/index.html#operation/getResource
     */
    @RequestMapping(value = "/styles/resources/{resourceId:.+}", method = GET)
    public ResponseEntity getResource(@PathVariable(value = "resourceId") String resourceId) throws ConstellationException {
        try {
            final Optional<Path> resource = findResourceIfExists(resourceId);
            if (resource.isPresent()) {
                final FileSystemResource result = new FileSystemResource(resource.get());
                return ResponseEntity.status(HttpStatus.OK)
                        // Copied from ResourceHttpMessageConverter, because, for a reason I do not understand, the
                        // response content type is not deduced from it. As a workaround, we set it here.
                        // Insight: Spring might prefer a less costly method by default, or use another set of metadata
                        // to determine the media-type by default.
                        .contentType(MediaTypeFactory.getMediaType(result).orElse(MediaType.APPLICATION_OCTET_STREAM))
                        .body(result);
            } else {
                return resourceNotFound(resourceId);
            }
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * PUT /styles/resources/{resourceId}
     * Create a resources.
     * <p>
     * reference : https://opengeospatial.github.io/architecture-dwg/api-styles/index.html#operation/updateResource
     *
     * @return
     */
    @RequestMapping(value = "/styles/resources/{resourceId:.+}", method = POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity createOrUpdateResource(@PathVariable(value = "resourceId") String resourceId, @RequestParam("data") MultipartFile data) {
        if (data.isEmpty()) {
            return new ResponseEntity<>(new ErrorMessage(HttpStatus.BAD_REQUEST, "Cannot find a correct request data param"), HttpStatus.BAD_REQUEST);
        }
        try {
            final Path resourcePath = findResource(resourceId);
            boolean isNew = !Files.exists(resourcePath);
            data.transferTo(resourcePath);
            return new ResponseEntity<>("", isNew ? HttpStatus.CREATED : HttpStatus.NO_CONTENT);
        } catch (IllegalArgumentException ex) {
            return new ResponseEntity<>(new ErrorMessage(HttpStatus.BAD_REQUEST, "Cannot find a correct request data param"), HttpStatus.BAD_REQUEST);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * DELETE /styles/resources/{resourceId}
     * Delete a resources.
     * <p>
     * reference : https://opengeospatial.github.io/architecture-dwg/api-styles/index.html#operation/deleteResource
     *
     * @return
     */
    @RequestMapping(value = "/styles/resources/{resourceId:.+}", method = DELETE)
    public ResponseEntity deleteResource(@PathVariable(value = "resourceId") String resourceId) {
        try {
            final Optional<Path> optFile = findResourceIfExists(resourceId);
            if (!optFile.isPresent()) {
                return resourceNotFound(resourceId);
            }
            Files.delete(optFile.get());
            return ResponseEntity.noContent().build();
        } catch (IOException ex) {
            return new ErrorMessage(ex).build();
        }
    }

    private ResponseEntity resourceNotFound(String resourceId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(new ErrorMessage(HttpStatus.NOT_FOUND, "Cannot find resource with id : " + resourceId), headers, HttpStatus.NOT_FOUND);
    }

    private @NonNull Path findResource(String resourceName) throws IOException {
        if (resourceName == null) throw new IllegalArgumentException("null resource name");
        // A slash can be here depending on how client app or intermediate proxy formatted its http request
        if (resourceName.endsWith("/")) {
            resourceName = resourceName.substring(0, resourceName.length() - 1);
        }
        if (resourceName.isEmpty()) throw new IllegalArgumentException("Empty resource name");

        final Path root = configBusiness.getAssetsDirectory();
        final Path targetFile = root.resolve(resourceName).toAbsolutePath().normalize();

        // Forbidden: user tried to get a file out of asset directory (example: '..', etc.)
        if (!targetFile.startsWith(root) || root.endsWith(targetFile))
            throw new SecurityException("User tried to access a path outside asset directory");

        return targetFile;
    }

    private Optional<Path> findResourceIfExists(String resourceName) throws IOException {
        try {
            final Path resource = findResource(resourceName);
            if (Files.isRegularFile(resource)) {
                return Optional.of(resource);
            }
        } catch (IllegalArgumentException | SecurityException e) {
            LOGGER.log(Level.FINE, "Forbidden file access", e);
        }

        return Optional.empty();
    }
}
