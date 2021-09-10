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
package com.examind.sts.core;

import java.util.ArrayList;
import java.util.List;
import org.geotoolkit.sts.STSRequest;

/**
 * temporary replace of {@link org.geotoolkit.sts.ExpandOptions} which has the problem of infinite sublevel of expand.
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ExpandOptions {

    public final boolean multiDatastreams;
    public final boolean featureOfInterest;
    public final boolean datastreams;
    public final boolean observedProperties;
    public final boolean observations;
    public final boolean sensors;
    public final boolean things;
    public final boolean historicalLocations;
    public final boolean locations;

    private boolean topLevel;

    private final List<String> expand;

    public ExpandOptions(STSRequest req) {
        this(req.getExpand(), true);
    }

    protected ExpandOptions(List<String> expandList, boolean topLevell) {
        topLevel            = topLevell;
        expand              = new ArrayList<>();
        for (String ex : expandList) {
            expand.add(ex.toLowerCase());
        }
        multiDatastreams    = isExpand("multidatastreams");
        featureOfInterest   = isExpand("featureofinterest", "featuresofinterest");
        datastreams         = isExpand("datastreams");
        observedProperties  = isExpand("observedproperties", "observedproperty");
        observations        = isExpand("observations");
        sensors             = isExpand("sensors");
        things              = isExpand("things");
        historicalLocations = isExpand("historicallocations");
        locations           = isExpand("locations");
    }

    private boolean isExpand(String entity) {
        return isExpand(entity, null);
    }
    
    private boolean isExpand(String entity, String alternate) {
        for (String ex : expand) {
            if (ex.startsWith(entity) || (alternate != null && ex.startsWith(alternate))) {
                return true;
            }
        }
        return false;
    }

    public ExpandOptions subLevel(String forEntity) {
        if (topLevel) {
            return new ExpandOptions(new ArrayList<>(expand), false);
        }
        forEntity = forEntity.toLowerCase();
        List<String> newExpand = new ArrayList<>();
        for (String ex : expand) {
            if (ex.startsWith(forEntity + '/')) {
                newExpand.add(ex.substring(forEntity.length() + 1));
            }
        }
        return new ExpandOptions(newExpand, false);
    }
}
