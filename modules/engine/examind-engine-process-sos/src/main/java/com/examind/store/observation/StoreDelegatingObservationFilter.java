/*
 *    Examind - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2022 Geomatys.
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
package com.examind.store.observation;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.gml.xml.Envelope;
import org.geotoolkit.observation.ObservationFilterReader;
import org.geotoolkit.observation.ObservationResult;
import org.geotoolkit.observation.ObservationStore;
import org.geotoolkit.observation.model.OMEntity;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.TemporalOperator;
import org.opengis.geometry.Geometry;
import org.opengis.observation.Observation;
import org.opengis.observation.Phenomenon;
import org.opengis.observation.Process;
import org.opengis.observation.sampling.SamplingFeature;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class StoreDelegatingObservationFilter implements ObservationFilterReader {
    
    private final ObservationStore store;

    protected OMEntity objectType = null;

    public StoreDelegatingObservationFilter(ObservationStore store) {
        this.store = store;
    }

    @Override
    public void init(OMEntity objectType, Map<String, Object> hints) throws DataStoreException {
         this.objectType = objectType;
    }

    @Override
    public void setProcedure(List<String> procedures) throws DataStoreException {
        if (!procedures.isEmpty()) throw new UnsupportedOperationException("Procedure filtering is not supported yet.");
    }

    @Override
    public void setProcedureType(String type) throws DataStoreException {
        if (type != null) throw new UnsupportedOperationException("Procedure type filtering is not supported yet.");
    }

    @Override
    public void setObservedProperties(List<String> phenomenons) {
        if (!phenomenons.isEmpty()) throw new UnsupportedOperationException("Observed properties filtering is not supported yet.");
    }

    @Override
    public void setFeatureOfInterest(List<String> fois) {
        if (!fois.isEmpty()) throw new UnsupportedOperationException("Feature od interest filtering is not supported yet.");
    }

    @Override
    public void setObservationIds(List<String> ids) {
        if (!ids.isEmpty()) throw new UnsupportedOperationException("Observed id filtering is not supported yet.");
    }

    @Override
    public void setTimeFilter(TemporalOperator tFilter) throws DataStoreException {
        if (tFilter != null) throw new UnsupportedOperationException("Time filtering is not supported yet.");
    }

    @Override
    public void setBoundingBox(Envelope e) throws DataStoreException {
         if (e != null) throw new UnsupportedOperationException("BBOX filtering is not supported yet.");
    }

    @Override
    public void setOfferings(List<String> offerings) throws DataStoreException {
        if (!offerings.isEmpty()) throw new UnsupportedOperationException("Offering filtering is not supported yet.");
    }

    @Override
    public void setResultFilter(BinaryComparisonOperator filter) throws DataStoreException {
        if (filter != null) throw new UnsupportedOperationException("Result filtering is not supported yet.");
    }

    @Override
    public List<ObservationResult> filterResult() throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<String> getIdentifiers() throws DataStoreException {
        if (objectType == null) {
            throw new DataStoreException("initialisation of the filter missing.");
        }
        String request;
        switch (objectType) {
            case OBSERVED_PROPERTY: return store.getPhenomenonNames();
            case PROCEDURE: return store.getProcedureNames().stream().map(gn -> gn.tip().toString()).collect(Collectors.toSet());
            case FEATURE_OF_INTEREST:
            case OFFERING:
            case OBSERVATION:
            case LOCATION:
            case HISTORICAL_LOCATION:
            case RESULT:
                throw new DataStoreException("not implemented yet.");
            default:
                throw new DataStoreException("unexpected object type:" + objectType);
        }
    }

    @Override
    public long getCount() throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void refresh() throws DataStoreException {
        //do nothing
    }

    @Override
    public void destroy() {
        // do nothing
    }

    @Override
    public List<Observation> getObservations() throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<SamplingFeature> getFeatureOfInterests() throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Phenomenon> getPhenomenons() throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Process> getProcesses() throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Map<String, Geometry> getSensorLocations() throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Map<String, Map<Date, Geometry>> getSensorHistoricalLocations() throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Map<String, List<Date>> getSensorTimes() throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object getResults() throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Envelope getCollectionBoundingShape() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
