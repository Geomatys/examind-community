package org.constellation.map.featureinfo;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.List;

import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.geometry.Envelope;

import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.geometry.ImmutableEnvelope;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.util.iso.Names;

import org.geotoolkit.display.PortrayalException;
import org.geotoolkit.display2d.service.CanvasDef;
import org.geotoolkit.display2d.service.SceneDef;
import org.geotoolkit.map.FeatureMapLayer;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.map.MapContext;
import org.geotoolkit.map.MapLayer;
import org.geotoolkit.storage.memory.InMemoryFeatureSet;
import org.geotoolkit.storage.memory.InMemoryGridCoverageResource;

/**
 * Aim to simplify Get Feature Info simulation by providing a configurable environment.
 */
class GetFeatureInfoContext {

    final MapContext context = MapBuilder.createContext(CommonCRS.defaultGeographic());;
    Dimension canvasSize = new Dimension(16, 16);
    Envelope displayEnvelope = new ImmutableEnvelope(new double[]{-180, -90}, new double[]{180, 90}, CommonCRS.defaultGeographic());
    Rectangle selection = new Rectangle(canvasSize);

    public GetFeatureInfoContext() {}

    /**
     * Createa feature map layer from given dataset, set it selectable and visible, then add to current {@link #context}.
     *
     * @param name Wanted name for the layer to create.
     * @param datatype Common Feature type of all given features.
     * @param dataset Set of features for the dataset.
     * @return Created layer for further configuration.
     */
    public FeatureMapLayer createLayer(String name, FeatureType datatype, List<Feature> dataset) {
        final FeatureMapLayer layer = MapBuilder.createFeatureLayer(new InMemoryFeatureSet(datatype, dataset));
        prepareLayer(name, layer);

        return layer;
    }

    private void prepareLayer(String name, MapLayer layer) {
        layer.setIdentifier(name);
        layer.setSelectable(true);
        layer.setVisible(true);

        context.getComponents().add(layer);
    }

    public MapLayer createLayer(String name, GridCoverage dataset) {
        final InMemoryGridCoverageResource resource = new InMemoryGridCoverageResource(Names.createLocalName("examind", ":", name), dataset);
        MapLayer layer = MapBuilder.createLayer(resource);
        prepareLayer(name, layer);
        return layer;
    }

    public Object getFeatureInfo(final FeatureInfoFormat target) throws PortrayalException {
        return target.getFeatureInfo(
                new SceneDef(context),
                new CanvasDef(canvasSize, displayEnvelope),
                selection,
                () -> { throw new UnsupportedOperationException("TODO: set configurable"); }
        );
    }
}

