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
package com.examind.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.constellation.dto.CstlUser;
import org.constellation.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class FileSystemUserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        Optional<CstlUser> userO = userRepository.findOne(login);
        if (userO.isPresent()) {
            CstlUser user = userO.get();

            List<String> roles = userRepository.getRoles(user.getId());
            List<GrantedAuthority> grantedAuths = new ArrayList<>();
            for (String role : roles) {
                grantedAuths.add(new SimpleGrantedAuthority(role));
            }

            return new User(login, user.getPassword(), grantedAuths);
        }
        throw new UsernameNotFoundException(login + " user not found");
    }

}
