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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
import javax.xml.bind.JAXBException;
import org.apache.commons.io.FilenameUtils;
import org.apache.sis.internal.system.DefaultFactories;
import org.constellation.api.rest.dto.Resource;
import org.constellation.api.rest.dto.Resources;
import org.constellation.business.IConfigurationBusiness;
import org.constellation.business.IStyleBusiness;
import org.constellation.business.IStyleConverterBusiness;
import org.constellation.dto.process.StyleProcessReference;
import org.constellation.exception.ConstellationException;
import org.constellation.ws.WebServiceUtilities;
import org.geotoolkit.atom.xml.Link;
import org.geotoolkit.sld.MutableLayer;
import org.geotoolkit.sld.MutableStyledLayerDescriptor;
import org.geotoolkit.sld.xml.Specification;
import org.geotoolkit.sld.xml.StyleXmlIO;
import org.geotoolkit.style.MutableStyle;
import org.geotoolkit.style.MutableStyleFactory;
import org.geotoolkit.style.io.PaletteReader;
import org.opengis.sld.LayerStyle;
import org.opengis.sld.NamedLayer;
import org.opengis.sld.UserLayer;
import org.opengis.style.ColorMap;
import org.opengis.style.RasterSymbolizer;
import org.opengis.style.Style;
import org.opengis.style.StyleFactory;
import org.opengis.util.FactoryException;
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
 */
@RestController
@RequestMapping("mcm")
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
    public ResponseEntity listStyles(@RequestParam(value = "f", required = false, defaultValue = "json") String f) {
        final List<StyleProcessReference> allStyles = styleBusiness.getAllStyleReferences();
        return new ResponseEntity<>(allStyles, HttpStatus.OK);
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
            return new ResponseEntity<>(new ErrorMessage(HttpStatus.BAD_REQUEST, "SLD file to import is empty!"), HttpStatus.BAD_REQUEST);
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

        //try to parse SLD from various form and version
        final List<MutableStyle> styles = new ArrayList<>();
        final StyleXmlIO io = new StyleXmlIO();
        MutableStyle style = null;

        //try to parse an SLD input
        MutableStyledLayerDescriptor sld = null;
        try {
            sld = io.readSLD(new ByteArrayInputStream(buffer), Specification.StyledLayerDescriptor.V_1_1_0);
        } catch (JAXBException | FactoryException ex) {
            LOGGER.log(Level.FINEST, ex.getMessage(), ex);
        }
        if (sld == null) {
            try {
                sld = io.readSLD(new ByteArrayInputStream(buffer), Specification.StyledLayerDescriptor.V_1_0_0);
            } catch (JAXBException | FactoryException ex) {
                LOGGER.log(Level.FINEST, ex.getMessage(), ex);
            }
        }

        if (sld != null) {
            for (MutableLayer sldLayer : sld.layers()) {
                if (sldLayer instanceof NamedLayer nl) {
                    for (LayerStyle ls : nl.styles()) {
                        if (ls instanceof MutableStyle ms) {
                            styles.add(ms);
                        }
                    }
                } else if (sldLayer instanceof UserLayer ul) {
                    for (org.opengis.style.Style ls : ul.styles()) {
                        if (ls instanceof MutableStyle ms) {
                            styles.add(ms);
                        }
                    }
                }
            }
            if (!styles.isEmpty()) {
                style = styles.remove(0);
            }
        } else {
            //try to parse a UserStyle input
            try {
                style = io.readStyle(new ByteArrayInputStream(buffer), Specification.SymbologyEncoding.V_1_1_0);
            } catch (JAXBException | FactoryException ex) {
                LOGGER.log(Level.FINEST, ex.getMessage(), ex);
            }
            if (style == null) {
                try {
                    style = io.readStyle(new ByteArrayInputStream(buffer), Specification.SymbologyEncoding.SLD_1_0_0);
                } catch (JAXBException | FactoryException ex) {
                    LOGGER.log(Level.FINEST, ex.getMessage(), ex);
                }
            }
            if (style == null) {
                //test cpt,clr,pal type
                String originalFilename = file.getOriginalFilename();
                if (originalFilename != null) {
                    originalFilename = originalFilename.toLowerCase();
                    ColorMap colormap = null;
                    try {
                        if (originalFilename.endsWith("cpt")) {
                            PaletteReader reader = new PaletteReader(PaletteReader.PATTERN_CPT);
                            colormap = reader.read(new String(buffer));
                        } else if (originalFilename.endsWith("clr")) {
                            PaletteReader reader = new PaletteReader(PaletteReader.PATTERN_CLR);
                            colormap = reader.read(new String(buffer));
                        } else if (originalFilename.endsWith("pal")) {
                            PaletteReader reader = new PaletteReader(PaletteReader.PATTERN_PAL);
                            colormap = reader.read(new String(buffer));
                        }
                    } catch (IOException ex) {
                        LOGGER.log(Level.FINEST, ex.getMessage(), ex);
                    }
                    if (colormap != null) {
                        final MutableStyleFactory SF = (MutableStyleFactory) DefaultFactories.forBuildin(StyleFactory.class);
                        RasterSymbolizer symbol = SF.rasterSymbolizer(null, null, null, null, colormap, null, null, null);
                        style = SF.style(symbol);
                    }
                }
            }
        }

        if (style == null) {
            final String message = "Failed to import style from file, no UserStyle element defined";
            LOGGER.log(Level.WARNING, message);
            return new ErrorMessage().message(message).build();
        }

        //log styles which have been ignored
        if (!styles.isEmpty()) {
            final StringBuilder sb = new StringBuilder("Ignored styles at import :");
            for (MutableStyle ms : styles) {
                sb.append(' ').append(ms.getName());
            }
            LOGGER.log(Level.FINEST, sb.toString());
        }

        //store imported style
        if (styleName != null && !styleName.isEmpty()) {
            style.setName(styleName);
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
                return new ResponseEntity<>(new ErrorMessage(HttpStatus.BAD_REQUEST, "Style with the same name and type exists"), HttpStatus.BAD_REQUEST);
            }
        } catch (Throwable ex) {
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
     * @param f,      "mapbox" "sld10" "sld11"
     *                The content type of the response.
     *                If no value is provided, the standard http rules apply, i.e.,
     *                the accept header will be used to determine the format.
     * @return
     */
    @RequestMapping(value = "/styles/{styleId}", method = GET)
    public ResponseEntity getStyle(@PathVariable(value = "styleId") int styleId, @RequestParam(value = "f", required = false, defaultValue = "json") String f) {
        try {
            final Object result;
            final HttpStatus status;
            final MediaType mType;
            if (!styleBusiness.existsStyle(styleId)) {
                mType = MediaType.APPLICATION_JSON;
                status = HttpStatus.NOT_FOUND;
                result = new ErrorMessage(status, "Cannot find style with this id : " + styleId);
            } else {
                final boolean asJson = f.toLowerCase().contains("json");
                Style style = styleBusiness.getStyle(styleId);
                if (asJson) {
                    org.constellation.json.binding.Style jstyle = styleConverterBusiness.getJsonStyle(style);
                    jstyle.setId(styleId);
                    mType = MediaType.APPLICATION_JSON;
                    result = jstyle;
                    status = HttpStatus.OK;
                } else {
                    mType = MediaType.APPLICATION_XML;
                    result = style;
                    status = HttpStatus.OK;
                }
            }
            final HttpHeaders headers = new HttpHeaders();
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

            //try to parse SLD from various form and version
            final List<MutableStyle> styles = new ArrayList<>();
            final StyleXmlIO io = new StyleXmlIO();
            MutableStyle style = null;

            //try to parse an SLD input
            MutableStyledLayerDescriptor sld = null;
            try {
                sld = io.readSLD(new ByteArrayInputStream(buffer), Specification.StyledLayerDescriptor.V_1_1_0);
            } catch (JAXBException | FactoryException ex) {
                LOGGER.log(Level.FINEST, ex.getMessage(), ex);
            }
            if (sld == null) {
                try {
                    sld = io.readSLD(new ByteArrayInputStream(buffer), Specification.StyledLayerDescriptor.V_1_0_0);
                } catch (JAXBException | FactoryException ex) {
                    LOGGER.log(Level.FINEST, ex.getMessage(), ex);
                }
            }

            if (sld != null) {
                for (MutableLayer sldLayer : sld.layers()) {
                    if (sldLayer instanceof NamedLayer nl) {
                        for (LayerStyle ls : nl.styles()) {
                            if (ls instanceof MutableStyle ms) {
                                styles.add(ms);
                            }
                        }
                    } else if (sldLayer instanceof UserLayer ul) {
                        for (org.opengis.style.Style ls : ul.styles()) {
                            if (ls instanceof MutableStyle ms) {
                                styles.add(ms);
                            }
                        }
                    }
                }
                if (!styles.isEmpty()) {
                    style = styles.remove(0);
                }
            } else {
                //try to parse a UserStyle input
                try {
                    style = io.readStyle(new ByteArrayInputStream(buffer), Specification.SymbologyEncoding.V_1_1_0);
                } catch (JAXBException | FactoryException ex) {
                    LOGGER.log(Level.FINEST, ex.getMessage(), ex);
                }
                if (style == null) {
                    try {
                        style = io.readStyle(new ByteArrayInputStream(buffer), Specification.SymbologyEncoding.SLD_1_0_0);
                    } catch (JAXBException | FactoryException ex) {
                        LOGGER.log(Level.FINEST, ex.getMessage(), ex);
                    }
                }
                if (style == null) {
                    //test cpt,clr,pal type
                    String originalFilename = file.getOriginalFilename();
                    if (originalFilename != null) {
                        originalFilename = originalFilename.toLowerCase();
                        ColorMap colormap = null;
                        try {
                            if (originalFilename.endsWith("cpt")) {
                                PaletteReader reader = new PaletteReader(PaletteReader.PATTERN_CPT);
                                colormap = reader.read(new String(buffer));
                            } else if (originalFilename.endsWith("clr")) {
                                PaletteReader reader = new PaletteReader(PaletteReader.PATTERN_CLR);
                                colormap = reader.read(new String(buffer));
                            } else if (originalFilename.endsWith("pal")) {
                                PaletteReader reader = new PaletteReader(PaletteReader.PATTERN_PAL);
                                colormap = reader.read(new String(buffer));
                            }
                        } catch (IOException ex) {
                            LOGGER.log(Level.FINEST, ex.getMessage(), ex);
                        }
                        if (colormap != null) {
                            final MutableStyleFactory SF = (MutableStyleFactory) DefaultFactories.forBuildin(StyleFactory.class);
                            RasterSymbolizer symbol = SF.rasterSymbolizer(null, null, null, null, colormap, null, null, null);
                            style = SF.style(symbol);
                        }
                    }
                }
            }

            if (style == null) {
                final String message = "Failed to import style from XML, no UserStyle element defined";
                LOGGER.log(Level.WARNING, message);
                return new ResponseEntity<>(new ErrorMessage(HttpStatus.BAD_REQUEST, message), HttpStatus.BAD_REQUEST);
            }

            //log styles which have been ignored
            if (!styles.isEmpty()) {
                final StringBuilder sb = new StringBuilder("Ignored styles at import :");
                for (MutableStyle ms : styles) {
                    sb.append(' ').append(ms.getName());
                }
                LOGGER.log(Level.FINEST, sb.toString());
            }
            try {
                if (!name.isEmpty()) {
                    style.setName(name);
                }
                styleBusiness.updateStyle(styleId, style);
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
            if (styleBusiness.existsStyle(styleId)) {
                styleBusiness.deleteStyle(styleId);
                return new ResponseEntity<>(String.format("Style with id %d has been deleted successfully", styleId), HttpStatus.NO_CONTENT);
            } else {
                String errMsg = "Cannot find a style with id : " + styleId;
                LOGGER.info(errMsg);
                return new ResponseEntity<>(new ErrorMessage(HttpStatus.NOT_FOUND, errMsg), HttpStatus.NOT_FOUND);
            }
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
