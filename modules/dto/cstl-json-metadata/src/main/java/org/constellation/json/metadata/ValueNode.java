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
package org.constellation.json.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import static org.constellation.dto.metadata.JsonMetadataConstants.cleanNumeratedPath;
import org.constellation.dto.metadata.Block;
import org.constellation.dto.metadata.Field;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ValueNode {
    
    List<ValueNode> children = new ArrayList<>();
    
    String name;
    
    String type;
    
    String path;
    
    String blockName;
    
    ValueNode parent;
    
    String defaultValue;
    
    String value;
    
    String render;
    
    int ordinal = 0;
    
    boolean strict = false;
    
    boolean multiple = false;
    
   List<String> predefinedValues;
   
   String completion;
    
    public ValueNode(String path, String type, int ordinal, ValueNode parent, String blockName, boolean strict) {
        this.path      = path;
        this.type      = type;
        this.strict    = strict;
        this.blockName = blockName;
        if (path.indexOf('.') != -1) {
            this.name = path.substring(path.lastIndexOf('.') + 1, path.length());
        } else {
            this.name = path;
        }
        this.ordinal = ordinal;
        if (parent != null) {
            parent.addChild(this);
        }
    }
    
    public ValueNode(String path, String type, String defaultValue, String render, int ordinal, String value, ValueNode parent, String fieldName, String completion) {
        this.path = path;
        this.type = type;
        if (path.indexOf('.') != -1) {
            this.name = path.substring(path.lastIndexOf('.') + 1, path.length());
        } else {
            this.name = path;
        }
        this.defaultValue = defaultValue;
        this.render       = render;
        this.ordinal      = ordinal;
        this.value        = value;
        this.blockName    = fieldName;
        this.completion   = completion;
        if (parent != null) {
            parent.addChild(this);
        }
    }
    
    
    
    public ValueNode(ValueNode node, ValueNode parent, int ordinal) {
        this.path         = node.path;
        this.name         = node.name;
        this.type         = node.type;
        this.render       = node.render;
        this.value        = node.value;
        this.defaultValue = node.defaultValue;
        this.strict       = node.strict;
        this.blockName    = node.blockName;
        this.multiple     = node.multiple;
        this.strict       = node.strict;
        this.ordinal      = ordinal;
        this.parent       = parent;
        this.completion   = node.completion;
        this.parent.addChild(this);
        this.predefinedValues = node.predefinedValues;
    }
    
    public ValueNode(ValueNode node) {
        this.path         = node.path;
        this.name         = node.name;
        this.type         = node.type;
        this.render       = node.render;
        this.value        = node.value;
        this.defaultValue = node.defaultValue;
        this.ordinal      = node.ordinal;
        this.parent       = node.parent;
        this.strict       = node.strict;
        this.blockName    = node.blockName;
        this.multiple     = node.multiple;
        this.strict       = node.strict;
        this.completion   = node.completion;
        this.predefinedValues = node.predefinedValues;
        for (ValueNode child : node.children) {
            this.children.add(new ValueNode(child));
        }
    }
    
    public ValueNode(Block block, int ordinal) {
        this.path         = cleanNumeratedPath(block.getPath());
        this.type         = block.getType();
        this.render       = block.getRender();
        this.strict       = block.isStrict();
        this.blockName    = block.getName();
        this.multiple     = block.getMultiplicity() > 1;
        this.ordinal      = ordinal;
        if (path.indexOf('.') != -1) {
            this.name = path.substring(path.lastIndexOf('.') + 1, path.length());
        } else {
            this.name = path;
        }
    }
    
    public ValueNode(Field field, int ordinal) {
        this.path         = cleanNumeratedPath(field.getPath());
        this.type         = field.getType();
        this.render       = field.getRender();
        this.defaultValue = field.defaultValue;
        this.value        = field.value;
        this.multiple     = field.getMultiplicity() > 1;
        this.ordinal      = ordinal;
        this.strict       = field.isStrict();
        this.blockName    = field.getName();
        this.completion   = field.getCompletion();
        this.predefinedValues = field.getPredefinedValues();
        if (path.indexOf('.') != -1) {
            this.name = path.substring(path.lastIndexOf('.') + 1, path.length());
        } else {
            this.name = path;
        }
    }

    public void addChild(ValueNode child) {
        child.parent = this;
        children.add(child);
    }
    
    public boolean isField() {
        return children.isEmpty();
    }
    
    public boolean hashParent(final ValueNode n) {
        if (parent != null) {
            if (parent.simpleEquals(n)) {
                return true;
            } else {
                return parent.hashParent(n);
            }
        }
        return false;
    }
    
    public boolean simpleEquals(ValueNode n) {
        return this.getNumeratedPath().equals(n.getNumeratedPath());
    }
    
    public String getNumeratedPath() {
        if (parent != null) {
            return parent.getNumeratedPath() + '.' + name + "[" + ordinal + "]";
        } else {
            return name + "[" + ordinal + "]";
        }
    }
    
    public List<ValueNode> getChildrenByName(String name) {
        final List<ValueNode> results = new ArrayList<>();
        for (ValueNode node : children) {
            if (node.name.equals(name)) {
                results.add(node);
            }
        }
        return results;
    }
    
    public void updateOrdinal(int i) {
        this.ordinal = i;
    }
    
    public List<String> getPredefinedValues() {
        if (predefinedValues == null) {
            predefinedValues = new ArrayList<>();
        }
        return predefinedValues;
    } 
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("[ValueNode]\n");
        sb.append("name:").append(name).append('\n');
        sb.append("ordinal:").append(ordinal).append('\n');
        if (type != null) {
            sb.append("type:").append(type).append('\n');
        }
        sb.append("path:").append(path).append('\n');
        if (parent != null) {
            sb.append("parent:").append(parent.path).append('[').append(parent.ordinal).append("]\n");
        }
        sb.append("children:").append(children.size()).append('\n');
        sb.append("render:").append(render).append('\n');
        sb.append("value:").append(value).append('\n');
        sb.append("default value:").append(defaultValue).append('\n');
        sb.append("strict:").append(strict).append('\n');
        sb.append("completion:").append(completion).append('\n');
        if (blockName != null){
            sb.append("block name:").append(blockName).append('\n');
        }
        if (predefinedValues != null) {
            sb.append("predefined values:\n");
            for (String pv : predefinedValues) {
                sb.append(pv).append('\n');
            }
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + Objects.hashCode(this.children);
        hash = 97 * hash + Objects.hashCode(this.name);
        hash = 97 * hash + Objects.hashCode(this.type);
        hash = 97 * hash + Objects.hashCode(this.path);
        hash = 97 * hash + Objects.hashCode(this.defaultValue);
        hash = 97 * hash + Objects.hashCode(this.value);
        hash = 97 * hash + Objects.hashCode(this.render);
        hash = 97 * hash + Objects.hashCode(this.strict);
        hash = 97 * hash + Objects.hashCode(this.blockName);
        hash = 97 * hash + Objects.hashCode(this.completion);
        hash = 97 * hash + Objects.hashCode(this.getPredefinedValues());
        hash = 97 * hash + this.ordinal;
        return hash;
    }
    
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ValueNode) {
            final ValueNode that = (ValueNode) obj;
            return Objects.equals(this.defaultValue,  that.defaultValue) &&
                   Objects.equals(this.name,          that.name) &&
                   Objects.equals(this.ordinal,       that.ordinal) && 
                   Objects.equals(this.path,          that.path) && 
                   Objects.equals(this.render,        that.render) && 
                   Objects.equals(this.type,          that.type) && 
                   Objects.equals(this.value,         that.value) && 
                   Objects.equals(this.children,      that.children) && 
                   Objects.equals(this.strict,        that.strict) &&
                   Objects.equals(this.blockName,     that.blockName) &&
                   Objects.equals(this.completion,    that.completion) &&
                   Objects.equals(this.getPredefinedValues(), that.getPredefinedValues()) &&
                   Objects.equals(this.getNumeratedPath(), that.getNumeratedPath());
        }
        return false;
    }

    public String treeRepresentation() {
        StringBuilder sb = new StringBuilder();
        treeRepresentation(sb);
        return sb.toString();
    }

    private void treeRepresentation(StringBuilder sb) {
        ValueNode currentParent = parent;
        List<ValueNode> parents = new ArrayList<>();
        while (currentParent != null) {
            parents.add(0, currentParent);
            currentParent = currentParent.parent;
        }

        for (ValueNode p : parents) {
            sb.append(p.getMargin(0));
        }
        sb.append('\n');

        if (!parents.isEmpty()) {
            parents.remove(parents.size() - 1);
            for (ValueNode p : parents) {
                sb.append(p.getMargin(0));
            }
        }

        if (parent != null) {
            sb.append(parent.getMargin(1));
        }

        
        sb.append(name).append('[').append(ordinal).append(']');

        if (children.isEmpty()) {
            sb.append(" value=[").append(value).append("]\n");
        } else {
            sb.append('\n');
            for (ValueNode child : children) {
                child.treeRepresentation(sb);
            }
        }
    }

    private String getMargin(int mode) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            if (mode == 0 && i == name.length() / 2) {
                sb.append("|");
            } else if (mode == 1 && i >= name.length() / 2) {
                    sb.append("-");
            } else {
                sb.append(" ");
            }
        }
        return sb.toString();
    }
}
