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

package org.constellation.dto.service.config.sos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.constellation.dto.Sensor;
import org.constellation.dto.SensorReference;
import org.constellation.util.NamedId;

/**
 *
 * @author Guilhem Legal
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SensorMLTree extends SensorReference {

    private String type;

    private String name;

    private String description;

    private String owner;

    private Object sml;

    private final Map<String, SensorMLTree> children = new HashMap<>();

    private SensorMLTree parent;

    private Date createDate;

    public SensorMLTree() {

    }

    public SensorMLTree(final Integer id, final String identifier, final String name,
            final String description, final String type, final String owner, final Date time, Object sml) {
        super(id, identifier);
        this.type = type;
        this.owner = owner;
        this.createDate = time;
        this.sml = sml;
        this.name = name;
        this.description = description;
    }

    public SensorMLTree(Sensor s) {
        super(s);
        if (s != null) {
            this.type = s.getType();
            this.createDate = s.getDate();
            this.name = s.getName();
            this.description = s.getDescription();
        }
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the id
     */
    public Integer getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * @param identifier the identifier to set
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the creationDate
     */
    public Date getCreateDate() {
        return createDate;
    }

    /**
     * @param creationDate the creationDate to set
     */
    public void setCreateDate(Date creationDate) {
        this.createDate = creationDate;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * @return the parent
     */
    @JsonIgnore
    public SensorMLTree getParent() {
        return parent;
    }

    /**
     * @return the sml
     */
    @JsonIgnore
    public Object getSml() {
        return sml;
    }

    /**
     * @param sml the sml to set
     */
    public void setSml(Object sml) {
        this.sml = sml;
    }

    /**
     * @return the children
     */
    public List<SensorMLTree> getChildren() {
        return new ArrayList<>(children.values());
    }

    public void addChildren(final SensorMLTree child) {
        child.parent = this;
        children.put(child.getIdentifier(), child);
    }

    /**
     * @param children the children to set
     */
    public void setChildren(List<SensorMLTree> children) {
        children.clear();
        for (SensorMLTree child : children) {
            addChildren(child);
        }
    }

    public void replaceChildren(final SensorMLTree newChild) {
        if (children.containsKey(newChild.getIdentifier())) {
            children.put(newChild.getIdentifier(), newChild);
            newChild.parent = this;
            return;
        }
        throw new IllegalArgumentException("No child to replace:" + newChild.getId());
    }

    public boolean hasChild(final String identifier) {
        return children.containsKey(identifier);
    }

    public SensorMLTree find(final String identifier) {
        if (this.identifier.equals(identifier)) {
            return this;
        }
        for (SensorMLTree child : getChildren()) {
            final SensorMLTree found = child.find(identifier);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    @JsonIgnore
    public List<String> getAllChildrenIds() {
        final List<String> results = new ArrayList<>();
        results.add(identifier);
        for (SensorMLTree child : getChildren()) {
            if (child != null) {
                results.addAll(child.getAllChildrenIds());
            }
        }
        return results;
    }

    @JsonIgnore
    public List<NamedId> getAllChildrenNamedIds() {
        final List<NamedId> results = new ArrayList<>();
        results.add(new NamedId(id, identifier));
        for (SensorMLTree child : getChildren()) {
            if (child != null) {
                results.addAll(child.getAllChildrenNamedIds());
            }
        }
        return results;
    }

    /**
     * Build a Tree of sensor from a flat sensor list.
     * Add a root node at the top.
     *
     * The "alreadyComputedFamilly" flag allow to rebuild a full hierachy from a list
     * where the parent of the nodes are not set.
     * 
     * @param nodeList a flat sensor list.
     * @param alreadyComputedFamilly indicate if the parent/children are already set on the elements.
     * @return
     */
    public static SensorMLTree buildTree(final List<SensorMLTree> nodeList, boolean alreadyComputedFamilly) {
        final SensorMLTree root = new SensorMLTree(null, "root", "root", null, "System", null, null, null);

        for (SensorMLTree node : nodeList) {
            // only link the top sensors to the root
            if (alreadyComputedFamilly) {
                if (node.getParent() == null) {
                    root.addChildren(node);
                }
            // build the full tree
            } else {
                final SensorMLTree parent = getParent(node, nodeList);
                if (parent == null) {
                    root.addChildren(node);
                } else {
                    parent.replaceChildren(node);
                }
            }
        }
        return root;
    }

    private static SensorMLTree getParent(final SensorMLTree current, final List<SensorMLTree> nodeList) {
        for (SensorMLTree node : nodeList) {
            if (node.hasChild(current.getIdentifier())) {
                return node;
            }
        }
        return null;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 47 * hash + Objects.hashCode(this.type);
        hash = 47 * hash + Objects.hashCode(this.createDate);
        hash = 47 * hash + Objects.hashCode(this.id);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (object == null) {
            return false;
        }
        if (this.getClass() == object.getClass()) {
            final SensorMLTree that = (SensorMLTree) object;
            return Objects.equals(this.id,           that.id)   &&
                   Objects.equals(this.identifier,   that.identifier)   &&
                   Objects.equals(this.createDate,   that.createDate)   &&
                   Objects.equals(this.type,         that.type);
        }
        return false;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("[SensorMLTree]\n");
        sb.append("id=").append(id).append('\n');
        if (identifier != null) {
            sb.append("identifier=").append(identifier).append("\n");
        }
        if (name != null) {
            sb.append("name=").append(name).append("\n");
        }
        if (description != null) {
            sb.append("description=").append(description).append("\n");
        }
        if (type != null) {
            sb.append("type=").append(type).append("\n");
        }
        if (createDate != null) {
            sb.append("creationDate=").append(createDate).append("\n");
        }
        if (parent != null) {
            sb.append("parent=").append(parent.id).append("\n");
        }
        if (sml != null) {
            sb.append("sml=").append(sml).append("\n");
        }
        if (children != null) {
            sb.append("children:");
            for (SensorMLTree child : children.values()) {
                sb.append(child.id).append("\n");
            }
        }
        return sb.toString();
    }
}
