/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2024 Geomatys.
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
package org.constellation.dto;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class GridGeometry {

    private CoordinateReferenceSystem crs;
    private GridExtent gridExtent;
    private String gridToCrs;
    private Envelope envelope;

    public GridGeometry() {
    }

    public GridGeometry(CoordinateReferenceSystem crs, GridExtent gridExtent, String gridToCrs, Envelope envelope) {
        this.crs = crs;
        this.gridExtent = gridExtent;
        this.gridToCrs = gridToCrs;
        this.envelope = envelope;
    }

    public CoordinateReferenceSystem getCrs() {
        return crs;
    }

    public void setCrs(CoordinateReferenceSystem crs) {
        this.crs = crs;
    }

    public GridExtent getGridExtent() {
        return gridExtent;
    }

    public void setGridExtent(GridExtent gridExtent) {
        this.gridExtent = gridExtent;
    }

    public String getGridToCrs() {
        return gridToCrs;
    }

    public void setGridToCrs(String gridToCrs) {
        this.gridToCrs = gridToCrs;
    }

    public Envelope getEnvelope() {
        return envelope;
    }

    public void setEnvelope(Envelope envelope) {
        this.envelope = envelope;
    }

}
