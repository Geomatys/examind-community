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


import org.locationtech.jts.geom.GeometryFactory;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.data.query.Query;
import org.geotoolkit.data.query.QueryBuilder;
import org.apache.sis.internal.system.DefaultFactories;
import org.geotoolkit.map.FeatureMapLayer;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.map.MapLayer;
import org.geotoolkit.style.MutableStyle;
import org.geotoolkit.style.RandomStyleBuilder;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.expression.PropertyName;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.extent.GeographicBoundingBox;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;
import org.apache.sis.metadata.iso.spatial.DefaultGeometricObjects;
import org.apache.sis.metadata.iso.spatial.DefaultVectorSpatialRepresentation;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.DataSet;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.Resource;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.constellation.admin.SpringHelper;
import org.constellation.api.DataType;
import org.constellation.business.IStyleBusiness;
import org.constellation.dto.FeatureDataDescription;
import org.constellation.dto.PropertyDescription;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.exception.TargetNotFoundException;
import static org.constellation.provider.AbstractData.LOGGER;
import org.constellation.util.StoreUtilities;
import org.geotoolkit.data.FeatureStoreUtilities;
import static org.geotoolkit.feature.FeatureExt.IS_NOT_CONVENTION;
import org.geotoolkit.metadata.ImageStatistics;
import org.geotoolkit.referencing.ReferencingUtilities;
import org.geotoolkit.style.DefaultDescription;
import org.geotoolkit.util.NamesExt;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureAssociationRole;
import org.opengis.feature.FeatureType;
import org.opengis.feature.IdentifiedType;
import org.opengis.feature.Operation;
import org.opengis.feature.PropertyNotFoundException;
import org.opengis.feature.PropertyType;
import org.opengis.metadata.spatial.GeometricObjectType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;

/**
 * Default layer details for a datastore type.
 *
 * @author Johann Sorel (Geomatys)
 */
public class DefaultFeatureData extends AbstractData implements FeatureData {


    protected static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    protected static final GeographicBoundingBox DUMMY_BBOX =
            new DefaultGeographicBoundingBox(-180, 180, -77, +77);
    /**
     * Defines the number of pixels we want to add to the specified coordinates given by
     * the GetFeatureInfo request.
     */
    protected static final int MARGIN = 4;

    protected final DataStore store;
    protected final PropertyName dateStartField;
    protected final PropertyName dateEndField;
    protected final PropertyName elevationStartField;
    protected final PropertyName elevationEndField;

    /**
     * Data version date. Use to query Features is input FeatureStore is versioned.
     */
    protected final Date versionDate;

    protected final FeatureSet fs;

    /**
     * Build a FeatureLayerDetails with layer name, store, favorite style names, temporal/elevation filters and
     * data version date.
     *
     * @param name layer name
     * @param store FeatureStore
     * @param favorites style names
     * @param dateStart temporal filter start
     * @param dateEnd temporal filter end
     * @param elevationStart elevation filter start
     * @param elevationEnd elevation filter end
     * @param versionDate data version date of the layer (can be null)
     */
    public DefaultFeatureData(GenericName name, DataStore store, FeatureSet fs, List<String> favorites,
                                        String dateStart, String dateEnd, String elevationStart, String elevationEnd, Date versionDate){

        super(name,favorites);

        if(store == null){
            throw new IllegalArgumentException("FeatureSource can not be null.");
        }
        /*try {
            if (!store.getNames().contains(name)) {
                throw new IllegalArgumentException("Provided name " + name + " is not in the datastore known names");
            }
        } catch (DataStoreException ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
        }*/

        this.fs    = fs;
        this.store = store;
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

    protected MapLayer createMapLayer(MutableStyle style, final Map<String, Object> params) throws DataStoreException, ConstellationStoreException {
        if(style == null && favorites.size() > 0){
            //no style provided, try to get the favorite one
            //there are some favorites styles
            final String namedStyle = favorites.get(0);
            final IStyleBusiness business = SpringHelper.getBean(IStyleBusiness.class);
            try {
                style = (MutableStyle) business.getStyle("sld", namedStyle);
            } catch (TargetNotFoundException ex) {
                LOGGER.log(Level.WARNING, null, ex);
            }
        }


        final FeatureType featureType = getType();
        if(style == null){
            //no favorites defined, create a default one
            style = RandomStyleBuilder.createDefaultVectorStyle(featureType);
        }

        final FeatureMapLayer layer = MapBuilder.createFeatureLayer((FeatureSet)getOrigin(), style);

        final String title = getName().tip().toString();
        layer.setName(title);
        final InternationalString isTitle = new SimpleInternationalString(title);
        layer.setDescription(new DefaultDescription(isTitle,isTitle));

        return layer;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public DataStore getStore(){
        return store;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final MapLayer getMapLayer(MutableStyle style, final Map<String, Object> params) throws ConstellationStoreException {

        final MapLayer layer;
        try {
            layer = createMapLayer(style, params);
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }

        // EXTRA FILTER extra parameter ////////////////////////////////////////
        if (params != null && layer instanceof FeatureMapLayer) {
            final Map<String,?> extras = (Map<String, ?>) params.get(KEY_EXTRA_PARAMETERS);
            if (extras != null){
                Filter filter = null;
                for (String key : extras.keySet()) {
                    if (key.equalsIgnoreCase("cql_filter")) {
                        final Object extra = extras.get(key);
                        String cqlFilter = null;
                        if (extra instanceof List) {
                            cqlFilter = ((List) extra).get(0).toString();
                        } else if (extra instanceof String){
                            cqlFilter = (String)extra;
                        }
                        if (cqlFilter != null) {
                            filter = buildCQLFilter(cqlFilter, filter);
                        }
                    } else if (key.startsWith("dim_") || key.startsWith("DIM_")) {
                        final String dimValue = ((List) extras.get(key)).get(0).toString();
                        final String dimName = key.substring(4);
                        filter = buildDimFilter(dimName, dimValue, filter);
                    }
                }
                if (filter != null) {
                    final FeatureMapLayer fml = (FeatureMapLayer) layer;
                    try {
                        final FeatureType type = fml.getResource().getType();
                        if (filter instanceof PropertyIsEqualTo) {
                            final String propName = ((PropertyName)((PropertyIsEqualTo)filter).getExpression1()).getPropertyName();
                            for (PropertyType desc : type.getProperties(true)) {
                                if (desc.getName().tip().toString().equalsIgnoreCase(propName)) {
                                    fml.setQuery(QueryBuilder.filtered(type.getName().toString(), filter));
                                    break;
                                }
                            }
                        }
                    } catch (DataStoreException ex) {
                        throw new ConstellationStoreException(ex);
                    }
                }
            }
        }
        ////////////////////////////////////////////////////////////////////////

        return layer;
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
            FeatureSet subfs = fs.subset(query.buildQuery());
            return FeatureStoreUtilities.getEnvelope(subfs);
        } catch (DataStoreException ex) {
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
                final AttributeType desc = (AttributeType) dateStartField.evaluate(fs.getType());

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

                try (Stream<Feature> stream = fs.subset(query).features(false)) {
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
                final AttributeType desc = (AttributeType) elevationStartField.evaluate(fs.getType());
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

                try (Stream<Feature> stream = fs.subset(query).features(false)) {
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
    public MeasurementRange<?>[] getSampleValueRanges() {
        return new MeasurementRange<?>[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FeatureType getType() throws ConstellationStoreException {
        try {
            return fs.getType();
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    /**
     * Returns a {@linkplain FeatureSet feature set} containing all the data.
     */
    @Override
    public Object getOrigin() {
        /*final QueryBuilder builder = new QueryBuilder();
        builder.setTypeName(name);

        build query using versionDate if not null and sotre support versioning.
        if (store instanceof FeatureStore) {
            FeatureStore fs = (FeatureStore) store;
            if (fs.getQueryCapabilities().handleVersioning()) {

                if (versionDate != null) {
                    builder.setVersionDate(versionDate);
                }
            }
            final Query query =  builder.buildQuery();
            return fs.createSession(false).getFeatureCollection(query);
        } else {*/
            return fs;
        //}
    }


    /**
     * Gives a {@link FeatureDataDescription} instance describing the feature layer
     * data source.
     */
    @Override
    public FeatureDataDescription getDataDescription(ImageStatistics stats) throws ConstellationStoreException {
        final FeatureDataDescription description = new FeatureDataDescription();
        try {

            // Acquire data feature type.
            final FeatureType featureType = fs.getType();

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

            final Envelope envelope = FeatureStoreUtilities.getEnvelope(fs);
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
            try (Stream<Feature> stream = fs.subset(qb.buildQuery()).features(false)) {
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
            FeatureType fType = fs.getType();
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

    @Override
    public DefaultMetadata getResourceMetadata() throws ConstellationStoreException {
        try {
            final Resource ft = StoreUtilities.findResource(store, getName().toString());

            final DefaultMetadata md = new DefaultMetadata();
            final DefaultDataIdentification ident = new DefaultDataIdentification();
            md.getIdentificationInfo().add(ident);

            if (ft instanceof DataSet) {
                DataSet ds = (DataSet) ft;

                // envelope extraction
                Envelope env = FeatureStoreUtilities.getEnvelope(ds);
                if (env != null) {
                    env = Envelopes.transform(env, CommonCRS.WGS84.normalizedGeographic());
                    final DefaultGeographicBoundingBox bbox = new DefaultGeographicBoundingBox(
                            env.getMinimum(0), env.getMaximum(0), env.getMinimum(1), env.getMaximum(1)
                    );
                    final DefaultExtent extent = new DefaultExtent("", bbox, null, null);
                    ident.getExtents().add(extent);
                }

                // geometry type extraction
                if (ft instanceof FeatureSet) {
                    FeatureSet fs = (FeatureSet) ft;
                    try {
                        final List<? extends PropertyType> geometries = fs.getType().getProperties(true).stream()
                                        .filter(IS_NOT_CONVENTION)
                                        .filter(AttributeConvention::isGeometryAttribute)
                                        .collect(Collectors.toList());
                        for (PropertyType geometry : geometries) {
                            final GeometricObjectType geomType = getGeomTypeFromJTS(geometry);
                            if (geomType != null) {
                                DefaultVectorSpatialRepresentation sr = new DefaultVectorSpatialRepresentation();
                                sr.getGeometricObjects().add(new DefaultGeometricObjects(geomType));
                                md.getSpatialRepresentationInfo().add(sr);
                            }
                        }
                    } catch (PropertyNotFoundException ex) {
                        LOGGER.log(Level.WARNING, "No default Geometry in vector data:{0}", getName());
                    }
                }
            }
            return md;
        } catch (DataStoreException | TransformException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public String getResourceCRSName() throws ConstellationStoreException {
        try {
            final Resource ft = StoreUtilities.findResource(store, getName().toString());
            if (ft instanceof DataSet) {
                Envelope env = FeatureStoreUtilities.getEnvelope((DataSet) ft);
                if (env != null) {
                    final CoordinateReferenceSystem crs = env.getCoordinateReferenceSystem();
                    if (crs != null) {
                        final String crsIdentifier = ReferencingUtilities.lookupIdentifier(crs, true);
                        if (crsIdentifier != null) {
                            return crsIdentifier;
                        }
                    }
                }
            }
        } catch(Exception ex) {
            LOGGER.finer(ex.getMessage());
        }
        return null;
    }

    private static GeometricObjectType getGeomTypeFromJTS(PropertyType defaultGeometry) {
        if (defaultGeometry != null) {
            while (defaultGeometry instanceof Operation) {
                defaultGeometry = (PropertyType) ((Operation) defaultGeometry).getResult();
            }
            Class binding = ((AttributeType)defaultGeometry).getValueClass();
            if (Point.class.equals(binding)) {
                return GeometricObjectType.POINT;
            } else if (LineString.class.equals(binding)) {
                return GeometricObjectType.CURVE;
            } else if (Polygon.class.equals(binding)) {
                return GeometricObjectType.SURFACE;
            } else if (GeometryCollection.class.equals(binding) ||
                       MultiLineString.class.equals(binding) ||
                       MultiPoint.class.equals(binding) ||
                       MultiPolygon.class.equals(binding)) {
                return GeometricObjectType.COMPLEX;
            } else if (Geometry.class.equals(binding)) {
                return GeometricObjectType.COMPLEX;
            } else if (binding != null) {
                LOGGER.info("Unexpected default geometry type:" + binding.getName());
            }
        }
        return null;
    }
}
