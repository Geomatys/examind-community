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


/**
 * Representation of a record of the {@code Services} table.
 *
 * @version $Id$
 * @author Cédric Briançon (Geomatys)
 *
 * @since 0.4
 */
public final class Service {
    /**
     * The service name.
     */
    private final String name;

    /**
     * The service version.
     */
    private final String version;

    public Service(final String name, final String version) {
        this.name = name;
        this.version = version;
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
        return "Service["+ name +","+ version +"]";
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 53 * hash + (this.version != null ? this.version.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Service other = (Service) obj;
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        if ((this.version == null) ? (other.version != null) : !this.version.equals(other.version)) {
            return false;
        }
        return true;
    }

}
