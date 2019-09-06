/*
 * Copyright 2019 Geomatys.
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
import org.constellation.dto.DataSourcePathComplete;

/**
 *
 * @author guilhem
 */
@XmlRootElement(name = "DatasourcePathCompleteList")
public class DatasourcePathCompleteList {

    protected int id;

    protected List<DataSourcePathComplete> paths;

    public DatasourcePathCompleteList() {

    }

    public DatasourcePathCompleteList(int id, List<DataSourcePathComplete> paths) {
        this.id = id;
        this.paths = paths;
    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return the paths
     */
    public List<DataSourcePathComplete> getPaths() {
        return paths;
    }

    /**
     * @param paths the paths to set
     */
    public void setPaths(List<DataSourcePathComplete> paths) {
        this.paths = paths;
    }
}
