/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2015-2016 Geomatys.
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

import java.io.Serializable;
import java.util.List;
import org.opengis.filter.MatchAction;

/**
 * @author Fabien Bernard (Geomatys).
 * @author Mehdi Sidhoum (Geomatys).
 */
public class Filter implements Serializable {

    private static final long serialVersionUID = -1224746509528265809L;

    private String field;

    private String value;

    private String operator;

    private List<Filter> filters;

    private Boolean matchCase;

    private MatchAction matchAction;

    private Double distance;

    private String units;

    public Filter() {}

    public Filter(final String field, final String value) {
        this.field = field;
        this.value = value;
    }

    public Filter(final String field, final String value, String operator) {
        this.field = field;
        this.value = value;
        this.operator = operator;
    }

    public Filter(String operator, List<Filter> filters) {
        this.operator = operator;
        this.filters = filters;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public List<Filter> getFilters() {
        return filters;
    }

    public void setFilters(List<Filter> filters) {
        this.filters = filters;
    }

    public Boolean getMatchCase() {
        return matchCase;
    }

    public void setMatchCase(Boolean matchCase) {
        this.matchCase = matchCase;
    }

    public MatchAction getMatchAction() {
        return matchAction;
    }

    public void setMatchAction(MatchAction matchAction) {
        this.matchAction = matchAction;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

}
