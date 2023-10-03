/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com
 *
 * Copyright 2023 Geomatys.
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
package org.constellation.util;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class PreparedSQLBatch {

    private final PreparedStatement stmt;
    private final boolean supportBatch;

    public PreparedSQLBatch(PreparedStatement stmt, boolean supportBatch) {
        this.stmt = stmt;
        this.supportBatch = supportBatch;
    }

    public void addBatch() throws SQLException {
        if (!supportBatch) {
            this.stmt.execute();
        } else {
            this.stmt.addBatch();
        }
    }

    public void executeBatch() throws SQLException {
        if (supportBatch) {
            this.stmt.executeBatch();
        }
    }

    public void setInt(int i, int value) throws SQLException {
        this.stmt.setInt(i, value);
    }

    public void setString(int i, String value) throws SQLException {
        this.stmt.setString(i, value);
    }
}
