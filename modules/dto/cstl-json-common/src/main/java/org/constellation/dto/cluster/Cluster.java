/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2016 Geomatys.
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
package org.constellation.dto.cluster;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class Cluster {

    private List<ClusterMember> members;

    /**
     * @return the cluster members
     */
    public List<ClusterMember> getMembers() {
        if (members == null) {
            members = new ArrayList<>();
        }
        return members;
    }

    /**
     * @param members the members to set
     */
    public void setMembers(List<ClusterMember> members) {
        this.members = members;
    }

}
