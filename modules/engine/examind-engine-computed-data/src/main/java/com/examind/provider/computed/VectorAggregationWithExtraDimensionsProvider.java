package com.examind.provider.computed;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.sis.cql.CQL;
import org.apache.sis.cql.CQLException;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.internal.filter.FunctionNames;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureQuery;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.Query;
import org.apache.sis.storage.aggregate.ConcatenatedFeatureSet;
import org.apache.sis.storage.event.StoreEvent;
import org.apache.sis.storage.event.StoreListener;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.iso.Names;
import org.constellation.exception.ConstellationException;
import org.constellation.provider.Data;
import org.constellation.provider.DataProviderFactory;
import org.constellation.provider.DataProviders;
import org.constellation.provider.DefaultFeatureData;
import org.constellation.util.DimensionDef;
import org.geotoolkit.filter.visitor.DuplicatingFilterVisitor;
import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyNotFoundException;
import org.opengis.feature.PropertyType;
import org.opengis.filter.Expression;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.ValueReference;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.util.GenericName;
import org.opengis.util.LocalName;

import static java.util.function.UnaryOperator.identity;
import static com.examind.provider.computed.ComputedResourceProviderDescriptor.DATA_NAME;
import static com.examind.provider.computed.VectorAggregationWithExtraDimensionsProviderDescriptor.*;

public class VectorAggregationWithExtraDimensionsProvider extends ComputedResourceProvider {

    public static final String AGG_TIME_MIN = "exa_time_dim_min";
    public static final String AGG_TIME_MAX = "exa_time_dim_max";
    public static final String AGG_ELEV_MIN = "exa_elevation_dim_min";
    public static final String AGG_ELEV_MAX = "exa_elevation_dim_max";

    public VectorAggregationWithExtraDimensionsProvider(String providerId, DataProviderFactory service, ParameterValueGroup param) {
        super(providerId, service, param);
    }

    @Override
    protected Data<?> getComputedData() {
        if (cachedData != null) return cachedData;
        var input = Parameters.castOrWrap(getSource());
        var dataName = input.getMandatoryValue(DATA_NAME);
        var confs = input.groups(DATA_CONFIGURATIONS.getName().getCode());
        var dataList = confs.stream()
                .map(this::resolveConf)
                .toList();
        final LocalName outName = Names.createLocalName(null, null, dataName);
        if (dataList.isEmpty()) throw new IllegalArgumentException("Input configuration contains no data !");
        if (dataList.size() == 1) {
            var data = dataList.get(0);
            return new DefaultFeatureData(outName, null, data.source, data.conf.time, data.conf.elevation, null);
        }

        try {
            var commonType = createCommonType(dataList.stream().map(it -> it.sourceType).toList());
            final FilterFactory<Feature, Object, Object> ff = DefaultFilterFactory.forFeatures();
            AtomicBoolean anyTime = new AtomicBoolean();
            AtomicBoolean anyElev = new AtomicBoolean();
            var dataToConcat = dataList.stream()
                    .map(data -> {
                        var tMin = data.conf.time == null ? null : data.conf.time.lower();
                        var tMax = data.conf.time == null ? null : data.conf.time.upper();
                        var eMin = data.conf.elevation == null ? null : data.conf.elevation.lower();
                        var eMax = data.conf.elevation == null ? null : data.conf.elevation.upper();
                        final boolean hasTime = tMin != null || tMax != null;
                        if (hasTime) anyTime.compareAndSet(false, true);
                        final boolean hasElevation = eMin != null || eMax != null;
                        if (hasElevation) anyElev.compareAndSet(false, true);
                        var mapping = createMapper(commonType, data);
                        WrapConfiguration conf;
                        if (hasTime || hasElevation) {
                            final Map<String, Expression<?, ?>> replacements = Map.of(
                                    AGG_TIME_MIN, tMin == null ? ff.literal(null) : tMin,
                                    AGG_TIME_MAX, tMax == null ? ff.literal(null) : tMax,
                                    AGG_ELEV_MIN, eMin == null ? ff.literal(null) : eMin,
                                    AGG_ELEV_MAX, eMax == null ? ff.literal(null) : eMax
                            );
                            var replaceVisitor = new ReplaceValueReferences(replacements);
                            conf = new WrapConfiguration(
                                    data.source, mapping.targetType, mapping.mapper,
                                    selection -> (Filter) replaceVisitor.visit((Filter) selection),
                                    projection -> (Expression) replaceVisitor.visit((Expression) projection)
                            );
                        } else {
                            conf = new WrapConfiguration(data.source, mapping.targetType, mapping.mapper, identity(), identity());
                        }
                        return new WrapFeatureSet(conf);
                    })
                    .toList();

            // TODO: concatenation will likely not work, because it requires a common super type, which is rarely used.
            var concatenation = ConcatenatedFeatureSet.create(dataToConcat);
            return new DefaultFeatureData(
                    outName, null, concatenation,
                    anyTime.get() ? new DimensionDef(CommonCRS.Temporal.JAVA.crs(), ff.property(AGG_TIME_MIN), ff.property(AGG_TIME_MAX)) : null,
                    anyElev.get() ? new DimensionDef(CommonCRS.Vertical.MEAN_SEA_LEVEL.crs(), ff.property(AGG_ELEV_MIN), ff.property(AGG_ELEV_MAX)) : null,
                    null
            );
        } catch (DataStoreException e) {
            throw new BackingStoreException(e);
        }
    }

    private SourceConf resolveConf(ParameterValueGroup conf) {
        try {
            var timeRange = fetchRange(Date.class, firstOrNull(conf.groups(TEMPORAL.getName().getCode())), CommonCRS.Temporal.JAVA.crs());
            final ParameterValueGroup verticalConf = firstOrNull(conf.groups(VERTICAL.getName().getCode()));
            VerticalCRS vcrs = fetchVerticalCRS(verticalConf);
            var elevRange = fetchRange(Double.class, verticalConf, vcrs);

            var data = DataProviders.getProviderData(Parameters.castOrWrap(conf).getMandatoryValue(DATA_ID)).getOrigin();
            if (data instanceof FeatureSet vector) {
                return new SourceConf(vector, vector.getType(), new DimensionConfiguration(timeRange, elevRange));
            } else throw new IllegalArgumentException("Input data is not a vector dataset");
        } catch (CQLException | ConstellationException | DataStoreException e) {
            throw new BackingStoreException("Cannot resolve configuration to add dimension to vector data", e);
        }
    }

    private VerticalCRS fetchVerticalCRS(ParameterValueGroup vertical) {
        if (vertical == null) return null;
        var vCrsLabel = Parameters.castOrWrap(vertical).getValue(VERTICAL_CRS);
        if (vCrsLabel == null || vCrsLabel.isBlank()) return null;
        return CommonCRS.Vertical.valueOf(vCrsLabel).crs();
    }

    private <CRS extends SingleCRS, V> DimensionDef<CRS, Feature, V> fetchRange(Class<V> valueType, ParameterValueGroup dimension, CRS crs) throws CQLException {
        if (dimension == null) return null;
        var dim = Parameters.castOrWrap(dimension);
        var start = dim.getValue(START);
        var end = dim.getValue(END);
        var startExpr = start == null ? null : CQL.parseExpression(start).toValueType(valueType);
        var endExpr = end == null ? null : CQL.parseExpression(end).toValueType(valueType);
        if (startExpr == null && endExpr == null) return null;
        else if (startExpr == null) return new DimensionDef<>(crs, endExpr);
        else if (endExpr == null) return new DimensionDef<>(crs, startExpr);
        else return new DimensionDef<>(crs, startExpr, endExpr);
    }

    private record DimensionConfiguration(DimensionDef<TemporalCRS, Feature, Date> time, DimensionDef<VerticalCRS, Feature, Double> elevation) {}
    private record SourceConf(FeatureSet source, FeatureType sourceType, DimensionConfiguration conf) {}

    private static <T> T firstOrNull(List<T> source) {
        if (source == null || source.isEmpty()) return null;
        return source.get(0);
    }

    private static class ReplaceValueReferences extends DuplicatingFilterVisitor {
        ReplaceValueReferences(Map<String, Expression<?, ?>> target) {
            setExpressionHandler(FunctionNames.ValueReference, (e) -> {
                assert e instanceof ValueReference : "Only value references should be received here";
                final ValueReference<?, ?> ref = (ValueReference<?,?>) e;
                return target.getOrDefault(ref.getXPath(), ref);
            });
        }
    }

    private FeatureType createCommonType(List<FeatureType> types) {
        FeatureType firstType = types.get(0);
        final List<String> mandatoryPropertiesFromFirstType = firstType.getProperties(true).stream()
                .filter(type -> type instanceof AttributeType attr && attr.getMinimumOccurs() > 0)
                .map(type -> type.getName().toString())
                .toList();
        FeatureTypeBuilder builder = new FeatureTypeBuilder(firstType);
        boolean firstTypeModified = false;
        for (int i = 1; i < types.size(); i++) {
            final FeatureType type = types.get(i);

            /* TODO: This is not very robust. A lot of cases are not managed. Example:
             * - Both types contains a property with the same name but different data types
             * - Both types contains the same property with different minimumOccurs or another characteristic
             * - Operations, associations and all that stuff is not properly managed.
             */
            if (type.isAssignableFrom(firstType)) continue;
            for (PropertyType p : type.getProperties(true)) {
                if (!(p instanceof AttributeType<?>)) continue;
                if (builder.getProperty(p.getName().toString()) == null) {
                    builder.addProperty(p).setMinimumOccurs(0);
                    firstTypeModified = true;
                }
            }

            for (String p : mandatoryPropertiesFromFirstType) {
                try {
                    type.getProperty(p);
                } catch (PropertyNotFoundException e) {
                    builder.getProperty(p).setMinimumOccurs(0);
                    firstTypeModified = true;
                }
            }
        }

        final FeatureType commonType;
        if (firstTypeModified) {
            commonType = builder.build();
            LOGGER.fine(() -> String.format("Temporal concatenation of different feature types.%nCommon output data type:%n%s%nFirst source data type:%n%s", commonType, firstType));
        } else commonType = firstType;

        builder.clear()
                .setName(commonType.getName() + " + additional dimensions")
                .setSuperTypes(commonType);

        builder.addAttribute(Date.class).setName(AGG_TIME_MIN);
        builder.addAttribute(Date.class).setName(AGG_TIME_MAX);
        builder.addAttribute(Double.class).setName(AGG_ELEV_MIN);
        builder.addAttribute(Double.class).setName(AGG_ELEV_MAX);

        return builder.build();
    }

    private record MappingConfiguration(FeatureType targetType, UnaryOperator<Feature> mapper) {}

    private MappingConfiguration createMapper(FeatureType commonType, SourceConf source) {
        final FeatureType sourceType = source.sourceType;
        if (commonType.isAssignableFrom(sourceType)) return new MappingConfiguration(sourceType, identity());

        final String commonName = commonType.getName().tip().toString();
        final Set<String> propertiesFromSourceType = extractAttributeNames(sourceType);
        final FeatureType targetType = new FeatureTypeBuilder()
                    .setName(Names.createScopedName(sourceType.getName(), null, commonName))
                    .setSuperTypes(sourceType, commonType)
                    .build();
        var time = source.conf.time;
        var elev = source.conf.elevation;

        UnaryOperator<Feature> mapper = input -> {
            var inputType = input.getType();
            final Set<String> propertiesToCopy;
            final Feature copy;
            if (sourceType.equals(inputType)) {
                /* Minor speed up: the current feature type is exactly the same as the type declared by source dataset.
                 * It means that we can directly copy feature into target type prepared in advance.
                 */
                copy = targetType.newInstance();
                propertiesToCopy = propertiesFromSourceType;
            } else {
                copy = new FeatureTypeBuilder()
                        .setName(Names.createScopedName(inputType.getName(), ":", commonName))
                        .setSuperTypes(inputType, commonType)
                        .build()
                        .newInstance();
                propertiesToCopy = extractAttributeNames(inputType);
            }
            for (String name : propertiesToCopy) {
                copy.setPropertyValue(name, input.getPropertyValue(name));
            }

            if (time != null) {
                if (time.lower() != null) copy.setPropertyValue(AGG_TIME_MIN, time.lower().apply(input));
                if (time.upper() != null) copy.setPropertyValue(AGG_TIME_MAX, time.upper().apply(input));
            }

            if (elev != null) {
                if (elev.lower() != null) copy.setPropertyValue(AGG_ELEV_MIN, elev.lower().apply(input));
                if (elev.upper() != null) copy.setPropertyValue(AGG_ELEV_MIN, elev.upper().apply(input));
            }

            return copy;
        };

        return new MappingConfiguration(targetType, mapper);
    }

    private static Set<String> extractAttributeNames(final FeatureType source) {
        return source.getProperties(true).stream()
                .filter(AttributeType.class::isInstance)
                .map(type -> type.getName().tip().toString())
                .collect(Collectors.toSet());
    }

    private record WrapConfiguration(FeatureSet source,
                                     FeatureType targetType,
                                     UnaryOperator<Feature> featureMapper,
                                     UnaryOperator<Filter<? super Feature>> replaceQuerySelection,
                                     UnaryOperator<Expression<? super Feature, ?>> replaceQueryProjection) {

    }

    private class WrapFeatureSet implements FeatureSet {

        private final WrapConfiguration conf;

        private WrapFeatureSet(WrapConfiguration conf) {
            this.conf = conf;
        }

        @Override
        public Stream<Feature> features(boolean parallel) throws DataStoreException {
            return conf.source.features(parallel).map(conf.featureMapper);
        }

        @Override
        public FeatureSet subset(Query query) throws DataStoreException {
            if (query instanceof FeatureQuery fq) {
                // Replace emulated dimension properties with actual dimension expressions on the source
                var qProj = fq.getProjection();
                qProj = qProj == null ? null : Arrays.stream(qProj)
                        .map(expr -> {
                            if (expr != null && expr.expression instanceof ValueReference<? super Feature,?> ref) {
                                var newExpr = conf.replaceQueryProjection.apply(ref);
                                return new FeatureQuery.NamedExpression(newExpr, expr.alias);
                            } else return expr;
                        })
                        .toArray(FeatureQuery.NamedExpression[]::new);

                // TODO: if expressions to replace are literals or constant expression (expression that always return the same result, like a math operation over a literal)
                //       then a potential heavy optimisation can be added:
                //         1. Check if input selection contains a filter over the property to replace
                //         2. If it does, then check the result of the sub-filter
                //         3. If it returns false, directly exclude this feature set, by returning an empty in-memory dataset
                //         4. Otherwise, propagate the filter, stripped from the constant evaluation already resolved.
                //       This kind of optimization is difficult however, because it requires to:
                //         1. be able to determine if an expression is a constant (or a calculus always returning the same result)
                //         2. Know if a sub-filter is a sufficient selection criterion:
                //             a. ensure it is not used in a logical `or` subtree.
                //             b. verify if it is wrapped by one or more `not` expression.
                var qSelection = fq.getSelection();
                qSelection = qSelection == null ? null : conf.replaceQuerySelection.apply(qSelection);
                fq = new FeatureQuery();
                fq.setProjection(qProj);
                fq.setSelection(qSelection);
                query = fq;
            }
            return FeatureSet.super.subset(query);
        }

        @Override
        public FeatureType getType() {
            return conf.targetType;
        }

        @Override
        public Optional<Envelope> getEnvelope() throws DataStoreException {
            return conf.source.getEnvelope();
        }

        @Override
        public Optional<GenericName> getIdentifier() throws DataStoreException {
            return conf.source.getIdentifier();
        }

        @Override
        public Metadata getMetadata() throws DataStoreException {
            return conf.source.getMetadata();
        }

        @Override
        public <T extends StoreEvent> void addListener(Class<T> eventType, StoreListener<? super T> listener) {
            conf.source.addListener(eventType, listener);
        }

        @Override
        public <T extends StoreEvent> void removeListener(Class<T> eventType, StoreListener<? super T> listener) {
            conf.source.removeListener(eventType, listener);
        }
    }
}
