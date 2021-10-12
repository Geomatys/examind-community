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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.xml.namespace.QName;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.util.logging.Logging;

import org.constellation.business.IDataBusiness;
import org.constellation.business.IMapContextBusiness;
import org.constellation.business.IMetadataBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IUserBusiness;
import org.constellation.dto.CstlUser;
import org.constellation.dto.Data;
import org.constellation.dto.DataBrief;
import org.constellation.dto.DataDescription;
import org.constellation.dto.Layer;
import org.constellation.dto.MapContextDTO;
import org.constellation.dto.MapContextLayersDTO;
import org.constellation.dto.MapContextStyledLayerDTO;
import org.constellation.dto.ParameterValues;
import org.constellation.dto.Style;
import org.constellation.dto.StyleBrief;
import org.constellation.dto.StyleReference;
import org.constellation.dto.service.Service;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.TargetNotFoundException;
import org.constellation.repository.LayerRepository;
import org.constellation.repository.MapContextRepository;
import org.constellation.repository.ServiceRepository;
import org.constellation.repository.StyleRepository;
import org.constellation.repository.StyledLayerRepository;
import org.constellation.util.Util;
import org.opengis.geometry.Envelope;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
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
    private IMetadataBusiness metadataBusiness;

    @Inject
    private IDataBusiness dataBusiness;

    @Inject
    private IProviderBusiness providerBusiness;

    @Inject
    private StyleRepository styleRepository;

    @Inject
    private ServiceRepository serviceRepository;

    @Inject
    private StyledLayerRepository styledLayerRepository;

    @Inject
    private IUserBusiness userBusiness;

    @Override
    @Transactional
    public void setMapItems(final int contextId, final List<MapContextStyledLayerDTO> layers) {
        mapContextRepository.setLinkedLayers(contextId, layers);
    }

    @Override
    @Transactional
    public Integer create(final MapContextLayersDTO mapContext) throws ConstellationException {
        int id = mapContextRepository.create(mapContext);
        if (mapContext.getLayers() != null) {
            for (MapContextStyledLayerDTO layer : mapContext.getLayers()) {
                layer.setMapcontextId(id);
            }
        }
        mapContextRepository.setLinkedLayers(id, mapContext.getLayers());
        return id;
    }

    @Override
    @Transactional
    public Integer createFromData(Integer userId, String contextName, String crs, Envelope env, List<DataBrief> briefs) throws ConstellationException {
        final MapContextLayersDTO mapContext = new MapContextLayersDTO();
        mapContext.setOwner(userId);
        mapContext.setCrs(crs);
        mapContext.setKeywords("");
        mapContext.setWest(env.getMinimum(0));
        mapContext.setSouth(env.getMinimum(1));
        mapContext.setEast(env.getMaximum(0));
        mapContext.setNorth(env.getMaximum(1));
        mapContext.setName(contextName);
        final Integer id =  mapContextRepository.create(mapContext);
        final List<MapContextStyledLayerDTO> mapcontextlayers = new ArrayList<>();
        for (final DataBrief db : briefs) {
            final MapContextStyledLayerDTO mcStyledLayer = new MapContextStyledLayerDTO();
            mcStyledLayer.setDataId(db.getId());
            final StyleBrief style = db.getFirstStyle();
            if (style != null) {
                mcStyledLayer.setExternalStyle(style.getName());
                mcStyledLayer.setStyleId(style.getId());
            }
            mcStyledLayer.setIswms(false);
            mcStyledLayer.setLayerId(null);
            mcStyledLayer.setOpacity(100);
            mcStyledLayer.setOrder(briefs.indexOf(db));
            mcStyledLayer.setVisible(true);
            mcStyledLayer.setMapcontextId(id);
            mapcontextlayers.add(mcStyledLayer);
        }
        mapContextRepository.setLinkedLayers(id, mapcontextlayers);
        return id;
    }

    @Override
    public List<MapContextLayersDTO> findAllMapContextLayers() throws ConstellationException {
        final List<MapContextLayersDTO> ctxtLayers = new ArrayList<>();
        final List<MapContextDTO> ctxts = mapContextRepository.findAll();
        for (final MapContextDTO ctxt : ctxts) {
            final MapContextLayersDTO mapcontext = convertToMapContextLayer(ctxt);
            ctxtLayers.add(mapcontext);
        }
        return ctxtLayers;
    }

    private MapContextLayersDTO convertToMapContextLayer(final MapContextDTO ctxt) throws ConstellationException {
        final List<MapContextStyledLayerDTO> styledLayers = mapContextRepository.getLinkedLayers(ctxt.getId());
        final List<MapContextStyledLayerDTO> styledLayersDto = generateLayerDto(styledLayers);

        //get owner login.
        String userLogin = null;
        final Optional<CstlUser> user = userBusiness.findById(ctxt.getOwner());
        if (user.isPresent()) {
            userLogin = user.get().getLogin();
        }
        return buildMapContextLayers(ctxt, userLogin, styledLayersDto);
    }

    @Override
    public MapContextLayersDTO findMapContextLayers(int contextId) throws ConstellationException {
        final MapContextDTO ctxt = mapContextRepository.findById(contextId);
        if (ctxt != null) {
            return convertToMapContextLayer(ctxt);
        }
        throw new TargetNotFoundException("No mapcontext found with id: " + contextId);
    }

    @Override
    public MapContextLayersDTO findByName(String contextName) throws ConstellationException {
        final MapContextDTO ctxt = mapContextRepository.findByName(contextName);
        if (ctxt != null) {
            return convertToMapContextLayer(ctxt);
        }
        throw new TargetNotFoundException("No mapcontext found with name: " + contextName);
    }

    /**
     * Get the extent of all included layers in this map context.
     *
     * @param contextId Context identifier
     * @return
     * @throws ConstellationException
     */
    @Override
    public ParameterValues getExtent(int contextId) throws ConstellationException {
        final ParameterValues values = new ParameterValues();
        final MapContextDTO context = mapContextRepository.findById(contextId);
        GeneralEnvelope env = null;
        if (context.getWest() != null && context.getSouth() != null && context.getEast() != null && context.getNorth() != null && context.getCrs() != null) {
            try {
                final CoordinateReferenceSystem crs = AbstractCRS.castOrCopy(CRS.forCode(context.getCrs())).forConvention(AxesConvention.RIGHT_HANDED);
                env = new GeneralEnvelope(crs);
                env.setRange(0, context.getWest(), context.getEast());
                env.setRange(1, context.getSouth(), context.getNorth());
            } catch (FactoryException ex) {
                throw new ConstellationException(ex);
            }
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
     * @throws ConstellationException
     */
    @Override
    public ParameterValues getExtentForLayers(final List<MapContextStyledLayerDTO> styledLayers) throws ConstellationException {
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
                                                 final GeneralEnvelope ctxtEnv) throws ConstellationException {
        GeneralEnvelope env = ctxtEnv;
        for (final MapContextStyledLayerDTO styledLayer : styledLayers) {
            if (!styledLayer.isVisible()) {
                continue;
            }
            Integer layerID = styledLayer.getLayerId();
            Integer dataID = styledLayer.getDataId();
            if (layerID != null || dataID != null) {
                if (dataID == null) {
                    final Layer layerRecord = layerRepository.findById(layerID);
                    dataID = layerRecord.getDataId();
                }
                DataBrief db = dataBusiness.getDataBrief(dataID, true);
                final DataDescription ddesc = db.getDataDescription();
                if (ddesc != null) {
                    final double[] bbox = ddesc.getBoundingBox();
                    final GeneralEnvelope dataEnv = new GeneralEnvelope(CommonCRS.defaultGeographic());
                    dataEnv.setRange(0,bbox[0],bbox[2]);
                    dataEnv.setRange(1,bbox[1],bbox[3]);
                    if (env == null) {
                        env = dataEnv;
                    } else {
                        env.add(dataEnv);
                    }
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
    public void updateContext(MapContextLayersDTO mapContext) throws ConstellationException {
        mapContextRepository.update(mapContext);
        for (MapContextStyledLayerDTO layer : mapContext.getLayers()) {
            layer.setMapcontextId(mapContext.getId());
        }
        mapContextRepository.setLinkedLayers(mapContext.getId(), mapContext.getLayers());
    }

    @Override
    @Transactional
    public void delete(int contextId) throws ConstellationException {
        metadataBusiness.deleteMapContextMetadata(contextId);
        mapContextRepository.delete(contextId);
    }

    @Override
    @Transactional
    public void deleteAll() throws ConstellationException {
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


    private static MapContextLayersDTO buildMapContextLayers(MapContextDTO mctx, String userOwner, List<MapContextStyledLayerDTO> layers) {
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
                userOwner,
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
                final Data db = dataBusiness.getData(layer.getDataId());

                final QName name = layer.getName();

                final org.constellation.dto.service.config.wxs.LayerConfig layerConfig = new org.constellation.dto.service.config.wxs.LayerConfig(layer.getId(), name);
                layerConfig.setAlias(layer.getAlias());
                layerConfig.setDate(layer.getDate());
                layerConfig.setOwnerId(layer.getOwnerId());
                layerConfig.setDataId(layer.getDataId());

                final List<Integer> styledLays = styledLayerRepository.findByLayer(layer.getId());
                final List<StyleReference> drs = new ArrayList<>();
                for (final Integer styledLay : styledLays) {
                    final Style s = styleRepository.findById(styledLay);
                    if (s == null) {
                        continue;
                    }
                    final StyleReference dr = new StyleReference(s.getId(), s.getName(), s.getProviderId(), "sld");
                    drs.add(dr);
                }
                layerConfig.setStyles(drs);
                String owner = userBusiness.findById(db.getOwnerId()).map(CstlUser::getLogin).orElse(null);

                dto = buildMapContextStyledLayer(styledLayer, layerConfig, db, owner);

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
                final Data db = dataBusiness.getData(dataID);
                final QName dataName = new QName(db.getNamespace(), db.getName());
                final org.constellation.dto.service.config.wxs.LayerConfig layerConfig = new org.constellation.dto.service.config.wxs.LayerConfig(styledLayer.getLayerId(), dataName);
                layerConfig.setAlias(db.getName());
                layerConfig.setDate(db.getDate());
                layerConfig.setOwnerId(db.getOwnerId());
                layerConfig.setDataId(dataID);

                // Fill styles
                final List<Integer> stylesIds = styleRepository.getStyleIdsForData(dataID);
                final List<StyleReference> drs = new ArrayList<>();
                for (final Integer styleId : stylesIds) {
                    final Style s = styleRepository.findById(styleId);
                    if (s == null) {
                        continue;
                    }
                    final StyleReference dr = new StyleReference(s.getId(), s.getName(), s.getProviderId(), "sld");
                    drs.add(dr);
                }
                layerConfig.setStyles(drs);
                String owner = userBusiness.findById(db.getOwnerId()).map(CstlUser::getLogin).orElse(null);
                dto = buildMapContextStyledLayer(styledLayer, layerConfig, db, owner);

                if (styledLayer.getStyleId() != null) {
                    // Extract style information for this layer
                    final Style style = styleRepository.findById(styledLayer.getStyleId());
                    if (style != null) {
                        dto.setStyleName(style.getName());
                    }
                }

            } else {
                dto = styledLayer;
            }

            styledLayersDto.add(dto);
        }
        Collections.sort(styledLayersDto);
        return styledLayersDto;
    }

    private static MapContextStyledLayerDTO buildMapContextStyledLayer(MapContextStyledLayerDTO mcSl, final org.constellation.dto.service.config.wxs.LayerConfig layer,
                final Data db, String owner) {
        List<StyleBrief> layerStyleBrief = null;
        if (layer != null) {
            layerStyleBrief = Util.convertRefIntoStylesBrief(layer.getStyles());
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
                mcSl.getDataId(), layer, db, owner, layerStyleBrief);

    }
}