package com.examind.community.storage.coverage.aggregation;

import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.measure.NumberRange;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.operation.TransformException;

import java.util.Objects;

record TemporalReferencing(CoordinateReferenceSystem parentCrs, int timeIndex, TemporalCRS timeCRS) {

    public NumberRange<Double> extractTimeRange(GridGeometry domain) {
        Objects.requireNonNull(domain);
        if (!domain.isDefined(GridGeometry.ENVELOPE)) {
            throw new IllegalArgumentException("GridGeometry must have an envelope defined");
        }

        try {
            Envelope timeEnvelope = domain.getEnvelope(timeCRS);
            if (timeEnvelope.getDimension() != 1) {
                throw new IllegalStateException("Temporal envelope has more than one dimension");
            }
            return NumberRange.create(timeEnvelope.getMinimum(0), true, timeEnvelope.getMaximum(0), true);
        }  catch (TransformException e) {
            throw new IllegalStateException("Impossible to recover the temporal envelope ",e);
        }
    }
}
