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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;
import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.geometry.Envelopes;

import org.opengis.geometry.Envelope;
import org.opengis.util.GenericName;

import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.map.MapItem;
import org.apache.sis.map.MapLayer;
import org.apache.sis.storage.DataSet;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.Resource;

import org.constellation.api.ServiceDef.Query;
import org.constellation.dto.DimensionRange;
import org.constellation.dto.SimpleDataDescription;
import org.constellation.dto.StatInfo;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.repository.DataRepository;
import org.geotoolkit.referencing.ReferencingUtilities;
import org.geotoolkit.storage.memory.InMemoryFeatureSet;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.style.Style;


/**
 * Abstract layer, handle name and styles.
 *
 * @author Johann Sorel (Geomatys)
 * @author Cédric Briançon (Geomatys)
 */
public abstract class AbstractData<T extends Resource> implements Data<T> {

    protected static final Logger LOGGER = Logger.getLogger("org.constellation.provider");

    /**
     * Layer name
     */
    protected final GenericName name;
    protected final T origin;
    protected final DataStore store;

    public AbstractData(GenericName name, final T origin, final DataStore store) {
        this.name = name;
        this.origin = origin;
        this.store = store;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GenericName getName() {
        return name;
    }

    @Override
    public T getOrigin() {
        return origin;
    }

    @Override
    public DataStore getStore() {
        return store;
    }

    /**
     * Returns the time range of this layer. The default implementation invoked
     * {@link #getAvailableTimes()} and extract the first and last date from it.
     * Subclasses are encouraged to provide more efficient implementation.
     */
    @Override
    public SortedSet<Date> getDateRange() throws ConstellationStoreException {
        final SortedSet<Date> dates = getAvailableTimes();
        if (dates != null && !dates.isEmpty()) {
            final SortedSet<Date> result = new TreeSet<>();
            result.add(dates.first());
            result.add(dates.last());
            return result;
        }
        return null;
    }

    /**
     * Always returns {@code true}.
     */
    @Override
    public boolean isQueryable(final Query query) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getResourceCRSName() throws ConstellationStoreException {
        try {
            Envelope env = getEnvelope();
            if (env != null) {
                final CoordinateReferenceSystem crs = env.getCoordinateReferenceSystem();
                if (crs != null) {
                    final String crsIdentifier = ReferencingUtilities.lookupIdentifier(crs, true);
                    if (crsIdentifier != null) {
                        return crsIdentifier;
                    }
                }
            }
        } catch(Exception ex) {
            LOGGER.finer(ex.getMessage());
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SortedSet<Date> getAvailableTimes() throws ConstellationStoreException {
        return new TreeSet<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SortedSet<Number> getAvailableElevations() throws ConstellationStoreException {
        return new TreeSet<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SortedSet<DimensionRange> getSampleValueRanges() {
        /*
        FOR history, how to pass from geotk measurement to dimension

        final MeasurementRange<?> firstRange = ranges[0];
        final double minRange = firstRange.getMinDouble();
        final double maxRange = firstRange.getMaxDouble();
        final String defaut = minRange + "," + maxRange;
        final Unit<?> u = firstRange.unit();
        final String unit = (u != null) ? u.toString() : null;
        String unitSymbol;
        try {
            unitSymbol = new org.apache.sis.measure.UnitFormat(Locale.UK).format(u);
        } catch (IllegalArgumentException e) {
            // Workaround for one more bug in javax.measure...
            unitSymbol = unit;
        }*/
        return new TreeSet<>();
    }

    @Override
    public Envelope getEnvelope() throws ConstellationStoreException {
        if (origin instanceof DataSet ds) {
            try {
                return ds.getEnvelope().orElse(null);
            } catch (DataStoreException ex) {
                throw new ConstellationStoreException(ex);
            }
        }
        return null;
    }

    @Override
    public Envelope getEnvelope(CoordinateReferenceSystem crs) throws ConstellationStoreException {
        Envelope env = getEnvelope();
        if (env != null) {
            try {
                return Envelopes.transform(env, crs);
            } catch (TransformException ex) {
                throw new ConstellationStoreException(ex);
            }
        }
        return null;
    }

    @Override
    public String getSubType() throws ConstellationStoreException {
        return null;
    }

    @Override
    public Boolean isRendered() {
        return null;
    }

    @Override
    public boolean isGeophysic() throws ConstellationStoreException {
        return false;
    }

    @Deprecated
    @Override
    public DefaultMetadata getResourceMetadata() throws ConstellationStoreException {
        try {
            return origin == null ? new DefaultMetadata() : new DefaultMetadata(origin.getMetadata());
        } catch (DataStoreException e) {
            throw new ConstellationStoreException(e);
        }
    }

    @Override
    public SimpleDataDescription getDataDescription(StatInfo statInfo, Envelope env) throws ConstellationStoreException {
        final SimpleDataDescription description = new SimpleDataDescription();
        try {
            if (env == null) {
                env = getEnvelope();
            }
        } catch (ConstellationStoreException ex) {
            // we always want to return a description because if not, the UI will not display the data
            LOGGER.finer(ex.getMessage());
        }
        DataProviders.fillGeographicDescription(env, description);
        return description;
    }

    @Override
    public Object computeStatistic(int dataId, DataRepository dataRepository) throws ConstellationStoreException {
        //do nothing
        return null;
    }

    /**
     * Create a default representation showing the bounds of the data.
     * Must be overriden by sub-classes that need a proper display.
     *
     * @param styleI A style to apply or {@code null}.
     */
    @Override
    public MapItem getMapLayer(Style styleI) throws ConstellationStoreException {
        String name = getName().tip().toString();
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName(name);
        ftb.addAttribute(Polygon.class).setName("bounds").addRole(AttributeRole.DEFAULT_GEOMETRY);
        final FeatureType type = ftb.build();

        List<Feature> feats = new ArrayList<>();
        Envelope env = getEnvelope();
        if (env != null) {
            final Feature f1 = type.newInstance();
            f1.setPropertyValue("bounds", DataProviders.getPolygon(env));
            feats.add(f1);
        }
        if (styleI == null) {
            try {
                styleI = DataProviders.getStyle("default-line");
            } catch (ConstellationException ex) {
                throw new ConstellationStoreException(ex);
            }
        }
        final MapLayer maplayer = new MapLayer();
        maplayer.setData(new InMemoryFeatureSet(type, feats));
        maplayer.setStyle((org.apache.sis.style.Style) styleI);
        maplayer.setIdentifier(name);
        maplayer.setTitle(name);
        maplayer.setOpacity(1.0);
        return maplayer;
    }
}
