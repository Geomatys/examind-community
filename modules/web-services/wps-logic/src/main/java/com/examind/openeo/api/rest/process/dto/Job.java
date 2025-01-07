package com.examind.openeo.api.rest.process.dto;

import com.examind.openeo.api.rest.process.dto.serializer.JobSerializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.geotoolkit.atom.xml.Link;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.List;
import java.util.Objects;

/**
 * @author Quentin BIALOTA (Geomatys)
 * Based on : <a href="https://api.openeo.org/#tag/Batch-Jobs/operation/list-jobs">OpenEO Doc</a>
 */
@JsonSerialize(using = JobSerializer.class)
public class Job {

    public Job() {
    }

    public Job(String id, String title, String description, Process process, Status status, float progress,
               XMLGregorianCalendar created, XMLGregorianCalendar updated, String plan, float costs,
               float budget, String logLevel, List<Link> links) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.process = process;
        this.status = status;
        this.progress = progress;
        this.created = created;
        this.updated = updated;
        this.plan = plan;
        this.costs = costs;
        this.budget = budget;
        this.logLevel = logLevel;
        this.links = links;
    }

    @JsonProperty("id")
    private String id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    @JsonProperty("process")
    private Process process;

    @JsonProperty("status")
    private Status status;

    @JsonProperty("progress")
    private float progress;

    @JsonProperty("created")
    private XMLGregorianCalendar created;

    @JsonProperty("updated")
    private XMLGregorianCalendar updated;

    @JsonProperty("plan")
    private String plan;

    @JsonProperty("costs")
    private float costs;

    @JsonProperty("budget")
    private float budget;

    @JsonProperty("log_level")
    private String logLevel;

    @JsonProperty("links")
    private List<Link> links;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Process getProcess() {
        return process;
    }

    public void setProcess(Process process) {
        this.process = process;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public float getProgress() {
        return progress;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    public XMLGregorianCalendar getCreated() {
        return created;
    }

    public void setCreated(XMLGregorianCalendar created) {
        this.created = created;
    }

    public XMLGregorianCalendar getUpdated() {
        return updated;
    }

    public void setUpdated(XMLGregorianCalendar updated) {
        this.updated = updated;
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public float getCosts() {
        return costs;
    }

    public void setCosts(float costs) {
        this.costs = costs;
    }

    public float getBudget() {
        return budget;
    }

    public void setBudget(float budget) {
        this.budget = budget;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Job job = (Job) o;
        return Float.compare(progress, job.progress) == 0 && Float.compare(costs, job.costs) == 0 &&
                Float.compare(budget, job.budget) == 0 && Objects.equals(id, job.id) &&
                Objects.equals(title, job.title) && Objects.equals(description, job.description) &&
                Objects.equals(process, job.process) && Objects.equals(status, job.status) &&
                Objects.equals(created, job.created) && Objects.equals(updated, job.updated) &&
                Objects.equals(plan, job.plan) && Objects.equals(logLevel, job.logLevel) &&
                Objects.equals(links, job.links);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, description, process, status, progress, created, updated, plan, costs, budget, logLevel, links);
    }

    @Override
    public String toString() {
        return "Job{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", process=" + process +
                ", status=" + status +
                ", progress=" + progress +
                ", created=" + created +
                ", updated=" + updated +
                ", plan='" + plan + '\'' +
                ", costs=" + costs +
                ", budget=" + budget +
                ", logLevel='" + logLevel + '\'' +
                ", links=" + links +
                '}';
    }
}