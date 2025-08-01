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

import org.constellation.business.ILayerBusiness;
import org.constellation.dto.service.config.wxs.DimensionDefinition;
import org.constellation.dto.service.config.wxs.GetFeatureInfoCfg;
import org.constellation.dto.service.config.wxs.LayerConfig;
import org.constellation.process.AbstractCstlProcess;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.filter.Filter;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.beans.factory.annotation.Autowired;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.sis.parameter.Parameters;
import org.constellation.business.IDataBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.dto.DataBrief;
import org.constellation.dto.StyleReference;
import org.constellation.dto.process.DataProcessReference;
import org.constellation.dto.process.ServiceProcessReference;
import org.constellation.exception.ConstellationException;

import static org.constellation.process.service.AddLayerToMapServiceDescriptor.*;
import org.constellation.map.util.OGCFilterToDTOTransformer;

/**
 * Process that add a new layer layerContext from a webMapService configuration.
 *
 * @author Quentin Boileau (Geomatys)
 * @author Cédric Briançon (Geomatys)
 */
public class AddLayerToMapService extends AbstractCstlProcess {

    @Autowired
    protected ILayerBusiness layerBusiness;

    @Autowired
    protected IDataBusiness dataBusiness;

    @Autowired
    protected IServiceBusiness serviceBusiness;

    AddLayerToMapService(final ProcessDescriptor desc, final ParameterValueGroup input) {
        super(desc, input);
    }

    public AddLayerToMapService (final ServiceProcessReference serviceInstance,
                                 final DataProcessReference layerRef, final String layerAlias,
                                 final StyleReference layerStyleRef, final Filter layerFilter,
                                 final String layerDimensionName, final String layerDimensionColumn, final GetFeatureInfoCfg[] customGFI) {
        this(INSTANCE, toParameters(serviceInstance, layerRef, layerAlias, layerStyleRef, layerFilter, layerDimensionName, layerDimensionColumn, customGFI));
    }

    public AddLayerToMapService (final ServiceProcessReference serviceInstance,
                                 final DataProcessReference layerRef, final StyleReference layerStyleRef) {
        this(INSTANCE, toParameters(serviceInstance, layerRef, null, layerStyleRef, null, null, null, null));
    }

    private static ParameterValueGroup toParameters(final ServiceProcessReference serviceInstance,
                                                    final DataProcessReference layerRef, final String layerAlias,
                                                    final StyleReference layerStyleRef, final Filter layerFilter,
                                                    final String layerDimensionName, final String layerDimensionColumn, final GetFeatureInfoCfg[] customGFI){
        final Parameters params = Parameters.castOrWrap(INSTANCE.getInputDescriptor().createValue());
        params.getOrCreate(LAYER_REF).setValue(layerRef);
        params.getOrCreate(LAYER_ALIAS).setValue(layerAlias);
        params.getOrCreate(LAYER_STYLE).setValue(layerStyleRef);
        params.getOrCreate(LAYER_FILTER).setValue(layerFilter);
        params.getOrCreate(LAYER_CUSTOM_GFI).setValue(customGFI);
        params.getOrCreate(SERVICE_INSTANCE).setValue(serviceInstance);
        if (layerDimensionName != null && layerDimensionColumn != null) {
            Parameters dimGrp = Parameters.castOrWrap(params.addGroup(LAYER_DIMENSION_PARAM_NAME));
            dimGrp.getOrCreate(LAYER_DIMENSION_NAME).setValue(layerDimensionName);
            dimGrp.getOrCreate(LAYER_DIMENSION_COLUMN).setValue(layerDimensionColumn);
        }
        return params;
    }

    @Override
    protected void execute() throws ProcessException {

        final DataProcessReference layerRef = inputParameters.getValue(LAYER_REF);
        final String layerAlias             = inputParameters.getValue(LAYER_ALIAS);
        final StyleReference layerStyleRef  = inputParameters.getValue(LAYER_STYLE);
        final Object layerFilter            = inputParameters.getValue(LAYER_FILTER);
        final GetFeatureInfoCfg[] customGFI = inputParameters.getValue(LAYER_CUSTOM_GFI);
        final ServiceProcessReference serviceInstance = inputParameters.getValue(SERVICE_INSTANCE);
        List<ParameterValueGroup> dimensions = inputParameters.groups(LAYER_DIMENSION.getName().toString());
        //test alias
        if (layerAlias != null && layerAlias.isEmpty()) {
            throw new ProcessException("Layer alias can't be empty string.", this, null);
        }

        //extract provider identifier and layer name
        final Integer providerID = layerRef.getProvider();
        //final Date dataVersion = layerRef.getDataVersion(); no longer used
        final String namespace = layerRef.getNamespace();
        final String name = layerRef.getName();
        final QName layerQName = new QName(namespace, name);

        //create future new layer
        final LayerConfig newLayer = new LayerConfig(layerQName);

        //add filter if exist
        if (layerFilter != null) {
            // TODO converter Geotk Filter => DTO filter
            if (layerFilter instanceof Filter) {
                final Filter filterType = (Filter) layerFilter;
                newLayer.setFilter(new OGCFilterToDTOTransformer().visit(filterType));
            } else if (layerFilter instanceof org.constellation.dto.Filter) {
                newLayer.setFilter((org.constellation.dto.Filter)layerFilter);
            }
        }


        //add extra dimension
        for (ParameterValueGroup dimensionGrp : dimensions) {
            Parameters dimGrp = Parameters.castOrWrap(dimensionGrp);
            String layerDimensionName = dimGrp.getValue(LAYER_DIMENSION_NAME);
            String layerDimensionCol  = dimGrp.getValue(LAYER_DIMENSION_COLUMN);
            final DimensionDefinition dimensionDef = new DimensionDefinition();
            dimensionDef.setCrs(layerDimensionName);
            dimensionDef.setLower(layerDimensionCol);
            dimensionDef.setUpper(layerDimensionCol);
            newLayer.getDimensions().add(dimensionDef);
        }

        //add style if exist
        if (layerStyleRef != null) {
            final List<StyleReference> styles = new ArrayList<>();
            styles.add(layerStyleRef);
            newLayer.setStyles(styles);
        }

        //add alias if exist
        if (layerAlias != null) {
            newLayer.setAlias(layerAlias);
        }

        /*forward data version if defined.
        if (dataVersion != null) {
            newLayer.setVersion(dataVersion.getTime());
        }*/

        //custom GetFeatureInfo
        if (customGFI != null) {
            newLayer.setGetFeatureInfoCfgs(Arrays.asList(customGFI));
        }

        try {
            Integer sid = serviceInstance.getId();
            if (sid != null) {
                sid = serviceBusiness.getServiceIdByIdentifierAndType(serviceInstance.getType(), serviceInstance.getName());
            }
            if (sid != null) {
                DataBrief db = dataBusiness.getDataBrief(layerQName, providerID, false, false);
                if (db != null) {
                    layerBusiness.add(db.getId(), layerAlias, namespace, name, null, sid, newLayer);
                }
            }
        } catch (ConstellationException ex) {
            throw new ProcessException("Error while saving layer", this, ex);
        }

        //output
        outputParameters.getOrCreate(OUT_LAYER).setValue(newLayer);

    }
}
