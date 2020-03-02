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

import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.util.logging.Logging;
import org.constellation.api.ServiceDef.Query;
import org.geotoolkit.util.DateRange;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.operation.TransformException;
import java.util.Date;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.storage.Resource;
import org.constellation.dto.DataDescription;
import org.constellation.dto.ProviderPyramidChoiceList;
import org.constellation.dto.StatInfo;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.repository.DataRepository;
import org.opengis.util.GenericName;


/**
 * Abstract layer, handle name and styles.
 *
 * @author Johann Sorel (Geomatys)
 * @author Cédric Briançon (Geomatys)
 */
public abstract class AbstractData implements Data{

    protected static final Logger LOGGER = Logging.getLogger("org.constellation.provider");

    /**
     * Layer name
     */
    protected final GenericName name;

    public AbstractData(GenericName name){
        this.name = name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GenericName getName() {
        return name;
    }

    @Override
    public Resource getOrigin(){
        return null;
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

    @Override
    public String getSubType() throws ConstellationStoreException {
        return null;
    }

    @Override
    public Boolean isRendered() {
        return null;
    }

    @Override
    public ProviderPyramidChoiceList.CachePyramid getPyramid() throws ConstellationStoreException {
        return null;
    }

    @Override
    public boolean isGeophysic() throws ConstellationStoreException {
        return false;
    }

    @Override
    public DefaultMetadata getResourceMetadata() throws ConstellationStoreException {
        return null;
    }

    @Override
    public DataDescription getDataDescription(StatInfo statInfo) throws ConstellationStoreException {
        return null;
    }

    @Override
    public void computeStatistic(int dataId, DataRepository dataRepository) {
        //do nothing
    }

}
