package com.examind.openeo.api.rest.process.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.geotoolkit.atom.xml.Link;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Quentin BIALOTA (Geomatys)
 * Based on : <a href="https://api.openeo.org/#tag/Data-Processing/operation/list-jobs">OpenEO Doc</a>
 */
public class Jobs {

    @JsonProperty("jobs")
    @Valid
    private List<Job> jobs = new ArrayList<>();

    @JsonProperty("links")
    @Valid
    private List<Link> links = new ArrayList<>();

    public Jobs jobs(List<Job> jobs) {
        this.jobs = jobs;
        return this;
    }

    public Jobs addJobsItem(Job jobsItem) {
        this.jobs.add(jobsItem);
        return this;
    }

    public List<Job> getJobs() {
        return jobs;
    }

    public void setJobs(List<Job> jobs) {
        this.jobs = jobs;
    }

    public Jobs links(List<Link> links) {
        this.links = links;
        return this;
    }

    public Jobs addLinksItem(Link linksItem) {
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Jobs jobs = (Jobs) o;
        return Objects.equals(this.jobs, jobs.jobs) &&
                Objects.equals(this.links, jobs.links);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobs, links);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Processes {\n");

        sb.append("    processes: ").append(toIndentedString(jobs)).append("\n");
        sb.append("    links: ").append(toIndentedString(links)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
