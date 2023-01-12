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
import javax.xml.namespace.QName;
import org.apache.sis.storage.Query;
import org.constellation.dto.service.config.sos.ExtractionResult;
import org.constellation.dto.service.config.sos.Offering;
import org.constellation.dto.service.config.sos.ProcedureTree;
import org.constellation.dto.service.config.sos.SOSProviderCapabilities;
import org.constellation.dto.service.config.sos.SensorMLTree;
import org.constellation.exception.ConstellationStoreException;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.Geometry;
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

    Collection<String> getOfferingNames(Query query, final Map<String, Object> hints) throws ConstellationStoreException;

    Collection<String> getProcedureNames(Query query, final Map<String, Object> hints) throws ConstellationStoreException;

    Collection<String> getPhenomenonNames(Query query, final Map<String, Object> hints) throws ConstellationStoreException;

    Collection<String> getFeatureOfInterestNames(Query query, final Map<String, Object> hints) throws ConstellationStoreException;
    
    Collection<String> getObservationNames(Query query, QName resultModel, String responseMode, final Map<String, Object> hints) throws ConstellationStoreException;

    List<ProcedureTree> getProcedureTrees(Query query, final Map<String, Object> hints) throws ConstellationStoreException;

    List<Phenomenon> getPhenomenon(Query query, final Map<String, Object> hints) throws ConstellationStoreException;

    List<SamplingFeature> getFeatureOfInterest(Query query, final Map<String, Object> hints) throws ConstellationStoreException;

    List<Observation> getObservations(Query query, QName resultModel, String responseMode, String responseFormat, final Map<String, Object> hints) throws ConstellationStoreException;

    List<Process> getProcedures(Query query, final Map<String, Object> hints) throws ConstellationStoreException;

    List<Offering> getOfferings(Query query, final Map<String, Object> hints) throws ConstellationStoreException;

    Object getResults(final String sensorID, QName resultModel, String responseMode, Query q, String responseFormat, Map<String, Object> hints) throws ConstellationStoreException;

    SOSProviderCapabilities getCapabilities()  throws ConstellationStoreException;

    Object getSensorLocation(final String sensorID, final String gmlVersion) throws ConstellationStoreException;

    boolean existPhenomenon(final String phenomenonName) throws ConstellationStoreException;

    boolean existProcedure(final String procedureName) throws ConstellationStoreException;

    boolean existFeatureOfInterest(final String foiName) throws ConstellationStoreException;

    boolean existOffering(final String offeringName, String version) throws ConstellationStoreException;

    Offering getOffering(String name, String version) throws ConstellationStoreException;

    Observation getTemplate(String sensorId, String version) throws ConstellationStoreException;

    TemporalGeometricPrimitive getTimeForProcedure(final String version, final String sensorID) throws ConstellationStoreException;

    TemporalGeometricPrimitive getTimeForFeatureOfInterest(final String version, final String fid) throws ConstellationStoreException;

    TemporalGeometricPrimitive getTime(final String version) throws ConstellationStoreException;

    void removeProcedure(String procedureID) throws ConstellationStoreException;

    void removeObservation(final String observationID) throws ConstellationStoreException;

    void writePhenomenons(final List<Phenomenon> phens) throws ConstellationStoreException;

    String writeObservation(final Observation observation) throws ConstellationStoreException;

    void writeProcedure(final ProcedureTree procedure) throws ConstellationStoreException;

    void writeTemplate(final Observation templateV100, Process procedure, List<? extends Object> observedProperties, String featureOfInterest) throws ConstellationStoreException;

    void updateProcedureLocation(final String procedureID, final Object position) throws ConstellationStoreException;

    void updateOffering(Offering offering) throws ConstellationStoreException;

    void writeOffering(Offering offering, List<? extends Object> observedProperties, List<String> smlFormats, String version) throws ConstellationStoreException;

    void writeLocation(String procedureId, Geometry geom) throws ConstellationStoreException;

    Map<String, Map<Date, Geometry>> getHistoricalLocation(Query q, final Map<String, Object> hints) throws ConstellationStoreException;

    Map<String, Geometry> getLocation(Query q, final Map<String, Object> hints) throws ConstellationStoreException;

    Map<String, List<Date>> getHistoricalTimes(Query q, final Map<String, Object> hints) throws ConstellationStoreException;

    ExtractionResult extractResults() throws ConstellationStoreException;
    ExtractionResult extractResults(final String affectedSensorID, final List<String> sensorIds) throws ConstellationStoreException;
    
   /*
    * The following 3 method will be removed and replace by the existing getFeatureOfInterestNames / getFeatureOfInterest with query
    */
    List<String> getFeaturesOfInterestForBBOX(List<String> offerings, final Envelope e, String version) throws ConstellationStoreException;
    List<String> getFeaturesOfInterestForBBOX(String offname, final Envelope e, String version) throws ConstellationStoreException;
    List<SamplingFeature> getFullFeaturesOfInterestForBBOX(String offname, final org.opengis.geometry.Envelope e, String version) throws ConstellationStoreException;
    
    /**
     * Special key computed by the provider.
     * Used to detect if different provider are using the exact same datasource.
     * 
     * @return a key identifyng the datasource.
     */
    String getDatasourceKey();

    /**
     * Count specific entities.
     * the hints map contains various parameters for filtering the request.
     * it must contains at least a parameter "objectType" which identify the entities we want to count.
     *
     * In the case of "observation" entity, you can specify the following hints:
     *  - "responseMode" an SOS response mode type (INLINE, ATTACHED, OUT_OF_BAND, RESULT_TEMPLATE)
     *  - "resultModel" a Qname determining the observation model (default to om:Observation).
     *
     * others hints may vary depending on the filter implementation and the entity type.
     *
     * @param q A query for filetring the result (can be {@code null}).
     * @param hints The parameters for the count (can be empty but not {@code null}).
     * 
     * @return A filtered count of the entities.
     * @throws ConstellationStoreException If the count can not be processed.
     */
    long getCount(Query q, final Map<String, Object> hints) throws ConstellationStoreException;

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
