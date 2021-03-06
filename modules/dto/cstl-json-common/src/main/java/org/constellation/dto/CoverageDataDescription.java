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

package org.constellation.dto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Fabien Bernard (Geomatys).
 * @version 0.9
 * @since 0.9
 */
public class CoverageDataDescription extends SimpleDataDescription {

    private List<BandDescription> bands;

    public CoverageDataDescription() {
        super();
        bands = new ArrayList<>(0);
    }

    public List<BandDescription> getBands() {
        return bands;
    }

    public void setBands(final List<BandDescription> bands) {
        this.bands = bands;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("boundingBox:[").append(Arrays.toString(boundingBox)).append("]\n");
        sb.append("bnds:\n");
        for (BandDescription band : bands) {
            sb.append(band);
        }
        return sb.toString();
    }
}
