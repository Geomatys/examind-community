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


package org.constellation.store.metadata;

import org.geotoolkit.csw.xml.DomainValues;

import java.util.List;
import org.geotoolkit.metadata.MetadataIoException;
import org.geotoolkit.metadata.MetadataReader;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public interface CSWMetadataReader extends MetadataReader {


    /**
     * @param propertyNames A comma speparated list of property to retrieve.
     *
     * @return a list of values for each specific fields specified as a coma separated String.
     */
    List<DomainValues> getFieldDomainofValues(final String propertyNames) throws MetadataIoException;

    /**
     * @param token A property to retrieve.
     * @param identifier A metadata identifier.
     *
     * @return a list of values the specified fields specified.
     */
    public List<String> getFieldDomainofValuesForMetadata(String token, String identifier) throws MetadataIoException ;

    /**
     * Execute a SQL query and return the result as a List of identifier;
     */
    String[] executeEbrimSQLQuery(final String sqlQuery) throws MetadataIoException;

}
