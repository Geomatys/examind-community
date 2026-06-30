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

import java.util.Objects;

/**
 *
 * @author Guilhem Legal (Geomatys) 
 */
public class DimensionItem {
    
    private String name;
    private String column;
    private String columnUpper;
    private String columnLower;
    
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
     * @return the column
     */
    public String getColumn() {
        return column;
    }

    /**
     * @param column the column to set
     */
    public void setColumn(String column) {
        this.column = column;
    }

    /**
     * @return the columnUpper
     */
    public String getColumnUpper() {
        return columnUpper;
    }

    /**
     * @param columnUpper the columnUpper to set
     */
    public void setColumnUpper(String columnUpper) {
        this.columnUpper = columnUpper;
    }

    /**
     * @return the columnLower
     */
    public String getColumnLower() {
        return columnLower;
    }

    /**
     * @param columnLower the columnLower to set
     */
    public void setColumnLower(String columnLower) {
        this.columnLower = columnLower;
    }
    
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("name: ").append(name).append("\n");
        
        if (column != null) {
            sb.append("column: ").append(column).append("\n");
        }
        if (columnUpper != null) {
            sb.append("columnUpper: ").append(columnUpper).append("\n");
        }
        if (columnLower != null) {
            sb.append("columnLower: ").append(columnLower).append("\n");
        }
        return sb.toString();
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        
        if (obj instanceof DimensionItem that) {
            return Objects.equals(this.column, that.column) &&
                   Objects.equals(this.name, that.name) &&
                   Objects.equals(this.columnUpper, that.columnUpper) &&
                   Objects.equals(this.columnLower, that.columnLower);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 43 * hash + Objects.hashCode(this.name);
        hash = 43 * hash + Objects.hashCode(this.column);
        hash = 43 * hash + Objects.hashCode(this.columnUpper);
        hash = 43 * hash + Objects.hashCode(this.columnLower);
        return hash;
    }
}
