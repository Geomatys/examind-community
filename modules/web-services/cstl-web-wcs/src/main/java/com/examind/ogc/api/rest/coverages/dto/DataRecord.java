package com.examind.ogc.api.rest.coverages.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * @author Quentin BIALOTA
 */
public class DataRecord {

    @JsonProperty("type")
    private final CoverageResponseType type = CoverageResponseType.DataRecord;

    @JsonProperty("field")
    private List<DataRecordField> field;

    public DataRecord(List<DataRecordField> field) {
        this.field = field;
    }

    public CoverageResponseType getType() {
        return type;
    }

    public List<DataRecordField> getField() {
        return field;
    }

    public void setField(List<DataRecordField> field) {
        this.field = field;
    }
}
