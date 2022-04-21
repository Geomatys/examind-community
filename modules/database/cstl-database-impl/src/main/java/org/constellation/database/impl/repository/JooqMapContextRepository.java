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


import static com.examind.database.api.jooq.Tables.MAPCONTEXT;
import static com.examind.database.api.jooq.Tables.MAPCONTEXT_STYLED_LAYER;

import java.util.List;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import static com.examind.database.api.jooq.Tables.CSTL_USER;

import com.examind.database.api.jooq.tables.pojos.Mapcontext;
import com.examind.database.api.jooq.tables.pojos.MapcontextStyledLayer;
import com.examind.database.api.jooq.tables.records.MapcontextRecord;
import com.examind.database.api.jooq.tables.records.MapcontextStyledLayerRecord;
import java.util.Date;
import org.constellation.dto.AbstractMCLayerDTO;
import org.constellation.dto.CstlUser;
import org.constellation.dto.Data;
import org.constellation.dto.DataMCLayerDTO;
import org.constellation.dto.ExternalServiceMCLayerDTO;
import org.constellation.dto.InternalServiceMCLayerDTO;
import org.constellation.dto.Layer;
import org.constellation.repository.MapContextRepository;
import org.constellation.dto.MapContextDTO;
import org.constellation.dto.Style;
import org.constellation.dto.service.Service;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationPersistenceException;
import org.constellation.repository.DataRepository;
import org.constellation.repository.LayerRepository;
import org.constellation.repository.ServiceRepository;
import org.constellation.repository.StyleRepository;
import org.constellation.repository.UserRepository;
import org.jooq.Condition;

import org.jooq.Field;
import org.jooq.InsertSetMoreStep;
import org.jooq.Select;
import org.jooq.SelectConnectByStep;
import org.jooq.SelectJoinStep;
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

    private static final Logger LOGGER = Logger.getLogger("org.constellation.database.impl.repository");

    @Autowired
    private LayerRepository layerRepository;

    @Autowired
    private DataRepository dataRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StyleRepository styleRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    private static final Field[] REDUCED = {MAPCONTEXT.ID, MAPCONTEXT.NAME};

    private static final Field[] REDUCED_LAYER = {MAPCONTEXT_STYLED_LAYER.ID,
                                                  MAPCONTEXT_STYLED_LAYER.DATA_ID,
                                                  MAPCONTEXT_STYLED_LAYER.LAYER_ID,
                                                  MAPCONTEXT_STYLED_LAYER.STYLE_ID,
                                                  MAPCONTEXT_STYLED_LAYER.EXTERNAL_LAYER,
                                                  MAPCONTEXT_STYLED_LAYER.EXTERNAL_SERVICE_URL,
                                                  MAPCONTEXT_STYLED_LAYER.EXTERNAL_STYLE};

    public JooqMapContextRepository() {
        super(Mapcontext.class, MAPCONTEXT);
    }

    @Override
    public MapContextDTO findById(int id, boolean full) {
        final Field[] fields = full ? MAPCONTEXT.fields() : REDUCED;
        return convertToDTO(dsl.select(fields).from(MAPCONTEXT).where(MAPCONTEXT.ID.eq(id)).fetchOneInto(Mapcontext.class));
    }

    @Override
    public MapContextDTO findByName(String name) {
        return convertToDTO(dsl.select().from(MAPCONTEXT).where(MAPCONTEXT.NAME.eq(name)).fetchOneInto(Mapcontext.class));
    }

    @Override
    public boolean existsByName(String name) {
        return dsl.selectCount().from(MAPCONTEXT)
                .where(MAPCONTEXT.NAME.eq(name))
                .fetchOne(0, Integer.class) > 0;
    }

    @Override
    public List<AbstractMCLayerDTO> getLinkedLayers(int mapContextId, boolean full) {
        final Field[] fields = full ? MAPCONTEXT_STYLED_LAYER.fields() : REDUCED_LAYER;
        List<MapcontextStyledLayer> mcLayers = dsl.select(fields).from(MAPCONTEXT_STYLED_LAYER)
                .where(MAPCONTEXT_STYLED_LAYER.MAPCONTEXT_ID.eq(mapContextId))
                .fetchInto(MapcontextStyledLayer.class);

        List<AbstractMCLayerDTO> results = new ArrayList<>();
        for (MapcontextStyledLayer mclayer : mcLayers) {
            try {
                results.add(convertToDto(mclayer, full));
            } catch (ConfigurationException ex) {
                LOGGER.log(Level.WARNING, "Error while reading mapcontext layer", ex);
            }
        }
        return results;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void setLinkedLayers(int contextId, List<AbstractMCLayerDTO> layers) {
        // Remove eventually existing old layers for this map context
        dsl.delete(MAPCONTEXT_STYLED_LAYER).where(MAPCONTEXT_STYLED_LAYER.MAPCONTEXT_ID.eq(contextId)).execute();

        if (layers == null || layers.isEmpty()) {
            return;
        }
        for (final AbstractMCLayerDTO layer : layers) {
            InsertSetMoreStep<MapcontextStyledLayerRecord> insert =
                 dsl.insertInto(MAPCONTEXT_STYLED_LAYER)
                    .set(MAPCONTEXT_STYLED_LAYER.MAPCONTEXT_ID, contextId)
                    .set(MAPCONTEXT_STYLED_LAYER.LAYER_VISIBLE, layer.isVisible())
                    .set(MAPCONTEXT_STYLED_LAYER.LAYER_ORDER, layer.getOrder())
                    .set(MAPCONTEXT_STYLED_LAYER.LAYER_OPACITY, layer.getOpacity());
            if (layer instanceof InternalServiceMCLayerDTO isLayer) {
                insert=
                insert.set(MAPCONTEXT_STYLED_LAYER.LAYER_ID, isLayer.getLayerId())
                      .set(MAPCONTEXT_STYLED_LAYER.STYLE_ID, isLayer.getStyleId())
                      .set(MAPCONTEXT_STYLED_LAYER.ISWMS, true);

            } else if (layer instanceof DataMCLayerDTO dLayer) {
                insert=
                insert.set(MAPCONTEXT_STYLED_LAYER.DATA_ID, dLayer.getDataId())
                      .set(MAPCONTEXT_STYLED_LAYER.STYLE_ID, dLayer.getStyleId())
                      .set(MAPCONTEXT_STYLED_LAYER.ISWMS, true);

            } else if (layer instanceof ExternalServiceMCLayerDTO eLayer) {
                insert=
                insert.set(MAPCONTEXT_STYLED_LAYER.EXTERNAL_LAYER, eLayer.getExternalLayer() != null ? eLayer.getExternalLayer().getLocalPart() : null)
                      .set(MAPCONTEXT_STYLED_LAYER.EXTERNAL_LAYER_EXTENT, eLayer.getExternalLayerExtent())
                      .set(MAPCONTEXT_STYLED_LAYER.EXTERNAL_SERVICE_URL, eLayer.getExternalServiceUrl())
                      .set(MAPCONTEXT_STYLED_LAYER.EXTERNAL_SERVICE_VERSION, eLayer.getExternalServiceVersion())
                      .set(MAPCONTEXT_STYLED_LAYER.EXTERNAL_STYLE, eLayer.getExternalStyle());
            }
            insert.execute();
        }
    }

    @Override
    public boolean existsById(Integer id) {
        return dsl.selectCount().from(MAPCONTEXT)
                .where(MAPCONTEXT.ID.eq(id))
                .fetchOne(0, Integer.class) > 0;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Integer create(MapContextDTO mapContext) {
        MapcontextRecord newRecord = dsl.newRecord(MAPCONTEXT);
        newRecord.from(convertToDAO(mapContext));
        newRecord.store();
        return newRecord.getId();
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
    public int delete(Integer id) {
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
        Collections.addAll(fields, MAPCONTEXT.fields());
        SelectJoinStep baseQuery = dsl.select(fields).from(MAPCONTEXT).leftOuterJoin(CSTL_USER).on(MAPCONTEXT.OWNER.eq(CSTL_USER.ID));
        SelectConnectByStep fquery = buildQuery(baseQuery, filterMap);
        
        // add sort
        Select query;
        if (sortEntry != null) {
            SortField f = null;
            if ("title".equals(sortEntry.getKey()) || "name".equals(sortEntry.getKey())) {
                f = "ASC".equals(sortEntry.getValue()) ? MAPCONTEXT.NAME.asc() : MAPCONTEXT.NAME.desc();
            } else if ("owner".equals(sortEntry.getKey())) {
                f = "ASC".equals(sortEntry.getValue()) ? CSTL_USER.LOGIN.asc() : CSTL_USER.LOGIN.desc();
            } else {
                throw new ConstellationPersistenceException("sort is not supported ont the parameter: " + sortEntry.getKey());
            }
            query = fquery.orderBy(f);
        } else {
            query = fquery;
        }

        final int count = dsl.fetchCount(query);
        final Map.Entry<Integer,List<MapContextDTO>> result = new AbstractMap.SimpleImmutableEntry<>(count,
                convertMCListToDto(((SelectLimitStep) query).limit(rowsPerPage).offset((pageNumber - 1) * rowsPerPage).fetchInto(Mapcontext.class)));
        return result;
    }

    @Override
    protected Condition buildSpecificCondition(String key, Object value) {
        if ("owner".equals(key)) {
            return MAPCONTEXT.OWNER.equal(castOrThrow(key, value, Integer.class));
        } else if ("term".equals(key)) {
            String term = castOrThrow(key, value, String.class);
            return MAPCONTEXT.NAME.likeIgnoreCase("%" + term  + "%")
               .or(MAPCONTEXT.DESCRIPTION.likeIgnoreCase("%" + term + "%"));
        }
        throw new ConstellationPersistenceException(key + " parameter is not supported.");
    }

    @Override
    public List<MapContextDTO> findAll(boolean full) {
        final Field[] fields = full ? MAPCONTEXT.fields() : REDUCED;
        return convertMCListToDto(dsl.select(fields).from(MAPCONTEXT).fetchInto(Mapcontext.class));
    }

    private AbstractMCLayerDTO convertToDto(MapcontextStyledLayer mcSl, boolean full) throws ConfigurationException {
        if (mcSl != null) {
            Layer layer = null;
            Data data = null;
            Style style = null;
            if (mcSl.getLayerId() != null) {
                layer = layerRepository.findById(mcSl.getLayerId());
                data = dataRepository.findById(layer.getDataId());
            } else if (mcSl.getDataId() != null) {
                data = dataRepository.findById(mcSl.getDataId());
            }
            if (mcSl.getStyleId() != null) {
                style = styleRepository.findById(mcSl.getStyleId());
            }
            
            if (layer != null) {
                String owner           = null;
                String dataType        = null;
                String styleName       = null;
                String serviceId       = null;
                Date creationDate      = null;
                 List<String> versions = null;
                if (full) {
                    owner = userRepository.findById(layer.getOwnerId())
                                                   .map(CstlUser::getLogin)
                                                   .orElse(null);
                    creationDate = layer.getDate();
                    dataType     = data != null ? data.getType() : null;
                    styleName    = style != null ? style.getName() : null;
                    final Service serv = serviceRepository.findById(layer.getService());
                    serviceId = serv.getIdentifier();
                    versions = Arrays.asList(serv.getVersions().split("µ"));
                }

                final QName layerName = layer.getAlias() != null ? new QName(layer.getAlias()) : layer.getName();
                return new InternalServiceMCLayerDTO(mcSl.getId(),
                                                    layerName,
                                                     mcSl.getLayerOrder(),
                                                     mcSl.getLayerOpacity(),
                                                     mcSl.getLayerVisible(),
                                                     mcSl.getLayerId(),
                                                     mcSl.getStyleId(),
                                                     styleName,
                                                     creationDate,
                                                     dataType,
                                                     owner,
                                                     data != null ? data.getId(): null,
                                                     serviceId,
                                                     versions);
            } else if (data != null) {
                String owner      = null;
                String dataType   = null;
                Date creationDate = null;
                String styleName  = null;
                if (full) {
                    owner = userRepository.findById(data.getOwnerId())
                                          .map(CstlUser::getLogin)
                                          .orElse(null);
                    dataType     = data.getType();
                    creationDate = data.getDate();
                    styleName    = style != null ? style.getName() : null;
                }
                return new DataMCLayerDTO(mcSl.getId(),
                                          new QName(data.getNamespace(), data.getName()),
                                          mcSl.getLayerOrder(),
                                          mcSl.getLayerOpacity(),
                                          mcSl.getLayerVisible(),
                                          creationDate,
                                          dataType,
                                          owner,
                                          mcSl.getDataId(),
                                          mcSl.getStyleId(),
                                          styleName);

            } else if (mcSl.getExternalLayer() != null) {
                QName layerName = new QName(mcSl.getExternalLayer());
                return new ExternalServiceMCLayerDTO(mcSl.getId(),
                                                     layerName,
                                                     mcSl.getLayerOrder(),
                                                     mcSl.getLayerOpacity(),
                                                     mcSl.getLayerVisible(),
                                                     null,
                                                     null,
                                                     null,
                                                     layerName,
                                                     mcSl.getExternalStyle(),
                                                     mcSl.getExternalServiceUrl(),
                                                     mcSl.getExternalServiceVersion(),
                                                     mcSl.getExternalLayerExtent());
            } else {
                throw new ConfigurationException("Unable to find a proper Mapcontext layer type");
            }
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