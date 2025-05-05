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

package org.constellation.api.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.apache.sis.storage.DataStoreProvider;
import org.constellation.business.IProviderBusiness;
import org.constellation.dto.DataBrief;
import org.constellation.dto.ParameterValues;
import org.constellation.dto.ProviderConfiguration;
import org.constellation.dto.SimpleValue;
import org.constellation.exception.ConfigurationException;
import org.constellation.provider.Data;
import org.constellation.provider.DataProvider;
import org.constellation.provider.DataProviders;
import org.opengis.util.GenericName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.web.bind.annotation.RequestMethod.*;
import org.springframework.web.bind.annotation.RequestParam;
import org.constellation.dto.ProviderBrief;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.provider.FeatureData;

/**
 * RestFull API for provider management/operations.
 *
 * @author Fabien Bernard (Geomatys)
 * @author Johann Sorel (Geomatys)
 * @version 0.9
 * @since 0.9
 */
@RestController
@RequestMapping("/providers")
public class ProviderRestAPI extends AbstractRestAPI {

    @Autowired
    private IProviderBusiness providerBusiness;

    /**
     * List all providers.
     *
     * @return list of providers
     */
    @RequestMapping(method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getProviders() {
        final List<ProviderBrief> providers = providerBusiness.getProviders();
        return new ResponseEntity(providers,OK);
    }

    /**
     * List all providers services.
     *
     * @return list of providers services
     */
    @RequestMapping(value="/services",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getServices() {
        final List<String> lst = new ArrayList<>();
        for (DataStoreProvider dp : DataProviders.listAcceptedProviders(true)) {
            lst.add(dp.getClass().getName() + " ("+dp.getShortName()+")");
        }
        return new ResponseEntity(lst,OK);
    }

    /**
     * Reload a provider.
     *
     * @param providerId the provider ID as integer.
     * 
     * @return HTTP code 200 if the reload went well.
     */
    @RequestMapping(value="/{id}/reload",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity reloadProvider(@PathVariable("id") final int providerId) {
        try {
            providerBusiness.reload(providerId);
            return new ResponseEntity(OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Test a provider configuration to see if its valid an contains recognized data.
     * 
     * @param providerIdentifier assigned identifier.
     * @param configuration provider configuration.
     * 
     * @return HTTP code 200 if the configuration is valid.
     */
    @RequestMapping(value="/{id}/test",method=POST,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity test(
            @PathVariable("id") final String providerIdentifier,
            @RequestBody final ProviderConfiguration configuration) {

        try {
            final boolean dataFound = providerBusiness.test(providerIdentifier, configuration);
            if (!dataFound){
                LOGGER.log(Level.WARNING, "non data found for provider: {0}", providerIdentifier);
                return new ErrorMessage().message("Unable to find any data, please check the database parameters and make sure that the database is properly configured.").build();
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Cannot open provider "+providerIdentifier, e);
            return new ErrorMessage(e).build();
        }
        return new ResponseEntity(OK);
    }

    @RequestMapping(value="/{id}",method=PUT,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity update(
            @PathVariable("id") final String id,
            @RequestBody        final ProviderConfiguration config) {
        if (readOnlyAPI) return readOnlyModeActivated();
        try {
            providerBusiness.update(id, config);
        } catch(Exception ex){
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            return new ErrorMessage(ex).build();
        }
        return new ResponseEntity(OK);
    }

    /**
     * Create a new provider from the given configuration.
     *
     * @param id
     * @param createdata
     * @param config
     * @return
     */
    @RequestMapping(value="/{id}",method=POST,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity create(
            @PathVariable("id") final String id,
            @RequestParam("createdata") boolean createdata,
            @RequestBody final ProviderConfiguration config) {
        if (readOnlyAPI) return readOnlyModeActivated();
        try {
            Integer prId = providerBusiness.create(id, config);
            if (createdata) {
                providerBusiness.createOrUpdateData(prId, null, true, false, null);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
        return new ResponseEntity(OK);
    }

    @RequestMapping(value="/{id}/createprj",method=POST,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity createPrj(
            @PathVariable("id")       final String providerIdentifier,
            @RequestBody              final Map<String,String> epsgCode) {
        if (readOnlyAPI) return readOnlyModeActivated();
        final DataProvider provider;
        try {
            provider = DataProviders.getProvider(providerIdentifier);
        } catch (ConfigurationException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            return new ErrorMessage(ex).build();
        }

        try {
            if (DataProviders.proceedToCreatePrj(provider, epsgCode)) {
                return new ResponseEntity(OK);
            }
            return new ErrorMessage().message("Cannot creates the prj file for the data. the operation is not implemented yet for this format.").build();

        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Delete a provider with the given id.
     *
     * @param id provider identifier.
     * @return
     */
    @RequestMapping(value="/{id}",method=DELETE,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity delete(@PathVariable("id") final Integer id) {
        if (readOnlyAPI) return readOnlyModeActivated();
        try {
            providerBusiness.removeProvider(id);
            return new ResponseEntity(OK);
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     *
     * @param dataName
     * @param id
     * @param property
     * @return
     */
    @RequestMapping(value="/{id}/{dataName}/{property}/propertyValues",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity propertyValues(
            @PathVariable("id")         final String id,
            @PathVariable("dataName")   final String dataName,
            @PathVariable("property")   final String property) {
        try {
            final Data data = getProviderData(id, dataName);
            if (data instanceof FeatureData) {
                return new ResponseEntity(((FeatureData)data).getPropertyValues(property),OK);
            } else {
                throw new ConstellationStoreException("No data " + dataName + " found in provider:" + id + " (or is not a faure data)");
            }

        } catch (Exception  ex) {
            LOGGER.log(Level.WARNING, "Cannot retrieve information for data " + dataName, ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Indicate if given provider contains a geophysic data.
     *
     * @param id
     * @param dataName
     * @return
     */
    @RequestMapping(value="/{id}/{dataName}/isGeophysic",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity isGeophysic(@PathVariable("id") final String id, @PathVariable("dataName") final String dataName) {
        boolean isGeophysic = false;
        try {
            final Data data = getProviderData(id, dataName);
            if (data != null) {
                isGeophysic = data.isGeophysic();
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Cannot retrieve information for data " + dataName, ex);
            return new ErrorMessage(ex).build();
        }
        return new ResponseEntity(new SimpleValue(isGeophysic), OK);
    }

    /**
     * List the data names in a provider
     *
     * @param providerId Identifier of the provider.
     *
     * @return a Map of index / data name
     */
    @RequestMapping(value="/{id}/datas/name",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getDataNamesList(@PathVariable("id") int providerId) {
        try {
            final DataProvider provider = DataProviders.getProvider(providerId);
            if (provider == null) {
                return new ResponseEntity(NOT_FOUND);
            }
            final Set<GenericName> nameSet = provider.getKeys();
            final List<String> names = new ArrayList<>();
            for (GenericName n : nameSet) {
                names.add(n.tip().toString());
            }
            Collections.sort(names);

            //Search on Metadata to found description
            final Map<String, String> dataDescriptions = new HashMap<>(0);
            for (int i = 0; i < names.size(); i++) {
                dataDescriptions.put(String.valueOf(i), names.get(i));
            }

            //Send String Map via REST
            return new ResponseEntity(new ParameterValues(dataDescriptions), OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING,"Error while accessing provider "+ providerId,ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * List the datas in a provider
     *
     * @param providerId Identifier of the provider.
     *
     * @return A list of {@code DataBrief}.
     */
    @RequestMapping(value="/{id}/datas",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getDataListsForProvider(@PathVariable("id") final int providerId) {
        final List<DataBrief> briefs;
        try {
            briefs = providerBusiness.getDataBriefsFromProviderId(providerId, null, true, false, true, true);
        } catch (Exception ex) {
            return new ErrorMessage(ex).build();
        }
        return new ResponseEntity(briefs, OK);
    }

    private Data getProviderData(final String providerId, final String dataName) throws ConfigurationException {
        Integer prId = providerBusiness.getIDFromIdentifier(providerId);
        final Data data  = DataProviders.getProviderData(prId, null, dataName);
        if (data == null) {
            throw new ConfigurationException("No data named \"" + dataName + "\" in provider with id \"" + prId + "\".");
        }
        return data;
    }
}
