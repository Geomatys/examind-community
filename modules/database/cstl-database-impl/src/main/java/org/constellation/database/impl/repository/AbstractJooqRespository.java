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

import org.jooq.DSLContext;
import org.jooq.Record;
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
}
