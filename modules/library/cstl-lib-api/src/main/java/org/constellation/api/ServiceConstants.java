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
package org.constellation.api;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ServiceConstants {

    public static final String GET_CAPABILITIES = "GetCapabilities";



   /*
    * SOS
    */
    public static final String GET_FEATURE_OF_INTEREST = "GetFeatureOfInterest";
    public static final String GET_FEATURE_OF_INTEREST_TIME = "GetFeatureOfInterestTime";
    public static final String GET_OBSERVATION = "GetObservation";
    public static final String GET_RESULT = "GetResult";
    public static final String DESCRIBE_SENSOR = "DescribeSensor";
    public static final String DELETE_SENSOR = "DeleteSensor";
    public static final String GET_RESULT_TEMPLATE = "GetResultTemplate";
    public static final String GET_OBSERVATION_BY_ID = "GetObservationById";
    public static final String INSERT_OBSERVATION = "InsertObservation";
    public static final String REGISTER_SENSOR = "RegisterSensor";
    public static final String INSERT_SENSOR = "InsertSensor";
    public static final String INSERT_RESULT = "InsertResult";
    public static final String INSERT_RESULT_TEMPLATE = "InsertResultTemplate";
}
