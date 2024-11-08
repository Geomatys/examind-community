package com.examind.openeo.api.rest.data.discovery.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Quentin BIALOTA (Geomatys)
 * Based on : <a href="https://api.openeo.org/#tag/EO-Data-Discovery">OpenEO Doc</a>
 */
public class DimensionTemporal extends Dimension {

    public DimensionTemporal() {}

    public DimensionTemporal(List<String> values, List<String> extent, String step) {
        this.values = values;
        this.extent = extent;
        this.step = step;
    }

    @JsonProperty("values")
    @Valid
    private List<String> values = null;

    @JsonProperty("extent")
    @Valid
    private List<String> extent = new ArrayList<>();

    @JsonProperty("step")
    private String step = null;

    public DimensionTemporal values(List<String> values) {
        this.values = values;
        return this;
    }

    public DimensionTemporal addValuesItem(String valuesItem) {
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

    public DimensionTemporal extent(List<String> extent) {
        this.extent = extent;
        return this;
    }

    public DimensionTemporal addExtentItem(String extentItem) {
        this.extent.add(extentItem);
        return this;
    }

    public List<String> getExtent() {
        return extent;
    }

    public void setExtent(List<String> extent) {
        this.extent = extent;
    }

    public DimensionTemporal step(String step) {
        this.step = step;
        return this;
    }

    public String get
    () {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DimensionTemporal dimensionTemporal = (DimensionTemporal) o;
        return Objects.equals(this.values, dimensionTemporal.values) &&
                Objects.equals(this.extent, dimensionTemporal.extent) &&
                Objects.equals(this.step, dimensionTemporal.step) &&
                super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(values, extent, step, super.hashCode());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class DimensionTemporal {\n");
        sb.append("    ").append(toIndentedString(super.toString())).append("\n");
        sb.append("    values: ").append(toIndentedString(values)).append("\n");
        sb.append("    extent: ").append(toIndentedString(extent)).append("\n");
        sb.append("    step: ").append(toIndentedString(step)).append("\n");
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
