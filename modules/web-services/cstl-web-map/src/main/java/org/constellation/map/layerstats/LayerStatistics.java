package org.constellation.map.layerstats;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LayerStatistics {
    private Double totalSurface;
    private Double totalLength;
    private Long totalPonctual;
    private Long totalCount;
    private final List<StatisticsType> types = new ArrayList<>();
    private final List<String> series = new ArrayList<>();
    private final List<Double> surfaces = new ArrayList<>();
    private final List<Double> lengths = new ArrayList<>();
    private final List<Long> ponctuals = new ArrayList<>();
    private final List<Long> counts = new ArrayList<>();
    private final List<String> colors = new ArrayList<>();
    private final List<String> sizes = new ArrayList<>();
    private final List<String> strokeColors = new ArrayList<>();
    private final List<String> strokeWidths = new ArrayList<>();

    public LayerStatistics() {
    }

    public LayerStatistics(final Collection<LayerStatisticsBucket> statisticsBuckets, final Double totalSurface, final Double totalLength, final Long totalPonctual, final Long totalCount) {
        this.totalSurface = totalSurface;
        this.totalLength = totalLength;
        this.totalPonctual = totalPonctual;
        this.totalCount = totalCount;
        for (LayerStatisticsBucket bucket : statisticsBuckets) {
            final StatisticsType type = bucket.getType();
            this.types.add(type);
            this.series.add(bucket.getSeries());
            this.surfaces.add(bucket.getSurface());
            this.lengths.add(bucket.getLength());
            this.ponctuals.add(bucket.getPointNb());
            this.counts.add(bucket.getCount());
            this.colors.add(bucket.getColor());
            this.sizes.add(bucket.getSize());
            this.strokeColors.add(bucket.getStrokeColor());
            this.strokeWidths.add(bucket.getStrokeWidth());
        }
    }

    public enum StatisticsType {
        SIMPLE, LINE, POLYGON
    }

    public Double getTotalSurface() {
        return totalSurface;
    }

    public Double getTotalLength() {
        return totalLength;
    }

    public Long getTotalPonctual() {
        return totalPonctual;
    }

    public List<StatisticsType> getTypes() {
        return types;
    }

    public List<String> getSeries() {
        return series;
    }

    public List<Double> getSurfaces() {
        return surfaces;
    }

    public List<Double> getLengths() {
        return lengths;
    }

    public List<Long> getPonctuals() {
        return ponctuals;
    }

    public List<String> getColors() {
        return colors;
    }

    public List<String> getSizes() {
        return sizes;
    }

    public List<String> getStrokeColors() {
        return strokeColors;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public List<Long> getCounts() {
        return counts;
    }

    public List<String> getStrokeWidths() {
        return strokeWidths;
    }

}
