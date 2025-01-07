package com.examind.openeo.api.rest.dto;

/**
 * @author Quentin BIALOTA (Geomatys)
 */
public class CheckMessage {

    public CheckMessage(boolean isValid, String message) {
        this.isValid = isValid;
        this.message = message;
    }

    private boolean isValid;

    private String message;

    public boolean isValid() {
        return isValid;
    }

    public String getMessage() {
        return message;
    }
}
