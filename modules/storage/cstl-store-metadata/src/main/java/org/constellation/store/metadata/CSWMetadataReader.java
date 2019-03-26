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
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;
import org.constellation.api.PathType;
import org.geotoolkit.metadata.ElementSetType;
import org.geotoolkit.metadata.MetadataIoException;
import org.geotoolkit.metadata.MetadataReader;
import org.geotoolkit.metadata.MetadataType;

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
     * Return a metadata object from the specified identifier.
     *
     * @param identifier The metadata identifier.
     * @param mode An output schema mode: EBRIM, ISO_19115, DUBLINCORE and SENSORML supported.
     * @param type An elementSet: FULL, SUMMARY and BRIEF. (implies elementName == null)
     * @param elementName A list of QName describing the requested fields. (implies type == null)
     *
     * @return A marshallable metadata object.
     */
    Node getMetadata(final String identifier, final MetadataType mode, final ElementSetType type, final List<QName> elementName) throws MetadataIoException;

    /**
     * @return the list of QName for additional queryable element.
     */
    List<QName> getAdditionalQueryableQName();

    /**
     * @return the list of path for the additional queryable element.
     */
    Map<String, PathType> getAdditionalQueryablePathMap();

    /**
     * Execute a SQL query and return the result as a List of identifier;
     */
    String[] executeEbrimSQLQuery(final String sqlQuery) throws MetadataIoException;

}
