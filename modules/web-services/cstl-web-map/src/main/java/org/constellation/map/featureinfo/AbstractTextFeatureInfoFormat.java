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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import org.apache.sis.internal.map.Presentation;
import org.apache.sis.portrayal.MapLayer;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.Resource;
import org.apache.sis.util.collection.BackingStoreException;
import org.geotoolkit.display.PortrayalException;
import org.geotoolkit.display.SearchArea;
import org.geotoolkit.display.canvas.RenderingContext;
import org.geotoolkit.display2d.GO2Hints;
import org.geotoolkit.display2d.GraphicVisitor;
import org.geotoolkit.display2d.canvas.RenderingContext2D;
import org.geotoolkit.display2d.primitive.SearchAreaJ2D;
import org.geotoolkit.display2d.service.CanvasDef;
import org.geotoolkit.display2d.service.DefaultPortrayalService;
import org.geotoolkit.display2d.service.SceneDef;
import org.geotoolkit.display2d.service.VisitDef;
import org.geotoolkit.feature.ReprojectMapper;
import org.geotoolkit.ows.xml.GetFeatureInfo;
import org.geotoolkit.storage.memory.InMemoryFeatureSet;
import org.opengis.feature.Feature;

/**
 * @author Quentin Boileau (Geomatys)
 */
public abstract class AbstractTextFeatureInfoFormat extends AbstractFeatureInfoFormat {

    /**
     * Contains the values for all coverage layers requested.
     */
    protected final Map<QName, List<String>> coverages = new HashMap<>();

    protected final Map<QName, List<String>> features = new HashMap<>();

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

                if (candidate instanceof Feature feat) {
                     // here we apply the query in mapLayer because the projection part is not applied by the renderer.
                    if (layer != null && layer.getQuery() != null) {
                        FeatureSet inter = new InMemoryFeatureSet(feat.getType(), Collections.singletonList(feat));
                        try {
                            inter = inter.subset(layer.getQuery());
                            feat = inter.features(false).findAny().orElseThrow(DataStoreException::new);
                        } catch (DataStoreException ex) {
                            throw new BackingStoreException("Error while applying query on single feature.", ex);
                        }
                    }

                    // Force data CRS to be the same as requested by user (CRS parameter from GetFeatureInfo query)
                    final ReprojectMapper mapper = new ReprojectMapper(feat.getType(), context.getObjectiveCRS2D());
                    feat = mapper.apply(feat);

                    final QName layerName = getNameForFeatureLayer(layer);
                    nextProjectedFeature(layerName, feat, (RenderingContext2D) context, (SearchAreaJ2D) area);
                } else if (resource instanceof GridCoverageResource) {
                    final QName layerName = getNameForCoverageLayer(layer);
                    nextProjectedCoverage(layerName, (GridCoverageResource) resource, (RenderingContext2D) context, (SearchAreaJ2D) area);
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
     * @param layerName Name of the layer.
     * @param feature The feature to process.
     * @param context rendering context
     * @param queryArea area of the search
     */
    protected abstract void nextProjectedFeature(QName layerName, final Feature feature, final RenderingContext2D context,
                                        final SearchAreaJ2D queryArea);

    /**
     * Store the {@link GridCoverageResource} in a list
     *
     * @param layerName Name of the layer.
     * @param resource The coverage to process.
     * @param context rendering context
     * @param queryArea area of the search
     */
    protected abstract void nextProjectedCoverage(QName layerName, final GridCoverageResource resource, final RenderingContext2D context,
                                         final SearchAreaJ2D queryArea);

     /**
     * Extract the max feature count number from WMS {@link GetFeatureInfo} requests or null.
     *
     * @param gfi {@link GetFeatureInfo} request
     * @return max Feature count if WMS {@link GetFeatureInfo} or null otherwise.
     */
    protected Integer getFeatureCount(GetFeatureInfo gfi) {
        if (gfi instanceof org.geotoolkit.wms.xml.GetFeatureInfo wmsGFI) {
            return wmsGFI.getFeatureCount();
        }
        return null;
    }
}
