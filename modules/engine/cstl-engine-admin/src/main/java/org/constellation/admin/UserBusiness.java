/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2018 Geomatys.
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
package org.constellation.admin;

import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import org.constellation.business.IUserBusiness;
import org.constellation.dto.CstlUser;
import org.constellation.dto.UserWithRole;
import org.constellation.repository.UserRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component("cstlUserBusiness")
@Primary
public class UserBusiness implements IUserBusiness {

    @Inject
    UserRepository userRepository;

    @Override
    public List<CstlUser> findAll() {
        return userRepository.findAll();
    }

    @Override
    public CstlUser create(CstlUser user) {
        return userRepository.create(user);
    }

    @Override
    public CstlUser update(CstlUser user) {
        return userRepository.update(user);
    }

    @Override
    public void addUserToRole(Integer userId, String roleName) {
        userRepository.addUserToRole(userId, roleName);
    }

    @Override
    public int delete(int userId) {
        return userRepository.delete(userId);
    }

    @Override
    public int desactivate(int userId) {
        return userRepository.desactivate(userId);
    }

    @Override
    public int activate(int userId) {
        return userRepository.activate(userId);
    }

    @Override
    public boolean isLastAdmin(int userId) {
        return userRepository.isLastAdmin(userId);
    }

    @Override
    public Optional<CstlUser> findOne(String login) {
        return userRepository.findOne(login);
    }

    @Override
    public Optional<CstlUser> findById(Integer id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<CstlUser> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public Optional<CstlUser> findByForgotPasswordUuid(String uuid) {
        return userRepository.findByForgotPasswordUuid(uuid);
    }

    @Override
    public List<String> getRoles(int userId) {
        return userRepository.getRoles(userId);
    }

    @Override
    public int countUser() {
        return userRepository.countUser();
    }

    @Override
    public boolean loginAvailable(String login) {
        return userRepository.loginAvailable(login);
    }

    @Override
    public Optional<UserWithRole> findOneWithRole(Integer id) {
        return userRepository.findOneWithRole(id);
    }

    @Override
    public Optional<UserWithRole> findOneWithRole(String name) {
        return userRepository.findOneWithRole(name);
    }

    @Override
    public List<UserWithRole> findActivesWithRole() {
        return userRepository.findActivesWithRole();
    }

    @Override
    public List<UserWithRole> search(String search, int size, int page, String sortFieldName, String order) {
        return userRepository.search(search, size, page, sortFieldName, order);
    }

    @Override
    public long searchCount(String search) {
        return userRepository.searchCount(search);
    }

}
