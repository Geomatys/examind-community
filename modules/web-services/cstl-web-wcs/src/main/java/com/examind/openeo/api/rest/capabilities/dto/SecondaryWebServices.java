package com.examind.openeo.api.rest.capabilities.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Quentin BIALOTA (Geomatys)
 * Based on : <a href="https://api.openeo.org/#tag/Capabilities">OpenEO Doc</a>
 */
public class SecondaryWebServices {

    @JsonProperty("services")
    @Valid
    private List<Service> services = new ArrayList<>();

    public SecondaryWebServices services(List<Service> services) {
        this.services = services;
        return this;
    }

    public SecondaryWebServices addServicesItem(Service servicesItem) {
        this.services.add(servicesItem);
        return this;
    }

    public List<Service> getServices() {
        return services;
    }

    public void setServices(List<Service> services) {
        this.services = services;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SecondaryWebServices secondaryWebServices = (SecondaryWebServices) o;
        return Objects.equals(this.services, secondaryWebServices.services);
    }

    @Override
    public int hashCode() {
        return Objects.hash(services);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class SecondaryWebServices {\n");

        sb.append("    services: ").append(toIndentedString(services)).append("\n");
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
