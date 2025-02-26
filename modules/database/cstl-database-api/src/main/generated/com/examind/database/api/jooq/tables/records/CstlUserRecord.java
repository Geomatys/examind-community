/*
 *     Examind Community - An open source and standard compliant SDI
 *     https://community.examind.com/
 * 
 *  Copyright 2022 Geomatys.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
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
package com.examind.database.api.jooq.tables.records;


import com.examind.database.api.jooq.tables.CstlUser;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record18;
import org.jooq.Row18;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.cstl_user
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class CstlUserRecord extends UpdatableRecordImpl<CstlUserRecord> implements Record18<Integer, String, String, String, String, String, Boolean, String, String, String, String, String, String, String, String, String, String, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.cstl_user.id</code>.
     */
    public CstlUserRecord setId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.cstl_user.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.cstl_user.login</code>.
     */
    public CstlUserRecord setLogin(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.cstl_user.login</code>.
     */
    @NotNull
    @Size(max = 255)
    public String getLogin() {
        return (String) get(1);
    }

    /**
     * Setter for <code>admin.cstl_user.password</code>.
     */
    public CstlUserRecord setPassword(String value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>admin.cstl_user.password</code>.
     */
    @NotNull
    @Size(max = 32)
    public String getPassword() {
        return (String) get(2);
    }

    /**
     * Setter for <code>admin.cstl_user.firstname</code>.
     */
    public CstlUserRecord setFirstname(String value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>admin.cstl_user.firstname</code>.
     */
    @NotNull
    @Size(max = 255)
    public String getFirstname() {
        return (String) get(3);
    }

    /**
     * Setter for <code>admin.cstl_user.lastname</code>.
     */
    public CstlUserRecord setLastname(String value) {
        set(4, value);
        return this;
    }

    /**
     * Getter for <code>admin.cstl_user.lastname</code>.
     */
    @NotNull
    @Size(max = 255)
    public String getLastname() {
        return (String) get(4);
    }

    /**
     * Setter for <code>admin.cstl_user.email</code>.
     */
    public CstlUserRecord setEmail(String value) {
        set(5, value);
        return this;
    }

    /**
     * Getter for <code>admin.cstl_user.email</code>.
     */
    @NotNull
    @Size(max = 255)
    public String getEmail() {
        return (String) get(5);
    }

    /**
     * Setter for <code>admin.cstl_user.active</code>.
     */
    public CstlUserRecord setActive(Boolean value) {
        set(6, value);
        return this;
    }

    /**
     * Getter for <code>admin.cstl_user.active</code>.
     */
    @NotNull
    public Boolean getActive() {
        return (Boolean) get(6);
    }

    /**
     * Setter for <code>admin.cstl_user.avatar</code>.
     */
    public CstlUserRecord setAvatar(String value) {
        set(7, value);
        return this;
    }

    /**
     * Getter for <code>admin.cstl_user.avatar</code>.
     */
    @Size(max = 255)
    public String getAvatar() {
        return (String) get(7);
    }

    /**
     * Setter for <code>admin.cstl_user.zip</code>.
     */
    public CstlUserRecord setZip(String value) {
        set(8, value);
        return this;
    }

    /**
     * Getter for <code>admin.cstl_user.zip</code>.
     */
    @Size(max = 64)
    public String getZip() {
        return (String) get(8);
    }

    /**
     * Setter for <code>admin.cstl_user.city</code>.
     */
    public CstlUserRecord setCity(String value) {
        set(9, value);
        return this;
    }

    /**
     * Getter for <code>admin.cstl_user.city</code>.
     */
    @Size(max = 255)
    public String getCity() {
        return (String) get(9);
    }

    /**
     * Setter for <code>admin.cstl_user.country</code>.
     */
    public CstlUserRecord setCountry(String value) {
        set(10, value);
        return this;
    }

    /**
     * Getter for <code>admin.cstl_user.country</code>.
     */
    @Size(max = 255)
    public String getCountry() {
        return (String) get(10);
    }

    /**
     * Setter for <code>admin.cstl_user.phone</code>.
     */
    public CstlUserRecord setPhone(String value) {
        set(11, value);
        return this;
    }

    /**
     * Getter for <code>admin.cstl_user.phone</code>.
     */
    @Size(max = 64)
    public String getPhone() {
        return (String) get(11);
    }

    /**
     * Setter for <code>admin.cstl_user.forgot_password_uuid</code>.
     */
    public CstlUserRecord setForgotPasswordUuid(String value) {
        set(12, value);
        return this;
    }

    /**
     * Getter for <code>admin.cstl_user.forgot_password_uuid</code>.
     */
    @Size(max = 64)
    public String getForgotPasswordUuid() {
        return (String) get(12);
    }

    /**
     * Setter for <code>admin.cstl_user.address</code>.
     */
    public CstlUserRecord setAddress(String value) {
        set(13, value);
        return this;
    }

    /**
     * Getter for <code>admin.cstl_user.address</code>.
     */
    public String getAddress() {
        return (String) get(13);
    }

    /**
     * Setter for <code>admin.cstl_user.additional_address</code>.
     */
    public CstlUserRecord setAdditionalAddress(String value) {
        set(14, value);
        return this;
    }

    /**
     * Getter for <code>admin.cstl_user.additional_address</code>.
     */
    public String getAdditionalAddress() {
        return (String) get(14);
    }

    /**
     * Setter for <code>admin.cstl_user.civility</code>.
     */
    public CstlUserRecord setCivility(String value) {
        set(15, value);
        return this;
    }

    /**
     * Getter for <code>admin.cstl_user.civility</code>.
     */
    @Size(max = 64)
    public String getCivility() {
        return (String) get(15);
    }

    /**
     * Setter for <code>admin.cstl_user.title</code>.
     */
    public CstlUserRecord setTitle(String value) {
        set(16, value);
        return this;
    }

    /**
     * Getter for <code>admin.cstl_user.title</code>.
     */
    public String getTitle() {
        return (String) get(16);
    }

    /**
     * Setter for <code>admin.cstl_user.locale</code>.
     */
    public CstlUserRecord setLocale(String value) {
        set(17, value);
        return this;
    }

    /**
     * Getter for <code>admin.cstl_user.locale</code>.
     */
    @NotNull
    public String getLocale() {
        return (String) get(17);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record18 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row18<Integer, String, String, String, String, String, Boolean, String, String, String, String, String, String, String, String, String, String, String> fieldsRow() {
        return (Row18) super.fieldsRow();
    }

    @Override
    public Row18<Integer, String, String, String, String, String, Boolean, String, String, String, String, String, String, String, String, String, String, String> valuesRow() {
        return (Row18) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return CstlUser.CSTL_USER.ID;
    }

    @Override
    public Field<String> field2() {
        return CstlUser.CSTL_USER.LOGIN;
    }

    @Override
    public Field<String> field3() {
        return CstlUser.CSTL_USER.PASSWORD;
    }

    @Override
    public Field<String> field4() {
        return CstlUser.CSTL_USER.FIRSTNAME;
    }

    @Override
    public Field<String> field5() {
        return CstlUser.CSTL_USER.LASTNAME;
    }

    @Override
    public Field<String> field6() {
        return CstlUser.CSTL_USER.EMAIL;
    }

    @Override
    public Field<Boolean> field7() {
        return CstlUser.CSTL_USER.ACTIVE;
    }

    @Override
    public Field<String> field8() {
        return CstlUser.CSTL_USER.AVATAR;
    }

    @Override
    public Field<String> field9() {
        return CstlUser.CSTL_USER.ZIP;
    }

    @Override
    public Field<String> field10() {
        return CstlUser.CSTL_USER.CITY;
    }

    @Override
    public Field<String> field11() {
        return CstlUser.CSTL_USER.COUNTRY;
    }

    @Override
    public Field<String> field12() {
        return CstlUser.CSTL_USER.PHONE;
    }

    @Override
    public Field<String> field13() {
        return CstlUser.CSTL_USER.FORGOT_PASSWORD_UUID;
    }

    @Override
    public Field<String> field14() {
        return CstlUser.CSTL_USER.ADDRESS;
    }

    @Override
    public Field<String> field15() {
        return CstlUser.CSTL_USER.ADDITIONAL_ADDRESS;
    }

    @Override
    public Field<String> field16() {
        return CstlUser.CSTL_USER.CIVILITY;
    }

    @Override
    public Field<String> field17() {
        return CstlUser.CSTL_USER.TITLE;
    }

    @Override
    public Field<String> field18() {
        return CstlUser.CSTL_USER.LOCALE;
    }

    @Override
    public Integer component1() {
        return getId();
    }

    @Override
    public String component2() {
        return getLogin();
    }

    @Override
    public String component3() {
        return getPassword();
    }

    @Override
    public String component4() {
        return getFirstname();
    }

    @Override
    public String component5() {
        return getLastname();
    }

    @Override
    public String component6() {
        return getEmail();
    }

    @Override
    public Boolean component7() {
        return getActive();
    }

    @Override
    public String component8() {
        return getAvatar();
    }

    @Override
    public String component9() {
        return getZip();
    }

    @Override
    public String component10() {
        return getCity();
    }

    @Override
    public String component11() {
        return getCountry();
    }

    @Override
    public String component12() {
        return getPhone();
    }

    @Override
    public String component13() {
        return getForgotPasswordUuid();
    }

    @Override
    public String component14() {
        return getAddress();
    }

    @Override
    public String component15() {
        return getAdditionalAddress();
    }

    @Override
    public String component16() {
        return getCivility();
    }

    @Override
    public String component17() {
        return getTitle();
    }

    @Override
    public String component18() {
        return getLocale();
    }

    @Override
    public Integer value1() {
        return getId();
    }

    @Override
    public String value2() {
        return getLogin();
    }

    @Override
    public String value3() {
        return getPassword();
    }

    @Override
    public String value4() {
        return getFirstname();
    }

    @Override
    public String value5() {
        return getLastname();
    }

    @Override
    public String value6() {
        return getEmail();
    }

    @Override
    public Boolean value7() {
        return getActive();
    }

    @Override
    public String value8() {
        return getAvatar();
    }

    @Override
    public String value9() {
        return getZip();
    }

    @Override
    public String value10() {
        return getCity();
    }

    @Override
    public String value11() {
        return getCountry();
    }

    @Override
    public String value12() {
        return getPhone();
    }

    @Override
    public String value13() {
        return getForgotPasswordUuid();
    }

    @Override
    public String value14() {
        return getAddress();
    }

    @Override
    public String value15() {
        return getAdditionalAddress();
    }

    @Override
    public String value16() {
        return getCivility();
    }

    @Override
    public String value17() {
        return getTitle();
    }

    @Override
    public String value18() {
        return getLocale();
    }

    @Override
    public CstlUserRecord value1(Integer value) {
        setId(value);
        return this;
    }

    @Override
    public CstlUserRecord value2(String value) {
        setLogin(value);
        return this;
    }

    @Override
    public CstlUserRecord value3(String value) {
        setPassword(value);
        return this;
    }

    @Override
    public CstlUserRecord value4(String value) {
        setFirstname(value);
        return this;
    }

    @Override
    public CstlUserRecord value5(String value) {
        setLastname(value);
        return this;
    }

    @Override
    public CstlUserRecord value6(String value) {
        setEmail(value);
        return this;
    }

    @Override
    public CstlUserRecord value7(Boolean value) {
        setActive(value);
        return this;
    }

    @Override
    public CstlUserRecord value8(String value) {
        setAvatar(value);
        return this;
    }

    @Override
    public CstlUserRecord value9(String value) {
        setZip(value);
        return this;
    }

    @Override
    public CstlUserRecord value10(String value) {
        setCity(value);
        return this;
    }

    @Override
    public CstlUserRecord value11(String value) {
        setCountry(value);
        return this;
    }

    @Override
    public CstlUserRecord value12(String value) {
        setPhone(value);
        return this;
    }

    @Override
    public CstlUserRecord value13(String value) {
        setForgotPasswordUuid(value);
        return this;
    }

    @Override
    public CstlUserRecord value14(String value) {
        setAddress(value);
        return this;
    }

    @Override
    public CstlUserRecord value15(String value) {
        setAdditionalAddress(value);
        return this;
    }

    @Override
    public CstlUserRecord value16(String value) {
        setCivility(value);
        return this;
    }

    @Override
    public CstlUserRecord value17(String value) {
        setTitle(value);
        return this;
    }

    @Override
    public CstlUserRecord value18(String value) {
        setLocale(value);
        return this;
    }

    @Override
    public CstlUserRecord values(Integer value1, String value2, String value3, String value4, String value5, String value6, Boolean value7, String value8, String value9, String value10, String value11, String value12, String value13, String value14, String value15, String value16, String value17, String value18) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        value8(value8);
        value9(value9);
        value10(value10);
        value11(value11);
        value12(value12);
        value13(value13);
        value14(value14);
        value15(value15);
        value16(value16);
        value17(value17);
        value18(value18);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached CstlUserRecord
     */
    public CstlUserRecord() {
        super(CstlUser.CSTL_USER);
    }

    /**
     * Create a detached, initialised CstlUserRecord
     */
    public CstlUserRecord(Integer id, String login, String password, String firstname, String lastname, String email, Boolean active, String avatar, String zip, String city, String country, String phone, String forgotPasswordUuid, String address, String additionalAddress, String civility, String title, String locale) {
        super(CstlUser.CSTL_USER);

        setId(id);
        setLogin(login);
        setPassword(password);
        setFirstname(firstname);
        setLastname(lastname);
        setEmail(email);
        setActive(active);
        setAvatar(avatar);
        setZip(zip);
        setCity(city);
        setCountry(country);
        setPhone(phone);
        setForgotPasswordUuid(forgotPasswordUuid);
        setAddress(address);
        setAdditionalAddress(additionalAddress);
        setCivility(civility);
        setTitle(title);
        setLocale(locale);
        resetChangedOnNotNull();
    }

    /**
     * Create a detached, initialised CstlUserRecord
     */
    public CstlUserRecord(com.examind.database.api.jooq.tables.pojos.CstlUser value) {
        super(CstlUser.CSTL_USER);

        if (value != null) {
            setId(value.getId());
            setLogin(value.getLogin());
            setPassword(value.getPassword());
            setFirstname(value.getFirstname());
            setLastname(value.getLastname());
            setEmail(value.getEmail());
            setActive(value.getActive());
            setAvatar(value.getAvatar());
            setZip(value.getZip());
            setCity(value.getCity());
            setCountry(value.getCountry());
            setPhone(value.getPhone());
            setForgotPasswordUuid(value.getForgotPasswordUuid());
            setAddress(value.getAddress());
            setAdditionalAddress(value.getAdditionalAddress());
            setCivility(value.getCivility());
            setTitle(value.getTitle());
            setLocale(value.getLocale());
            resetChangedOnNotNull();
        }
    }
}
