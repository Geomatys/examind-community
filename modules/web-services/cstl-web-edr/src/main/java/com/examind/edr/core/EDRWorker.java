/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2026 Geomatys.
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
package com.examind.edr.core;

import org.constellation.ws.CstlServiceException;
import org.constellation.ws.Worker;
import org.constellation.ws.rs.ResponseObject;
import org.geotoolkit.ogcapi.dto.common.Collections;
import org.geotoolkit.ogcapi.dto.common.LandingPage;
import org.geotoolkit.ogcapi.dto.edr.EdrCollection;
import org.geotoolkit.ogcapi.dto.edr.EdrInstances;
import org.geotoolkit.ogcapi.request.common.GetCollection;
import org.geotoolkit.ogcapi.request.common.GetCollectionList;
import org.geotoolkit.ogcapi.request.edr.GetArea;
import org.geotoolkit.ogcapi.request.edr.GetCorridor;
import org.geotoolkit.ogcapi.request.edr.GetCube;
import org.geotoolkit.ogcapi.request.edr.GetEdrInstances;
import org.geotoolkit.ogcapi.request.edr.GetPosition;
import org.geotoolkit.ogcapi.request.edr.GetRadius;
import org.geotoolkit.ogcapi.request.edr.GetTrajectory;

/**
 * Worker contract for an OGC API - Environmental Data Retrieval (EDR) service instance.
 * <p>
 * Queries are executed against examind coverage data providers (see {@link DefaultEDRWorker}),
 * following the same {@code LayerWorker}-based pattern as the DGGS worker rather than delegating
 * to another service's worker.
 *
 * @author Quentin Bialota (Geomatys)
 */
public interface EDRWorker extends Worker {

    /**
     * Builds the service landing page (self, API definition, conformance and collections links).
     *
     * @return the landing page DTO.
     * @throws CstlServiceException never thrown in practice, declared for symmetry with the other operations.
     */
    LandingPage getLandingPage() throws CstlServiceException;

    /**
     * Lists every collection (one per accessible coverage layer) exposed by this service instance.
     *
     * @param parameters the {@code GetCollectionList} request; currently unused, reserved for future filtering.
     * @return the collections listing.
     * @throws CstlServiceException if the underlying layer list cannot be resolved.
     */
    Collections getCollectionList(GetCollectionList parameters) throws CstlServiceException;

    /**
     * Describes a single collection, including its spatial/temporal extent and data-query links.
     *
     * @param parameters the {@code GetCollection} request, carrying the target collection id.
     * @return the collection description.
     * @throws CstlServiceException if the collection id does not resolve to a layer the caller may access.
     */
    EdrCollection getCollection(GetCollection parameters) throws CstlServiceException;

    /**
     * Lists the instances of a collection.
     * <p>
     * Examind's coverage providers have no notion of multiple EDR instances per collection, so this
     * always returns a single synthetic instance mirroring the collection itself.
     *
     * @param parameters the {@code GetEdrInstances} request, carrying the target collection id.
     * @return the instance listing (always exactly one instance).
     * @throws CstlServiceException if the collection id does not resolve to a layer the caller may access.
     */
    EdrInstances getInstances(GetEdrInstances parameters) throws CstlServiceException;

    /**
     * Executes a position query: the coverage evaluated at (or around) a single point.
     *
     * @param parameters the {@code GetPosition} request (coords, z, datetime, parameter-name, crs, limit).
     * @return the resulting coverage encoded as CoverageJSON.
     * @throws CstlServiceException if the collection is unknown, is not a coverage, or the coverage extraction fails.
     */
    ResponseObject getPosition(GetPosition parameters) throws CstlServiceException;

    /**
     * Executes a radius query: the coverage evaluated within a distance ({@code within}/{@code within-units})
     * of a point.
     *
     * @param parameters the {@code GetRadius} request (coords, within, within-units, z, datetime, parameter-name, crs, limit).
     * @return the resulting coverage encoded as CoverageJSON.
     * @throws CstlServiceException if the collection is unknown, is not a coverage, or the coverage extraction fails.
     */
    ResponseObject getRadius(GetRadius parameters) throws CstlServiceException;

    /**
     * Executes an area query: the coverage evaluated over the bounding box of an arbitrary polygon.
     *
     * @param parameters the {@code GetArea} request (coords, z, datetime, parameter-name, crs, resolution-x/y, limit).
     * @return the resulting coverage encoded as CoverageJSON.
     * @throws CstlServiceException if the collection is unknown, is not a coverage, or the coverage extraction fails.
     */
    ResponseObject getArea(GetArea parameters) throws CstlServiceException;

    /**
     * Executes a cube query: the coverage evaluated over an explicit bounding box.
     *
     * @param parameters the {@code GetCube} request (bbox, z, datetime, parameter-name, crs, resolution-x/y/z).
     * @return the resulting coverage encoded as CoverageJSON.
     * @throws CstlServiceException if {@code bbox} is missing, the collection is unknown, is not a coverage,
     *                               or the coverage extraction fails.
     */
    ResponseObject getCube(GetCube parameters) throws CstlServiceException;

    /**
     * Executes a trajectory query: the coverage evaluated over the bounding box of a WKT path.
     *
     * @param parameters the {@code GetTrajectory} request (coords, z, datetime, parameter-name, crs).
     * @return the resulting coverage encoded as CoverageJSON.
     * @throws CstlServiceException if the collection is unknown, is not a coverage, or the coverage extraction fails.
     */
    ResponseObject getTrajectory(GetTrajectory parameters) throws CstlServiceException;

    /**
     * Executes a corridor query: the coverage evaluated over the bounding box of a WKT centerline
     * buffered by {@code corridor-width}.
     *
     * @param parameters the {@code GetCorridor} request (coords, corridor-width, width-units,
     *                    corridor-height, height-units, z, datetime, parameter-name, crs, resolution-x/y).
     * @return the resulting coverage encoded as CoverageJSON.
     * @throws CstlServiceException if the collection is unknown, is not a coverage, or the coverage extraction fails.
     */
    ResponseObject getCorridor(GetCorridor parameters) throws CstlServiceException;

}
