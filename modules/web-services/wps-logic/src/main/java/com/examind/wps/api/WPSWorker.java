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

import java.util.Set;
import org.constellation.ws.CstlServiceException;
import org.constellation.ws.Worker;
import org.geotoolkit.wps.xml.v200.Capabilities;
import org.geotoolkit.wps.xml.v200.DescribeProcess;
import org.geotoolkit.wps.xml.v200.Dismiss;
import org.geotoolkit.wps.xml.v200.Execute;
import org.geotoolkit.wps.xml.v200.GetCapabilities;
import org.geotoolkit.wps.xml.v200.GetResult;
import org.geotoolkit.wps.xml.v200.GetStatus;
import org.geotoolkit.wps.xml.v200.ProcessOfferings;
import org.geotoolkit.wps.xml.v200.StatusInfo;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public interface WPSWorker extends Worker {

    Capabilities getCapabilities(final GetCapabilities request) throws CstlServiceException;

    ProcessOfferings describeProcess(final DescribeProcess request) throws CstlServiceException;

    Set<String> getJobList(String processId) throws CstlServiceException;

    StatusInfo getStatus(GetStatus request) throws CstlServiceException;

    Object getResult(GetResult request) throws CstlServiceException;

    StatusInfo dismiss(Dismiss request) throws CstlServiceException;

    Object execute(final Execute request) throws CstlServiceException;
}
