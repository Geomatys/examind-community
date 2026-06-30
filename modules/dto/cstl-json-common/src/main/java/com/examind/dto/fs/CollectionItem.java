/*
 *    Examind community - An open source and standard compliant SDI
 *
 * Copyright 2025 Geomatys.
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
package com.examind.dto.fs;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author Guilhem Legal (Geomatys) 
 */
public class CollectionItem {
    
    private String provider;
    private String name;
    private String namespace;
    private String title;
    private String alias;
    private String aliasNamespace;
    private String style;
    private List<DimensionItem> dimensions;

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
     * @return the namespace
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * @param namespace the namespace to set
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * @return the alias
     */
    public String getAlias() {
        return alias;
    }

    /**
     * @param alias the alias to set
     */
    public void setAlias(String alias) {
        this.alias = alias;
    }

    /**
     * @return the style
     */
    public String getStyle() {
        return style;
    }

    /**
     * @param style the style to set
     */
    public void setStyle(String style) {
        this.style = style;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return the dimensions
     */
    public List<DimensionItem> getDimensions() {
        if (dimensions == null) dimensions = new ArrayList<>();
        return dimensions;
    }

    /**
     * @param dimensions the dimensions to set
     */
    public void setDimensions(List<DimensionItem> dimensions) {
        this.dimensions = dimensions;
    }

    /**
     * @return the provider
     */
    public String getProvider() {
        return provider;
    }

    /**
     * @param provider the provider to set
     */
    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getAliasNamespace() {
        return aliasNamespace;
    }

    public void setAliasNamespace(String aliasNamespace) {
        this.aliasNamespace = aliasNamespace;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("name: ");
        if (namespace != null) sb.append("[").append(namespace).append("] ");
        sb.append(name).append("\n");
        
        if (provider != null) {
            sb.append("provider: ").append(provider).append("\n");
        }
        if (title != null) {
            sb.append("title: ").append(title).append("\n");
        }
        sb.append("alias: ");
        if (aliasNamespace != null) sb.append("[").append(aliasNamespace).append("] ");
        sb.append(alias).append("\n");
        
        if (style != null) {
            sb.append("style: ").append(style).append("\n");
        }
        
        if (dimensions != null) {
            sb.append("dimensions:\n");
            for (DimensionItem col : dimensions) {
                sb.append(" - ").append(col).append("\n");
            }
        }
        return sb.toString();
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        
        if (obj instanceof CollectionItem that) {
            return Objects.equals(this.namespace, that.namespace) &&
                   Objects.equals(this.name, that.name) &&
                   Objects.equals(this.provider, that.provider) &&
                   Objects.equals(this.title, that.title) &&
                   Objects.equals(this.aliasNamespace, that.aliasNamespace) &&
                   Objects.equals(this.alias, that.alias) &&
                   Objects.equals(this.style, that.style) &&
                   Objects.equals(this.dimensions, that.dimensions);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + Objects.hashCode(this.provider);
        hash = 71 * hash + Objects.hashCode(this.name);
        hash = 71 * hash + Objects.hashCode(this.namespace);
        hash = 71 * hash + Objects.hashCode(this.title);
        hash = 71 * hash + Objects.hashCode(this.alias);
        hash = 71 * hash + Objects.hashCode(this.aliasNamespace);
        hash = 71 * hash + Objects.hashCode(this.style);
        hash = 71 * hash + Objects.hashCode(this.dimensions);
        return hash;
    }
}
