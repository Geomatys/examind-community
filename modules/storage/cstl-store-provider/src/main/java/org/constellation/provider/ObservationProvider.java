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
package org.constellation.provider;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.sis.storage.Query;
import org.geotoolkit.observation.model.ObservationDataset;
import org.geotoolkit.observation.model.Offering;
import org.geotoolkit.observation.model.ProcedureDataset;
import org.constellation.dto.service.config.sos.SOSProviderCapabilities;
import org.constellation.dto.service.config.sos.SensorMLTree;
import org.constellation.exception.ConstellationStoreException;
import org.locationtech.jts.geom.Geometry;
import org.opengis.observation.Observation;
import org.opengis.observation.Phenomenon;
import org.opengis.observation.Process;
import org.opengis.observation.sampling.SamplingFeature;
import org.opengis.temporal.TemporalGeometricPrimitive;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public interface ObservationProvider extends DataProvider {

    Collection<String> getIdentifiers(Query q) throws ConstellationStoreException;

    List<ProcedureDataset> getProcedureTrees(Query query) throws ConstellationStoreException;

    List<Phenomenon> getPhenomenon(Query query) throws ConstellationStoreException;

    List<SamplingFeature> getFeatureOfInterest(Query query) throws ConstellationStoreException;

    List<Observation> getObservations(Query query) throws ConstellationStoreException;

    List<Process> getProcedures(Query query) throws ConstellationStoreException;

    List<Offering> getOfferings(Query query) throws ConstellationStoreException;

    Object getResults(Query q) throws ConstellationStoreException;

    SOSProviderCapabilities getCapabilities()  throws ConstellationStoreException;

    Geometry getSensorLocation(final String sensorID) throws ConstellationStoreException;

    boolean existEntity(final Query q) throws ConstellationStoreException;

    Offering getOffering(String name) throws ConstellationStoreException;

    Observation getTemplate(String sensorId) throws ConstellationStoreException;

    TemporalGeometricPrimitive getTimeForProcedure(final String sensorID) throws ConstellationStoreException;

    TemporalGeometricPrimitive getTimeForFeatureOfInterest(final String fid) throws ConstellationStoreException;

    TemporalGeometricPrimitive getTime() throws ConstellationStoreException;

    void removeProcedure(String procedureID) throws ConstellationStoreException;

    void removeObservation(final String observationID) throws ConstellationStoreException;
    
    void removePhenomenon(String phenomenonID) throws ConstellationStoreException;

    void writePhenomenons(final List<? extends Phenomenon> phens) throws ConstellationStoreException;

    String writeObservation(final Observation observation) throws ConstellationStoreException;

    void writeProcedure(final ProcedureDataset procedure) throws ConstellationStoreException;

    void updateProcedure(Process procedure) throws ConstellationStoreException;

    void writeOffering(Offering offering) throws ConstellationStoreException;

    void writeLocation(String procedureId, Geometry geom) throws ConstellationStoreException;

    Map<String, Map<Date, Geometry>> getHistoricalLocation(Query q) throws ConstellationStoreException;

    Map<String, Geometry> getLocation(Query q) throws ConstellationStoreException;

    Map<String, Set<Date>> getHistoricalTimes(Query q) throws ConstellationStoreException;

    ObservationDataset extractResults(Query query) throws ConstellationStoreException;

    List<String> removeDataset(ObservationDataset dataset) throws ConstellationStoreException;

    /**
     * Special key computed by the provider.
     * Used to detect if different provider are using the exact same datasource.
     *
     * @return a key identifyng the datasource.
     */
    String getDatasourceKey();

    /**
     * Count specific entities.
     *
     * @param q A query for filetring the result (can be {@code null}).
     *
     * @return A filtered count of the entities.
     * @throws ConstellationStoreException If the count can not be processed.
     */
    long getCount(Query q) throws ConstellationStoreException;

    /**
     * Return JTS geometries extract from a sensor tree.
     * If the sensor is a System, all its children geometries will be included in the result.
     *
     * @param sensor A sensor with all its children.
     *
     * @return A list of JTS geometries for the sensor and all its children.
     * @throws ConstellationStoreException If the sensor geometr can not be retrieved, or of the transformation to JTS fail.
     */
    List<org.locationtech.jts.geom.Geometry> getJTSGeometryFromSensor(final SensorMLTree sensor) throws ConstellationStoreException;
}