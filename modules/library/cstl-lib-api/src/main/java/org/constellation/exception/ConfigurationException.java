/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
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

package org.constellation.exception;

/**
 *
 * @author Guilhem Legal
 */
public class ConfigurationException extends ConstellationException {
    private static final long serialVersionUID = 6771141662229835127L;


    private final String cause;

    public ConfigurationException() {
        super();
        this.cause   = "";
    }

    public ConfigurationException(final String message) {
        super(message);
        this.cause   = "";
    }

    public ConfigurationException(final String message, final String cause) {
        super(message);
        this.cause   = cause;
    }

    public ConfigurationException(final String message, final Exception cause) {
        super(message, cause);
        this.cause   = cause.getMessage();
    }

    public ConfigurationException(final Exception cause) {
        super(cause);
        this.cause   = cause.getMessage();
    }

    @Override
    public String toString() {
        return getMessage() + '\n' + "Cause: " + cause;
    }

}
