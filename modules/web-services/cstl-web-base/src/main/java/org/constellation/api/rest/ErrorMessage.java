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

import java.io.PrintWriter;
import java.io.StringWriter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Error object returned by Rest API.
 * 
 * @author Johann Sorel (Geomatys)
 */
public class ErrorMessage {
    
    public int status = HttpStatus.INTERNAL_SERVER_ERROR.value();
    public String errorMessageI18nCode = I18nCodes.API_MSG_SERVER_ERROR;
    public String errorMessage;
    public String errorStackTrace;

    /**
     * Create a defautl error message configured with status 500 and unknowned
     * exception i18 code.
     * 
     */
    public ErrorMessage() {
    }

    public ErrorMessage(HttpStatus status) {
        this.status = status.value();
    }

    public ErrorMessage(String errorMsg) {
        this.errorMessage = errorMsg;
    }

    public ErrorMessage(String errorMsg, Throwable ex) {
        error(ex);
        this.errorMessage = errorMsg;
    }
    
    public ErrorMessage(Throwable ex) {
        this(null, ex);
    }
    
    public ErrorMessage(int status, String errorMessageI18nCode, String errorMessage, String errorStackTrace) {
        this.status = status;
        this.errorMessageI18nCode = errorMessageI18nCode;
        this.errorMessage = errorMessage;
        this.errorStackTrace = errorStackTrace;
    }

    
    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getErrorMessageI18nCode() {
        return errorMessageI18nCode;
    }
    
    public void setErrorMessageI18nCode(String errorMessageI18nCode) {
        this.errorMessageI18nCode = errorMessageI18nCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorStackTrace() {
        return errorStackTrace;
    }

    public void setErrorStackTrace(String errorStackTrace) {
        this.errorStackTrace = errorStackTrace;
    }
    
    /**
     * Configure error message and stach from given exception.
     * 
     * @param ex extract informations from given exception
     * @return this ErrorMessage
     */
    public ErrorMessage error(Throwable ex){
        if (ex != null) {
            this.errorMessage = ex.getLocalizedMessage();        
            final StringWriter swriter = new StringWriter();
            final PrintWriter writer = new PrintWriter(swriter, true);
            ex.printStackTrace(writer);
            writer.flush();
            this.errorStackTrace = swriter.toString();
        }
        return this;
    }
    
    /**
     * Set translation bundle key.
     * 
     * @param code i18n code
     * @return this ErrorMessage
     */
    public ErrorMessage i18N(String code){
        this.errorMessageI18nCode = code;
        return this;
    }
    
    /**
     * Set http status code.
     * 
     * @param status
     * @return this ErrorMessage
     */
    public ErrorMessage status(HttpStatus status){
        this.status = status.value();
        return this;
    }
    
    /**
     * Set http status code.
     * 
     * @param status
     * @return this ErrorMessage
     */
    public ErrorMessage status(int status){
        this.status = status;
        return this;
    }
    
    /**
     * Set http error message.
     * 
     * @param message
     * @return this ErrorMessage
     */
    public ErrorMessage message(String message){
        this.errorMessage = message;
        return this;
    }
    
    /**
     * Build spring ResponseEntity.
     * 
     * @return ResponseEntity, never null
     */
    public ResponseEntity build(){
        return new ResponseEntity(this, HttpStatus.valueOf(status));
    }
    
}
