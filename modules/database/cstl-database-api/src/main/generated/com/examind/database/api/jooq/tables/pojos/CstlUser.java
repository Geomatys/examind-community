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
package com.examind.database.api.jooq.tables.pojos;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;


/**
 * Generated DAO object for table admin.cstl_user
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class CstlUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String login;
    private String password;
    private String firstname;
    private String lastname;
    private String email;
    private Boolean active;
    private String avatar;
    private String zip;
    private String city;
    private String country;
    private String phone;
    private String forgotPasswordUuid;
    private String address;
    private String additionalAddress;
    private String civility;
    private String title;
    private String locale;

    public CstlUser() {}

    public CstlUser(CstlUser value) {
        this.id = value.id;
        this.login = value.login;
        this.password = value.password;
        this.firstname = value.firstname;
        this.lastname = value.lastname;
        this.email = value.email;
        this.active = value.active;
        this.avatar = value.avatar;
        this.zip = value.zip;
        this.city = value.city;
        this.country = value.country;
        this.phone = value.phone;
        this.forgotPasswordUuid = value.forgotPasswordUuid;
        this.address = value.address;
        this.additionalAddress = value.additionalAddress;
        this.civility = value.civility;
        this.title = value.title;
        this.locale = value.locale;
    }

    public CstlUser(
        Integer id,
        String login,
        String password,
        String firstname,
        String lastname,
        String email,
        Boolean active,
        String avatar,
        String zip,
        String city,
        String country,
        String phone,
        String forgotPasswordUuid,
        String address,
        String additionalAddress,
        String civility,
        String title,
        String locale
    ) {
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

    /**
     * Getter for <code>admin.cstl_user.id</code>.
     */
    public Integer getId() {
        return this.id;
    }

    /**
     * Setter for <code>admin.cstl_user.id</code>.
     */
    public CstlUser setId(Integer id) {
        this.id = id;
        return this;
    }

    /**
     * Getter for <code>admin.cstl_user.login</code>.
     */
    @NotNull
    @Size(max = 255)
    public String getLogin() {
        return this.login;
    }

    /**
     * Setter for <code>admin.cstl_user.login</code>.
     */
    public CstlUser setLogin(String login) {
        this.login = login;
        return this;
    }

    /**
     * Getter for <code>admin.cstl_user.password</code>.
     */
    @NotNull
    @Size(max = 32)
    public String getPassword() {
        return this.password;
    }

    /**
     * Setter for <code>admin.cstl_user.password</code>.
     */
    public CstlUser setPassword(String password) {
        this.password = password;
        return this;
    }

    /**
     * Getter for <code>admin.cstl_user.firstname</code>.
     */
    @NotNull
    @Size(max = 255)
    public String getFirstname() {
        return this.firstname;
    }

    /**
     * Setter for <code>admin.cstl_user.firstname</code>.
     */
    public CstlUser setFirstname(String firstname) {
        this.firstname = firstname;
        return this;
    }

    /**
     * Getter for <code>admin.cstl_user.lastname</code>.
     */
    @NotNull
    @Size(max = 255)
    public String getLastname() {
        return this.lastname;
    }

    /**
     * Setter for <code>admin.cstl_user.lastname</code>.
     */
    public CstlUser setLastname(String lastname) {
        this.lastname = lastname;
        return this;
    }

    /**
     * Getter for <code>admin.cstl_user.email</code>.
     */
    @NotNull
    @Size(max = 255)
    public String getEmail() {
        return this.email;
    }

    /**
     * Setter for <code>admin.cstl_user.email</code>.
     */
    public CstlUser setEmail(String email) {
        this.email = email;
        return this;
    }

    /**
     * Getter for <code>admin.cstl_user.active</code>.
     */
    @NotNull
    public Boolean getActive() {
        return this.active;
    }

    /**
     * Setter for <code>admin.cstl_user.active</code>.
     */
    public CstlUser setActive(Boolean active) {
        this.active = active;
        return this;
    }

    /**
     * Getter for <code>admin.cstl_user.avatar</code>.
     */
    @Size(max = 255)
    public String getAvatar() {
        return this.avatar;
    }

    /**
     * Setter for <code>admin.cstl_user.avatar</code>.
     */
    public CstlUser setAvatar(String avatar) {
        this.avatar = avatar;
        return this;
    }

    /**
     * Getter for <code>admin.cstl_user.zip</code>.
     */
    @Size(max = 64)
    public String getZip() {
        return this.zip;
    }

    /**
     * Setter for <code>admin.cstl_user.zip</code>.
     */
    public CstlUser setZip(String zip) {
        this.zip = zip;
        return this;
    }

    /**
     * Getter for <code>admin.cstl_user.city</code>.
     */
    @Size(max = 255)
    public String getCity() {
        return this.city;
    }

    /**
     * Setter for <code>admin.cstl_user.city</code>.
     */
    public CstlUser setCity(String city) {
        this.city = city;
        return this;
    }

    /**
     * Getter for <code>admin.cstl_user.country</code>.
     */
    @Size(max = 255)
    public String getCountry() {
        return this.country;
    }

    /**
     * Setter for <code>admin.cstl_user.country</code>.
     */
    public CstlUser setCountry(String country) {
        this.country = country;
        return this;
    }

    /**
     * Getter for <code>admin.cstl_user.phone</code>.
     */
    @Size(max = 64)
    public String getPhone() {
        return this.phone;
    }

    /**
     * Setter for <code>admin.cstl_user.phone</code>.
     */
    public CstlUser setPhone(String phone) {
        this.phone = phone;
        return this;
    }

    /**
     * Getter for <code>admin.cstl_user.forgot_password_uuid</code>.
     */
    @Size(max = 64)
    public String getForgotPasswordUuid() {
        return this.forgotPasswordUuid;
    }

    /**
     * Setter for <code>admin.cstl_user.forgot_password_uuid</code>.
     */
    public CstlUser setForgotPasswordUuid(String forgotPasswordUuid) {
        this.forgotPasswordUuid = forgotPasswordUuid;
        return this;
    }

    /**
     * Getter for <code>admin.cstl_user.address</code>.
     */
    public String getAddress() {
        return this.address;
    }

    /**
     * Setter for <code>admin.cstl_user.address</code>.
     */
    public CstlUser setAddress(String address) {
        this.address = address;
        return this;
    }

    /**
     * Getter for <code>admin.cstl_user.additional_address</code>.
     */
    public String getAdditionalAddress() {
        return this.additionalAddress;
    }

    /**
     * Setter for <code>admin.cstl_user.additional_address</code>.
     */
    public CstlUser setAdditionalAddress(String additionalAddress) {
        this.additionalAddress = additionalAddress;
        return this;
    }

    /**
     * Getter for <code>admin.cstl_user.civility</code>.
     */
    @Size(max = 64)
    public String getCivility() {
        return this.civility;
    }

    /**
     * Setter for <code>admin.cstl_user.civility</code>.
     */
    public CstlUser setCivility(String civility) {
        this.civility = civility;
        return this;
    }

    /**
     * Getter for <code>admin.cstl_user.title</code>.
     */
    public String getTitle() {
        return this.title;
    }

    /**
     * Setter for <code>admin.cstl_user.title</code>.
     */
    public CstlUser setTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * Getter for <code>admin.cstl_user.locale</code>.
     */
    @NotNull
    public String getLocale() {
        return this.locale;
    }

    /**
     * Setter for <code>admin.cstl_user.locale</code>.
     */
    public CstlUser setLocale(String locale) {
        this.locale = locale;
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final CstlUser other = (CstlUser) obj;
        if (this.id == null) {
            if (other.id != null)
                return false;
        }
        else if (!this.id.equals(other.id))
            return false;
        if (this.login == null) {
            if (other.login != null)
                return false;
        }
        else if (!this.login.equals(other.login))
            return false;
        if (this.password == null) {
            if (other.password != null)
                return false;
        }
        else if (!this.password.equals(other.password))
            return false;
        if (this.firstname == null) {
            if (other.firstname != null)
                return false;
        }
        else if (!this.firstname.equals(other.firstname))
            return false;
        if (this.lastname == null) {
            if (other.lastname != null)
                return false;
        }
        else if (!this.lastname.equals(other.lastname))
            return false;
        if (this.email == null) {
            if (other.email != null)
                return false;
        }
        else if (!this.email.equals(other.email))
            return false;
        if (this.active == null) {
            if (other.active != null)
                return false;
        }
        else if (!this.active.equals(other.active))
            return false;
        if (this.avatar == null) {
            if (other.avatar != null)
                return false;
        }
        else if (!this.avatar.equals(other.avatar))
            return false;
        if (this.zip == null) {
            if (other.zip != null)
                return false;
        }
        else if (!this.zip.equals(other.zip))
            return false;
        if (this.city == null) {
            if (other.city != null)
                return false;
        }
        else if (!this.city.equals(other.city))
            return false;
        if (this.country == null) {
            if (other.country != null)
                return false;
        }
        else if (!this.country.equals(other.country))
            return false;
        if (this.phone == null) {
            if (other.phone != null)
                return false;
        }
        else if (!this.phone.equals(other.phone))
            return false;
        if (this.forgotPasswordUuid == null) {
            if (other.forgotPasswordUuid != null)
                return false;
        }
        else if (!this.forgotPasswordUuid.equals(other.forgotPasswordUuid))
            return false;
        if (this.address == null) {
            if (other.address != null)
                return false;
        }
        else if (!this.address.equals(other.address))
            return false;
        if (this.additionalAddress == null) {
            if (other.additionalAddress != null)
                return false;
        }
        else if (!this.additionalAddress.equals(other.additionalAddress))
            return false;
        if (this.civility == null) {
            if (other.civility != null)
                return false;
        }
        else if (!this.civility.equals(other.civility))
            return false;
        if (this.title == null) {
            if (other.title != null)
                return false;
        }
        else if (!this.title.equals(other.title))
            return false;
        if (this.locale == null) {
            if (other.locale != null)
                return false;
        }
        else if (!this.locale.equals(other.locale))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
        result = prime * result + ((this.login == null) ? 0 : this.login.hashCode());
        result = prime * result + ((this.password == null) ? 0 : this.password.hashCode());
        result = prime * result + ((this.firstname == null) ? 0 : this.firstname.hashCode());
        result = prime * result + ((this.lastname == null) ? 0 : this.lastname.hashCode());
        result = prime * result + ((this.email == null) ? 0 : this.email.hashCode());
        result = prime * result + ((this.active == null) ? 0 : this.active.hashCode());
        result = prime * result + ((this.avatar == null) ? 0 : this.avatar.hashCode());
        result = prime * result + ((this.zip == null) ? 0 : this.zip.hashCode());
        result = prime * result + ((this.city == null) ? 0 : this.city.hashCode());
        result = prime * result + ((this.country == null) ? 0 : this.country.hashCode());
        result = prime * result + ((this.phone == null) ? 0 : this.phone.hashCode());
        result = prime * result + ((this.forgotPasswordUuid == null) ? 0 : this.forgotPasswordUuid.hashCode());
        result = prime * result + ((this.address == null) ? 0 : this.address.hashCode());
        result = prime * result + ((this.additionalAddress == null) ? 0 : this.additionalAddress.hashCode());
        result = prime * result + ((this.civility == null) ? 0 : this.civility.hashCode());
        result = prime * result + ((this.title == null) ? 0 : this.title.hashCode());
        result = prime * result + ((this.locale == null) ? 0 : this.locale.hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("CstlUser (");

        sb.append(id);
        sb.append(", ").append(login);
        sb.append(", ").append(password);
        sb.append(", ").append(firstname);
        sb.append(", ").append(lastname);
        sb.append(", ").append(email);
        sb.append(", ").append(active);
        sb.append(", ").append(avatar);
        sb.append(", ").append(zip);
        sb.append(", ").append(city);
        sb.append(", ").append(country);
        sb.append(", ").append(phone);
        sb.append(", ").append(forgotPasswordUuid);
        sb.append(", ").append(address);
        sb.append(", ").append(additionalAddress);
        sb.append(", ").append(civility);
        sb.append(", ").append(title);
        sb.append(", ").append(locale);

        sb.append(")");
        return sb.toString();
    }
}
