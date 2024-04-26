package org.constellation.process.dynamic.galaxy.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.constellation.process.dynamic.galaxy.model.deserializer.CaseInsensitiveStatusDeserializer;

/**
 * @author Quentin BIALOTA (Geomatys)
 */
@JsonDeserialize(using = CaseInsensitiveStatusDeserializer.class)
public enum Status {
    NEW("new"),
    QUEUED("queued"),
    SCHEDULED("scheduled"),
    RUNNING("running"),
    UPLOAD("upload"),
    PAUSED("paused"),
    ERROR("error"),
    OK("ok"),
    CANCELED("canceled"),
    DELETING("deleting"),
    DELETED("deleted");

    private final String value;

    Status(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Status fromValue(String value) {
        for (Status status : Status.values()) {
            if (status.getValue().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid job status: " + value);
    }
}
