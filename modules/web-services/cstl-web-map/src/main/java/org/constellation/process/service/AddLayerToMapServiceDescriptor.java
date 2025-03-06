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
package org.constellation.process.service;

import java.util.Arrays;
import java.util.Collections;
import static org.constellation.api.CommonConstants.WXS;
import org.constellation.dto.service.config.wxs.GetFeatureInfoCfg;
import org.constellation.dto.service.config.wxs.LayerConfig;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.process.AbstractCstlProcessDescriptor;
import org.constellation.process.ExamindProcessFactory;
import org.opengis.filter.Filter;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.InternationalString;

import org.constellation.dto.StyleReference;
import org.constellation.dto.process.DataProcessReference;
import org.constellation.dto.process.ServiceProcessReference;
import org.geotoolkit.utility.parameter.ExtendedParameterDescriptor;

/**
 * Add a layer to a map service. If service instance doesn't exist, process will create it.
 * Inputs :
 * - layer_reference : reference to data
 * - layer_alias : alias used for this layer in service
 * - layer_style_reference : reference to style used by this layer
 * - layer_filter : CQL filter for the layer
 * - service_type : service type where layer will be published like (WMS, WFS, WMTS, WCS)
 * - service_instance : service instance name where layer will be published.
 *
 * Outputs :
 * - layer_context : result service configuration.
 *
 * @author Quentin Boileau (Geomatys)
 * @author Cédric Briançon (Geomatys)
 */
public class AddLayerToMapServiceDescriptor extends AbstractCstlProcessDescriptor {

    /*
     * Bundle path and keys
     */
    private static final String ADD_SFLAYER_ABSTRACT_KEY            = "service.add_layer_Abstract";
    private static final String LAYER_REF_PARAM_REMARKS_KEY         = "service.add_layer.layerReference";
    private static final String LAYER_ALIAS_PARAM_REMARKS_KEY       = "service.add_layer.layerAlias";
    private static final String LAYER_STYLE_PARAM_REMARKS_KEY       = "service.add_layer.layerStyleRef";
    private static final String LAYER_FILTER_PARAM_REMARKS_KEY      = "service.add_layer.layerFilter";
    private static final String LAYER_DIMENSION_PARAM_REMARKS_KEY   = "service.add_layer.layerDimension";
    private static final String LAYER_DIMENSION_NAME_PARAM_REMARKS_KEY   = "service.add_layer.layerDimension.name";
    private static final String LAYER_DIMENSION_COLUMN_PARAM_REMARKS_KEY = "service.add_layer.layerDimension.column";
    private static final String LAYER_CUSTOM_GFI_PARAM_REMARKS_KEY  = "service.add_layer.featureInfos";
    private static final String SERVICE_INSTANCE_PARAM_REMARKS_KEY  = "service.add_layer.serviceInstance";
    private static final String OUT_LAYER_PARAM_REMARKS_KEY     = "service.add_layer.outLayerContext";

    /*
     * Name and description
     */
    public static final String NAME = "service.add_layer";
    public static final InternationalString ABSTRACT = new BundleInternationalString(ADD_SFLAYER_ABSTRACT_KEY);

    /*
     * Layer reference
     */
    public static final String LAYER_REF_PARAM_NAME = "layer_reference";
    public static final InternationalString LAYER_REF_PARAM_REMARKS = new BundleInternationalString(LAYER_REF_PARAM_REMARKS_KEY);
    public static final ParameterDescriptor<DataProcessReference> LAYER_REF = BUILDER
            .addName(LAYER_REF_PARAM_NAME)
            .setRemarks(LAYER_REF_PARAM_REMARKS)
            .setRequired(true)
            .create(DataProcessReference.class, null);

    /*
     * Layer alias
     */
    public static final String LAYER_ALIAS_PARAM_NAME = "layer_alias";
    public static final InternationalString LAYER_ALIAS_PARAM_REMARKS = new BundleInternationalString(LAYER_ALIAS_PARAM_REMARKS_KEY);
    public static final ParameterDescriptor<String> LAYER_ALIAS = BUILDER
            .addName(LAYER_ALIAS_PARAM_NAME)
            .setRemarks(LAYER_ALIAS_PARAM_REMARKS)
            .setRequired(false)
            .create(String.class, null);

    /*
     * Layer Style
     */
    public static final String LAYER_STYLE_PARAM_NAME = "layer_style_reference";
    public static final InternationalString LAYER_STYLE_PARAM_REMARKS = new BundleInternationalString(LAYER_STYLE_PARAM_REMARKS_KEY);
    public static final ParameterDescriptor<StyleReference> LAYER_STYLE = BUILDER
            .addName(LAYER_STYLE_PARAM_NAME)
            .setRemarks(LAYER_STYLE_PARAM_REMARKS)
            .setRequired(false)
            .create(StyleReference.class, null);

    /*
     * Layer filter
     */
    public static final String LAYER_FILTER_PARAM_NAME = "layer_filter";
    public static final InternationalString LAYER_FILTER_PARAM_REMARKS = new BundleInternationalString(LAYER_FILTER_PARAM_REMARKS_KEY);
    public static final ParameterDescriptor<Filter> LAYER_FILTER = BUILDER
            .addName(LAYER_FILTER_PARAM_NAME)
            .setRemarks(LAYER_FILTER_PARAM_REMARKS)
            .setRequired(false)
            .create(Filter.class, null);
    
    public static final String LAYER_DIMENSION_NAME_PARAM_NAME = "layer_dimension_name";
    public static final InternationalString LAYER_DIMENSION_NAME_PARAM_REMARKS = new BundleInternationalString(LAYER_DIMENSION_NAME_PARAM_REMARKS_KEY);
    
    public static final ParameterDescriptor<String> LAYER_DIMENSION_NAME = BUILDER
            .addName(LAYER_DIMENSION_NAME_PARAM_NAME)
            .setRemarks(LAYER_DIMENSION_NAME_PARAM_REMARKS)
            .setRequired(false)
            .create(String.class, null);
    
    public static final String LAYER_DIMENSION_COLUMN_PARAM_NAME = "layer_dimension_column";
    public static final InternationalString LAYER_DIMENSION_COLUMN_PARAM_REMARKS = new BundleInternationalString(LAYER_DIMENSION_COLUMN_PARAM_REMARKS_KEY);
    
    public static final ParameterDescriptor<String> LAYER_DIMENSION_COLUMN = BUILDER
            .addName(LAYER_DIMENSION_COLUMN_PARAM_NAME)
            .setRemarks(LAYER_DIMENSION_COLUMN_PARAM_REMARKS)
            .setRequired(false)
            .create(String.class, null);

    public static final String LAYER_DIMENSION_PARAM_NAME = "layer_dimension";
    public static final InternationalString LAYER_DIMENSION_PARAM_REMARKS = new BundleInternationalString(LAYER_DIMENSION_PARAM_REMARKS_KEY);
    public static final ParameterDescriptorGroup LAYER_DIMENSION = BUILDER.addName(LAYER_DIMENSION_PARAM_NAME).setRequired(false)
             .createGroup(LAYER_DIMENSION_NAME, LAYER_DIMENSION_COLUMN);

    /*
     * Custom GetFeatureInfo
     */
    public static final String LAYER_CUSTOM_GFI_PARAM_NAME = "layer_feature_infos";
    public static final InternationalString LAYER_CUSTOM_GFI_PARAM_REMARKS = new BundleInternationalString(LAYER_CUSTOM_GFI_PARAM_REMARKS_KEY);
    public static final ParameterDescriptor<GetFeatureInfoCfg[]> LAYER_CUSTOM_GFI = BUILDER
            .addName(LAYER_CUSTOM_GFI_PARAM_NAME)
            .setRemarks(LAYER_CUSTOM_GFI_PARAM_REMARKS)
            .setRequired(false)
            .create(GetFeatureInfoCfg[].class, null);

    /*
     * Service instance
     */
    public static final String SERVICE_INSTANCE_PARAM_NAME = "service_instance";
    public static final InternationalString SERVICE_INSTANCE_PARAM_REMARKS = new BundleInternationalString(SERVICE_INSTANCE_PARAM_REMARKS_KEY);
    public static final ParameterDescriptor<ServiceProcessReference> SERVICE_INSTANCE = 
    new ExtendedParameterDescriptor<>(
                SERVICE_INSTANCE_PARAM_NAME, SERVICE_INSTANCE_PARAM_REMARKS, 1, 1, ServiceProcessReference.class, null, null, Collections.singletonMap("filter", Collections.singletonMap("type", WXS)));


     /**Input parameters */
    public static final ParameterDescriptorGroup INPUT_DESC = BUILDER.addName("InputParameters").setRequired(true)
             .createGroup(LAYER_REF, SERVICE_INSTANCE, LAYER_ALIAS, LAYER_STYLE, LAYER_FILTER,
                     LAYER_DIMENSION, LAYER_CUSTOM_GFI);

    /*
     * Output Layer context
     */
    public static final String OUT_LAYER_PARAM_NAME = "layer";
    public static final InternationalString OUT_LAYER_PARAM_REMARKS = new BundleInternationalString(OUT_LAYER_PARAM_REMARKS_KEY);
    public static final ParameterDescriptor<LayerConfig> OUT_LAYER = BUILDER
            .addName(OUT_LAYER_PARAM_NAME)
            .setRemarks(OUT_LAYER_PARAM_REMARKS)
            .setRequired(true)
            .create(LayerConfig.class, null);

    /**Output parameters */
    public static final ParameterDescriptorGroup OUTPUT_DESC =  BUILDER.addName("OutputParameters").setRequired(true)
            .createGroup(OUT_LAYER);


    public AddLayerToMapServiceDescriptor() {
        super(NAME, ExamindProcessFactory.IDENTIFICATION, ABSTRACT, INPUT_DESC, OUTPUT_DESC);
    }

    public static final AddLayerToMapServiceDescriptor INSTANCE = new AddLayerToMapServiceDescriptor();

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractCstlProcess buildProcess(final ParameterValueGroup input) {
        return new AddLayerToMapService(this, input);
    }

}
