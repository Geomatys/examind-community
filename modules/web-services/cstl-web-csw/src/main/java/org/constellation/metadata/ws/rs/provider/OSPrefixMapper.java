/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2020 Geomatys.
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
package org.constellation.metadata.ws.rs.provider;

import java.util.HashMap;
import java.util.Map;
import org.glassfish.jaxb.runtime.marshaller.NamespacePrefixMapper;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class OSPrefixMapper extends NamespacePrefixMapper {

    private final Map<String, String> namespaceMap = new HashMap<>();

    /**
     * Create mappings.
     */
    public OSPrefixMapper() {
        namespaceMap.put("http://www.w3.org/2001/XMLSchema-instance", "xsi");
        namespaceMap.put("http://www.w3.org/1999/xlink", "xlink");
        namespaceMap.put("http://www.opengis.net/gml/3.2", "gml");
        namespaceMap.put("http://earth.esa.int/eop", "eop");
        namespaceMap.put("http://earth.esa.int/opt", "opt");
        namespaceMap.put("http://www.georss.org/georss", "georss");
        namespaceMap.put("http://www.w3.org/2005/Atom", "a");
        namespaceMap.put("http://a9.com/-/spec/opensearch/1.1/", "");
        namespaceMap.put("http://a9.com/-/spec/opensearch/extensions/parameters/1.0/", "param");
        namespaceMap.put("http://a9.com/-/opensearch/extensions/time/1.0/", "time");
        namespaceMap.put("http://a9.com/-/opensearch/extensions/geo/1.0/", "geo");
        namespaceMap.put("http://purl.org/dc/elements/1.1/", "dc");
    }

    /* (non-Javadoc)
     * Returning null when not found based on spec.
     * @see NamespacePrefixMapper#getPreferredPrefix(java.lang.String, java.lang.String, boolean)
     */
    @Override
    public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
        return namespaceMap.getOrDefault(namespaceUri, suggestion);
    }
}
