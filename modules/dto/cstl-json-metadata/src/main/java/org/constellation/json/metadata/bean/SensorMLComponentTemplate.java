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
import org.geotoolkit.sml.xml.v101.SensorML;
import org.geotoolkit.sml.xml.v101.SensorMLStandard;
import org.springframework.stereotype.Component;

/**
 * @author Quentin Boileau (Geomatys)
 */
@Component("profile_sensorml_component")
public class SensorMLComponentTemplate extends Template {

    public SensorMLComponentTemplate() {
        super("profile_sensorml_component",
              SensorMLStandard.SYSTEM,
              "org/constellation/json/metadata/profile_sensorml_component.json",
              new ArrayList<>(), false);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Object emptyMetadata() {
        return new SensorML();
    }
}
