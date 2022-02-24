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

import java.util.List;
import java.util.Map;
import org.constellation.exception.ConstellationPersistenceException;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SelectConditionStep;
import org.jooq.SelectConnectByStep;
import org.jooq.SelectJoinStep;
import org.jooq.TableLike;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;


public abstract class AbstractJooqRespository<T extends Record, U> {

    @Autowired
    @Qualifier("dsl") // use qualifier to prevent multiple DSL conflict
    DSLContext dsl;

    private Class<U> dtoClass;

    private TableLike<T> table;

    public AbstractJooqRespository(Class<U> dtoClass, TableLike<T> table) {
        this.dtoClass = dtoClass;
        this.table = table;
    }

    /**
     * Select count of table and return the result.
     * @return count of rows in table
     */
    public Integer countAll() {
        return dsl.selectCount().from(table).fetchOne(0,int.class);
    }

    /**
     * Tranform recursively a Filter map entry into a JOOQ SQL condition.
     *
     * @param key name of the parameter.
     * @param value parameter value.
     *
     * @return A JOOQ SQL condition (ALways not {@code null}).
     */
    protected Condition buildCondition(String key, Object value) {
        if ("OR".equals(key) || "AND".equals(key)) {
            boolean and = "AND".equals(key);
            if (value instanceof List) {
                List<Map.Entry<String, Object>> values =  (List<Map.Entry<String, Object>>) value;
                if (!values.isEmpty()) {
                    Condition c = null;
                    for (Map.Entry<String, Object> e: values) {
                        Condition c2 = buildCondition(e.getKey(), e.getValue());
                        if (c == null) {
                            c = c2;
                        } else {
                            if (and) c = c.and(c2); else c = c.or(c2);
                        }
                    }
                    return c;
                } else {
                    throw new ConstellationPersistenceException("logical filter parameter require a non empty List");
                }
            } else {
                throw new ConstellationPersistenceException("logical filter parameter require a List");
            }
        }
        return buildSpecificCondition(key, value);
    }

    /**
     * Treat a specifical filter condition and transform it into a JOOQ condition.
     *
     * @param key name of the parameter.
     * @param value parameter value.
     *
     * @return A JOOQ SQL condition (ALways not {@code null}).
     */
    protected Condition buildSpecificCondition(String key, Object value) {
        throw new UnsupportedOperationException("This repository does not support condition filter");
    }

    /**
     * Try to cast the specified value into the expected class.
     * If not possible, throw a ConstellationPersistenceException.
     *
     * @param <A> The expected class.
     * @param field The field name (used for exception message)
     * @param value The value to cast.
     * @param expectedClass The expected class.
     *
     * @return A casted object.
     */
    protected static <A> A castOrThrow(String field, Object value, Class<A> expectedClass) {
        if (!expectedClass.isInstance(value)) {
            throw new ConstellationPersistenceException(field + " parameter must be of type: " + expectedClass.getSimpleName());
        }
        return (A) value;
    }

    /**
     * Add the SQL condition to a query.
     * if specified filterQuery is null, then a "WHERE" clause will be added to the base query.
     * If not, a "AND" query will be added to the filterQuery.
     *
     * @param baseQuery Basic "select from" query.
     * @param filterQuery Filtered query (meaning a where as already been set).
     * @param condition An SQL condition.
     * 
     * @return A filtered query including the specified condition.
     */
    protected static SelectConditionStep addCondition(SelectJoinStep baseQuery, SelectConditionStep filterQuery, Condition condition) {
        return (filterQuery == null) ?  baseQuery.where(condition) : filterQuery.and(condition);
    }

    protected SelectConnectByStep buildQuery(SelectJoinStep baseQuery, Map<String, Object> filterMap) {
        SelectConnectByStep query;
        SelectConditionStep filteredQuery = null;
        if (filterMap != null && !filterMap.isEmpty()) {
            for (final Map.Entry<String, Object> entry : filterMap.entrySet()) {
                final Condition cond = buildCondition(entry.getKey(), entry.getValue());
                filteredQuery = addCondition(baseQuery, filteredQuery, cond);
            }
            query = filteredQuery;
        } else {
            query = baseQuery;
        }
        return query;
    }

    protected SelectConditionStep buildQuery(SelectConditionStep filterQuery, Map<String, Object> filterMap) {
        if (filterMap != null) {
            for (final Map.Entry<String, Object> entry : filterMap.entrySet()) {
                filterQuery = filterQuery.and(buildCondition(entry.getKey(), entry.getValue()));
            }
        }
        return filterQuery;
    }
}
