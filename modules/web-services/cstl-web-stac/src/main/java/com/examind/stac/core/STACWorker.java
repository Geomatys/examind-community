package com.examind.stac.core;

import org.constellation.ws.CstlServiceException;
import org.constellation.ws.Worker;
import org.geotoolkit.ogcapi.model.common.ConfClasses;
import org.geotoolkit.ogcapi.model.common.LandingPage;
import org.geotoolkit.stac.dto.Collection;
import org.geotoolkit.stac.dto.Collections;
import org.geotoolkit.stac.dto.Item;
import org.geotoolkit.stac.dto.ItemCollection;

/**
 * STAC API worker: exposes the layers configured on a STAC service instance as
 * STAC collections and items.
 *
 * @author Quentin BIALOTA (Geomatys)
 */
public interface STACWorker extends Worker {

    /**
     * Returns the STAC landing page, with links to the conformance and collections endpoints.
     */
    LandingPage getLandingPage();

    /**
     * Returns the list of OGC API / STAC conformance classes implemented by this service.
     */
    ConfClasses getConformance();

    /**
     * Returns all STAC collections exposed by this service instance (one per configured layer).
     */
    Collections getCollections() throws CstlServiceException;

    /**
     * Returns the STAC collection matching the given identifier, or {@code null} if no layer matches it.
     *
     * @param collectionId collection identifier (layer name)
     */
    Collection getCollection(String collectionId) throws CstlServiceException;

    /**
     * Returns the items of the given collection, or {@code null} if no layer matches it.
     *
     * @param collectionId collection identifier (layer name)
     */
    ItemCollection getItems(String collectionId) throws CstlServiceException;

    /**
     * Returns a single item of the given collection, or {@code null} if no layer/item matches it.
     *
     * @param collectionId collection identifier (layer name)
     * @param itemId item identifier
     */
    Item getItem(String collectionId, String itemId) throws CstlServiceException;
}
