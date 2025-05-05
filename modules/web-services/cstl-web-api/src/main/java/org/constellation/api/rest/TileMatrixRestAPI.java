/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014-2024 Geomatys.
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
package org.constellation.api.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import org.apache.sis.coverage.grid.GridOrientation;
import org.apache.sis.coverage.grid.GridRoundingMode;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.IllegalNameException;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.tiling.TiledResource;
import org.apache.sis.storage.tiling.WritableTileMatrix;
import org.apache.sis.storage.tiling.WritableTileMatrixSet;
import org.apache.sis.storage.tiling.WritableTiledResource;
import org.apache.sis.util.iso.Names;
import org.constellation.business.IDataBusiness;
import org.constellation.dto.CoordinateReferenceSystem;
import org.constellation.dto.DataBrief;
import org.constellation.dto.Envelope;
import org.constellation.dto.GridExtent;
import org.constellation.dto.GridGeometry;
import org.constellation.dto.tiling.TileMatrix;
import org.constellation.dto.tiling.TileMatrixSet;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.constellation.provider.Data;
import org.constellation.provider.DataProviders;
import org.constellation.provider.datastore.ResourceProxy;
import org.geotoolkit.storage.multires.DefiningTileMatrix;
import org.geotoolkit.storage.multires.DefiningTileMatrixSet;
import org.opengis.metadata.Identifier;
import org.apache.sis.coverage.grid.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.util.FactoryException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import static org.springframework.web.bind.annotation.RequestMethod.*;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * RestFull API for tiled resource management/operations.
 *
 * @author Johann Sorel (Geomatys)
 */
@RestController
public class TileMatrixRestAPI extends AbstractRestAPI {

    @Autowired
    private IDataBusiness dataBusiness;


    /**
     * Return {@code true} if the specified data is tiled
     *
     * @param dataId data identifier.
     * @param req
     * @return
     */
    @RequestMapping(value = "/tiling/{dataId}/istiled", method = GET)
    public ResponseEntity isTiled(@PathVariable("dataId") int dataId, HttpServletRequest req) {
        try {
            if (toTiledResource(dataId) != null) {
                return new ResponseEntity(true, OK);
            } else {
                return new ResponseEntity(false, OK);
            }
        } catch (ConstellationException ex) {
            LOGGER.log(Level.WARNING, "error while getting data:" + dataId, ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Return {@code true} if the specified data is tiled and writable
     *
     * @param dataId data identifier.
     * @param req
     * @return
     */
    @RequestMapping(value = "/tiling/{dataId}/iswritable", method = GET)
    public ResponseEntity isWritable(@PathVariable("dataId") int dataId, HttpServletRequest req) {
        try {
            final TiledResource tr = toTiledResource(dataId);
            if (tr instanceof WritableTiledResource) {
                return new ResponseEntity(true, OK);
            } else {
                return new ResponseEntity(false, OK);
            }
        } catch (ConstellationException ex) {
            LOGGER.log(Level.WARNING, "error while getting data:" + dataId, ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Get all tile matrix sets in the data.
     *
     * @param dataId tiled data identifier.
     * @return list of all tile matrix sets.
     */
    @RequestMapping(value = "/tiling/{dataId}", method = GET, produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getTileMatrixSets(@PathVariable("dataId") int dataId) {
        try {
            final TiledResource tr = toTiledResource(dataId);
            if (tr == null) throw new ConstellationException("Data is not tiled");

            final List<TileMatrixSet> list = new ArrayList<>();
            for (org.apache.sis.storage.tiling.TileMatrixSet tms : tr.getTileMatrixSets()) {
                list.add(toDTO(tms));
            }
            return new ResponseEntity(list, OK);
        } catch (DataStoreException | ConstellationException ex) {
            LOGGER.log(Level.WARNING, "error while processing tiled resource:" + dataId, ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Create a new tile matrix set.
     *
     * @param dataId tiled data identifier.
     * @return
     */
    @RequestMapping(value = "/tiling/{dataId}", method = POST, consumes=APPLICATION_JSON_VALUE)
    public ResponseEntity createTileMatrixSet(@PathVariable("dataId") int dataId, @RequestBody final TileMatrixSet tms, HttpServletRequest req) throws ConstellationException {
        if (readOnlyAPI) return readOnlyModeActivated();
        try {
            final WritableTiledResource wtr = toWritableTiledResource(dataId, req);
            final WritableTileMatrixSet created = wtr.createTileMatrixSet(fromDTO(tms));
            return new ResponseEntity(created.getIdentifier().toString(), OK);
        } catch (DataStoreException | FactoryException | ConstellationException ex) {
            LOGGER.log(Level.WARNING, "error while processing tiled resource:" + dataId, ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Delete a tile matrix set.
     *
     * @param dataId tiled data identifier.
     * @param tmsId tile matrix set identifier
     * @return
     */
    @RequestMapping(value = "/tiling/{dataId}/{tmsid}", method = DELETE)
    public ResponseEntity deleteTileMatrixSet(@PathVariable("dataId") int dataId, @PathVariable("tmsid") String tmsId, HttpServletRequest req) throws ConstellationException {
        if (readOnlyAPI) return readOnlyModeActivated();
        try {
            final WritableTiledResource wtr = toWritableTiledResource(dataId, req);
            wtr.deleteTileMatrixSet(tmsId);
            return new ResponseEntity(OK);
        } catch (DataStoreException | ConstellationException ex) {
            LOGGER.log(Level.WARNING, "error while processing tiled resource:" + dataId, ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Create a new tile matrix.
     *
     * @param dataId tiled data identifier
     * @param tmsId tile matrix set identifier
     * @return created tile matrix identifier
     */
    @RequestMapping(value = "/tiling/{dataId}/{tmsid}", method = POST, consumes=APPLICATION_JSON_VALUE)
    public ResponseEntity createTileMatrix(@PathVariable("dataId") int dataId, @PathVariable("tmsid") String tmsId, @RequestBody final TileMatrix tm, HttpServletRequest req) throws ConstellationException {
        if (readOnlyAPI) return readOnlyModeActivated();
        try {
            final WritableTiledResource wtr = toWritableTiledResource(dataId, req);
            for (WritableTileMatrixSet tms : wtr.getTileMatrixSets()) {
                if (tms.getIdentifier().toString().equals(tmsId)) {
                    final WritableTileMatrix created = tms.createTileMatrix(fromDTO(tm));
                    return new ResponseEntity(created.getIdentifier().toString(), OK);
                }
            }
            return new ResponseEntity( HttpStatus.BAD_REQUEST);
        } catch (DataStoreException | FactoryException | ConstellationException ex) {
            LOGGER.log(Level.WARNING, "error while processing tiled resource:" + dataId, ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Delete a tile matrix.
     *
     * @param dataId tiled data identifier
     * @param tmsId tile matrix set identifier
     * @param tmId tile matrix identifier
     * @return
     */
    @RequestMapping(value = "/tiling/{dataId}/{tmsid}/{tmid}", method = DELETE)
    public ResponseEntity deleteTileMatrix(@PathVariable("dataId") int dataId, @PathVariable("tmsid") String tmsId, @PathVariable("tmsid") String tmId, HttpServletRequest req) throws ConstellationException {
        if (readOnlyAPI) return readOnlyModeActivated();
        try {
            final WritableTiledResource wtr = toWritableTiledResource(dataId, req);
            for (WritableTileMatrixSet tms : wtr.getTileMatrixSets()) {
                if (tms.getIdentifier().toString().equals(tmsId)) {
                    tms.deleteTileMatrix(tmId);
                    return new ResponseEntity(OK);
                }
            }
            return new ResponseEntity( HttpStatus.BAD_REQUEST);
        } catch (DataStoreException | ConstellationException ex) {
            LOGGER.log(Level.WARNING, "error while processing tiled resource:" + dataId, ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Clear tile matrix area.
     * Removes tiles in given range
     *
     * @param dataId tiled data identifier
     * @param tmsId tile matrix set identifier
     * @param tmId tile matrix identifier
     * @param lowerX gridextent lower X range, inclusive
     * @param lowerY gridextent lower Y range, inclusive
     * @param upperX gridextent upper X range, exclusive
     * @param upperY gridextent upper Y range, exclusive
     * @return
     */
    @RequestMapping(value = "/tiling/{dataId}/{tmsid}/{tmid}/clearRange", method = DELETE)
    public ResponseEntity clearTileMatrixByRange(@PathVariable("dataId") int dataId, @PathVariable("tmsid") String tmsId, @PathVariable("tmsid") String tmId,
            @RequestParam(name="lowerX", required = true) long lowerX,
            @RequestParam(name="lowerX", required = true) long lowerY,
            @RequestParam(name="lowerX", required = true) long upperX,
            @RequestParam(name="lowerX", required = true) long upperY,
            HttpServletRequest req) throws ConstellationException {
        if (readOnlyAPI) return readOnlyModeActivated();
        try {
            final WritableTiledResource wtr = toWritableTiledResource(dataId, req);
            final org.apache.sis.storage.tiling.WritableTileMatrix tm = (org.apache.sis.storage.tiling.WritableTileMatrix) getTileMatrix(wtr, tmsId, tmId);
            final org.apache.sis.coverage.grid.GridExtent clear = new org.apache.sis.coverage.grid.GridExtent(null,new long[]{lowerX, lowerY}, new long[]{upperX, upperY}, false);
            tm.deleteTiles(clear);

            return new ResponseEntity(HttpStatus.OK);
        } catch (IllegalNameException ex) {
            return new ResponseEntity( HttpStatus.BAD_REQUEST);
        } catch (DataStoreException | ConstellationException ex) {
            LOGGER.log(Level.WARNING, "error while processing tiled resource:" + dataId, ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Clear tile matrix area.
     * Removes tiles in given envelope
     *
     * @param dataId tiled data identifier
     * @param tmsId tile matrix set identifier
     * @param tmId tile matrix identifier
     * @param lowerX gridextent lower X range, inclusive
     * @param lowerY gridextent lower Y range, inclusive
     * @param upperX gridextent upper X range, exclusive
     * @param upperY gridextent upper Y range, exclusive
     * @param crs envelope CRS code
     * @return
     */
    @RequestMapping(value = "/tiling/{dataId}/{tmsid}/{tmid}/clearEnvelope", method = DELETE)
    public ResponseEntity clearTileMatrixByEnvelope(@PathVariable("dataId") int dataId, @PathVariable("tmsid") String tmsId, @PathVariable("tmsid") String tmId,
            @RequestParam(name="lowerX", required = true) double lowerX,
            @RequestParam(name="lowerX", required = true) double lowerY,
            @RequestParam(name="lowerX", required = true) double upperX,
            @RequestParam(name="lowerX", required = true) double upperY,
            @RequestParam(name="crs", required = true) String crs,
            HttpServletRequest req) throws ConstellationException {
        if (readOnlyAPI) return readOnlyModeActivated();
        try {
            final WritableTiledResource wtr = toWritableTiledResource(dataId, req);
            final org.apache.sis.storage.tiling.WritableTileMatrix tm = (org.apache.sis.storage.tiling.WritableTileMatrix) getTileMatrix(wtr, tmsId, tmId);
            final GeneralEnvelope env = new GeneralEnvelope(CRS.forCode(crs));
            env.setRange(0, lowerX, upperX);
            env.setRange(1, lowerY, upperY);
            final org.apache.sis.coverage.grid.GridExtent clear = tm.getTilingScheme().derive().rounding(GridRoundingMode.ENCLOSING).subgrid(env).getIntersection();
            tm.deleteTiles(clear);

            return new ResponseEntity(HttpStatus.OK);
        } catch (IllegalNameException ex) {
            return new ResponseEntity( HttpStatus.BAD_REQUEST);
        } catch (FactoryException | DataStoreException | ConstellationException ex) {
            LOGGER.log(Level.WARNING, "error while processing tiled resource:" + dataId, ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Get data resource as a TiledResource.
     *
     * @param dataId tiled data identifier.
     * @return TiledResource or null if data is not tiled
     * @throws ConfigurationException
     * @throws ConstellationException
     */
    private TiledResource toTiledResource(int dataId) throws ConfigurationException, ConstellationException {
        DataBrief db = dataBusiness.getDataBrief(dataId, true, false);
        Data d = DataProviders.getProviderData(db.getProviderId(), db.getNamespace(), db.getName());
        Resource origin = d.getOrigin();
        if (origin instanceof ResourceProxy) {
            origin = ((ResourceProxy)origin).getOrigin();
        }
        if (origin instanceof TiledResource tr) {
            return tr;
        } else {
            return null;
        }
    }

    private WritableTiledResource toWritableTiledResource(int dataId, HttpServletRequest req) throws ConstellationException {
        assertAuthentificated(req);
        TiledResource tr = toTiledResource(dataId);
        if (tr == null) throw new ConstellationException("Data is not tiled");
        if (tr instanceof WritableTiledResource wtr) return wtr;
        throw new ConstellationException("Data is not a writable tiled resource");
    }

    private static org.apache.sis.storage.tiling.TileMatrix getTileMatrix(TiledResource resource, String tmsId, String tmId) throws DataStoreException {
        for (org.apache.sis.storage.tiling.TileMatrixSet tms : resource.getTileMatrixSets()) {
            if (tms.getIdentifier().toString().equals(tmsId)) {
                for (org.apache.sis.storage.tiling.TileMatrix tm : tms.getTileMatrices().values()) {
                    if (tm.getIdentifier().toString().equals(tmId)) {
                        return tm;
                    }
                }
            }
        }
        throw new IllegalNameException("TileMatrix not found");
    }

    public static TileMatrixSet toDTO(org.apache.sis.storage.tiling.TileMatrixSet tms) {
        final TileMatrixSet dto = new TileMatrixSet();
        dto.setIdentifier(tms.getIdentifier().toString());
        dto.setCrs(toDTO(tms.getCoordinateReferenceSystem()));
        final List<TileMatrix> matrices = new ArrayList<>();
        for (org.apache.sis.storage.tiling.TileMatrix tm : tms.getTileMatrices().values()) {
            matrices.add(toDTO(tm));
        }
        dto.setMatrices(matrices);
        return dto;
    }

    public static TileMatrix toDTO(org.apache.sis.storage.tiling.TileMatrix tm) {
        return new TileMatrix(tm.getIdentifier().toString(), toDTO(tm.getTilingScheme()));
    }

    public static GridGeometry toDTO(org.apache.sis.coverage.grid.GridGeometry gg) {
        final GridGeometry dto = new GridGeometry();
        if (gg.isDefined(org.apache.sis.coverage.grid.GridGeometry.CRS)) dto.setCrs(toDTO(gg.getCoordinateReferenceSystem()));
        if (gg.isDefined(org.apache.sis.coverage.grid.GridGeometry.EXTENT)) dto.setGridExtent(toDTO(gg.getExtent()));
        if (gg.isDefined(org.apache.sis.coverage.grid.GridGeometry.GRID_TO_CRS)) dto.setGridToCrs(gg.getGridToCRS(PixelInCell.CELL_CENTER).toWKT());
        if (gg.isDefined(org.apache.sis.coverage.grid.GridGeometry.ENVELOPE)) dto.setEnvelope(toDTO(gg.getEnvelope()));
        return dto;
    }

    public static GridExtent toDTO(org.apache.sis.coverage.grid.GridExtent extent) {
        return new GridExtent(extent.getLow().getCoordinateValues(), extent.getHigh().getCoordinateValues());
    }

    public static Envelope toDTO(org.opengis.geometry.Envelope env) {
        return new Envelope(toDTO(env.getCoordinateReferenceSystem()), env.getLowerCorner().getCoordinates(), env.getUpperCorner().getCoordinates());
    }

    public static CoordinateReferenceSystem toDTO(org.opengis.referencing.crs.CoordinateReferenceSystem crs) {
        final Identifier name = crs.getName();
        if (name != null) {
            final String desc = name.getDescription() != null ? name.getDescription().toString() : "";
            return new CoordinateReferenceSystem(name.getCode(),desc);
        }
        return null;
    }

    public static org.apache.sis.storage.tiling.TileMatrixSet fromDTO(TileMatrixSet dto) throws FactoryException {

        final DefiningTileMatrixSet tms = new DefiningTileMatrixSet(Names.createGenericName(null, null, dto.getIdentifier()), fromDTO(dto.getCrs()), Collections.EMPTY_LIST);
        for (TileMatrix tm : dto.getMatrices()) {
            tms.createTileMatrix(fromDTO(tm));
        }
        return tms;
    }

    public static org.apache.sis.storage.tiling.TileMatrix fromDTO(TileMatrix dto) throws FactoryException {
        return new DefiningTileMatrix(Names.createGenericName(null, null, dto.getIdentifier()), fromDTO(dto.getTilingScheme()), new int[]{256,256});
    }

    public static org.apache.sis.coverage.grid.GridGeometry fromDTO(GridGeometry dto) throws FactoryException {
        org.opengis.referencing.crs.CoordinateReferenceSystem crs = null;
        MathTransform gridToCrs = null;
        org.apache.sis.coverage.grid.GridExtent extent = null;
        org.opengis.geometry.Envelope env = null;
        if (dto.getCrs() != null) crs = fromDTO(dto.getCrs());
        if (dto.getGridToCrs() != null) gridToCrs = DefaultMathTransformFactory.provider().createFromWKT(dto.getGridToCrs());
        if (dto.getEnvelope() != null) env = fromDTO(dto.getEnvelope());
        if (dto.getGridExtent() != null) extent = fromDTO(dto.getGridExtent());

        if (gridToCrs != null) {
            return new org.apache.sis.coverage.grid.GridGeometry(extent, PixelInCell.CELL_CENTER, gridToCrs, crs);
        } else {
            return new org.apache.sis.coverage.grid.GridGeometry(extent, env, GridOrientation.REFLECTION_Y);
        }
    }

    public static org.apache.sis.coverage.grid.GridExtent fromDTO(GridExtent dto) {
        return new org.apache.sis.coverage.grid.GridExtent(null, dto.getLower(), dto.getUpper(), true);
    }

    public static org.opengis.geometry.Envelope fromDTO(Envelope dto) throws FactoryException {
        final GeneralEnvelope env = new GeneralEnvelope(dto.getLower(), dto.getUpper());
        env.setCoordinateReferenceSystem(fromDTO(dto.getCrs()));
        return env;
    }

    public static org.opengis.referencing.crs.CoordinateReferenceSystem fromDTO(CoordinateReferenceSystem dto) throws FactoryException {
        return CRS.forCode(dto.getCode());
    }

}
