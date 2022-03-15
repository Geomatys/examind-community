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
package org.constellation.api;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

/**
 *
 * @author Guilhem Legal (Geomatys)
 * @since 0.9
 */
public class CommonConstants {

    /*
     * Default declareded CRS codes for each layer in the getCapabilities
     */
    public static final List<String> DEFAULT_CRS = new ArrayList<>();
    static {
        DEFAULT_CRS.add("EPSG:4326");
        DEFAULT_CRS.add("CRS:84");
        DEFAULT_CRS.add("EPSG:3395");
        DEFAULT_CRS.add("EPSG:3857");
        DEFAULT_CRS.add("EPSG:27571");
        DEFAULT_CRS.add("EPSG:27572");
        DEFAULT_CRS.add("EPSG:27573");
        DEFAULT_CRS.add("EPSG:27574");
    }

    public static final List<String> WXS = new ArrayList<>();
    static {
        WXS.add("WMS");
        WXS.add("WCS");
        WXS.add("WFS");
        WXS.add("WMTS");
    }

    public static final String SUCCESS = "Success";

    public static final String SERVICE = "Service";

    /**
     * SOS related constants
     */

    public static final String SENSORML_100_FORMAT_V100 = "text/xml;subtype=\"sensorML/1.0.0\"";
    public static final String SENSORML_101_FORMAT_V100 = "text/xml;subtype=\"sensorML/1.0.1\"";
    public static final String SENSORML_100_FORMAT_V200 = "http://www.opengis.net/sensorML/1.0.0";
    public static final String SENSORML_101_FORMAT_V200 = "http://www.opengis.net/sensorML/1.0.1";

    public static final String SENSOR_ID_BASE = "sensor-id-base";
    public static final String ALWAYS_FEATURE_COLLECTION = "alwaysFeatureCollection";
    public static final String SHEMA_PREFIX = "schemaPrefix";
    public static final String SENSOR_TYPE_FILTER = "sensorTypeFilter";

    public static final String SOS = "SOS";
    public static final String ALL = "All";
    public static final String OBJECT_TYPE = "objectType";
    public static final String OFFERING = "offering";
    public static final String LOCATION = "location";
    public static final String OBSERVED_PROPERTY = "observedProperty";
    public static final String EVENT_TIME = "eventTime";
    public static final String PROCEDURE = "procedure";
    public static final String SRS_NAME = "srsName";
    public static final String OBSERVATION = "observation";
    public static final String HISTORICAL_LOCATION = "historicalLocation";
    public static final String FEATURE_OF_INTEREST = "featureOfInterest";
    public static final String OBSERVATION_ID = "observationId";
    public static final String OUTPUT_FORMAT = "outputFormat";
    public static final String OBSERVATION_TEMPLATE = "observationTemplate";
    public static final String PROCEDURE_DESCRIPTION_FORMAT = "procedureDescriptionFormat";
    public static final String RESPONSE_MODE = "responseMode";
    public static final String RESPONSE_FORMAT = "responseFormat";
    public static final String RESULT_MODEL = "resultModel";
    public static final String RESULT = "result";
    public static final String NOT_SUPPORTED = "This operation is not take in charge by the Web Service";
    public static final String RESPONSE_FORMAT_V100_XML     = "text/xml; subtype=\"om/1.0.0\"";
    public static final String RESPONSE_FORMAT_V200_XML     = "http://www.opengis.net/om/2.0";
    public static final String RESPONSE_FORMAT_V200_JSON    = "application/json;subtype=\"http://www.opengis.net/om/2.0\"";

    public static final String OM_NAMESPACE = "http://www.opengis.net/om/1.0";

    /**
     * The base Qname for complex observation.
     */
    public static final QName OBSERVATION_QNAME = new QName(OM_NAMESPACE, "Observation", "om");

    public static final String OBSERVATION_MODEL = "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Observation";

    /**
     * The base Qname for measurement observation.
     */
    public static final QName MEASUREMENT_QNAME = new QName(OM_NAMESPACE, "Measurement", "om");

    public static final String QUERY_CONSTRAINT = "QueryConstraint";

    public static final String XML_EXT    = ".xml";
    public static final String NETCDF_EXT = ".nc";
    public static final String NCML_EXT   = ".ncml";

    public static final String NULL_VALUE = "null";

    public static final String CSW_CONFIG_ONLY_PUBLISHED = "onlyPublished";
    public static final String CSW_CONFIG_PARTIAL = "partial";
    public static final String TRANSACTIONAL = "transactional";
    public static final String TRANSACTION_SECURIZED = "transactionSecurized";
}
