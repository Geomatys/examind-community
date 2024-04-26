package org.constellation.process.dynamic.galaxy.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Quentin BIALOTA (Geomatys)
 */
public class StepJobSummary {
    @JsonProperty("job_states")
    private List<StepJobState> jobStates;

    public StepJobSummary(List<StepJobState> jobStates) {
        this.jobStates = jobStates;
    }

    public List<StepJobState> getStepJobStates() {
        return jobStates;
    }

    public void setStepJobStates(List<StepJobState> jobStates) {
        this.jobStates = jobStates;
    }

    public int getCompletedStepJobsCount() {
        int completedJobsCount = 0;
        for (StepJobState stepJob : jobStates) {
            if (!stepJob.hasOtherStatesThanOk()) {
                completedJobsCount++;
            }
        }
        return completedJobsCount;
    }

    public Status getGlobalStatus() {
        Status status = Status.QUEUED;
        for (StepJobState stepJob : jobStates) {
            Set<Status> statusList = stepJob.getStates().keySet();

            if (statusList.contains(Status.ERROR)) {
                return Status.ERROR;
            }
            if (statusList.contains(Status.CANCELED)) {
                return Status.CANCELED;
            }
            if (statusList.contains(Status.DELETING)) {
                return Status.DELETING;
            }
            if (statusList.contains(Status.DELETED)) {
                return Status.DELETED;
            }
            if (statusList.contains(Status.RUNNING)) {
                status = Status.RUNNING;
            }
            if (statusList.contains(Status.SCHEDULED)) {
                return Status.SCHEDULED;
            }
        }

        int totalJobs = jobStates.size();
        int completedJobs = getCompletedStepJobsCount();
        if (totalJobs == completedJobs) return Status.OK;

        return status;
    }
}
