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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.sis.storage.DataStoreException;
import org.constellation.sos.ws.DatablockParser.Values;
import static org.constellation.sos.ws.DatablockParser.getResultValues;
import org.geotoolkit.gml.xml.Envelope;
import org.geotoolkit.observation.ObservationFilterReader;
import org.geotoolkit.observation.ObservationReader;
import static org.geotoolkit.observation.ObservationReader.IDENTIFIER;
import static org.geotoolkit.observation.ObservationReader.SOS_VERSION;
import org.geotoolkit.observation.ObservationResult;
import org.geotoolkit.swe.xml.DataArray;
import org.geotoolkit.swe.xml.DataArrayProperty;
import org.opengis.geometry.Geometry;
import org.opengis.observation.Measure;
import org.opengis.observation.Observation;
import org.opengis.observation.Phenomenon;
import org.opengis.observation.Process;
import org.opengis.observation.sampling.SamplingFeature;
import org.opengis.temporal.Period;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class LuceneObservationFilterReader extends LuceneObservationFilter implements ObservationFilterReader {

    private ObservationReader reader;

    private String responseFormat;

    public LuceneObservationFilterReader(final LuceneObservationFilterReader omFilter) throws DataStoreException {
        super(omFilter);
        this.reader = omFilter.reader;
    }

    public LuceneObservationFilterReader(final Path confDirectory, final Map<String, Object> properties, ObservationReader reader) throws DataStoreException {
        super(confDirectory, properties);
        this.reader = reader;
    }

    /**
     * TODO Not sure if it is working.
     *
     * @param hints
     * @return
     * @throws DataStoreException
     */
    @Override
    public List<Observation> getObservationTemplates(Map<String, String> hints) throws DataStoreException {
        final String version                 = getVersionFromHints(hints);
        final List<Observation> observations = new ArrayList<>();
        final Set<String> oid = filterObservation(hints);
        for (String foid : oid) {
            final Observation obs = reader.getObservation(foid, resultModel, requestMode, version);
            observations.add(obs);
        }
        return observations;
    }

    @Override
    public List<Observation> getObservations(Map<String, String> hints) throws DataStoreException {
        final String version                 = getVersionFromHints(hints);
        final List<Observation> observations = new ArrayList<>();
        final Set<String> oid = filterObservation(hints);
        for (String foid : oid) {
            final Observation obs = reader.getObservation(foid, resultModel, requestMode, version);

            // parse result values to eliminate wrong results
            if (obs.getSamplingTime() instanceof Period) {
                final Timestamp tbegin;
                final Timestamp tend;
                final Period p = (Period)obs.getSamplingTime();
                if (p.getBeginning() != null && p.getBeginning().getDate() != null) {
                    tbegin = new Timestamp(p.getBeginning().getDate().getTime());
                } else {
                    tbegin = null;
                }
                if (p.getEnding() != null && p.getEnding().getDate() != null) {
                    tend = new Timestamp(p.getEnding().getDate().getTime());
                } else {
                    tend = null;
                }
                if (obs.getResult() instanceof DataArrayProperty) {
                    final DataArray array = ((DataArrayProperty)obs.getResult()).getDataArray();
                    final Values result   = getResultValues(tbegin, tend, array, eventTimes);
                    array.setValues(result.values.toString());
                    array.setElementCount(result.nbBlock);
                }
            }
            observations.add(obs);
        }
        return observations;
    }

    @Override
    public List<SamplingFeature> getFeatureOfInterests(Map<String, String> hints) throws DataStoreException {
        final String version                 = getVersionFromHints(hints);
        final List<SamplingFeature> features = new ArrayList<>();
        final Set<String> fid = filterFeatureOfInterest(hints);
        for (String foid : fid) {
            final SamplingFeature feature = reader.getFeatureOfInterest(foid, version);
            features.add(feature);
        }
        return features;
    }

    @Override
    public List<Phenomenon> getPhenomenons(Map<String, String> hints) throws DataStoreException {
        final String version  = getVersionFromHints(hints);
        final Set<String> fid = filterPhenomenon(hints);
        Map<String, Object> filters = new HashMap<>();
        filters.put(SOS_VERSION, version);
        filters.put(IDENTIFIER,  fid);
        return new ArrayList<>(reader.getPhenomenons(filters));
    }

    @Override
    public List<Process> getProcesses(Map<String, String> hints) throws DataStoreException {
        final String version          = getVersionFromHints(hints);
        final List<Process> processes = new ArrayList<>();
        final Set<String> pids = filterProcedure(hints);
        for (String pid : pids) {
            final Process pr = reader.getProcess(pid, version);
            processes.add(pr);
        }
        return processes;
    }

    @Override
    public String getResults(Map<String, String> hints) throws DataStoreException {
        final List<ObservationResult> results = filterResult(hints);
        final StringBuilder datablock         = new StringBuilder();

        for (ObservationResult result: results) {
            final Timestamp tBegin = result.beginTime;
            final Timestamp tEnd   = result.endTime;
            final Object r         =  reader.getResult(result.resultID, resultModel, "2.0.0");
            if (r instanceof DataArray || r instanceof DataArrayProperty) {
                final DataArray array;
                if (r instanceof DataArrayProperty) {
                    array = ((DataArrayProperty)r).getDataArray();
                } else {
                    array = (DataArray)r;
                }
                if (array != null) {
                    final Values resultValues = getResultValues(tBegin, tEnd, array, eventTimes);
                    final String brutValues   = resultValues.values.toString();
                    if (!brutValues.isEmpty()) {
                        datablock.append(brutValues);
                    }
                } else {
                    throw new IllegalArgumentException("Array is null");
                }
            } else if (r instanceof Measure) {
                final Measure meas = (Measure) r;
                datablock.append(tBegin).append(',').append(meas.getValue()).append("@@");
            } else {
                throw new IllegalArgumentException("Unexpected result type:" + r);
            }
        }
        return datablock.toString();
    }

    @Override
    public Object getOutOfBandResults() throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setResponseFormat(String responseFormat) {
        this.responseFormat = responseFormat;
    }

    @Override
    public boolean computeCollectionBound() {
        return false;
    }

    @Override
    public Envelope getCollectionBoundingShape() {
        throw new UnsupportedOperationException("Not supported.");
    }

    private String getVersionFromHints(Map<String, String> hints) {
        String version = "2.0.0";
        if (hints != null) {
            if (hints.containsKey("version")) {
                version = hints.get("version");
            }
        }
        return version;
    }

    @Override
    public Map<String, Geometry> getSensorLocations(Map<String, String> hints) throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public Map<String, Map<Date, Geometry>> getSensorHistoricalLocations(Map<String, String> hints) throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void initFilterGetProcedureTimes() throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Map<String, List<Date>> getSensorTimes(Map<String, String> hints) throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
