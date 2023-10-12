package org.constellation.map.layerstats;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.util.concurrent.AtomicDouble;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.Resource;
import org.apache.sis.util.ArgumentChecks;
import org.constellation.exception.ConstellationException;
import org.constellation.json.binding.*;
import org.locationtech.jts.geom.*;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.PropertyNotFoundException;
import org.opengis.filter.Filter;
import org.opengis.style.Description;
import org.opengis.style.Rule;
import org.opengis.style.Style;
import org.opengis.util.InternationalString;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * @author Estelle Idée (Geomatys)
 */
public class LayerStatisticsUtils {
    private static final Logger LOGGER = Logger.getLogger("com.geomatys.ozhm.utils");

    private static LayerStatistics computeStatistics(final List<Rule> rules, final FeatureSet featureSet)
            throws ConstellationException {

        ArgumentChecks.ensureNonNull("rules", rules);
        ArgumentChecks.ensureNonNull("featureSet", featureSet);

        final LinkedHashMap<Rule, LayerStatisticsBucket> ruleStatsMap = new LinkedHashMap<>();
        rules.forEach(rule -> {
            // Extract the symbolizers to determine which one to use.
            // Priority order : POLYGON > LINE > POINT
            // Other symbolizers type are ignored.
            // If more than one symbolizer of the same type, then first one kept only.
            final List<PolygonSymbolizer> polygoneSymbs = new ArrayList<>();
            final List<LineSymbolizer> lineSymbs = new ArrayList<>();
            final List<PointSymbolizer> pointSymbs = new ArrayList<>();

            for (final org.opengis.style.Symbolizer symbolizer : rule.symbolizers()) {
                if (symbolizer instanceof org.opengis.style.PointSymbolizer ps) {
                    pointSymbs.add(new PointSymbolizer(ps));
                } else if (symbolizer instanceof org.opengis.style.LineSymbolizer ls) {
                    lineSymbs.add(new LineSymbolizer(ls));
                } else if (symbolizer instanceof org.opengis.style.PolygonSymbolizer ps) {
                    polygoneSymbs.add(new PolygonSymbolizer(ps));
                }
            }

            LayerStatistics.StatisticsType type;
            String color;
            String series = null;
            String strokeWidth = null;
            String size = null;
            String strokeColor = null;
            if (!polygoneSymbs.isEmpty()) {
                final Fill fill = polygoneSymbs.get(0).getFill();
                color = fill.getColor();
                type = LayerStatistics.StatisticsType.POLYGON;
            } else if (!lineSymbs.isEmpty()) {
                final Stroke stroke = lineSymbs.get(0).getStroke();
                color = stroke.getColor();
                strokeWidth = stroke.getWidth();
                strokeColor = stroke.getColor();
                type = LayerStatistics.StatisticsType.LINE;
            } else if (!pointSymbs.isEmpty()) {
                final Graphic graphic = pointSymbs.get(0).getGraphic();
                final Mark mark = graphic.getMark();
                final Fill fill = mark.getFill();
                color = fill.getColor();
                size = graphic.getSize();
                final Stroke stroke = mark.getStroke();
                strokeWidth = stroke.getWidth();
                strokeColor = stroke.getColor();
                type = LayerStatistics.StatisticsType.SIMPLE;
            } else {
                // If there is no symbolizer then the rule is skipped from the statistics calulations.
                return;
            }
            final Description description = rule.getDescription();
            if (description != null) {
                final InternationalString title = description.getTitle();
                if (title != null) {
                    series = title.toString();
                }
            } else {
                series = rule.getName();
            }
            ruleStatsMap.put(rule, new LayerStatisticsBucket(type, color, series, size, strokeColor, strokeWidth));
        });

        final AtomicDouble totalSurface = new AtomicDouble();
        final AtomicDouble totalLength = new AtomicDouble();
        final AtomicLong totalPonctual = new AtomicLong();
        final AtomicLong totalCount = new AtomicLong();
        try (Stream<Feature> features = featureSet.features(false)) {
            features.forEach(feature -> {
                updateBucket(feature, totalSurface, totalLength, totalPonctual, totalCount, ruleStatsMap);
            });
        } catch (DataStoreException dse) {
            throw new ConstellationException("Error while accessing featureSet features.", dse);
        } catch (RuntimeException re) {
            throw new ConstellationException("Error while computing area from property \"the_geom\"", re);
        }

        return new LayerStatistics(ruleStatsMap.values(), totalSurface.get(), totalLength.get(), totalPonctual.get(), totalCount.get());
    }

    public static String computeStatisticsForLayerWithStyle(final Resource dataPOrigin, final Style style) throws ConstellationException {
        ArgumentChecks.ensureNonNull("dataOrigin", dataPOrigin);
        ArgumentChecks.ensureNonNull("style", style);
        if (dataPOrigin instanceof FeatureSet featureSet) {

            final List<Rule> rules = new ArrayList<>();
            style.featureTypeStyles().forEach(fts -> rules.addAll(fts.rules()));
            final LayerStatistics statistics = computeStatistics(rules, featureSet);

            try {
                final ObjectMapper mapper = getMapper();
                return mapper.writeValueAsString(statistics);

            } catch (JsonProcessingException e) {
                throw new ConstellationException("Error while serializing statistics to String.");
            }
        } else {
            LOGGER.warning("Type of resource not supported yet in statistics computation : " + dataPOrigin.getClass());
            return null;
        }
    }

    private static Object[] getGeom(final Feature feature) {
        ArgumentChecks.ensureNonNull("feature", feature);
        final Property theGeomProp;
        try {
            theGeomProp = feature.getProperty("the_geom");
        } catch (PropertyNotFoundException e) {
            LOGGER.warning("No property \"the_geom\" found in feature " + feature.getProperty("name"));
            return null;
        }
        final Object theGeom = theGeomProp.getValue();
        if (theGeom instanceof MultiPolygon multiPolygon) {
            return new Object[]{multiPolygon.getArea(), GeomType.POLYGON};
        } else if (theGeom instanceof Polygon polygon) {
            return new Object[]{polygon.getArea(), GeomType.POLYGON};
        } else if (theGeom instanceof MultiLineString multiLine) {
            return new Object[]{multiLine.getLength(), GeomType.LINE};
        } else if (theGeom instanceof LineString line) {
            return new Object[]{line.getLength(), GeomType.LINE};
        } else if (theGeom instanceof MultiPoint || theGeom instanceof Point) {
            return new Object[]{1L, GeomType.POINT};
        } else {
            LOGGER.warning("Type of the_geom not supported in statistics computation : " + theGeom.getClass());
            return null;
        }
    }

    static ObjectMapper getMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        return mapper;
    }

    static void updateBucket(final Feature feature, AtomicDouble totalSurface, AtomicDouble totalLength, final AtomicLong totalPonctual, final AtomicLong totalCount, final LinkedHashMap<Rule, LayerStatisticsBucket> ruleStatsMap) {
        ArgumentChecks.ensureNonNull("feature", feature);
        totalCount.incrementAndGet();

        final Object[] valueAndType = getGeom(feature);
        final Object typeo = valueAndType[1];

        if (typeo == null) return;
        GeomType type = (GeomType) typeo;
        Object value = valueAndType[0];
        switch (type) {
            case POLYGON -> totalSurface.addAndGet((Double) value);
            case LINE -> totalLength.addAndGet((Double) value);
            case POINT -> totalPonctual.addAndGet((Long) value);
        }

        // If several filters match -> feature info will be added to more than one StatisticsBucket.
        // Style shall be created to respect the rule : each feature shall match with only one rule/filter.
        ruleStatsMap.forEach((rule, stats) -> {
            final Filter filter = rule.getFilter();
            if (filter == null || filter.test(feature)) {
                switch (type) {
                    case POLYGON -> stats.updateSurface((Double) value);
                    case LINE -> stats.updateLength((Double) value);
                    case POINT -> stats.updatePointNb((Long) value);
                }
            }
        });
    }

    enum GeomType {
        POLYGON, LINE, POINT;
    }
}
