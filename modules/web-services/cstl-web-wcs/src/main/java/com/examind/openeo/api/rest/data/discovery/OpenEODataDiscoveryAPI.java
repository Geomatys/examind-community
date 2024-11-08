package com.examind.openeo.api.rest.data.discovery;

import com.examind.openeo.api.rest.data.discovery.dto.Collection;
import com.examind.openeo.api.rest.data.discovery.dto.Collections;
import org.constellation.api.ServiceDef;
import org.constellation.api.rest.ErrorMessage;
import org.constellation.coverage.core.WCSWorker;
import org.constellation.ws.CstlServiceException;
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
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import static org.constellation.coverage.core.AtomLinkBuilder.buildDocumentLinks;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 * @author Quentin BIALOTA (Geomatys)
 */
@RestController
@RequestMapping("openeo/{serviceId:.+}/collections")
public class OpenEODataDiscoveryAPI extends GridWebService<WCSWorker> {

    public OpenEODataDiscoveryAPI() {
        // here we use wcs for worker retrieval purpose
        super(ServiceDef.Specification.WCS);
    }

    @Override
    protected ResponseObject treatIncomingRequest(Object objectRequest, WCSWorker worker) {
        String format = "application/json";

        //For the moment only json format is accepted
        MediaType media = MediaType.APPLICATION_JSON;;
        try {
            Collections collections  = buildCollections(worker, format);
            return new ResponseObject(collections, media, HttpStatus.OK);
        } catch (CstlServiceException ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ResponseObject(HttpStatus.NOT_FOUND);
        }
    }

    @Override
    protected ResponseObject processExceptionResponse(final Exception exc, ServiceDef serviceDef, final Worker w, MediaType mimeType) {
        LOGGER.log(Level.WARNING, exc.getLocalizedMessage(), exc);
        return new ResponseObject(new ErrorMessage(exc));
    }

    @RequestMapping(value="/", method = GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity getCollections(@PathVariable("serviceId") String serviceId, int limit){
        putServiceIdParam(serviceId);
        final WCSWorker worker = getWorker(serviceId);

        if (worker != null) {
            try {
                Collections response = buildCollections(worker, MimeType.APP_JSON);
                return new ResponseObject(response, MediaType.APPLICATION_JSON, HttpStatus.OK).getResponseEntity();
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
                return new ErrorMessage(ex).build();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value = "/{collectionId}", method = GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity getCollection(@PathVariable("serviceId") String serviceId,
                                        @PathVariable("collectionId") String collectionId){
        putServiceIdParam(serviceId);
        final WCSWorker worker = getWorker(serviceId);

        if (worker != null) {
            try {
                // if the layer does not exist an exception will be thrown
                final Optional<Collection> layer = worker.getCollections(List.of(collectionId), true).stream().map(collection -> (Collection) collection).findFirst();
                if (!layer.isEmpty()) {
                    return new ResponseObject(layer.get(), MediaType.APPLICATION_JSON, HttpStatus.OK).getResponseEntity();
                }
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
                return new ErrorMessage(ex).build();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    private com.examind.openeo.api.rest.data.discovery.dto.Collections buildCollections(final WCSWorker worker, String format) throws CstlServiceException {
        List<Collection> layers = worker.getCollections(List.of(), true).stream().map(collection -> (Collection) collection).toList();

        List<Link> links = new ArrayList<>();
        final boolean asJson = format.contains(MimeType.APP_JSON);
        String url = getServiceURL() + "/openeo/" + worker.getId() + "/collections";
        buildDocumentLinks(url, asJson, links, false);

        return new Collections(layers, links);
    }
}