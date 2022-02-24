/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com/
 *
 * Copyright 2015 Geomatys.
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
import org.constellation.dto.Role;
import com.examind.database.api.jooq.tables.records.RoleRecord;
import org.constellation.repository.RoleRepository;
import org.springframework.stereotype.Component;


import static com.examind.database.api.jooq.Tables.ROLE;
import org.springframework.context.annotation.DependsOn;

/**
 * @author Laurent Tisseyre
 */
@Component("cstlRoleRepository")
@DependsOn("database-initer")
public class JooqRoleRepository extends AbstractJooqRespository<RoleRecord, Role> implements RoleRepository{

    public JooqRoleRepository() {
        super(Role.class, ROLE);
    }

    @Override
    public List<Role> findAll() {
        return dsl.select().from(ROLE).fetchInto(Role.class);
    }
}
