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
import org.constellation.json.metadata.Template;
import org.geotoolkit.feature.catalog.FeatureCatalogueImpl;
import org.geotoolkit.feature.catalog.FeatureCatalogueStandard;
import org.springframework.stereotype.Component;

/**
 * @author Guilhem Legal (Geomatys)
 */
@Component("profile_default_feature_catalogue")
public class FeatureCatalogueTemplate extends Template {

    public FeatureCatalogueTemplate() {
        super("profile_default_feature_catalogue",
              FeatureCatalogueStandard.ISO_19110,
              "org/constellation/json/metadata/profile_feature_catalogue.json",
              new ArrayList<>(), false);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean isDefault() {
        return true;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean matchMetadata(Object metadata) {
        return metadata instanceof FeatureCatalogueImpl;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Object emptyMetadata() {
        return new FeatureCatalogueImpl();
    }
}
