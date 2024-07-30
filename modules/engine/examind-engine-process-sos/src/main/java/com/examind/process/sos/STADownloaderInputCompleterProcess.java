/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com/
 *
 * Copyright 2024 Geomatys.
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
package com.examind.process.sos;

import static com.examind.process.sos.STADownloaderDescriptor.*;
import static com.examind.process.sos.STADownloaderUtils.buildBboxFilter;
import static com.examind.process.sos.STADownloaderUtils.readResponse;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.process.ProcessUtils;
import static org.constellation.process.ProcessUtils.getMultipleValues;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.geotoolkit.sts.json.ObservedPropertiesResponse;
import org.geotoolkit.sts.json.ThingsResponse;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class STADownloaderInputCompleterProcess  extends AbstractCstlProcess {
    
    public STADownloaderInputCompleterProcess(final ProcessDescriptor desc, final ParameterValueGroup input) {
        super(desc,input);
    }

    @Override
    protected void execute() throws ProcessException {
        final String staUrl              = inputParameters.getValue(STA_URL_IC);
        final List<String> thingIds      = getMultipleValues(inputParameters, THING_ID);
        final List<String> observedProps = getMultipleValues(inputParameters, OBSERVED_PROPERTY);
        final Envelope bounds            = inputParameters.getValue(BOUNDARY);
        
        // prepare results
        final Set<String> thingResults = new HashSet<>();
        final Set<String> obsPropResults = new HashSet<>();
        
        if (staUrl != null) {
            
            try {
                Filters filter = new Filters();

                // Add bbox filter
                if (bounds != null) {
                    filter.addFilter();
                    filter.append(buildBboxFilter(bounds));
                }

                // add observed properties filter
                if (!observedProps.isEmpty()) {
                    filter.addFilter();
                    filter.thFilter.append(STADownloaderUtils.buildEntityFilter(observedProps, "Datastream/ObservedProperty/id"));
                    filter.opFilter.append(STADownloaderUtils.buildEntityFilter(observedProps, "ObservedProperty/id"));
                }
                
                // add things filter
                if (!thingIds.isEmpty()) {
                    filter.addFilter();
                    filter.thFilter.append(STADownloaderUtils.buildEntityFilter(thingIds, "Thing/id"));
                    filter.opFilter.append(STADownloaderUtils.buildEntityFilter(thingIds, "Datastream/Thing/id"));
                }

               /*
                * Things Extraction
                */
                String thQuery = staUrl + "/Things?$select=id";
                if (filter.hasFilter) {
                    thQuery = thQuery + "&$filter=" + filter.thFilter.toString();
                }
               
                LOGGER.info("STA thing initial query: " + thQuery);
                URL thingUrl = new URL(thQuery.replace(" ", "%20"));
                ThingsResponse thResponse = readResponse(thingUrl, ThingsResponse.class);
                extractFromThingsResponse(thResponse, thingResults);

                while (thResponse.getIotNextLink() != null) {
                    thQuery = thResponse.getIotNextLink();
                    LOGGER.info("STA thing next query: " + thQuery);
                    thingUrl = new URL(thQuery.replace(" ", "%20"));
                    thResponse = readResponse(thingUrl, ThingsResponse.class);
                    extractFromThingsResponse(thResponse, thingResults);

                }

                /*
                * Observed properties Extraction
                */
                String opQuery = staUrl + "/ObservedProperties?$select=id";
                if (filter.hasFilter) {
                    opQuery = opQuery + "&$filter=" + filter.opFilter.toString();
                }

                LOGGER.info("STA obsprop initial query: " + opQuery);
                URL opUrl = new URL(opQuery.replace(" ", "%20"));
                ObservedPropertiesResponse opResponse = readResponse(opUrl, ObservedPropertiesResponse.class);
                extractFromObsPropResponse(opResponse, obsPropResults);

                while (opResponse.getIotNextLink() != null) {
                    opQuery = opResponse.getIotNextLink();
                    LOGGER.info("STA obsprop next query: " + opQuery);
                    opUrl = new URL(opQuery.replace(" ", "%20"));
                    opResponse = readResponse(opUrl, ObservedPropertiesResponse.class);
                    extractFromObsPropResponse(opResponse, obsPropResults);
                }
            } catch (IOException ex) {
                throw new ProcessException("Error while requesting thing url", this, ex);
            }
        }
        
        outputParameters.getOrCreate(STA_URL).setValue(staUrl);
        ProcessUtils.addMultipleValues(outputParameters, thingResults, THING_ID);
        ProcessUtils.addMultipleValues(outputParameters, obsPropResults, OBSERVED_PROPERTY);
    }
    
    private static void extractFromThingsResponse(ThingsResponse response, Set<String> thingResults) {
        thingResults.addAll(response.getValue()
                                    .stream()
                                    .map(th -> th.getIotId())
                                    .toList());
    }
    
    private static void extractFromObsPropResponse(ObservedPropertiesResponse response, Set<String> obspropResults) {
        obspropResults.addAll(response.getValue()
                                    .stream()
                                    .map(op -> op.getIotId())
                                    .toList());
    }
    
    public static class Filters {
        public boolean hasFilter = false;
        public StringBuilder thFilter = new StringBuilder();
        public StringBuilder opFilter = new StringBuilder();
        
        public void append(String s) {
            thFilter.append(s);
            opFilter.append(s);
        }
        
        public void addFilter() {
            if (hasFilter) append(" and ");
            hasFilter = true;
        }
    }
}
