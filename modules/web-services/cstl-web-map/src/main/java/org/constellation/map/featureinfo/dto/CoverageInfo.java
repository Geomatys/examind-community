package org.constellation.map.featureinfo.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.measure.Unit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A simple JSON representation of a GetFeatureInfo on a single coverage.
 *
 * @implNote Works only for writing.
 */
public class CoverageInfo implements LayerInfo {
    private final String layer;
    private final Instant time;
    private final Double elevation;

    public CoverageInfo(String layer, Instant time, Double elevation) {
        this.layer = layer;
        this.time = time;
        this.elevation = elevation;
        // Mostly encounters single banded or rgb(a) data. No need to reserve more space by default.
        this.values = new ArrayList<>(4);
    }

    public String getLayer() {
        return layer;
    }

    @JsonIgnore
    public Instant getTime() {
        return time;
    }

    @JsonProperty("time")
    public String getISOTime() {
        return time == null? null : time.toString();
    }

    public Double getElevation() {
        return elevation;
    }

    private final List<Sample> values;

    public List<Sample> getValues() {
        return values;
    }

    public static class Sample {
        final String name;
        final Number value;
        final Unit unit;

        public Sample(String name, Number value, Unit unit) {
            this.name = name;
            this.value = value;
            this.unit = unit;
        }

        public String getName() {
            return name;
        }

        public Number getValue() {
            return value;
        }

        @JsonIgnore
        public Unit getUnit() {
            return unit;
        }

        @JsonProperty("unit")
        public String getUnitSymbol() {
            return unit == null? null : unit.getSymbol();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Sample sample = (Sample) o;
            return Objects.equals(name, sample.name) &&
                    Objects.equals(value, sample.value) &&
                    Objects.equals(unit, sample.unit);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, value, unit);
        }
    }
}
