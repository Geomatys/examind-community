package com.examind.openeo.api.rest.capabilities.dto;

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
 * Based on : <a href="https://api.openeo.org/#tag/Capabilities">OpenEO Doc</a>
 */
public class Argument {

    public enum TypeEnum {
        STRING("string"),
        NUMBER("number"),
        INTEGER("integer"),
        BOOLEAN("boolean"),
        ARRAY("array"),
        OBJECT("object");

        private String value;

        TypeEnum(String value) {
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
        public static TypeEnum fromValue(String value) {
            for (TypeEnum b : TypeEnum.values()) {
                if (b.value.equals(value)) {
                    return b;
                }
            }
            throw new IllegalArgumentException("Unexpected value '" + value + "'");
        }
    }

    @JsonProperty("type")
    private TypeEnum type;

    @JsonProperty("description")
    private String description;

    @JsonProperty("required")
    private Boolean required = false;

    @JsonProperty("default")
    private Object _default = null;

    @JsonProperty("minimum")
    private BigDecimal minimum;

    @JsonProperty("maximum")
    private BigDecimal maximum;

    @JsonProperty("enum")
    @Valid
    private List<Object> _enum = null;

    @JsonProperty("example")
    private Object example = null;

    public Argument type(TypeEnum type) {
        this.type = type;
        return this;
    }

    public TypeEnum getType() {
        return type;
    }

    public void setType(TypeEnum type) {
        this.type = type;
    }

    public Argument description(String description) {
        this.description = description;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Argument required(Boolean required) {
        this.required = required;
        return this;
    }

    public Boolean isRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public Argument _default(Object _default) {
        this._default = _default;
        return this;
    }

    public Object getDefault() {
        return _default;
    }

    public void setDefault(Object _default) {
        this._default = _default;
    }

    public Argument minimum(BigDecimal minimum) {
        this.minimum = minimum;
        return this;
    }

    public BigDecimal getMinimum() {
        return minimum;
    }

    public void setMinimum(BigDecimal minimum) {
        this.minimum = minimum;
    }

    public Argument maximum(BigDecimal maximum) {
        this.maximum = maximum;
        return this;
    }

    public BigDecimal getMaximum() {
        return maximum;
    }

    public void setMaximum(BigDecimal maximum) {
        this.maximum = maximum;
    }

    public Argument _enum(List<Object> _enum) {
        this._enum = _enum;
        return this;
    }

    public Argument addEnumItem(Object _enumItem) {
        if (this._enum == null) {
            this._enum = new ArrayList<>();
        }
        this._enum.add(_enumItem);
        return this;
    }

    public List<Object> getEnum() {
        return _enum;
    }

    public void setEnum(List<Object> _enum) {
        this._enum = _enum;
    }

    public Argument example(Object example) {
        this.example = example;
        return this;
    }

    public Object getExample() {
        return example;
    }

    public void setExample(Object example) {
        this.example = example;
    }


    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Argument argument = (Argument) o;
        return Objects.equals(this.type, argument.type) &&
                Objects.equals(this.description, argument.description) &&
                Objects.equals(this.required, argument.required) &&
                Objects.equals(this._default, argument._default) &&
                Objects.equals(this.minimum, argument.minimum) &&
                Objects.equals(this.maximum, argument.maximum) &&
                Objects.equals(this._enum, argument._enum) &&
                Objects.equals(this.example, argument.example);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, description, required, _default, minimum, maximum, _enum, example);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Argument {\n");

        sb.append("    type: ").append(toIndentedString(type)).append("\n");
        sb.append("    description: ").append(toIndentedString(description)).append("\n");
        sb.append("    required: ").append(toIndentedString(required)).append("\n");
        sb.append("    _default: ").append(toIndentedString(_default)).append("\n");
        sb.append("    minimum: ").append(toIndentedString(minimum)).append("\n");
        sb.append("    maximum: ").append(toIndentedString(maximum)).append("\n");
        sb.append("    _enum: ").append(toIndentedString(_enum)).append("\n");
        sb.append("    example: ").append(toIndentedString(example)).append("\n");
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
