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
package org.constellation.process.dynamic.galaxy;

/**
 * @author Quentin BIALOTA (Geomatys)
 */
public class GalaxyException extends Exception {

    private String responseBody;

    public GalaxyException(String message) {
        super(message);
    }

    public GalaxyException(Exception cause) {
        super(cause);
    }

    public GalaxyException(Exception cause, String responseBody) {
        super(cause);
        this.responseBody = responseBody;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
