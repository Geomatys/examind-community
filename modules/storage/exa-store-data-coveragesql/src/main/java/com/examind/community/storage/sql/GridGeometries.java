/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com
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
package com.examind.community.storage.sql;

import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.coverage.grid.PixelInCell;

/**
 * A few pre-defined grid geoemtries for product to insert.
 */
final class GridGeometries {

    private GridGeometries() {
    }

    public static final GridGeometry WORLD = new GridGeometry(
            new GridExtent(36000, 18000), PixelInCell.CELL_CORNER,
            MathTransforms.linear(new Matrix3(
                    +0.01,     0, -180,
                        0, -0.01,  +90,
                        0,     0,    1)),
            CommonCRS.defaultGeographic());


    public static GridGeometry getWorldGG(Double res) {
        if (res == null) {
            return WORLD;
        }
        return new GridGeometry(
                new GridExtent(Math.round(360 / res) , Math.round(180 / res)), PixelInCell.CELL_CORNER,
            MathTransforms.linear(new Matrix3(
                    res,      0,  -180,
                      0, -1*res,   +90,
                      0,      0,     1)),
            CommonCRS.defaultGeographic());
    }
}