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
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;
import org.apache.sis.measure.MeasurementRange;

import org.opengis.geometry.Envelope;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.GenericName;

import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.Resource;
import org.apache.sis.util.logging.Logging;

import org.geotoolkit.util.DateRange;

import org.constellation.api.ServiceDef.Query;
import org.constellation.dto.DataDescription;
import org.constellation.dto.SimpleDataDescription;
import org.constellation.dto.StatInfo;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.repository.DataRepository;
import org.geotoolkit.referencing.ReferencingUtilities;
import org.geotoolkit.storage.feature.FeatureStoreUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * Abstract layer, handle name and styles.
 *
 * @author Johann Sorel (Geomatys)
 * @author Cédric Briançon (Geomatys)
 */
public abstract class AbstractData<T extends Resource> implements Data<T> {

    protected static final Logger LOGGER = Logging.getLogger("org.constellation.provider");

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
    public DateRange getDateRange() throws ConstellationStoreException {
        final SortedSet<Date> dates = getAvailableTimes();
        if (dates != null && !dates.isEmpty()) {
            return new DateRange(dates.first(), dates.last());
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
    public final GeographicBoundingBox getGeographicBoundingBox() throws ConstellationStoreException {
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
    public MeasurementRange<?>[] getSampleValueRanges() {
        return new MeasurementRange<?>[0];
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
    public SimpleDataDescription getDataDescription(StatInfo statInfo) throws ConstellationStoreException {
        final SimpleDataDescription description = new SimpleDataDescription();
        try {
            Envelope env = getEnvelope();
            if (env != null) {
                DataProviders.fillGeographicDescription(env, description);
            }
        } catch (ConstellationStoreException ex) {
            // we always want to return a description because if not, the UI will not display the data
            LOGGER.finer(ex.getMessage());
        }
        return description;
    }

    @Override
    public Object computeStatistic(int dataId, DataRepository dataRepository) {
        //do nothing
        return null;
    }

}
