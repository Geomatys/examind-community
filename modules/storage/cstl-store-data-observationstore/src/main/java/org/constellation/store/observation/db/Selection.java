/*
 *    Examind Community An open source and standard compliant SDI
 *    https://community.examind.com/
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
package org.constellation.store.observation.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.constellation.util.SQLResult;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class Selection {
    
    private List<String> selection = new ArrayList<>();
    
    public Selection(List<String> selection) {
        this.selection = selection != null ? selection : new ArrayList<>();
    }
    
    public void addSelection(Collection<String> selection) {
        if (selection != null) {
            this.selection.addAll(selection);
        }
    }
    
    public Selection() {
        this.selection = new ArrayList<>();
    }
    
    public boolean isSelected(String s) {
        return (selection.isEmpty() || selection.contains(s));
    }
    
    public String getIfSelected(SQLResult rs, String propertyName) throws SQLException {
        return getIfSelected(rs, propertyName, propertyName);
    }
    
    public String getIfSelected(SQLResult rs, String propertyName, String columnName) throws SQLException {
        if (isSelected(propertyName)) {
            return rs.getString(columnName);
        }
        return null;
    }
    
    public String getIfSelected(ResultSet rs, String propertyName) throws SQLException {
        return getIfSelected(rs, propertyName, propertyName);
    }
    
    public String getIfSelected(ResultSet rs, String propertyName, String columnName) throws SQLException {
        if (isSelected(propertyName)) {
            return rs.getString(columnName);
        }
        return null;
    }
}
