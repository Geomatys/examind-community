package com.examind.openeo.api.rest.process.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.geotoolkit.atom.xml.Link;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Quentin BIALOTA (Geomatys)
 * Based on : <a href="https://api.openeo.org/#tag/Process-Discovery">OpenEO Doc</a>
 */
public class Processes {

    @JsonProperty("processes")
    @Valid
    private List<Process> processes = new ArrayList<>();

    @JsonProperty("links")
    @Valid
    private List<Link> links = new ArrayList<>();

    public Processes processes(List<Process> processes) {
        this.processes = processes;
        return this;
    }

    public Processes addProcessesItem(Process processesItem) {
        this.processes.add(processesItem);
        return this;
    }

    public List<Process> getProcesses() {
        return processes;
    }

    public void setProcesses(List<Process> processes) {
        this.processes = processes;
    }

    public Processes links(List<Link> links) {
        this.links = links;
        return this;
    }

    public Processes addLinksItem(Link linksItem) {
        this.links.add(linksItem);
        return this;
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Processes processes = (Processes) o;
        return Objects.equals(this.processes, processes.processes) &&
                Objects.equals(this.links, processes.links);
    }

    @Override
    public int hashCode() {
        return Objects.hash(processes, links);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Processes {\n");

        sb.append("    processes: ").append(toIndentedString(processes)).append("\n");
        sb.append("    links: ").append(toIndentedString(links)).append("\n");
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
