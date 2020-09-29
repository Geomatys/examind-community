/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2020 Geomatys.
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

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class DimensionRange {

    private Double min;
    private Double max;
    private String unit;
    private String unitsymbol;

    public DimensionRange() {
    }

    public DimensionRange(
            Double min,
            Double max,
            String unit,
            String unitsymbol
    ) {
        this.min = min;
        this.max = max;
        this.unit = unit;
        this.unitsymbol = unitsymbol;
    }

    /**
     * @return the min
     */
    public Double getMin() {
        return min;
    }

    /**
     * @param min the min to set
     */
    public void setMin(Double min) {
        this.min = min;
    }

    /**
     * @return the max
     */
    public Double getMax() {
        return max;
    }

    /**
     * @param max the max to set
     */
    public void setMax(Double max) {
        this.max = max;
    }

    /**
     * @return the unit
     */
    public String getUnit() {
        return unit;
    }

    /**
     * @param unit the unit to set
     */
    public void setUnit(String unit) {
        this.unit = unit;
    }

    /**
     * @return the unitsymbol
     */
    public String getUnitsymbol() {
        return unitsymbol;
    }

    /**
     * @param unitsymbol the unitsymbol to set
     */
    public void setUnitsymbol(String unitsymbol) {
        this.unitsymbol = unitsymbol;
    }
}
