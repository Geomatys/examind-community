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
package org.constellation.database.impl.repository;


import static org.constellation.database.api.jooq.Tables.MAPCONTEXT;
import static org.constellation.database.api.jooq.Tables.MAPCONTEXT_STYLED_LAYER;

import java.util.List;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import static org.constellation.database.api.jooq.Tables.CSTL_USER;

import org.constellation.database.api.jooq.tables.pojos.Mapcontext;
import org.constellation.database.api.jooq.tables.pojos.MapcontextStyledLayer;
import org.constellation.database.api.jooq.tables.records.MapcontextRecord;
import org.constellation.dto.CstlUser;
import org.constellation.dto.Data;
import org.constellation.dto.Layer;
import org.constellation.repository.MapContextRepository;
import org.constellation.dto.MapContextDTO;
import org.constellation.dto.MapContextStyledLayerDTO;
import org.constellation.repository.DataRepository;
import org.constellation.repository.LayerRepository;
import org.constellation.repository.ProviderRepository;
import org.constellation.repository.UserRepository;

import org.jooq.Field;
import org.jooq.Select;
import org.jooq.SelectConditionStep;
import org.jooq.SelectLimitStep;
import org.jooq.SortField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@DependsOn("database-initer")
public class JooqMapContextRepository extends AbstractJooqRespository<MapcontextRecord, Mapcontext> implements MapContextRepository {

    @Autowired
    private LayerRepository layerRepository;

    @Autowired
    private DataRepository dataRepository;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private UserRepository userRepository;

    public JooqMapContextRepository() {
        super(Mapcontext.class, MAPCONTEXT);
    }

    @Override
    public MapContextDTO findById(int id) {
        return convertToDTO(dsl.select().from(MAPCONTEXT).where(MAPCONTEXT.ID.eq(id)).fetchOneInto(Mapcontext.class));
    }

    @Override
    public List<MapContextStyledLayerDTO> getLinkedLayers(int mapContextId) {
        List<MapcontextStyledLayer> mcLayers = dsl.select().from(MAPCONTEXT_STYLED_LAYER)
                .where(MAPCONTEXT_STYLED_LAYER.MAPCONTEXT_ID.eq(mapContextId))
                .fetchInto(MapcontextStyledLayer.class);

        List<MapContextStyledLayerDTO> results = new ArrayList<>();
        for (MapcontextStyledLayer mclayer : mcLayers) {
            Layer layer = null;
            Data data = null;
            if (mclayer.getLayerId() != null) {
                layer = layerRepository.findById(mclayer.getLayerId());
            }
            if (mclayer.getDataId() != null) {
                data = dataRepository.findById(mclayer.getDataId());
            }
            results.add(convertToDto(mclayer, layer, data));
        }
        return results;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void setLinkedLayers(int contextId, List<MapContextStyledLayerDTO> layers) {
        // Remove eventually existing old layers for this map context
        dsl.delete(MAPCONTEXT_STYLED_LAYER).where(MAPCONTEXT_STYLED_LAYER.MAPCONTEXT_ID.eq(contextId)).execute();

        if (layers.isEmpty()) {
            return;
        }

        for (final MapContextStyledLayerDTO layer : layers) {
            dsl.insertInto(MAPCONTEXT_STYLED_LAYER)
                .set(MAPCONTEXT_STYLED_LAYER.LAYER_ID, layer.getLayerId())
                .set(MAPCONTEXT_STYLED_LAYER.MAPCONTEXT_ID, layer.getMapcontextId())
                .set(MAPCONTEXT_STYLED_LAYER.STYLE_ID, layer.getStyleId())
                .set(MAPCONTEXT_STYLED_LAYER.LAYER_VISIBLE, layer.isVisible())
                .set(MAPCONTEXT_STYLED_LAYER.LAYER_ORDER, layer.getOrder())
                .set(MAPCONTEXT_STYLED_LAYER.LAYER_OPACITY, layer.getOpacity())
                .set(MAPCONTEXT_STYLED_LAYER.EXTERNAL_LAYER, layer.getExternalLayer())
                .set(MAPCONTEXT_STYLED_LAYER.EXTERNAL_LAYER_EXTENT, layer.getExternalLayerExtent())
                .set(MAPCONTEXT_STYLED_LAYER.EXTERNAL_SERVICE_URL, layer.getExternalServiceUrl())
                .set(MAPCONTEXT_STYLED_LAYER.EXTERNAL_SERVICE_VERSION, layer.getExternalServiceVersion())
                .set(MAPCONTEXT_STYLED_LAYER.EXTERNAL_STYLE, layer.getExternalStyle())
                .set(MAPCONTEXT_STYLED_LAYER.ISWMS, layer.isIswms())
                .set(MAPCONTEXT_STYLED_LAYER.DATA_ID, layer.getDataId())
                .execute();
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Integer create(MapContextDTO mapContext) {
        MapcontextRecord newRecord = dsl.newRecord(MAPCONTEXT);
        newRecord.from(convertToDAO(mapContext));
        newRecord.store();
        return newRecord.into(Mapcontext.class).getId();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int update(MapContextDTO mapContext) {
        return dsl.update(MAPCONTEXT)
                   .set(MAPCONTEXT.CRS, mapContext.getCrs())
                   .set(MAPCONTEXT.WEST, mapContext.getWest())
                   .set(MAPCONTEXT.SOUTH, mapContext.getSouth())
                   .set(MAPCONTEXT.EAST, mapContext.getEast())
                   .set(MAPCONTEXT.NORTH, mapContext.getNorth())
                   .set(MAPCONTEXT.DESCRIPTION, mapContext.getDescription())
                   .set(MAPCONTEXT.KEYWORDS, mapContext.getKeywords())
                   .set(MAPCONTEXT.NAME, mapContext.getName())
                   .set(MAPCONTEXT.OWNER, mapContext.getOwner())
                   .where(MAPCONTEXT.ID.eq(mapContext.getId())).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int delete(int id) {
        return dsl.delete(MAPCONTEXT).where(MAPCONTEXT.ID.eq(id)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int deleteAll() {
        return dsl.delete(MAPCONTEXT).execute();
    }

    @Override
    public List<Integer> findAllId() {
        return dsl.select(MAPCONTEXT.ID).from(MAPCONTEXT).fetchInto(Integer.class);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void updateOwner(Integer contextId, int newOwner) {
        dsl.update(MAPCONTEXT)
                .set(MAPCONTEXT.OWNER, newOwner)
                .where(MAPCONTEXT.ID.eq(contextId))
                .execute();
    }

    /**
     * Returns a singleton map that contains the total count of records as key,
     * and the list of records as value.
     * the list is resulted by filters, it use pagination and sorting.

     * @param filterMap given filters
     * @param sortEntry given sort, can be null
     * @param pageNumber pagination page
     * @param rowsPerPage count of rows per page
     * @return Map
     */
    @Override
    public Map.Entry<Integer, List<MapContextDTO>> filterAndGet(final Map<String,Object> filterMap,
                                       final Map.Entry<String,String> sortEntry,
                                       final int pageNumber,
                                       final int rowsPerPage) {
        Collection<Field<?>> fields = new ArrayList<>();
        Collections.addAll(fields,MAPCONTEXT.fields());
        Select query = null;
        if(filterMap != null) {
            for(final Map.Entry<String,Object> entry : filterMap.entrySet()) {
                if("owner".equals(entry.getKey())) {
                    if(query == null) {
                        query = dsl.select(fields).from(MAPCONTEXT).leftOuterJoin(CSTL_USER).on(MAPCONTEXT.OWNER.eq(CSTL_USER.ID)).where(MAPCONTEXT.OWNER.equal((Integer)entry.getValue()));
                    }else {
                        query = ((SelectConditionStep)query).and(MAPCONTEXT.OWNER.equal((Integer)entry.getValue()));
                    }
                }else if("term".equals(entry.getKey())) {
                    if(query == null) {
                        query = dsl.select(fields).from(MAPCONTEXT).leftOuterJoin(CSTL_USER).on(MAPCONTEXT.OWNER.eq(CSTL_USER.ID)).where(MAPCONTEXT.NAME.likeIgnoreCase("%"+entry.getValue()+"%").or(MAPCONTEXT.DESCRIPTION.likeIgnoreCase("%"+entry.getValue()+"%")));
                    }else {
                        query = ((SelectConditionStep)query).and(MAPCONTEXT.NAME.likeIgnoreCase("%"+entry.getValue()+"%").or(MAPCONTEXT.DESCRIPTION.likeIgnoreCase("%"+entry.getValue()+"%")));
                    }
                }
            }
        }
        if(sortEntry != null) {
            SortField f = null;
            if ("title".equals(sortEntry.getKey()) || "name".equals(sortEntry.getKey())) {
                f = "ASC".equals(sortEntry.getValue()) ? MAPCONTEXT.NAME.asc() : MAPCONTEXT.NAME.desc();
            } else if ("owner".equals(sortEntry.getKey())) {
                f = "ASC".equals(sortEntry.getValue()) ? CSTL_USER.LOGIN.asc() : CSTL_USER.LOGIN.desc();
            }
            if (f != null) {
                if (query == null) {
                    query = dsl.select(fields).from(MAPCONTEXT).leftOuterJoin(CSTL_USER).on(MAPCONTEXT.OWNER.eq(CSTL_USER.ID)).orderBy(f);
                } else {
                    query = ((SelectConditionStep) query).orderBy(f);
                }
            }
        }

        final Map.Entry<Integer,List<MapContextDTO>> result;
        if(query == null) { //means there are no sorting and no filters
            final int count = dsl.selectCount().from(MAPCONTEXT).fetchOne(0,int.class);
            result = new AbstractMap.SimpleImmutableEntry<>(count,
                    convertMCListToDto(dsl.select(fields).from(MAPCONTEXT).leftOuterJoin(CSTL_USER).on(MAPCONTEXT.OWNER.eq(CSTL_USER.ID)).limit(rowsPerPage).offset((pageNumber - 1) * rowsPerPage).fetchInto(Mapcontext.class)));
        }else {
            final int count = dsl.fetchCount(query);
            result = new AbstractMap.SimpleImmutableEntry<>(count,
                    convertMCListToDto(((SelectLimitStep) query).limit(rowsPerPage).offset((pageNumber - 1) * rowsPerPage).fetchInto(Mapcontext.class)));
        }
        return result;
    }

    @Override
    public List<MapContextDTO> findAll() {
        return convertMCListToDto(dsl.select().from(MAPCONTEXT).fetchInto(Mapcontext.class));
    }

    private MapContextStyledLayerDTO convertToDto(MapcontextStyledLayer mcSl, Layer layer, Data data) {
        if (mcSl != null) {
            String layerName = null;
            String layerNamespace = null;
            String layerAlias = null;
            Integer serviceID = null;
            Date date = null;
            String layerConfig = null;
            String layerTitle = null;
            if (layer != null) {
                layerName = layer.getName();
                layerNamespace = layer.getNamespace();
                layerAlias = layer.getAlias();
                serviceID = layer.getService();
                date = layer.getDate();
                layerConfig = layer.getConfig();
                layerTitle = layer.getTitle();
            }

            String dataType = null;
            String dataSubType = null;
            String dataOwner = null;
            String dataProvider = null;
            Integer dataProviderID = null;
            Integer ownerId = null;
            if (data != null) {
                dataType = data.getType();
                dataSubType = data.getSubtype();
                Optional<CstlUser> user = userRepository.findById(data.getOwnerId());
                if (user.isPresent()) {
                    dataOwner = user.get().getLogin();
                }
                dataProvider = providerRepository.findOne(data.getProviderId()).getIdentifier();
                dataProviderID = data.getProviderId();
                ownerId = data.getOwnerId();
            }
            // TODO target styles

            return new MapContextStyledLayerDTO(mcSl.getId(),
                    mcSl.getMapcontextId(),
                    mcSl.getLayerId(),
                    mcSl.getStyleId(),
                    mcSl.getLayerOrder(),
                    mcSl.getLayerOpacity(),
                    mcSl.getLayerVisible(),
                    mcSl.getExternalLayer(),
                    mcSl.getExternalLayerExtent(),
                    mcSl.getExternalServiceUrl(),
                    mcSl.getExternalServiceVersion(),
                    mcSl.getExternalStyle(),
                    mcSl.getIswms(),
                    mcSl.getDataId(),
                    layerName,
                    layerNamespace,
                    layerAlias,
                    serviceID,
                    date,
                    layerConfig,
                    ownerId,
                    layerTitle,
                    dataType,
                    dataSubType,
                    dataOwner,
                    dataProvider,
                    dataProviderID,
                    null);
        }
        return null;
    }

    private static List<MapContextDTO> convertMCListToDto(List<Mapcontext> daos) {
        List<MapContextDTO> dtos = new ArrayList<>();
        for (Mapcontext dao : daos) {
            dtos.add(convertToDTO(dao));
        }
        return dtos;
    }

    private static MapContextDTO convertToDTO(Mapcontext mctx) {
        if (mctx != null) {
            return new MapContextDTO(mctx.getId(),
                    mctx.getName(),
                    mctx.getOwner(),
                    mctx.getDescription(),
                    mctx.getCrs(),
                    mctx.getWest(),
                    mctx.getNorth(),
                    mctx.getEast(),
                    mctx.getSouth(),
                    mctx.getKeywords(),
                    null);
        }
        return null;
    }

    private static Mapcontext convertToDAO(MapContextDTO mctx) {
        if (mctx != null) {
            return new Mapcontext(mctx.getId(),
                    mctx.getName(),
                    mctx.getOwner(),
                    mctx.getDescription(),
                    mctx.getCrs(),
                    mctx.getWest(),
                    mctx.getNorth(),
                    mctx.getEast(),
                    mctx.getSouth(),
                    mctx.getKeywords());
        }
        return null;
    }
}
