/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2020 Geomatys.
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
package org.constellation.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.awt.Dimension;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.xml.namespace.QName;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.WritableAggregate;
import org.apache.sis.util.logging.Logging;
import org.constellation.business.IDataBusiness;
import org.constellation.business.IMapContextBusiness;
import org.constellation.business.IProcessBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IPyramidBusiness;
import org.constellation.business.IStyleBusiness;
import org.constellation.dto.DataBrief;
import org.constellation.dto.MapContextLayersDTO;
import org.constellation.dto.MapContextStyledLayerDTO;
import org.constellation.dto.ProviderData;
import org.constellation.dto.StyleBrief;
import org.constellation.dto.process.TaskParameter;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.provider.Data;
import org.constellation.provider.DataProvider;
import org.constellation.provider.DataProviders;
import org.constellation.provider.GeoData;
import org.constellation.util.ParamUtilities;
import org.constellation.util.Util;
import org.geotoolkit.coverage.grid.ViewType;
import org.geotoolkit.coverage.xmlstore.XMLCoverageResource;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.map.MapContext;
import org.geotoolkit.map.MapItem;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessFinder;
import org.geotoolkit.storage.coverage.DefiningCoverageResource;
import org.geotoolkit.storage.multires.DefiningPyramid;
import org.geotoolkit.storage.multires.Pyramids;
import org.geotoolkit.style.MutableStyle;
import org.geotoolkit.util.NamesExt;
import org.geotoolkit.wms.WMSResource;
import org.geotoolkit.wms.WebMapClient;
import org.geotoolkit.wms.xml.WMSVersion;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;
import org.opengis.util.NoSuchIdentifierException;
import org.springframework.stereotype.Component;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component("pyramidBusiness")
public class PyramidBusiness implements IPyramidBusiness {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.admin");

    protected static final String RENDERED_PREFIX = "rendered_";

    @Inject
    private IProcessBusiness processBusiness;

    @Inject
    protected IDataBusiness dataBusiness;

    @Inject
    protected IStyleBusiness styleBusiness;

    @Inject
    protected IMapContextBusiness mapContextBusiness;

    @Inject
    protected IProviderBusiness providerBusiness;

    @Override
    public ProviderData pyramidDatas(Integer userId, String pyramidDataName, List<Integer> dataIds, final String crs) throws ConstellationException {

        // verify CRS validity
        final CoordinateReferenceSystem coordsys = verifyCrs(crs, true);

        final List<DataBrief> briefs = new ArrayList<>();
        for (Integer dataId : dataIds) {
            briefs.add(dataBusiness.getDataBrief(dataId));
        }

        if (!briefs.isEmpty()) {
            /**
             * 1) calculate best scales array. loop on each data and determine
             * the largest scales that wrap all data.
             */
            final double[] scales = DataProviders.getBestScales(briefs, crs);

            /**
             * 2) creates the styled pyramid that contains all selected layers
             * we need to loop on data and creates a mapcontext to send to
             * pyramid process
             */
            final String tileFormat = "PNG";
            String firstDataProv = null;
            String firstDataName = null;
            GeneralEnvelope globalEnv = null;
            final MapContext context = MapBuilder.createContext();
            for (final DataBrief db : briefs) {
                final String providerIdentifier = db.getProvider();
                final String dataName = db.getName();
                if (firstDataProv == null) {
                    firstDataProv = providerIdentifier;
                    firstDataName = dataName;
                }
                //get data
                final DataProvider inProvider;
                try {
                    inProvider = DataProviders.getProvider(db.getProviderId());
                } catch (ConfigurationException ex) {
                    LOGGER.log(Level.WARNING, "Provider " + providerIdentifier + " does not exist");
                    continue;
                }

                final Data inD = inProvider.get(NamesExt.create(dataName));
                if (!(inD instanceof GeoData)) {
                    LOGGER.log(Level.WARNING, "Data " + dataName + " does not exist in provider " + providerIdentifier + " (or is not a GeoData)");
                    continue;
                }
                final GeoData inData = (GeoData) inD;

                Envelope dataEnv;
                try {
                    dataEnv = inData.getEnvelope();
                } catch (ConstellationStoreException ex) {
                    LOGGER.log(Level.WARNING, "Failed to extract envelope for data " + dataName);
                    continue;
                }

                //if style is null, a default style will be used in maplayer.
                MutableStyle style = null;
                try {
                    final StyleBrief styleb = db.getFirstStyle();
                    if (styleb != null) {
                        style = (MutableStyle) styleBusiness.getStyle(styleb.getId());
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
                }

                try {
                    context.items().add((MapItem) inData.getMapLayer(style, null));
                } catch (ConstellationStoreException ex) {
                    LOGGER.log(Level.WARNING, "Failed to create map context layer for data " + ex.getMessage(), ex);
                    continue;
                }

                if (coordsys != null) {
                    try {
                        //reproject data envelope
                        dataEnv = Envelopes.transform(dataEnv, coordsys);
                    } catch (TransformException ex) {
                        LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
                        throw new ConstellationException("Could not transform data envelope to crs " + crs);
                    }
                }
                if (globalEnv == null) {
                    globalEnv = new GeneralEnvelope(dataEnv);
                } else {
                    globalEnv.add(dataEnv);
                }
            }

            globalEnv.intersect(CRS.getDomainOfValidity(coordsys));

            final String uuid = UUID.randomUUID().toString();
            final String providerId = briefs.size() == 1 ? firstDataProv : uuid;
            context.setName("Styled pyramid " + crs + " for " + providerId + ":" + pyramidDataName);

            XMLCoverageResource outRef;
            String pyramidProviderId = RENDERED_PREFIX + uuid;

            //create the output provider
            final int pojoProviderID;
            final DataProvider outProvider;
            try {
                pojoProviderID = providerBusiness.createPyramidProvider(providerId, pyramidProviderId);
                outProvider = DataProviders.getProvider(pojoProviderID);

                // Update the parent attribute of the created provider
                if (briefs.size() == 1) {
                    providerBusiness.updateParent(outProvider.getId(), providerId);
                }
            } catch (Exception ex) {
                DataProviders.LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                throw new ConstellationException("Failed to create pyramid provider " + ex.getMessage());
            }

            try {
                //create the output pyramid coverage reference
                DataStore pyramidStore = outProvider.getMainStore();
                outRef = (XMLCoverageResource) ((WritableAggregate) pyramidStore).add(new DefiningCoverageResource(NamesExt.create(pyramidDataName)));
                outRef.setPackMode(ViewType.RENDERED);
                ((XMLCoverageResource) outRef).setPreferredFormat(tileFormat);
                //this produces an update event which will create the DataRecord
                outProvider.reload();

                pyramidStore = outProvider.getMainStore();
                Optional<GenericName> optIdentifier = outRef.getIdentifier();
                if (optIdentifier.isPresent()) {
                    outRef = (XMLCoverageResource) pyramidStore.findResource(optIdentifier.get().toString());
                }
                //create database data object
                providerBusiness.createOrUpdateData(pojoProviderID, null, false);

                // Get the new data created
                Optional<GenericName> optOutId = outRef.getIdentifier();
                if (optOutId.isPresent()) {
                    GenericName outID = optOutId.get();
                    final QName outDataQName = new QName(NamesExt.getNamespace(outID), outID.tip().toString());
                    final Integer dataId = dataBusiness.getDataId(outDataQName, pojoProviderID);

                    //set data as RENDERED
                    dataBusiness.updateDataRendered(dataId, true);

                    //set hidden value to true for the pyramid styled map
                    dataBusiness.updateDataHidden(dataId, true);

                    //link pyramid data to original data
                    for (final DataBrief db : briefs) {
                        dataBusiness.linkDataToData(db.getId(), dataId);
                    }
                }
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                throw new ConstellationException("Failed to create pyramid layer " + ex.getMessage());
            }

            try {
                //insert a mapcontext for this pyramid of data
                final MapContextLayersDTO mapContext = new MapContextLayersDTO();
                mapContext.setOwner(userId);
                mapContext.setCrs(crs);
                mapContext.setKeywords("");
                mapContext.setWest(globalEnv.getLower(0));
                mapContext.setSouth(globalEnv.getLower(1));
                mapContext.setEast(globalEnv.getUpper(0));
                mapContext.setNorth(globalEnv.getUpper(1));
                mapContext.setName(pyramidDataName + " (wmts context)");
                final Integer mapcontextId = mapContextBusiness.create(mapContext);
                final List<MapContextStyledLayerDTO> mapcontextlayers = new ArrayList<>();
                for (final DataBrief db : briefs) {
                    final MapContextStyledLayerDTO mcStyledLayer = new MapContextStyledLayerDTO();
                    mcStyledLayer.setDataId(db.getId());
                    final List<StyleBrief> styles = db.getTargetStyle();
                    if (styles != null && !styles.isEmpty()) {
                        final String styleName = styles.get(0).getName();
                        mcStyledLayer.setExternalStyle(styleName);
                        mcStyledLayer.setStyleId(styles.get(0).getId());
                    }
                    mcStyledLayer.setIswms(false);
                    mcStyledLayer.setLayerId(null);
                    mcStyledLayer.setOpacity(100);
                    mcStyledLayer.setOrder(briefs.indexOf(db));
                    mcStyledLayer.setVisible(true);
                    mcStyledLayer.setMapcontextId(mapcontextId);
                    mapcontextlayers.add(mcStyledLayer);
                }
                mapContextBusiness.setMapItems(mapcontextId, mapcontextlayers);
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Can not create mapcontext for WMTS layer", ex);
            }

            runTilingProcess(userId, outRef, context, globalEnv, 256, scales);

            return new ProviderData(pyramidProviderId, pyramidDataName);

        } else {
            throw new ConstellationException("The given list of data to pyramid is empty.");
        }
    }

    @Override
    public ProviderData pyramidMapContext(Integer userId, String pyramidDataName, final String crs, final MapContextLayersDTO mc) throws ConstellationException {

        // verify CRS validity
        final CoordinateReferenceSystem crsOutput = verifyCrs(crs, false);
        final CoordinateReferenceSystem crsObj = verifyCrs(mc.getCrs(), false);

        // build output envelope
        final GeneralEnvelope env = new GeneralEnvelope(crsObj);
        env.setRange(0, mc.getWest(), mc.getEast());
        env.setRange(1, mc.getSouth(), mc.getNorth());
        GeneralEnvelope globalEnv;
        try {
            globalEnv = new GeneralEnvelope(Envelopes.transform(env, crsOutput));
        } catch (TransformException ex) {
            globalEnv = null;
        }

        if (globalEnv == null || globalEnv.isEmpty()) {
            globalEnv = new GeneralEnvelope(CRS.getDomainOfValidity(crsOutput));
        }

        if (Util.containsInfinity(globalEnv)) {
            globalEnv.intersect(CRS.getDomainOfValidity(crsOutput));
        }

        // compute scales
        final double geospanX = globalEnv.getSpan(0);
        final int tileSize = 256;
        final double[] scales = new double[8];
        scales[0] = geospanX / tileSize;
        for (int i = 1; i < scales.length; i++) {
            scales[i] = scales[i - 1] / 2.0;
        }

        final String tileFormat = "PNG";
        final MapContext context = MapBuilder.createContext();

        for (final MapContextStyledLayerDTO layer : mc.getLayers()) {

            final String providerIdentifier = layer.getProvider();
            final String dataName = layer.getName();
            if (providerIdentifier == null) {
                URL serviceUrl;
                try {
                    serviceUrl = new URL(layer.getExternalServiceUrl());
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, "An external wms layer in mapcontext have invalid service url! " + layer.getName());
                    continue;
                }
                //it is a wms layer
                final String serviceVersion = layer.getExternalServiceVersion() != null ? layer.getExternalServiceVersion() : "1.3.0";
                final WebMapClient wmsServer = new WebMapClient(serviceUrl, WMSVersion.getVersion(serviceVersion));
                final WMSResource wmsLayer = new WMSResource(wmsServer, dataName);
                context.items().add(MapBuilder.createCoverageLayer(wmsLayer));
                continue;
            }
            //get data
            final DataProvider inProvider;
            try {
                inProvider = DataProviders.getProvider(providerIdentifier);
            } catch (ConfigurationException ex) {
                LOGGER.log(Level.WARNING, "Provider " + providerIdentifier + " does not exist");
                continue;
            }

            final Data inD = inProvider.get(NamesExt.create(dataName));
            if (!(inD instanceof GeoData)) {
                LOGGER.log(Level.WARNING, "Data " + dataName + " does not exist in provider " + providerIdentifier + " (or is not a GeoData)");
                continue;
            }
            final GeoData inData = (GeoData) inD;

            MutableStyle style = null;
            try {
                final List<StyleBrief> styles = layer.getTargetStyle();
                if (styles != null && !styles.isEmpty()) {
                    final String styleName = styles.get(0).getName();
                    style = (MutableStyle) styleBusiness.getStyle("sld", styleName);
                }
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            }

            try {
                //if style is null, a default style will be used in maplayer.
                context.items().add((MapItem) inData.getMapLayer(style, null));
            } catch (ConstellationStoreException ex) {
                LOGGER.log(Level.WARNING, "Failed to create map context item for data " + ex.getMessage(), ex);
            }
        }

        final String uuid = UUID.randomUUID().toString();
        final String providerId = uuid;
        context.setName("Styled pyramid " + crs + " for " + providerId + ":" + pyramidDataName);

        //create the output folder for pyramid
        XMLCoverageResource outRef;
        String pyramidProviderId = RENDERED_PREFIX + uuid;

        //create the output provider
        final DataProvider outProvider;
        final Integer pyramidProvider;
        try {
            pyramidProvider = providerBusiness.createPyramidProvider(providerId, pyramidProviderId);
            outProvider = DataProviders.getProvider(pyramidProvider);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            throw new ConstellationStoreException("Failed to create pyramid provider " + ex.getMessage());
        }

        try {
            //create the output pyramid coverage reference
            DataStore pyramidStore = outProvider.getMainStore();
            outRef = (XMLCoverageResource) ((WritableAggregate) pyramidStore).add(new DefiningCoverageResource(NamesExt.create(pyramidDataName)));
            outRef.setPackMode(ViewType.RENDERED);
            ((XMLCoverageResource) outRef).setPreferredFormat(tileFormat);
            //this produces an update event which will create the DataRecord
            outProvider.reload();

            pyramidStore = outProvider.getMainStore();
            outRef = (XMLCoverageResource) pyramidStore.findResource(String.valueOf(outRef.getIdentifier().orElse(null)));
            //create database data object
            providerBusiness.createOrUpdateData(pyramidProvider, null, false);

            // Get the new data created
            Optional<GenericName> optOutId = outRef.getIdentifier();
            if (optOutId.isPresent()) {
                GenericName outID = optOutId.get();
                final QName outDataQName = new QName(NamesExt.getNamespace(outID), outID.toString());
                final Integer dataId = dataBusiness.getDataId(outDataQName, pyramidProvider);

                //set data as RENDERED
                dataBusiness.updateDataRendered(dataId, true);

                //set hidden value to true for the pyramid styled map
                dataBusiness.updateDataHidden(dataId, true);
            } else {
                throw new ConstellationException("Empty identifier in pyramid resource.");
            }

        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            throw new ConstellationStoreException("Failed to create pyramid layer " + ex.getMessage());
        }

        //prepare the pyramid and mosaics
        final Dimension tileDim = new Dimension(tileSize, tileSize);
        try {
            final DefiningPyramid template = Pyramids.createTemplate(globalEnv, tileDim, scales);
            outRef.createModel(template);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            throw new ConstellationException("Failed to initialize output pyramid. Cause : " + ex.getMessage());
        }

        runTilingProcess(userId, outRef, context, globalEnv, tileSize, scales);

        return new ProviderData(pyramidProviderId, pyramidDataName);
    }

    private void runTilingProcess(Integer userId, XMLCoverageResource outRef, MapContext context, Envelope globalEnv, int tileSize, double[] scales) throws ConstellationException {
        try {
            final Dimension tileDim = new Dimension(tileSize, tileSize);
            final DefiningPyramid template = Pyramids.createTemplate(globalEnv, tileDim, scales);
            outRef.createModel(template);

            final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor("administration", "gen-pyramid");
            final ParameterValueGroup input = desc.getInputDescriptor().createValue();
            input.parameter("mapcontext").setValue(context);
            input.parameter("resource").setValue(outRef);
            input.parameter("mode").setValue("rgb");
            final org.geotoolkit.process.Process p = desc.createProcess(input);

            //add task in scheduler
            TaskParameter taskParameter = new TaskParameter();
            taskParameter.setProcessAuthority(Util.getProcessAuthorityCode(desc));
            taskParameter.setProcessCode(desc.getIdentifier().getCode());
            taskParameter.setDate(System.currentTimeMillis());
            taskParameter.setInputs(ParamUtilities.writeParameterJSON(input));
            taskParameter.setOwner(userId);
            taskParameter.setName(context.getName() + " | " + System.currentTimeMillis());
            taskParameter.setType("INTERNAL");
            Integer newID = processBusiness.addTaskParameter(taskParameter);
            //add task in scheduler
            processBusiness.runProcess("Create " + context.getName(), p, newID, userId);

        } catch (NoSuchIdentifierException | JsonProcessingException | DataStoreException ex) {
            throw new ConstellationException("Error while tiling data", ex);
        }
    }

    private static CoordinateReferenceSystem verifyCrs(String crs, boolean allowNull) throws ConstellationException {
        if (crs != null) {
            try {
                return AbstractCRS.castOrCopy(CRS.forCode(crs)).forConvention(AxesConvention.RIGHT_HANDED);
            } catch (FactoryException ex) {
                throw new ConstellationException("Invalid CRS code : " + crs);
            }
        } else if (!allowNull) {
            throw new ConstellationException("Supplied CRS is null.");
        }
        return null;
    }
}
