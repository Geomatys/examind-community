package com.examind.community.storage.coverage.aggregation;

import org.apache.sis.storage.GridCoverageResource;

record ResourceTimeSpan(double min, double max, GridCoverageResource source) implements Comparable<ResourceTimeSpan> {

    @Override
    public int compareTo(ResourceTimeSpan other) {
        int minComparison = Double.compare(min, other.min);
        return (minComparison != 0) ? minComparison : Double.compare(max, other.max);
    }
}
