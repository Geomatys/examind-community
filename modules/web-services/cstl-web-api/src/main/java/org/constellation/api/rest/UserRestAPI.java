/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2016 Geomatys.
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
package org.constellation.api.rest;

import java.util.Collections;
import java.util.Optional;
import java.util.logging.Level;
import static org.constellation.api.rest.AbstractRestAPI.LOGGER;
import org.constellation.dto.Page;
import org.constellation.dto.PagedSearch;
import org.constellation.dto.Sort;
import org.constellation.dto.UserWithRole;
import org.constellation.dto.CstlUser;
import org.springframework.http.HttpStatus;
import static org.springframework.http.HttpStatus.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import static org.springframework.web.bind.annotation.RequestMethod.*;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
@RestController()
public class UserRestAPI extends AbstractRestAPI {

    /**
     *
     * @param page start page index
     * @param limit number of result per page
     * @param sort sort values, example : 'name+ASC'
     * @param q simple term search term query, example: geomatys
     * @return ResponseEntity never null
     */
    @RequestMapping(value="/users", method=GET, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getUsers(@RequestParam(value="page",required=false) Integer page,
                                   @RequestParam(value="limit",required=false) Integer limit,
                                   @RequestParam(value="sort",required=false) String sort,
                                   @RequestParam(value="q",required=false) String q
                                   ){
        final PagedSearch search = new PagedSearch();
        search.setSize(limit==null ? 50 : limit);
        search.setPage(page==null ? 0 : page);

        if (sort!=null && !sort.isEmpty()) {
            final String[] parts = sort.split("\\+");
            final Sort sor = new Sort();
            sor.setField(parts[0]);
            sor.setOrder("ASC".equalsIgnoreCase(parts[1])? Sort.Order.ASC : Sort.Order.DESC);
            search.setSort(sor);
        }

        if (q!=null && !q.isEmpty()) {
            search.setText(q);
        }

        return searchUsers(search);
    }

    /**
     * Advanced search query.
     *
     * @param pagedSearch
     * @return ResponseEntity never null
     */
    @RequestMapping(value="/users/search", method=POST, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity searchUsers(@RequestBody(required=true) PagedSearch pagedSearch){
        try {
            final int pageIndex = pagedSearch.getPage();
            final int size = pagedSearch.getSize();
            final String text = pagedSearch.getText();
            String sortFieldName = null;
            String sortOrder = null;
            if(pagedSearch.getSort() != null) {
                sortFieldName = pagedSearch.getSort().getField();
                sortOrder = pagedSearch.getSort().getOrder().name();
            }

            final Page page = new Page<UserWithRole>()
                            .setNumber(pagedSearch.getPage())
                            .setSize(pagedSearch.getSize())
                            .setContent(userBusiness.search(text, size, pageIndex, sortFieldName, sortOrder))
                            .setTotal(userBusiness.searchCount(text));

            return new ResponseEntity(page, OK);
        } catch(Throwable ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Count total number of users.
     *
     * @return number of users
     */
    @RequestMapping(value="/users/count", method=GET, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity countUsers() {
        try{
            return new ResponseEntity(Collections.singletonMap("count", userBusiness.countUser()), OK);
        } catch (Throwable ex){
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Get a single user by identifier.
     *
     * @param id user identifier
     * @return ResponseEntity never null
     */
    @RequestMapping(value="/users/{id}", method=GET, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getUser(@PathVariable(value="id") int id){
        try {
            final Optional<UserWithRole> optUser = userBusiness.findOneWithRole(id);
            if (optUser.isPresent()) {
                return new ResponseEntity(optUser.get(),OK);
            } else {
                return new ErrorMessage(NOT_FOUND).i18N(I18nCodes.User.NOT_FOUND).build();
            }
        } catch(Throwable ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Create a new user.
     *
     * @param user new user
     * @return ResponseEntity with created user
     */
    @RequestMapping(value="/users", method=POST, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity createUser(@RequestBody CstlUser user){
        try {
            user = userBusiness.create(user);
            return new ResponseEntity(user,CREATED);
        } catch(Throwable ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Update an existing user.
     *
     * @param id user identifier
     * @param user updated user properties
     * @return ResponseEntity with updated user
     */
    @RequestMapping(value="/users/{id}", method=POST, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity updateUser(
            @PathVariable(value="id") int id,
            @RequestBody CstlUser user){
        try {
            user.setId(id);
            user = userBusiness.update(user);
            return new ResponseEntity(user,OK);
        } catch(Throwable ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/users/{id}/activate", method=PUT, produces=MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity activate(@PathVariable("id") int id) {
        try {
            final Optional<CstlUser> user = userBusiness.findById(id);
            if (user.isPresent()) {
                if (user.get().getActive()) {
                    if (userBusiness.isLastAdmin(id)) {
                        return new ErrorMessage(UNPROCESSABLE_ENTITY).i18N(I18nCodes.User.LAST_ADMIN).build();
                    } else {
                        userBusiness.desactivate(id);
                    }
                } else {
                    userBusiness.activate(id);
                }
            }
            return new ResponseEntity(OK);
        } catch(Throwable ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Delete an existing user.
     * This actually only desactivate the user.
     *
     * @param id user identifier
     * @return ResponseEntity with updated user
     */
    @RequestMapping(value="/users/{id}", method=DELETE, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity deleteUser(
            @PathVariable(value="id") int id){
        try {
            if (userBusiness.isLastAdmin(id)) {
                return new ErrorMessage(HttpStatus.BAD_REQUEST).i18N(I18nCodes.User.LAST_ADMIN).build();
            }
            userBusiness.desactivate(id);
            return new ResponseEntity(NO_CONTENT);
        } catch(Throwable ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

}
