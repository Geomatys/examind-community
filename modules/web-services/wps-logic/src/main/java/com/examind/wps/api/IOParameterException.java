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
package com.examind.wps.api;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class IOParameterException extends WPSException {

    private final String paramId;

    private final boolean unsupported;

    private final boolean missing;

    public IOParameterException(String message, String paramId) {
        super(message);
        this.paramId = paramId;
        this.unsupported = false;
        this.missing = false;
    }

    public IOParameterException(String message, Exception cause, String paramId) {
        super(message, cause);
        this.paramId = paramId;
        this.unsupported = false;
        this.missing = false;
    }

    public IOParameterException(String message, boolean unsupported, boolean missing, String paramId) {
        super(message);
        this.paramId = paramId;
        this.unsupported = unsupported;
        this.missing = missing;
    }

    public IOParameterException(String message, Exception cause, boolean unsupported, String paramId) {
        super(message, cause);
        this.paramId = paramId;
        this.unsupported = unsupported;
        this.missing = false;
    }

    public String getParamId() {
        return paramId;
    }

    public boolean isUnsupported() {
        return unsupported;
    }

    public boolean isMissing() {
        return missing;
    }

}
