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

    public static final String HANDLE    = "handle";
    public static final String NAMESPACE = "namespace";
    public static final String FILTER    = "filter";

    public static final String STR_GETCAPABILITIES         = "GetCapabilities";
    public static final String STR_DESCRIBEFEATURETYPE     = "DescribeFeatureType";
    public static final String STR_GETFEATURE              = "GetFeature";
    public static final String STR_GETGMLOBJECT            = "getGMLObject";
    public static final String STR_LOCKFEATURE             = "lockFeature";
    public static final String STR_TRANSACTION             = "Transaction";
    public static final String STR_DESCRIBE_STORED_QUERIES = "DescribeStoredQueries";
    public static final String STR_LIST_STORED_QUERIES     = "ListStoredQueries";
    public static final String STR_GET_PROPERTY_VALUE      = "GetPropertyValue";
    public static final String STR_CREATE_STORED_QUERY     = "CreateStoredQuery";
    public static final String STR_DROP_STORED_QUERY       = "DropStoredQuery";
    public static final String STR_XSD                     = "xsd";

    public static final String UNKNOW_TYPENAME= "The specified TypeNames does not exist:";

    /**
     * The Mime type for describe feature GML 3.1.1
     */
    public final static String GML_3_1_1_MIME = "text/xml; subtype=\"gml/3.1.1\"";

    public final static String GML_3_2_1_MIME = "application/gml+xml; version=3.2";


}
