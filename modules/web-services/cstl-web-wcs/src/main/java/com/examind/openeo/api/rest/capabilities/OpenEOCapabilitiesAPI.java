package com.examind.openeo.api.rest.capabilities;

import com.examind.openeo.api.rest.capabilities.dto.Billing;
import com.examind.openeo.api.rest.capabilities.dto.Capabilities;
import com.examind.openeo.api.rest.capabilities.dto.Conformance;
import com.examind.openeo.api.rest.capabilities.dto.Endpoint;
import com.examind.openeo.api.rest.capabilities.dto.FileFormat;
import com.examind.openeo.api.rest.capabilities.dto.FileFormats;
import com.examind.openeo.api.rest.capabilities.dto.SecondaryWebServices;
import org.constellation.api.ServiceDef;
import org.constellation.api.rest.ErrorMessage;
import org.constellation.coverage.core.WCSWorker;
import org.constellation.ws.MimeType;
import org.constellation.ws.Worker;
import org.constellation.ws.rs.GridWebService;
import org.constellation.ws.rs.ResponseObject;
import org.geotoolkit.atom.xml.Link;
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

import static org.constellation.coverage.core.AtomLinkBuilder.buildDocumentLinks;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 * @author Quentin BIALOTA (Geomatys)
 * TODO: When the examind refactor has been done so that there are no longer any services, transfer openEo to a dedicated module (no longer linked to wcs).
 */
@RestController
@RequestMapping("openeo/{serviceId:.+}")
public class OpenEOCapabilitiesAPI extends GridWebService<WCSWorker> {

    private static final List<String> CONFORMS = Arrays.asList(
            "https://api.openeo.org/1.2.0",
            "https://api.openeo.org/extensions/commercial-data/0.1.0",
            "https://api.openeo.org/extensions/federation/0.1.0",
            "https://api.stacspec.org/v1.0.0/collections"
    );

    public OpenEOCapabilitiesAPI() {
        // here we use wcs for worker retrieval purpose
        super(ServiceDef.Specification.WCS);
    }

    @Override
    protected ResponseObject treatIncomingRequest(Object objectRequest, WCSWorker worker) {
        String format = "application/json";

        //For the moment only json format is accepted
        MediaType media = MediaType.APPLICATION_JSON;

        Capabilities capabilities = buildCapabilitiesPage(format, worker.getId());
        return new ResponseObject(capabilities, media, HttpStatus.OK);
    }

    @Override
    protected ResponseObject processExceptionResponse(final Exception exc, ServiceDef serviceDef, final Worker w, MediaType mimeType) {
        LOGGER.log(Level.WARNING, exc.getLocalizedMessage(), exc);
        return new ResponseObject(new ErrorMessage(exc));
    }

    @RequestMapping(method = GET, produces = {MediaType.APPLICATION_JSON_VALUE})
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

    private Capabilities buildCapabilitiesPage(String format, String serviceId) {
        Capabilities capabilities = new Capabilities();
        final boolean asJson = format.contains(MimeType.APP_JSON);
        String url    = getServiceURL() + "/openeo/" + serviceId;

        List<Link> links = new ArrayList<>();
        buildDocumentLinks(url, asJson, links, false);
        links.add(new Link(url + "/conformance",   "conformance",  MimeType.APP_JSON, "OGC Conformance Classes"));
        links.add(new Link(url + "/collections",   "data",         MimeType.APP_JSON, "List of Datasets"));
        links.add(new Link(url + "/file_formats",  "service-desc", MimeType.APP_JSON, "List of supported File Formats"));
        links.add(new Link(url + "/service_types", "service-desc", MimeType.APP_JSON, "List of other services supported by this server"));
        capabilities.setLinks(links);

        List<Endpoint> endpoints = new ArrayList<>();
        endpoints.add(new Endpoint("/collections", List.of(Endpoint.MethodsEnum.GET)));
        endpoints.add(new Endpoint("/collections/{collection_id}", List.of(Endpoint.MethodsEnum.GET)));
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

    @RequestMapping(value = "/file_formats", method = GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity getSupportedFileFormats() {
        try {
            MediaType media = MediaType.APPLICATION_JSON;
            Map<String, FileFormat> outputs = new HashMap<>();

            FileFormat gtiffFormat = new FileFormat();
            gtiffFormat.setDescription("Export to GeoTiff. Support of Cloud-Optimized GeoTiffs (COGs)");
            gtiffFormat.setGisDataTypes(List.of(FileFormat.GisDataTypesEnum.RASTER));
            gtiffFormat.setLinks(List.of(new Link("https://gdal.org/drivers/raster/gtiff.html", "about", MimeType.APP_JSON, "DAL on the GeoTiff file format and storage options")));
            outputs.put("GTiff", gtiffFormat);

            FileFormats fileFormats = new FileFormats(null, outputs);
            return new ResponseObject(fileFormats, media, HttpStatus.OK).getResponseEntity();
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value = "/service_types", method = GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity getOtherServiceTypes() {
        try {
            MediaType media = MediaType.APPLICATION_JSON;
            SecondaryWebServices services = new SecondaryWebServices();
            services.setServices(List.of()); //TODO: Add WCS, WMS, ...

            return new ResponseObject(services, media, HttpStatus.OK).getResponseEntity();
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }
}
