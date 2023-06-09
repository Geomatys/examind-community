package org.constellation.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Quentin Boileau (Geomatys)
 */
public class SQLUtilitiesTest {

    @Test
    public void testDatabaseURLParser() {
        String postgresURL = "postgres://login:passwd@localhost:5432/database";
        final String postgresJDBC = SQLUtilities.convertToJDBCUrl(postgresURL);
        final String[] userInfo = SQLUtilities.extractUserPasswordUrl(postgresURL);
        Assert.assertEquals("jdbc:postgresql://localhost:5432/database", postgresJDBC);
        Assert.assertEquals("postgres://localhost:5432/database",  userInfo[0]);
        Assert.assertEquals("login", userInfo[1]);
        Assert.assertEquals("passwd", userInfo[2]);

        String derbyMemoryURL = "derby:derby:memory:db";
        final String derbyMemJDBC = SQLUtilities.convertToJDBCUrl(derbyMemoryURL);
        Assert.assertEquals("jdbc:derby:derby:memory:db;create=true", derbyMemJDBC);

        String derbyFSURL = "derby:/folder/derby/database";
        final String derbyFSJDBC = SQLUtilities.convertToJDBCUrl(derbyFSURL);
        Assert.assertEquals("jdbc:derby:/folder/derby/database;create=true", derbyFSJDBC);

    }

}
