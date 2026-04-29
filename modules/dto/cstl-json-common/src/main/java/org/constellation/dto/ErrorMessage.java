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
package org.constellation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Error object returned by Rest API.
 *
 * https://github.com/opengeospatial/OGC-Web-API-Guidelines#principle-7--error-handling-and-use-of-http-status-codes
 *
 * @author Johann Sorel (Geomatys)
 */
@JsonPropertyOrder({
    ErrorMessage.JSON_PROPERTY_DEVELOPER_MESSAGE,
    ErrorMessage.JSON_PROPERTY_USER_MESSAGE,
    ErrorMessage.JSON_PROPERTY_ERROR_CODE,
    ErrorMessage.JSON_PROPERTY_ERROR_STACKTRACE,
    ErrorMessage.JSON_PROPERTY_CONTACT_DETAILS
})
@XmlRootElement(name = "Error")
@XmlAccessorType(XmlAccessType.FIELD)
public class ErrorMessage {

    public static final String JSON_PROPERTY_DEVELOPER_MESSAGE = "developer_message";
    @XmlElement(name = "developer_message")
    private String developerMessage;

    public static final String JSON_PROPERTY_USER_MESSAGE = "user_message";
    @XmlElement(name = "user_message")
    private String userMessage;

    public static final String JSON_PROPERTY_ERROR_CODE = "error_code";
    @XmlElement(name = "error_code")
    private String errorCode;

    /**
     * Not defined in the guildeline, but seems a crucial debugging property.
     */
    public static final String JSON_PROPERTY_ERROR_STACKTRACE = "error_stacktrace";
    @XmlElement(name = "error_stacktrace")
    private String errorStacktrace;

    public static final String JSON_PROPERTY_CONTACT_DETAILS = "contact_details";
    @XmlElement(name = "contact_details")
    private String contactDetails;

    public ErrorMessage() {
    }

    @JsonProperty(JSON_PROPERTY_DEVELOPER_MESSAGE)
    public String getDeveloperMessage() {
        return developerMessage;
    }

    @JsonProperty(JSON_PROPERTY_DEVELOPER_MESSAGE)
    public void setDeveloperMessage(String developerMessage) {
        this.developerMessage = developerMessage;
    }

    @JsonProperty(JSON_PROPERTY_USER_MESSAGE)
    public String getUserMessage() {
        return userMessage;
    }

    @JsonProperty(JSON_PROPERTY_USER_MESSAGE)
    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    @JsonProperty(JSON_PROPERTY_ERROR_CODE)
    public String getErrorCode() {
        return errorCode;
    }

    @JsonProperty(JSON_PROPERTY_ERROR_CODE)
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    @JsonProperty(JSON_PROPERTY_ERROR_STACKTRACE)
    public String getErrorStacktrace() {
        return errorStacktrace;
    }

    @JsonProperty(JSON_PROPERTY_ERROR_STACKTRACE)
    public void setErrorStacktrace(String errorStacktrace) {
        this.errorStacktrace = errorStacktrace;
    }

    @JsonProperty(JSON_PROPERTY_CONTACT_DETAILS)
    public String getContactDetails() {
        return contactDetails;
    }

    @JsonProperty(JSON_PROPERTY_CONTACT_DETAILS)
    public void setContactDetails(String contactDetails) {
        this.contactDetails = contactDetails;
    }

}
