/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2018 Geomatys.
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
package com.examind.wps.api;

import com.examind.wps.ExecutionInfo;
import com.examind.wps.QuotationInfo;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import org.geotoolkit.ows.xml.v200.CodeType;
import org.geotoolkit.wps.xml.v200.DataInput;
import org.geotoolkit.wps.xml.v200.DataOutput;
import org.geotoolkit.wps.xml.v200.Execute;
import org.geotoolkit.wps.xml.v200.JobControlOptions;
import org.geotoolkit.wps.xml.v200.OutputDefinition;
import org.geotoolkit.wps.xml.v200.ProcessOffering;
import org.geotoolkit.wps.xml.v200.ProcessSummary;
import org.geotoolkit.wps.xml.v200.Quotation;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public interface WPSProcess {

    CodeType getIdentifier();

    void checkForSchemasToStore() throws IOException;

    ProcessSummary getProcessSummary(Locale loc);

    List<JobControlOptions> getJobControlOptions();

    ProcessOffering getProcessOffering(Locale loc) throws WPSException;

    void checkValidInputOuputRequest(Execute request) throws IOParameterException;

    boolean isSupportedProcess();

    String getLayerName();

    List<OutputDefinition> getOutputDefinitions();

    Callable createRawProcess(boolean async, String version, List<Path> tempFiles, ExecutionInfo execInfo, QuotationInfo quoteInfo, Execute request, String jobId, String quoteId) throws IOParameterException;

    Object createRawOutput(final String version, final String outputId, final Object result) throws WPSException;

    Callable createDocProcess(boolean async, String version, List<Path> tempFiles, ExecutionInfo execInfo, QuotationInfo quoteInfo, Execute request, String serviceInstance, ProcessSummary procSum,
            final List<DataInput> inputsResponse, final List<OutputDefinition> outputsResponse, final String jobId, final String quoteId, final Map<String, Object> parameters) throws IOParameterException;

    List<DataOutput> createDocOutput(final String version, final List<? extends OutputDefinition> wantedOutputs,  final Object result, final Map<String, Object> parameters,
            final boolean progressing) throws WPSException;

    Quotation quote(Execute request) throws WPSException;

}
