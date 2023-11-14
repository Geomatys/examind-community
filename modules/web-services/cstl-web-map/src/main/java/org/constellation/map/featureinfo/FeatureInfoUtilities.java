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

import java.awt.image.RenderedImage;
import java.util.function.IntToDoubleFunction;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridRoundingMode;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.image.PixelIterator;
import org.apache.sis.referencing.util.AxisDirections;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.util.ArgumentChecks;
import org.constellation.dto.service.config.wxs.GetFeatureInfoCfg;
import org.constellation.dto.service.config.wxs.LayerConfig;
import org.constellation.dto.service.config.wxs.LayerContext;
import org.constellation.exception.ConfigurationException;
import org.geotoolkit.display2d.canvas.RenderingContext2D;
import org.geotoolkit.display2d.primitive.SearchAreaJ2D;
import org.geotoolkit.lang.Static;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.*;
import java.util.logging.Level;
import org.apache.sis.coverage.grid.GridOrientation;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.referencing.CRS;
import org.apache.sis.util.Utilities;
import org.opengis.coverage.CannotEvaluateException;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import static org.constellation.map.featureinfo.AbstractFeatureInfoFormat.LOGGER;

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
        final Iterator<FeatureInfoFormat> ite = ServiceLoader.load(FeatureInfoFormat.class).iterator();
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
    public static FeatureInfoFormat getFeatureInfoFormat (final LayerContext serviceConf, final LayerConfig layerConf, final String mimeType)
            throws ClassNotFoundException, ConfigurationException {

        ArgumentChecks.ensureNonNull("serviceConf", serviceConf);
        ArgumentChecks.ensureNonNull("mimeType", mimeType);

        FeatureInfoFormat featureInfo = null;

        if (layerConf != null) {
            final List<GetFeatureInfoCfg> infos = layerConf.getGetFeatureInfoCfgs();
            if (infos != null && !infos.isEmpty()) {
                for (GetFeatureInfoCfg infoCfg : infos) {
                    if (infoCfg.getMimeType().equals(mimeType)) {
                        featureInfo = FeatureInfoUtilities.getFeatureInfoFormatFromConf(infoCfg);
                    } else if (infoCfg.getMimeType() == null || infoCfg.getMimeType().isEmpty()) {

                        //Find supported mimetypes in FeatureInfoFormat
                        final FeatureInfoFormat tmpFormat = FeatureInfoUtilities.getFeatureInfoFormatFromConf(infoCfg);
                        if (tmpFormat != null) {
                            final List<String> supportedMime = tmpFormat.getSupportedMimeTypes();
                            if (!(supportedMime.isEmpty()) && supportedMime.contains(mimeType)) {
                                featureInfo = tmpFormat;
                            }
                        }
                    }
                }
            }
        }

        //try generics assigned to the service
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
    public static Set<String> allSupportedMimeTypes (final LayerContext config) throws ConfigurationException {
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

        //JSON
        infoFormat = new JSONFeatureInfoFormat();
        for (String mime : infoFormat.getSupportedMimeTypes()) {
            featureInfos.add(new GetFeatureInfoCfg(mime, infoFormat.getClass().getCanonicalName()));
        }

        //Coverage Profile
        infoFormat = new CoverageProfileInfoFormat();
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
    public static List<Sample> getCoverageValues(final GridCoverageResource ref,
                                                                            final RenderingContext2D context,
                                                                            final SearchAreaJ2D queryArea) {

        if (ref != null) {
            //create envelope around searched area
            final GeneralEnvelope searchEnv = new GeneralEnvelope(context.getCanvasObjectiveBounds());
            final int xAxis = AxisDirections.indexOfColinear(
                    context.getObjectiveCRS().getCoordinateSystem(),
                    context.getObjectiveCRS2D().getCoordinateSystem()
            );
            final Rectangle2D bounds2D = queryArea.getObjectiveShape().getBounds2D();
            searchEnv.setRange(xAxis, bounds2D.getMinX(), bounds2D.getMaxX());
            searchEnv.setRange(xAxis+1, bounds2D.getMinY(), bounds2D.getMaxY());

            try {
                return getCoverageValues(ref, searchEnv.getMedian());
            } catch (DataStoreException|FactoryException|TransformException ex) {

                context.getMonitor().exceptionOccured(ex, Level.INFO);
                return null;
            }
        } else {
            context.getMonitor().exceptionOccured(new DataStoreException("Resource is not a apache sis coverage"), Level.INFO);
            return null;
        }
    }

    /**
     * TODO : we should allow for interpolation. It would mean to set rounding method to enclosing, and compute average
     * of image values using specified interpolation method.
     * TODO : replace map entries with proper abstraction containing number values.
     *
     * @param datasource Coverage data to extract data from.
     * @param location The point in space and time to extract value from.
     * @return List of all samples present at given location.
     * @throws DataStoreException If we cannot inspect given resource (acquire grid geometry or read pixels)
     * @throws FactoryException If analyzed coverage cannot give back a single point, then we try to manually find a
     * transform between wanted location and data grid. If it fails, this error is thrown.
     * @throws TransformException In case of error while manually projecting input location in target grid.
     */
    static List<Sample> getCoverageValues(final GridCoverageResource datasource, DirectPosition location) throws DataStoreException, FactoryException, TransformException {
        final GridGeometry pointGeom;
        final GridGeometry dsrcGeom = datasource.getGridGeometry();
        if (dsrcGeom.isDefined(GridGeometry.EXTENT)) {
            pointGeom = datasource.getGridGeometry().derive()
                    .rounding(GridRoundingMode.NEAREST)
                    .slice(location)
                    .build();
        } else {
            final CoordinateReferenceSystem pointCrs = location.getCoordinateReferenceSystem();
            if (pointCrs != null && dsrcGeom.isDefined(GridGeometry.CRS)) {
                final CoordinateReferenceSystem dataCrs = dsrcGeom.getCoordinateReferenceSystem();
                final CoordinateReferenceSystem dataCrs2D = CRS.getHorizontalComponent(dataCrs);
                if (!Utilities.equalsApproximately(pointCrs, dataCrs2D)) {
                    final GeneralDirectPosition convertedLoc = new GeneralDirectPosition(dataCrs2D);
                    CRS.findOperation(pointCrs, dataCrs2D, null).getMathTransform().transform(location, convertedLoc);
                    location = convertedLoc;
                }
            }

            Envelope sourceEnv = dsrcGeom.getEnvelope();
            GeneralEnvelope env = new GeneralEnvelope(location, location);
            for (int i = 0; i < env.getDimension(); i++) {
                double s = sourceEnv.getSpan(i) / 100;
                env.setRange(i, env.getMinimum(i) - s, env.getMaximum(i) + s);
            }
            pointGeom = new GridGeometry(new GridExtent(1, 1), env, GridOrientation.HOMOTHETY);
        }

        final GridCoverage cvg = datasource.read(pointGeom).forConvertedValues(true);
        final List<SampleDimension> sampleDimensions = cvg.getSampleDimensions();
        final List<Sample> samples = new ArrayList<>(sampleDimensions.size());
        IntToDoubleFunction sampleValue;
        try {
            final double[] values = cvg.evaluator().apply(location);
            sampleValue = i -> values[i];
        } catch (CannotEvaluateException ex) {
            // Workaround: If there's a problem with grid evaluator (for example: bad wrap-around management),
            // but read coverage is a single cell, then there's no ambiguity. The value is the only available pixel.
            try {
                final RenderedImage rendering = cvg.render(null);
                final PixelIterator pxIt = PixelIterator.create(rendering);
                pxIt.next();
                final double[] pixel = pxIt.getPixel((double[]) null);
                sampleValue = i -> pixel[i];
            } catch (Exception bis) {
                ex.addSuppressed(bis);
                LOGGER.log(Level.FINE, "GetFeatureInfo point evaluation fails: point outside domain", ex);
                sampleValue = idx -> Double.NaN;
            }
        }

        for (int i = 0; i < sampleDimensions.size(); i++) {
            samples.add(new Sample(sampleDimensions.get(i), sampleValue.applyAsDouble(i)));
        }
        return samples;
    }

    // TODO: convert to record once Rest doclet support it
    public static class Sample {
        private final SampleDimension description;
        private final double value;

        public Sample(SampleDimension description, double value) {
            this.description = description;
            this.value = value;
        }

        public SampleDimension description() {
            return description;
        }

        public double value() {
            return value;
        }
    }
}
