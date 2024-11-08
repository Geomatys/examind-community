package com.examind.openeo.api.rest.data.discovery.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Quentin BIALOTA (Geomatys)
 * Based on : <a href="https://api.openeo.org/#tag/EO-Data-Discovery">OpenEO Doc</a>
 */
public class DimensionOther extends Dimension {
    @JsonProperty("extent")
    @Valid
    private List<BigDecimal> extent = null;

    @JsonProperty("values")
    @Valid
    private List<String> values = null;

    @JsonProperty("step")
    private String step = null;

    @JsonProperty("unit")
    private String unit;

    @JsonProperty("reference_system")
    private String referenceSystem;

    public DimensionOther extent(List<BigDecimal> extent) {
        this.extent = extent;
        return this;
    }

    public DimensionOther addExtentItem(BigDecimal extentItem) {
        if (this.extent == null) {
            this.extent = new ArrayList<>();
        }
        this.extent.add(extentItem);
        return this;
    }

    public List<BigDecimal> getExtent() {
        return extent;
    }

    public void setExtent(List<BigDecimal> extent) {
        this.extent = extent;
    }

    public DimensionOther values(List<String> values) {
        this.values = values;
        return this;
    }

    public DimensionOther addValuesItem(String valuesItem) {
        if (this.values == null) {
            this.values = new ArrayList<>();
        }
        this.values.add(valuesItem);
        return this;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    public DimensionOther step(String step) {
        this.step = step;
        return this;
    }

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public DimensionOther unit(String unit) {
        this.unit = unit;
        return this;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public DimensionOther referenceSystem(String referenceSystem) {
        this.referenceSystem = referenceSystem;
        return this;
    }

    public String getReferenceSystem() {
        return referenceSystem;
    }

    public void setReferenceSystem(String referenceSystem) {
        this.referenceSystem = referenceSystem;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DimensionOther dimensionOther = (DimensionOther) o;
        return Objects.equals(this.extent, dimensionOther.extent) &&
                Objects.equals(this.values, dimensionOther.values) &&
                Objects.equals(this.step, dimensionOther.step) &&
                Objects.equals(this.unit, dimensionOther.unit) &&
                Objects.equals(this.referenceSystem, dimensionOther.referenceSystem) &&
                super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(extent, values, step, unit, referenceSystem, super.hashCode());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class DimensionOther {\n");
        sb.append("    ").append(toIndentedString(super.toString())).append("\n");
        sb.append("    extent: ").append(toIndentedString(extent)).append("\n");
        sb.append("    values: ").append(toIndentedString(values)).append("\n");
        sb.append("    step: ").append(toIndentedString(step)).append("\n");
        sb.append("    unit: ").append(toIndentedString(unit)).append("\n");
        sb.append("    referenceSystem: ").append(toIndentedString(referenceSystem)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(java.lang.Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
