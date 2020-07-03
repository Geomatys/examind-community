/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2019 Geomatys.
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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.StdConverter;
import java.awt.Rectangle;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridRoundingMode;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.math.Statistics;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.crs.DefaultCompoundCRS;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.Resource;
import org.geotoolkit.display.PortrayalException;
import org.geotoolkit.display2d.service.CanvasDef;
import org.geotoolkit.display2d.service.SceneDef;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.geometry.jts.JTSEnvelope2D;
import org.geotoolkit.map.MapLayer;
import org.geotoolkit.ows.xml.GetFeatureInfo;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class CoverageProfileInfoFormat extends AbstractFeatureInfoFormat {

    private static final String MIME = "application/json; subtype=profile";
    private static final String PARAM_PROFILE = "profile";
    private static final String PARAM_NBPOINT = "samplingCount";
    private static final String PARAM_ALTITUDE = "alt";
    private static final String PARAM_REDUCER = "reducer";

    private static final Map<Unit,List<Unit>> UNIT_GROUPS = new HashMap<>();
    static {
        final List<Unit> tempUnits = new ArrayList<>();
        tempUnits.add(Units.CELSIUS);
        tempUnits.add(Units.FAHRENHEIT);
        tempUnits.add(Units.KELVIN);

        final List<Unit> pressUnits = new ArrayList<>();
        pressUnits.add(Units.BAR);
        pressUnits.add(Units.BAR.multiply(14.503773773));
        pressUnits.add(Units.PASCAL);

        final List<Unit> speedUnits = new ArrayList<>();
        speedUnits.add(Units.METRES_PER_SECOND);
        speedUnits.add(Units.METRES_PER_SECOND.divide(1000));

        for (Unit u : tempUnits) UNIT_GROUPS.put(u, tempUnits);
        for (Unit u : pressUnits) UNIT_GROUPS.put(u, pressUnits);
        for (Unit u : speedUnits) UNIT_GROUPS.put(u, speedUnits);
    }

    @Override
    public Object getFeatureInfo(SceneDef sdef, CanvasDef cdef, Rectangle searchArea, GetFeatureInfo getFI) throws PortrayalException {

        //extract parameters : profile geometry and point count
        String geomStr = null;
        Integer samplingCount = null;
        if (getFI instanceof org.geotoolkit.wms.xml.GetFeatureInfo) {
            Object parameters = ((org.geotoolkit.wms.xml.GetFeatureInfo) getFI).getParameters();
            if (parameters instanceof Map) {
                Object cdt = ((Map) parameters).get(PARAM_PROFILE);
                if (cdt instanceof String){
                    geomStr = (String) cdt;
                } else if (cdt instanceof String[]) {
                    geomStr = ((String[]) cdt)[0];
                }

                Object cdt2 = ((Map) parameters).get(PARAM_NBPOINT);
                if (cdt2 instanceof String[]) {
                    cdt2 = ((String[]) cdt2)[0];
                }
                if (cdt2 instanceof String) {
                    try {
                        samplingCount = Double.valueOf((String) cdt2).intValue();
                    } catch (NumberFormatException ex) {
                        throw new PortrayalException(ex.getMessage(), ex);
                    }
                } else if (cdt2 instanceof Number) {
                    samplingCount = ((Number) cdt2).intValue();
                }
            }
        }

        if (geomStr == null) throw new PortrayalException("Missing PROFILE geometry parameter.");
        final WKTReader reader = new WKTReader();
        Geometry geom;
        try {
            geom = reader.read(geomStr);
        } catch (ParseException ex) {
            throw new PortrayalException(ex.getMessage(), ex);
        }
        if (!(geom instanceof LineString || geom instanceof Point)) {
            throw new PortrayalException("PROFILE geometry parameter must be a point or a LineString.");
        }

        //geometry is in view crs
        final CoordinateReferenceSystem geomCrs = CRS.getHorizontalComponent(cdef.getEnvelope().getCoordinateReferenceSystem());
        geom.setUserData(geomCrs);

        final Profile profil = new Profile();

        for (MapLayer layer : sdef.getContext().layers()) {
            Resource resource = layer.getResource();
            if (resource instanceof GridCoverageResource) {
                final GridCoverageResource ressource = (GridCoverageResource) resource;
                try {
                    final ProfilLayer l = extract(cdef, getFI, geom, ressource, samplingCount);
                    l.name = layer.getName();
                    if (l.name == null) {
                           l.name = ressource.getIdentifier()
                                   .orElseThrow(() -> new PortrayalException("resource identifier not present")).tip().toString();
                    }
                    profil.layers.add(l);
                } catch (TransformException | DataStoreException | FactoryException ex) {
                    throw new PortrayalException(ex.getMessage(), ex);
                }
            }
        }
        return profil;
    }

    @Override
    public List<String> getSupportedMimeTypes() {
        return Collections.singletonList(MIME);
    }

    private ProfilLayer extract(CanvasDef cdef, GetFeatureInfo getFI, Geometry geom, GridCoverageResource resource, Integer samplingCount) throws TransformException, FactoryException, DataStoreException {

        final ProfilLayer layer = new ProfilLayer();
        tryAddMetadata(layer, resource);

        final ProfilData baseData;
        try {
            //build temporal and vertical slice
            final JTSEnvelope2D geomEnv = JTS.toEnvelope(geom);
            final Envelope venv = cdef.getEnvelope();
            final CoordinateReferenceSystem vcrs = venv.getCoordinateReferenceSystem();
            final TemporalCRS vtcrs = CRS.getTemporalComponent(vcrs);
            final VerticalCRS vacrs = CRS.getVerticalComponent(vcrs, true);

            final GridGeometry gridGeometry = resource.getGridGeometry();
            final CoordinateReferenceSystem ccrs = gridGeometry.getCoordinateReferenceSystem();
            final TemporalCRS ctcrs = CRS.getTemporalComponent(ccrs);
            final VerticalCRS cacrs = CRS.getVerticalComponent(ccrs, true);

            Double time = null;
            Double alti = null;
            TemporalCRS ftcrs = null;
            VerticalCRS facrs = null;
            if (vtcrs == null) {
                //pick first coverage temporal slice
                if (ctcrs != null) {
                    ftcrs = ctcrs;
                    final Envelope tenv = Envelopes.transform(gridGeometry.getEnvelope(), ctcrs);
                    time = tenv.getMaximum(0);
                }
            } else {
                //extract user requested time
                ftcrs = vtcrs;
                final Envelope tenv = Envelopes.transform(venv, vtcrs);
                time = tenv.getMedian(0);
            }

            if (geom instanceof LineString) {
                //extract altitude parameter
                String altStr = null;
                if (getFI instanceof org.geotoolkit.wms.xml.GetFeatureInfo) {
                    Object parameters = ((org.geotoolkit.wms.xml.GetFeatureInfo) getFI).getParameters();
                    if (parameters instanceof Map) {
                        Object cdt = ((Map) parameters).get(PARAM_ALTITUDE);
                        if (cdt instanceof String){
                            altStr = (String) cdt;
                        } else if (cdt instanceof String[]) {
                            altStr = ((String[]) cdt)[0];
                        }
                    }
                }

                if (altStr != null) {
                    if (cacrs != null) {
                        facrs = cacrs;
                        alti = Double.parseDouble(altStr);
                    }
                } else if (vacrs == null) {
                    //pick first coverage altitude slice
                    if (altStr != null) {
                        facrs = CommonCRS.Vertical.ELLIPSOIDAL.crs();
                        alti = Double.parseDouble(altStr);
                    } else if (cacrs != null) {
                        facrs = cacrs;
                        final Envelope aenv = Envelopes.transform(gridGeometry.getEnvelope(), cacrs);
                        alti = aenv.getMaximum(0);
                    }
                } else {
                    //extract user requested altitude
                    facrs = vacrs;
                    final Envelope aenv = Envelopes.transform(venv, vacrs);
                    alti = aenv.getMedian(0);
                }
            }

            final GeneralEnvelope workEnv;
            if (time == null && alti == null) {
                workEnv = new GeneralEnvelope(geomEnv);
            } else if (alti == null) {
                final Map props = new HashMap();
                props.put("name", "2d+t");
                final CoordinateReferenceSystem crs = new DefaultCompoundCRS(props, geomEnv.getCoordinateReferenceSystem(), ftcrs);
                workEnv = new GeneralEnvelope(crs);
                workEnv.setRange(0, geomEnv.getMinimum(0), geomEnv.getMaximum(0));
                workEnv.setRange(1, geomEnv.getMinimum(1), geomEnv.getMaximum(1));
                workEnv.setRange(2, time, time);
            } else if (time == null) {
                final Map props = new HashMap();
                props.put("name", "2d+a");
                final CoordinateReferenceSystem crs = new DefaultCompoundCRS(props, geomEnv.getCoordinateReferenceSystem(), facrs);
                workEnv = new GeneralEnvelope(crs);
                workEnv.setRange(0, geomEnv.getMinimum(0), geomEnv.getMaximum(0));
                workEnv.setRange(1, geomEnv.getMinimum(1), geomEnv.getMaximum(1));
                workEnv.setRange(2, alti, alti);
            } else {
                final Map props = new HashMap();
                props.put("name", "2d+t+a");
                final CoordinateReferenceSystem crs = new DefaultCompoundCRS(props, geomEnv.getCoordinateReferenceSystem(), ftcrs, facrs);
                workEnv = new GeneralEnvelope(crs);
                workEnv.setRange(0, geomEnv.getMinimum(0), geomEnv.getMaximum(0));
                workEnv.setRange(1, geomEnv.getMinimum(1), geomEnv.getMaximum(1));
                workEnv.setRange(2, time, time);
                workEnv.setRange(3, alti, alti);
            }

            final GridCoverage coverage = readCoverage(resource, workEnv);
            Object parameters = ((org.geotoolkit.wms.xml.GetFeatureInfo) getFI).getParameters();
            ReductionMethod reducer = null;
            if (parameters instanceof Map) {
                String reduceParam = null;
                Object cdt = ((Map) parameters).get(PARAM_REDUCER);
                if (cdt instanceof String){
                    reduceParam = (String) cdt;
                } else if (cdt instanceof String[]) {
                    reduceParam = ((String[]) cdt)[0];
                }
                if (reduceParam != null) {
                    reducer = ReductionMethod.valueOf(reduceParam);
                }
            }
            baseData = extractData(coverage, geom, samplingCount, reducer);

        } catch (DataStoreException ex) {
            layer.message = ex.getMessage();
            return layer;
        }

        //convert data in different units
        final List<Unit> group = UNIT_GROUPS.get(baseData.getUnit());
        if (group != null) {
            for (Unit u : group) {
                if (u.equals(baseData.getUnit())) {
                    layer.data.add(baseData);
                    continue;
                }
                //create converted datas
                final ProfilData data = new ProfilData();
                final Statistics stats = new Statistics("");
                final UnitConverter converter = baseData.getUnit().getConverterTo(u);

                for (XY xy : baseData.points) {
                    final XY c = new XY(xy.x, xy.y);
                    if (geom instanceof Point) {
                        c.x = converter.convert(c.x);
                        stats.accept(c.x);
                    } else {
                        c.y = converter.convert(c.y);
                        stats.accept(c.y);
                    }
                    data.points.add(c);
                }

                data.unit = u;
                data.min = stats.minimum();
                data.max = stats.maximum();
                layer.data.add(data);
            }
        } else {
            layer.data.add(baseData);
        }


        return layer;
    }

    private void tryAddMetadata(ProfilLayer layer, GridCoverageResource resource) {
        try {
            final Metadata md = resource.getMetadata();
            final List<String> titles = md.getIdentificationInfo().stream()
                    .map(id -> id.getCitation())
                    .filter(Objects::nonNull)
                    .map(Citation::getTitle)
                    .filter(Objects::nonNull)
                    .map(Objects::toString)
                    .collect(Collectors.toList());
            if (!titles.isEmpty()) layer.setTitles(titles);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Cannot extract title from layer metadata", e);
        }
    }

    private ProfilData extractData(GridCoverage coverage, Geometry geom, Integer samplingCount, ReductionMethod reducer) throws TransformException, FactoryException {

        final ProfilData pdata = new ProfilData();

        final CoordinateReferenceSystem crs = coverage.getCoordinateReferenceSystem();
        final GridGeometry gridGeometry     = coverage.getGridGeometry();
        final GridEnvelope extent           = gridGeometry.getExtent();
        final MathTransform gridToCrs       = gridGeometry.getGridToCRS(PixelInCell.CELL_CENTER);
        final List<SampleDimension> samples = coverage.getSampleDimensions();

        //build axes informations
        final int dim = crs.getCoordinateSystem().getDimension();
        final long[] lowsI = extent.getLow().getCoordinateValues();
        final double[] lowsD = new double[dim];
        for (int i = 0;i < dim; i++) lowsD[i] = lowsI[i];
        final double[] gridPt = new double[extent.getDimension()];
        final double[] crsPt = new double[extent.getDimension()];
        final List<Axe> axes = new ArrayList<>();
        Integer altiIdx = null;
        for (int i = 2;i < dim; i++) {
            final CoordinateSystemAxis axis = crs.getCoordinateSystem().getAxis(i);
            System.arraycopy(lowsD, 0, gridPt, 0, dim);
            final double[] range = new double[(int) extent.getSize(i)];
            for (int k = 0, kg = (int) extent.getLow(k); k < range.length; k++, kg++) {
                gridPt[i] = kg;
                gridToCrs.transform(gridPt, 0, crsPt, 0, 1);
                range[k] = crsPt[i];
            }
            final Unit<?> unit = axis.getUnit();

            final Axe axe = new Axe();
            axe.name = axis.getName().toString();
            axe.direction = axis.getDirection().name();
            if (unit != null) axe.unit = unit;
            axe.range = range;
            axes.add(axe);

            if (axis.getDirection().equals(AxisDirection.UP) || axis.getDirection().equals(AxisDirection.DOWN)) {
                altiIdx = i-2;
            }
        }
        final int ALTIIDX = altiIdx == null ? -1 : altiIdx;

        //build sample dimension informations
        final Band[] bands = samples.stream()
                .map(Band::new)
            //limit to first band, request by david/mehdi
                .limit(1)
                .toArray(size -> new Band[size]);

        final boolean isPoint = geom instanceof Point;
        if (isPoint) {
            final Object userData = geom.getUserData();
            final Coordinate[] coords = geom.getCoordinates();
            geom = new GeometryFactory().createLineString(new Coordinate[]{coords[0], coords[0]});
            geom.setUserData(userData);
        }

        final DataProfile dp = new DataProfile(coverage, (LineString) geom);

        final Statistics stats = new Statistics("");

        if (isPoint) {
            StreamSupport.stream(dp, false).forEach(new Consumer<DataProfile.DataPoint>() {
                @Override
                public void accept(DataProfile.DataPoint t) {
                    Object value = t.value;

                    // due to NPE if we click outside the data domain see jira issue SDMS-313
                    if (value == null) {
                        return;
                    }

                    //unpack first band
                    value = Array.get(value, 0);

                    final List<Double> values = new ArrayList<>();
                    //unpack dimensions
                    final int as = axes.size();

                    //lazy, we expect only time and alti dimensions.
                    if (as == 0) {
                        extractValues(value, values);
                    } else if (as == 1) {
                        if (ALTIIDX == 0) {
                            //pick all
                            extractValues(value, values);
                        } else {
                            //pick first
                            value = Array.get(value, 0);
                            extractValues(value, values);
                        }
                    } else if (as == 2) {
                        if (ALTIIDX == 0) {
                            //pick first - alti
                            for (int i=0,n=Array.getLength(value);i<n;i++) {
                                Object sub = Array.get(value, i);
                                //pick first - not alti
                                sub = Array.get(sub, 0);
                                extractValues(sub, values);
                            }
                        } else if (ALTIIDX == 1) {
                            //pick first - not alti
                            value = Array.get(value, 0);
                            //pick all - alti
                            extractValues(value, values);
                        } else {
                            //pick first - not alti
                            value = Array.get(value, 0);
                            //pick first - not alti
                            value = Array.get(value, 0);
                            extractValues(value, values);
                        }
                    }

                    for (Double d : values) {
                        stats.accept(d);
                    }

                    if (ALTIIDX >= 0) {
                        final Axe axe = axes.get(ALTIIDX);
                        for (int i=0,n=values.size();i<n;i++) {
                            pdata.points.add(new XY(values.get(i), axe.range[i]));
                        }
                    } else {
                        pdata.points.add(new XY(0, values.get(0)));
                    }
                }
            });

        } else {
            double[] d = new double[1];
            StreamSupport.stream(dp, false).forEach(new Consumer<DataProfile.DataPoint>() {
                @Override
                public void accept(DataProfile.DataPoint t) {
                    Object value = t.value;
                    d[0] += t.distanceFromPrevious / 1000.0;
                    if (value != null) {
                        final double distancekm = d[0];

                        //unpack first band
                        value = Array.get(value, 0);

                        //pick first value
                        while (value.getClass().isArray()) {
                            value = Array.get(value, 0);
                        }

                        double num = ((Number)value).doubleValue();
                        stats.accept(num);
                        pdata.points.add(new XY(distancekm, num));
                    }
                }
            });
        }

        pdata.points = reduce(pdata.points, samplingCount == null ? pdata.points.size() : samplingCount, reducer);

        pdata.setUnit(bands[0].getUnit());
        pdata.min = stats.minimum();
        pdata.max = stats.maximum();
        return pdata;
    }

    /**
     * Reduce list to number of requested points.
     * At least first and last points will be preserved.
     * Decimate points trying to preserve a regular distance
     *
     * @param lst list to decimate
     * @param samplingCount
     * @return
     */
    static List<XY> reduce(List<XY> lst, int samplingCount) {
        return reduce(lst, samplingCount, ReductionMethod.NEAREST);
    }

    static List<XY> reduce(List<XY> lst, int samplingCount, ReductionMethod reductionStrategy) {
        if (reductionStrategy == null) reductionStrategy = ReductionMethod.AVG;
        switch (reductionStrategy) {
            case NEAREST: return reduceNearest(lst, samplingCount);
            case MIN    : return reduce(lst, samplingCount, CoverageProfileInfoFormat::minReduction);
            case MAX    : return reduce(lst, samplingCount, CoverageProfileInfoFormat::maxReduction);
            default     : return reduce(lst, samplingCount, CoverageProfileInfoFormat::avgReduction);
        }
    }

    private static XY minReduction(List<XY> values) {
        final DoubleBinaryOperator min = nanOp(Math::min);
        final double[] reduced = values.stream()
                .reduce(
                        new double[]{ 0, 0, Double.NaN }, // 0: nb points ; 1: sum of X ; 2: minimal y
                        (dd, xy) -> { dd[0]++        ; dd[1] += xy.x  ; dd[2] = min.applyAsDouble(dd[2], xy.y)  ; return dd; },
                        (d1, d2) -> { d1[0] += d2[0] ; d1[1] += d2[1] ; d1[2] = min.applyAsDouble(d1[2], d2[2]) ; return d1; }
                );
        return new XY(reduced[1] / reduced[0], reduced[2]);
    }

    private static XY maxReduction(List<XY> values) {
        final DoubleBinaryOperator max = nanOp(Math::max);
        final double[] reduced = values.stream()
                .reduce(
                        new double[]{ 0, 0, Double.NEGATIVE_INFINITY }, // 0: nb points ; 1: sum of X ; 2: maximal y
                        (dd, xy) -> { dd[0]++        ; dd[1] += xy.x  ; dd[2] = max.applyAsDouble(dd[2], xy.y)  ; return dd; },
                        (d1, d2) -> { d1[0] += d2[0] ; d1[1] += d2[1] ; d1[2] = max.applyAsDouble(d1[2], d2[2]) ; return d1; }
                );
        return new XY(reduced[1] / reduced[0], reduced[2]);
    }

    private static XY avgReduction(List<XY> values) {
        final DoubleBinaryOperator sum = nanOp((d1, d2) -> d1 + d2);
        final double[] reduced = values.stream()
                .reduce(
                        new double[3], // 0: nb points ; 1: sum of X ; 2: sum of y
                        (dd, xy) -> { dd[0]++        ; dd[1] += xy.x  ; dd[2] = sum.applyAsDouble(dd[2], xy.y)  ; return dd; },
                        (d1, d2) -> { d1[0] += d2[0] ; d1[1] += d2[1] ; d1[2] = sum.applyAsDouble(d1[2], d2[2]) ; return d1; }
                );
        return new XY(reduced[1] / reduced[0], reduced[2] / reduced[0]);
    }

    /**
     * Decorate given double operation, but also ignore any NaN value present
     * @param base
     * @return
     */
    private static DoubleBinaryOperator nanOp(DoubleBinaryOperator base) {
        return (d1, d2) -> Double.isNaN(d1) ? d2 : Double.isNaN(d2) ? d1 : base.applyAsDouble(d1, d2);
    }

    /**
     * HACK : quick fix to mimic advanced reduction algorithms. Little things to know:
     * <ul>
     *     <li>First and last points are preserved</li>
     *     <li>Overlap behavior : to simplify algorithm, window management is approximative</li>
     * </ul>
     * @param datasource
     * @param samplingCount
     * @param reducer
     * @return
     */
    static List<XY> reduce(List<XY> datasource, int samplingCount, Function<List<XY>, XY> reducer) {
        final int sourceSize = datasource.size();
        // HACK: for reduction to make sense, we want at least a central point with 2 edges
        final int ptsPerWindow = Math.max(3, Math.round(sourceSize / (float) samplingCount));
        final int outSamplingCount = sourceSize / ptsPerWindow;
        final List<XY> reduced = IntStream.rangeClosed(1, outSamplingCount)
                .mapToObj(ptIdx -> datasource.subList(ptsPerWindow * ptIdx - ptsPerWindow, ptsPerWindow * ptIdx))
                .map(reducer)
                .collect(Collectors.toList());
        reduced.add(0, datasource.get(0));
        reduced.add(datasource.get(sourceSize - 1));
        return reduced;
    }

    static List<XY> reduceNearest(List<XY> lst, int samplingCount) {
        final int sourceSize = lst.size();
        if (sourceSize <= samplingCount) return lst;
        final List<BiSegment> prepared = new ArrayList<>();

        final BiSegment first = new BiSegment(lst.get(0));
        final BiSegment last = new BiSegment(lst.get(sourceSize -1));
        BiSegment previous = null;
        for (int i = 1, n = sourceSize -1; i < n ; i++) {
            final BiSegment bis = new BiSegment(lst.get(i));
            if (previous != null) {
                bis.updatePrevious(previous);
            }
            previous = bis;
            prepared.add(bis);
        }
        prepared.get(0).updatePrevious(first);
        prepared.get(prepared.size()-1).updateNext(last);

        final TreeSet<BiSegment> distanceSort = new TreeSet<>(prepared);

        //remove segments until we have the wanted count
        samplingCount -= 2; //reduce by two we are working with segments, first and last points are not in the Set.
        while (distanceSort.size() > samplingCount && !distanceSort.isEmpty()) {
            final BiSegment bis = distanceSort.pollFirst();
            final BiSegment nextb = bis.next;
            final BiSegment prevb = bis.prev;
            distanceSort.remove(nextb);
            distanceSort.remove(prevb);

            //update next segment
            nextb.updatePrevious(prevb);
            if (!nextb.isEdge()) {
                distanceSort.add(nextb);
            }

            //update previous segment
            prevb.updateNext(nextb);
            if (!prevb.isEdge()) {
                distanceSort.add(prevb);
            }
        }

        //rebuild list
        BiSegment seg = first;
        lst = new ArrayList<>();
        while (seg != null) {
            lst.add(seg.xy);
            seg = seg.next;
        }

        return lst;
    }

    private static void extractValues(Object value, List<Double> values) {
        if (value instanceof Number) {
            values.add( ((Number) value).doubleValue() );
        } else {
            for (int i = 0, n = Array.getLength(value); i < n; i++) {
                Object sub = Array.get(value, i);
                extractValues(sub, values);
            }
        }
    }

    private static GridCoverage readCoverage(GridCoverageResource resource, Envelope work)
            throws TransformException, DataStoreException {

        //ensure envelope is no flat
        final GeneralEnvelope workEnv = new GeneralEnvelope(work);
        if (workEnv.isEmpty()) {
            if (workEnv.getSpan(0) <= 0.0) {
                double buffer = workEnv.getSpan(1) / 100.0;
                if (buffer <= 0.0) buffer = 0.00001;
                workEnv.setRange(0, workEnv.getLower(0)-buffer, workEnv.getLower(0)+buffer);
            }
            if (workEnv.getSpan(1) <= 0.0) {
                double buffer = workEnv.getSpan(0) / 100.0;
                if (buffer <= 0.0) buffer = 0.00001;
                workEnv.setRange(1, workEnv.getLower(1)-buffer, workEnv.getLower(1)+buffer);
            }
        }

        GridGeometry gg = resource.getGridGeometry().derive().rounding(GridRoundingMode.ENCLOSING).subgrid(workEnv).build();
        return resource.read(gg).forConvertedValues(true);
    }

    public static class Band {

        public String name;
        public Unit unit;

        Band() {}

        Band(SampleDimension source) {
            name = source.getName().toString();
            unit= source.getUnits().orElse(null);
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @JsonSerialize(converter = UnitSerializer.class)
        public Unit getUnit() {
            return unit;
        }

        @JsonDeserialize(converter = UnitDeSerializer.class)
        public void setUnit(Unit unit) {
            this.unit = unit;
        }
    }

    public static class Axe {
        public String name;
        public String direction;
        public Unit unit;
        public double[] range;

        @JsonSerialize(converter = UnitSerializer.class)
        public Unit getUnit() {
            return unit;
        }

        @JsonDeserialize(converter = UnitDeSerializer.class)
        public void setUnit(Unit unit) {
            this.unit = unit;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDirection() {
            return direction;
        }

        public void setDirection(String direction) {
            this.direction = direction;
        }

        public double[] getRange() {
            return range;
        }

        public void setRange(double[] range) {
            this.range = range;
        }
    }

    public static class Profile {
        public List<ProfilLayer> layers = new ArrayList<>();
    }

    public static class ProfilLayer {

        public String name;
        private List<String> titles;
        public List<ProfilData> data = new ArrayList<>();
        public String message;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<String> getTitles() {
            return titles;
        }

        public void setTitles(List<String> titles) {
            this.titles = titles;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public List<ProfilData> getData() {
            return data;
        }

        public void setData(List<ProfilData> data) {
            this.data = data;
        }
    }

    public static class ProfilData {

        private Unit unit;
        private double min;
        private double max;
        public List<XY> points = new ArrayList<>();

        @JsonSerialize(converter = UnitSerializer.class)
        public Unit getUnit() {
            return unit;
        }

        @JsonDeserialize(converter = UnitDeSerializer.class)
        public void setUnit(Unit unit) {
            this.unit = unit;
        }

        public double getMin() {
            return min;
        }

        public void setMin(double min) {
            this.min = min;
        }

        public double getMax() {
            return max;
        }

        public void setMax(double max) {
            this.max = max;
        }
    }

    public static class XY {
        public double x;
        public double y;

        public XY() {}

        public XY(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double getX() {
            return x;
        }

        public void setX(double x) {
            this.x = x;
        }

        public double getY() {
            return y;
        }

        public void setY(double y) {
            this.y = y;
        }

        @Override
        public String toString() {
            return "[" + x +" " + y + "]";
        }
    }

    /**
     * Used for point decimation only.
     */
    private static class BiSegment implements Comparable<BiSegment> {
        XY xy;
        double distanceIfRemoved;
        BiSegment prev;
        BiSegment next;

        public BiSegment(XY current) {
            this.xy = current;
        }

        public boolean isEdge() {
            return prev == null || next == null;
        }

        public void updatePrevious(BiSegment previousSegment) {
            if (this.prev == previousSegment) return;
            this.prev = previousSegment;
            updateDistance();
            previousSegment.updateNext(this);
        }

        public void updateNext(BiSegment nextSegment) {
            if (this.next == nextSegment) return;
            this.next = nextSegment;
            updateDistance();
            nextSegment.updatePrevious(this);
        }

        private void updateDistance() {
            if (prev != null && next != null) {
                this.distanceIfRemoved = next.xy.x - prev.xy.x;
            } else {
                this.distanceIfRemoved = Double.POSITIVE_INFINITY;
            }
        }

        @Override
        public int compareTo(BiSegment o) {
            int cmp = Double.compare(distanceIfRemoved, o.distanceIfRemoved);
            if (cmp == 0) {
                //we need to have all segment different otherwise the TreeMap will remove entries
                //use distance, it is always increasing
                return Double.compare(xy.x, o.xy.x);
            }
            return cmp;
        }

        @Override
        public String toString() {
            return ((prev == null) ? "null" : prev.xy) + " -> " + xy + " -> " + ((next == null) ? "null" : next.xy) + " d:" + distanceIfRemoved;
        }

    }

    public enum ReductionMethod {
        NEAREST, MIN, MAX, AVG
    }

    private static class UnitSerializer extends StdConverter<Unit, String> {

        @Override
        public String convert(Unit unit) {
            return unit == null? null : unit.getSymbol();
        }
    }

    private static class UnitDeSerializer extends StdConverter<String, Unit> {

        @Override
        public Unit convert(String symbol) {
            return symbol == null ? null : Units.valueOf(symbol);
        }
    }
}
