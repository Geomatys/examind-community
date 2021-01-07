/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2020 Geomatys.
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
package com.examind.repository.filesystem.dto;

import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;
import org.constellation.dto.MapContextDTO;
import org.constellation.dto.MapContextStyledLayerDTO;

/**
 *
 * @author Guilhem (geomatys)
 */
@XmlRootElement
public class MapContextCompleteDto extends MapContextDTO {

    private List<MapContextStyledLayerDTO> layers;

    public MapContextCompleteDto() {

    }

    public MapContextCompleteDto(MapContextDTO mc) {
        super(mc);
    }

    public MapContextCompleteDto(MapContextDTO mc, List<MapContextStyledLayerDTO> layers) {
        super(mc);
        this.layers = layers;
    }

    public List<MapContextStyledLayerDTO> getLayers() {
        return layers;
    }

    public void setLayers(List<MapContextStyledLayerDTO> layers) {
        this.layers = layers;
    }

}
