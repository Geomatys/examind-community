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

import java.util.AbstractMap;
import java.util.HashMap;
import static org.constellation.database.api.jooq.Tables.PROPERTY;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.constellation.database.api.jooq.Tables;
import org.constellation.database.api.jooq.tables.pojos.Property;
import org.constellation.database.api.jooq.tables.records.PropertyRecord;
import org.constellation.repository.PropertyRepository;
import org.jooq.DeleteConditionStep;
import org.jooq.Record1;
import org.jooq.SelectQuery;
import org.jooq.UpdateConditionStep;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@DependsOn("database-initer")
public class JooqPropertiesRepository extends AbstractJooqRespository<PropertyRecord, Property> implements
        PropertyRepository {


    public JooqPropertiesRepository() {
        super(Property.class, PROPERTY);
    }


    @Override
    public Entry<String, String> findOne(String key) {
        SelectQuery<PropertyRecord> selectQuery = dsl.selectQuery(PROPERTY);
        selectQuery.addConditions(PROPERTY.NAME.equal(key));
        PropertyRecord fetchOne = dsl.fetchOne(selectQuery);
        if (fetchOne != null) {
            return new AbstractMap.SimpleEntry<>(fetchOne.getName(), fetchOne.getValue());
        }
        return null;
    }

    @Override
    public Map<String, String> findIn(List<String> keys) {
        SelectQuery<PropertyRecord> selectQuery = dsl.selectQuery(PROPERTY);
        selectQuery.addConditions(PROPERTY.NAME.in(keys));
        return toMap(mapResult(selectQuery));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void update(String key, String value) {
        final Entry old = findOne(key);
        if (old == null) {
            PropertyRecord newRecord = dsl.newRecord(PROPERTY, new Property(key, value));
            newRecord.store();
        } else {
            final UpdateConditionStep<PropertyRecord> updateQuery = dsl.update(PROPERTY)
                    .set(PROPERTY.VALUE, value)
                    .where(PROPERTY.NAME.eq(key));

            updateQuery.execute();
        }
    }

    @Override
    public Map<String, String> startWith(String string) {
        SelectQuery<PropertyRecord> selectQuery = dsl.selectQuery(Tables.PROPERTY);
        selectQuery.addConditions(Tables.PROPERTY.NAME.like((string)));
        return toMap(selectQuery.fetch().into(Property.class));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void delete(String key) {
        DeleteConditionStep<PropertyRecord> deleteConditionStep = dsl.delete(PROPERTY).where(
                PROPERTY.NAME.eq(key));
        deleteConditionStep.execute();

    }

    @Override
    public String getValue(String key, String defaultValue) {
        Record1<String> fetchOne = dsl.select(PROPERTY.VALUE).from(PROPERTY).where(PROPERTY.NAME.eq(key)).fetchOne();
        if (fetchOne == null)
            return defaultValue;
        return fetchOne.value1();
    }

    @Override
    public Map<String, String> findAll() {
        return toMap(dsl.select().from(PROPERTY).fetchInto(Property.class));
    }

    private Map<String, String> toMap(List<? extends Property> properties) {
        Map<String, String> results = new HashMap<>();
        for (Property prop : properties) {
            results.put(prop.getName(), prop.getValue());
        }
        return results;
    }
}
