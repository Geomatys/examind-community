/*
 *    Examind Community - An open source and standard compliant SDI
 *    https://www.examind.com/en/examind-community-2/about/
 *
 * Copyright 2020 Geomatys.
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
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.logging.Level;

import org.apache.sis.storage.GridCoverageResource;
import org.constellation.map.featureinfo.dto.LayerError;
import org.opengis.feature.Feature;
import org.opengis.util.GenericName;

import org.apache.sis.coverage.SampleDimension;

import org.geotoolkit.display.PortrayalException;
import org.geotoolkit.display2d.canvas.RenderingContext2D;
import org.geotoolkit.display2d.primitive.SearchAreaJ2D;
import org.geotoolkit.display2d.service.CanvasDef;
import org.geotoolkit.display2d.service.SceneDef;
import org.geotoolkit.ows.xml.GetFeatureInfo;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.constellation.api.DataType;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.map.featureinfo.dto.CoverageInfo;
import org.constellation.map.featureinfo.dto.FeatureInfo;
import org.constellation.map.featureinfo.dto.LayerInfo;
import org.constellation.ws.LayerCache;
import org.constellation.ws.MimeType;

/**
 * Create a list of {@link CoverageInfo} and/or {@link FeatureInfo}. The output is the serialized JSON
 * text representation of it.
 *
 * Notes:
 * <ul>
 *     <li>must be used only ONCE</li>
 *     <li>
 *         Use custom Jackson serializers for proper management of:
 *         <ul>
 *             <li>{@link EnvelopeSerializer Envelopes}</li>
 *             <li>{@link JTSSerializer JTS Geometries (as WKT)}</li>
 *             <li>{@link FeatureSerializer Feature association}</li>
 *         </ul>
 *     </li>
 * </ul>
 *
 * @author Alexis Manin (Geomatys)
 */
public class JSONFeatureInfoFormat extends AbstractTextFeatureInfoFormat {

    private static final ObjectWriter WRITER;
    static {
        WRITER = new ObjectMapper()
                .registerModule(new SimpleModule(
                        "GetFeatureInfo_JSON",
                        Version.unknownVersion(),
                        Arrays.asList(new FeatureSerializer(), new JTSSerializer(), new EnvelopeSerializer()))
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

        if (infoQueue.stream().allMatch(LayerError.class::isInstance)) {
            final PortrayalException err = new PortrayalException("None of requested layers can be evaluated for input GetFeatureInfo query");
            for(LayerInfo info : infoQueue) err.addSuppressed(((LayerError)info).getError());
            throw err;
        }
        
        try (final ByteArrayOutputStream buffer = new ByteArrayOutputStream(8192)) {
            try (final OutputStreamWriter writer = new OutputStreamWriter(buffer, StandardCharsets.UTF_8);
                 final SequenceWriter sw = WRITER.writeValuesAsArray(writer);
            ) {
                sw.writeAll(infoQueue);
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot serialize GetFeatureInfo JSON", e);
        }
    }

    @Override
    public List<String> getSupportedMimeTypes() {
        return Arrays.asList(MimeType.APP_JSON, MimeType.APP_JSON_UTF8);
    }

    @Override
    protected void nextProjectedFeature(GenericName layerName, Feature candidate, RenderingContext2D context, SearchAreaJ2D queryArea) {
        final String layerNameStr = layerName.tip().toString();
        try {
            infoQueue.add(new FeatureInfo(layerNameStr, candidate));
        } catch (Exception e) {
            final LayerError err = new LayerError();
            err.setError(e);
            err.setLayer(layerNameStr);
            infoQueue.add(err);
        }
    }

    @Override
    protected void nextProjectedCoverage(GenericName layerName, GridCoverageResource resource, RenderingContext2D context, SearchAreaJ2D queryArea) {
        try {
            final List<FeatureInfoUtilities.Sample> results =
                    FeatureInfoUtilities.getCoverageValues(resource, context, queryArea);

            if (results == null || results.isEmpty()) return;

            final CoverageInfo info = buildCoverageInfo(layerName, getLayer(layerName, DataType.COVERAGE), gfi);
            fill(info, results);
            infoQueue.add(info);
        } catch (Exception e) {
            final LayerError err = new LayerError();
            err.setError(e);
            err.setLayer(layerName.tip().toString());
            infoQueue.add(err);
        }
    }

    protected static CoverageInfo buildCoverageInfo(final GenericName layerName, final Optional<LayerCache> layerO, final GetFeatureInfo gfi) {

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
        return new CoverageInfo(layerName.toString(), javaTime, elevation);
    }

    static void fill(CoverageInfo target, final List<FeatureInfoUtilities.Sample> results) {
        final List<CoverageInfo.Sample> outValues = target.getValues();
        for (final FeatureInfoUtilities.Sample entry : results) {
            if (Double.isFinite(entry.value())) {

                final SampleDimension sd = entry.description();
                outValues.add(new CoverageInfo.Sample(
                        sd.getName().tip().toString(),
                        entry.value(),
                        sd.getUnits().orElse(null)
                ));
            } else {
                // Could no-data reach this point ?
                LOGGER.log(Level.FINE, "Ignoring non finite value");
            }
        }
    }
}
