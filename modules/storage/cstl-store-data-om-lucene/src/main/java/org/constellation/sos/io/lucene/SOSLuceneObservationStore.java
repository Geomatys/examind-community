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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.apache.sis.metadata.ModifiableMetadata;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.constellation.api.CommonConstants;
import static org.constellation.api.CommonConstants.OBSERVATION_QNAME;
import org.constellation.sos.io.filesystem.FileObservationReader;
import org.constellation.sos.io.filesystem.FileObservationWriter;
import static org.constellation.sos.io.lucene.SOSLuceneObservationStoreFactory.OBSERVATION_ID_BASE;
import static org.constellation.sos.io.lucene.SOSLuceneObservationStoreFactory.OBSERVATION_TEMPLATE_ID_BASE;
import static org.constellation.sos.io.lucene.SOSLuceneObservationStoreFactory.PHENOMENON_ID_BASE;
import static org.constellation.sos.io.lucene.SOSLuceneObservationStoreFactory.SENSOR_ID_BASE;
import org.geotoolkit.data.om.xml.XmlObservationUtils;
import org.geotoolkit.util.NamesExt;
import org.geotoolkit.gml.GMLUtilities;
import org.geotoolkit.gml.xml.AbstractGeometry;
import org.geotoolkit.gml.xml.AbstractRing;
import org.geotoolkit.gml.xml.Envelope;
import org.geotoolkit.gml.xml.LineString;
import org.geotoolkit.gml.xml.Point;
import org.geotoolkit.gml.xml.Polygon;
import org.geotoolkit.gml.xml.v321.TimeInstantType;
import org.geotoolkit.observation.AbstractObservationStore;
import org.geotoolkit.observation.ObservationFilter;
import org.geotoolkit.observation.ObservationFilterReader;
import org.geotoolkit.observation.ObservationReader;
import org.geotoolkit.observation.ObservationWriter;
import org.geotoolkit.observation.xml.AbstractObservation;
import org.geotoolkit.sampling.xml.SamplingFeature;
import org.geotoolkit.sos.netcdf.ExtractionResult;
import org.geotoolkit.sos.netcdf.GeoSpatialBound;
import org.geotoolkit.sos.xml.ResponseModeType;
import org.geotoolkit.storage.DataStores;
import org.geotoolkit.swe.xml.PhenomenonProperty;
import org.opengis.geometry.Geometry;
import org.opengis.metadata.Metadata;
import org.opengis.observation.AnyFeature;
import org.opengis.observation.Observation;
import org.opengis.observation.Phenomenon;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.opengis.temporal.TemporalGeometricPrimitive;
import org.opengis.temporal.TemporalObject;
import org.opengis.util.GenericName;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class SOSLuceneObservationStore extends AbstractObservationStore {

    private final ObservationReader reader;
    private final ObservationWriter writer;
    private final ObservationFilter filter;

    public SOSLuceneObservationStore(final ParameterValueGroup params) throws DataStoreException {
        super(params);

        final Path dataDir = (Path) params.parameter(SOSLuceneObservationStoreFactory.DATA_DIRECTORY.getName().toString()).getValue();
        final Path confDir = (Path) params.parameter(SOSLuceneObservationStoreFactory.CONFIG_DIRECTORY.getName().toString()).getValue();

        final Map<String,Object> properties = new HashMap<>();
        extractParameter(params, CommonConstants.PHENOMENON_ID_BASE, PHENOMENON_ID_BASE, properties);
        extractParameter(params, CommonConstants.OBSERVATION_ID_BASE, OBSERVATION_ID_BASE, properties);
        extractParameter(params, CommonConstants.OBSERVATION_TEMPLATE_ID_BASE, OBSERVATION_TEMPLATE_ID_BASE, properties);
        extractParameter(params, CommonConstants.SENSOR_ID_BASE, SENSOR_ID_BASE, properties);

        reader = new FileObservationReader(dataDir, properties);
        writer = new FileObservationWriter(dataDir, confDir, properties);
        filter = new LuceneObservationFilter(confDir, properties);
    }

    private void extractParameter(final ParameterValueGroup params, String key, ParameterDescriptor<String> param, final Map<String,Object> properties) {
        final String value = (String) params.parameter(param.getName().toString()).getValue();
        if (value != null) {
            properties.put(key, value);
        }
    }

    @Override
    public DataStoreProvider getProvider() {
        return DataStores.getProviderById(SOSLuceneObservationStoreFactory.NAME);
    }

    @Override
    public Metadata getMetadata() throws DataStoreException {
        final String name = "lucene-observation";
        final DefaultMetadata metadata = new DefaultMetadata();
        final DefaultDataIdentification identification = new DefaultDataIdentification();
        final NamedIdentifier identifier = new NamedIdentifier(new DefaultIdentifier(name));
        final DefaultCitation citation = new DefaultCitation(name);
        citation.setIdentifiers(Collections.singleton(identifier));
        identification.setCitation(citation);
        metadata.setIdentificationInfo(Collections.singleton(identification));
        metadata.transitionTo(ModifiableMetadata.State.FINAL);
        return metadata;
    }

    @Override
    public Set<GenericName> getProcedureNames() {
        final Set<GenericName> names = new HashSet<>();
        try {
            for (String process : reader.getProcedureNames()) {
                names.add(NamesExt.create(process));
            }

        } catch (DataStoreException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        return names;
    }

    @Override
    public ExtractionResult getResults() throws DataStoreException {
        return getResults(null);
    }

    @Override
    public ExtractionResult getResults(List<String> sensorIds) throws DataStoreException {
        return getResults(null, sensorIds, new HashSet<>(), new HashSet<>());
    }

    @Override
    public ExtractionResult getResults(final String affectedSensorId, final List<String> sensorIDs) throws DataStoreException {
        return getResults(affectedSensorId, sensorIDs, new HashSet<>(), new HashSet<>());
    }

    @Override
    public ExtractionResult getResults(final String affectedSensorId, final List<String> sensorIDs, Set<Phenomenon> phenomenons, final Set<org.opengis.observation.sampling.SamplingFeature> samplingFeatures) throws DataStoreException {
        if (affectedSensorId != null) {
            LOGGER.warning("SOSLuceneObservationStore does not allow to override sensor ID");
        }

        final ExtractionResult result = new ExtractionResult();
        result.spatialBound.initBoundary();

        final ObservationFilter currentFilter = cloneObservationFilter(filter);
        currentFilter.setProcedure(sensorIDs, null);

        final Set<String> observationIDS = filter.filterObservation();
        for (String oid : observationIDS) {
            final AbstractObservation o = (AbstractObservation) reader.getObservation(oid, OBSERVATION_QNAME, ResponseModeType.INLINE, "2.0.0");
            final ExtractionResult.ProcedureTree procedure = new ExtractionResult.ProcedureTree(o.getProcedure().getHref(), "Component");
            if (sensorIDs == null || sensorIDs.contains(procedure.id)) {
                if (!result.procedures.contains(procedure)) {
                    result.procedures.add(procedure);
                }
                final PhenomenonProperty phenProp = o.getPropertyObservedProperty();
                final List<String> fields = XmlObservationUtils.getPhenomenonsFields(phenProp);
                for (String field : fields) {
                    if (!result.fields.contains(field)) {
                        result.fields.add(field);
                    }
                }
                final Phenomenon phen = XmlObservationUtils.getPhenomenons(phenProp);
                if (!result.phenomenons.contains(phen)) {
                    result.phenomenons.add(phen);
                }
                appendTime(o.getSamplingTime(), result.spatialBound);
                appendTime(o.getSamplingTime(), procedure.spatialBound);
                appendGeometry(o.getFeatureOfInterest(), result.spatialBound);
                appendGeometry(o.getFeatureOfInterest(), procedure.spatialBound);
                result.observations.add(o);
            }
        }
        return result;
    }

    @Override
    public List<ExtractionResult.ProcedureTree> getProcedures() throws DataStoreException {
        final List<ExtractionResult.ProcedureTree> result = new ArrayList<>();

        // TODO optimize we don't need to call the filter here
        final ObservationFilterReader currentFilter = (ObservationFilterReader) cloneObservationFilter(filter);
        final List<Observation> observations = currentFilter.getObservations("2.0.0");
        for (Observation obs : observations) {
            final AbstractObservation o = (AbstractObservation)obs;
            final ExtractionResult.ProcedureTree procedure = new ExtractionResult.ProcedureTree(o.getProcedure().getHref(), "Component");

            if (!result.contains(procedure)) {
                result.add(procedure);
            }
            final PhenomenonProperty phenProp = o.getPropertyObservedProperty();
            final List<String> fields = XmlObservationUtils.getPhenomenonsFields(phenProp);
            for (String field : fields) {
                if (!procedure.fields.contains(field)) {
                    procedure.fields.add(field);
                }
            }
            appendTime(obs.getSamplingTime(), procedure.spatialBound);
            appendGeometry(obs.getFeatureOfInterest(), procedure.spatialBound);
        }
        return result;
    }

    private void appendTime(final TemporalObject time, final GeoSpatialBound spatialBound) {
        if (time instanceof Instant) {
            final Instant i = (Instant) time;
            spatialBound.addDate(i.getDate());
        } else if (time instanceof Period) {
            final Period p = (Period) time;
            spatialBound.addDate(p.getBeginning().getDate());
            spatialBound.addDate(p.getEnding().getDate());
        }
    }

    private void appendGeometry(final AnyFeature feature, final GeoSpatialBound spatialBound){
        if (feature instanceof SamplingFeature) {
            final SamplingFeature sf = (SamplingFeature) feature;
            final Geometry geom = sf.getGeometry();
            final AbstractGeometry ageom;
            if (geom instanceof AbstractGeometry) {
                ageom = (AbstractGeometry)geom;
            } else if (geom != null) {
                ageom = GMLUtilities.getGMLFromISO(geom);
            } else {
                ageom = null;
            }
            spatialBound.addGeometry(ageom);
            spatialBound.addGeometry(ageom);
            extractBoundary(ageom, spatialBound);
            extractBoundary(ageom, spatialBound);
        }
    }

    private void extractBoundary(final AbstractGeometry geom, final GeoSpatialBound spatialBound) {
        if (geom instanceof Point) {
            final Point p = (Point) geom;
            if (p.getPos() != null) {
                spatialBound.addXCoordinate(p.getPos().getOrdinate(0));
                spatialBound.addYCoordinate(p.getPos().getOrdinate(1));
            }
        } else if (geom instanceof LineString) {
            final LineString ls = (LineString) geom;
            final Envelope env = ls.getBounds();
            if (env != null) {
                spatialBound.addXCoordinate(env.getMinimum(0));
                spatialBound.addXCoordinate(env.getMaximum(0));
                spatialBound.addYCoordinate(env.getMinimum(1));
                spatialBound.addYCoordinate(env.getMaximum(1));
            }
        } else if (geom instanceof Polygon) {
            final Polygon p = (Polygon) geom;
            AbstractRing ext = p.getExterior().getAbstractRing();
            // TODO
        }
    }


    @Override
    public void close() throws DataStoreException {
        if (reader != null) reader.destroy();
        if (writer != null) writer.destroy();
        if (filter != null) filter.destroy();
    }

    @Override
    public Set<String> getPhenomenonNames() {
        try {
            return new HashSet(reader.getPhenomenonNames());
        } catch (DataStoreException ex) {
            LOGGER.log(Level.WARNING, "Error while retrieving phenomenons", ex);
        }
        return new HashSet<>();
    }

    @Override
    public TemporalGeometricPrimitive getTemporalBounds() throws DataStoreException {
        final ExtractionResult result = new ExtractionResult();
        result.spatialBound.initBoundary();
        List<String> dates = reader.getEventTime();
        appendTime(new TimeInstantType(dates.get(0)), result.spatialBound);
        appendTime(new TimeInstantType(dates.get(1)), result.spatialBound);
        return result.spatialBound.getTimeObject("2.0.0");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public ObservationReader getReader() {
        return reader;
    }

    @Override
    public ObservationWriter getWriter() {
        return writer;
    }

    @Override
    public ObservationFilter getFilter() {
        return filter;
    }

    @Override
    public ObservationFilter cloneObservationFilter(ObservationFilter toClone) {
        try {
            return new LuceneObservationFilter((LuceneObservationFilter) filter);
        } catch (DataStoreException ex) {
            LOGGER.log(Level.WARNING, null, ex);
        }
        return null;
    }

    @Override
    public Optional<GenericName> getIdentifier() {
        return Optional.empty();
    }
}
