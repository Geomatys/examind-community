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

    private boolean topLevel;

    private final List<String> select;
    private final List<String> expand;

    public RequestOptions(STSRequest req) {
        this(req.getExpand(), req.getSelect(), true);
    }

    protected RequestOptions(List<String> expandList, List<String> selectList, boolean topLevell) {
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
        things              = new FieldInfo(isExpand("things"), isSelect("things"));
        historicalLocations = new FieldInfo(isExpand("historicallocations"), isSelect("historicallocations"));
        locations           = new FieldInfo(isExpand("locations"), isSelect("locations"));
    }


    public boolean isSelected(String attribute) {
        if (select.isEmpty()) {
            return true;
        }
        for (String sel : select) {
            if (sel.equalsIgnoreCase(attribute)) {
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
            return new RequestOptions(new ArrayList<>(expand), new ArrayList<>(select), false);
        }
        forEntity = forEntity.toLowerCase();
        List<String> newExpand = new ArrayList<>();
        for (String ex : expand) {
            if (ex.startsWith(forEntity + '/')) {
                newExpand.add(ex.substring(forEntity.length() + 1));
            }
        }
        List<String> newSelect = new ArrayList<>();
        for (String sel : select) {
            if (sel.startsWith(forEntity + '/')) {
                newSelect.add(sel.substring(forEntity.length() + 1));
            }
        }
        return new RequestOptions(newExpand, newSelect, false);
    }
}
