package org.constellation.process.utils.coverage.openeo;

import org.constellation.process.utils.coverage.Utils;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.GridCoverageResource;
import org.constellation.process.AbstractCstlProcess;
import org.geotoolkit.process.Process;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.geotoolkit.process.ProcessFinder;
import org.geotoolkit.processing.AbstractProcess;
import org.geotoolkit.client.openapi.OpenApiConfiguration;
import org.geotoolkit.stac.process.StacClientItemsGetURIsDescriptor;
import org.geotoolkit.stac.client.StacApi;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.NoSuchIdentifierException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.constellation.process.utils.coverage.openeo.LoadStacDescriptor.BANDS;
import static org.constellation.process.utils.coverage.openeo.LoadStacDescriptor.OUTPUT;
import static org.constellation.process.utils.coverage.openeo.LoadStacDescriptor.PROPERTIES;
import static org.constellation.process.utils.coverage.openeo.LoadStacDescriptor.SPATIAL_EXTENT;
import static org.constellation.process.utils.coverage.openeo.LoadStacDescriptor.STAC_URL;
import static org.constellation.process.utils.coverage.openeo.LoadStacDescriptor.TEMPORAL_EXTENT;
import static org.constellation.process.utils.coverage.openeo.LoadStacDescriptor.EXTERNAL_STAC_CUSTOM_PROCESS;

/**
 * @author Quentin BIALOTA (Geomatys)
 */
public class LoadStacProcess extends AbstractCstlProcess  {

    /**
     * REST template used to make HTTP calls.
     */
    private final RestTemplate restTemplate = new RestTemplate();

    public LoadStacProcess(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
        super(desc, parameter);
    }

    @Override
    protected void execute() throws ProcessException {
        String stacURL = inputParameters.getMandatoryValue(STAC_URL);
        String externalStacCustomProcess = inputParameters.getValue(EXTERNAL_STAC_CUSTOM_PROCESS);
        Envelope spatialExtent = inputParameters.getValue(SPATIAL_EXTENT);
        String[] temporalExtent = inputParameters.getValue(TEMPORAL_EXTENT);
        String[] bands = inputParameters.getValue(BANDS);
        Map properties = inputParameters.getValue(PROPERTIES);

        DataStore ds;
        temporalExtent = Utils.formatTemporalExtent(temporalExtent);

        // Custom external STAC process handling
        if (externalStacCustomProcess != null && !externalStacCustomProcess.isEmpty()) {
            final ProcessDescriptor stacCustomProcess;
            try {
                stacCustomProcess = ProcessFinder.getProcessDescriptor("examind-dynamic", externalStacCustomProcess);
                final Parameters paramsLoad = Parameters.castOrWrap(stacCustomProcess.getInputDescriptor().createValue());

                paramsLoad.parameter("stac_url").setValue(stacURL);

                final Process externalStacProcess = stacCustomProcess.createProcess(paramsLoad);
                if (externalStacProcess instanceof AbstractProcess ap) {
                    ap.setJobId(UUID.randomUUID().toString());
                }

                Object result = externalStacProcess.call().parameter("downloaded_asset").getValue();
                if (!(result instanceof File)) {
                    throw new ProcessException("Custom External STAC Process need to export a file, the result is not a file but : " + result.getClass().getName(), this);
                }

                ds = DataStores.open(result);

            } catch (NoSuchIdentifierException | DataStoreException ex) {
                throw new ProcessException(ex.getLocalizedMessage(), this);
            }
        } else {
            // Use our custom
            try {
                final ProcessDescriptor stacClientDesc = StacClientItemsGetURIsDescriptor.INSTANCE;
                final Parameters paramsLoad = Parameters.castOrWrap(stacClientDesc.getInputDescriptor().createValue());

                paramsLoad.parameter(StacClientItemsGetURIsDescriptor.STAC_URL.getName().getCode()).setValue(stacURL);
                paramsLoad.parameter(StacClientItemsGetURIsDescriptor.SPATIAL_EXTENT.getName().getCode()).setValue(spatialExtent);
                paramsLoad.parameter(StacClientItemsGetURIsDescriptor.TEMPORAL_EXTENT.getName().getCode()).setValue(temporalExtent);
                paramsLoad.parameter(StacClientItemsGetURIsDescriptor.BANDS.getName().getCode()).setValue(bands);

                Map<String, Map<String, Object>> stacQuery;
                try {
                    stacQuery = toStacQuery(properties);
                } catch (RuntimeException ex) {
                    throw new ProcessException("Invalid 'properties' argument: expected a map of STAC property name to a single-node openEO condition process graph (e.g. {\"eo:cloud_cover\": {\"process_graph\": {...}}}). Message : " + ex.getMessage(), this, ex);
                }
                paramsLoad.parameter(StacClientItemsGetURIsDescriptor.QUERY.getName().getCode()).setValue(stacQuery);

                // Uncomment these lines if we need somewhere to set Collection, or the Extractor Class (we have one by default)
                // paramsLoad.parameter(StacClientItemsGetURIsDescriptor.COLLECTION.getName().getCode()).setValue(null);
                // paramsLoad.parameter(StacClientItemsGetURIsDescriptor.EXTRACTOR_CLASS.getName().getCode()).setValue(null);

                final Process stacClientProcess = stacClientDesc.createProcess(paramsLoad);
                List<URI> result = (List<URI>) stacClientProcess.call().parameter(StacClientItemsGetURIsDescriptor.OUTPUT_NAME).getValue();


                // TODO : COVERAGE AGGREGATOR (it not works well for the moment)
                if (result.isEmpty()) {
                    throw new ProcessException("No data was found.", this);
                }
                URI toOpen = result.get(0);
                LOGGER.info("Selected first element from items : " + toOpen.toString());
                // We download for the moment because it can be very very slow to input from distant endpoint
                // Theoretically we have to remove that !
                Path tempDir = Files.createTempDirectory("tempDownload");
                tempDir.toFile().deleteOnExit();

                StacApi client = new StacApi(new OpenApiConfiguration.Builder().updateBaseUri(stacURL).build());
                Path downloadFile = client.downloadFile(toOpen, tempDir);

                //TODO : the resource store is still open but since the data is in memory it should be okay.
                LOGGER.info("Trying to open file...");
                ds = DataStores.open(downloadFile, "NetCDF");
            } catch (DataStoreException ex) {
                throw new ProcessException("Error while opening the datastore with items stored in the external stac collection. Message : " + ex.getMessage(), this, ex);
            } catch (IOException ex) {
                throw new ProcessException("Error while creating the download folder. Message : " + ex.getMessage(), this, ex);
            } catch (ProcessException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new ProcessException("Error while downloading the item. Message : " + ex.getMessage(), this, ex);
            }
        }

        try {
            GridCoverageResource gcr = Utils.getGridCoverageResource(ds, this);

            GridGeometry originGeometry = gcr.getGridGeometry();
            Envelope originEnvelope = Utils.getSourceEnvelope(gcr, this);
            GridGeometry gridGeometry = Utils.generateResultGridGeometry(originEnvelope, originGeometry, spatialExtent, temporalExtent, this);

            GridCoverage gridCoverage = gcr.read(gridGeometry);
            gridCoverage = Utils.selectBands(bands, gridCoverage);

            outputParameters.getOrCreate(OUTPUT).setValue(gridCoverage);

        } catch (DataStoreException ex) {
            throw new ProcessException("Error while opening the datastore with items stored in the external stac collection. Message : " + ex.getMessage(), this);
        }
    }

    /**
     * Translates openEO's {@code load_stac}/{@code load_collection} {@code properties} argument
     * (a map of STAC item property name to a comparison condition expressed as a single-node child
     * process graph, e.g.
     * {@code {"eo:cloud_cover": {"process_graph": {"n1": {"process_id": "lte",
     * "arguments": {"x": {"from_parameter": "x"}, "y": 50}, "result": true}}}}}
     * ) into the generic STAC API Query Extension shape ({@code {"eo:cloud_cover": {"lte": 50}}})
     * expected by {@link StacClientItemsGetURIsDescriptor#QUERY}.
     *
     * NOTE : (only handles the flat single-node comparison shape openEO generates for this
     * argument. Nested/callback conditions are out of scope, same ceiling as the descriptor's
     * "properties" argument elsewhere)
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, Object>> toStacQuery(Map properties) {
        if (properties == null || properties.isEmpty()) {
            return null;
        }
        Map<String, Map<String, Object>> query = new java.util.LinkedHashMap<>();
        for (Object entryObj : properties.entrySet()) {
            Map.Entry<String, Object> entry = (Map.Entry<String, Object>) entryObj;
            Map<String, Object> processGraph = asMap(asMap(entry.getValue()).get("process_graph"));
            for (Object nodeObj : processGraph.values()) {
                Map<String, Object> node = asMap(nodeObj);
                if (!Boolean.TRUE.equals(node.get("result"))) {
                    continue;
                }
                String processId = (String) node.get("process_id");
                Map<String, Object> arguments = asMap(node.get("arguments"));
                query.put(entry.getKey(), Map.of(processId, arguments.get("y")));
                break;
            }
        }
        return query;
    }

    /**
     * @return {@code o} cast to a {@code Map}, or an empty map if {@code o} is not a {@code Map}
     * (defensive, since the openEO condition JSON arrives as untyped nested {@code Map}s).
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : Map.of();
    }
}
