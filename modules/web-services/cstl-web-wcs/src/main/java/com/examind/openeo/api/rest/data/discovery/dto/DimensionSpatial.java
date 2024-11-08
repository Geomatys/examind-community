package com.examind.openeo.api.rest.data.discovery.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Quentin BIALOTA (Geomatys)
 * Based on : <a href="https://api.openeo.org/#tag/EO-Data-Discovery">OpenEO Doc</a>
 */
public class DimensionSpatial extends Dimension {

    /**
     * Axis of the spatial dimension (`x`, `y` or `z`).
     */
    public enum AxisEnum {
        X("x"),
        Y("y"),
        Z("z");

        private String value;

        AxisEnum(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        @JsonCreator
        public static AxisEnum fromValue(String value) {
            for (AxisEnum b : AxisEnum.values()) {
                if (b.value.equals(value)) {
                    return b;
                }
            }
            throw new IllegalArgumentException("Unexpected value '" + value + "'");
        }
    }

    public DimensionSpatial(AxisEnum axis, List<Double> extent, List<Double> values, String step, String unit, Integer referenceSystem) {
        this.axis = axis;
        this.extent = extent;
        this.values = values;
        this.step = step;
        this.unit = unit;
        this.referenceSystem = referenceSystem;
    }

    @JsonProperty("axis")
    private AxisEnum axis;

    @JsonProperty("extent")
    @Valid
    private List<Double> extent = null;

    @JsonProperty("values")
    @Valid
    private List<Double> values = null;

    @JsonProperty("step")
    private String step = null;

    /** Units should be compliant with {@link https://ncics.org/portfolio/other-resources/udunits2/}. */
    @JsonProperty("unit")
    private String unit = null;

    @JsonProperty("reference_system")
    private Integer referenceSystem = null;

    public DimensionSpatial axis(AxisEnum axis) {
        this.axis = axis;
        return this;
    }

    public AxisEnum getAxis() {
        return axis;
    }

    public void setAxis(AxisEnum axis) {
        this.axis = axis;
    }

    public DimensionSpatial extent(List<Double> extent) {
        this.extent = extent;
        return this;
    }

    public DimensionSpatial addExtentItem(Double extentItem) {
        if (this.extent == null) {
            this.extent = new ArrayList<>();
        }
        this.extent.add(extentItem);
        return this;
    }

    public List<Double> getExtent() {
        return extent;
    }

    public void setExtent(List<Double> extent) {
        this.extent = extent;
    }

    public DimensionSpatial values(List<Double> values) {
        this.values = values;
        return this;
    }

    public DimensionSpatial addValuesItem(Double valuesItem) {
        if (this.values == null) {
            this.values = new ArrayList<>();
        }
        this.values.add(valuesItem);
        return this;
    }

    public List<Double> getValues() {
        return values;
    }

    public void setValues(List<Double> values) {
        this.values = values;
    }

    public DimensionSpatial step(String step) {
        this.step = step;
        return this;
    }

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public DimensionSpatial referenceSystem(Integer referenceSystem) {
        this.referenceSystem = referenceSystem;
        return this;
    }

    public Integer getReferenceSystem() {
        return referenceSystem;
    }

    public void setReferenceSystem(Integer referenceSystem) {
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
        DimensionSpatial dimensionSpatial = (DimensionSpatial) o;
        return Objects.equals(this.axis, dimensionSpatial.axis) &&
                Objects.equals(this.extent, dimensionSpatial.extent) &&
                Objects.equals(this.values, dimensionSpatial.values) &&
                Objects.equals(this.step, dimensionSpatial.step) &&
                Objects.equals(this.referenceSystem, dimensionSpatial.referenceSystem) &&
                super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(axis, extent, values, step, referenceSystem, super.hashCode());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class DimensionSpatial {\n");
        sb.append("    ").append(toIndentedString(super.toString())).append("\n");
        sb.append("    axis: ").append(toIndentedString(axis)).append("\n");
        sb.append("    extent: ").append(toIndentedString(extent)).append("\n");
        sb.append("    values: ").append(toIndentedString(values)).append("\n");
        sb.append("    step: ").append(toIndentedString(step)).append("\n");
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
