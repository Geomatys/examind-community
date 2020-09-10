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
package org.constellation.dto;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class CstlUser extends Identifiable {

    private String  login;
    private String  password;
    private String  firstname;
    private String  lastname;
    private String  email;
    private Boolean active;
    private String  avatar;
    private String  zip;
    private String  city;
    private String  country;
    private String  phone;
    private String  forgotPasswordUuid;
    private String  address;
    private String  additionalAddress;
    private String  civility;
    private String  title;
    private String  locale;

    public CstlUser() {
    }

    public CstlUser(Integer id, String login, String password, String firstname,
            String lastname, String email, Boolean active, String avatar,
            String zip, String city, String country, String phone, String forgotPasswordUuid,
            String address, String additionalAddress, String civility, String title,
            String locale) {
        this.id = id;
        this.login = login;
        this.password = password;
        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
        this.active = active;
        this.avatar = avatar;
        this.zip = zip;
        this.city = city;
        this.country = country;
        this.phone = phone;
        this.forgotPasswordUuid = forgotPasswordUuid;
        this.address = address;
        this.additionalAddress = additionalAddress;
        this.civility = civility;
        this.title = title;
        this.locale = locale;
    }

    public CstlUser(CstlUser that) {
        if (that != null) {
            this.id = that.id;
            this.login = that.login;
            this.password = that.password;
            this.firstname = that.firstname;
            this.lastname = that.lastname;
            this.email = that.email;
            this.active = that.active;
            this.avatar = that.avatar;
            this.zip = that.zip;
            this.city = that.city;
            this.country = that.country;
            this.phone = that.phone;
            this.forgotPasswordUuid = that.forgotPasswordUuid;
            this.address = that.address;
            this.additionalAddress = that.additionalAddress;
            this.civility = that.civility;
            this.title = that.title;
            this.locale = that.locale;
        }
    }

    /**
     * @return the login
     */
    public String getLogin() {
        return login;
    }

    /**
     * @param login the login to set
     */
    public void setLogin(String login) {
        this.login = login;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return the firstname
     */
    public String getFirstname() {
        return firstname;
    }

    /**
     * @param firstname the firstname to set
     */
    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    /**
     * @return the lastname
     */
    public String getLastname() {
        return lastname;
    }

    /**
     * @param lastname the lastname to set
     */
    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * @return the active
     */
    public Boolean getActive() {
        return active;
    }

    /**
     * @param active the active to set
     */
    public void setActive(Boolean active) {
        this.active = active;
    }

    /**
     * @return the avatar
     */
    public String getAvatar() {
        return avatar;
    }

    /**
     * @param avatar the avatar to set
     */
    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    /**
     * @return the zip
     */
    public String getZip() {
        return zip;
    }

    /**
     * @param zip the zip to set
     */
    public void setZip(String zip) {
        this.zip = zip;
    }

    /**
     * @return the city
     */
    public String getCity() {
        return city;
    }

    /**
     * @param city the city to set
     */
    public void setCity(String city) {
        this.city = city;
    }

    /**
     * @return the country
     */
    public String getCountry() {
        return country;
    }

    /**
     * @param country the country to set
     */
    public void setCountry(String country) {
        this.country = country;
    }

    /**
     * @return the phone
     */
    public String getPhone() {
        return phone;
    }

    /**
     * @param phone the phone to set
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * @return the forgotPasswordUuid
     */
    public String getForgotPasswordUuid() {
        return forgotPasswordUuid;
    }

    /**
     * @param forgotPasswordUuid the forgotPasswordUuid to set
     */
    public void setForgotPasswordUuid(String forgotPasswordUuid) {
        this.forgotPasswordUuid = forgotPasswordUuid;
    }

    /**
     * @return the address
     */
    public String getAddress() {
        return address;
    }

    /**
     * @param address the address to set
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * @return the additionalAddress
     */
    public String getAdditionalAddress() {
        return additionalAddress;
    }

    /**
     * @param additionalAddress the additionalAddress to set
     */
    public void setAdditionalAddress(String additionalAddress) {
        this.additionalAddress = additionalAddress;
    }

    /**
     * @return the civility
     */
    public String getCivility() {
        return civility;
    }

    /**
     * @param civility the civility to set
     */
    public void setCivility(String civility) {
        this.civility = civility;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return the locale
     */
    public String getLocale() {
        return locale;
    }

    /**
     * @param locale the locale to set
     */
    public void setLocale(String locale) {
        this.locale = locale;
    }
}
