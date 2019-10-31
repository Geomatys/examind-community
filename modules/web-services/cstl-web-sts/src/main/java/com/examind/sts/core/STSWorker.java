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

package com.examind.sts.core;


import org.constellation.ws.CstlServiceException;
import org.constellation.ws.Worker;
import org.geotoolkit.sts.json.Datastream;
import org.geotoolkit.sts.json.DatastreamsResponse;
import org.geotoolkit.sts.json.FeatureOfInterest;
import org.geotoolkit.sts.json.FeatureOfInterestsResponse;
import org.geotoolkit.sts.json.HistoricalLocation;
import org.geotoolkit.sts.json.HistoricalLocationsResponse;
import org.geotoolkit.sts.json.Location;
import org.geotoolkit.sts.json.LocationsResponse;
import org.geotoolkit.sts.json.Observation;
import org.geotoolkit.sts.json.ObservationsResponse;
import org.geotoolkit.sts.json.ObservedPropertiesResponse;
import org.geotoolkit.sts.json.ObservedProperty;
import org.geotoolkit.sts.json.Sensor;
import org.geotoolkit.sts.json.SensorsResponse;
import org.geotoolkit.sts.json.Thing;
import org.geotoolkit.sts.json.ThingsResponse;


/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public interface STSWorker extends Worker {

    ThingsResponse getThings() throws CstlServiceException;

    void addThing(Thing thing) throws CstlServiceException;

    ObservationsResponse getObservations() throws CstlServiceException;

    void addObservation(Observation observation) throws CstlServiceException;

    DatastreamsResponse getDatastreams() throws CstlServiceException;

    void addDatastream(Datastream datastream) throws CstlServiceException;

    ObservedPropertiesResponse getObservedProperties() throws CstlServiceException;

    void addObservedProperty(ObservedProperty observedProperty) throws CstlServiceException;

    LocationsResponse getLocations() throws CstlServiceException;

    void addLocation(Location location) throws CstlServiceException;

    SensorsResponse getSensors() throws CstlServiceException;

    void addSensor(Sensor sensor) throws CstlServiceException;

    FeatureOfInterestsResponse getFeatureOfInterests() throws CstlServiceException;

    void addFeatureOfInterest(FeatureOfInterest foi) throws CstlServiceException;

    HistoricalLocationsResponse getHistoricalLocations() throws CstlServiceException;

    void addHistoricalLocation(HistoricalLocation HistoricalLocation) throws CstlServiceException;
}
