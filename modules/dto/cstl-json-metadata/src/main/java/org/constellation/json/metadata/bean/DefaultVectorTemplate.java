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
package org.constellation.json.metadata.bean;

import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.constellation.json.metadata.Template;
import org.springframework.stereotype.Component;

/**
 * @author Quentin Boileau (Geomatys)
 */
@Component("profile_default_vector")
public class DefaultVectorTemplate extends Template {


    public DefaultVectorTemplate() {
        super(MetadataStandard.ISO_19115, "org/constellation/json/metadata/profile_default_vector.json");
    }

    @Override
    public String getIdentifier() {
        return "profile_default_vector";
    }

    @Override
    public boolean isDefault() {
        return true;
    }

    @Override
    public boolean matchMetadata(Object metadata) {
        if(metadata instanceof DefaultMetadata) {
            String standardName = ((DefaultMetadata)metadata).getMetadataStandardName();
            if ("x-urn:schema:ISO19115:INSPIRE:dataset:geo-vector".equals(standardName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean matchDataType(String dataType) {
        return "vector".equalsIgnoreCase(dataType);
    }
}
