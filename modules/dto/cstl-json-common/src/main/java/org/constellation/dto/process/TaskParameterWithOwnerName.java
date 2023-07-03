/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2018 Geomatys.
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
package org.constellation.dto.process;

import java.io.Serializable;
import java.util.Objects;
import jakarta.xml.bind.annotation.XmlRootElement;


/**
 * @author Thomas Rouby (Geomatys))
 */
@XmlRootElement
public class TaskParameterWithOwnerName extends TaskParameter implements Serializable {

    private String ownerName;

    public TaskParameterWithOwnerName() {

    }

    public TaskParameterWithOwnerName(TaskParameter param, String ownerName) {
        super(param.getId(), param.getOwner(), param.getName(), param.getDate(),
              param.getProcessAuthority(), param.getProcessCode(), param.getInputs(),
              param.getTrigger(), param.getTriggerType(), param.getType());
        this.ownerName = ownerName;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() == obj.getClass() && super.equals(obj)) {
            final TaskParameterWithOwnerName that = (TaskParameterWithOwnerName) obj;
            return Objects.equals(this.ownerName, that.ownerName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + super.hashCode();
        hash = 59 * hash + Objects.hashCode(this.ownerName);
        return hash;
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(super.toString());
        sb.append("ownerName=").append(ownerName);
        return sb.toString();
    }
}
