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
package org.constellation.coverage.core;

import com.examind.ogc.api.rest.common.dto.Collection;
import com.examind.ogc.api.rest.coverages.dto.DataRecord;
import com.examind.ogc.api.rest.coverages.dto.DomainSet;
import org.constellation.ws.CstlServiceException;
import org.constellation.ws.Worker;
import org.geotoolkit.wcs.xml.DescribeCoverage;
import org.geotoolkit.wcs.xml.DescribeCoverageResponse;
import org.geotoolkit.wcs.xml.GetCapabilities;
import org.geotoolkit.wcs.xml.GetCapabilitiesResponse;
import org.geotoolkit.wcs.xml.GetCoverage;

import java.util.List;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public interface WCSWorker extends Worker{

    GetCapabilitiesResponse getCapabilities(final GetCapabilities request) throws CstlServiceException;

    DescribeCoverageResponse describeCoverage(final DescribeCoverage request) throws CstlServiceException;

    Object getCoverage(final GetCoverage request) throws CstlServiceException;

    List<Collection> getCollections(List<String> collectionIds, boolean forOpenEO) throws CstlServiceException;

    Object getCoverage(String collectionId, String format, List<Double> bbox, String scaleData, List<String> subsetData, List<String> properties, String bboxCrs) throws CstlServiceException;

    DomainSet getDomainSet(String collectionId, List<Double> bbox, String bboxCrs) throws CstlServiceException;

    DataRecord getDataRecord(String collectionId) throws CstlServiceException;

    List<String> getDimensionsNames(String collectionId) throws CstlServiceException;
}
