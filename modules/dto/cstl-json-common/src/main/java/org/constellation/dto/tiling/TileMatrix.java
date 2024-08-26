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

import org.constellation.dto.GridGeometry;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class TileMatrix {

    private String identifier;
    private GridGeometry tilingScheme;

    public TileMatrix() {
    }

    public TileMatrix(String identifier, GridGeometry tilingScheme) {
        this.identifier = identifier;
        this.tilingScheme = tilingScheme;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public GridGeometry getTilingScheme() {
        return tilingScheme;
    }

    public void setTilingScheme(GridGeometry tilingScheme) {
        this.tilingScheme = tilingScheme;
    }

}
