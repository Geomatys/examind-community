/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2019 Geomatys.
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
package org.constellation.sos.io.lucene;

import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.sis.storage.DataStoreException;
import static org.constellation.api.CommonConstants.RESPONSE_MODE;
import org.geotoolkit.observation.OMUtils;
import org.geotoolkit.observation.ObservationFilterReader;
import org.geotoolkit.observation.ObservationReader;
import org.geotoolkit.observation.ObservationResult;
import org.geotoolkit.observation.ObservationStoreException;
import org.geotoolkit.observation.model.ComplexResult;
import org.geotoolkit.observation.model.MeasureResult;
import org.geotoolkit.observation.model.Observation;
import org.geotoolkit.observation.model.Offering;
import org.geotoolkit.observation.model.ResponseMode;
import static org.geotoolkit.observation.model.ResponseMode.RESULT_TEMPLATE;
import org.geotoolkit.observation.xml.ObservationComparator;
import static org.geotoolkit.ows.xml.OWSExceptionCode.NO_APPLICABLE_CODE;
import org.locationtech.jts.geom.Geometry;
import org.opengis.geometry.Envelope;
import org.opengis.observation.Phenomenon;
import org.opengis.observation.Process;
import org.opengis.observation.sampling.SamplingFeature;
import static org.geotoolkit.observation.result.ResultTimeNarrower.applyTimeConstraint;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class LuceneObservationFilterReader extends LuceneObservationFilter implements ObservationFilterReader {

    private ObservationReader reader;

    public LuceneObservationFilterReader(final LuceneObservationFilterReader omFilter) throws DataStoreException {
        super(omFilter);
        this.reader = omFilter.reader;
    }

    public LuceneObservationFilterReader(final Path confDirectory, final Map<String, Object> properties, ObservationReader reader) throws DataStoreException {
        super(confDirectory, properties);
        this.reader = reader;
    }

    @Override
    public List<org.opengis.observation.Observation> getObservations() throws DataStoreException {
        final List<Observation> observations = new ArrayList<>();
        final Set<String> ids = getIdentifiers();
        for (String id : ids) {
            observations.add((Observation) reader.getObservation(id, resultModel, responseMode));
        }
        Collections.sort(observations, new ObservationComparator());
        final List<Observation> results = new ArrayList<>();
        for (Observation obs : observations) {
            if (!RESULT_TEMPLATE.equals(responseMode)) {
                // parse result values to eliminate wrong results
                applyTimeConstraint(obs, timeFilters);
                // in measurement mode we need to split the complex observation into measurement
                if (OMUtils.MEASUREMENT_QNAME.equals(resultModel)) {
                    results.addAll(OMUtils.splitComplexObservationIntoMeasurement(obs, fieldFilters, measureIdFilters));
                } else {
                    results.add(obs);
                }
            } else {
                // in measurement mode we need to split the complex observation into measurement
                if (OMUtils.MEASUREMENT_QNAME.equals(resultModel)) {
                    results.addAll(OMUtils.splitComplexTemplateIntoMeasurement(obs, fieldFilters));
                } else {
                    results.add(obs);
                }
            }
        }
        return new ArrayList<>(results);
    }

    @Override
    public List<SamplingFeature> getFeatureOfInterests() throws DataStoreException {
        final List<SamplingFeature> results = new ArrayList<>();
        final Set<String> ids = getIdentifiers();
        for (String id : ids) {
            results.add(reader.getFeatureOfInterest(id));
        }
        return results;
    }

    @Override
    public List<Phenomenon> getPhenomenons() throws DataStoreException {
        List<Phenomenon> results = new ArrayList<>();
        final Set<String> ids    = getIdentifiers();
        for (String id : ids) {
            results.add(reader.getPhenomenon(id));
        }
        return results;
    }

    @Override
    public List<Process> getProcesses() throws DataStoreException {
        final List<Process> results = new ArrayList<>();
        final Set<String> ids = getIdentifiers();
        for (String id : ids) {
            results.add(reader.getProcess(id));
        }
        return results;
    }

    @Override
    public List<Offering> getOfferings() throws DataStoreException {
        final List<Offering> results = new ArrayList<>();
        final Set<String> ids = getIdentifiers();
        for (String id : ids) {
            results.add(reader.getObservationOffering(id));
        }
        return results;
    }

    @Override
    public String getResults() throws DataStoreException {
        if (ResponseMode.OUT_OF_BAND.equals(responseMode)) {
            throw new ObservationStoreException("Out of band response mode has not been implemented yet", NO_APPLICABLE_CODE, RESPONSE_MODE);
        }
        final Set<ObservationResult> results = new LinkedHashSet<>(filterResult());
        final StringBuilder datablock        = new StringBuilder();

        for (ObservationResult result: results) {
            final Timestamp tBegin = result.beginTime;
            final Timestamp tEnd   = result.endTime;
            final Object r         =  reader.getResult(result.resultID, resultModel);
            if (r instanceof ComplexResult cr) {
                
                applyTimeConstraint(tBegin, tEnd, cr, timeFilters);
                if (!cr.getValues().isEmpty()) {
                    datablock.append(cr.getValues());
                }
                
            } else if (r instanceof MeasureResult meas) {
                datablock.append(tBegin).append(',').append(meas.getValue()).append("@@");
            } else {
                throw new IllegalArgumentException("Unexpected result type:" + r);
            }
        }
        return datablock.toString();
    }

    @Override
    public Envelope getCollectionBoundingShape() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public Map<String, Geometry> getSensorLocations() throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public Map<String, Map<Date, Geometry>> getSensorHistoricalLocations() throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Map<String, List<Date>> getSensorTimes() throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
