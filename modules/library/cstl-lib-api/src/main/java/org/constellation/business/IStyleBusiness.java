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

import org.constellation.api.StyleType;
import org.constellation.exception.ConfigurationException;
import org.constellation.dto.StyleBrief;
import org.constellation.exception.TargetNotFoundException;
import org.constellation.dto.process.StyleProcessReference;
import org.constellation.exception.ConstellationException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Cédric Briançon (Geomatys)
 */
public interface IStyleBusiness {

    List<StyleProcessReference> getAllStyleReferences();

    void deleteStyle(int id) throws ConfigurationException;

    void writeStyle(String key, Integer id, StyleType styleType, org.opengis.style.Style style) throws IOException;

    org.opengis.style.Style getStyle(String providerID, String styleName) throws TargetNotFoundException;

    Integer getStyleId(final String providerId, final String styleName) throws TargetNotFoundException;

    org.opengis.style.Style getStyle(int styleId) throws TargetNotFoundException;

    boolean existsStyle(final String providerId, final String styleName) throws TargetNotFoundException;

    boolean existsStyle(final int styleId);

    /**
     *
     * @param serviceType
     * @param serviceIdentifier
     * @param layerName
     * @param styleProviderId
     * @param styleName
     * @throws TargetNotFoundException
     * @deprecated Do not use it anymore ! It uses layer name to find one in database, but name has not any unique
     * constraint. Please prefer {@link #linkToLayer(int, int)} instead.
     */
    @Deprecated
    void createOrUpdateStyleFromLayer(String serviceType, String serviceIdentifier, String layerName, String styleProviderId,
                                      String styleName) throws TargetNotFoundException;

    void linkToLayer(final int styleId, final int layerId) throws ConfigurationException;

    void removeStyleFromLayer(String serviceIdentifier, String serviceType, String layerName, String styleProviderId,
                              String styleName) throws TargetNotFoundException;

    StyleBrief createStyle(String providerId, org.opengis.style.Style style) throws ConfigurationException;

    List<StyleBrief> getAvailableStyles(String category) throws ConstellationException;

    List<StyleBrief> getAvailableStyles(String providerId, String category) throws ConstellationException;

    void updateStyle(int id, org.opengis.style.Style style) throws ConfigurationException;

    void linkToData(int styleId, int dataId) throws ConfigurationException;

    void unlinkFromData(int styleId, int dataId) throws ConfigurationException;

    void unlinkAllFromData(int dataId) throws ConfigurationException;

    Map.Entry<Integer, List<StyleBrief>> filterAndGetBrief(final Map<String,Object> filterMap, final Map.Entry<String,String> sortEntry,final int pageNumber,final int rowsPerPage);

    void updateSharedProperty(final List<Integer> ids, final boolean shared) throws ConfigurationException;

    void updateSharedProperty(final int id, final boolean shared) throws ConfigurationException;

}
