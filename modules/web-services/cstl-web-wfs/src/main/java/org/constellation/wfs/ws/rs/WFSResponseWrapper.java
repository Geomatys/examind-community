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

package org.constellation.wfs.ws.rs;

import org.geotoolkit.wfs.xml.WFSResponse;

/**
 *
 * @author Guilhem Legal
 */
public class WFSResponseWrapper implements WFSResponse {

    private final Object response;
    private final String version;

    public WFSResponseWrapper(final Object response,String version) {
        this.response = response;
        this.version = version;
    }

    /**
     * @return the response
     */
    public Object getResponse() {
        return response;
    }

    @Override
    public String getVersion() {
        return version;
    }
    
}
