package com.examind.openeo.api.rest.capabilities.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Quentin BIALOTA (Geomatys)
 * Based on : <a href="https://api.openeo.org/#tag/Capabilities">OpenEO Doc</a>
 */
public class FileFormats {

    @JsonProperty("input")
    @Valid
    private Map<String, FileFormat> input = new HashMap<>();

    @JsonProperty("output")
    @Valid
    private Map<String, FileFormat> output = new HashMap<>();

    public FileFormats() {
        
    }
    
    public FileFormats(Map<String, FileFormat> input, Map<String, FileFormat> output) {
        this.input = input;
        this.output = output;
    }
    
    public FileFormats input(Map<String, FileFormat> input) {
        this.input = input;
        return this;
    }

    public FileFormats putInputItem(String key, FileFormat inputItem) {
        this.input.put(key, inputItem);
        return this;
    }

    public Map<String, FileFormat> getInput() {
        return input;
    }

    public void setInput(Map<String, FileFormat> input) {
        this.input = input;
    }

    public FileFormats output(Map<String, FileFormat> output) {
        this.output = output;
        return this;
    }

    public FileFormats putOutputItem(String key, FileFormat outputItem) {
        this.output.put(key, outputItem);
        return this;
    }

    public Map<String, FileFormat> getOutput() {
        return output;
    }

    public void setOutput(Map<String, FileFormat> output) {
        this.output = output;
    }


    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FileFormats fileFormats = (FileFormats) o;
        return Objects.equals(this.input, fileFormats.input) &&
                Objects.equals(this.output, fileFormats.output);
    }

    @Override
    public int hashCode() {
        return Objects.hash(input, output);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class FileFormats {\n");

        sb.append("    input: ").append(toIndentedString(input)).append("\n");
        sb.append("    output: ").append(toIndentedString(output)).append("\n");
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
