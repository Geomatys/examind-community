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
package org.constellation.admin;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.xml.namespace.QName;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.util.logging.Logging;
import org.constellation.business.IDataBusiness;
import org.constellation.business.IDatasetBusiness;
import org.constellation.business.IMapContextBusiness;
import org.constellation.dto.CstlUser;
import org.constellation.dto.Data;
import org.constellation.dto.DataBrief;
import org.constellation.dto.Layer;
import org.constellation.dto.MapContextLayersDTO;
import org.constellation.dto.MapContextStyledLayerDTO;
import org.constellation.dto.ParameterValues;
import org.constellation.dto.Style;
import org.constellation.dto.service.Service;
import org.constellation.repository.DataRepository;
import org.constellation.repository.LayerRepository;
import org.constellation.repository.MapContextRepository;
import org.constellation.repository.ProviderRepository;
import org.constellation.repository.ServiceRepository;
import org.constellation.repository.StyleRepository;
import org.constellation.repository.StyledLayerRepository;
import org.constellation.util.DataReference;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.identification.DataIdentification;
import org.opengis.metadata.identification.Identification;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.constellation.exception.ConstellationException;
import org.constellation.business.IMetadataBusiness;
import org.constellation.business.IProcessBusiness;
import org.constellation.business.IUserBusiness;
import org.constellation.dto.MapContextDTO;
import org.constellation.dto.ProviderBrief;
import org.constellation.dto.StyleBrief;
import org.constellation.exception.ConfigurationException;
import org.constellation.util.Util;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

@Component("cstlMapContextBusiness")
@Primary
public class MapContextBusiness implements IMapContextBusiness {

    protected static final Logger LOGGER = Logging.getLogger("org.constellation.admin");

    @Inject
    private MapContextRepository mapContextRepository;

    @Inject
    private LayerRepository layerRepository;

    @Inject
    private DataRepository dataRepository;

    @Inject
    private IMetadataBusiness metadataBusiness;

    @Inject
    private IDataBusiness dataBusiness;

    @Inject
    private IDatasetBusiness datasetBusiness;

    @Inject
    private ProviderRepository providerRepository;

    @Inject
    private StyleRepository styleRepository;

    @Inject
    private ServiceRepository serviceRepository;

    @Inject
    private StyledLayerRepository styledLayerRepository;

    @Inject
    private IUserBusiness userBusiness;

    @Inject
    private IProcessBusiness processBusiness;

    @Inject
    private PlatformTransactionManager platformTransactionManager;

    @Override
    @Transactional
    public void setMapItems(final int contextId, final List<MapContextStyledLayerDTO> layers) {
        mapContextRepository.setLinkedLayers(contextId, layers);
    }

    @Override
    @Transactional
    public Integer create(final MapContextLayersDTO mapContext) throws ConstellationException {
        return mapContextRepository.create(mapContext);
    }

    @Override
    public List<MapContextLayersDTO> findAllMapContextLayers() throws ConstellationException {
        final List<MapContextLayersDTO> ctxtLayers = new ArrayList<>();
        final List<MapContextDTO> ctxts = mapContextRepository.findAll();
        for (final MapContextDTO ctxt : ctxts) {
            final List<MapContextStyledLayerDTO> styledLayers = mapContextRepository.getLinkedLayers(ctxt.getId());

            final List<MapContextStyledLayerDTO> styledLayersDto = generateLayerDto(styledLayers);
            final MapContextLayersDTO mapcontext = buildMapContextLayers(ctxt, styledLayersDto);

            //getOwner and set userName to pojo.
            final Optional<CstlUser> user = userBusiness.findById(ctxt.getOwner());
            if (user != null && user.isPresent()) {
                final CstlUser cstlUser = user.get();
                if(cstlUser!=null){
                    mapcontext.setUserOwner(cstlUser.getLogin());
                }
            }
            ctxtLayers.add(mapcontext);
        }
        return ctxtLayers;
    }

    private MapContextLayersDTO convertToMapContextLayer(final MapContextDTO ctxt) throws ConstellationException {
        final List<MapContextStyledLayerDTO> styledLayers = mapContextRepository.getLinkedLayers(ctxt.getId());
        final List<MapContextStyledLayerDTO> styledLayersDto = generateLayerDto(styledLayers);
        final MapContextLayersDTO mapcontext = buildMapContextLayers(ctxt, styledLayersDto);

        //getOwner and set userName to pojo.
        final Optional<CstlUser> user = userBusiness.findById(ctxt.getOwner());
        if (user != null && user.isPresent()) {
            final CstlUser cstlUser = user.get();
            if (cstlUser != null) {
                mapcontext.setUserOwner(cstlUser.getLogin());
            }
        }
        return mapcontext;
    }

    @Override
    public MapContextLayersDTO findMapContextLayers(int contextId) throws ConstellationException {
        final MapContextDTO ctxt = mapContextRepository.findById(contextId);
        final List<MapContextStyledLayerDTO> styledLayers = mapContextRepository.getLinkedLayers(contextId);
        final List<MapContextStyledLayerDTO> styledLayersDto = generateLayerDto(styledLayers);
        return buildMapContextLayers(ctxt, styledLayersDto);
    }

    @Override
    public String findStyleName(Integer styleId) {
        return styleRepository.findById(styleId).getName();
    }

    /**
     * Get the extent of all included layers in this map context.
     *
     * @param contextId Context identifier
     * @return
     * @throws FactoryException
     */
    @Override
    public ParameterValues getExtent(int contextId) throws FactoryException,ConstellationException {
        final ParameterValues values = new ParameterValues();
        final MapContextDTO context = mapContextRepository.findById(contextId);
        GeneralEnvelope env = null;
        if (context.getWest() != null && context.getSouth() != null && context.getEast() != null && context.getNorth() != null && context.getCrs() != null) {
            final CoordinateReferenceSystem crs = AbstractCRS.castOrCopy(CRS.forCode(context.getCrs())).forConvention(AxesConvention.RIGHT_HANDED);
            env = new GeneralEnvelope(crs);
            env.setRange(0, context.getWest(), context.getEast());
            env.setRange(1, context.getSouth(), context.getNorth());
        }

        final List<MapContextStyledLayerDTO> styledLayers = generateLayerDto(mapContextRepository.getLinkedLayers(contextId));
        env = getEnvelopeForLayers(styledLayers, env);

        if (env == null) {
            return null;
        }

        final HashMap<String,String> vals = new HashMap<>();
        vals.put("crs", (context.getCrs() != null && !context.getCrs().isEmpty()) ? context.getCrs() : "CRS:84");
        vals.put("west", String.valueOf(env.getLower(0)));
        vals.put("east", String.valueOf(env.getUpper(0)));
        vals.put("south", String.valueOf(env.getLower(1)));
        vals.put("north", String.valueOf(env.getUpper(1)));
        values.setValues(vals);
        return values;
    }

    /**
     * Get the extent for the given layers.
     *
     * @param styledLayers Layers to consider.
     * @return
     * @throws FactoryException
     */
    @Override
    public ParameterValues getExtentForLayers(final List<MapContextStyledLayerDTO> styledLayers) throws FactoryException,ConstellationException {
        final GeneralEnvelope env = getEnvelopeForLayers(styledLayers, null);

        if (env == null) {
            return null;
        }

        final ParameterValues values = new ParameterValues();
        final HashMap<String,String> vals = new HashMap<>();
        vals.put("crs", "CRS:84");
        vals.put("west", String.valueOf(env.getLower(0)));
        vals.put("east", String.valueOf(env.getUpper(0)));
        vals.put("south", String.valueOf(env.getLower(1)));
        vals.put("north", String.valueOf(env.getUpper(1)));
        values.setValues(vals);
        return values;
    }

    private GeneralEnvelope getEnvelopeForLayers(final List<MapContextStyledLayerDTO> styledLayers,
                                                 final GeneralEnvelope ctxtEnv) throws FactoryException,ConstellationException {
        GeneralEnvelope env = ctxtEnv;
        for (final MapContextStyledLayerDTO styledLayer : styledLayers) {
            if (!styledLayer.isVisible()) {
                continue;
            }
            Integer layerID = styledLayer.getLayerId();
            Integer dataID = styledLayer.getDataId();
            if (layerID != null || dataID != null) {
                if(dataID == null) {
                    final Layer layerRecord = layerRepository.findById(layerID);
                    dataID = layerRecord.getDataId();
                }
                DefaultMetadata metadata = null;
                if(dataID != null) {
                    try {
                        metadata = (DefaultMetadata) metadataBusiness.getIsoMetadataForData(dataID);
                    } catch (Exception ex) {
                        LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
                    }
                }
                if(metadata == null && dataID != null) {
                    //try to get dataset metadata.
                    final Data data = dataRepository.findById(dataID);
                    if (data.getDatasetId() != null) {
                        try{
                            metadata = (DefaultMetadata) datasetBusiness.getMetadata(data.getDatasetId());
                        }catch(Exception ex){
                            //skip for this layer
                            continue;
                        }
                    }
                }
                if (metadata == null || metadata.getIdentificationInfo() == null || metadata.getIdentificationInfo().isEmpty()) {
                    continue;
                }
                final Identification identification = metadata.getIdentificationInfo().iterator().next();
                if (!(identification instanceof DataIdentification)) {
                    continue;
                }
                final Collection<? extends Extent> extents = ((DataIdentification) identification).getExtents();
                if (extents == null || extents.isEmpty()) {
                    continue;
                }
                final DefaultExtent extent = (DefaultExtent) extents.iterator().next();
                if (extent.getGeographicElements() == null || extent.getGeographicElements().isEmpty()) {
                    continue;
                }
                final GeographicBoundingBox geoBBox = (GeographicBoundingBox) extent.getGeographicElements().iterator().next();

                final GeneralEnvelope tempEnv = new GeneralEnvelope(CommonCRS.defaultGeographic());
                tempEnv.setRange(0, geoBBox.getWestBoundLongitude(), geoBBox.getEastBoundLongitude());
                tempEnv.setRange(1, geoBBox.getSouthBoundLatitude(), geoBBox.getNorthBoundLatitude());
                if (env == null) {
                    env = tempEnv;
                } else {
                    env.add(tempEnv);
                }
            } else {
                final String extLayerExtent = styledLayer.getExternalLayerExtent();
                if (extLayerExtent != null && !extLayerExtent.isEmpty()) {
                    final String[] layExtent = extLayerExtent.split(",");
                    final GeneralEnvelope tempEnv = new GeneralEnvelope(CommonCRS.defaultGeographic());
                    tempEnv.setRange(0, Double.parseDouble(layExtent[0]), Double.parseDouble(layExtent[2]));
                    tempEnv.setRange(1, Double.parseDouble(layExtent[1]), Double.parseDouble(layExtent[3]));
                    if (env == null) {
                        env = tempEnv;
                    } else {
                        env.add(tempEnv);
                    }
                }
            }
        }
        return env;
    }

    @Override
    public List<MapContextDTO> getAllContexts() {
        return mapContextRepository.findAll();
    }

    @Override
    @Transactional
    public void updateContext(MapContextLayersDTO mapContext) {
        mapContextRepository.update(mapContext);
    }

    @Override
    @Transactional
    public void delete(int contextId) throws ConfigurationException {
        metadataBusiness.deleteMapContextMetadata(contextId);
        mapContextRepository.delete(contextId);
    }

    @Override
    @Transactional
    public void deleteAllContexts() throws ConfigurationException {
        List<Integer> ids = mapContextRepository.findAllId();
        for (Integer id : ids) {
            delete(id);
        }
    }

    @Override
    public MapContextDTO getContextById(int id) {
        return mapContextRepository.findById(id);
    }

    @Override
    @Transactional
    public void updateOwner(Integer contextId, int newOwner) {
        mapContextRepository.updateOwner(contextId, newOwner);
    }

    @Override
    public Map.Entry<Integer, List<MapContextDTO>> filterAndGetBrief(final Map<String,Object> filterMap, final Map.Entry<String,String> sortEntry,final int pageNumber,final int rowsPerPage) {
        return mapContextRepository.filterAndGet(filterMap, sortEntry, pageNumber, rowsPerPage);
    }

    @Override
    public Map.Entry<Integer, List<MapContextLayersDTO>> filterAndGetMapContextLayers(final Map<String,Object> filterMap, final Map.Entry<String,String> sortEntry,final int pageNumber,final int rowsPerPage) throws ConstellationException {
        Map.Entry<Integer, List<MapContextDTO>> entry = mapContextRepository.filterAndGet(filterMap, sortEntry, pageNumber, rowsPerPage);
        List<MapContextLayersDTO> results = new ArrayList<>();
        final List<MapContextDTO> contextList = entry.getValue();
        if (contextList != null) {
            for (final MapContextDTO mp : contextList) {
                results.add(convertToMapContextLayer(mp));
            }
        }
        return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), results);
    }


    private static MapContextLayersDTO buildMapContextLayers(MapContextDTO mctx, List<MapContextStyledLayerDTO> layers) {
        return new MapContextLayersDTO(mctx.getId(),
                mctx.getName(),
                mctx.getOwner(),
                mctx.getDescription(),
                mctx.getCrs(),
                mctx.getWest(),
                mctx.getNorth(),
                mctx.getEast(),
                mctx.getSouth(),
                mctx.getKeywords(),
                layers);
    }

    private List<MapContextStyledLayerDTO> generateLayerDto(final List<MapContextStyledLayerDTO> styledLayers) throws ConstellationException{
        final List<MapContextStyledLayerDTO> styledLayersDto = new ArrayList<>();
        for (final MapContextStyledLayerDTO styledLayer : styledLayers) {
            final MapContextStyledLayerDTO dto;
            final Integer layerID = styledLayer.getLayerId();
            final Integer dataID = styledLayer.getDataId();
            if (layerID != null) {
                final Layer layer = layerRepository.findById(layerID);
                final Data data = dataRepository.findById(layer.getDataId());
                final ProviderBrief provider = providerRepository.findOne(data.getProviderId());
                final QName name = new QName(layer.getNamespace(), layer.getName());

                final org.constellation.dto.service.config.wxs.Layer layerConfig = new org.constellation.dto.service.config.wxs.Layer(layer.getId(), name);
                layerConfig.setAlias(layer.getAlias());
                layerConfig.setDate(layer.getDate());
                layerConfig.setOwner(layer.getOwnerId());
                layerConfig.setProviderID(provider.getIdentifier());
                layerConfig.setProviderType(provider.getType());
                layerConfig.setDataId(layer.getDataId());

                final List<Integer> styledLays = styledLayerRepository.findByLayer(layer.getId());
                final List<DataReference> drs = new ArrayList<>();
                for (final Integer styledLay : styledLays) {
                    final Style s = styleRepository.findById(styledLay);
                    if (s == null) {
                        continue;
                    }
                    final DataReference dr = DataReference.createProviderDataReference(DataReference.PROVIDER_STYLE_TYPE, "sld", s.getName());
                    drs.add(dr);
                }
                layerConfig.setStyles(drs);

                final QName dataName = new QName(data.getNamespace(), data.getName());
                final DataBrief db = dataBusiness.getDataBrief(dataName, provider.getId());
                dto = buildMapContextStyledLayer(styledLayer, layerConfig, db);

                if (styledLayer.getStyleId() != null) {
                    // Extract style information for this layer
                    final Style style = styleRepository.findById(styledLayer.getStyleId());
                    if (style != null) {
                        dto.setStyleName(style.getName());
                    }
                }

                // Extract service information for this layer
                final Layer layerRecord = layerRepository.findById(styledLayer.getLayerId());
                final Service serviceRecord = serviceRepository.findById(layerRecord.getService());
                dto.setServiceIdentifier(serviceRecord.getIdentifier());
                dto.setServiceVersions(serviceRecord.getVersions());
            } else if (dataID != null) {
                final Data data = dataRepository.findById(dataID);
                final ProviderBrief provider = providerRepository.findOne(data.getProviderId());
                final QName dataName = new QName(data.getNamespace(), data.getName());
                final DataBrief db = dataBusiness.getDataBrief(dataName, provider.getId());
                final org.constellation.dto.service.config.wxs.Layer layerConfig = new org.constellation.dto.service.config.wxs.Layer(styledLayer.getLayerId(), dataName);
                layerConfig.setAlias(data.getName());
                layerConfig.setDate(data.getDate());
                layerConfig.setOwner(data.getOwnerId());
                layerConfig.setProviderID(provider.getIdentifier());
                layerConfig.setProviderType(provider.getType());
                layerConfig.setDataId(dataID);

                // Fill styles
                final List<Integer> stylesIds = styleRepository.getStyleIdsForData(dataID);
                final List<DataReference> drs = new ArrayList<>();
                for (final Integer styleId : stylesIds) {
                    final Style s = styleRepository.findById(styleId);
                    if (s == null) {
                        continue;
                    }
                    final DataReference dr = DataReference.createProviderDataReference(DataReference.PROVIDER_STYLE_TYPE, "sld", s.getName());
                    drs.add(dr);
                }
                layerConfig.setStyles(drs);

                dto = buildMapContextStyledLayer(styledLayer, layerConfig, db);

                if (styledLayer.getStyleId() != null) {
                    // Extract style information for this layer
                    final Style style = styleRepository.findById(styledLayer.getStyleId());
                    if (style != null) {
                        dto.setStyleName(style.getName());
                    }
                }

            } else{
                dto = styledLayer;
            }

            styledLayersDto.add(dto);
        }
        Collections.sort(styledLayersDto);
        return styledLayersDto;
    }


    private static MapContextStyledLayerDTO buildMapContextStyledLayer(MapContextStyledLayerDTO mcSl, final org.constellation.dto.service.config.wxs.Layer layer,
                final DataBrief db) {
        List<StyleBrief> layerStyleBrief = null;
        if (layer != null) {
            layerStyleBrief = Util.convertIntoStylesBrief(layer.getStyles());
        }
        return new MapContextStyledLayerDTO(mcSl.getId(),
                mcSl.getMapcontextId(),
                mcSl.getLayerId(),
                mcSl.getStyleId(),
                mcSl.getOrder(),
                mcSl.getOpacity(),
                mcSl.isVisible(),
                mcSl.getExternalLayer(),
                mcSl.getExternalLayerExtent(),
                mcSl.getExternalServiceUrl(),
                mcSl.getExternalServiceVersion(),
                mcSl.getExternalStyle(),
                mcSl.isIswms(),
                mcSl.getDataId(), layer, db, layerStyleBrief);

    }
}
