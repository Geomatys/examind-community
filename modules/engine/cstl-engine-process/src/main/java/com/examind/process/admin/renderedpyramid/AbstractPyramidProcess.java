
package com.examind.process.admin.renderedpyramid;

import com.examind.process.admin.AdminProcessRegistry;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.iso.Names;
import org.apache.sis.util.iso.ResourceInternationalString;
import org.constellation.admin.SpringHelper;
import org.constellation.business.IDataBusiness;
import org.constellation.business.ILayerBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IStyleBusiness;
import org.constellation.dto.DataBrief;
import org.constellation.dto.MapContextLayersDTO;
import org.constellation.dto.MapContextStyledLayerDTO;
import org.constellation.dto.StyleBrief;
import org.constellation.dto.process.DatasetProcessReference;
import org.constellation.dto.process.ServiceProcessReference;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.constellation.provider.DataProvider;
import org.constellation.provider.DataProviders;
import org.constellation.provider.GeoData;
import org.constellation.util.Util;
import org.geotoolkit.data.multires.DefiningPyramid;
import org.geotoolkit.data.multires.Mosaic;
import org.geotoolkit.data.multires.Pyramids;
import org.geotoolkit.display.PortrayalException;
import org.geotoolkit.display.canvas.CanvasUtilities;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.map.MapContext;
import org.geotoolkit.map.MapItem;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.geotoolkit.processing.AbstractProcess;
import org.geotoolkit.processing.AbstractProcessDescriptor;
import org.geotoolkit.style.MutableStyle;
import org.geotoolkit.utility.parameter.ExtendedParameterDescriptor;
import org.geotoolkit.wms.WebMapClient;
import org.geotoolkit.wms.map.WMSMapLayer;
import org.geotoolkit.wms.xml.WMSVersion;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.style.Style;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
@Component
abstract class AbstractPyramidProcess extends AbstractProcessDescriptor {

    public static final String BUNDLE_LOCATION = "com/examind/process/admin/renderedpyramid/bundle";
    protected static final String TEMPLATE_WGS = "WGS:84 (TMS,Cesium)";
    protected static final String TEMPLATE_GM = "Pseudo-Mercator (OSM,Google)";
    protected static final String FORMAT_PNG = "image/png";
    protected static final String FORMAT_JPEG = "image/jpeg";
    protected static final String FORMAT_QMESH = "terrain";

    protected static final ParameterDescriptor<MapContextLayersDTO> MAP_CONTEXT;
    protected static final ParameterDescriptor<DatasetProcessReference> DATASET;
    protected static final ParameterDescriptor<String> TEMPLATE;
    protected static final ParameterDescriptor<Double> ENVELOPE_MINX;
    protected static final ParameterDescriptor<Double> ENVELOPE_MAXX;
    protected static final ParameterDescriptor<Double> ENVELOPE_MINY;
    protected static final ParameterDescriptor<Double> ENVELOPE_MAXY;

    protected static final ParameterDescriptor<ServiceProcessReference> SERVICE_WMS;
    protected static final ParameterDescriptor<ServiceProcessReference> SERVICE_WMTS;
    protected static final ParameterDescriptor<String> DATA_NAME;
    protected static final ParameterDescriptor<Integer> TILING_LEVELS;
    protected static final ParameterDescriptor<Integer> MIN_LEVEL;
    protected static final ParameterDescriptor<Integer> MAX_LEVEL;
    protected static final ParameterDescriptor<String> FORMAT;
    protected static final ParameterDescriptor<String> OUTPUT_FOLDER;
    protected static final ParameterDescriptor<Boolean> NO_EMPTY_TILES;


    static {
        final ParameterBuilder builder = new ParameterBuilder();

        TEMPLATE = builder.addName("template")
                .setRequired(true)
                .setDescription(load("input.crs"))
                .createEnumerated(String.class, new String[]{TEMPLATE_WGS,TEMPLATE_GM},TEMPLATE_WGS);

        MAP_CONTEXT = builder.addName("map-context")
                .setRequired(false)
                .setDescription(load("input.mapcontext"))
                .create(MapContextLayersDTO.class, null);

        DATASET = builder.addName("dataset")
                .setRequired(false)
                .setDescription(load("input.dataset"))
                .create(DatasetProcessReference.class, null);

        FORMAT = builder.addName("format")
                .setRequired(true)
                .setDescription(load("input.format"))
                .createEnumerated(String.class, new String[]{FORMAT_PNG,FORMAT_JPEG,FORMAT_QMESH},FORMAT_PNG);

        OUTPUT_FOLDER = builder.addName("output_folder")
                .setRequired(true)
                .create(String.class, null);

        NO_EMPTY_TILES = builder.addName("no_empty_tiles")
                .setRequired(true)
                .create(Boolean.class, true);

        ENVELOPE_MINX = builder.addName("minx").setRequired(true).setDescription(load("input.minx")).create(Double.class, -180.0);
        ENVELOPE_MAXX = builder.addName("maxx").setRequired(true).setDescription(load("input.maxx")).create(Double.class,  180.0);
        ENVELOPE_MINY = builder.addName("miny").setRequired(true).setDescription(load("input.miny")).create(Double.class, -80.0);
        ENVELOPE_MAXY = builder.addName("maxy").setRequired(true).setDescription(load("input.maxy")).create(Double.class,  80.0);

        SERVICE_WMS = new ExtendedParameterDescriptor<>(
                "serviceWms", load("input.service"), ServiceProcessReference.class, null, false,
                Collections.singletonMap("filter", Collections.singletonMap("type", "wms"))
        );
        SERVICE_WMTS = new ExtendedParameterDescriptor<>(
                "serviceWmts", load("input.service"), ServiceProcessReference.class, null, true,
                Collections.singletonMap("filter", Collections.singletonMap("type", "wmts"))
        );

        DATA_NAME = builder.addName("create-data")
                .setRequired(true)
                .setDescription(load("input.create-data"))
                .create(String.class, null);

        TILING_LEVELS = builder.addName("tile-levels")
                .setRequired(true)
                .setDescription(load("input.tile-levels"))
                .createBounded(Integer.class, 1, 21, 14);

        MIN_LEVEL = builder.addName("min-level")
                .setRequired(true)
                .setDescription(load("input.min-level"))
                .createBounded(Integer.class, 0, 21, 0);
        MAX_LEVEL = builder.addName("max-level")
                .setRequired(true)
                .setDescription(load("input.max-level"))
                .createBounded(Integer.class, 0, 21, 0);

    }

    protected static InternationalString load(final String key) {
        return new ResourceInternationalString(BUNDLE_LOCATION, key);
    }

    @Autowired
    protected IDataBusiness dataBusiness;

    @Autowired
    protected IStyleBusiness styleBusiness;

    @Autowired
    protected IProviderBusiness providerBusiness;

    @Autowired
    protected ILayerBusiness layerBusiness;

    public AbstractPyramidProcess(String name, InternationalString description, InternationalString title,
            ParameterDescriptorGroup input, ParameterDescriptorGroup output) {
        super(name, AdminProcessRegistry.IDENTIFICATION, description, title, input, output);
    }

    protected abstract class AbsProcess extends AbstractProcess {

        public AbsProcess(ProcessDescriptor desc, ParameterValueGroup input) {
            super(desc, input);
            SpringHelper.injectDependencies(this);
        }

        /**
         * Analyze user parameter to build the map context to use for tiling.
         *
         * @return An object containing the map context to use for tiling. The
         * map context CRS is initialized to the tile target system. All layers
         * to embed in it.
         *
         * @throws ProcessException If there's a problem while retrieving data
         * or layers configured by user.
         */
        protected SourceInfo getOrCreateMapContext() throws ProcessException {
            MapContextLayersDTO mc = inputParameters.getValue(MAP_CONTEXT);
            DatasetProcessReference dataset = inputParameters.getValue(DATASET);

            if (dataset != null && mc != null) {
                throw new ProcessException("Either a dataset or a map context should be given, not both.", this);
            } else if (dataset == null && mc == null) {
                throw new ProcessException("No source data given for tiling operation. Either map context or dataset parameter must be set.", this);
            }

            final String template = inputParameters.getMandatoryValue(TEMPLATE);

            CoordinateReferenceSystem targetCrs = CommonCRS.WGS84.normalizedGeographic();
            if (TEMPLATE_GM.equals(template)) {
                try {
                    targetCrs = CRS.forCode("EPSG:3857");
                } catch (FactoryException ex) {
                    throw new ProcessException(ex.getMessage(), this, ex);
                }
            }

            final SourceInfo s = new SourceInfo();
            final List<MapContextStyledLayerDTO> layers;
            final String name;
            if (mc != null) {
                name = mc.getName();
                layers = mc.getLayers();
                if (layers == null || layers.isEmpty()) {
                    throw new ProcessException("Given map context (named " + name + ") is empty (no layer embedded)", this);
                }

                s.data = mc.getLayers().stream()
                        .filter(l -> l.getLayerId() != null)
                        .map(l -> {
                            try {
                                return dataBusiness.getDataLayer(l.getLayerId());
                            } catch (ConstellationException ex) {
                                throw new RuntimeException("Cannot load data brief for layer " + l.getName(), ex);
                            }
                        })
                        .collect(Collectors.toList());
            } else if (dataset != null) {
                name = dataset.getIdentifier();
                s.data = dataBusiness.getDataBriefsFromDatasetId(dataset.getId());
                if (s.data == null || s.data.isEmpty()) {
                    throw new ProcessException("Given dataset (named " + name + ") is empty", this);
                }

                layers = s.data.stream()
                        .map(this::convert)
                        .collect(Collectors.toList());
            } else {
                throw new ProcessException("No data source configured.", this);
            }

            s.ctx = MapBuilder.createContext(targetCrs);
            for (final MapContextStyledLayerDTO dto : layers) {
                try {
                    s.ctx.items().add(load(dto));
                } catch (ConfigurationException | PortrayalException | MalformedURLException ex) {
                    fireWarningOccurred("Layer " + dto.getName() + " cannot be loaded", Float.NaN, ex);
                }
            }

            if (s.ctx.items().isEmpty()) {
                throw new ProcessException("No layer available for publication. See previous warnings for details", this);
            }

            s.ctx.setName(name);

            return s;
        }

        /**
         * Define the view to use while tiling data. We aim to fill source map
         * context {@link MapContext#getAreaOfInterest() } property.
         *
         * @param source The source containing the map context to fill.
         * @throws ProcessException If a problem occurs while analyzing user
         * input envelope, or while computing source data boundaries.
         */
        protected void prepareBoundaries(final SourceInfo source) throws ProcessException {
            double minx = inputParameters.getValue(ENVELOPE_MINX);
            double maxx = inputParameters.getValue(ENVELOPE_MAXX);
            double miny = inputParameters.getValue(ENVELOPE_MINY);
            double maxy = inputParameters.getValue(ENVELOPE_MAXY);

            GeneralEnvelope envelope = new GeneralEnvelope(CommonCRS.WGS84.normalizedGeographic());
            envelope.setRange(0, minx, maxx);
            envelope.setRange(1, miny, maxy);

            final CoordinateReferenceSystem envCrs = envelope.getCoordinateReferenceSystem();
            final CoordinateReferenceSystem ctxCrs = source.ctx.getCoordinateReferenceSystem();
            if (envCrs == null) {
                final GeneralEnvelope tmpEnv = new GeneralEnvelope(ctxCrs);
                tmpEnv.setEnvelope(envelope);
                envelope = tmpEnv;
            } else if (!Utilities.equalsApproximatively(envCrs, ctxCrs)) {
                try {
                    envelope = GeneralEnvelope.castOrCopy(Envelopes.transform(envelope, ctxCrs));
                } catch (TransformException ex) {
                    throw new ProcessException("Cannot convert tiling boundaries into queried coordinate reference system.", this, ex);
                }
            }

            GeneralEnvelope readyEnvelope = GeneralEnvelope.castOrCopy(envelope);
            if (readyEnvelope.isEmpty() || Util.containsInfinity(readyEnvelope)) {
                readyEnvelope = new GeneralEnvelope(org.apache.sis.referencing.CRS.getDomainOfValidity(ctxCrs));
            }

            source.ctx.setAreaOfInterest(readyEnvelope);
        }

        /**
         * Build a map layer from an examind data. The style used for rendering
         * is either the first style associated with the data, or Examind default
         * rendering style, if no rule is associated withe input data.
         *
         * @param data The data to transform into a renderable layer.
         * @return A renderable layer, fully opaque, to render input data. Never
         * null.
         */
        private MapContextStyledLayerDTO convert(final DataBrief data) {
            final MapContextStyledLayerDTO layerDto = new MapContextStyledLayerDTO();
            layerDto.setDataId(data.getId());

            final List<StyleBrief> styles = data.getTargetStyle();
            if (styles != null && !styles.isEmpty()) {
                final StyleBrief style = styles.get(0);
                layerDto.setStyleName(style.getName());
                layerDto.setStyleId(style.getId());
            }

            layerDto.setIswms(false);
            layerDto.setOpacity(100);
            layerDto.setVisible(true);
            return layerDto;
        }

        /**
         * Build a Geotoolkit map item from an Examind Map context layer.
         *
         * TODO : move in some utility module/class ?
         *
         * @param layer The Map context layer to transform.
         * @return A map item ready-to-be rendered by Geotoolkit.
         * @throws ConfigurationException If the data or style referenced from this
         * layer cannot be found in administration database.
         * @throws PortrayalException If referenced data is found, but we cannot
         * prepare a rendering context.
         * @throws MalformedURLException If input DTO references an external
         * service, but embedded URL is not a valid URL.
         * @throws RuntimeException Other various errors can happen while analyzing
         * input object. For example, if given object is empty, an
         * IllegalArgumentException will be thrown.
         */
        private MapItem load(final MapContextStyledLayerDTO layer) throws ConfigurationException, PortrayalException, MalformedURLException {
            final Integer layerId = layer.getLayerId();
            if (layerId != null) {
                // TODO : What is this ? Should we load an already existing Examind layer ?
            }

            final Integer dataId = layer.getDataId();
            if (dataId != null) {
                final DataBrief data;
                try {
                    data = dataBusiness.getDataBrief(dataId);
                } catch (ConstellationException ex) {
                    throw new IllegalStateException("Data referenced by layer " + layer.getName() + " cannot be find.");
                }

                final Integer styleId = layer.getStyleId();
                final Style layerStyle;
                if (styleId != null) {
                    layerStyle = styleBusiness.getStyle(styleId);
                } else {
                    layerStyle = null; // Should be replaced by default style on layer build
                }

                final DataProvider provider = DataProviders.getProvider(data.getProvider());
                org.constellation.provider.Data realData = provider.get(Names.createLocalName(data.getNamespace(), ":", data.getName()));
                try {
                    return ((GeoData) realData).getMapLayer((MutableStyle) layerStyle, null);
                } catch (ConstellationException ex) {
                    throw new PortrayalException(ex.getMessage(), ex);
                }
            }

            String serviceUrl = layer.getExternalServiceUrl();
            if (serviceUrl != null) {
                // TODO : Could it be something else than a WMS ?
                URL verifiedUrl = new URL(serviceUrl);
                final WMSVersion wmsVersion;
                if (layer.getExternalServiceVersion() == null) {
                    wmsVersion = WMSVersion.auto;
                } else {
                    wmsVersion = WMSVersion.getVersion(layer.getExternalServiceVersion());
                }

                WebMapClient wmsClient = new WebMapClient(verifiedUrl, wmsVersion);
                final WMSMapLayer wmsLayer = new WMSMapLayer(wmsClient, layer.getExternalLayer());
                final String wmsStyle = layer.getExternalStyle();
                if (wmsStyle != null) {
                    wmsLayer.getCoverageReference().setStyles(wmsStyle);
                }

                return wmsLayer;
            }

            throw new IllegalArgumentException("Not enough information in map context layer named " + layer.getName() + ". We cannot load back related data.");
        }

        protected double getScale(String templateName, int level) throws FactoryException {

            final DefiningPyramid pyramid;
            if (TEMPLATE_GM.equals(templateName)) {
                pyramid = Pyramids.createPseudoMercatorTemplate(level);
            } else {
                pyramid = Pyramids.createWorldWGS84Template(level);
            }
            final Mosaic mosaic = pyramid.getMosaics(0).iterator().next();
            return mosaic.getScale();
        }

        protected double getSEScale(String templateName, int level) throws FactoryException {

            final DefiningPyramid pyramid;
            if (TEMPLATE_GM.equals(templateName)) {
                pyramid = Pyramids.createPseudoMercatorTemplate(level);
            } else {
                pyramid = Pyramids.createWorldWGS84Template(level);
            }
            final Mosaic mosaic = pyramid.getMosaics(0).iterator().next();

            final Dimension gridSize = mosaic.getGridSize();
            final Dimension tileSize = mosaic.getTileSize();
            final Rectangle rec = new Rectangle(0, 0, gridSize.width*tileSize.width, gridSize.height*tileSize.height);
            final GeneralEnvelope env = Pyramids.computeMosaicEnvelope(mosaic);
            env.setCoordinateReferenceSystem(pyramid.getCoordinateReferenceSystem());
            return CanvasUtilities.computeSEScale(env, new AffineTransform(), rec);
        }

    }


    /**
     * Embed information about the map/data to tile.
     */
    protected static class SourceInfo {

        MapContext ctx;
        List<DataBrief> data;
    }

}
