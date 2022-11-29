/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2014, Geomatys
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.constellation.sos.io.lucene;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import static org.constellation.api.CommonConstants.RESPONSE_FORMAT_V100_XML;
import static org.constellation.api.CommonConstants.RESPONSE_FORMAT_V200_XML;
import org.constellation.sos.io.filesystem.FileObservationReader;
import org.constellation.sos.io.filesystem.FileObservationWriter;
import org.geotoolkit.observation.AbstractObservationStore;
import org.geotoolkit.observation.ObservationFilterReader;
import org.geotoolkit.observation.ObservationReader;
import org.geotoolkit.observation.ObservationStoreCapabilities;
import org.geotoolkit.observation.ObservationWriter;
import org.geotoolkit.observation.model.ObservationDataset;
import org.geotoolkit.observation.model.ResponseMode;
import org.geotoolkit.storage.DataStores;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.temporal.TemporalGeometricPrimitive;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class SOSLuceneObservationStore extends AbstractObservationStore {

    private final ObservationReader reader;
    private final ObservationWriter writer;
    private final LuceneObservationFilterReader filter;

    public SOSLuceneObservationStore(final ParameterValueGroup params) throws DataStoreException {
        super(params);

        final Path dataDir = (Path) params.parameter(SOSLuceneObservationStoreFactory.DATA_DIRECTORY.getName().toString()).getValue();
        final Path confDir = (Path) params.parameter(SOSLuceneObservationStoreFactory.CONFIG_DIRECTORY.getName().toString()).getValue();

        final Map<String,Object> properties = getBasicProperties();

        reader = new FileObservationReader(dataDir, properties);
        writer = new FileObservationWriter(dataDir, confDir, properties);
        filter = new LuceneObservationFilterReader(confDir, properties, reader);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public DataStoreProvider getProvider() {
        return DataStores.getProviderById(SOSLuceneObservationStoreFactory.NAME);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    protected String getStoreIdentifier() {
        return "lucene-observation";
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void close() throws DataStoreException {
        if (reader != null) reader.destroy();
        if (writer != null) writer.destroy();
        if (filter != null) filter.destroy();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public TemporalGeometricPrimitive getTemporalBounds() throws DataStoreException {
        final ObservationDataset result = new ObservationDataset();
        result.spatialBound.addTime(reader.getEventTime());
        return result.spatialBound.getTimeObject();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public ObservationReader getReader() {
        return reader;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public ObservationWriter getWriter() {
        return writer;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public ObservationStoreCapabilities getCapabilities() {
        final Map<String, List<String>> results = new HashMap<>();
        results.put("1.0.0", Arrays.asList(RESPONSE_FORMAT_V100_XML));
        results.put("2.0.0", Arrays.asList(RESPONSE_FORMAT_V200_XML));
        final List<ResponseMode> responseMode = Arrays.asList(ResponseMode.INLINE, ResponseMode.RESULT_TEMPLATE);
        return new ObservationStoreCapabilities(true, false, false, new ArrayList<>(), results, responseMode, true);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public ObservationFilterReader getFilter() {
        try {
            return new LuceneObservationFilterReader(filter);
        } catch (DataStoreException ex) {
            LOGGER.log(Level.WARNING, null, ex);
        }
        return null;
    }
}
