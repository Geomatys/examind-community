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
package org.constellation.ws;

import java.util.Comparator;

/**
 * Order the {@link LayerCache} by identifier name.
 * 
 * @author Guilhem Legal
 */
public class LayerCacheComparator implements Comparator<LayerCache> {

    @Override
    public int compare(LayerCache o1, LayerCache o2) {
        return LayerWorker.identifier(o1).compareTo(LayerWorker.identifier(o2));
    }

}
