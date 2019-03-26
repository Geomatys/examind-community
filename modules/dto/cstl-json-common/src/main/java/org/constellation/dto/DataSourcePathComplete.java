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

import java.util.Map;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class DataSourcePathComplete extends DataSourcePath {

    private Map<String, String> types;

    public DataSourcePathComplete() {
        super(null, null, null, Boolean.FALSE, null, null);
    }

    public DataSourcePathComplete(DataSourcePath dsPath, Map<String, String> types) {
        super(dsPath.getDatasourceId(), dsPath.getPath(), dsPath.getName(), dsPath.getFolder(), dsPath.getParentPath(), dsPath.getSize());
        this.types = types;
    }

    /**
     * @return the types
     */
    public Map<String, String> getTypes() {
        return types;
    }

    /**
     * @param types the types to set
     */
    public void setTypes(Map<String, String> types) {
        this.types = types;
    }
}
