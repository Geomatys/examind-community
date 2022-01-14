/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.examind.wps;

import com.examind.wps.api.IOParameterException;
import com.examind.wps.api.WPSException;
import com.examind.wps.api.WPSProcess;
import java.net.URI;
import org.constellation.api.ServiceDef;
import com.examind.wps.util.WPSUtils;
import org.constellation.ws.CstlServiceException;
import org.geotoolkit.process.ProcessEvent;
import org.geotoolkit.process.ProcessListener;
import org.geotoolkit.util.Exceptions;
import org.geotoolkit.util.StringUtilities;
import org.geotoolkit.wps.converters.WPSConvertersUtils;
import org.geotoolkit.wps.xml.v200.Execute;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.datatype.XMLGregorianCalendar;
import static com.examind.wps.util.WPSConstants.WPS_SERVICE;
import org.geotoolkit.ows.xml.ExceptionResponse;
import org.geotoolkit.ows.xml.v200.ExceptionReport;
import org.geotoolkit.wps.xml.v200.DataOutput;
import org.geotoolkit.wps.xml.v200.Result;
import org.geotoolkit.wps.xml.v200.DataInput;
import org.geotoolkit.wps.xml.v200.OutputDefinition;
import org.geotoolkit.wps.xml.v200.ProcessSummary;
import org.geotoolkit.wps.xml.v200.Status;
import org.geotoolkit.wps.xml.v200.StatusInfo;
import static com.examind.wps.util.WPSConstants.WPS_LANG_EN;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;

/**
 * ProcessListener implementation for WPS asynchronous process execution.
 *
 * @author Quentin Boileau (Geomatys).
 */
public class WPSProcessListener implements ProcessListener{

    private static final Logger LOGGER = Logger.getLogger("org.constellation.wps.ws");
    private static final int TIMEOUT = 200; //processing time step

    private final Execute request;
    private final String jobId;
    private final String quoteId;
    private final ServiceDef def;
    final Map<String, Object> parameters;
    private final URI folderPath;
    private long nextTimestamp;
    private final boolean recordStatusProgress;
    private final String wpsVersion;

    /*
    -------------------------------
      Execute response parameters
    -------------------------------
     */

    private final String serviceInstance;
    private final ProcessSummary procSum;
    private final List<DataInput> inputsResponse;
    private final List<OutputDefinition> outputsResponse;

    private final ExecutionInfo execInfo;
    private final QuotationInfo quoteInfo;

    private final String language;

    private final WPSProcess process;

    /**
     *
     * @param wpsVersion
     * @param execInfo
     * @param request execute request
     * @param serviceInstance ExecuteResponse base parameter
     * @param procSum ExecuteResponse base parameter
     * @param inputsResponse ExecuteResponse base parameter
     * @param outputsResponse ExecuteResponse base parameter
     * @param jobId name of the file to update
     * @param parameters
     */
    public WPSProcessListener(final String wpsVersion, final ExecutionInfo execInfo, final QuotationInfo quoteInfo,
            final Execute request, final String serviceInstance, final ProcessSummary procSum,
            final List<DataInput> inputsResponse, final List<OutputDefinition> outputsResponse,
            final String jobId, final String quoteId, final Map<String, Object> parameters, final WPSProcess process) {
        this.request = request;
        this.jobId = jobId;
        this.quoteId = quoteId;
        this.execInfo = execInfo;
        this.quoteInfo = quoteInfo;
        this.def = ServiceDef.getServiceDefinition(ServiceDef.Specification.WPS, wpsVersion);
        this.parameters = parameters;
        this.folderPath = (URI) parameters.get(WPSConvertersUtils.OUT_STORAGE_DIR);
        this.nextTimestamp = System.currentTimeMillis() + TIMEOUT;
        this.recordStatusProgress = wpsVersion.equals("2.0.0") || this.request.isStatus();
        this.wpsVersion = wpsVersion;
        this.serviceInstance = serviceInstance;
        this.inputsResponse = inputsResponse;
        this.outputsResponse = outputsResponse;
        this.procSum = procSum;
        this.process = process;

        if (request.getLanguage() != null) {
            this.language = request.getLanguage();
        } else {
            this.language = WPS_LANG_EN;
        }
    }

    @Override
    public void started(final ProcessEvent event) {
        LOGGER.log(Level.INFO, "Process {0} is started (JobId:{1}).", new Object[]{WPSUtils.buildProcessIdentifier(event.getSource().getDescriptor()), jobId});
        XMLGregorianCalendar creationTime = WPSUtils.getCurrentXMLGregorianCalendar();
        String msg = "Process " + request.getIdentifier().getValue() + " is started";
        StatusInfo status = new StatusInfo(Status.RUNNING, creationTime, 0, msg, jobId);
        execInfo.addJob(request.getIdentifier().getValue(), jobId, status, process, event.getSource());
        if (recordStatusProgress) {
            final Result response = new Result(WPS_SERVICE, wpsVersion, language, serviceInstance, procSum, inputsResponse, outputsResponse, null, status, jobId);
            WPSUtils.storeResponse(response, folderPath, jobId);
        }
    }

    @Override
    public void progressing(final ProcessEvent event) {
        final long currentTimestamp = System.currentTimeMillis();
        if (currentTimestamp >= (nextTimestamp)){
            //LOGGER.log(Level.INFO, "Process {0} is progressing : {1}.", new Object[]{WPSUtils.buildProcessIdentifier(event.getSource().getDescriptor()), event.getProgress()});
            nextTimestamp += TIMEOUT;
            try {

                XMLGregorianCalendar creationTime = WPSUtils.getCurrentXMLGregorianCalendar();
                String msg = "Process " + request.getIdentifier().getValue() + " is running";
                StatusInfo status = new StatusInfo(Status.RUNNING, creationTime, (int) event.getProgress(), msg, jobId);
                execInfo.setStatus(jobId, status);

                final Result response;
                // if intermediate result is not null
                if (event.getOutput() != null) {
                    List<DataOutput> outputs = process.createDocOutput(wpsVersion, request.getOutput(), event.getOutput(), parameters, true);

                    response = new Result(WPS_SERVICE, wpsVersion, language, serviceInstance, procSum, inputsResponse, outputsResponse, outputs, status, jobId);
                    execInfo.setResult(jobId, response);
                } else {
                    response = new Result(WPS_SERVICE, wpsVersion, language, serviceInstance, procSum, inputsResponse, outputsResponse, null, status, jobId);
                }
                if (recordStatusProgress) {
                    WPSUtils.storeResponse(response, folderPath, jobId);
                }
            } catch (IOParameterException ex) {
                writeException(new CstlServiceException(ex.getMessage(), INVALID_PARAMETER_VALUE, ex.getParamId()));
            } catch (WPSException ex) {
                writeException(new CstlServiceException(ex));
            }
        }
    }

    @Override
    public void completed(final ProcessEvent event) {
        LOGGER.log(Level.INFO, "Process {0} is finished (JobId:{1}).", new Object[]{WPSUtils.buildProcessIdentifier(event.getSource().getDescriptor()), jobId});
        try {
	    List<OutputDefinition> wantedOutputs;
	    if (request.isLineage()) {
		wantedOutputs = outputsResponse;
	    }	
	    else {
		wantedOutputs = request.getOutput();
	    }
 

	    List<DataOutput> outputs =  process.createDocOutput(wpsVersion, wantedOutputs, event.getOutput(), parameters, false);
           

            XMLGregorianCalendar creationTime = WPSUtils.getCurrentXMLGregorianCalendar();
            String msg = "Process completed.";
            StatusInfo status = new StatusInfo(Status.SUCCEEDED, creationTime,  msg, jobId);
            execInfo.setStatus(jobId, status);

            final Result response = new Result(WPS_SERVICE, wpsVersion, language, serviceInstance, procSum, inputsResponse, outputsResponse, outputs, status, jobId);
            WPSUtils.storeResponse(response, folderPath, jobId);
            execInfo.setResult(jobId, response);
            quoteInfo.addBill(quoteId, jobId);
        } catch (IOParameterException ex) {
            writeException(new CstlServiceException(ex.getMessage(), INVALID_PARAMETER_VALUE, ex.getParamId()));
        } catch (WPSException ex) {
            writeException(new CstlServiceException(ex));
        }

    }

    @Override
    public void failed(final ProcessEvent event) {
        LOGGER.log(Level.WARNING, "Process "+WPSUtils.buildProcessIdentifier(event.getSource().getDescriptor())+" has failed. (JobId:" + jobId +").", event.getException());
        XMLGregorianCalendar creationTime = WPSUtils.getCurrentXMLGregorianCalendar();
        ExceptionResponse report;
        if (event.getException() != null) {
            report = new ExceptionReport(Exceptions.formatStackTrace(event.getException()), null, null, this.def.exceptionVersion.toString());
        } else {
            report = new ExceptionReport("Process failed for some unknown reason.", null, null, this.def.exceptionVersion.toString());
        }
        StatusInfo status = new StatusInfo(Status.FAILED, creationTime, report.toException().getMessage(), jobId);
        execInfo.setStatus(jobId, status);
        final Result response = new Result(WPS_SERVICE, wpsVersion, language, serviceInstance, procSum, inputsResponse, outputsResponse, null, status, jobId);
        WPSUtils.storeResponse(response, folderPath, jobId);
        execInfo.setResult(jobId, response);
    }

    @Override
    public void dismissed(ProcessEvent event) {
        LOGGER.log(Level.INFO, "Process "+WPSUtils.buildProcessIdentifier(event.getSource().getDescriptor())+" has been dismissed. (JobId:" + jobId +").", event.getException());
        XMLGregorianCalendar creationTime = WPSUtils.getCurrentXMLGregorianCalendar();
        ExceptionResponse report;
        if (event.getException() != null) {
            report = new ExceptionReport(Exceptions.formatStackTrace(event.getException()), null, null, this.def.exceptionVersion.toString());
        } else {
            report = new ExceptionReport("Process dismissed.", null, null, this.def.exceptionVersion.toString());
        }
        StatusInfo status = new StatusInfo(Status.DISMISS, creationTime, report.toException().getMessage(), jobId);
        execInfo.setStatus(jobId, status);
        final Result response = new Result(WPS_SERVICE, wpsVersion, language, serviceInstance, procSum, inputsResponse, outputsResponse, null, status, jobId);
        WPSUtils.storeResponse(response, folderPath, jobId);
        execInfo.setResult(jobId, response);
    }

    @Override
    public void paused(final ProcessEvent event) {
        LOGGER.log(Level.INFO, "Process {0} is paused (JobId:{1}).", new Object[]{WPSUtils.buildProcessIdentifier(event.getSource().getDescriptor()), jobId});
        XMLGregorianCalendar creationTime = WPSUtils.getCurrentXMLGregorianCalendar();
        String msg = "Process " + request.getIdentifier().getValue() + " is paused";
        StatusInfo status = new StatusInfo(Status.PAUSED, creationTime, (int) event.getProgress(), msg, jobId);
        execInfo.setStatus(jobId, status);
        if (recordStatusProgress) {
            final Result response = new Result(WPS_SERVICE, wpsVersion, language, serviceInstance, procSum, inputsResponse, outputsResponse, null, status, jobId);
            WPSUtils.storeResponse(response, folderPath, jobId);
        }
    }

    @Override
    public void resumed(final ProcessEvent event) {
        LOGGER.log(Level.INFO, "Process {0} is resumed (JobId:{1}).", new Object[]{WPSUtils.buildProcessIdentifier(event.getSource().getDescriptor()), jobId});
        final XMLGregorianCalendar creationTime = WPSUtils.getCurrentXMLGregorianCalendar();
        final String msg = "Process " + request.getIdentifier().getValue() + " is resumed";
        final StatusInfo status = new StatusInfo(Status.PAUSED, creationTime, (int) event.getProgress(), msg, jobId);
        execInfo.setStatus(jobId, status);
        if (recordStatusProgress) {
            final Result response = new Result(WPS_SERVICE, wpsVersion, language, serviceInstance, procSum, inputsResponse, outputsResponse, null, status, jobId);
            WPSUtils.storeResponse(response, folderPath, jobId);
        }
    }

    /**
     * Write the occurred exception in the response file.
     *
     * @param ex exception
     */
    private void writeException(final CstlServiceException ex){

        final String codeRepresentation;
        if (ex.getExceptionCode() instanceof org.constellation.ws.ExceptionCode) {
            codeRepresentation = StringUtilities.transformCodeName(ex.getExceptionCode().name());
        } else {
            codeRepresentation = ex.getExceptionCode().name();
        }

        final ExceptionResponse report = new ExceptionReport(ex.getMessage(), codeRepresentation, ex.getLocator(),
                                                     def.exceptionVersion.toString());

        WPSUtils.storeResponse(report, folderPath, jobId);
        execInfo.setResult(jobId, report);
    }

}
