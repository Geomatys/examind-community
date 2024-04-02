package com.examind.community.storage.coverage.aggregation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.sis.measure.NumberRange;

class TimeIndex {
    private final List<ResourceTimeSpan> sources;
    private final List<Double> sourcesMin;

    public TimeIndex(List<ResourceTimeSpan> sources) {
        Objects.requireNonNull(sources);
        if (!isSorted(sources)) {
            throw new IllegalArgumentException("TimeIndex sources must be sorted");
        }
        this.sources = new ArrayList<>(sources);
        this.sourcesMin = sources.stream()
                .map(ResourceTimeSpan::min)
                .collect(Collectors.toList());
    }

    public Stream<ResourceTimeSpan> search(NumberRange<Double> timeSpan) {
        //int fromIdx = binarySearchBy(sources, timeSpan.lowerEndpoint(), Comparator.comparingDouble(ResourceTimeSpan::getMin));
        int fromIdx = Collections.binarySearch(sourcesMin, timeSpan.getMinValue(), Double::compare);
        fromIdx = (fromIdx >= 0) ? fromIdx : -fromIdx - 2;

        //int toIdx = binarySearchBy(sources, timeSpan.upperEndpoint(), Comparator.comparingDouble(ResourceTimeSpan::getMin));
        int toIdx = Collections.binarySearch(sourcesMin, timeSpan.getMaxValue(), Double::compare);
        toIdx = (toIdx >= 0) ? toIdx : -toIdx - 1;

        return sources.subList(fromIdx, toIdx).stream()
                .filter(span -> NumberRange.create(span.min(), true, span.max(), true).intersectsAny(timeSpan));
    }

    private static boolean isSorted(List<ResourceTimeSpan> list) {
        return IntStream.range(0, list.size() - 1)
                .noneMatch(i -> list.get(i).compareTo(list.get(i + 1)) > 0);
    }
}
