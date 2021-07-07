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
package org.constellation.sos.io.generic;

import java.util.ArrayList;
import java.util.Collections;
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
import static org.constellation.api.CommonConstants.OBSERVATION_QNAME;
import org.constellation.dto.service.config.generic.Automatic;
import org.constellation.exception.ConstellationMetadataException;
import org.geotoolkit.data.om.xml.XmlObservationUtils;
import org.geotoolkit.util.NamesExt;
import org.geotoolkit.observation.AbstractObservationStore;
import org.geotoolkit.observation.ObservationFilterReader;
import org.geotoolkit.observation.ObservationReader;
import org.geotoolkit.observation.ObservationWriter;
import org.geotoolkit.observation.xml.AbstractObservation;
import org.geotoolkit.observation.xml.Process;
import org.geotoolkit.sos.netcdf.ExtractionResult;
import org.geotoolkit.sos.xml.ResponseModeType;
import org.geotoolkit.storage.DataStores;
import org.geotoolkit.swe.xml.PhenomenonProperty;
import org.opengis.metadata.Metadata;
import org.opengis.observation.Observation;
import org.opengis.observation.Phenomenon;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.temporal.TemporalGeometricPrimitive;
import org.opengis.util.GenericName;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class SOSGenericObservationStore extends AbstractObservationStore {

    private ObservationReader reader;
    private ObservationWriter writer;
    private ObservationFilterReader filter;

    public SOSGenericObservationStore(final ParameterValueGroup params) throws DataStoreException {
        super(params);
        try {
            final Automatic conf = (Automatic) params.parameter(SOSGenericObservationStoreFactory.CONFIGURATION.getName().toString()).getValue();

            final Map<String,Object> properties = getBasicProperties();

            reader = new DefaultGenericObservationReader(conf, properties);
            writer = null;
            filter = new GenericObservationFilter(conf, properties, reader);
        } catch(ConstellationMetadataException ex) {
            throw new DataStoreException(ex);
        }
    }

    @Override
    public DataStoreProvider getProvider() {
        return DataStores.getProviderById(SOSGenericObservationStoreFactory.NAME);
    }

    @Override
    public Metadata getMetadata() throws DataStoreException {
        final String name = "generic-observation";
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
    public ExtractionResult getResults(String affectedSensorID, List<String> sensorIDs, Set<Phenomenon> phenomenons, Set<org.opengis.observation.sampling.SamplingFeature> samplingFeatures) throws DataStoreException {
        if (affectedSensorID != null) {
            LOGGER.warning("GenericObservation store does not allow to override sensor ID");
        }
        final ExtractionResult result = new ExtractionResult();
        result.spatialBound.initBoundary();

        final ObservationFilterReader currentFilter = getFilter();
        currentFilter.setProcedure(sensorIDs);

        final Set<String> observationIDS = currentFilter.filterObservation();
        for (String oid : observationIDS) {
            final AbstractObservation o = (AbstractObservation) reader.getObservation(oid, OBSERVATION_QNAME, ResponseModeType.INLINE, "2.0.0");
            final Process proc          =  o.getProcedure();
            final ExtractionResult.ProcedureTree procedure = new ExtractionResult.ProcedureTree(proc.getHref(), proc.getName(), proc.getDescription(), "Component", "timeseries");
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
                result.spatialBound.appendLocation(o.getSamplingTime(), o.getFeatureOfInterest());
                procedure.spatialBound.appendLocation(o.getSamplingTime(), o.getFeatureOfInterest());
                result.observations.add(o);
            }
        }


        return result;
    }

    @Override
    public List<ExtractionResult.ProcedureTree> getProcedures() throws DataStoreException {
        final List<ExtractionResult.ProcedureTree> result = new ArrayList<>();

        // TODO optimize we don't need to call the filter here
        final ObservationFilterReader currentFilter = (ObservationFilterReader) getFilter();
        final List<Observation> observations = currentFilter.getObservations(Collections.emptyMap());
        for (Observation obs : observations) {
            final AbstractObservation o = (AbstractObservation)obs;
            final Process proc          =  o.getProcedure();
            final ExtractionResult.ProcedureTree procedure = new ExtractionResult.ProcedureTree(proc.getHref(), proc.getName(), proc.getDescription(), "Component", "timeseries");

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
            procedure.spatialBound.appendLocation(obs.getSamplingTime(), obs.getFeatureOfInterest());
        }
        return result;
    }

    @Override
    public void close() throws DataStoreException {
        if (reader != null) reader.destroy();
        if (writer != null) writer.destroy();
        if (filter != null) filter.destroy();
    }

    @Override
    public TemporalGeometricPrimitive getTemporalBounds() throws DataStoreException {
        final ExtractionResult result = new ExtractionResult();
        result.spatialBound.initBoundary();
        result.spatialBound.addTime(reader.getEventTime("2.0.0"));
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
    public ObservationFilterReader getFilter() {
        return new GenericObservationFilter((GenericObservationFilter) filter);
    }

    @Override
    public Optional<GenericName> getIdentifier() {
        return Optional.empty();
    }
}
