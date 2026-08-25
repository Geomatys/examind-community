package com.examind.stac.ws.rs;

import com.examind.stac.core.STACWorker;
import java.util.logging.Level;
import org.constellation.api.ServiceDef;
import org.constellation.api.rest.ErrorMessage;
import org.constellation.ws.CstlServiceException;
import org.constellation.ws.Worker;
import org.constellation.ws.rs.OGCWebService;
import org.constellation.ws.rs.ResponseObject;
import org.geotoolkit.ogcapi.dto.common.ConfClasses;
import org.geotoolkit.ogcapi.dto.common.LandingPage;
import org.geotoolkit.stac.dto.Collection;
import org.geotoolkit.stac.dto.Collections;
import org.geotoolkit.stac.dto.Item;
import org.geotoolkit.stac.dto.ItemCollection;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 * STAC API REST endpoint. Exposes the layers configured on a STAC service
 * instance as STAC collections/items.
 *
 * @author Quentin BIALOTA (Geomatys)
 */
@RestController
@RequestMapping("stac/{serviceId:.+}")
public class STACService extends OGCWebService<STACWorker> {

    public STACService() {
        super(ServiceDef.Specification.STAC);
    }

    /**
     * Required by {@link OGCWebService}, but STAC has no KVP request dispatch: every
     * endpoint below is routed directly by Spring's {@code @RequestMapping}. Kept as a
     * fallback returning the landing page.
     */
    @Override
    protected ResponseObject treatIncomingRequest(Object objectRequest, STACWorker worker) {
        return new ResponseObject(worker.getLandingPage(), MediaType.APPLICATION_JSON, HttpStatus.OK);
    }

    @Override
    protected ResponseObject processExceptionResponse(final Exception exc, ServiceDef serviceDef, final Worker w, MediaType mimeType) {
        LOGGER.log(Level.WARNING, exc.getLocalizedMessage(), exc);
        return new ResponseObject(new ErrorMessage(exc));
    }

    /**
     * STAC landing page, with links to the conformance and collections endpoints.
     */
    @RequestMapping(value = {"", "/"}, method = GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity getLandingPage(@PathVariable("serviceId") String serviceId) {
        putServiceIdParam(serviceId);
        final STACWorker worker = getWorker(serviceId);
        if (worker == null) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        final LandingPage landingPage = worker.getLandingPage();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(landingPage);
    }

    /**
     * OGC API / STAC conformance classes implemented by this service.
     */
    @RequestMapping(value = {"/conformance", "/conformance/"}, method = GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity getConformance(@PathVariable("serviceId") String serviceId) {
        putServiceIdParam(serviceId);
        final STACWorker worker = getWorker(serviceId);
        if (worker == null) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        final ConfClasses conformance = worker.getConformance();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(conformance);
    }

    /**
     * All STAC collections exposed by this service instance (one per configured layer).
     */
    @RequestMapping(value = {"/collections", "/collections/"}, method = GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity getCollections(@PathVariable("serviceId") String serviceId) {
        putServiceIdParam(serviceId);
        final STACWorker worker = getWorker(serviceId);
        if (worker == null) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        try {
            final Collections collections = worker.getCollections();
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(collections);
        } catch (CstlServiceException ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * A single STAC collection, or 404 if no layer matches {@code collectionId}.
     */
    @RequestMapping(value = {"/collections/{collectionId}", "/collections/{collectionId}/"}, method = GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity getCollection(@PathVariable("serviceId") String serviceId,
                                         @PathVariable("collectionId") String collectionId) {
        putServiceIdParam(serviceId);
        final STACWorker worker = getWorker(serviceId);
        if (worker == null) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        try {
            final Collection collection = worker.getCollection(collectionId);
            if (collection == null) {
                return new ResponseEntity(HttpStatus.NOT_FOUND);
            }
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(collection);
        } catch (CstlServiceException ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * The items of a collection (a single synthetic item in this v1), or 404 if no layer
     * matches {@code collectionId}.
     */
    @RequestMapping(value = {"/collections/{collectionId}/items", "/collections/{collectionId}/items/"}, method = GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity getItems(@PathVariable("serviceId") String serviceId,
                                    @PathVariable("collectionId") String collectionId) {
        putServiceIdParam(serviceId);
        final STACWorker worker = getWorker(serviceId);
        if (worker == null) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        try {
            final ItemCollection items = worker.getItems(collectionId);
            if (items == null) {
                return new ResponseEntity(HttpStatus.NOT_FOUND);
            }
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(items);
        } catch (CstlServiceException ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * A single STAC item, or 404 if no layer/item matches the given identifiers.
     */
    @RequestMapping(value = {"/collections/{collectionId}/items/{itemId}", "/collections/{collectionId}/items/{itemId}/"}, method = GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity getItem(@PathVariable("serviceId") String serviceId,
                                   @PathVariable("collectionId") String collectionId,
                                   @PathVariable("itemId") String itemId) {
        putServiceIdParam(serviceId);
        final STACWorker worker = getWorker(serviceId);
        if (worker == null) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        try {
            final Item item = worker.getItem(collectionId, itemId);
            if (item == null) {
                return new ResponseEntity(HttpStatus.NOT_FOUND);
            }
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(item);
        } catch (CstlServiceException ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }
}
