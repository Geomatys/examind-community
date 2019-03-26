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
package org.constellation.dto.portrayal;

import javax.xml.bind.annotation.XmlRegistry;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@XmlRegistry
public class ObjectFactory {

    public TextDecoration createTextDecoration() {
        return new TextDecoration();
    }

    public GridDecoration createGridDecoration() {
        return new GridDecoration();
    }

    public WMSPortrayal createWMSMapPortrayal() {
        return new WMSPortrayal();
    }
}
