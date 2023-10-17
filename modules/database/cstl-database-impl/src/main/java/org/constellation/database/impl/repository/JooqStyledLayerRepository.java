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

import static com.examind.database.api.jooq.Tables.STYLED_LAYER;

import java.util.List;

import com.examind.database.api.jooq.tables.pojos.StyledLayer;
import com.examind.database.api.jooq.tables.records.StyledLayerRecord;
import org.constellation.repository.StyledLayerRepository;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


@Component
@DependsOn("database-initer")
public class JooqStyledLayerRepository extends AbstractJooqRespository<StyledLayerRecord, StyledLayer> implements StyledLayerRepository {
    public JooqStyledLayerRepository() {
        super(StyledLayer.class, STYLED_LAYER);
    }

    @Override
    public List<Integer> findByLayer(int layerId) {
        return dsl.select(STYLED_LAYER.STYLE).from(STYLED_LAYER).where(STYLED_LAYER.LAYER.eq(layerId)).fetchInto(Integer.class);
    }

    @Override
    public List<org.constellation.dto.StyledLayer> findByStyleId(int styleId) {
        return dsl.select(STYLED_LAYER).from(STYLED_LAYER).where(STYLED_LAYER.STYLE.eq(styleId)).fetchInto(org.constellation.dto.StyledLayer.class);
    }

    @Override
    public org.constellation.dto.StyledLayer findByStyleAndLayer(int styleId, int layerId) {
        return dsl.select(STYLED_LAYER)
                .from(STYLED_LAYER)
                .where(STYLED_LAYER.LAYER.eq(layerId)
                        .and(STYLED_LAYER.STYLE.eq(styleId)))
                .fetchOneInto(org.constellation.dto.StyledLayer.class);
    }

    @Override
    public List<org.constellation.dto.StyledLayer> findStatisticLess() {
        return dsl.select(STYLED_LAYER)
                .from(STYLED_LAYER)
                .where((STYLED_LAYER.EXTRA_INFO.isNull().or(STYLED_LAYER.EXTRA_INFO.equal("")))
                        .and(STYLED_LAYER.ACTIVATE_STATS.eq(true)))
                .fetchInto(org.constellation.dto.StyledLayer.class);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void updateStatistics(final int styleId, final int layerId, final String statsResult, final String statsState) {
        dsl.update(STYLED_LAYER)
                .set(STYLED_LAYER.EXTRA_INFO, statsResult)
                .set(STYLED_LAYER.STATS_STATE, statsState)
                .where(STYLED_LAYER.STYLE.eq(styleId)
                .and(STYLED_LAYER.LAYER.eq(layerId)))
                .execute();
    }

    @Override
    public void updateActivateStats(final int styleId, final int layerId, final boolean activateStats) {
        dsl.update(STYLED_LAYER)
                .set(STYLED_LAYER.ACTIVATE_STATS, activateStats)
                .set(STYLED_LAYER.STATS_STATE, (String) null)
                .set(STYLED_LAYER.EXTRA_INFO, (String) null)
                .where(STYLED_LAYER.STYLE.eq(styleId))
                .and(STYLED_LAYER.LAYER.eq(layerId))
                .execute();
    }

    @Override
    public boolean getActivateStats(final int styleId, final int layerId) {
        return dsl.select(STYLED_LAYER.ACTIVATE_STATS)
                .from(STYLED_LAYER)
                .where(STYLED_LAYER.STYLE.equal(styleId))
                .and(STYLED_LAYER.LAYER.equal(layerId))
                .fetchOneInto(Boolean.class);
    }
}
