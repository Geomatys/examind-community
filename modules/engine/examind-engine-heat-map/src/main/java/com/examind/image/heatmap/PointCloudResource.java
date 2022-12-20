/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com/
 *
 * Copyright 2022 Geomatys.
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
package com.examind.image.heatmap;

import org.apache.sis.storage.DataSet;
import org.apache.sis.storage.DataStoreException;
import org.opengis.geometry.Envelope;

import java.awt.geom.Point2D;
import java.util.stream.Stream;

public interface PointCloudResource extends DataSet {

    /**
     *
     * @param envelope : envelope of the area from which points are requested.
     * @return stream of the points of the resource included in the input Envelope
     */
    Stream<? extends Point2D> points(final Envelope envelope, final boolean parallel) throws DataStoreException;

}
