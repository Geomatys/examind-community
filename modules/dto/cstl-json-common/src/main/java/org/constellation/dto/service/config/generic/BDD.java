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


package org.constellation.dto.service.config.generic;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Guilhem Legal
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "BDD")
@JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
public class BDD {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.generic.database");

    public static final String POSTGRES_DRIVER_CLASS = "org.postgresql.Driver";

    public static final String ORACLE_DRIVER_CLASS = "oracle.jdbc.driver.OracleDriver";

    /**
     * The className of the driver
     */
    private String className;

    /**
     * The url to connect the database
     */
    private String connectURL;

    /**
     * The username connecting the database
     */
    private String user;

    /**
     * The password of the user
     */
    private String password;

    /**
     * The database schema.
     */
    private String schema;

    private boolean sharedConnection = false;

    /**
     * Constructor used by JAXB
     */
    public BDD() {

    }

    public BDD(final BDD that) {
        this.className        = that.className;
        this.connectURL       = that.connectURL;
        this.password         = that.password;
        this.schema           = that.schema;
        this.sharedConnection = that.sharedConnection;
        this.user             = that.user;
    }

    /**
     * Build a new DataSource informations.
     *
     * @param className the type of the driver (such as "org.postgresql.Driver").
     * @param connectURL the url of the database.
     * @param user The user name.
     * @param password The password.
     */
    public BDD(String className, String connectURL, String user, String password) {
        this.className  = className;
        this.connectURL = connectURL;
        this.password   = password;
        this.user       = user;
    }

    /**
     * Return the type of the driver (such as "org.postgresql.Driver").
     * @return
     */
    public String getClassName() {
        return className;
    }

    /**
     * Return the url of the database
     * @return
     */
    public String getConnectURL() {
        return connectURL;
    }

    /**
     * Return  The user name.
     * @return
     */
    public String getUser() {
        return user;
    }

    /**
     * return the passsword of the user.
     * @return
     */
    public String getPassword() {
        return password;
    }

    /**
     * extract the host name from the database url.
     * @return
     */
    public String getHostName() {
        if (connectURL != null && connectURL.contains("://")) {
            String hostName = connectURL.substring(connectURL.indexOf("://") + 3);
            if (hostName.indexOf(':') != -1) {
                hostName        = hostName.substring(0, hostName.indexOf(':'));
                return hostName;
            } else {
                return null;
            }
        }
        return null;
    }

    /**
     * extract the database name from the database url.
     * @return
     */
    public String getDatabaseName() {
        if (connectURL != null && connectURL.lastIndexOf('/') != -1) {
            return connectURL.substring(connectURL.lastIndexOf('/') + 1);
        }
        return null;
    }

    /**
     * extract the port number from the database url or 5432 if its not present.
     * @return
     */
    public int getPortNumber() {
        if (connectURL != null && connectURL.lastIndexOf(':') != -1) {
            String portName = connectURL.substring(connectURL.lastIndexOf(':') + 1);
            if (portName.indexOf('/') != -1) {
                portName        = portName.substring(0, portName.indexOf('/'));
                try {
                    return Integer.parseInt(portName);
                } catch (NumberFormatException ex) {
                    LOGGER.log(Level.WARNING, "unable to parse the port number: {0} using default", portName);
                    return 5432;
                }
            } else {
                return 5432;
            }
        }
        return 5432;
    }

    /**
     * @param className the className to set
     */
    public void setClassName(final String className) {
        this.className = className;
    }

    /**
     * @param connectURL the connectURL to set
     */
    public void setConnectURL(final String connectURL) {
        this.connectURL = connectURL;
    }

    /**
     * @param user the user to set
     */
    public void setUser(final String user) {
        this.user = user;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(final String password) {
        this.password = password;
    }

    /**
     * @return the schema
     */
    public String getSchema() {
        return schema;
    }

    /**
     * @param schema the schema to set
     */
    public void setSchema(String schema) {
        this.schema = schema;
    }

    /**
     * @return the sharedConnection
     */
    public boolean isSharedConnection() {
        return sharedConnection;
    }

    /**
     * @param sharedConnection the sharedConnection to set
     */
    public void setSharedConnection(boolean sharedConnection) {
        this.sharedConnection = sharedConnection;
    }

    /**
     * Return a new connection to the database.
     *
     * @return
     * @throws java.sql.SQLException
     *
     * todo The call to Class.forName(...) is not needed anymore since Java 6 and should be removed.
     */
    public Connection getFreshConnection() throws SQLException {

        // by Default  we use the postgres driver.
        if (className == null) {
            className = POSTGRES_DRIVER_CLASS;
        }
        try {
            Class.forName(className);
        } catch (ClassNotFoundException e) {
            // Non-fatal exception, ignore. If there is really a problem, the
            // following line is expected to throw the appropriate SQLException.
        }
        return DriverManager.getConnection(connectURL, user, password);
    }

    public boolean isPostgres() {
        if (className == null) {
            className = POSTGRES_DRIVER_CLASS;
        }
        return className.equals(POSTGRES_DRIVER_CLASS);
    }

    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder("[BDD]");
        s.append("className: ").append(className).append('\n');
        s.append("connectURL: ").append(connectURL).append('\n');
        s.append("user: ").append(user).append('\n');
        s.append("password: ").append(password).append('\n');
        return s.toString();
    }

    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (object == null) {
            return false;
        }
        if (this.getClass() == object.getClass()) {
            final BDD that = (BDD) object;

            return Objects.equals(this.className,  that.className)  &&
                   Objects.equals(this.connectURL, that.connectURL) &&
                   Objects.equals(this.user  ,     that.user)       &&
                   Objects.equals(this.schema  ,   that.schema)     &&
                   Objects.equals(this.password,   that.password);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + (this.className != null ? this.className.hashCode() : 0);
        hash = 59 * hash + (this.connectURL != null ? this.connectURL.hashCode() : 0);
        hash = 59 * hash + (this.user != null ? this.user.hashCode() : 0);
        hash = 59 * hash + (this.password != null ? this.password.hashCode() : 0);
        hash = 59 * hash + (this.schema != null ? this.schema.hashCode() : 0);
        return hash;
    }
}