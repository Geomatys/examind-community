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

import java.util.Arrays;
import java.util.List;
import org.constellation.dto.CstlUser;
import org.constellation.repository.UserRepository;
import java.util.Optional;
import org.constellation.dto.UserWithRole;
import org.constellation.repository.DataRepository;
import org.constellation.repository.DatasetRepository;
import org.constellation.repository.LayerRepository;
import org.constellation.repository.MapContextRepository;
import org.constellation.repository.ProviderRepository;
import org.constellation.repository.ServiceRepository;
import org.constellation.repository.TaskParameterRepository;
import org.constellation.repository.TaskRepository;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class UserRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MapContextRepository mapcontextRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private LayerRepository layerRepository;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private DataRepository dataRepository;

    @Autowired
    private DatasetRepository datasetRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskParameterRepository taskParamRepository;

    @Test
    @Transactional()
    public void crude() throws Throwable {
        // big cleanup
        taskRepository.deleteAll();
        taskParamRepository.deleteAll();
        mapcontextRepository.deleteAll();
        dataRepository.deleteAll();
        providerRepository.deleteAll();
        datasetRepository.deleteAll();
        layerRepository.deleteAll();
        serviceRepository.deleteAll();
        userRepository.deleteAll();
        
        List<CstlUser> all = userRepository.findAll();
        Assert.assertTrue(all.isEmpty());
        
        Integer uid1 = userRepository.create(TestSamples.newAdminUser());
        Assert.assertNotNull(uid1);

        UserWithRole u1 = userRepository.findOneWithRole(uid1).get();
        Assert.assertNotNull(u1);

        Integer uid2 = userRepository.create(TestSamples.newDataUser());
        Assert.assertNotNull(uid2);

        UserWithRole u2 = userRepository.findOneWithRole(uid2).get();
        Assert.assertNotNull(u2);

        Integer uid3 = userRepository.create(TestSamples.newQuoteUser());
        Assert.assertNotNull(uid3);

        UserWithRole u3 = userRepository.findOneWithRole(uid3).get();
        Assert.assertNotNull(u3);


        /**
         * Search
         */

        CstlUser u = userRepository.findByEmail("pedro.'ramirez@gmail.';com").get();
        Assert.assertNotNull(u);
        Assert.assertEquals(uid3, u.getId());

        u = userRepository.findByForgotPasswordUuid("uu'id").get();
        Assert.assertNotNull(u);
        Assert.assertEquals(uid3, u.getId());

        u = userRepository.findOne("pe''dra").get();
        Assert.assertNotNull(u);
        Assert.assertEquals(uid3, u.getId());

        u = userRepository.findOneWithRole("pe''dra").get();
        Assert.assertNotNull(u);
        Assert.assertEquals(uid3, u.getId());

        u = userRepository.findOneWithRoleByMail("pedro.'ramirez@gmail.';com").get();
        Assert.assertNotNull(u);
        Assert.assertEquals(uid3, u.getId());

        long count = userRepository.countUser();
        Assert.assertEquals(count, 3);

        List<String> roles = userRepository.getRoles(uid3);
        Assert.assertEquals(2, roles.size());
        Assert.assertTrue(roles.contains("d'ata"));
        Assert.assertTrue(roles.contains("publishe'r"));

        Assert.assertFalse(userRepository.loginAvailable("pe''dra"));
        Assert.assertTrue(userRepository.loginAvailable("pedrolinette"));
        
        String search = "pe''d";

        List<UserWithRole> allwr = userRepository.search(search, 10, 1, null, null, null);
        Assert.assertEquals(1, allwr.size());
        Assert.assertEquals(uid3, allwr.get(0).getId());

        count = userRepository.searchCount(search);
        Assert.assertEquals(1, count);

        search = "pe";
        allwr = userRepository.search(search, 10, 1, null, null, null);
        Assert.assertEquals(2, allwr.size());

        count = userRepository.searchCount(search);
        Assert.assertEquals(2, count);

        // field filter
        allwr = userRepository.search(null, 10, 1, null, null, Arrays.asList("cstl_user.login"));
        Assert.assertEquals(3, allwr.size());
        Assert.assertNotNull(allwr.get(0));
        UserWithRole filtered = allwr.get(0);
        Assert.assertNotNull(filtered.getLogin());
        Assert.assertNull(filtered.getFirstname());
        Assert.assertNull(filtered.getLastname());

        /**
         * Update
         */
        userRepository.activate(uid3);
        allwr = userRepository.findActivesWithRole();
        Assert.assertEquals(3, allwr.size());

        userRepository.desactivate(uid3);
        allwr = userRepository.findActivesWithRole();
        Assert.assertEquals(2, allwr.size());

        /**
         * Deletion
         */
        Assert.assertEquals("Should have deleled 1 record",1, userRepository.delete(uid1));
        Assert.assertFalse(userRepository.findById(uid1).isPresent());

        userRepository.deleteAll();

    }

}
