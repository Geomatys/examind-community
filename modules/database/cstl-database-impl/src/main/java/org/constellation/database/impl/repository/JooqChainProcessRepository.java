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

import java.util.ArrayList;
import static org.constellation.database.api.jooq.Tables.CHAIN_PROCESS;

import java.util.List;

import org.constellation.dto.process.ChainProcess;
import org.constellation.database.api.jooq.tables.records.ChainProcessRecord;
import org.constellation.repository.ChainProcessRepository;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component
@DependsOn("database-initer")
public class JooqChainProcessRepository extends AbstractJooqRespository<ChainProcessRecord, org.constellation.database.api.jooq.tables.pojos.ChainProcess> implements ChainProcessRepository {

    public JooqChainProcessRepository() {
        super(org.constellation.database.api.jooq.tables.pojos.ChainProcess.class, CHAIN_PROCESS);
    }

    @Override
    public List<ChainProcess> findAll() {
        return convertListToDto(dsl.select().from(CHAIN_PROCESS).fetchInto(org.constellation.database.api.jooq.tables.pojos.ChainProcess.class));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Integer create(ChainProcess chain) {
        ChainProcessRecord newRecord = dsl.newRecord(CHAIN_PROCESS);
        newRecord.from(convertToDao(chain));
        newRecord.store();
        return newRecord.into(ChainProcess.class).getId();
    }

    @Override
    public boolean existsById(Integer id) {
        return dsl.selectCount().from(CHAIN_PROCESS)
                .where(CHAIN_PROCESS.ID.eq(id))
                .fetchOne(0, Integer.class) > 0;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int delete(Integer id) {
        return dsl.delete(CHAIN_PROCESS).where(CHAIN_PROCESS.ID.eq(id)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int delete(String auth, String code) {
        return dsl.delete(CHAIN_PROCESS).where(CHAIN_PROCESS.AUTH.eq(auth)).and(CHAIN_PROCESS.CODE.eq(code)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int deleteAll() {
        return dsl.delete(CHAIN_PROCESS).execute();
    }

    @Override
    public ChainProcess findOne(String auth, String code) {
        return convertToDto(dsl.select()
                               .from(CHAIN_PROCESS)
                               .where(CHAIN_PROCESS.AUTH.eq(auth))
                               .and(CHAIN_PROCESS.CODE.eq(code))
                               .fetchOneInto(org.constellation.database.api.jooq.tables.pojos.ChainProcess.class));
    }

    @Override
    public Integer findId(String auth, String code) {
        return dsl.select(CHAIN_PROCESS.ID).from(CHAIN_PROCESS).where(CHAIN_PROCESS.AUTH.eq(auth)).and(CHAIN_PROCESS.CODE.eq(code)).fetchOneInto(Integer.class);
    }

    private List<ChainProcess> convertListToDto(List<org.constellation.database.api.jooq.tables.pojos.ChainProcess> daos) {
        List<ChainProcess> results = new ArrayList<>();
        for (org.constellation.database.api.jooq.tables.pojos.ChainProcess dao : daos) {
            results.add(convertToDto(dao));
        }
        return results;
    }

    private ChainProcess convertToDto(org.constellation.database.api.jooq.tables.pojos.ChainProcess dao) {
        if (dao != null) {
            ChainProcess dto = new ChainProcess();
            dto.setAuth(dao.getAuth());
            dto.setCode(dao.getCode());
            dto.setConfig(dao.getConfig());
            dto.setId(dao.getId());
            return dto;
        }
        return null;
    }

    private org.constellation.database.api.jooq.tables.pojos.ChainProcess convertToDao(ChainProcess dto) {
        if (dto != null) {
            org.constellation.database.api.jooq.tables.pojos.ChainProcess dao = new org.constellation.database.api.jooq.tables.pojos.ChainProcess();
            dao.setAuth(dto.getAuth());
            dao.setCode(dto.getCode());
            dao.setConfig(dto.getConfig());
            dao.setId(dto.getId());
            return dao;
        }
        return null;
    }
}
