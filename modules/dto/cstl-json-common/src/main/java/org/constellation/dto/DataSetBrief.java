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

package org.constellation.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.constellation.dto.metadata.MetadataLightBrief;

/**
 * Class that represents a summary of dataset information necessary for dashboard.
 *
 * @author Mehdi Sidhoum (Geomatys).
 * @version 0.9
 * @since 0.9
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class DataSetBrief extends DataSet implements Serializable {

    private String owner;
    private List<DataBrief> data = new ArrayList<>();
    private List<String> keywords = new ArrayList<>();
    private String resume;
    private Integer mdCompletion;
    private Integer dataCount;
    private List<MetadataLightBrief> metadatas;

    public DataSetBrief() {}

    public DataSetBrief(final Integer id, final String name, final String type, final Integer ownerId, final String owner,
                        final List<DataBrief> children, final Long date, final Integer mdCompletion,
                        final Integer dataCount, final List<MetadataLightBrief> metadatas) {
        super(id, name, ownerId, date, type);
        this.owner = owner;
        this.data = children;
        this.mdCompletion = mdCompletion;
        this.dataCount = dataCount;
        this.metadatas = metadatas;
    }

    public DataSetBrief(final DataSetBrief item, final List<DataBrief> children) {
        super(item.getId(), item.getName(), item.getOwnerId(), item.getDate(), item.getType());
        this.data                      = children;
        this.dataCount                 = item.getDataCount();
        this.owner                     = item.getOwner();
        this.keywords                  = item.getKeywords();
        this.resume                    = item.getResume();
        this.mdCompletion              = item.getMdCompletion();
        this.metadatas                  = item.getMetadatas();
    }

    public String getName() {
        return getIdentifier();
    }

    public void setName(String name) {
        setIdentifier(name);
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public List<DataBrief> getData() {
        return data;
    }

    public void setData(List<DataBrief> children) {
        this.data = children;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public String getResume() {
        return resume;
    }

    public void setResume(String resume) {
        this.resume = resume;
    }

    public Integer getMdCompletion() {
        return mdCompletion;
    }

    public void setMdCompletion(Integer mdCompletion) {
        this.mdCompletion = mdCompletion;
    }

    public Integer getDataCount() {
        return dataCount;
    }

    public void setDataCount(Integer dataCount) {
        this.dataCount = dataCount;
    }

    public List<MetadataLightBrief> getMetadatas() {
        return metadatas;
    }

    public void setMetadatas(List<MetadataLightBrief> metadatas) {
        this.metadatas = metadatas;
    }

    @Override
    public String toString() {
        return "DatasetBrief{" +
                "id='" + getId() + '\'' +
                ", name='" + getIdentifier() + '\'' +
                ", type='" + getType() + '\'' +
                ", owner='" + owner + '\'' +
                ", children=" + data +
                ", keywords=" + keywords +
                ", resume=" + resume +
                ", date=" + getDate() +
                '}';
    }
}
