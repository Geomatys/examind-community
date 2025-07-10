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

package org.constellation.coverage.ws.rs;

import org.geotoolkit.image.io.metadata.SpatialMetadata;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.IncompleteGridGeometryException;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.geotoolkit.image.io.metadata.ReferencingBuilder;
import org.geotoolkit.image.io.metadata.SpatialMetadataFormat;
import org.geotoolkit.internal.image.io.DimensionAccessor;
import org.geotoolkit.internal.image.io.GridDomainAccessor;
import org.opengis.coverage.CannotEvaluateException;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class GridCoverageNCWriter implements HttpMessageConverter<GridCoverage> {

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return Entry.class.isAssignableFrom(clazz) && mediaType.toString().equals("application/x-netcdf");
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(MediaType.parseMediaType("application/x-netcdf"));
    }

    @Override
    public GridCoverage read(Class<? extends GridCoverage> type, HttpInputMessage him) throws IOException, HttpMessageNotReadableException {
        throw new HttpMessageNotReadableException("Entry message converter do not support reading.", him);
    }

    @Override
    public void write(GridCoverage gc, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        writeInStream(gc, outputMessage.getBody());
    }
    
    /**
     * Add information about given coverage in input metadata.
     *
     * @return A new spatial metadata. The metadata has been modified with target data information.
     * @throws IncompleteGridGeometryException when the coordinate reference system or the grid to crs of given geometry
     * is not available.
     * @throws CannotEvaluateException when we cannot safely extract a 2D part of input grid geometry. Note that this is
     * caused due to limitations of the {@link SpatialMetadata} capabilities.
     */
    @Deprecated
    private static SpatialMetadata adapt(final GridCoverage targetData) throws IncompleteGridGeometryException, CannotEvaluateException {
        
        //  The metadata to modify to match given coverage structure. we'll create and send back a fresh one.
        SpatialMetadata source = new SpatialMetadata(SpatialMetadataFormat.getImageInstance(SpatialMetadataFormat.GEOTK_FORMAT_NAME));
        
        // The grid geometry which will serve to fill metadata grid information. If null, no grid information is added into metadata.
        GridGeometry gg = targetData.getGridGeometry();
        
        final SampleDimension[] targetDims = targetData.getSampleDimensions().toArray(new SampleDimension[0]);

        //add ImageCRS in SpatialMetadata
        if (gg != null) {
            if (!(gg.getGridToCRS(PixelInCell.CELL_CENTER) instanceof LinearTransform)) {
                /* HACK : For now, we cannot write properly additional dimension information, because grid domain
                 * accessor skip the entire grid to CRS if it is not linear. So, to at least keep 2D projection information,
                 * We delete time and elevation information. Another solution would be to use jacobian matrix to reduce
                 * non linear parts of the grid geometry to constants, but I have no time for now. Plus, all this code
                 * should not be necessary if we used proper APIs for response writing.
                 */
                gg = gg.selectDimensions(gg.getExtent().getSubspaceDimensions(2));
            }

            new ReferencingBuilder(source).setCoordinateReferenceSystem(gg.getCoordinateReferenceSystem());
            new GridDomainAccessor(source).setGridGeometry(gg, null, null);
        }

        if (targetDims != null) {
            DimensionAccessor dimAccess = new org.geotoolkit.internal.image.io.DimensionAccessor(source);
            if (dimAccess.childCount() != targetDims.length) {
                dimAccess.removeChildren();
                for (SampleDimension gsd : targetDims) {
                    dimAccess.selectChild(dimAccess.appendChild());
                    dimAccess.setDimension(gsd, Locale.ROOT);
                    dimAccess.selectParent();
                }
            }
        }
        source.clearInstancesCache();

        return source;
    }

    public static void writeInStream(final GridCoverage coverage, final OutputStream out) throws IOException {
        final SpatialMetadata metadata = adapt(coverage);
        final IIOImage iioimage        = new IIOImage(coverage.render(null), null, metadata);
        final ImageWriter iowriter     = ImageIO.getImageWritersByFormatName("netcdf").next();

        iowriter.setOutput(ImageIO.createImageOutputStream(out));
        iowriter.write(null, iioimage, null);
        iowriter.dispose();
    }
}
