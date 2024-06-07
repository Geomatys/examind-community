/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com
 *
 * Copyright 2024 Geomatys.
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
package org.constellation.store.observation.db.model;

import org.geotoolkit.observation.model.Field;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ProcedureInfo {
    public final int pid;
    public final int nbTable;
    public final String procedureId;
    public final String procedureName;
    public final String type;
    public final Field mainField;

    public ProcedureInfo(int pid, int nbTable, String procedureId, String procedureName, String type, Field mainField) {
        this.pid = pid;
        this.nbTable = nbTable;
        this.procedureId = procedureId;
        this.procedureName = procedureName;
        this.type = type;
        this.mainField = mainField;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[ProcedureInfo]");
        sb.append("pid: ").append(pid).append('\n');
        sb.append("nbTable: ").append(nbTable).append('\n');
        sb.append("procedureId: ").append(procedureId).append('\n');
        sb.append("procedureName: ").append(procedureName).append('\n');
        sb.append("type: ").append(type).append('\n');
        sb.append("mainField:\n").append(mainField).append('\n');
        return sb.toString();
    }
    
    
}
