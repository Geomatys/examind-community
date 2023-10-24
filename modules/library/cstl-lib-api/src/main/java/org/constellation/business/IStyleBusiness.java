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
import org.opengis.sld.StyledLayerDescriptor;
import org.opengis.style.Style;

/**
 * @author Cédric Briançon (Geomatys)
 * @author Guilhem Legal (Geomatys)
 */
public interface IStyleBusiness {

    /**
     * Return all the styles references.
     *
     * @param providerID Optional parameter to filter on provider (sld or sld_temp).
     * @return a list of styles references.
     */
    List<StyleProcessReference> getAllStyleReferences(String providerID) throws ConfigurationException;

    /**
     * Delete the stye with the specified identifier.
     * if the style does not exist return 0.
     *
     * @param id Style identifier
     *
     * @return 1 if the deletetion succeed, 0 if the style can not be found.
     *
     * @throws ConfigurationException If an error occurs during the removal.
     */
    int deleteStyle(int id) throws ConfigurationException;

    /**
     * Delete all the styles registered in the datasource.
     * Mostly used in test, be cautious when calling this method.
     *
     * @return Te number of styles deleted.
     *
     * @throws ConfigurationException If an error occurs during the removal.
     */
    int deleteAll() throws ConfigurationException;

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

    void updateActivateStatsForLayerAndStyle(final int styleId, final int layerId, final boolean activateStats) throws TargetNotFoundException;
    void unlinkToLayer(final int styleId, final int layerId) throws ConfigurationException;

    void setDefaultStyleToLayer(int styleId, int layerId) throws ConfigurationException;

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
     * Return the @{@link StyleBrief} object for a given style id.
     *
     * @param id The style identifier.
     * @return The {@link StyleBrief}.
     */
    StyleBrief getStyleBrief(final int id) throws TargetNotFoundException;

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

    /**
     * Try to extract a style from various input, with all the available known specification.
     * - User style in Symbology encoding 1.1.0 or 1.O.O
     * - First style in a SLD in format 1.1.0 or 1.O.O
     * - Feature type style.
     * - created style from a palette file.
     *
     * For pallete file import, source must be either a String or a byte[], as palette reader only support these two.
     *
     * @param styleName Affected style name.
     * @param source source object containing the xml.
     * @param fileName Optional parameter used for palette type recognition.
     *
     * @return A parsed style
     */
    public Style parseStyle(final String styleName, final Object source, final String fileName);

    /**
     * Read a SLD from either a XML String or from an URL (pointing to an XML file).This method try to read it in all the available version until it succesfully read it.
     *
     * @param sldSrc A xml source containing a SLD (can be an input stream, an URL, a String, etc).
     * @param throwEx If set to false, any read error will be ignored an {@code null} can be returned.
     *
     * @return A StyledLayerDescriptor.
     * @throws ConstellationException If the XML can not be read in any version (and throwEx set to {@code true}).
     */
    StyledLayerDescriptor readSLD(final Object sldSrc, boolean throwEx) throws ConstellationException;


    /**
     * Read a SLD from either a XML String or from an URL (pointing to an XML file).
     *
     * @param sldSrc A xml source containing a SLD (can be an input stream, an URL, a String, etc).
     * @param sldVersion SLD version of the object.
     *
     * @return A StyledLayerDescriptor.
     * @throws ConstellationException If the XML is malformed. If the sldVersion is not supported.
     */
    StyledLayerDescriptor readSLD(final Object sldSrc, final String sldVersion) throws ConstellationException;


    /**
     * Read a Style from either a XML String or from an URL (pointing to an XML file).This method try to read it in all the available version until it succesfully read it.
     *
     * @param sldSrc A xml source containing a Style (can be an input stream, an URL, a String, etc).
     * @param throwEx If set to false, any read error will be ignored an {@code null} can be returned.
     *
     * @return A Style.
     * @throws ConstellationException If the XML can not be read in any version (and throwEx set to {@code true}).
     */
    Style readStyle(final Object sldSrc, boolean throwEx) throws ConstellationException;

    /**
     * Read a Style from either a XML String or from an URL (pointing to an XML file).
     *
     * @param styleSrc A xml source containing a Style (can be an input stream, an URL, a String, etc).
     * @param seVersion Symbology encoding version of the object.
     *
     * @return A Style.
     * @throws ConstellationException If the XML is malformed. If the seVersion is not supported.
     */
    Style readStyle(final Object styleSrc, final String seVersion) throws ConstellationException;

    /**
     * Get the extraInfo from StyledLayer for a Style and a Layer.
     *
     * @param styleId The style identifier
     * @param layerId The layer identifier
     * @return The extraInfo
     * @throws TargetNotFoundException If the styleId or the layerId does not exist.
     */
    String getExtraInfoForStyleAndLayer(final Integer styleId, final Integer layerId) throws ConstellationException;

    /**
     * Set the extraInfo in StyledLayer for a Style and a Layer.
     *
     * @param styleId The style identifier
     * @param layerId The layer identifier
     * @throws TargetNotFoundException If the styleId or the layerId does not exist.
     */
    void addExtraInfoForStyleAndLayer(final Integer styleId, final Integer layerId, final String extraInfo) throws TargetNotFoundException;

}
