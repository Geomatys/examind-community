/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2020 Geomatys.
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
package org.constellation.ws;

import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.TreeSet;
import java.util.logging.Logger;
import org.apache.sis.cql.CQL;
import javax.xml.namespace.QName;
import org.apache.sis.cql.CQLException;
import org.apache.sis.feature.builder.AttributeTypeBuilder;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.feature.internal.FeatureExpression;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.measure.Units;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.crs.DefaultEngineeringCRS;
import org.apache.sis.referencing.crs.DefaultTemporalCRS;
import org.apache.sis.referencing.cs.AbstractCS;
import org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis;
import org.apache.sis.referencing.datum.DefaultEngineeringDatum;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.util.Utilities;
import org.constellation.admin.SpringHelper;
import org.constellation.api.DataType;
import org.constellation.api.ServiceDef;
import org.constellation.business.IDataBusiness;
import org.constellation.dto.DimensionRange;
import org.constellation.dto.FeatureDataDescription;
import org.constellation.dto.NameInProvider;
import org.constellation.dto.StyleReference;
import org.constellation.dto.service.config.wxs.DimensionDefinition;
import org.constellation.dto.service.config.wxs.LayerConfig;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.util.DimensionDef;
import org.constellation.provider.Data;
import org.constellation.map.util.DtoToOGCFilterTransformer;
import org.constellation.provider.CoverageData;
import org.constellation.provider.FeatureData;
import org.geotoolkit.filter.FilterFactoryImpl;
import static org.geotoolkit.filter.FilterUtilities.FF;

import org.opengis.feature.FeatureType;
import org.opengis.filter.Expression;
import org.opengis.filter.Filter;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.datum.EngineeringDatum;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Note about thread-safety: This object is <em>NOT</em> thread-safe.
 * It is user responsibility to synchronize accesses if (s)he uses it in a multi-threaded environment.
 *
 * @author Guilhem Legal (Geomatys)
 */
public class LayerCache {
    
    private static final Logger LOGGER = Logger.getLogger("org.constellation.ws");

    private final NameInProvider nip;
    private final QName name;
    private final List<StyleReference> styles;
    private final Data<?> data;
    private final LayerConfig configuration;

    @Autowired
    private IDataBusiness dataBusiness;

    private ExtraDimensions layerAdditionalDimensions;

    public LayerCache(final NameInProvider nip, QName name, Data d, List<StyleReference> styles, final LayerConfig configuration) {
        SpringHelper.injectDependencies(this);
        this.nip = nip;
        this.data = d;
        this.name = name;
        this.styles = styles;
        this.configuration = configuration;
    }

    public Integer getId() {
        return nip.layerId;
    }

    public Optional<String> getAlias() {
        if (nip.alias != null) {
            return Optional.of(nip.alias);
        }
        return Optional.empty();
    }

    public QName getName() {
        return name;
    }

    public List<StyleReference> getStyles() {
        return styles;
    }

    public Data getData() {
        return data;
    }

    public LayerConfig getConfiguration() {
        return configuration;
    }

    /**
     * lazy cached native data envelope.
     */
    private Envelope envelope;

    /**
     * @return The native envelope.
     */
    public Envelope getEnvelope() throws ConstellationStoreException {
        if (envelope == null) {
            Optional<Envelope> env = dataBusiness.getEnvelope(nip.dataId);
            if (env.isPresent()) {
                envelope = env.get();
            } else {
                envelope = data.getEnvelope();
            }
        }
        return envelope;
    }

    /**
     * Return a reprojected data envelope.
     * 
     * @param crs A coordinate referenceSystem
     */
    public Envelope getEnvelope(CoordinateReferenceSystem crs) throws ConstellationStoreException {
        if (envelope != null && Utilities.equalsIgnoreMetadata(envelope.getCoordinateReferenceSystem(), crs)) {
            return envelope;
        } else {
            return data.getEnvelope(crs);
        }
    }
    
    public GeographicBoundingBox getGeographicBoundingBox() throws ConstellationStoreException {
        try {
            final Envelope env = getEnvelope();
            if (env != null) {
                final DefaultGeographicBoundingBox result = new DefaultGeographicBoundingBox();
                result.setBounds(env);
                return result;
            } else {
                LOGGER.warning("Null boundingBox for Layer:" + name + ". Returning World BBOX.");
                return new DefaultGeographicBoundingBox(-180, 180, -90, 90);
            }
        } catch (TransformException ex) {
            throw new ConstellationStoreException(ex);
        }
    }
    
    public CoordinateReferenceSystem getCoordinateReferenceSystem() throws ConstellationStoreException {
        CoordinateReferenceSystem dataCRS = null;
        Envelope env = getEnvelope();
        if (env != null) {
            dataCRS = env.getCoordinateReferenceSystem();
            // if the data has extra dimension, we need to add them in the crs.
            if (configuration != null && !configuration.getDimensions().isEmpty()) {
                try {
                    final int nbExtraDim = configuration.getDimensions().size();
                    CoordinateReferenceSystem[] crss = new CoordinateReferenceSystem[nbExtraDim + 1];
                    crss[0] = dataCRS;
                    for (int i = 0; i < nbExtraDim; i++) {
                        var dd = getDimensionDef(configuration.getDimensions().get(i));
                        crss[i+1] = dd.crs();
                    }
                    dataCRS = CRS.compound(crss);
                } catch (CQLException | FactoryException ex) {
                    throw new ConstellationStoreException("Error while building a compound CRS with custom dimensions.", ex);
                }
            }
        }
        return dataCRS;
    }

    private org.constellation.dto.Data dbData;
    
    private org.constellation.dto.Data getDbData() throws ConstellationStoreException {
        if (dbData == null) {
            try {
                dbData = dataBusiness.getData(nip.dataId);
            } catch (ConstellationException ex) {
                throw new ConstellationStoreException(ex);
            }
        }
        return dbData;
    }

    public boolean isQueryable(ServiceDef.Query query) {
        return data.isQueryable(query);
    }
    
    public DataType getDataType() {
        return data.getDataType();
    }
    
    public SortedSet<Number> getAvailableElevations() throws ConstellationStoreException {
        SortedSet<Number> elevations;
        if (getDbData().getCachedInfo()) {
            if (getDbData().getHasElevation()) {
                elevations = dataBusiness.getDataElevations(nip.dataId);
            } else {
                elevations = new TreeSet<>();
            }
        } else {
            elevations = data.getAvailableElevations();
        }
        return elevations;
    }
    
    public Number getFirstElevation() throws ConstellationStoreException {
        // can be optimized like times behaviour
        final SortedSet<Number> elevations = getAvailableElevations();
        if (elevations != null && !elevations.isEmpty()) {
            return elevations.first();
        }
        return null;
    }
    
    public SortedSet<Date> getAvailableTimes() throws ConstellationStoreException {
        SortedSet<Date> dates;
        if (getDbData().getCachedInfo()) {
            if (getDbData().getHasTime()) {
                dates = dataBusiness.getDataTimes(nip.dataId, false);
            } else {
                dates = new TreeSet<>();
            }
        } else {
            dates = data.getAvailableTimes();
        }
        return dates;
    }
    
    public SortedSet<Date> getDateRange() throws ConstellationStoreException {
        SortedSet<Date> dates;
        if (getDbData().getCachedInfo()) {
            if (getDbData().getHasTime()) {
                dates = dataBusiness.getDataTimes(nip.dataId, true);
            } else {
                dates = new TreeSet<>();
            }
        } else {
            dates = data.getDateRange();
        }
        return dates;
    }
    
    public SortedSet<DimensionRange> getSampleValueRanges() throws ConstellationStoreException {
        final SortedSet<DimensionRange> dims;
        if (getDbData().getCachedInfo()) {
            if (getDbData().getHasDim()) {
                dims = dataBusiness.getDataDimensionRange(nip.dataId);
            } else {
                dims = new TreeSet<>();
            }
        } else {
            dims = data.getSampleValueRanges();
        }
        return dims;
    }

    public Optional<DimensionDef<TemporalCRS, ?, ?>> getTimeDimension() {
        return Optional.ofNullable(getOrCreateAdditionalDimensions().temporal);
    }

    public Optional<DimensionDef<VerticalCRS, ?, ?>> getElevationDimension() {
        return Optional.ofNullable(getOrCreateAdditionalDimensions().vertical);
    }

    public Double[] getResolution() throws ConstellationStoreException {
        Double[] results = new Double[2];
        if (data instanceof CoverageData covdata) {
            double[] nativeResolution = covdata.getGeometry().getResolution(true);
            results[0] = nativeResolution[0];
            results[1] = nativeResolution[1];
        }
        return results;
    }

    public boolean hasFilterAndDimension() {
        return configuration != null && (configuration.getFilter() != null || !configuration.getDimensions().isEmpty());
    }

    /**
     * Try to get back data type.
     *
     * @return {@link Optional#empty()} if the {@link #getData() layer data} is not a {@link FeatureSet vector dataset},
     *         or if an error occurs (errors are silenced).
     *         Otherwise, the  {@link FeatureType feature type} declared by {@link #getData() layer data}.
     */
    public Optional<FeatureType> getFeatureType() {
        if (getData() != null && getData().getOrigin() instanceof FeatureSet fs) {
            try {
                return Optional.of(fs.getType());
            } catch (DataStoreException | RuntimeException e) {
                LOGGER.log(Level.WARNING, "Cannot get feature type from feature data", e);
            }
        }

        return Optional.empty();
    }

    public Optional<Filter> getLayerFilter(Envelope env, Filter extraFilter) {
        final List<Filter> filters = new ArrayList<>();
        if (extraFilter != null) {
            filters.add(extraFilter);
        }
        if (configuration != null) {
            final org.constellation.dto.Filter confFilter = configuration.getFilter();
            if (confFilter != null) {
                try {
                    filters.add(new DtoToOGCFilterTransformer(FF).visitFilter(confFilter));
                } catch (FactoryException e) {
                    LOGGER.log(Level.WARNING, "Error while transforming layer custom filter", e);
                }
            }
        }

        if (env != null) {
            final List<DimensionDef<?, ?, ?>> dimensions = getAdditionalDimensions();
            if (!dimensions.isEmpty()) {
                for (var def : dimensions) {
                    final Envelope dimEnv;
                    try {
                        dimEnv = Envelopes.transform(env, def.crs());
                    } catch (TransformException ex) {
                        LOGGER.log(Level.FINER, "Error while reprojecting the envelope to dimension CRS.", ex);
                        continue;
                    }

                    assert dimEnv.getDimension() == 1 : "Layer dimension filter work only one dimension at a time";

                    final double dimEnvMin = dimEnv.getMinimum(0);
                    final double dimEnvMax = dimEnv.getMaximum(0);
                    Expression<?, ?> dimMin = FF.literal(dimEnvMin);
                    Expression<?, ?> dimMax = FF.literal(dimEnvMax);

                    /**
                     * Workaround: Special analysis / optimization for temporal dimension.
                     * To allow native handling (and therefore potential optimization) of time filter,
                     * we try to express time boundaries in data native time representation.
                     * Note that, currently, this fix is very brittle and might not work anymore due to:
                     *  - the lack of homogeneity in FeatureSet time handling
                     *  - the complexity of the filter engine
                     *  - The lack of review on the temporal part of filter/expression
                     *
                     * IMPORTANT: Different datastores might manage time differently.
                     *            Before modifying below logic,
                     *            try to test it at least against a PostGIS table with a date or timestamp field.
                     */
                    if (def.crs() instanceof TemporalCRS tcrs && def.lower() instanceof FeatureExpression<?,?> fe) {
                        final Class<?> dimValueType = getFeatureType()
                                .map(type -> fe.expectedType(type, new FeatureTypeBuilder().setName("tmp")) instanceof AttributeTypeBuilder att ? att.getValueClass() : null)
                                .orElse(fe.getValueClass());
                        final boolean isTemporal = Temporal.class.isAssignableFrom(dimValueType);
                        final boolean isDate = Date.class.isAssignableFrom(dimValueType);
                        if (isTemporal || isDate) {
                            final DefaultTemporalCRS dtcrs = DefaultTemporalCRS.castOrCopy(tcrs);
                            if (isDate) {
                                dimMin = FF.literal(dtcrs.toDate(dimEnvMin));
                                dimMax = FF.literal(dtcrs.toDate(dimEnvMax));
                            } else if (isTemporal) {
                                var instantMin = dtcrs.toInstant(dimEnvMin);
                                var instantMax = dtcrs.toInstant(dimEnvMax);
                                dimMin = FF.literal(instantMin).toValueType(dimValueType);
                                dimMax = FF.literal(instantMax).toValueType(dimValueType);
                            }
                        }
                    }

                    // Define intersection with dimension values
                    final Filter dimFilter = FF.and(
                            FF.lessOrEqual(dimMin, def.upper()),
                            FF.greaterOrEqual(dimMax, def.lower())
                    );
                    filters.add(dimFilter);
                }
            }
        }

        return filters.stream().reduce(FF::and);
    }

    public List<String> getLayerProperties(List<String> propertyNames) throws ConstellationStoreException {
        if (propertyNames == null || propertyNames.isEmpty()) return Collections.EMPTY_LIST;

        List<String> results = new ArrayList<>();
        Set<String> inverted = new HashSet<>();

        for (String propertyName : propertyNames) {
            if (propertyName == null || propertyName.isEmpty()) continue;
            if (propertyName.startsWith("-") || propertyName.startsWith("!")) {
                inverted.add(propertyName.substring(1));
            } else {
                results.add(propertyName);
            }
        }
        if (results.isEmpty() && inverted.isEmpty()) {
            // Input contained only null or empty names
            LOGGER.fine("Invalid list of parameters: only null or empty values. Selection ignored.");
            return Collections.EMPTY_LIST;

         } else if (!results.isEmpty() && !inverted.isEmpty()) {
             throw new ConstellationStoreException("Mixed exclusive and inclusive property names");

         } else if (!inverted.isEmpty()) {
           
            if (data.getDataDescription(null, getEnvelope()) instanceof  FeatureDataDescription fd) {
                return fd.getProperties().stream()
                                  .map(p -> p.getName())
                                  .filter(p -> !inverted.contains(p))
                                  .toList();
            } else {
                LOGGER.warning("Layer property omission is only supported for Feature data");
                return Collections.EMPTY_LIST;
            }
        }
        
        return results;
    }

    public List<DimensionDef<?, ?, ?>> getAdditionalDimensions() {
        return getOrCreateAdditionalDimensions().all();
    }

    private ExtraDimensions getOrCreateAdditionalDimensions() {
        if (layerAdditionalDimensions != null) return layerAdditionalDimensions;
        final List<DimensionDef<?, ?, ?>> results = new ArrayList<>();
        DimensionDef<TemporalCRS, ?, ?> timeDim = null; DimensionDef<VerticalCRS, ?, ?> elevationDim = null;
        if (configuration != null && !configuration.getDimensions().isEmpty()) {
            for (DimensionDefinition ddef : configuration.getDimensions()) {
                try {
                    final DimensionDef<?, ?, ?> dim = getDimensionDef(ddef);
                    if (timeDim == null && dim.crs() instanceof TemporalCRS) timeDim = (DimensionDef<TemporalCRS, ?, ?>) dim;
                    else if (elevationDim == null && dim.crs() instanceof VerticalCRS) elevationDim = (DimensionDef<VerticalCRS, ?, ?>) dim;
                    else results.add(dim);
                } catch (CQLException ex) {
                    LOGGER.log(Level.WARNING, "Error while building a dimension filter.", ex);
                }
            }
        }

        if (timeDim == null && data instanceof FeatureData fd) {
            timeDim = fd.getTimeDimension().orElse(null);
        }

        if (elevationDim == null && data instanceof FeatureData fd) {
            elevationDim = fd.getElevationDimension().orElse(null);
        }

        layerAdditionalDimensions = new ExtraDimensions(timeDim, elevationDim, results);
        return layerAdditionalDimensions;
    }

    private static DimensionDef<?, ?, ?> getDimensionDef(DimensionDefinition ddef) throws CQLException {
        final String crsname = ddef.getCrs();
        var lower = CQL.parseExpression(ddef.getLower());
        var upper = CQL.parseExpression(ddef.getUpper());
        final SingleCRS dimCrs;

        if ("elevation".equalsIgnoreCase(crsname)) {
            dimCrs = CommonCRS.Vertical.ELLIPSOIDAL.crs();
        } else if ("temporal".equalsIgnoreCase(crsname)) {
            dimCrs = CommonCRS.Temporal.JAVA.crs();
        } else {
            final EngineeringDatum customDatum = new DefaultEngineeringDatum(Collections.singletonMap("name", crsname));
            final CoordinateSystemAxis csAxis = new DefaultCoordinateSystemAxis(Collections.singletonMap("name", crsname), "u", AxisDirection.valueOf(crsname), Units.UNITY);
            final AbstractCS customCs = new AbstractCS(Collections.singletonMap("name", crsname), csAxis);
            dimCrs = new DefaultEngineeringCRS(Collections.singletonMap("name", crsname), customDatum, customCs);
        }
        return new DimensionDef(dimCrs, lower, upper);
    }

    private record ExtraDimensions(DimensionDef<TemporalCRS, ?, ?> temporal,
                                   DimensionDef<VerticalCRS, ?, ?> vertical,
                                   List<DimensionDef<?, ?, ?>> others) {
        public List<DimensionDef<?, ?, ?>> all() {
            final List<DimensionDef<?, ?, ?>> dims = new ArrayList<>();
            if (vertical != null) dims.add(vertical);
            if (temporal != null) dims.add(temporal);
            if (others != null && !others.isEmpty()) dims.addAll(others);
            return dims;
        }
    }
}
