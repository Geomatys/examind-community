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
package org.constellation.provider.mapcontext;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.portrayal.MapItem;
import org.apache.sis.portrayal.MapLayers;
import org.apache.sis.referencing.CRS;
import org.constellation.admin.SpringHelper;
import org.constellation.business.IDataBusiness;
import org.constellation.business.ILayerBusiness;
import org.constellation.business.IStyleBusiness;
import org.constellation.dto.AbstractMCLayerDTO;
import org.constellation.dto.DataMCLayerDTO;
import org.constellation.dto.ExternalServiceMCLayerDTO;
import org.constellation.dto.InternalServiceMCLayerDTO;
import org.constellation.dto.Layer;
import org.constellation.dto.MapContextLayersDTO;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.constellation.provider.Data;
import org.constellation.provider.DataProviders;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.wms.WMSResource;
import org.geotoolkit.wms.WebMapClient;
import org.geotoolkit.wms.xml.WMSVersion;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

/**
 * Utility class that convert Examind MapContext / MapContext layer in SIS MapLayers / MapItem.
 *
 * @author Guilhem Legal (Geomatys)
 */
public class MapContextUtils {
    
    private static final Logger LOGGER = Logger.getLogger("org.constellation.provider.mapcontext");

    public static MapLayers getMapLayers(MapContextLayersDTO mc) throws FactoryException {
        CoordinateReferenceSystem crs = CRS.forCode(mc.getCrs());
        final MapLayers ctx = MapBuilder.createContext(crs);
        GeneralEnvelope envelope = new GeneralEnvelope(crs);
        envelope.setRange(0, mc.getWest(),  mc.getEast());
        envelope.setRange(1, mc.getSouth(), mc.getNorth());
        ctx.setAreaOfInterest(envelope);
        ctx.setIdentifier(mc.getName());

        final IDataBusiness dataBusiness = SpringHelper.getBean(IDataBusiness.class);
        final IStyleBusiness styleBusiness = SpringHelper.getBean(IStyleBusiness.class);
        final ILayerBusiness layerBusiness = SpringHelper.getBean(ILayerBusiness.class);
        final List<MapItem> ctxItems = ctx.getComponents();
        for (final AbstractMCLayerDTO dto : mc.getLayers()) {
            try {
                ctxItems.add(MapContextUtils.load(dto, dataBusiness, styleBusiness, layerBusiness));
            } catch (ConstellationException ex) {
                LOGGER.log(Level.WARNING, "Layer " + dto.getName() + " cannot be loaded",  ex);
            }
        }
        return ctx;
    }

    /**
    * Build a Geotoolkit map item from an Examind Map context layer.
    *
    *
    * @param layer The Map context layer to transform.
    * @return A map item ready-to-be rendered by Geotoolkit.
    * @throws ConfigurationException If the data or style referenced from this
    * layer cannot be found in administration database.
    */
   public static MapItem load(final AbstractMCLayerDTO layer, IDataBusiness dataBusiness, IStyleBusiness styleBusiness, ILayerBusiness layerBusiness) throws ConstellationException {

        if (layer instanceof InternalServiceMCLayerDTO) {
            InternalServiceMCLayerDTO isLayer = (InternalServiceMCLayerDTO) layer;
            final Integer layerId = isLayer.getLayerId();
            final Layer layerConf = layerBusiness.getLayer(layerId, null);

            final org.constellation.dto.Data data = dataBusiness.getData(layerConf.getDataId());
            
            final Integer styleId = isLayer.getStyleId();
            final org.opengis.style.Style layerStyle;
            if (styleId != null) {
                layerStyle = styleBusiness.getStyle(styleId);
            } else {
                layerStyle = null; // Should be replaced by default style on layer build
            }

            final Data realData = DataProviders.getProviderData(data.getProviderId(), data.getNamespace(), data.getName());
            return realData.getMapLayer(layerStyle, null);

        } else if (layer instanceof DataMCLayerDTO) {
            DataMCLayerDTO dtLayer = (DataMCLayerDTO) layer;
            final Integer dataId = dtLayer.getDataId();
            final org.constellation.dto.Data data;
            try {
                data = dataBusiness.getData(dataId);
            } catch (ConstellationException ex) {
                throw new IllegalStateException("Data referenced by map context layer " + layer.getName() + " cannot be find.");
            }

            final Integer styleId = dtLayer.getStyleId();
            final org.opengis.style.Style layerStyle;
            if (styleId != null) {
                layerStyle = styleBusiness.getStyle(styleId);
            } else {
                layerStyle = null; // Should be replaced by default style on layer build
            }

            final Data realData = DataProviders.getProviderData(data.getProviderId(), data.getNamespace(), data.getName());
            return realData.getMapLayer(layerStyle, null);

        } else if (layer instanceof ExternalServiceMCLayerDTO) {
             ExternalServiceMCLayerDTO extLayer = (ExternalServiceMCLayerDTO) layer;
             String serviceUrl = extLayer.getExternalServiceUrl();
            if (serviceUrl != null) {
                try {
                    // TODO : Could it be something else than a WMS ?
                    URL verifiedUrl = new URL(serviceUrl);
                    final WMSVersion wmsVersion;
                    if (extLayer.getExternalServiceVersion() == null) {
                        wmsVersion = WMSVersion.auto;
                    } else {
                        wmsVersion = WMSVersion.getVersion(extLayer.getExternalServiceVersion());
                    }

                    WebMapClient wmsClient = new WebMapClient(verifiedUrl, wmsVersion);
                    final WMSResource wmsLayer = new WMSResource(wmsClient, extLayer.getExternalLayer().getLocalPart());
                    final String wmsStyle = extLayer.getExternalStyle();
                    if (wmsStyle != null) {
                        wmsLayer.setStyles(wmsStyle);
                    }

                    return MapBuilder.createLayer(wmsLayer);
                } catch (MalformedURLException ex) {
                    throw new ConstellationException(ex);
                }
            }
        }
        throw new ConstellationException("Not enough information in map context layer named " + layer.getName() + ". We cannot load back related data.");
    }
}
