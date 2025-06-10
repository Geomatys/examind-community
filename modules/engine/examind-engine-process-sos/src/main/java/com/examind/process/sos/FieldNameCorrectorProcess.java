/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com/
 *
 * Copyright 2024 Geomatys.
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
package com.examind.process.sos;

import static com.examind.process.sos.FieldNameCorrectorDescriptor.OBSERVATION_PROVIDER_ID;
import static com.examind.process.sos.FieldNameCorrectorDescriptor.SENSOR_ID;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.util.UnconvertibleObjectException;
import org.constellation.business.IDatasourceBusiness;
import org.constellation.dto.ProviderBrief;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.provider.DataProvider;
import org.constellation.provider.DataProviderFactory;
import org.constellation.provider.DataProviders;
import static org.constellation.provider.DataProviders.getFactory;
import org.constellation.provider.ObservationProvider;
import org.constellation.repository.ProviderRepository;
import org.constellation.util.ParamUtilities;
import org.geotoolkit.observation.OMUtils;
import org.geotoolkit.observation.model.ComplexResult;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.Phenomenon;
import org.geotoolkit.observation.model.ResponseMode;
import org.geotoolkit.observation.query.ObservationQuery;
import org.geotoolkit.observation.query.ObservedPropertyQuery;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.geotoolkit.observation.model.Observation;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class FieldNameCorrectorProcess extends AbstractCstlProcess {
 
    @Autowired
    private ProviderRepository repository;
    
    @Autowired
    private IDatasourceBusiness dsBusiness;
            
    public FieldNameCorrectorProcess(final ProcessDescriptor desc, final ParameterValueGroup input) {
        super(desc,input);
    }

    @Override
    protected void execute() throws ProcessException {
        final String providerId = inputParameters.getValue(OBSERVATION_PROVIDER_ID);
        final String sensorId = inputParameters.getValue(SENSOR_ID);
        
        try {
            ProviderBrief config = repository.findByIdentifier(providerId);
            if(config == null) throw new ProcessException("No provider configuration for id " + providerId, this);
            
            DataProvider provider = DataProviders.getProvider(config.getId());
            if (!(provider instanceof ObservationProvider)) throw new ProcessException(providerId + "is not an observation.", this);
            ObservationProvider op = (ObservationProvider) provider;
            
            //find factory
            final DataProviderFactory factory = getFactory(config.getImpl());
            if(factory==null) throw new ProcessException("No provider factory for id " + config.getImpl(), this);
            
            //read provider parameters
            ParameterValueGroup params;
            try {
                params = (ParameterValueGroup) ParamUtilities.readParameter(config.getConfig(), factory.getProviderDescriptor());
            } catch (IOException | UnconvertibleObjectException ex) {
                throw new ProcessException("Error while reading provider configuration for:" + providerId, this, ex);
            }
            
            //parameter is a choice of different types
            //extract the first one
            params = params.groups("choice").get(0);
            ParameterValueGroup factoryconfig = null;
            for(GeneralParameterValue val : params.values()){
                if(val instanceof ParameterValueGroup){
                    factoryconfig = (ParameterValueGroup) val;
                    break;
                }
            }

            if (factoryconfig == null) {
               throw new ProcessException("No configuration for feature store source.", this);
            }
            Parameters parameters = Parameters.castOrWrap(factoryconfig);
            
            final ParameterDescriptorGroup desc = parameters.getDescriptor();
            final Integer dsId  = parameters.getValue((ParameterDescriptor<Integer>) desc.descriptor("datasource-id"));
            final DataSource source =  dsBusiness.getSQLDatasource(dsId).orElse(null);
            
            String schemPrefix = parameters.getValue((ParameterDescriptor<String>) parameters.getDescriptor().descriptor("schema-prefix"));
            if (schemPrefix == null) {
                schemPrefix = "";
            }
            
            List<Phenomenon> phenomenons = op.getPhenomenon(new ObservedPropertyQuery(true)).stream().map(p -> (Phenomenon)p).toList();
            Map<String, String> phenLabels = new HashMap<>();
            List<Observation> obs = op.getObservations(new ObservationQuery(OMUtils.OBSERVATION_QNAME, ResponseMode.RESULT_TEMPLATE, null));
            for (Observation ob : obs) {
                String pid = ob.getProcedure().getId();
                try (Connection c = source.getConnection();
                     PreparedStatement stmt = c.prepareStatement("UPDATE \"" + schemPrefix + "om\".\"procedure_descriptions\" SET \"label\"= ? where \"procedure\"= ? AND \"field_name\" = ?")) {
                    stmt.setString(2, pid);
                    if (ob.getResult() instanceof ComplexResult cr) {
                        for (Field f : cr.getFields()) {
                            String label = getFieldLabel(f.name, phenomenons, phenLabels);
                            if (label != null) {
                                stmt.setString(1, label);
                                stmt.setString(3, f.name);
                                stmt.executeUpdate();
                            }
                        }
                    }
                }
            }
            
        } catch (Exception ex) {
            throw new ProcessException("Error while accessing the provider: " + providerId, this, ex);
        }
    }
    
    private static String getFieldLabel(String fieldName, List<Phenomenon> phenomenons,  Map<String, String> phenLabels) {
        if (phenLabels.containsKey(fieldName)) {
            return phenLabels.get(fieldName);
        } else {
            for (Phenomenon phen : phenomenons) {
                if (phen.getId().equals(fieldName)) {
                    phenLabels.put(fieldName, phen.getName());
                    return phen.getName();
                }
            }
        }
        return null;
    }
}
