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

package org.constellation.sos.core;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotoolkit.gml.xml.GMLInstant;
import org.geotoolkit.gml.xml.AbstractFeature;
import org.geotoolkit.gml.xml.Envelope;
import org.geotoolkit.gml.xml.FeatureProperty;
import org.geotoolkit.gml.xml.TimeIndeterminateValueType;
import org.geotoolkit.observation.xml.AbstractObservation;
import org.geotoolkit.observation.xml.Process;
import org.geotoolkit.observation.xml.v100.MeasureType;
import org.geotoolkit.observation.xml.v100.MeasurementType;
import org.geotoolkit.sos.xml.Capabilities;
import org.geotoolkit.sos.xml.SOSXmlFactory;
import org.geotoolkit.sos.xml.v100.ObservationOfferingType;
import org.geotoolkit.swe.xml.AbstractDataRecord;
import org.geotoolkit.swe.xml.AbstractEncodingProperty;
import org.geotoolkit.swe.xml.AbstractTime;
import org.geotoolkit.swe.xml.AnyScalar;
import org.geotoolkit.swe.xml.DataArray;
import org.geotoolkit.swe.xml.DataArrayProperty;
import org.geotoolkit.swe.xml.DataComponentProperty;
import org.geotoolkit.swe.xml.DataRecord;
import org.geotoolkit.swe.xml.PhenomenonProperty;
import org.geotoolkit.swe.xml.SimpleDataRecord;
import org.geotoolkit.swe.xml.v101.CompositePhenomenonType;
import org.opengis.observation.Observation;
import org.opengis.observation.ObservationCollection;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;

/**
 * Static methods use to create valid XML file, by setting object into referenceMode.
 * The goal is to avoid to declare the same block many times in a XML file.
 *
 * @author Guilhem Legal (Geomatys)
 */
public final class Normalizer {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.sos");

    private Normalizer() {}

    /**
     * Normalize the capabilities document by replacing the double by reference
     *
     * @param capa the unnormalized document.
     *
     * @return a normalized document
     */
    public static Capabilities normalizeDocument(final Capabilities capa){
        if (capa instanceof org.geotoolkit.sos.xml.v100.Capabilities cp) {
            return normalizeDocumentv100(cp);
        } else {
            return capa; // no necessary in SOS 2
        }
    }

    /**
     * Normalize a SOS  capabilities verion 1.0.0 document by replacing the doublons by reference.
     *
     * @param capa the unnormalized document.
     *
     * @return a normalized document
     */
    private static Capabilities normalizeDocumentv100(final org.geotoolkit.sos.xml.v100.Capabilities capa){
        final List<PhenomenonProperty> alreadySee = new ArrayList<>();
        if (capa.getContents() != null) {
            for (ObservationOfferingType off: capa.getContents().getObservationOfferingList().getObservationOffering()) {
                for (PhenomenonProperty pheno: off.getRealObservedProperty()) {
                    if (alreadySee.contains(pheno)) {
                        pheno.setToHref();
                    } else {
                        if (pheno.getPhenomenon() instanceof CompositePhenomenonType compo) {
                            for (PhenomenonProperty pheno2: compo.getRealComponent()) {
                                if (alreadySee.contains(pheno2)) {
                                    pheno2.setToHref();
                                } else {
                                    alreadySee.add(pheno2);
                                }
                            }
                        }
                        alreadySee.add(pheno);
                    }
                }
            }
        }
        return capa;
    }

    /**
     * Regroup the different observations by sensor and by feature of interest.
     *
     * @param version SOS version.
     * @param bounds Already computed bounds of the collection.
     * @param collection An observation collection.
     *
     * @return An observation collection.
     */
    public static ObservationCollection regroupObservation(final String version, final Envelope bounds, final ObservationCollection collection){
        final List<Observation> members = collection.getMember();
        final Map<String, AbstractObservation> merged = new LinkedHashMap<>();
        for (Observation obs : members) {
            final Process process    = (Process) obs.getProcedure();
            final String featureID   = getFeatureID(obs);
            final String key;
            // we don't want to regroup the profile observations.
            if (isProfile(obs)) {
                key = UUID.randomUUID().toString();
            } else {
                key = process.getHref() + '-' + featureID;
            }

            if (obs instanceof MeasurementType meas) {
                // measurment are not merged
                merged.put(UUID.randomUUID().toString(), meas);

            } else if (merged.containsKey(key)) {
                final AbstractObservation uniqueObs = (AbstractObservation) merged.get(key);
                if (uniqueObs.getResult() instanceof DataArrayProperty mergedArrayP) {
                    final DataArray mergedArray          = mergedArrayP.getDataArray();

                    if (obs.getResult() instanceof DataArrayProperty arrayP) {
                        final DataArray array          = arrayP.getDataArray();

                        //we merge this observation with the map one
                        mergedArray.setElementCount(mergedArray.getElementCount().getCount().getValue() + array.getElementCount().getCount().getValue());
                        mergedArray.setValues(mergedArray.getValues() + array.getValues());
                    }
                }
                // merge the samplingTime
                if (uniqueObs.getSamplingTime() instanceof Period totalPeriod) {
                    if (obs.getSamplingTime() instanceof Instant instant) {
                        if (totalPeriod.getBeginning().getDate().getTime() > instant.getDate().getTime()) {
                            final Period newPeriod = SOSXmlFactory.buildTimePeriod(version,  new Timestamp(instant.getDate().getTime()), new Timestamp(totalPeriod.getEnding().getDate().getTime()));
                            uniqueObs.setSamplingTimePeriod(newPeriod);
                        }
                        if (totalPeriod.getEnding().getDate().getTime() < instant.getDate().getTime()) {
                            final Period newPeriod = SOSXmlFactory.buildTimePeriod(version,  totalPeriod.getBeginning().getDate(), instant.getDate());
                            uniqueObs.setSamplingTimePeriod(newPeriod);
                        }
                    } else if (obs.getSamplingTime() instanceof Period period) {
                        // BEGIN
                        if (TimeIndeterminateValueType.BEFORE.equals((((GMLInstant)totalPeriod.getBeginning()).getTimePosition()).getIndeterminatePosition()) ||
                            TimeIndeterminateValueType.BEFORE.equals((((GMLInstant)     period.getBeginning()).getTimePosition()).getIndeterminatePosition())) {
                            final Period newPeriod = SOSXmlFactory.buildTimePeriod(version,  ((GMLInstant) totalPeriod.getBeginning()).getTimePosition(), ((GMLInstant) period.getEnding()).getTimePosition());
                            uniqueObs.setSamplingTimePeriod(newPeriod);

                        } else if (totalPeriod.getBeginning().getDate().getTime() > period.getBeginning().getDate().getTime()) {
                            final Period newPeriod = SOSXmlFactory.buildTimePeriod(version,  period.getBeginning().getDate(), totalPeriod.getEnding().getDate());
                            uniqueObs.setSamplingTimePeriod(newPeriod);
                        }

                        // END
                        if (TimeIndeterminateValueType.NOW.equals((((GMLInstant)totalPeriod.getEnding()).getTimePosition()).getIndeterminatePosition()) ||
                            TimeIndeterminateValueType.NOW.equals((((GMLInstant)     period.getEnding()).getTimePosition()).getIndeterminatePosition())) {
                            final Period newPeriod = SOSXmlFactory.buildTimePeriod(version,  totalPeriod.getBeginning().getDate(), period.getEnding().getDate());
                            uniqueObs.setSamplingTimePeriod(newPeriod);

                        } else if (totalPeriod.getEnding().getDate().getTime() < period.getEnding().getDate().getTime()) {
                            final Period newPeriod = SOSXmlFactory.buildTimePeriod(version,  totalPeriod.getBeginning().getDate(), period.getEnding().getDate());
                            uniqueObs.setSamplingTimePeriod(newPeriod);
                        }
                    }
                }
            } else {
                merged.put(key, SOSXmlFactory.cloneObservation(version, obs));
            }
        }

        final List<AbstractObservation> obervations = new ArrayList<>();
        for (AbstractObservation entry: merged.values()) {
            obervations.add(entry);
        }
        return SOSXmlFactory.buildGetObservationResponse(version, "collection-1", bounds, obervations);
    }

    /**
     * Return the feature of interest identifier got the specified observation.
     *
     * @param obs A XML observation.
     * @return An identifer or {@code null}
     */
    private static String getFeatureID(final Observation obs) {
        if (obs instanceof AbstractObservation observation) {
            final FeatureProperty featProp = observation.getPropertyFeatureOfInterest();
            if (featProp != null) {
                if (featProp.getHref() != null) {
                    return featProp.getHref();
                } else if (featProp.getAbstractFeature() != null) {
                    final AbstractFeature feature = featProp.getAbstractFeature();
                    if (feature.getName() != null) {
                        return feature.getName().getCode();
                    } else if (feature.getId() != null) {
                        return feature.getId();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Normalize the Observation collection document by replacing the double by reference
     *
     * @param version SOS version.
     * @param collection the unnormalized document.
     *
     * @return a normalized document
     */
    public static ObservationCollection normalizeDocument(final String version, final ObservationCollection collection) {
        //first if the collection is empty
        if (collection.getMember().isEmpty()) {
            return SOSXmlFactory.buildObservationCollection(version, "urn:ogc:def:nil:OGC:inapplicable");
        }

        final List<FeatureProperty>          foiAlreadySee   = new ArrayList<>();
        final List<PhenomenonProperty>       phenoAlreadySee = new ArrayList<>();
        final List<AbstractEncodingProperty> encAlreadySee   = new ArrayList<>();
        final List<DataComponentProperty>    dataAlreadySee  = new ArrayList<>();
        for (Observation observation: collection.getMember()) {
            //we do this for the feature of interest
            final FeatureProperty foi =  getPropertyFeatureOfInterest(observation);
            if (foi != null) {
                if (foiAlreadySee.contains(foi)){
                    foi.setToHref();
                } else {
                    foiAlreadySee.add(foi);
                }
            }
            //for the phenomenon
            final PhenomenonProperty phenomenon = getPhenomenonProperty(observation);
            if (phenomenon != null) {
                if (phenoAlreadySee.contains(phenomenon)){
                    phenomenon.setToHref();
                } else {
                    if (phenomenon.getPhenomenon() instanceof CompositePhenomenonType compo) {
                        for (PhenomenonProperty pheno2: compo.getRealComponent()) {
                            if (phenoAlreadySee.contains(pheno2)) {
                                pheno2.setToHref();
                            } else {
                                phenoAlreadySee.add(pheno2);
                            }
                        }
                    }
                    phenoAlreadySee.add(phenomenon);
                }
            }
            //for the result : textBlock encoding and element type
            if (observation.getResult() instanceof DataArrayProperty dap) {
                final DataArray array = dap.getDataArray();

                //element type
                final DataComponentProperty elementType = array.getPropertyElementType();
                if (dataAlreadySee.contains(elementType)){
                    elementType.setToHref();
                } else {
                    dataAlreadySee.add(elementType);
                }

                //encoding
                final AbstractEncodingProperty encoding = array.getPropertyEncoding();
                if (encAlreadySee.contains(encoding)){
                    encoding.setToHref();

                } else {
                    encAlreadySee.add(encoding);
                }
            } else if (observation.getResult() instanceof MeasureType) {
                // do nothing
            } else {
                if (observation.getResult() != null) {
                    LOGGER.log(Level.WARNING, "NormalizeDocument: Class not recognized for result:{0}", observation.getResult().getClass().getSimpleName());
                } else {
                    LOGGER.warning("NormalizeDocument: The result is null");
                }
            }
        }
        return collection;
    }

    /**
     * Return the encapsulated feature of interest for an XML observation.
     *
     * @param obs A XML observation.
     * @return
     */
    private static FeatureProperty getPropertyFeatureOfInterest(final Observation obs) {
        if (obs instanceof AbstractObservation observation) {
            return observation.getPropertyFeatureOfInterest();
        }
        return null;
    }

    /**
     * Return the encapsulated phenomenon for an XML observation.
     *
     * @param obs A XML observation.
     * @return
     */
    private static PhenomenonProperty getPhenomenonProperty(final Observation obs) {
        if (obs instanceof AbstractObservation observation) {
            return observation.getPropertyObservedProperty();
        }
        return null;
    }

    /**
     * Determine if the observation is a profile by looking for its data array first field type.
     * 
     * @param obs A XML observation.
     * @return
     */
    private static boolean isProfile(final Observation obs) {
        if (obs.getResult() instanceof DataArrayProperty dap && dap.getDataArray() != null) {
            DataArray dataArray = dap.getDataArray();
            DataComponentProperty dcp = dataArray.getPropertyElementType();
            if (dcp.getValue() instanceof DataRecord dr && !dr.getField().isEmpty()) {
                DataComponentProperty f = dr.getField().get(0);
                return f.getTime() == null;
            } else if (dcp.getValue() instanceof SimpleDataRecord dr && !dr.getField().isEmpty()) {
                AnyScalar f = dr.getField().iterator().next();
                return !(f.getValue() instanceof AbstractTime);
            }
            return false;
        }
        return false;
    }
}
