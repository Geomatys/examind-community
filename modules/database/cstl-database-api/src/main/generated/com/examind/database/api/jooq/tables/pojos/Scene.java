/*
 *     Examind Community - An open source and standard compliant SDI
 *     https://community.examind.com/
 * 
 *  Copyright 2022 Geomatys.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/
package com.examind.database.api.jooq.tables.pojos;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.Arrays;


/**
 * Generated DAO object for table admin.scene
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Scene implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String name;
    private Integer mapContextId;
    private Integer dataId;
    private Integer layerId;
    private String type;
    private Integer[] surface;
    private String surfaceParameters;
    private Double surfaceFactor;
    private String status;
    private Long creationDate;
    private Integer minLod;
    private Integer maxLod;
    private Double bboxMinx;
    private Double bboxMiny;
    private Double bboxMaxx;
    private Double bboxMaxy;
    private String time;
    private String extras;
    private Double vectorSimplifyFactor;

    public Scene() {}

    public Scene(Scene value) {
        this.id = value.id;
        this.name = value.name;
        this.mapContextId = value.mapContextId;
        this.dataId = value.dataId;
        this.layerId = value.layerId;
        this.type = value.type;
        this.surface = value.surface;
        this.surfaceParameters = value.surfaceParameters;
        this.surfaceFactor = value.surfaceFactor;
        this.status = value.status;
        this.creationDate = value.creationDate;
        this.minLod = value.minLod;
        this.maxLod = value.maxLod;
        this.bboxMinx = value.bboxMinx;
        this.bboxMiny = value.bboxMiny;
        this.bboxMaxx = value.bboxMaxx;
        this.bboxMaxy = value.bboxMaxy;
        this.time = value.time;
        this.extras = value.extras;
        this.vectorSimplifyFactor = value.vectorSimplifyFactor;
    }

    public Scene(
        Integer id,
        String name,
        Integer mapContextId,
        Integer dataId,
        Integer layerId,
        String type,
        Integer[] surface,
        String surfaceParameters,
        Double surfaceFactor,
        String status,
        Long creationDate,
        Integer minLod,
        Integer maxLod,
        Double bboxMinx,
        Double bboxMiny,
        Double bboxMaxx,
        Double bboxMaxy,
        String time,
        String extras,
        Double vectorSimplifyFactor
    ) {
        this.id = id;
        this.name = name;
        this.mapContextId = mapContextId;
        this.dataId = dataId;
        this.layerId = layerId;
        this.type = type;
        this.surface = surface;
        this.surfaceParameters = surfaceParameters;
        this.surfaceFactor = surfaceFactor;
        this.status = status;
        this.creationDate = creationDate;
        this.minLod = minLod;
        this.maxLod = maxLod;
        this.bboxMinx = bboxMinx;
        this.bboxMiny = bboxMiny;
        this.bboxMaxx = bboxMaxx;
        this.bboxMaxy = bboxMaxy;
        this.time = time;
        this.extras = extras;
        this.vectorSimplifyFactor = vectorSimplifyFactor;
    }

    /**
     * Getter for <code>admin.scene.id</code>.
     */
    @NotNull
    public Integer getId() {
        return this.id;
    }

    /**
     * Setter for <code>admin.scene.id</code>.
     */
    public Scene setId(Integer id) {
        this.id = id;
        return this;
    }

    /**
     * Getter for <code>admin.scene.name</code>.
     */
    @NotNull
    @Size(max = 10000)
    public String getName() {
        return this.name;
    }

    /**
     * Setter for <code>admin.scene.name</code>.
     */
    public Scene setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Getter for <code>admin.scene.map_context_id</code>.
     */
    @NotNull
    public Integer getMapContextId() {
        return this.mapContextId;
    }

    /**
     * Setter for <code>admin.scene.map_context_id</code>.
     */
    public Scene setMapContextId(Integer mapContextId) {
        this.mapContextId = mapContextId;
        return this;
    }

    /**
     * Getter for <code>admin.scene.data_id</code>.
     */
    public Integer getDataId() {
        return this.dataId;
    }

    /**
     * Setter for <code>admin.scene.data_id</code>.
     */
    public Scene setDataId(Integer dataId) {
        this.dataId = dataId;
        return this;
    }

    /**
     * Getter for <code>admin.scene.layer_id</code>.
     */
    public Integer getLayerId() {
        return this.layerId;
    }

    /**
     * Setter for <code>admin.scene.layer_id</code>.
     */
    public Scene setLayerId(Integer layerId) {
        this.layerId = layerId;
        return this;
    }

    /**
     * Getter for <code>admin.scene.type</code>.
     */
    @Size(max = 100)
    public String getType() {
        return this.type;
    }

    /**
     * Setter for <code>admin.scene.type</code>.
     */
    public Scene setType(String type) {
        this.type = type;
        return this;
    }

    /**
     * Getter for <code>admin.scene.surface</code>.
     */
    public Integer[] getSurface() {
        return this.surface;
    }

    /**
     * Setter for <code>admin.scene.surface</code>.
     */
    public Scene setSurface(Integer[] surface) {
        this.surface = surface;
        return this;
    }

    /**
     * Getter for <code>admin.scene.surface_parameters</code>.
     */
    @Size(max = 10000)
    public String getSurfaceParameters() {
        return this.surfaceParameters;
    }

    /**
     * Setter for <code>admin.scene.surface_parameters</code>.
     */
    public Scene setSurfaceParameters(String surfaceParameters) {
        this.surfaceParameters = surfaceParameters;
        return this;
    }

    /**
     * Getter for <code>admin.scene.surface_factor</code>.
     */
    public Double getSurfaceFactor() {
        return this.surfaceFactor;
    }

    /**
     * Setter for <code>admin.scene.surface_factor</code>.
     */
    public Scene setSurfaceFactor(Double surfaceFactor) {
        this.surfaceFactor = surfaceFactor;
        return this;
    }

    /**
     * Getter for <code>admin.scene.status</code>.
     */
    @NotNull
    @Size(max = 100)
    public String getStatus() {
        return this.status;
    }

    /**
     * Setter for <code>admin.scene.status</code>.
     */
    public Scene setStatus(String status) {
        this.status = status;
        return this;
    }

    /**
     * Getter for <code>admin.scene.creation_date</code>.
     */
    @NotNull
    public Long getCreationDate() {
        return this.creationDate;
    }

    /**
     * Setter for <code>admin.scene.creation_date</code>.
     */
    public Scene setCreationDate(Long creationDate) {
        this.creationDate = creationDate;
        return this;
    }

    /**
     * Getter for <code>admin.scene.min_lod</code>.
     */
    public Integer getMinLod() {
        return this.minLod;
    }

    /**
     * Setter for <code>admin.scene.min_lod</code>.
     */
    public Scene setMinLod(Integer minLod) {
        this.minLod = minLod;
        return this;
    }

    /**
     * Getter for <code>admin.scene.max_lod</code>.
     */
    public Integer getMaxLod() {
        return this.maxLod;
    }

    /**
     * Setter for <code>admin.scene.max_lod</code>.
     */
    public Scene setMaxLod(Integer maxLod) {
        this.maxLod = maxLod;
        return this;
    }

    /**
     * Getter for <code>admin.scene.bbox_minx</code>.
     */
    public Double getBboxMinx() {
        return this.bboxMinx;
    }

    /**
     * Setter for <code>admin.scene.bbox_minx</code>.
     */
    public Scene setBboxMinx(Double bboxMinx) {
        this.bboxMinx = bboxMinx;
        return this;
    }

    /**
     * Getter for <code>admin.scene.bbox_miny</code>.
     */
    public Double getBboxMiny() {
        return this.bboxMiny;
    }

    /**
     * Setter for <code>admin.scene.bbox_miny</code>.
     */
    public Scene setBboxMiny(Double bboxMiny) {
        this.bboxMiny = bboxMiny;
        return this;
    }

    /**
     * Getter for <code>admin.scene.bbox_maxx</code>.
     */
    public Double getBboxMaxx() {
        return this.bboxMaxx;
    }

    /**
     * Setter for <code>admin.scene.bbox_maxx</code>.
     */
    public Scene setBboxMaxx(Double bboxMaxx) {
        this.bboxMaxx = bboxMaxx;
        return this;
    }

    /**
     * Getter for <code>admin.scene.bbox_maxy</code>.
     */
    public Double getBboxMaxy() {
        return this.bboxMaxy;
    }

    /**
     * Setter for <code>admin.scene.bbox_maxy</code>.
     */
    public Scene setBboxMaxy(Double bboxMaxy) {
        this.bboxMaxy = bboxMaxy;
        return this;
    }

    /**
     * Getter for <code>admin.scene.time</code>.
     */
    @Size(max = 10000)
    public String getTime() {
        return this.time;
    }

    /**
     * Setter for <code>admin.scene.time</code>.
     */
    public Scene setTime(String time) {
        this.time = time;
        return this;
    }

    /**
     * Getter for <code>admin.scene.extras</code>.
     */
    @Size(max = 10000)
    public String getExtras() {
        return this.extras;
    }

    /**
     * Setter for <code>admin.scene.extras</code>.
     */
    public Scene setExtras(String extras) {
        this.extras = extras;
        return this;
    }

    /**
     * Getter for <code>admin.scene.vector_simplify_factor</code>.
     */
    public Double getVectorSimplifyFactor() {
        return this.vectorSimplifyFactor;
    }

    /**
     * Setter for <code>admin.scene.vector_simplify_factor</code>.
     */
    public Scene setVectorSimplifyFactor(Double vectorSimplifyFactor) {
        this.vectorSimplifyFactor = vectorSimplifyFactor;
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Scene other = (Scene) obj;
        if (this.id == null) {
            if (other.id != null)
                return false;
        }
        else if (!this.id.equals(other.id))
            return false;
        if (this.name == null) {
            if (other.name != null)
                return false;
        }
        else if (!this.name.equals(other.name))
            return false;
        if (this.mapContextId == null) {
            if (other.mapContextId != null)
                return false;
        }
        else if (!this.mapContextId.equals(other.mapContextId))
            return false;
        if (this.dataId == null) {
            if (other.dataId != null)
                return false;
        }
        else if (!this.dataId.equals(other.dataId))
            return false;
        if (this.layerId == null) {
            if (other.layerId != null)
                return false;
        }
        else if (!this.layerId.equals(other.layerId))
            return false;
        if (this.type == null) {
            if (other.type != null)
                return false;
        }
        else if (!this.type.equals(other.type))
            return false;
        if (this.surface == null) {
            if (other.surface != null)
                return false;
        }
        else if (!Arrays.deepEquals(this.surface, other.surface))
            return false;
        if (this.surfaceParameters == null) {
            if (other.surfaceParameters != null)
                return false;
        }
        else if (!this.surfaceParameters.equals(other.surfaceParameters))
            return false;
        if (this.surfaceFactor == null) {
            if (other.surfaceFactor != null)
                return false;
        }
        else if (!this.surfaceFactor.equals(other.surfaceFactor))
            return false;
        if (this.status == null) {
            if (other.status != null)
                return false;
        }
        else if (!this.status.equals(other.status))
            return false;
        if (this.creationDate == null) {
            if (other.creationDate != null)
                return false;
        }
        else if (!this.creationDate.equals(other.creationDate))
            return false;
        if (this.minLod == null) {
            if (other.minLod != null)
                return false;
        }
        else if (!this.minLod.equals(other.minLod))
            return false;
        if (this.maxLod == null) {
            if (other.maxLod != null)
                return false;
        }
        else if (!this.maxLod.equals(other.maxLod))
            return false;
        if (this.bboxMinx == null) {
            if (other.bboxMinx != null)
                return false;
        }
        else if (!this.bboxMinx.equals(other.bboxMinx))
            return false;
        if (this.bboxMiny == null) {
            if (other.bboxMiny != null)
                return false;
        }
        else if (!this.bboxMiny.equals(other.bboxMiny))
            return false;
        if (this.bboxMaxx == null) {
            if (other.bboxMaxx != null)
                return false;
        }
        else if (!this.bboxMaxx.equals(other.bboxMaxx))
            return false;
        if (this.bboxMaxy == null) {
            if (other.bboxMaxy != null)
                return false;
        }
        else if (!this.bboxMaxy.equals(other.bboxMaxy))
            return false;
        if (this.time == null) {
            if (other.time != null)
                return false;
        }
        else if (!this.time.equals(other.time))
            return false;
        if (this.extras == null) {
            if (other.extras != null)
                return false;
        }
        else if (!this.extras.equals(other.extras))
            return false;
        if (this.vectorSimplifyFactor == null) {
            if (other.vectorSimplifyFactor != null)
                return false;
        }
        else if (!this.vectorSimplifyFactor.equals(other.vectorSimplifyFactor))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
        result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
        result = prime * result + ((this.mapContextId == null) ? 0 : this.mapContextId.hashCode());
        result = prime * result + ((this.dataId == null) ? 0 : this.dataId.hashCode());
        result = prime * result + ((this.layerId == null) ? 0 : this.layerId.hashCode());
        result = prime * result + ((this.type == null) ? 0 : this.type.hashCode());
        result = prime * result + ((this.surface == null) ? 0 : Arrays.deepHashCode(this.surface));
        result = prime * result + ((this.surfaceParameters == null) ? 0 : this.surfaceParameters.hashCode());
        result = prime * result + ((this.surfaceFactor == null) ? 0 : this.surfaceFactor.hashCode());
        result = prime * result + ((this.status == null) ? 0 : this.status.hashCode());
        result = prime * result + ((this.creationDate == null) ? 0 : this.creationDate.hashCode());
        result = prime * result + ((this.minLod == null) ? 0 : this.minLod.hashCode());
        result = prime * result + ((this.maxLod == null) ? 0 : this.maxLod.hashCode());
        result = prime * result + ((this.bboxMinx == null) ? 0 : this.bboxMinx.hashCode());
        result = prime * result + ((this.bboxMiny == null) ? 0 : this.bboxMiny.hashCode());
        result = prime * result + ((this.bboxMaxx == null) ? 0 : this.bboxMaxx.hashCode());
        result = prime * result + ((this.bboxMaxy == null) ? 0 : this.bboxMaxy.hashCode());
        result = prime * result + ((this.time == null) ? 0 : this.time.hashCode());
        result = prime * result + ((this.extras == null) ? 0 : this.extras.hashCode());
        result = prime * result + ((this.vectorSimplifyFactor == null) ? 0 : this.vectorSimplifyFactor.hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Scene (");

        sb.append(id);
        sb.append(", ").append(name);
        sb.append(", ").append(mapContextId);
        sb.append(", ").append(dataId);
        sb.append(", ").append(layerId);
        sb.append(", ").append(type);
        sb.append(", ").append(Arrays.deepToString(surface));
        sb.append(", ").append(surfaceParameters);
        sb.append(", ").append(surfaceFactor);
        sb.append(", ").append(status);
        sb.append(", ").append(creationDate);
        sb.append(", ").append(minLod);
        sb.append(", ").append(maxLod);
        sb.append(", ").append(bboxMinx);
        sb.append(", ").append(bboxMiny);
        sb.append(", ").append(bboxMaxx);
        sb.append(", ").append(bboxMaxy);
        sb.append(", ").append(time);
        sb.append(", ").append(extras);
        sb.append(", ").append(vectorSimplifyFactor);

        sb.append(")");
        return sb.toString();
    }
}
