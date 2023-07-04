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
package org.constellation.api.rest;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.apache.sis.geometry.Envelopes;
import org.apache.sis.referencing.CommonCRS;
import org.constellation.business.IMapContextBusiness;
import org.constellation.dto.AbstractMCLayerDTO;
import org.constellation.dto.BoundingBox;
import org.constellation.dto.CoordinateReferenceSystem;
import org.constellation.dto.ParameterValues;
import org.constellation.dto.wms.WMSLayer;
import org.constellation.dto.wms.StyleDTO;
import org.constellation.exception.ConstellationException;
import org.geotoolkit.wms.WebMapClient;
import org.geotoolkit.wms.xml.AbstractLayer;
import org.geotoolkit.wms.xml.AbstractWMSCapabilities;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Identifier;
import static org.springframework.http.HttpStatus.OK;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
@RestController
public class InternalMapContextRestAPI extends AbstractRestAPI {

    @Autowired
    private IMapContextBusiness contextBusiness;

    @RequestMapping(value="/internal/mapcontexts/extent/layers",method=POST,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getContextExtents(@RequestBody final List<AbstractMCLayerDTO> layers) {

        final ParameterValues values;
        try {
            values = contextBusiness.getExtentForLayers(layers);
        } catch (ConstellationException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            return new ErrorMessage(ex).message("Failed to extract envelope for these layers.").build();
        }
        if (values == null) {
            return new ErrorMessage().message("Cannot calculate envelope for layers, maybe the layers array sent is empty!").build();
        }
        return new ResponseEntity(values, OK);
    }

    @RequestMapping(value="/internal/mapcontexts/external/capabilities/layers/{version}",method=POST,consumes=MediaType.TEXT_PLAIN_VALUE,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getLayersForWms(
            @PathVariable("version") final String version,
            @RequestBody final String url) {

        try (final WebMapClient client = (version != null && !version.isEmpty()) ?
                    new WebMapClient(new URL(url), version) : new WebMapClient(new URL(url))) {

            AbstractWMSCapabilities capa = client.getServiceCapabilities();

            final List<AbstractLayer> layers = capa.getLayers();
            final List<WMSLayer> finalList = new ArrayList<>();
            for (final AbstractLayer layer : layers) {
                // Remove layer groups, if any
                if (layer.getLayer() != null && layer.getLayer().size() > 0) {
                    continue;
                }
                final List<StyleDTO> styles = new ArrayList<>();
                if(layer.getStyle() != null) {
                    for(final org.geotoolkit.wms.xml.Style s : layer.getStyle()) {
                        styles.add(new StyleDTO(s.getName(),s.getTitle()));
                    }
                }
                final BoundingBox bbox = new BoundingBox();
                Envelope env =  layer.getEnvelope();
                if(env != null) {

                    //force envelope order to longitude first
                    env = Envelopes.transform(env, CommonCRS.WGS84.normalizedGeographic());

                    final DirectPosition lower = env.getLowerCorner();
                    final DirectPosition upper = env.getUpperCorner();
                    if(lower != null && upper != null) {
                        final double[] lowerCoords = lower.getCoordinate();
                        final double[] upperCoords = upper.getCoordinate();
                        bbox.setMinx(lowerCoords[0]);
                        bbox.setMiny(lowerCoords[1]);
                        bbox.setMaxx(upperCoords[0]);
                        bbox.setMaxy(upperCoords[1]);
                        final org.opengis.referencing.crs.CoordinateReferenceSystem crs = env.getCoordinateReferenceSystem();
                        if(crs != null) {
                            final Identifier name = crs.getName();
                            if(name != null) {
                                final String desc = name.getDescription() != null ? name.getDescription().toString() : "";
                                bbox.setCrs(new CoordinateReferenceSystem(name.getCode(),desc));
                            }
                        }
                    }
                }
                finalList.add(new WMSLayer(layer.getName(),layer.getTitle(), layer.getAbstract(),styles,bbox,version));
            }

            return new ResponseEntity(finalList, OK);
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }
}