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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geotoolkit.internal.geojson.binding.GeoJSONGeometry;
import org.geotoolkit.sts.STSRequest;

/**
 * temporary replace of {@link org.geotoolkit.sts.ExpandOptions} which has the problem of infinite sublevel of expand.
 *
 * @author Guilhem Legal (Geomatys)
 */
public class RequestOptions {

    protected static class FieldInfo {
        public final boolean expanded;
        public final boolean selected;

        protected FieldInfo(boolean expanded, boolean selected) {
            this.expanded = expanded;
            this.selected = selected;
        }
    }

    public final FieldInfo multiDatastreams;
    public final FieldInfo featureOfInterest;
    public final FieldInfo datastreams;
    public final FieldInfo observedProperties;
    public final FieldInfo observations;
    public final FieldInfo sensors;
    public final FieldInfo things;
    public final FieldInfo historicalLocations;
    public final FieldInfo locations;

    /**
     * Used for caching parsed sensor geometrie during a request
     */
    public final Map<String, GeoJSONGeometry> sensorArea;

    /**
     * Used for caching parsed formatted times during a request
     *
     */
    public final Map<List<Object>, String> timesCache;

    private boolean topLevel;

    private final List<String> select;
    private final List<String> expand;

    public RequestOptions(STSRequest req) {
        this(req.getExpand(), req.getSelect(), true, new HashMap<>(), new HashMap<>());
    }

    protected RequestOptions(List<String> expandList, List<String> selectList, boolean topLevell, Map<String, GeoJSONGeometry> sensorArea, Map<List<Object>, String> timesCache) {
        topLevel            = topLevell;
        expand              = new ArrayList<>();
        if (expandList != null) {
            for (String ex : expandList) {
                expand.add(ex.toLowerCase());
            }
        }
        select               = new ArrayList<>();
        if (selectList != null) {
            for (String ex : selectList) {
                select.add(ex.toLowerCase());
            }
        }
        multiDatastreams    = new FieldInfo(isExpand("multidatastreams"), isSelect("multidatastreams"));
        featureOfInterest   = new FieldInfo(isExpand("featureofinterest", "featuresofinterest"), isSelect("featureofinterest", "featuresofinterest"));
        datastreams         = new FieldInfo(isExpand("datastreams"), isSelect("datastreams"));
        observedProperties  = new FieldInfo(isExpand("observedproperties", "observedproperty"), isSelect("observedproperties", "observedproperty"));
        observations        = new FieldInfo(isExpand("observations"), isSelect("observations"));
        sensors             = new FieldInfo(isExpand("sensors"), isSelect("sensors"));
        things              = new FieldInfo(isExpand("things", "thing"), isSelect("things", "thing"));
        historicalLocations = new FieldInfo(isExpand("historicallocations"), isSelect("historicallocations"));
        locations           = new FieldInfo(isExpand("locations"), isSelect("locations"));

        this.sensorArea = sensorArea;
        this.timesCache = timesCache;
    }


    /**
     * Return {@code true} if the attribute is part of the selection.
     * 
     * @param attribute the attribute to search not {@code null}
     * @return 
     */
    public boolean isSelected(String attribute) {
        return isSelected(attribute, false);
    }
    
    /**
     * Return {@code true} if the attribute is part of the selection.
     * 
     * @param attribute the attribute to search not {@code null}
     * @param complexAttribute If set to true, the method will search for partial selection in a complexe attribute context.
     * @return 
     */
    public boolean isSelected(String attribute, boolean complexAttribute) {
        if (select.isEmpty()) {
            return true;
        }
        attribute = attribute.toLowerCase();
        for (String sel : select) {
            if (sel.equals(attribute) || 
               (complexAttribute && (sel.startsWith(attribute) || attribute.startsWith(sel)))) {  // integrate main complex attribute
                
                return true;
            }
        }
        return false;
    }

    private boolean isSelect(String entity) {
        return isSelect(entity, null);
    }

    private boolean isSelect(String entity, String alternate) {
        if (select.isEmpty()) {
            return true;
        }
        for (String sel : select) {
            if (sel.startsWith(entity) || (alternate != null && sel.startsWith(alternate))) {
                return true;
            }
        }
        return false;
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

    public RequestOptions subLevel(String forEntity) {
        if (topLevel) {
            return new RequestOptions(new ArrayList<>(expand), new ArrayList<>(select), false, sensorArea, timesCache);
        }
        List<String> entityNames = alternates(forEntity);
        List<String> newExpand = new ArrayList<>();
        for (String ex : expand) {
            for (String entityName : entityNames) {
                if (ex.startsWith(entityName + '/')) {
                    newExpand.add(ex.substring(entityName.length() + 1));
                }
            }
        }
        List<String> newSelect = new ArrayList<>();
        for (String sel : select) {
            for (String entityName : entityNames) {
                if (sel.startsWith(entityName + '/')) {
                    newSelect.add(sel.substring(entityName.length() + 1));
                }
            }
        }
        return new RequestOptions(newExpand, newSelect, false, sensorArea, timesCache);
    }
    
    private List<String> alternates(String entity) {
        entity = entity.toLowerCase();
        switch(entity) {
            case "multidatastreams"    : return List.of("multidatastreams");
            case "featureofinterest"   :
            case"featuresofinterest"   : return List.of("featureofinterest", "featuresofinterest");
            case "datastreams"         : return List.of("datastreams");
            case "observedproperties"  :
            case "observedproperty"    : return List.of("observedproperties", "observedproperty");
            case "observations"        : return List.of("observations");
            case "sensors"             : return List.of("sensors");
            case "things"              :
            case "thing"               : return List.of("things", "thing");
            case "historicallocations" : return List.of("historicallocations");
            case "locations"           : return List.of("locations");
            default: throw new IllegalArgumentException("Invalid entity label: " + entity);
        }
    }
}
