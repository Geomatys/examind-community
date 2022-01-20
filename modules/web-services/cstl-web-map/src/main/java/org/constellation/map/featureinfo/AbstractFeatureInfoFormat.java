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

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.Resource;
import org.apache.sis.util.logging.Logging;
import org.constellation.api.DataType;
import org.constellation.dto.service.config.wxs.GetFeatureInfoCfg;
import org.constellation.ws.LayerCache;
import org.geotoolkit.display.PortrayalException;
import org.geotoolkit.display.SearchArea;
import org.geotoolkit.display.canvas.RenderingContext;
import org.geotoolkit.display2d.GO2Hints;
import org.geotoolkit.display2d.GraphicVisitor;
import org.geotoolkit.display2d.canvas.RenderingContext2D;
import org.apache.sis.internal.map.Presentation;
import org.geotoolkit.display2d.primitive.ProjectedCoverage;
import org.geotoolkit.display2d.primitive.ProjectedFeature;
import org.geotoolkit.display2d.primitive.SearchAreaJ2D;
import org.geotoolkit.display2d.service.CanvasDef;
import org.geotoolkit.display2d.service.DefaultPortrayalService;
import org.geotoolkit.display2d.service.SceneDef;
import org.geotoolkit.display2d.service.VisitDef;
import org.apache.sis.portrayal.MapLayer;
import org.geotoolkit.ows.xml.GetFeatureInfo;
import org.geotoolkit.util.NamesExt;
import org.opengis.feature.Feature;
import org.opengis.util.GenericName;

/**
 * @author Quentin Boileau (Geomatys)
 */
public abstract class AbstractFeatureInfoFormat implements FeatureInfoFormat {

    protected static final Logger LOGGER = Logging.getLogger("org.constellation.map.featureinfo");

    /**
     * Contains the values for all coverage layers requested.
     */
    protected final Map<String, List<GridCoverageResource>> coverages = new HashMap<>();

    /**
     * Contains all features that cover the point requested, for feature layers.
     */
    protected final Map<String, List<Feature>> features = new HashMap<>();

    /**
     * GetFeatureInfo configuration.
     */
    private GetFeatureInfoCfg configuration;

    /**
     * Layers informations
     */
    private List<LayerCache> layers;

    /**
     * {@inheritDoc}
     */
    @Override
    public GetFeatureInfoCfg getConfiguration() {
        return configuration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setConfiguration(GetFeatureInfoCfg conf) {
        this.configuration = conf;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<LayerCache> getLayers() {
        return layers;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLayers(List<LayerCache> layers) {
        this.layers = layers;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<LayerCache> getLayer(GenericName name, DataType type) {
        if (layers == null) return Optional.empty();
        return layers.stream()
                .filter(layer -> layer.getDataType().equals(type) && layer.getName().equals(name))
                .findAny();
    }

    /**
     * Visit all intersected Graphic objects and call {link #nextProjectedFeature}
     * or {link #nextProjectedCoverage}.
     *
     *
     * @param sDef {@link org.geotoolkit.display2d.service.SceneDef}
     * @param cDef {@link org.geotoolkit.display2d.service.CanvasDef}
     * @param searchArea {@link java.awt.Rectangle} of the searching area
     * @param maxCandidat Maximum number of layers to collect information for. If null or less than 1, we consider there
     *                    is no limit to the number of collectable layers.
     * @see #nextProjectedFeature(MapLayer, Feature, RenderingContext2D, SearchAreaJ2D)
     * @see #nextProjectedCoverage(MapLayer, GridCoverageResource, RenderingContext2D, SearchAreaJ2D)
     */
    protected void getCandidates(final SceneDef sDef, final CanvasDef cDef, final Rectangle searchArea,
                                 final Integer maxCandidat) throws PortrayalException {

        final VisitDef visitDef = new VisitDef();
        visitDef.setArea(searchArea);
        visitDef.setVisitor(new GraphicVisitor() {

            int idx = 0;
            @Override
            public void startVisit() {
            }

            @Override
            public void endVisit() {
            }

            @Override
            public void visit(Presentation graphic, RenderingContext context, SearchArea area) {
                if(graphic == null ) return;

                final Object candidate = graphic.getCandidate();
                final MapLayer layer = graphic.getLayer();
                final Resource resource = (layer == null) ? null : layer.getData();

                if (candidate instanceof Feature) {
                    nextProjectedFeature(layer, (Feature) candidate, (RenderingContext2D) context, (SearchAreaJ2D) area);
                } else if (resource instanceof GridCoverageResource) {
                    nextProjectedCoverage(layer, (GridCoverageResource) resource, (RenderingContext2D) context, (SearchAreaJ2D) area);
                }
                idx++;
            }

            @Override
            public boolean isStopRequested() {
                return maxCandidat != null && maxCandidat > 0 && idx >= maxCandidat;
            }
        });

        sDef.getHints().put(GO2Hints.KEY_PRESERVE_PROPERTIES, true);
        DefaultPortrayalService.visit(cDef, sDef, visitDef);
    }

    /**
     * Store the {@link Feature} in a list
     *
     * @param context rendering context
     * @param queryArea area of the search
     */
    protected void nextProjectedFeature(MapLayer layer, final Feature feature, final RenderingContext2D context,
                                        final SearchAreaJ2D queryArea) {

        final String layerName = layer.getIdentifier();
        List<Feature> feat = features.get(layerName);
        if (feat == null) {
            feat = new ArrayList<>();
            features.put(layerName, feat);
        }
        feat.add(feature);
    }

    /**
     * Store the {@link GridCoverageResource} in a list
     *
     * @param context rendering context
     * @param queryArea area of the search
     */
    protected void nextProjectedCoverage(MapLayer layer, final GridCoverageResource resource, final RenderingContext2D context,
                                         final SearchAreaJ2D queryArea) {

        final String layerName = layer.getIdentifier();
        List<GridCoverageResource> cov = coverages.get(layerName);
        if (cov == null) {
            cov = new ArrayList<>();
            coverages.put(layerName, cov);
        }
        cov.add(resource);
    }

    /**
     * Extract the max feature count number from WMS {@link GetFeatureInfo} requests or null.
     *
     * @param gfi {@link GetFeatureInfo} request
     * @return max Feature count if WMS {@link GetFeatureInfo} or null otherwise.
     */
    protected Integer getFeatureCount(GetFeatureInfo gfi) {
        if (gfi != null && gfi instanceof org.geotoolkit.wms.xml.GetFeatureInfo) {
            org.geotoolkit.wms.xml.GetFeatureInfo wmsGFI = (org.geotoolkit.wms.xml.GetFeatureInfo) gfi;
            return wmsGFI.getFeatureCount();
        }
        return null;
    }

    protected GenericName getNameForFeatureLayer(MapLayer ml) {
        final GenericName layerName ;
        if (ml.getUserProperties().containsKey("layerName")) {
            layerName = (GenericName) ml.getUserProperties().get("layerName");
        } else {
            layerName = NamesExt.create(ml.getIdentifier());
        }
        return layerName;
    }

    protected GenericName getNameForCoverageLayer(MapLayer ml) {
        if (ml.getUserProperties().containsKey("layerName")) {
            return (GenericName) ml.getUserProperties().get("layerName");
        } else {
            final Resource ref = ml.getData();
            try {
                return ref.getIdentifier().orElseThrow(() -> new RuntimeException("Cannot extract resource identifier"));
            } catch (DataStoreException e) {
                throw new RuntimeException("Cannot extract resource identifier", e);
            }
        }
    }
}
