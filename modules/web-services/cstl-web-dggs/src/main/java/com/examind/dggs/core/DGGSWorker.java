/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2025 Geomatys.
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
package com.examind.dggs.core;

import java.util.List;
import org.constellation.ws.CstlServiceException;
import org.constellation.ws.Worker;
import org.constellation.ws.rs.ResponseObject;
import org.geotoolkit.ogcapi.dto.common.CollectionDescription;
import org.geotoolkit.ogcapi.dto.common.Collections;
import org.geotoolkit.ogcapi.dto.common.LandingPage;
import org.geotoolkit.ogcapi.dto.dggs.Dggrs;
import org.geotoolkit.ogcapi.dto.dggs.DggrsDefinition;
import org.geotoolkit.ogcapi.dto.dggs.DggrsListResponse;
import org.geotoolkit.ogcapi.dto.dggs.ZoneInfo;
import org.geotoolkit.ogcapi.dto.feature.Functions;
import org.geotoolkit.ogcapi.dto.jsonschema.JSONSchema;
import org.geotoolkit.ogcapi.request.common.GetCollection;
import org.geotoolkit.ogcapi.request.common.GetCollectionList;
import org.geotoolkit.ogcapi.request.common.GetCollectionMetadata;
import org.geotoolkit.ogcapi.request.common.GetCollectionQueryables;
import org.geotoolkit.ogcapi.request.common.GetCollectionSchema;
import org.geotoolkit.ogcapi.request.dggs.GetDggrs;
import org.geotoolkit.ogcapi.request.dggs.GetDggrsDefinition;
import org.geotoolkit.ogcapi.request.dggs.GetDggrsList;
import org.geotoolkit.ogcapi.request.dggs.GetZone;
import org.geotoolkit.ogcapi.request.dggs.GetZoneData;
import org.geotoolkit.ogcapi.request.dggs.GetZoneList;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public interface DGGSWorker extends Worker {

    LandingPage getLandingPage() throws CstlServiceException;

    String getAPI() throws CstlServiceException;

    Collections getCollectionList(GetCollectionList parameters) throws CstlServiceException;

    CollectionDescription getCollection(GetCollection parameters) throws CstlServiceException;

    JSONSchema getCollectionSchema(GetCollectionSchema parameters) throws CstlServiceException;

    JSONSchema getCollectionQueryables(GetCollectionQueryables parameters) throws CstlServiceException;

    String getCollectionMetadata(GetCollectionMetadata parameters) throws CstlServiceException;

    Functions getFunctions() throws CstlServiceException;

    DggrsListResponse getDggrsList(GetDggrsList parameters) throws CstlServiceException;

    Dggrs getDggrs(GetDggrs parameters) throws CstlServiceException;

    DggrsDefinition getDggrsDefinition(GetDggrsDefinition parameters) throws CstlServiceException;

    ResponseObject getDggrsZoneList(GetZoneList parameters) throws CstlServiceException;

    ZoneInfo getDggrsZone(GetZone parameters) throws CstlServiceException;

    ResponseObject getDggrsZoneData(GetZoneData parameters) throws CstlServiceException;

    ResponseObject getDggrsData(GetZoneData parameters) throws CstlServiceException;

}
