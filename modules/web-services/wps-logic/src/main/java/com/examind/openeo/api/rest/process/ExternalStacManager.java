package com.examind.openeo.api.rest.process;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.constellation.configuration.Application;
import org.geotoolkit.openeo.dto.ResponseMessage;
import org.geotoolkit.openeo.process.dto.Process;
import org.geotoolkit.openeo.process.dto.ProcessDescriptionArgument;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.constellation.configuration.AppProperty.EXA_OPENEO_EXTERNAL_STAC_PER_WPS_SERVICE;
import static org.geotoolkit.openeo.process.OpenEOUtils.examindProcessIdToOpenEOProcessId;

/**
 * Management class for external STAC use cases.
 *
 * @author Quentin Bialota (Geomatys)
 */
public class ExternalStacManager {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger("com.examind.openeo.api.rest.process");

    /**
     * REST template used to make HTTP calls.
     */
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Cache of external STAC endpoints per serviceId.
     */
    private final Map<String, Pair<Boolean, String>> externalStacByServiceId = new HashMap<>();

    /**
     * Check if for the given serviceId an external STAC endpoint is configured.
     * @param serviceId the WPS serviceId
     * @return true if an external STAC endpoint is configured, false otherwise
     */
    public boolean isExternalStac(String serviceId) {
        String stacUrl = remoteStacUrl(serviceId);
        return stacUrl != null;
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
     * Get the list of collection ids from the external STAC endpoint configured for the given serviceId.
     *
     * @param serviceId the WPS serviceId
     * @return the list of collection ids, or an empty list
     */
    public List<String> getCollectionIds(String serviceId) {
        String stacUrl = remoteStacUrl(serviceId);
        if (stacUrl == null) return List.of();
        try {
            String url = stacUrl.endsWith("/") ? stacUrl + "collections" : stacUrl + "/collections";
            String json = restTemplate.getForObject(url, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            JsonNode collections = root.get("collections");
            if (collections != null && collections.isArray()) {
                List<String> ids = new java.util.ArrayList<>();
                for (JsonNode coll : collections) {
                    JsonNode id = coll.get("id");
                    if (id != null) ids.add(id.asText());
                }
                return ids;
            }
        } catch (JsonProcessingException ex) {
            LOGGER.log(Level.WARNING, "Error getting collections from STAC endpoint : " + ex.getMessage(), ex);
        }
        return List.of();
    }

    /**
     * Check if a collection exists in the external STAC endpoint configured for the given serviceId.
     *
     * @param serviceId the WPS serviceId
     * @param collectionId the collection id to check
     * @return true if the collection exists, false otherwise
     */
    public boolean collectionExists(String serviceId, String collectionId) {
        String stacUrl = remoteStacUrl(serviceId);
        if (stacUrl == null) return false;
        try {
            String url = (stacUrl.endsWith("/") ? stacUrl : stacUrl + "/") + "collections/" + collectionId;
            String json = restTemplate.getForObject(url, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            return root.get("id") != null && root.get("id").asText().equals(collectionId);
        } catch (JsonProcessingException | HttpClientErrorException e) {
            LOGGER.log(Level.INFO, "Could not find collection " + collectionId + ": " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Check in the given process if there is any load_collection process
     * and if the collection id exists in the external STAC endpoint configured
     * for the given serviceId.
     * @param process the process to check
     * @param serviceId the WPS serviceId
     * @param listError the list to fill with errors found
     */
    public void checkProcessLoadCollectionsWithExternalStac(Process process, String serviceId, List<ResponseMessage> listError) {
        if (isExternalStac(serviceId)) {
            process.getProcessGraph().forEach(
                    (key, value) -> {
                        try {
                            String processId = examindProcessIdToOpenEOProcessId(value.getProcessId());
                            if (processId.equalsIgnoreCase("load_collection")) {
                                ProcessDescriptionArgument arg = value.getArguments().get("id");
                                if (arg == null || arg.getValue() == null) {
                                    listError.add(new ResponseMessage(UUID.randomUUID().toString(), "InvalidArgument",
                                            "In process node " + key + " : Info : The 'id' argument is missing for the 'load_collection' process.", List.of()));
                                } else {
                                    String collectionId = null;
                                    if (arg.getValue() instanceof String) {
                                        collectionId = (String) arg.getValue();
                                    }

                                    if (collectionId == null || collectionId.isEmpty()) {
                                        listError.add(new ResponseMessage(UUID.randomUUID().toString(), "InvalidArgument",
                                                "In process node " + key + " : Info : The 'id' argument for the 'load_collection' process is invalid.", List.of()));
                                    }

                                    if (!collectionExists(serviceId, collectionId)) {
                                        listError.add(new ResponseMessage(UUID.randomUUID().toString(), "CollectionNotFound",
                                                "In process node " + key + " : Info : The collection with id '" + collectionId + "' does not exist in the external STAC endpoint.", List.of()));
                                    }

                                    List<String> downloadableAssets = this.getDownloadableAssetUrls(serviceId, collectionId);
                                    if (downloadableAssets.isEmpty()) {
                                        listError.add(new ResponseMessage(UUID.randomUUID().toString(), "NoDownloadableAssets",
                                                "In process node " + key + " : Info : The collection with id '" + collectionId + "' does not contain any downloadable assets in the external STAC endpoint.", List.of()));

                                    }
                                }
                            }
                        } catch (UnsupportedOperationException | IllegalArgumentException ex) {
                            listError.add(new ResponseMessage(UUID.randomUUID().toString(), "InvalidArgument",
                                    "In process node " + key + " : Info : " + ex.getMessage(), List.of()));
                        }
                    }
            );
        }
    }

    public List<String> getDownloadableAssetUrls(String serviceId, String collectionId) {
        String stacUrl = remoteStacUrl(serviceId);
        if (stacUrl == null) return List.of();

        try {
            String url = (stacUrl.endsWith("/") ? stacUrl : stacUrl + "/") + "collections/" + collectionId + "/items";
            // fetch items from the collection
            String json = restTemplate.getForObject(url, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            JsonNode features = root.get("features");
            List<String> result = new java.util.ArrayList<>();
            if (features != null && features.isArray()) {
                for (JsonNode feature : features) {
                    JsonNode assets = feature.get("assets");
                    if (assets != null && assets.isObject()) {
                        // Each asset is a JSON object with at least an 'href' property
                        assets.fields().forEachRemaining(entry -> {
                            JsonNode hrefNode = entry.getValue().get("href");
                            if (hrefNode != null)
                                result.add(hrefNode.asText());
                        });
                    }
                }
            }
            return result;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error getting downloadable assets from STAC: " + e.getMessage(), e);
        }
        return List.of();
    }
}
