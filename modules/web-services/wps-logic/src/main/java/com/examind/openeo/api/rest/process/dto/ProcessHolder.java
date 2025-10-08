package com.examind.openeo.api.rest.process.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.geotoolkit.atom.xml.Link;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Quentin BIALOTA (Geomatys)
 * Based on : <a href="https://api.openeo.org/#tag/Data-Processing/operation/compute-result">OpenEO Doc</a>
 */
public class ProcessHolder {

    @JsonProperty("process")
    @Valid
    private Process process;

    @JsonProperty("budget")
    private Float budget;

    @JsonProperty("plan")
    private String plan;

    @JsonProperty("log_level")
    private String logLevel;

    public Process process() {
        return process;
    }

    public Float budget() {
        return budget;
    }

    public String plan() {
        return plan;
    }

    public String logLevel() {
        return logLevel;
    }
}
