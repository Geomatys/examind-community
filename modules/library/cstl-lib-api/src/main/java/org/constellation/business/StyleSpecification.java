/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2024 Geomatys.
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
package org.constellation.business;

import java.util.Map.Entry;
import java.util.Set;
import org.apache.sis.style.Style;
import org.constellation.exception.ConfigurationException;

/**
 * A registered style specification.
 * Provides methodes to create, encode and decode style from the database.
 *
 * @author Johann Sorel (Geomatys)
 */
public interface StyleSpecification<T extends Style> {

    /**
     * @return specification name
     */
    String getName();

    /**
     * @return collection of pre-existing style templates
     */
    Set<String> getTemplates();

    /**
     * @return Style class implementation supported
     */
    Class<T> getStyleClass();

    /**
     * Create a new style.
     *
     * @param template optional selected template, is null or blank a default one is created
     * @return created style
     * @throws ConfigurationException
     */
    T create(String template) throws ConfigurationException;

    /**
     * Store style parameters as a String to be stored in Database.
     *
     * @param style to store
     * @return encoded style parameters
     * @throws ConfigurationException
     */
    String encode(T style) throws ConfigurationException;

    /**
     * Decode style parameters from database configuration.
     *
     * @param content database stored parameters
     * @return style
     * @throws ConfigurationException
     */
    T decode(String content) throws ConfigurationException;

    /**
     * Encode the style for external viewers or editors.
     *
     * @param style to encode
     * @param subPath path element in the style to export
     * @return marshallable objet for jackson
     * @throws ConfigurationException
     */
    Entry<String, Object> exportToEdition(T style, String subPath) throws ConfigurationException;

    /**
     * Update the style with given json.
     * This json must have been generated with {@link #exportToEdition(org.apache.sis.style.Style) }
     *
     * @param style to update
     * @param subPath path element in the style to update
     * @param update to update with
     * @return modified style, may be a new instance
     * @throws ConfigurationException
     */
    T importFromEdition(T style, String subPath, Object update) throws ConfigurationException;

    /**
     * Delete all style resources stored in the examind folder.
     * @param style
     * @throws ConfigurationException
     */
    void deleteResources(T style) throws ConfigurationException;

}
