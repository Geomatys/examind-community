/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
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
package org.constellation.dto.service.config.csw;


import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * @author Cédric Briançon (Geomatys)
 */
@XmlRootElement(name="BriefNodeList")
@XmlAccessorType(XmlAccessType.FIELD)
public class BriefNodeList {
    @XmlElement(name = "BriefNode")
    private Collection<BriefNode> list;

    public BriefNodeList() {

    }

    public BriefNodeList(final Collection<BriefNode> list) {
        this.list = list;
    }

    public Collection<BriefNode> getList() {
        if(list == null){
            list = new ArrayList<>();
        }
        return list;
    }

    public void setList(final Collection<BriefNode> list) {
        this.list = list;
    }

    public boolean isEmpty() {
        return getList().isEmpty();
    }

    public int size() {
        return getList().size();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("[BriefNodeList]:\n");
        if (list != null) {
            for (BriefNode n : list) {
                sb.append(n).append(",");
            }
        }
        return sb.toString();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() == obj.getClass()) {
            final BriefNodeList that = (BriefNodeList) obj;
            return Objects.equals(this.list, that.list);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 11 * hash + (this.list != null ? this.list.hashCode() : 0);
        return hash;
    }
}
