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
import java.util.Date;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.constellation.json.metadata.Template;
import org.constellation.metadata.utils.MetadataFeeder;
import org.springframework.stereotype.Component;

/**
 * @author Quentin Boileau (Geomatys)
 */
@Component("profile_import")
public class ImportTemplate extends Template {

    public ImportTemplate() {
        super("profile_import",
              MetadataStandard.ISO_19115,
              "org/constellation/json/metadata/profile_import.json",
              new ArrayList<>(), true);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Object emptyMetadata() {
        DefaultMetadata meta = new DefaultMetadata();
        MetadataFeeder feeder = new MetadataFeeder(meta);
        feeder.setCreationDate(new Date());
        return meta;
    }

}
