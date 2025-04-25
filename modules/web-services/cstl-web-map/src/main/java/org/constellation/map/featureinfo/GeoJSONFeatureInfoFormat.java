/*
 *    Examind community - An open source and standard compliant SDI
 *    https://www.examind.com/examind-community/
 *
 * Copyright 2025 Geomatys.
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

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import javax.xml.namespace.QName;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.storage.GridCoverageResource;
import org.constellation.api.DataType;
import static org.constellation.map.featureinfo.AbstractFeatureInfoFormat.LOGGER;
import static org.constellation.map.featureinfo.AbstractFeatureInfoFormat.searchElevation;
import static org.constellation.map.featureinfo.AbstractFeatureInfoFormat.searchLastTime;
import org.constellation.map.featureinfo.dto.FeatureCollectionInfo;
import org.constellation.map.featureinfo.dto.GeoCoverageInfo;
import org.constellation.map.featureinfo.dto.GeoFeatureInfo;
import org.constellation.map.featureinfo.dto.LayerError;
import org.constellation.map.featureinfo.dto.LayerInfo;
import org.constellation.ws.LayerCache;
import org.constellation.ws.MimeType;
import org.geotoolkit.display.PortrayalException;
import org.geotoolkit.display2d.canvas.RenderingContext2D;
import org.geotoolkit.display2d.primitive.SearchAreaJ2D;
import org.geotoolkit.display2d.service.CanvasDef;
import org.geotoolkit.display2d.service.SceneDef;
import org.geotoolkit.internal.geojson.binding.GeoJSONGeometry;
import org.geotoolkit.ows.xml.GetFeatureInfo;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.feature.Feature;
import org.opengis.geometry.DirectPosition;

/**
 *
 * @author glegal
 */
public class GeoJSONFeatureInfoFormat extends AbstractTextFeatureInfoFormat {

    private static final GeometryFactory GF = org.geotoolkit.geometry.jts.JTS.getFactory();
    
    private static final ObjectWriter WRITER;
    static {
        WRITER = new ObjectMapper()
                .registerModules(new SimpleModule(
                        "GetFeatureInfo_JSON",
                        Version.unknownVersion(),
                        Arrays.asList(new GeoJSONFeatureSerializer(), new JTSSerializer(), new EnvelopeSerializer())),
                        new JavaTimeModule() 
                )
                .disable(SerializationFeature.INDENT_OUTPUT)
                .writerFor(LayerInfo.class);
    }

    private final List<LayerInfo> infoQueue = new ArrayList<>();
    private GetFeatureInfo gfi;
    
    @Override
    public Object getFeatureInfo(SceneDef sdef, CanvasDef cdef, Rectangle searchArea, GetFeatureInfo getFI) throws PortrayalException {
        if (gfi != null || !infoQueue.isEmpty()) throw new IllegalStateException("Reuse of FeatureInfoFormat object is illegal ! You must create a new instance for each query");
        this.gfi = getFI;
        getCandidates(sdef, cdef, searchArea, -1);

        if (!infoQueue.isEmpty() && infoQueue.stream().allMatch(LayerError.class::isInstance)) {
            final PortrayalException err = new PortrayalException("None of requested layers can be evaluated for input GetFeatureInfo query");
            for(LayerInfo info : infoQueue) err.addSuppressed(((LayerError)info).getError());
            throw err;
        }
        
        // build a feature collection
        try (final ByteArrayOutputStream buffer = new ByteArrayOutputStream(8192)) {
            try (final OutputStreamWriter writer = new OutputStreamWriter(buffer, StandardCharsets.UTF_8);
                 final SequenceWriter sw = WRITER.writeValues(writer);
            ) {
                sw.write(new FeatureCollectionInfo(infoQueue));
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot serialize GetFeatureInfo JSON", e);
        }
    }

    @Override
    public List<String> getSupportedMimeTypes() {
        return Arrays.asList(MimeType.APP_GEOJSON);
    }

    @Override
    protected void nextProjectedFeature(QName layerName, Feature candidate, RenderingContext2D context, SearchAreaJ2D queryArea) {
        final String layerNameStr = layerName.getLocalPart();
        try {
            infoQueue.add(new GeoFeatureInfo(layerNameStr, candidate));
        } catch (Exception e) {
            final LayerError err = new LayerError();
            err.setError(e);
            err.setLayer(layerNameStr);
            infoQueue.add(err);
        }
    }

    @Override
    protected void nextProjectedCoverage(QName layerName, GridCoverageResource resource, RenderingContext2D context, SearchAreaJ2D queryArea) {
        try {
            final List<FeatureInfoUtilities.Sample> results = FeatureInfoUtilities.getCoverageValues(resource, context, queryArea);

            if (results == null || results.isEmpty()) return;

            final DirectPosition searchPoint = FeatureInfoUtilities.getSearchPoint(context, queryArea);
            final GeoCoverageInfo info = buildCoverageInfo(layerName, getLayer(layerName, DataType.COVERAGE), gfi, results, searchPoint);
            infoQueue.add(info);
        } catch (Exception e) {
            final LayerError err = new LayerError();
            err.setError(e);
            err.setLayer(layerName.getLocalPart());
            infoQueue.add(err);
        }
    }

    protected static GeoCoverageInfo buildCoverageInfo(final QName layerName, final Optional<LayerCache> layerO, final GetFeatureInfo gfi, final List<FeatureInfoUtilities.Sample> results, DirectPosition searchPoint) {
        Map properties = new LinkedHashMap<>();
        LayerCache layer = layerO.orElse(null);
        List<Date> time;
        Double elevation;
        if (gfi != null && gfi instanceof org.geotoolkit.wms.xml.GetFeatureInfo) {
            org.geotoolkit.wms.xml.GetFeatureInfo wmsGFI = (org.geotoolkit.wms.xml.GetFeatureInfo) gfi;
            time = wmsGFI.getTime();
            elevation = wmsGFI.getElevation();
        } else {
            time = null;
            elevation = null;
        }

        if (time == null || time.isEmpty()) {
            final Date lastTime = searchLastTime(layer);
            if (lastTime != null) time = Collections.singletonList(lastTime);
        }

        if (elevation == null) elevation = searchElevation(layer);

        final Instant javaTime = time == null || time.isEmpty() ? null : time.get(time.size() - 1).toInstant();
        if (javaTime != null) {
            properties.put("time", javaTime);
        }
        if (elevation != null) {
            properties.put("elevation", elevation);
        }
        
        for (final FeatureInfoUtilities.Sample entry : results) {
            if (Double.isFinite(entry.value())) {
                final SampleDimension sd = entry.description();
                properties.put(
                        sd.getName().tip().toString(),
                        entry.value()
                );
            } else {
                // Could no-data reach this point ?
                LOGGER.log(Level.FINE, "Ignoring non finite value");
            }
        }
        GeoJSONGeometry point = null;
        if (searchPoint != null) {
            properties.put("x", searchPoint.getCoordinate(0));
            properties.put("y", searchPoint.getCoordinate(1));
            point = GeoJSONGeometry.toGeoJSONGeometry(GF.createPoint(new Coordinate(searchPoint.getCoordinate(0), searchPoint.getCoordinate(1))));
        }
        GeoCoverageInfo target = new GeoCoverageInfo(layerName.getLocalPart(), properties, point);
        return target;
    }
}
