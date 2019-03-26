package com.examind.wps.api;

import java.util.Locale;
import java.util.stream.Stream;
import org.geotoolkit.feature.xml.ExceptionReport;
import org.geotoolkit.wps.xml.v200.Execute;
import org.geotoolkit.wps.xml.v200.ProcessDescription;
import org.geotoolkit.wps.xml.v200.ProcessOffering;
import org.geotoolkit.wps.xml.v200.ProcessProperties;

/**
 * A component managing a set of processes. This is a level of abstraction to
 * allow proper management of different types of processes.
 *
 * The aim here is to get a component which can give WPS compliant descriptions
 * of a set of processes, as well as providing operation logic for process
 * execution.
 *
 * For example, you could make one implementation whicch work with pure Java
 * {@link org.geotoolkit.process.Process}, and another one to manage distant
 * processing service proxying (i.e: proxying a pure WPS, making only HTTP
 * request and response parsing).
 *
 * TODO:
 * <ol>
 * <li>Add internationalization for description queries</li>
 * <li>Add methods to ease process discovery</li>
 * <li>Should we introduce the notion of WPS version here ?</li>
 * <li>To be usable, we should introduce a concept of parameterization. The idea
 * is that ledgers behave like Apache Camel components, i.e routing an arbitrary
 * execution endpoint to WPS API.</li>
 * </ol>
 *
 * @author Alexis Manin (Geomatys)
 */
public interface WebProcessingComponent {

    /**
     * Enumerate processes available through the current ledger.
     *
     * @param lang Language to use for process title and description.
     *
     * @return A stream giving process urns and succint description. Should be
     * {@link ProcessProperties} or
     * {@link org.geotoolkit.wps.xml.v200.ProcessOffering}
     */
    Stream<ProcessProperties> list(Locale lang);

    /**
     * Give detailed description of a single process. This should include
     * general description, as with both input and output specification.
     *
     * @param process The process to get description for.
     * @param lang Language to use for process title and description.
     *
     * @return Detailed description of input process.
     * @throws IllegalArgumentException if given process is not known by this
     * component.
     */
    ProcessDescription describe(final ProcessProperties process, Locale lang) throws IllegalArgumentException;

    /**
     * Request the execution of a process using a given set of parameters.
     *
     * @param request The execution request, containing input definition as with
     * processing mode (synchronous, asynchronous, etc.).
     *
     * @return A handle to follow the execution (job) status.
     *
     * @throws ExceptionReport If an error occurs while submitting the execution
     * query.
     */
    ExecutionStatus execute(final Execute request) throws ExceptionReport;
}
