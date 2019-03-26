package org.constellation.dto.metadata;

import java.io.Serializable;

/**
 * @author Mehdi Sidhoum (Geomatys).
 */
public class User implements Serializable {

    private java.lang.Integer id;
    private java.lang.String  login;
    private java.lang.String  password;
    private java.lang.String  firstname;
    private java.lang.String  lastname;
    private java.lang.String  email;
    private java.lang.Boolean active;
    private java.lang.String  avatar;
    private java.lang.String  zip;
    private java.lang.String  city;
    private java.lang.String  country;
    private java.lang.String  phone;
    private java.lang.String  forgotPasswordUuid;
    private java.lang.String  address;
    private java.lang.String  additionalAddress;
    private java.lang.String  civility;
    private java.lang.String  title;
    private java.lang.String  locale;
        
    private GroupBrief group;
    
    public User() {}

    public User(
            java.lang.Integer id,
            java.lang.String  login,
            java.lang.String  password,
            java.lang.String  firstname,
            java.lang.String  lastname,
            java.lang.String  email,
            java.lang.Boolean active,
            java.lang.String  avatar,
            java.lang.String  zip,
            java.lang.String  city,
            java.lang.String  country,
            java.lang.String  phone,
            java.lang.String  forgotPasswordUuid,
            java.lang.String  address,
            java.lang.String  additionalAddress,
            java.lang.String  civility,
            java.lang.String  title,
            java.lang.String  locale
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

    public java.lang.Integer getId() {
        return this.id;
    }

    public void setId(java.lang.Integer id) {
        this.id = id;
    }

    public java.lang.String getLogin() {
        return this.login;
    }

    public void setLogin(java.lang.String login) {
        this.login = login;
    }

    public java.lang.String getPassword() {
        return this.password;
    }

    public void setPassword(java.lang.String password) {
        this.password = password;
    }

    public java.lang.String getFirstname() {
        return this.firstname;
    }

    public void setFirstname(java.lang.String firstname) {
        this.firstname = firstname;
    }

    public java.lang.String getLastname() {
        return this.lastname;
    }

    public void setLastname(java.lang.String lastname) {
        this.lastname = lastname;
    }

    public java.lang.String getEmail() {
        return this.email;
    }

    public void setEmail(java.lang.String email) {
        this.email = email;
    }

    public java.lang.Boolean getActive() {
        return this.active;
    }

    public void setActive(java.lang.Boolean active) {
        this.active = active;
    }

    public java.lang.String getAvatar() {
        return this.avatar;
    }

    public void setAvatar(java.lang.String avatar) {
        this.avatar = avatar;
    }

    public java.lang.String getZip() {
        return this.zip;
    }

    public void setZip(java.lang.String zip) {
        this.zip = zip;
    }

    public java.lang.String getCity() {
        return this.city;
    }

    public void setCity(java.lang.String city) {
        this.city = city;
    }

    public java.lang.String getCountry() {
        return this.country;
    }

    public void setCountry(java.lang.String country) {
        this.country = country;
    }

    public java.lang.String getPhone() {
        return this.phone;
    }

    public void setPhone(java.lang.String phone) {
        this.phone = phone;
    }

    public java.lang.String getForgotPasswordUuid() {
        return this.forgotPasswordUuid;
    }

    public void setForgotPasswordUuid(java.lang.String forgotPasswordUuid) {
        this.forgotPasswordUuid = forgotPasswordUuid;
    }

    public java.lang.String getAddress() {
        return this.address;
    }

    public void setAddress(java.lang.String address) {
        this.address = address;
    }

    public java.lang.String getAdditionalAddress() {
        return this.additionalAddress;
    }

    public void setAdditionalAddress(java.lang.String additionalAddress) {
        this.additionalAddress = additionalAddress;
    }

    public java.lang.String getCivility() {
        return this.civility;
    }

    public void setCivility(java.lang.String civility) {
        this.civility = civility;
    }

    public java.lang.String getTitle() {
        return this.title;
    }

    public void setTitle(java.lang.String title) {
        this.title = title;
    }

    public java.lang.String getLocale() {
        return this.locale;
    }

    public void setLocale(java.lang.String locale) {
        this.locale = locale;
    }
    
    public GroupBrief getGroup() {
        return group;
    }

    public void setGroup(GroupBrief group) {
        this.group = group;
    }
}
