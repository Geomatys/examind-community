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

import java.util.ArrayList;
import org.apache.sis.metadata.MetadataStandard;
import org.constellation.json.metadata.Template;
import org.constellation.util.ReflectionUtilities;
import org.springframework.stereotype.Component;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component("profile_ebrim")
public class EbrimTemplate extends Template {

    public EbrimTemplate() {
        super("profile_ebrim",
              MetadataStandard.ISO_19115,
             "org/constellation/json/metadata/profile_ebrim.json",
             new ArrayList<>(), false);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean matchMetadata(Object meta) {
        return ReflectionUtilities.instanceOf("org.geotoolkit.ebrim.xml.v250.RegistryObjectType", meta.getClass()) ||
               ReflectionUtilities.instanceOf("org.geotoolkit.ebrim.xml.v300.IdentifiableType", meta.getClass());
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Object emptyMetadata() {
        try {
            Class c = Class.forName("org.geotoolkit.ebrim.xml.v300.IdentifiableType");
            return ReflectionUtilities.newInstance(c);
        } catch (ClassNotFoundException ex) {}
        return null;
    }
}

