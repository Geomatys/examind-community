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
package com.examind.repository;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.logging.Logger;
import org.constellation.dto.CstlUser;
import org.constellation.dto.UserWithRole;
import org.constellation.repository.UserRepository;

public abstract class AbstractRepositoryTest {

    protected final static Logger LOGGER = Logger.getLogger("com.examind.repository");

    @Autowired
    DSLContext create;

    @Autowired
    protected UserRepository userRepository;

    protected CstlUser getOrCreateUser() {
        return getOrCreateUser(TestSamples.newAdminUser());
    }

    protected CstlUser getOrCreateUser(UserWithRole user) {
        return userRepository.findByEmail(user.getEmail())
                             .orElse(userRepository.findById(userRepository.create(user)).get());
    }

    protected void dump(List<?> findAll) {
        for (Object property : findAll) {
                LOGGER.finer(property.toString());
        }

    }

    protected void dump(Result<Record> o) {
        if(o != null)
            LOGGER.finer(o.toString());

    }

    protected void dump(Object o) {
        if(o != null)
            LOGGER.finer(o.toString());

    }

}
