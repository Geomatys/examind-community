package com.examind.wps.api;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.geotoolkit.feature.xml.ExceptionReport;
import org.geotoolkit.wps.xml.v200.Result;
import org.geotoolkit.wps.xml.v200.StatusInfo;

/**
 * Handles the lifecycle of a WPS process execution. The aim is to allow
 * users to follow a job status without having to know what is the execution
 * environment below. Implementations of this interface could be pure java,
 * managing what is going on in a quartz scheduler or a standard executor service.
 * It could also be a web client proxying user requests a distant execution service.
 *
 * @author Alexis Manin (Geomatys)
 */
public interface ExecutionStatus {

    /**
     *
     * @return Identifier of the WPS job this object refers to.
     */
    String getId();

    /**
     * Ask for the current status of the process, if available.
     * @return The job status if available. If the process is synchronous, no
     * status can be obtained, and an empty object will be returned.
     */
    Optional<StatusInfo> requestStatus() throws ExceptionReport;

    /**
     *
     * @return true if the job is an asynchronous WPS process (meaning we can
     * obtain its status), false if it is synchroous (so no status document can
     * be requested).
     */
    boolean isAsynchronous();

    /**
     * Wait for the job to terminate and get its result. For an asynnxhronous
     * behavior, you can use {@link #tryGetResult() } or {@link #getResult(long, java.util.concurrent.TimeUnit) }
     * instead.
     *
     * @return Process response when succeeded.
     *
     * @throws ExceptionReport Process error if it fails.
     * @throws InterruptedException If the wait for response has been stopped
     * before we could get the result.
     */
    Result getResult() throws ExceptionReport, InterruptedException;

    /**
     * Fetch the job result. If we cannot get job result before specified timeout,
     * an empty response is returned.
     *
     * @param timeout The time to wait before returning an empty result.
     * @param timeoutUnit Unit of input timeout.
     * @return The job result if it's available before specified timeout. An
     * empty answer otherwise.
     *
     * @throws ExceptionReport If the job has failed.
     * @throws InterruptedException If the query for result has been cancelled
     * while waiting for job response.
     */
    Optional<Result> getResult(final long timeout, final TimeUnit timeoutUnit) throws ExceptionReport, InterruptedException;

    /**
     * Try to immediately fetch job result, returning an empty value if the job
     * is not over.
     *
     * @return Job result if available, an empty object otherwise.
     * @throws ExceptionReport If the process has failed.
     */
    Optional<Result> tryGetResult() throws ExceptionReport;
}
