package org.constellation.util.converter;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Set;
import java.util.function.Supplier;
import org.apache.sis.math.FunctionProperty;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.UnconvertibleObjectException;

public final class TemporalConverters {
    private TemporalConverters() {}

    public static class LocalDateToDate extends AbstractConverter<LocalDate, Date> {

        public LocalDateToDate() {
            super(LocalDate.class, Date.class, DateToLocalDate::new);
        }


        @Override
        public Date apply(LocalDate object) throws UnconvertibleObjectException {
            if (object == null) return null;
            return Date.from(object.atTime(12, 0).toInstant(ZoneOffset.UTC));
        }
    }

    public static class DateToLocalDate extends AbstractConverter<Date, LocalDate> {

        public DateToLocalDate() {
            super(Date.class, LocalDate.class, LocalDateToDate::new);
        }

        @Override
        public LocalDate apply(Date object) throws UnconvertibleObjectException {
            if (object == null) return null;
            return object.toInstant().atOffset(ZoneOffset.UTC).toLocalDate();
        }
    }

    public static class DateToDouble extends AbstractConverter<Date, Double> {

        public DateToDouble() {
            super(Date.class, Double.class, null);
        }

        @Override
        public Double apply(Date object) throws UnconvertibleObjectException {
            return object == null ? null : (double) object.getTime();
        }
    }

    public static class DateToLong extends AbstractConverter<Date, Long> {

        public DateToLong() {
            super(Date.class, Long.class, null);
        }

        @Override
        public Long apply(Date object) throws UnconvertibleObjectException {
            return object == null ? null : object.getTime();
        }
    }


    private static abstract class AbstractConverter<I, O> implements ObjectConverter<I, O> {

        private final Class<I> inType;
        private final Class<O> outType;
        private final Set<FunctionProperty> properties;
        private final Supplier<ObjectConverter<O, I>> inverseSupplier;

        AbstractConverter(Class<I> inType, Class<O> outType, Supplier<ObjectConverter<O, I>> inverseSupplier, FunctionProperty... properties) {
            this.inType = inType;
            this.outType = outType;
            this.inverseSupplier = inverseSupplier;
            this.properties = Set.of(properties);
        }

        @Override
        public Set<FunctionProperty> properties() {
            return properties;
        }

        @Override
        public Class<I> getSourceClass() { return inType; }

        @Override
        public Class<O> getTargetClass() { return outType; }

        @Override
        public ObjectConverter<O, I> inverse() throws UnsupportedOperationException {
            var inverse = inverseSupplier == null ? null : inverseSupplier.get();
            if (inverse == null) throw new UnsupportedOperationException("No inverse converter available");
            return inverse;
        }
    }
}
