package com.examind.openeo.api.rest.capabilities;

import com.examind.wps.api.WPSWorker;
import org.constellation.api.ServiceDef;
import org.constellation.api.rest.ErrorMessage;
import org.constellation.ws.MimeType;
import org.constellation.ws.Worker;
import org.constellation.ws.rs.OGCWebService;
import org.constellation.ws.rs.ResponseObject;
import org.geotoolkit.atom.xml.Link;
import org.geotoolkit.openeo.capabilities.dto.Argument;
import org.geotoolkit.openeo.capabilities.dto.Billing;
import org.geotoolkit.openeo.capabilities.dto.Capabilities;
import org.geotoolkit.openeo.capabilities.dto.Conformance;
import org.geotoolkit.openeo.capabilities.dto.Endpoint;
import org.geotoolkit.openeo.capabilities.dto.FileFormat;
import org.geotoolkit.openeo.capabilities.dto.FileFormats;
import org.geotoolkit.openeo.capabilities.dto.ServiceType;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import static com.examind.openeo.api.rest.capabilities.AtomLinkBuilder.buildDocumentLinks;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 * Open EO Capabilities API.
 *
 * @author Quentin BIALOTA (Geomatys)
 * TODO: When the examind refactor has been done so that there are no longer any services, transfer openEo to a dedicated module (no longer linked to wps).
 */
@RestController
@RequestMapping("openeo/{serviceId:.+}")
public class OpenEOCapabilitiesAPI extends OGCWebService<WPSWorker> {

    /**
     * List of conformance classes supported by this server.
     */
    private static final List<String> CONFORMS = Arrays.asList(
            "https://api.openeo.org/1.2.0",
            "https://api.openeo.org/extensions/commercial-data/0.1.0",
            "https://api.openeo.org/extensions/federation/0.1.0",
            "https://api.stacspec.org/v1.0.0/collections"
    );

    /**
     * Default constructor.
     */
    public OpenEOCapabilitiesAPI() {
        // here we use wcs for worker retrieval purpose
        super(ServiceDef.Specification.WPS);
    }

    /**
     * {@inheritDoc}
     * @param objectRequest if the server receive a POST request in XML,
     *        this object contain the request. Else for a GET or a POST kvp
     *        request this parameter is {@code null}
     *
     * @param worker the selected worker on which apply the request.
     *
     * @return
     */
    @Override
    protected ResponseObject treatIncomingRequest(Object objectRequest, WPSWorker worker) {
        String format = "application/json";

        //For the moment only json format is accepted
        MediaType media = MediaType.APPLICATION_JSON;

        Capabilities capabilities = buildCapabilitiesPage(format, worker.getId());
        return new ResponseObject(capabilities, media, HttpStatus.OK);
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
     * Get the OpenEO capabilities.
     * @param serviceId the service identifier
     * @return the capabilities document
     */
    @RequestMapping(value="/", method = GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity getCapabilities(@PathVariable("serviceId") String serviceId) {
        try {
            String format = "application/json";
            MediaType media = MediaType.APPLICATION_JSON;
            Capabilities capabilities = buildCapabilitiesPage(format, serviceId);
            return new ResponseObject(capabilities, media, HttpStatus.OK).getResponseEntity();
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Build the capabilities page.
     * @param format the requested format
     * @param serviceId the service identifier
     * @return the capabilities
     */
    private Capabilities buildCapabilitiesPage(String format, String serviceId) {
        Capabilities capabilities = new Capabilities();
        final boolean asJson = format.contains(MimeType.APP_JSON);
        String url    = getServiceURL() + "/openeo/" + serviceId;

        List<Link> links = new ArrayList<>();
        // TODO restore buildDocumentLinks(url, asJson, links, false);
        links.add(new Link(url + "/conformance",   "conformance",  MimeType.APP_JSON, "OGC Conformance Classes"));
        links.add(new Link(url + "/collections",   "data",         MimeType.APP_JSON, "List of Datasets"));
        links.add(new Link(url + "/file_formats",  "service-desc", MimeType.APP_JSON, "List of supported File Formats"));
        links.add(new Link(url + "/service_types", "service-desc", MimeType.APP_JSON, "List of other services supported by this server"));
        capabilities.setLinks(links);

        List<Endpoint> endpoints = new ArrayList<>();
        endpoints.add(new Endpoint("/file_formats", List.of(Endpoint.MethodsEnum.GET)));
        endpoints.add(new Endpoint("/service_types", List.of(Endpoint.MethodsEnum.GET)));
        endpoints.add(new Endpoint("/collections", List.of(Endpoint.MethodsEnum.GET)));
        endpoints.add(new Endpoint("/collections/{collection_id}", List.of(Endpoint.MethodsEnum.GET)));
        endpoints.add(new Endpoint("/collections/{collection_id}/items", List.of(Endpoint.MethodsEnum.GET)));
        endpoints.add(new Endpoint("/collections/{collection_id}/queryables", List.of(Endpoint.MethodsEnum.GET)));
        endpoints.add(new Endpoint("/credentials/basic", List.of(Endpoint.MethodsEnum.GET)));
        endpoints.add(new Endpoint("/processes", List.of(Endpoint.MethodsEnum.GET)));
        endpoints.add(new Endpoint("/process_graphs", List.of(Endpoint.MethodsEnum.GET, Endpoint.MethodsEnum.POST)));
        endpoints.add(new Endpoint("/process_graphs/{process_graph_id}", List.of(Endpoint.MethodsEnum.GET, Endpoint.MethodsEnum.DELETE, Endpoint.MethodsEnum.PUT)));
        endpoints.add(new Endpoint("/validation", List.of(Endpoint.MethodsEnum.POST)));
        endpoints.add(new Endpoint("/result", List.of(Endpoint.MethodsEnum.POST)));
        endpoints.add(new Endpoint("/jobs", List.of(Endpoint.MethodsEnum.POST, Endpoint.MethodsEnum.GET)));
        endpoints.add(new Endpoint("/jobs/{job_id}/results", List.of(Endpoint.MethodsEnum.POST, Endpoint.MethodsEnum.GET)));
        endpoints.add(new Endpoint("/jobs/{job_id}/results/download", List.of(Endpoint.MethodsEnum.GET)));
        endpoints.add(new Endpoint("/jobs/{job_id}", List.of(Endpoint.MethodsEnum.GET, Endpoint.MethodsEnum.DELETE)));

        capabilities.setEndpoints(endpoints);

        capabilities.setBilling(new Billing("EUR", null, List.of()));

        capabilities.setApiVersion("1.2.0");
        capabilities.setBackendVersion("1.1.2");
        capabilities.setStacVersion("1.0.0");
        capabilities.setId("examind-openeo-endpoint");
        capabilities.setTitle("Examind OpenEO Endpoint");
        capabilities.setDescription("OpenEO endpoint from Examind-Community service");
        capabilities.setConformsTo(CONFORMS);
        capabilities.setProduction(true);

        return capabilities;
    }

    /**
     * Get the conformance classes supported by this server.
     * @return the conformance document
     */
    @RequestMapping(value = "/conformance", method = GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity getConformance() {
        try {
            MediaType media = MediaType.APPLICATION_JSON;
            Conformance conformance = new Conformance(CONFORMS);
            return new ResponseObject(conformance, media, HttpStatus.OK).getResponseEntity();
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Get the .well-known/openeo document.
     * @param serviceId the service identifier
     * @return the well-known document
     */
    @RequestMapping(value = ".well-known/openeo", method = GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity getWellKnown(@PathVariable("serviceId") String serviceId) {
        try {
            putServiceIdParam(serviceId);
            final WPSWorker worker = getWorker(serviceId);

            Map<String, List<Map<String, String>>> versions = new HashMap<>();

            MediaType media = MediaType.APPLICATION_JSON;
            List<Map<String, String>> versionsList = new ArrayList<>();
            Map<String, String> v120 = new HashMap<>();
            v120.put("api_version", "1.2.0");
            v120.put("url", getServiceURL() + "/openeo/" + worker.getId() + "/");
            versionsList.add(v120);

            versions.put("versions", versionsList);

            return new ResponseObject(versions, media, HttpStatus.OK).getResponseEntity();
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Get the supported file formats.
     * @return the file formats document
     */
    @RequestMapping(value = "/file_formats", method = GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity getSupportedFileFormats() {
        try {
            MediaType media = MediaType.APPLICATION_JSON;
            Map<String, FileFormat> outputs = new HashMap<>();
            Map<String, FileFormat> inputs  = new HashMap<>();

            FileFormat gtiffFormat = new FileFormat();
            gtiffFormat.setTitle("GeoTiff");
            gtiffFormat.setDescription("Export to GeoTiff. Support of Cloud-Optimized GeoTiffs (COGs)");
            gtiffFormat.setGisDataTypes(List.of(FileFormat.GisDataTypesEnum.RASTER));
            gtiffFormat.setLinks(List.of(new Link("https://gdal.org/drivers/raster/gtiff.html", "about", MimeType.APP_JSON, "GDAL on the GeoTiff file format and storage options")));
            outputs.put("GTiff", gtiffFormat);
            inputs.put("GTiff", gtiffFormat);

            FileFormat netcdfFormat = new FileFormat();
            netcdfFormat.setTitle("NetCDF");
            netcdfFormat.setDescription("Export to NetCDF.");
            netcdfFormat.setGisDataTypes(List.of(FileFormat.GisDataTypesEnum.RASTER, FileFormat.GisDataTypesEnum.VECTOR));
            netcdfFormat.setLinks(List.of(new Link("https://www.unidata.ucar.edu/software/netcdf/", "about", MimeType.APP_JSON, "Information about the NetCDF file format")));
            outputs.put("NetCDF", netcdfFormat);
            inputs.put("NetCDF", netcdfFormat);

            FileFormat zarrFormat = new FileFormat();
            zarrFormat.setTitle("Zarr");
            zarrFormat.setDescription("Export to Zarr. Support of GeoZarr conventions.");
            zarrFormat.setGisDataTypes(List.of(FileFormat.GisDataTypesEnum.RASTER));
            zarrFormat.setLinks(List.of(new Link("https://zarr-specs.readthedocs.io/en/latest/specs.html", "about", MimeType.APP_JSON, "Information about the Zarr file format")));
            outputs.put("Zarr", zarrFormat);
            inputs.put("Zarr", zarrFormat);

            FileFormats fileFormats = new FileFormats(inputs, outputs);
            return new ResponseObject(fileFormats, media, HttpStatus.OK).getResponseEntity();
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Get other service types supported by this server.
     * @param serviceId the service identifier
     * @return the other service types document
     */
    @RequestMapping(value = "/service_types", method = GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity getOtherServiceTypes(@PathVariable("serviceId") String serviceId) {
        try {
            MediaType media = MediaType.APPLICATION_JSON;

            Map<String, ServiceType> serviceTypes = new HashMap<>();

            // -----------------------------------------------------------
            // 1. Define WMS Service Type
            // -----------------------------------------------------------
            ServiceType wmsType = new ServiceType()
                    .title("Web Map Service")
                    .description("Visualizes the data using the OGC Web Map Service (WMS) protocol.");

            // Define the configuration parameters a user can send when creating a WMS
            Argument wmsVersion = new Argument()
                    .type(Argument.TypeEnum.STRING)
                    .description("The WMS version to use.")
                    ._default("1.3.0")
                    .addEnumItem("1.1.1")
                    .addEnumItem("1.3.0");

            // Add to WMS configuration map
            wmsType.putConfigurationItem("version", wmsVersion);

            serviceTypes.put("WMS", wmsType);

            // -----------------------------------------------------------
            // 2. Define WCS Service Type
            // -----------------------------------------------------------
            ServiceType wcsType = new ServiceType()
                    .title("Web Coverage Service")
                    .description("Provides access to raw data using the OGC Web Coverage Service (WCS) protocol.");

            Argument wcsVersion = new Argument()
                    .type(Argument.TypeEnum.STRING)
                    .description("The WCS version to use.")
                    ._default("2.0.1")
                    .addEnumItem("1.0.0")
                    .addEnumItem("2.0.1");

            // Add to WCS configuration map
            wcsType.putConfigurationItem("version", wcsVersion);

            serviceTypes.put("WCS", wcsType);

            // -----------------------------------------------------------
            // 3. Return Response
            // -----------------------------------------------------------
            return new ResponseObject(serviceTypes, media, HttpStatus.OK).getResponseEntity();

        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }
}
