/*
 *     Examind Community - An open source and standard compliant SDI
 *     https://community.examind.com/
 *
 *  Copyright 2022 Geomatys.
 *
 *  Licensed under the Apache License, Version 2.0 (    the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/
package com.examind.store.observation;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.apache.sis.feature.AbstractIdentifiedType.NAME_KEY;
import org.apache.sis.feature.DefaultAttributeType;
import org.apache.sis.io.stream.IOUtilities;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import static org.apache.sis.storage.DataStoreProvider.LOCATION;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.StorageConnector;
import org.geotoolkit.data.csv.Bundle;
import org.geotoolkit.observation.AbstractObservationStoreFactory;
import org.geotoolkit.storage.ProviderOnFileSystem;
import org.geotoolkit.util.NamesExt;
import org.geotoolkit.util.StringUtilities;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.AttributeType;
import org.opengis.metadata.Identifier;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class FileParsingObservationStoreFactory extends AbstractObservationStoreFactory implements ProviderOnFileSystem {

    protected static final Logger LOGGER = Logger.getLogger("com.examind.process.sos");

    protected static final ParameterBuilder PARAM_BUILDER = new ParameterBuilder();

    public static final ParameterDescriptor<URI> PATH = PARAM_BUILDER
            .addName(LOCATION)
            .setRequired(true)
            .create(URI.class, null);

    public static final ParameterDescriptor<Character> SEPARATOR = new ParameterBuilder()
            .addName("separator")
            .addName(Bundle.formatInternational(Bundle.Keys.paramSeparatorAlias))
            .setRemarks(Bundle.formatInternational(Bundle.Keys.paramSeparatorRemarks))
            .setRequired(false)
            .create(Character.class, ';');

    public static final ParameterDescriptor<String> MAIN_COLUMN = PARAM_BUILDER
            .addName("main_column")
            .setRequired(true)
            .create(String.class, "");

    public static final ParameterDescriptor<String> DATE_COLUMN = PARAM_BUILDER
            .addName("date_column")
            .setRequired(true)
            .create(String.class, "");

    public static final ParameterDescriptor<String> DATE_FORMAT = PARAM_BUILDER
            .addName("date_format")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> LONGITUDE_COLUMN = PARAM_BUILDER
            .addName("longitude_column")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> LATITUDE_COLUMN = PARAM_BUILDER
            .addName("latitude_column")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> FOI_COLUMN = PARAM_BUILDER
            .addName("feature_of_interest_column")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> OBSERVATION_TYPE = PARAM_BUILDER
            .addName("observation_type")
            .setRequired(false)
            .createEnumerated(String.class, new String[]{"Timeserie", "Trajectory", "Profile"}, null);

    public static final ParameterDescriptor<String> PROCEDURE_ID = PARAM_BUILDER
            .addName("procedure_id")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> PROCEDURE_COLUMN = PARAM_BUILDER
            .addName("procedure_column")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> PROCEDURE_NAME_COLUMN = PARAM_BUILDER
            .addName("procedure_name_column")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> PROCEDURE_DESC_COLUMN = PARAM_BUILDER
            .addName("procedure_desc_column")
            .setRequired(false)
            .create(String.class, null);
    
    public static final ParameterDescriptor<String> PROCEDURE_PROPERTIES_COLUMN = PARAM_BUILDER
            .addName("procedure_props_columns")
            .setRequired(false)
            .create(String.class, null);
    
    public static final ParameterDescriptor<String> PROCEDURE_PROPERTIES_MAP_COLUMN = PARAM_BUILDER
            .addName("procedure_props_map_column")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> PROCEDURE_NAME = PARAM_BUILDER
            .addName("procedure_name")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> PROCEDURE_DESC = PARAM_BUILDER
            .addName("procedure_desc")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> PROCEDURE_REGEX = PARAM_BUILDER
            .addName("procedure_regex")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> Z_COLUMN = PARAM_BUILDER
            .addName("z_column")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> UOM_REGEX = PARAM_BUILDER
            .addName("uom_regex")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> RESULT_COLUMN = PARAM_BUILDER
            .addName("result_column")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> OBS_PROP_COLUMN = PARAM_BUILDER
            .addName("observed_properties_columns")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> OBS_PROP_COLUMN_TYPE = PARAM_BUILDER
            .addName("observed_properties_columns_types")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> OBS_PROP_ID = PARAM_BUILDER
            .addName("observed_properties_id")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> OBS_PROP_NAME_COLUMN = PARAM_BUILDER
            .addName("observed_properties_name_columns")
            .setRequired(false)
            .create(String.class, null);
    
    public static final ParameterDescriptor<String> OBS_PROP_DESC = PARAM_BUILDER
            .addName("observed_properties_desc")
            .setRequired(false)
            .create(String.class, null);
    
    public static final ParameterDescriptor<String> OBS_PROP_DESC_COLUMN = PARAM_BUILDER
            .addName("observed_properties_desc_columns")
            .setRequired(false)
            .create(String.class, null);
    
    public static final ParameterDescriptor<String> OBS_PROP_PROPERTIES_COLUMN = PARAM_BUILDER
            .addName("observed_properties_prop_columns")
            .setRequired(false)
            .create(String.class, null);
    
    public static final ParameterDescriptor<String> OBS_PROP_PROPERTIES_MAP_COLUMN = PARAM_BUILDER
            .addName("observed_properties_prop_map_column")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> OBS_PROP_NAME = PARAM_BUILDER
            .addName("observed_properties_name")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> OBS_PROP_FILTER_COLUMN = PARAM_BUILDER
            .addName("observed_properties_filter_columns")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> OBS_PROP_REGEX = PARAM_BUILDER
            .addName("observed_properties_regex")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> QUALITY_COLUMN = PARAM_BUILDER
            .addName("qualtity_column")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> QUALITY_COLUMN_ID = PARAM_BUILDER
            .addName("qualtity_column_id")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> QUALITY_COLUMN_TYPE = PARAM_BUILDER
            .addName("qualtity_column_type")
            .setRequired(false)
            .create(String.class, null);
    
    public static final ParameterDescriptor<String> PARAMETER_COLUMN = PARAM_BUILDER
            .addName("parameter_column")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> PARAMETER_COLUMN_ID = PARAM_BUILDER
            .addName("parameter_column_id")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> PARAMETER_COLUMN_TYPE = PARAM_BUILDER
            .addName("parameter_column_type")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> TYPE_COLUMN = PARAM_BUILDER
            .addName("type_column")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> UOM_COLUMN = PARAM_BUILDER
            .addName("uom_column")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> UOM_ID = PARAM_BUILDER
            .addName("uom_id")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<Character> CHARQUOTE = PARAM_BUILDER
            .addName("char_quote")
            .setRequired(false)
            .create(Character.class, null);

    public static final ParameterDescriptor<String> FILE_MIME_TYPE = PARAM_BUILDER
            .addName("file_mime_type")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<Boolean> DIRECT_COLUMN_INDEX = PARAM_BUILDER
            .addName("direct_column_index")
            .setRequired(false)
            .create(Boolean.class, false);

    public static final ParameterDescriptor<Boolean> NO_HEADER = PARAM_BUILDER
            .addName("no_header")
            .setRequired(false)
            .create(Boolean.class, false);

    public static final ParameterDescriptor<Boolean> LAX_HEADER = PARAM_BUILDER
            .addName("lax_header")
            .setRequired(false)
            .create(Boolean.class, false);

    public static final ParameterDescriptor<Boolean> COMPUTE_FOI = PARAM_BUILDER
            .addName("compute_foi")
            .setRequired(false)
            .create(Boolean.class, true);

    @Override
    public DataStore open(StorageConnector sc) throws DataStoreException {
        GeneralParameterDescriptor desc;
        try {
            desc = getOpenParameters().descriptor(LOCATION);
        } catch (ParameterNotFoundException e) {
            throw new DataStoreException("Unsupported input");
        }

        if (!(desc instanceof ParameterDescriptor)) {
            throw new DataStoreException("Unsupported input");
        }

        try {
            final Object locationValue = sc.getStorageAs(((ParameterDescriptor)desc).getValueClass());
            final ParameterValueGroup params = getOpenParameters().createValue();
            params.parameter(LOCATION).setValue(locationValue);

            if (canProcess(params)) {
                return open(params);
            }
        } catch(IllegalArgumentException ex) {
            throw new DataStoreException("Unsupported input:" + ex.getMessage());
        }

        throw new DataStoreException("Unsupported input");
    }

    private static ParameterDescriptorGroup parameters(final String name, final int minimumOccurs) {
        final Map<String,Object> properties = new HashMap<>(4);
        properties.put(ParameterDescriptorGroup.NAME_KEY, name);
        properties.put(Identifier.AUTHORITY_KEY, Citations.SIS);
        return new DefaultParameterDescriptorGroup(properties, minimumOccurs, 1);
    }

    protected static final AttributeType<Point> TYPE = new DefaultAttributeType<>(
            Collections.singletonMap(NAME_KEY, NamesExt.create("Point")), Point.class, 1, 1, null);

    protected static final ParameterDescriptorGroup EMPTY_PARAMS = parameters("CalculatePoint", 1);

    protected static final GeometryFactory GF = new GeometryFactory();

    public static Set<String> getMultipleValues(final ParameterValueGroup params, final String descCode) {
        try {
            final ParameterValue<String> paramValues = (ParameterValue<String>) params.parameter(descCode);
            return paramValues.getValue() == null ?
                    new HashSet<>() : new HashSet<>(StringUtilities.toStringList(paramValues.getValue()));
        } catch (ParameterNotFoundException ex) {
            LOGGER.log(Level.FINE, ex.getMessage(), ex);
            return new HashSet<>();
        }
    }

    public static List<String> getMultipleValuesList(final ParameterValueGroup params, final String descCode) {
        try {
            final ParameterValue<String> paramValues = (ParameterValue<String>) params.parameter(descCode);
            return paramValues.getValue() == null ?
                    new ArrayList<>() : StringUtilities.toStringList(paramValues.getValue());
        } catch (ParameterNotFoundException ex) {
            LOGGER.log(Level.FINE, ex.getMessage(), ex);
            return new ArrayList<>();
        }
    }

    @Override
    public Collection<byte[]> getSignature() {
        return Collections.emptyList();
    }

    @Override
    public ProbeResult probeContent(StorageConnector connector) throws DataStoreException {
        final Path path = connector.getStorageAs(Path.class);
        final Collection<String> suffix = getSuffix();
        if (!suffix.isEmpty() && path != null) {
            final String extension = IOUtilities.extension(path).toLowerCase();
            final boolean extValid = suffix.contains(extension);
            if (extValid) {
                switch (extension) {
                    case "tsv" :  return new ProbeResult(true, "text/csv; subtype=\"om\"", null);
                    case "csv" :  return new ProbeResult(true, "text/csv; subtype=\"om\"", null);
                    case "xlsx":  return new ProbeResult(true, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet; subtype=\"om\"", null);
                    case "xls" :  return new ProbeResult(true, "application/vnd.ms-excel; subtype=\"om\"", null);
                    case "dbf" :  return new ProbeResult(true, "application/dbase; subtype=\"om\"", null);
                }
            }
        }
        return ProbeResult.UNSUPPORTED_STORAGE;
    }

}
