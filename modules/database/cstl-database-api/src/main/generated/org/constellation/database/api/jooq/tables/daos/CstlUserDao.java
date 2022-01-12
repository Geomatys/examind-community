/*
 *     Examind Community - An open source and standard compliant SDI
 *     https://community.examind.com/
 * 
 *  Copyright 2022 Geomatys.
 * 
 *  Licensed under the Apache License, Version 2.0 (    the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/
package org.constellation.database.api.jooq.tables.daos;


import java.util.List;

import org.constellation.database.api.jooq.tables.CstlUser;
import org.constellation.database.api.jooq.tables.records.CstlUserRecord;
import org.jooq.Configuration;
import org.jooq.impl.DAOImpl;


/**
 * Generated DAO object for table admin.cstl_user
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class CstlUserDao extends DAOImpl<CstlUserRecord, org.constellation.database.api.jooq.tables.pojos.CstlUser, Integer> {

    /**
     * Create a new CstlUserDao without any configuration
     */
    public CstlUserDao() {
        super(CstlUser.CSTL_USER, org.constellation.database.api.jooq.tables.pojos.CstlUser.class);
    }

    /**
     * Create a new CstlUserDao with an attached configuration
     */
    public CstlUserDao(Configuration configuration) {
        super(CstlUser.CSTL_USER, org.constellation.database.api.jooq.tables.pojos.CstlUser.class, configuration);
    }

    @Override
    public Integer getId(org.constellation.database.api.jooq.tables.pojos.CstlUser object) {
        return object.getId();
    }

    /**
     * Fetch records that have <code>id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.CstlUser> fetchRangeOfId(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(CstlUser.CSTL_USER.ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>id IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.CstlUser> fetchById(Integer... values) {
        return fetch(CstlUser.CSTL_USER.ID, values);
    }

    /**
     * Fetch a unique record that has <code>id = value</code>
     */
    public org.constellation.database.api.jooq.tables.pojos.CstlUser fetchOneById(Integer value) {
        return fetchOne(CstlUser.CSTL_USER.ID, value);
    }

    /**
     * Fetch records that have <code>login BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.CstlUser> fetchRangeOfLogin(String lowerInclusive, String upperInclusive) {
        return fetchRange(CstlUser.CSTL_USER.LOGIN, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>login IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.CstlUser> fetchByLogin(String... values) {
        return fetch(CstlUser.CSTL_USER.LOGIN, values);
    }

    /**
     * Fetch a unique record that has <code>login = value</code>
     */
    public org.constellation.database.api.jooq.tables.pojos.CstlUser fetchOneByLogin(String value) {
        return fetchOne(CstlUser.CSTL_USER.LOGIN, value);
    }

    /**
     * Fetch records that have <code>password BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.CstlUser> fetchRangeOfPassword(String lowerInclusive, String upperInclusive) {
        return fetchRange(CstlUser.CSTL_USER.PASSWORD, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>password IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.CstlUser> fetchByPassword(String... values) {
        return fetch(CstlUser.CSTL_USER.PASSWORD, values);
    }

    /**
     * Fetch records that have <code>firstname BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.CstlUser> fetchRangeOfFirstname(String lowerInclusive, String upperInclusive) {
        return fetchRange(CstlUser.CSTL_USER.FIRSTNAME, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>firstname IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.CstlUser> fetchByFirstname(String... values) {
        return fetch(CstlUser.CSTL_USER.FIRSTNAME, values);
    }

    /**
     * Fetch records that have <code>lastname BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.CstlUser> fetchRangeOfLastname(String lowerInclusive, String upperInclusive) {
        return fetchRange(CstlUser.CSTL_USER.LASTNAME, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>lastname IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.CstlUser> fetchByLastname(String... values) {
        return fetch(CstlUser.CSTL_USER.LASTNAME, values);
    }

    /**
     * Fetch records that have <code>email BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.CstlUser> fetchRangeOfEmail(String lowerInclusive, String upperInclusive) {
        return fetchRange(CstlUser.CSTL_USER.EMAIL, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>email IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.CstlUser> fetchByEmail(String... values) {
        return fetch(CstlUser.CSTL_USER.EMAIL, values);
    }

    /**
     * Fetch a unique record that has <code>email = value</code>
     */
    public org.constellation.database.api.jooq.tables.pojos.CstlUser fetchOneByEmail(String value) {
        return fetchOne(CstlUser.CSTL_USER.EMAIL, value);
    }

    /**
     * Fetch records that have <code>active BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.CstlUser> fetchRangeOfActive(Boolean lowerInclusive, Boolean upperInclusive) {
        return fetchRange(CstlUser.CSTL_USER.ACTIVE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>active IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.CstlUser> fetchByActive(Boolean... values) {
        return fetch(CstlUser.CSTL_USER.ACTIVE, values);
    }

    /**
     * Fetch records that have <code>avatar BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.CstlUser> fetchRangeOfAvatar(String lowerInclusive, String upperInclusive) {
        return fetchRange(CstlUser.CSTL_USER.AVATAR, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>avatar IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.CstlUser> fetchByAvatar(String... values) {
        return fetch(CstlUser.CSTL_USER.AVATAR, values);
    }

    /**
     * Fetch records that have <code>zip BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.CstlUser> fetchRangeOfZip(String lowerInclusive, String upperInclusive) {
        return fetchRange(CstlUser.CSTL_USER.ZIP, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>zip IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.CstlUser> fetchByZip(String... values) {
        return fetch(CstlUser.CSTL_USER.ZIP, values);
    }

    /**
     * Fetch records that have <code>city BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.CstlUser> fetchRangeOfCity(String lowerInclusive, String upperInclusive) {
        return fetchRange(CstlUser.CSTL_USER.CITY, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>city IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.CstlUser> fetchByCity(String... values) {
        return fetch(CstlUser.CSTL_USER.CITY, values);
    }

    /**
     * Fetch records that have <code>country BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.CstlUser> fetchRangeOfCountry(String lowerInclusive, String upperInclusive) {
        return fetchRange(CstlUser.CSTL_USER.COUNTRY, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>country IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.CstlUser> fetchByCountry(String... values) {
        return fetch(CstlUser.CSTL_USER.COUNTRY, values);
    }

    /**
     * Fetch records that have <code>phone BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.CstlUser> fetchRangeOfPhone(String lowerInclusive, String upperInclusive) {
        return fetchRange(CstlUser.CSTL_USER.PHONE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>phone IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.CstlUser> fetchByPhone(String... values) {
        return fetch(CstlUser.CSTL_USER.PHONE, values);
    }

    /**
     * Fetch records that have <code>forgot_password_uuid BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.CstlUser> fetchRangeOfForgotPasswordUuid(String lowerInclusive, String upperInclusive) {
        return fetchRange(CstlUser.CSTL_USER.FORGOT_PASSWORD_UUID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>forgot_password_uuid IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.CstlUser> fetchByForgotPasswordUuid(String... values) {
        return fetch(CstlUser.CSTL_USER.FORGOT_PASSWORD_UUID, values);
    }

    /**
     * Fetch a unique record that has <code>forgot_password_uuid = value</code>
     */
    public org.constellation.database.api.jooq.tables.pojos.CstlUser fetchOneByForgotPasswordUuid(String value) {
        return fetchOne(CstlUser.CSTL_USER.FORGOT_PASSWORD_UUID, value);
    }

    /**
     * Fetch records that have <code>address BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.CstlUser> fetchRangeOfAddress(String lowerInclusive, String upperInclusive) {
        return fetchRange(CstlUser.CSTL_USER.ADDRESS, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>address IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.CstlUser> fetchByAddress(String... values) {
        return fetch(CstlUser.CSTL_USER.ADDRESS, values);
    }

    /**
     * Fetch records that have <code>additional_address BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.CstlUser> fetchRangeOfAdditionalAddress(String lowerInclusive, String upperInclusive) {
        return fetchRange(CstlUser.CSTL_USER.ADDITIONAL_ADDRESS, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>additional_address IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.CstlUser> fetchByAdditionalAddress(String... values) {
        return fetch(CstlUser.CSTL_USER.ADDITIONAL_ADDRESS, values);
    }

    /**
     * Fetch records that have <code>civility BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.CstlUser> fetchRangeOfCivility(String lowerInclusive, String upperInclusive) {
        return fetchRange(CstlUser.CSTL_USER.CIVILITY, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>civility IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.CstlUser> fetchByCivility(String... values) {
        return fetch(CstlUser.CSTL_USER.CIVILITY, values);
    }

    /**
     * Fetch records that have <code>title BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.CstlUser> fetchRangeOfTitle(String lowerInclusive, String upperInclusive) {
        return fetchRange(CstlUser.CSTL_USER.TITLE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>title IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.CstlUser> fetchByTitle(String... values) {
        return fetch(CstlUser.CSTL_USER.TITLE, values);
    }

    /**
     * Fetch records that have <code>locale BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.CstlUser> fetchRangeOfLocale(String lowerInclusive, String upperInclusive) {
        return fetchRange(CstlUser.CSTL_USER.LOCALE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>locale IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.CstlUser> fetchByLocale(String... values) {
        return fetch(CstlUser.CSTL_USER.LOCALE, values);
    }
}
