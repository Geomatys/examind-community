/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2016 Geomatys.
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

import java.awt.Image;
import java.awt.image.RenderedImage;
import java.util.List;
import java.util.concurrent.CancellationException;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.geotoolkit.coverage.grid.GridCoverage;
import org.geotoolkit.coverage.grid.GridCoverage2D;
import org.geotoolkit.coverage.io.CoverageStoreException;
import org.geotoolkit.coverage.io.GridCoverageReadParam;
import org.geotoolkit.coverage.io.GridCoverageReader;
import org.geotoolkit.coverage.io.GridCoverageWriter;
import org.geotoolkit.storage.coverage.AbstractCoverageResource;
import org.geotoolkit.storage.coverage.CoverageResource;
import org.opengis.util.GenericName;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class ForcedSampleDimensionsCoverageResource extends AbstractCoverageResource {


    private final List<SampleDimension> dimensions;
    private final CoverageResource baseRef;

    public ForcedSampleDimensionsCoverageResource(CoverageResource baseRef, List<SampleDimension> dimensions) throws DataStoreException {
        super(null, baseRef.getIdentifier());
        this.baseRef = baseRef;
        this.dimensions = dimensions;
    }

    @Override
    public boolean isWritable() throws DataStoreException {
        return false;
    }

    @Override
    public GridCoverageReader acquireReader() throws CoverageStoreException {
        final GridCoverageReader baseReader = (GridCoverageReader) baseRef.acquireReader();
        return new ForcedSDCoverageReader(baseReader);
    }

    @Override
    public GridCoverageWriter acquireWriter() throws CoverageStoreException {
        throw new CoverageStoreException("Not supported.");
    }

    @Override
    public Image getLegend() throws DataStoreException {
        return null;
    }

    @Override
    public GridGeometry getGridGeometry() throws DataStoreException {
        return ((GridCoverageResource) baseRef).getGridGeometry();
    }


    private class ForcedSDCoverageReader extends GridCoverageReader{

        private final GridCoverageReader baseReader;

        public ForcedSDCoverageReader(GridCoverageReader baseReader){
            this.baseReader = baseReader;
        }

        @Override
        public GenericName getCoverageName() throws CoverageStoreException, CancellationException {
            return baseReader.getCoverageName();
        }

        @Override
        public GridGeometry getGridGeometry() throws CoverageStoreException, CancellationException {
            return baseReader.getGridGeometry();
        }

        @Override
        public List<SampleDimension> getSampleDimensions() throws CoverageStoreException, CancellationException {
            return dimensions;
        }

        @Override
        public GridCoverage read(GridCoverageReadParam param) throws CoverageStoreException, CancellationException {
            final GridCoverage baseCoverage = baseReader.read(param);
            if(!(baseCoverage instanceof GridCoverage2D)){
                throw new CoverageStoreException("Forced alpha reader only support grid coverage 2d, but found a "+baseCoverage.getClass().getName());
            }
            final GridCoverage2D cov2d = (GridCoverage2D) baseCoverage;
            final RenderedImage ri = cov2d.getRenderedImage();
            return new GridCoverage2D(cov2d.getName(), ri, cov2d.getGridGeometry(),
                    dimensions.toArray(new SampleDimension[0]), null, cov2d.getProperties(), null);

        }

    }


}
