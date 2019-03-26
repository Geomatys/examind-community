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
import static org.constellation.database.api.jooq.Tables.DATA;
import static org.constellation.database.api.jooq.Tables.LAYER;
import static org.constellation.database.api.jooq.Tables.PROVIDER;
import static org.constellation.database.api.jooq.Tables.SERVICE;
import static org.constellation.database.api.jooq.Tables.STYLE;

import java.util.List;
import static org.constellation.database.api.jooq.Tables.PROVIDER_X_SOS;
import static org.constellation.database.api.jooq.Tables.PROVIDER_X_CSW;

import org.constellation.dto.Data;
import org.constellation.database.api.jooq.tables.pojos.Provider;
import org.constellation.dto.Style;
import org.constellation.database.api.jooq.tables.records.ProviderRecord;
import org.constellation.repository.ProviderRepository;
import static org.constellation.database.impl.repository.JooqDataRepository.convertDataListToDto;
import static org.constellation.database.impl.repository.JooqStyleRepository.convertStyleListToDto;
import org.constellation.dto.ProviderBrief;
import org.jooq.SelectConditionStep;
import org.jooq.UpdateConditionStep;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@DependsOn("database-initer")
public class JooqProviderRepository extends AbstractJooqRespository<ProviderRecord, Provider> implements
        ProviderRepository {

    private final org.constellation.database.api.jooq.tables.Provider provider = PROVIDER.as("p");

    private final org.constellation.database.api.jooq.tables.Data data = DATA.as("d");

    public JooqProviderRepository() {
        super(Provider.class, PROVIDER);
    }

    @Override
    public ProviderBrief findOne(Integer id) {
        return convertToDto(dsl.select().from(PROVIDER).where(PROVIDER.ID.eq(id)).fetchOneInto(Provider.class));
    }

    @Override
    public boolean existById(Integer id) {
        return dsl.selectCount().from(PROVIDER)
                .where(PROVIDER.ID.eq(id))
                .fetchOne(0, Integer.class) > 0;
    }

    @Override
    public ProviderBrief findForData(Integer dataId) {
        return convertToDto(dsl.select().from(PROVIDER, DATA).where(PROVIDER.ID.eq(DATA.PROVIDER)).and(DATA.ID.eq(dataId)).fetchOneInto(Provider.class));
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
    public ProviderBrief getProviderParentIdOfLayer(String serviceType, String serviceId, String layerid) {
        return convertToDto(dsl.select(provider.fields()).from(provider).join(data).on(data.PROVIDER.eq(provider.ID)).join(LAYER)
                .on(LAYER.DATA.eq(data.ID)).join(SERVICE).on(SERVICE.ID.eq(LAYER.SERVICE))
                .where(LAYER.NAME.eq(layerid).and(SERVICE.IDENTIFIER.eq(serviceId)).and(SERVICE.TYPE.eq(serviceType)))
                .fetchOneInto(Provider.class));
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
        newRecord.setParent(provider.getParent());
        newRecord.store();
        return newRecord.into(Provider.class).getId();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int delete(int id) {
        dsl.delete(PROVIDER_X_SOS).where(PROVIDER_X_SOS.PROVIDER_ID.eq(id)).execute();
        dsl.delete(PROVIDER_X_CSW).where(PROVIDER_X_CSW.PROVIDER_ID.eq(id)).execute();
        return dsl.delete(PROVIDER).where(PROVIDER.ID.eq(id)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int deleteByIdentifier(String providerID) {
        return dsl.delete(PROVIDER).where(PROVIDER.IDENTIFIER.eq(providerID)).execute();
    }

    @Override
    public List<ProviderBrief> findChildren(String id) {
        return convertListToDto(dsl.select().from(PROVIDER).where(PROVIDER.PARENT.eq(id)).fetchInto(Provider.class));
    }

    @Override
    public List<Data> findDatasByProviderId(Integer id) {
        return convertDataListToDto(dsl.select(DATA.fields()).from(DATA).join(PROVIDER).on(DATA.PROVIDER.eq(PROVIDER.ID))
                .where(PROVIDER.ID.eq(id)).fetchInto(org.constellation.database.api.jooq.tables.pojos.Data.class));
    }

    @Override
    public List<Integer> findDataIdsByProviderId(Integer id) {
        return dsl.select(DATA.ID).from(DATA).join(PROVIDER).on(DATA.PROVIDER.eq(PROVIDER.ID))
                .where(PROVIDER.ID.eq(id)).fetchInto(Integer.class);
    }

    @Override
    public List<Data> findDatasByProviderId(Integer id, String dataType) {
        return convertDataListToDto(dsl.select(DATA.fields()).from(DATA).join(PROVIDER).on(DATA.PROVIDER.eq(PROVIDER.ID))
                .where(PROVIDER.ID.eq(id)).and(DATA.TYPE.eq(dataType)).fetchInto(org.constellation.database.api.jooq.tables.pojos.Data.class));
    }


    @Override
    public List<Data> findDatasByProviderId(Integer id, String dataType, boolean included, boolean hidden) {
        SelectConditionStep c = dsl.select(DATA.fields()).from(DATA).join(PROVIDER).on(DATA.PROVIDER.eq(PROVIDER.ID))
                .where(PROVIDER.ID.eq(id)).and(DATA.INCLUDED.eq(included)).and(DATA.HIDDEN.eq(hidden));

        if (dataType != null) {
            c = c.and(DATA.TYPE.eq(dataType));
        }
        return convertDataListToDto(c.fetchInto(org.constellation.database.api.jooq.tables.pojos.Data.class));
    }

    @Override
    public List<Integer> findDataIdsByProviderId(Integer id, String dataType, boolean included, boolean hidden) {
        SelectConditionStep c = dsl.select(DATA.ID).from(DATA).join(PROVIDER).on(DATA.PROVIDER.eq(PROVIDER.ID))
                .where(PROVIDER.ID.eq(id)).and(DATA.INCLUDED.eq(included)).and(DATA.HIDDEN.eq(hidden));

        if (dataType != null) {
            c = c.and(DATA.TYPE.eq(dataType));
        }
        return c.fetchInto(Integer.class);
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
                .set(PROVIDER.PARENT, provider.getParent())
                .set(PROVIDER.TYPE, provider.getType())
                .where(PROVIDER.ID.eq(provider.getId()));

        return set.execute();

    }

    @Override
    public List<Style> findStylesByProviderId(Integer providerId) {
        return convertStyleListToDto(dsl.select().from(STYLE).join(PROVIDER).on(STYLE.PROVIDER.eq(PROVIDER.ID))
                .where(PROVIDER.ID.eq(providerId)).fetchInto(org.constellation.database.api.jooq.tables.pojos.Style.class));
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
    public List<Integer> getAllIdsWithNoParent() {
        return dsl.select(PROVIDER.ID).from(PROVIDER).where(PROVIDER.PARENT.isNull()).fetchInto(Integer.class);
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
            p.setParent(dao.getParent());
            p.setType(dao.getType());
            return p;
        }
        return null;
    }
}
