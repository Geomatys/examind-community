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

import java.io.Serializable;


/**
 * Generated DAO object for table admin.theater_scene
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class TheaterScene implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer theaterId;
    private Integer sceneId;

    public TheaterScene() {}

    public TheaterScene(TheaterScene value) {
        this.theaterId = value.theaterId;
        this.sceneId = value.sceneId;
    }

    public TheaterScene(
        Integer theaterId,
        Integer sceneId
    ) {
        this.theaterId = theaterId;
        this.sceneId = sceneId;
    }

    /**
     * Getter for <code>admin.theater_scene.theater_id</code>.
     */
    @NotNull
    public Integer getTheaterId() {
        return this.theaterId;
    }

    /**
     * Setter for <code>admin.theater_scene.theater_id</code>.
     */
    public TheaterScene setTheaterId(Integer theaterId) {
        this.theaterId = theaterId;
        return this;
    }

    /**
     * Getter for <code>admin.theater_scene.scene_id</code>.
     */
    @NotNull
    public Integer getSceneId() {
        return this.sceneId;
    }

    /**
     * Setter for <code>admin.theater_scene.scene_id</code>.
     */
    public TheaterScene setSceneId(Integer sceneId) {
        this.sceneId = sceneId;
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
        final TheaterScene other = (TheaterScene) obj;
        if (this.theaterId == null) {
            if (other.theaterId != null)
                return false;
        }
        else if (!this.theaterId.equals(other.theaterId))
            return false;
        if (this.sceneId == null) {
            if (other.sceneId != null)
                return false;
        }
        else if (!this.sceneId.equals(other.sceneId))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.theaterId == null) ? 0 : this.theaterId.hashCode());
        result = prime * result + ((this.sceneId == null) ? 0 : this.sceneId.hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TheaterScene (");

        sb.append(theaterId);
        sb.append(", ").append(sceneId);

        sb.append(")");
        return sb.toString();
    }
}
