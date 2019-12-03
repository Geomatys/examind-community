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
import org.geotoolkit.sts.GetCapabilities;
import org.geotoolkit.sts.GetDatastreamById;
import org.geotoolkit.sts.GetDatastreams;
import org.geotoolkit.sts.GetFeatureOfInterestById;
import org.geotoolkit.sts.GetFeatureOfInterests;
import org.geotoolkit.sts.GetHistoricalLocations;
import org.geotoolkit.sts.GetLocations;
import org.geotoolkit.sts.GetMultiDatastreamById;
import org.geotoolkit.sts.GetMultiDatastreams;
import org.geotoolkit.sts.GetObservationById;
import org.geotoolkit.sts.GetObservations;
import org.geotoolkit.sts.GetObservedProperties;
import org.geotoolkit.sts.GetObservedPropertyById;
import org.geotoolkit.sts.GetSensorById;
import org.geotoolkit.sts.GetSensors;
import org.geotoolkit.sts.GetThings;
import org.geotoolkit.sts.json.Datastream;
import org.geotoolkit.sts.json.DatastreamsResponse;
import org.geotoolkit.sts.json.FeatureOfInterest;
import org.geotoolkit.sts.json.FeatureOfInterestsResponse;
import org.geotoolkit.sts.json.HistoricalLocation;
import org.geotoolkit.sts.json.HistoricalLocationsResponse;
import org.geotoolkit.sts.json.Location;
import org.geotoolkit.sts.json.LocationsResponse;
import org.geotoolkit.sts.json.MultiDatastream;
import org.geotoolkit.sts.json.MultiDatastreamsResponse;
import org.geotoolkit.sts.json.Observation;
import org.geotoolkit.sts.json.ObservedPropertiesResponse;
import org.geotoolkit.sts.json.ObservedProperty;
import org.geotoolkit.sts.json.STSCapabilities;
import org.geotoolkit.sts.json.Sensor;
import org.geotoolkit.sts.json.SensorsResponse;
import org.geotoolkit.sts.json.Thing;
import org.geotoolkit.sts.json.ThingsResponse;


/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public interface STSWorker extends Worker {

    ThingsResponse getThings(GetThings gt) throws CstlServiceException;

    void addThing(Thing thing) throws CstlServiceException;

    Object getObservations(GetObservations go) throws CstlServiceException;

    Observation getObservationById(GetObservationById goi) throws CstlServiceException;

    void addObservation(Observation observation) throws CstlServiceException;

    DatastreamsResponse getDatastreams(GetDatastreams gd) throws CstlServiceException;

    Datastream getDatastreamById(GetDatastreamById gd) throws CstlServiceException;

    MultiDatastreamsResponse getMultiDatastreams(GetMultiDatastreams gd) throws CstlServiceException;

    MultiDatastream getMultiDatastreamById(GetMultiDatastreamById gd) throws CstlServiceException;

    void addDatastream(Datastream datastream) throws CstlServiceException;

    ObservedPropertiesResponse getObservedProperties(GetObservedProperties gop) throws CstlServiceException;

    ObservedProperty getObservedPropertyById(GetObservedPropertyById gop) throws CstlServiceException;

    void addObservedProperty(ObservedProperty observedProperty) throws CstlServiceException;

    LocationsResponse getLocations(GetLocations gl) throws CstlServiceException;

    void addLocation(Location location) throws CstlServiceException;

    SensorsResponse getSensors(GetSensors gs) throws CstlServiceException;

    Sensor getSensorById(GetSensorById gs) throws CstlServiceException;

    void addSensor(Sensor sensor) throws CstlServiceException;

    FeatureOfInterestsResponse getFeatureOfInterests(GetFeatureOfInterests gfi) throws CstlServiceException;

    FeatureOfInterest getFeatureOfInterestById(GetFeatureOfInterestById gfi) throws CstlServiceException;

    void addFeatureOfInterest(FeatureOfInterest foi) throws CstlServiceException;

    HistoricalLocationsResponse getHistoricalLocations(GetHistoricalLocations gh) throws CstlServiceException;

    void addHistoricalLocation(HistoricalLocation HistoricalLocation) throws CstlServiceException;

    STSCapabilities getCapabilities(GetCapabilities gc);
}
