/*
 *    Examind community - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2022 Geomatys.
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
package com.examind.process.admin.renderedpyramid;

import com.examind.process.admin.AdminProcessDescriptor;
import com.examind.process.admin.AdminProcessRegistry;
import java.util.ArrayList;
import java.util.List;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.Resource;
import org.apache.sis.util.ResourceInternationalString;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.storage.multires.TileGenerator;
import org.geotoolkit.display2d.MapContextTileGenerator;
import org.geotoolkit.factory.Hints;
import org.geotoolkit.image.interpolation.InterpolationCase;
import org.apache.sis.portrayal.MapLayers;
import org.apache.sis.portrayal.MapLayer;
import org.geotoolkit.process.ProcessException;
import org.geotoolkit.processing.AbstractProcess;
import org.geotoolkit.processing.AbstractProcessDescriptor;
import org.geotoolkit.processing.ForwardProcessListener;
import org.geotoolkit.storage.coverage.CoverageTileGenerator;
import org.geotoolkit.storage.coverage.mosaic.AggregatedCoverageResource;
import org.geotoolkit.storage.multires.WritableTileMatrixSet;
import org.geotoolkit.storage.multires.WritableTiledResource;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.springframework.stereotype.Component;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
@Component
public class PyramidProcess extends AbstractProcessDescriptor implements AdminProcessDescriptor {

    public static final String BUNDLE_LOCATION = "com/examind/process/admin/renderedpyramid/bundle";
    protected static final ParameterDescriptor<MapLayers> MAPCONTEXT;
    protected static final ParameterDescriptor<WritableTiledResource> RESOURCE;
    protected static final ParameterDescriptor<InterpolationCase> INTERPOLATION;
    protected static final ParameterDescriptor<String> MODE;


    private static final ParameterDescriptorGroup INPUT;

    private static final ParameterDescriptorGroup OUTPUT;

    static {
        final ParameterBuilder builder = new ParameterBuilder();


        MAPCONTEXT = builder.addName("mapcontext")
                .setRequired(true)
                .create(MapLayers.class, null);

        RESOURCE = builder.addName("resource")
                .setRequired(true)
                .create(WritableTiledResource.class, null);

        INTERPOLATION = builder.addName("interpolation")
                .setRequired(true)
                .create(InterpolationCase.class, InterpolationCase.NEIGHBOR);

        MODE = builder.addName("mode")
                .setRequired(true)
                .createEnumerated(String.class, new String[]{"CONFORM","RENDERED"},"CONFORM");


        INPUT = builder.addName("input").createGroup(MAPCONTEXT, RESOURCE, INTERPOLATION, MODE);
        OUTPUT = builder.addName("output").createGroup();
    }

    public PyramidProcess() {
        super("gen-pyramid", AdminProcessRegistry.IDENTIFICATION,
                new ResourceInternationalString(BUNDLE_LOCATION, "gen.description"),
                new ResourceInternationalString(BUNDLE_LOCATION, "gen.title"), INPUT, OUTPUT);
    }

    @Override
    public org.geotoolkit.process.Process createProcess(ParameterValueGroup input) {
        return new Processor(input);
    }

    private class Processor extends AbstractProcess {

        public Processor(ParameterValueGroup input) {
            super(PyramidProcess.this, input);
        }

        @Override
        protected void execute() throws ProcessException {

            final MapLayers context = inputParameters.getMandatoryValue(MAPCONTEXT);
            final WritableTiledResource resource = inputParameters.getMandatoryValue(RESOURCE);
            final InterpolationCase interpolation = inputParameters.getMandatoryValue(INTERPOLATION);
            final String mode = inputParameters.getMandatoryValue(MODE);

            final TileGenerator generator;
            switch (mode) {
                case "RENDERED" : generator = new MapContextTileGenerator(context, new Hints(Hints.KEY_ANTIALIASING, Hints.VALUE_ANTIALIAS_ON)); break;
                case "CONFORM" : {
                    GridCoverageResource singleRes = null;
                    List<GridCoverageResource> bands = new ArrayList<>();
                    for (MapLayer layer : MapBuilder.getLayers(context)) {
                        Resource res = layer.getData();
                        if (res instanceof GridCoverageResource) {
                            singleRes = (GridCoverageResource) res;
                            bands.add(singleRes);
                        }
                    }

                    if (bands.isEmpty()) {
                        throw new ProcessException("MapContext must contain at least one coverage layer ", this);
                    } else if (bands.size() > 1) {
                        try {
                            CoordinateReferenceSystem crs;
                            if (context.getAreaOfInterest() != null) {
                                crs = context.getAreaOfInterest().getCoordinateReferenceSystem();
                            } else {
                                throw new ProcessException("Missing CRS on map context", this);
                            }
                            singleRes = AggregatedCoverageResource.create(crs, bands.toArray(new GridCoverageResource[bands.size()]));
                        } catch (DataStoreException | TransformException ex) {
                            throw new ProcessException(ex.getMessage(), this, ex);
                        }
                    }

                    CoverageTileGenerator ctg;
                    try {
                        ctg = new CoverageTileGenerator(singleRes);
                    } catch (DataStoreException ex) {
                        throw new ProcessException(ex.getMessage(), this, ex);
                    }
                    ctg.setInterpolation(interpolation);
                    generator = ctg;
                } break;
                default : throw new ProcessException("Unexpected pyramid mode "+mode, this);
            }

            try {
                for (WritableTileMatrixSet pyramid : resource.getTileMatrixSets()) {
                    final ForwardProcessListener fp = new ForwardProcessListener(this, 1, 99);
                    generator.generate(pyramid, null, null, fp);
                }
            } catch (DataStoreException | InterruptedException ex) {
                throw new ProcessException(ex.getMessage(), this, ex);
            }

        }

    }

}
