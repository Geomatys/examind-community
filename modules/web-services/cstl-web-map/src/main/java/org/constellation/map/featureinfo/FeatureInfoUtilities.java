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

import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridRoundingMode;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.internal.metadata.AxisDirections;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.Resource;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.collection.BackingStoreException;
import org.constellation.dto.service.config.wxs.GetFeatureInfoCfg;
import org.constellation.dto.service.config.wxs.Layer;
import org.constellation.dto.service.config.wxs.LayerContext;
import org.constellation.exception.ConfigurationException;
import org.geotoolkit.display2d.canvas.RenderingContext2D;
import org.geotoolkit.display2d.primitive.ProjectedCoverage;
import org.geotoolkit.display2d.primitive.SearchAreaJ2D;
import org.geotoolkit.lang.Static;
import org.geotoolkit.math.XMath;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

import javax.imageio.spi.ServiceRegistry;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.List;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
                                                                            final SearchAreaJ2D queryArea) {
        final Resource ref = gra.getLayer().getResource();
        if (ref instanceof GridCoverageResource) {
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
                return getCoverageValues((GridCoverageResource)ref, searchEnv.getMedian())
                        .collect(Collectors.toList());
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
     * @return List of all samples present at given location. The list contains a pair whose key is the sample metadata,
     * and the value is GEOPHYSIC value associated to the input point for this sample. Note that transformation of
     * values will only occurred on stream consumption. It means that {@link BackingStoreException} can be thrown,
     * wrapping an underlying {@link TransformException}.
     * @throws DataStoreException If we cannot inspect given resource (acquire grid geometry or read pixels)
     * @throws FactoryException If analyzed coverage cannot give back a single point, then we try to manually find a
     * transform between wanted location and data grid. If it fails, this error is thrown.
     * @throws TransformException In case of error while manually projecting input location in target grid.
     */
    public static Stream<Map.Entry<SampleDimension, Object>> getCoverageValues(final GridCoverageResource datasource, final DirectPosition location) throws DataStoreException, FactoryException, TransformException {
        final GridGeometry pointGeom = datasource.getGridGeometry().derive()
                .rounding(GridRoundingMode.NEAREST)
                .slice(location)
                .build();

        final GridCoverage cvg = datasource.read(pointGeom);
        // TODO: we should check further read coordinates, but it would require more processing, so for now stay simple:
        //  ensure we've got our 1 pixel large image.
        final GridGeometry effectiveGeom = cvg.getGridGeometry();
        final GridExtent effectiveExtent = effectiveGeom.getExtent();
        final boolean isTooLarge = IntStream.range(0, effectiveExtent.getDimension())
                .mapToLong(effectiveExtent::getSize)
                .anyMatch(size -> size > 1);
        final Point pixelCoord = new Point();
        if (isTooLarge) {
            // Ok, what we can do here is project our point in output image to get the precise pixel. It will be costly,
            // but for now I can't think of a better solution.
            final CoordinateReferenceSystem baseCrs = effectiveGeom.getCoordinateReferenceSystem();
            final SingleCRS hCrs = CRS.getHorizontalComponent(baseCrs);
            final int xAxis = AxisDirections.indexOfColinear(baseCrs.getCoordinateSystem(), hCrs.getCoordinateSystem());
            final GridGeometry geom2d = effectiveGeom.reduce(xAxis, xAxis + 1);
            final CoordinateOperation op = CRS.findOperation(location.getCoordinateReferenceSystem(), geom2d.getCoordinateReferenceSystem(), null);
            final MathTransform objective2Grid = MathTransforms.concatenate(op.getMathTransform(), geom2d.getGridToCRS(PixelInCell.CELL_CENTER).inverse());
            final DirectPosition gridPt = objective2Grid.transform(location, null);
            final GridExtent extent2d = geom2d.getExtent();
            pixelCoord.setLocation(clamp(gridPt, extent2d, 0), clamp(gridPt, extent2d, 1));
        }
        final RenderedImage image = cvg.render(effectiveExtent);
        // We checked that we've got a single point image, so we can short-circuit a lot of things here
        final Raster tile = image.getTile(0, 0);
        final int numBands = tile.getNumBands();
        final List<SampleDimension> sampleDims = cvg.getSampleDimensions();
        if (sampleDims == null || sampleDims.size() < numBands) {
            throw new IllegalStateException("Sample dimensions don't match image band number !");
        }

        final double[] pixel = tile.getPixel(pixelCoord.x, pixelCoord.y, new double[numBands]);
        return IntStream.range(0, numBands)
                .mapToObj(i -> {
                   final SampleDimension sd = sampleDims.get(i);
                   final Double geophysicVal = sd.getTransferFunction()
                           .filter(t -> !t.isIdentity())
                           .map(transfer -> uncheck(transfer, pixel[i]))
                           .orElse(pixel[i]);
                    return new AbstractMap.SimpleImmutableEntry<>(sd, geophysicVal);
                });
    }

    /**
     * Just apply given math transform to input value, but wraps potential transformation exception into a
     * {@link BackingStoreException}.
     *
     * @param function The math transform to apply.
     * @param value The value to transform.
     * @return Transformed value.
     * @throws BackingStoreException If any {@link TransformException} happened while applying transformation.
     */
    private static double uncheck(final MathTransform1D function, final double value) throws BackingStoreException {
        try {
            return function.transform(value);
        } catch (TransformException e) {
            throw new BackingStoreException(e);
        }
    }

    private static int clamp(final DirectPosition source, final GridExtent bounds, final int dimension) {
        return (int) XMath.clamp(
                Math.round(source.getOrdinate(dimension)),
                bounds.getLow(dimension),
                bounds.getHigh(dimension)
        );
    }
}
