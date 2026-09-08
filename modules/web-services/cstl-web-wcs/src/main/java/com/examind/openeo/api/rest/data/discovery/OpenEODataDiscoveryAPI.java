package com.examind.openeo.api.rest.data.discovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.geotoolkit.ogcapi.model.common.Link;
import org.geotoolkit.stac.dto.Collection;
import org.geotoolkit.stac.dto.Collections;
import org.apache.commons.lang3.tuple.Pair;
import org.constellation.admin.SpringHelper;
import org.constellation.api.ServiceDef;
import org.constellation.api.rest.ErrorMessage;
import org.constellation.business.IServiceBusiness;
import org.constellation.configuration.Application;
import org.constellation.coverage.core.WCSWorker;
import org.constellation.exception.ConfigurationException;
import org.constellation.ws.CstlServiceException;
import org.constellation.ws.Worker;
import org.constellation.ws.rs.GridWebService;
import org.constellation.ws.rs.ResponseObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

import static org.constellation.configuration.AppProperty.EXA_OPENEO_EXTERNAL_STAC_PER_WPS_SERVICE;
import static org.constellation.coverage.core.AtomLinkBuilder.buildDocumentLinks;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 * OpenEO Data Discovery API implementation.
 *
 * @author Quentin BIALOTA (Geomatys)
 */
@RestController
@RequestMapping("openeo/{serviceId:.+}/collections")
public class OpenEODataDiscoveryAPI extends GridWebService<WCSWorker> {

    /**
     * REST template used to make HTTP calls.
     */
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Cache of external STAC endpoints per serviceId.
     */
    private final Map<String, Pair<Boolean, String>> externalStacByServiceId = new HashMap<>();

    /**
     * Default constructor.
     */
    public OpenEODataDiscoveryAPI() {
        // here we use wcs for worker retrieval purpose
        super(ServiceDef.Specification.WCS);
    }

    /**
     * Check if for the given serviceId an external STAC endpoint is configured.
     * We look for the app property "exa.openeo.external.stac.per.wps.service" which
     * must contains pairs of values (serviceId, stacUrl).
     * You can set in your env variable EXA_OPENGEO_EXTERNAL_STAC_PER_WPS_SERVICE a list of values like:
     * ["serviceId1", "https://external-stac-endpoint-1.com", "serviceId2", "https://external-stac-endpoint-2.com"]
     * @param serviceId the WPS serviceId
     * @return the external STAC url or null
     */
    private String remoteStacUrl(String serviceId) {
        Pair<Boolean, String> external = externalStacByServiceId.get(serviceId);
        if (external == null || external.getLeft() == null) {
            String configUrl = serviceConfigStacUrl(serviceId);
            if (configUrl != null) {
                external = Pair.of(true, configUrl);
                externalStacByServiceId.put(serviceId, external);
                return external.getRight();
            }

            List<String> map = Application.getListProperty(EXA_OPENEO_EXTERNAL_STAC_PER_WPS_SERVICE);
            if (map.isEmpty()) {
                externalStacByServiceId.put(serviceId, Pair.of(false, null));
                return null;
            }

            if (map.size() % 2 == 1) {
                LOGGER.log(Level.WARNING, "The app property " +  EXA_OPENEO_EXTERNAL_STAC_PER_WPS_SERVICE + " is not valid (odd numbers of values," +
                        " we need to have an url per serviceId. Cannot set the service for External STAC endpoint.");
                externalStacByServiceId.put(serviceId, Pair.of(false, null));
                return null;
            }

            for (int i = 0; i < map.size(); i += 2) {
                String mapServiceId = map.get(i);
                if (mapServiceId.equals(serviceId)) {
                    external = Pair.of(true, map.get(i + 1));
                    externalStacByServiceId.put(serviceId, external);
                    break;
                }
            }
        }
        return (external != null ? external.getRight() : null);
    }

    /**
     * File system config (per-service "externalStacUrl" property on the WPS service sharing this serviceId)
     * takes priority over the {@code EXA_OPENEO_EXTERNAL_STAC_PER_WPS_SERVICE} app property.
     *
     * @param serviceId the WPS serviceId
     * @return the configured url, or null if none
     */
    private String serviceConfigStacUrl(String serviceId) {
        return SpringHelper.getBean(IServiceBusiness.class).map(sb -> {
            try {
                return sb.getConfiguration("wps", serviceId).getProperty("externalStacUrl");
            } catch (ConfigurationException ex) {
                return null;
            }
        }).orElse(null);
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Fetches a STAC document from the external server and rewrites every {@code href} field
     * (in {@code links[]}, {@code assets{}}, or anywhere else nested in the document, e.g.
     * {@code Item.assets} inside an item collection's {@code features[]}) that points at
     * {@code remoteStacUrl} so it points at {@code proxyBaseUrl} instead. Parses/rewrites the
     * JSON tree structurally rather than doing a whole-body string replace, so unrelated
     * occurrences of the remote URL substring (in an id, title, description, etc.) are left
     * untouched.
     *
     * @param remoteStacUrl base URL of the external STAC server, e.g. {@code https://stac.example.org}
     * @param targetUrl     full external STAC URL to fetch (under {@code remoteStacUrl})
     * @param proxyBaseUrl  base URL under which Examind proxies this external STAC server
     * @return the rewritten document, serialized back to JSON
     */
    private String fetchAndRewriteHrefs(String remoteStacUrl, String targetUrl, String proxyBaseUrl) throws java.io.IOException {
        String rawJson = restTemplate.getForObject(targetUrl, String.class);
        JsonNode root = MAPPER.readTree(rawJson);
        rewriteHrefs(root, remoteStacUrl, proxyBaseUrl);
        return MAPPER.writeValueAsString(root);
    }

    static void rewriteHrefs(JsonNode node, String remoteStacUrl, String proxyBaseUrl) {
        if (node instanceof ObjectNode obj) {
            JsonNode href = obj.get("href");
            if (href != null && href.isTextual() && href.asText().startsWith(remoteStacUrl)) {
                obj.put("href", proxyBaseUrl + href.asText().substring(remoteStacUrl.length()));
            }
            obj.fields().forEachRemaining(entry -> rewriteHrefs(entry.getValue(), remoteStacUrl, proxyBaseUrl));
        } else if (node instanceof ArrayNode arr) {
            arr.forEach(child -> rewriteHrefs(child, remoteStacUrl, proxyBaseUrl));
        }
    }

    /**
     * {@inheritDoc}
     * @param objectRequest if the server receive a POST request in XML/json,
     *        this object contain the request. Else for a GET or a POST kvp
     *        request this parameter is {@code null}
     *
     * @param worker the selected worker on which apply the request.
     *
     * @return
     */
    @Override
    protected ResponseObject treatIncomingRequest(Object objectRequest, WCSWorker worker) {
        String remoteStacUrl = remoteStacUrl(worker.getId());
        if (remoteStacUrl != null) {
            try {
                String targetUrl = remoteStacUrl + "/collections";
                String rawJson = restTemplate.getForObject(targetUrl, String.class);
                String proxyBaseUrl = getServiceURL() + "/openeo/" + worker.getId();

                assert rawJson != null;
                String proxiedJson = rawJson.replace(remoteStacUrl, proxyBaseUrl);

                return new ResponseObject(proxiedJson, MediaType.APPLICATION_JSON, HttpStatus.OK);
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Failed to proxy /collections for serviceId " + worker.getId(), ex);
                return new ResponseObject(HttpStatus.NOT_FOUND);
            }
        } else {
            //For the moment only json format is accepted
            try {
                Collections collections  = buildCollections(worker);
                return new ResponseObject(collections, MediaType.APPLICATION_JSON, HttpStatus.OK);
            } catch (CstlServiceException ex) {
                LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
                return new ResponseObject(HttpStatus.NOT_FOUND);
            }
        }
    }

    /**
     * {@inheritDoc}
     * @param exc        The exception that has been generated during the web-service operation requested.
     * @param serviceDef The service definition, from which the version number of exception report will
     *                   be extracted. if {@code null} the default version of the worker will be used.
     * @param w the selected worker on which apply the request.
     * @param mimeType The mime type to use to return the http response.
     *
     * @return
     */
    @Override
    protected ResponseObject processExceptionResponse(final Exception exc, ServiceDef serviceDef, final Worker w, MediaType mimeType) {
        LOGGER.log(Level.WARNING, exc.getLocalizedMessage(), exc);
        return new ResponseObject(new ErrorMessage(exc));
    }

    /**
     * Get collections list as STAC /collections endpoint.
     * @param serviceId the WPS serviceId
     * @return the response entity containing the collections list
     */
    @RequestMapping(value = {"", "/"}, method = GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity getCollections(@PathVariable("serviceId") String serviceId){
        String remoteStacUrl = remoteStacUrl(serviceId);
        if (remoteStacUrl != null) {
            try {
                String targetUrl = remoteStacUrl + "/collections";
                String rawJson = restTemplate.getForObject(targetUrl, String.class);
                String proxyBaseUrl = getServiceURL() + "/openeo/" + serviceId;

                assert rawJson != null;
                String proxiedJson = rawJson.replace(remoteStacUrl, proxyBaseUrl);

                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(proxiedJson);
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Failed to proxy /collections for serviceId " + serviceId, ex);
                return new ErrorMessage(ex).build();
            }
        } else {
            putServiceIdParam(serviceId);
            final WCSWorker worker = getWorker(serviceId);

            if (worker != null) {
                try {
                    Collections response = buildCollections(worker);
                    return new ResponseObject(response, MediaType.APPLICATION_JSON, HttpStatus.OK).getResponseEntity();
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
                    return new ErrorMessage(ex).build();
                }
            }
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Get a collection by id as STAC /collections/{collectionId} endpoint.
     * @param serviceId the WPS serviceId
     * @param collectionId the collection id
     * @return the response entity containing the collection
     */
    @RequestMapping(value = {"/{collectionId}", "/{collectionId}/"}, method = GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity getCollection(@PathVariable("serviceId") String serviceId,
                                        @PathVariable("collectionId") String collectionId){
        String remoteStacUrl = remoteStacUrl(serviceId);
        if (remoteStacUrl != null) {
            try {
                String targetUrl = remoteStacUrl + "/collections/" + collectionId;
                String rawJson = restTemplate.getForObject(targetUrl, String.class);
                String proxyBaseUrl = getServiceURL() + "/openeo/" + serviceId;

                assert rawJson != null;
                String proxiedJson = rawJson.replace(remoteStacUrl, proxyBaseUrl);

                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(proxiedJson);
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Failed to proxy /collections/" + collectionId + " for serviceId " + serviceId, ex);
                return new ErrorMessage(ex).build();
            }
        } else {
            putServiceIdParam(serviceId);
            final WCSWorker worker = getWorker(serviceId);

            if (worker != null) {
                try {
                    // if the layer does not exist an exception will be thrown
                    final Optional<Collection> layer = worker.getCollections(List.of(collectionId), true).stream().map(collection -> (Collection) collection).findFirst();
                    if (!layer.isEmpty()) {
                        return new ResponseObject(layer.get(), MediaType.APPLICATION_JSON, HttpStatus.OK).getResponseEntity();
                    }
                } catch (CstlServiceException ex) {
                    LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
                    return new ErrorMessage(HttpStatus.NOT_FOUND, ex.getLocalizedMessage()).build();
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
                    return new ErrorMessage(ex).build();
                }
            }
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Get a collection items by id as STAC /collections/{collectionId}/items endpoint.
     * @param serviceId the WPS serviceId
     * @param collectionId the collection id
     * @return the response entity containing the collection items
     */
    @RequestMapping(value = {"/{collectionId}/items", "/{collectionId}/items/"}, method = GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity getCollectionItems(@PathVariable("serviceId") String serviceId,
                                             @PathVariable("collectionId") String collectionId){
        String remoteStacUrl = remoteStacUrl(serviceId);
        if (remoteStacUrl != null) {
            try {
                String targetUrl = remoteStacUrl + "/collections/" + collectionId + "/items";
                String rawJson = restTemplate.getForObject(targetUrl, String.class);
                String proxyBaseUrl = getServiceURL() + "/openeo/" + serviceId;

                assert rawJson != null;
                String proxiedJson = rawJson.replace(remoteStacUrl, proxyBaseUrl);

                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(proxiedJson);
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Failed to proxy /collections/" + collectionId + "/items for serviceId " + serviceId, ex);
                return new ErrorMessage(ex).build();
            }
        } else {
            //TODO : Items are not yet supported natively in Examind
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Get a collection queryables by id as STAC /collections/{collectionId}/queryables endpoint.
     * @param serviceId the WPS serviceId
     * @param collectionId the collection id
     * @return the response entity containing the collection queryables
     */
    @RequestMapping(value = {"/{collectionId}/queryables", "/{collectionId}/queryables/"}, method = GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity getCollectionQueryables(@PathVariable("serviceId") String serviceId,
                                             @PathVariable("collectionId") String collectionId){
        String remoteStacUrl = remoteStacUrl(serviceId);
        if (remoteStacUrl != null) {
            try {
                String targetUrl = remoteStacUrl + "/collections/" + collectionId + "/queryables";
                String rawJson = restTemplate.getForObject(targetUrl, String.class);
                String proxyBaseUrl = getServiceURL() + "/openeo/" + serviceId;

                assert rawJson != null;
                String proxiedJson = rawJson.replace(remoteStacUrl, proxyBaseUrl);

                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(proxiedJson);
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Failed to proxy /collections/" + collectionId + "/queryables for serviceId " + serviceId, ex);
                return new ErrorMessage(ex).build();
            }
        } else {
            //TODO : Queryables are not yet supported natively in Examind
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Build the collections response.
     * @param worker the WCS worker
     * @param format the requested format
     * @return the collections response
     * @throws CstlServiceException if an error occurs
     */
    private Collections buildCollections(final WCSWorker worker) throws CstlServiceException {
        List<Collection> layers = worker.getCollections(List.of(), true).stream().map(collection -> (Collection) collection).toList();

        List<Link> links = new ArrayList<>();
        String url = getServiceURL() + "/openeo/" + worker.getId() + "/collections";
        buildDocumentLinks(url, links);

        return new Collections(layers, links);
    }
}
