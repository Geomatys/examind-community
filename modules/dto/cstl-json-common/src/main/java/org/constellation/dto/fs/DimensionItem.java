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
package org.constellation.dto.fs;

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
    
}
