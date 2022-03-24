/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2021 Geomatys.
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
package org.constellation.provider;

import org.opengis.util.GenericName;

import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.Resource;

import org.constellation.api.DataType;

/**
 * Fallback for data with a resource we can't handle.
 * 
 * @author Guilhem Legal(Geomatys)
 */
public class DefaultOtherData extends AbstractData {

    public DefaultOtherData(GenericName name, final Resource ref, final DataStore store) {
        super(name, ref, store);
    }

    @Override
    public DataType getDataType() {
        return DataType.OTHER;
    }
}
