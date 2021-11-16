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
package org.constellation.sql;

import java.util.Date;
import java.util.Objects;


/**
 * Representation of a record of the {@code Suites} table.
 *
 * @version $Id$
 * @author Cédric Briançon (Geomatys)
 *
 * @since 0.4
 */
public final class Suite {
    /**
     * The date of the suite.
     */
    private final Date date;

    /**
     * The service name.
     */
    private final String name;

    /**
     * The service version.
     */
    private final String version;

    public Suite(final Date date, final String name, final String version) {
        this.date = date;
        this.name = name;
        this.version = version;
    }

    /**
     * Returns the date of the suite tests.
     */
    public Date getDate() {
        return date;
    }

    /**
     * Returns the service name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the service version.
     */
    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "Suite["+ date +","+ name +","+ version +"]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() == obj.getClass()) {
            final Suite other = (Suite) obj;
            return Objects.equals(this.name, other.name) &&
                   Objects.equals(this.date, other.date) &&
                   Objects.equals(this.version, other.version);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.date);
        hash = 53 * hash + Objects.hashCode(this.name.hashCode());
        hash = 53 * hash + Objects.hashCode(this.version);
        return hash;
    }
}
