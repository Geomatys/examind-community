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
package org.constellation.business;

import org.constellation.exception.ConfigurationException;
import org.constellation.dto.StyleBrief;
import org.constellation.exception.TargetNotFoundException;
import org.constellation.dto.process.StyleProcessReference;
import org.constellation.exception.ConstellationException;

import java.util.List;
import java.util.Map;

/**
 * @author Cédric Briançon (Geomatys)
 */
public interface IStyleBusiness {

    List<StyleProcessReference> getAllStyleReferences();

    void deleteStyle(int id) throws ConfigurationException;

    void deleteAll() throws ConfigurationException;

    /**
     * Gets and returns the {@link org.opengis.style.Style} that matches with the specified
     * identifier.
     *
     * @param providerID The style provider identifier (sld or sld_temp).
     * @param styleName The style name.
     *
     * @return the {@link org.opengis.style.Style} instance
     * @throws TargetNotFoundException If the style with the specified identifier can't be found.
     */
    org.opengis.style.Style getStyle(String providerID, String styleName) throws TargetNotFoundException;

    /**
     * Find and return an id that matches with the specified provider id / style name.
     *
     * @param providerId The style provider identifier (sld or sld_temp).
     * @param styleName The style name.
     *
     * @return the {@link Style} instance.
     * @throws TargetNotFoundException If the style with the specified providerId and styleName can't be found.
     */
    Integer getStyleId(final String providerId, final String styleName) throws TargetNotFoundException;

    /**
     * Gets and returns the {@link org.opengis.style.Style} that matches with the specified id.
     *
     * @param styleId style entity id.
     * @return the {@link org.opengis.style.Style} instance
     * @throws TargetNotFoundException
     *             if the style with the specified identifier can't be found
     */
    org.opengis.style.Style getStyle(int styleId) throws TargetNotFoundException;

    /**
     * Flag that returns if style exists for given provider and style name.
     *
     * @param providerId The style provider identifier (sld or sld_temp).
     * @param styleName The style identifier.
     *
     * @return boolean
     * @throws TargetNotFoundException
     *             if the provider with the specified identifier can't be found
     */
    boolean existsStyle(final String providerId, final String styleName) throws TargetNotFoundException;

    /**
     * Returns if style exists for given identifier.
     *
     * @param styleId The style identifier.
     * @return boolean
     */
    boolean existsStyle(final int styleId);

    void linkToLayer(final int styleId, final int layerId) throws ConfigurationException;

    void unlinkToLayer(final int styleId, final int layerId) throws ConfigurationException;

    /**
     * Creates a new style into a style provider instance.
     *
     * @param providerId The style provider identifier (sld or sld_temp).
     * @param style The style body.
     * @return The assigned style id.
     *
     * @throws TargetNotFoundException If the style with the specified identifier can't be found.
     * @throws ConfigurationException If the operation has failed for any reason.
     */
    Integer createStyle(String providerId, org.opengis.style.Style style) throws ConfigurationException;

    /**
     * Returns the list of available styles as {@link StyleBrief} object.
     *
     * @param type Style type (VECTOR / COVERAGE)
     * @return a {@link List} of {@link StyleBrief} instances
     * @throws org.constellation.exception.ConstellationException
     */
    List<StyleBrief> getAvailableStyles(String type) throws ConstellationException;

    /**
     * Returns the list of available styles as {@link StyleBrief} object for the
     * style provider with the specified identifier.
     *
     * @param providerId The style provider identifier (sld or sld_temp).
     * @param type (VECTOR / COVERAGE)
     * @throws TargetNotFoundException if the style provider does not exist.
     * @return a {@link List} of {@link StyleBrief} instances
     */
    List<StyleBrief> getAvailableStyles(String providerId, String type) throws ConstellationException;

    void updateStyle(int id, org.opengis.style.Style style) throws ConfigurationException;

    /**
     * Links a style resource to an existing data resource.
     *
     * @param styleId The style identifier.
     * @param dataId The data identifier.
     *
     * @throws TargetNotFoundException If the style or Data with the specified identifier can't be found.
     * @throws ConfigurationException If the operation has failed for any reason.
     */
    void linkToData(int styleId, int dataId) throws ConfigurationException;

    /**
     * Unlink a style resource from an existing data resource.
     *
     * @param styleId The style identifier
     * @param dataId The data identifier
     * @throws TargetNotFoundException if the style or Data with the specified identifier can't be found
     * @throws ConfigurationException if the operation has failed for any reason
     */
    void unlinkFromData(int styleId, int dataId) throws ConfigurationException;

    void unlinkAllFromData(int dataId) throws ConfigurationException;

    Map.Entry<Integer, List<StyleBrief>> filterAndGetBrief(final Map<String,Object> filterMap, final Map.Entry<String,String> sortEntry,final int pageNumber,final int rowsPerPage);

    void updateSharedProperty(final List<Integer> ids, final boolean shared) throws ConfigurationException;

    void updateSharedProperty(final int id, final boolean shared) throws ConfigurationException;
    
    void unlinkAllFromLayer(int layerId) throws ConfigurationException;

}
