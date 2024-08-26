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
package org.constellation.dto.tiling;

import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.constellation.dto.CoordinateReferenceSystem;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
@XmlRootElement
public final class TileMatrixSet implements Serializable {

    private String identifier;
    private CoordinateReferenceSystem crs;
    private List<TileMatrix> matrices = new ArrayList<>();

    public TileMatrixSet() {
    }

    public TileMatrixSet(String identifier, CoordinateReferenceSystem crs, List<TileMatrix> matrices) {
        this.identifier = identifier;
        this.crs = crs;
        this.matrices = matrices;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public CoordinateReferenceSystem getCrs() {
        return crs;
    }

    public void setCrs(CoordinateReferenceSystem crs) {
        this.crs = crs;
    }

    public List<TileMatrix> getMatrices() {
        return matrices;
    }

    public void setMatrices(List<TileMatrix> matrices) {
        this.matrices = matrices;
    }

}
