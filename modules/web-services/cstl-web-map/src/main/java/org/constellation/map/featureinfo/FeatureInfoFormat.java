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
package org.constellation.map.featureinfo;

import java.awt.*;
import java.util.List;
import java.util.Optional;
import javax.xml.namespace.QName;
import org.constellation.api.DataType;
import org.constellation.dto.service.config.wxs.GetFeatureInfoCfg;
import org.constellation.ws.LayerCache;
import org.geotoolkit.display.PortrayalException;
import org.geotoolkit.display2d.service.CanvasDef;
import org.geotoolkit.display2d.service.SceneDef;
import org.geotoolkit.ows.xml.GetFeatureInfo;

/**
 * FeatureInfo formatter.
 *
 * @author Quentin Boileau (Geomatys)
 */
public interface FeatureInfoFormat {

    /**
     * Compute and return FeatureInfoFormat object.
     * Parameters sdef, vdef, cdef and searchArea must be used to create a GraphicVisitor which search the intersected
     * Features/Coverage from which FeatureInfo object can be computed.
     *
     * @param sdef {@link SceneDef}
     * @param cdef {@link CanvasDef}
     * @param searchArea {@link Rectangle} searching area
     * @param getFI {@link GetFeatureInfo} source request from a map service like WMS or WMTS.
     * @return an object compatible with requested mimetype. For example an BufferedImage for image/png or String for
     * text/html or text/plain.
     * @throws PortrayalException
     * @see AbstractFeatureInfoFormat#getCandidates(SceneDef, ViewDef, CanvasDef, Rectangle, Integer)
     */
    public Object getFeatureInfo(final SceneDef sdef, final CanvasDef cdef, final Rectangle searchArea,
                                 final GetFeatureInfo getFI) throws PortrayalException;

    /**
     * FeatureInfoFormat supported mimeTypes.
     *
     * @return a list of supported mimeTypes of the current FeatureInfoFormat.
     */
    public List<String> getSupportedMimeTypes();

    /**
     * Set {@link org.constellation.dto.service.config.wxs.GetFeatureInfoCfg} configuration from a service configuration.
     * This object can contain parameters which can be used to render FeatureInfoFormat
     *
     * @param conf {@link GetFeatureInfoCfg}
     */
    public void setConfiguration(GetFeatureInfoCfg conf);

    /**
     * Get {@link org.constellation.dto.service.config.wxs.GetFeatureInfoCfg} set configuration
     * This object can contain parameters which can be used to render FeatureInfoFormat
     *
     * @return {@link GetFeatureInfoCfg} configuration, can be null.
     */
    public GetFeatureInfoCfg getConfiguration();

    /**
     * Set the list of {@link LayerCache} from which the {@link SceneDef}
     * {@link org.apache.sis.portrayal.MapLayers} was build.
     * @param layers
     */
    public void setLayers(List<LayerCache> layers);

    /**
     * Get the list of {@link LayerCache} from which the {@link SceneDef}
     * {@link org.apache.sis.portrayal.MapLayers} was build.
     * @return layers or null
     */
    public List<LayerCache> getLayers();
    
    /**
     * Get a layer with the specified name and type.
     * @param name qualified name of the layer.
     * @param type data type of the layer.
     * 
     * @return Optional of layer or empty
     */
    public Optional<LayerCache> getLayer(QName name, DataType type);
}
