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
package org.constellation.util;

import java.util.Comparator;
import org.constellation.dto.NameInProvider;

/**
 *
 * @author guilhem
 */
public class NameInProviderComparator  implements Comparator<NameInProvider>{

    @Override
    public int compare(final NameInProvider o1, final NameInProvider o2) {
        if (o1 != null && o2 != null) {
            String nmsp1 = o1.layerName.getNamespaceURI();
            String nmsp2 = o2.layerName.getNamespaceURI();
            if (nmsp1 != null && nmsp2 != null) {
                if (nmsp1.equals(nmsp2)) {
                    return o1.layerName.getLocalPart().compareTo(o2.layerName.getLocalPart());
                } else {
                    return nmsp1.compareTo(nmsp2);
                }
            }
            return o1.layerName.getLocalPart().compareTo(o2.layerName.getLocalPart());
        }
        return -1;
    }
}
