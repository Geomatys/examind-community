/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
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
package org.constellation.provider;


import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.stream.Stream;

import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureAssociationRole;
import org.opengis.feature.FeatureType;
import org.opengis.feature.IdentifiedType;
import org.opengis.feature.Operation;
import org.opengis.feature.PropertyNotFoundException;
import org.opengis.feature.PropertyType;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.PropertyName;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.GenericName;

import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;

import org.geotoolkit.referencing.ReferencingUtilities;
import org.geotoolkit.storage.feature.FeatureStoreUtilities;
import org.geotoolkit.storage.feature.query.Query;
import org.geotoolkit.storage.feature.query.QueryBuilder;
import org.geotoolkit.style.MutableStyle;
import org.geotoolkit.style.RandomStyleBuilder;
import org.geotoolkit.util.NamesExt;

import org.constellation.api.DataType;
import org.constellation.dto.FeatureDataDescription;
import org.constellation.dto.PropertyDescription;
import org.constellation.dto.StatInfo;
import org.constellation.exception.ConstellationStoreException;
import org.locationtech.jts.geom.Geometry;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;

/**
 * Default layer details for a datastore type.
 *
 * @author Johann Sorel (Geomatys)
 */
public class DefaultFeatureData extends DefaultGeoData<FeatureSet> implements FeatureData {

    /**
     * Defines the number of pixels we want to add to the specified coordinates given by
     * the GetFeatureInfo request.
     */
    protected static final int MARGIN = 4;

    protected final PropertyName dateStartField;
    protected final PropertyName dateEndField;
    protected final PropertyName elevationStartField;
    protected final PropertyName elevationEndField;

    /**
     * Data version date. Use to query Features is input FeatureStore is versioned.
     */
    protected final Date versionDate;

    /**
     * Build a FeatureData with layer name, store, favorite style names, temporal/elevation filters and
     * data version date.
     *
     * @param name layer name
     * @param store FeatureStore
     * @param origin Wrapped feature set.
     * @param dateStart temporal filter start
     * @param dateEnd temporal filter end
     * @param elevationStart elevation filter start
     * @param elevationEnd elevation filter end
     * @param versionDate data version date of the layer (can be null)
     */
    public DefaultFeatureData(GenericName name, DataStore store, FeatureSet origin,
                                        String dateStart, String dateEnd, String elevationStart, String elevationEnd, Date versionDate){
        super(name, origin, store);
        this.versionDate = versionDate;

        final FilterFactory ff = DefaultFactories.forBuildin(FilterFactory.class);

        if(dateStart != null)       this.dateStartField = ff.property(dateStart);
        else                        this.dateStartField = null;

        if(dateEnd != null)         this.dateEndField = ff.property(dateEnd);
        else                        this.dateEndField = null;

        if(elevationStart != null)      this.elevationStartField = ff.property(elevationStart);
        else                            this.elevationStartField = null;

        if(elevationEnd != null)        this.elevationEndField = ff.property(elevationEnd);
        else                            this.elevationEndField = null;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected MutableStyle getDefaultStyle() throws ConstellationStoreException {
        return RandomStyleBuilder.createDefaultVectorStyle(getType());
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Envelope getEnvelope() throws ConstellationStoreException {
        try {
            final QueryBuilder query = new QueryBuilder();
            query.setVersionDate(versionDate);
            query.setTypeName(name);
            FeatureSet subfs = origin.subset(query.buildQuery());
            return FeatureStoreUtilities.getEnvelope(subfs);
        } catch (Exception ex) {
            throw new ConstellationStoreException(ex);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public SortedSet<Date> getAvailableTimes() throws ConstellationStoreException {
        final SortedSet<Date> dates = new TreeSet<>();
        if (dateStartField != null) {
            try {
                final AttributeType desc = (AttributeType) dateStartField.evaluate(origin.getType());

                if(desc == null){
                    LOGGER.log(Level.WARNING , "Invalide field : "+ dateStartField + " Doesnt exists in layer :" + name);
                    return dates;
                }

                final Class type = desc.getValueClass();
                if( !(Date.class.isAssignableFrom(type)) ){
                    LOGGER.log(Level.WARNING , "Invalide field type for dates, layer " + name +", must be a Date, found a " + type);
                    return dates;
                }

                final QueryBuilder builder = new QueryBuilder();
                builder.setTypeName(name);
                builder.setProperties(new String[]{dateStartField.getPropertyName()});
                builder.setVersionDate(versionDate);
                final Query query = builder.buildQuery();

                try (Stream<Feature> stream = origin.subset(query).features(false)) {
                    Iterator<Feature> features = stream.iterator();
                    while(features.hasNext()){
                        final Feature sf = features.next();
                        final Date date = dateStartField.evaluate(sf,Date.class);
                        if(date != null){
                            dates.add(date);
                        }
                    }
                }
            } catch(DataStoreException ex) {
                LOGGER.log(Level.WARNING , "Could not evaluate dates",ex);
            }

        }

        return dates;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SortedSet<Number> getAvailableElevations() throws ConstellationStoreException {
        final SortedSet<Number> elevations = new TreeSet<>();
        if (elevationStartField != null) {

            try {
                final AttributeType desc = (AttributeType) elevationStartField.evaluate(origin.getType());
                if(desc == null){
                    LOGGER.log(Level.WARNING , "Invalid field : "+ elevationStartField + " Does not exist in layer :" + name);
                    return elevations;
                }

                final Class type = desc.getValueClass();
                if (!(Number.class.isAssignableFrom(type)) ){
                    LOGGER.log(Level.WARNING , "Invalid field type for elevations, layer " + name +", must be a Number, found a " + type);
                    return elevations;
                }

                final QueryBuilder builder = new QueryBuilder();
                builder.setTypeName(name);
                builder.setProperties(new String[]{elevationStartField.getPropertyName()});
                builder.setVersionDate(versionDate);
                final Query query = builder.buildQuery();

                try (Stream<Feature> stream = origin.subset(query).features(false)) {
                    Iterator<Feature> features = stream.iterator();
                    while (features.hasNext()) {
                        final Feature sf = features.next();
                        final Number ele = elevationStartField.evaluate(sf,Number.class);
                        if(ele != null){
                            elevations.add(ele);
                        }
                    }
                }

            } catch(DataStoreException ex) {
                LOGGER.log(Level.WARNING , "Could not evaluate elevationss",ex);
            }
        }
        return elevations;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FeatureType getType() throws ConstellationStoreException {
        try {
            return origin.getType();
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }


    /**
     * Gives a {@link FeatureDataDescription} instance describing the feature layer
     * data source.
     */
    @Override
    public FeatureDataDescription getDataDescription(StatInfo statInfo) throws ConstellationStoreException {
        final FeatureDataDescription description = new FeatureDataDescription();
        try {

            // Acquire data feature type.
            final FeatureType featureType = origin.getType();

            // Feature attributes description.
            for (PropertyType pt : featureType.getProperties(true)) {
                if (pt instanceof AttributeType && !AttributeConvention.contains(pt.getName())) {
                    final AttributeType attType = (AttributeType) pt;
                    description.getProperties().add(new PropertyDescription(
                        NamesExt.getNamespace(pt.getName()),
                        pt.getName().tip().toString(),
                        attType.getValueClass()));
                }
            }

            // Geographic extent description.
            final QueryBuilder queryBuilder = new QueryBuilder();
            queryBuilder.setTypeName(getName());

            final Envelope envelope = getEnvelope();
            DataProviders.fillGeographicDescription(envelope, description);

        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
        return description;
    }

    @Override
    public Object[] getPropertyValues(String property) throws ConstellationStoreException {
        try {
            // Visit collection.
            final QueryBuilder qb = new QueryBuilder();
            qb.setProperties(new String[]{property});
            qb.setTypeName(getName());
            try (Stream<Feature> stream = origin.subset(qb.buildQuery()).features(false)) {
                return stream
                        .map(f -> f.getPropertyValue(property))
                        .toArray();
            }

        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public DataType getDataType() {
        return DataType.VECTOR;
    }

    @Override
    public String getSubType() throws ConstellationStoreException {
        try {
            FeatureType fType = origin.getType();
            return findGeometryType(fType, null);
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    private static String findGeometryType(FeatureType ft, Set<GenericName> visited){
        if(ft==null) return null;

        if(visited==null) visited = new HashSet<>();
        if(visited.contains(ft.getName())) return null;
        visited.add(ft.getName());

        if(ft instanceof FeatureType){
            try {
                IdentifiedType property = ((FeatureType)ft).getProperty(AttributeConvention.GEOMETRY_PROPERTY.toString());
                if (property instanceof Operation) {
                   property = ((Operation)property).getResult();
                }
                if(property instanceof AttributeType) {
                    return ((AttributeType)property).getValueClass().getSimpleName();
                }
            } catch (PropertyNotFoundException ex){
                //continue
            }
        }

        for (PropertyType type : ft.getProperties(true)) {

            if (type instanceof FeatureAssociationRole) {
                try {
                    FeatureType propFt = ((FeatureAssociationRole) type).getValueType();
                    String subType = findGeometryType(propFt, visited);
                    if (subType != null) {
                        return subType;
                    }
                } catch (IllegalStateException ex) {
                    LOGGER.log(Level.WARNING, "Unable to resolver feature type attribute:" + ex.getMessage());
                }
            } else if (type instanceof AttributeType) {
                final Class<?> valueClass = ((AttributeType) type).getValueClass();
                if (Geometry.class.isAssignableFrom(valueClass)) {
                    return valueClass.getSimpleName();
                }
            }
        }

        return null;
    }
}
