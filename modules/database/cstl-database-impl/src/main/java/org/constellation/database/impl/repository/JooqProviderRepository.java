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
import static com.examind.database.api.jooq.Tables.DATA;
import static com.examind.database.api.jooq.Tables.PROVIDER;

import java.util.List;
import static com.examind.database.api.jooq.Tables.INTERNAL_METADATA;
import static com.examind.database.api.jooq.Tables.METADATA;
import static com.examind.database.api.jooq.Tables.PROVIDER_X_SOS;
import static com.examind.database.api.jooq.Tables.PROVIDER_X_CSW;
import static com.examind.database.api.jooq.Tables.SENSOR;

import com.examind.database.api.jooq.tables.pojos.Provider;
import com.examind.database.api.jooq.tables.records.ProviderRecord;
import org.constellation.repository.ProviderRepository;
import org.constellation.dto.ProviderBrief;
import org.jooq.UpdateConditionStep;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@DependsOn("database-initer")
public class JooqProviderRepository extends AbstractJooqRespository<ProviderRecord, Provider> implements
        ProviderRepository {

    private final com.examind.database.api.jooq.tables.Provider provider = PROVIDER.as("p");

    private final com.examind.database.api.jooq.tables.Data data = DATA.as("d");

    public JooqProviderRepository() {
        super(Provider.class, PROVIDER);
    }

    @Override
    public ProviderBrief findOne(Integer id) {
        return convertToDto(dsl.select().from(PROVIDER).where(PROVIDER.ID.eq(id)).fetchOneInto(Provider.class));
    }

    @Override
    public boolean existsById(Integer id) {
        return dsl.selectCount().from(PROVIDER)
                .where(PROVIDER.ID.eq(id))
                .fetchOne(0, Integer.class) > 0;
    }

    @Override
    public ProviderBrief findForData(Integer dataId) {
        return convertToDto(dsl.select(PROVIDER.fields()).from(PROVIDER, DATA).where(PROVIDER.ID.eq(DATA.PROVIDER)).and(DATA.ID.eq(dataId)).fetchOneInto(Provider.class));
    }

    @Override
    public List<ProviderBrief> findByImpl(String impl) {
        return convertListToDto(dsl.select().from(PROVIDER).where(PROVIDER.IMPL.eq(impl)).fetch().into(Provider.class));
    }

    @Override
    public List<String> getProviderIds() {
        return dsl.select(PROVIDER.IDENTIFIER).from(PROVIDER).fetch(PROVIDER.IDENTIFIER);
    }

    @Override
    public ProviderBrief findByIdentifier(String identifier) {
        return convertToDto(dsl.select().from(PROVIDER).where(PROVIDER.IDENTIFIER.eq(identifier)).fetchOneInto(Provider.class));
    }

    @Override
    public Integer findIdForIdentifier(String identifier) {
        return dsl.select(PROVIDER.ID).from(PROVIDER).where(PROVIDER.IDENTIFIER.eq(identifier)).fetchOneInto(Integer.class);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Integer create(ProviderBrief provider) {
        ProviderRecord newRecord = dsl.newRecord(PROVIDER);
        newRecord.setConfig(provider.getConfig());
        newRecord.setIdentifier(provider.getIdentifier());
        newRecord.setImpl(provider.getImpl());
        newRecord.setOwner(provider.getOwner());
        newRecord.setType(provider.getType());
        newRecord.store();
        return newRecord.into(Provider.class).getId();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int delete(Integer id) {
        List<Integer> metadIds = dsl.select(METADATA.ID).from(METADATA).where(METADATA.PROVIDER_ID.eq(id)).fetchInto(Integer.class);
        for (Integer metadId : metadIds) {
            dsl.delete(INTERNAL_METADATA).where(INTERNAL_METADATA.ID.eq(metadId)).execute();
        }
        dsl.delete(METADATA).where(METADATA.PROVIDER_ID.eq(id)).execute();
        dsl.delete(SENSOR).where(SENSOR.PROVIDER_ID.eq(id)).execute();
        dsl.delete(PROVIDER_X_SOS).where(PROVIDER_X_SOS.PROVIDER_ID.eq(id)).execute();
        dsl.delete(PROVIDER_X_CSW).where(PROVIDER_X_CSW.PROVIDER_ID.eq(id)).execute();
        return dsl.delete(PROVIDER).where(PROVIDER.ID.eq(id)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int deleteAll() {
        List<Integer> ids = getAllIds();
        int i = 0;
        for (Integer id : ids) {
            i = i + delete(id);
        }
        return i;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int deleteByIdentifier(String providerID) {
        return dsl.delete(PROVIDER).where(PROVIDER.IDENTIFIER.eq(providerID)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int update(ProviderBrief provider) {
        ProviderRecord providerRecord = new ProviderRecord();
        providerRecord.from(provider);
        UpdateConditionStep<ProviderRecord> set = dsl.update(PROVIDER)
                .set(PROVIDER.CONFIG, provider.getConfig())
                .set(PROVIDER.IDENTIFIER, provider.getIdentifier())
                .set(PROVIDER.IMPL, provider.getImpl())
                .set(PROVIDER.OWNER, provider.getOwner())
                .set(PROVIDER.TYPE, provider.getType())
                .where(PROVIDER.ID.eq(provider.getId()));

        return set.execute();
    }

    @Override
    public int removeLinkedServices(int providerID) {
        return dsl.delete(PROVIDER_X_SOS).where(PROVIDER_X_SOS.PROVIDER_ID.eq(providerID)).execute();
    }

    @Override
    public List<Integer> getAllIds() {
        return dsl.select(PROVIDER.ID).from(PROVIDER).fetchInto(Integer.class);
    }

    @Override
    public List<ProviderBrief> findAll() {
        return convertListToDto(dsl.select().from(PROVIDER).fetchInto(Provider.class));
    }

    private List<ProviderBrief> convertListToDto(List<Provider> daos) {
        List<ProviderBrief> dtos = new ArrayList<>();
        for (Provider dao : daos) {
            dtos.add(convertToDto(dao));
        }
        return dtos;
    }

    private ProviderBrief convertToDto(Provider dao) {
        if (dao != null) {
            ProviderBrief p = new ProviderBrief();
            p.setConfig(dao.getConfig());
            p.setId(dao.getId());
            p.setIdentifier(dao.getIdentifier());
            p.setImpl(dao.getImpl());
            p.setOwner(dao.getOwner());
            p.setType(dao.getType());
            return p;
        }
        return null;
    }
}
