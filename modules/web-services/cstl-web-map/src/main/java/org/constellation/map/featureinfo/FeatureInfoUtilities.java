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
package org.constellation.map.featureinfo;

import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javax.imageio.spi.ServiceRegistry;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.DisjointExtentException;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.image.PixelIterator;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.ArgumentChecks;
import org.constellation.dto.service.config.wxs.GetFeatureInfoCfg;
import org.constellation.dto.service.config.wxs.Layer;
import org.constellation.dto.service.config.wxs.LayerContext;
import org.constellation.exception.ConfigurationException;
import org.geotoolkit.display2d.canvas.RenderingContext2D;
import org.geotoolkit.display2d.primitive.ProjectedCoverage;
import org.geotoolkit.display2d.primitive.SearchAreaJ2D;
import org.geotoolkit.lang.Static;
import org.geotoolkit.map.CoverageMapLayer;
import org.geotoolkit.storage.coverage.CoverageResource;
import org.opengis.geometry.Envelope;

/**
 * Set of utilities methods for FeatureInfoFormat and GetFeatureInfoCfg manipulation.
 *
 * @author Quentin Boileau (Geomatys)
 */
public final class FeatureInfoUtilities extends Static {

    /**
     * Get all declared in resources/META-INF/service/org.constellation.map.featureinfo.FeatureInfoFormat file
     * FeatureInfoFormat.
     * @return an array of FeatureInfoFormat instances.
     */
    public static FeatureInfoFormat[] getAllFeatureInfoFormat() {

        final Set<FeatureInfoFormat> infoFormats = new HashSet<>();
        final Iterator<FeatureInfoFormat> ite = ServiceRegistry.lookupProviders(FeatureInfoFormat.class);
        while (ite.hasNext()) {
            infoFormats.add(ite.next());
        }
        return infoFormats.toArray(new FeatureInfoFormat[infoFormats.size()]);
    }

    /**
     * Search a specific instance of {@link FeatureInfoFormat} in layer (if not null) then in service configuration using
     * mimeType.
     *
     * @param serviceConf service configuration (can't be null)
     * @param layerConf layer configuration. Can be null. If not null, search in layer configuration first.
     * @param mimeType searched mimeType (can't be null)
     * @return found FeatureInfoFormat of <code>null</code> if not found.
     * @throws ClassNotFoundException if a {@link GetFeatureInfoCfg} binding class is not in classpath
     * @throws ConfigurationException if binding class is not an {@link FeatureInfoFormat} instance
     * or declared {@link GetFeatureInfoCfg} MimeType is not supported by the {@link FeatureInfoFormat} implementation.
     */
    public static FeatureInfoFormat getFeatureInfoFormat (final LayerContext serviceConf, final Layer layerConf, final String mimeType)
            throws ClassNotFoundException, ConfigurationException {

        ArgumentChecks.ensureNonNull("serviceConf", serviceConf);
        ArgumentChecks.ensureNonNull("mimeType", mimeType);

        FeatureInfoFormat featureInfo = null;

        if (layerConf != null) {
            final List<GetFeatureInfoCfg> infos = layerConf.getGetFeatureInfoCfgs();
            if (infos != null && infos.size() > 0) {
                for (GetFeatureInfoCfg infoCfg : infos) {
                    if (infoCfg.getMimeType().equals(mimeType)) {
                        featureInfo = FeatureInfoUtilities.getFeatureInfoFormatFromConf(infoCfg);
                    } else if (infoCfg.getMimeType() == null || infoCfg.getMimeType().isEmpty()) {

                        //Find supported mimetypes in FeatureInfoFormat
                        final FeatureInfoFormat tmpFormat = FeatureInfoUtilities.getFeatureInfoFormatFromConf(infoCfg);

                        final List<String> supportedMime = tmpFormat.getSupportedMimeTypes();
                        if (!(supportedMime.isEmpty()) && supportedMime.contains(mimeType)) {
                            featureInfo = tmpFormat;
                        }
                    }
                }
            }
        }

        //try generics
        if (featureInfo == null) {
            final Set<GetFeatureInfoCfg> generics = FeatureInfoUtilities.getGenericFeatureInfos(serviceConf);
            for (GetFeatureInfoCfg infoCfg : generics) {
                if (infoCfg.getMimeType().equals(mimeType)) {
                    featureInfo = FeatureInfoUtilities.getFeatureInfoFormatFromConf(infoCfg);
                }
            }
        }

        return featureInfo;

    }

    /**
     * Find {@link FeatureInfoFormat} from a given {@link GetFeatureInfoCfg}.
     * Also check if {@link GetFeatureInfoCfg} mimeType is supported by {@link FeatureInfoFormat} found.
     *
     * @param infoConf {@link GetFeatureInfoCfg} input
     * @return a {@link FeatureInfoFormat} or null if not found
     * @throws ClassNotFoundException if a {@link GetFeatureInfoCfg} binding class is not in classpath
     * @throws ConfigurationException if binding class is not an {@link FeatureInfoFormat} instance
     * or declared {@link GetFeatureInfoCfg} MimeType is not supported by the {@link FeatureInfoFormat} implementation.
     */
    public static FeatureInfoFormat getFeatureInfoFormatFromConf (final GetFeatureInfoCfg infoConf) throws ClassNotFoundException, ConfigurationException {
        final String mime = infoConf.getMimeType();
        final String binding = infoConf.getBinding();

        final FeatureInfoFormat featureInfo = getFeatureInfoFormatFromBinding(binding);
        if (featureInfo != null) {
            featureInfo.setConfiguration(infoConf);//give his configuration

            if (mime == null || mime.isEmpty()) {
                return featureInfo; // empty config mime type -> no need to check
            } else {
                if (featureInfo.getSupportedMimeTypes().contains(mime)) {
                    return featureInfo;
                } else {
                    throw new ConfigurationException("MimeType "+mime+" not supported by FeatureInfo "+binding+
                            ". Supported output MimeTypes are "+ featureInfo.getSupportedMimeTypes());
                }
            }
        }
        return null;
    }

    /**
     *  Find {@link FeatureInfoFormat} from a given canonical class name.
     *
     * @param binding canonical class name String
     * @return {@link FeatureInfoFormat} or null if binding class is not an instance of {@link FeatureInfoFormat}.
     * @throws ClassNotFoundException if binding class is not an {@link FeatureInfoFormat} instance
     */
    private static FeatureInfoFormat getFeatureInfoFormatFromBinding (final String binding) throws ClassNotFoundException {
        ArgumentChecks.ensureNonNull("binding", binding);

        final Class clazz = Class.forName(binding);
        final FeatureInfoFormat[] FIs = getAllFeatureInfoFormat();

        for (FeatureInfoFormat fi : FIs) {
            if (clazz.isInstance(fi)) {
                return fi;
            }
        }
        return null;
    }

    /**
     * Check {@link GetFeatureInfoCfg} configuration in {@link LayerContext}.
     *
     * @param config service configuration
     * @throws ConfigurationException if binding class is not an {@link FeatureInfoFormat} instance
     * or declared MimeType is not supported by the {@link FeatureInfoFormat} implementation.
     * @throws ClassNotFoundException if binding class is not in classpath
     * @throws ConfigurationException
     */
    public static void checkConfiguration(final LayerContext config) throws ConfigurationException, ClassNotFoundException {
        if (config != null) {
            final Set<GetFeatureInfoCfg> generics = getGenericFeatureInfos(config);

            FeatureInfoFormat featureinfo;
            for (final GetFeatureInfoCfg infoConf : generics) {
                featureinfo = getFeatureInfoFormatFromConf(infoConf);
                if (featureinfo == null) {
                    throw new ConfigurationException("Unknown generic FeatureInfo configuration binding "+infoConf.getBinding());
                }
            }

            /*for (Source source : config.getLayers()) {
                if (source != null) {
                    for (Layer layer : source.getInclude()) {
                        if (layer != null && layer.getGetFeatureInfoCfgs() != null) {
                            for (GetFeatureInfoCfg infoConf : layer.getGetFeatureInfoCfgs()) {
                                featureinfo = getFeatureInfoFormatFromConf(infoConf);
                                if (featureinfo == null) {
                                    throw new ConfigurationException("Unknown FeatureInfo configuration binding "+infoConf.getBinding()+
                                    " for layer "+layer.getName().getLocalPart());
                                }
                            }
                        }
                    }
                }
            }*/
        }
    }

    /**
     * Get all configured mimeTypes from a service {@link LayerContext}.
     * @param config service configuration
     * @return a Set of all MimeType from generic list and from layers config without duplicates.
     */
    public static Set<String> allSupportedMimeTypes (final LayerContext config) throws ConfigurationException, ClassNotFoundException {
        final Set<String> mimes = new HashSet<>();
        if (config != null) {
            final Set<GetFeatureInfoCfg> generics = getGenericFeatureInfos(config);
            for (GetFeatureInfoCfg infoConf : generics) {
                if (infoConf.getMimeType() != null && infoConf.getBinding() != null) {
                    mimes.add(infoConf.getMimeType());
                } else {
                    throw new ConfigurationException("Binding or MimeType not define for GetFeatureInfoCfg "+infoConf);
                }
            }

            /*for (Source source : config.getLayers()) {
                if (source != null) {
                    for (Layer layer : source.getInclude()) {
                        if (layer != null && layer.getGetFeatureInfoCfgs() != null) {
                            for (GetFeatureInfoCfg infoConf : layer.getGetFeatureInfoCfgs()) {

                                if (infoConf.getMimeType() == null || infoConf.getMimeType().isEmpty()) {
                                    //Empty mimeType -> Find supported mimetypes in format
                                    final FeatureInfoFormat tmpFormat = FeatureInfoUtilities.getFeatureInfoFormatFromConf(infoConf);
                                    tmpFormat.setConfiguration(infoConf); //give his configuration
                                    final List<String> supportedMime = tmpFormat.getSupportedMimeTypes();
                                    mimes.addAll(supportedMime);
                                } else {
                                    mimes.add(infoConf.getMimeType());
                                }
                            }
                        }
                    }
                }
            }*/
        }
        return mimes;
    }

    /**
     * Extract generic {@link GetFeatureInfoCfg} configurations from {@link LayerContext} base.
     *
     * @param config service configuration
     * @return a Set of GetFeatureInfoCfg
     */
    public static Set<GetFeatureInfoCfg> getGenericFeatureInfos (final LayerContext config) {
        final Set<GetFeatureInfoCfg> fis = new HashSet<>();
        if (config != null) {
            final List<GetFeatureInfoCfg> globalFI = config.getGetFeatureInfoCfgs();
            if (globalFI != null && !(globalFI.isEmpty())) {
                for (GetFeatureInfoCfg infoConf : globalFI) {
                    fis.add(infoConf);
                }
            }
        }
        return fis;
    }

    /**
     * Create the default {@link GetFeatureInfoCfg} list to configure a LayerContext.
     * This list is build from generic {@link FeatureInfoFormat} and there supported mimetype.
     * HTMLFeatureInfoFormat, CSVFeatureInfoFormat, GMLFeatureInfoFormat
     *
     * @return a list of {@link GetFeatureInfoCfg}
     */
    public static List<GetFeatureInfoCfg> createGenericConfiguration () {
        //Default featureInfo configuration
        final List<GetFeatureInfoCfg> featureInfos = new ArrayList<>();

        //HTML
        FeatureInfoFormat infoFormat = new HTMLFeatureInfoFormat();
        for (String mime : infoFormat.getSupportedMimeTypes()) {
            featureInfos.add(new GetFeatureInfoCfg(mime, infoFormat.getClass().getCanonicalName()));
        }

        //CSV
        infoFormat = new CSVFeatureInfoFormat();
        for (String mime : infoFormat.getSupportedMimeTypes()) {
            featureInfos.add(new GetFeatureInfoCfg(mime, infoFormat.getClass().getCanonicalName()));
        }

        //GML
        infoFormat = new GMLFeatureInfoFormat();
        for (String mime : infoFormat.getSupportedMimeTypes()) {
            featureInfos.add(new GetFeatureInfoCfg(mime, infoFormat.getClass().getCanonicalName()));
        }

        //XML
        infoFormat = new XMLFeatureInfoFormat();
        for (String mime : infoFormat.getSupportedMimeTypes()) {
            featureInfos.add(new GetFeatureInfoCfg(mime, infoFormat.getClass().getCanonicalName()));
        }
        return featureInfos;
    }


    /**
     * Returns the data values of the given coverage, or {@code null} if the
     * values can not be obtained.
     *
     * @return list : each entry contain a gridsampledimension and value associated.
     */
    public static List<Map.Entry<SampleDimension,Object>> getCoverageValues(final ProjectedCoverage gra,
                                                                            final RenderingContext2D context,
                                                                            final SearchAreaJ2D queryArea){

        final CoverageMapLayer layer = gra.getLayer();
        Envelope objBounds = context.getCanvasObjectiveBounds();

        final CoverageResource ref = layer.getCoverageReference();

        if (ref instanceof org.apache.sis.storage.GridCoverageResource) {
            //create envelope around searched area
            final GeneralEnvelope dp = new GeneralEnvelope(objBounds);
            final Rectangle2D bounds2D = queryArea.getObjectiveShape().getBounds2D();
            dp.setRange(0, bounds2D.getCenterX(), bounds2D.getCenterX());
            dp.setRange(1, bounds2D.getCenterY(), bounds2D.getCenterY());

            try {
                //slice grid geometry on envelope
                final org.apache.sis.storage.GridCoverageResource gr = (org.apache.sis.storage.GridCoverageResource) ref;
                final GridGeometry gridGeom = gr.getGridGeometry().derive().subgrid(dp).build();
                final GridCoverage coverage = gr.read(gridGeom);
                //pick first slice if several are available
                final GridExtent extent = coverage.getGridGeometry().getExtent();
                final long[] low = new long[extent.getDimension()];
                final long[] high = new long[extent.getDimension()];
                for (int i=0;i<low.length;i++) {
                    low[i] = extent.getLow(i);
                    high[i] = (i>1) ? low[i] : extent.getHigh(i);
                }
                final GridExtent subExt = new GridExtent(null, low, high, true);

                //read samples from image
                final RenderedImage img = coverage.render(subExt);
                final int numBands = img.getSampleModel().getNumBands();
                float[] values = new float[numBands];
                final PixelIterator ite = PixelIterator.create(img);
                if (ite.next()) {
                    ite.getPixel(values);
                } else {
                    context.getMonitor().exceptionOccured(new DataStoreException("No pixel in image"), Level.INFO);
                    return null;
                }

                final List<Map.Entry<SampleDimension,Object>> results = new ArrayList<>();
                for (int i=0; i<values.length; i++){
                    final SampleDimension sample = coverage.getSampleDimensions().get(i);
                    results.add(new AbstractMap.SimpleImmutableEntry<SampleDimension, Object>(sample, values[i]));
                }
                return results;
            } catch (DataStoreException | DisjointExtentException ex) {
                context.getMonitor().exceptionOccured(ex, Level.INFO);
                return null;
            }

        } else {
            context.getMonitor().exceptionOccured(new DataStoreException("Resource is not a apache sis coverage"), Level.INFO);
            return null;
        }
    }
}
