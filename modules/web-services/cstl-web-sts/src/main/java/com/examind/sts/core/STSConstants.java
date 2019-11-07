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

/**
 *  WFS Constants
 *
 * @author Guilhem Legal (Geomatys)
 */
public final class STSConstants {

    private STSConstants() {}

    public static final String ORDERBY    = "$orderby";
    public static final String SELECT    = "$select";
    public static final String EXPAND    = "$expand";
    public static final String TOP       = "$top";
    public static final String SKIP      = "$skip";
    public static final String COUNT     = "$count";
    public static final String FILTER    = "$filter";

    public static final String STR_GETFEATUREOFINTEREST    = "GetFeatureOfInterests";
    public static final String STR_GETTHINGS               = "GetThings";
    public static final String STR_GETOBSERVATION          = "GetObservations";
    public static final String STR_GETDATASTREAMS          = "getDatastreams";
    public static final String STR_GETOBSERVEDPROPERTIES   = "GetObservedProperties";
    public static final String STR_GETLOCATIONS            = "GetLocations";
    public static final String STR_GETSENSORS              = "GetSensors";
    public static final String STR_GETSENSOR_BYID          = "GetSensorById";
    public static final String STR_GETHISTORICALLOCATIONS  = "HistoricalLocations";


}
