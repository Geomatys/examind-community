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

import org.constellation.dto.CstlUser;
import org.constellation.repository.UserRepository;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class UserRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    public void all() {
        dump(userRepository.findAll());
    }

    @Test
    @Transactional()
    public void crude() throws Throwable {

        Optional<CstlUser> previous = userRepository.findByEmail(TestSamples.newAdminUser().getEmail());
        if (previous.isPresent()) {
            userRepository.delete(previous.get().getId());
        }

        Integer uid = userRepository.create(TestSamples.newAdminUser());
        Assert.assertNotNull(uid);

        Assert.assertEquals("Should have deleled 1 record",1, userRepository.delete(uid));

        Assert.assertFalse(userRepository.findById(uid).isPresent());

    }

}
