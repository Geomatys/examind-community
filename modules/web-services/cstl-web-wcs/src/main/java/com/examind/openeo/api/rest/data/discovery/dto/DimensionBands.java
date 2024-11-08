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
public class DimensionBands extends Dimension {

    public DimensionBands() {}

    public DimensionBands(List<String> values) {
        this.values = values;
    }

    @JsonProperty("values")
    @Valid
    private List<String> values = new ArrayList<>();

    public DimensionBands values(List<String> values) {
        this.values = values;
        return this;
    }

    public DimensionBands addValuesItem(String valuesItem) {
        this.values.add(valuesItem);
        return this;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    public boolean containsValue(String value) {
        return values.contains(value);
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DimensionBands dimensionBands = (DimensionBands) o;
        return Objects.equals(this.values, dimensionBands.values) &&
                super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(values, super.hashCode());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class DimensionBands {\n");
        sb.append("    ").append(toIndentedString(super.toString())).append("\n");
        sb.append("    values: ").append(toIndentedString(values)).append("\n");
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
