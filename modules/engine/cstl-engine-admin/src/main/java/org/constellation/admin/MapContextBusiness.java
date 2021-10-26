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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
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
import static org.constellation.api.ProviderConstants.INTERNAL_MAP_CONTEXT_PROVIDER;
import org.constellation.api.ProviderType;

import org.constellation.business.IDataBusiness;
import org.constellation.business.IMapContextBusiness;
import org.constellation.business.IMetadataBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IUserBusiness;
import org.constellation.dto.AbstractMCLayerDTO;
import org.constellation.dto.CstlUser;
import org.constellation.dto.Data;
import org.constellation.dto.DataBrief;
import org.constellation.dto.DataDescription;
import org.constellation.dto.DataMCLayerDTO;
import org.constellation.dto.ExternalServiceMCLayerDTO;
import org.constellation.dto.InternalServiceMCLayerDTO;
import org.constellation.dto.Layer;
import org.constellation.dto.MapContextDTO;
import org.constellation.dto.MapContextLayersDTO;
import org.constellation.dto.ParameterValues;
import org.constellation.dto.StyleBrief;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.TargetNotFoundException;
import org.constellation.provider.DataProviderFactory;
import org.constellation.provider.DataProviders;
import org.constellation.repository.LayerRepository;
import org.constellation.repository.MapContextRepository;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.ParameterValueGroup;
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
    private IUserBusiness userBusiness;

    @Override
    @Transactional
    public void setMapItems(final int contextId, final List<AbstractMCLayerDTO> layers) {
        mapContextRepository.setLinkedLayers(contextId, layers);
        reloadMapContextProvider();
    }

    @Override
    @Transactional
    public Integer create(final MapContextLayersDTO mapContext) throws ConstellationException {
        if (mapContextRepository.existsByName(mapContext.getName())) {
            throw new ConfigurationException("Map context name is already used");
        }
        int id = mapContextRepository.create(mapContext);
        mapContextRepository.setLinkedLayers(id, mapContext.getLayers());
        reloadMapContextProvider();
        return id;
    }

    @Override
    @Transactional
    public Integer createFromData(Integer userId, String contextName, String crs, Envelope env, List<DataBrief> briefs) throws ConstellationException {
        if (mapContextRepository.existsByName(contextName)) {
            throw new ConfigurationException("Map context name is already used");
        }
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
        final List<AbstractMCLayerDTO> mapcontextlayers = new ArrayList<>();
        for (final DataBrief db : briefs) {
            final StyleBrief style = db.getFirstStyle();
            mapcontextlayers.add(new DataMCLayerDTO(
                                          new QName(db.getNamespace(), db.getName()),
                                          briefs.indexOf(db),
                                          100,
                                          true,
                                          db.getDate(),
                                          db.getType(),
                                          db.getOwner(),
                                          db.getId(),
                                          style != null ? style.getId(): null,
                                          style != null ? style.getName() : null));
        }
        mapContextRepository.setLinkedLayers(id, mapcontextlayers);
        reloadMapContextProvider();
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
        final List<AbstractMCLayerDTO> styledLayers = mapContextRepository.getLinkedLayers(ctxt.getId());
        //get owner login.
        String userLogin = userBusiness.findById(ctxt.getOwner()).map(CstlUser::getLogin).orElse(null);
        return buildMapContextLayers(ctxt, userLogin, styledLayers);
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

        final List<AbstractMCLayerDTO> styledLayers = mapContextRepository.getLinkedLayers(contextId);
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
    public ParameterValues getExtentForLayers(final List<AbstractMCLayerDTO> styledLayers) throws ConstellationException {
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

    private GeneralEnvelope getEnvelopeForLayers(final List<AbstractMCLayerDTO> styledLayers,
                                                 final GeneralEnvelope ctxtEnv) throws ConstellationException {
        GeneralEnvelope env = ctxtEnv;
        for (final AbstractMCLayerDTO styledLayer : styledLayers) {
            if (!styledLayer.isVisible()) {
                continue;
            }
            Integer dataID = null;
            if (styledLayer instanceof InternalServiceMCLayerDTO) {
                InternalServiceMCLayerDTO isLayer = (InternalServiceMCLayerDTO) styledLayer;
                final Layer layerRecord = layerRepository.findById(isLayer.getLayerId());
                dataID = layerRecord.getDataId();
            } else if (styledLayer instanceof DataMCLayerDTO) {
                DataMCLayerDTO dLayer = (DataMCLayerDTO) styledLayer;
                dataID = dLayer.getDataId();
            } else if (styledLayer instanceof ExternalServiceMCLayerDTO) {
                ExternalServiceMCLayerDTO eLayer = (ExternalServiceMCLayerDTO) styledLayer;
                final String extLayerExtent = eLayer.getExternalLayerExtent();
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
            if (dataID != null) {
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
        // verify if there is a new name and if it is already used.
        MapContextDTO old = mapContextRepository.findById(mapContext.getId());
        if (old == null) {
            throw new TargetNotFoundException("Uable to find a mapcontext with  the id:" + mapContext.getId());

        // look for a name change
        } else if (!old.getName().equals(mapContext.getName()) && mapContextRepository.existsByName(mapContext.getName())) {
            throw new ConfigurationException("Map context name is already used");

        }
        mapContextRepository.update(mapContext);
        mapContextRepository.setLinkedLayers(mapContext.getId(), mapContext.getLayers());
        // in case of name change
        reloadMapContextProvider();
    }

    @Override
    @Transactional
    public void delete(int contextId) throws ConstellationException {
        metadataBusiness.deleteMapContextMetadata(contextId);
        mapContextRepository.delete(contextId);
        reloadMapContextProvider();
    }

    @Override
    @Transactional
    public void deleteAll() throws ConstellationException {
        List<Integer> ids = mapContextRepository.findAllId();
        for (Integer id : ids) {
            delete(id);
        }
        reloadMapContextProvider();
    }

    @Override
    public void initializeDefaultMapContextData() {
        if (providerBusiness.getIDFromIdentifier(INTERNAL_MAP_CONTEXT_PROVIDER) == null) {
            try {
               final DataProviderFactory factory = DataProviders.getFactory("mapcontext-provider");
                final ParameterValueGroup source = factory.getProviderDescriptor().createValue();
                source.parameter("id").setValue(INTERNAL_MAP_CONTEXT_PROVIDER);
                final ParameterValueGroup config = source.addGroup("MapContextProvider");

                int pid = providerBusiness.storeProvider(INTERNAL_MAP_CONTEXT_PROVIDER, ProviderType.LAYER, "mapcontext-provider", source);
                providerBusiness.createOrUpdateData(pid, null, false, true, null);

            } catch (ConstellationException ex) {
                LOGGER.log(Level.WARNING, "An error occurred when creating default map context provider.", ex);
            }
        }
    }

    private void reloadMapContextProvider() {
        try {
            providerBusiness.reload(INTERNAL_MAP_CONTEXT_PROVIDER);

        // in some context the provider can be missing.
        } catch (TargetNotFoundException ex) {
           LOGGER.log(Level.INFO, "Error while reloading map context provider:" +  ex.getMessage());

        // catch also the persistence runtime exception for old database with non unique mapcontext name
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error while reloading map context provider", ex);
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

    @Override
    public Data getMapContextDataId(int id) throws ConstellationException {
        final MapContextDTO mc = mapContextRepository.findById(id);
        if (mc != null) {
            return dataBusiness.getDataBrief(new QName(mc.getName()), INTERNAL_MAP_CONTEXT_PROVIDER, false);
        }
        throw new TargetNotFoundException("No mapcontext found with id: " + id);
    }

    private static MapContextLayersDTO buildMapContextLayers(MapContextDTO mctx, String userOwner, List<AbstractMCLayerDTO> layers) {
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
}