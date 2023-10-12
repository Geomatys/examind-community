package org.constellation.map.layerstats;

/**
 *
 * @author Estelle Idee (Geomatys)
 */
public class LayerStatisticsBucket {
    private double surface;
    private double length;
    private long pointNb;
    private Long count = 0L;
    private final LayerStatistics.StatisticsType type;
    private final String color;
    private final String strokeWidth;
    private final String size;
    private final String strokeColor;
    private final String series;

    LayerStatisticsBucket(final LayerStatistics.StatisticsType type, final String color, final String series, final String size, final String strokeColor, final String strokeWidth) {
        this.type = type;
        this.color = color;
        this.series = series;
        this.size = size;
        this.strokeColor = strokeColor;
        this.strokeWidth = strokeWidth;
    }

    void updateSurface(double surface) {
        this.surface = this.surface + surface;
        this.count++;
    }
    void updateLength(double length) {
        this.length = this.length + length;
        this.count++;
    }
    void updatePointNb(Long pointNb) {
        this.pointNb = this.pointNb + pointNb;
        this.count++;
    }

    public double getSurface() {
        return surface;
    }

    public double getLength() {
        return length;
    }

    public long getPointNb() {
        return pointNb;
    }

    public Long getCount() {
        return count;
    }

    String getColor() {
        return color;
    }

    String getStrokeWidth() {
        return strokeWidth;
    }

    String getSize() {
        return size;
    }

    String getStrokeColor() {
        return strokeColor;
    }

    String getSeries() {
        return series;
    }

    LayerStatistics.StatisticsType getType() {
        return type;
    }
}
